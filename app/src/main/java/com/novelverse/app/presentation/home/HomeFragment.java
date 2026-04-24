package com.novelverse.app.presentation.home;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.novelverse.app.R;
import com.novelverse.app.ads.AdManager;
import com.novelverse.app.data.local.preferences.UserPreferences;
import com.novelverse.app.data.remote.supabase.SupabaseDatabaseService;
import com.novelverse.app.domain.models.ReviewPost;
import com.novelverse.app.presentation.common.adapters.NovelAdapter;
import com.novelverse.app.presentation.home.ReadingChallengeAdapter;
import com.novelverse.app.presentation.home.ContinueReadingAdapter;
import com.novelverse.app.presentation.novellist.NovelListActivity;
import com.novelverse.app.presentation.reviews.ReviewSlideshowAdapter;
import com.novelverse.app.presentation.reviews.ReviewsActivity;
import com.novelverse.app.ui.banner.BannerHelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import com.novelverse.app.presentation.onboarding.OnboardingManager;
import com.novelverse.app.presentation.onboarding.StreakTutorialSheet;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class HomeFragment extends Fragment {

    @Inject AdManager               adManager;
    @Inject UserPreferences         userPreferences;
    @Inject SupabaseDatabaseService dbService;

    private HomeViewModel viewModel;
    private SwipeRefreshLayout swipeRefreshLayout;
    private RecyclerView trendingRecyclerView;
    private RecyclerView newReleasesRecyclerView;
    private LinearLayout genrePillContainer;

    private ContinueReadingAdapter continueAdapter;
    private NovelAdapter trendingAdapter;
    private NovelAdapter newReleasesAdapter;

    private View shimmerTrending, shimmerNewReleases;
    private String selectedGenre = "All";

    // ── Reviews slideshow ─────────────────────────────────────────────────
    private ViewPager2              reviewsPager;
    private LinearLayout            reviewsDots;
    private ReviewSlideshowAdapter  slideshowAdapter;

    /** All fetched reviews — we rotate 5 random ones from this pool. */
    private final List<ReviewPost> reviewPool = new ArrayList<>();

    private static final int SLIDE_INTERVAL_MS  =  5_000;  // 5 s auto-advance
    private static final int REFRESH_INTERVAL_MS = 10 * 60 * 1000; // 10 min pool refresh
    private static final int SLIDE_COUNT         = 5;

    private final Handler slideHandler   = new Handler(Looper.getMainLooper());
    private final Handler refreshHandler = new Handler(Looper.getMainLooper());

    private final Runnable autoSlideRunnable = new Runnable() {
        @Override public void run() {
            if (reviewsPager == null || slideshowAdapter == null
                    || slideshowAdapter.getItemCount() == 0) return;
            int next = (reviewsPager.getCurrentItem() + 1) % slideshowAdapter.getItemCount();
            reviewsPager.setCurrentItem(next, true);
            slideHandler.postDelayed(this, SLIDE_INTERVAL_MS);
        }
    };

    private final Runnable poolRefreshRunnable = new Runnable() {
        @Override public void run() {
            fetchReviewsFromSupabase();
            refreshHandler.postDelayed(this, REFRESH_INTERVAL_MS);
        }
    };

    // ── Fragment lifecycle ────────────────────────────────────────────────

    @Override
    public void onResume() {
        super.onResume();
        if (viewModel != null) viewModel.loadContinueReading();
        // Restart auto-slide when returning to screen
        slideHandler.removeCallbacks(autoSlideRunnable);
        if (slideshowAdapter != null && slideshowAdapter.getItemCount() > 0) {
            slideHandler.postDelayed(autoSlideRunnable, SLIDE_INTERVAL_MS);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        // Pause auto-slide while off-screen to save battery
        slideHandler.removeCallbacks(autoSlideRunnable);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        slideHandler.removeCallbacksAndMessages(null);
        refreshHandler.removeCallbacksAndMessages(null);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
            @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(HomeViewModel.class);
        initViews(view);
        setupAdapters();
        setupReviewsSlideshow(view);
        // Continue Reading
        RecyclerView continueRv = view.findViewById(R.id.continue_reading_recycler);
        if (continueRv != null) {
            continueAdapter = new ContinueReadingAdapter();
            continueAdapter.setOnItemClickListener(novel -> {
                Intent i = new Intent(requireContext(),
                        com.novelverse.app.presentation.novel.reader.ReaderActivity.class);
                i.putExtra(com.novelverse.app.presentation.novel.reader.ReaderActivity.EXTRA_NOVEL_ID,
                        novel.getId());
                if (novel.getCurrentChapterId() != null)
                    i.putExtra(com.novelverse.app.presentation.novel.reader.ReaderActivity.EXTRA_CHAPTER_ID,
                            novel.getCurrentChapterId());
                if (novel.getTitle() != null)
                    i.putExtra(com.novelverse.app.presentation.novel.reader.ReaderActivity.EXTRA_NOVEL_TITLE,
                            novel.getTitle());
                startActivity(i);
            });
            continueRv.setLayoutManager(
                    new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
            continueRv.setAdapter(continueAdapter);
        }
        setupListeners(view);
        observeViewModel();
        loadData();
        maybeShowOnboarding();
    }

    // ── View init ─────────────────────────────────────────────────────────

    private void initViews(View view) {
        swipeRefreshLayout      = view.findViewById(R.id.swipe_refresh);
        trendingRecyclerView    = view.findViewById(R.id.trending_recycler);
        newReleasesRecyclerView = view.findViewById(R.id.new_releases_recycler);
        genrePillContainer      = view.findViewById(R.id.genre_pill_container);
        shimmerTrending         = view.findViewById(R.id.shimmer_trending);
        shimmerNewReleases      = view.findViewById(R.id.shimmer_new_releases);

        androidx.core.widget.NestedScrollView homeScroll = view.findViewById(R.id.home_scroll);
        if (homeScroll != null && getActivity() instanceof HomeActivity) {
            ((HomeActivity) getActivity()).attachNavToScroll(homeScroll);
        }
    }

    private void setupAdapters() {
        trendingAdapter = new NovelAdapter(NovelAdapter.VIEW_TYPE_HORIZONTAL);
        trendingRecyclerView.setLayoutManager(
                new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        trendingRecyclerView.setAdapter(trendingAdapter);

        newReleasesAdapter = new NovelAdapter(NovelAdapter.VIEW_TYPE_HORIZONTAL);
        newReleasesRecyclerView.setLayoutManager(
                new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        newReleasesRecyclerView.setAdapter(newReleasesAdapter);
    }

    // ── Reviews slideshow ─────────────────────────────────────────────────

    private void setupReviewsSlideshow(View view) {
        reviewsPager    = view.findViewById(R.id.reviews_pager);
        reviewsDots     = view.findViewById(R.id.reviews_dots);
        slideshowAdapter = new ReviewSlideshowAdapter();

        reviewsPager.setAdapter(slideshowAdapter);
        reviewsPager.setOffscreenPageLimit(1);

        // Dot indicator — update on page change
        reviewsPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                updateDots(position);
                // Reset auto-slide timer on manual swipe
                slideHandler.removeCallbacks(autoSlideRunnable);
                slideHandler.postDelayed(autoSlideRunnable, SLIDE_INTERVAL_MS);
            }
        });

        // "See All" button
        View seeAll = view.findViewById(R.id.btn_see_all_reviews);
        if (seeAll != null) {
            seeAll.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), ReviewsActivity.class)));
        }

        // Initial data load + start periodic refresh
        fetchReviewsFromSupabase();
        refreshHandler.postDelayed(poolRefreshRunnable, REFRESH_INTERVAL_MS);
    }

    /** Fetch reviews from Supabase. Falls back to mock data if the query fails. */
    private void fetchReviewsFromSupabase() {
        if (!isAdded()) return;
        String token  = userPreferences.getAccessToken();
        String select = "id,rating,review,created_at,user_id," +
                        "profiles!inner(display_name,username,avatar_url)," +
                        "novels!inner(id,title,author_name,cover_url,genre,total_views)," +
                        "user_levels!left(current_level,xp_total)";
        String filter = "review=not.is.null&rating=gte.1";
        String url    = dbService.buildSelectUrl("ratings", select, filter, "created_at.desc")
                        + "&limit=30";

        dbService.rawSelect(url, token, new SupabaseDatabaseService.DatabaseCallback() {
            @Override public void onSuccess(String json) {
                if (!isAdded()) return;
                List<ReviewPost> fetched = parseSlideReviews(json);
                requireActivity().runOnUiThread(() -> {
                    reviewPool.clear();
                    if (fetched != null && !fetched.isEmpty()) {
                        reviewPool.addAll(fetched);
                    }
                    pickAndShowSlides();
                });
            }
            @Override public void onError(String message) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> pickAndShowSlides());
            }
        });
    }

    /** Randomly select SLIDE_COUNT reviews from the pool and push them to the pager. */
    private void pickAndShowSlides() {
        if (!isAdded() || reviewsPager == null) return;

        View section = getView() != null
                ? getView().findViewById(R.id.section_community_reviews) : null;

        if (reviewPool.isEmpty()) {
            if (section != null) section.setVisibility(View.GONE);
            slideHandler.removeCallbacks(autoSlideRunnable);
            return;
        }
        if (section != null) section.setVisibility(View.VISIBLE);

        List<ReviewPost> pool = new ArrayList<>(reviewPool);
        Collections.shuffle(pool);
        List<ReviewPost> slides = pool.subList(0, Math.min(SLIDE_COUNT, pool.size()));
        slideshowAdapter.submitList(slides);
        buildDots(slides.size());
        updateDots(0);
        reviewsPager.setCurrentItem(0, false);
        // Kick off auto-slide
        slideHandler.removeCallbacks(autoSlideRunnable);
        if (slides.size() > 1) {
            slideHandler.postDelayed(autoSlideRunnable, SLIDE_INTERVAL_MS);
        }
    }

    // ── Dot indicators ────────────────────────────────────────────────────

    private void buildDots(int count) {
        if (reviewsDots == null) return;
        reviewsDots.removeAllViews();
        for (int i = 0; i < count; i++) {
            View dot = new View(requireContext());
            reviewsDots.addView(dot, makeDotParams(false));
        }
    }

    private void updateDots(int activeIndex) {
        if (reviewsDots == null) return;
        int count = reviewsDots.getChildCount();
        for (int i = 0; i < count; i++) {
            View dot = reviewsDots.getChildAt(i);
            boolean active = (i == activeIndex);
            dot.setBackground(requireContext().getDrawable(
                    active ? R.drawable.bg_dot_active : R.drawable.bg_dot_inactive));
            // Active dot is wider (pill shape), inactive is smaller circle
            LinearLayout.LayoutParams lp = makeDotParams(active);
            dot.setLayoutParams(lp);
        }
    }

    private LinearLayout.LayoutParams makeDotParams(boolean active) {
        int size   = dp(active ? 8 : 6);
        int width  = active ? dp(22) : size;  // active dot is wider pill
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(width, size);
        lp.setMargins(dp(3), 0, dp(3), 0);
        return lp;
    }

    // ── JSON parse for slides ─────────────────────────────────────────────

    private List<ReviewPost> parseSlideReviews(String json) {
        List<ReviewPost> list = new ArrayList<>();
        try {
            JsonArray arr = JsonParser.parseString(json).getAsJsonArray();
            for (int i = 0; i < arr.size(); i++) {
                JsonObject obj = arr.get(i).getAsJsonObject();
                ReviewPost p = new ReviewPost();
                p.setId(str(obj, "id"));
                p.setUserId(str(obj, "user_id"));
                p.setRating(obj.has("rating") && !obj.get("rating").isJsonNull()
                        ? obj.get("rating").getAsInt() : 0);
                p.setReviewText(str(obj, "review"));
                p.setCreatedAt(str(obj, "created_at"));

                // chaptersCompleted: derive from real user_levels.xp_total if present.
                // Each chapter read awards 10 XP in the gamification system, so
                // xp_total / 10 gives a reasonable approximation. Falls back to 0.
                int chaptersCompleted = 0;
                if (obj.has("user_levels") && !obj.get("user_levels").isJsonNull()) {
                    JsonObject lvl = obj.getAsJsonObject("user_levels");
                    if (lvl.has("xp_total") && !lvl.get("xp_total").isJsonNull()) {
                        long xp = lvl.get("xp_total").getAsLong();
                        chaptersCompleted = (int) Math.max(0, xp / 10);
                    }
                }
                p.setChaptersCompleted(chaptersCompleted);

                if (obj.has("profiles") && !obj.get("profiles").isJsonNull()) {
                    JsonObject prof = obj.getAsJsonObject("profiles");
                    p.setDisplayName(str(prof, "display_name"));
                    String uname = str(prof, "username");
                    p.setUsername(uname != null ? uname
                            : (p.getDisplayName() != null
                               ? p.getDisplayName().toLowerCase().replace(" ", "_") : "reader"));
                    p.setUserAvatarUrl(str(prof, "avatar_url"));
                }
                if (obj.has("novels") && !obj.get("novels").isJsonNull()) {
                    JsonObject nov = obj.getAsJsonObject("novels");
                    p.setNovelId(str(nov, "id"));
                    p.setNovelTitle(str(nov, "title"));
                    p.setNovelAuthor(str(nov, "author_name"));
                    p.setNovelCoverUrl(str(nov, "cover_url"));
                    p.setNovelGenre(str(nov, "genre"));
                }
                list.add(p);
            }
        } catch (Exception e) {
            return null;
        }
        return list;
    }

    private static String str(JsonObject o, String key) {
        return (o.has(key) && !o.get(key).isJsonNull()) ? o.get(key).getAsString() : null;
    }

    /** Rich fallback slides — shown when network is unavailable. */
    private List<ReviewPost> buildFallbackSlides() {
        List<ReviewPost> slides = new ArrayList<>();
        slides.add(new ReviewPost("s1","u1","shadow_quill","Shadow Quill",null,
                520,"n1","The Fallen Throne","Erisa Vale",null,"Fantasy",2024,14200,5,
                "An absolutely riveting read. The world-building is breathtaking — "
                + "Vale weaves political intrigue and magic into something genuinely earned.",
                87,14,"2025-04-16T08:23:00"));
        slides.add(new ReviewPost("s2","u2","moonreader99","Moon Reader",null,
                112,"n2","Crimson Letter","Dae-jung Oh",null,"Romance",2024,9800,4,
                "Sweet, warm, and beautifully paced. The slow burn feels completely "
                + "authentic. The ending made me tear up in the best way.",
                54,8,"2025-04-15T21:05:00"));
        slides.add(new ReviewPost("s3","u3","loreseeker","Lore Seeker",null,
                63,"n3","Circuit Ghosts","Amara Nwosu",null,"Sci-Fi",2023,6400,5,
                "Nwosu has a cyberpunk voice entirely her own. Sharp prose and "
                + "relentless pacing. I read all forty chapters in one weekend.",
                102,22,"2025-04-15T14:30:00"));
        slides.add(new ReviewPost("s4","u4","inkwhisperer","Ink Whisperer",null,
                35,"n4","The Paper Kingdom","Sofia Reyes",null,"Literary",2024,2900,5,
                "A quiet, devastating novel about grief and memory. Reyes writes with "
                + "restraint that makes every beat land twice as hard.",
                73,16,"2025-04-13T15:33:00"));
        slides.add(new ReviewPost("s5","u5","oracle_reads","Oracle Reads",null,
                560,"n5","Midnight Garden","Aiko Tanaka",null,"Fantasy",2024,8600,5,
                "From chapter one this grabbed me and wouldn't let go. My favourite "
                + "discovery on NovelVerse this year.",
                91,18,"2025-04-12T10:50:00"));
        return slides;
    }

    // ── Listeners ─────────────────────────────────────────────────────────

    private void setupListeners(View view) {
        swipeRefreshLayout.setOnRefreshListener(this::loadData);
        trendingAdapter.setOnItemClickListener(novel -> navigateToDetail(novel.getId()));
        newReleasesAdapter.setOnItemClickListener(novel -> navigateToDetail(novel.getId()));

        wireViewAll(view, R.id.btn_see_all_trending,     "trending",     "Trending");
        wireViewAll(view, R.id.btn_see_all_new_releases, "new_releases", "New Releases");
        wireViewAll(view, R.id.btn_see_all_continue,     "continue",     "Continue Reading");
        wireViewAll(view, R.id.btn_see_all_for_you,      "for_you",      "For You");
    }

    private void wireViewAll(View root, int btnId, String section, String title) {
        View btn = root.findViewById(btnId);
        if (btn != null) btn.setOnClickListener(v -> {
            Intent i = new Intent(requireContext(), NovelListActivity.class);
            i.putExtra(NovelListActivity.EXTRA_SECTION, section);
            i.putExtra(NovelListActivity.EXTRA_TITLE, title);
            startActivity(i);
        });
    }

    // ── ViewModel observers ───────────────────────────────────────────────

    private boolean trendingLoaded    = false;
    private boolean newReleasesLoaded = false;

    private void stopRefreshIfDone() {
        if (trendingLoaded && newReleasesLoaded) {
            swipeRefreshLayout.setRefreshing(false);
            trendingLoaded    = false;
            newReleasesLoaded = false;
        }
    }

    private void observeViewModel() {
        viewModel.getTrendingNovels().observe(getViewLifecycleOwner(), novels -> {
            stopShimmer(shimmerTrending, trendingRecyclerView);
            trendingAdapter.submitList(novels);
            trendingLoaded = true;
            stopRefreshIfDone();
        });

        viewModel.getNewReleases().observe(getViewLifecycleOwner(), novels -> {
            stopShimmer(shimmerNewReleases, newReleasesRecyclerView);
            newReleasesAdapter.submitList(novels);
            newReleasesLoaded = true;
            stopRefreshIfDone();
        });

        viewModel.getGenres().observe(getViewLifecycleOwner(), this::buildGenrePills);

        viewModel.getIsLoading().observe(getViewLifecycleOwner(),
            isLoading -> { if (isLoading != null && isLoading) swipeRefreshLayout.setRefreshing(true); });

        viewModel.getContinueReadingNovels().observe(getViewLifecycleOwner(), novels -> {
            if (getView() == null) return;
            View section = getView().findViewById(R.id.section_continue_reading);
            if (section == null) return;
            boolean hasData = novels != null && !novels.isEmpty();
            section.setVisibility(hasData ? View.VISIBLE : View.GONE);
            if (hasData && continueAdapter != null) continueAdapter.submitList(novels);
        });

        viewModel.getForYouNovels().observe(getViewLifecycleOwner(), novels -> {
            if (getView() == null) return;
            View section = getView().findViewById(R.id.section_for_you);
            if (section == null) return;
            boolean hasData = novels != null && !novels.isEmpty();
            section.setVisibility(hasData ? View.VISIBLE : View.GONE);
            if (hasData) bindHorizontalShelf(section, R.id.for_you_recycler, novels);
        });

        viewModel.getActiveChallenges().observe(getViewLifecycleOwner(), challenges -> {
            if (getView() == null) return;
            View section = getView().findViewById(R.id.section_challenges);
            if (section == null) return;
            boolean hasData = challenges != null && !challenges.isEmpty();
            section.setVisibility(hasData ? View.VISIBLE : View.GONE);
            if (hasData) {
                RecyclerView rv = section.findViewById(R.id.challenges_recycler);
                if (rv != null) {
                    if (rv.getLayoutManager() == null)
                        rv.setLayoutManager(new LinearLayoutManager(
                            requireContext(), LinearLayoutManager.HORIZONTAL, false));
                    if (rv.getAdapter() == null) rv.setAdapter(new ReadingChallengeAdapter());
                    ((ReadingChallengeAdapter) rv.getAdapter()).submitList(challenges);
                }
            }
        });
    }

    private void bindHorizontalShelf(View section, int rvId,
            List<com.novelverse.app.domain.models.Novel> novels) {
        RecyclerView rv = section.findViewById(rvId);
        if (rv == null) return;
        if (rv.getLayoutManager() == null)
            rv.setLayoutManager(new LinearLayoutManager(
                requireContext(), LinearLayoutManager.HORIZONTAL, false));
        if (rv.getAdapter() == null) {
            NovelAdapter a = new NovelAdapter(NovelAdapter.VIEW_TYPE_HORIZONTAL);
            a.setOnItemClickListener(n -> navigateToDetail(n.getId()));
            rv.setAdapter(a);
        }
        ((NovelAdapter) rv.getAdapter()).submitList(novels);
    }

    // ── Genre pills ───────────────────────────────────────────────────────

    private void buildGenrePills(List<String> genres) {
        if (genrePillContainer == null) return;
        genrePillContainer.removeAllViews();

        List<String> all = new ArrayList<>();
        all.add("All");
        if (genres != null) {
            for (String g : genres) if (!"All".equalsIgnoreCase(g)) all.add(g);
        }

        int selectedBg      = requireContext().getColor(R.color.pill_selected_bg);
        int selectedText    = requireContext().getColor(R.color.pill_selected_text);
        int unselectedBg    = requireContext().getColor(R.color.pill_unselected_bg);
        int unselectedText  = requireContext().getColor(R.color.pill_unselected_text);
        int unselectedStroke= requireContext().getColor(R.color.pill_unselected_border);

        for (int i = 0; i < all.size(); i++) {
            final String genre = all.get(i);
            boolean active = genre.equals(selectedGenre);

            TextView pill = new TextView(requireContext());
            pill.setText(genre);
            pill.setTextSize(14f);
            pill.setPadding(dp(16), dp(8), dp(16), dp(8));
            pill.setMaxLines(1);

            android.graphics.drawable.GradientDrawable bg =
                    new android.graphics.drawable.GradientDrawable();
            bg.setCornerRadius(dp(100));
            if (active) {
                bg.setColor(selectedBg);
                pill.setTextColor(selectedText);
            } else {
                bg.setColor(unselectedBg);
                bg.setStroke(dp(1), unselectedStroke);
                pill.setTextColor(unselectedText);
            }
            pill.setBackground(bg);

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
            if (i < all.size() - 1) lp.setMarginEnd(dp(8));
            pill.setLayoutParams(lp);
            pill.setClickable(true);
            pill.setFocusable(true);

            final List<String> ref = all;
            pill.setOnClickListener(v -> {
                selectedGenre = genre;
                buildGenrePills(ref.subList(1, ref.size()));
                viewModel.filterByGenre("All".equals(genre) ? null : genre);
            });
            genrePillContainer.addView(pill);
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private void navigateToDetail(String novelId) {
        Intent intent = new Intent(requireContext(),
            com.novelverse.app.presentation.novel.detail.NovelDetailActivity.class);
        intent.putExtra(
            com.novelverse.app.presentation.novel.detail.NovelDetailActivity.EXTRA_NOVEL_ID,
            novelId);
        startActivity(intent);
    }

    private void startShimmers() {
        if (shimmerTrending    != null) shimmerTrending.setVisibility(View.VISIBLE);
        if (shimmerNewReleases != null) shimmerNewReleases.setVisibility(View.VISIBLE);
    }

    private void stopShimmer(View s, RecyclerView rv) {
        if (s  != null) s.setVisibility(View.GONE);
        if (rv != null) rv.setVisibility(View.VISIBLE);
    }

    private void loadData() {
        startShimmers();
        viewModel.loadTrendingNovels();
        viewModel.loadNewReleases();
        viewModel.loadGenres();
        viewModel.loadContinueReading();
        viewModel.loadForYou();
        viewModel.loadActiveChallenges();
    }

    private void maybeShowOnboarding() {
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (!isAdded() || getParentFragmentManager().isStateSaved()) return;
            OnboardingManager mgr = OnboardingManager.get(requireContext());
            if (mgr.shouldShow(OnboardingManager.KEY_STREAK_TUTORIAL)) {
                new StreakTutorialSheet().show(getParentFragmentManager(), StreakTutorialSheet.TAG);
            }
        }, 600);
    }

    private int dp(int v) {
        return Math.round(v * requireContext().getResources().getDisplayMetrics().density);
    }
}

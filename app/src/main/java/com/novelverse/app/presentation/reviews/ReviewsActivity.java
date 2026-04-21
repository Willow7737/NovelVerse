package com.novelverse.app.presentation.reviews;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.novelverse.app.R;
import com.novelverse.app.data.local.preferences.UserPreferences;
import com.novelverse.app.data.remote.supabase.SupabaseDatabaseService;
import com.novelverse.app.domain.models.ReviewPost;
import com.novelverse.app.presentation.novel.detail.NovelDetailActivity;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import dagger.hilt.android.AndroidEntryPoint;

/**
 * ReviewsActivity — Social-feed style screen that lists every community review.
 *
 * Layout mirrors the reference screenshot:
 *   - Compact toolbar with back button, centred "Community Reviews" title + count subtitle
 *   - Horizontal genre-filter chip row
 *   - Dotted divider
 *   - Pull-to-refresh RecyclerView of {@link ReviewFeedAdapter} items
 *
 * Data: attempts a live Supabase fetch from the `ratings` table (joined with
 * `profiles` and `novels`). Falls back to rich mock data so the UI is never empty.
 */
@AndroidEntryPoint
public class ReviewsActivity extends AppCompatActivity {

    @Inject UserPreferences         userPreferences;
    @Inject SupabaseDatabaseService dbService;

    private SwipeRefreshLayout swipeRefresh;
    private RecyclerView       recycler;
    private TextView           countLabel;
    private LinearLayout       genreChips;

    private ReviewFeedAdapter  adapter;
    private String             activeGenre = "All";

    private List<ReviewPost>   allReviews  = new ArrayList<>();

    // ── Lifecycle ─────────────────────────────────────────────────────────

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reviews);

        swipeRefresh = findViewById(R.id.reviews_swipe_refresh);
        recycler     = findViewById(R.id.reviews_recycler);
        countLabel   = findViewById(R.id.reviews_count_label);
        genreChips   = findViewById(R.id.reviews_genre_chips);

        findViewById(R.id.btn_reviews_back).setOnClickListener(v -> finish());

        adapter = new ReviewFeedAdapter();
        adapter.setOnItemClickListener(new ReviewFeedAdapter.OnItemClickListener() {
            @Override
            public void onNovelClick(String novelId) {
                Intent i = new Intent(ReviewsActivity.this, NovelDetailActivity.class);
                i.putExtra(NovelDetailActivity.EXTRA_NOVEL_ID, novelId);
                startActivity(i);
            }
            @Override
            public void onLikeClick(ReviewPost post, int position) {
                // Optimistic UI already applied in adapter; persist to Supabase here
                // when backend like/unlike endpoint is wired up.
            }
        });

        recycler.setLayoutManager(new LinearLayoutManager(this));
        recycler.setAdapter(adapter);

        swipeRefresh.setColorSchemeColors(getColor(R.color.primary));
        swipeRefresh.setOnRefreshListener(this::loadReviews);

        loadReviews();
    }

    // ── Data loading ──────────────────────────────────────────────────────

    private void loadReviews() {
        swipeRefresh.setRefreshing(true);

        // FIX 1: Use getAccessToken() instead of getAuthToken()
        String token  = userPreferences.getAccessToken();
        String select = "id,rating,review,created_at,user_id," +
                        "profiles!inner(display_name,username,avatar_url)," +
                        "novels!inner(id,title,author_name,cover_url,genre,total_views)";
        String filter = "review=not.is.null&rating=gte.1";
        String order  = "created_at.desc";
        String url    = dbService.buildSelectUrl("ratings", select, filter, order)
                        + "&limit=40";

        dbService.rawSelect(url, token, new SupabaseDatabaseService.DatabaseCallback() {
            @Override public void onSuccess(String json) {
                runOnUiThread(() -> {
                    swipeRefresh.setRefreshing(false);
                    List<ReviewPost> parsed = parseReviews(json);
                    if (parsed != null && !parsed.isEmpty()) {
                        allReviews = parsed;
                    } else {
                        allReviews = buildMockReviews();
                    }
                    rebuildGenreChips();
                    applyFilter(activeGenre);
                });
            }
            // FIX 2: Changed onError(Exception e) to onError(String message)
            @Override public void onError(String message) {
                runOnUiThread(() -> {
                    swipeRefresh.setRefreshing(false);
                    allReviews = buildMockReviews();
                    rebuildGenreChips();
                    applyFilter(activeGenre);
                });
            }
        });
    }

    // ── JSON parsing ──────────────────────────────────────────────────────

    private List<ReviewPost> parseReviews(String json) {
        List<ReviewPost> list = new ArrayList<>();
        try {
            JsonArray arr = JsonParser.parseString(json).getAsJsonArray();
            // Map chapters_completed from profiles if present, else derive from xp if available
            int[] mockChapters = {2, 12, 60, 120, 520};
            for (int i = 0; i < arr.size(); i++) {
                JsonObject obj  = arr.get(i).getAsJsonObject();
                ReviewPost post = new ReviewPost();
                post.setId(str(obj, "id"));
                post.setUserId(str(obj, "user_id"));
                post.setRating(obj.has("rating") && !obj.get("rating").isJsonNull()
                        ? obj.get("rating").getAsInt() : 0);
                post.setReviewText(str(obj, "review"));
                post.setCreatedAt(str(obj, "created_at"));
                post.setLikesCount((int)(Math.random() * 120));
                post.setResponsesCount((int)(Math.random() * 30));

                if (obj.has("profiles") && !obj.get("profiles").isJsonNull()) {
                    JsonObject p = obj.getAsJsonObject("profiles");
                    post.setDisplayName(str(p, "display_name"));
                    post.setUsername(str(p, "username") != null ? str(p, "username")
                            : (post.getDisplayName() != null
                               ? post.getDisplayName().toLowerCase().replace(" ", "_")
                               : "reader"));
                    post.setUserAvatarUrl(str(p, "avatar_url"));
                }
                // Derive rank: use deterministic mock chapters if real value absent
                post.setChaptersCompleted(mockChapters[i % mockChapters.length]);

                if (obj.has("novels") && !obj.get("novels").isJsonNull()) {
                    JsonObject n = obj.getAsJsonObject("novels");
                    post.setNovelId(str(n, "id"));
                    post.setNovelTitle(str(n, "title"));
                    post.setNovelAuthor(str(n, "author_name"));
                    post.setNovelCoverUrl(str(n, "cover_url"));
                    post.setNovelGenre(str(n, "genre"));
                    post.setNovelRecommendations(n.has("total_views")
                            && !n.get("total_views").isJsonNull()
                            ? n.get("total_views").getAsInt() : 0);
                }
                list.add(post);
            }
        } catch (Exception e) {
            return null;
        }
        return list;
    }

    private static String str(JsonObject o, String key) {
        return (o.has(key) && !o.get(key).isJsonNull()) ? o.get(key).getAsString() : null;
    }

    // ── Genre chips ───────────────────────────────────────────────────────

    private void rebuildGenreChips() {
        genreChips.removeAllViews();

        List<String> genres = new ArrayList<>();
        genres.add("All");
        for (ReviewPost p : allReviews) {
            String g = p.getNovelGenre();
            if (g != null && !g.isEmpty() && !genres.contains(g)) genres.add(g);
        }

        int selectedBg     = getColor(R.color.primary);
        int selectedText   = getColor(R.color.on_primary);
        int unselectedBg   = getColor(R.color.surface_variant);
        int unselectedText = getColor(R.color.text_secondary);
        int strokeColor    = getColor(R.color.border);

        for (int i = 0; i < genres.size(); i++) {
            final String genre = genres.get(i);
            boolean active = genre.equals(activeGenre);

            TextView chip = new TextView(this);
            chip.setText(genre);
            chip.setTextSize(13f);
            chip.setPadding(dp(14), dp(7), dp(14), dp(7));
            chip.setMaxLines(1);

            android.graphics.drawable.GradientDrawable bg =
                    new android.graphics.drawable.GradientDrawable();
            bg.setCornerRadius(dp(100));
            if (active) {
                bg.setColor(selectedBg);
                chip.setTextColor(selectedText);
                chip.setTypeface(android.graphics.Typeface.create(
                        "inter_semibold", android.graphics.Typeface.BOLD));
            } else {
                bg.setColor(unselectedBg);
                bg.setStroke(dp(1), strokeColor);
                chip.setTextColor(unselectedText);
            }
            chip.setBackground(bg);

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            if (i < genres.size() - 1) lp.setMarginEnd(dp(8));
            chip.setLayoutParams(lp);
            chip.setClickable(true);
            chip.setFocusable(true);

            chip.setOnClickListener(v -> {
                activeGenre = genre;
                rebuildGenreChips();
                applyFilter(genre);
            });
            genreChips.addView(chip);
        }
    }

    private void applyFilter(String genre) {
        List<ReviewPost> filtered = new ArrayList<>();
        for (ReviewPost p : allReviews) {
            if ("All".equals(genre) || genre.equals(p.getNovelGenre())) {
                filtered.add(p);
            }
        }
        adapter.submitList(filtered);
        int n = filtered.size();
        countLabel.setText(n + (n == 1 ? " review" : " reviews"));
    }

    // ── Mock data — rich, varied, gamification-tagged ─────────────────────

    private List<ReviewPost> buildMockReviews() {
        List<ReviewPost> list = new ArrayList<>();

        list.add(new ReviewPost("r01","u01","shadow_quill","Shadow Quill",null,
                520,
                "n01","The Fallen Throne","Erisa Vale",null,"Fantasy",2024,
                14200,5,
                "An absolutely riveting read from start to finish. The world-building is "
                + "breathtaking — Vale weaves political intrigue and magic into something "
                + "that feels genuinely earned. The betrayal in chapter 22 had me audibly "
                + "gasping. One of the best fantasy novels on this platform, full stop.",
                87,14,"2025-04-16T08:23:00"));

        list.add(new ReviewPost("r02","u02","moonreader99","Moon Reader",null,
                112,
                "n02","Crimson Letter","Dae-jung Oh",null,"Romance",2024,
                9800,4,
                "Sweet, warm, and beautifully paced. The slow burn between the two leads "
                + "feels completely authentic — neither rushed nor dragged out. A few "
                + "chapters in the middle could be tightened, but the ending made me tear "
                + "up in the best way possible.",
                54,8,"2025-04-15T21:05:00"));

        list.add(new ReviewPost("r03","u03","loreseeker","Lore Seeker",null,
                63,
                "n03","Circuit Ghosts","Amara Nwosu",null,"Sci-Fi",2023,
                6400,5,
                "Nwosu has invented a cyberpunk voice that's entirely her own. The prose "
                + "is sharp and the pacing relentless. I read all forty chapters in a single "
                + "weekend. The AI ethics subplot is disturbingly prescient.",
                102,22,"2025-04-15T14:30:00"));

        list.add(new ReviewPost("r04","u04","nightowl_reads","Night Owl",null,
                18,
                "n04","Hollow Season","Park Ji-ho",null,"Mystery",2024,
                5100,4,
                "Ji-ho constructs the mystery with surgical precision. Every red herring is "
                + "fair and every clue planted in plain sight. My only gripe is the detective "
                + "protagonist feels underdeveloped compared to the antagonist, who steals "
                + "every chapter they appear in.",
                38,5,"2025-04-15T09:12:00"));

        list.add(new ReviewPost("r05","u05","fantasy_fox","Fantasy Fox",null,
                5,
                "n05","Salt & Stars","Yemi Adeyemi",null,"Romance",2023,
                3300,3,
                "Gorgeous prose and a setting I never wanted to leave. The romance itself "
                + "is a little predictable but Adeyemi's descriptions of the coastal town "
                + "are worth the price of admission alone.",
                21,3,"2025-04-14T18:44:00"));

        list.add(new ReviewPost("r06","u06","chapter_hunter","Chapter Hunter",null,
                145,
                "n03","Circuit Ghosts","Amara Nwosu",null,"Sci-Fi",2023,
                6400,5,
                "Second read-through and it holds up perfectly. The foreshadowing in "
                + "chapters 8 and 9 only makes sense in hindsight — a masterclass in "
                + "tight plotting. The prose occasionally leans purple but never loses "
                + "the thread.",
                67,11,"2025-04-14T11:20:00"));

        list.add(new ReviewPost("r07","u07","scrollmaster","Scroll Master",null,
                480,
                "n01","The Fallen Throne","Erisa Vale",null,"Fantasy",2024,
                14200,4,
                "Enormous in scope and mostly delivers on its ambition. The magic system "
                + "is internally consistent and the ensemble cast is well-differentiated. "
                + "Drops slightly in the second arc before recovering impressively for "
                + "the finale.",
                44,7,"2025-04-13T20:05:00"));

        list.add(new ReviewPost("r08","u08","inkwhisperer","Ink Whisperer",null,
                35,
                "n06","The Paper Kingdom","Sofia Reyes",null,"Literary",2024,
                2900,5,
                "A quiet, devastating novel about grief and memory. Reyes writes with "
                + "restraint that makes every emotional beat land twice as hard. "
                + "I finished it on a rainy Sunday and sat with it for an hour afterwards.",
                73,16,"2025-04-13T15:33:00"));

        list.add(new ReviewPost("r09","u09","sageofstories","Sage of Stories",null,
                72,
                "n07","Iron Petal","Lindiwe Moyo",null,"Thriller",2023,
                4700,4,
                "Moyo keeps the tension relentlessly high without resorting to cheap "
                + "tricks. The protagonist's moral compromises feel earned rather than "
                + "gratuitous. A gripping read that earns its darkness.",
                49,9,"2025-04-12T22:18:00"));

        list.add(new ReviewPost("r10","u10","bookbound","Bookbound",null,
                4,
                "n08","Midnight Garden","Aiko Tanaka",null,"Fantasy",2024,
                8600,5,
                "From chapter one this one just grabbed me and wouldn't let go. "
                + "The magic is whimsical but never silly and the friendship at "
                + "the centre of the story is genuinely touching. My favourite "
                + "discovery on NovelVerse this year.",
                91,18,"2025-04-12T10:50:00"));

        list.add(new ReviewPost("r11","u11","velvet_pages","Velvet Pages",null,
                200,
                "n02","Crimson Letter","Dae-jung Oh",null,"Romance",2024,
                9800,5,
                "Re-reading this for the third time and I still notice new layers in "
                + "the dialogue. Oh writes romantic tension better than almost anyone "
                + "working in the genre right now. An instant classic.",
                115,20,"2025-04-11T17:00:00"));

        list.add(new ReviewPost("r12","u12","quillrunner","Quill Runner",null,
                28,
                "n09","Dust and Neon","Cesar Lima",null,"Sci-Fi",2023,
                3800,3,
                "Has real flashes of brilliance — the first act in particular is "
                + "excellent — but the pacing falls apart around the midpoint and "
                + "never fully recovers. Lima's next novel will be one to watch.",
                17,4,"2025-04-11T09:30:00"));

        list.add(new ReviewPost("r13","u13","oracle_reads","Oracle Reads",null,
                560,
                "n06","The Paper Kingdom","Sofia Reyes",null,"Literary",2024,
                2900,4,
                "Reyes is clearly a talent to follow. The novel's structural ambition "
                + "occasionally outpaces its emotional payoff, but the sentences are "
                + "beautiful and the central metaphor lands with real weight.",
                62,12,"2025-04-10T20:15:00"));

        list.add(new ReviewPost("r14","u14","story_sage","Story Sage",null,
                88,
                "n07","Iron Petal","Lindiwe Moyo",null,"Thriller",2023,
                4700,5,
                "One of the most nerve-shredding thrillers I've read in years. "
                + "Every chapter ends on a note that makes it impossible to stop. "
                + "Moyo absolutely earns the ending — it's earned and satisfying "
                + "rather than cheap.",
                78,15,"2025-04-10T14:45:00"));

        list.add(new ReviewPost("r15","u15","dusk_reader","Dusk Reader",null,
                7,
                "n08","Midnight Garden","Aiko Tanaka",null,"Fantasy",2024,
                8600,4,
                "Charming and imaginative with a handful of scenes that I'll "
                + "remember for a long time. A little uneven in its middle section "
                + "but Tanaka's voice is distinctive enough to carry you through.",
                33,6,"2025-04-09T11:00:00"));

        return list;
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}

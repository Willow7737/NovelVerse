package com.novelverse.app.presentation.novel.detail;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.palette.graphics.Palette;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.novelverse.app.R;
import com.novelverse.app.data.local.preferences.UserPreferences;
import com.novelverse.app.data.remote.supabase.SupabaseDatabaseService;
import com.novelverse.app.data.repository.ChapterRepository;
import com.novelverse.app.data.repository.LibraryRepository;
import com.novelverse.app.data.repository.NovelRepository;
import com.novelverse.app.domain.models.Novel;
import com.novelverse.app.presentation.novel.reader.ReaderActivity;
import com.novelverse.app.ui.banner.BannerHelper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.inject.Inject;
import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class NovelDetailActivity extends AppCompatActivity {

    public static final String EXTRA_NOVEL_ID = "novel_id";

    @Inject NovelRepository novelRepository;
    @Inject ChapterRepository chapterRepository;
    @Inject LibraryRepository libraryRepository;
    @Inject UserPreferences userPreferences;
    @Inject SupabaseDatabaseService databaseService;
    @Inject com.novelverse.app.domain.gamification.AchievementEngine achievementEngine;

    private String novelId;
    private Novel currentNovel;
    private boolean inLibrary = false;
    private boolean chaptersNewestFirst = false;

    private final ExecutorService bgExecutor = Executors.newSingleThreadExecutor();

    // ─────────────────────────────────────────────────────────────────────────
    // onCreate
    // ─────────────────────────────────────────────────────────────────────────
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // ① Edge-to-edge BEFORE setContentView so the window flag is in place
        //    before the view hierarchy is measured.
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_novel_detail);

        // ② Make the status bar icons white (they sit on our dark/coloured header)
        WindowInsetsControllerCompat wic =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        wic.setAppearanceLightStatusBars(false); // false = white icons

        // ③ Push the top nav row down by exactly the status-bar height so icons
        //    are never hidden behind the system bar on any device / notch shape.
        View topNavRow = findViewById(R.id.top_nav_row);
        ViewCompat.setOnApplyWindowInsetsListener(
                topNavRow,
                (v, insets) -> {
                    Insets bars = insets.getInsets(WindowInsetsCompat.Type.statusBars());
                    // Preserve the horizontal padding set in XML (10 dp); only override top.
                    v.setPadding(dp(10), bars.top + dp(8), dp(10), dp(6));
                    return insets; // pass through so the rest of the hierarchy can also handle
                });

        // ④ Bump the bottom CTA padding by the navigation-bar height so the
        //    buttons always sit above gesture handles / soft nav bars.
        View bottomCta = findViewById(R.id.bottom_cta);
        ViewCompat.setOnApplyWindowInsetsListener(
                bottomCta,
                (v, insets) -> {
                    Insets bars = insets.getInsets(WindowInsetsCompat.Type.navigationBars());
                    v.setPadding(dp(16), dp(12), dp(16), bars.bottom + dp(12));
                    return insets;
                });

        novelId = getIntent().getStringExtra(EXTRA_NOVEL_ID);

        // Wire up every clickable element
        bindClickListeners();

        loadNovelData();
    }

    private void bindClickListeners() {
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        findViewById(R.id.btn_share).setOnClickListener(v -> shareNovelWithCard());
        findViewById(R.id.btn_more).setOnClickListener(v -> showNovelOptionsSheet());
        findViewById(R.id.btn_read).setOnClickListener(v -> openReader(null));
        findViewById(R.id.btn_read_more).setOnClickListener(v -> expandSynopsis());

        View bmBtn = findViewById(R.id.btn_bookmark);
        if (bmBtn != null) bmBtn.setOnClickListener(v -> toggleLibrary());

        View reviewBtn = findViewById(R.id.btn_write_review);
        if (reviewBtn != null) reviewBtn.setOnClickListener(v -> showReviewSheet());

        View dlBtn = findViewById(R.id.btn_download_next_10);
        if (dlBtn != null) dlBtn.setOnClickListener(v -> showDownloadConfirmSheet());

        View sortBtn = findViewById(R.id.btn_sort_chapters);
        if (sortBtn != null) sortBtn.setOnClickListener(v -> showChapterSortSheet());

        View authorRow = findViewById(R.id.author_row);
        if (authorRow != null) authorRow.setOnClickListener(v -> showAuthorProfileSheet());

        View allCh = findViewById(R.id.btn_all_chapters);
        if (allCh != null) allCh.setOnClickListener(v -> openChapterListPage());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Data loading
    // ─────────────────────────────────────────────────────────────────────────
    private void loadNovelData() {
        if (novelId == null) return;

        novelRepository
                .getNovelById(novelId)
                .observe(
                        this,
                        novel -> {
                            if (novel == null) return;
                            currentNovel = novel;
                            bindNovel(novel);
                        });

        chapterRepository
                .getChapters(novelId)
                .observe(
                        this,
                        chapters -> {
                            if (chapters == null || chapters.isEmpty()) return;
                            TextView chapsView = findViewById(R.id.novel_chapters);
                            if (chapsView != null)
                                chapsView.setText(String.valueOf(chapters.size()));
                            RecyclerView rv = findViewById(R.id.chapters_recycler);
                            if (rv != null) {
                                rv.setLayoutManager(new LinearLayoutManager(this));
                                rv.setAdapter(
                                        new ChapterListAdapter(chapters, id -> openReader(id)));
                            }
                        });

        libraryRepository.isInLibrary(
                novelId,
                inLib ->
                        runOnUiThread(
                                () -> {
                                    inLibrary = inLib;
                                    updateBookmarkIcon();
                                }));

        incrementViewCount();
        loadReviews();
    }

    private void incrementViewCount() {
        String token = userPreferences.getAccessToken();
        if (token == null || novelId == null) return;
        bgExecutor.execute(
                () -> {
                    try {
                        com.google.gson.JsonObject body = new com.google.gson.JsonObject();
                        body.addProperty("novel_id_input", novelId);
                        databaseService.rpcPost(
                                "increment_novel_views",
                                body,
                                token,
                                new SupabaseDatabaseService.DatabaseCallback() {
                                    @Override
                                    public void onSuccess(String r) {}

                                    @Override
                                    public void onError(String e) {}
                                });
                    } catch (Exception ignored) {
                    }
                });
    }

    private void loadReviews() {
        String token = userPreferences.getAccessToken();
        if (novelId == null) return;
        String filter = "novel_id=eq." + novelId + "&rating=gte.1&order=created_at.desc&limit=5";
        String select =
                "rating,review,created_at,profiles!ratings_user_id_fkey(display_name,avatar_url)";
        String url = databaseService.buildSelectUrl("ratings", select, filter, null);
        databaseService.rawSelect(
                url,
                token,
                new SupabaseDatabaseService.DatabaseCallback() {
                    @Override
                    public void onSuccess(String r) {
                        runOnUiThread(() -> bindReviews(r));
                    }

                    @Override
                    public void onError(String e) {}
                });
    }

    private void bindReviews(String json) {
        try {
            com.google.gson.JsonArray arr =
                    new com.google.gson.Gson().fromJson(json, com.google.gson.JsonArray.class);
            if (arr == null || arr.size() == 0) return;

            View section = findViewById(R.id.reviews_section);
            RecyclerView rv = findViewById(R.id.reviews_recycler);
            TextView countLabel = findViewById(R.id.reviews_count_label);
            if (section == null || rv == null) return;

            section.setVisibility(View.VISIBLE);
            if (countLabel != null)
                countLabel.setText(arr.size() + " review" + (arr.size() != 1 ? "s" : ""));

            rv.setLayoutManager(new LinearLayoutManager(this));
            rv.setAdapter(new ReviewAdapter(arr));
        } catch (Exception ignored) {
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // bindNovel — populates all UI from the Novel domain model
    // ─────────────────────────────────────────────────────────────────────────
    private void bindNovel(Novel novel) {
        setText(R.id.novel_title, novel.getTitle());
        setText(
                R.id.novel_author,
                novel.getAuthorDisplayName() != null ? novel.getAuthorDisplayName() : "Unknown");

        // Synopsis expand/collapse
        TextView desc = findViewById(R.id.novel_description);
        TextView readMoreBtn = findViewById(R.id.btn_read_more);
        if (desc != null && novel.getDescription() != null) {
            desc.setText(novel.getDescription());
            desc.getViewTreeObserver()
                    .addOnPreDrawListener(
                            new android.view.ViewTreeObserver.OnPreDrawListener() {
                                @Override
                                public boolean onPreDraw() {
                                    desc.getViewTreeObserver().removeOnPreDrawListener(this);
                                    if (readMoreBtn != null)
                                        readMoreBtn.setVisibility(
                                                desc.getLineCount() > 6 ? View.VISIBLE : View.GONE);
                                    return true;
                                }
                            });
        }

        // ── Cover → drives blur bg + Palette tint + cover ImageView ──────────
        String coverUrl = novel.getCoverImageUrl();
        if (coverUrl != null) {
            // Blurred background texture
            Glide.with(this)
                    .load(coverUrl)
                    .transform(new jp.wasabeef.glide.transformations.BlurTransformation(22, 3))
                    .centerCrop()
                    .into((ImageView) findViewById(R.id.novel_cover_blur_bg));

            // Bitmap → cover ImageView + Palette in one decode
            Glide.with(this)
                    .asBitmap()
                    .load(coverUrl)
                    .centerCrop()
                    .into(
                            new CustomTarget<Bitmap>() {
                                @Override
                                public void onResourceReady(
                                        @NonNull Bitmap bitmap,
                                        @Nullable Transition<? super Bitmap> transition) {
                                    ImageView cover = findViewById(R.id.novel_cover);
                                    if (cover != null) cover.setImageBitmap(bitmap);
                                    Palette.from(bitmap)
                                            .maximumColorCount(24)
                                            .generate(
                                                    palette ->
                                                            runOnUiThread(
                                                                    () ->
                                                                            applyDynamicHeaderTint(
                                                                                    palette)));
                                }

                                @Override
                                public void onLoadCleared(
                                        @Nullable android.graphics.drawable.Drawable p) {
                                    ImageView cover = findViewById(R.id.novel_cover);
                                    if (cover != null)
                                        cover.setImageResource(R.drawable.img_placeholder_cover);
                                }
                            });
        } else {
            ImageView cover = findViewById(R.id.novel_cover);
            if (cover != null) cover.setImageResource(R.drawable.img_placeholder_cover);
        }

        // Author avatar
        ImageView avatarView = findViewById(R.id.author_avatar);
        if (avatarView != null) {
            String avatarUrl = novel.getAuthorAvatarUrl();
            if (avatarUrl != null && !avatarUrl.isEmpty())
                Glide.with(this)
                        .load(avatarUrl)
                        .circleCrop()
                        .placeholder(R.drawable.ic_avatar_placeholder)
                        .into(avatarView);
            else avatarView.setImageResource(R.drawable.ic_avatar_placeholder);
        }

        // Rating
        TextView ratingView = findViewById(R.id.novel_rating);
        if (ratingView != null) {
            double r = novel.getAverageRating();
            ratingView.setText(r > 0 ? String.format("%.1f", r) : "New");
        }

        // Views
        TextView viewsView = findViewById(R.id.novel_views);
        if (viewsView != null) {
            long views = novel.getTotalViews();
            viewsView.setText(views > 0 ? formatCount(views) : "0");
        }

        // Read button
        TextView readBtn = findViewById(R.id.btn_read);
        if (readBtn != null) {
            boolean hasProgress = novel.getCurrentChapterId() != null;
            readBtn.setText(hasProgress ? "Continue Reading" : "Start Reading");
            readBtn.setOnClickListener(v -> openReader(novel.getCurrentChapterId()));
        }

        // ★ Review button — only visible once the user has actually read something
        View reviewBtn = findViewById(R.id.btn_write_review);
        if (reviewBtn != null)
            reviewBtn.setVisibility(novel.getCurrentChapterId() != null ? View.VISIBLE : View.GONE);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // applyDynamicHeaderTint
    // Derives the cover's most glaring colour via Palette, darkens it so text
    // remains readable, and applies a top→bottom gradient down to near-black.
    // Each novel ends up with a unique, on-brand header.
    // ─────────────────────────────────────────────────────────────────────────
    private void applyDynamicHeaderTint(@NonNull Palette palette) {
        View overlay = findViewById(R.id.header_tint_overlay);
        if (overlay == null) return;

        // Priority: Vibrant → DarkVibrant → Muted → Dominant → hard-coded fallback
        Palette.Swatch swatch = palette.getVibrantSwatch();
        if (swatch == null) swatch = palette.getDarkVibrantSwatch();
        if (swatch == null) swatch = palette.getMutedSwatch();
        if (swatch == null) swatch = palette.getDominantSwatch();

        int topColor;
        if (swatch != null) {
            int base = swatch.getRgb();
            float[] hsv = new float[3];
            Color.colorToHSV(base, hsv);
            hsv[2] = Math.min(hsv[2], 0.38f); // keep it dark
            hsv[1] = Math.min(hsv[1] * 1.25f, 1.0f); // punch up the saturation slightly
            int darkened = Color.HSVToColor(hsv);
            topColor =
                    Color.argb(
                            210, Color.red(darkened), Color.green(darkened), Color.blue(darkened));
        } else {
            topColor = Color.argb(200, 12, 12, 38); // deep indigo fallback
        }

        // Bottom is always near-black so the cover card + white text are readable
        int bottomColor = Color.argb(245, 6, 6, 10);

        GradientDrawable gradient =
                new GradientDrawable(
                        GradientDrawable.Orientation.TOP_BOTTOM, new int[] {topColor, bottomColor});
        overlay.setBackground(gradient);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Library bookmark
    // ─────────────────────────────────────────────────────────────────────────
    private void toggleLibrary() {
        if (novelId == null) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
            ((View) findViewById(R.id.btn_bookmark))
                    .performHapticFeedback(HapticFeedbackConstants.CONFIRM);

        if (inLibrary) {
            libraryRepository.removeFromLibrary(
                    novelId,
                    (ok, err) ->
                            runOnUiThread(
                                    () -> {
                                        if (ok) {
                                            inLibrary = false;
                                            updateBookmarkIcon();
                                            BannerHelper.info(this, "Removed from library");
                                        } else
                                            BannerHelper.error(
                                                    this,
                                                    "Error",
                                                    err != null ? err : "Could not remove");
                                    }));
        } else {
            libraryRepository.addToLibrary(
                    novelId,
                    LibraryRepository.STATUS_READING,
                    (ok, err) ->
                            runOnUiThread(
                                    () -> {
                                        if (ok) {
                                            inLibrary = true;
                                            updateBookmarkIcon();
                                            BannerHelper.success(this, "Added to library! 📚");
                                        } else
                                            BannerHelper.error(
                                                    this,
                                                    "Error",
                                                    err != null ? err : "Could not add");
                                    }));
        }
    }

    private void updateBookmarkIcon() {
        ImageView btn = findViewById(R.id.btn_bookmark);
        if (btn == null) return;
        btn.setImageResource(inLibrary ? R.drawable.ic_bookmark_filled : R.drawable.ic_bookmark);
        btn.clearColorFilter();
        if (!inLibrary) btn.setColorFilter(resolveAttrColor(android.R.attr.textColorTertiary));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Review bottom sheet
    // ─────────────────────────────────────────────────────────────────────────
    private void showReviewSheet() {
        if (userPreferences.getUserId() == null) {
            BannerHelper.warning(this, "Sign in required", "Log in to leave a review.");
            return;
        }
        BottomSheetDialog sheet = new BottomSheetDialog(this, R.style.BottomSheetTheme);
        View root = getLayoutInflater().inflate(R.layout.bottom_sheet_review, null);

        LinearLayout starRow = root.findViewById(R.id.review_star_row);
        final int[] starRating = {5};
        ImageView[] stars = new ImageView[5];
        for (int i = 0; i < 5; i++) {
            final int idx = i + 1;
            stars[i] = new ImageView(this);
            stars[i].setImageResource(R.drawable.ic_star_filled);
            stars[i].setColorFilter(Color.parseColor("#F59E0B"));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(40), dp(40));
            lp.setMarginEnd(dp(6));
            stars[i].setLayoutParams(lp);
            stars[i].setOnClickListener(
                    v -> {
                        starRating[0] = idx;
                        for (int j = 0; j < 5; j++)
                            stars[j].setColorFilter(
                                    j < idx
                                            ? Color.parseColor("#F59E0B")
                                            : Color.parseColor("#CBD5E1"));
                    });
            starRow.addView(stars[i]);
        }

        android.widget.EditText reviewInput = root.findViewById(R.id.review_input);
        root.findViewById(R.id.btn_submit_review)
                .setOnClickListener(
                        v -> {
                            sheet.dismiss();
                            submitReview(starRating[0], reviewInput.getText().toString().trim());
                        });
        root.findViewById(R.id.btn_cancel_review).setOnClickListener(v -> sheet.dismiss());
        sheet.setContentView(root);
        sheet.show();
    }

    private void submitReview(int rating, String reviewText) {
        String userId = userPreferences.getUserId();
        String token = userPreferences.getAccessToken();
        if (userId == null || novelId == null) return;
        com.google.gson.JsonObject body = new com.google.gson.JsonObject();
        body.addProperty("novel_id", novelId);
        body.addProperty("user_id", userId);
        body.addProperty("rating", rating);
        body.addProperty("review", reviewText);
        databaseService.upsert(
                "ratings",
                body,
                "user_id,novel_id",
                token,
                new SupabaseDatabaseService.DatabaseCallback() {
                    @Override
                    public void onSuccess(String r) {
                        runOnUiThread(
                                () ->
                                        BannerHelper.success(
                                                NovelDetailActivity.this, "Review submitted! ⭐"));
                    }

                    @Override
                    public void onError(String e) {
                        runOnUiThread(
                                () ->
                                        BannerHelper.error(
                                                NovelDetailActivity.this, "Submit failed", e));
                    }
                });
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Author profile sheet
    // ─────────────────────────────────────────────────────────────────────────
    private void showAuthorProfileSheet() {
        if (currentNovel == null) return;
        BottomSheetDialog sheet = new BottomSheetDialog(this, R.style.BottomSheetTheme);
        View root = getLayoutInflater().inflate(R.layout.bottom_sheet_author_profile, null);

        ImageView avatar = root.findViewById(R.id.author_sheet_avatar);
        String avatarUrl = currentNovel.getAuthorAvatarUrl();
        if (avatarUrl != null && !avatarUrl.isEmpty())
            Glide.with(this)
                    .load(avatarUrl)
                    .circleCrop()
                    .placeholder(R.drawable.ic_avatar_placeholder)
                    .into(avatar);

        TextView nameView = root.findViewById(R.id.author_sheet_name);
        String authorName = currentNovel.getAuthorDisplayName();
        nameView.setText(
                (authorName != null && !authorName.isEmpty()) ? authorName : "Unknown Author");

        root.findViewById(R.id.author_sheet_close).setOnClickListener(v -> sheet.dismiss());

        android.widget.Button followBtn = root.findViewById(R.id.btn_follow_author);
        if (followBtn != null) {
            String userId = userPreferences.getUserId();
            String token = userPreferences.getAccessToken();
            String authorId = currentNovel.getAuthorId();
            if (userId != null && authorId != null && !userId.equals(authorId)) {
                com.google.gson.JsonObject checkP = new com.google.gson.JsonObject();
                checkP.addProperty("p_follower_id", userId);
                checkP.addProperty("p_author_id", authorId);
                databaseService.callRpc(
                        "is_following",
                        checkP,
                        token,
                        new SupabaseDatabaseService.DatabaseCallback() {
                            @Override
                            public void onSuccess(String r) {
                                boolean following = "true".equalsIgnoreCase(r.trim());
                                runOnUiThread(
                                        () -> {
                                            followBtn.setText(following ? "Following ✓" : "Follow");
                                            followBtn.setEnabled(!following);
                                        });
                            }

                            @Override
                            public void onError(String e) {}
                        });
            } else if (userId != null && userId.equals(currentNovel.getAuthorId())) {
                followBtn.setVisibility(View.GONE);
            }
            followBtn.setOnClickListener(
                    v -> {
                        if (userId == null || authorId == null) return;
                        followBtn.setEnabled(false);
                        com.google.gson.JsonObject params = new com.google.gson.JsonObject();
                        params.addProperty("p_follower_id", userId);
                        params.addProperty("p_author_id", authorId);
                        databaseService.callRpc(
                                "follow_author",
                                params,
                                token,
                                new SupabaseDatabaseService.DatabaseCallback() {
                                    @Override
                                    public void onSuccess(String r) {
                                        runOnUiThread(
                                                () -> {
                                                    BannerHelper.success(
                                                            NovelDetailActivity.this,
                                                            "Now following!");
                                                    followBtn.setText("Following ✓");
                                                });
                                    }

                                    @Override
                                    public void onError(String e) {
                                        runOnUiThread(
                                                () -> {
                                                    followBtn.setEnabled(true);
                                                    BannerHelper.error(
                                                            NovelDetailActivity.this,
                                                            "Follow failed. Try again.");
                                                });
                                    }
                                });
                        sheet.dismiss();
                    });
        }
        fetchAuthorBioIntoSheet(currentNovel.getAuthorId(), root);
        sheet.setContentView(root);
        sheet.show();
    }

    private void fetchAuthorBioIntoSheet(String authorId, View sheetRoot) {
        if (authorId == null) return;
        String token = userPreferences.getAccessToken();
        databaseService.selectById(
                "profiles",
                authorId,
                token,
                new SupabaseDatabaseService.DatabaseCallback() {
                    @Override
                    public void onSuccess(String r) {
                        runOnUiThread(
                                () -> {
                                    try {
                                        com.google.gson.JsonObject obj =
                                                new com.google.gson.Gson()
                                                        .fromJson(
                                                                r,
                                                                com.google.gson.JsonObject.class);
                                        if (obj == null) return;
                                        if (obj.has("bio") && !obj.get("bio").isJsonNull()) {
                                            String bio = obj.get("bio").getAsString();
                                            if (!bio.isEmpty()) {
                                                TextView bioView =
                                                        sheetRoot.findViewById(
                                                                R.id.author_sheet_bio);
                                                if (bioView != null) {
                                                    bioView.setText(bio);
                                                    bioView.setVisibility(View.VISIBLE);
                                                }
                                            }
                                        }
                                    } catch (Exception ignored) {
                                    }
                                });
                    }

                    @Override
                    public void onError(String e) {}
                });
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Share — builds a custom cover card and shares via Intent
    // ─────────────────────────────────────────────────────────────────────────
    private void shareNovelWithCard() {
        if (currentNovel == null) {
            shareSimple();
            return;
        }
        bgExecutor.execute(
                () -> {
                    Bitmap coverBmp = null;
                    String coverUrl = currentNovel.getCoverImageUrl();
                    if (coverUrl != null) {
                        try {
                            coverBmp =
                                    Glide.with(this)
                                            .asBitmap()
                                            .load(coverUrl)
                                            .submit(160, 240)
                                            .get();
                        } catch (Exception ignored) {
                        }
                    }
                    final Bitmap finalCover = coverBmp;
                    runOnUiThread(() -> buildAndShareCard(finalCover));
                });
    }

    private void buildAndShareCard(Bitmap coverBmp) {
        int W = 880, H = 400;
        Bitmap bmp = Bitmap.createBitmap(W, H, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bmp);
        Paint bgPaint = new Paint();
        bgPaint.setShader(
                new LinearGradient(
                        0,
                        0,
                        W * 0.67f,
                        H,
                        Color.parseColor("#0085FF"),
                        Color.parseColor("#0055BB"),
                        Shader.TileMode.CLAMP));
        canvas.drawRect(0, 0, W, H, bgPaint);
        int coverLeft = (int) (W * 0.62f);
        if (coverBmp != null) {
            canvas.drawBitmap(
                    coverBmp, null, new Rect(coverLeft, 0, W, H), new Paint(Paint.ANTI_ALIAS_FLAG));
            Paint fadePaint = new Paint();
            fadePaint.setShader(
                    new LinearGradient(
                            coverLeft,
                            0,
                            coverLeft + dp(60),
                            0,
                            Color.parseColor("#FF0085FF"),
                            Color.TRANSPARENT,
                            Shader.TileMode.CLAMP));
            canvas.drawRect(coverLeft, 0, coverLeft + dp(60), H, fadePaint);
        }
        Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.WHITE);
        android.graphics.Typeface tf = ResourcesCompat.getFont(this, R.font.inter_semibold);
        if (tf != null) textPaint.setTypeface(tf);
        textPaint.setTextSize(22f);
        textPaint.setAlpha(220);
        canvas.drawText("Read on NovelVerse", dp(32), dp(52), textPaint);
        textPaint.setTextSize(46f);
        textPaint.setAlpha(255);
        String t = currentNovel.getTitle() != null ? currentNovel.getTitle() : "";
        if (t.length() > 22) {
            int s = t.lastIndexOf(' ', 22);
            if (s < 0) s = 22;
            canvas.drawText(t.substring(0, s), dp(32), H / 2 - dp(20), textPaint);
            canvas.drawText(t.substring(s).trim(), dp(32), H / 2 + dp(36), textPaint);
        } else {
            canvas.drawText(t, dp(32), H / 2 + dp(10), textPaint);
        }
        textPaint.setTextSize(26f);
        textPaint.setAlpha(190);
        String author =
                currentNovel.getAuthorDisplayName() != null
                        ? "by " + currentNovel.getAuthorDisplayName()
                        : "";
        canvas.drawText(author, dp(32), H - dp(40), textPaint);

        File cachePath = new File(getCacheDir(), "shares");
        cachePath.mkdirs();
        File file = new File(cachePath, "novel_share.png");
        try (FileOutputStream fos = new FileOutputStream(file)) {
            bmp.compress(Bitmap.CompressFormat.PNG, 95, fos);
        } catch (IOException e) {
            shareSimple();
            return;
        }

        Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", file);
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("image/png");
        shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
        shareIntent.putExtra(
                Intent.EXTRA_TEXT,
                "Reading \""
                        + currentNovel.getTitle()
                        + "\" on NovelVerse!\nnovelverse://novel/"
                        + novelId);
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(shareIntent, "Share Novel"));
    }

    private void shareSimple() {
        Intent i = new Intent(Intent.ACTION_SEND);
        i.setType("text/plain");
        i.putExtra(Intent.EXTRA_TEXT, "Check out this novel on NovelVerse!");
        startActivity(Intent.createChooser(i, "Share"));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Options / Download / Sort sheets
    // ─────────────────────────────────────────────────────────────────────────
    private void showNovelOptionsSheet() {
        BottomSheetDialog sheet = new BottomSheetDialog(this, R.style.BottomSheetTheme);
        View root = getLayoutInflater().inflate(R.layout.bottom_sheet_novel_options, null);
        root.findViewById(R.id.options_row_share)
                .setOnClickListener(
                        v -> {
                            sheet.dismiss();
                            shareNovelWithCard();
                        });
        root.findViewById(R.id.options_row_report).setOnClickListener(v -> sheet.dismiss());
        root.findViewById(R.id.options_row_author)
                .setOnClickListener(
                        v -> {
                            sheet.dismiss();
                            showAuthorProfileSheet();
                        });
        root.findViewById(R.id.options_row_library)
                .setOnClickListener(
                        v -> {
                            sheet.dismiss();
                            toggleLibrary();
                        });
        sheet.setContentView(root);
        sheet.show();
    }

    private void showDownloadConfirmSheet() {
        BottomSheetDialog sheet = new BottomSheetDialog(this, R.style.BottomSheetTheme);
        View root = getLayoutInflater().inflate(R.layout.bottom_sheet_download, null);
        root.findViewById(R.id.btn_download_5)
                .setOnClickListener(
                        v -> {
                            sheet.dismiss();
                            enqueueDownload(5);
                        });
        root.findViewById(R.id.btn_download_10)
                .setOnClickListener(
                        v -> {
                            sheet.dismiss();
                            enqueueDownload(10);
                        });
        root.findViewById(R.id.btn_download_all)
                .setOnClickListener(
                        v -> {
                            sheet.dismiss();
                            enqueueDownload(Integer.MAX_VALUE);
                        });
        root.findViewById(R.id.btn_cancel_download).setOnClickListener(v -> sheet.dismiss());
        sheet.setContentView(root);
        sheet.show();
    }

    private void showChapterSortSheet() {
        BottomSheetDialog sheet = new BottomSheetDialog(this, R.style.BottomSheetTheme);
        View root = getLayoutInflater().inflate(R.layout.bottom_sheet_chapter_sort, null);
        View checkOldest = root.findViewById(R.id.sort_check_oldest);
        View checkNewest = root.findViewById(R.id.sort_check_newest);
        if (checkOldest != null)
            checkOldest.setVisibility(chaptersNewestFirst ? View.INVISIBLE : View.VISIBLE);
        if (checkNewest != null)
            checkNewest.setVisibility(chaptersNewestFirst ? View.VISIBLE : View.INVISIBLE);
        root.findViewById(R.id.sort_oldest_first)
                .setOnClickListener(
                        v -> {
                            chaptersNewestFirst = false;
                            updateSortButton("Oldest first");
                            sheet.dismiss();
                        });
        root.findViewById(R.id.sort_newest_first)
                .setOnClickListener(
                        v -> {
                            chaptersNewestFirst = true;
                            updateSortButton("Newest first");
                            sheet.dismiss();
                        });
        sheet.setContentView(root);
        sheet.show();
    }

    private void updateSortButton(String label) {
        TextView btn = findViewById(R.id.btn_sort_chapters);
        if (btn != null) btn.setText(label);
        RecyclerView rv = findViewById(R.id.chapters_recycler);
        if (rv != null && rv.getAdapter() instanceof ChapterListAdapter)
            ((ChapterListAdapter) rv.getAdapter()).setReversed(chaptersNewestFirst);
    }

    private void enqueueDownload(int chapCount) {
        int actual = Math.min(chapCount == Integer.MAX_VALUE ? 20 : chapCount, 20);
        List<OneTimeWorkRequest> tasks = new ArrayList<>();
        for (int i = 0; i < actual; i++)
            tasks.add(
                    new OneTimeWorkRequest.Builder(com.novelverse.app.workers.SyncWorker.class)
                            .build());
        if (!tasks.isEmpty()) {
            WorkManager wm = WorkManager.getInstance(this);
            if (tasks.size() == 1) wm.enqueue(tasks.get(0));
            else wm.beginWith(tasks.get(0)).then(tasks.subList(1, tasks.size())).enqueue();
        }
        BannerHelper.success(this, "Downloading…", "Chapters will be available offline shortly.");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Navigation helpers
    // ─────────────────────────────────────────────────────────────────────────
    private void openReader(String chapterId) {
        Intent i = new Intent(this, ReaderActivity.class);
        i.putExtra(ReaderActivity.EXTRA_NOVEL_ID, novelId);
        if (currentNovel != null)
            i.putExtra(ReaderActivity.EXTRA_NOVEL_TITLE, currentNovel.getTitle());
        if (chapterId != null) i.putExtra(ReaderActivity.EXTRA_CHAPTER_ID, chapterId);
        startActivity(i);
    }

    private void openChapterListPage() {
        Intent i = new Intent(this, ChapterListActivity.class);
        i.putExtra(ChapterListActivity.EXTRA_NOVEL_ID, novelId);
        if (currentNovel != null)
            i.putExtra(ChapterListActivity.EXTRA_NOVEL_TITLE, currentNovel.getTitle());
        startActivity(i);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Synopsis expand / collapse
    // ─────────────────────────────────────────────────────────────────────────
    private void expandSynopsis() {
        TextView desc = findViewById(R.id.novel_description);
        TextView btn = findViewById(R.id.btn_read_more);
        if (desc == null) return;
        if (desc.getMaxLines() == 6) {
            desc.setMaxLines(Integer.MAX_VALUE);
            if (btn != null) btn.setText("Show less");
        } else {
            desc.setMaxLines(6);
            if (btn != null) btn.setText("Read more");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Review list adapter (inner class)
    // ─────────────────────────────────────────────────────────────────────────
    private class ReviewAdapter extends RecyclerView.Adapter<ReviewAdapter.VH> {
        private final com.google.gson.JsonArray reviews;

        ReviewAdapter(com.google.gson.JsonArray reviews) {
            this.reviews = reviews;
        }

        @Override
        public VH onCreateViewHolder(@NonNull android.view.ViewGroup parent, int viewType) {
            return new VH(getLayoutInflater().inflate(R.layout.item_review, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int pos) {
            try {
                com.google.gson.JsonObject rev = reviews.get(pos).getAsJsonObject();
                int rating =
                        rev.has("rating") && !rev.get("rating").isJsonNull()
                                ? rev.get("rating").getAsInt()
                                : 0;
                h.starRow.removeAllViews();
                for (int i = 0; i < 5; i++) {
                    ImageView star = new ImageView(NovelDetailActivity.this);
                    star.setImageResource(R.drawable.ic_star_filled);
                    star.setColorFilter(i < rating ? 0xFFF59E0B : 0xFFCBD5E1);
                    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(14), dp(14));
                    lp.setMarginEnd(dp(2));
                    star.setLayoutParams(lp);
                    h.starRow.addView(star);
                }
                String name = "Anonymous";
                if (rev.has("profiles") && !rev.get("profiles").isJsonNull()) {
                    com.google.gson.JsonObject prof = rev.getAsJsonObject("profiles");
                    if (prof.has("display_name") && !prof.get("display_name").isJsonNull())
                        name = prof.get("display_name").getAsString();
                }
                h.name.setText(name);
                String text =
                        rev.has("review") && !rev.get("review").isJsonNull()
                                ? rev.get("review").getAsString().trim()
                                : "";
                h.text.setVisibility(!text.isEmpty() ? View.VISIBLE : View.GONE);
                if (!text.isEmpty()) h.text.setText(text);
                if (rev.has("created_at") && !rev.get("created_at").isJsonNull()) {
                    try {
                        java.text.SimpleDateFormat in =
                                new java.text.SimpleDateFormat(
                                        "yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.US);
                        java.text.SimpleDateFormat out =
                                new java.text.SimpleDateFormat("MMM d", java.util.Locale.US);
                        h.date.setText(out.format(in.parse(rev.get("created_at").getAsString())));
                    } catch (Exception ignored) {
                        h.date.setText("");
                    }
                }
                if (rev.has("profiles") && !rev.get("profiles").isJsonNull()) {
                    com.google.gson.JsonObject prof = rev.getAsJsonObject("profiles");
                    if (prof.has("avatar_url") && !prof.get("avatar_url").isJsonNull())
                        Glide.with(NovelDetailActivity.this)
                                .load(prof.get("avatar_url").getAsString())
                                .circleCrop()
                                .placeholder(R.drawable.ic_avatar_placeholder)
                                .into(h.avatar);
                }
            } catch (Exception ignored) {
            }
        }

        @Override
        public int getItemCount() {
            return reviews.size();
        }

        class VH extends RecyclerView.ViewHolder {
            ImageView avatar;
            TextView name, text, date;
            LinearLayout starRow;

            VH(View v) {
                super(v);
                avatar = v.findViewById(R.id.reviewer_avatar);
                name = v.findViewById(R.id.reviewer_name);
                text = v.findViewById(R.id.review_text);
                date = v.findViewById(R.id.review_date);
                starRow = v.findViewById(R.id.star_row);
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Tiny helpers
    // ─────────────────────────────────────────────────────────────────────────
    private void setText(int id, String text) {
        TextView v = findViewById(id);
        if (v != null && text != null) v.setText(text);
    }

    private String formatCount(long n) {
        if (n >= 1_000_000) return String.format("%.1fM", n / 1_000_000f);
        if (n >= 1_000) return String.format("%.1fk", n / 1_000f);
        return String.valueOf(n);
    }

    private int resolveAttrColor(int attr) {
        android.util.TypedValue tv = new android.util.TypedValue();
        getTheme().resolveAttribute(attr, tv, true);
        return tv.data;
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}

package com.novelverse.app.presentation.novel.detail;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Shader;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.core.content.res.ResourcesCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.FutureTarget;
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
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.inject.Inject;
import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class NovelDetailActivity extends AppCompatActivity {

    public  static final String EXTRA_NOVEL_ID = "novel_id";

    @Inject NovelRepository   novelRepository;
    @Inject ChapterRepository chapterRepository;
    @Inject LibraryRepository libraryRepository;
    @Inject UserPreferences   userPreferences;
    @Inject SupabaseDatabaseService databaseService;
    @Inject com.novelverse.app.domain.gamification.AchievementEngine achievementEngine;

    private String  novelId;
    private Novel   currentNovel;
    private boolean inLibrary = false;

    private final ExecutorService bgExecutor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_novel_detail);

        novelId = getIntent().getStringExtra(EXTRA_NOVEL_ID);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        findViewById(R.id.btn_share).setOnClickListener(v -> shareNovelWithCard());
        findViewById(R.id.btn_more).setOnClickListener(v -> showNovelOptionsSheet());
        findViewById(R.id.btn_read).setOnClickListener(v -> openReader(null));
        findViewById(R.id.btn_read_more).setOnClickListener(v -> expandSynopsis());

        View bmBtn = findViewById(R.id.btn_bookmark);
        if (bmBtn != null) bmBtn.setOnClickListener(v -> toggleLibrary());

        // Write Review — redesigned bottom sheet
        View reviewBtn = findViewById(R.id.btn_write_review);
        if (reviewBtn != null) reviewBtn.setOnClickListener(v -> showReviewSheet());

        View dlBtn = findViewById(R.id.btn_download_next_10);
        if (dlBtn != null) dlBtn.setOnClickListener(v -> showDownloadConfirmSheet());

        // Chapter sort toggle
        View sortBtn = findViewById(R.id.btn_sort_chapters);
        if (sortBtn != null) sortBtn.setOnClickListener(v -> showChapterSortSheet());

        View authorRow = findViewById(R.id.author_row);
        if (authorRow != null) authorRow.setOnClickListener(v -> showAuthorProfileSheet());

        // View all chapters → dedicated page
        View allChaptersBtn = findViewById(R.id.btn_all_chapters);
        if (allChaptersBtn != null) allChaptersBtn.setOnClickListener(v -> openChapterListPage());

        loadNovelData();
    }

    private void loadNovelData() {
        if (novelId == null) return;

        novelRepository.getNovelById(novelId).observe(this, novel -> {
            if (novel == null) return;
            currentNovel = novel;
            bindNovel(novel);
        });

        chapterRepository.getChapters(novelId).observe(this, chapters -> {
            if (chapters == null || chapters.isEmpty()) return;
            TextView chapsView = findViewById(R.id.novel_chapters);
            if (chapsView != null) chapsView.setText(String.valueOf(chapters.size()));
            RecyclerView rv = findViewById(R.id.chapters_recycler);
            if (rv != null) {
                rv.setLayoutManager(new LinearLayoutManager(this));
                rv.setAdapter(new ChapterListAdapter(chapters, id -> openReader(id)));
            }
        });

        if (novelId != null) {
            libraryRepository.isInLibrary(novelId, inLib -> runOnUiThread(() -> {
                inLibrary = inLib;
                updateBookmarkIcon();
            }));
        }

        // Increment view count when novel page is opened
        incrementViewCount();

        // Load reviews
        loadReviews();
    }

    private void incrementViewCount() {
        String token = userPreferences.getAccessToken();
        if (token == null || novelId == null) return;
        bgExecutor.execute(() -> {
            try {
                com.google.gson.JsonObject body = new com.google.gson.JsonObject();
                body.addProperty("novel_id_input", novelId);
                databaseService.rpcPost("increment_novel_views", body, token,
                        new SupabaseDatabaseService.DatabaseCallback() {
                            @Override public void onSuccess(String r) {}
                            @Override public void onError(String e) {
                                // Fallback: raw SQL increment via update isn't possible without RPC,
                                // so silently ignore — views are best-effort
                                android.util.Log.d("NovelDetail", "View increment skipped: " + e);
                            }
                        });
            } catch (Exception ignored) {}
        });
    }

    private void loadReviews() {
        String token = userPreferences.getAccessToken();
        if (novelId == null) return;
        String filter = "novel_id=eq." + novelId + "&rating=gte.1&order=created_at.desc&limit=5";
        String select = "rating,review,created_at,profiles(display_name,avatar_url)";
        String url    = databaseService.buildSelectUrl("ratings", select, filter, null);
        databaseService.rawSelect(url, token, new SupabaseDatabaseService.DatabaseCallback() {
            @Override public void onSuccess(String r) {
                runOnUiThread(() -> bindReviews(r));
            }
            @Override public void onError(String e) {
                android.util.Log.d("NovelDetail", "Reviews load error: " + e);
            }
        });
    }

    private void bindReviews(String json) {
        try {
            com.google.gson.JsonArray arr = new com.google.gson.Gson()
                    .fromJson(json, com.google.gson.JsonArray.class);
            if (arr == null || arr.size() == 0) return;

            android.view.View section = findViewById(R.id.reviews_section);
            RecyclerView rv = findViewById(R.id.reviews_recycler);
            TextView countLabel = findViewById(R.id.reviews_count_label);
            if (section == null || rv == null) return;

            section.setVisibility(android.view.View.VISIBLE);
            if (countLabel != null) countLabel.setText(arr.size() + " review" + (arr.size() != 1 ? "s" : ""));

            rv.setLayoutManager(new LinearLayoutManager(this));
            rv.setAdapter(new ReviewAdapter(arr));
        } catch (Exception e) {
            android.util.Log.d("NovelDetail", "bindReviews parse error: " + e.getMessage());
        }
    }

    private void bindNovel(Novel novel) {
        setText(R.id.novel_title,  novel.getTitle());
        setText(R.id.novel_author, novel.getAuthorDisplayName() != null
                ? novel.getAuthorDisplayName() : "Unknown");

        TextView desc = findViewById(R.id.novel_description);
        TextView readMoreBtn = findViewById(R.id.btn_read_more);
        if (desc != null && novel.getDescription() != null) {
            desc.setText(novel.getDescription());
            // Show "Read more" only if description actually overflows 6 lines
            desc.getViewTreeObserver().addOnPreDrawListener(new android.view.ViewTreeObserver.OnPreDrawListener() {
                @Override public boolean onPreDraw() {
                    desc.getViewTreeObserver().removeOnPreDrawListener(this);
                    if (readMoreBtn != null) {
                        readMoreBtn.setVisibility(desc.getLineCount() > 6
                                ? android.view.View.VISIBLE : android.view.View.GONE);
                    }
                    return true;
                }
            });
        }

        String coverUrl = novel.getCoverImageUrl();

        ImageView blurBg = findViewById(R.id.novel_cover_blur_bg);
        if (blurBg != null && coverUrl != null)
            Glide.with(this).load(coverUrl).centerCrop().into(blurBg);

        ImageView cover = findViewById(R.id.novel_cover);
        if (cover != null)
            Glide.with(this).load(coverUrl)
                    .placeholder(R.drawable.img_placeholder_cover)
                    .centerCrop().into(cover);

        ImageView avatarView = findViewById(R.id.author_avatar);
        if (avatarView != null) {
            String avatarUrl = novel.getAuthorAvatarUrl();
            if (avatarUrl != null && !avatarUrl.isEmpty())
                Glide.with(this).load(avatarUrl).circleCrop()
                        .placeholder(R.drawable.ic_avatar_placeholder).into(avatarView);
            else avatarView.setImageResource(R.drawable.ic_avatar_placeholder);
        }

        TextView ratingView = findViewById(R.id.novel_rating);
        if (ratingView != null) {
            double r = novel.getAverageRating();
            ratingView.setText(r > 0 ? String.format("%.1f", r) : "New");
        }
        TextView viewsView = findViewById(R.id.novel_views);
        if (viewsView != null) {
            long views = novel.getTotalViews();
            viewsView.setText(views > 0 ? formatCount(views) : "0");
        }

        // Update the read button label based on library status
        TextView readBtn = findViewById(R.id.btn_read);
        if (readBtn != null) {
            boolean hasProgress = novel.getCurrentChapterId() != null;
            readBtn.setText(hasProgress ? "Continue Reading" : "Start Reading");
        }
        // Wire "Continue Reading" to resume from saved chapter
        if (readBtn != null) {
            readBtn.setOnClickListener(v -> {
                String resumeChapterId = novel.getCurrentChapterId();
                openReader(resumeChapterId);
            });
        }
    }

    private void setText(int id, String text) {
        TextView v = findViewById(id);
        if (v != null && text != null) v.setText(text);
    }

    // ── Library toggle ─────────────────────────────────────────────────────

    private void toggleLibrary() {
        if (novelId == null) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            View btn = findViewById(R.id.btn_bookmark);
            if (btn != null) btn.performHapticFeedback(HapticFeedbackConstants.CONFIRM);
        }
        if (inLibrary) {
            libraryRepository.removeFromLibrary(novelId, (success, err) -> runOnUiThread(() -> {
                if (success) { inLibrary = false; updateBookmarkIcon(); BannerHelper.info(this, "Removed from library"); }
                else BannerHelper.error(this, "Error", err != null ? err : "Could not remove");
            }));
        } else {
            libraryRepository.addToLibrary(novelId, LibraryRepository.STATUS_READING,
                    (success, err) -> runOnUiThread(() -> {
                if (success) { inLibrary = true; updateBookmarkIcon(); BannerHelper.success(this, "Added to library! 📚"); }
                else BannerHelper.error(this, "Error", err != null ? err : "Could not add");
            }));
        }
    }

    private void updateBookmarkIcon() {
        ImageView btn = findViewById(R.id.btn_bookmark);
        if (btn == null) return;
        // FIX: use filled icon when saved
        btn.setImageResource(inLibrary ? R.drawable.ic_bookmark_filled : R.drawable.ic_bookmark);
        btn.clearColorFilter();
        if (!inLibrary) btn.setColorFilter(resolveAttrColor(android.R.attr.textColorTertiary));
    }

    // ── Review bottom sheet (redesigned) ──────────────────────────────────

    private void showReviewSheet() {
        if (userPreferences.getUserId() == null) {
            BannerHelper.warning(this, "Sign in required", "Log in to leave a review.");
            return;
        }

        BottomSheetDialog sheet = new BottomSheetDialog(this, R.style.BottomSheetTheme);
        android.view.View root = getLayoutInflater().inflate(R.layout.bottom_sheet_review, null);

        // Build star row programmatically into the container
        LinearLayout starRow = root.findViewById(R.id.review_star_row);
        final int[] starRating = {5};
        ImageView[] stars = new ImageView[5];
        for (int i = 0; i < 5; i++) {
            final int idx = i + 1;
            stars[i] = new ImageView(this);
            stars[i].setImageResource(R.drawable.ic_star_filled);
            stars[i].setColorFilter(Color.parseColor("#F59E0B"));
            LinearLayout.LayoutParams starLp = new LinearLayout.LayoutParams(dp(40), dp(40));
            starLp.setMarginEnd(dp(6));
            stars[i].setLayoutParams(starLp);
            stars[i].setOnClickListener(v -> {
                starRating[0] = idx;
                for (int j = 0; j < 5; j++) {
                    stars[j].setColorFilter(j < idx
                            ? Color.parseColor("#F59E0B")
                            : Color.parseColor("#CBD5E1"));
                }
            });
            starRow.addView(stars[i]);
        }

        android.widget.EditText reviewInput = root.findViewById(R.id.review_input);

        root.findViewById(R.id.btn_submit_review).setOnClickListener(v -> {
            sheet.dismiss();
            submitReview(starRating[0], reviewInput.getText().toString().trim());
        });
        root.findViewById(R.id.btn_cancel_review).setOnClickListener(v -> sheet.dismiss());

        sheet.setContentView(root);
        sheet.show();
    }

    private void submitReview(int rating, String reviewText) {
        String userId = userPreferences.getUserId();
        String token  = userPreferences.getAccessToken();
        if (userId == null || novelId == null) return;

        com.google.gson.JsonObject body = new com.google.gson.JsonObject();
        body.addProperty("novel_id",    novelId);
        body.addProperty("user_id",     userId);
        body.addProperty("rating",      rating);
        body.addProperty("review", reviewText);

        databaseService.upsert("ratings", body, "user_id,novel_id", token,
            new SupabaseDatabaseService.DatabaseCallback() {
                @Override public void onSuccess(String r) {
                    runOnUiThread(() -> {
                        BannerHelper.success(NovelDetailActivity.this, "Review submitted! ⭐");
                        // Fire review achievement (50-char minimum enforced by UI guard before call)
                        String uid = userPreferences.getUserId();
                        if (uid != null && achievementEngine != null && reviewText.length() >= 50) {
                            // Track total reviews in prefs (lightweight, no DB read)
                            android.content.SharedPreferences sp =
                                getSharedPreferences("gam_review_prefs", MODE_PRIVATE);
                            int totalReviews = sp.getInt("total_reviews_" + uid, 0) + 1;
                            sp.edit().putInt("total_reviews_" + uid, totalReviews).apply();
                            achievementEngine.onReviewWritten(
                                NovelDetailActivity.this, uid,
                                reviewText.length(), totalReviews,
                                System.currentTimeMillis());
                        }
                    });
                }
                @Override public void onError(String e) {
                    runOnUiThread(() -> BannerHelper.error(
                            NovelDetailActivity.this, "Submit failed", e));
                }
            });
    }

    // ── Author profile sheet ───────────────────────────────────────────────

    private void showAuthorProfileSheet() {
        if (currentNovel == null) return;
        BottomSheetDialog sheet = new BottomSheetDialog(this, R.style.BottomSheetTheme);
        android.view.View root = getLayoutInflater().inflate(R.layout.bottom_sheet_author_profile, null);

        ImageView avatar = root.findViewById(R.id.author_sheet_avatar);
        String avatarUrl = currentNovel.getAuthorAvatarUrl();
        if (avatarUrl != null && !avatarUrl.isEmpty())
            Glide.with(this).load(avatarUrl).circleCrop()
                    .placeholder(R.drawable.ic_avatar_placeholder).into(avatar);

        TextView nameView = root.findViewById(R.id.author_sheet_name);
        String authorName = currentNovel.getAuthorDisplayName();
        nameView.setText((authorName != null && !authorName.isEmpty()) ? authorName : "Unknown Author");

        TextView closeBtn = root.findViewById(R.id.author_sheet_close);
        closeBtn.setOnClickListener(v -> sheet.dismiss());

        // Follow button
        android.widget.Button followBtn = root.findViewById(R.id.btn_follow_author);
        if (followBtn != null) {
            followBtn.setOnClickListener(v -> {
                String userId = userPreferences.getUserId();
                String token  = userPreferences.getAccessToken();
                if (userId == null || currentNovel == null) return;
                String authorId = currentNovel.getAuthorId();
                if (authorId == null) return;

                // Persist follow to Supabase
                com.google.gson.JsonObject body = new com.google.gson.JsonObject();
                body.addProperty("follower_id", userId);
                body.addProperty("following_id", authorId);
                databaseService.upsert("user_follows", body, "follower_id,following_id", token,
                    new SupabaseDatabaseService.DatabaseCallback() {
                        @Override public void onSuccess(String r) {
                            runOnUiThread(() -> {
                                BannerHelper.success(NovelDetailActivity.this, "Now following!");
                                followBtn.setEnabled(false);
                                followBtn.setText("Following ✓");
                                // Achievement trigger
                                if (achievementEngine != null) {
                                    int totalFollowing = userPreferences.incrementFollowingCount();
                                    achievementEngine.onFollowAdded(
                                        NovelDetailActivity.this, userId,
                                        totalFollowing, System.currentTimeMillis());
                                }
                            });
                        }
                        @Override public void onError(String e) {
                            runOnUiThread(() -> BannerHelper.error(
                                NovelDetailActivity.this, "Follow failed", e));
                        }
                    });
                sheet.dismiss();
            });
        }

        fetchAuthorProfileIntoView(currentNovel.getAuthorId(), root);

        sheet.setContentView(root);
        sheet.show();
    }

    private void fetchAuthorProfileIntoView(String authorId, android.view.View sheetRoot) {
        if (authorId == null) return;
        String token = userPreferences.getAccessToken();
        databaseService.selectById("profiles", authorId, token,
            new SupabaseDatabaseService.DatabaseCallback() {
                @Override public void onSuccess(String r) {
                    runOnUiThread(() -> {
                        try {
                            com.google.gson.JsonObject obj =
                                new com.google.gson.Gson().fromJson(r, com.google.gson.JsonObject.class);
                            if (obj == null) return;
                            String bio = obj.has("bio") && !obj.get("bio").isJsonNull()
                                    ? obj.get("bio").getAsString() : null;
                            if (bio != null && !bio.isEmpty()) {
                                TextView bioView = sheetRoot.findViewById(R.id.author_sheet_bio);
                                if (bioView != null) {
                                    bioView.setText(bio);
                                    bioView.setVisibility(android.view.View.VISIBLE);
                                }
                            }
                        } catch (Exception ignored) {}
                    });
                }
                @Override public void onError(String e) {}
            });
    }

    // ── Sheet handle helper (kept for legacy callers) ─────────────────────

    private void addHandle(LinearLayout root) {
        View h = new View(this);
        android.graphics.drawable.GradientDrawable hBg = new android.graphics.drawable.GradientDrawable();
        hBg.setColor(Color.parseColor("#CBD5E1")); hBg.setCornerRadius(dp(100));
        h.setBackground(hBg);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(40), dp(4));
        lp.gravity = Gravity.CENTER_HORIZONTAL; lp.bottomMargin = dp(20);
        h.setLayoutParams(lp); root.addView(h);
    }

    // ── Share ──────────────────────────────────────────────────────────────

    private void shareNovelWithCard() {
        if (currentNovel == null) { shareSimple(); return; }
        bgExecutor.execute(() -> {
            Bitmap coverBmp = null;
            String coverUrl = currentNovel.getCoverImageUrl();
            if (coverUrl != null && !coverUrl.isEmpty()) {
                try { coverBmp = Glide.with(this).asBitmap().load(coverUrl).submit(160, 240).get(); }
                catch (Exception ignored) {}
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
        bgPaint.setShader(new LinearGradient(0, 0, W * 0.67f, H,
                Color.parseColor("#0085FF"), Color.parseColor("#0055BB"), Shader.TileMode.CLAMP));
        canvas.drawRect(0, 0, W, H, bgPaint);
        int coverLeft = (int)(W * 0.62f);
        if (coverBmp != null) {
            canvas.drawBitmap(coverBmp, null, new Rect(coverLeft, 0, W, H), new Paint(Paint.ANTI_ALIAS_FLAG));
            Paint fadePaint = new Paint();
            fadePaint.setShader(new LinearGradient(coverLeft, 0, coverLeft + dp(60), 0,
                    Color.parseColor("#FF0085FF"), Color.TRANSPARENT, Shader.TileMode.CLAMP));
            canvas.drawRect(coverLeft, 0, coverLeft + dp(60), H, fadePaint);
        }
        try {
            Bitmap logoBmp = android.graphics.BitmapFactory.decodeResource(getResources(), R.drawable.ic_logo);
            if (logoBmp != null) canvas.drawBitmap(Bitmap.createScaledBitmap(logoBmp, dp(32), dp(32), true), dp(32), dp(28), null);
        } catch (Exception ignored) {}
        Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.WHITE);
        android.graphics.Typeface tf = ResourcesCompat.getFont(this, R.font.inter_semibold);
        if (tf != null) textPaint.setTypeface(tf);
        textPaint.setTextSize(22f); textPaint.setAlpha(220);
        canvas.drawText("Read on NovelVerse", dp(76), dp(52), textPaint);
        textPaint.setTextSize(46f); textPaint.setAlpha(255);
        String t = currentNovel.getTitle() != null ? currentNovel.getTitle() : "";
        if (t.length() > 22) { int s = t.lastIndexOf(' ', 22); if (s < 0) s = 22;
            canvas.drawText(t.substring(0, s), dp(32), H / 2 - dp(20), textPaint);
            canvas.drawText(t.substring(s).trim(), dp(32), H / 2 + dp(36), textPaint);
        } else canvas.drawText(t, dp(32), H / 2 + dp(10), textPaint);
        textPaint.setTextSize(26f); textPaint.setAlpha(190);
        String author = currentNovel.getAuthorDisplayName() != null ? "by " + currentNovel.getAuthorDisplayName() : "";
        canvas.drawText(author, dp(32), H - dp(40), textPaint);
        File cachePath = new File(getCacheDir(), "shares");
        cachePath.mkdirs();
        File file = new File(cachePath, "novel_share.png");
        try (FileOutputStream fos = new FileOutputStream(file)) {
            bmp.compress(Bitmap.CompressFormat.PNG, 95, fos);
        } catch (IOException e) { shareSimple(); return; }
        Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", file);
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("image/png");
        shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
        shareIntent.putExtra(Intent.EXTRA_TEXT, "Reading \"" + currentNovel.getTitle() + "\" on NovelVerse!\nnovelverse://novel/" + novelId);
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(shareIntent, "Share Novel"));
    }

    private void shareSimple() {
        Intent i = new Intent(Intent.ACTION_SEND);
        i.setType("text/plain");
        i.putExtra(Intent.EXTRA_TEXT, "Check out this novel on NovelVerse!");
        startActivity(Intent.createChooser(i, "Share"));
    }

    // ── Options sheet ──────────────────────────────────────────────────────

    private void showNovelOptionsSheet() {
        BottomSheetDialog sheet = new BottomSheetDialog(this, R.style.BottomSheetTheme);
        android.view.View root = getLayoutInflater().inflate(R.layout.bottom_sheet_novel_options, null);
        root.findViewById(R.id.options_row_share).setOnClickListener(v -> { sheet.dismiss(); shareNovelWithCard(); });
        root.findViewById(R.id.options_row_report).setOnClickListener(v -> sheet.dismiss());
        root.findViewById(R.id.options_row_author).setOnClickListener(v -> { sheet.dismiss(); showAuthorProfileSheet(); });
        root.findViewById(R.id.options_row_library).setOnClickListener(v -> { sheet.dismiss(); toggleLibrary(); });
        sheet.setContentView(root); sheet.show();
    }

    // ── Download sheet ─────────────────────────────────────────────────────

    private void showDownloadConfirmSheet() {
        BottomSheetDialog sheet = new BottomSheetDialog(this, R.style.BottomSheetTheme);
        android.view.View root = getLayoutInflater().inflate(R.layout.bottom_sheet_download, null);
        root.findViewById(R.id.btn_download_5).setOnClickListener(v -> { sheet.dismiss(); enqueueDownload(5); });
        root.findViewById(R.id.btn_download_10).setOnClickListener(v -> { sheet.dismiss(); enqueueDownload(10); });
        root.findViewById(R.id.btn_download_all).setOnClickListener(v -> { sheet.dismiss(); enqueueDownload(Integer.MAX_VALUE); });
        root.findViewById(R.id.btn_cancel_download).setOnClickListener(v -> sheet.dismiss());
        sheet.setContentView(root); sheet.show();
    }

    // ── Chapter sort bottom sheet ──────────────────────────────────────────

    private boolean chaptersNewestFirst = false;

    private void showChapterSortSheet() {
        BottomSheetDialog sheet = new BottomSheetDialog(this, R.style.BottomSheetTheme);
        android.view.View root = getLayoutInflater().inflate(R.layout.bottom_sheet_chapter_sort, null);

        TextView checkOldest = root.findViewById(R.id.sort_check_oldest);
        TextView checkNewest = root.findViewById(R.id.sort_check_newest);
        checkOldest.setVisibility(chaptersNewestFirst ? View.INVISIBLE : View.VISIBLE);
        checkNewest.setVisibility(chaptersNewestFirst ? View.VISIBLE : View.INVISIBLE);

        root.findViewById(R.id.sort_oldest_first).setOnClickListener(v -> {
            chaptersNewestFirst = false;
            updateSortButton("Oldest first");
            sheet.dismiss();
        });
        root.findViewById(R.id.sort_newest_first).setOnClickListener(v -> {
            chaptersNewestFirst = true;
            updateSortButton("Newest first");
            RecyclerView rv = findViewById(R.id.chapters_recycler);
            if (rv != null && rv.getAdapter() instanceof ChapterListAdapter)
                ((ChapterListAdapter) rv.getAdapter()).setReversed(true);
            sheet.dismiss();
        });
        sheet.setContentView(root);
        sheet.show();
    }

    private void updateSortButton(String label) {
        TextView btnSortView = findViewById(R.id.btn_sort_chapters);
        if (btnSortView != null) btnSortView.setText(label);
        RecyclerView rv = findViewById(R.id.chapters_recycler);
        if (rv != null && rv.getAdapter() instanceof ChapterListAdapter)
            ((ChapterListAdapter) rv.getAdapter()).setReversed(chaptersNewestFirst);
    }

    private void enqueueDownload(int chapterCount) {
        int actual = Math.min(chapterCount == Integer.MAX_VALUE ? 20 : chapterCount, 20);
        List<OneTimeWorkRequest> tasks = new ArrayList<>();
        for (int i = 0; i < actual; i++)
            tasks.add(new OneTimeWorkRequest.Builder(com.novelverse.app.workers.SyncWorker.class).build());
        if (!tasks.isEmpty()) {
            WorkManager wm = WorkManager.getInstance(this);
            if (tasks.size() == 1) wm.enqueue(tasks.get(0));
            else wm.beginWith(tasks.get(0)).then(tasks.subList(1, tasks.size())).enqueue();
        }
        BannerHelper.success(this, "Downloading…", "Chapters will be available offline shortly.");
    }

    // ── Misc helpers ───────────────────────────────────────────────────────

    private void openReader(String chapterId) {
        Intent i = new Intent(this, ReaderActivity.class);
        i.putExtra(ReaderActivity.EXTRA_NOVEL_ID, novelId);
        if (currentNovel != null) i.putExtra(ReaderActivity.EXTRA_NOVEL_TITLE, currentNovel.getTitle());
        if (chapterId != null) i.putExtra(ReaderActivity.EXTRA_CHAPTER_ID, chapterId);
        startActivity(i);
    }

    private void openChapterListPage() {
        Intent i = new Intent(this, ChapterListActivity.class);
        i.putExtra(ChapterListActivity.EXTRA_NOVEL_ID, novelId);
        if (currentNovel != null) i.putExtra(ChapterListActivity.EXTRA_NOVEL_TITLE, currentNovel.getTitle());
        startActivity(i);
    }

    // ── Review adapter ─────────────────────────────────────────────────────

    private class ReviewAdapter extends RecyclerView.Adapter<ReviewAdapter.VH> {
        private final com.google.gson.JsonArray reviews;

        ReviewAdapter(com.google.gson.JsonArray reviews) { this.reviews = reviews; }

        @Override public VH onCreateViewHolder(android.view.ViewGroup parent, int viewType) {
            android.view.View v = getLayoutInflater().inflate(R.layout.item_review, parent, false);
            return new VH(v);
        }

        @Override public void onBindViewHolder(VH h, int pos) {
            try {
                com.google.gson.JsonObject review = reviews.get(pos).getAsJsonObject();

                // Stars
                int rating = review.has("rating") && !review.get("rating").isJsonNull()
                        ? review.get("rating").getAsInt() : 0;
                h.starRow.removeAllViews();
                for (int i = 0; i < 5; i++) {
                    android.widget.ImageView star = new android.widget.ImageView(NovelDetailActivity.this);
                    star.setImageResource(R.drawable.ic_star_filled);
                    star.setColorFilter(i < rating ? 0xFFF59E0B : 0xFFCBD5E1);
                    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(14), dp(14));
                    lp.setMarginEnd(dp(2));
                    star.setLayoutParams(lp);
                    h.starRow.addView(star);
                }

                // Reviewer name
                String name = "Anonymous";
                if (review.has("profiles") && !review.get("profiles").isJsonNull()) {
                    com.google.gson.JsonObject prof = review.getAsJsonObject("profiles");
                    if (prof.has("display_name") && !prof.get("display_name").isJsonNull())
                        name = prof.get("display_name").getAsString();
                }
                h.name.setText(name);

                // Review text
                String text = review.has("review") && !review.get("review").isJsonNull()
                        ? review.get("review").getAsString().trim() : "";
                if (!text.isEmpty()) {
                    h.text.setText(text);
                    h.text.setVisibility(android.view.View.VISIBLE);
                } else {
                    h.text.setVisibility(android.view.View.GONE);
                }

                // Date
                if (review.has("created_at") && !review.get("created_at").isJsonNull()) {
                    String raw = review.get("created_at").getAsString();
                    try {
                        java.text.SimpleDateFormat in = new java.text.SimpleDateFormat(
                                "yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.US);
                        java.text.SimpleDateFormat out = new java.text.SimpleDateFormat(
                                "MMM d", java.util.Locale.US);
                        h.date.setText(out.format(in.parse(raw)));
                    } catch (Exception e) {
                        h.date.setText("");
                    }
                }

                // Avatar
                if (review.has("profiles") && !review.get("profiles").isJsonNull()) {
                    com.google.gson.JsonObject prof = review.getAsJsonObject("profiles");
                    if (prof.has("avatar_url") && !prof.get("avatar_url").isJsonNull()) {
                        Glide.with(NovelDetailActivity.this)
                                .load(prof.get("avatar_url").getAsString())
                                .circleCrop()
                                .placeholder(R.drawable.ic_avatar_placeholder)
                                .into(h.avatar);
                    }
                }

            } catch (Exception ignored) {}
        }

        @Override public int getItemCount() { return reviews.size(); }

        class VH extends RecyclerView.ViewHolder {
            android.widget.ImageView avatar;
            TextView name, text, date;
            LinearLayout starRow;
            VH(android.view.View v) {
                super(v);
                avatar  = v.findViewById(R.id.reviewer_avatar);
                name    = v.findViewById(R.id.reviewer_name);
                text    = v.findViewById(R.id.review_text);
                date    = v.findViewById(R.id.review_date);
                starRow = v.findViewById(R.id.star_row);
            }
        }
    }

    private void expandSynopsis() {
        TextView desc = findViewById(R.id.novel_description);
        TextView btn  = findViewById(R.id.btn_read_more);
        if (desc == null) return;
        if (desc.getMaxLines() == 6) { desc.setMaxLines(Integer.MAX_VALUE); if (btn != null) btn.setText("Show less"); }
        else { desc.setMaxLines(6); if (btn != null) btn.setText("Read more"); }
    }

    private String formatCount(long n) {
        if (n >= 1_000_000) return String.format("%.1fM", n / 1_000_000f);
        if (n >= 1_000)     return String.format("%.1fk", n / 1_000f);
        return String.valueOf(n);
    }

    private int resolveAttrColor(int attr) {
        android.util.TypedValue tv = new android.util.TypedValue();
        getTheme().resolveAttribute(attr, tv, true);
        return tv.data;
    }

    private int dp(int v) { return Math.round(v * getResources().getDisplayMetrics().density); }
}

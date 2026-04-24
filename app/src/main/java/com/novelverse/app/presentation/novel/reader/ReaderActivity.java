package com.novelverse.app.presentation.novel.reader;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Layout;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.LeadingMarginSpan;
import android.view.ActionMode;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.ViewGroup;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.widget.NestedScrollView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.novelverse.app.R;
import com.novelverse.app.data.local.preferences.UserPreferences;
import com.novelverse.app.data.repository.ChapterRepository;
import com.novelverse.app.data.repository.NovelRepository;
import com.novelverse.app.domain.models.Chapter;
import com.novelverse.app.presentation.common.views.ReaderSliderView;
import com.novelverse.app.data.remote.supabase.SupabaseDatabaseService;
import com.novelverse.app.presentation.common.views.ShimmerView;
import com.novelverse.app.presentation.tts.TtsPlayerActivity;
import com.novelverse.app.presentation.tts.TtsMiniPlayerFragment;
import com.novelverse.app.ui.banner.BannerHelper;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executor;

import javax.inject.Inject;
import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class ReaderActivity extends AppCompatActivity {

    public static final String EXTRA_NOVEL_ID    = "novel_id";
    public static final String EXTRA_CHAPTER_ID  = "chapter_id";
    public static final String EXTRA_NOVEL_TITLE = "novel_title";
    public static final String EXTRA_COVER_URL   = "novel_cover_url";

    @Inject ChapterRepository        chapterRepository;

    private TtsMiniPlayerFragment miniPlayerFrag;
    @Inject NovelRepository          novelRepository;
    @Inject UserPreferences          userPreferences;
    @Inject SupabaseDatabaseService  dbService;
    @Inject com.novelverse.app.domain.gamification.AchievementEngine achievementEngine;
    @Inject com.novelverse.app.data.repository.GamificationRepository gamificationRepository;

    // Content views
    private NestedScrollView readerScroll;
    private TextView         chapterContent;
    private ViewPager2       readerPager;
    private ReaderPageAdapter pageAdapter;

    // Top bar
    private View     topBar;
    private TextView readerNovelTitle;
    private ShimmerView skeletonNovelTitle;

    // Bottom bar
    private View     bottomBar;
    private SeekBar  readerSeekbar;
    private TextView progressText;
    private TextView chapterNumberLabel;

    // Skeletons
    private ShimmerView skeletonChapterTitle;
    private ShimmerView skeletonChapterLabel;
    private LinearLayout skeletonContentGroup;
    private TextView    chapterTitle;

    // State
    private boolean barsVisible = false;
    private PopupWindow selectionPopup;
    private GestureDetector tapDetector;
    private String  novelId;
    private String  currentChapterId;
    private String  novelCoverUrl;
    private List<Chapter> allChapters;
    private int currentIndex = 0;
    private boolean chaptersReversed = false;
    private int     lastSavedProgressPct = -1;  // -1 = never saved; avoids redundant upserts

    // Gamification tracking
    private long    sessionStartMs   = 0;
    private double  sessionStartPct  = 0.0;
    private int     totalNovelsRead;   // loaded from UserPreferences
    private int     totalChaptersRead; // loaded from UserPreferences

    private final Handler autoScrollHandler = new Handler(Looper.getMainLooper());
    private Runnable autoScrollRunnable;

    // Font resource IDs matched by family key
    private static final String[] FONT_KEYS  = { "inter", "lora", "merriweather", "playfair", "nunito", "opendyslexic" };
    private static final String[] FONT_LABELS = { "Inter", "Lora", "Merriweather", "Playfair", "Nunito", "Dyslexic" };
    private static final int[]    FONT_RES    = { R.font.inter_regular, R.font.lora_regular,
                                                   R.font.merriweather_regular, R.font.playfair_display_regular,
                                                   R.font.nunito_regular, R.font.opendyslexic_regular };

    // ── Lifecycle ─────────────────────────────────────────────────────────

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reader);

        novelId          = getIntent().getStringExtra(EXTRA_NOVEL_ID);
        currentChapterId = getIntent().getStringExtra(EXTRA_CHAPTER_ID);
        novelCoverUrl    = getIntent().getStringExtra(EXTRA_COVER_URL);
        String novelTitle = getIntent().getStringExtra(EXTRA_NOVEL_TITLE);

        // Bind views
        topBar               = findViewById(R.id.reader_top_bar);
        bottomBar            = findViewById(R.id.reader_bottom_bar);

        // Start with bars hidden — user taps to reveal
        if (topBar    != null) topBar.setVisibility(View.GONE);
        if (bottomBar != null) bottomBar.setVisibility(View.GONE);
        readerScroll         = findViewById(R.id.reader_scroll);
        chapterContent       = findViewById(R.id.chapter_content);
        readerPager          = findViewById(R.id.reader_pager);
        readerNovelTitle     = findViewById(R.id.reader_novel_title);
        skeletonNovelTitle   = findViewById(R.id.skeleton_novel_title);
        chapterTitle         = findViewById(R.id.chapter_title);
        skeletonChapterTitle = findViewById(R.id.skeleton_chapter_title);
        skeletonContentGroup = findViewById(R.id.skeleton_content_group);
        chapterNumberLabel   = findViewById(R.id.chapter_number_label);
        skeletonChapterLabel = findViewById(R.id.skeleton_chapter_label);
        readerSeekbar        = findViewById(R.id.reader_seekbar);
        progressText         = findViewById(R.id.progress_text);

        // Show novel title immediately if passed in (no skeleton needed for top bar title)
        if (novelTitle != null) {
            showNovelTitle(novelTitle);
        }

        // Bind currency HUD (shows Ink balance while reading)
        com.novelverse.app.presentation.common.views.CurrencyHudView currencyHud =
            findViewById(R.id.reader_currency_hud);
        String hudUserId = userPreferences.getUserId();
        if (currencyHud != null && hudUserId != null && gamificationRepository != null) {
            gamificationRepository.getCurrencyLive(hudUserId)
                .observe(this, currency -> {
                    if (currency != null) {
                        currencyHud.setInk(currency.getInkBalance());
                        currencyHud.setVisibility(android.view.View.VISIBLE);
                    }
                });
        }

        // Register level-up listener → show celebration toast on main thread
        if (gamificationRepository != null) {
            gamificationRepository.setLevelUpListener(newLevel ->
                runOnUiThread(() ->
                    com.novelverse.app.presentation.common.views.LevelUpToastView.show(this, newLevel)));
        }

        // Page adapter
        pageAdapter = new ReaderPageAdapter();
        readerPager.setAdapter(pageAdapter);

        setupAnnotation();
        setupTapToToggle();
        setupScrollProgress();
        setupSwipeNavigation();

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        // ── TTS button ─────────────────────────────────────────────────────
        View ttsBtn = findViewById(R.id.btn_reader_tts);
        if (ttsBtn != null) ttsBtn.setOnClickListener(v -> openTtsPlayer());

        // ── TTS Mini Player Fragment ─────────────────────────────────────────
        miniPlayerFrag = new TtsMiniPlayerFragment();
        getSupportFragmentManager().beginTransaction()
            .replace(R.id.tts_mini_player_container, miniPlayerFrag, "tts_mini")
            .commitNow();

        View settingsBtn = findViewById(R.id.btn_reader_settings);
        if (settingsBtn != null) settingsBtn.setOnClickListener(v -> openReaderSettings());

        View prevBtn = findViewById(R.id.btn_prev_chapter);
        if (prevBtn != null) prevBtn.setOnClickListener(v -> goToPrevChapter());

        View nextBtn = findViewById(R.id.btn_next_chapter);
        if (nextBtn != null) nextBtn.setOnClickListener(v -> goToNextChapter());

        if (chapterNumberLabel != null)
            chapterNumberLabel.setOnClickListener(v -> showChapterListSheet());

        applyWindowPreferences();

        if (novelId != null) loadAllChapters();
        if (userPreferences.isAutoScrollEnabled()) startAutoScroll(userPreferences.getAutoScrollSpeed());
    }

    @Override
    protected void onResume() {
        super.onResume();
        applyReaderPreferences();
        applyWindowPreferences();
        if (userPreferences != null) userPreferences.updateAndGetStreak();
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Persist progress in onPause so library status is committed to Supabase
        // BEFORE the previous screen's onResume fires (Android lifecycle order).
        saveReadingProgress();
        fireGamificationOnPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Also persist on stop for safety (handles process-death edge cases).
        saveReadingProgress();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopAutoScroll();
    }

    // ── Gamification hooks ────────────────────────────────────────────────────

    /**
     * Called at every 10% scroll milestone.
     * Only fires AchievementEngine for real progress (anti-exploit: ignores backward scrolls).
     */
    private void fireGamificationOnScroll(int newPct) {
        String userId = userPreferences.getUserId();
        if (userId == null || achievementEngine == null) return;

        double prevPct = sessionStartPct;
        long nowMs = System.currentTimeMillis();
        int secondsRead = (int)((nowMs - sessionStartMs) / 1000L);

        // Only advance counter, never retreat
        if (newPct > prevPct) {
            sessionStartPct = newPct;

            // Evaluate completion flags and increment SharedPrefs counters on main thread
            // before dispatching, so counters stay consistent even if called rapidly.
            final boolean chapterComplete = newPct >= 98 && prevPct < 98;
            final boolean novelComplete   = chapterComplete
                    && allChapters != null
                    && currentIndex == allChapters.size() - 1;

            final int chaptersForEngine;
            final int novelsForEngine;
            if (chapterComplete) {
                totalChaptersRead = userPreferences.incrementChaptersRead();
                chaptersForEngine = totalChaptersRead;
                if (novelComplete) {
                    totalNovelsRead = userPreferences.incrementNovelsRead();
                    novelsForEngine = totalNovelsRead;
                } else {
                    novelsForEngine = totalNovelsRead;
                }
            } else {
                chaptersForEngine = totalChaptersRead;
                novelsForEngine   = totalNovelsRead;
            }

            // All AchievementEngine calls touch Room (via tryUnlock → checkAchievementRateLimit).
            // Dispatch the entire block to a background thread.
            final double finalNewPct  = newPct / 100.0;
            final double finalPrevPct = prevPct / 100.0;
            final int    finalSeconds = secondsRead;
            new Thread(() -> {
                achievementEngine.onReadingPause(
                    ReaderActivity.this, userId,
                    finalSeconds, finalNewPct, finalPrevPct,
                    novelsForEngine, chaptersForEngine, nowMs);
                if (chapterComplete) {
                    achievementEngine.onChapterComplete(
                        ReaderActivity.this, userId, chaptersForEngine, nowMs);
                    if (novelComplete) {
                        achievementEngine.onNovelComplete(
                            ReaderActivity.this, userId, novelsForEngine, nowMs);
                    }
                }
            }).start();
        }
    }

    /** Called from onPause — fires reading pause event with total session time. */
    private void fireGamificationOnPause() {
        String userId = userPreferences.getUserId();
        if (userId == null || achievementEngine == null) return;

        long nowMs = System.currentTimeMillis();
        int secondsRead = (int)((nowMs - sessionStartMs) / 1000L);

        // Calculate current scroll %
        double currentPct = 0.0;
        if (readerScroll != null && readerScroll.getChildAt(0) != null) {
            int totalH = readerScroll.getChildAt(0).getHeight() - readerScroll.getHeight();
            if (totalH > 0) {
                currentPct = Math.min(1.0, (double) readerScroll.getScrollY() / totalH);
            }
        }

        // Record streak activity — Room forbids main-thread queries; dispatch to background
        final String _userId = userId;
        final long _nowMs = nowMs;
        new Thread(() -> gamificationRepository.recordReadingActivity(_userId, _nowMs)).start();

        achievementEngine.onReadingPause(
            this, userId,
            secondsRead,
            currentPct,
            sessionStartPct / 100.0,
            totalNovelsRead,
            totalChaptersRead,
            nowMs
        );

        // Reset session baseline for next open
        sessionStartMs  = nowMs;
        sessionStartPct = currentPct * 100.0;
    }

    // ── Reading progress persistence ──────────────────────────────────────

    /**
     * Upserts current reading progress to Supabase.
     * Called from onStop (always) and from the scroll listener every 10% change.
     * Uses user_id + novel_id as the conflict key so it's safe to call repeatedly.
     */
    private void saveReadingProgress() {
        if (novelId == null || currentChapterId == null) return;
        String userId = userPreferences.getUserId();
        String token  = userPreferences.getAccessToken();
        if (userId == null || token == null) return;

        // Calculate current scroll progress
        int pct = 0;
        if (readerScroll != null && readerScroll.getChildAt(0) != null) {
            int totalH = readerScroll.getChildAt(0).getHeight() - readerScroll.getHeight();
            if (totalH > 0) {
                pct = Math.min(100, (readerScroll.getScrollY() * 100) / totalH);
            }
        }

        // Skip if nothing changed (avoid redundant network calls)
        if (pct == lastSavedProgressPct) return;
        lastSavedProgressPct = pct;

        final boolean isCompleted = (pct >= 98);

        // Upsert reading_progress row (conflict on user_id + novel_id)
        com.google.gson.JsonObject progress = new com.google.gson.JsonObject();
        progress.addProperty("id",                  java.util.UUID.randomUUID().toString());
        progress.addProperty("user_id",             userId);
        progress.addProperty("novel_id",            novelId);
        progress.addProperty("chapter_id",          currentChapterId);
        progress.addProperty("scroll_position",     readerScroll != null ? readerScroll.getScrollY() : 0);
        progress.addProperty("progress_percentage", (double) pct);
        progress.addProperty("is_completed",        isCompleted);
        progress.addProperty("last_read_at",        new java.text.SimpleDateFormat(
                "yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US)
                .format(new java.util.Date()));

        dbService.upsert("reading_progress", progress, "user_id,novel_id", token,
                new SupabaseDatabaseService.DatabaseCallback() {
                    @Override public void onSuccess(String r) {
                        // If the user finished the book, also update user_library status
                        if (isCompleted) updateLibraryStatus(userId, token, "completed");
                    }
                    @Override public void onError(String e) {
                        android.util.Log.w("ReaderActivity", "Progress save failed: " + e);
                    }
                });

        // Also ensure user_library row exists with "reading" status
        if (!isCompleted) updateLibraryStatus(userId, token, "reading");
    }

    /** Upserts user_library row to keep status in sync with reading progress. */
    private void updateLibraryStatus(String userId, String token, String status) {
        com.google.gson.JsonObject lib = new com.google.gson.JsonObject();
        lib.addProperty("user_id",    userId);
        lib.addProperty("novel_id",   novelId);
        lib.addProperty("status",     status);
        lib.addProperty("updated_at", new java.text.SimpleDateFormat(
                "yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US)
                .format(new java.util.Date()));
        dbService.upsert("user_library", lib, "user_id,novel_id", token,
                new SupabaseDatabaseService.DatabaseCallback() {
                    @Override public void onSuccess(String r) {}
                    @Override public void onError(String e) {
                        android.util.Log.w("ReaderActivity", "Library status update failed: " + e);
                    }
                });
    }

    // ── dispatchTouchEvent — feeds the tap detector before any view sees the event ──

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        // Stop auto-scroll on any touch
        if (ev.getAction() == MotionEvent.ACTION_DOWN) stopAutoScroll();
        // Feed the tap detector; super handles normal dispatch (scrolling, clicks, etc.)
        if (tapDetector != null) tapDetector.onTouchEvent(ev);
        return super.dispatchTouchEvent(ev);
    }

    // ── Volume key chapter flip ───────────────────────────────────────────

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (userPreferences.isVolumeFlipEnabled()) {
            if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) { goToNextChapter(); return true; }
            if (keyCode == KeyEvent.KEYCODE_VOLUME_UP)   { goToPrevChapter(); return true; }
        }
        return super.onKeyDown(keyCode, event);
    }

    // ── Skeleton helpers ──────────────────────────────────────────────────

    /** Call when chapter data is about to load — show skeletons, hide real views. */
    private void showSkeletons() {
        if (skeletonChapterTitle != null) skeletonChapterTitle.setVisibility(View.VISIBLE);
        if (skeletonContentGroup != null) skeletonContentGroup.setVisibility(View.VISIBLE);
        if (skeletonChapterLabel != null) skeletonChapterLabel.setVisibility(View.VISIBLE);
        if (chapterTitle         != null) chapterTitle.setVisibility(View.GONE);
        if (chapterContent       != null) chapterContent.setVisibility(View.GONE);
        if (chapterNumberLabel   != null) chapterNumberLabel.setVisibility(View.GONE);
    }

    /** Call after chapter content is set — hide skeletons, reveal real views. */
    private void hideSkeletons() {
        if (skeletonChapterTitle != null) skeletonChapterTitle.setVisibility(View.GONE);
        if (skeletonContentGroup != null) skeletonContentGroup.setVisibility(View.GONE);
        if (skeletonChapterLabel != null) skeletonChapterLabel.setVisibility(View.GONE);
        if (chapterTitle         != null) chapterTitle.setVisibility(View.VISIBLE);
        if (chapterContent       != null) chapterContent.setVisibility(View.VISIBLE);
        if (chapterNumberLabel   != null) chapterNumberLabel.setVisibility(View.VISIBLE);
    }

    private void showNovelTitle(String title) {
        if (skeletonNovelTitle != null) skeletonNovelTitle.setVisibility(View.GONE);
        if (readerNovelTitle   != null) {
            readerNovelTitle.setVisibility(View.VISIBLE);
            readerNovelTitle.setText(title);
        }
    }

    // ── Chapter loading ───────────────────────────────────────────────────

    private void loadAllChapters() {
        showSkeletons();
        chapterRepository.getChapters(novelId).observe(this, chapters -> {
            if (chapters == null || chapters.isEmpty()) return;
            allChapters = chapters;
            if (currentChapterId != null) {
                for (int i = 0; i < chapters.size(); i++) {
                    if (currentChapterId.equals(chapters.get(i).getId())) { currentIndex = i; break; }
                }
            } else {
                currentIndex     = 0;
                currentChapterId = chapters.get(0).getId();
            }
            updateChapterLabel();
            loadChapterWithBiometricCheck(currentChapterId);
        });
    }

    private void updateChapterLabel() {
        if (allChapters == null) return;
        int total = allChapters.size();
        if (chapterNumberLabel != null)
            chapterNumberLabel.setText("Chapter " + (currentIndex + 1) + " of " + total);
        View prev = findViewById(R.id.btn_prev_chapter);
        View next = findViewById(R.id.btn_next_chapter);
        boolean hasPrev = currentIndex > 0;
        boolean hasNext = currentIndex < total - 1;
        if (prev != null) {
            prev.setBackground(hasPrev
                    ? getDrawable(R.drawable.bg_chapter_nav_active)
                    : getDrawable(R.drawable.bg_chapter_nav_inactive));
            if (prev instanceof android.widget.ImageView) {
                ((android.widget.ImageView) prev).setColorFilter(hasPrev ? ContextCompat.getColor(this, R.color.white) : resolveAttrColor(com.google.android.material.R.attr.colorOnSurfaceVariant));
            }
            prev.setAlpha(1f);
        }
        if (next != null) {
            next.setBackground(hasNext
                    ? getDrawable(R.drawable.bg_chapter_nav_active)
                    : getDrawable(R.drawable.bg_chapter_nav_inactive));
            if (next instanceof android.widget.ImageView) {
                ((android.widget.ImageView) next).setColorFilter(hasNext ? ContextCompat.getColor(this, R.color.white) : resolveAttrColor(com.google.android.material.R.attr.colorOnSurfaceVariant));
            }
            next.setAlpha(1f);
        }
    }

    // ── Biometric gate ────────────────────────────────────────────────────

    private void loadChapterWithBiometricCheck(String chapterId) {
        if (userPreferences.isBiometricLockEnabled()) {
            Executor exec = ContextCompat.getMainExecutor(this);
            BiometricPrompt.PromptInfo info = new BiometricPrompt.PromptInfo.Builder()
                    .setTitle("Verify Identity").setSubtitle("Unlock chapter")
                    .setNegativeButtonText("Cancel").build();
            new BiometricPrompt(this, exec, new BiometricPrompt.AuthenticationCallback() {
                @Override public void onAuthenticationSucceeded(BiometricPrompt.AuthenticationResult r) {
                    runOnUiThread(() -> loadChapter(chapterId));
                }
                @Override public void onAuthenticationFailed() {}
            }).authenticate(info);
        } else {
            loadChapter(chapterId);
        }
    }

    private void loadChapter(String chapterId) {
        showSkeletons();
        lastSavedProgressPct = -1;  // reset so first scroll saves immediately
        chapterRepository.getChapterById(chapterId).observe(this, chapter -> {
            if (chapter == null) {
                // Chapter failed to load — hide skeletons and show an error placeholder
                // so the skeleton doesn't stay on screen indefinitely
                hideSkeletons();
                if (chapterTitle != null) {
                    chapterTitle.setText("Could not load chapter");
                }
                if (chapterContent != null) {
                    chapterContent.setText("There was a problem loading this chapter. Please go back and try again.");
                }
                return;
            }

            if (chapterTitle != null) chapterTitle.setText(chapter.getTitle());
            if (chapterContent != null) {
                chapterContent.setText(chapter.getContent());
            }

            // Feed page adapter too
            pageAdapter.setContent(chapter.getContent());

            applyReaderPreferences();
            applyScreenshotLock(chapter);

            hideSkeletons();

            if (readerScroll != null) readerScroll.smoothScrollTo(0, 0);
        });
    }

    // ── Reader preferences ────────────────────────────────────────────────

    private void applyReaderPreferences() {
        if (chapterContent == null) return;

        int    fontSize     = userPreferences.getFontSize();
        float  lineSpacing  = userPreferences.getLineSpacing();
        float  paraSpacing  = userPreferences.getParagraphSpacing();
        float  letterSp     = userPreferences.getLetterSpacing();
        String theme        = userPreferences.getReaderTheme();
        String alignment    = userPreferences.getTextAlignment();
        int    sideMarginDp = userPreferences.getSideMargin();
        String readingMode  = userPreferences.getReadingMode();

        // ─ Font size
        chapterContent.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, fontSize);

        // ─ Line spacing
        chapterContent.setLineSpacing(0f, lineSpacing);

        // ─ Letter spacing
        chapterContent.setLetterSpacing(letterSp);

        // ─ Text alignment
        if ("justify".equals(alignment)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                chapterContent.setJustificationMode(Layout.JUSTIFICATION_MODE_INTER_WORD);
            }
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                chapterContent.setJustificationMode(Layout.JUSTIFICATION_MODE_NONE);
            }
            chapterContent.setGravity(Gravity.START);
        }

        // ─ Side margin as padding
        int sidePx = dp(sideMarginDp);
        chapterContent.setPadding(sidePx, 0, sidePx, 0);

        // ─ Font family (dyslexia flag overrides everything)
        if (userPreferences.isDyslexiaFontEnabled()) {
            android.graphics.Typeface tf = ResourcesCompat.getFont(this, R.font.opendyslexic_regular);
            if (tf != null) chapterContent.setTypeface(tf);
        } else {
            applyFontFamily(userPreferences.getFontFamily());
        }

        // ─ Theme colors
        int bgColor, textColor, titleColor, barBg;
        switch (theme != null ? theme : "light") {
            case "dark":
                bgColor    = ContextCompat.getColor(this, R.color.reader_theme_dark_bg);
                textColor  = ContextCompat.getColor(this, R.color.reader_text_dark);
                titleColor = ContextCompat.getColor(this, R.color.white);
                barBg      = ContextCompat.getColor(this, R.color.reader_theme_dark_bar);
                break;
            case "sepia":
                bgColor    = ContextCompat.getColor(this, R.color.reader_background_sepia);
                textColor  = ContextCompat.getColor(this, R.color.reader_text_sepia);
                titleColor = ContextCompat.getColor(this, R.color.reader_theme_sepia_title);
                barBg      = ContextCompat.getColor(this, R.color.reader_theme_sepia_bar);
                break;
            case "amoled":
                bgColor    = ContextCompat.getColor(this, R.color.black);
                textColor  = ContextCompat.getColor(this, R.color.reader_theme_amoled_text);
                titleColor = ContextCompat.getColor(this, R.color.white);
                barBg      = ContextCompat.getColor(this, R.color.reader_theme_amoled_bar);
                break;
            default: // light
                bgColor    = ContextCompat.getColor(this, R.color.white);
                textColor  = ContextCompat.getColor(this, R.color.reader_theme_light_text);
                titleColor = ContextCompat.getColor(this, R.color.reader_theme_light_title);
                barBg      = ContextCompat.getColor(this, R.color.reader_theme_light_bar);
                break;
        }

        if (readerScroll   != null) readerScroll.setBackgroundColor(bgColor);
        chapterContent.setTextColor(textColor);
        chapterContent.setBackgroundColor(bgColor);
        // Bars always stay with the app surface color — they do NOT follow the reading theme
        // so the UI chrome remains consistent regardless of sepia/dark/amoled modes
        barBg = resolveAttrColor(com.google.android.material.R.attr.colorSurface);
        if (topBar    != null) topBar.setBackgroundColor(barBg);
        if (bottomBar != null) bottomBar.setBackgroundColor(barBg);

        if (chapterTitle != null) {
            chapterTitle.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, fontSize + 3);
            chapterTitle.setTextColor(titleColor);
            chapterTitle.setBackgroundColor(bgColor);
        }

        // ─ Update shimmer colors for the active theme
        int cAmoled = ContextCompat.getColor(this, R.color.black);
        int cDark   = ContextCompat.getColor(this, R.color.reader_theme_dark_bg);
        int cSepia  = ContextCompat.getColor(this, R.color.reader_background_sepia);
        int shimBase  = (bgColor == cAmoled) ? ContextCompat.getColor(this, R.color.reader_shimmer_base_amoled)
                      : (bgColor == cDark)   ? ContextCompat.getColor(this, R.color.reader_shimmer_base_dark)
                      : (bgColor == cSepia)  ? ContextCompat.getColor(this, R.color.reader_shimmer_base_sepia)
                      :                        ContextCompat.getColor(this, R.color.reader_shimmer_base_light);
        int shimShine = (bgColor == cAmoled) ? ContextCompat.getColor(this, R.color.reader_shimmer_shine_amoled)
                      : (bgColor == cDark)   ? ContextCompat.getColor(this, R.color.reader_shimmer_shine_dark)
                      : (bgColor == cSepia)  ? ContextCompat.getColor(this, R.color.reader_shimmer_shine_sepia)
                      :                        ContextCompat.getColor(this, R.color.reader_shimmer_shine_light);
        updateShimmerColors(shimBase, shimShine);

        // ─ Reading mode
        boolean paged = "paged".equals(readingMode);
        readerScroll.setVisibility(paged ? View.GONE  : View.VISIBLE);
        readerPager .setVisibility(paged ? View.VISIBLE : View.GONE);
        if (paged) {
            pageAdapter.applyPreferences(fontSize, lineSpacing, letterSp, bgColor, textColor, sidePx, this);
        }

        // ─ Brightness
        applyBrightness();

        // ─ Keep screen on
        if (userPreferences.isKeepScreenOn()) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }

        // ─ Sync status bar color to reader background
        getWindow().setStatusBarColor(bgColor);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            View decor = getWindow().getDecorView();
            int sysFlags = decor.getSystemUiVisibility();
            // Light backgrounds (light/sepia) need dark icons; dark backgrounds need light icons
            boolean lightBackground = (bgColor == ContextCompat.getColor(this, R.color.white) || bgColor == cSepia);
            if (lightBackground) {
                sysFlags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            } else {
                sysFlags &= ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            }
            decor.setSystemUiVisibility(sysFlags);
        }
    }

    private void applyFontFamily(String key) {
        if (key == null) key = "inter";
        for (int i = 0; i < FONT_KEYS.length; i++) {
            if (FONT_KEYS[i].equals(key)) {
                try {
                    android.graphics.Typeface tf = ResourcesCompat.getFont(this, FONT_RES[i]);
                    if (tf != null) chapterContent.setTypeface(tf);
                } catch (Exception ignored) { /* font file may not exist yet */ }
                return;
            }
        }
    }

    private void applyBrightness() {
        WindowManager.LayoutParams lp = getWindow().getAttributes();
        if (userPreferences.isFollowSystemBrightness()) {
            lp.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE;
        } else {
            float b = userPreferences.getScreenBrightness();
            lp.screenBrightness = Math.max(0.01f, Math.min(1f, b));
        }
        getWindow().setAttributes(lp);
    }

    private void applyWindowPreferences() {
        if (userPreferences.isKeepScreenOn()) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
        applyBrightness();
    }

    private void updateShimmerColors(int base, int shine) {
        // Apply to all skeleton shimmers in the layout
        int[] skelIds = { R.id.skeleton_novel_title, R.id.skeleton_chapter_title,
                          R.id.skeleton_chapter_label };
        for (int id : skelIds) {
            View v = findViewById(id);
            if (v instanceof ShimmerView) ((ShimmerView) v).setShimmerColors(base, shine);
        }
        if (skeletonContentGroup != null) {
            for (int i = 0; i < skeletonContentGroup.getChildCount(); i++) {
                View child = skeletonContentGroup.getChildAt(i);
                if (child instanceof ShimmerView) ((ShimmerView) child).setShimmerColors(base, shine);
            }
        }
    }

    // ── Tap-to-toggle ──────────────────────────────────────────────────────

    private void setupTapToToggle() {
        // Build the detector once — dispatchTouchEvent feeds it with all events
        // before any view gets a chance to consume them, so it works regardless
        // of whether NestedScrollView swallows the touch for scrolling.
        tapDetector = new GestureDetector(this,
            new GestureDetector.SimpleOnGestureListener() {
                @Override
                public boolean onSingleTapUp(MotionEvent e) {
                    if (readerScroll == null) return false;
                    float x = e.getX(), w = readerScroll.getWidth();
                    float edgeFraction = 0.18f;

                    if (userPreferences.isTapZoneNavEnabled()) {
                        if (x < w * edgeFraction)        { goToPrevChapter(); return true; }
                        if (x > w * (1f - edgeFraction)) { goToNextChapter(); return true; }
                    }

                    if (x > w * edgeFraction && x < w * (1f - edgeFraction)) {
                        toggleBars();
                        return true;
                    }
                    return false;
                }
            });
        tapDetector.setIsLongpressEnabled(false);
    }

    private void toggleBars() {
        barsVisible = !barsVisible;
        animateBar(topBar,    barsVisible, true);
        animateBar(bottomBar, barsVisible, false);
    }

    private void animateBar(View bar, boolean show, boolean isTop) {
        if (bar == null) return;
        if (show) {
            bar.setVisibility(View.VISIBLE);
            bar.animate().translationY(0).alpha(1f).setDuration(220)
               .setInterpolator(new AccelerateDecelerateInterpolator()).start();
        } else {
            float to = isTop ? -bar.getHeight() : bar.getHeight();
            bar.animate().translationY(to).alpha(0f).setDuration(220)
               .setInterpolator(new AccelerateDecelerateInterpolator())
               .withEndAction(() -> bar.setVisibility(View.GONE)).start();
        }
    }

    // ── Scroll progress ───────────────────────────────────────────────────

    private void setupScrollProgress() {
        if (readerScroll == null || readerSeekbar == null) return;
        sessionStartMs    = System.currentTimeMillis();
        sessionStartPct   = 0.0;
        totalChaptersRead = userPreferences.getTotalChaptersRead();
        totalNovelsRead   = userPreferences.getTotalNovelsRead();
        readerScroll.getViewTreeObserver().addOnScrollChangedListener(() -> {
            if (readerScroll.getChildAt(0) == null) return;
            int scrollY = readerScroll.getScrollY();
            int totalH  = readerScroll.getChildAt(0).getHeight() - readerScroll.getHeight();
            if (totalH <= 0) return;
            int pct = Math.min(100, (scrollY * 100) / totalH);
            readerSeekbar.setProgress(pct);
            if (progressText != null) progressText.setText(pct + "%");
            // Auto-save every 10% milestone to avoid hammering the network on every scroll event
            if (lastSavedProgressPct < 0 || Math.abs(pct - lastSavedProgressPct) >= 10) {
                saveReadingProgress();
                // Fire gamification milestone at each 10% scroll advance
                fireGamificationOnScroll(pct);
            }
        });
        readerSeekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar sb, int p, boolean user) {
                if (!user || readerScroll.getChildAt(0) == null) return;
                int totalH = readerScroll.getChildAt(0).getHeight() - readerScroll.getHeight();
                readerScroll.smoothScrollTo(0, totalH * p / 100);
            }
            @Override public void onStartTrackingTouch(SeekBar sb) {}
            @Override public void onStopTrackingTouch(SeekBar sb)  {}
        });
    }

    // ── Swipe navigation ──────────────────────────────────────────────────

    private void setupSwipeNavigation() {
        if (chapterContent == null) return;
        GestureDetector detector = new GestureDetector(this,
            new GestureDetector.SimpleOnGestureListener() {
                @Override public boolean onFling(MotionEvent e1, MotionEvent e2, float vX, float vY) {
                    if (e1 == null || e2 == null) return false;
                    float dx = e2.getX() - e1.getX();
                    if (Math.abs(dx) > 120 && Math.abs(vX) > 100) {
                        if (dx < 0) goToNextChapter(); else goToPrevChapter();
                        return true;
                    }
                    return false;
                }
            });
        chapterContent.setOnTouchListener((v, ev) -> { detector.onTouchEvent(ev); return false; });
    }

    // ── Chapter navigation ────────────────────────────────────────────────

    private void goToNextChapter() {
        if (allChapters == null || currentIndex >= allChapters.size() - 1) {
            BannerHelper.info(this, "Last chapter reached"); return;
        }
        currentIndex++;
        currentChapterId = allChapters.get(currentIndex).getId();
        updateChapterLabel();
        loadChapterWithBiometricCheck(currentChapterId);
    }

    private void goToPrevChapter() {
        if (allChapters == null || currentIndex <= 0) {
            BannerHelper.info(this, "Already at first chapter"); return;
        }
        currentIndex--;
        currentChapterId = allChapters.get(currentIndex).getId();
        updateChapterLabel();
        loadChapterWithBiometricCheck(currentChapterId);
    }

    // ── Chapter list bottom sheet ─────────────────────────────────────────

    private void showChapterListSheet() {
        if (allChapters == null || allChapters.isEmpty()) return;
        BottomSheetDialog sheet = new BottomSheetDialog(this, R.style.BottomSheetTheme);
        View root = getLayoutInflater().inflate(R.layout.bottom_sheet_chapter_list, null);
        final boolean[] rev = {chaptersReversed};
        android.widget.Button sortBtn = root.findViewById(R.id.btn_chapter_sort_toggle);
        sortBtn.setText(rev[0] ? "↑ Newest" : "↓ Oldest");
        RecyclerView rv = root.findViewById(R.id.chapter_list_rv);
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter(buildChapterAdapter(sheet, rev[0]));
        sortBtn.setOnClickListener(v -> {
            rev[0] = !rev[0]; chaptersReversed = rev[0];
            sortBtn.setText(rev[0] ? "↑ Newest" : "↓ Oldest");
            rv.setAdapter(buildChapterAdapter(sheet, rev[0]));
        });
        rv.scrollToPosition(currentIndex);
        sheet.setContentView(root);
        sheet.show();
    }

    private RecyclerView.Adapter<RecyclerView.ViewHolder> buildChapterAdapter(BottomSheetDialog sheet, boolean rev) {
        List<Chapter> display;
        if (rev) {
            display = new java.util.ArrayList<>(allChapters);
            java.util.Collections.reverse(display);
        } else { display = allChapters; }

        return new RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            @Override public RecyclerView.ViewHolder onCreateViewHolder(android.view.ViewGroup p, int t) {
                LinearLayout row = new LinearLayout(p.getContext());
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setGravity(Gravity.CENTER_VERTICAL);
                row.setPadding(dp(20), dp(14), dp(20), dp(14));
                row.setBackground(getDrawable(android.R.color.transparent));
                row.setClickable(true); row.setFocusable(true);
                TextView num = new TextView(p.getContext()); num.setTag("num");
                num.setTextSize(13f); num.setMinWidth(dp(32)); row.addView(num);
                TextView title = new TextView(p.getContext()); title.setTag("title");
                title.setTextSize(15f); title.setMaxLines(1);
                title.setEllipsize(android.text.TextUtils.TruncateAt.END);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
                lp.leftMargin = dp(8); title.setLayoutParams(lp); row.addView(title);
                View dot = new View(p.getContext()); dot.setTag("dot");
                android.graphics.drawable.GradientDrawable dotBg = new android.graphics.drawable.GradientDrawable();
                dotBg.setShape(android.graphics.drawable.GradientDrawable.OVAL);
                dotBg.setColor(ContextCompat.getColor(ReaderActivity.this, R.color.reader_accent));
                LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(dp(7), dp(7));
                dlp.leftMargin = dp(8); dlp.gravity = Gravity.CENTER_VERTICAL; dot.setLayoutParams(dlp); row.addView(dot);
                return new RecyclerView.ViewHolder(row) {};
            }
            @Override public void onBindViewHolder(RecyclerView.ViewHolder h, int pos) {
                Chapter ch = display.get(pos);
                LinearLayout row = (LinearLayout) h.itemView;
                int realIdx = rev ? (allChapters.size() - 1 - pos) : pos;
                boolean cur = (realIdx == currentIndex);
                int accent = ContextCompat.getColor(ReaderActivity.this, R.color.reader_accent);
                int muted   = resolveAttrColor(com.google.android.material.R.attr.colorOnSurfaceVariant);
                int primary = resolveAttrColor(com.google.android.material.R.attr.colorOnSurface);
                TextView num   = row.findViewWithTag("num");
                TextView title = row.findViewWithTag("title");
                View     dot   = row.findViewWithTag("dot");
                num.setText(String.valueOf(realIdx + 1));
                num.setTextColor(cur ? accent : muted);
                title.setText(ch.getTitle() != null ? ch.getTitle() : "Chapter " + (realIdx + 1));
                title.setTextColor(cur ? accent : primary);
                title.setTypeface(null, cur ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
                dot.setVisibility(cur ? View.VISIBLE : View.GONE);
                row.setOnClickListener(v -> {
                    sheet.dismiss();
                    currentIndex = realIdx; currentChapterId = ch.getId();
                    updateChapterLabel(); loadChapterWithBiometricCheck(ch.getId());
                });
            }
            @Override public int getItemCount() { return display.size(); }
        };
    }

    // ── Auto-scroll ───────────────────────────────────────────────────────

    private void startAutoScroll(int speed) {
        int delay = Math.max(50, 1100 - (speed * 100));
        autoScrollRunnable = new Runnable() {
            @Override public void run() {
                if (readerScroll != null) readerScroll.smoothScrollBy(0, 3);
                autoScrollHandler.postDelayed(this, delay);
            }
        };
        autoScrollHandler.post(autoScrollRunnable);
    }

    private void stopAutoScroll() {
        if (autoScrollRunnable != null) autoScrollHandler.removeCallbacks(autoScrollRunnable);
    }

    // ── Reader settings bottom sheet ──────────────────────────────────────

    private int activeTab = 0;
    private View[] panels;
    private View[] tabViews;
    private ImageView[] tabIcons;
    private View[] tabDots;

    private void openReaderSettings() {
        BottomSheetDialog sheet = new BottomSheetDialog(this, R.style.BottomSheetTheme);
        View root = getLayoutInflater().inflate(R.layout.bottom_sheet_reader_settings, null);

        // Preview text
        TextView preview = root.findViewById(R.id.settings_preview_text);

        // Panels
        panels = new View[]{
            root.findViewById(R.id.panel_text),
            root.findViewById(R.id.panel_display),
            root.findViewById(R.id.panel_layout),
            root.findViewById(R.id.panel_automation),
            root.findViewById(R.id.panel_access)
        };

        // Tab containers, icons, dots
        tabViews = new View[]{
            root.findViewById(R.id.tab_text),
            root.findViewById(R.id.tab_display),
            root.findViewById(R.id.tab_layout_btn),
            root.findViewById(R.id.tab_automation),
            root.findViewById(R.id.tab_access)
        };
        tabIcons = new ImageView[]{
            root.findViewById(R.id.tab_icon_text),
            root.findViewById(R.id.tab_icon_display),
            root.findViewById(R.id.tab_icon_layout),
            root.findViewById(R.id.tab_icon_automation),
            root.findViewById(R.id.tab_icon_access)
        };
        tabDots = new View[]{
            root.findViewById(R.id.tab_dot_text),
            root.findViewById(R.id.tab_dot_display),
            root.findViewById(R.id.tab_dot_layout),
            root.findViewById(R.id.tab_dot_automation),
            root.findViewById(R.id.tab_dot_access)
        };
        int[] tabColors = {
            ContextCompat.getColor(this, R.color.reader_settings_tab_text),
            ContextCompat.getColor(this, R.color.reader_settings_tab_display),
            ContextCompat.getColor(this, R.color.reader_settings_tab_layout),
            ContextCompat.getColor(this, R.color.reader_settings_tab_automation),
            ContextCompat.getColor(this, R.color.reader_settings_tab_access)
        };

        selectTab(activeTab, tabColors);

        for (int i = 0; i < tabViews.length; i++) {
            final int idx = i;
            final int[] colors = tabColors;
            tabViews[i].setOnClickListener(v -> {
                activeTab = idx;
                selectTab(idx, colors);
            });
        }

        // ── PANEL 0: Text ─────────────────────────────────────────────────
        float[] fontSizes    = { 13f, 15f, 17f, 19f, 21f, 23f };
        float[] lineSpacings = { 1.2f, 1.4f, 1.6f, 1.8f, 2.0f, 2.2f };
        float[] paraSpacings = { 0f, 4f, 8f, 12f, 16f, 20f };
        float[] letterSpacings = { 0f, 0.02f, 0.04f, 0.07f, 0.10f, 0.14f };

        ReaderSliderView fontSlider = root.findViewById(R.id.slider_font_size);
        fontSlider.setMax(fontSizes.length - 1);
        fontSlider.setValue(closestIdx(fontSizes, userPreferences.getFontSize()));
        fontSlider.setFillColor(tabColors[0]);
        fontSlider.setOnProgressChangedListener((p, user) -> {
            if (!user) return;
            userPreferences.setFontSize((int) fontSizes[p]);
            applyReaderPreferences();
            if (preview != null) preview.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, fontSizes[p]);
        });

        ReaderSliderView lineSlider = root.findViewById(R.id.slider_line_spacing);
        lineSlider.setMax(lineSpacings.length - 1);
        lineSlider.setValue(closestIdxF(lineSpacings, userPreferences.getLineSpacing()));
        lineSlider.setFillColor(tabColors[0]);
        lineSlider.setOnProgressChangedListener((p, user) -> {
            if (!user) return;
            userPreferences.setLineSpacing(lineSpacings[p]);
            applyReaderPreferences();
            if (preview != null) preview.setLineSpacing(0f, lineSpacings[p]);
        });

        ReaderSliderView paraSlider = root.findViewById(R.id.slider_paragraph_spacing);
        paraSlider.setMax(paraSpacings.length - 1);
        paraSlider.setValue(closestIdxF(paraSpacings, userPreferences.getParagraphSpacing()));
        paraSlider.setFillColor(tabColors[0]);
        paraSlider.setOnProgressChangedListener((p, user) -> {
            if (!user) return;
            userPreferences.setParagraphSpacing(paraSpacings[p]);
            applyReaderPreferences();
        });

        ReaderSliderView letterSlider = root.findViewById(R.id.slider_letter_spacing);
        letterSlider.setMax(letterSpacings.length - 1);
        letterSlider.setValue(closestIdxF(letterSpacings, userPreferences.getLetterSpacing()));
        letterSlider.setFillColor(tabColors[0]);
        letterSlider.setOnProgressChangedListener((p, user) -> {
            if (!user) return;
            userPreferences.setLetterSpacing(letterSpacings[p]);
            applyReaderPreferences();
            if (preview != null) preview.setLetterSpacing(letterSpacings[p]);
        });

        // Font family chips
        LinearLayout chipGroup = root.findViewById(R.id.font_chip_group);
        buildFontChips(chipGroup, preview, tabColors[0]);

        // ── PANEL 1: Display ──────────────────────────────────────────────
        int[] themeIds   = { R.id.theme_btn_light, R.id.theme_btn_dark, R.id.theme_btn_sepia, R.id.theme_btn_amoled };
        String[] themes  = { "light", "dark", "sepia", "amoled" };
        String curTheme  = userPreferences.getReaderTheme();
        for (int i = 0; i < themeIds.length; i++) {
            final String t = themes[i];
            View btn = root.findViewById(themeIds[i]);
            if (btn != null) {
                highlightThemeBtn(btn, t.equals(curTheme));
                btn.setOnClickListener(v -> {
                    userPreferences.setReaderTheme(t);
                    for (int j = 0; j < themeIds.length; j++)
                        highlightThemeBtn(root.findViewById(themeIds[j]), themes[j].equals(t));
                    applyReaderPreferences();
                    sheet.dismiss();
                });
            }
        }

        // Brightness slider
        ReaderSliderView brightSlider = root.findViewById(R.id.slider_brightness);
        brightSlider.setMax(100);
        float curBright = userPreferences.getScreenBrightness();
        brightSlider.setValue(curBright < 0 ? 70 : (int)(curBright * 100));
        brightSlider.setFillColor(tabColors[1]);
        brightSlider.setOnProgressChangedListener((p, user) -> {
            if (!user) return;
            float b = p / 100f;
            userPreferences.setScreenBrightness(b);
            userPreferences.setFollowSystemBrightness(false);
            applyBrightness();
            // Update "Auto" button appearance
            TextView autoBtn = root.findViewById(R.id.btn_follow_brightness);
            if (autoBtn != null) {
                autoBtn.setText("Auto");
                autoBtn.setTextColor(getColor(R.color.text_tertiary));
                autoBtn.setBackground(getDrawable(R.drawable.bg_font_chip));
            }
        });

        // Follow system brightness toggle
        TextView followBtn = root.findViewById(R.id.btn_follow_brightness);
        boolean isAuto = userPreferences.isFollowSystemBrightness();
        updateFollowBrightnessBtn(followBtn, isAuto);
        followBtn.setOnClickListener(v -> {
            boolean newAuto = !userPreferences.isFollowSystemBrightness();
            userPreferences.setFollowSystemBrightness(newAuto);
            updateFollowBrightnessBtn(followBtn, newAuto);
            if (newAuto) brightSlider.setValue(70);
            applyBrightness();
        });

        // Keep screen on
        Switch keepScreenSwitch = root.findViewById(R.id.toggle_keep_screen_on);
        keepScreenSwitch.setChecked(userPreferences.isKeepScreenOn());
        keepScreenSwitch.setOnCheckedChangeListener((v, checked) -> {
            userPreferences.setKeepScreenOn(checked);
            applyWindowPreferences();
        });

        // ── PANEL 2: Layout ───────────────────────────────────────────────
        String curMode = userPreferences.getReadingMode();
        View scrollBtn = root.findViewById(R.id.mode_btn_scroll);
        View pagedBtn  = root.findViewById(R.id.mode_btn_paged);
        refreshModeButtons(scrollBtn, pagedBtn, curMode);
        scrollBtn.setOnClickListener(v -> {
            userPreferences.setReadingMode("scroll");
            refreshModeButtons(scrollBtn, pagedBtn, "scroll");
            applyReaderPreferences();
        });
        pagedBtn.setOnClickListener(v -> {
            userPreferences.setReadingMode("paged");
            refreshModeButtons(scrollBtn, pagedBtn, "paged");
            applyReaderPreferences();
        });

        // Alignment
        TextView alignLeft    = root.findViewById(R.id.btn_align_left);
        TextView alignJustify = root.findViewById(R.id.btn_align_justify);
        refreshAlignmentBtns(alignLeft, alignJustify, userPreferences.getTextAlignment());
        alignLeft.setOnClickListener(v -> {
            userPreferences.setTextAlignment("left");
            refreshAlignmentBtns(alignLeft, alignJustify, "left");
            applyReaderPreferences();
        });
        alignJustify.setOnClickListener(v -> {
            userPreferences.setTextAlignment("justify");
            refreshAlignmentBtns(alignLeft, alignJustify, "justify");
            applyReaderPreferences();
        });

        // Side margin slider
        int[] marginDps = { 0, 4, 8, 16, 24, 36 };
        ReaderSliderView marginSlider = root.findViewById(R.id.slider_side_margin);
        marginSlider.setMax(marginDps.length - 1);
        marginSlider.setValue(closestIntIdx(marginDps, userPreferences.getSideMargin()));
        marginSlider.setFillColor(tabColors[2]);
        marginSlider.setOnProgressChangedListener((p, user) -> {
            if (!user) return;
            userPreferences.setSideMargin(marginDps[p]);
            applyReaderPreferences();
        });

        // ── PANEL 3: Automation ───────────────────────────────────────────
        Switch autoScrollSwitch = root.findViewById(R.id.toggle_auto_scroll);
        autoScrollSwitch.setChecked(userPreferences.isAutoScrollEnabled());
        View speedContainer = root.findViewById(R.id.auto_scroll_speed_container);
        speedContainer.setVisibility(userPreferences.isAutoScrollEnabled() ? View.VISIBLE : View.GONE);
        autoScrollSwitch.setOnCheckedChangeListener((v, checked) -> {
            userPreferences.setAutoScrollEnabled(checked);
            speedContainer.setVisibility(checked ? View.VISIBLE : View.GONE);
            if (checked) startAutoScroll(userPreferences.getAutoScrollSpeed());
            else          stopAutoScroll();
        });

        int[] speeds = { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 };
        ReaderSliderView speedSlider = root.findViewById(R.id.slider_auto_scroll_speed);
        speedSlider.setMax(speeds.length - 1);
        speedSlider.setValue(userPreferences.getAutoScrollSpeed() - 1);
        speedSlider.setFillColor(tabColors[3]);
        speedSlider.setOnProgressChangedListener((p, user) -> {
            if (!user) return;
            userPreferences.setAutoScrollSpeed(speeds[p]);
            stopAutoScroll();
            if (userPreferences.isAutoScrollEnabled()) startAutoScroll(speeds[p]);
        });

        Switch volFlipSwitch = root.findViewById(R.id.toggle_volume_flip);
        volFlipSwitch.setChecked(userPreferences.isVolumeFlipEnabled());
        volFlipSwitch.setOnCheckedChangeListener((v, checked) -> userPreferences.setVolumeFlipEnabled(checked));

        Switch tapZoneSwitch = root.findViewById(R.id.toggle_tap_zone);
        tapZoneSwitch.setChecked(userPreferences.isTapZoneNavEnabled());
        tapZoneSwitch.setOnCheckedChangeListener((v, checked) -> userPreferences.setTapZoneNavEnabled(checked));

        // ── PANEL 4: Accessibility ────────────────────────────────────────
        Switch dyslexiaSwitch = root.findViewById(R.id.toggle_dyslexia);
        dyslexiaSwitch.setChecked(userPreferences.isDyslexiaFontEnabled());
        dyslexiaSwitch.setOnCheckedChangeListener((v, checked) -> {
            userPreferences.setDyslexiaFontEnabled(checked);
            applyReaderPreferences();
        });

        Switch bioSwitch = root.findViewById(R.id.toggle_biometric);
        bioSwitch.setChecked(userPreferences.isBiometricLockEnabled());
        bioSwitch.setOnCheckedChangeListener((v, checked) -> userPreferences.setBiometricLockEnabled(checked));

        sheet.setContentView(root);
        sheet.show();
    }

    // ── Settings helpers ──────────────────────────────────────────────────

    private void selectTab(int idx, int[] colors) {
        for (int i = 0; i < panels.length; i++) {
            panels[i].setVisibility(i == idx ? View.VISIBLE : View.GONE);
            tabDots[i].setVisibility(i == idx ? View.VISIBLE : View.INVISIBLE);
            if (i == idx) {
                tabIcons[i].setColorFilter(colors[i]);
                // Tint dot with tab color
                android.graphics.drawable.GradientDrawable dotBg = new android.graphics.drawable.GradientDrawable();
                dotBg.setShape(android.graphics.drawable.GradientDrawable.OVAL);
                dotBg.setColor(colors[i]);
                tabDots[i].setBackground(dotBg);
            } else {
                tabIcons[i].clearColorFilter();
                tabIcons[i].setColorFilter(ContextCompat.getColor(this, R.color.reader_icon_inactive));
            }
        }
    }

    private void buildFontChips(LinearLayout group, TextView preview, int activeColor) {
        if (group == null) return;
        String curFont = userPreferences.isDyslexiaFontEnabled() ? "opendyslexic" : userPreferences.getFontFamily();
        group.removeAllViews();
        for (int i = 0; i < FONT_KEYS.length; i++) {
            final String key   = FONT_KEYS[i];
            final String label = FONT_LABELS[i];
            final int    resId = FONT_RES[i];

            TextView chip = new TextView(this);
            chip.setText(label);
            chip.setTextSize(13f);
            chip.setPadding(dp(14), dp(8), dp(14), dp(8));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.rightMargin = dp(8);
            chip.setLayoutParams(lp);

            boolean active = key.equals(curFont);
            chip.setBackground(getDrawable(active ? R.drawable.bg_font_chip_active : R.drawable.bg_font_chip));
            // Use colorPrimary from theme — light mode = #0085FF, dark mode = #5EB3FF
            int primaryColor = resolveAttrColor(com.google.android.material.R.attr.colorPrimary);
            chip.setTextColor(active ? primaryColor : resolveAttrColor(com.google.android.material.R.attr.colorOnSurface));

            try {
                android.graphics.Typeface tf = ResourcesCompat.getFont(this, resId);
                if (tf != null) chip.setTypeface(tf);
            } catch (Exception ignored) {}

            chip.setOnClickListener(v -> {
                if ("opendyslexic".equals(key)) {
                    userPreferences.setDyslexiaFontEnabled(true);
                } else {
                    userPreferences.setDyslexiaFontEnabled(false);
                    userPreferences.setFontFamily(key);
                }
                applyReaderPreferences();
                // Refresh all chips
                buildFontChips(group, preview, activeColor);
                // Update preview
                if (preview != null) {
                    try {
                        android.graphics.Typeface tf = ResourcesCompat.getFont(this, resId);
                        if (tf != null) preview.setTypeface(tf);
                    } catch (Exception ignored) {}
                }
            });
            group.addView(chip);
        }
    }

    private void refreshAlignmentBtns(TextView left, TextView justify, String current) {
        boolean isLeft = "left".equals(current);
        left.setBackground(getDrawable(isLeft ? R.drawable.bg_mode_btn_active : R.drawable.bg_mode_btn));
        justify.setBackground(getDrawable(!isLeft ? R.drawable.bg_mode_btn_active : R.drawable.bg_mode_btn));
        left.setTextColor(isLeft ? resolveAttrColor(com.google.android.material.R.attr.colorPrimary) : resolveAttrColor(com.google.android.material.R.attr.colorOnSurface));
        justify.setTextColor(!isLeft ? resolveAttrColor(com.google.android.material.R.attr.colorPrimary) : resolveAttrColor(com.google.android.material.R.attr.colorOnSurface));
    }

    private void refreshModeButtons(View scroll, View paged, String current) {
        boolean isScroll = "scroll".equals(current);
        scroll.setBackground(getDrawable(isScroll ? R.drawable.bg_mode_btn_active : R.drawable.bg_mode_btn));
        paged.setBackground(getDrawable(!isScroll ? R.drawable.bg_mode_btn_active : R.drawable.bg_mode_btn));
    }

    private void highlightThemeBtn(View btn, boolean active) {
        if (btn == null) return;
        btn.setAlpha(active ? 1f : 0.55f);
        btn.setScaleX(active ? 1.05f : 1f);
        btn.setScaleY(active ? 1.05f : 1f);
    }

    private void updateFollowBrightnessBtn(TextView btn, boolean isAuto) {
        if (btn == null) return;
        if (isAuto) {
            btn.setTextColor(ContextCompat.getColor(this, R.color.reader_accent));
            btn.setBackground(getDrawable(R.drawable.bg_font_chip_active));
            btn.setText("Auto ✓");
        } else {
            btn.setTextColor(resolveAttrColor(com.google.android.material.R.attr.colorOnSurfaceVariant));
            btn.setBackground(getDrawable(R.drawable.bg_font_chip));
            btn.setText("Auto");
        }
    }

    // ── Annotation / custom text selection ──────────────────────────────

    private void setupAnnotation() {
        if (chapterContent == null) return;
        chapterContent.setCustomSelectionActionModeCallback(new ActionMode.Callback() {
            @Override
            public boolean onCreateActionMode(ActionMode mode, android.view.Menu menu) {
                chapterContent.performHapticFeedback(HapticFeedbackConstants.TEXT_HANDLE_MOVE);
                showTextSelectionPopup(mode);
                return true;
            }
            @Override
            public boolean onPrepareActionMode(ActionMode mode, android.view.Menu menu) {
                menu.clear();
                return true;
            }
            @Override
            public boolean onActionItemClicked(ActionMode mode, android.view.MenuItem item) {
                return false;
            }
            @Override
            public void onDestroyActionMode(ActionMode mode) {
                dismissTextSelectionPopup();
            }
        });
    }

    private void showTextSelectionPopup(ActionMode mode) {
        int start = chapterContent.getSelectionStart();
        int end   = chapterContent.getSelectionEnd();
        if (start < 0 || end <= start) return;

        View popupView = getLayoutInflater().inflate(R.layout.popup_text_selection, null);
        View commentBtn = popupView.findViewById(R.id.btn_action_comment);
        View shareBtn   = popupView.findViewById(R.id.btn_action_share);

        commentBtn.setOnClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            EditText input = new EditText(ReaderActivity.this);
            input.setHint("Write your note…");
            input.setPadding(dp(16), dp(12), dp(16), dp(12));
            new AlertDialog.Builder(ReaderActivity.this)
                .setTitle("Add Note")
                .setView(input)
                .setPositiveButton("Save", (d, w) -> {
                    if (!input.getText().toString().trim().isEmpty())
                        BannerHelper.success(ReaderActivity.this, "Note saved");
                })
                .setNegativeButton("Cancel", null)
                .show();
            dismissTextSelectionPopup();
            if (mode != null) mode.finish();
        });

        shareBtn.setOnClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            int safeEnd = Math.min(end, chapterContent.getText().length());
            String selected = chapterContent.getText().toString().substring(start, safeEnd);
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_TEXT, selected);
            startActivity(Intent.createChooser(shareIntent, "Share passage"));
            dismissTextSelectionPopup();
            if (mode != null) mode.finish();
        });

        selectionPopup = new PopupWindow(
            popupView,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            false
        );
        selectionPopup.setElevation(12f);
        selectionPopup.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        selectionPopup.setOutsideTouchable(true);

        android.text.Layout textLayout = chapterContent.getLayout();
        if (textLayout == null) return;

        // Measure popup so we know its dimensions for positioning
        popupView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        int popupW = popupView.getMeasuredWidth();
        int popupH = popupView.getMeasuredHeight();

        // Selection start line → position popup centered above that line
        int selLine = textLayout.getLineForOffset(start);
        int lineTop  = textLayout.getLineTop(selLine);

        int[] loc = new int[2];
        chapterContent.getLocationInWindow(loc);

        // Center the popup horizontally over the selection start
        int selX = (int) textLayout.getPrimaryHorizontal(start);
        int rawX = loc[0] + selX - (popupW / 2);

        // Place popup so its tail touches the top of the selection line
        // popup bottom (including 8dp tail) = lineTop → y = lineTop - popupH
        int rawY = loc[1] + lineTop - popupH - dp(4);

        // If that would go off-screen top, flip: show below the selection end line
        if (rawY < getResources().getDisplayMetrics().heightPixels / 8) {
            int endLine    = textLayout.getLineForOffset(end);
            int lineBottom = textLayout.getLineBottom(endLine);
            rawY = loc[1] + lineBottom + dp(6);
        }

        // Clamp X so popup never clips outside screen edges
        int screenW = getResources().getDisplayMetrics().widthPixels;
        rawX = Math.max(dp(8), Math.min(rawX, screenW - popupW - dp(8)));

        selectionPopup.showAtLocation(chapterContent, Gravity.NO_GRAVITY, rawX, rawY);
    }

    private void dismissTextSelectionPopup() {
        if (selectionPopup != null && selectionPopup.isShowing()) selectionPopup.dismiss();
        selectionPopup = null;
    }

    // ── Misc ──────────────────────────────────────────────────────────────

    private void applyScreenshotLock(Chapter chapter) {
        if (chapter.isPremium() && chapter.isUnlocked())
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
        else
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_SECURE);
    }

    private void openTtsPlayer() {
        String text = (chapterContent != null && chapterContent.getText() != null)
            ? chapterContent.getText().toString() : "";
        String novelTitle = (readerNovelTitle != null && readerNovelTitle.getText() != null)
            ? readerNovelTitle.getText().toString() : "";

        Intent intent = new Intent(this, TtsPlayerActivity.class);
        intent.putExtra(TtsPlayerActivity.EXTRA_CHAPTER_TEXT,  text);
        intent.putExtra(TtsPlayerActivity.EXTRA_NOVEL_TITLE,   novelTitle);
        intent.putExtra(TtsPlayerActivity.EXTRA_CHAPTER_TITLE,
            chapterTitle != null && chapterTitle.getText() != null
                ? chapterTitle.getText().toString() : "");
        intent.putExtra(TtsPlayerActivity.EXTRA_NOVEL_ID,      novelId);
        intent.putExtra(TtsPlayerActivity.EXTRA_CHAPTER_ID,    currentChapterId);
        if (novelCoverUrl != null) {
            intent.putExtra(TtsPlayerActivity.EXTRA_COVER_URL, novelCoverUrl);
        }
        startActivity(intent);
        // TtsMiniPlayerFragment auto-shows via onPlaying() ServiceListener callback.
    }

    private int resolveAttrColor(int attr) {
        android.util.TypedValue tv = new android.util.TypedValue();
        getTheme().resolveAttribute(attr, tv, true);
        return tv.data;
    }

    private int dp(int v) { return Math.round(v * getResources().getDisplayMetrics().density); }

    private int closestIdx(float[] arr, float val) {
        int best = 0;
        for (int i = 1; i < arr.length; i++)
            if (Math.abs(arr[i] - val) < Math.abs(arr[best] - val)) best = i;
        return best;
    }
    private int closestIdxF(float[] arr, float val) { return closestIdx(arr, val); }
    private int closestIntIdx(int[] arr, int val) {
        int best = 0;
        for (int i = 1; i < arr.length; i++)
            if (Math.abs(arr[i] - val) < Math.abs(arr[best] - val)) best = i;
        return best;
    }
}

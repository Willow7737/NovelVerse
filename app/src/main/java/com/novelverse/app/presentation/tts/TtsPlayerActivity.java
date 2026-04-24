package com.novelverse.app.presentation.tts;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.view.GravityCompat;
import androidx.core.widget.NestedScrollView;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.novelverse.app.R;
import com.novelverse.app.data.repository.ChapterRepository;
import com.novelverse.app.data.repository.NovelRepository;
import com.novelverse.app.domain.models.Chapter;
import com.novelverse.app.domain.models.Novel;
import com.novelverse.app.services.TtsService;
import com.novelverse.app.tts.TtsPreferences;
import com.novelverse.app.tts.TtsVoice;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class TtsPlayerActivity extends AppCompatActivity
        implements TtsService.ServiceListener {

    // ── Intent extras ────────────────────────────────────────────────
    public static final String EXTRA_CHAPTER_TEXT  = "tts_chapter_text";
    public static final String EXTRA_NOVEL_TITLE   = "tts_novel_title";
    public static final String EXTRA_CHAPTER_TITLE = "tts_chapter_title";
    public static final String EXTRA_COVER_URL     = "tts_cover_url";
    public static final String EXTRA_NOVEL_ID      = "tts_novel_id";
    public static final String EXTRA_CHAPTER_ID    = "tts_chapter_id";

    // ── Sentence colours (resolved from resources in onCreate) ────────
    private int colorPast;
    private int colorCurrent;
    private int colorFuture;

    // ── Speed presets ────────────────────────────────────────────────
    private static final float[] SPEED_VALUES = {0.8f, 1.0f, 1.25f, 1.5f, 2.0f};
    private static final int[] SPEED_IDS = {
        R.id.tts_speed_08, R.id.tts_speed_1, R.id.tts_speed_125,
        R.id.tts_speed_15, R.id.tts_speed_2
    };

    // ── Text size presets (sp) ────────────────────────────────────────
    private static final float[] TEXT_SIZE_VALUES = {14f, 16f, 18f, 20f};
    private static final int[] TEXT_SIZE_IDS = {
        R.id.tts_text_size_14, R.id.tts_text_size_16,
        R.id.tts_text_size_18, R.id.tts_text_size_20
    };

    // ── Sleep timer options (minutes; -1 = end of chapter) ───────────
    private static final int[] SLEEP_MINUTES = {0, 15, 30, 45, -1};
    private static final int[] SLEEP_IDS = {
        R.id.tts_sleep_off, R.id.tts_sleep_15, R.id.tts_sleep_30,
        R.id.tts_sleep_45, R.id.tts_sleep_end_chap
    };

    // ── Highlight states ─────────────────────────────────────────────
    private static final int HIGHLIGHT_OFF      = 0;
    private static final int HIGHLIGHT_DIM_ONLY = 1;
    private static final int HIGHLIGHT_FULL     = 2;

    // ── Prefs keys ───────────────────────────────────────────────────
    private static final String PREFS_TTS_UI      = "tts_ui_prefs";
    private static final String KEY_COVER_SHOWN   = "cover_shown";
    private static final String KEY_TEXT_SIZE     = "text_size_sp";
    private static final String KEY_AUTO_SCROLL   = "auto_scroll";
    private static final String KEY_HIGHLIGHT     = "highlight_mode";
    private static final String KEY_REDUCE_MOTION = "reduce_motion";

    // ── Main views ───────────────────────────────────────────────────
    private DrawerLayout drawerLayout;
    private NestedScrollView scrollView;
    private TextView tvChapterText;
    private ImageView imgCover;
    private FrameLayout coverCard;
    private TextView tvNovelTitle;
    private TextView tvChapterTitle;
    private TextView tvVoiceLabel;
    private ImageView voiceAvatar;
    private TextView offlinePill;
    private TextView tvTimeCurrent;
    private TextView tvTimeEnd;
    private SeekBar progressSeekbar;
    private ImageView btnPlayPause;
    private View mainContent;
    private View topBar;
    private View topGradient;

    // ── Drawer views ─────────────────────────────────────────────────
    private ImageView tocCover;
    private TextView tocNovelTitle;
    private TextView tocNovelAuthor;
    private RecyclerView tocChaptersRv;

    // ── State ────────────────────────────────────────────────────────
    private List<String> chunks;
    private int[] chunkStart;
    private int currentChunkIndex = 0;
    private boolean showTimeRemaining = false;
    private int sleepTimerSetting = 0;
    private int highlightMode = HIGHLIGHT_FULL;
    private float currentTextSizeSp = 16f;
    private boolean autoScroll = true;
    private boolean reduceMotion = false;
    private boolean seekbarUserTouching = false;
    private SharedPreferences uiPrefs;

    // ── Sleep timer ──────────────────────────────────────────────────
    private final Handler sleepHandler = new Handler(Looper.getMainLooper());
    private Runnable sleepRunnable = null;

    // ── Service ──────────────────────────────────────────────────────
    private TtsService ttsService;
    private boolean serviceBound = false;

    // ── Repositories ─────────────────────────────────────────────────
    @Inject NovelRepository novelRepository;
    @Inject ChapterRepository chapterRepository;

    // ── Intent data ──────────────────────────────────────────────────
    private String novelId;
    private String currentChapterId;
    private String coverUrlFromIntent;

    // ── ToC data ─────────────────────────────────────────────────────
    private final List<Chapter> allChapters = new ArrayList<>();
    private ChapterTocAdapter tocAdapter;

    private final ServiceConnection conn = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            ttsService = ((TtsService.TtsBinder) service).getService();
            serviceBound = true;
            ttsService.setServiceListener(TtsPlayerActivity.this);

            String text   = getIntent().getStringExtra(EXTRA_CHAPTER_TEXT);
            String nTitle = getIntent().getStringExtra(EXTRA_NOVEL_TITLE);
            String cTitle = getIntent().getStringExtra(EXTRA_CHAPTER_TITLE);
            if (text != null && !text.isEmpty()) {
                ttsService.loadChapter(text, nTitle, cTitle);
            }
            // Persist session metadata so TtsMiniPlayerFragment (in any host activity)
            // can open the full player and load the cover without needing extras.
            ttsService.setSessionMeta(novelId, currentChapterId, coverUrlFromIntent);
            syncFromService();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            serviceBound = false;
            ttsService = null;
        }
    };

    // ── Lifecycle ────────────────────────────────────────────────────

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupEdgeToEdge();
        setContentView(R.layout.activity_tts_player);

        colorPast    = ContextCompat.getColor(this, R.color.tts_sentence_past);
        colorCurrent = ContextCompat.getColor(this, R.color.tts_sentence_current);
        colorFuture  = ContextCompat.getColor(this, R.color.tts_sentence_future);

        uiPrefs = getSharedPreferences(PREFS_TTS_UI, Context.MODE_PRIVATE);
        restoreUiPrefs();

        bindViews();
        wireTopBar();
        wireProgressBar();
        wireBottomBar();
        wireGesturesOnText();
        applyTextSize(currentTextSizeSp);
        adjustLayoutForStatusBar();

        novelId = getIntent().getStringExtra(EXTRA_NOVEL_ID);
        currentChapterId = getIntent().getStringExtra(EXTRA_CHAPTER_ID);
        coverUrlFromIntent = getIntent().getStringExtra(EXTRA_COVER_URL);

        setupDrawer();

        if (coverUrlFromIntent != null && !coverUrlFromIntent.isEmpty()) {
            loadCoverArt(coverUrlFromIntent);
        } else if (novelId != null) {
            fetchNovelCoverFromRepository();
        } else {
            applyCoverVisibility();
        }

        if (novelId != null) {
            loadChaptersForToc();
        }

        Intent serviceIntent = new Intent(this, TtsService.class);
        startService(serviceIntent);
        bindService(serviceIntent, conn, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (serviceBound && ttsService != null) {
            ttsService.setServiceListener(this);
            syncFromService();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (serviceBound && ttsService != null) {
            ttsService.setServiceListener(null);
        }
    }

    @Override
    protected void onDestroy() {
        cancelSleepTimer();
        if (serviceBound) {
            unbindService(conn);
            serviceBound = false;
        }
        super.onDestroy();
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(0, R.anim.slide_down);
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout != null && drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    // ── UI Setup ─────────────────────────────────────────────────────

    private void setupEdgeToEdge() {
        Window window = getWindow();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.setStatusBarColor(Color.TRANSPARENT);
        }
        // Use LAYOUT_FULLSCREEN to draw behind the status bar.
        // We REMOVED SYSTEM_UI_FLAG_FULLSCREEN because it hides the status bar icons entirely.
        window.getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
    }

    private void adjustLayoutForStatusBar() {
        int sbHeight = getStatusBarHeight();
        if (topGradient != null) {
            FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) topGradient.getLayoutParams();
            lp.height = sbHeight + dp(160);
            topGradient.setLayoutParams(lp);
        }
        if (topBar != null) {
            // Let the top bar start at y=0 (behind the status bar area) so that
            // img_top_gradient visually fills the whole top including the status bar.
            // Push the bar's *content* down by the status bar height via padding.
            FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) topBar.getLayoutParams();
            lp.topMargin = 0;
            lp.height = sbHeight + dp(56);
            topBar.setLayoutParams(lp);
            topBar.setPaddingRelative(
                topBar.getPaddingStart(),
                sbHeight,
                topBar.getPaddingEnd(),
                topBar.getPaddingBottom());
        }
        if (mainContent != null) {
            FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) mainContent.getLayoutParams();
            lp.topMargin = sbHeight + dp(56);
            mainContent.setLayoutParams(lp);
        }
    }

    private int getStatusBarHeight() {
        int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        return resourceId > 0 ? getResources().getDimensionPixelSize(resourceId) : dp(24);
    }

    private void bindViews() {
        drawerLayout    = findViewById(R.id.tts_drawer_layout);
        scrollView      = findViewById(R.id.tts_scroll);
        tvChapterText   = findViewById(R.id.tts_chapter_text);
        imgCover        = findViewById(R.id.tts_img_cover);
        coverCard       = findViewById(R.id.tts_cover_card);
        tvNovelTitle    = findViewById(R.id.tts_novel_title);
        tvChapterTitle  = findViewById(R.id.tts_chapter_title);
        tvVoiceLabel    = findViewById(R.id.tts_voice_label);
        voiceAvatar     = findViewById(R.id.tts_voice_avatar);
        offlinePill     = findViewById(R.id.tts_offline_pill);
        tvTimeCurrent   = findViewById(R.id.tts_time_current);
        tvTimeEnd       = findViewById(R.id.tts_time_end);
        progressSeekbar = findViewById(R.id.tts_seekbar);
        btnPlayPause    = findViewById(R.id.tts_btn_play_pause);
        mainContent     = findViewById(R.id.tts_main_content);
        topBar          = findViewById(R.id.tts_top_bar);
        topGradient     = findViewById(R.id.tts_top_gradient);

        tocCover        = findViewById(R.id.tts_toc_cover);
        tocNovelTitle   = findViewById(R.id.tts_toc_novel_title);
        tocNovelAuthor  = findViewById(R.id.tts_toc_novel_author);
        tocChaptersRv   = findViewById(R.id.tts_toc_chapters_rv);
    }

    private void setupDrawer() {
        if (drawerLayout == null) return;

        drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED, GravityCompat.END);
        drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED, GravityCompat.START);

        drawerLayout.addDrawerListener(new DrawerLayout.SimpleDrawerListener() {
            @Override
            public void onDrawerSlide(View drawerView, float slideOffset) {
                if (mainContent != null) {
                    mainContent.setAlpha(1 - slideOffset * 0.3f);
                }
            }
        });

        if (tocNovelTitle != null) {
            tocNovelTitle.setText(getIntent().getStringExtra(EXTRA_NOVEL_TITLE));
        }

        tocChaptersRv.setLayoutManager(new LinearLayoutManager(this));
        tocAdapter = new ChapterTocAdapter();
        tocChaptersRv.setAdapter(tocAdapter);
    }

    private void loadChaptersForToc() {
        chapterRepository.getChapters(novelId).observe(this, chapters -> {
            if (chapters != null) {
                allChapters.clear();
                allChapters.addAll(chapters);
                tocAdapter.setChapters(allChapters, currentChapterId);
            }
        });
    }

    private void fetchNovelCoverFromRepository() {
        if (novelId == null) return;
        novelRepository.getNovelById(novelId).observe(this, novel -> {
            if (novel != null) {
                String coverUrl = novel.getCoverImageUrl();
                if (coverUrl != null && !coverUrl.isEmpty()) {
                    loadCoverArt(coverUrl);
                }
                if (tocNovelAuthor != null) {
                    tocNovelAuthor.setText(novel.getAuthorDisplayName());
                }
            }
            applyCoverVisibility();
        });
    }

    private void loadCoverArt(String url) {
        if (imgCover != null) {
            Glide.with(this).load(url)
                    .placeholder(R.drawable.img_cover_placeholder_default)
                    .error(R.drawable.img_cover_placeholder_default)
                    .centerCrop()
                    .into(imgCover);
        }
        if (tocCover != null) {
            Glide.with(this).load(url)
                    .placeholder(R.drawable.img_cover_placeholder_default)
                    .centerCrop()
                    .into(tocCover);
        }
        applyCoverVisibility();
    }

    private void wireTopBar() {
        View minimize = findViewById(R.id.tts_btn_minimize);
        if (minimize != null) minimize.setOnClickListener(v -> finish());

        View voiceInfoRow = findViewById(R.id.tts_voice_info_row);
        if (voiceInfoRow != null) voiceInfoRow.setOnClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            showVoiceSelector();
        });
    }

    private void applyCoverVisibility() {
        boolean show = uiPrefs.getBoolean(KEY_COVER_SHOWN, true);
        if (coverCard != null) {
            coverCard.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }

    private void wireProgressBar() {
        if (progressSeekbar != null) {
            progressSeekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                    seekbarUserTouching = true;
                }

                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (!fromUser || !serviceBound || ttsService == null) return;
                    int target = (int) (progress / 100f * ttsService.getTotalChunks());
                    target = Math.max(0, Math.min(target, ttsService.getTotalChunks() - 1));
                    if (progress % 10 == 0) {
                        seekBar.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
                    }
                    updateTimeDisplay(target, ttsService.getTotalChunks());
                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                    seekbarUserTouching = false;
                    if (!serviceBound || ttsService == null) return;
                    int target = (int) (seekBar.getProgress() / 100f * ttsService.getTotalChunks());
                    target = Math.max(0, Math.min(target, ttsService.getTotalChunks() - 1));
                    ttsService.seekToChunk(target);
                }
            });
        }

        if (tvTimeEnd != null) {
            tvTimeEnd.setOnClickListener(v -> {
                v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                showTimeRemaining = !showTimeRemaining;
                if (serviceBound && ttsService != null) {
                    updateTimeDisplay(ttsService.getCurrentChunkIndex(), ttsService.getTotalChunks());
                }
            });
        }
    }

    private void updateTimeDisplay(int currentIdx, int total) {
        if (chunks == null || chunks.isEmpty()) {
            if (tvTimeCurrent != null) tvTimeCurrent.setText("0:00");
            if (tvTimeEnd != null) tvTimeEnd.setText("0:00");
            return;
        }
        float speed = (serviceBound && ttsService != null) ? ttsService.getSpeed() : 1f;
        float wpm = 140f * speed;

        int wordsUpToCurrent = 0;
        int totalWords = 0;
        for (int i = 0; i < chunks.size(); i++) {
            int w = countWords(chunks.get(i));
            if (i < currentIdx) wordsUpToCurrent += w;
            totalWords += w;
        }

        int currentSec = (int) (wordsUpToCurrent / wpm * 60);
        int totalSec = (int) (totalWords / wpm * 60);
        int remainSec = totalSec - currentSec;

        if (tvTimeCurrent != null) tvTimeCurrent.setText(formatTime(currentSec));
        if (tvTimeEnd != null) {
            tvTimeEnd.setText(showTimeRemaining ? "-" + formatTime(remainSec) : formatTime(totalSec));
        }
    }

    private static int countWords(String s) {
        if (s == null || s.trim().isEmpty()) return 0;
        return s.trim().split("\\s+").length;
    }

    private static String formatTime(int totalSec) {
        int m = totalSec / 60;
        int s = totalSec % 60;
        return String.format(Locale.US, "%d:%02d", m, s);
    }

    private void wireBottomBar() {
        if (btnPlayPause != null) {
            btnPlayPause.setOnClickListener(v -> {
                v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                animatePlayButton(v);
                if (serviceBound && ttsService != null) ttsService.togglePlayPause();
            });
        }

        View btnPrev = findViewById(R.id.tts_btn_prev);
        if (btnPrev != null) {
            btnPrev.setOnClickListener(v -> {
                v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                if (serviceBound && ttsService != null) ttsService.prevChunk();
            });
            btnPrev.setOnLongClickListener(v -> {
                v.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                seekRelativeChunks(-1);
                return true;
            });
        }

        View btnNext = findViewById(R.id.tts_btn_next);
        if (btnNext != null) {
            btnNext.setOnClickListener(v -> {
                v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                if (serviceBound && ttsService != null) ttsService.nextChunk();
            });
            btnNext.setOnLongClickListener(v -> {
                v.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                seekRelativeChunks(1);
                return true;
            });
        }

        View btnContents = findViewById(R.id.tts_btn_contents);
        if (btnContents != null) {
            btnContents.setOnClickListener(v -> {
                v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                if (drawerLayout != null) {
                    drawerLayout.openDrawer(GravityCompat.START);
                }
            });
        }

        View btnSettings = findViewById(R.id.tts_btn_settings);
        if (btnSettings != null) {
            btnSettings.setOnClickListener(v -> {
                v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                showSettingsSheet();
            });
        }
    }

    private void animatePlayButton(View v) {
        if (reduceMotion) return;
        v.animate().scaleX(0.88f).scaleY(0.88f).setDuration(75)
                .withEndAction(() -> v.animate().scaleX(1f).scaleY(1f).setDuration(75).start())
                .start();
    }

    private void seekRelativeChunks(int chunkDelta) {
        if (!serviceBound || ttsService == null) return;
        int target = ttsService.getCurrentChunkIndex() + chunkDelta;
        target = Math.max(0, Math.min(target, ttsService.getTotalChunks() - 1));
        ttsService.seekToChunk(target);
    }

    private void wireGesturesOnText() {
        if (scrollView == null) return;

        GestureDetector gd = new GestureDetector(this,
                new GestureDetector.SimpleOnGestureListener() {
                    private static final int SWIPE_MIN_DIST = 80;
                    private static final int SWIPE_MIN_SPEED = 100;

                    @Override
                    public boolean onSingleTapConfirmed(MotionEvent e) {
                        if (serviceBound && ttsService != null) {
                            ttsService.togglePlayPause();
                            if (btnPlayPause != null) animatePlayButton(btnPlayPause);
                        }
                        return true;
                    }

                    @Override
                    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                        if (e1 == null) return false;
                        float dX = e2.getX() - e1.getX();
                        float dY = e2.getY() - e1.getY();

                        if (Math.abs(dX) > Math.abs(dY)) {
                            if (Math.abs(dX) > SWIPE_MIN_DIST && Math.abs(velocityX) > SWIPE_MIN_SPEED) {
                                scrollView.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                                if (dX < 0) {
                                    if (serviceBound && ttsService != null) ttsService.nextChunk();
                                } else {
                                    if (serviceBound && ttsService != null) ttsService.prevChunk();
                                }
                                return true;
                            }
                        } else {
                            if (Math.abs(dY) > SWIPE_MIN_DIST && Math.abs(velocityY) > SWIPE_MIN_SPEED) {
                                scrollView.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
                                float current = (serviceBound && ttsService != null)
                                        ? ttsService.getSpeed()
                                        : new TtsPreferences(TtsPlayerActivity.this).getSpeed();
                                float delta = dY > 0 ? -0.1f : 0.1f;
                                float next = Math.round((current + delta) * 10) / 10f;
                                next = Math.max(0.5f, Math.min(3.0f, next));
                                if (serviceBound && ttsService != null) ttsService.setSpeed(next);
                                else new TtsPreferences(TtsPlayerActivity.this).setSpeed(next);
                                return true;
                            }
                        }
                        return false;
                    }
                });

        scrollView.setOnTouchListener((v, event) -> {
            if (event.getPointerCount() == 2 && event.getActionMasked() == MotionEvent.ACTION_POINTER_DOWN) {
                v.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                cycleSleepTimer();
                return true;
            }
            gd.onTouchEvent(event);
            return false;
        });
    }

    // ── Highlighting ─────────────────────────────────────────────────

    private void buildChunkMap(List<String> chunkList) {
        this.chunks = chunkList;
        this.chunkStart = new int[chunkList.size()];

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < chunkList.size(); i++) {
            chunkStart[i] = sb.length();
            sb.append(chunkList.get(i));
            if (i < chunkList.size() - 1) sb.append(" ");
        }
        if (tvChapterText != null) tvChapterText.setText(sb.toString());
    }

    private void applyHighlight(int idx) {
        if (chunks == null || tvChapterText == null) return;
        this.currentChunkIndex = idx;

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < chunks.size(); i++) {
            if (i > 0) sb.append(" ");
            sb.append(chunks.get(i));
        }
        SpannableStringBuilder ssb = new SpannableStringBuilder(sb);

        for (int i = 0; i < chunks.size(); i++) {
            int start = chunkStart[i];
            int end = start + chunks.get(i).length();
            if (start < 0 || end > ssb.length()) continue;

            if (highlightMode == HIGHLIGHT_OFF) {
                ssb.setSpan(new ForegroundColorSpan(colorFuture), start, end,
                        SpannableStringBuilder.SPAN_EXCLUSIVE_EXCLUSIVE);
            } else if (highlightMode == HIGHLIGHT_DIM_ONLY) {
                int c = (i < idx) ? colorPast : colorFuture;
                ssb.setSpan(new ForegroundColorSpan(c), start, end,
                        SpannableStringBuilder.SPAN_EXCLUSIVE_EXCLUSIVE);
            } else {
                if (i < idx) {
                    ssb.setSpan(new ForegroundColorSpan(colorPast), start, end,
                            SpannableStringBuilder.SPAN_EXCLUSIVE_EXCLUSIVE);
                } else if (i == idx) {
                    ssb.setSpan(new ForegroundColorSpan(colorCurrent), start, end,
                            SpannableStringBuilder.SPAN_EXCLUSIVE_EXCLUSIVE);
                    ssb.setSpan(new RelativeSizeSpan(1.0625f), start, end,
                            SpannableStringBuilder.SPAN_EXCLUSIVE_EXCLUSIVE);
                    ssb.setSpan(new StyleSpan(Typeface.BOLD), start, end,
                            SpannableStringBuilder.SPAN_EXCLUSIVE_EXCLUSIVE);
                } else {
                    ssb.setSpan(new ForegroundColorSpan(colorFuture), start, end,
                            SpannableStringBuilder.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
            }
        }
        tvChapterText.setText(ssb);

        if (autoScroll && idx < chunkStart.length) {
            scrollToCurrent(idx);
        }

        if (!seekbarUserTouching && progressSeekbar != null && chunks.size() > 1) {
            int prog = (int) (idx / (float) (chunks.size() - 1) * 100);
            progressSeekbar.setProgress(prog);
        }
        updateTimeDisplay(idx, chunks.size());
    }

    private void scrollToCurrent(int idx) {
        if (tvChapterText == null || scrollView == null) return;
        tvChapterText.post(() -> {
            try {
                android.text.Layout layout = tvChapterText.getLayout();
                if (layout == null || idx >= chunkStart.length) return;
                int line = layout.getLineForOffset(chunkStart[idx]);
                int lineY = layout.getLineTop(line);
                int target = tvChapterText.getTop() + lineY - dp(80);
                scrollView.smoothScrollTo(0, Math.max(0, target));
            } catch (Exception ignored) {}
        });
    }

    // ── Settings Sheet ───────────────────────────────────────────────

    private void showSettingsSheet() {
        BottomSheetDialog sheet = new BottomSheetDialog(this, R.style.BottomSheetTheme);
        View root = getLayoutInflater().inflate(R.layout.bottom_sheet_tts_settings, null);

        View voiceRow = root.findViewById(R.id.tts_settings_voice_row);
        if (voiceRow != null) {
            voiceRow.setOnClickListener(v -> {
                sheet.dismiss();
                showVoiceSelector();
            });
        }
        updateSettingsVoiceRow(root);

        SeekBar speedSeekbar = root.findViewById(R.id.tts_settings_speed_seekbar);
        TextView speedLabel = root.findViewById(R.id.tts_settings_speed_label);
        float currentSpeed = (serviceBound && ttsService != null)
                ? ttsService.getSpeed()
                : new TtsPreferences(this).getSpeed();

        if (speedSeekbar != null) {
            speedSeekbar.setProgress(speedToProgress(currentSpeed));
            speedSeekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    float s = progressToSpeed(progress);
                    if (speedLabel != null) speedLabel.setText(String.format(Locale.US, "%.2f×", s));
                    if (fromUser) {
                        if (serviceBound && ttsService != null) ttsService.setSpeed(s);
                        else new TtsPreferences(TtsPlayerActivity.this).setSpeed(s);
                        highlightSpeedChips(root, s);
                    }
                }

                @Override public void onStartTrackingTouch(SeekBar seekBar) {}
                @Override public void onStopTrackingTouch(SeekBar seekBar) {}
            });
        }
        if (speedLabel != null) speedLabel.setText(String.format(Locale.US, "%.2f×", currentSpeed));
        highlightSpeedChips(root, currentSpeed);

        for (int i = 0; i < SPEED_IDS.length; i++) {
            View chip = root.findViewById(SPEED_IDS[i]);
            if (chip == null) continue;
            final float s = SPEED_VALUES[i];
            chip.setOnClickListener(v -> {
                v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                if (serviceBound && ttsService != null) ttsService.setSpeed(s);
                else new TtsPreferences(this).setSpeed(s);
                if (speedSeekbar != null) speedSeekbar.setProgress(speedToProgress(s));
                if (speedLabel != null) speedLabel.setText(String.format(Locale.US, "%.2f×", s));
                highlightSpeedChips(root, s);
            });
        }

        for (int i = 0; i < SLEEP_IDS.length; i++) {
            View chip = root.findViewById(SLEEP_IDS[i]);
            if (chip == null) continue;
            final int minutes = SLEEP_MINUTES[i];
            chip.setOnClickListener(v -> {
                v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                setSleepTimer(minutes);
                highlightSleepChips(root);
            });
        }
        highlightSleepChips(root);

        for (int i = 0; i < TEXT_SIZE_IDS.length; i++) {
            View chip = root.findViewById(TEXT_SIZE_IDS[i]);
            if (chip == null) continue;
            final float sp = TEXT_SIZE_VALUES[i];
            chip.setOnClickListener(v -> {
                v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                currentTextSizeSp = sp;
                uiPrefs.edit().putFloat(KEY_TEXT_SIZE, sp).apply();
                applyTextSize(sp);
                highlightTextSizeChips(root);
            });
        }
        highlightTextSizeChips(root);

        SwitchCompat switchAutoScroll = root.findViewById(R.id.tts_toggle_auto_scroll);
        if (switchAutoScroll != null) {
            switchAutoScroll.setChecked(autoScroll);
            View autoScrollRow = root.findViewById(R.id.tts_toggle_auto_scroll_row);
            if (autoScrollRow != null) {
                autoScrollRow.setOnClickListener(v -> {
                    autoScroll = !autoScroll;
                    switchAutoScroll.setChecked(autoScroll);
                    uiPrefs.edit().putBoolean(KEY_AUTO_SCROLL, autoScroll).apply();
                });
            }
        }

        TextView highlightBtn = root.findViewById(R.id.tts_highlight_cycle_btn);
        TextView highlightStateLabel = root.findViewById(R.id.tts_highlight_state_label);
        if (highlightBtn != null) {
            updateHighlightBtn(highlightBtn, highlightStateLabel);
            highlightBtn.setOnClickListener(v -> {
                v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                highlightMode = (highlightMode + 1) % 3;
                uiPrefs.edit().putInt(KEY_HIGHLIGHT, highlightMode).apply();
                updateHighlightBtn(highlightBtn, highlightStateLabel);
                if (chunks != null) applyHighlight(currentChunkIndex);
            });
        }

        SwitchCompat switchReduceMotion = root.findViewById(R.id.tts_toggle_reduce_motion);
        if (switchReduceMotion != null) {
            switchReduceMotion.setChecked(reduceMotion);
            View reduceMotionRow = root.findViewById(R.id.tts_toggle_reduce_motion_row);
            if (reduceMotionRow != null) {
                reduceMotionRow.setOnClickListener(v -> {
                    reduceMotion = !reduceMotion;
                    switchReduceMotion.setChecked(reduceMotion);
                    uiPrefs.edit().putBoolean(KEY_REDUCE_MOTION, reduceMotion).apply();
                });
            }
        }

        sheet.setContentView(root);
        sheet.show();
    }

    private void updateSettingsVoiceRow(View root) {
        TextView nameTv = root.findViewById(R.id.tts_settings_voice_name);
        TextView langTv = root.findViewById(R.id.tts_settings_voice_lang);
        ImageView avatarIv = root.findViewById(R.id.tts_settings_voice_avatar);
        TtsVoice v = (serviceBound && ttsService != null) ? ttsService.getCurrentVoice() : null;
        if (v != null) {
            if (nameTv != null) nameTv.setText(v.getName());
            if (langTv != null) langTv.setText(v.getCharacter() + " · " + v.getTier());
            if (avatarIv != null) {
                int res = getVoiceAvatarRes(v.getId());
                if (res != 0) {
                    avatarIv.clearColorFilter();
                    avatarIv.setPadding(0, 0, 0, 0);
                    Glide.with(this).load(res).centerCrop().into(avatarIv);
                }
            }
        }
    }

    private void updateHighlightBtn(TextView btn, TextView stateLabel) {
        String[] labels = {"Off", "Dim only", "Color + dim"};
        String label = labels[highlightMode];
        if (btn != null) btn.setText(label);
        if (stateLabel != null) stateLabel.setText(label);
        boolean active = (highlightMode != HIGHLIGHT_OFF);
        if (btn != null) {
            btn.setBackground(getDrawable(active ? R.drawable.bg_tts_speed_chip_active : R.drawable.bg_tts_speed_chip));
            btn.setTextColor(active ? ContextCompat.getColor(this, R.color.white) : ContextCompat.getColor(this, R.color.tts_chip_text_inactive));
        }
    }

    private void highlightSpeedChips(View root, float speed) {
        for (int i = 0; i < SPEED_IDS.length; i++) {
            View chip = root.findViewById(SPEED_IDS[i]);
            if (chip == null) continue;
            boolean active = Math.abs(SPEED_VALUES[i] - speed) < 0.01f;
            chip.setBackground(getDrawable(active ? R.drawable.bg_tts_speed_chip_active : R.drawable.bg_tts_speed_chip));
            if (chip instanceof TextView) {
                ((TextView) chip).setTextColor(active ? ContextCompat.getColor(this, R.color.white) : ContextCompat.getColor(this, R.color.tts_chip_text_inactive));
            }
        }
    }

    private void highlightSleepChips(View root) {
        for (int i = 0; i < SLEEP_IDS.length; i++) {
            View chip = root.findViewById(SLEEP_IDS[i]);
            if (chip == null) continue;
            boolean active = (SLEEP_MINUTES[i] == sleepTimerSetting);
            chip.setBackground(getDrawable(active ? R.drawable.bg_tts_speed_chip_active : R.drawable.bg_tts_speed_chip));
            if (chip instanceof TextView) {
                ((TextView) chip).setTextColor(active ? ContextCompat.getColor(this, R.color.white) : ContextCompat.getColor(this, R.color.tts_chip_text_inactive));
            }
        }
    }

    private void highlightTextSizeChips(View root) {
        for (int i = 0; i < TEXT_SIZE_IDS.length; i++) {
            View chip = root.findViewById(TEXT_SIZE_IDS[i]);
            if (chip == null) continue;
            boolean active = Math.abs(TEXT_SIZE_VALUES[i] - currentTextSizeSp) < 0.1f;
            chip.setBackground(getDrawable(active ? R.drawable.bg_tts_speed_chip_active : R.drawable.bg_tts_speed_chip));
            if (chip instanceof TextView) {
                ((TextView) chip).setTextColor(active ? ContextCompat.getColor(this, R.color.white) : ContextCompat.getColor(this, R.color.tts_chip_text_inactive));
            }
        }
    }

    private void applyTextSize(float sp) {
        if (tvChapterText != null) tvChapterText.setTextSize(sp);
    }

    // ── Voice Selector ───────────────────────────────────────────────

    private void showVoiceSelector() {
        BottomSheetDialog sheet = new BottomSheetDialog(this, R.style.BottomSheetTheme);
        View root = getLayoutInflater().inflate(R.layout.bottom_sheet_tts_voice_selector, null);
        RecyclerView rv = root.findViewById(R.id.voice_selector_rv);
        if (rv == null) {
            sheet.dismiss();
            return;
        }
        rv.setLayoutManager(new LinearLayoutManager(this));
        List<TtsVoice> catalog = TtsVoice.getCatalog();
        TtsVoice selected = (ttsService != null) ? ttsService.getCurrentVoice() : null;
        rv.setAdapter(buildVoiceAdapter(sheet, catalog, selected));
        sheet.setContentView(root);
        sheet.show();
    }

    private RecyclerView.Adapter<RecyclerView.ViewHolder> buildVoiceAdapter(
            BottomSheetDialog sheet, List<TtsVoice> voices, TtsVoice current) {
        return new RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            @NonNull
            @Override
            public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                android.widget.LinearLayout row = new android.widget.LinearLayout(parent.getContext());
                row.setOrientation(android.widget.LinearLayout.HORIZONTAL);
                row.setGravity(Gravity.CENTER_VERTICAL);
                row.setPadding(dp(16), dp(11), dp(16), dp(11));
                row.setClickable(true);
                row.setFocusable(true);
                row.setBackground(new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));

                android.widget.FrameLayout af = new android.widget.FrameLayout(parent.getContext());
                android.widget.LinearLayout.LayoutParams afLp =
                        new android.widget.LinearLayout.LayoutParams(dp(46), dp(46));
                afLp.setMarginEnd(dp(12));
                af.setLayoutParams(afLp);
                af.setBackground(androidx.core.content.ContextCompat.getDrawable(
                        parent.getContext(), R.drawable.bg_tts_avatar_squircle));
                af.setClipToOutline(true);
                af.setOutlineProvider(android.view.ViewOutlineProvider.BACKGROUND);
                ImageView avatar = new ImageView(parent.getContext());
                avatar.setTag("avatar");
                avatar.setLayoutParams(new android.widget.FrameLayout.LayoutParams(
                        android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                        android.widget.FrameLayout.LayoutParams.MATCH_PARENT));
                avatar.setScaleType(ImageView.ScaleType.CENTER_CROP);
                af.addView(avatar);
                row.addView(af);

                android.widget.LinearLayout col = new android.widget.LinearLayout(parent.getContext());
                col.setOrientation(android.widget.LinearLayout.VERTICAL);
                col.setLayoutParams(new android.widget.LinearLayout.LayoutParams(
                        0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
                TextView nameV = new TextView(parent.getContext());
                nameV.setTag("name");
                nameV.setTextSize(14f);
                nameV.setTypeface(ResourcesCompat.getFont(parent.getContext(), R.font.inter_semibold));
                nameV.setTextColor(ContextCompat.getColor(TtsPlayerActivity.this, R.color.white));
                col.addView(nameV);
                TextView descV = new TextView(parent.getContext());
                descV.setTag("desc");
                descV.setTextSize(11f);
                descV.setTypeface(ResourcesCompat.getFont(parent.getContext(), R.font.inter_semibold));
                descV.setTextColor(ContextCompat.getColor(TtsPlayerActivity.this, R.color.tts_voice_desc_alpha));
                col.addView(descV);
                row.addView(col);

                TextView tierV = new TextView(parent.getContext());
                tierV.setTag("tier");
                tierV.setTextSize(9f);
                tierV.setTypeface(ResourcesCompat.getFont(parent.getContext(), R.font.inter_semibold));
                tierV.setPadding(dp(7), dp(3), dp(7), dp(3));
                tierV.setBackground(androidx.core.content.ContextCompat.getDrawable(
                        parent.getContext(), R.drawable.bg_tts_speed_chip));
                row.addView(tierV);

                return new RecyclerView.ViewHolder(row) {};
            }

            @Override
            public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
                TtsVoice v = voices.get(position);
                android.widget.LinearLayout row = (android.widget.LinearLayout) holder.itemView;
                ImageView avatar = row.findViewWithTag("avatar");
                TextView name = row.findViewWithTag("name");
                TextView desc = row.findViewWithTag("desc");
                TextView tier = row.findViewWithTag("tier");

                boolean sel = current != null && current.getId().equals(v.getId());
                if (name != null) {
                    name.setText(v.getName() + (sel ? "  ✓" : ""));
                    name.setTextColor(sel ? ContextCompat.getColor(TtsPlayerActivity.this, R.color.tts_sentence_current) : ContextCompat.getColor(TtsPlayerActivity.this, R.color.white));
                }
                if (desc != null) desc.setText(v.getCharacter() + "  ·  " + v.getSizeLabel());

                if (tier != null) {
                    int tierColor = ContextCompat.getColor(TtsPlayerActivity.this, R.color.tts_tier_default);
                    switch (v.getTier()) {
                        case TtsVoice.TIER_PREMIUM:  tierColor = ContextCompat.getColor(TtsPlayerActivity.this, R.color.tts_tier_premium); break;
                        case TtsVoice.TIER_STANDARD: tierColor = ContextCompat.getColor(TtsPlayerActivity.this, R.color.tts_sentence_current); break;
                    }
                    tier.setText(v.getTier());
                    tier.setTextColor(tierColor);
                }

                if (avatar != null) {
                    int res = getVoiceAvatarRes(v.getId());
                    if (res != 0) {
                        avatar.clearColorFilter();
                        avatar.setPadding(0, 0, 0, 0);
                        Glide.with(row.getContext()).load(res).centerCrop().into(avatar);
                    } else {
                        avatar.setImageResource(R.drawable.ic_waveform);
                        avatar.setColorFilter(ContextCompat.getColor(TtsPlayerActivity.this, R.color.tts_avatar_tint));
                        avatar.setPadding(dp(9), dp(9), dp(9), dp(9));
                    }
                }
                row.setOnClickListener(view -> {
                    view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                    if (ttsService != null) ttsService.setVoice(v);
                    sheet.dismiss();
                });
            }

            @Override
            public int getItemCount() {
                return voices.size();
            }
        };
    }

    // ── Sleep Timer ──────────────────────────────────────────────────

    private void cycleSleepTimer() {
        int[] cycle = {0, 15, 30, 45, -1};
        int cur = 0;
        for (int i = 0; i < cycle.length; i++) {
            if (cycle[i] == sleepTimerSetting) {
                cur = i;
                break;
            }
        }
        setSleepTimer(cycle[(cur + 1) % cycle.length]);
    }

    private void setSleepTimer(int minutes) {
        cancelSleepTimer();
        sleepTimerSetting = minutes;
        if (minutes == 0) return;
        if (minutes == -1) return;

        long delayMs = minutes * 60L * 1000L;
        sleepRunnable = () -> {
            if (serviceBound && ttsService != null) ttsService.stopPlayback();
        };
        sleepHandler.postDelayed(sleepRunnable, delayMs);
    }

    private void cancelSleepTimer() {
        if (sleepRunnable != null) {
            sleepHandler.removeCallbacks(sleepRunnable);
            sleepRunnable = null;
        }
    }

    // ── Speed ↔ Progress Conversion ──────────────────────────────────

    private static int speedToProgress(float speed) {
        return Math.round((speed - 0.5f) / 2.5f * 100f);
    }

    private static float progressToSpeed(int progress) {
        float raw = 0.5f + progress / 100f * 2.5f;
        return Math.round(raw * 10f) / 10f;
    }

    // ── Online/Offline Pill ──────────────────────────────────────────

    private void updateOnlineState(boolean online) {
        if (offlinePill == null) return;
        offlinePill.setVisibility(online ? View.GONE : View.VISIBLE);
    }

    // ── ServiceListener Callbacks ────────────────────────────────────

    @Override
    public void onChaptersLoaded(int totalChunks) {
        List<String> list = (ttsService != null) ? ttsService.getChunks() : null;
        if (list != null && !list.isEmpty()) {
            buildChunkMap(list);
            applyHighlight(0);
        }
        if (progressSeekbar != null) progressSeekbar.setProgress(0);
    }

    @Override
    public void onChunkStarted(int chunkIndex, String chunkText) {
        applyHighlight(chunkIndex);
    }

    @Override
    public void onPlaying(int chunkIndex) {
        if (btnPlayPause != null) btnPlayPause.setImageResource(R.drawable.ic_pause);
        applyHighlight(chunkIndex);
    }

    @Override
    public void onPaused(int chunkIndex) {
        if (btnPlayPause != null) btnPlayPause.setImageResource(R.drawable.ic_play);
    }

    @Override
    public void onStopped() {
        if (btnPlayPause != null) btnPlayPause.setImageResource(R.drawable.ic_play);
    }

    @Override
    public void onFinished() {
        if (btnPlayPause != null) btnPlayPause.setImageResource(R.drawable.ic_play);
        if (sleepTimerSetting == -1) {
            cancelSleepTimer();
        }
    }

    @Override
    public void onProgress(int chunkIndex, int totalChunks) {
        if (!seekbarUserTouching && progressSeekbar != null && totalChunks > 1) {
            int prog = (int) (chunkIndex / (float) (totalChunks - 1) * 100);
            progressSeekbar.setProgress(prog);
        }
        updateTimeDisplay(chunkIndex, totalChunks);
    }

    @Override
    public void onVoiceChanged(TtsVoice voice) {
        if (tvVoiceLabel != null && voice != null) {
            tvVoiceLabel.setText(voice.getName() + " / " + voice.getLanguage());
        }
        if (voiceAvatar != null && voice != null) {
            int res = getVoiceAvatarRes(voice.getId());
            if (res != 0) {
                voiceAvatar.clearColorFilter();
                voiceAvatar.setPadding(0, 0, 0, 0);
                Glide.with(this).load(res).centerCrop().into(voiceAvatar);
            }
        }
    }

    @Override
    public void onSpeedChanged(float speed) {
        // handled in settings sheet
    }

    @Override
    public void onModeChanged(String activeMode) {
        updateOnlineState("online".equals(activeMode));
    }

    // ── Prefs & Sync ─────────────────────────────────────────────────

    private void restoreUiPrefs() {
        currentTextSizeSp = uiPrefs.getFloat(KEY_TEXT_SIZE, 16f);
        autoScroll = uiPrefs.getBoolean(KEY_AUTO_SCROLL, true);
        highlightMode = uiPrefs.getInt(KEY_HIGHLIGHT, HIGHLIGHT_FULL);
        reduceMotion = uiPrefs.getBoolean(KEY_REDUCE_MOTION, false);
    }

    private void syncFromService() {
        if (ttsService == null) return;
        if (tvNovelTitle != null) tvNovelTitle.setText(ttsService.getNovelTitle());
        if (tvChapterTitle != null) tvChapterTitle.setText(ttsService.getChapterTitle());
        if (btnPlayPause != null) {
            btnPlayPause.setImageResource(ttsService.isPlaying() ? R.drawable.ic_pause : R.drawable.ic_play);
        }
        onVoiceChanged(ttsService.getCurrentVoice());
        updateOnlineState(ttsService.isInOnlineMode());

        List<String> list = ttsService.getChunks();
        if (list != null && !list.isEmpty()) {
            buildChunkMap(list);
            applyHighlight(ttsService.getCurrentChunkIndex());
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────

    private int getVoiceAvatarRes(String id) {
        if (id == null) return 0;
        switch (id) {
            case "piper-lessac-high":    return R.drawable.img_va_sarah;
            case "piper-ryan-high":      return R.drawable.img_va_michael;
            case "piper-amy-medium":     return R.drawable.img_va_amy;
            case "piper-kristin-medium": return R.drawable.img_va_kristin;
            case "piper-john-medium":    return R.drawable.img_va_john;
            case "coqui-jenny":          return R.drawable.img_va_jenny;
            case "coqui-vctk-p260":      return R.drawable.img_va_david;
            case "coqui-vctk-p267":      return R.drawable.img_va_emma;
            default:                     return 0;
        }
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }

    // ── Table of Contents Adapter ────────────────────────────────────

    private class ChapterTocAdapter extends RecyclerView.Adapter<ChapterTocAdapter.VH> {

        private List<Chapter> chapters = new ArrayList<>();
        private String currentChapterId;

        void setChapters(List<Chapter> list, String currentId) {
            chapters = list != null ? list : new ArrayList<>();
            currentChapterId = currentId;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_toc_chapter, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            Chapter ch = chapters.get(position);
            holder.number.setText(String.valueOf(position + 1));
            holder.title.setText(ch.getTitle() != null ? ch.getTitle() : "Chapter " + (position + 1));
            boolean isCurrent = ch.getId().equals(currentChapterId);
            holder.playingIcon.setVisibility(isCurrent ? View.VISIBLE : View.GONE);
            holder.itemView.setOnClickListener(v -> {
                if (drawerLayout != null) drawerLayout.closeDrawer(GravityCompat.START);
                if (btnPlayPause != null) btnPlayPause.setEnabled(false);

                // Use a one-shot wrapper so we don't leave dangling observers
                androidx.lifecycle.Observer<Chapter>[] wrapper = new androidx.lifecycle.Observer[1];
                wrapper[0] = chapter -> {
                    chapterRepository.getChapterById(ch.getId())
                            .removeObserver(wrapper[0]);

                    if (chapter == null || chapter.getContent() == null) {
                        if (btnPlayPause != null) btnPlayPause.setEnabled(true);
                        return;
                    }

                    currentChapterId = ch.getId();

                    String nTitle = getIntent().getStringExtra(EXTRA_NOVEL_TITLE);
                    String cTitle = chapter.getTitle() != null
                            ? chapter.getTitle() : "Chapter " + (position + 1);

                    if (serviceBound && ttsService != null) {
                        ttsService.loadChapter(chapter.getContent(), nTitle, cTitle);
                        ttsService.play();
                    }

                    setChapters(chapters, ch.getId());
                    if (btnPlayPause != null) btnPlayPause.setEnabled(true);
                };
                chapterRepository.getChapterById(ch.getId())
                        .observe(TtsPlayerActivity.this, wrapper[0]);
            });
        }

        @Override
        public int getItemCount() {
            return chapters.size();
        }

        class VH extends RecyclerView.ViewHolder {
            TextView number, title;
            ImageView playingIcon;

            VH(@NonNull View itemView) {
                super(itemView);
                number = itemView.findViewById(R.id.toc_chapter_number);
                title = itemView.findViewById(R.id.toc_chapter_title);
                playingIcon = itemView.findViewById(R.id.toc_playing_icon);
            }
        }
    }
}
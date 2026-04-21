package com.novelverse.app.presentation.novel.reader;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.os.Bundle;
import android.os.IBinder;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.widget.NestedScrollView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.novelverse.app.R;
import com.novelverse.app.services.TtsService;
import com.novelverse.app.tts.TtsPreferences;
import com.novelverse.app.tts.TtsVoice;
import com.novelverse.app.ui.banner.BannerHelper;

import java.util.List;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * TTS Player Activity — lean, inline sentence-highlighting reader.
 *
 * Past chunks:    dimmed (#44FFFFFF)
 * Current chunk:  vivid blue (#0085FF)
 * Future chunks:  muted (#AAFFFFFF)
 *
 * Controls live in a bottom sheet (not the main view).
 */
@AndroidEntryPoint
public class TtsPlayerActivity extends AppCompatActivity
        implements TtsService.ServiceListener {

    // ── Extras ──────────────────────────────────────────────────────
    public static final String EXTRA_CHAPTER_TEXT  = "tts_chapter_text";
    public static final String EXTRA_NOVEL_TITLE   = "tts_novel_title";
    public static final String EXTRA_CHAPTER_TITLE = "tts_chapter_title";
    public static final String EXTRA_COVER_URL     = "tts_cover_url";

    // ── Sentence colours ─────────────────────────────────────────────
    private static final int COLOR_PAST    = 0x44FFFFFF;   // heavily dimmed
    private static final int COLOR_CURRENT = 0xFF0085FF;   // vivid blue
    private static final int COLOR_FUTURE  = 0xAAFFFFFF;   // muted white

    // ── Speed chips ──────────────────────────────────────────────────
    private static final float[] SPEED_VALUES = {0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f, 3.0f};
    private static final int[]   SPEED_IDS_SHEET = {
        R.id.tts_speed_05, R.id.tts_speed_075, R.id.tts_speed_1,
        R.id.tts_speed_125, R.id.tts_speed_15, R.id.tts_speed_2, R.id.tts_speed_3
    };

    // ── Views (main) ─────────────────────────────────────────────────
    private NestedScrollView scrollView;
    private TextView         tvChapterText;
    private ImageView        imgCover;
    private TextView         tvNovelTitle;
    private TextView         tvChapterTitle;
    private ImageView        btnPlayPause;
    private View             btnPrev;
    private View             btnNext;
    private View             btnOpenControls;

    // ── Chunk state ──────────────────────────────────────────────────
    private List<String> chunks;
    private int[]        chunkStart;   // char offset of each chunk in full text
    private int          currentChunkIndex = 0;

    // ── Service ──────────────────────────────────────────────────────
    private TtsService ttsService;
    private boolean    serviceBound = false;

    private final ServiceConnection conn = new ServiceConnection() {
        @Override public void onServiceConnected(ComponentName n, IBinder b) {
            ttsService   = ((TtsService.TtsBinder) b).getService();
            serviceBound = true;
            ttsService.setServiceListener(TtsPlayerActivity.this);

            String text    = getIntent().getStringExtra(EXTRA_CHAPTER_TEXT);
            String nTitle  = getIntent().getStringExtra(EXTRA_NOVEL_TITLE);
            String cTitle  = getIntent().getStringExtra(EXTRA_CHAPTER_TITLE);
            if (text != null && !text.isEmpty()) {
                ttsService.loadChapter(text, nTitle, cTitle);
            }
            syncFromService();
        }
        @Override public void onServiceDisconnected(ComponentName n) {
            serviceBound = false; ttsService = null;
        }
    };

    // ── Lifecycle ────────────────────────────────────────────────────

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tts_player);

        scrollView      = findViewById(R.id.tts_scroll);
        tvChapterText   = findViewById(R.id.tts_chapter_text);
        imgCover        = findViewById(R.id.tts_img_cover);
        tvNovelTitle    = findViewById(R.id.tts_novel_title);
        tvChapterTitle  = findViewById(R.id.tts_chapter_title);
        btnPlayPause    = findViewById(R.id.tts_btn_play_pause);
        btnPrev         = findViewById(R.id.tts_btn_prev);
        btnNext         = findViewById(R.id.tts_btn_next);
        btnOpenControls = findViewById(R.id.tts_btn_open_controls);

        loadCoverArt();
        wireMainControls();

        Intent si = new Intent(this, TtsService.class);
        startService(si);
        bindService(si, conn, Context.BIND_AUTO_CREATE);
    }

    @Override protected void onResume() {
        super.onResume();
        if (serviceBound && ttsService != null) {
            ttsService.setServiceListener(this);
            syncFromService();
        }
    }

    @Override protected void onPause() {
        super.onPause();
        if (serviceBound && ttsService != null) ttsService.setServiceListener(null);
    }

    @Override protected void onDestroy() {
        if (serviceBound) { unbindService(conn); serviceBound = false; }
        super.onDestroy();
    }

    // ── Setup ────────────────────────────────────────────────────────

    private void loadCoverArt() {
        String url = getIntent().getStringExtra(EXTRA_COVER_URL);
        if (imgCover == null) return;
        if (url != null && !url.isEmpty()) {
            Glide.with(this)
                .load(url)
                .centerCrop()
                .placeholder(R.drawable.img_cover_placeholder_default)
                .error(R.drawable.img_cover_placeholder_default)
                .into(imgCover);
        }
    }

    private void wireMainControls() {
        View back = findViewById(R.id.tts_btn_back);
        if (back != null) back.setOnClickListener(v -> finish());

        if (btnPlayPause != null)
            btnPlayPause.setOnClickListener(v -> {
                v.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY);
                if (serviceBound && ttsService != null) ttsService.togglePlayPause();
            });

        if (btnPrev != null)
            btnPrev.setOnClickListener(v -> {
                v.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY);
                if (serviceBound && ttsService != null) ttsService.prevChunk();
            });

        if (btnNext != null)
            btnNext.setOnClickListener(v -> {
                v.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY);
                if (serviceBound && ttsService != null) ttsService.nextChunk();
            });

        if (btnOpenControls != null)
            btnOpenControls.setOnClickListener(v -> showControlsSheet());
    }

    // ── Sentence highlighting ────────────────────────────────────────

    /**
     * Build chunk offset map and render the full chapter text once,
     * then apply colour spans on each chunk change.
     */
    private void buildChunkMap(List<String> chunkList) {
        this.chunks = chunkList;
        this.chunkStart = new int[chunkList.size()];

        // Reconstruct the display text (paragraphs joined by double newline)
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < chunkList.size(); i++) {
            chunkStart[i] = sb.length();
            sb.append(chunkList.get(i));
            if (i < chunkList.size() - 1) sb.append("\n\n");
        }
        // Set initial text with all-future colour
        SpannableStringBuilder ssb = new SpannableStringBuilder(sb);
        ssb.setSpan(new ForegroundColorSpan(COLOR_FUTURE), 0, ssb.length(), 0);
        if (tvChapterText != null) tvChapterText.setText(ssb);
    }

    private void applyHighlight(int idx) {
        if (chunks == null || chunkStart == null || tvChapterText == null) return;
        currentChunkIndex = idx;

        SpannableStringBuilder ssb = new SpannableStringBuilder(
            tvChapterText.getText());

        int total = chunks.size();

        // Past: 0 → idx-1
        if (idx > 0) {
            int pastEnd = chunkStart[idx] > 0 ? chunkStart[idx] - 2 : chunkStart[idx];
            if (pastEnd > 0 && pastEnd <= ssb.length()) {
                ssb.setSpan(new ForegroundColorSpan(COLOR_PAST), 0, pastEnd, 0);
            }
        }

        // Current chunk
        int cStart = chunkStart[idx];
        int cEnd   = (idx < total - 1)
            ? Math.max(cStart, chunkStart[idx + 1] - 2)
            : ssb.length();
        cEnd = Math.min(cEnd, ssb.length());
        if (cStart < cEnd) {
            ssb.setSpan(new ForegroundColorSpan(COLOR_CURRENT), cStart, cEnd, 0);
        }

        // Future: idx+1 → end
        if (idx < total - 1) {
            int fStart = chunkStart[idx + 1];
            if (fStart < ssb.length()) {
                ssb.setSpan(new ForegroundColorSpan(COLOR_FUTURE), fStart, ssb.length(), 0);
            }
        }

        tvChapterText.setText(ssb);
        scrollToChunk(idx);
    }

    private void scrollToChunk(int idx) {
        if (scrollView == null || tvChapterText == null
                || chunkStart == null || idx >= chunkStart.length) return;
        tvChapterText.post(() -> {
            // Approximate line height * fraction of text
            int textH = tvChapterText.getHeight();
            int total = tvChapterText.getText().length();
            if (total == 0) return;
            int charPos = chunkStart[idx];
            int scrollY = (int) ((charPos / (float) total) * textH) - dp(80);
            scrollView.smoothScrollTo(0, Math.max(0, scrollY));
        });
    }

    // ── ServiceListener ──────────────────────────────────────────────

    @Override
    public void onChaptersLoaded(int totalChunks) {
        if (ttsService == null) return;
        List<String> list = ttsService.getChunks();
        buildChunkMap(list);
    }

    @Override
    public void onChunkStarted(int chunkIndex, String chunkText) {
        applyHighlight(chunkIndex);
    }

    @Override
    public void onPlaying(int chunkIndex) {
        if (btnPlayPause != null)
            btnPlayPause.setImageResource(R.drawable.ic_pause);
        applyHighlight(chunkIndex);
    }

    @Override public void onPaused(int chunkIndex) {
        if (btnPlayPause != null)
            btnPlayPause.setImageResource(R.drawable.ic_play);
    }

    @Override public void onStopped() {
        if (btnPlayPause != null)
            btnPlayPause.setImageResource(R.drawable.ic_play);
    }

    @Override public void onFinished() {
        if (btnPlayPause != null)
            btnPlayPause.setImageResource(R.drawable.ic_play);
        BannerHelper.success(this, "Finished reading chapter");
    }

    @Override public void onProgress(int chunkIndex, int totalChunks) {
        applyHighlight(chunkIndex);
    }

    @Override public void onVoiceChanged(TtsVoice voice) {}
    @Override public void onSpeedChanged(float speed) {}
    @Override public void onModeChanged(String mode) {}

    // ── Controls bottom sheet ────────────────────────────────────────

    private BottomSheetDialog controlsSheet;

    private void showControlsSheet() {
        if (controlsSheet != null && controlsSheet.isShowing()) {
            controlsSheet.dismiss(); return;
        }
        controlsSheet = new BottomSheetDialog(this, R.style.BottomSheetTheme);
        View root = getLayoutInflater().inflate(R.layout.bottom_sheet_tts_controls, null);

        // Wire sheet views
        ImageView sheetPlay = root.findViewById(R.id.tts_sheet_btn_play_pause);
        View      sheetPrev = root.findViewById(R.id.tts_sheet_btn_prev);
        View      sheetNext = root.findViewById(R.id.tts_sheet_btn_next);
        View      sheetStop = root.findViewById(R.id.tts_sheet_btn_stop);
        SeekBar   seekbar   = root.findViewById(R.id.tts_sheet_seekbar);
        TextView  chunkInfo = root.findViewById(R.id.tts_sheet_chunk_info);
        TextView  modeBadge = root.findViewById(R.id.tts_sheet_mode_badge);
        ImageView sheetAvatar = root.findViewById(R.id.tts_sheet_voice_avatar);
        TextView  sheetVoiceName = root.findViewById(R.id.tts_sheet_voice_name);
        View      voiceRow  = root.findViewById(R.id.tts_sheet_btn_voice);
        View      modeAuto  = root.findViewById(R.id.tts_sheet_mode_auto);
        View      modeOnline  = root.findViewById(R.id.tts_sheet_mode_online);
        View      modeOffline = root.findViewById(R.id.tts_sheet_mode_offline);

        // Populate from service
        if (ttsService != null) {
            int cur   = ttsService.getCurrentChunkIndex();
            int total = ttsService.getTotalChunks();
            if (total > 0) {
                seekbar.setMax(total - 1);
                seekbar.setProgress(cur);
                chunkInfo.setText("Paragraph " + (cur + 1) + " of " + total);
            }
            boolean playing = ttsService.isPlaying();
            if (sheetPlay != null)
                sheetPlay.setImageResource(playing ? R.drawable.ic_pause : R.drawable.ic_play);

            TtsVoice voice = ttsService.getCurrentVoice();
            if (voice != null) {
                if (sheetVoiceName != null) sheetVoiceName.setText(voice.getName());
                int res = getVoiceAvatarRes(voice.getId());
                if (sheetAvatar != null && res != 0) {
                    sheetAvatar.clearColorFilter();
                    sheetAvatar.setPadding(0, 0, 0, 0);
                    Glide.with(this).load(res).centerCrop().into(sheetAvatar);
                }
            }
            updateSheetModeBadge(modeBadge, ttsService.resolveActiveMode());
            highlightSheetSpeedChip(root, ttsService.getSpeed());
        }
        updateSheetModeButtons(root);

        // Listeners
        if (sheetPlay != null) sheetPlay.setOnClickListener(v -> {
            v.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY);
            if (serviceBound && ttsService != null) ttsService.togglePlayPause();
            if (ttsService != null) {
                boolean nowPlaying = ttsService.isPlaying();
                sheetPlay.setImageResource(nowPlaying ? R.drawable.ic_pause : R.drawable.ic_play);
                if (btnPlayPause != null)
                    btnPlayPause.setImageResource(nowPlaying ? R.drawable.ic_pause : R.drawable.ic_play);
            }
        });

        if (sheetPrev != null) sheetPrev.setOnClickListener(v -> {
            v.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY);
            if (serviceBound && ttsService != null) ttsService.prevChunk();
        });

        if (sheetNext != null) sheetNext.setOnClickListener(v -> {
            v.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY);
            if (serviceBound && ttsService != null) ttsService.nextChunk();
        });

        if (sheetStop != null) sheetStop.setOnClickListener(v -> {
            v.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY);
            if (serviceBound && ttsService != null) ttsService.stopPlayback();
            controlsSheet.dismiss();
        });

        if (seekbar != null) seekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar sb, int p, boolean user) {
                if (!user || !serviceBound || ttsService == null) return;
                ttsService.seekToChunk(p);
                chunkInfo.setText("Paragraph " + (p + 1) + " of " + ttsService.getTotalChunks());
            }
            @Override public void onStartTrackingTouch(SeekBar sb) {}
            @Override public void onStopTrackingTouch(SeekBar sb) {}
        });

        // Speed chips
        for (int i = 0; i < SPEED_IDS_SHEET.length; i++) {
            View chip = root.findViewById(SPEED_IDS_SHEET[i]);
            if (chip == null) continue;
            final float speed = SPEED_VALUES[i];
            chip.setOnClickListener(v -> {
                v.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY);
                if (ttsService != null) ttsService.setSpeed(speed);
                else new TtsPreferences(this).setSpeed(speed);
                highlightSheetSpeedChip(root, speed);
            });
        }

        // Voice selector
        if (voiceRow != null) voiceRow.setOnClickListener(v -> {
            controlsSheet.dismiss();
            showVoiceSelector();
        });

        // Mode buttons
        if (modeAuto    != null) modeAuto.setOnClickListener(v    -> { setMode("auto");    updateSheetModeButtons(root); });
        if (modeOnline  != null) modeOnline.setOnClickListener(v  -> { setMode("online");  updateSheetModeButtons(root); });
        if (modeOffline != null) modeOffline.setOnClickListener(v -> { setMode("offline"); updateSheetModeButtons(root); });

        controlsSheet.setContentView(root);
        controlsSheet.show();
    }

    private void showVoiceSelector() {
        BottomSheetDialog sheet = new BottomSheetDialog(this, R.style.BottomSheetTheme);
        View root = getLayoutInflater().inflate(R.layout.bottom_sheet_tts_voice_selector, null);
        RecyclerView rv = root.findViewById(R.id.voice_selector_rv);
        if (rv == null) { sheet.dismiss(); return; }
        rv.setLayoutManager(new LinearLayoutManager(this));
        List<TtsVoice> catalog  = TtsVoice.getCatalog();
        TtsVoice selected = (ttsService != null) ? ttsService.getCurrentVoice() : null;
        rv.setAdapter(buildVoiceAdapter(sheet, catalog, selected));
        sheet.setContentView(root);
        sheet.show();
    }

    private RecyclerView.Adapter<RecyclerView.ViewHolder> buildVoiceAdapter(
            BottomSheetDialog sheet, List<TtsVoice> voices, TtsVoice current) {

        return new RecyclerView.Adapter<RecyclerView.ViewHolder>() {

            @Override
            public RecyclerView.ViewHolder onCreateViewHolder(
                    android.view.ViewGroup parent, int type) {

                android.widget.LinearLayout row = new android.widget.LinearLayout(parent.getContext());
                row.setOrientation(android.widget.LinearLayout.HORIZONTAL);
                row.setGravity(android.view.Gravity.CENTER_VERTICAL);
                row.setPadding(dp(16), dp(11), dp(16), dp(11));
                row.setClickable(true);
                row.setFocusable(true);
                row.setBackground(new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));

                // Avatar squircle
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

                // Text
                android.widget.LinearLayout col = new android.widget.LinearLayout(parent.getContext());
                col.setOrientation(android.widget.LinearLayout.VERTICAL);
                col.setLayoutParams(new android.widget.LinearLayout.LayoutParams(
                    0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

                TextView nameV = new TextView(parent.getContext());
                nameV.setTag("name");
                nameV.setTextSize(14f);
                nameV.setTypeface(ResourcesCompat.getFont(parent.getContext(), R.font.inter_semibold));
                nameV.setTextColor(0xFFFFFFFF);
                col.addView(nameV);

                TextView descV = new TextView(parent.getContext());
                descV.setTag("desc");
                descV.setTextSize(11f);
                descV.setTypeface(ResourcesCompat.getFont(parent.getContext(), R.font.inter_semibold));
                descV.setTextColor(0x55FFFFFF);
                col.addView(descV);
                row.addView(col);

                // Tier pill
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
            public void onBindViewHolder(RecyclerView.ViewHolder h, int pos) {
                TtsVoice v = voices.get(pos);
                android.widget.LinearLayout row = (android.widget.LinearLayout) h.itemView;

                ImageView avatar = row.findViewWithTag("avatar");
                TextView  name   = row.findViewWithTag("name");
                TextView  desc   = row.findViewWithTag("desc");
                TextView  tier   = row.findViewWithTag("tier");

                boolean sel = current != null && current.getId().equals(v.getId());
                name.setText(v.getName() + (sel ? "  ✓" : ""));
                name.setTextColor(sel ? 0xFF0085FF : 0xFFFFFFFF);
                desc.setText(v.getCharacter() + "  ·  " + v.getSizeLabel());

                int tierColor = 0xFF64748B;
                switch (v.getTier()) {
                    case TtsVoice.TIER_PREMIUM:  tierColor = 0xFFF59E0B; break;
                    case TtsVoice.TIER_STANDARD: tierColor = 0xFF0085FF; break;
                }
                tier.setText(v.getTier()); tier.setTextColor(tierColor);

                int res = getVoiceAvatarRes(v.getId());
                if (res != 0) {
                    avatar.setColorFilter(null);
                    avatar.setPadding(0, 0, 0, 0);
                    Glide.with(row.getContext()).load(res).centerCrop().into(avatar);
                } else {
                    avatar.setImageResource(R.drawable.ic_volume);
                    avatar.setColorFilter(0x88FFFFFF);
                    avatar.setPadding(dp(9), dp(9), dp(9), dp(9));
                }

                row.setOnClickListener(view -> {
                    view.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY);
                    if (ttsService != null) ttsService.setVoice(v);
                    sheet.dismiss();
                });
            }

            @Override public int getItemCount() { return voices.size(); }
        };
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

    private void highlightSheetSpeedChip(View root, float speed) {
        for (int i = 0; i < SPEED_IDS_SHEET.length; i++) {
            View chip = root.findViewById(SPEED_IDS_SHEET[i]);
            if (chip == null) continue;
            boolean active = Math.abs(SPEED_VALUES[i] - speed) < 0.01f;
            chip.setBackground(getDrawable(active
                ? R.drawable.bg_tts_speed_chip_active
                : R.drawable.bg_tts_speed_chip));
            if (chip instanceof TextView)
                ((TextView) chip).setTextColor(active ? 0xFFFFFFFF : 0xAAFFFFFF);
        }
    }

    private void setMode(String mode) {
        if (ttsService != null) ttsService.setMode(mode);
        else new TtsPreferences(this).setMode(mode);
    }

    private void updateSheetModeBadge(TextView badge, String mode) {
        if (badge == null) return;
        if ("online".equals(mode)) { badge.setText("Online");  badge.setTextColor(0xFF10B981); }
        else                       { badge.setText("Offline"); badge.setTextColor(0xFFF59E0B); }
    }

    private void updateSheetModeButtons(View root) {
        String pref = new TtsPreferences(this).getMode();
        setModeBtn(root.findViewById(R.id.tts_sheet_mode_auto),    "auto".equals(pref));
        setModeBtn(root.findViewById(R.id.tts_sheet_mode_online),  "online".equals(pref));
        setModeBtn(root.findViewById(R.id.tts_sheet_mode_offline), "offline".equals(pref));
    }

    private void setModeBtn(View v, boolean active) {
        if (v == null) return;
        v.setBackground(getDrawable(active
            ? R.drawable.bg_tts_mode_btn_active
            : R.drawable.bg_tts_mode_btn));
        if (v instanceof TextView)
            ((TextView) v).setTextColor(active ? 0xFFFFFFFF : 0x55FFFFFF);
    }

    private void syncFromService() {
        if (ttsService == null) return;
        if (tvNovelTitle   != null) tvNovelTitle.setText(ttsService.getNovelTitle());
        if (tvChapterTitle != null) tvChapterTitle.setText(ttsService.getChapterTitle());
        if (btnPlayPause   != null)
            btnPlayPause.setImageResource(ttsService.isPlaying()
                ? R.drawable.ic_pause : R.drawable.ic_play);

        List<String> list = ttsService.getChunks();
        if (list != null && !list.isEmpty()) {
            buildChunkMap(list);
            applyHighlight(ttsService.getCurrentChunkIndex());
        }
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}

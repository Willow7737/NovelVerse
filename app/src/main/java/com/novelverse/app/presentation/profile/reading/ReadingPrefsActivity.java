package com.novelverse.app.presentation.profile.reading;

import android.os.Bundle;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.novelverse.app.R;
import com.novelverse.app.presentation.auth.AuthViewModel;
import com.novelverse.app.ui.banner.BannerHelper;
import com.novelverse.app.ui.banner.BannerManager;
import com.novelverse.app.ui.banner.BannerConfig;
import com.novelverse.app.ui.banner.BannerType;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * Reading Preferences — font size, line spacing, theme picker.
 *
 * FIXES:
 *  - Added Save button that persists preferences via AuthViewModel
 *  - Dark theme now sets white text on the preview (previously invisible)
 *  - Preferences are loaded from the current user on open
 */
@AndroidEntryPoint
public class ReadingPrefsActivity extends AppCompatActivity {

    private TextView txtPreview;
    private AuthViewModel authViewModel;

    private static final float[] FONT_SIZES    = {14f, 16f, 18f, 20f, 22f};
    private static final float[] LINE_SPACINGS = {1.2f, 1.4f, 1.6f, 1.8f, 2.0f};

    private float   currentFont      = 18f;
    private float   currentSpacing   = 1.6f;
    private String  currentTheme     = "light";
    private int     currentFontIndex = 2;
    private int     currentSpacingIndex = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reading_prefs);

        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        txtPreview = findViewById(R.id.txt_preview);

        // ── Back ─────────────────────────────────────────────────────────────
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        // ── Save button ───────────────────────────────────────────────────────
        TextView btnSave = findViewById(R.id.btn_save_prefs);
        if (btnSave != null) {
            btnSave.setOnClickListener(v -> savePreferences());
        }

        // ── Font size seekbar ─────────────────────────────────────────────────
        SeekBar fontBar = findViewById(R.id.seekbar_font_size);
        fontBar.setMax(FONT_SIZES.length - 1);
        fontBar.setProgress(currentFontIndex);
        fontBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar sb, int p, boolean user) {
                currentFontIndex = p;
                currentFont = FONT_SIZES[p];
                refreshPreview();
            }
            public void onStartTrackingTouch(SeekBar sb) {}
            public void onStopTrackingTouch(SeekBar sb) {}
        });

        // ── Line spacing seekbar ──────────────────────────────────────────────
        SeekBar spacingBar = findViewById(R.id.seekbar_line_spacing);
        spacingBar.setMax(LINE_SPACINGS.length - 1);
        spacingBar.setProgress(currentSpacingIndex);
        spacingBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar sb, int p, boolean user) {
                currentSpacingIndex = p;
                currentSpacing = LINE_SPACINGS[p];
                refreshPreview();
            }
            public void onStartTrackingTouch(SeekBar sb) {}
            public void onStopTrackingTouch(SeekBar sb) {}
        });

        // ── Theme pickers ─────────────────────────────────────────────────────
        findViewById(R.id.theme_light).setOnClickListener(v -> applyTheme("light"));
        findViewById(R.id.theme_dark).setOnClickListener(v  -> applyTheme("dark"));
        findViewById(R.id.theme_sepia).setOnClickListener(v -> applyTheme("sepia"));

        // ── Load saved preferences ────────────────────────────────────────────
        authViewModel.getCurrentUser().observe(this, user -> {
            if (user == null) return;

            // Font size
            int savedFontSize = user.getFontSize();
            if (savedFontSize > 0) {
                currentFont = savedFontSize;
                int idx = closestFontIndex(savedFontSize);
                currentFontIndex = idx;
                fontBar.setProgress(idx);
            }

            // Line spacing
            float savedSpacing = user.getLineSpacing();
            if (savedSpacing > 0) {
                currentSpacing = savedSpacing;
                int idx = closestSpacingIndex(savedSpacing);
                currentSpacingIndex = idx;
                spacingBar.setProgress(idx);
            }

            // Theme
            String savedTheme = user.getThemePreference();
            if (savedTheme != null && !savedTheme.isEmpty()) {
                applyTheme(savedTheme);
            } else {
                refreshPreview();
            }
        });

        refreshPreview();
    }

    // ── Theme ─────────────────────────────────────────────────────────────────

    private void applyTheme(String theme) {
        currentTheme = theme;
        if (txtPreview == null) return;
        switch (theme) {
            case "dark":
                txtPreview.setBackgroundColor(0xFF1E293B);
                txtPreview.setTextColor(0xFFFFFFFF);  // FIX: white text on dark bg
                break;
            case "sepia":
                txtPreview.setBackgroundColor(0xFFF4ECD8);
                txtPreview.setTextColor(0xFF5B4636);
                break;
            default: // light
                txtPreview.setBackgroundColor(0xFFFFFFFF);
                txtPreview.setTextColor(0xFF0F172A);
                break;
        }
        refreshPreview();
    }

    private void refreshPreview() {
        if (txtPreview == null) return;
        txtPreview.setTextSize(currentFont);
        txtPreview.setLineSpacing(0f, currentSpacing);
    }

    // ── Save ──────────────────────────────────────────────────────────────────

    private void savePreferences() {
        authViewModel.updateReadingPreferences(
                Math.round(currentFont), currentSpacing, currentTheme);
        BannerConfig config = new BannerConfig()
                .setType(BannerType.SUCCESS)
                .setTitle("Preferences saved!")
                .setMessage("Your reading settings have been applied.")
                .setDuration(3000)
                .setSwipeToDismiss(true);
        BannerManager.getInstance().queueForNextActivity(config);
        finish();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private int closestFontIndex(float fontSize) {
        int best = 0;
        float bestDist = Math.abs(FONT_SIZES[0] - fontSize);
        for (int i = 1; i < FONT_SIZES.length; i++) {
            float dist = Math.abs(FONT_SIZES[i] - fontSize);
            if (dist < bestDist) { bestDist = dist; best = i; }
        }
        return best;
    }

    private int closestSpacingIndex(float spacing) {
        int best = 0;
        float bestDist = Math.abs(LINE_SPACINGS[0] - spacing);
        for (int i = 1; i < LINE_SPACINGS.length; i++) {
            float dist = Math.abs(LINE_SPACINGS[i] - spacing);
            if (dist < bestDist) { bestDist = dist; best = i; }
        }
        return best;
    }
}

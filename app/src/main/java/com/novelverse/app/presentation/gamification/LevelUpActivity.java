package com.novelverse.app.presentation.gamification;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.novelverse.app.R;
import com.novelverse.app.data.local.preferences.UserPreferences;
import com.novelverse.app.data.remote.supabase.SupabaseDatabaseService;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * Full-screen level-up celebration takeover.
 *
 * <p>Only launched from {@link com.novelverse.app.presentation.home.HomeFragment}
 * when a level-up event fires. The window is translucent so the home screen is
 * dimmed underneath rather than replaced.
 *
 * <h3>Asset loading</h3>
 * Queries the {@code levels} Supabase table for the reached level's metadata
 * (title, icon_url, perk_text). Falls back to locally-computed defaults if
 * the table doesn't exist yet or the network is unavailable.
 *
 * <h3>Required Supabase table (run once in SQL editor)</h3>
 * <pre>
 * CREATE TABLE IF NOT EXISTS public.levels (
 *     level_number  INTEGER PRIMARY KEY,
 *     title         TEXT    NOT NULL DEFAULT 'Reader',
 *     icon_url      TEXT,
 *     perk_text     TEXT
 * );
 * ALTER TABLE public.levels ENABLE ROW LEVEL SECURITY;
 * CREATE POLICY "levels_read_all" ON public.levels
 *     FOR SELECT USING (true);
 * </pre>
 *
 * <h3>Extras</h3>
 * <ul>
 *   <li>{@link #EXTRA_NEW_LEVEL}  – int  – the level just reached</li>
 *   <li>{@link #EXTRA_XP_TOTAL}   – long – user's cumulative XP at this point</li>
 * </ul>
 */
@AndroidEntryPoint
public class LevelUpActivity extends AppCompatActivity {

    public static final String EXTRA_NEW_LEVEL = "levelup_new_level";
    public static final String EXTRA_XP_TOTAL  = "levelup_xp_total";

    /** Convenience factory — use this instead of constructing the intent manually. */
    public static Intent buildIntent(Context ctx, int newLevel, long xpTotal) {
        return new Intent(ctx, LevelUpActivity.class)
                .putExtra(EXTRA_NEW_LEVEL, newLevel)
                .putExtra(EXTRA_XP_TOTAL,  xpTotal);
    }

    @Inject SupabaseDatabaseService dbService;
    @Inject UserPreferences         userPreferences;

    // ── Views ─────────────────────────────────────────────────────────────────

    private View          cardContainer;
    private ProgressBar   loadingSpinner;
    private LinearLayout  contentGroup;
    private ImageView     iconView;
    private TextView      levelNumberView;
    private TextView      levelTitleView;
    private TextView      perkTextView;
    private TextView      xpTotalView;
    private ImageView     closeBtn;

    // ── State ─────────────────────────────────────────────────────────────────

    private int  newLevel;
    private long xpTotal;

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_level_up);

        newLevel = getIntent().getIntExtra(EXTRA_NEW_LEVEL, 1);
        xpTotal  = getIntent().getLongExtra(EXTRA_XP_TOTAL, 0L);

        bindViews();
        setStaticContent();
        animateCardIn();
        fetchLevelMetadata();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        dismissWithAnimation();
    }

    // ── View setup ────────────────────────────────────────────────────────────

    private void bindViews() {
        cardContainer   = findViewById(R.id.levelup_card_container);
        loadingSpinner  = findViewById(R.id.levelup_loading);
        contentGroup    = findViewById(R.id.levelup_content);
        iconView        = findViewById(R.id.levelup_icon);
        levelNumberView = findViewById(R.id.levelup_level_number);
        levelTitleView  = findViewById(R.id.levelup_title);
        perkTextView    = findViewById(R.id.levelup_perk_text);
        xpTotalView     = findViewById(R.id.levelup_xp_total);
        closeBtn        = findViewById(R.id.levelup_close_btn);

        closeBtn.setOnClickListener(v -> dismissWithAnimation());

        // Tap on scrim (outside card) also dismisses
        View root = findViewById(R.id.levelup_root);
        root.setOnClickListener(v -> dismissWithAnimation());
        // But don't let card taps bubble up to the scrim
        cardContainer.setOnClickListener(v -> { /* consume */ });
    }

    private void setStaticContent() {
        levelNumberView.setText("Level " + newLevel);
        xpTotalView.setText(String.format("%,d total XP", xpTotal));
        // Pre-populate perk text from local engine while we wait for Supabase
        perkTextView.setText(localPerkText(newLevel));
    }

    // ── Supabase fetch ────────────────────────────────────────────────────────

    /**
     * Fetches level metadata from the {@code levels} table.
     * On success: populates title, icon, and perk text then reveals content.
     * On failure: reveals content with local fallback values.
     */
    private void fetchLevelMetadata() {
        String token  = userPreferences.getAccessToken();
        String select = "level_number,title,icon_url,perk_text";
        String filter = "level_number=eq." + newLevel;
        String url    = dbService.buildSelectUrl("levels", select, filter, null)
                + "&limit=1";

        dbService.rawSelect(url, token, new SupabaseDatabaseService.DatabaseCallback() {
            @Override
            public void onSuccess(String json) {
                LevelMeta meta = parseLevelMeta(json);
                runOnUiThread(() -> applyMetaAndReveal(meta));
            }

            @Override
            public void onError(String message) {
                runOnUiThread(() -> applyMetaAndReveal(null));
            }
        });
    }

    private void applyMetaAndReveal(LevelMeta meta) {
        if (isFinishing() || isDestroyed()) return;

        // Title
        if (meta != null && meta.title != null && !meta.title.isEmpty()) {
            levelTitleView.setText(meta.title);
            levelTitleView.setVisibility(View.VISIBLE);
        } else {
            levelTitleView.setVisibility(View.GONE);
        }

        // Perk text — prefer Supabase value over local default
        if (meta != null && meta.perkText != null && !meta.perkText.isEmpty()) {
            perkTextView.setText(meta.perkText);
        }

        // Icon — load from URL if available, otherwise keep fallback star
        if (meta != null && meta.iconUrl != null && !meta.iconUrl.isEmpty()) {
            Glide.with(this)
                    .load(meta.iconUrl)
                    .placeholder(R.drawable.ic_star)
                    .error(R.drawable.ic_star)
                    .transition(DrawableTransitionOptions.withCrossFade(200))
                    .into(iconView);
        }

        // Swap loading → content
        loadingSpinner.setVisibility(View.GONE);
        contentGroup.setVisibility(View.VISIBLE);
        contentGroup.setAlpha(0f);
        contentGroup.animate()
                .alpha(1f)
                .setDuration(300)
                .setInterpolator(new DecelerateInterpolator())
                .start();
    }

    // ── Animations ────────────────────────────────────────────────────────────

    private void animateCardIn() {
        cardContainer.setAlpha(0f);
        cardContainer.setScaleX(0.88f);
        cardContainer.setScaleY(0.88f);
        cardContainer.setTranslationY(60f);

        cardContainer.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .translationY(0f)
                .setDuration(450)
                .setInterpolator(new OvershootInterpolator(1.05f))
                .start();
    }

    private void dismissWithAnimation() {
        cardContainer.animate()
                .alpha(0f)
                .scaleX(0.90f)
                .scaleY(0.90f)
                .translationY(40f)
                .setDuration(280)
                .setInterpolator(new DecelerateInterpolator())
                .withEndAction(this::finish)
                .start();
    }

    @Override
    public void finish() {
        super.finish();
        // No slide transition — fade handled by the animation above
        overridePendingTransition(0, 0);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private LevelMeta parseLevelMeta(String json) {
        try {
            JsonArray arr = JsonParser.parseString(json).getAsJsonArray();
            if (arr.size() == 0) return null;
            JsonObject obj = arr.get(0).getAsJsonObject();
            LevelMeta m = new LevelMeta();
            m.title    = str(obj, "title");
            m.iconUrl  = str(obj, "icon_url");
            m.perkText = str(obj, "perk_text");
            return m;
        } catch (Exception e) {
            return null;
        }
    }

    private static String str(JsonObject o, String key) {
        return (o.has(key) && !o.get(key).isJsonNull()) ? o.get(key).getAsString() : null;
    }

    /**
     * Local fallback perk text — matches XpLevelEngine milestone descriptions.
     * Used immediately before Supabase responds and whenever the fetch fails.
     */
    private static String localPerkText(int level) {
        if (level == 10) return "You've unlocked your 2nd badge slot. More ways to show who you are.";
        if (level == 25) return "3rd badge slot unlocked. Animated avatars are now available.";
        if (level == 50) return "Veteran status earned. Exclusive theme and title unlocked.";
        if (level % 10 == 0) return "A new milestone. Keep reading to unlock more perks.";
        return "Keep reading and writing — the next milestone is closer than you think.";
    }

    // ── Data class ────────────────────────────────────────────────────────────

    private static final class LevelMeta {
        String title;
        String iconUrl;
        String perkText;
    }
}
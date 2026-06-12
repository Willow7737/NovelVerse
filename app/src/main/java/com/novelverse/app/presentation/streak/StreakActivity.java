package com.novelverse.app.presentation.streak;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.novelverse.app.R;
import com.novelverse.app.data.local.entities.UserStreakEntity;
import com.novelverse.app.domain.models.StreakHistoryItem;
import com.novelverse.app.presentation.streak.StreakViewModel.AdResult;
import com.novelverse.app.ui.banner.BannerHelper;

import java.util.Calendar;
import java.util.List;
import java.util.concurrent.TimeUnit;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * Full-screen Streak hub.
 *
 * Features:
 *   • Hero card  — big flame + current streak count + status badges
 *   • 7-day week view — shows which of the last 7 days had reading activity
 *   • Stats row   — Longest streak | Freeze status | Grace status
 *   • Ad rewards  — Watch ad for +25 Ink, Streak Freeze, or Streak Recovery
 *   • Past streaks — chronological list of broken streak runs from Supabase
 *
 * Entry point: {@link com.novelverse.app.presentation.home.HomeActivity} streak icon click.
 */
@AndroidEntryPoint
public class StreakActivity extends AppCompatActivity {

    private static final long DAY_MS = TimeUnit.DAYS.toMillis(1);

    // ── Views ─────────────────────────────────────────────────────────────────

    private TextView           tvStreakNumber;
    private ImageView          ivFlame;
    private LinearLayout       statusRow;
    private LinearLayout       weekRow;
    private TextView           tvLongest;
    private TextView           tvFreezeValue;
    private TextView           tvGraceValue;
    private TextView           tvInkAdCount;
    private View               btnAdInk;
    private View               btnAdFreeze;
    private View               btnAdRecover;
    private TextView           tvRecoverLabel;
    private RecyclerView       rvPastStreaks;
    private View               emptyPastStreaks;

    // ── State ─────────────────────────────────────────────────────────────────

    private StreakViewModel      viewModel;
    private StreakHistoryAdapter historyAdapter;

    /** Streak length of the most recently broken run (0 = none recoverable). */
    private int recoverableStreak = 0;

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Edge-to-edge, matching the rest of the app
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setStatusBarColor(android.graphics.Color.TRANSPARENT);

        setContentView(R.layout.activity_streak);

        setupToolbar();
        bindViews();
        setupRecyclerView();

        viewModel = new ViewModelProvider(this).get(StreakViewModel.class);
        observeViewModel();
        setupAdButtons();
    }

    // ── Setup ─────────────────────────────────────────────────────────────────

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar_streak);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setTitle("My Streak");
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void bindViews() {
        tvStreakNumber  = findViewById(R.id.tv_streak_number);
        ivFlame         = findViewById(R.id.iv_streak_flame);
        statusRow       = findViewById(R.id.streak_status_row);
        weekRow         = findViewById(R.id.week_row);
        tvLongest       = findViewById(R.id.tv_longest_value);
        tvFreezeValue   = findViewById(R.id.tv_freeze_value);
        tvGraceValue    = findViewById(R.id.tv_grace_value);
        tvInkAdCount    = findViewById(R.id.tv_ink_ad_count);
        btnAdInk        = findViewById(R.id.btn_ad_ink);
        btnAdFreeze     = findViewById(R.id.btn_ad_freeze);
        btnAdRecover    = findViewById(R.id.btn_ad_recover);
        tvRecoverLabel  = findViewById(R.id.tv_recover_label);
        rvPastStreaks   = findViewById(R.id.rv_past_streaks);
        emptyPastStreaks = findViewById(R.id.past_streaks_empty);
    }

    private void setupRecyclerView() {
        historyAdapter = new StreakHistoryAdapter();
        rvPastStreaks.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        rvPastStreaks.setAdapter(historyAdapter);
        rvPastStreaks.setNestedScrollingEnabled(false);
    }

    // ── Observation ───────────────────────────────────────────────────────────

    private void observeViewModel() {
        viewModel.streakLive.observe(this, this::renderStreak);
        viewModel.historyLive.observe(this, this::renderHistory);
        viewModel.adResultLive.observe(this, this::handleAdResult);
    }

    // ── Streak rendering ──────────────────────────────────────────────────────

    private void renderStreak(UserStreakEntity streak) {
        long nowMs = System.currentTimeMillis();
        int current = streak != null ? streak.getCurrentStreak() : 0;

        // ─ Hero number ─
        tvStreakNumber.setText(String.valueOf(current));

        // ─ Status badges ─
        renderStatusBadges(streak, nowMs);

        // ─ Week calendar ─
        buildWeekRow(streak, nowMs);

        // ─ Stats ─
        tvLongest.setText(streak != null
                ? String.valueOf(streak.getLongestStreak()) : "0");

        boolean freezeActive = streak != null && streak.getFreezeExpiresAt() > nowMs;
        tvFreezeValue.setText(freezeActive ? "Active ❄" : "None");
        tvFreezeValue.setTextColor(freezeActive
                ? getColor(R.color.primary)
                : getColor(R.color.text_secondary_dark));

        boolean graceAvailable = streak != null && !streak.isGraceUsedInWindow();
        tvGraceValue.setText(graceAvailable ? "Ready" : "Used");
        tvGraceValue.setTextColor(graceAvailable
                ? getColor(R.color.accent_teal)
                : getColor(R.color.text_tertiary_dark));
    }

    private void renderStatusBadges(UserStreakEntity streak, long nowMs) {
        statusRow.removeAllViews();
        if (streak == null) { statusRow.setVisibility(View.GONE); return; }

        boolean freezeActive = streak.getFreezeExpiresAt() > nowMs;
        boolean graceAvailable = !streak.isGraceUsedInWindow();
        int current = streak.getCurrentStreak();

        if (!freezeActive && !graceAvailable && current == 0) {
            statusRow.setVisibility(View.GONE);
            return;
        }

        statusRow.setVisibility(View.VISIBLE);

        if (freezeActive) addBadge(statusRow, "❄  Freeze Active", "#2563EB", "#EFF6FF");
        if (graceAvailable) addBadge(statusRow, "✓  Grace Available", "#059669", "#ECFDF5");
        if (current >= 30) addBadge(statusRow, "🛡  Shield Eligible", "#7C3AED", "#F5F3FF");
    }

    private void addBadge(LinearLayout parent, String text, String textHex, String bgHex) {
        TextView badge = new TextView(this);
        badge.setText(text);
        badge.setTextSize(11f);
        badge.setTextColor(Color.parseColor(textHex));
        int padH = dp(10), padV = dp(5);
        badge.setPadding(padH, padV, padH, padV);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.parseColor(bgHex));
        bg.setCornerRadius(dp(20));
        badge.setBackground(bg);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMarginEnd(dp(6));
        badge.setLayoutParams(lp);
        parent.addView(badge);
    }

    // ── 7-day week calendar ───────────────────────────────────────────────────

    /**
     * Populates the week_row LinearLayout with 7 day columns.
     *
     * Each column = day letter + circular dot indicator + date number.
     * A filled dot (amber) = reading activity that day; outline dot = no activity.
     *
     * The columns span [6 days ago … today], left to right.
     */
    private void buildWeekRow(UserStreakEntity streak, long nowMs) {
        weekRow.removeAllViews();

        boolean[] active = computeActiveDays(streak, nowMs);

        // Day-of-week letters starting from 6 days ago
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(nowMs);
        String[] dayLetters = {"S", "M", "T", "W", "T", "F", "S"};

        // Build day labels for the 7 cells (oldest to newest)
        String[] labels = new String[7];
        int[]    dates  = new int[7];
        for (int i = 6; i >= 0; i--) {
            Calendar c = Calendar.getInstance();
            c.setTimeInMillis(nowMs - (long) i * DAY_MS);
            labels[6 - i] = dayLetters[c.get(Calendar.DAY_OF_WEEK) - 1];
            dates[6 - i]  = c.get(Calendar.DAY_OF_MONTH);
        }

        for (int i = 0; i < 7; i++) {
            weekRow.addView(buildDayCell(labels[i], dates[i], active[i], i == 6));
        }
    }

    /**
     * Returns a boolean[7] where index 0 = 6 days ago, index 6 = today.
     * A cell is {@code true} if reading activity occurred on that day based on
     * current streak + last activity date.
     */
    private boolean[] computeActiveDays(UserStreakEntity streak, long nowMs) {
        boolean[] active = new boolean[7];
        if (streak == null || streak.getCurrentStreak() == 0) return active;

        long lastMs       = streak.getLastActivityDate();
        long todayStart   = (nowMs / DAY_MS) * DAY_MS;
        long lastDayStart = (lastMs / DAY_MS) * DAY_MS;
        long daysAgoLast  = (todayStart - lastDayStart) / DAY_MS;

        // daysAgoLast == 0 → last activity was today
        // daysAgoLast == 1 → last activity was yesterday
        // If > 2, streak is effectively broken (but we still show the state)
        int streak_len = streak.getCurrentStreak();

        for (int col = 0; col < 7; col++) {
            // col 0 = 6 days ago, col 6 = today
            long daysAgo = 6 - col;
            // Active if this day falls within [daysAgoLast, daysAgoLast + streak_len)
            if (daysAgo >= daysAgoLast && daysAgo < daysAgoLast + streak_len) {
                active[col] = true;
            }
        }
        return active;
    }

    @NonNull
    private View buildDayCell(String letter, int dateNum, boolean active, boolean isToday) {
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams colLp = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        col.setLayoutParams(colLp);
        col.setPadding(0, dp(4), 0, dp(4));

        // Day letter (M/T/W…)
        TextView tvLetter = new TextView(this);
        tvLetter.setText(letter);
        tvLetter.setTextSize(10f);
        tvLetter.setGravity(Gravity.CENTER);
        tvLetter.setTextColor(isToday
                ? getColor(R.color.secondary)
                : getColor(R.color.text_tertiary_dark));
        tvLetter.setTypeface(null, android.graphics.Typeface.BOLD);
        col.addView(tvLetter);

        // Circle dot
        int dotSize = dp(28);
        FrameLayout dotFrame = new FrameLayout(this);
        LinearLayout.LayoutParams frameLp = new LinearLayout.LayoutParams(dotSize, dotSize);
        frameLp.topMargin = dp(6);
        frameLp.bottomMargin = dp(6);
        frameLp.gravity = Gravity.CENTER_HORIZONTAL;
        dotFrame.setLayoutParams(frameLp);

        View dot = new View(this);
        GradientDrawable circle = new GradientDrawable();
        circle.setShape(GradientDrawable.OVAL);

        if (active) {
            circle.setColor(getColor(R.color.secondary));
        } else if (isToday) {
            circle.setColor(Color.TRANSPARENT);
            circle.setStroke(dp(2), getColor(R.color.secondary));
        } else {
            circle.setColor(Color.TRANSPARENT);
            circle.setStroke(dp(1), getColor(R.color.divider_dark));
        }
        dot.setBackground(circle);
        FrameLayout.LayoutParams dotLp = new FrameLayout.LayoutParams(dotSize, dotSize);
        dotLp.gravity = Gravity.CENTER;
        dot.setLayoutParams(dotLp);

        // Checkmark text inside active dot
        if (active) {
            TextView check = new TextView(this);
            check.setText("✓");
            check.setTextSize(12f);
            check.setTextColor(Color.WHITE);
            check.setGravity(Gravity.CENTER);
            FrameLayout.LayoutParams checkLp = new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT);
            check.setLayoutParams(checkLp);
            dotFrame.addView(dot);
            dotFrame.addView(check);
        } else {
            dotFrame.addView(dot);
        }
        col.addView(dotFrame);

        // Date number below dot
        TextView tvDate = new TextView(this);
        tvDate.setText(String.valueOf(dateNum));
        tvDate.setTextSize(10f);
        tvDate.setGravity(Gravity.CENTER);
        tvDate.setTextColor(isToday
                ? getColor(R.color.secondary)
                : getColor(R.color.text_tertiary_dark));
        col.addView(tvDate);

        return col;
    }

    // ── History rendering ─────────────────────────────────────────────────────

    private void renderHistory(List<StreakHistoryItem> items) {
        if (items == null || items.isEmpty()) {
            rvPastStreaks.setVisibility(View.GONE);
            emptyPastStreaks.setVisibility(View.VISIBLE);
            recoverableStreak = 0;
            updateRecoverButton();
            return;
        }

        rvPastStreaks.setVisibility(View.VISIBLE);
        emptyPastStreaks.setVisibility(View.GONE);
        historyAdapter.submitList(items);

        // Check if the most recent break is recoverable (within 48h)
        long nowMs = System.currentTimeMillis();
        StreakHistoryItem latest = items.get(0);
        if (latest.isRecentlyBroken(nowMs) && latest.streakLength > 1) {
            recoverableStreak = latest.streakLength;
        } else {
            recoverableStreak = 0;
        }
        updateRecoverButton();
    }

    // ── Ad buttons ────────────────────────────────────────────────────────────

    private void setupAdButtons() {
        btnAdInk.setOnClickListener(v -> viewModel.watchAdForInk(this));
        btnAdFreeze.setOnClickListener(v -> viewModel.watchAdForFreeze(this));
        btnAdRecover.setOnClickListener(v -> {
            if (recoverableStreak > 0) {
                viewModel.watchAdToRecover(this, recoverableStreak);
            }
        });
    }

    private void updateRecoverButton() {
        if (recoverableStreak > 0) {
            btnAdRecover.setVisibility(View.VISIBLE);
            tvRecoverLabel.setText(
                    "Watch Ad → Recover " + recoverableStreak + "-Day Streak");
        } else {
            btnAdRecover.setVisibility(View.GONE);
        }
    }

    // ── Ad result handling ────────────────────────────────────────────────────

    private void handleAdResult(AdResult result) {
        if (result == null) return;

        if ("ad_not_ready".equals(result.reason)) {
            BannerHelper.info(this, "Ad not ready", "Please try again in a moment.");
            return;
        }

        switch (result.type) {
            case INK:
                if (result.success) {
                    BannerHelper.success(this,
                            "+" + result.inkAwarded + " Ink earned!",
                            "Thanks for watching.");
                    // Refresh ink count label — the RPC updates user_currency on server;
                    // local Room stays in sync on next app session / sync cycle.
                } else if ("daily_limit".equals(result.reason)) {
                    BannerHelper.warning(this,
                            "Daily limit reached",
                            "You can watch up to 3 Ink ads per day. Come back tomorrow!");
                } else {
                    BannerHelper.error(this, "Could not award Ink", "Please try again.");
                }
                break;

            case FREEZE:
                if (result.success) {
                    BannerHelper.success(this,
                            "Streak Freeze activated! ❄",
                            "Your streak is protected for the next 48 hours.");
                    // Update local Room so the UI refreshes immediately
                    long freezeMs = System.currentTimeMillis() + 2L * 24 * 60 * 60 * 1000;
                    // The repo.adRestoreFreeze already updates Room; Room LiveData will re-emit.
                } else if ("daily_limit".equals(result.reason)) {
                    BannerHelper.warning(this,
                            "Already used today",
                            "You can get one free freeze per day via ads.");
                } else {
                    BannerHelper.error(this, "Freeze failed", "Please try again.");
                }
                break;

            case RECOVERY:
                if (result.success) {
                    BannerHelper.success(this,
                            "Streak recovered! 🔥",
                            "Your " + result.recoveredTo + "-day streak is back!");
                    recoverableStreak = 0;
                    updateRecoverButton();
                } else if ("daily_limit".equals(result.reason)) {
                    BannerHelper.warning(this,
                            "Already recovered today",
                            "Streak recovery via ads is once per day.");
                } else if ("no_recent_break".equals(result.reason)) {
                    BannerHelper.info(this,
                            "Nothing to recover",
                            "No recent streak break found.");
                    recoverableStreak = 0;
                    updateRecoverButton();
                } else {
                    BannerHelper.error(this, "Recovery failed", "Please try again.");
                }
                break;
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }

    private int resolveColor(int attr) {
        android.util.TypedValue tv = new android.util.TypedValue();
        getTheme().resolveAttribute(attr, tv, true);
        return tv.data;
    }
}
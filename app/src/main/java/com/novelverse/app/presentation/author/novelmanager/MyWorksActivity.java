package com.novelverse.app.presentation.author.novelmanager;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.novelverse.app.R;
import com.novelverse.app.data.local.preferences.UserPreferences;
import com.novelverse.app.domain.gamification.AchievementEngine;
import com.novelverse.app.domain.models.Novel;
import com.novelverse.app.presentation.write.WriterViewModel;
import com.novelverse.app.ui.banner.BannerHelper;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class MyWorksActivity extends AppCompatActivity {

    private int[][] CHIP_COLORS;

    private void initChipColors() {
        // Loaded from color resources so dark mode overrides apply automatically
        int white = android.graphics.Color.WHITE;
        CHIP_COLORS =
                new int[][] {
                    {
                        white,
                        getColor(com.novelverse.app.R.color.chip_all_active),
                        getColor(com.novelverse.app.R.color.chip_all_active)
                    },
                    {
                        white,
                        getColor(com.novelverse.app.R.color.chip_published_active),
                        getColor(com.novelverse.app.R.color.chip_published_active)
                    },
                    {
                        white,
                        getColor(com.novelverse.app.R.color.chip_draft_active),
                        getColor(com.novelverse.app.R.color.chip_draft_active)
                    },
                    {
                        white,
                        getColor(com.novelverse.app.R.color.chip_completed_active),
                        getColor(com.novelverse.app.R.color.chip_completed_active)
                    },
                };
    }

    private WriterViewModel vm;
    private LinearLayout container;
    private LinearLayout emptyState;
    private String activeFilter = "all";
    private List<Novel> allNovels = new ArrayList<>();

    @Inject AchievementEngine achievementEngine;
    @Inject UserPreferences userPreferences;

    // Keep references so we can re-style on filter change
    private TextView chipAll, chipPublished, chipDraft, chipCompleted;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_works);

        container = findViewById(R.id.novels_container);
        emptyState = findViewById(R.id.empty_state);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        findViewById(R.id.btn_new_novel)
                .setOnClickListener(
                        v -> startActivity(new Intent(this, NovelEditorActivity.class)));

        chipAll = findViewById(R.id.filter_all);
        chipPublished = findViewById(R.id.filter_published);
        chipDraft = findViewById(R.id.filter_draft);
        chipCompleted = findViewById(R.id.filter_completed);

        initChipColors();

        View.OnClickListener chipListener =
                v -> {
                    if (v == chipAll) activeFilter = "all";
                    else if (v == chipPublished) activeFilter = "ongoing";
                    else if (v == chipDraft) activeFilter = "draft";
                    else if (v == chipCompleted) activeFilter = "completed";
                    styleChips();
                    applyFilter();
                };
        chipAll.setOnClickListener(chipListener);
        chipPublished.setOnClickListener(chipListener);
        chipDraft.setOnClickListener(chipListener);
        chipCompleted.setOnClickListener(chipListener);

        // Initial style
        styleChips();

        vm = new ViewModelProvider(this).get(WriterViewModel.class);
        vm.getMyNovels()
                .observe(
                        this,
                        novels -> {
                            allNovels = novels != null ? novels : new ArrayList<>();
                            applyFilter();
                            checkAuthorReadAchievements(allNovels);
                        });
        vm.getError()
                .observe(
                        this,
                        err -> {
                            if (err != null && !err.isEmpty())
                                BannerHelper.error(this, "Error", err);
                        });
    }

    /** Sum total views across all published novels and fire achievement checks. */
    private void checkAuthorReadAchievements(List<Novel> novels) {
        String userId = userPreferences.getUserId();
        if (userId == null || achievementEngine == null || novels == null) return;

        long totalReads = 0;
        for (Novel n : novels) {
            Long views = n.getTotalViews(); // Get as wrapper if possible
            if (views != null) {
                totalReads += views;
            }
            // Alternative (if getTotalViews() is primitive long):
            // totalReads += n.getTotalViews();       // just add it directly (0 if not set)
        }

        achievementEngine.onTotalReadsUpdated(
                this,
                userId,
                (int) Math.min(totalReads, Integer.MAX_VALUE),
                System.currentTimeMillis());
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (vm != null) vm.refreshMyNovels();
    }

    // ── Chip styling ──────────────────────────────────────────────────────

    private void styleChips() {
        String[] filters = {"all", "ongoing", "draft", "completed"};
        TextView[] chips = {chipAll, chipPublished, chipDraft, chipCompleted};
        for (int i = 0; i < chips.length; i++) {
            if (chips[i] == null) continue;
            boolean active = filters[i].equals(activeFilter);
            int[] colors = CHIP_COLORS[i];
            applyChipStyle(chips[i], active, colors[0], colors[1], colors[2]);
        }
    }

    /**
     * Active → solid fill with matching color, white text Inactive → transparent bg with colored
     * border, colored text
     */
    private void applyChipStyle(
            TextView chip,
            boolean active,
            int activeTextColor,
            int activeBgColor,
            int inactiveBorderColor) {
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(100));
        if (active) {
            bg.setColor(activeBgColor);
            bg.setStroke(0, android.graphics.Color.TRANSPARENT);
            chip.setTextColor(activeTextColor);
        } else {
            // Use themed inactive background from chip index (passed as inactiveBorderColor slot)
            // Fallback: derive a light tint from the active color
            int inactiveBg = blendWithSurface(activeBgColor, 0.10f);
            bg.setColor(inactiveBg);
            bg.setStroke(dp(1), inactiveBorderColor);
            chip.setTextColor(inactiveBorderColor);
        }
        chip.setBackground(bg);
    }

    /** Blends a color with surface at the given alpha — gives a soft tinted background. */
    private int blendWithSurface(int color, float alpha) {
        int a = (int) (alpha * 255);
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    // ── Filter + render ───────────────────────────────────────────────────

    private void applyFilter() {
        if (container == null) return;
        List<Novel> filtered = new ArrayList<>();
        for (Novel n : allNovels) {
            if ("all".equals(activeFilter)) {
                filtered.add(n);
                continue;
            }
            String status = n.getStatus() != null ? n.getStatus() : "draft";
            if (activeFilter.equals(status)) filtered.add(n);
        }
        render(filtered);
    }

    private void render(List<Novel> novels) {
        container.removeAllViews();
        if (novels == null || novels.isEmpty()) {
            emptyState.setVisibility(View.VISIBLE);
            container.setVisibility(View.GONE);
            return;
        }
        emptyState.setVisibility(View.GONE);
        container.setVisibility(View.VISIBLE);
        for (Novel novel : novels) {
            container.addView(buildCard(novel));
        }
    }

    private View buildCard(Novel novel) {
        View card =
                LayoutInflater.from(this)
                        .inflate(R.layout.item_novel_author_card, container, false);

        ImageView cover = card.findViewById(R.id.card_cover);
        TextView title = card.findViewById(R.id.card_title);
        TextView meta = card.findViewById(R.id.card_meta);
        TextView badge = card.findViewById(R.id.card_status);

        if (novel.getCoverImageUrl() != null && !novel.getCoverImageUrl().isEmpty()) {
            Glide.with(this)
                    .load(novel.getCoverImageUrl())
                    .placeholder(R.drawable.img_placeholder_cover)
                    .centerCrop()
                    .into(cover);
        } else {
            cover.setImageResource(R.drawable.img_placeholder_cover);
        }

        title.setText(novel.getTitle() != null ? novel.getTitle() : "Untitled");

        String chStr = novel.getTotalChapters() + " ch";
        String views = formatCount(novel.getTotalViews());
        meta.setText(chStr + "  ·  " + views + " views");

        String status = novel.getStatus() != null ? novel.getStatus() : "draft";
        badge.setText(capitalize(status));

        // Badge colors match the chip colors above
        int badgeColor;
        switch (status) {
            case "completed":
                badgeColor = Color.parseColor("#3B82F6");
                break; // blue
            case "ongoing":
                badgeColor = Color.parseColor("#10B981");
                break; // green
            default:
                badgeColor = Color.parseColor("#F59E0B");
                break; // amber
        }
        GradientDrawable badgeBg = new GradientDrawable();
        badgeBg.setColor((badgeColor & 0x00FFFFFF) | 0x26000000); // ~15% opacity fill
        badgeBg.setStroke(dp(1), badgeColor);
        badgeBg.setCornerRadius(dp(100));
        badge.setBackground(badgeBg);
        badge.setTextColor(badgeColor);

        // Cover tap → detail view
        View coverContainer = card.findViewById(R.id.card_cover_container);
        if (coverContainer != null) {
            coverContainer.setOnClickListener(
                    v -> {
                        Intent i =
                                new Intent(
                                        this,
                                        com.novelverse.app.presentation.novel.detail
                                                .NovelDetailActivity.class);
                        i.putExtra(
                                com.novelverse.app.presentation.novel.detail.NovelDetailActivity
                                        .EXTRA_NOVEL_ID,
                                novel.getId());
                        startActivity(i);
                    });
        }

        // Card body tap → manage
        card.setOnClickListener(
                v -> {
                    Intent i = new Intent(this, NovelManagerActivity.class);
                    i.putExtra(NovelManagerActivity.EXTRA_NOVEL_ID, novel.getId());
                    startActivity(i);
                });

        return card;
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private String formatCount(long n) {
        if (n >= 1_000_000) return String.format("%.1fM", n / 1_000_000f);
        if (n >= 1_000) return String.format("%.1fk", n / 1_000f);
        return String.valueOf(n);
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1).toLowerCase();
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}

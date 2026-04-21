package com.novelverse.app.presentation.write;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.novelverse.app.R;
import com.novelverse.app.domain.models.Novel;
import com.novelverse.app.domain.models.User;
import com.novelverse.app.presentation.auth.AuthViewModel;
import com.novelverse.app.presentation.author.analytics.AuthorAnalyticsActivity;
import com.novelverse.app.presentation.author.chaptermanager.ChapterEditorActivity;
import com.novelverse.app.presentation.author.novelmanager.NovelEditorActivity;
import com.novelverse.app.ui.banner.BannerHelper;

import java.util.List;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class WriteFragment extends Fragment {

    private AuthViewModel  authViewModel;
    private WriterViewModel writerViewModel;

    private TextView    txtTotalEarnings;
    private TextView    txtTotalWorks;
    private TextView    txtTotalViews;
    private TextView    txtAvailablePayout;
    private LinearLayout novelsContainer;
    private LinearLayout emptyNovels;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_write, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        authViewModel  = new ViewModelProvider(requireActivity()).get(AuthViewModel.class);
        writerViewModel = new ViewModelProvider(this).get(WriterViewModel.class);

        txtTotalEarnings   = view.findViewById(R.id.txt_total_earnings);
        txtTotalWorks      = view.findViewById(R.id.txt_total_works);
        txtTotalViews      = view.findViewById(R.id.txt_total_views);
        txtAvailablePayout = view.findViewById(R.id.txt_available_payout);
        novelsContainer    = view.findViewById(R.id.novels_container);
        emptyNovels        = view.findViewById(R.id.empty_novels);

        // Header buttons
        view.findViewById(R.id.btn_new_novel).setOnClickListener(v -> openNewNovel());
        view.findViewById(R.id.btn_empty_new_novel).setOnClickListener(v -> openNewNovel());
        view.findViewById(R.id.btn_see_all_novels).setOnClickListener(v -> startActivity(new Intent(requireContext(), com.novelverse.app.presentation.author.novelmanager.MyWorksActivity.class)));

        // Quick actions
        view.findViewById(R.id.action_chapters).setOnClickListener(v ->
                BannerHelper.info(requireActivity(), "Select a novel below", "Tap a novel card to add or manage chapters."));
        view.findViewById(R.id.action_analytics).setOnClickListener(v ->
                startActivity(new Intent(requireContext(), AuthorAnalyticsActivity.class)));
        view.findViewById(R.id.action_payout).setOnClickListener(v ->
                startActivity(new Intent(requireContext(),
                        com.novelverse.app.presentation.payment.subscriptions.SubscriptionActivity.class)));

        // Wire scroll-hide on the nav bar
        androidx.core.widget.NestedScrollView writeScroll = view.findViewById(R.id.write_scroll);
        if (writeScroll != null && getActivity() instanceof com.novelverse.app.presentation.home.HomeActivity) {
            ((com.novelverse.app.presentation.home.HomeActivity) getActivity()).attachNavToScroll(writeScroll);
        }

        authViewModel.getCurrentUser().observe(getViewLifecycleOwner(), this::updateHero);

        // Load real novels
        writerViewModel.getMyNovels().observe(getViewLifecycleOwner(), this::displayNovels);

        writerViewModel.getError().observe(getViewLifecycleOwner(), err -> {
            if (err != null && !err.isEmpty()) {
                BannerHelper.error(requireActivity(), "Novel error", err);
            }
        });
    }

    private void updateHero(User user) {
        if (user == null) return;
        if (txtTotalEarnings   != null) txtTotalEarnings.setText(String.format("$%.2f", user.getTotalEarnings()));
        if (txtAvailablePayout != null) txtAvailablePayout.setText(String.format("$%.2f", user.getAvailableForPayout()));
    }

    private void displayNovels(List<Novel> novels) {
        if (novelsContainer == null || emptyNovels == null) return;
        novelsContainer.removeAllViews();

        if (novels == null || novels.isEmpty()) {
            emptyNovels.setVisibility(View.VISIBLE);
            novelsContainer.setVisibility(View.GONE);
            if (txtTotalWorks != null) txtTotalWorks.setText("0");
            if (txtTotalViews != null) txtTotalViews.setText("0");
            return;
        }

        emptyNovels.setVisibility(View.GONE);
        novelsContainer.setVisibility(View.VISIBLE);

        if (txtTotalWorks != null) txtTotalWorks.setText(String.valueOf(novels.size()));
        long views = 0;
        for (Novel n : novels) views += n.getTotalViews();
        if (txtTotalViews != null) txtTotalViews.setText(formatCount(views));

        // Show only the 2 most recent novels in the dashboard preview
        int limit = Math.min(2, novels.size());
        for (int i = 0; i < limit; i++) {
            buildNovelCard(novels.get(i));
        }
    }

    private void buildNovelCard(Novel novel) {
        // Inflate the shared author card layout
        android.view.View card = android.view.LayoutInflater.from(requireContext())
                .inflate(R.layout.item_novel_author_card, novelsContainer, false);

        android.widget.ImageView cover = card.findViewById(R.id.card_cover);
        android.widget.TextView  title = card.findViewById(R.id.card_title);
        android.widget.TextView  meta  = card.findViewById(R.id.card_meta);
        android.widget.TextView  badge = card.findViewById(R.id.card_status);

        if (novel.getCoverImageUrl() != null && !novel.getCoverImageUrl().isEmpty()) {
            com.bumptech.glide.Glide.with(this).load(novel.getCoverImageUrl())
                    .placeholder(R.drawable.img_placeholder_cover).centerCrop().into(cover);
        } else {
            cover.setImageResource(R.drawable.img_placeholder_cover);
        }
        title.setText(novel.getTitle() != null ? novel.getTitle() : "Untitled");
        String status = novel.getStatus() != null ? novel.getStatus() : "draft";
        meta.setText(novel.getTotalChapters() + " ch  ·  " + formatCount(novel.getTotalViews()) + " views");
        badge.setText(capitalize(status));
        int badgeColor;
        switch (status) {
            case "completed": badgeColor = android.graphics.Color.parseColor("#10B981"); break;
            case "ongoing":   badgeColor = android.graphics.Color.parseColor("#6366F1"); break;
            default:          badgeColor = android.graphics.Color.parseColor("#F59E0B"); break;
        }
        android.graphics.drawable.GradientDrawable badgeBg = new android.graphics.drawable.GradientDrawable();
        badgeBg.setColor((badgeColor & 0x00FFFFFF) | 0x22000000);
        badgeBg.setCornerRadius(dp(100));
        badge.setBackground(badgeBg);
        badge.setTextColor(badgeColor);

        // Cover tap → reader detail view
        android.view.View coverContainer = card.findViewById(R.id.card_cover_container);
        if (coverContainer != null) {
            coverContainer.setOnClickListener(v -> {
                android.content.Intent i = new android.content.Intent(requireContext(),
                        com.novelverse.app.presentation.novel.detail.NovelDetailActivity.class);
                i.putExtra(com.novelverse.app.presentation.novel.detail.NovelDetailActivity.EXTRA_NOVEL_ID, novel.getId());
                startActivity(i);
            });
        }
        // Rest of card → manage/edit
        card.setOnClickListener(v -> {
            android.content.Intent i = new android.content.Intent(requireContext(),
                    com.novelverse.app.presentation.author.novelmanager.NovelManagerActivity.class);
            i.putExtra(com.novelverse.app.presentation.author.novelmanager.NovelManagerActivity.EXTRA_NOVEL_ID, novel.getId());
            startActivity(i);
        });
        novelsContainer.addView(card);
        return; // early return — old code below is replaced
        // ──────────────────────────────────────────────────────────────────
    }

    @SuppressWarnings("unused")
    private void buildNovelCard_UNUSED(Novel novel) {
        LinearLayout card = new LinearLayout(requireContext());
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(dp(14), dp(14), dp(14), dp(14));
        GradientDrawable cardBg = new GradientDrawable();
        cardBg.setColor(Color.WHITE);
        cardBg.setCornerRadius(dp(14));
        card.setBackground(cardBg);
        LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        cardLp.bottomMargin = dp(8);
        card.setLayoutParams(cardLp);

        // Status colour strip
        View strip = new View(requireContext());
        String status  = novel.getStatus() != null ? novel.getStatus() : "draft";
        int stripColor = "completed".equals(status) ? Color.parseColor("#10B981")
                : "ongoing".equals(status)  ? Color.parseColor("#6366F1")
                : Color.parseColor("#F59E0B");
        GradientDrawable stripBg = new GradientDrawable();
        stripBg.setColor(stripColor);
        stripBg.setCornerRadius(dp(4));
        strip.setBackground(stripBg);
        LinearLayout.LayoutParams stripLp = new LinearLayout.LayoutParams(dp(4), LinearLayout.LayoutParams.MATCH_PARENT);
        stripLp.rightMargin = dp(12);
        strip.setLayoutParams(stripLp);
        card.addView(strip);

        // Info block
        LinearLayout info = new LinearLayout(requireContext());
        info.setOrientation(LinearLayout.VERTICAL);
        info.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        TextView tvTitle = new TextView(requireContext());
        tvTitle.setText(novel.getTitle());
        tvTitle.setTextColor(resolveAttrColor(android.R.attr.textColorPrimary));
        tvTitle.setTextSize(15f);
        tvTitle.setTypeface(tvTitle.getTypeface(), Typeface.BOLD);
        info.addView(tvTitle);

        String meta = (novel.getTotalChapters() + " ch")
                + "  ·  " + formatCount(novel.getTotalViews()) + " views"
                + "  ·  " + capitalize(status);
        TextView tvMeta = new TextView(requireContext());
        tvMeta.setText(meta);
        tvMeta.setTextColor(resolveAttrColor(android.R.attr.textColorSecondary));
        tvMeta.setTextSize(13f);
        LinearLayout.LayoutParams metaLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        metaLp.topMargin = dp(2);
        tvMeta.setLayoutParams(metaLp);
        info.addView(tvMeta);
        card.addView(info);

        // Add chapter button
        TextView btnChapter = new TextView(requireContext());
        btnChapter.setText("+ Chapter");
        btnChapter.setTextColor(Color.parseColor("#6366F1"));
        btnChapter.setTextSize(13f);
        btnChapter.setTypeface(btnChapter.getTypeface(), Typeface.BOLD);
        btnChapter.setPadding(dp(10), dp(6), dp(10), dp(6));
        GradientDrawable chBg = new GradientDrawable();
        chBg.setColor(Color.parseColor("#EEF2FF"));
        chBg.setCornerRadius(dp(8));
        btnChapter.setBackground(chBg);
        LinearLayout.LayoutParams btnLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        btnLp.leftMargin = dp(8);
        btnChapter.setLayoutParams(btnLp);
        btnChapter.setOnClickListener(v -> openChapterEditor(novel.getId()));
        card.addView(btnChapter);

        // Chevron / edit
        TextView chevron = new TextView(requireContext());
        chevron.setText("›");
        chevron.setTextColor(resolveAttrColor(android.R.attr.textColorTertiary));
        chevron.setTextSize(22f);
        LinearLayout.LayoutParams chLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        chLp.leftMargin = dp(6);
        chevron.setLayoutParams(chLp);
        card.addView(chevron);

        card.setOnClickListener(v -> openEditNovel(novel.getId()));
        novelsContainer.addView(card);
    }

    private void openNewNovel() {
        startActivity(new Intent(requireContext(), NovelEditorActivity.class));
    }

    private void openEditNovel(String novelId) {
        Intent i = new Intent(requireContext(),
                com.novelverse.app.presentation.author.novelmanager.NovelManagerActivity.class);
        i.putExtra(com.novelverse.app.presentation.author.novelmanager.NovelManagerActivity.EXTRA_NOVEL_ID, novelId);
        startActivity(i);
    }

    private void openChapterEditor(String novelId) {
        Intent i = new Intent(requireContext(), ChapterEditorActivity.class);
        i.putExtra(ChapterEditorActivity.EXTRA_NOVEL_ID, novelId);
        startActivity(i);
    }

    private String formatCount(long n) {
        if (n >= 1_000_000) return String.format("%.1fM", n / 1_000_000f);
        if (n >= 1_000)     return String.format("%.1fk", n / 1_000f);
        return String.valueOf(n);
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1).toLowerCase();
    }

    @Override
    public void onResume() {
        super.onResume();
        // Refresh the novels list each time this fragment becomes visible
        // (e.g. returning from NovelEditorActivity after creating/editing a novel)
        if (writerViewModel != null) {
            writerViewModel.refreshMyNovels();
        }
    }

    private int dp(int v) {
        return Math.round(v * requireContext().getResources().getDisplayMetrics().density);
    }
    private int resolveAttrColor(int attr) {
        android.util.TypedValue tv = new android.util.TypedValue();
        requireContext().getTheme().resolveAttribute(attr, tv, true);
        return tv.data;
    }

}

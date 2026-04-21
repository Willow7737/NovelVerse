package com.novelverse.app.presentation.author.novelmanager;

import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.novelverse.app.R;
import com.novelverse.app.data.repository.NovelRepository;
import com.novelverse.app.domain.models.Novel;
import com.novelverse.app.presentation.write.WriterViewModel;
import com.novelverse.app.ui.banner.BannerHelper;
import com.novelverse.app.ui.banner.BannerManager;
import com.novelverse.app.ui.banner.BannerConfig;
import com.novelverse.app.ui.banner.BannerType;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * Novel Manager — ViewPager2 host for a single novel's management.
 *
 * Tabs: Chapters | Info & Settings | Drafts
 * Header: tilted cover + title + "more" menu (delete novel)
 *
 * Launch with: intent.putExtra(EXTRA_NOVEL_ID, id)
 */
@AndroidEntryPoint
public class NovelManagerActivity extends AppCompatActivity {

    public static final String EXTRA_NOVEL_ID = "novel_id";
    private static final String[] TAB_LABELS = { "Chapters", "Drafts", "Info" };

    private WriterViewModel vm;
    private String          novelId;
    private Novel           novel;

    private ImageView headerCover;
    private TextView  headerTitle;
    private TextView  headerStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_novel_manager);

        novelId = getIntent().getStringExtra(EXTRA_NOVEL_ID);
        if (novelId == null) { finish(); return; }

        vm = new ViewModelProvider(this).get(WriterViewModel.class);

        headerCover  = findViewById(R.id.header_cover);
        headerTitle  = findViewById(R.id.header_title);
        headerStatus = findViewById(R.id.header_status);

        // Back
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        // More / delete menu
        findViewById(R.id.btn_more).setOnClickListener(this::showMoreMenu);

        // ViewPager2
        ViewPager2 pager = findViewById(R.id.view_pager);
        pager.setAdapter(new PagerAdapter(this, novelId));
        pager.setOffscreenPageLimit(2); // keep all tabs alive

        // TabLayout
        TabLayout tabs = findViewById(R.id.tab_layout);
        new TabLayoutMediator(tabs, pager, (tab, pos) -> tab.setText(TAB_LABELS[pos])).attach();

        // Load novel details into header
        vm.getNovelById(novelId).observe(this, this::updateHeader);
    }

    private void updateHeader(Novel n) {
        if (n == null) return;
        this.novel = n;
        if (headerTitle != null) headerTitle.setText(n.getTitle() != null ? n.getTitle() : "Novel");
        if (headerStatus != null) {
            String status = n.getStatus() != null ? capitalize(n.getStatus()) : "Draft";
            boolean published = n.isPublished();
            headerStatus.setText((published ? "● Published  " : "○ Draft  ") + "· " + status);
        }
        if (headerCover != null && n.getCoverImageUrl() != null && !n.getCoverImageUrl().isEmpty()) {
            Glide.with(this)
                    .load(n.getCoverImageUrl())
                    .placeholder(R.drawable.img_placeholder_cover)
                    .centerCrop()
                    .into(headerCover);
        }
    }

    private void showMoreMenu(View anchor) {
        PopupMenu popup = new PopupMenu(this, anchor, Gravity.END);
        popup.getMenu().add(0, 1, 0, "Edit Novel Info");
        popup.getMenu().add(0, 2, 0, "Delete Novel Permanently");
        popup.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == 1) {
                // Switch to Info tab (index 2: Chapters=0, Drafts=1, Info=2)
                ViewPager2 pager = findViewById(R.id.view_pager);
                if (pager != null) pager.setCurrentItem(2, true);
                return true;
            } else if (item.getItemId() == 2) {
                confirmDelete();
                return true;
            }
            return false;
        });
        popup.show();
    }

    private void confirmDelete() {
        com.google.android.material.bottomsheet.BottomSheetDialog sheet =
            new com.google.android.material.bottomsheet.BottomSheetDialog(this, R.style.BottomSheetTheme);
        android.view.View root = getLayoutInflater().inflate(R.layout.bottom_sheet_delete_novel, null);

        android.widget.ImageView coverImg = root.findViewById(R.id.delete_cover_img);
        if (novel != null && novel.getCoverImageUrl() != null && !novel.getCoverImageUrl().isEmpty())
            com.bumptech.glide.Glide.with(this).load(novel.getCoverImageUrl())
                .placeholder(R.drawable.img_placeholder_cover).centerCrop().into(coverImg);

        String novelName = novel != null && novel.getTitle() != null ? novel.getTitle() : "this novel";
        ((android.widget.TextView) root.findViewById(R.id.delete_novel_body)).setText(
            "\"" + novelName + "\" will be permanently deleted — all chapters, analytics, and reader data will be gone forever. This cannot be undone.");

        root.findViewById(R.id.btn_delete_forever).setOnClickListener(v -> { sheet.dismiss(); executeDelete(); });
        root.findViewById(R.id.btn_keep_novel).setOnClickListener(v -> sheet.dismiss());
        sheet.setContentView(root);
        sheet.show();
    }

    private void executeDelete() {
        vm.deleteNovel(novelId, (success, error) -> runOnUiThread(() -> {
            if (success) {
                BannerConfig cfg = new BannerConfig()
                        .setType(BannerType.SUCCESS).setTitle("Deleted")
                        .setMessage(novel != null ? "\"" + novel.getTitle() + "\" removed." : "Novel deleted.")
                        .setDuration(3000);
                BannerManager.getInstance().queueForNextActivity(cfg);
                finish();
            } else {
                BannerHelper.error(this, "Delete failed", error != null ? error : "Unknown error");
            }
        }));
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1).toLowerCase();
    }

    private int dp(int v) { return Math.round(v * getResources().getDisplayMetrics().density); }

    // ── Pager adapter ─────────────────────────────────────────────────────────

    private static class PagerAdapter extends FragmentStateAdapter {
        private final String novelId;
        PagerAdapter(FragmentActivity fa, String novelId) {
            super(fa);
            this.novelId = novelId;
        }
        @NonNull @Override
        public Fragment createFragment(int pos) {
            switch (pos) {
                case 1:  return NovelDraftsFragment.newInstance(novelId);
                case 2:  return NovelInfoFragment.newInstance(novelId);
                default: return NovelChaptersFragment.newInstance(novelId);
            }
        }
        @Override public int getItemCount() { return 3; }
    }
    private int resolveAttrColor(int attr) {
        android.util.TypedValue tv = new android.util.TypedValue();
        getTheme().resolveAttribute(attr, tv, true);
        return tv.data;
    }

}

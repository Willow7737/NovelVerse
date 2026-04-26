package com.novelverse.app.presentation.profile.downloads;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.novelverse.app.R;
import com.novelverse.app.data.local.dao.NovelDao;
import com.novelverse.app.data.local.entities.NovelEntity;
import com.novelverse.app.presentation.home.HomeActivity;
import com.novelverse.app.presentation.novel.detail.NovelDetailActivity;

import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * Downloads screen — lists novels the user has saved for offline reading.
 * Observes Room {@link NovelDao#getDownloadedNovels()} so the list stays
 * up-to-date as downloads complete in the background.
 */
@AndroidEntryPoint
public class DownloadsActivity extends AppCompatActivity {

    @Inject NovelDao novelDao;

    private LinearLayout  emptyState;
    private RecyclerView  recyclerView;
    private TextView      tvStorageUsed;
    private ProgressBar   storageBar;

    private DownloadsAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_downloads);

        emptyState    = findViewById(R.id.empty_state_layout);
        recyclerView  = findViewById(R.id.downloads_recycler);
        tvStorageUsed = findViewById(R.id.tv_storage_used);
        storageBar    = findViewById(R.id.storage_progress);

        // Navigation
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        View btnBrowse = findViewById(R.id.btn_browse);
        if (btnBrowse != null) {
            btnBrowse.setOnClickListener(v -> {
                Intent i = new Intent(this, HomeActivity.class);
                i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(i);
                finish();
            });
        }

        // RecyclerView
        if (recyclerView != null) {
            adapter = new DownloadsAdapter(novel -> {
                Intent i = new Intent(this, NovelDetailActivity.class);
                i.putExtra(NovelDetailActivity.EXTRA_NOVEL_ID, novel.getId());
                startActivity(i);
            });
            recyclerView.setLayoutManager(new LinearLayoutManager(this));
            recyclerView.setAdapter(adapter);
        }

        // Observe downloaded novels from Room
        novelDao.getDownloadedNovels().observe(this, this::onDownloadsChanged);
    }

    private void onDownloadsChanged(List<NovelEntity> novels) {
        boolean empty = novels == null || novels.isEmpty();
        if (emptyState   != null) emptyState.setVisibility(empty ? View.VISIBLE : View.GONE);
        if (recyclerView != null) recyclerView.setVisibility(empty ? View.GONE : View.VISIBLE);

        if (!empty && adapter != null) {
            adapter.submitList(novels);
            updateStorageStats(novels);
        }
    }

    /** Calculates approximate download sizes and updates the storage bar. */
    private void updateStorageStats(List<NovelEntity> novels) {
        if (tvStorageUsed == null || storageBar == null) return;

        // Estimate: ~50 KB per chapter stored, average 30 chapters → ~1.5 MB/novel
        long estimatedBytes = (long) novels.size() * 30 * 50 * 1024;
        String label;
        if (estimatedBytes >= 1_048_576) {
            label = String.format("%.1f MB used", estimatedBytes / 1_048_576.0);
        } else {
            label = (estimatedBytes / 1024) + " KB used";
        }
        tvStorageUsed.setText(label);

        // Show up to 100 novels as ~100% of a "soft" 500 MB quota
        int progress = Math.min(100, (int) (novels.size() * 2));
        storageBar.setProgress(progress);
    }
}

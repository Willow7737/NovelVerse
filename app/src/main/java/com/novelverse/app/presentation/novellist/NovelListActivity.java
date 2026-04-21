package com.novelverse.app.presentation.novellist;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.novelverse.app.R;
import com.novelverse.app.presentation.common.adapters.NovelAdapter;
import com.novelverse.app.presentation.home.HomeViewModel;
import com.novelverse.app.presentation.novel.detail.NovelDetailActivity;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class NovelListActivity extends AppCompatActivity {

    public static final String EXTRA_SECTION = "section";
    public static final String EXTRA_TITLE   = "title";

    private HomeViewModel viewModel;
    private NovelAdapter  adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_novel_list);

        String section = getIntent().getStringExtra(EXTRA_SECTION);
        String title   = getIntent().getStringExtra(EXTRA_TITLE);
        if (title == null) title = "Novels";

        TextView tvTitle = findViewById(R.id.list_title);
        if (tvTitle != null) tvTitle.setText(title);
        ImageView btnBack = findViewById(R.id.btn_back);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        RecyclerView rv = findViewById(R.id.novel_list_recycler);

        // Single-column vertical list — full-width cards with cover left, details right
        adapter = new NovelAdapter(NovelAdapter.VIEW_TYPE_VERTICAL);
        adapter.setOnItemClickListener(novel -> {
            Intent i = new Intent(this, NovelDetailActivity.class);
            i.putExtra(NovelDetailActivity.EXTRA_NOVEL_ID, novel.getId());
            startActivity(i);
        });
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter(adapter);

        viewModel = new ViewModelProvider(this).get(HomeViewModel.class);
        observeSection(section);
    }

    private void observeSection(String section) {
        if (section == null) return;
        switch (section) {
            case "trending":
                viewModel.loadTrendingNovels();
                viewModel.getTrendingNovels().observe(this, novels -> { if (novels != null) adapter.submitList(novels); });
                break;
            case "new_releases":
                viewModel.loadNewReleases();
                viewModel.getNewReleases().observe(this, novels -> { if (novels != null) adapter.submitList(novels); });
                break;
            case "featured":
                viewModel.loadFeaturedNovels();
                viewModel.getFeaturedNovels().observe(this, novels -> { if (novels != null) adapter.submitList(novels); });
                break;
            case "continue":
                viewModel.loadContinueReading();
                viewModel.getContinueReadingNovels().observe(this, novels -> { if (novels != null) adapter.submitList(novels); });
                break;
            case "for_you":
                viewModel.loadForYou();
                viewModel.getForYouNovels().observe(this, novels -> { if (novels != null) adapter.submitList(novels); });
                break;
        }
    }
}

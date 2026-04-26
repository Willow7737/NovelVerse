package com.novelverse.app.presentation.author.dashboard;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.lifecycle.ViewModelProvider;

import com.novelverse.app.R;
import com.novelverse.app.domain.models.Novel;
import com.novelverse.app.domain.models.User;
import com.novelverse.app.presentation.auth.AuthViewModel;
import com.novelverse.app.presentation.author.analytics.AuthorAnalyticsActivity;
import com.novelverse.app.presentation.author.chaptermanager.ChapterEditorActivity;
import com.novelverse.app.presentation.author.novelmanager.MyWorksActivity;
import com.novelverse.app.presentation.base.BaseActivity;
import com.novelverse.app.presentation.write.WriterViewModel;

import java.util.List;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * Author Dashboard — central hub for content creators.
 * Provides quick-access navigation to Works, Analytics, Chapter Editor, and Earnings.
 */
@AndroidEntryPoint
public class AuthorDashboardActivity extends BaseActivity {

    private AuthViewModel  authVm;
    private WriterViewModel writerVm;

    private TextView tvGreeting;
    private TextView tvEarningsPreview;
    private TextView statNovels;
    private TextView statViews;
    private TextView statFollowers;

    @Override
    protected int getLayoutResId() {
        return R.layout.activity_author_dashboard;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        requireRole("author", "admin");
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void initViews() {
        tvGreeting       = findViewById(R.id.tv_author_greeting);
        tvEarningsPreview = findViewById(R.id.tv_earnings_preview);
        statNovels       = findViewById(R.id.stat_novels);
        statViews        = findViewById(R.id.stat_views);
        statFollowers    = findViewById(R.id.stat_followers);

        authVm   = new ViewModelProvider(this).get(AuthViewModel.class);
        writerVm = new ViewModelProvider(this).get(WriterViewModel.class);
    }

    @Override
    protected void initObservers() {
        authVm.getCurrentUser().observe(this, this::bindUser);
        writerVm.getMyNovels().observe(this, this::bindNovels);
    }

    @Override
    protected void initListeners() {
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        findViewById(R.id.btn_analytics).setOnClickListener(v ->
                startActivity(new Intent(this, AuthorAnalyticsActivity.class)));

        findViewById(R.id.row_my_works).setOnClickListener(v ->
                startActivity(new Intent(this, MyWorksActivity.class)));

        findViewById(R.id.row_analytics).setOnClickListener(v ->
                startActivity(new Intent(this, AuthorAnalyticsActivity.class)));

        findViewById(R.id.row_new_chapter).setOnClickListener(v ->
                startActivity(new Intent(this, ChapterEditorActivity.class)));

        // Earnings row — for now navigates to Analytics (earnings tab)
        findViewById(R.id.row_earnings).setOnClickListener(v ->
                startActivity(new Intent(this, AuthorAnalyticsActivity.class)));
    }

    // ── Data binding ──────────────────────────────────────────────────────────

    private void bindUser(User user) {
        if (user == null) return;
        String name = user.getDisplayName();
        if (name == null || name.isEmpty()) name = user.getUsername();
        tvGreeting.setText("Welcome back, " + name + "!");

        if (statFollowers != null) {
            statFollowers.setText(fmt(user.getFollowersCount()));
        }
        if (tvEarningsPreview != null) {
            tvEarningsPreview.setText(String.format("$%.2f available", user.getAvailableForPayout()));
        }
    }

    private void bindNovels(List<Novel> novels) {
        if (novels == null) return;

        if (statNovels != null) statNovels.setText(String.valueOf(novels.size()));

        long totalViews = 0;
        for (Novel n : novels) totalViews += n.getTotalViews();
        if (statViews != null) statViews.setText(fmt(totalViews));
    }

    /** Format a number with K / M suffix. */
    private String fmt(long n) {
        if (n >= 1_000_000) return String.format("%.1fM", n / 1_000_000.0);
        if (n >= 1_000)     return String.format("%.1fK", n / 1_000.0);
        return String.valueOf(n);
    }
}

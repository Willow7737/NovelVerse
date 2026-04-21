package com.novelverse.app.presentation.author.analytics;

import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.novelverse.app.R;
import com.novelverse.app.domain.models.Novel;
import com.novelverse.app.domain.models.User;
import com.novelverse.app.presentation.auth.AuthViewModel;
import com.novelverse.app.presentation.write.WriterViewModel;

import java.util.List;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class AuthorAnalyticsActivity extends AppCompatActivity {

    private AuthViewModel  authVm;
    private WriterViewModel writerVm;

    private TextView statViews, statReads, statFollowers,
                     statLikes, statComments, statEarnings, statPayout;
    private LinearLayout novelsList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_author_analytics);

        authVm   = new ViewModelProvider(this).get(AuthViewModel.class);
        writerVm = new ViewModelProvider(this).get(WriterViewModel.class);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        statViews     = findViewById(R.id.stat_total_views);
        statReads     = findViewById(R.id.stat_total_reads);
        statFollowers = findViewById(R.id.stat_followers);
        statLikes     = findViewById(R.id.stat_likes);
        statComments  = findViewById(R.id.stat_comments);
        statEarnings  = findViewById(R.id.stat_earnings);
        statPayout    = findViewById(R.id.stat_payout);
        novelsList    = findViewById(R.id.novels_list);

        authVm.getCurrentUser().observe(this, this::bindUser);
        writerVm.getMyNovels().observe(this, this::bindNovels);
    }

    private void bindUser(User u) {
        if (u == null) return;
        set(statFollowers, fmt(u.getFollowersCount()));
        set(statEarnings,  String.format("$%.2f", u.getTotalEarnings()));
        set(statPayout,    String.format("$%.2f", u.getAvailableForPayout()));
    }

    private void bindNovels(List<Novel> novels) {
        if (novels == null) return;
        long totalViews = 0; int totalLikes = 0; int totalComments = 0;
        for (Novel n : novels) {
            totalViews    += n.getTotalViews();
            totalLikes    += n.getTotalLikes();
            totalComments += n.getTotalComments();
        }
        set(statViews,    fmt(totalViews));
        set(statReads,    fmt(novels.size() > 0 ? totalViews / novels.size() : 0));
        set(statLikes,    fmt(totalLikes));
        set(statComments, fmt(totalComments));

        // Per-novel rows
        if (novelsList != null) {
            novelsList.removeAllViews();
            for (int i = 0; i < novels.size(); i++) {
                Novel n = novels.get(i);
                LinearLayout row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setPadding(dp(16), dp(14), dp(16), dp(14));
                row.setBackground(getDrawable(android.R.color.transparent));

                TextView title = new TextView(this);
                title.setText(n.getTitle());
                title.setTextColor(getColor(R.color.text_primary));
                title.setTextSize(15f);
                title.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
                row.addView(title);

                TextView meta = new TextView(this);
                meta.setText(fmt(n.getTotalViews()) + " views");
                meta.setTextColor(getColor(R.color.primary));
                meta.setTextSize(14f);
                row.addView(meta);
                novelsList.addView(row);

                if (i < novels.size() - 1) {
                    android.view.View divider = new android.view.View(this);
                    divider.setBackgroundColor(getColor(R.color.divider));
                    divider.setLayoutParams(new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT, 1));
                    novelsList.addView(divider);
                }
            }
        }
    }

    private void set(TextView tv, String val) { if (tv != null) tv.setText(val); }
    private String fmt(long n) {
        if (n >= 1_000_000) return String.format("%.1fM", n / 1_000_000f);
        if (n >= 1_000)     return String.format("%.1fk", n / 1_000f);
        return String.valueOf(n);
    }
    private int dp(int v) { return Math.round(v * getResources().getDisplayMetrics().density); }
}

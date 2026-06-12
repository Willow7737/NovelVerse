package com.novelverse.app.presentation.profile.follows;

import android.os.Bundle;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.novelverse.app.R;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class FollowsActivity extends AppCompatActivity {

    private static final String TAG = "FollowsActivity";

    public static final String EXTRA_USER_ID     = "user_id";
    public static final String EXTRA_INITIAL_TAB = "initial_tab";
    public static final int    TAB_FOLLOWERS     = 0;
    public static final int    TAB_FOLLOWING     = 1;

    private TabLayout        tabLayout;
    private FollowsViewModel viewModel;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_follows);

        String userId  = getIntent().getStringExtra(EXTRA_USER_ID);
        int initialTab = getIntent().getIntExtra(EXTRA_INITIAL_TAB, TAB_FOLLOWERS);

        if (userId == null || userId.isEmpty()) {
            Log.e(TAG, "FollowsActivity launched without EXTRA_USER_ID — finishing.");
            finish();
            return;
        }

        // Toolbar
        com.google.android.material.appbar.MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        // ViewPager + tabs
        ViewPager2 viewPager = findViewById(R.id.follows_view_pager);
        tabLayout = findViewById(R.id.follows_tab_layout);

        FollowsPagerAdapter adapter = new FollowsPagerAdapter(this, userId);
        viewPager.setAdapter(adapter);

        // Initial tab labels — will be updated once counts arrive
        new TabLayoutMediator(tabLayout, viewPager, (tab, position) ->
                tab.setText(position == TAB_FOLLOWERS ? "Followers" : "Following")
        ).attach();

        viewPager.setCurrentItem(initialTab, false);

        // Activity-scoped ViewModel — shared with FollowsPageFragment instances
        viewModel = new ViewModelProvider(this).get(FollowsViewModel.class);
        viewModel.loadAll(userId);

        // Update tab labels as soon as the counts are available
        viewModel.getFollowersCount().observe(this, count -> {
            if (count != null) updateTabLabel(TAB_FOLLOWERS, "Followers", count);
        });
        viewModel.getFollowingCount().observe(this, count -> {
            if (count != null) updateTabLabel(TAB_FOLLOWING, "Following", count);
        });
    }

    /**
     * Rewrites a tab's text to include the live count, e.g. "Followers (42)".
     */
    private void updateTabLabel(int position, String base, int count) {
        TabLayout.Tab tab = tabLayout.getTabAt(position);
        if (tab != null) {
            tab.setText(base + " (" + count + ")");
        }
    }
}

package com.novelverse.app.presentation.profile.achievements;

import android.os.Bundle;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;

import com.novelverse.app.R;
import com.novelverse.app.presentation.profile.ProfileAchievementsFragment;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * Full-screen Achievements screen.
 *
 * Hosts {@link ProfileAchievementsFragment} which owns:
 *   - category filter chips (All / Reader / Writer / Streak / Social / Supporter)
 *   - RecyclerView of achievement cards loaded from Supabase via GameAssets
 *   - empty state when no achievements match the active filter
 *
 * Navigation: launched from ProfileFragment's achievements preview card.
 * Back: standard Up navigation via the toolbar back arrow.
 */
@AndroidEntryPoint
public class AchievementsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_achievements);

        // Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar_achievements);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setTitle("Achievements");
        }

        // Inject the fragment only on first create, not on config change
        if (savedInstanceState == null) {
            getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.achievements_fragment_container, new ProfileAchievementsFragment())
                .commit();
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}

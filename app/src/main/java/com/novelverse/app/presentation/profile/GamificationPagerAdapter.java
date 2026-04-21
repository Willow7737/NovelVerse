package com.novelverse.app.presentation.profile;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

/**
 * ViewPager2 adapter for the three gamification sub-tabs on the Profile screen.
 * Tab 0: Stats      — XP, level, currency, streak
 * Tab 1: Achievements — full achievement catalog with filter chips
 * Tab 2: Collection  — badge slots, avatar frames, profile themes
 */
public class GamificationPagerAdapter extends FragmentStateAdapter {

    public GamificationPagerAdapter(@NonNull FragmentActivity activity) {
        super(activity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 1:  return new ProfileAchievementsFragment();
            case 2:  return new ProfileCollectionFragment();
            default: return new ProfileStatsFragment();
        }
    }

    @Override
    public int getItemCount() {
        return 3;
    }
}

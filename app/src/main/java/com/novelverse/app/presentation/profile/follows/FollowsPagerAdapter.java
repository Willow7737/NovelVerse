package com.novelverse.app.presentation.profile.follows;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class FollowsPagerAdapter extends FragmentStateAdapter {

    private final String userId;

    public FollowsPagerAdapter(@NonNull FragmentActivity fragmentActivity, String userId) {
        super(fragmentActivity);
        this.userId = userId;
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        boolean isFollowers = position == FollowsActivity.TAB_FOLLOWERS;
        return FollowsPageFragment.newInstance(userId, isFollowers);
    }

    @Override
    public int getItemCount() {
        return 2;
    }
}

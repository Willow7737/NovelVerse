package com.novelverse.app.presentation.library;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Lifecycle;
import androidx.viewpager2.adapter.FragmentStateAdapter;

/**
 * Provides one {@link LibraryPageFragment} per library tab.
 * Tabs and their Supabase status keys are co-located here so there is
 * a single source of truth — {@link LibraryFragment} and the adapter
 * both read from {@link #getTabs()}.
 */
public class LibraryPagerAdapter extends FragmentStateAdapter {

    static final String[] TABS     = {"Reading", "Completed", "Plan to Read", "Favorites", "Downloads"};
    static final String[] STATUSES = {"reading", "completed", "plan_to_read", "favorites", "downloads"};

    public LibraryPagerAdapter(@NonNull FragmentManager fm, @NonNull Lifecycle lifecycle) {
        super(fm, lifecycle);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        return LibraryPageFragment.newInstance(STATUSES[position], position);
    }

    @Override
    public int getItemCount() {
        return TABS.length;
    }

    /** Tab labels in display order. */
    public static String[] getTabs() {
        return TABS;
    }
}

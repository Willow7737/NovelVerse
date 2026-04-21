package com.novelverse.app.presentation.library;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.novelverse.app.R;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * Library screen host.
 *
 * <p>Owns the {@link TabLayout} + {@link ViewPager2} pair. Each tab's data
 * fetching and UI lives in its own {@link LibraryPageFragment} so this class
 * stays thin — its only job is wiring the two widgets together.
 */
@AndroidEntryPoint
public class LibraryFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_library, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        TabLayout  tabs  = view.findViewById(R.id.library_tab_layout);
        ViewPager2 pager = view.findViewById(R.id.library_view_pager);

        LibraryPagerAdapter adapter =
                new LibraryPagerAdapter(getChildFragmentManager(), getLifecycle());
        pager.setAdapter(adapter);

        // Reduce over-eager pre-loading — only keep adjacent pages alive
        pager.setOffscreenPageLimit(1);

        new TabLayoutMediator(tabs, pager,
                (tab, position) -> tab.setText(LibraryPagerAdapter.getTabs()[position])
        ).attach();
    }
}

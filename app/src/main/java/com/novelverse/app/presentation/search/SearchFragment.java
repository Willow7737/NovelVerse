package com.novelverse.app.presentation.search;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.ChipGroup;
import com.novelverse.app.R;
import com.novelverse.app.data.local.preferences.UserPreferences;
import com.novelverse.app.data.repository.SearchRepository;
import com.novelverse.app.domain.models.Novel;
import com.novelverse.app.domain.models.TrendingItem;
import com.novelverse.app.presentation.common.adapters.NovelAdapter;
import com.novelverse.app.presentation.novel.detail.NovelDetailActivity;

import java.util.List;

import javax.inject.Inject;
import dagger.hilt.android.AndroidEntryPoint;

/**
 * Search screen — Tasks 18 (state preservation), 19 (debounce),
 * 20 (filter sheet), 21 (mode chips), 22 (trending real-time),
 * 42 (genre grid RecyclerView).
 */
@AndroidEntryPoint
public class SearchFragment extends Fragment {

    @Inject SearchRepository searchRepository;
    @Inject UserPreferences  userPreferences;

    private SearchViewModel viewModel;

    // Task 19: debounce
    private final Handler debounceHandler = new Handler(Looper.getMainLooper());
    private Runnable debounceRunnable;

    // Views
    private EditText     searchInput;
    private LinearLayout sectionRecent;
    private LinearLayout trendingContainer;
    private LinearLayout resultsSection;
    private LinearLayout emptyResults;
    private TextView     resultsLabel;
    private RecyclerView searchResultsRv;
    private RecyclerView genreGridRv;       // Task 42
    private ChipGroup    searchModeChips;   // Task 21
    private View         filterBtn;         // Task 20
    private TextView     filterBadge;       // Task 20
    private NovelAdapter resultsAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_search, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(SearchViewModel.class);

        bindViews(view);
        setupResultsAdapter();
        setupGenreGrid();    // Task 42
        setupSearchInput();  // Task 19
        setupModeChips();    // Task 21
        setupFilterBtn();    // Task 20
        restoreSearchState();// Task 18
        observeViewModel();
        viewModel.loadTrending(); // Task 22
    }

    private void bindViews(View v) {
        searchInput       = v.findViewById(R.id.search_input);
        sectionRecent     = v.findViewById(R.id.section_recent);
        trendingContainer = v.findViewById(R.id.trending_container);
        resultsSection    = v.findViewById(R.id.results_section);
        emptyResults      = v.findViewById(R.id.empty_results);
        resultsLabel      = v.findViewById(R.id.results_label);
        searchResultsRv   = v.findViewById(R.id.search_results_rv);
        genreGridRv       = v.findViewById(R.id.genre_grid_rv);
        searchModeChips   = v.findViewById(R.id.search_mode_chips);
        filterBtn         = v.findViewById(R.id.btn_search_filter);
        filterBadge       = v.findViewById(R.id.filter_badge_count);
    }

    private void setupResultsAdapter() {
        resultsAdapter = new NovelAdapter(NovelAdapter.VIEW_TYPE_VERTICAL);
        resultsAdapter.setOnItemClickListener(novel -> {
            android.content.Intent i = new android.content.Intent(requireContext(), NovelDetailActivity.class);
            i.putExtra(NovelDetailActivity.EXTRA_NOVEL_ID, novel.getId());
            startActivity(i);
        });
        if (searchResultsRv != null) {
            searchResultsRv.setLayoutManager(new LinearLayoutManager(requireContext()));
            searchResultsRv.setAdapter(resultsAdapter);
        }
    }

    /** Task 42: Genre grid via RecyclerView with GridLayoutManager(2) */
    private void setupGenreGrid() {
        if (genreGridRv == null) return;
        genreGridRv.setLayoutManager(new GridLayoutManager(requireContext(), 2));
        genreGridRv.setNestedScrollingEnabled(false);
        genreGridRv.setAdapter(new GenreAdapter(genre -> {
            if (searchInput != null) {
                searchInput.setText(genre);
                viewModel.search(genre);
            }
        }));
    }

    /** Task 19: Debounced search input */
    private void setupSearchInput() {
        if (searchInput == null) return;
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {
                if (debounceRunnable != null) debounceHandler.removeCallbacks(debounceRunnable);
                debounceRunnable = () -> {
                    String q = s.toString().trim();
                    if (q.length() >= 2) viewModel.search(q);
                    else showDefaultState();
                };
                debounceHandler.postDelayed(debounceRunnable, 300);
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    /** Task 21: Search mode chips */
    private void setupModeChips() {
        if (searchModeChips == null) return;
        searchModeChips.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) return;
            int id = checkedIds.get(0);
            if (id == R.id.chip_mode_novels)     viewModel.setSearchMode(SearchViewModel.SearchMode.NOVELS);
            else if (id == R.id.chip_mode_authors) viewModel.setSearchMode(SearchViewModel.SearchMode.AUTHORS);
            else                                  viewModel.setSearchMode(SearchViewModel.SearchMode.CHARACTERS);
            // Re-run current query in new mode
            String q = viewModel.getCurrentQuery().getValue();
            if (q != null && !q.isEmpty()) viewModel.search(q);
        });
    }

    /** Task 20: Filter button */
    private void setupFilterBtn() {
        if (filterBtn == null) return;
        filterBtn.setOnClickListener(v -> {
            SearchFilterBottomSheet sheet = new SearchFilterBottomSheet();
            SearchFilterBottomSheet.SearchFilter current = viewModel.getActiveFilter().getValue();
            if (current != null) sheet.setCurrentFilter(current);
            sheet.setOnApplyListener(filter -> {
                viewModel.applyFilter(filter);
                updateFilterBadge(filter);
            });
            sheet.show(getChildFragmentManager(), "search_filter");
        });
    }

    private void updateFilterBadge(SearchFilterBottomSheet.SearchFilter filter) {
        if (filterBadge == null) return;
        int count = filter != null ? filter.activeFilterCount() : 0;
        filterBadge.setVisibility(count > 0 ? View.VISIBLE : View.GONE);
        filterBadge.setText(String.valueOf(count));
    }

    /** Task 18: Restore state from ViewModel after back navigation */
    private void restoreSearchState() {
        String saved = viewModel.getCurrentQuery().getValue();
        if (saved != null && !saved.isEmpty()) {
            if (searchInput != null) searchInput.setText(saved);
            List<Novel> results = viewModel.getCurrentResults().getValue();
            if (results != null && !results.isEmpty()) showResults(results);
        }
    }

    private void observeViewModel() {
        viewModel.getCurrentResults().observe(getViewLifecycleOwner(), this::showResults);
        viewModel.getIsLoading().observe(getViewLifecycleOwner(), loading -> {
            // TODO: show shimmer while loading
        });
        // Task 22: Trending items
        viewModel.getTrendingItems().observe(getViewLifecycleOwner(), items -> {
            if (trendingContainer == null || items == null) return;
            trendingContainer.removeAllViews();
            for (TrendingItem item : items) {
                TextView row = new TextView(requireContext());
                row.setPadding(dp(16), dp(14), dp(16), dp(14));
                row.setTextColor(0xFF0F172A);
                row.setTextSize(15f);
                String arrow = item.getTrendArrow();
                row.setText("#" + item.getRank() + "  " + item.getQuery() + "  " + arrow);
                row.setOnClickListener(v -> {
                    if (searchInput != null) searchInput.setText(item.getQuery());
                    viewModel.search(item.getQuery());
                });
                trendingContainer.addView(row);
            }
        });
    }

    private void showResults(@Nullable List<Novel> results) {
        if (resultsSection == null) return;
        boolean hasResults = results != null && !results.isEmpty();
        resultsSection.setVisibility(View.VISIBLE);
        if (sectionRecent != null) sectionRecent.setVisibility(View.GONE);
        if (trendingContainer != null) trendingContainer.setVisibility(View.GONE);
        if (emptyResults != null) emptyResults.setVisibility(hasResults ? View.GONE : View.VISIBLE);
        if (searchResultsRv != null) searchResultsRv.setVisibility(hasResults ? View.VISIBLE : View.GONE);
        if (hasResults) resultsAdapter.submitList(results);
    }

    private void showDefaultState() {
        if (resultsSection != null) resultsSection.setVisibility(View.GONE);
        if (sectionRecent  != null) sectionRecent.setVisibility(View.VISIBLE);
        if (trendingContainer != null) trendingContainer.setVisibility(View.VISIBLE);
    }

    private int dp(int v) { return Math.round(v * getResources().getDisplayMetrics().density); }

    @Override public void onDestroyView() {
        super.onDestroyView();
        if (debounceRunnable != null) debounceHandler.removeCallbacks(debounceRunnable);
    }
}

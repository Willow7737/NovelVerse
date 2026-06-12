package com.novelverse.app.presentation.search;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.novelverse.app.R;
import com.novelverse.app.domain.models.Novel;
import com.novelverse.app.presentation.common.adapters.NovelAdapter;
import com.novelverse.app.presentation.novel.detail.NovelDetailActivity;

import java.util.ArrayList;
import java.util.List;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * Full-screen Search activity — launched from the header search icon.
 * Delegates all search logic to SearchViewModel / SearchRepository.
 */
@AndroidEntryPoint
public class SearchActivity extends AppCompatActivity {

    // ── Debounce ──────────────────────────────────────────────────────────
    private final Handler debounceHandler = new Handler(Looper.getMainLooper());
    private Runnable debounceRunnable;
    private static final long DEBOUNCE_MS = 300;

    // ── ViewModel ─────────────────────────────────────────────────────────
    private SearchViewModel viewModel;

    // ── Adapter ───────────────────────────────────────────────────────────
    private NovelAdapter resultsAdapter;

    // ── Recent searches (in-memory) ───────────────────────────────────────
    private final List<String> recentSearches = new ArrayList<>();

    // ── Views ─────────────────────────────────────────────────────────────
    private EditText      searchInput;
    private LinearLayout  recentChipsContainer;
    private LinearLayout  sectionRecent;
    private LinearLayout  trendingContainer;
    private LinearLayout  genreGrid;
    private LinearLayout  resultsSection;
    private LinearLayout  emptyResults;
    private RecyclerView  searchResultsRv;
    private TextView      resultsLabel;

    // ── Hardcoded discovery content ───────────────────────────────────────
    private static final String[] TRENDING = {
        "Reincarnation Saga", "Magic Academy", "Second Chance Love",
        "Sword Saint",  "Moon Empress", "CEO Romance"
    };

    private static final String[][] GENRES = {
        {"Fantasy",   "#9B59B6"}, {"Romance",   "#FF6B9D"},
        {"Sci-Fi",    "#3498DB"}, {"Mystery",   "#E74C3C"},
        {"Thriller",  "#F39C12"}, {"Adventure", "#27AE60"},
        {"Horror",    "#2C3E50"}, {"Comedy",    "#F1C40F"},
        {"Action",    "#C0392B"}, {"Drama",     "#E67E22"},
    };

    // ─────────────────────────────────────────────────────────────────────

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        viewModel = new ViewModelProvider(this).get(SearchViewModel.class);

        bindViews();
        setupResultsAdapter();
        setupSearchInput();
        buildTrending();
        buildGenreGrid();
        updateRecentChips();
        observeViewModel();

        // Pre-load trending from Supabase (real data, not hardcoded)
        viewModel.loadTrending();

        searchInput.requestFocus();
    }

    // ── View binding ──────────────────────────────────────────────────────

    private void bindViews() {
        searchInput          = findViewById(R.id.search_input);
        recentChipsContainer = findViewById(R.id.recent_chips_container);
        sectionRecent        = findViewById(R.id.section_recent);
        trendingContainer    = findViewById(R.id.trending_container);
        genreGrid            = findViewById(R.id.genre_grid);
        resultsSection       = findViewById(R.id.results_section);
        emptyResults         = findViewById(R.id.empty_results);
        searchResultsRv      = findViewById(R.id.search_results_rv);
        resultsLabel         = findViewById(R.id.results_label);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        TextView clearBtn = findViewById(R.id.btn_clear_recent);
        if (clearBtn != null) clearBtn.setOnClickListener(v -> {
            recentSearches.clear();
            updateRecentChips();
        });
    }

    // ── Results adapter ───────────────────────────────────────────────────

    private void setupResultsAdapter() {
        resultsAdapter = new NovelAdapter(NovelAdapter.VIEW_TYPE_VERTICAL);
        resultsAdapter.setOnItemClickListener(novel -> {
            Intent i = new Intent(this, NovelDetailActivity.class);
            i.putExtra(NovelDetailActivity.EXTRA_NOVEL_ID, novel.getId());
            startActivity(i);
        });
        if (searchResultsRv != null) {
            searchResultsRv.setLayoutManager(new LinearLayoutManager(this));
            searchResultsRv.setNestedScrollingEnabled(false);
            searchResultsRv.setAdapter(resultsAdapter);
        }
    }

    // ── Debounced search input ────────────────────────────────────────────

    private void setupSearchInput() {
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void afterTextChanged(Editable s) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {
                String query = s.toString().trim();
                if (debounceRunnable != null) debounceHandler.removeCallbacks(debounceRunnable);

                if (query.isEmpty()) {
                    showDiscoveryState();
                    return;
                }
                // Show results section immediately with loading state
                showResultsState(query, null);

                debounceRunnable = () -> viewModel.search(query);
                debounceHandler.postDelayed(debounceRunnable, DEBOUNCE_MS);
            }
        });

        searchInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                    (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                String q = searchInput.getText().toString().trim();
                if (!q.isEmpty()) {
                    if (debounceRunnable != null) debounceHandler.removeCallbacks(debounceRunnable);
                    addToRecentSearches(q);
                    viewModel.search(q);
                }
                return true;
            }
            return false;
        });
    }

    // ── Observe ViewModel ─────────────────────────────────────────────────

    private void observeViewModel() {
        viewModel.getCurrentResults().observe(this, results -> {
            String q = viewModel.getCurrentQuery().getValue();
            showResultsState(q, results);
        });

        // Replace hardcoded trending with real Supabase data
        viewModel.getTrendingItems().observe(this, items -> {
            if (trendingContainer == null || items == null || items.isEmpty()) return;
            trendingContainer.removeAllViews();
            for (int i = 0; i < items.size(); i++) {
                com.novelverse.app.domain.models.TrendingItem item = items.get(i);
                final String term = item.getQuery();
                TextView row = new TextView(this);
                row.setPadding(dp(16), dp(14), dp(16), dp(14));
                row.setTextColor(resolveAttrColor(android.R.attr.textColorPrimary));
                row.setTextSize(15f);
                row.setText("#" + item.getRank() + "  " + term + "  " + item.getTrendArrow());
                row.setOnClickListener(v -> {
                    searchInput.setText(term);
                    searchInput.setSelection(term.length());
                    addToRecentSearches(term);
                    viewModel.search(term);
                });
                trendingContainer.addView(row);
                if (i < items.size() - 1) {
                    View div = new View(this);
                    div.setBackgroundColor(Color.parseColor("#E2E8F0"));
                    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT, 1);
                    lp.setMarginStart(dp(16));
                    trendingContainer.addView(div, lp);
                }
            }
        });
    }

    // ── UI state helpers ──────────────────────────────────────────────────

    private void showDiscoveryState() {
        if (resultsSection != null) resultsSection.setVisibility(View.GONE);
        if (sectionRecent != null)  sectionRecent.setVisibility(
                recentSearches.isEmpty() ? View.GONE : View.VISIBLE);
        if (trendingContainer != null) trendingContainer.setVisibility(View.VISIBLE);
        if (genreGrid != null) genreGrid.setVisibility(View.VISIBLE);
    }

    private void showResultsState(String query, List<Novel> results) {
        if (resultsSection == null) return;

        resultsSection.setVisibility(View.VISIBLE);
        if (sectionRecent != null)     sectionRecent.setVisibility(View.GONE);
        if (trendingContainer != null) trendingContainer.setVisibility(View.GONE);
        if (genreGrid != null)         genreGrid.setVisibility(View.GONE);

        if (resultsLabel != null && query != null && !query.isEmpty()) {
            resultsLabel.setText("Results for \u201C" + query + "\u201D");
        }

        if (results == null) {
            // Still loading — show neither list nor empty state yet
            if (searchResultsRv != null) searchResultsRv.setVisibility(View.GONE);
            if (emptyResults != null)    emptyResults.setVisibility(View.GONE);
            return;
        }

        boolean hasResults = !results.isEmpty();
        if (searchResultsRv != null) searchResultsRv.setVisibility(hasResults ? View.VISIBLE : View.GONE);
        if (emptyResults != null)    emptyResults.setVisibility(hasResults ? View.GONE : View.VISIBLE);
        if (hasResults) resultsAdapter.submitList(results);
    }

    // ── Recent searches ───────────────────────────────────────────────────

    private void addToRecentSearches(String query) {
        if (!recentSearches.contains(query)) {
            recentSearches.add(0, query);
            if (recentSearches.size() > 8) recentSearches.remove(recentSearches.size() - 1);
        }
        updateRecentChips();
    }

    private void updateRecentChips() {
        if (recentChipsContainer == null) return;
        recentChipsContainer.removeAllViews();
        if (recentSearches.isEmpty()) {
            if (sectionRecent != null) sectionRecent.setVisibility(View.GONE);
            return;
        }
        if (sectionRecent != null) sectionRecent.setVisibility(View.VISIBLE);
        for (String term : recentSearches) {
            TextView chip = new TextView(this);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.rightMargin = dp(8);
            chip.setLayoutParams(lp);
            chip.setText(term);
            chip.setTextColor(Color.parseColor("#6366F1"));
            chip.setTextSize(16f);
            chip.setPadding(dp(14), dp(7), dp(14), dp(7));
            GradientDrawable bg = new GradientDrawable();
            bg.setColor(Color.parseColor("#EEF2FF"));
            bg.setCornerRadius(dp(100));
            chip.setBackground(bg);
            chip.setOnClickListener(v -> {
                searchInput.setText(term);
                searchInput.setSelection(term.length());
                viewModel.search(term);
            });
            recentChipsContainer.addView(chip);
        }
    }

    // ── Discovery content ─────────────────────────────────────────────────

    private void buildTrending() {
        if (trendingContainer == null) return;
        trendingContainer.removeAllViews();
        for (int i = 0; i < TRENDING.length; i++) {
            final String term = TRENDING[i].replaceAll("^[^a-zA-Z]+", "").trim();
            TextView row = new TextView(this);
            row.setText((i + 1) + ".  " + TRENDING[i]);
            row.setTextColor(resolveAttrColor(android.R.attr.textColorPrimary));
            row.setTextSize(16f);
            row.setPadding(dp(16), dp(14), dp(16), dp(14));
            row.setOnClickListener(v -> {
                searchInput.setText(term);
                searchInput.setSelection(term.length());
                addToRecentSearches(term);
                viewModel.search(term);
            });
            trendingContainer.addView(row);
            if (i < TRENDING.length - 1) {
                View div = new View(this);
                div.setBackgroundColor(Color.parseColor("#E2E8F0"));
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, 1);
                lp.setMarginStart(dp(16));
                trendingContainer.addView(div, lp);
            }
        }
    }

    private void buildGenreGrid() {
        if (genreGrid == null) return;
        genreGrid.removeAllViews();
        for (int i = 0; i < GENRES.length; i += 2) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            rowLp.bottomMargin = dp(8);
            row.setLayoutParams(rowLp);
            row.addView(makeGenreChip(GENRES[i][0], GENRES[i][1], true));
            if (i + 1 < GENRES.length) row.addView(makeGenreChip(GENRES[i + 1][0], GENRES[i + 1][1], false));
            genreGrid.addView(row);
        }
    }

    private View makeGenreChip(String label, String hex, boolean marginEnd) {
        TextView chip = new TextView(this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(44), 1f);
        if (marginEnd) lp.rightMargin = dp(8);
        chip.setLayoutParams(lp);
        chip.setText(label);
        chip.setGravity(android.view.Gravity.CENTER);
        chip.setTextColor(Color.WHITE);
        chip.setTextSize(16f);
        chip.setTypeface(chip.getTypeface(), android.graphics.Typeface.BOLD);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.parseColor(hex));
        bg.setCornerRadius(dp(12));
        chip.setBackground(bg);
        chip.setOnClickListener(v -> {
            searchInput.setText(label);
            searchInput.setSelection(label.length());
            addToRecentSearches(label);
            viewModel.search(label);
        });
        return chip;
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (debounceRunnable != null) debounceHandler.removeCallbacks(debounceRunnable);
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }

    private int resolveAttrColor(int attr) {
        android.util.TypedValue tv = new android.util.TypedValue();
        getTheme().resolveAttribute(attr, tv, true);
        return tv.data;
    }
}

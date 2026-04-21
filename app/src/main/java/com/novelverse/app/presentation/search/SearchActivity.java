package com.novelverse.app.presentation.search;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.novelverse.app.R;

import java.util.ArrayList;
import java.util.List;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * Full-screen Search activity — launched from the header bell icon.
 * Owns its own toolbar with back navigation and inline search bar.
 */
@AndroidEntryPoint
public class SearchActivity extends AppCompatActivity {

    private final List<String> recentSearches = new ArrayList<>();

    private EditText searchInput;
    private LinearLayout recentChipsContainer;
    private LinearLayout sectionRecent;
    private LinearLayout trendingContainer;
    private LinearLayout genreGrid;
    private LinearLayout resultsSection;
    private LinearLayout emptyResults;
    private TextView resultsLabel;

    private static final String[] TRENDING = {
        "\uD83D\uDD25  Reincarnation Saga", "✨  Magic Academy", "\uD83D\uDC94  Second Chance Love",
        "\uD83D\uDDE1\uFE0F  Sword Saint", "\uD83C\uDF19  Moon Empress", "\uD83D\uDC51  CEO Romance"
    };

    private static final String[][] GENRES = {
        {"Fantasy", "#9B59B6"}, {"Romance", "#FF6B9D"},
        {"Sci-Fi",  "#3498DB"}, {"Mystery", "#E74C3C"},
        {"Thriller","#F39C12"}, {"Adventure","#27AE60"},
        {"Horror",  "#2C3E50"}, {"Comedy",  "#F1C40F"},
        {"Action",  "#C0392B"}, {"Drama",   "#E67E22"},
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        searchInput          = findViewById(R.id.search_input);
        recentChipsContainer = findViewById(R.id.recent_chips_container);
        sectionRecent        = findViewById(R.id.section_recent);
        trendingContainer    = findViewById(R.id.trending_container);
        genreGrid            = findViewById(R.id.genre_grid);
        resultsSection       = findViewById(R.id.results_section);
        emptyResults         = findViewById(R.id.empty_results);
        resultsLabel         = findViewById(R.id.results_label);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        TextView clearBtn = findViewById(R.id.btn_clear_recent);
        if (clearBtn != null) clearBtn.setOnClickListener(v -> {
            recentSearches.clear();
            updateRecentChips();
        });

        buildTrending();
        buildGenreGrid();
        updateRecentChips();

        searchInput.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            public void afterTextChanged(Editable s) {}
            public void onTextChanged(CharSequence s, int st, int b, int c) {
                onQueryChanged(s.toString().trim());
            }
        });

        searchInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                    (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                String q = searchInput.getText().toString().trim();
                if (!q.isEmpty()) submitSearch(q);
                return true;
            }
            return false;
        });

        searchInput.requestFocus();
    }

    private void onQueryChanged(String query) {
        boolean has = !query.isEmpty();
        resultsSection.setVisibility(has ? android.view.View.VISIBLE : android.view.View.GONE);
        sectionRecent.setVisibility(has ? android.view.View.GONE : android.view.View.VISIBLE);
        trendingContainer.setVisibility(has ? android.view.View.GONE : android.view.View.VISIBLE);
        genreGrid.setVisibility(has ? android.view.View.GONE : android.view.View.VISIBLE);
        if (has) {
            resultsLabel.setText("Results for \u201C" + query + "\u201D");
            emptyResults.setVisibility(android.view.View.VISIBLE);
        }
    }

    private void submitSearch(String query) {
        if (!recentSearches.contains(query)) {
            recentSearches.add(0, query);
            if (recentSearches.size() > 8) recentSearches.remove(recentSearches.size() - 1);
        }
        updateRecentChips();
    }

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
            trendingContainer.addView(row);
            if (i < TRENDING.length - 1) {
                android.view.View div = new android.view.View(this);
                div.setBackgroundColor(Color.parseColor("#E2E8F0"));
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, 1);
                lp.setMarginStart(dp(16));
                trendingContainer.addView(div, lp);
            }
            row.setOnClickListener(v -> { searchInput.setText(term); searchInput.setSelection(term.length()); submitSearch(term); });
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
            if (i + 1 < GENRES.length) row.addView(makeGenreChip(GENRES[i+1][0], GENRES[i+1][1], false));
            genreGrid.addView(row);
        }
    }

    private android.view.View makeGenreChip(String label, String hex, boolean marginEnd) {
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
        chip.setOnClickListener(v -> { searchInput.setText(label); searchInput.setSelection(label.length()); submitSearch(label); });
        return chip;
    }

    private void updateRecentChips() {
        if (recentChipsContainer == null) return;
        recentChipsContainer.removeAllViews();
        if (recentSearches.isEmpty()) { sectionRecent.setVisibility(android.view.View.GONE); return; }
        sectionRecent.setVisibility(android.view.View.VISIBLE);
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
            chip.setOnClickListener(v -> { searchInput.setText(term); searchInput.setSelection(term.length()); submitSearch(term); });
            recentChipsContainer.addView(chip);
        }
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

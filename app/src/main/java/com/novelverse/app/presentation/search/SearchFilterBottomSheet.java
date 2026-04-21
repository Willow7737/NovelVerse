package com.novelverse.app.presentation.search;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.chip.ChipGroup;
import com.novelverse.app.R;

import java.util.function.Consumer;

/** Task 20: Search filter bottom sheet */
public class SearchFilterBottomSheet extends BottomSheetDialogFragment {

    public static class SearchFilter {
        public String status       = "All";
        public float  minRating    = 0f;
        public String chapterRange = "Any";
        public String contentRating= "All";

        public int activeFilterCount() {
            int c = 0;
            if (!"All".equals(status)) c++;
            if (minRating > 0f) c++;
            if (!"Any".equals(chapterRange)) c++;
            if (!"All".equals(contentRating)) c++;
            return c;
        }

        public SearchFilter copy() {
            SearchFilter f = new SearchFilter();
            f.status = status; f.minRating = minRating;
            f.chapterRange = chapterRange; f.contentRating = contentRating;
            return f;
        }
    }

    private Consumer<SearchFilter> applyListener;
    private SearchFilter currentFilter = new SearchFilter();

    public void setOnApplyListener(Consumer<SearchFilter> listener) {
        this.applyListener = listener;
    }

    public void setCurrentFilter(SearchFilter f) {
        if (f != null) this.currentFilter = f.copy();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_search_filters, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ChipGroup statusGroup    = view.findViewById(R.id.chip_group_status);
        ChipGroup ratingGroup    = view.findViewById(R.id.chip_group_rating);
        ChipGroup chaptersGroup  = view.findViewById(R.id.chip_group_chapters);
        ChipGroup contentGroup   = view.findViewById(R.id.chip_group_content);

        view.findViewById(R.id.btn_apply_filters).setOnClickListener(v -> {
            // Read selected chips
            if (statusGroup   != null) currentFilter.status        = getSelectedChipText(statusGroup);
            if (ratingGroup   != null) currentFilter.minRating     = parseRating(getSelectedChipText(ratingGroup));
            if (chaptersGroup != null) currentFilter.chapterRange  = getSelectedChipText(chaptersGroup);
            if (contentGroup  != null) currentFilter.contentRating = getSelectedChipText(contentGroup);
            if (applyListener != null) applyListener.accept(currentFilter);
            dismiss();
        });

        view.findViewById(R.id.btn_reset_filters).setOnClickListener(v -> {
            currentFilter = new SearchFilter();
            if (applyListener != null) applyListener.accept(currentFilter);
            dismiss();
        });
    }

    private String getSelectedChipText(ChipGroup group) {
        if (group == null || group.getCheckedChipId() == View.NO_ID) return "All";
        View chip = group.findViewById(group.getCheckedChipId());
        if (chip instanceof com.google.android.material.chip.Chip)
            return ((com.google.android.material.chip.Chip) chip).getText().toString();
        return "All";
    }

    private float parseRating(String text) {
        if (text == null) return 0f;
        if (text.contains("4")) return 4f;
        if (text.contains("3")) return 3f;
        return 0f;
    }
}

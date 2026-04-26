package com.novelverse.app.presentation.onboarding.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.novelverse.app.R;
import com.novelverse.app.presentation.onboarding.OnboardingActivity;
import com.novelverse.app.presentation.onboarding.OnboardingAnalytics;
import com.novelverse.app.presentation.onboarding.OnboardingViewModel;
import com.novelverse.app.utils.HapticUtils;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * Interest selection screen — choose 3+ genres.
 */
@AndroidEntryPoint
public class InterestSelectionFragment extends Fragment {

    private OnboardingViewModel viewModel;
    @Inject
    OnboardingAnalytics analytics;

    private MaterialButton continueButton;
    private SelectionChipAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_interest_selection, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(OnboardingViewModel.class);

        RecyclerView recyclerView = view.findViewById(R.id.recycler_genres);
        continueButton = view.findViewById(R.id.btn_continue);

        List<SelectionChipAdapter.ChipItem> genres = buildGenreList();
        // Restore selections
        for (SelectionChipAdapter.ChipItem item : genres) {
            item.selected = viewModel.isGenreSelected(item.id);
        }

        adapter = new SelectionChipAdapter(genres, false, (position, item) -> {
            viewModel.toggleGenre(item.id);
            updateContinueButton();
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);

        updateContinueButton();

        continueButton.setOnClickListener(v -> {
            HapticUtils.medium(v);
            if (analytics != null) analytics.logInterestsSelected(viewModel.getSelectedGenreCount());
            if (getActivity() instanceof OnboardingActivity) {
                ((OnboardingActivity) getActivity()).goToStep(OnboardingViewModel.STEP_ATTRIBUTION);
            }
        });
    }

    private void updateContinueButton() {
        int count = viewModel.getSelectedGenreCount();
        boolean enabled = count >= 3;
        continueButton.setEnabled(enabled);

        if (enabled) {
            continueButton.setText("CONTINUE (" + count + "/3)");
            continueButton.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                    androidx.core.content.ContextCompat.getColor(requireContext(), R.color.black)));
        } else {
            continueButton.setText(R.string.action_continue);
            continueButton.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                    androidx.core.content.ContextCompat.getColor(requireContext(), R.color.onboarding_border)));
        }
    }

    private List<SelectionChipAdapter.ChipItem> buildGenreList() {
        List<SelectionChipAdapter.ChipItem> list = new ArrayList<>();
        list.add(new SelectionChipAdapter.ChipItem("fantasy", "Fantasy", R.drawable.ic_fantasy));
        list.add(new SelectionChipAdapter.ChipItem("romance", "Romance", R.drawable.ic_romance));
        list.add(new SelectionChipAdapter.ChipItem("science-fiction", "Science Fiction", R.drawable.ic_scifi));
        list.add(new SelectionChipAdapter.ChipItem("mystery-thriller", "Mystery and Thriller", R.drawable.ic_mystery));
        list.add(new SelectionChipAdapter.ChipItem("action-adventure", "Action and Adventure", R.drawable.ic_action));
        list.add(new SelectionChipAdapter.ChipItem("dystopia", "Dystopia", R.drawable.ic_dystopia));
        list.add(new SelectionChipAdapter.ChipItem("business-economics", "Business and Economics", R.drawable.ic_business));
        list.add(new SelectionChipAdapter.ChipItem("technology", "Technology", R.drawable.ic_technology));
        list.add(new SelectionChipAdapter.ChipItem("christian-inspirational", "Christian & Inspirational", R.drawable.ic_christian));
        list.add(new SelectionChipAdapter.ChipItem("horror", "Horror", R.drawable.ic_horror));
        list.add(new SelectionChipAdapter.ChipItem("historical-fiction", "Historical Fiction", R.drawable.ic_historical));
        list.add(new SelectionChipAdapter.ChipItem("young-adult", "Young Adult", R.drawable.ic_ya));
        list.add(new SelectionChipAdapter.ChipItem("poetry", "Poetry", R.drawable.ic_poetry));
        list.add(new SelectionChipAdapter.ChipItem("fan-fiction", "Fan Fiction", R.drawable.ic_fanfic));
        list.add(new SelectionChipAdapter.ChipItem("non-fiction", "Non-Fiction", R.drawable.ic_nonfiction));
        list.add(new SelectionChipAdapter.ChipItem("litrpg", "LitRPG", R.drawable.ic_litrpg));
        list.add(new SelectionChipAdapter.ChipItem("contemporary-fiction", "Contemporary Fiction", R.drawable.ic_contemporary));
        list.add(new SelectionChipAdapter.ChipItem("self-help", "Self-Help", R.drawable.ic_selfhelp));
        return list;
    }
}

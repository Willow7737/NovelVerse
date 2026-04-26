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
 * Attribution screen — how did you hear about us.
 */
@AndroidEntryPoint
public class AttributionFragment extends Fragment {

    private OnboardingViewModel viewModel;
    @Inject
    OnboardingAnalytics analytics;

    private MaterialButton continueButton;
    private View skipButton;
    private SelectionChipAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_attribution, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(OnboardingViewModel.class);

        RecyclerView recyclerView = view.findViewById(R.id.recycler_attribution);
        continueButton = view.findViewById(R.id.btn_continue);
        skipButton = view.findViewById(R.id.btn_skip);

        List<SelectionChipAdapter.ChipItem> options = buildAttributionList();
        adapter = new SelectionChipAdapter(options, true, (position, item) -> {
            // Single select handled by adapter
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);

        continueButton.setOnClickListener(v -> {
            HapticUtils.medium(v);
            saveAttributionAndContinue();
        });

        skipButton.setOnClickListener(v -> {
            HapticUtils.light(v);
            if (analytics != null) analytics.logOnboardingSkip("attribution");
            if (getActivity() instanceof OnboardingActivity) {
                ((OnboardingActivity) getActivity()).goToStep(OnboardingViewModel.STEP_PROFILE_SETUP);
            }
        });
    }

    private void saveAttributionAndContinue() {
        String selectedSource = null;
        for (SelectionChipAdapter.ChipItem item : adapter.getItems()) {
            if (item.selected) {
                selectedSource = item.id;
                break;
            }
        }
        if (selectedSource != null) {
            viewModel.setAttribution(selectedSource);
            if (analytics != null) analytics.logAttributionSelected(selectedSource);
        }
        if (getActivity() instanceof OnboardingActivity) {
            ((OnboardingActivity) getActivity()).goToStep(OnboardingViewModel.STEP_PROFILE_SETUP);
        }
    }

    private List<SelectionChipAdapter.ChipItem> buildAttributionList() {
        List<SelectionChipAdapter.ChipItem> list = new ArrayList<>();
        list.add(new SelectionChipAdapter.ChipItem("podcast", "Podcast", R.drawable.ic_headphones));
        list.add(new SelectionChipAdapter.ChipItem("facebook", "Facebook", R.drawable.ic_facebook));
        list.add(new SelectionChipAdapter.ChipItem("tiktok", "TikTok", R.drawable.ic_tiktok));
        list.add(new SelectionChipAdapter.ChipItem("instagram", "Instagram", R.drawable.ic_instagram));
        list.add(new SelectionChipAdapter.ChipItem("friends-family", "Friends / Family", R.drawable.ic_people));
        list.add(new SelectionChipAdapter.ChipItem("email-newsletter", "Email newsletter", R.drawable.ic_mail));
        list.add(new SelectionChipAdapter.ChipItem("google-search", "Google search", R.drawable.ic_google));
        list.add(new SelectionChipAdapter.ChipItem("app-store", "App Store / Play Store", R.drawable.ic_store));
        list.add(new SelectionChipAdapter.ChipItem("youtube", "YouTube", R.drawable.ic_youtube));
        list.add(new SelectionChipAdapter.ChipItem("other", "Other", R.drawable.ic_more));
        return list;
    }
}

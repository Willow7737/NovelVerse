package com.novelverse.app.presentation.onboarding.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.button.MaterialButton;
import com.novelverse.app.R;
import com.novelverse.app.presentation.onboarding.OnboardingActivity;
import com.novelverse.app.presentation.onboarding.OnboardingSlide;
import com.novelverse.app.presentation.onboarding.OnboardingSlideAdapter;
import com.novelverse.app.presentation.onboarding.OnboardingViewModel;

import java.util.Arrays;
import java.util.List;

/**
 * Onboarding carousel fragment.
 *
 * Hosts a ViewPager2 with slide pages, page indicators, and navigation buttons.
 * Skip / Get Started / Login actions are wired directly to the hosting activity.
 */
public class OnboardingCarouselFragment extends Fragment {

    private ViewPager2 viewPager;
    private TextView skipButton;
    private MaterialButton getStartedButton;
    private TextView loginLink;
    private LinearLayout indicatorsContainer;

    private View[] indicatorViews;

    // Define your 4 slides here — replace drawable/string resources with your actual ones
    private final List<OnboardingSlide> slides = Arrays.asList(
            new OnboardingSlide(
                    R.drawable.img_onboarding_slide_1,   // replace with your drawable
                    R.string.onboarding_title_1,         // existing string
                    R.string.onboarding_desc_1           // existing string
            ),
            new OnboardingSlide(
                    R.drawable.img_onboarding_slide_2,
                    R.string.onboarding_title_2,
                    R.string.onboarding_desc_2
            ),
            new OnboardingSlide(
                    R.drawable.img_onboarding_slide_3,
                    R.string.onboarding_title_3,
                    R.string.onboarding_desc_3
            ),
            new OnboardingSlide(
                    R.drawable.img_onboarding_slide_4,
                    R.string.onboarding_title_4,
                    R.string.onboarding_desc_4
            )
    );

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_onboarding_carousel, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        bindViews(view);
        setupViewPager();
        setupIndicators(view);
        setupClickListeners();
        updateIndicators(0);
    }

    private void bindViews(@NonNull View view) {
        viewPager = view.findViewById(R.id.view_pager);
        skipButton = view.findViewById(R.id.skip_button);
        getStartedButton = view.findViewById(R.id.get_started_button);
        loginLink = view.findViewById(R.id.login_link);
        indicatorsContainer = view.findViewById(R.id.page_indicators);
    }

    private void setupViewPager() {
        OnboardingSlideAdapter adapter = new OnboardingSlideAdapter(slides);
        viewPager.setAdapter(adapter);

        // Page change callback updates dot indicators
        viewPager.registerOnPageChangeCallback(pageChangeCallback);
    }

    private void setupIndicators(@NonNull View view) {
        // Map indicator IDs to views for easy updates
        indicatorViews = new View[]{
                view.findViewById(R.id.indicator_0),
                view.findViewById(R.id.indicator_1),
                view.findViewById(R.id.indicator_2),
                view.findViewById(R.id.indicator_3)
        };
    }

    private void setupClickListeners() {
        // Skip button → jump directly to the Get Started step
        skipButton.setOnClickListener(v -> navigateToStep(OnboardingViewModel.STEP_GET_STARTED));

        // Get Started button → same destination as skip
        getStartedButton.setOnClickListener(v -> navigateToStep(OnboardingViewModel.STEP_GET_STARTED));

        // Login link → go to login step
        loginLink.setOnClickListener(v -> navigateToStep(OnboardingViewModel.STEP_LOGIN));
    }

    private void navigateToStep(int step) {
        OnboardingActivity activity = (OnboardingActivity) requireActivity();
        activity.goToStep(step);
    }

    /**
     * Updates page indicator dots to show the active slide.
     * Active: 24dp wide bar with active background.
     * Inactive: 6dp circle with inactive background.
     */
    private void updateIndicators(int activePosition) {
        if (indicatorViews == null) return;

        for (int i = 0; i < indicatorViews.length; i++) {
            View indicator = indicatorViews[i];
            if (indicator == null) continue;

            boolean isActive = (i == activePosition);

            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) indicator.getLayoutParams();
            params.width = (int) (isActive
                    ? getResources().getDimension(R.dimen.indicator_active_width)
                    : getResources().getDimension(R.dimen.indicator_inactive_width));
            params.height = (int) getResources().getDimension(R.dimen.indicator_height);
            indicator.setLayoutParams(params);

            indicator.setBackgroundResource(isActive
                    ? R.drawable.bg_page_indicator_active
                    : R.drawable.bg_page_indicator_inactive);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        viewPager.unregisterOnPageChangeCallback(pageChangeCallback);
    }

    // Keep a reference so we can unregister it in onDestroyView
    private final ViewPager2.OnPageChangeCallback pageChangeCallback =
            new ViewPager2.OnPageChangeCallback() {
                @Override
                public void onPageSelected(int position) {
                    updateIndicators(position);
                }
            };
}

package com.novelverse.app.presentation.onboarding;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.novelverse.app.R;
import com.novelverse.app.data.local.preferences.UserPreferences;
import com.novelverse.app.data.repository.GamificationRepository;
import com.novelverse.app.data.repository.UserRepository;
import com.novelverse.app.presentation.home.HomeActivity;
import com.novelverse.app.presentation.onboarding.fragments.AttributionFragment;
import com.novelverse.app.presentation.onboarding.fragments.EmailSignUpFragment;
import com.novelverse.app.presentation.onboarding.fragments.GetStartedFragment;
import com.novelverse.app.presentation.onboarding.fragments.InterestSelectionFragment;
import com.novelverse.app.presentation.onboarding.fragments.OnboardingCarouselFragment;
import com.novelverse.app.presentation.onboarding.fragments.OnboardingLoginFragment;
import com.novelverse.app.presentation.onboarding.fragments.ProfileSetupFragment;
import com.novelverse.app.ui.LoadingSpinner;
import com.novelverse.app.ui.banner.BannerHelper;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * Single activity that hosts the entire onboarding flow.
 * Manages fragment navigation, state restoration, and completion.
 */
@AndroidEntryPoint
public class OnboardingActivity extends AppCompatActivity {

    private OnboardingViewModel viewModel;

    @Inject
    UserPreferences userPreferences;
    @Inject
    GamificationRepository gamificationRepository;
    @Inject
    UserRepository userRepository;
    @Inject
    OnboardingAnalytics analytics;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding);

        viewModel = new ViewModelProvider(this).get(OnboardingViewModel.class);

        if (analytics != null) analytics.logEvent("onboarding_start");

        // Handle intent extras from SplashActivity
        Intent intent = getIntent();
        boolean skipCarousel = intent.getBooleanExtra("skip_carousel", false);
        int restoreStep = intent.getIntExtra("restore_step", -1);

        if (savedInstanceState == null) {
            if (skipCarousel) {
                viewModel.setStep(OnboardingViewModel.STEP_GET_STARTED);
            } else if (restoreStep >= OnboardingViewModel.STEP_GET_STARTED
                    && restoreStep < OnboardingViewModel.STEP_COMPLETE) {
                viewModel.setStep(restoreStep);
            } else {
                int savedStep = userPreferences.getOnboardingStep();
                if (savedStep >= OnboardingViewModel.STEP_GET_STARTED
                        && savedStep < OnboardingViewModel.STEP_COMPLETE) {
                    viewModel.setStep(savedStep);
                } else {
                    showFragment(new OnboardingCarouselFragment(), false);
                }
            }
        }

        // Observe step changes
        viewModel.getCurrentStep().observe(this, this::navigateToStep);

        // Observe completion
        viewModel.getOnboardingComplete().observe(this, complete -> {
            if (Boolean.TRUE.equals(complete)) {
                navigateToHome();
            }
        });

        // Observe errors
        viewModel.getErrorMessage().observe(this, error -> {
            if (error != null && !error.isEmpty()) {
                BannerHelper.error(this, error);
                viewModel.clearError();
            }
        });

        // Observe loading
        viewModel.getIsLoading().observe(this, loading -> {
            if (Boolean.TRUE.equals(loading)) {
                LoadingSpinner.show(this);
            } else {
                LoadingSpinner.hide(this);
            }
        });
    }

    private void navigateToStep(Integer step) {
        if (step == null) return;
        Fragment fragment;
        boolean addToBackStack = true;

        switch (step) {
            case OnboardingViewModel.STEP_CAROUSEL:
                fragment = new OnboardingCarouselFragment();
                addToBackStack = false;
                break;
            case OnboardingViewModel.STEP_GET_STARTED:
                fragment = new GetStartedFragment();
                break;
            case OnboardingViewModel.STEP_EMAIL_SIGN_UP:
                fragment = new EmailSignUpFragment();
                break;
            case OnboardingViewModel.STEP_LOGIN:
                fragment = new OnboardingLoginFragment();
                break;
            case OnboardingViewModel.STEP_INTERESTS:
                fragment = new InterestSelectionFragment();
                break;
            case OnboardingViewModel.STEP_ATTRIBUTION:
                fragment = new AttributionFragment();
                break;
            case OnboardingViewModel.STEP_PROFILE_SETUP:
                fragment = new ProfileSetupFragment();
                break;
            default:
                fragment = new OnboardingCarouselFragment();
                addToBackStack = false;
                break;
        }

        showFragment(fragment, addToBackStack);
    }

    public void showFragment(Fragment fragment, boolean addToBackStack) {
        androidx.fragment.app.FragmentTransaction transaction = getSupportFragmentManager()
                .beginTransaction()
                .setCustomAnimations(
                        R.anim.slide_in_right,
                        R.anim.slide_out_left,
                        R.anim.slide_in_left,
                        R.anim.slide_out_right
                )
                .replace(R.id.onboarding_container, fragment);

        if (addToBackStack) {
            transaction.addToBackStack(null);
        }

        transaction.commit();
    }

    /**
     * Navigate to home activity and finish onboarding.
     */
    public void navigateToHome() {
        if (analytics != null) analytics.logOnboardingComplete();
        String userId = userPreferences.getUserId();
        if (userId != null && gamificationRepository != null) {
            gamificationRepository.ensureUserRows(userId, System.currentTimeMillis());
        }
        Intent intent = new Intent(this, HomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    /**
     * Navigate directly to a specific onboarding step from a fragment.
     */
    public void goToStep(int step) {
        viewModel.setStep(step);
    }

    /**
     * Called by fragments when auth is successful and we should skip to interests.
     */
    public void onAuthSuccess() {
        if (analytics != null) analytics.logAuthSuccess("unknown");
        viewModel.setStep(OnboardingViewModel.STEP_INTERESTS);
    }

    /**
     * Called by login fragment when sign-in is successful — skip onboarding and go home.
     */
    public void onLoginSuccess() {
        if (analytics != null) analytics.logAuthSuccess("email_login");
        userPreferences.setOnboardingCompleted(true);
        userPreferences.setOnboardingStep(OnboardingViewModel.STEP_COMPLETE);
        navigateToHome();
    }

    @Override
    public void onBackPressed() {
        int step = viewModel.getCurrentStep().getValue() != null
                ? viewModel.getCurrentStep().getValue() : OnboardingViewModel.STEP_CAROUSEL;

        String stepName = stepToName(step);
        if (analytics != null) analytics.logOnboardingBack(stepName);

        // From carousel, exit the app
        if (step == OnboardingViewModel.STEP_CAROUSEL) {
            finishAffinity();
            return;
        }

        // From interests going back, sign out and return to auth
        if (step == OnboardingViewModel.STEP_INTERESTS) {
            viewModel.clearSelectedGenres();
            if (!userPreferences.isGuestMode()) {
                userRepository.signOut((success, error) -> {
                    new Handler(Looper.getMainLooper()).post(() -> {
                        viewModel.setStep(OnboardingViewModel.STEP_GET_STARTED);
                    });
                });
            } else {
                viewModel.setStep(OnboardingViewModel.STEP_GET_STARTED);
            }
            return;
        }

        // From get started, go back to carousel
        if (step == OnboardingViewModel.STEP_GET_STARTED) {
            viewModel.setStep(OnboardingViewModel.STEP_CAROUSEL);
            return;
        }

        // Default: pop back stack or decrement step
        if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
            getSupportFragmentManager().popBackStack();
            viewModel.previousStep();
        } else {
            super.onBackPressed();
        }
    }

    private String stepToName(int step) {
        switch (step) {
            case OnboardingViewModel.STEP_CAROUSEL: return "carousel";
            case OnboardingViewModel.STEP_GET_STARTED: return "get_started";
            case OnboardingViewModel.STEP_EMAIL_SIGN_UP: return "email_sign_up";
            case OnboardingViewModel.STEP_LOGIN: return "login";
            case OnboardingViewModel.STEP_INTERESTS: return "interests";
            case OnboardingViewModel.STEP_ATTRIBUTION: return "attribution";
            case OnboardingViewModel.STEP_PROFILE_SETUP: return "profile_setup";
            default: return "unknown";
        }
    }
}

package com.novelverse.app.presentation.onboarding.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.novelverse.app.R;
import com.novelverse.app.presentation.onboarding.OnboardingActivity;
import com.novelverse.app.presentation.onboarding.OnboardingAnalytics;
import com.novelverse.app.presentation.onboarding.OnboardingViewModel;
import com.novelverse.app.ui.banner.BannerHelper;
import com.novelverse.app.utils.HapticUtils;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * Email sign-up fragment with validation.
 */
@AndroidEntryPoint
public class EmailSignUpFragment extends Fragment {

    private OnboardingViewModel viewModel;
    @Inject
    OnboardingAnalytics analytics;

    private TextInputLayout emailLayout;
    private TextInputEditText emailInput;
    private TextInputLayout passwordLayout;
    private TextInputEditText passwordInput;
    private TextInputLayout confirmPasswordLayout;
    private TextInputEditText confirmPasswordInput;
    private MaterialButton createAccountButton;
    private View backButton;
    private View headerLoginLink;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_email_sign_up, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(OnboardingViewModel.class);

        emailLayout = view.findViewById(R.id.email_layout);
        emailInput = view.findViewById(R.id.email_input);
        passwordLayout = view.findViewById(R.id.password_layout);
        passwordInput = view.findViewById(R.id.password_input);
        confirmPasswordLayout = view.findViewById(R.id.confirm_password_layout);
        confirmPasswordInput = view.findViewById(R.id.confirm_password_input);
        createAccountButton = view.findViewById(R.id.btn_create_account);
        backButton = view.findViewById(R.id.btn_back);
        headerLoginLink = view.findViewById(R.id.btn_header_login);

        View btnSocialGoogle = view.findViewById(R.id.btn_social_google);
        View btnSocialApple = view.findViewById(R.id.btn_social_apple);
        View btnSocialFacebook = view.findViewById(R.id.btn_social_facebook);

        createAccountButton.setOnClickListener(v -> {
            HapticUtils.medium(v);
            if (analytics != null) analytics.logSignUpStart();
            attemptRegister();
        });

        backButton.setOnClickListener(v -> {
            HapticUtils.light(v);
            if (getActivity() instanceof OnboardingActivity) {
                ((OnboardingActivity) getActivity()).navigateBack();
            }
        });

        headerLoginLink.setOnClickListener(v -> {
            HapticUtils.light(v);
            if (getActivity() instanceof OnboardingActivity) {
                ((OnboardingActivity) getActivity()).goToStep(OnboardingViewModel.STEP_LOGIN);
            }
        });

        if (btnSocialGoogle != null) {
            btnSocialGoogle.setOnClickListener(v -> {
                HapticUtils.light(v);
                // Go back to GetStarted so the user can tap the proper Google button there
                BannerHelper.info(requireActivity(),
                        "Tap \"Back\" to use Google sign-in from the previous screen.");
            });
        }
        if (btnSocialApple != null) {
            btnSocialApple.setOnClickListener(v -> {
                HapticUtils.light(v);
                if (getActivity() instanceof OnboardingActivity) {
                    ((OnboardingActivity) getActivity()).startOAuthPkce("apple");
                }
            });
        }
        if (btnSocialFacebook != null) {
            btnSocialFacebook.setOnClickListener(v -> {
                HapticUtils.light(v);
                if (getActivity() instanceof OnboardingActivity) {
                    ((OnboardingActivity) getActivity()).startOAuthPkce("facebook");
                }
            });
        }

        viewModel.getIsLoading().observe(getViewLifecycleOwner(), loading -> {
            createAccountButton.setEnabled(!Boolean.TRUE.equals(loading));
        });
    }

    private void attemptRegister() {
        String email = emailInput.getText() != null ? emailInput.getText().toString().trim() : "";
        String password = passwordInput.getText() != null ? passwordInput.getText().toString().trim() : "";
        String confirmPassword = confirmPasswordInput.getText() != null ? confirmPasswordInput.getText().toString().trim() : "";

        boolean valid = true;

        if (email.isEmpty()) {
            emailLayout.setError(getString(R.string.error_field_required));
            valid = false;
        } else if (!viewModel.isValidEmail(email)) {
            emailLayout.setError(getString(R.string.error_invalid_email));
            valid = false;
        } else {
            emailLayout.setError(null);
        }

        if (password.isEmpty()) {
            passwordLayout.setError(getString(R.string.error_field_required));
            valid = false;
        } else if (!viewModel.isValidPassword(password)) {
            passwordLayout.setError("Password must be at least 8 characters with 1 uppercase, 1 lowercase, and 1 number.");
            valid = false;
        } else {
            passwordLayout.setError(null);
        }

        if (confirmPassword.isEmpty()) {
            confirmPasswordLayout.setError(getString(R.string.error_field_required));
            valid = false;
        } else if (!viewModel.passwordsMatch(password, confirmPassword)) {
            confirmPasswordLayout.setError(getString(R.string.error_passwords_dont_match));
            valid = false;
        } else {
            confirmPasswordLayout.setError(null);
        }

        if (!valid) return;

        // Use email prefix as username
        String username = email.split("@")[0];
        viewModel.signUpWithEmail(email, password, username, new com.novelverse.app.data.repository.UserRepository.AuthCallback() {
            @Override
            public void onSuccess(com.novelverse.app.domain.models.User user) {
                if (analytics != null) analytics.logAuthSuccess("email");
                if (getActivity() instanceof OnboardingActivity) {
                    ((OnboardingActivity) getActivity()).onAuthSuccess();
                }
            }

            @Override
            public void onPendingVerification(com.novelverse.app.domain.models.User user) {
                // Account created — email confirmation sent.
                // Show a success/info banner and lock the button so the user doesn't
                // keep tapping and burning through Supabase's email rate limit.
                if (analytics != null) analytics.logAuthSuccess("email_pending");
                if (!isAdded()) return;
                BannerHelper.success(requireActivity(),
                        "Account created! Check your inbox for a confirmation email, then sign in.");
                createAccountButton.setEnabled(false);
                createAccountButton.setText("Check your email ✓");
            }

            @Override
            public void onError(String error) {
                if (analytics != null) analytics.logAuthError("email", error);
                if (!isAdded()) return;
                BannerHelper.error(requireActivity(), error);
            }
        });
    }
}
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
 * Login screen for existing users.
 *
 * Social sign-in buttons here delegate to the same PKCE / Google flows used
 * by GetStartedFragment — the user can always switch back to sign-up vs. login
 * but the underlying auth mechanism is identical.
 */
@AndroidEntryPoint
public class OnboardingLoginFragment extends Fragment {

    private OnboardingViewModel viewModel;
    @Inject OnboardingAnalytics analytics;

    private TextInputLayout emailLayout;
    private TextInputEditText emailInput;
    private TextInputLayout passwordLayout;
    private TextInputEditText passwordInput;
    private MaterialButton signInButton;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_onboarding_login, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(OnboardingViewModel.class);

        emailLayout    = view.findViewById(R.id.email_layout);
        emailInput     = view.findViewById(R.id.email_input);
        passwordLayout = view.findViewById(R.id.password_layout);
        passwordInput  = view.findViewById(R.id.password_input);
        signInButton   = view.findViewById(R.id.btn_sign_in);

        View backButton         = view.findViewById(R.id.btn_back);
        View headerSignUpLink   = view.findViewById(R.id.btn_header_sign_up);
        View forgotPasswordLink = view.findViewById(R.id.forgot_password_link);

        // Social buttons on the login screen (mirrors GetStartedFragment)
        View btnSocialGoogle   = view.findViewById(R.id.btn_social_google);
        View btnSocialApple    = view.findViewById(R.id.btn_social_apple);
        View btnSocialFacebook = view.findViewById(R.id.btn_social_facebook);

        // ── Primary action ────────────────────────────────────────────────────
        signInButton.setOnClickListener(v -> {
            HapticUtils.medium(v);
            if (analytics != null) analytics.logLoginStart();
            attemptLogin();
        });

        // ── Navigation ────────────────────────────────────────────────────────
        backButton.setOnClickListener(v -> {
            HapticUtils.light(v);
            if (getActivity() instanceof OnboardingActivity) {
                ((OnboardingActivity) getActivity()).navigateBack();
            }
        });

        headerSignUpLink.setOnClickListener(v -> {
            HapticUtils.light(v);
            // Lateral: Login → SignUp at the same depth
            if (getActivity() instanceof OnboardingActivity) {
                ((OnboardingActivity) getActivity()).goToStep(OnboardingViewModel.STEP_EMAIL_SIGN_UP);
            }
        });

        forgotPasswordLink.setOnClickListener(v -> {
            HapticUtils.light(v);
            if (getActivity() instanceof OnboardingActivity) {
                ((OnboardingActivity) getActivity()).goToStep(OnboardingViewModel.STEP_FORGOT_PASSWORD);
            }
        });

        // ── Social sign-in (same providers as GetStartedFragment) ─────────────
        if (btnSocialGoogle != null) {
            btnSocialGoogle.setOnClickListener(v -> {
                HapticUtils.light(v);
                // Delegate back to GetStartedFragment's Google flow is not possible from here
                // without a shared helper. Instead route user back to GetStarted first, which
                // then auto-starts Google sign-in. For simplicity, show a helpful redirect.
                BannerHelper.info(requireActivity(),
                        "Tap \"Back\" and use the Google button on the previous screen to sign in with Google.");
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

        // ── Loading state ─────────────────────────────────────────────────────
        viewModel.getIsLoading().observe(getViewLifecycleOwner(), loading -> {
            boolean enabled = !Boolean.TRUE.equals(loading);
            signInButton.setEnabled(enabled);
        });
    }

    // ── Email/password login ──────────────────────────────────────────────────

    private void attemptLogin() {
        String email    = emailInput.getText() != null
                ? emailInput.getText().toString().trim() : "";
        String password = passwordInput.getText() != null
                ? passwordInput.getText().toString().trim() : "";

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
        } else {
            passwordLayout.setError(null);
        }

        if (!valid) return;

        viewModel.signInWithEmail(email, password,
                new com.novelverse.app.data.repository.UserRepository.AuthCallback() {
                    @Override
                    public void onSuccess(com.novelverse.app.domain.models.User user) {
                        if (analytics != null) analytics.logAuthSuccess("email");
                        if (!isAdded()) return;
                        if (getActivity() instanceof OnboardingActivity) {
                            ((OnboardingActivity) getActivity()).onLoginSuccess();
                        }
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

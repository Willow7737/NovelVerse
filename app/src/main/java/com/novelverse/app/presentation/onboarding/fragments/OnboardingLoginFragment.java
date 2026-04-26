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
 * Onboarding login fragment for existing users.
 */
@AndroidEntryPoint
public class OnboardingLoginFragment extends Fragment {

    private OnboardingViewModel viewModel;
    @Inject
    OnboardingAnalytics analytics;

    private TextInputLayout emailLayout;
    private TextInputEditText emailInput;
    private TextInputLayout passwordLayout;
    private TextInputEditText passwordInput;
    private MaterialButton signInButton;
    private View backButton;
    private View headerSignUpLink;
    private View forgotPasswordLink;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_onboarding_login, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(OnboardingViewModel.class);

        emailLayout = view.findViewById(R.id.email_layout);
        emailInput = view.findViewById(R.id.email_input);
        passwordLayout = view.findViewById(R.id.password_layout);
        passwordInput = view.findViewById(R.id.password_input);
        signInButton = view.findViewById(R.id.btn_sign_in);
        backButton = view.findViewById(R.id.btn_back);
        headerSignUpLink = view.findViewById(R.id.btn_header_sign_up);
        forgotPasswordLink = view.findViewById(R.id.forgot_password_link);

        View btnSocialGoogle = view.findViewById(R.id.btn_social_google);
        View btnSocialApple = view.findViewById(R.id.btn_social_apple);
        View btnSocialFacebook = view.findViewById(R.id.btn_social_facebook);

        signInButton.setOnClickListener(v -> {
            HapticUtils.medium(v);
            if (analytics != null) analytics.logLoginStart();
            attemptLogin();
        });

        backButton.setOnClickListener(v -> {
            HapticUtils.light(v);
            requireActivity().onBackPressed();
        });

        headerSignUpLink.setOnClickListener(v -> {
            HapticUtils.light(v);
            if (getActivity() instanceof OnboardingActivity) {
                ((OnboardingActivity) getActivity()).goToStep(OnboardingViewModel.STEP_EMAIL_SIGN_UP);
            }
        });

        forgotPasswordLink.setOnClickListener(v -> {
            HapticUtils.light(v);
            BannerHelper.info(requireActivity(), "Password reset coming soon.");
        });

        if (btnSocialGoogle != null) {
            btnSocialGoogle.setOnClickListener(v -> {
                HapticUtils.light(v);
                BannerHelper.info(requireActivity(), "Use the Google button on the previous screen.");
            });
        }
        if (btnSocialApple != null) {
            btnSocialApple.setOnClickListener(v -> {
                HapticUtils.light(v);
                BannerHelper.info(requireActivity(), "Apple Sign-In coming soon.");
            });
        }
        if (btnSocialFacebook != null) {
            btnSocialFacebook.setOnClickListener(v -> {
                HapticUtils.light(v);
                BannerHelper.info(requireActivity(), "Facebook Sign-In coming soon.");
            });
        }

        viewModel.getIsLoading().observe(getViewLifecycleOwner(), loading -> {
            signInButton.setEnabled(!Boolean.TRUE.equals(loading));
        });
    }

    private void attemptLogin() {
        String email = emailInput.getText() != null ? emailInput.getText().toString().trim() : "";
        String password = passwordInput.getText() != null ? passwordInput.getText().toString().trim() : "";

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

        viewModel.signInWithEmail(email, password, new com.novelverse.app.data.repository.UserRepository.AuthCallback() {
            @Override
            public void onSuccess(com.novelverse.app.domain.models.User user) {
                if (analytics != null) analytics.logAuthSuccess("email");
                if (getActivity() instanceof OnboardingActivity) {
                    ((OnboardingActivity) getActivity()).onLoginSuccess();
                }
            }

            @Override
            public void onError(String error) {
                if (analytics != null) analytics.logAuthError("email", error);
                BannerHelper.error(requireActivity(), error);
            }
        });
    }
}

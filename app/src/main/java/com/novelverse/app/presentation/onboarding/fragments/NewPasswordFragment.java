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
 * Lets the user choose a new password after arriving via the recovery deep link.
 *
 * Receives a temporary Supabase access token via fragment arguments (key: "access_token").
 * That token was extracted by OnboardingActivity from the incoming
 * novelverse://auth/reset#access_token=... deep link and is valid only once.
 */
@AndroidEntryPoint
public class NewPasswordFragment extends Fragment {

    public static final String ARG_ACCESS_TOKEN = "access_token";

    @Inject OnboardingAnalytics analytics;

    private OnboardingViewModel viewModel;

    private TextInputLayout passwordLayout;
    private TextInputEditText passwordInput;
    private TextInputLayout confirmPasswordLayout;
    private TextInputEditText confirmPasswordInput;
    private MaterialButton btnUpdatePassword;

    public static NewPasswordFragment newInstance(String accessToken) {
        NewPasswordFragment f = new NewPasswordFragment();
        Bundle args = new Bundle();
        args.putString(ARG_ACCESS_TOKEN, accessToken);
        f.setArguments(args);
        return f;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_new_password, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(OnboardingViewModel.class);

        passwordLayout        = view.findViewById(R.id.password_layout);
        passwordInput         = view.findViewById(R.id.password_input);
        confirmPasswordLayout = view.findViewById(R.id.confirm_password_layout);
        confirmPasswordInput  = view.findViewById(R.id.confirm_password_input);
        btnUpdatePassword     = view.findViewById(R.id.btn_update_password);

        view.findViewById(R.id.btn_back).setOnClickListener(v -> {
            HapticUtils.light(v);
            requireActivity().onBackPressed();
        });

        btnUpdatePassword.setOnClickListener(v -> {
            HapticUtils.medium(v);
            attemptUpdate();
        });

        viewModel.getIsLoading().observe(getViewLifecycleOwner(), loading -> {
            btnUpdatePassword.setEnabled(!Boolean.TRUE.equals(loading));
        });
    }

    // ── Actions ───────────────────────────────────────────────────────────────

    private void attemptUpdate() {
        String password        = text(passwordInput);
        String confirmPassword = text(confirmPasswordInput);
        boolean valid = true;

        if (password.isEmpty()) {
            passwordLayout.setError(getString(R.string.error_field_required));
            valid = false;
        } else if (!viewModel.isValidPassword(password)) {
            passwordLayout.setError(getString(R.string.error_invalid_password));
            valid = false;
        } else {
            passwordLayout.setError(null);
        }

        if (confirmPassword.isEmpty()) {
            confirmPasswordLayout.setError(getString(R.string.error_field_required));
            valid = false;
        } else if (!password.equals(confirmPassword)) {
            confirmPasswordLayout.setError(getString(R.string.error_passwords_dont_match));
            valid = false;
        } else {
            confirmPasswordLayout.setError(null);
        }

        if (!valid) return;

        String accessToken = getArguments() != null
                ? getArguments().getString(ARG_ACCESS_TOKEN, "") : "";

        if (accessToken.isEmpty()) {
            // Token missing — deep link may have been malformed; send them back to request a new link
            BannerHelper.error(requireActivity(),
                    "Reset link is invalid or expired. Please request a new one.");
            if (getActivity() instanceof OnboardingActivity) {
                ((OnboardingActivity) getActivity()).goToStep(OnboardingViewModel.STEP_FORGOT_PASSWORD);
            }
            return;
        }

        viewModel.updatePasswordWithToken(accessToken, password, (success, error) -> {
            if (!isAdded()) return;
            if (success) {
                if (analytics != null) analytics.logEvent("password_reset_success");
                requireActivity().runOnUiThread(() -> {
                    BannerHelper.success(requireActivity(),
                            "Password updated! Please sign in with your new password.");
                    if (getActivity() instanceof OnboardingActivity) {
                        ((OnboardingActivity) getActivity()).goToStep(OnboardingViewModel.STEP_LOGIN);
                    }
                });
            } else {
                requireActivity().runOnUiThread(() ->
                        BannerHelper.error(requireActivity(),
                                error != null ? error : "Failed to update password. Please try again."));
            }
        });
    }

    private String text(TextInputEditText field) {
        return field.getText() != null ? field.getText().toString().trim() : "";
    }
}
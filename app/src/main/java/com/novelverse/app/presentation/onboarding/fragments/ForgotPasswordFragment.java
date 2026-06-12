package com.novelverse.app.presentation.onboarding.fragments;

import android.content.Intent;
import android.net.Uri;
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
 * Forgot password screen.
 *
 * Two visual states managed by view visibility:
 *  - REQUEST state: email input + "Send Reset Link" button
 *  - SENT state:    confirmation message + "Open Email App" + resend option
 *
 * Flow:
 *  1. User enters email → tap Send → POST /auth/v1/recover with redirect_to deep link
 *  2. Supabase emails a recovery link that opens novelverse://auth/reset
 *  3. OnboardingActivity intercepts the deep link, extracts the access token,
 *     and navigates to NewPasswordFragment with the token as an argument.
 */
@AndroidEntryPoint
public class ForgotPasswordFragment extends Fragment {

    private OnboardingViewModel viewModel;
    @Inject OnboardingAnalytics analytics;

    private View stateRequest;
    private View stateSent;

    private TextInputLayout emailLayout;
    private TextInputEditText emailInput;
    private MaterialButton btnSendReset;

    private android.widget.TextView sentSubtitle;
    private MaterialButton btnOpenEmail;
    private android.widget.TextView btnResend;

    /** Tracks the last email submitted so the resend button can reuse it. */
    private String lastEmail = "";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_forgot_password, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(OnboardingViewModel.class);

        stateRequest = view.findViewById(R.id.state_request);
        stateSent    = view.findViewById(R.id.state_sent);

        emailLayout   = view.findViewById(R.id.email_layout);
        emailInput    = view.findViewById(R.id.email_input);
        btnSendReset  = view.findViewById(R.id.btn_send_reset);
        sentSubtitle  = view.findViewById(R.id.sent_subtitle);
        btnOpenEmail  = view.findViewById(R.id.btn_open_email);
        btnResend     = view.findViewById(R.id.btn_resend);

        view.findViewById(R.id.btn_back).setOnClickListener(v -> {
            HapticUtils.light(v);
            requireActivity().onBackPressed();
        });

        view.findViewById(R.id.btn_back_to_login).setOnClickListener(v -> {
            HapticUtils.light(v);
            goToLogin();
        });

        view.findViewById(R.id.btn_sent_back_to_login).setOnClickListener(v -> {
            HapticUtils.light(v);
            goToLogin();
        });

        btnSendReset.setOnClickListener(v -> {
            HapticUtils.medium(v);
            attemptSendReset();
        });

        btnOpenEmail.setOnClickListener(v -> {
            HapticUtils.light(v);
            openEmailApp();
        });

        btnResend.setOnClickListener(v -> {
            HapticUtils.light(v);
            if (!lastEmail.isEmpty()) {
                sendReset(lastEmail);
            } else {
                showRequestState();
            }
        });

        viewModel.getIsLoading().observe(getViewLifecycleOwner(), loading -> {
            btnSendReset.setEnabled(!Boolean.TRUE.equals(loading));
        });
    }

    // ── Actions ──────────────────────────────────────────────────────────────

    private void attemptSendReset() {
        String email = emailInput.getText() != null
                ? emailInput.getText().toString().trim() : "";

        if (email.isEmpty()) {
            emailLayout.setError(getString(R.string.error_field_required));
            return;
        }
        if (!viewModel.isValidEmail(email)) {
            emailLayout.setError(getString(R.string.error_invalid_email));
            return;
        }
        emailLayout.setError(null);

        sendReset(email);
    }

    private void sendReset(String email) {
        lastEmail = email;
        viewModel.resetPassword(email, (success, error) -> {
            if (!isAdded()) return;
            if (success) {
                if (analytics != null) analytics.logEvent("password_reset_requested");
                requireActivity().runOnUiThread(() -> showSentState(email));
            } else {
                requireActivity().runOnUiThread(() ->
                        BannerHelper.error(requireActivity(),
                                error != null ? error : "Failed to send reset email. Please try again."));
            }
        });
    }

    // ── State transitions ─────────────────────────────────────────────────────

    private void showSentState(String email) {
        stateRequest.setVisibility(View.GONE);
        stateSent.setVisibility(View.VISIBLE);
        sentSubtitle.setText(
                "We sent a password reset link to " + email + ".");
    }

    private void showRequestState() {
        stateSent.setVisibility(View.GONE);
        stateRequest.setVisibility(View.VISIBLE);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void goToLogin() {
        if (getActivity() instanceof OnboardingActivity) {
            ((OnboardingActivity) getActivity()).goToStep(OnboardingViewModel.STEP_LOGIN);
        }
    }

    /** Opens whatever email app is installed on the device. */
    private void openEmailApp() {
        try {
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_APP_EMAIL);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } catch (android.content.ActivityNotFoundException e) {
            // Fallback: open Gmail directly if installed, else do nothing
            try {
                Intent gmail = requireContext().getPackageManager()
                        .getLaunchIntentForPackage("com.google.android.gm");
                if (gmail != null) startActivity(gmail);
            } catch (Exception ignored) {
                BannerHelper.info(requireActivity(), "Please open your email app to find the reset link.");
            }
        }
    }
}
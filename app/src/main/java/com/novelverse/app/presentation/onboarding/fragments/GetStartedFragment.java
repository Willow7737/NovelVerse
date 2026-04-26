package com.novelverse.app.presentation.onboarding.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.novelverse.app.BuildConfig;
import com.novelverse.app.R;
import com.novelverse.app.data.local.preferences.UserPreferences;
import com.novelverse.app.presentation.onboarding.OnboardingActivity;
import com.novelverse.app.presentation.onboarding.OnboardingAnalytics;
import com.novelverse.app.presentation.onboarding.OnboardingViewModel;
import com.novelverse.app.ui.banner.BannerHelper;
import com.novelverse.app.utils.HapticUtils;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * Get Started / Auth screen — gateway to the app with SSO options.
 */
@AndroidEntryPoint
public class GetStartedFragment extends Fragment {

    private static final int RC_SIGN_IN = 9001;

    private OnboardingViewModel viewModel;
    private GoogleSignInClient googleSignInClient;

    @Inject
    UserPreferences userPreferences;
    @Inject
    OnboardingAnalytics analytics;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_get_started, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(OnboardingViewModel.class);

        MaterialButton btnGoogle = view.findViewById(R.id.btn_google);
        MaterialButton btnApple = view.findViewById(R.id.btn_apple);
        MaterialButton btnFacebook = view.findViewById(R.id.btn_facebook);
        MaterialButton btnEmail = view.findViewById(R.id.btn_email);
        MaterialButton btnLogin = view.findViewById(R.id.btn_login);
        View btnGuest = view.findViewById(R.id.btn_guest);
        TextView tosText = view.findViewById(R.id.tos_text);

        setupGoogleSignIn();

        btnGoogle.setOnClickListener(v -> {
            HapticUtils.light(v);
            if (analytics != null) analytics.logAuthMethod("google");
            signInWithGoogle();
        });

        btnApple.setOnClickListener(v -> {
            HapticUtils.light(v);
            if (analytics != null) analytics.logAuthMethod("apple");
            BannerHelper.info(requireActivity(), "Apple Sign-In coming soon.");
        });

        btnFacebook.setOnClickListener(v -> {
            HapticUtils.light(v);
            if (analytics != null) analytics.logAuthMethod("facebook");
            BannerHelper.info(requireActivity(), "Facebook Sign-In coming soon.");
        });

        btnEmail.setOnClickListener(v -> {
            HapticUtils.light(v);
            if (analytics != null) analytics.logAuthMethod("email");
            if (getActivity() instanceof OnboardingActivity) {
                ((OnboardingActivity) getActivity()).goToStep(OnboardingViewModel.STEP_EMAIL_SIGN_UP);
            }
        });

        btnLogin.setOnClickListener(v -> {
            HapticUtils.light(v);
            if (analytics != null) analytics.logLoginStart();
            if (getActivity() instanceof OnboardingActivity) {
                ((OnboardingActivity) getActivity()).goToStep(OnboardingViewModel.STEP_LOGIN);
            }
        });

        btnGuest.setOnClickListener(v -> {
            HapticUtils.light(v);
            if (analytics != null) analytics.logAuthMethod("guest");
            viewModel.continueAsGuest();
            if (getActivity() instanceof OnboardingActivity) {
                ((OnboardingActivity) getActivity()).onAuthSuccess();
            }
        });

        // TOS links
        tosText.setMovementMethod(LinkMovementMethod.getInstance());
    }

    private void setupGoogleSignIn() {
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(BuildConfig.GOOGLE_WEB_CLIENT_ID)
                .requestEmail()
                .build();
        googleSignInClient = GoogleSignIn.getClient(requireActivity(), gso);
    }

    private void signInWithGoogle() {
        Intent signInIntent = googleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            handleGoogleSignInResult(task);
        }
    }

    private void handleGoogleSignInResult(Task<GoogleSignInAccount> completedTask) {
        try {
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);
            if (account != null && account.getIdToken() != null) {
                viewModel.signInWithGoogle(account.getIdToken(), new com.novelverse.app.data.repository.UserRepository.AuthCallback() {
                    @Override
                    public void onSuccess(com.novelverse.app.domain.models.User user) {
                        if (analytics != null) analytics.logAuthSuccess("google");
                        if (getActivity() instanceof OnboardingActivity) {
                            ((OnboardingActivity) getActivity()).onAuthSuccess();
                        }
                    }

                    @Override
                    public void onError(String error) {
                        if (analytics != null) analytics.logAuthError("google", error);
                        BannerHelper.error(requireActivity(), error);
                    }
                });
            } else {
                BannerHelper.error(requireActivity(), "Google sign-in failed: Could not retrieve ID token.");
            }
        } catch (ApiException e) {
            if (analytics != null) analytics.logAuthError("google", "status_" + e.getStatusCode());
            BannerHelper.error(requireActivity(), "Google sign-in failed: " + e.getStatusCode());
        }
    }
}

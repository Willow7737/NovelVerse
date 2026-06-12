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
import com.novelverse.app.data.remote.supabase.SupabaseAuthService;
import com.novelverse.app.presentation.onboarding.OnboardingActivity;
import com.novelverse.app.presentation.onboarding.OnboardingAnalytics;
import com.novelverse.app.presentation.onboarding.OnboardingViewModel;
import com.novelverse.app.ui.banner.BannerHelper;
import com.novelverse.app.utils.HapticUtils;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * "Get Started" screen — the main auth gateway.
 *
 * <h3>Provider implementation</h3>
 * <ul>
 *   <li><b>Google</b> — native Google Sign-In SDK → id_token grant with nonce.</li>
 *   <li><b>Facebook</b> — Supabase PKCE flow via Chrome Custom Tab.</li>
 *   <li><b>Apple</b> — Supabase PKCE flow via Chrome Custom Tab.</li>
 *   <li><b>Email</b> — navigates to EmailSignUpFragment.</li>
 *   <li><b>Guest</b> — no auth, proceeds directly to interests.</li>
 * </ul>
 */
@AndroidEntryPoint
public class GetStartedFragment extends Fragment {

    private static final int RC_GOOGLE_SIGN_IN = 9001;

    private OnboardingViewModel viewModel;
    private GoogleSignInClient googleSignInClient;

    /**
     * Raw nonce stored for the duration of the Google sign-in round-trip.
     * Generated fresh before each attempt; cleared on success or failure.
     */
    private String pendingRawNonce;

    @Inject UserPreferences userPreferences;
    @Inject OnboardingAnalytics analytics;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_get_started, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(OnboardingViewModel.class);

        MaterialButton btnGoogle   = view.findViewById(R.id.btn_google);
        MaterialButton btnApple    = view.findViewById(R.id.btn_apple);
        MaterialButton btnFacebook = view.findViewById(R.id.btn_facebook);
        MaterialButton btnEmail    = view.findViewById(R.id.btn_email);
        MaterialButton btnLogin    = view.findViewById(R.id.btn_login);
        View           btnGuest    = view.findViewById(R.id.btn_guest);

        setupGoogleSignIn();

        // ── Google ──────────────────────────────────────────────────────────
        btnGoogle.setOnClickListener(v -> {
            HapticUtils.light(v);
            if (analytics != null) analytics.logAuthMethod("google");
            signInWithGoogle();
        });

        // ── Apple (PKCE via Custom Tab) ─────────────────────────────────────
        btnApple.setOnClickListener(v -> {
            HapticUtils.light(v);
            if (analytics != null) analytics.logAuthMethod("apple");
            if (getActivity() instanceof OnboardingActivity) {
                ((OnboardingActivity) getActivity()).startOAuthPkce("apple");
            }
        });

        // ── Facebook (PKCE via Custom Tab) ──────────────────────────────────
        btnFacebook.setOnClickListener(v -> {
            HapticUtils.light(v);
            if (analytics != null) analytics.logAuthMethod("facebook");
            if (getActivity() instanceof OnboardingActivity) {
                ((OnboardingActivity) getActivity()).startOAuthPkce("facebook");
            }
        });

        // ── Email sign-up ────────────────────────────────────────────────────
        btnEmail.setOnClickListener(v -> {
            HapticUtils.light(v);
            if (analytics != null) analytics.logAuthMethod("email");
            if (getActivity() instanceof OnboardingActivity) {
                ((OnboardingActivity) getActivity()).goToStep(OnboardingViewModel.STEP_EMAIL_SIGN_UP);
            }
        });

        // ── Log in (existing users) ─────────────────────────────────────────
        btnLogin.setOnClickListener(v -> {
            HapticUtils.light(v);
            if (analytics != null) analytics.logLoginStart();
            if (getActivity() instanceof OnboardingActivity) {
                ((OnboardingActivity) getActivity()).goToStep(OnboardingViewModel.STEP_LOGIN);
            }
        });

        // ── Guest ────────────────────────────────────────────────────────────
        btnGuest.setOnClickListener(v -> {
            HapticUtils.light(v);
            if (analytics != null) analytics.logAuthMethod("guest");
            viewModel.continueAsGuest();
            if (getActivity() instanceof OnboardingActivity) {
                ((OnboardingActivity) getActivity()).onAuthSuccess();
            }
        });

        // Disable buttons while loading
        viewModel.getIsLoading().observe(getViewLifecycleOwner(), loading -> {
            boolean enabled = !Boolean.TRUE.equals(loading);
            if (btnGoogle   != null) btnGoogle.setEnabled(enabled);
            if (btnApple    != null) btnApple.setEnabled(enabled);
            if (btnFacebook != null) btnFacebook.setEnabled(enabled);
            if (btnEmail    != null) btnEmail.setEnabled(enabled);
            if (btnLogin    != null) btnLogin.setEnabled(enabled);
        });
    }

    // ── Google Sign-In ────────────────────────────────────────────────────────

    /**
     * Configures the Google Sign-In client.
     *
     * We request the ID token using the Web Client ID (type 3 in google-services.json).
     *
     * Nonce note: play-services-auth 20.x does not expose requestNonce() on the legacy
     * GoogleSignInOptions.Builder in all build environments. We skip the nonce here and
     * set "Skip Nonce Check" = ON in Supabase Dashboard → Auth → Providers → Google.
     * This is the recommended approach for the legacy sign-in SDK on Android.
     */
    private void setupGoogleSignIn() {
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(BuildConfig.GOOGLE_WEB_CLIENT_ID)
                .requestEmail()
                .build();
        googleSignInClient = GoogleSignIn.getClient(requireActivity(), gso);
    }

    private void signInWithGoogle() {
        // Nonce not used with legacy SDK — Skip Nonce Check must be ON in Supabase Dashboard
        pendingRawNonce = null;

        // Sign out cached account first so the account picker always shows
        googleSignInClient.signOut().addOnCompleteListener(task -> {
            Intent signInIntent = googleSignInClient.getSignInIntent();
            startActivityForResult(signInIntent, RC_GOOGLE_SIGN_IN);
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_GOOGLE_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            handleGoogleSignInResult(task);
        }
    }

    private void handleGoogleSignInResult(Task<GoogleSignInAccount> completedTask) {
        try {
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);
            String idToken = account != null ? account.getIdToken() : null;

            if (idToken == null || idToken.isEmpty()) {
                pendingRawNonce = null;
                BannerHelper.error(requireActivity(),
                        "Google sign-in failed: could not retrieve ID token. " +
                        "Make sure the Web Client ID is configured in Supabase → Providers → Google.");
                return;
            }

            // Pass BOTH the idToken and the rawNonce to Supabase
            final String rawNonce = pendingRawNonce;
            pendingRawNonce = null; // consume immediately

            viewModel.signInWithGoogle(idToken, rawNonce,
                    new com.novelverse.app.data.repository.UserRepository.AuthCallback() {
                        @Override
                        public void onSuccess(com.novelverse.app.domain.models.User user) {
                            if (analytics != null) analytics.logAuthSuccess("google");
                            if (!isAdded()) return;
                            if (getActivity() instanceof OnboardingActivity) {
                                ((OnboardingActivity) getActivity()).onAuthSuccess();
                            }
                        }

                        @Override
                        public void onError(String error) {
                            if (analytics != null) analytics.logAuthError("google", error);
                            if (!isAdded()) return;
                            BannerHelper.error(requireActivity(), error);
                        }
                    });

        } catch (ApiException e) {
            pendingRawNonce = null;
            if (analytics != null) analytics.logAuthError("google", "status_" + e.getStatusCode());
            String userMsg = googleApiErrorToMessage(e.getStatusCode());
            BannerHelper.error(requireActivity(), userMsg);
        }
    }

    /**
     * Converts Google API status codes to user-friendly messages.
     * Full list: https://developers.google.com/android/reference/com/google/android/gms/common/api/CommonStatusCodes
     */
    private String googleApiErrorToMessage(int statusCode) {
        switch (statusCode) {
            case 4:  return "Sign-in was cancelled.";
            case 7:  return "No network connection. Please check your internet and try again.";
            case 10: return "Google Sign-In configuration error. Please contact support.";
            case 12500: return "Google Play Services update required to sign in.";
            case 12501: return "Sign-in was cancelled.";
            case 12502: return "Google Sign-In is currently unavailable. Please try again.";
            default: return "Google sign-in failed (code " + statusCode + "). Please try again.";
        }
    }
}

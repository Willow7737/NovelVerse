package com.novelverse.app.presentation.auth.login;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.novelverse.app.ui.banner.BannerHelper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import dagger.hilt.android.AndroidEntryPoint;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.novelverse.app.BuildConfig;
import com.novelverse.app.R;
import com.novelverse.app.domain.utils.InputValidator;
import com.novelverse.app.presentation.auth.AuthActivity;
import com.novelverse.app.presentation.auth.AuthViewModel;

@AndroidEntryPoint
public class LoginFragment extends Fragment {

    private static final int RC_SIGN_IN = 9001;
    private static final int MAX_ATTEMPTS = 5;
    private static final long LOCKOUT_DURATION_MS = 30_000L;

    private AuthViewModel viewModel;
    private GoogleSignInClient googleSignInClient;

    private TextInputLayout emailLayout;
    private TextInputEditText emailInput;
    private TextInputLayout passwordLayout;
    private TextInputEditText passwordInput;
    private MaterialButton loginButton;
    private MaterialButton googleButton;
    private MaterialButton guestButton;
    private View registerLink;
    private View forgotPasswordLink;
    private TextView lockoutCountdownView;

    // Brute-force protection
    private int failedAttempts = 0;
    private long lockoutUntil = 0;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable countdownRunnable;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_login, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(AuthViewModel.class);
        initViews(view);
        setupGoogleSignIn();
        setupListeners();
        observeViewModel();
    }

    private void initViews(View view) {
        emailLayout = view.findViewById(R.id.email_layout);
        emailInput = view.findViewById(R.id.email_input);
        passwordLayout = view.findViewById(R.id.password_layout);
        passwordInput = view.findViewById(R.id.password_input);
        loginButton = view.findViewById(R.id.login_button);
        googleButton = view.findViewById(R.id.google_button);
        guestButton = view.findViewById(R.id.guest_button);
        registerLink = view.findViewById(R.id.register_link);
        forgotPasswordLink = view.findViewById(R.id.forgot_password_link);
        lockoutCountdownView = new TextView(requireContext());
        lockoutCountdownView.setTextColor(0xFFEF4444);
        lockoutCountdownView.setTextSize(14f);
        lockoutCountdownView.setGravity(android.view.Gravity.CENTER);
        lockoutCountdownView.setVisibility(android.view.View.GONE);
        lockoutCountdownView.setPadding(0, 16, 0, 0);
        android.view.ViewGroup loginParent = (android.view.ViewGroup) view.findViewById(R.id.login_button).getParent();
        if (loginParent != null) {
            android.widget.LinearLayout.LayoutParams lp = new android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT);
            loginParent.addView(lockoutCountdownView, lp);
        }
    }

    private void setupGoogleSignIn() {
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(BuildConfig.GOOGLE_WEB_CLIENT_ID)
            .requestEmail()
            .build();
        googleSignInClient = GoogleSignIn.getClient(requireActivity(), gso);
    }

    private void setupListeners() {
        loginButton.setOnClickListener(v -> attemptLogin());
        googleButton.setOnClickListener(v -> signInWithGoogle());
        guestButton.setOnClickListener(v -> continueAsGuest());
        registerLink.setOnClickListener(v -> {
            if (getActivity() instanceof AuthActivity) {
                ((AuthActivity) getActivity()).showRegisterFragment();
            }
        });
        forgotPasswordLink.setOnClickListener(v -> {
            if (getActivity() instanceof AuthActivity) {
                ((AuthActivity) getActivity()).showForgotPasswordFragment();
            }
        });
    }

    private void observeViewModel() {
        viewModel.getAuthResult().observe(getViewLifecycleOwner(), result -> {
            if (result == null) return;
            if (result.isSuccess()) {
                failedAttempts = 0;
                lockoutUntil = 0;
            } else {
                onAuthFailure();
            }
        });
    }

    private void onAuthFailure() {
        failedAttempts++;
        if (failedAttempts >= MAX_ATTEMPTS) {
            lockoutUntil = System.currentTimeMillis() + LOCKOUT_DURATION_MS;
            loginButton.setEnabled(false);
            startCountdown();
        }
    }

    private void startCountdown() {
        if (lockoutCountdownView != null) lockoutCountdownView.setVisibility(View.VISIBLE);
        countdownRunnable = new Runnable() {
            @Override
            public void run() {
                long remaining = lockoutUntil - System.currentTimeMillis();
                if (remaining <= 0) {
                    loginButton.setEnabled(true);
                    if (lockoutCountdownView != null) lockoutCountdownView.setVisibility(View.GONE);
                    failedAttempts = 0;
                } else {
                    int secs = (int) (remaining / 1000) + 1;
                    if (lockoutCountdownView != null) {
                        lockoutCountdownView.setText("Too many attempts. Try again in " + secs + "s");
                    }
                    handler.postDelayed(this, 1000);
                }
            }
        };
        handler.post(countdownRunnable);
    }

    private void attemptLogin() {
        if (System.currentTimeMillis() < lockoutUntil) return;

        String rawEmail = emailInput.getText() != null ? emailInput.getText().toString() : "";
        String password = passwordInput.getText() != null ? passwordInput.getText().toString().trim() : "";
        String email = InputValidator.sanitizeEmail(rawEmail);

        boolean isValid = true;
        if (email.isEmpty()) {
            emailLayout.setError(getString(R.string.error_field_required));
            isValid = false;
        } else if (!InputValidator.isValidEmail(email)) {
            emailLayout.setError(getString(R.string.error_invalid_email));
            isValid = false;
        } else {
            emailLayout.setError(null);
        }
        if (password.isEmpty()) {
            passwordLayout.setError(getString(R.string.error_field_required));
            isValid = false;
        } else {
            passwordLayout.setError(null);
        }
        if (isValid) {
            viewModel.signIn(email, password);
        }
    }

    private void signInWithGoogle() {
        Intent signInIntent = googleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    private void continueAsGuest() {
        viewModel.continueAsGuest();
        BannerHelper.info(requireActivity(), "Browsing as guest. Sign up to unlock all features.");
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
                // FIX: pass the actual ID token to the ViewModel instead of calling
                // the no-arg method which never forwarded it to Supabase
                viewModel.signInWithGoogle(account.getIdToken());
            } else {
                BannerHelper.error(requireActivity(), "Google sign-in failed: Could not retrieve ID token.");
            }
        } catch (ApiException e) {
            BannerHelper.error(requireActivity(), "Google sign-in failed: " + e.getStatusCode());
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (countdownRunnable != null) handler.removeCallbacks(countdownRunnable);
    }
}

package com.novelverse.app.presentation.auth;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.novelverse.app.ui.banner.BannerHelper;
import com.novelverse.app.ui.LoadingSpinner;
import com.novelverse.app.R;
import com.novelverse.app.data.local.preferences.UserPreferences;
import com.novelverse.app.data.repository.GamificationRepository;
import com.novelverse.app.domain.utils.Resource;
import com.novelverse.app.presentation.auth.login.LoginFragment;
import com.novelverse.app.presentation.auth.register.RegisterFragment;
import com.novelverse.app.presentation.home.HomeActivity;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * Authentication activity that hosts login, register, and forgot password fragments
 */
@AndroidEntryPoint
public class AuthActivity extends AppCompatActivity {

    private AuthViewModel viewModel;
    private ProgressBar progressBar;

    @Inject GamificationRepository gamificationRepository;
    @Inject UserPreferences         userPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auth);

        viewModel = new ViewModelProvider(this).get(AuthViewModel.class);
        progressBar = findViewById(R.id.progress_bar);

        // Check if already logged in
        viewModel.isLoggedIn().observe(this, isLoggedIn -> {
            if (isLoggedIn != null && isLoggedIn) {
                navigateToHome();
            }
        });

        // Observe auth result
        viewModel.getAuthResult().observe(this, result -> {
            if (result == null) return;

            switch (result.getStatus()) {
                case LOADING:
                    showLoading(true);
                    break;
                case SUCCESS:
                    showLoading(false);
                    navigateToHome();
                    break;
                case ERROR:
                    showLoading(false);
                    showError(result.getMessage());
                    break;
            }
        });

        // Show login or register fragment based on intent extra
        if (savedInstanceState == null) {
            if (getIntent().getBooleanExtra("start_register", false)) {
                showRegisterFragment();
            } else {
                showLoginFragment();
            }
        }
    }

    /**
     * Show login fragment
     */
    public void showLoginFragment() {
        replaceFragment(new LoginFragment());
    }

    /**
     * Show register fragment
     */
    public void showRegisterFragment() {
        replaceFragment(new RegisterFragment());
    }

    /**
     * Show forgot password fragment
     */
    public void showForgotPasswordFragment() {
        replaceFragment(new com.novelverse.app.presentation.auth.forgotpassword.ForgotPasswordFragment());
    }

    /**
     * Replace fragment in container
     */
    private void replaceFragment(Fragment fragment) {
        getSupportFragmentManager()
            .beginTransaction()
            .setCustomAnimations(
                R.anim.slide_in_right,
                R.anim.slide_out_left,
                R.anim.slide_in_left,
                R.anim.slide_out_right
            )
            .replace(R.id.fragment_container, fragment)
            .addToBackStack(null)
            .commit();
    }

    /**
     * Navigate to home activity
     */
    private void navigateToHome() {
        // Ensure gamification rows exist for this user (idempotent)
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
     * Show/hide loading indicator
     */
    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        if (show) LoadingSpinner.show(this);
        else      LoadingSpinner.hide(this);
    }

    /**
     * Show error message
     */
    private void showError(String message) {
        if (message != null) {
            BannerHelper.error(this, message);
        }
    }

    @Override
    public void onBackPressed() {
        if (getSupportFragmentManager().getBackStackEntryCount() > 1) {
            getSupportFragmentManager().popBackStack();
        } else {
            super.onBackPressed();
        }
    }
}

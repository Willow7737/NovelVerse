package com.novelverse.app.presentation.splash;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;

import com.novelverse.app.R;
import com.novelverse.app.data.local.preferences.UserPreferences;
import com.novelverse.app.presentation.home.HomeActivity;
import com.novelverse.app.presentation.onboarding.OnboardingActivity;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * Custom splash — does NOT use the AndroidX SplashScreen API so there is only
 * ONE splash screen (this one). The system's default "enlarged launcher icon"
 * splash is suppressed by setting windowDisableSplashScreen in the theme.
 * 
 * Routes to onboarding for new users, home for returning authenticated users.
 */
@AndroidEntryPoint
public class SplashActivity extends AppCompatActivity {

    @Inject
    UserPreferences userPreferences;

    private static final long BRAND_HOLD_MS = 1600L;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        try {
            setContentView(R.layout.activity_splash);
            
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                try {
                    boolean isLoggedIn = userPreferences.getAccessToken() != null
                            && !userPreferences.getAccessToken().isEmpty();
                    boolean onboardingCompleted = userPreferences.isOnboardingCompleted();
                    int onboardingStep = userPreferences.getOnboardingStep();

                    Intent intent;
                    if (!onboardingCompleted) {
                        // New user or incomplete onboarding — go to onboarding flow
                        intent = new Intent(this, OnboardingActivity.class);
                        if (isLoggedIn && onboardingStep > 0) {
                            // Restore to last step if they were in progress
                            intent.putExtra("restore_step", onboardingStep);
                        }
                    } else if (isLoggedIn) {
                        // Onboarding done + logged in → home
                        intent = new Intent(this, HomeActivity.class);
                    } else {
                        // Onboarding done but not logged in → onboarding auth screen
                        intent = new Intent(this, OnboardingActivity.class);
                        intent.putExtra("skip_carousel", true);
                    }

                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                    finish();
                } catch (Exception e) {
                    // This will be caught by our custom CrashHandler
                    throw new RuntimeException("Failed to navigate from Splash", e);
                }
            }, BRAND_HOLD_MS);
        } catch (Exception e) {
            // Immediate crash during inflation or setup
            throw new RuntimeException("Failed to initialize SplashActivity", e);
        }
    }
}

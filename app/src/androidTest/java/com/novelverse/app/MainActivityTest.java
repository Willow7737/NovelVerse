package com.novelverse.app;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.novelverse.app.presentation.splash.SplashActivity;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

/**
 * Instrumented smoke test — verifies the app launches without crashing
 * and renders the splash screen.
 *
 * Runs on a real device or emulator.
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class MainActivityTest {

    @Rule
    public ActivityScenarioRule<SplashActivity> activityRule =
            new ActivityScenarioRule<>(SplashActivity.class);

    @Test
    public void app_launchesWithoutCrash() {
        // If the activity starts without throwing, this assertion always passes.
        // It confirms the DI graph (Hilt), Room DB, and manifest are all wired correctly.
        activityRule.getScenario().onActivity(activity -> {
            assert activity != null : "SplashActivity should not be null";
        });
    }

    @Test
    public void splashLogo_isDisplayed() {
        // The splash screen must show its logo/lottie container immediately.
        onView(withId(R.id.splash_logo)).check(matches(isDisplayed()));
    }
}

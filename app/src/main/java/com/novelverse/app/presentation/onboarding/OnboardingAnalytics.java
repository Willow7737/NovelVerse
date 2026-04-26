package com.novelverse.app.presentation.onboarding;

import android.os.Bundle;

import com.google.firebase.analytics.FirebaseAnalytics;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Firebase Analytics helper for the onboarding flow.
 * Tracks user progression, auth methods, interests, and completion.
 */
@Singleton
public class OnboardingAnalytics {

    private final FirebaseAnalytics firebaseAnalytics;

    @Inject
    public OnboardingAnalytics(FirebaseAnalytics firebaseAnalytics) {
        this.firebaseAnalytics = firebaseAnalytics;
    }

    public void logEvent(String eventName, Bundle params) {
        firebaseAnalytics.logEvent(eventName, params);
    }

    public void logEvent(String eventName) {
        firebaseAnalytics.logEvent(eventName, null);
    }

    // ── Carousel ────────────────────────────────────────────────────────────

    public void logCarouselSlide(int slideNumber) {
        Bundle bundle = new Bundle();
        bundle.putInt("slide_number", slideNumber);
        logEvent("onboarding_carousel_slide_viewed", bundle);
    }

    public void logGetStartedTap() {
        logEvent("onboarding_get_started_tap");
    }

    public void logSkipCarousel() {
        logEvent("onboarding_skip_carousel");
    }

    // ── Auth ────────────────────────────────────────────────────────────────

    public void logAuthMethod(String method) {
        Bundle bundle = new Bundle();
        bundle.putString("method", method);
        logEvent("onboarding_auth_method", bundle);
    }

    public void logSignUpStart() {
        logEvent("onboarding_signup_start");
    }

    public void logLoginStart() {
        logEvent("onboarding_login_start");
    }

    public void logAuthSuccess(String method) {
        Bundle bundle = new Bundle();
        bundle.putString("method", method);
        logEvent("onboarding_auth_success", bundle);
    }

    public void logAuthError(String method, String error) {
        Bundle bundle = new Bundle();
        bundle.putString("method", method);
        bundle.putString("error", error);
        logEvent("onboarding_auth_error", bundle);
    }

    // ── Interests ─────────────────────────────────────────────────────────────

    public void logInterestsSelected(int count) {
        Bundle bundle = new Bundle();
        bundle.putInt("genre_count", count);
        logEvent("onboarding_interests_selected", bundle);
    }

    // ── Attribution ─────────────────────────────────────────────────────────

    public void logAttributionSelected(String source) {
        Bundle bundle = new Bundle();
        bundle.putString("source", source);
        logEvent("onboarding_attribution_selected", bundle);
    }

    // ── Profile Setup ───────────────────────────────────────────────────────

    public void logProfileSetupComplete(boolean hasPhoto, boolean hasName, String role) {
        Bundle bundle = new Bundle();
        bundle.putBoolean("has_photo", hasPhoto);
        bundle.putBoolean("has_name", hasName);
        bundle.putString("role", role);
        logEvent("onboarding_profile_setup_complete", bundle);
    }

    public void logProfileSetupSkip() {
        logEvent("onboarding_profile_setup_skip");
    }

    // ── Completion ────────────────────────────────────────────────────────────

    public void logOnboardingComplete() {
        logEvent("onboarding_complete");
    }

    public void logOnboardingSkip(String step) {
        Bundle bundle = new Bundle();
        bundle.putString("step", step);
        logEvent("onboarding_skip", bundle);
    }

    public void logOnboardingBack(String fromStep) {
        Bundle bundle = new Bundle();
        bundle.putString("from_step", fromStep);
        logEvent("onboarding_back", bundle);
    }
}

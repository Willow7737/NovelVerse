package com.novelverse.app.presentation.onboarding;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProvider;

import com.novelverse.app.R;
import com.novelverse.app.data.local.preferences.UserPreferences;
import com.novelverse.app.data.remote.supabase.SupabaseAuthService;
import com.novelverse.app.data.repository.GamificationRepository;
import com.novelverse.app.data.repository.UserRepository;
import com.novelverse.app.presentation.home.HomeActivity;
import com.novelverse.app.presentation.onboarding.fragments.AttributionFragment;
import com.novelverse.app.presentation.onboarding.fragments.EmailSignUpFragment;
import com.novelverse.app.presentation.onboarding.fragments.ForgotPasswordFragment;
import com.novelverse.app.presentation.onboarding.fragments.GetStartedFragment;
import com.novelverse.app.presentation.onboarding.fragments.InterestSelectionFragment;
import com.novelverse.app.presentation.onboarding.fragments.NewPasswordFragment;
import com.novelverse.app.presentation.onboarding.fragments.OnboardingCarouselFragment;
import com.novelverse.app.presentation.onboarding.fragments.OnboardingLoginFragment;
import com.novelverse.app.presentation.onboarding.fragments.ProfileSetupFragment;
import com.novelverse.app.ui.LoadingSpinner;
import com.novelverse.app.ui.banner.BannerHelper;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * Single-activity host for the entire onboarding / auth flow.
 *
 * <h3>Navigation architecture</h3>
 * <p>Navigation is <em>driven by direct method calls</em> on this Activity, NOT by
 * a LiveData step observer. This avoids the classic observer-re-emission bugs that
 * caused duplicate fragment pushes and de-synced back-stacks.
 *
 * <ul>
 *   <li>{@link #goToStep(int)} — main entry point; picks animation automatically.</li>
 *   <li>{@link #showFragmentForward(Fragment, String)} — slide-in from right, adds to back-stack.</li>
 *   <li>{@link #showFragmentLateral(Fragment, String)} — crossfade, REPLACES without pushing.</li>
 *   <li>{@link #showFragmentModal(Fragment, String)} — slides up from bottom (ForgotPassword, NewPassword).</li>
 *   <li>{@link #navigateBack()} — pops back-stack with slide-back animation.</li>
 * </ul>
 *
 * <h3>Social auth (OAuth PKCE)</h3>
 * <p>For Facebook, Apple, and any other browser-based provider, the activity stores
 * the PKCE code verifier in SharedPreferences under {@code NV_PKCE_PREFS}, opens the
 * Supabase OAuth URL in a Chrome Custom Tab, and handles the callback when the system
 * delivers {@code novelverse://auth/callback?code=...} via {@link #onNewIntent(Intent)}.
 */
@AndroidEntryPoint
public class OnboardingActivity extends AppCompatActivity {

    // ── PKCE storage ──────────────────────────────────────────────────────────
    private static final String PKCE_PREFS         = "nv_pkce_prefs";
    private static final String KEY_CODE_VERIFIER   = "pkce_code_verifier";
    private static final String KEY_PENDING_PROVIDER = "pkce_pending_provider";

    // ── Back-stack tags ───────────────────────────────────────────────────────
    private static final String TAG_CAROUSEL     = "carousel";
    private static final String TAG_GET_STARTED  = "get_started";
    private static final String TAG_AUTH_METHOD  = "auth_method";   // email_signup OR login
    private static final String TAG_MODAL        = "modal";         // forgot_password, new_password

    // ── Step ordering (for direction detection) ───────────────────────────────
    private static final int[] STEP_ORDER = {
            OnboardingViewModel.STEP_CAROUSEL,
            OnboardingViewModel.STEP_GET_STARTED,
            OnboardingViewModel.STEP_EMAIL_SIGN_UP,
            OnboardingViewModel.STEP_LOGIN,
            OnboardingViewModel.STEP_INTERESTS,
            OnboardingViewModel.STEP_ATTRIBUTION,
            OnboardingViewModel.STEP_PROFILE_SETUP,
    };

    private OnboardingViewModel viewModel;
    private int currentStep = OnboardingViewModel.STEP_CAROUSEL;

    @Inject UserPreferences userPreferences;
    @Inject GamificationRepository gamificationRepository;
    @Inject UserRepository userRepository;
    @Inject OnboardingAnalytics analytics;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding);

        viewModel = new ViewModelProvider(this).get(OnboardingViewModel.class);

        // Handle incoming recovery deep link (novelverse://auth/reset#access_token=...)
        handleResetDeepLink(getIntent());

        if (analytics != null) analytics.logEvent("onboarding_start");

        if (savedInstanceState == null) {
            // Determine initial screen
            Intent intent = getIntent();
            boolean skipCarousel = intent.getBooleanExtra("skip_carousel", false);
            int restoreStep     = intent.getIntExtra("restore_step", -1);

            if (skipCarousel) {
                showInitial(OnboardingViewModel.STEP_GET_STARTED);
            } else if (isValidRestoreStep(restoreStep)) {
                showInitial(restoreStep);
            } else {
                int savedStep = userPreferences.getOnboardingStep();
                if (isValidRestoreStep(savedStep)) {
                    showInitial(savedStep);
                } else {
                    showInitial(OnboardingViewModel.STEP_CAROUSEL);
                }
            }
        }

        // Observe errors (from ViewModel auth ops)
        viewModel.getErrorMessage().observe(this, error -> {
            if (error != null && !error.isEmpty()) {
                BannerHelper.error(this, error);
                viewModel.clearError();
            }
        });

        // Observe loading
        viewModel.getIsLoading().observe(this, loading -> {
            if (Boolean.TRUE.equals(loading)) LoadingSpinner.show(this);
            else LoadingSpinner.hide(this);
        });

        // Observe completion
        viewModel.getOnboardingComplete().observe(this, complete -> {
            if (Boolean.TRUE.equals(complete)) navigateToHome();
        });
    }

    // ── Public navigation API ─────────────────────────────────────────────────

    /**
     * Navigate to a step. Determines the correct animation automatically:
     *  - FORWARD  → slide-in from right
     *  - BACKWARD → slide-in from left  (pop)
     *  - LATERAL  → crossfade (Login ↔ SignUp, same "depth" level)
     *  - MODAL    → slide-up (ForgotPassword, NewPassword)
     */
    public void goToStep(int step) {
        viewModel.setStep(step); // persist for app-restart restoration

        switch (step) {
            case OnboardingViewModel.STEP_CAROUSEL:
                // Pop everything back to carousel (or replace)
                getSupportFragmentManager().popBackStack(TAG_CAROUSEL,
                        FragmentManager.POP_BACK_STACK_INCLUSIVE);
                replaceNoAnim(new OnboardingCarouselFragment());
                currentStep = step;
                return;

            case OnboardingViewModel.STEP_GET_STARTED:
                // Pop back to get_started if on stack; else push forward
                if (getSupportFragmentManager().findFragmentByTag(TAG_GET_STARTED) != null) {
                    getSupportFragmentManager().popBackStack(TAG_GET_STARTED, 0);
                } else {
                    showFragmentForward(new GetStartedFragment(), TAG_GET_STARTED);
                }
                currentStep = step;
                return;

            case OnboardingViewModel.STEP_EMAIL_SIGN_UP: {
                // Lateral if currently on LOGIN, forward otherwise
                Fragment f = new EmailSignUpFragment();
                if (currentStep == OnboardingViewModel.STEP_LOGIN) {
                    showFragmentLateral(f, TAG_AUTH_METHOD);
                } else {
                    showFragmentForward(f, TAG_AUTH_METHOD);
                }
                currentStep = step;
                return;
            }

            case OnboardingViewModel.STEP_LOGIN: {
                Fragment f = new OnboardingLoginFragment();
                if (currentStep == OnboardingViewModel.STEP_EMAIL_SIGN_UP) {
                    showFragmentLateral(f, TAG_AUTH_METHOD);
                } else {
                    showFragmentForward(f, TAG_AUTH_METHOD);
                }
                currentStep = step;
                return;
            }

            case OnboardingViewModel.STEP_INTERESTS:
                showFragmentForward(new InterestSelectionFragment(), null);
                currentStep = step;
                return;

            case OnboardingViewModel.STEP_ATTRIBUTION:
                showFragmentForward(new AttributionFragment(), null);
                currentStep = step;
                return;

            case OnboardingViewModel.STEP_PROFILE_SETUP:
                showFragmentForward(new ProfileSetupFragment(), null);
                currentStep = step;
                return;

            case OnboardingViewModel.STEP_FORGOT_PASSWORD:
                showFragmentModal(new ForgotPasswordFragment(), TAG_MODAL);
                currentStep = step;
                return;

            case OnboardingViewModel.STEP_NEW_PASSWORD:
                // Requires access token — use showNewPasswordFragment() instead
                showFragmentModal(new ForgotPasswordFragment(), TAG_MODAL);
                currentStep = step;
                return;
        }
    }

    /**
     * Called by fragments when a sign-UP is complete — go to interest selection.
     */
    public void onAuthSuccess() {
        if (analytics != null) analytics.logAuthSuccess("social");
        goToStep(OnboardingViewModel.STEP_INTERESTS);
    }

    /**
     * Called by login fragment when sign-IN is complete — skip onboarding, go home.
     */
    public void onLoginSuccess() {
        if (analytics != null) analytics.logAuthSuccess("email_login");
        userPreferences.setOnboardingCompleted(true);
        userPreferences.setOnboardingStep(OnboardingViewModel.STEP_COMPLETE);
        navigateToHome();
    }

    /**
     * Pops the back stack (navigates back) with the slide-back animation.
     */
    public void navigateBack() {
        if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
            getSupportFragmentManager().popBackStack();
            // Step tracking
            currentStep = stepForBackStack();
        } else {
            finish();
        }
    }

    // ── Fragment show helpers ─────────────────────────────────────────────────

    /**
     * Slides in from right. Added to back-stack with optional tag.
     */
    public void showFragmentForward(Fragment fragment, String backStackTag) {
        getSupportFragmentManager()
                .beginTransaction()
                .setCustomAnimations(
                        R.anim.nav_enter_forward,   // enter: new slides in from right
                        R.anim.nav_exit_forward,    // exit:  old recedes to left
                        R.anim.nav_enter_back,      // popEnter: old comes back from left
                        R.anim.nav_exit_back        // popExit:  current flies out right
                )
                .replace(R.id.onboarding_container, fragment)
                .addToBackStack(backStackTag)
                .commit();
    }

    /**
     * Crossfade + micro-slide. Replaces without pushing back-stack entry.
     * Used for lateral navigation between siblings (Login ↔ SignUp).
     */
    private void showFragmentLateral(Fragment fragment, String backStackTag) {
        // Pop any existing entry at this "level" first, then push fresh
        // so back always goes to GetStarted, not to the previous sibling.
        getSupportFragmentManager().popBackStack(backStackTag,
                FragmentManager.POP_BACK_STACK_INCLUSIVE);

        getSupportFragmentManager()
                .beginTransaction()
                .setCustomAnimations(
                        R.anim.nav_lateral_enter,
                        R.anim.nav_lateral_exit,
                        R.anim.nav_enter_back,
                        R.anim.nav_exit_back
                )
                .replace(R.id.onboarding_container, fragment)
                .addToBackStack(backStackTag)
                .commit();
    }

    /**
     * Slides up from bottom. Used for modal-style screens (ForgotPassword, NewPassword).
     */
    public void showFragmentModal(Fragment fragment, String backStackTag) {
        getSupportFragmentManager()
                .beginTransaction()
                .setCustomAnimations(
                        R.anim.nav_modal_enter,
                        R.anim.nav_modal_exit,
                        R.anim.nav_modal_enter,
                        R.anim.nav_modal_exit
                )
                .replace(R.id.onboarding_container, fragment)
                .addToBackStack(backStackTag)
                .commit();
    }

    /** Replace with NO animation (used for initial/restore loads). */
    private void replaceNoAnim(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.onboarding_container, fragment)
                .commit();
    }

    // ── Special navigation ────────────────────────────────────────────────────

    /** Shows NewPasswordFragment with the recovery access token. */
    public void showNewPasswordFragment(String accessToken) {
        showFragmentModal(NewPasswordFragment.newInstance(accessToken), TAG_MODAL);
        currentStep = OnboardingViewModel.STEP_NEW_PASSWORD;
    }

    /** Navigates to HomeActivity and finishes the onboarding flow. */
    public void navigateToHome() {
        if (analytics != null) analytics.logOnboardingComplete();
        String userId = userPreferences.getUserId();
        if (userId != null && gamificationRepository != null) {
            gamificationRepository.ensureUserRows(userId, System.currentTimeMillis());
        }
        Intent intent = new Intent(this, HomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    // ── PKCE OAuth (Facebook, Apple, etc.) ────────────────────────────────────

    /**
     * Initiates an OAuth PKCE flow for a given provider by opening the Supabase
     * authorization URL in a Chrome Custom Tab.
     *
     * The app receives the callback via {@code novelverse://auth/callback?code=...}
     * which is handled in {@link #onNewIntent(Intent)}.
     *
     * @param provider Supabase provider name: "facebook", "apple", "github", etc.
     */
    public void startOAuthPkce(String provider) {
        // Generate PKCE pair
        String codeVerifier  = SupabaseAuthService.generateCodeVerifier();
        String codeChallenge = SupabaseAuthService.deriveCodeChallenge(codeVerifier);

        // Persist verifier so we can use it after the browser callback
        getSharedPreferences(PKCE_PREFS, MODE_PRIVATE)
                .edit()
                .putString(KEY_CODE_VERIFIER, codeVerifier)
                .putString(KEY_PENDING_PROVIDER, provider)
                .apply();

        // Build auth URL using the SupabaseClient's URL
        String supabaseUrl  = com.novelverse.app.BuildConfig.SUPABASE_URL;
        String authBase     = supabaseUrl + "/auth/v1";
        String codeChallEnc = codeChallenge; // already URL-safe base64

        String authUrl = authBase
                + "/authorize?provider=" + provider
                + "&redirect_to=" + Uri.encode("novelverse://auth/callback")
                + "&flow_type=pkce"
                + "&code_challenge=" + codeChallEnc
                + "&code_challenge_method=S256";

        // Open in Custom Tab (falls back to system browser if Chrome not available)
        try {
            androidx.browser.customtabs.CustomTabsIntent customTabsIntent =
                    new androidx.browser.customtabs.CustomTabsIntent.Builder()
                            .setShowTitle(true)
                            .build();
            customTabsIntent.launchUrl(this, Uri.parse(authUrl));
        } catch (Exception e) {
            // Fallback to plain browser if Custom Tabs not available
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(authUrl)));
        }
    }

    /**
     * Handles the OAuth PKCE callback deep-link: novelverse://auth/callback?code=...
     *
     * Exchanges the authorization code + stored code verifier for a Supabase session.
     */
    private void handleOAuthCallback(Uri uri) {
        String code = uri.getQueryParameter("code");
        if (code == null || code.isEmpty()) {
            String error = uri.getQueryParameter("error_description");
            BannerHelper.error(this,
                    error != null ? error : "Sign-in was cancelled. Please try again.");
            return;
        }

        SharedPreferences pkcePrefs = getSharedPreferences(PKCE_PREFS, MODE_PRIVATE);
        String codeVerifier = pkcePrefs.getString(KEY_CODE_VERIFIER, null);
        String provider     = pkcePrefs.getString(KEY_PENDING_PROVIDER, "unknown");
        pkcePrefs.edit().remove(KEY_CODE_VERIFIER).remove(KEY_PENDING_PROVIDER).apply();

        if (codeVerifier == null) {
            BannerHelper.error(this, "Authentication session expired. Please try again.");
            return;
        }

        LoadingSpinner.show(this);
        viewModel.exchangeOAuthCode(code, codeVerifier,
                new com.novelverse.app.data.repository.UserRepository.AuthCallback() {
                    @Override
                    public void onSuccess(com.novelverse.app.domain.models.User user) {
                        if (analytics != null) analytics.logAuthSuccess(provider);
                        new Handler(Looper.getMainLooper()).post(() -> {
                            LoadingSpinner.hide(OnboardingActivity.this);
                            onAuthSuccess();
                        });
                    }

                    @Override
                    public void onError(String error) {
                        if (analytics != null) analytics.logAuthError(provider, error);
                        new Handler(Looper.getMainLooper()).post(() -> {
                            LoadingSpinner.hide(OnboardingActivity.this);
                            BannerHelper.error(OnboardingActivity.this, error);
                        });
                    }
                });
    }

    // ── Back press ────────────────────────────────────────────────────────────

    @Override
    public void onBackPressed() {
        int count = getSupportFragmentManager().getBackStackEntryCount();

        if (currentStep == OnboardingViewModel.STEP_CAROUSEL || count == 0) {
            finishAffinity(); // Exit app from carousel
            return;
        }

        if (currentStep == OnboardingViewModel.STEP_GET_STARTED) {
            goToStep(OnboardingViewModel.STEP_CAROUSEL);
            return;
        }

        if (currentStep == OnboardingViewModel.STEP_INTERESTS) {
            // Going back from interests: sign out and return to auth
            viewModel.clearSelectedGenres();
            if (!userPreferences.isGuestMode()) {
                userRepository.signOut((success, error) ->
                        new Handler(Looper.getMainLooper()).post(() ->
                                goToStep(OnboardingViewModel.STEP_GET_STARTED)));
            } else {
                goToStep(OnboardingViewModel.STEP_GET_STARTED);
            }
            return;
        }

        // Default: pop the back stack — animation is handled by the popEnter/popExit
        // registered when the fragment was pushed. No need to call viewModel.
        getSupportFragmentManager().popBackStack();
        currentStep = stepForBackStack();
        viewModel.setStep(currentStep);

        if (analytics != null) analytics.logOnboardingBack(stepToName(currentStep));
    }

    // ── Deep links ────────────────────────────────────────────────────────────

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);

        Uri data = intent.getData();
        if (data == null) return;

        String scheme = data.getScheme();
        String host   = data.getHost();

        if ("novelverse".equals(scheme)) {
            if ("auth".equals(host)) {
                String path = data.getPath();
                if ("/reset".equals(path)) {
                    handleResetDeepLink(intent);
                } else if ("/callback".equals(path)) {
                    handleOAuthCallback(data);
                }
            }
        }
    }

    /**
     * Handles novelverse://auth/reset#access_token=...&type=recovery
     * Triggered by the Supabase password-reset email link.
     */
    private void handleResetDeepLink(Intent intent) {
        if (intent == null) return;
        Uri data = intent.getData();
        if (data == null) return;

        if (!"novelverse".equals(data.getScheme()) || !"auth".equals(data.getHost())) return;
        if (!"/reset".equals(data.getPath())) return;

        String fragment = data.getFragment();
        if (fragment == null || fragment.isEmpty()) {
            BannerHelper.error(this, "Reset link is invalid or expired. Please request a new one.");
            goToStep(OnboardingViewModel.STEP_FORGOT_PASSWORD);
            return;
        }

        String accessToken = extractParam(fragment, "access_token");
        String type        = extractParam(fragment, "type");

        if (accessToken.isEmpty() || !"recovery".equals(type)) {
            BannerHelper.error(this, "Reset link is invalid or expired. Please request a new one.");
            goToStep(OnboardingViewModel.STEP_FORGOT_PASSWORD);
            return;
        }

        showNewPasswordFragment(accessToken);
    }

    // ── Internal helpers ──────────────────────────────────────────────────────

    /** Shows the initial fragment without any transition animation. */
    private void showInitial(int step) {
        currentStep = step;
        viewModel.setStep(step);

        Fragment fragment;
        switch (step) {
            case OnboardingViewModel.STEP_GET_STARTED:
                fragment = new GetStartedFragment();
                break;
            case OnboardingViewModel.STEP_EMAIL_SIGN_UP:
                fragment = new EmailSignUpFragment();
                break;
            case OnboardingViewModel.STEP_LOGIN:
                fragment = new OnboardingLoginFragment();
                break;
            case OnboardingViewModel.STEP_INTERESTS:
                fragment = new InterestSelectionFragment();
                break;
            case OnboardingViewModel.STEP_ATTRIBUTION:
                fragment = new AttributionFragment();
                break;
            case OnboardingViewModel.STEP_PROFILE_SETUP:
                fragment = new ProfileSetupFragment();
                break;
            default:
                fragment = new OnboardingCarouselFragment();
                currentStep = OnboardingViewModel.STEP_CAROUSEL;
                break;
        }

        replaceNoAnim(fragment);
    }

    private boolean isValidRestoreStep(int step) {
        return step >= OnboardingViewModel.STEP_GET_STARTED
                && step < OnboardingViewModel.STEP_COMPLETE;
    }

    /**
     * Guesses the current step based on back-stack size (fallback for tracking).
     * Not perfect but keeps the step approximately correct for analytics/logging.
     */
    private int stepForBackStack() {
        int count = getSupportFragmentManager().getBackStackEntryCount() - 1;
        if (count <= 0) return OnboardingViewModel.STEP_GET_STARTED;
        // Best-effort: return the step before the one we're popping away from
        return Math.max(OnboardingViewModel.STEP_CAROUSEL, currentStep - 1);
    }

    private String stepToName(int step) {
        switch (step) {
            case OnboardingViewModel.STEP_CAROUSEL:       return "carousel";
            case OnboardingViewModel.STEP_GET_STARTED:    return "get_started";
            case OnboardingViewModel.STEP_EMAIL_SIGN_UP:  return "email_sign_up";
            case OnboardingViewModel.STEP_LOGIN:           return "login";
            case OnboardingViewModel.STEP_INTERESTS:       return "interests";
            case OnboardingViewModel.STEP_ATTRIBUTION:     return "attribution";
            case OnboardingViewModel.STEP_PROFILE_SETUP:   return "profile_setup";
            case OnboardingViewModel.STEP_FORGOT_PASSWORD: return "forgot_password";
            case OnboardingViewModel.STEP_NEW_PASSWORD:    return "new_password";
            default: return "unknown";
        }
    }

    private String extractParam(String fragment, String key) {
        for (String pair : fragment.split("&")) {
            String[] kv = pair.split("=", 2);
            if (kv.length == 2 && key.equals(kv[0])) return kv[1];
        }
        return "";
    }
}

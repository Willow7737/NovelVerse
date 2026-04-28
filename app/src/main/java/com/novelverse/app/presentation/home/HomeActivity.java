package com.novelverse.app.presentation.home;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.novelverse.app.R;
import com.novelverse.app.crash.CrashActivity;
import com.novelverse.app.data.local.preferences.UserPreferences;
import com.novelverse.app.data.repository.GamificationRepository;
import com.novelverse.app.domain.gamification.DailyCheckInManager;
import com.novelverse.app.domain.models.User;
import com.novelverse.app.presentation.auth.AuthActivity;
import com.novelverse.app.presentation.onboarding.OnboardingManager;
import com.novelverse.app.presentation.auth.AuthViewModel;
import com.novelverse.app.presentation.common.views.FloatingNavBar;
import com.novelverse.app.presentation.library.LibraryFragment;
import com.novelverse.app.presentation.notifications.NotificationsActivity;
import com.novelverse.app.presentation.profile.ProfileFragment;
import com.novelverse.app.presentation.search.SearchActivity;
import com.novelverse.app.presentation.write.WriteFragment;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * Main host activity.
 *
 * <p>Nav slot mapping: Guest / Reader / Moderator : [Home, Library, Profile] Author : [Home,
 * Library, Write, Profile] Admin : [Home, Library, Write, Admin, Profile]
 *
 * <p>Guests who tap protected tabs (Library/Profile/Notifications) see a bottom-sheet auth prompt
 * instead of being force-navigated away.
 */
@AndroidEntryPoint
public class HomeActivity extends AppCompatActivity {

    private AuthViewModel authViewModel;
    private FloatingNavBar floatingNavBar;
    private android.widget.ImageView streakFlameIcon;
    private String currentRole = "reader";
    private android.animation.AnimatorSet streakAnimator;

    @Inject GamificationRepository gamificationRepository;
    @Inject UserPreferences         userPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (checkCrashAndRedirect()) return;
        setContentView(R.layout.activity_home);
        // Go edge-to-edge once here. AppBarLayout's own fitsSystemWindows="true"
        // handles status-bar inset when visible; profile fills full-bleed when AppBar is GONE.
        // Toggling this per-fragment (the old approach) caused double status-bar offset on
        // non-profile fragments and timing-dependent gaps on profile.
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setStatusBarColor(android.graphics.Color.TRANSPARENT);
        handleDeepLink(getIntent());

        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);
        authViewModel.getCurrentUser().observe(this, this::onUserLoaded);

        streakFlameIcon = findViewById(R.id.ic_streak_flame_icon);
        startStreakFlameAnimation();
        floatingNavBar = findViewById(R.id.bottom_navigation);

        ImageButton btnSearch = findViewById(R.id.btn_search_header);
        if (btnSearch != null)
            btnSearch.setOnClickListener(
                    v -> startActivity(new Intent(this, SearchActivity.class)));

        ImageButton btnNotif = findViewById(R.id.btn_notification);
        if (btnNotif != null)
            btnNotif.setOnClickListener(
                    v -> {
                        if (isGuest()) {
                            showAuthBottomSheet();
                            return;
                        }
                        startActivity(new Intent(this, NotificationsActivity.class));
                    });

        floatingNavBar.setOnItemSelectedListener(this::onTabSelected);
        View fragmentContainer = findViewById(R.id.fragment_container);
        if (fragmentContainer != null) floatingNavBar.setSourceView(fragmentContainer);
        if (savedInstanceState == null) navigateToIndex(0);
    }

    @Override
    protected void onResume() {
        super.onResume();
        triggerDailyCheckIn();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (streakAnimator != null) {
            streakAnimator.removeAllListeners();
            streakAnimator.cancel();
            streakAnimator = null;
        }
    }

    /**
     * Daily check-in: awards Ink once per calendar day when the user opens the app.
     * Silently skips guests and repeat opens on the same day.
     */
    private void triggerDailyCheckIn() {
        String userId = userPreferences.getUserId();
        if (userId == null || isGuest()) return;

        long nowMs = System.currentTimeMillis();
        DailyCheckInManager.CheckInResult result =
            DailyCheckInManager.checkIn(this, nowMs);

        if (!result.isFirstToday) return; // already claimed today

        // Credit Ink
        if (gamificationRepository != null && result.inkReward > 0) {
            gamificationRepository.addInk(userId, result.inkReward,
                "BONUS_DAILY", "Daily check-in bonus", nowMs);
        }

        // Update the check-in card in the current fragment if visible
        updateCheckInCard(result);
    }

    private void updateCheckInCard(DailyCheckInManager.CheckInResult result) {
        // The check-in card lives inside HomeFragment's layout
        // Post to give the fragment a chance to attach
        View root = findViewById(android.R.id.content);
        if (root == null) return;
        root.post(() -> {
            View card    = root.findViewWithTag("daily_checkin_card");
            if (card == null) return;
            TextView streakLabel = card.findViewById(R.id.checkin_streak_label);
            TextView rewardText  = card.findViewWithTag("checkin_reward_text");
            View claimBtn        = card.findViewById(R.id.btn_claim_checkin);

            if (streakLabel != null)
                streakLabel.setText("Day " + result.consecutiveDays + " streak");
            if (rewardText != null)
                rewardText.setText("+" + result.inkReward + " Ink");
            if (claimBtn != null)
                claimBtn.setVisibility(View.GONE); // auto-claimed; just show the card
        });
    }

    // ── Role setup ────────────────────────────────────────────────────────────

    private void onUserLoaded(User user) {
        if (floatingNavBar == null) return;
        currentRole =
                (user == null || user.getRole() == null) ? "guest" : user.getRole().toLowerCase();
        applyNavIcons();

        // Avatar removed from nav — always show ic_profile icon
    }

    /** Uses Glide to load the user's avatar into the last (profile) nav icon slot. */
    private void loadNavAvatar(String url) {
        com.bumptech.glide.Glide.with(this)
                .asBitmap()
                .load(url)
                .circleCrop()
                .into(
                        new com.bumptech.glide.request.target.CustomTarget<
                                android.graphics.Bitmap>() {
                            @Override
                            public void onResourceReady(
                                    @NonNull android.graphics.Bitmap resource,
                                    @Nullable
                                            com.bumptech.glide.request.transition.Transition<
                                                            ? super android.graphics.Bitmap>
                                                    t) {
                                if (floatingNavBar != null) {
                                    floatingNavBar.setProfileIcon(
                                            new android.graphics.drawable.BitmapDrawable(
                                                    getResources(), resource));
                                }
                            }

                            @Override
                            public void onLoadCleared(
                                    @Nullable android.graphics.drawable.Drawable p) {}
                        });
    }

    private void applyNavIcons() {
        switch (currentRole) {
            case "admin":
                floatingNavBar.setIcons(
                        new int[] {
                            R.drawable.ic_home, R.drawable.ic_library,
                            R.drawable.ic_write, R.drawable.ic_admin,
                            R.drawable.ic_profile
                        });
                break;
            case "author":
                floatingNavBar.setIcons(
                        new int[] {
                            R.drawable.ic_home, R.drawable.ic_library,
                            R.drawable.ic_write, R.drawable.ic_profile
                        });
                break;
            default:
                floatingNavBar.setIcons(
                        new int[] {
                            R.drawable.ic_home, R.drawable.ic_library, R.drawable.ic_profile
                        });
                break;
        }
    }

    // ── Navigation ────────────────────────────────────────────────────────────

    private void onTabSelected(int index) {
        if (isGuest() && index > 0) {
            showAuthBottomSheet();
            floatingNavBar.setActiveIndex(0);
            return;
        }
        navigateToIndex(index);
    }

    public void navigateToIndex(int index) {
        Fragment fragment;
        boolean isProfile;
        switch (currentRole) {
            case "admin":
                switch (index) {
                    case 1:  fragment = new LibraryFragment(); break;
                    case 2:  fragment = new WriteFragment();   break;
                    case 3:  fragment = new WriteFragment();   break;
                    case 4:  fragment = new ProfileFragment(); break;
                    default: fragment = new HomeFragment();    break;
                }
                isProfile = (index == 4);
                break;
            case "author":
                switch (index) {
                    case 1:  fragment = new LibraryFragment(); break;
                    case 2:  fragment = new WriteFragment();   break;
                    case 3:  fragment = new ProfileFragment(); break;
                    default: fragment = new HomeFragment();    break;
                }
                isProfile = (index == 3);
                break;
            default:
                switch (index) {
                    case 1:  fragment = new LibraryFragment(); break;
                    case 2:  fragment = new ProfileFragment(); break;
                    default: fragment = new HomeFragment();    break;
                }
                isProfile = (index == 2);
                break;
        }
        // Hide the top toolbar when the Profile tab is active
        setAppBarVisible(!isProfile);
        getSupportFragmentManager()
                .beginTransaction()
                .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                .replace(R.id.fragment_container, fragment)
                .commit();
    }

    /**
     * Show / hide the header AppBarLayout.
     *
     * fragment_container has NO appbar_scrolling_view_behavior — it is always full-screen
     * (top=0, behind the status bar). We push its content down manually by setting
     * paddingTop = appBar.getHeight() for non-profile tabs, and 0 for profile so the
     * cover photo bleeds into the status bar with zero timing dependency.
     */
    public void setAppBarVisible(boolean visible) {
        com.google.android.material.appbar.AppBarLayout appBar = findViewById(R.id.app_bar);
        FrameLayout fragmentContainer = findViewById(R.id.fragment_container);

        if (visible) {
            if (appBar != null) {
                appBar.setVisibility(View.VISIBLE);
                appBar.animate().alpha(1f).setDuration(180).start();
                // Measure AppBar height after layout so paddingTop is exact
                // (it includes statusBar inset because AppBar has fitsSystemWindows=true).
                appBar.post(() -> {
                    if (fragmentContainer != null && appBar.getHeight() > 0) {
                        fragmentContainer.setPadding(
                            0, appBar.getHeight(), 0, fragmentContainer.getPaddingBottom());
                    }
                });
            }
        } else {
            // Zero out top padding immediately — no behavior, no timing race.
            // Profile cover now starts at y=0, edge-to-edge behind the status bar.
            if (fragmentContainer != null)
                fragmentContainer.setPadding(0, 0, 0, fragmentContainer.getPaddingBottom());
            if (appBar != null) {
                appBar.animate().cancel();
                appBar.setAlpha(1f);
                appBar.setVisibility(View.GONE);
            }
        }
    }

    public void showTab(int index) {
        if (floatingNavBar != null) floatingNavBar.setActiveIndex(index);
        navigateToIndex(index);
    }

    /** Called by child fragments to wire scroll-hide on the nav bar. */
    public void attachNavToScroll(androidx.core.widget.NestedScrollView scrollView) {
        if (floatingNavBar != null) floatingNavBar.attachToScroll(scrollView);
    }

    /** Called by LibraryFragment to wire scroll-hide via RecyclerView. */
    public void attachNavToRecyclerView(androidx.recyclerview.widget.RecyclerView rv) {
        if (floatingNavBar != null) floatingNavBar.attachToRecyclerView(rv);
    }

    // ── Guest auth BottomSheet ────────────────────────────────────────────────

    private boolean isGuest() {
        return "guest".equals(currentRole) || currentRole == null || currentRole.isEmpty();
    }

    private void showAuthBottomSheet() {
        BottomSheetDialog sheet = new BottomSheetDialog(this, R.style.BottomSheetTheme);
        android.view.View root = getLayoutInflater().inflate(R.layout.bottom_sheet_auth, null);
        root.findViewById(R.id.auth_sheet_btn_sign_in)
                .setOnClickListener(
                        v -> {
                            sheet.dismiss();
                            navigateToLogin();
                        });
        root.findViewById(R.id.auth_sheet_btn_create)
                .setOnClickListener(
                        v -> {
                            sheet.dismiss();
                            navigateToRegister();
                        });
        root.findViewById(R.id.auth_sheet_dismiss).setOnClickListener(v -> sheet.dismiss());
        sheet.setContentView(root);
        sheet.show();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private boolean checkCrashAndRedirect() {
        try {
            SharedPreferences prefs = getSharedPreferences("crash_prefs", MODE_PRIVATE);
            boolean crashed = prefs.getBoolean("crashed", false);
            if (crashed) {
                String trace = prefs.getString("stack_trace", "No crash log available");
                prefs.edit().putBoolean("crashed", false).remove("stack_trace").apply();
                Intent i = new Intent(this, CrashActivity.class);
                i.putExtra("crash_log", trace);
                i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(i);
                finish();
                return true;
            }
        } catch (Exception ignored) {
        }
        return false;
    }

    private void navigateToLogin() {
        // Clear guest session so AuthActivity's isLoggedIn observer doesn't
        // immediately bounce back to HomeActivity
        authViewModel.signOut(
                (success, error) -> {
                    OnboardingManager.get(HomeActivity.this).clearSession();
                    Intent i = new Intent(HomeActivity.this, AuthActivity.class);
                    i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(i);
                    finish();
                });
    }

    private void navigateToRegister() {
        authViewModel.signOut(
                (success, error) -> {
                    OnboardingManager.get(HomeActivity.this).clearSession();
                    Intent i = new Intent(HomeActivity.this, AuthActivity.class);
                    i.putExtra("start_register", true);
                    i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(i);
                    finish();
                });
    }

    // ── Streak flame animation ─────────────────────────────────────────────────

    /**
     * Runs a looping pulse → shake → pulse sequence on the streak flame icon.
     * Sequence per cycle (~4.6 s total):
     *   1. 3× smooth pulse  (scale 1→1.18→1, 700 ms each, FastOutSlowIn)
     *   2. Shake            (translateX ±6dp, 5 oscillations over 400 ms)
     *   3. Brief rest       (300 ms)
     *   4. Repeat
     */
    private void startStreakFlameAnimation() {
        if (streakFlameIcon == null) return;

        float shake = dp(6);

        // ── Pulse set (3 cycles) ──────────────────────────────────────────
        android.animation.AnimatorSet pulse = new android.animation.AnimatorSet();
        android.animation.AnimatorSet.Builder b = null;
        for (int i = 0; i < 3; i++) {
            android.animation.ObjectAnimator scaleUp = android.animation.ObjectAnimator.ofPropertyValuesHolder(
                    streakFlameIcon,
                    android.animation.PropertyValuesHolder.ofFloat("scaleX", 1f, 1.18f),
                    android.animation.PropertyValuesHolder.ofFloat("scaleY", 1f, 1.18f));
            scaleUp.setDuration(700);
            scaleUp.setInterpolator(new androidx.interpolator.view.animation.FastOutSlowInInterpolator());

            android.animation.ObjectAnimator scaleDown = android.animation.ObjectAnimator.ofPropertyValuesHolder(
                    streakFlameIcon,
                    android.animation.PropertyValuesHolder.ofFloat("scaleX", 1.18f, 1f),
                    android.animation.PropertyValuesHolder.ofFloat("scaleY", 1.18f, 1f));
            scaleDown.setDuration(700);
            scaleDown.setInterpolator(new androidx.interpolator.view.animation.FastOutSlowInInterpolator());

            if (b == null) {
                b = pulse.play(scaleUp).before(scaleDown);
            } else {
                b = b.before(scaleUp).before(scaleDown);
            }
        }

        // ── Shake (translateX oscillation) ────────────────────────────────
        android.animation.ObjectAnimator shakeAnim = android.animation.ObjectAnimator.ofFloat(
                streakFlameIcon, "translationX",
                0f, shake, -shake, shake, -shake, shake, -shake, shake, -shake, 0f);
        shakeAnim.setDuration(450);
        shakeAnim.setInterpolator(new android.view.animation.DecelerateInterpolator(1.5f));

        // ── Rest pause before next cycle ──────────────────────────────────
        android.animation.ValueAnimator rest = android.animation.ValueAnimator.ofFloat(0f, 0f);
        rest.setDuration(300);

        // ── Outer looping sequence ─────────────────────────────────────────
        android.animation.AnimatorSet full = new android.animation.AnimatorSet();
        full.playSequentially(pulse, shakeAnim, rest);
        full.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(android.animation.Animator animation) {
                if (isDestroyed()) return;
                if (streakFlameIcon != null) streakFlameIcon.setTranslationX(0f);
                full.start();
            }
        });
        streakAnimator = full;
        full.start();
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }

    /** Task 14: Handle deep link navigation */
    private void handleDeepLink(android.content.Intent intent) {
        if (intent == null) return;
        Uri data = intent.getData();
        if (data == null) return;
        String host = data.getHost();
        String id = data.getLastPathSegment();
        if ("novel".equals(host) && id != null) {
            android.content.Intent i =
                    new android.content.Intent(
                            this,
                            com.novelverse.app.presentation.novel.detail.NovelDetailActivity.class);
            i.putExtra("novel_id", id);
            startActivity(i);
        } else if ("chapter".equals(host) && id != null) {
            android.content.Intent i =
                    new android.content.Intent(
                            this,
                            com.novelverse.app.presentation.novel.reader.ReaderActivity.class);
            i.putExtra("chapter_id", id);
            startActivity(i);
        }
    }

    @Override
    protected void onNewIntent(android.content.Intent intent) {
        super.onNewIntent(intent);
        handleDeepLink(intent);
    }
}

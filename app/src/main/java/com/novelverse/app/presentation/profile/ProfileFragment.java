package com.novelverse.app.presentation.profile;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.novelverse.app.ui.banner.BannerHelper;
import com.novelverse.app.utils.GameAssets;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.novelverse.app.R;
import com.novelverse.app.data.local.entities.AchievementEntity;
import com.novelverse.app.data.local.entities.UserAchievementEntity;
import com.novelverse.app.data.local.entities.UserLevelEntity;
import com.novelverse.app.data.local.entities.UserStreakEntity;
import com.novelverse.app.data.local.preferences.UserPreferences;
import com.novelverse.app.domain.models.User;
import com.novelverse.app.presentation.auth.AuthActivity;
import com.novelverse.app.presentation.auth.AuthViewModel;
import com.novelverse.app.presentation.profile.achievements.AchievementsActivity;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * Profile screen — card-based design:
 *  - Horizontal hero: avatar (left) + name/handle/bio/email (right), banner above
 *  - XP / Level card: level badge from Supabase + progress bar + streak
 *  - Achievements preview card: count + 5 recent badge thumbnails from Supabase
 *  - Conditional: Become Writer / Author Dashboard
 *  - Account menu card
 *  - Sign Out card (always last)
 *
 * All gamification images load from the Supabase game-assets bucket via
 * Glide + GameAssets. No local AssetManager reads.
 */
@AndroidEntryPoint
public class ProfileFragment extends Fragment {

    @javax.inject.Inject
    com.novelverse.app.data.repository.LibraryRepository libraryRepository;

    @Inject UserPreferences prefs;

    private AuthViewModel         viewModel;
    private GamificationViewModel gamificationVm;

    // ── Identity ────────────────────────────────────────────────────────────
    private ImageView    avatarImage;
    private TextView     avatarInitials;
    private TextView     displayNameText;
    private TextView     usernameText;
    private TextView     bioText;
    private TextView     emailText;
    private boolean      emailMasked  = true;
    private String       currentEmail = "";
    private TextView     roleBadge;
    private TextView     subscriptionBadge;
    private TextView     pointsText;
    private TextView     statBooksRead;
    private TextView     statFollowers;
    private TextView     statFollowing;

    // ── XP / Level / Streak ─────────────────────────────────────────────────
    private ImageView    levelBadgeImage;
    private TextView     levelNameText;
    private TextView     levelProgressText;
    private ProgressBar  levelProgressBar;
    private LinearLayout streakRow;
    private TextView     streakCountText;

    // ── Achievements preview ─────────────────────────────────────────────────
    private LinearLayout achievementsPreviewCard;
    private TextView     achievementsUnlockedCount;
    private ImageView    achievementPreview1;
    private ImageView    achievementPreview2;
    private ImageView    achievementPreview3;
    private ImageView    achievementPreview4;
    private ImageView    achievementPreview5;
    private TextView     achievementsEmptyHint;

    // ── Conditional ─────────────────────────────────────────────────────────
    private FrameLayout  becomeWriterCard;
    private LinearLayout authorStatsSection;
    private TextView     statTotalEarnings;
    private TextView     statPayout;

    private View logoutButton;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel      = new ViewModelProvider(requireActivity()).get(AuthViewModel.class);
        gamificationVm = new ViewModelProvider(requireActivity()).get(GamificationViewModel.class);

        // Wire scroll-hide on the nav bar
        androidx.core.widget.NestedScrollView profileScroll = view.findViewById(R.id.profile_scroll);
        if (profileScroll != null && getActivity() instanceof com.novelverse.app.presentation.home.HomeActivity) {
            ((com.novelverse.app.presentation.home.HomeActivity) getActivity()).attachNavToScroll(profileScroll);
        }

        bindViews(view);

        viewModel.getCurrentUser().observe(getViewLifecycleOwner(), user -> {
            updateUI(user);
            if (user != null && user.getId() != null) {
                gamificationVm.init(user.getId());
            }
        });

        logoutButton.setOnClickListener(v -> showLogoutDialog());
        observeGamification();
    }

    // ─── View binding ────────────────────────────────────────────────────────

    private void bindViews(View v) {
        avatarImage       = v.findViewById(R.id.avatar_image);
        avatarInitials    = v.findViewById(R.id.avatar_initials);
        displayNameText   = v.findViewById(R.id.display_name_text);
        usernameText      = v.findViewById(R.id.username_text);
        bioText           = v.findViewById(R.id.bio_text);
        emailText         = v.findViewById(R.id.email_text);
        roleBadge         = v.findViewById(R.id.role_badge);
        subscriptionBadge = v.findViewById(R.id.subscription_badge);
        pointsText        = v.findViewById(R.id.points_text);
        statBooksRead     = v.findViewById(R.id.stat_books_read);
        statFollowers     = v.findViewById(R.id.stat_followers);
        statFollowing     = v.findViewById(R.id.stat_following);

        levelBadgeImage   = v.findViewById(R.id.level_badge_image);
        levelNameText     = v.findViewById(R.id.level_name_text);
        levelProgressText = v.findViewById(R.id.level_progress_text);
        levelProgressBar  = v.findViewById(R.id.level_progress_bar);
        streakRow         = v.findViewById(R.id.streak_row);
        streakCountText   = v.findViewById(R.id.streak_count_text);

        achievementsPreviewCard   = v.findViewById(R.id.achievements_preview_card);
        achievementsUnlockedCount = v.findViewById(R.id.achievements_unlocked_count);
        achievementPreview1       = v.findViewById(R.id.achievement_preview_1);
        achievementPreview2       = v.findViewById(R.id.achievement_preview_2);
        achievementPreview3       = v.findViewById(R.id.achievement_preview_3);
        achievementPreview4       = v.findViewById(R.id.achievement_preview_4);
        achievementPreview5       = v.findViewById(R.id.achievement_preview_5);
        achievementsEmptyHint     = v.findViewById(R.id.achievements_empty_hint);

        becomeWriterCard   = v.findViewById(R.id.become_writer_card);
        authorStatsSection = v.findViewById(R.id.author_stats_section);
        statTotalEarnings  = v.findViewById(R.id.stat_total_earnings);
        statPayout         = v.findViewById(R.id.stat_payout);
        logoutButton       = v.findViewById(R.id.logout_button);

        // Email tap — toggle mask
        if (emailText != null) {
            emailText.setOnClickListener(x -> {
                emailMasked = !emailMasked;
                emailText.setText(emailMasked ? maskEmail(currentEmail) : currentEmail);
            });
        }

        // Avatar tap → Edit Profile
        if (avatarImage != null) avatarImage.setOnClickListener(x ->
            startActivity(new Intent(requireContext(),
                com.novelverse.app.presentation.profile.edit.EditProfileActivity.class)));

        // Edit Profile button
        View btnEditProfile = v.findViewById(R.id.btn_edit_profile);
        if (btnEditProfile != null) btnEditProfile.setOnClickListener(x ->
            startActivity(new Intent(requireContext(),
                com.novelverse.app.presentation.profile.edit.EditProfileActivity.class)));

        // Become Writer
        TextView btnBecomeWriter = v.findViewById(R.id.btn_become_writer);
        if (btnBecomeWriter != null) btnBecomeWriter.setOnClickListener(x -> showBecomeWriterDialog());

        // Author Dashboard
        View btnViewDashboard = v.findViewById(R.id.btn_view_dashboard);
        if (btnViewDashboard != null) btnViewDashboard.setOnClickListener(x ->
            startActivity(new Intent(requireContext(),
                com.novelverse.app.presentation.author.analytics.AuthorAnalyticsActivity.class)));
        View authorSection = v.findViewById(R.id.author_stats_section);
        if (authorSection != null) authorSection.setOnClickListener(x ->
            startActivity(new Intent(requireContext(),
                com.novelverse.app.presentation.author.analytics.AuthorAnalyticsActivity.class)));

        // Points chip → Point Store
        View pointsContainer = v.findViewById(R.id.points_container);
        if (pointsContainer != null) pointsContainer.setOnClickListener(x ->
            startActivity(new Intent(requireContext(),
                com.novelverse.app.presentation.payment.store.PointStoreActivity.class)));

        // Achievements preview → full Achievements screen
        if (achievementsPreviewCard != null)
            achievementsPreviewCard.setOnClickListener(x ->
                startActivity(new Intent(requireContext(), AchievementsActivity.class)));
        View btnViewAll = v.findViewById(R.id.btn_view_all_achievements);
        if (btnViewAll != null) btnViewAll.setOnClickListener(x ->
            startActivity(new Intent(requireContext(), AchievementsActivity.class)));

        // Account menu
        View btnSettings = v.findViewById(R.id.btn_settings);
        if (btnSettings != null) btnSettings.setOnClickListener(x ->
            startActivity(new Intent(requireContext(),
                com.novelverse.app.presentation.profile.settings.SettingsActivity.class)));
        View btnReadingPrefs = v.findViewById(R.id.btn_reading_prefs);
        if (btnReadingPrefs != null) btnReadingPrefs.setOnClickListener(x ->
            startActivity(new Intent(requireContext(),
                com.novelverse.app.presentation.profile.reading.ReadingPrefsActivity.class)));
        View btnDownloads = v.findViewById(R.id.btn_downloads);
        if (btnDownloads != null) btnDownloads.setOnClickListener(x ->
            startActivity(new Intent(requireContext(),
                com.novelverse.app.presentation.profile.downloads.DownloadsActivity.class)));
        View btnHelp = v.findViewById(R.id.btn_help);
        if (btnHelp != null) btnHelp.setOnClickListener(x ->
            startActivity(new Intent(requireContext(),
                com.novelverse.app.presentation.profile.help.HelpActivity.class)));
    }

    // ─── Gamification observation ────────────────────────────────────────────

    private void observeGamification() {
        // Level + badge
        gamificationVm.getLevel().observe(getViewLifecycleOwner(), this::updateLevelCard);

        // Streak
        gamificationVm.getStreak().observe(getViewLifecycleOwner(), this::updateStreakRow);

        // Achievements preview: needs both catalog + user states
        final List<AchievementEntity>     catalogRef = new ArrayList<>();
        final List<UserAchievementEntity> statesRef  = new ArrayList<>();

        gamificationVm.getCatalog().observe(getViewLifecycleOwner(), catalog -> {
            catalogRef.clear();
            if (catalog != null) catalogRef.addAll(catalog);
            updateAchievementsPreview(catalogRef, statesRef);
        });
        gamificationVm.getAllAchievements().observe(getViewLifecycleOwner(), states -> {
            statesRef.clear();
            if (states != null) statesRef.addAll(states);
            updateAchievementsPreview(catalogRef, statesRef);
        });
    }

    private void updateLevelCard(UserLevelEntity lv) {
        if (lv == null || !isAdded()) return;

        int level = lv.getCurrentLevel();

        // Level badge from Supabase
        if (levelBadgeImage != null) {
            Glide.with(this)
                .load(GameAssets.getLevelBadgeUrl(level))
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .placeholder(R.drawable.ic_star_filled)
                .into(levelBadgeImage);
        }

        // Tier name + level number
        if (levelNameText != null) {
            levelNameText.setText(GameAssets.getLevelTierName(level) + "  ·  Lv " + level);
        }

        // XP progress
        if (levelProgressText != null) {
            levelProgressText.setText(lv.getXpInLevel() + " / " + lv.getXpLevelTarget() + " XP");
        }
        if (levelProgressBar != null && lv.getXpLevelTarget() > 0) {
            levelProgressBar.setMax(lv.getXpLevelTarget());
            levelProgressBar.setProgress(lv.getXpInLevel());
        }
    }

    private void updateStreakRow(UserStreakEntity streak) {
        if (streak == null || streakRow == null || !isAdded()) return;
        int current = streak.getCurrentStreak();
        if (current > 0) {
            streakRow.setVisibility(View.VISIBLE);
            if (streakCountText != null) streakCountText.setText(String.valueOf(current));
        } else {
            streakRow.setVisibility(View.GONE);
        }
    }

    private void updateAchievementsPreview(List<AchievementEntity>     catalog,
                                           List<UserAchievementEntity> states) {
        if (!isAdded()) return;

        int total    = catalog.size();
        int unlocked = 0;
        List<String> recentUrls = new ArrayList<>();

        // Walk user states (ordered most-recently-unlocked first by the DAO)
        for (UserAchievementEntity s : states) {
            if (s.isUnlocked() && s.getUnlockedAt() > 0) {  // FIX: long field, not nullable
                unlocked++;
                if (recentUrls.size() < 5) {
                    // Build badge URL directly from achievement ID via GameAssets
                    // FIX: AchievementEntity has no getBadgeUrl(); use GameAssets
                    for (AchievementEntity a : catalog) {
                        if (a.getId().equals(s.getAchievementId())) {
                            recentUrls.add(GameAssets.getAchievementUrl(a.getId()));
                            break;
                        }
                    }
                }
            }
        }

        if (achievementsUnlockedCount != null) {
            achievementsUnlockedCount.setText(unlocked + " / " + total);
        }

        ImageView[] previews = {
            achievementPreview1, achievementPreview2, achievementPreview3,
            achievementPreview4, achievementPreview5
        };
        for (int i = 0; i < previews.length; i++) {
            if (previews[i] == null) continue;
            if (i < recentUrls.size()) {
                previews[i].setVisibility(View.VISIBLE);
                Glide.with(this)
                    .load(recentUrls.get(i))
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .placeholder(R.drawable.ic_challenge)
                    .into(previews[i]);
            } else {
                previews[i].setVisibility(View.GONE);
            }
        }

        boolean hasUnlocked = unlocked > 0;
        if (achievementsEmptyHint != null)
            achievementsEmptyHint.setVisibility(hasUnlocked ? View.GONE : View.VISIBLE);
        View badgesRow = getView() != null ? getView().findViewById(R.id.achievement_badges_row) : null;
        if (badgesRow != null)
            badgesRow.setVisibility(hasUnlocked ? View.VISIBLE : View.GONE);
    }

    // ─── Identity data binding ───────────────────────────────────────────────

    private void updateUI(User user) {
        if (user == null) return;

        String name = user.getDisplayNameOrUsername();
        if (displayNameText != null) displayNameText.setText(name);
        if (usernameText != null && user.getUsername() != null)
            usernameText.setText("@" + user.getUsername());

        if (bioText != null) {
            String bio = user.getBio();
            if (bio != null && !bio.isEmpty()) { bioText.setText(bio); bioText.setVisibility(View.VISIBLE); }
            else bioText.setVisibility(View.GONE);
        }
        if (emailText != null) {
            currentEmail = user.getEmail();
            emailText.setText(maskEmail(currentEmail));
            emailMasked = true;
        }

        // Avatar initials
        if (avatarInitials != null && name != null && !name.isEmpty()) {
            String initials = String.valueOf(Character.toUpperCase(name.charAt(0)));
            if (name.contains(" ")) {
                String[] parts = name.split(" ");
                if (parts.length > 1 && !parts[1].isEmpty())
                    initials += Character.toUpperCase(parts[1].charAt(0));
            }
            avatarInitials.setText(initials);
        }

        // Avatar image via Glide
        String avatarUrl = user.getAvatarUrl();
        if (avatarUrl != null && !avatarUrl.isEmpty() && avatarImage != null) {
            Glide.with(this).load(avatarUrl)
                .placeholder(R.drawable.ic_avatar_placeholder).circleCrop().into(avatarImage);
            avatarImage.setVisibility(View.VISIBLE);
            if (avatarInitials != null) avatarInitials.setVisibility(View.GONE);
        } else {
            if (avatarImage    != null) avatarImage.setVisibility(View.GONE);
            if (avatarInitials != null) avatarInitials.setVisibility(View.VISIBLE);
        }

        applyRoleBadge(user.getRole());
        if (subscriptionBadge != null)
            subscriptionBadge.setText(user.getSubscriptionTier() != null ? capitalize(user.getSubscriptionTier()) : "Free");
        if (pointsText != null) pointsText.setText(user.getPointsBalance() + " pts");
        if (statFollowers != null) statFollowers.setText(formatCount(user.getFollowersCount()));
        if (statFollowing != null) statFollowing.setText(formatCount(user.getFollowingCount()));
        loadBooksReadCount(user.getId());

        String role    = user.getRole() != null ? user.getRole().toLowerCase() : "reader";
        boolean isWriter = "author".equals(role) || "admin".equals(role);
        if (becomeWriterCard   != null) becomeWriterCard.setVisibility(isWriter ? View.GONE  : View.VISIBLE);
        if (authorStatsSection != null) authorStatsSection.setVisibility(isWriter ? View.VISIBLE : View.GONE);
        if (isWriter) {
            if (statTotalEarnings != null) statTotalEarnings.setText(String.format("$%.2f", user.getTotalEarnings()));
            if (statPayout        != null) statPayout.setText(String.format("$%.2f", user.getAvailableForPayout()));
        }
    }

    private void loadBooksReadCount(String userId) {
        if (statBooksRead == null || libraryRepository == null) return;
        libraryRepository.getCompletedCount(userId, count ->
            requireActivity().runOnUiThread(() -> {
                if (statBooksRead != null) statBooksRead.setText(String.valueOf(count));
            }));
    }

    private void applyRoleBadge(String role) {
        if (roleBadge == null || getContext() == null) return;
        if (role == null) role = "reader";
        roleBadge.setText("✦ " + capitalize(role));
        switch (role.toLowerCase()) {
            case "admin":
                roleBadge.setBackgroundResource(R.drawable.bg_role_badge_admin);
                roleBadge.setTextColor(requireContext().getColor(R.color.error)); break;
            case "author":
                roleBadge.setBackgroundResource(R.drawable.bg_role_badge_author);
                roleBadge.setTextColor(requireContext().getColor(R.color.warning)); break;
            case "moderator":
                roleBadge.setBackgroundResource(R.drawable.bg_stat_card);
                roleBadge.setTextColor(requireContext().getColor(R.color.accent)); break;
            default:
                roleBadge.setBackgroundResource(R.drawable.bg_role_badge);
                roleBadge.setTextColor(requireContext().getColor(R.color.primary)); break;
        }
    }

    // ─── Dialogs ─────────────────────────────────────────────────────────────

    private void showBecomeWriterDialog() {
        if (getContext() == null) return;
        BottomSheetDialog sheet = new BottomSheetDialog(requireContext(), R.style.BottomSheetTheme);
        View root = LayoutInflater.from(requireContext()).inflate(R.layout.bottom_sheet_become_writer, null);
        root.findViewById(R.id.btn_start_writing).setOnClickListener(v -> { sheet.dismiss(); upgradeToAuthor(); });
        root.findViewById(R.id.btn_not_yet).setOnClickListener(v -> sheet.dismiss());
        sheet.setContentView(root); sheet.show();
    }

    private void upgradeToAuthor() {
        User user = viewModel.getCurrentUser().getValue();
        if (user == null) return;
        user.setRole("author");
        viewModel.updateProfile(user, (success, error) -> {
            if (!isAdded()) return;
            new android.os.Handler(android.os.Looper.getMainLooper()).post(() ->
                BannerHelper.success(requireActivity(), "Welcome!",
                    "You're now a writer. Go create your first novel!"));
        });
    }

    private void showLogoutDialog() {
        if (getContext() == null) return;
        BottomSheetDialog sheet = new BottomSheetDialog(requireContext(), R.style.BottomSheetTheme);
        View root = LayoutInflater.from(requireContext()).inflate(R.layout.bottom_sheet_logout, null);
        root.findViewById(R.id.btn_sign_out_confirm).setOnClickListener(v -> { sheet.dismiss(); performLogout(); });
        root.findViewById(R.id.btn_keep_reading).setOnClickListener(v -> sheet.dismiss());
        sheet.setContentView(root); sheet.show();
    }

    private void performLogout() {
        viewModel.signOut((success, error) -> {
            if (!isAdded()) return;
            if (success) {
                Intent intent = new Intent(requireContext(), AuthActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                requireActivity().finish();
            } else {
                BannerHelper.error(requireActivity(), "Sign out failed. Try again.");
            }
        });
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1).toLowerCase();
    }

    private String formatCount(int n) {
        if (n >= 1_000_000) return String.format("%.1fM", n / 1_000_000f);
        if (n >= 1_000)     return String.format("%.1fk", n / 1_000f);
        return String.valueOf(n);
    }

    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) return email;
        String[] parts = email.split("@");
        String local   = parts[0];
        String visible = local.length() > 2 ? local.substring(0, 2) : local.substring(0, 1);
        return visible + "***@" + parts[1];
    }
}

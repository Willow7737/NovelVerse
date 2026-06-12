package com.novelverse.app.presentation.profile;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
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
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.novelverse.app.R;
import com.novelverse.app.data.local.entities.AchievementEntity;
import com.novelverse.app.data.local.entities.UserAchievementEntity;
import com.novelverse.app.data.local.entities.UserLevelEntity;
import com.novelverse.app.data.local.entities.UserStreakEntity;
import com.novelverse.app.domain.models.User;
import com.novelverse.app.presentation.auth.AuthViewModel;
import com.novelverse.app.presentation.home.HomeActivity;
import com.novelverse.app.presentation.profile.achievements.AchievementsActivity;
import com.novelverse.app.presentation.profile.edit.EditProfileActivity;
import com.novelverse.app.presentation.profile.follows.FollowsActivity;
import com.novelverse.app.presentation.profile.settings.SettingsActivity;
import com.novelverse.app.ui.banner.BannerHelper;
import com.novelverse.app.utils.GameAssets;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * Profile screen — v2 redesign: - Toolbar hidden by HomeActivity while this fragment is active -
 * Full-bleed hero (no card): cover photo → overlapping avatar + status bubble → identity - Status
 * bubble derived from lastActiveAt (set to NOW on every login/profile fetch) - Cropping handled by
 * ImagePicker library (avatar = 1:1, cover = 3:1) - Settings / Reading Prefs / Downloads / Help all
 * route to SettingsActivity
 */
@AndroidEntryPoint
public class ProfileFragment extends Fragment {

    @Inject com.novelverse.app.data.repository.LibraryRepository libraryRepository;

    private AuthViewModel viewModel;
    private GamificationViewModel gamificationVm;

    // Hero
    private ImageView coverImage;
    private View bannerGradient;
    private View statusBubbleDot;
    private ImageView avatarImage;
    private TextView avatarInitials;
    private TextView displayNameText;
    private TextView usernameText;
    private TextView bioText;

    // Action chips
    private TextView roleBadge;
    private TextView subscriptionBadge;
    private TextView pointsText;

    // Stats
    private TextView statBooksRead;
    private TextView statFollowers;
    private TextView statFollowing;

    // XP / Level / Streak
    private ImageView levelBadgeImage;
    private TextView levelNameText;
    private TextView levelProgressText;
    private ProgressBar levelProgressBar;
    private LinearLayout xpSegmentsContainer;
    private LinearLayout streakRow;
    private TextView streakCountText;

    // Achievements
    private LinearLayout achievementsPreviewCard;
    private TextView achievementsUnlockedCount;
    private ImageView[] achievementPreviews = new ImageView[5];
    private TextView achievementsEmptyHint;

    // Conditional
    private FrameLayout becomeWriterCard;
    private LinearLayout authorStatsSection;
    private TextView statTotalEarnings;
    private TextView statPayout;

    private String currentUsername = "";
    private String currentAvatarUrl = "";
    private String currentCoverUrl = "";

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);

        viewModel = new ViewModelProvider(requireActivity()).get(AuthViewModel.class);
        gamificationVm = new ViewModelProvider(requireActivity()).get(GamificationViewModel.class);

        // Wire scroll-hide for the bottom nav bar
        androidx.core.widget.NestedScrollView scroll = v.findViewById(R.id.profile_scroll);
        if (scroll != null && getActivity() instanceof HomeActivity) {
            ((HomeActivity) getActivity()).attachNavToScroll(scroll);
        }

        bindViews(v);

        // Apply window insets: let the banner fill behind the status bar (edge-to-edge),
        // then push the camera-edit FAB and the identity/stats block below the inset
        ViewCompat.setOnApplyWindowInsetsListener(
                v,
                (view, insets) -> {
                    int statusBarHeight =
                            insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
                    int navBarHeight =
                            insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom;

                    // Expand banner to absorb the status bar
                    View banner = view.findViewById(R.id.banner_container);
                    if (banner != null) {
                        ViewGroup.LayoutParams lp = banner.getLayoutParams();
                        // base height 130dp + statusBarHeight
                        lp.height =
                                (int) (130 * view.getResources().getDisplayMetrics().density)
                                        + statusBarHeight;
                        banner.setLayoutParams(lp);
                    }

                    // Push the camera-edit button down by statusBarHeight so it's not under the bar
                    View btnCover = view.findViewById(R.id.btn_edit_cover);
                    if (btnCover != null) {
                        ViewGroup.MarginLayoutParams clp =
                                (ViewGroup.MarginLayoutParams) btnCover.getLayoutParams();
                        clp.bottomMargin =
                                (int) (12 * view.getResources().getDisplayMetrics().density);
                        clp.rightMargin =
                                (int) (12 * view.getResources().getDisplayMetrics().density);
                        btnCover.setLayoutParams(clp);
                    }

                    // Slide avatar FrameLayout down to stay overlapping the expanded banner
                    View avatarFrame = view.findViewById(R.id.avatar_frame);
                    if (avatarFrame != null) {
                        ViewGroup.MarginLayoutParams alp =
                                (ViewGroup.MarginLayoutParams) avatarFrame.getLayoutParams();
                        alp.topMargin =
                                (int) (86 * view.getResources().getDisplayMetrics().density)
                                        + statusBarHeight;
                        avatarFrame.setLayoutParams(alp);
                    }

                    // Add bottom padding for the nav bar (on top of the 80dp tab bar padding)
                    androidx.core.widget.NestedScrollView scrollView =
                            view.findViewById(R.id.profile_scroll);
                    if (scrollView != null) {
                        scrollView.setPadding(0, 0, 0, navBarHeight);
                        scrollView.setClipToPadding(false);
                    }

                    return insets;
                });

        viewModel
                .getCurrentUser()
                .observe(
                        getViewLifecycleOwner(),
                        user -> {
                            updateUI(user);
                            if (user != null && user.getId() != null) {
                                gamificationVm.init(user.getId());
                            }
                        });

        observeGamification();
    }

    // ─── View binding ────────────────────────────────────────────────────────

    private void bindViews(View v) {
        coverImage = v.findViewById(R.id.cover_image);
        bannerGradient = v.findViewById(R.id.banner_gradient);
        statusBubbleDot = v.findViewById(R.id.status_bubble_dot);
        avatarImage = v.findViewById(R.id.avatar_image);
        avatarInitials = v.findViewById(R.id.avatar_initials);
        displayNameText = v.findViewById(R.id.display_name_text);
        usernameText = v.findViewById(R.id.username_text);
        bioText = v.findViewById(R.id.bio_text);

        roleBadge = v.findViewById(R.id.role_badge);
        subscriptionBadge = v.findViewById(R.id.subscription_badge);
        pointsText = v.findViewById(R.id.points_text);

        statBooksRead = v.findViewById(R.id.stat_books_read);
        statFollowers = v.findViewById(R.id.stat_followers);
        statFollowing = v.findViewById(R.id.stat_following);

        levelBadgeImage = v.findViewById(R.id.level_badge_image);
        levelNameText = v.findViewById(R.id.level_name_text);
        levelProgressText = v.findViewById(R.id.level_progress_text);
        levelProgressBar = v.findViewById(R.id.level_progress_bar);
        xpSegmentsContainer = v.findViewById(R.id.xp_segments_container);
        streakRow = v.findViewById(R.id.streak_row);
        streakCountText = v.findViewById(R.id.streak_count_text);

        achievementsPreviewCard = v.findViewById(R.id.achievements_preview_card);
        achievementsUnlockedCount = v.findViewById(R.id.achievements_unlocked_count);
        achievementPreviews[0] = v.findViewById(R.id.achievement_preview_1);
        achievementPreviews[1] = v.findViewById(R.id.achievement_preview_2);
        achievementPreviews[2] = v.findViewById(R.id.achievement_preview_3);
        achievementPreviews[3] = v.findViewById(R.id.achievement_preview_4);
        achievementPreviews[4] = v.findViewById(R.id.achievement_preview_5);
        achievementsEmptyHint = v.findViewById(R.id.achievements_empty_hint);

        becomeWriterCard = v.findViewById(R.id.become_writer_card);
        authorStatsSection = v.findViewById(R.id.author_stats_section);
        statTotalEarnings = v.findViewById(R.id.stat_total_earnings);
        statPayout = v.findViewById(R.id.stat_payout);

        // ── Click wiring ────────────────────────────────────────────────────

        // Cover photo — tapping the banner opens the cover in the viewer
        //               the camera button in the corner still goes to EditProfile
        View bannerContainer = v.findViewById(R.id.banner_container);
        View btnEditCover = v.findViewById(R.id.btn_edit_cover);
        if (bannerContainer != null) bannerContainer.setOnClickListener(x -> openCoverViewer());
        if (btnEditCover != null) btnEditCover.setOnClickListener(x -> openCoverPicker());

        // Avatar → opens avatar in the full-screen viewer
        if (avatarImage != null) avatarImage.setOnClickListener(x -> openAvatarViewer());
        if (avatarInitials != null) avatarInitials.setOnClickListener(x -> openEditProfile());

        // Edit Profile button
        View btnEdit = v.findViewById(R.id.btn_edit_profile);
        if (btnEdit != null) btnEdit.setOnClickListener(x -> openEditProfile());

        // Points chip
        View pointsContainer = v.findViewById(R.id.points_container);
        if (pointsContainer != null)
            pointsContainer.setOnClickListener(
                    x ->
                            go(
                                    com.novelverse.app.presentation.payment.store.PointStoreActivity
                                            .class));

        // Stats tap → followers / following sheets
        View followersContainer = v.findViewById(R.id.stat_followers_container);
        View followingContainer = v.findViewById(R.id.stat_following_container);
        if (followersContainer != null)
            followersContainer.setOnClickListener(x -> showFollowersSheet());
        if (followingContainer != null)
            followingContainer.setOnClickListener(x -> showFollowingSheet());

        // Achievements
        if (achievementsPreviewCard != null)
            achievementsPreviewCard.setOnClickListener(x -> go(AchievementsActivity.class));
        View btnViewAll = v.findViewById(R.id.btn_view_all_achievements);
        if (btnViewAll != null) btnViewAll.setOnClickListener(x -> go(AchievementsActivity.class));

        // Author Dashboard
        View btnDash = v.findViewById(R.id.btn_view_dashboard);
        if (btnDash != null)
            btnDash.setOnClickListener(
                    x ->
                            go(
                                    com.novelverse.app.presentation.author.analytics
                                            .AuthorAnalyticsActivity.class));
        if (authorStatsSection != null)
            authorStatsSection.setOnClickListener(
                    x ->
                            go(
                                    com.novelverse.app.presentation.author.analytics
                                            .AuthorAnalyticsActivity.class));

        // Become Writer
        TextView btnBecomeWriter = v.findViewById(R.id.btn_become_writer);
        if (btnBecomeWriter != null)
            btnBecomeWriter.setOnClickListener(x -> showBecomeWriterDialog());

        // Quick Actions (now live inside the banner, but IDs are unchanged)
        bindQuickAction(v, R.id.quick_share, x -> shareProfile());
        bindQuickAction(v, R.id.quick_copy_link, x -> copyProfileLink());
        bindQuickAction(v, R.id.quick_settings, x -> go(SettingsActivity.class));
        bindQuickAction(
                v,
                R.id.quick_premium,
                x -> go(com.novelverse.app.presentation.payment.store.PointStoreActivity.class));
    }

    private void bindQuickAction(View root, int id, View.OnClickListener l) {
        View chip = root.findViewById(id);
        if (chip != null) chip.setOnClickListener(l);
    }

    private void openEditProfile() {
        go(EditProfileActivity.class);
    }

    private void openCoverPicker() {
        Intent i = new Intent(requireContext(), EditProfileActivity.class);
        i.putExtra("open_cover_picker", true);
        startActivity(i);
    }

    /** Opens the cover photo in the swipe-to-dismiss full-screen viewer. */
    private void openCoverViewer() {
        if (!isAdded()) return;
        if (currentCoverUrl != null && !currentCoverUrl.isEmpty()) {
            com.novelverse.app.presentation.common.ImageViewerActivity.start(
                    requireContext(), currentCoverUrl, "Cover photo");
        } else {
            // No custom cover — open EditProfile to let them set one
            openCoverPicker();
        }
    }

    /** Opens the avatar in the swipe-to-dismiss full-screen viewer. */
    private void openAvatarViewer() {
        if (!isAdded()) return;
        if (currentAvatarUrl != null && !currentAvatarUrl.isEmpty()) {
            com.novelverse.app.presentation.common.ImageViewerActivity.start(
                    requireContext(), currentAvatarUrl, "Profile photo");
        } else {
            // No avatar photo yet — go to edit
            openEditProfile();
        }
    }

    private void go(Class<?> activityClass) {
        startActivity(new Intent(requireContext(), activityClass));
    }

    // ─── Gamification observation ────────────────────────────────────────────

    private void observeGamification() {
        gamificationVm.getLevel().observe(getViewLifecycleOwner(), this::updateLevelCard);
        gamificationVm.getStreak().observe(getViewLifecycleOwner(), this::updateStreakRow);

        final List<AchievementEntity> catalog = new ArrayList<>();
        final List<UserAchievementEntity> states = new ArrayList<>();

        gamificationVm
                .getCatalog()
                .observe(
                        getViewLifecycleOwner(),
                        c -> {
                            catalog.clear();
                            if (c != null) catalog.addAll(c);
                            updateAchievementsPreview(catalog, states);
                        });
        gamificationVm
                .getAllAchievements()
                .observe(
                        getViewLifecycleOwner(),
                        s -> {
                            states.clear();
                            if (s != null) states.addAll(s);
                            updateAchievementsPreview(catalog, states);
                        });
    }

    private void updateLevelCard(UserLevelEntity lv) {
        if (lv == null || !isAdded()) return;
        int level = lv.getCurrentLevel();

        if (levelBadgeImage != null) {
            Glide.with(this)
                    .load(GameAssets.getLevelBadgeUrl(level))
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .placeholder(R.drawable.ic_star_filled)
                    .into(levelBadgeImage);
        }
        if (levelNameText != null)
            levelNameText.setText(GameAssets.getLevelTierName(level) + "  ·  Lv " + level);
        if (levelProgressText != null)
            levelProgressText.setText(lv.getXpInLevel() + " / " + lv.getXpLevelTarget() + " XP");
        if (levelProgressBar != null && lv.getXpLevelTarget() > 0) {
            levelProgressBar.setMax(lv.getXpLevelTarget());
            levelProgressBar.setProgress(lv.getXpInLevel());
        }
        buildXpSegments(lv.getXpInLevel(), lv.getXpLevelTarget());
    }

    private void buildXpSegments(int current, int max) {
        if (xpSegmentsContainer == null || !isAdded()) return;
        xpSegmentsContainer.removeAllViews();
        if (max <= 0) return;

        int totalSeg = 10;
        int filledSeg = (int) Math.round((double) current / max * totalSeg);
        int gapPx = Math.round(3 * getResources().getDisplayMetrics().density);

        for (int i = 0; i < totalSeg; i++) {
            View seg = new View(requireContext());
            LinearLayout.LayoutParams p =
                    new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f);
            if (i < totalSeg - 1) p.rightMargin = gapPx;
            seg.setLayoutParams(p);
            seg.setBackgroundResource(
                    i < filledSeg
                            ? R.drawable.bg_xp_segment_filled
                            : R.drawable.bg_xp_segment_empty);
            xpSegmentsContainer.addView(seg);
        }
    }

    private void updateStreakRow(UserStreakEntity streak) {
        if (streak == null || streakRow == null || !isAdded()) return;
        int current = streak.getCurrentStreak();
        streakRow.setVisibility(current > 0 ? View.VISIBLE : View.GONE);
        if (current > 0 && streakCountText != null)
            streakCountText.setText(String.valueOf(current));
    }

    private void updateAchievementsPreview(
            List<AchievementEntity> catalog, List<UserAchievementEntity> states) {
        if (!isAdded()) return;

        int total = catalog.size(), unlocked = 0;
        List<String> recentUrls = new ArrayList<>();

        for (UserAchievementEntity s : states) {
            if (!s.isUnlocked()) continue;
            unlocked++;
            if (recentUrls.size() < 5) {
                for (AchievementEntity a : catalog) {
                    if (a.getId().equals(s.getAchievementId())) {
                        recentUrls.add(GameAssets.getAchievementUrl(a.getId()));
                        break;
                    }
                }
            }
        }

        if (achievementsUnlockedCount != null)
            achievementsUnlockedCount.setText(unlocked + " / " + total);

        for (int i = 0; i < achievementPreviews.length; i++) {
            ImageView iv = achievementPreviews[i];
            if (iv == null) continue;
            if (i < recentUrls.size()) {
                iv.setVisibility(View.VISIBLE);
                Glide.with(this)
                        .load(recentUrls.get(i))
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .placeholder(R.drawable.ic_challenge)
                        .into(iv);
            } else {
                iv.setVisibility(View.GONE);
            }
        }

        boolean hasUnlocked = unlocked > 0;
        if (achievementsEmptyHint != null)
            achievementsEmptyHint.setVisibility(hasUnlocked ? View.GONE : View.VISIBLE);
        View badgesRow =
                getView() != null ? getView().findViewById(R.id.achievement_badges_row) : null;
        if (badgesRow != null) badgesRow.setVisibility(hasUnlocked ? View.VISIBLE : View.GONE);
    }

    // ─── User data binding ───────────────────────────────────────────────────

    private void updateUI(User user) {
        if (user == null) return;

        String name = user.getDisplayNameOrUsername();
        currentUsername = user.getUsername() != null ? user.getUsername() : "";

        if (displayNameText != null) displayNameText.setText(name);
        if (usernameText != null && currentUsername != null)
            usernameText.setText("@" + currentUsername);

        // Bio
        if (bioText != null) {
            String bio = user.getBio();
            boolean hasBio = bio != null && !bio.isEmpty();
            bioText.setText(hasBio ? bio : "");
            bioText.setVisibility(hasBio ? View.VISIBLE : View.GONE);
        }

        // Avatar initials
        if (avatarInitials != null && name != null && !name.isEmpty()) {
            String init = String.valueOf(Character.toUpperCase(name.charAt(0)));
            if (name.contains(" ")) {
                String[] parts = name.split(" ");
                if (parts.length > 1 && !parts[1].isEmpty())
                    init += Character.toUpperCase(parts[1].charAt(0));
            }
            avatarInitials.setText(init);
        }

        // Avatar image
        String avatarUrl = user.getAvatarUrl();
        currentAvatarUrl = avatarUrl != null ? avatarUrl : "";
        if (avatarUrl != null && !avatarUrl.isEmpty() && avatarImage != null) {
            Glide.with(this)
                    .load(avatarUrl)
                    .placeholder(R.drawable.ic_avatar_placeholder)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .circleCrop()
                    .into(avatarImage);
            avatarImage.setVisibility(View.VISIBLE);
            if (avatarInitials != null) avatarInitials.setVisibility(View.GONE);
        } else {
            if (avatarImage != null) avatarImage.setVisibility(View.GONE);
            if (avatarInitials != null) avatarInitials.setVisibility(View.VISIBLE);
        }

        // Cover photo
        loadCoverPhoto(user);

        // Status bubble — lastActiveAt is stamped to NOW in mergeProfileJson,
        // so the current user will always show as online.
        updateStatusBubble(user.getLastActiveAt());

        // Role / subscription chips
        applyRoleBadge(user.getRole());
        if (subscriptionBadge != null)
            subscriptionBadge.setText(
                    user.getSubscriptionTier() != null
                            ? capitalize(user.getSubscriptionTier())
                            : "Free");
        if (pointsText != null) pointsText.setText(user.getPointsBalance() + " pts");

        // Stats
        if (statFollowers != null) statFollowers.setText(formatCount(user.getFollowersCount()));
        if (statFollowing != null) statFollowing.setText(formatCount(user.getFollowingCount()));
        loadBooksReadCount(user.getId());

        // Role-conditional cards
        String role = user.getRole() != null ? user.getRole().toLowerCase() : "reader";
        boolean isWriter = "author".equals(role) || "admin".equals(role);
        if (becomeWriterCard != null)
            becomeWriterCard.setVisibility(isWriter ? View.GONE : View.VISIBLE);
        if (authorStatsSection != null)
            authorStatsSection.setVisibility(isWriter ? View.VISIBLE : View.GONE);
        if (isWriter) {
            if (statTotalEarnings != null)
                statTotalEarnings.setText(String.format("$%.2f", user.getTotalEarnings()));
            if (statPayout != null)
                statPayout.setText(String.format("$%.2f", user.getAvailableForPayout()));
        }
    }

    private void loadCoverPhoto(User user) {
        if (coverImage == null || bannerGradient == null || !isAdded()) return;
        String coverUrl = user.getCoverUrl();
        currentCoverUrl = coverUrl != null ? coverUrl : "";
        if (coverUrl != null && !coverUrl.isEmpty()) {
            coverImage.setVisibility(View.VISIBLE);
            bannerGradient.setVisibility(View.GONE);
            Glide.with(this)
                    .load(coverUrl)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .centerCrop()
                    .placeholder(R.drawable.bg_profile_banner)
                    .into(coverImage);
        } else {
            coverImage.setVisibility(View.GONE);
            bannerGradient.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Status bubble: - lastActiveAt is always stamped to NOW in mergeProfileJson, so the logged-in
     * user will always show green (online). - When viewing other users' profiles (future), pass
     * their lastActiveAt here. - null → offline (gray).
     */
    private void updateStatusBubble(Date lastActiveAt) {
        if (statusBubbleDot == null || !isAdded()) return;
        if (lastActiveAt == null) {
            statusBubbleDot.setBackgroundResource(R.drawable.bg_status_offline);
            return;
        }
        long diffMin = (System.currentTimeMillis() - lastActiveAt.getTime()) / 60_000;
        if (diffMin < 5) statusBubbleDot.setBackgroundResource(R.drawable.bg_status_active);
        else if (diffMin < 30) statusBubbleDot.setBackgroundResource(R.drawable.bg_status_away);
        else statusBubbleDot.setBackgroundResource(R.drawable.bg_status_offline);
    }

    private void loadBooksReadCount(String userId) {
        if (statBooksRead == null || libraryRepository == null) return;
        libraryRepository.getCompletedCount(
                userId,
                count ->
                        requireActivity()
                                .runOnUiThread(
                                        () -> {
                                            if (statBooksRead != null)
                                                statBooksRead.setText(String.valueOf(count));
                                        }));
    }

    private void applyRoleBadge(String role) {
        if (roleBadge == null || getContext() == null) return;
        if (role == null) role = "reader";
        roleBadge.setText("✦ " + capitalize(role));
        switch (role.toLowerCase()) {
            case "admin":
                roleBadge.setTextColor(requireContext().getColor(R.color.error));
                break;
            case "author":
                roleBadge.setTextColor(requireContext().getColor(R.color.warning));
                break;
            default:
                roleBadge.setTextColor(requireContext().getColor(R.color.primary));
                break;
        }
    }

    // ─── Quick actions ───────────────────────────────────────────────────────

    private void shareProfile() {
        if (!isAdded()) return;
        Intent share = new Intent(Intent.ACTION_SEND);
        share.setType("text/plain");
        share.putExtra(
                Intent.EXTRA_TEXT,
                "Check out my NovelVerse profile: novelverse.app/u/" + currentUsername);
        startActivity(Intent.createChooser(share, "Share Profile"));
    }

    private void copyProfileLink() {
        if (!isAdded()) return;
        ClipboardManager cb =
                (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
        if (cb != null)
            cb.setPrimaryClip(
                    ClipData.newPlainText("Profile link", "novelverse.app/u/" + currentUsername));
        Toast.makeText(requireContext(), "Link copied!", Toast.LENGTH_SHORT).show();
    }

    // ─── Follower sheets ────────

    private void showFollowersSheet() {
        openFollowsActivity(FollowsActivity.TAB_FOLLOWERS);
    }

    private void showFollowingSheet() {
        openFollowsActivity(FollowsActivity.TAB_FOLLOWING);
    }

    private void openFollowsActivity(int tab) {
        User user = viewModel.getCurrentUser().getValue();
        if (user == null || user.getId() == null) return;
        Intent intent = new Intent(requireContext(), FollowsActivity.class);
        intent.putExtra(FollowsActivity.EXTRA_USER_ID, user.getId());
        intent.putExtra(FollowsActivity.EXTRA_INITIAL_TAB, tab);
        startActivity(intent);
    }

    // ─── Dialogs ─────────────────────────────────────────────────────────────

    private void showBecomeWriterDialog() {
        if (getContext() == null) return;
        BottomSheetDialog sheet = new BottomSheetDialog(requireContext(), R.style.BottomSheetTheme);
        View root =
                LayoutInflater.from(requireContext())
                        .inflate(R.layout.bottom_sheet_become_writer, null);
        root.findViewById(R.id.btn_start_writing)
                .setOnClickListener(
                        v -> {
                            sheet.dismiss();
                            upgradeToAuthor();
                        });
        root.findViewById(R.id.btn_not_yet).setOnClickListener(v -> sheet.dismiss());
        sheet.setContentView(root);
        sheet.show();
    }

    private void upgradeToAuthor() {
        User user = viewModel.getCurrentUser().getValue();
        if (user == null) return;
        user.setRole("author");
        viewModel.updateProfile(
                user,
                (success, error) -> {
                    if (!isAdded()) return;
                    new android.os.Handler(android.os.Looper.getMainLooper())
                            .post(
                                    () ->
                                            BannerHelper.success(
                                                    requireActivity(),
                                                    "Welcome!",
                                                    "You're now a writer. Create your first novel!"));
                });
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1).toLowerCase();
    }

    private String formatCount(int n) {
        if (n >= 1_000_000) return String.format("%.1fM", n / 1_000_000f);
        if (n >= 1_000) return String.format("%.1fk", n / 1_000f);
        return String.valueOf(n);
    }
}

package com.novelverse.app.presentation.common.views;

import android.app.Activity;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.novelverse.app.R;
import com.novelverse.app.data.local.entities.AchievementEntity;
import com.novelverse.app.domain.gamification.AchievementDefinitions;

import java.io.IOException;
import java.io.InputStream;

/**
 * Dopamine-hit achievement toast.
 * Slides up from the bottom, stays 3s, then slides away.
 * Loads PNG from assets/gamification_icons/ with rarity-color fallback.
 */
public class AchievementToastView {

    public static void show(Activity activity, AchievementEntity achievement) {
        if (activity == null || achievement == null || activity.isFinishing()) return;

        ViewGroup root = activity.findViewById(android.R.id.content);
        View toast = LayoutInflater.from(activity)
            .inflate(R.layout.view_achievement_toast, root, false);

        ImageView badge  = toast.findViewById(R.id.achievement_badge_icon);
        TextView  label  = toast.findViewById(R.id.achievement_label);
        TextView  title  = toast.findViewById(R.id.achievement_title);
        TextView  desc   = toast.findViewById(R.id.achievement_desc);
        TextView  reward = toast.findViewById(R.id.achievement_reward);
        View      rarityBar = toast.findViewById(R.id.rarity_accent_bar);

        if (label != null) label.setText("Achievement Unlocked!");
        if (title != null) title.setText(achievement.getTitle());
        if (desc  != null) desc.setText(achievement.getDescription());
        if (reward != null) {
            StringBuilder sb = new StringBuilder();
            if (achievement.getXpReward() > 0)   sb.append("+").append(achievement.getXpReward()).append(" XP  ");
            if (achievement.getInkReward() > 0)   sb.append("+").append(achievement.getInkReward()).append(" Ink");
            if (achievement.getQuillReward() > 0)  sb.append("  +").append(achievement.getQuillReward()).append(" Quill");
            reward.setText(sb.toString().trim());
        }

        // Rarity accent bar color
        if (rarityBar != null) {
            String color = AchievementDefinitions.rarityColor(achievement.getRarity());
            try {
                rarityBar.setBackgroundColor(android.graphics.Color.parseColor(color));
            } catch (Exception ignored) {}
        }

        // Load PNG badge from assets
        if (badge != null && achievement.getAssetName() != null) {
            Bitmap bmp = loadAssetBitmap(activity, "gamification_icons/" + achievement.getAssetName());
            if (bmp != null) {
                badge.setImageBitmap(bmp);
            } else {
                badge.setBackgroundColor(android.graphics.Color.parseColor(
                    AchievementDefinitions.rarityColor(achievement.getRarity())));
            }
        }

        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.WRAP_CONTENT);
        lp.gravity = android.view.Gravity.BOTTOM | android.view.Gravity.CENTER_HORIZONTAL;
        lp.bottomMargin = 120;
        lp.leftMargin = 16;
        lp.rightMargin = 16;
        toast.setLayoutParams(lp);

        // Animate in
        toast.setTranslationY(400f);
        toast.setAlpha(0f);
        root.addView(toast);
        toast.animate()
            .translationY(0f)
            .alpha(1f)
            .setDuration(350)
            .setInterpolator(new android.view.animation.OvershootInterpolator(0.8f))
            .start();

        // Auto-dismiss after 3.5s
        toast.postDelayed(() -> toast.animate()
            .translationY(400f)
            .alpha(0f)
            .setDuration(300)
            .withEndAction(() -> {
                if (toast.getParent() != null) root.removeView(toast);
            })
            .start(), 3500);
    }

    /** Show for the legacy Achievement model (backward compat) */
    public static void show(Activity activity, com.novelverse.app.domain.models.Achievement achievement) {
        if (activity == null || achievement == null || activity.isFinishing()) return;
        // Wrap in a temporary AchievementEntity for display
        AchievementEntity e = new AchievementEntity();
        e.setTitle(achievement.getTitle());
        e.setDescription(achievement.getDescription());
        e.setXpReward(achievement.getPointsAwarded());
        e.setRarity("COMMON");
        show(activity, e);
    }

    private static Bitmap loadAssetBitmap(Activity activity, String path) {
        try {
            AssetManager assets = activity.getAssets();
            InputStream is = assets.open(path);
            Bitmap bmp = BitmapFactory.decodeStream(is);
            is.close();
            return bmp;
        } catch (IOException e) {
            return null;
        }
    }
}

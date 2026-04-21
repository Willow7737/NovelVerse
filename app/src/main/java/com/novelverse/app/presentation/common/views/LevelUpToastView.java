package com.novelverse.app.presentation.common.views;

import android.app.Activity;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.OvershootInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.novelverse.app.R;
import com.novelverse.app.domain.gamification.XpLevelEngine;

import java.io.IOException;
import java.io.InputStream;

/**
 * Full-width level-up celebration card.
 * Shows for 4s then auto-dismisses.
 * Fires from GamificationViewModel.addXp() when didLevelUp == true.
 */
public class LevelUpToastView {

    public static void show(Activity activity, int newLevel) {
        if (activity == null || activity.isFinishing()) return;

        ViewGroup root = activity.findViewById(android.R.id.content);
        View toast = LayoutInflater.from(activity)
            .inflate(R.layout.view_levelup_toast, root, false);

        ImageView burstIcon = toast.findViewById(R.id.levelup_burst_icon);
        ImageView badgeIcon = toast.findViewById(R.id.levelup_badge_icon);
        TextView  levelText = toast.findViewById(R.id.levelup_level_text);
        TextView  perkText  = toast.findViewById(R.id.levelup_perk_text);

        if (levelText != null) levelText.setText("Level " + newLevel + " Reached!");

        // Perk hint
        if (perkText != null) {
            if (newLevel == 10) perkText.setText("2nd badge slot unlocked!");
            else if (newLevel == 25) perkText.setText("3rd badge slot + animated avatars!");
            else if (newLevel == 50) perkText.setText("Veteran title + exclusive theme!");
            else perkText.setText("Keep reading to unlock more perks!");
        }

        // Load PNG assets
        loadAsset(activity, burstIcon, "gamification_icons/level_up_burst.png");
        loadAsset(activity, badgeIcon, "gamification_icons/level_badge_frame.png");

        // Layout params: centered near top
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.WRAP_CONTENT);
        lp.gravity = android.view.Gravity.TOP | android.view.Gravity.CENTER_HORIZONTAL;
        lp.topMargin = 80;
        lp.leftMargin = 24;
        lp.rightMargin = 24;
        toast.setLayoutParams(lp);

        // Animate in from top
        toast.setTranslationY(-300f);
        toast.setAlpha(0f);
        toast.setScaleX(0.85f);
        toast.setScaleY(0.85f);
        root.addView(toast);
        toast.animate()
            .translationY(0f)
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(450)
            .setInterpolator(new OvershootInterpolator(1.1f))
            .start();

        // Auto-dismiss after 4s
        toast.postDelayed(() -> toast.animate()
            .translationY(-300f)
            .alpha(0f)
            .setDuration(350)
            .withEndAction(() -> {
                if (toast.getParent() != null) root.removeView(toast);
            })
            .start(), 4000);
    }

    private static void loadAsset(Activity activity, ImageView iv, String path) {
        if (iv == null || activity == null) return;
        try {
            AssetManager assets = activity.getAssets();
            InputStream is = assets.open(path);
            Bitmap bmp = BitmapFactory.decodeStream(is);
            is.close();
            iv.setImageBitmap(bmp);
        } catch (IOException ignored) {}
    }
}

package com.novelverse.app.presentation.common.views;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.novelverse.app.R;
import com.novelverse.app.data.local.entities.UserStreakEntity;

import java.io.IOException;
import java.io.InputStream;

/**
 * Compound view (horizontal LinearLayout) showing:
 *   [🔥 flame] 7  day streak   [❄️ or 🛡 badge when active]
 *
 * Usage in XML:
 *   <com.novelverse.app.presentation.common.views.StreakWidgetView
 *       android:layout_width="wrap_content"
 *       android:layout_height="wrap_content" />
 */
public class StreakWidgetView extends LinearLayout {

    private ImageView flameIcon;
    private ImageView statusBadge;
    private TextView  streakCount;
    private TextView  streakLabel;

    public StreakWidgetView(Context context) {
        super(context); init(context);
    }
    public StreakWidgetView(Context context, AttributeSet attrs) {
        super(context, attrs); init(context);
    }
    public StreakWidgetView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle); init(context);
    }

    private void init(Context context) {
        setOrientation(HORIZONTAL);
        setGravity(android.view.Gravity.CENTER_VERTICAL);
        LayoutInflater.from(context).inflate(R.layout.view_streak_widget, this, true);
        flameIcon   = findViewById(R.id.streak_flame_icon);
        statusBadge = findViewById(R.id.streak_status_badge);
        streakCount = findViewById(R.id.streak_count_text);
        streakLabel = findViewById(R.id.streak_label_text);

        loadAsset(context, flameIcon, "gamification_icons/ui_flame_streak.png");
    }

    public void bind(UserStreakEntity streak, long nowMs) {
        if (streak == null) {
            setText(streakCount, "0");
            setText(streakLabel, "day streak");
            if (statusBadge != null) statusBadge.setVisibility(GONE);
            return;
        }

        int current = streak.getCurrentStreak();
        setText(streakCount, String.valueOf(current));
        setText(streakLabel, current == 1 ? "day streak" : "day streak");

        if (statusBadge != null) {
            boolean freezeActive = streak.getFreezeExpiresAt() > nowMs;
            if (freezeActive) {
                statusBadge.setVisibility(VISIBLE);
                loadAsset(getContext(), statusBadge, "gamification_icons/ui_snowflake_freeze.png");
            } else if (current >= 30) {
                statusBadge.setVisibility(VISIBLE);
                loadAsset(getContext(), statusBadge, "gamification_icons/ui_shield_recovery.png");
            } else {
                statusBadge.setVisibility(GONE);
            }
        }
    }

    private void setText(TextView tv, String v) { if (tv != null) tv.setText(v); }

    private void loadAsset(Context ctx, ImageView iv, String path) {
        if (iv == null) return;
        try {
            AssetManager assets = ctx.getAssets();
            InputStream is = assets.open(path);
            Bitmap bmp = BitmapFactory.decodeStream(is);
            is.close();
            iv.setImageBitmap(bmp);
        } catch (IOException ignored) {}
    }
}

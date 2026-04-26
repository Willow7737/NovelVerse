package com.novelverse.app.utils;

import android.content.Context;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.HapticFeedbackConstants;
import android.view.View;

/**
 * Utility class for haptic feedback across the app.
 * Provides consistent tactile feedback for buttons, chips, and other interactive elements.
 */
public class HapticUtils {

    private HapticUtils() {
        // utility class
    }

    /**
     * Light haptic feedback for button presses and chip selections.
     */
    public static void light(View view) {
        if (view == null) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
        } else {
            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
        }
    }

    /**
     * Medium haptic feedback for important actions (e.g., completing onboarding step).
     */
    public static void medium(View view) {
        if (view == null) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            view.performHapticFeedback(HapticFeedbackConstants.CONFIRM);
        } else {
            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
        }
    }

    /**
     * Heavy haptic feedback for errors or critical actions.
     */
    public static void heavy(View view) {
        if (view == null) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            view.performHapticFeedback(HapticFeedbackConstants.REJECT);
        } else {
            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
        }
    }

    /**
     * Tick feedback for small interactions like toggles.
     */
    public static void tick(View view) {
        if (view == null) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
        } else {
            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
        }
    }

    /**
     * Apply a scale-press animation + haptic to a view.
     */
    public static void press(View view) {
        if (view == null) return;
        light(view);
        view.animate()
                .scaleX(0.97f)
                .scaleY(0.97f)
                .setDuration(100)
                .start();
    }

    /**
     * Release the scale-press animation.
     */
    public static void release(View view) {
        if (view == null) return;
        view.animate()
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(100)
                .start();
    }

    /**
     * Custom vibration pattern for older devices without haptic feedback.
     */
    public static void vibrate(Context context, long milliseconds) {
        if (context == null) return;
        Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator == null || !vibrator.hasVibrator()) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(milliseconds, VibrationEffect.DEFAULT_AMPLITUDE));
        } else {
            vibrator.vibrate(milliseconds);
        }
    }
}

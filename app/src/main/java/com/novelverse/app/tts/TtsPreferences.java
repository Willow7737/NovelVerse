package com.novelverse.app.tts;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Dedicated SharedPreferences wrapper for TTS settings.
 *
 * Keeps TTS prefs isolated from the main UserPreferences file so they
 * can be cleared independently and don't pollute the primary prefs store.
 */
public class TtsPreferences {

    private static final String PREFS_NAME = "novelverse_tts_prefs";

    // ── Keys ─────────────────────────────────────────────────────────────
    private static final String KEY_VOICE_ID        = "tts_voice_id";
    private static final String KEY_SPEED           = "tts_speed";
    private static final String KEY_PITCH           = "tts_pitch";
    private static final String KEY_MODE            = "tts_mode";       // "auto" | "online" | "offline"
    private static final String KEY_AUTO_PLAY       = "tts_auto_play";
    private static final String KEY_HIGHLIGHT_WORDS = "tts_highlight_words";
    private static final String KEY_SKIP_PARAGRAPHS = "tts_skip_empty_paras";
    private static final String KEY_CHUNK_SIZE      = "tts_chunk_size_words";

    // ── Defaults ──────────────────────────────────────────────────────────
    public static final float DEFAULT_SPEED      = 1.0f;
    public static final float DEFAULT_PITCH      = 1.0f;
    public static final String DEFAULT_VOICE_ID  = "android-default";
    public static final String DEFAULT_MODE      = "auto";
    public static final int    DEFAULT_CHUNK_SIZE = 150; // words per synthesis chunk

    private final SharedPreferences prefs;

    public TtsPreferences(Context context) {
        prefs = context.getApplicationContext()
                       .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    // ── Voice ─────────────────────────────────────────────────────────────

    public String getVoiceId() {
        return prefs.getString(KEY_VOICE_ID, DEFAULT_VOICE_ID);
    }

    public void setVoiceId(String voiceId) {
        prefs.edit().putString(KEY_VOICE_ID, voiceId).apply();
    }

    /** Resolved voice object — falls back to system default if saved ID not found */
    public TtsVoice getVoice() {
        TtsVoice v = TtsVoice.findById(getVoiceId());
        return v != null ? v : TtsVoice.getSystemDefault();
    }

    // ── Speed ─────────────────────────────────────────────────────────────

    /** Speech speed multiplier. Range: 0.5 – 3.0. Default: 1.0 */
    public float getSpeed() {
        return prefs.getFloat(KEY_SPEED, DEFAULT_SPEED);
    }

    public void setSpeed(float speed) {
        float clamped = Math.max(0.5f, Math.min(3.0f, speed));
        prefs.edit().putFloat(KEY_SPEED, clamped).apply();
    }

    /** Speed label for UI display, e.g. "1.0×", "1.5×" */
    public String getSpeedLabel() {
        float s = getSpeed();
        // Format without trailing zero if whole number
        if (s == Math.floor(s)) return ((int) s) + "×";
        return String.format("%.1f×", s);
    }

    // ── Pitch ─────────────────────────────────────────────────────────────

    /** Voice pitch multiplier. Range: 0.5 – 2.0. Default: 1.0 */
    public float getPitch() {
        return prefs.getFloat(KEY_PITCH, DEFAULT_PITCH);
    }

    public void setPitch(float pitch) {
        float clamped = Math.max(0.5f, Math.min(2.0f, pitch));
        prefs.edit().putFloat(KEY_PITCH, clamped).apply();
    }

    // ── Mode ──────────────────────────────────────────────────────────────

    /**
     * TTS mode preference.
     *  "auto"    → use network if available, fall back to offline automatically
     *  "online"  → only use remote voices (fail if offline)
     *  "offline" → always use Android system TTS regardless of connectivity
     */
    public String getMode() {
        return prefs.getString(KEY_MODE, DEFAULT_MODE);
    }

    public void setMode(String mode) {
        prefs.edit().putString(KEY_MODE, mode).apply();
    }

    public boolean isAutoMode()    { return "auto".equals(getMode()); }
    public boolean isOnlineMode()  { return "online".equals(getMode()); }
    public boolean isOfflineMode() { return "offline".equals(getMode()); }

    // ── Misc ──────────────────────────────────────────────────────────────

    /** Whether TTS should start playing automatically when entering the reader */
    public boolean isAutoPlay() {
        return prefs.getBoolean(KEY_AUTO_PLAY, false);
    }

    public void setAutoPlay(boolean autoPlay) {
        prefs.edit().putBoolean(KEY_AUTO_PLAY, autoPlay).apply();
    }

    /** Whether the reader should highlight the currently-spoken word */
    public boolean isWordHighlightEnabled() {
        return prefs.getBoolean(KEY_HIGHLIGHT_WORDS, true);
    }

    public void setWordHighlightEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_HIGHLIGHT_WORDS, enabled).apply();
    }

    /** Whether empty / whitespace-only paragraphs are skipped during synthesis */
    public boolean isSkipEmptyParagraphsEnabled() {
        return prefs.getBoolean(KEY_SKIP_PARAGRAPHS, true);
    }

    public void setSkipEmptyParagraphs(boolean skip) {
        prefs.edit().putBoolean(KEY_SKIP_PARAGRAPHS, skip).apply();
    }

    /** Words per synthesis chunk sent to the engine */
    public int getChunkSizeWords() {
        return prefs.getInt(KEY_CHUNK_SIZE, DEFAULT_CHUNK_SIZE);
    }

    public void setChunkSizeWords(int words) {
        prefs.edit().putInt(KEY_CHUNK_SIZE, Math.max(50, Math.min(300, words))).apply();
    }

    // ── Speed presets ─────────────────────────────────────────────────────

    /** The discrete speed steps available in the UI */
    public static final float[] SPEED_STEPS = {
        0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f, 2.5f, 3.0f
    };

    /** Returns the nearest step index for a given speed value */
    public static int speedToStepIndex(float speed) {
        int best = 0;
        for (int i = 1; i < SPEED_STEPS.length; i++) {
            if (Math.abs(SPEED_STEPS[i] - speed) < Math.abs(SPEED_STEPS[best] - speed)) {
                best = i;
            }
        }
        return best;
    }
}

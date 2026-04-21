package com.novelverse.app.tts;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a TTS voice available in the hybrid synthesis system.
 *
 * Voices come from three tiers:
 *   - SYSTEM:  Android built-in TTS (always available, offline)
 *   - PIPER:   Oracle Cloud / Piper neural voices (online, fast)
 *   - COQUI:   Oracle Cloud / Coqui TTS voices (online, high-quality)
 */
public class TtsVoice {

    // ── Engine constants ─────────────────────────────────────────────────
    public static final String ENGINE_SYSTEM = "system";
    public static final String ENGINE_PIPER  = "piper";
    public static final String ENGINE_COQUI  = "coqui";

    // ── Quality constants ────────────────────────────────────────────────
    public static final String QUALITY_LOW       = "low";
    public static final String QUALITY_MEDIUM    = "medium";
    public static final String QUALITY_HIGH      = "high";
    public static final String QUALITY_EXCELLENT = "excellent";

    // ── Tier labels for UI ────────────────────────────────────────────────
    public static final String TIER_INSTANT  = "Instant";
    public static final String TIER_STANDARD = "Standard";
    public static final String TIER_PREMIUM  = "Premium";

    // ── Fields ────────────────────────────────────────────────────────────
    private final String id;            // unique voice ID
    private final String name;          // display name
    private final String engine;        // ENGINE_* constant
    private final String gender;        // "Male" | "Female" | "Neutral"
    private final String language;      // BCP-47, e.g. "en-US"
    private final String quality;       // QUALITY_* constant
    private final String tier;          // TIER_* for display
    private final String character;     // short description e.g. "Warm, natural"
    private final String modelId;       // engine-specific model path / ID
    private final String speakerId;     // VCTK speaker ID (null if not applicable)
    private final boolean requiresNetwork;
    private final int sizeKb;          // approx model size (0 for system)

    public TtsVoice(String id, String name, String engine, String gender,
                    String language, String quality, String tier,
                    String character, String modelId, String speakerId,
                    boolean requiresNetwork, int sizeKb) {
        this.id             = id;
        this.name           = name;
        this.engine         = engine;
        this.gender         = gender;
        this.language       = language;
        this.quality        = quality;
        this.tier           = tier;
        this.character      = character;
        this.modelId        = modelId;
        this.speakerId      = speakerId;
        this.requiresNetwork = requiresNetwork;
        this.sizeKb         = sizeKb;
    }

    // ── Getters ───────────────────────────────────────────────────────────
    public String getId()              { return id; }
    public String getName()            { return name; }
    public String getEngine()          { return engine; }
    public String getGender()          { return gender; }
    public String getLanguage()        { return language; }
    public String getQuality()         { return quality; }
    public String getTier()            { return tier; }
    public String getCharacter()       { return character; }
    public String getModelId()         { return modelId; }
    public String getSpeakerId()       { return speakerId; }
    public boolean isRequiresNetwork() { return requiresNetwork; }
    public int getSizeKb()             { return sizeKb; }

    /** Human-readable size string, e.g. "120 MB" */
    public String getSizeLabel() {
        if (sizeKb == 0) return "Built-in";
        if (sizeKb < 1024) return sizeKb + " KB";
        return (sizeKb / 1024) + " MB";
    }

    /** True if this voice requires an active network connection */
    public boolean isOnlineOnly() { return requiresNetwork; }

    /** True if this voice uses Android's system TTS engine */
    public boolean isSystemVoice() { return ENGINE_SYSTEM.equals(engine); }

    // ── Static catalog ────────────────────────────────────────────────────

    /**
     * Returns the full voice catalog.
     * Offline (system) voices appear first so they always work regardless of connectivity.
     *
     * Remote (Piper/Coqui) voices are stubbed here — their synthesis will route through
     * Android TTS until the Oracle Cloud endpoint is active.
     * When hooking up the endpoint, only TtsService.synthesizeRemote() needs updating.
     */
    public static List<TtsVoice> getCatalog() {
        List<TtsVoice> voices = new ArrayList<>();

        // ── Tier 0: System / Instant (Offline) ────────────────────────────
        voices.add(new TtsVoice(
            "android-default",
            "Device Default",
            ENGINE_SYSTEM,
            "Neutral",
            "varies",
            QUALITY_MEDIUM,
            TIER_INSTANT,
            "Built-in device voice, always available",
            null, null,
            false, 0
        ));

        // ── Tier 1: Piper / Standard (Online) ─────────────────────────────
        voices.add(new TtsVoice(
            "piper-lessac-high",
            "Sarah",
            ENGINE_PIPER,
            "Female",
            "en-US",
            QUALITY_HIGH,
            TIER_STANDARD,
            "Warm, natural — audiobook quality",
            "en_US-lessac-high",
            null,
            true, 120_000
        ));
        voices.add(new TtsVoice(
            "piper-ryan-high",
            "Michael",
            ENGINE_PIPER,
            "Male",
            "en-US",
            QUALITY_HIGH,
            TIER_STANDARD,
            "Deep, authoritative, professional",
            "en_US-ryan-high",
            null,
            true, 120_000
        ));
        voices.add(new TtsVoice(
            "piper-amy-medium",
            "Amy",
            ENGINE_PIPER,
            "Female",
            "en-US",
            QUALITY_MEDIUM,
            TIER_STANDARD,
            "Soft, gentle, intimate",
            "en_US-amy-medium",
            null,
            true, 65_000
        ));
        voices.add(new TtsVoice(
            "piper-kristin-medium",
            "Kristin",
            ENGINE_PIPER,
            "Female",
            "en-US",
            QUALITY_MEDIUM,
            TIER_STANDARD,
            "Young, clear, energetic",
            "en_US-kristin-medium",
            null,
            true, 65_000
        ));
        voices.add(new TtsVoice(
            "piper-john-medium",
            "John",
            ENGINE_PIPER,
            "Male",
            "en-US",
            QUALITY_MEDIUM,
            TIER_STANDARD,
            "Authoritative, news-reader clarity",
            "en_US-john-medium",
            null,
            true, 65_000
        ));

        // ── Tier 2: Coqui / Premium (Online) ──────────────────────────────
        voices.add(new TtsVoice(
            "coqui-jenny",
            "Jenny",
            ENGINE_COQUI,
            "Female",
            "en-US",
            QUALITY_EXCELLENT,
            TIER_PREMIUM,
            "Natural prosody, excellent for narration",
            "tts_models/en/jenny/jenny",
            null,
            true, 110_000
        ));
        voices.add(new TtsVoice(
            "coqui-vctk-p260",
            "David",
            ENGINE_COQUI,
            "Male",
            "en-US",
            QUALITY_EXCELLENT,
            TIER_PREMIUM,
            "American male, neutral and expressive",
            "tts_models/en/vctk/vits",
            "p260",
            true, 150_000
        ));
        voices.add(new TtsVoice(
            "coqui-vctk-p267",
            "Emma",
            ENGINE_COQUI,
            "Female",
            "en-US",
            QUALITY_EXCELLENT,
            TIER_PREMIUM,
            "American female, warm and clear",
            "tts_models/en/vctk/vits",
            "p267",
            true, 150_000
        ));

        return voices;
    }

    /** Returns only voices that work without a network connection */
    public static List<TtsVoice> getOfflineVoices() {
        List<TtsVoice> result = new ArrayList<>();
        for (TtsVoice v : getCatalog()) {
            if (!v.requiresNetwork) result.add(v);
        }
        return result;
    }

    /** Find a voice by ID, or null if not found */
    public static TtsVoice findById(String id) {
        for (TtsVoice v : getCatalog()) {
            if (v.id.equals(id)) return v;
        }
        return null;
    }

    /** Returns the default fallback voice for offline use */
    public static TtsVoice getSystemDefault() {
        return findById("android-default");
    }

    /** Returns the recommended voice for a given network state */
    public static TtsVoice getRecommended(boolean isOnline) {
        if (!isOnline) return getSystemDefault();
        // Default to Sarah (Piper lessac-high) — fast, high quality, audiobook feel
        TtsVoice preferred = findById("piper-lessac-high");
        return preferred != null ? preferred : getSystemDefault();
    }

    @Override
    public String toString() {
        return name + " (" + engine + ", " + language + ", " + quality + ")";
    }
}

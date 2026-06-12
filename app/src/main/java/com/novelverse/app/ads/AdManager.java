package com.novelverse.app.ads;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;

import com.novelverse.app.BuildConfig;
import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.hilt.android.qualifiers.ApplicationContext;

/** Manager for AdMob advertisements */
@Singleton
public class AdManager {

    private static final String TAG = "AdManager";

    // ── Ad Unit IDs ───────────────────────────────────────────────────────────

    private static final String BANNER_AD_UNIT_ID =
            "ca-app-pub-8357919311461882/3385332963"; // Real ID
    private static final String REWARDED_AD_UNIT_ID =
            "ca-app-pub-8357919311461882/6425240565"; // Real ID

    // ── Retry config ──────────────────────────────────────────────────────────

    /** Maximum number of automatic retry attempts after a load failure. */
    private static final int MAX_RETRIES = 3;

    /** Base delay (ms) for exponential back-off: 2 s -> 4 s -> 8 s. */
    private static final long RETRY_BASE_DELAY_MS = 2_000L;

    // ── State ─────────────────────────────────────────────────────────────────

    private final Context context;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private RewardedAd rewardedAd;
    private RewardedAdListener rewardedAdListener;

    /** True once MobileAds.initialize() callback has fired. */
    private boolean sdkReady = false;

    /** True while a RewardedAd.load() call is in-flight. Prevents concurrent load storms. */
    private boolean isLoading = false;

    /** Current retry attempt count (reset to 0 on a successful load). */
    private int retryCount = 0;

    // ── Constructor ───────────────────────────────────────────────────────────

    @Inject
    public AdManager(@ApplicationContext Context context) {
        this.context = context;
        initializeAds();
    }

    // ── Initialization ────────────────────────────────────────────────────────

    /**
     * Initialise the AdMob SDK.
     *
     * <p>FIX: loadRewardedAd() is now called <em>inside</em> the initialization callback, not after
     * the call returns. MobileAds.initialize() is async; firing a load request before the callback
     * arrives causes the load to fail silently, leaving rewardedAd == null forever.
     */
    private void initializeAds() {
        // Configure test devices BEFORE initializing the SDK.
        // This makes AdMob serve test ads instead of real ones during development,
        // avoiding "no fill" failures on the rewarded ad slot.
        if (BuildConfig.DEBUG) {
            com.google.android.gms.ads.RequestConfiguration config =
                    new com.google.android.gms.ads.RequestConfiguration.Builder()
                            .setTestDeviceIds(
                                    java.util.Arrays.asList(
                                            "281C73F53D5F63516515913D6B50E234" // physical test
                                            // device
                                            ))
                            .build();
            MobileAds.setRequestConfiguration(config);
        }

        MobileAds.initialize(
                context,
                initializationStatus -> {
                    Log.d(TAG, "AdMob SDK initialized");
                    sdkReady = true;
                    // Only load ads AFTER the SDK confirms it is ready.
                    loadRewardedAd();
                });
    }

    // ── Banner ad ─────────────────────────────────────────────────────────────

    /** Load and attach a banner ad to the provided AdView. */
    public void loadBannerAd(AdView adView) {
        AdRequest adRequest = new AdRequest.Builder().build();
        adView.loadAd(adRequest);

        adView.setAdListener(
                new AdListener() {
                    @Override
                    public void onAdLoaded() {
                        Log.d(TAG, "Banner ad loaded");
                    }

                    @Override
                    public void onAdFailedToLoad(LoadAdError adError) {
                        Log.e(TAG, "Banner ad failed to load: " + adError.getMessage());
                    }
                });
    }

    // ── Rewarded ad ───────────────────────────────────────────────────────────

    /**
     * Load a rewarded ad.
     *
     * <p>FIX: onAdFailedToLoad now schedules an automatic retry with exponential back-off (up to
     * MAX_RETRIES attempts) so a transient network hiccup during startup does not permanently leave
     * the slot empty.
     */
    public void loadRewardedAd() {
        if (!sdkReady) {
            Log.d(TAG, "loadRewardedAd: SDK not ready yet, skipping.");
            return;
        }
        if (isLoading) {
            Log.d(TAG, "loadRewardedAd: already in-flight, skipping.");
            return;
        }
        isLoading = true;

        AdRequest adRequest = new AdRequest.Builder().build();

        RewardedAd.load(
                context,
                REWARDED_AD_UNIT_ID,
                adRequest,
                new RewardedAdLoadCallback() {
                    @Override
                    public void onAdLoaded(RewardedAd ad) {
                        isLoading = false;
                        rewardedAd = ad;
                        retryCount = 0;
                        Log.d(TAG, "Rewarded ad loaded successfully");
                        setupRewardedAdCallbacks();
                    }

                    @Override
                    public void onAdFailedToLoad(LoadAdError loadAdError) {
                        isLoading = false;
                        rewardedAd = null;
                        Log.e(
                                TAG,
                                "Rewarded ad failed to load: "
                                        + loadAdError.getMessage()
                                        + " (code="
                                        + loadAdError.getCode()
                                        + ")");

                        if (retryCount < MAX_RETRIES) {
                            long delay = RETRY_BASE_DELAY_MS * (1L << retryCount);
                            retryCount++;
                            Log.d(
                                    TAG,
                                    "Scheduling retry "
                                            + retryCount
                                            + "/"
                                            + MAX_RETRIES
                                            + " in "
                                            + delay
                                            + " ms");
                            mainHandler.postDelayed(AdManager.this::loadRewardedAd, delay);
                        } else {
                            Log.w(TAG, "Max retries reached - giving up until next trigger.");
                            retryCount = 0;
                        }
                    }
                });
    }

    /** Wire up dismiss / fail callbacks on the freshly loaded rewarded ad. */
    private void setupRewardedAdCallbacks() {
        if (rewardedAd == null) return;

        rewardedAd.setFullScreenContentCallback(
                new FullScreenContentCallback() {
                    @Override
                    public void onAdClicked() {
                        Log.d(TAG, "Rewarded ad clicked");
                    }

                    @Override
                    public void onAdDismissedFullScreenContent() {
                        Log.d(TAG, "Rewarded ad dismissed - preloading next ad");
                        rewardedAd = null;
                        retryCount = 0;
                        loadRewardedAd(); // preload for next time
                    }

                    @Override
                    public void onAdFailedToShowFullScreenContent(AdError adError) {
                        Log.e(TAG, "Rewarded ad failed to show: " + adError.getMessage());
                        rewardedAd = null;
                        // FIX: also reload here so the slot is not permanently empty
                        // after a failed show (e.g. activity paused mid-show).
                        retryCount = 0;
                        loadRewardedAd();
                    }

                    @Override
                    public void onAdShowedFullScreenContent() {
                        Log.d(TAG, "Rewarded ad is showing");
                    }
                });
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Attempt to show the rewarded ad.
     *
     * <p>If no ad is ready, {@link RewardedAdListener#onAdNotReady()} is called and a fresh load is
     * scheduled so the next attempt is more likely to work.
     */
    public void showRewardedAd(Activity activity, RewardedAdListener listener) {
        this.rewardedAdListener = listener;

        if (rewardedAd != null) {
            rewardedAd.show(
                    activity,
                    rewardItem -> {
                        int rewardAmount = rewardItem.getAmount();
                        String rewardType = rewardItem.getType();
                        Log.d(TAG, "User earned reward: " + rewardAmount + " " + rewardType);

                        if (listener != null) {
                            listener.onUserEarnedReward(rewardAmount, rewardType);
                        }
                    });
        } else {
            Log.w(TAG, "Rewarded ad not ready - notifying listener and retrying load");
            if (listener != null) {
                listener.onAdNotReady();
            }
            // Kick off a fresh load attempt with a clean retry counter.
            retryCount = 0;
            loadRewardedAd();
        }
    }

    /**
     * @return true if a rewarded ad is loaded and ready to show.
     */
    public boolean isRewardedAdReady() {
        return rewardedAd != null;
    }

    // ── Banner lifecycle helpers ───────────────────────────────────────────────

    public void destroyBannerAd(AdView adView) {
        if (adView != null) adView.destroy();
    }

    public void pauseBannerAd(AdView adView) {
        if (adView != null) adView.pause();
    }

    public void resumeBannerAd(AdView adView) {
        if (adView != null) adView.resume();
    }

    // ── Listener interface ────────────────────────────────────────────────────

    /** Callbacks delivered to callers of {@link #showRewardedAd}. */
    public interface RewardedAdListener {
        void onUserEarnedReward(int amount, String type);

        void onAdNotReady();
    }
}

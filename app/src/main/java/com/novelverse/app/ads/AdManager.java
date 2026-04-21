package com.novelverse.app.ads;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.OnUserEarnedRewardListener;
import com.google.android.gms.ads.rewarded.RewardItem;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.hilt.android.qualifiers.ApplicationContext;

/** Manager for AdMob advertisements */
@Singleton
public class AdManager {

    private static final String TAG = "AdManager";

    private static final String BANNER_AD_UNIT_ID =
            "ca-app-pub-8357919311461882/7839286200"; // Real ID
    private static final String REWARDED_AD_UNIT_ID =
            "ca-app-pub-8357919311461882/1305667441"; // Real ID

    private final Context context;
    private RewardedAd rewardedAd;
    private RewardedAdListener rewardedAdListener;

    @Inject
    public AdManager(@ApplicationContext Context context) {
        this.context = context;
        initializeAds();
    }

    /** Initialize AdMob */
    private void initializeAds() {
        MobileAds.initialize(
                context,
                initializationStatus -> {
                    Log.d(TAG, "AdMob initialized");
                });

        loadRewardedAd();
    }

    /** Load banner ad */
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

    /** Load rewarded ad */
    public void loadRewardedAd() {
        AdRequest adRequest = new AdRequest.Builder().build();

        RewardedAd.load(
                context,
                REWARDED_AD_UNIT_ID,
                adRequest,
                new RewardedAdLoadCallback() {
                    @Override
                    public void onAdLoaded(RewardedAd ad) {
                        rewardedAd = ad;
                        Log.d(TAG, "Rewarded ad loaded");
                        setupRewardedAdCallbacks();
                    }

                    @Override
                    public void onAdFailedToLoad(LoadAdError loadAdError) {
                        rewardedAd = null;
                        Log.e(TAG, "Rewarded ad failed to load: " + loadAdError.getMessage());
                    }
                });
    }

    /** Setup rewarded ad callbacks */
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
                        Log.d(TAG, "Rewarded ad dismissed");
                        rewardedAd = null;
                        loadRewardedAd(); // Preload next ad
                    }

                    @Override
                    public void onAdFailedToShowFullScreenContent(AdError adError) {
                        Log.e(TAG, "Rewarded ad failed to show: " + adError.getMessage());
                        rewardedAd = null;
                    }

                    @Override
                    public void onAdShowedFullScreenContent() {
                        Log.d(TAG, "Rewarded ad showed");
                    }
                });
    }

    /** Show rewarded ad */
    public void showRewardedAd(Activity activity, RewardedAdListener listener) {
        this.rewardedAdListener = listener;

        if (rewardedAd != null) {
            rewardedAd.show(
                    activity,
                    rewardItem -> {
                        // User earned reward
                        int rewardAmount = rewardItem.getAmount();
                        String rewardType = rewardItem.getType();

                        Log.d(TAG, "User earned reward: " + rewardAmount + " " + rewardType);

                        if (listener != null) {
                            listener.onUserEarnedReward(rewardAmount, rewardType);
                        }
                    });
        } else {
            Log.w(TAG, "Rewarded ad not ready");
            if (listener != null) {
                listener.onAdNotReady();
            }
            loadRewardedAd();
        }
    }

    /** Check if rewarded ad is ready */
    public boolean isRewardedAdReady() {
        return rewardedAd != null;
    }

    /** Destroy banner ad */
    public void destroyBannerAd(AdView adView) {
        if (adView != null) {
            adView.destroy();
        }
    }

    /** Pause banner ad */
    public void pauseBannerAd(AdView adView) {
        if (adView != null) {
            adView.pause();
        }
    }

    /** Resume banner ad */
    public void resumeBannerAd(AdView adView) {
        if (adView != null) {
            adView.resume();
        }
    }

    /** Rewarded ad listener interface */
    public interface RewardedAdListener {
        void onUserEarnedReward(int amount, String type);

        void onAdNotReady();
    }
}

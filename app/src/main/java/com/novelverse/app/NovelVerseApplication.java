package com.novelverse.app;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;

import androidx.hilt.work.HiltWorkerFactory;
import androidx.work.Configuration;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.google.firebase.FirebaseApp;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.google.firebase.messaging.FirebaseMessaging;
import com.novelverse.app.data.local.preferences.UserPreferences;
import com.novelverse.app.utils.SecurityUtils;
import com.novelverse.app.crash.CrashHandler;

import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import dagger.hilt.android.HiltAndroidApp;

@HiltAndroidApp
public class NovelVerseApplication extends Application implements Configuration.Provider {

    public static final String CHANNEL_ID_GENERAL = "general_notifications";
    public static final String CHANNEL_ID_NEW_CHAPTERS = "new_chapter_notifications";
    public static final String CHANNEL_ID_SOCIAL = "social_notifications";
    public static final String CHANNEL_ID_PROMOTIONS = "promotion_notifications";

    public static final String SYNC_WORK_NAME = "novelverse_sync_work";
    public static final String CLEANUP_WORK_NAME = "novelverse_cleanup_work";

    @Inject
    HiltWorkerFactory workerFactory;

    @Inject
    UserPreferences userPreferences;

    @Inject
    com.novelverse.app.data.repository.GamificationRepository gamificationRepository;

    private static NovelVerseApplication instance;
    private FirebaseAnalytics firebaseAnalytics;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;

        // Install crash handler FIRST so we catch any initialization errors
        Thread.setDefaultUncaughtExceptionHandler(
                new CrashHandler(this)
        );

        // Register BannerManager lifecycle tracking — must be first so banners
        // can survive activity transitions (e.g. "Published!" after NovelEditor closes)
        com.novelverse.app.ui.banner.BannerManager.getInstance().init(this);

        // ⚠️ DO NOT LAUNCH UI HERE — ONLY PREP STATE
        prepareCrashState();

        try {
            initializeFirebase();
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            createNotificationChannels();
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            SecurityUtils.initialize(this);
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            scheduleBackgroundWork();
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            subscribeToFcmTopics();
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Seed gamification achievement catalog (idempotent — INSERT OR IGNORE)
        try {
            gamificationRepository.seedAchievementsIfNeeded();
            // If user already signed in, ensure their gamification rows exist
            String existingUserId = userPreferences.getUserId();
            if (existingUserId != null) {
                gamificationRepository.ensureUserRows(existingUserId, System.currentTimeMillis());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void prepareCrashState() {
        // Intentionally empty — handled in Activity layer
        // Keeps Application lifecycle safe
    }

    private void initializeFirebase() {
        FirebaseApp.initializeApp(this);
        firebaseAnalytics = FirebaseAnalytics.getInstance(this);

        if (!BuildConfig.DEBUG) {
            FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(true);
        } else {
            FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(false);
        }
    }

    private void createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager notificationManager =
                    (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

            NotificationChannel generalChannel = new NotificationChannel(
                    CHANNEL_ID_GENERAL,
                    getString(R.string.channel_general),
                    NotificationManager.IMPORTANCE_DEFAULT
            );

            NotificationChannel chapterChannel = new NotificationChannel(
                    CHANNEL_ID_NEW_CHAPTERS,
                    getString(R.string.channel_new_chapters),
                    NotificationManager.IMPORTANCE_HIGH
            );

            NotificationChannel socialChannel = new NotificationChannel(
                    CHANNEL_ID_SOCIAL,
                    getString(R.string.channel_social),
                    NotificationManager.IMPORTANCE_DEFAULT
            );

            NotificationChannel promoChannel = new NotificationChannel(
                    CHANNEL_ID_PROMOTIONS,
                    getString(R.string.channel_promotions),
                    NotificationManager.IMPORTANCE_LOW
            );

            notificationManager.createNotificationChannels(
                    java.util.Arrays.asList(
                            generalChannel,
                            chapterChannel,
                            socialChannel,
                            promoChannel
                    )
            );
        }
    }

    private void scheduleBackgroundWork() {
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresBatteryNotLow(true)
                .build();

        PeriodicWorkRequest syncWork = new PeriodicWorkRequest.Builder(
                com.novelverse.app.workers.SyncWorker.class,
                15, TimeUnit.MINUTES
        ).setConstraints(constraints).build();

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                SYNC_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                syncWork
        );

        PeriodicWorkRequest cleanupWork = new PeriodicWorkRequest.Builder(
                com.novelverse.app.workers.CleanupWorker.class,
                1, TimeUnit.DAYS
        ).setConstraints(constraints).build();

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                CLEANUP_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                cleanupWork
        );

        // Gamification sync — runs every 15 minutes when connected
        PeriodicWorkRequest gamSyncWork = new PeriodicWorkRequest.Builder(
                com.novelverse.app.workers.GamificationSyncWorker.class,
                15, TimeUnit.MINUTES
        ).setConstraints(constraints).build();

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "gamification_sync_work",
                ExistingPeriodicWorkPolicy.KEEP,
                gamSyncWork
        );
    }

    private void subscribeToFcmTopics() {
        FirebaseMessaging.getInstance().subscribeToTopic("all_users");

        try {
            if (userPreferences.isMarketingEmailsEnabled()) {
                FirebaseMessaging.getInstance().subscribeToTopic("promotions");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public Configuration getWorkManagerConfiguration() {
        return new Configuration.Builder()
                .setWorkerFactory(workerFactory)
                .setMinimumLoggingLevel(android.util.Log.INFO)
                .build();
    }

    public static NovelVerseApplication getInstance() {
        return instance;
    }

    public FirebaseAnalytics getFirebaseAnalytics() {
        return firebaseAnalytics;
    }
}
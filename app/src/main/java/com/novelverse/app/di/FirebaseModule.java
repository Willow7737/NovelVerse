package com.novelverse.app.di.module;

import android.content.Context;

import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.android.qualifiers.ApplicationContext;
import dagger.hilt.components.SingletonComponent;

import javax.inject.Singleton;

@Module
@InstallIn(SingletonComponent.class)
public class FirebaseModule {

    @Provides
    @Singleton
    public FirebaseAnalytics provideFirebaseAnalytics(
            @ApplicationContext Context context
    ) {
        return FirebaseAnalytics.getInstance(context);
    }

    @Provides
    @Singleton
    public FirebaseCrashlytics provideFirebaseCrashlytics() {
        return FirebaseCrashlytics.getInstance();
    }

    @Provides
    @Singleton
    public FirebaseMessaging provideFirebaseMessaging() {
        return FirebaseMessaging.getInstance();
    }

    @Provides
    @Singleton
    public FirebaseRemoteConfig provideFirebaseRemoteConfig() {
        FirebaseRemoteConfig remoteConfig = FirebaseRemoteConfig.getInstance();
        FirebaseRemoteConfigSettings configSettings = new FirebaseRemoteConfigSettings.Builder()
                .setMinimumFetchIntervalInSeconds(3600)
                .build();
        remoteConfig.setConfigSettingsAsync(configSettings);
        return remoteConfig;
    }
}

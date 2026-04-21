package com.novelverse.app.di;

import android.content.Context;

import androidx.datastore.core.DataStore;
import androidx.datastore.preferences.core.PreferenceDataStoreFactory;
import androidx.datastore.preferences.core.Preferences;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.novelverse.app.BuildConfig;
import com.novelverse.app.NovelVerseApplication;

import java.io.File;
import java.util.concurrent.TimeUnit;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.android.qualifiers.ApplicationContext;
import dagger.hilt.components.SingletonComponent;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;

@Module
@InstallIn(SingletonComponent.class)
public class AppModule {

    public static final String USER_PREFERENCES_NAME = "user_preferences";

    @Provides
    @Singleton
    public NovelVerseApplication provideApplication(@ApplicationContext Context context) {
        return (NovelVerseApplication) context.getApplicationContext();
    }

    @Provides
    @Singleton
    public Gson provideGson() {
        return new GsonBuilder()
            .setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
            .setLenient()
            .create();
    }

    @Provides
    @Singleton
    public OkHttpClient provideOkHttpClient() {
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
        loggingInterceptor.setLevel(BuildConfig.DEBUG
            ? HttpLoggingInterceptor.Level.BODY
            : HttpLoggingInterceptor.Level.NONE);

        return new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(loggingInterceptor)
            .addInterceptor(chain -> {
                okhttp3.Request original = chain.request();
                okhttp3.Request.Builder builder = original.newBuilder()
                    .header("Accept", "application/json")
                    .header("Content-Type", "application/json");
                return chain.proceed(builder.build());
            })
            .build();
    }

    @Provides
    @Singleton
    public DataStore<Preferences> provideDataStore(@ApplicationContext Context context) {
        // preferencesDataStoreFile is a Kotlin extension; equivalent Java path:
        File file = new File(context.getFilesDir(), "datastore/" + USER_PREFERENCES_NAME + ".preferences_pb");
        return PreferenceDataStoreFactory.INSTANCE.create(() -> file);
    }
}

package com.novelverse.app.di;

import com.google.gson.Gson;
import com.novelverse.app.BuildConfig;
import com.novelverse.app.data.remote.api.NovelApi;
import com.novelverse.app.data.remote.api.PaymentApi;
import com.novelverse.app.data.remote.api.UserApi;
import com.novelverse.app.data.remote.supabase.SupabaseAuthService;
import com.novelverse.app.data.remote.supabase.SupabaseClient;
import com.novelverse.app.data.remote.supabase.SupabaseDatabaseService;
import com.novelverse.app.data.remote.supabase.SupabaseStorageService;

import java.util.concurrent.TimeUnit;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;
import okhttp3.CertificatePinner;
import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava3.RxJava3CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Network Dagger Hilt Module
 */
@Module
@InstallIn(SingletonComponent.class)
public class NetworkModule {



    @Provides
    @Singleton
    public Retrofit provideRetrofit(OkHttpClient okHttpClient, Gson gson) {
        return new Retrofit.Builder()
            .baseUrl(BuildConfig.SUPABASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .addCallAdapterFactory(RxJava3CallAdapterFactory.create())
            .build();
    }

    @Provides
    @Singleton
    public NovelApi provideNovelApi(Retrofit retrofit) {
        return retrofit.create(NovelApi.class);
    }

    @Provides
    @Singleton
    public UserApi provideUserApi(Retrofit retrofit) {
        return retrofit.create(UserApi.class);
    }

    @Provides
    @Singleton
    public PaymentApi providePaymentApi(Retrofit retrofit) {
        return retrofit.create(PaymentApi.class);
    }

    @Provides
    @Singleton
    public SupabaseClient provideSupabaseClient() {
        return new SupabaseClient(
            BuildConfig.SUPABASE_URL,
            BuildConfig.SUPABASE_ANON_KEY
        );
    }

    @Provides
    @Singleton
    public SupabaseAuthService provideSupabaseAuthService(SupabaseClient client, OkHttpClient httpClient) {
        return new SupabaseAuthService(client, httpClient);
    }

    @Provides
    @Singleton
    public SupabaseDatabaseService provideSupabaseDatabaseService(SupabaseClient client) {
        return new SupabaseDatabaseService(client);
    }

    @Provides
    @Singleton
    public SupabaseStorageService provideSupabaseStorageService(SupabaseClient client) {
        return new SupabaseStorageService(client);
    }
}

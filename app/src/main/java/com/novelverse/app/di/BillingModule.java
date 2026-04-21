package com.novelverse.app.di;

import android.content.Context;

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.novelverse.app.billing.BillingManager;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.android.qualifiers.ApplicationContext;
import dagger.hilt.components.SingletonComponent;

/**
 * Billing Dagger Hilt Module
 */
@Module
@InstallIn(SingletonComponent.class)
public class BillingModule {

    @Provides
    @Singleton
    public BillingClient provideBillingClient(
            @ApplicationContext Context context,
            PurchasesUpdatedListener purchasesUpdatedListener
    ) {
        return BillingClient.newBuilder(context)
            .setListener(purchasesUpdatedListener)
            .enablePendingPurchases()
            .build();
    }

    @Provides
    @Singleton
    public PurchasesUpdatedListener providePurchasesUpdatedListener() {
        return (billingResult, purchases) -> {
            // This will be handled by BillingManager
        };
    }

    @Provides
    @Singleton
    public BillingManager provideBillingManager(
            @ApplicationContext Context context,
            BillingClient billingClient
    ) {
        return new BillingManager(context, billingClient);
    }
}

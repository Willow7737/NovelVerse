package com.novelverse.app.billing;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import com.android.billingclient.api.AcknowledgePurchaseParams;
import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.ConsumeParams;
import com.android.billingclient.api.ProductDetails;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.QueryProductDetailsParams;
import com.android.billingclient.api.QueryPurchasesParams;
import com.android.billingclient.api.SkuDetails;
import com.android.billingclient.api.SkuDetailsParams;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Manager for Google Play Billing operations
 */
@Singleton
public class BillingManager {

    private static final String TAG = "BillingManager";

    private final Context context;
    private final BillingClient billingClient;
    private final ExecutorService executor;

    private List<ProductDetails> productDetailsList = new ArrayList<>();
    private List<Purchase> purchases = new ArrayList<>();
    private BillingListener billingListener;

    @Inject
    public BillingManager(Context context, BillingClient billingClient) {
        this.context = context;
        this.billingClient = billingClient;
        this.executor = Executors.newSingleThreadExecutor();
        
        startConnection();
    }

    /**
     * Start billing connection
     */
    private void startConnection() {
        billingClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(BillingResult billingResult) {
                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    Log.d(TAG, "Billing setup finished successfully");
                    queryAvailableProducts();
                    queryPurchases();
                } else {
                    Log.e(TAG, "Billing setup failed: " + billingResult.getDebugMessage());
                }
            }

            @Override
            public void onBillingServiceDisconnected() {
                Log.w(TAG, "Billing service disconnected");
                // Retry connection
                startConnection();
            }
        });
    }

    /**
     * Query available products
     */
    public void queryAvailableProducts() {
        List<QueryProductDetailsParams.Product> productList = new ArrayList<>();
        
        // Point packages
        productList.add(QueryProductDetailsParams.Product.newBuilder()
            .setProductId("points_100")
            .setProductType(BillingClient.ProductType.INAPP)
            .build());
        productList.add(QueryProductDetailsParams.Product.newBuilder()
            .setProductId("points_500")
            .setProductType(BillingClient.ProductType.INAPP)
            .build());
        productList.add(QueryProductDetailsParams.Product.newBuilder()
            .setProductId("points_1000")
            .setProductType(BillingClient.ProductType.INAPP)
            .build());
        
        // Subscriptions
        productList.add(QueryProductDetailsParams.Product.newBuilder()
            .setProductId("premium_monthly")
            .setProductType(BillingClient.ProductType.SUBS)
            .build());
        productList.add(QueryProductDetailsParams.Product.newBuilder()
            .setProductId("premium_yearly")
            .setProductType(BillingClient.ProductType.SUBS)
            .build());

        QueryProductDetailsParams params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build();

        billingClient.queryProductDetailsAsync(params, (billingResult, productDetailsList) -> {
            if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                this.productDetailsList = productDetailsList;
                if (billingListener != null) {
                    billingListener.onProductsLoaded(productDetailsList);
                }
            } else {
                Log.e(TAG, "Failed to load products: " + billingResult.getDebugMessage());
            }
        });
    }

    /**
     * Query purchases
     */
    public void queryPurchases() {
        // Query in-app purchases
        billingClient.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.INAPP)
                .build(),
            (billingResult, purchases) -> {
                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    this.purchases.addAll(purchases);
                    processPurchases();
                }
            }
        );

        // Query subscriptions
        billingClient.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.SUBS)
                .build(),
            (billingResult, purchases) -> {
                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    this.purchases.addAll(purchases);
                    processPurchases();
                }
            }
        );
    }

    /**
     * Launch purchase flow
     */
    public void launchPurchaseFlow(Activity activity, ProductDetails productDetails) {
        List<BillingFlowParams.ProductDetailsParams> productDetailsParamsList = new ArrayList<>();
        
        BillingFlowParams.ProductDetailsParams.Builder paramsBuilder = 
            BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(productDetails);
        
        // Add offer token for subscriptions
        if (productDetails.getSubscriptionOfferDetails() != null && 
            !productDetails.getSubscriptionOfferDetails().isEmpty()) {
            paramsBuilder.setOfferToken(productDetails.getSubscriptionOfferDetails().get(0).getOfferToken());
        }
        
        productDetailsParamsList.add(paramsBuilder.build());

        BillingFlowParams billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(productDetailsParamsList)
            .build();

        BillingResult result = billingClient.launchBillingFlow(activity, billingFlowParams);
        if (result.getResponseCode() != BillingClient.BillingResponseCode.OK) {
            Log.e(TAG, "Failed to launch billing flow: " + result.getDebugMessage());
        }
    }

    /**
     * Consume a purchase (for consumable products like points)
     */
    public void consumePurchase(Purchase purchase) {
        ConsumeParams consumeParams = ConsumeParams.newBuilder()
            .setPurchaseToken(purchase.getPurchaseToken())
            .build();

        billingClient.consumeAsync(consumeParams, (billingResult, purchaseToken) -> {
            if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                Log.d(TAG, "Purchase consumed successfully");
                if (billingListener != null) {
                    billingListener.onPurchaseConsumed(purchase);
                }
            } else {
                Log.e(TAG, "Failed to consume purchase: " + billingResult.getDebugMessage());
            }
        });
    }

    /**
     * Acknowledge a purchase
     */
    public void acknowledgePurchase(Purchase purchase) {
        if (purchase.isAcknowledged()) {
            return;
        }

        AcknowledgePurchaseParams acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.getPurchaseToken())
            .build();

        billingClient.acknowledgePurchase(acknowledgePurchaseParams, billingResult -> {
            if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                Log.d(TAG, "Purchase acknowledged successfully");
            } else {
                Log.e(TAG, "Failed to acknowledge purchase: " + billingResult.getDebugMessage());
            }
        });
    }

    /**
     * Process purchases
     */
    private void processPurchases() {
        for (Purchase purchase : purchases) {
            if (purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED) {
                if (!purchase.isAcknowledged()) {
                    acknowledgePurchase(purchase);
                }
                
                if (billingListener != null) {
                    billingListener.onPurchaseSuccess(purchase);
                }
            }
        }
    }

    /**
     * Handle purchase updates
     */
    public void handlePurchaseUpdates(BillingResult billingResult, List<Purchase> purchases) {
        if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK && purchases != null) {
            this.purchases = purchases;
            processPurchases();
        } else if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.USER_CANCELED) {
            if (billingListener != null) {
                billingListener.onPurchaseCancelled();
            }
        } else {
            if (billingListener != null) {
                billingListener.onPurchaseError(billingResult.getDebugMessage());
            }
        }
    }

    /**
     * Check if user has premium subscription
     */
    public boolean hasPremiumSubscription() {
        for (Purchase purchase : purchases) {
            if (purchase.getProducts().contains("premium_monthly") || 
                purchase.getProducts().contains("premium_yearly")) {
                return true;
            }
        }
        return false;
    }

    /**
     * Get product details by ID
     */
    public ProductDetails getProductDetails(String productId) {
        for (ProductDetails details : productDetailsList) {
            if (details.getProductId().equals(productId)) {
                return details;
            }
        }
        return null;
    }

    /**
     * Set billing listener
     */
    public void setBillingListener(BillingListener listener) {
        this.billingListener = listener;
    }

    /**
     * End billing connection
     */
    public void endConnection() {
        if (billingClient.isReady()) {
            billingClient.endConnection();
        }
    }

    /**
     * Billing listener interface
     */
    public interface BillingListener {
        void onProductsLoaded(List<ProductDetails> productDetails);
        void onPurchaseSuccess(Purchase purchase);
        void onPurchaseConsumed(Purchase purchase);
        void onPurchaseCancelled();
        void onPurchaseError(String error);
    }
}

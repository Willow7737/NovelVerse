package com.novelverse.app.data.remote.api;

import com.google.gson.JsonObject;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Query;

/**
 * Retrofit API interface for payment operations
 */
public interface PaymentApi {

    /**
     * Get point packages
     */
    @GET("/point_packages")
    Call<JsonObject> getPointPackages(
        @Header("Authorization") String authToken
    );

    /**
     * Get point transactions
     */
    @GET("/point_transactions")
    Call<JsonObject> getPointTransactions(
        @Header("Authorization") String authToken,
        @Query("user_id") String userId
    );

    /**
     * Create a purchase
     */
    @POST("/purchases")
    Call<JsonObject> createPurchase(
        @Header("Authorization") String authToken,
        @Body JsonObject purchase
    );

    /**
     * Get user purchases
     */
    @GET("/purchases")
    Call<JsonObject> getPurchases(
        @Header("Authorization") String authToken,
        @Query("user_id") String userId
    );

    /**
     * Create a tip
     */
    @POST("/tips")
    Call<JsonObject> createTip(
        @Header("Authorization") String authToken,
        @Body JsonObject tip
    );

    /**
     * Get subscriptions
     */
    @GET("/subscriptions")
    Call<JsonObject> getSubscriptions(
        @Header("Authorization") String authToken,
        @Query("user_id") String userId
    );

    /**
     * Create a subscription
     */
    @POST("/subscriptions")
    Call<JsonObject> createSubscription(
        @Header("Authorization") String authToken,
        @Body JsonObject subscription
    );

    /**
     * Redeem promo code
     */
    @POST("/promo_code_redemptions")
    Call<JsonObject> redeemPromoCode(
        @Header("Authorization") String authToken,
        @Body JsonObject redemption
    );
}

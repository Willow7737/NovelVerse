package com.novelverse.app.data.remote.api;

import com.google.gson.JsonObject;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

/**
 * Retrofit API interface for user operations
 */
public interface UserApi {

    /**
     * Get user profile
     */
    @GET("/profiles")
    Call<JsonObject> getProfile(
        @Header("Authorization") String authToken,
        @Query("id") String userId
    );

    /**
     * Update user profile
     */
    @PATCH("/profiles")
    Call<JsonObject> updateProfile(
        @Header("Authorization") String authToken,
        @Query("id") String userId,
        @Body JsonObject updates
    );

    /**
     * Get user library
     */
    @GET("/user_library")
    Call<JsonObject> getLibrary(
        @Header("Authorization") String authToken,
        @Query("user_id") String userId
    );

    /**
     * Add novel to library
     */
    @POST("/user_library")
    Call<JsonObject> addToLibrary(
        @Header("Authorization") String authToken,
        @Body JsonObject libraryItem
    );

    /**
     * Get reading progress
     */
    @GET("/reading_progress")
    Call<JsonObject> getReadingProgress(
        @Header("Authorization") String authToken,
        @Query("user_id") String userId,
        @Query("novel_id") String novelId
    );

    /**
     * Update reading progress
     */
    @POST("/reading_progress")
    Call<JsonObject> updateReadingProgress(
        @Header("Authorization") String authToken,
        @Body JsonObject progress
    );

    /**
     * Get user bookmarks
     */
    @GET("/bookmarks")
    Call<JsonObject> getBookmarks(
        @Header("Authorization") String authToken,
        @Query("user_id") String userId
    );

    /**
     * Create bookmark
     */
    @POST("/bookmarks")
    Call<JsonObject> createBookmark(
        @Header("Authorization") String authToken,
        @Body JsonObject bookmark
    );

    /**
     * Delete bookmark
     */
    @PATCH("/bookmarks")
    Call<JsonObject> deleteBookmark(
        @Header("Authorization") String authToken,
        @Query("id") String bookmarkId,
        @Body JsonObject updates
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
     * Get user notifications
     */
    @GET("/notifications")
    Call<JsonObject> getNotifications(
        @Header("Authorization") String authToken,
        @Query("user_id") String userId
    );

    /**
     * Mark notification as read
     */
    @PATCH("/notifications")
    Call<JsonObject> markNotificationRead(
        @Header("Authorization") String authToken,
        @Query("id") String notificationId,
        @Body JsonObject updates
    );

    /**
     * Follow author
     */
    @POST("/author_follows")
    Call<JsonObject> followAuthor(
        @Header("Authorization") String authToken,
        @Body JsonObject follow
    );

    /**
     * Unfollow author
     */
    @PATCH("/author_follows")
    Call<JsonObject> unfollowAuthor(
        @Header("Authorization") String authToken,
        @Query("follower_id") String followerId,
        @Query("author_id") String authorId,
        @Body JsonObject updates
    );

    /**
     * Get user achievements
     */
    @GET("/user_achievements")
    Call<JsonObject> getAchievements(
        @Header("Authorization") String authToken,
        @Query("user_id") String userId
    );

    /**
     * Get reading streak
     */
    @GET("/reading_streaks")
    Call<JsonObject> getReadingStreak(
        @Header("Authorization") String authToken,
        @Query("user_id") String userId
    );
}

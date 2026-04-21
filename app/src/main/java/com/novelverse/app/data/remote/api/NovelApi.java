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
 * Retrofit API interface for novel operations
 */
public interface NovelApi {

    /**
     * Get all novels
     */
    @GET("/novels")
    Call<JsonObject> getNovels(
        @Header("Authorization") String authToken,
        @Query("select") String select,
        @Query("is_published") boolean isPublished,
        @Query("order") String order
    );

    /**
     * Get novel by ID
     */
    @GET("/novels")
    Call<JsonObject> getNovelById(
        @Header("Authorization") String authToken,
        @Query("id") String novelId,
        @Query("select") String select
    );

    /**
     * Create a novel
     */
    @POST("/novels")
    Call<JsonObject> createNovel(
        @Header("Authorization") String authToken,
        @Body JsonObject novel
    );

    /**
     * Update a novel
     */
    @PATCH("/novels")
    Call<JsonObject> updateNovel(
        @Header("Authorization") String authToken,
        @Query("id") String novelId,
        @Body JsonObject updates
    );

    /**
     * Get chapters for a novel
     */
    @GET("/chapters")
    Call<JsonObject> getChapters(
        @Header("Authorization") String authToken,
        @Query("novel_id") String novelId,
        @Query("is_published") boolean isPublished,
        @Query("order") String order
    );

    /**
     * Get chapter by ID
     */
    @GET("/chapters")
    Call<JsonObject> getChapterById(
        @Header("Authorization") String authToken,
        @Query("id") String chapterId
    );

    /**
     * Create a chapter
     */
    @POST("/chapters")
    Call<JsonObject> createChapter(
        @Header("Authorization") String authToken,
        @Body JsonObject chapter
    );

    /**
     * Update a chapter
     */
    @PATCH("/chapters")
    Call<JsonObject> updateChapter(
        @Header("Authorization") String authToken,
        @Query("id") String chapterId,
        @Body JsonObject updates
    );

    /**
     * Get characters for a novel
     */
    @GET("/characters")
    Call<JsonObject> getCharacters(
        @Header("Authorization") String authToken,
        @Query("novel_id") String novelId
    );

    /**
     * Create a character
     */
    @POST("/characters")
    Call<JsonObject> createCharacter(
        @Header("Authorization") String authToken,
        @Body JsonObject character
    );

    /**
     * Get genres
     */
    @GET("/genres")
    Call<JsonObject> getGenres(
        @Header("Authorization") String authToken
    );

    /**
     * Get tags
     */
    @GET("/tags")
    Call<JsonObject> getTags(
        @Header("Authorization") String authToken
    );
}

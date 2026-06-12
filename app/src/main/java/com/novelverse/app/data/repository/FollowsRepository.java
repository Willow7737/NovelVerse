package com.novelverse.app.data.repository;

import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.novelverse.app.data.remote.supabase.SupabaseDatabaseService;
import com.novelverse.app.domain.models.User;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Repository for follower / following data.
 *
 * Uses PostgREST embedded-resource syntax to join author_follows → profiles in a
 * single request, avoiding an N+1 pattern.
 *
 * Followers query  (people who follow userId):
 *   /author_follows?author_id=eq.{userId}
 *       &select=follower:profiles!author_follows_follower_id_fkey(...)
 *
 * Following query  (people userId follows):
 *   /author_follows?follower_id=eq.{userId}
 *       &select=author:profiles!author_follows_author_id_fkey(...)
 *
 * RLS on author_follows allows public SELECT (qual = true), so the anon key
 * that SupabaseDatabaseService#queryTable uses is sufficient.
 */
@Singleton
public class FollowsRepository {

    private static final String TAG = "FollowsRepository";

    private final SupabaseDatabaseService db;
    private final Gson gson = new Gson();

    public interface FollowsCallback {
        void onSuccess(List<User> users);
        void onError(String error);
    }

    public interface CountsCallback {
        void onSuccess(int followersCount, int followingCount);
        void onError(String error);
    }

    @Inject
    public FollowsRepository(SupabaseDatabaseService db) {
        this.db = db;
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Fetch followers or following for the given userId.
     *
     * @param userId      The profile whose followers/following we want.
     * @param isFollowers true  → return users who follow userId
     *                    false → return users that userId follows
     * @param cb          Callback on the OkHttp thread — post to main if needed.
     */
    public void getFollows(String userId, boolean isFollowers, FollowsCallback cb) {
        String query;
        if (isFollowers) {
            // Who follows userId?  Match on author_id, embed the follower's profile.
            query = "author_id=eq." + userId
                    + "&select=follower:profiles!author_follows_follower_id_fkey"
                    + "(id,username,display_name,avatar_url,followers_count,following_count)";
        } else {
            // Who does userId follow?  Match on follower_id, embed the author's profile.
            query = "follower_id=eq." + userId
                    + "&select=author:profiles!author_follows_author_id_fkey"
                    + "(id,username,display_name,avatar_url,followers_count,following_count)";
        }

        db.queryTable("author_follows", query, new SupabaseDatabaseService.DatabaseCallback() {
            @Override
            public void onSuccess(String result) {
                try {
                    List<User> users = new ArrayList<>();
                    JsonArray arr = gson.fromJson(result, JsonArray.class);
                    String key = isFollowers ? "follower" : "author";

                    for (JsonElement el : arr) {
                        JsonObject row = el.getAsJsonObject();
                        // Skip rows where the embedded profile is missing or null
                        if (!row.has(key) || row.get(key).isJsonNull()) continue;
                        users.add(parseProfile(row.getAsJsonObject(key)));
                    }

                    cb.onSuccess(users);
                } catch (Exception e) {
                    Log.e(TAG, "JSON parse error in getFollows", e);
                    cb.onError(e.getMessage());
                }
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "getFollows error: " + error);
                cb.onError(error);
            }
        });
    }

    /**
     * Fetch the cached follower/following counts stored on the profiles row.
     * These counters are maintained by DB triggers on author_follows INSERT/DELETE.
     */
    public void getCounts(String userId, CountsCallback cb) {
        String query = "id=eq." + userId + "&select=followers_count,following_count";
        db.queryTable("profiles", query, new SupabaseDatabaseService.DatabaseCallback() {
            @Override
            public void onSuccess(String result) {
                try {
                    JsonArray arr = gson.fromJson(result, JsonArray.class);
                    if (arr.isEmpty()) {
                        cb.onSuccess(0, 0);
                        return;
                    }
                    JsonObject row = arr.get(0).getAsJsonObject();
                    int fc = safeInt(row, "followers_count");
                    int fg = safeInt(row, "following_count");
                    cb.onSuccess(fc, fg);
                } catch (Exception e) {
                    Log.e(TAG, "JSON parse error in getCounts", e);
                    cb.onError(e.getMessage());
                }
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "getCounts error: " + error);
                cb.onError(error);
            }
        });
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private User parseProfile(JsonObject p) {
        User user = new User();
        if (has(p, "id"))             user.setId(p.get("id").getAsString());
        if (has(p, "username"))       user.setUsername(p.get("username").getAsString());
        if (has(p, "display_name"))   user.setDisplayName(p.get("display_name").getAsString());
        if (has(p, "avatar_url"))     user.setAvatarUrl(p.get("avatar_url").getAsString());
        if (has(p, "followers_count")) user.setFollowersCount(p.get("followers_count").getAsInt());
        if (has(p, "following_count")) user.setFollowingCount(p.get("following_count").getAsInt());
        return user;
    }

    private boolean has(JsonObject obj, String key) {
        return obj.has(key) && !obj.get(key).isJsonNull();
    }

    private int safeInt(JsonObject obj, String key) {
        return has(obj, key) ? obj.get(key).getAsInt() : 0;
    }
}

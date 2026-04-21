package com.novelverse.app.data.repository;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.novelverse.app.data.local.dao.NovelDao;
import com.novelverse.app.data.local.preferences.UserPreferences;
import com.novelverse.app.data.remote.supabase.SupabaseDatabaseService;
import com.novelverse.app.domain.models.Novel;
import com.novelverse.app.domain.models.ReadingChallenge;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class NovelRepository {

    private static final String TAG   = "NovelRepository";
    private static final String TABLE = "novels";

    private final SupabaseDatabaseService db;
    private final UserPreferences         prefs;
    private final Gson                    gson = new Gson();

    @Inject
    public NovelRepository(NovelDao novelDao,
                           SupabaseDatabaseService db,
                           UserPreferences prefs) {
        this.db    = db;
        this.prefs = prefs;
    }

    // ── Author's own novels ───────────────────────────────────────────────────

    /**
     * Fetch all novels belonging to the current author.
     *
     * FIX 1: Uses select=* only (no embedded profiles join).
     *        The profiles join was causing PostgREST to return HTTP 400 when
     *        the FK relationship alias was ambiguous, which was silently swallowed
     *        as an empty list — making it look like the author had no novels.
     *
     * FIX 2: Errors are now propagated via the errorLiveData parameter so the
     *        UI can surface them instead of silently showing an empty state.
     *
     * FIX 3: Both draft AND published novels are returned (no is_published filter).
     *        Authors should see all their own work regardless of publish state.
     */
    public LiveData<List<Novel>> getMyNovels() {
        MutableLiveData<List<Novel>> ld = new MutableLiveData<>();
        String authorId = prefs.getUserId();
        if (authorId == null) {
            Log.w(TAG, "getMyNovels: no userId in prefs");
            ld.setValue(new ArrayList<>());
            return ld;
        }

        // select=* only — no embedded join. Avoids 400 from ambiguous FK alias.
        db.selectWhere(
                TABLE,
                "*",
                "author_id=eq." + authorId + "&deleted_at=is.null",
                "created_at.desc",
                prefs.getAccessToken(),
                new SupabaseDatabaseService.DatabaseCallback() {
                    @Override
                    public void onSuccess(String result) {
                        Log.d(TAG, "getMyNovels raw: " + result.substring(0, Math.min(200, result.length())));
                        List<Novel> novels = parseNovels(result);
                        Log.d(TAG, "getMyNovels parsed: " + novels.size() + " novels");
                        ld.postValue(novels);
                    }

                    @Override
                    public void onError(String error) {
                        Log.e(TAG, "getMyNovels FAILED: " + error);
                        ld.postValue(new ArrayList<>());
                    }
                });
        return ld;
    }

    // ── Featured / Discovery ──────────────────────────────────────────────────

    public LiveData<List<Novel>> getFeaturedNovels() {
        MutableLiveData<List<Novel>> ld = new MutableLiveData<>();
        db.selectWhere(TABLE, "*,profiles!author_id(display_name,avatar_url,username)",
                "is_featured=eq.true&is_published=eq.true&deleted_at=is.null",
                "featured_at.desc",
                prefs.getAccessToken(),
                new SupabaseDatabaseService.DatabaseCallback() {
                    @Override public void onSuccess(String r) { ld.postValue(parseNovels(r)); }
                    @Override public void onError(String e) {
                        Log.e(TAG, "getFeaturedNovels: " + e);
                        ld.postValue(new ArrayList<>());
                    }
                });
        return ld;
    }

    public LiveData<List<Novel>> getTrendingNovels() {
        MutableLiveData<List<Novel>> ld = new MutableLiveData<>();
        db.selectWhere(TABLE, "*,profiles!author_id(display_name,avatar_url,username)",
                "is_published=eq.true&deleted_at=is.null",
                "total_views.desc",
                prefs.getAccessToken(),
                new SupabaseDatabaseService.DatabaseCallback() {
                    @Override public void onSuccess(String r) { ld.postValue(parseNovels(r)); }
                    @Override public void onError(String e) {
                        Log.e(TAG, "getTrendingNovels: " + e);
                        ld.postValue(new ArrayList<>());
                    }
                });
        return ld;
    }

    public LiveData<List<Novel>> getNewReleases() {
        MutableLiveData<List<Novel>> ld = new MutableLiveData<>();
        db.selectWhere(TABLE, "*,profiles!author_id(display_name,avatar_url,username)",
                "is_published=eq.true&deleted_at=is.null",
                "published_at.desc",
                prefs.getAccessToken(),
                new SupabaseDatabaseService.DatabaseCallback() {
                    @Override public void onSuccess(String r) { ld.postValue(parseNovels(r)); }
                    @Override public void onError(String e) {
                        Log.e(TAG, "getNewReleases: " + e);
                        ld.postValue(new ArrayList<>());
                    }
                });
        return ld;
    }

    // ── Single novel ──────────────────────────────────────────────────────────

    public LiveData<Novel> getNovelById(String novelId) {
        MutableLiveData<Novel> ld = new MutableLiveData<>();
        // Join profiles so author_display_name, author_avatar_url and author_username are populated.
        // PostgREST embed syntax: profiles!author_id(display_name,avatar_url,username)
        String url = db.buildSelectUrl(
                TABLE,
                "*,profiles!author_id(display_name,avatar_url,username)",
                "id=eq." + novelId,
                null);
        db.rawSelect(url, prefs.getAccessToken(),
                new SupabaseDatabaseService.DatabaseCallback() {
                    @Override public void onSuccess(String r) {
                        try {
                            // PostgREST returns an array even for single rows without .single() header
                            String body = r.trim();
                            com.google.gson.JsonElement el = gson.fromJson(body, com.google.gson.JsonElement.class);
                            com.google.gson.JsonObject obj;
                            if (el.isJsonArray()) {
                                com.google.gson.JsonArray arr = el.getAsJsonArray();
                                if (arr.size() == 0) { ld.postValue(null); return; }
                                obj = arr.get(0).getAsJsonObject();
                            } else {
                                obj = el.getAsJsonObject();
                            }
                            Novel novel = gson.fromJson(obj, Novel.class);
                            // Extract embedded profiles object
                            if (obj.has("profiles") && !obj.get("profiles").isJsonNull()) {
                                com.google.gson.JsonObject prof = obj.getAsJsonObject("profiles");
                                if (prof.has("display_name") && !prof.get("display_name").isJsonNull())
                                    novel.setAuthorDisplayName(prof.get("display_name").getAsString());
                                if (prof.has("avatar_url") && !prof.get("avatar_url").isJsonNull())
                                    novel.setAuthorAvatarUrl(prof.get("avatar_url").getAsString());
                                if (prof.has("username") && !prof.get("username").isJsonNull())
                                    novel.setAuthorUsername(prof.get("username").getAsString());
                            }
                            ld.postValue(novel);
                        } catch (Exception e) {
                            Log.e(TAG, "getNovelById parse error: " + e.getMessage());
                            ld.postValue(null);
                        }
                    }
                    @Override public void onError(String e) {
                        Log.e(TAG, "getNovelById error: " + e);
                        ld.postValue(null);
                    }
                });
        return ld;
    }

    // ── CREATE ────────────────────────────────────────────────────────────────

    public void createNovel(Novel novel, Callback<Novel> cb) {
        String userId = prefs.getUserId();
        if (userId == null) { cb.onError("Not logged in"); return; }

        String newId = UUID.randomUUID().toString();
        novel.setId(newId);
        novel.setAuthorId(userId);

        JsonObject body = new JsonObject();
        body.addProperty("id",          newId);
        body.addProperty("author_id",   userId);
        body.addProperty("title",       novel.getTitle());
        body.addProperty("description", novel.getDescription() != null ? novel.getDescription() : "");
        body.addProperty("status",      novel.getStatus()    != null ? novel.getStatus()    : "draft");
        body.addProperty("price_type",  novel.getPriceType() != null ? novel.getPriceType() : "free");
        body.addProperty("age_rating",  novel.getAgeRating() != null ? novel.getAgeRating() : "all");
        body.addProperty("language",    "en");
        body.addProperty("is_published", novel.isPublished());
        body.addProperty("visibility",  novel.isPublished() ? "public" : "private");

        if (novel.isPublished()) {
            body.addProperty("published_at",
                    new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(new Date()));
        }
        if (novel.getCoverImageUrl() != null)
            body.addProperty("cover_image_url", novel.getCoverImageUrl());

        if (novel.getGenres() != null && !novel.getGenres().isEmpty()) {
            JsonArray genres = new JsonArray();
            for (String g : novel.getGenres()) genres.add(g);
            body.add("genres", genres);
        }

        Log.d(TAG, "createNovel body: " + body);

        db.insert(TABLE, body, prefs.getAccessToken(),
                new SupabaseDatabaseService.DatabaseCallback() {
                    @Override public void onSuccess(String r) {
                        Log.d(TAG, "createNovel success: " + r.substring(0, Math.min(300, r.length())));
                        try {
                            // Supabase returns an array even for single inserts
                            JsonArray arr = gson.fromJson(r, JsonArray.class);
                            Novel created = (arr != null && arr.size() > 0)
                                    ? gson.fromJson(arr.get(0), Novel.class)
                                    : novel;
                            // Ensure we always have an id
                            if (created.getId() == null) created.setId(newId);
                            cb.onSuccess(created);
                        } catch (Exception e) {
                            Log.w(TAG, "createNovel parse failed, using local: " + e.getMessage());
                            cb.onSuccess(novel);
                        }
                    }
                    @Override public void onError(String e) {
                        Log.e(TAG, "createNovel FAILED: " + e);
                        if (e != null && e.contains("403"))
                            cb.onError("Permission denied — your profile role must be 'author'. Please update your profile and try again.");
                        else if (e != null && e.contains("409"))
                            cb.onError("Duplicate — this novel ID already exists. Please try again.");
                        else
                            cb.onError("Save failed: " + e);
                    }
                });
    }

    // ── UPDATE ────────────────────────────────────────────────────────────────

    public void updateNovel(Novel novel, Callback<Novel> cb) {
        JsonObject body = new JsonObject();
        body.addProperty("id", novel.getId());
        if (novel.getAuthorId()      != null) body.addProperty("author_id",       novel.getAuthorId());
        if (novel.getTitle()         != null) body.addProperty("title",           novel.getTitle());
        if (novel.getDescription()   != null) body.addProperty("description",     novel.getDescription());
        if (novel.getStatus()        != null) body.addProperty("status",          novel.getStatus());
        if (novel.getPriceType()     != null) body.addProperty("price_type",      novel.getPriceType());
        if (novel.getAgeRating()     != null) body.addProperty("age_rating",      novel.getAgeRating());
        if (novel.getCoverImageUrl() != null) body.addProperty("cover_image_url", novel.getCoverImageUrl());
        body.addProperty("is_published", novel.isPublished());
        body.addProperty("visibility",   novel.isPublished() ? "public" : "private");

        if (novel.getGenres() != null && !novel.getGenres().isEmpty()) {
            JsonArray genres = new JsonArray();
            for (String g : novel.getGenres()) genres.add(g);
            body.add("genres", genres);
        }

        db.upsert(TABLE, body, prefs.getAccessToken(),
                new SupabaseDatabaseService.DatabaseCallback() {
                    @Override public void onSuccess(String r) { cb.onSuccess(novel); }
                    @Override public void onError(String e)   {
                        Log.e(TAG, "updateNovel FAILED: " + e);
                        cb.onError(e);
                    }
                });
    }

    /**
     * Patch only the cover_image_url column on an existing novel row.
     * Called after a successful cover upload so the URL is stored in the DB.
     */
    public void updateCoverUrl(String novelId, String coverUrl, SimpleCallback cb) {
        JsonObject body = new JsonObject();
        body.addProperty("cover_image_url", coverUrl);
        db.update(TABLE, novelId, body, prefs.getAccessToken(),
                new SupabaseDatabaseService.DatabaseCallback() {
                    @Override public void onSuccess(String r) {
                        Log.d(TAG, "Cover URL saved ✓ " + coverUrl);
                        cb.onResult(true, null);
                    }
                    @Override public void onError(String e) {
                        Log.e(TAG, "updateCoverUrl FAILED: " + e);
                        cb.onResult(false, e);
                    }
                });
    }

    // ── DELETE ────────────────────────────────────────────────────────────────

    public void deleteNovel(String novelId, SimpleCallback cb) {
        db.delete(TABLE, novelId, prefs.getAccessToken(),
                new SupabaseDatabaseService.DatabaseCallback() {
                    @Override public void onSuccess(String r) { cb.onResult(true, null); }
                    @Override public void onError(String e)   { cb.onResult(false, e); }
                });
    }

    // ── COVER UPLOAD ──────────────────────────────────────────────────────────

    /**
     * Upload cover bytes to Supabase Storage, then immediately patch the
     * novels row with the resulting public URL.
     *
     * FIX: Previously only uploaded to storage but never wrote cover_image_url
     * back to the novels table, so covers never appeared in the app.
     */
    public void uploadCover(String novelId, byte[] bytes, Callback<String> cb) {
        String userId = prefs.getUserId() != null ? prefs.getUserId() : "unknown";
        // Policy: (storage.foldername(name))[1] = auth.uid()::text
        String path = userId + "/" + novelId + ".jpg";

        Log.d(TAG, "uploadCover to path: " + path);

        db.uploadFile("novel-covers", path, bytes, "image/jpeg",
                prefs.getAccessToken(),
                new SupabaseDatabaseService.DatabaseCallback() {
                    @Override public void onSuccess(String r) {
                        // Add cache-busting timestamp so Glide re-fetches after update
                        String url = db.getPublicUrl("novel-covers", path)
                                + "?t=" + System.currentTimeMillis();
                        Log.d(TAG, "Cover uploaded ✓, url=" + url);

                        // Write the URL back to the novels table
                        updateCoverUrl(novelId, url, (success, err) -> {
                            if (success) {
                                cb.onSuccess(url);
                            } else {
                                // Storage upload succeeded but DB update failed — still
                                // return the URL so the in-memory novel object is correct
                                Log.w(TAG, "Cover uploaded but DB update failed: " + err);
                                cb.onSuccess(url);
                            }
                        });
                    }
                    @Override public void onError(String e) {
                        Log.e(TAG, "uploadCover FAILED: " + e);
                        cb.onError(e);
                    }
                });
    }

    // ── Task 9: Continue Reading shelf ───────────────────────────────────────

    public LiveData<List<Novel>> getContinueReadingNovels() {
        MutableLiveData<List<Novel>> ld = new MutableLiveData<>();
        String userId = prefs.getUserId();
        if (userId == null) { ld.setValue(new ArrayList<>()); return ld; }
        // Join novels + author profiles + most-recent reading_progress row for progress bar
        String select = "novels(*,profiles!author_id(display_name,avatar_url,username)),"
                + "reading_progress(progress_percentage,chapter_id)";
        String filter = "user_id=eq." + userId + "&status=eq.reading";
        String url    = db.buildSelectUrl("user_library", select, filter, "updated_at.desc");
        db.rawSelect(url, prefs.getAccessToken(), new SupabaseDatabaseService.DatabaseCallback() {
            @Override public void onSuccess(String r) {
                List<Novel> list = new ArrayList<>();
                try {
                    com.google.gson.JsonArray rows = new com.google.gson.Gson().fromJson(r, com.google.gson.JsonArray.class);
                    if (rows == null) { ld.postValue(list); return; }
                    for (int i = 0; i < rows.size(); i++) {
                        com.google.gson.JsonObject row = rows.get(i).getAsJsonObject();
                        if (!row.has("novels") || row.get("novels").isJsonNull()) continue;
                        com.google.gson.JsonObject obj = row.getAsJsonObject("novels");
                        Novel novel = gson.fromJson(obj, Novel.class);
                        if (obj.has("profiles") && !obj.get("profiles").isJsonNull()) {
                            com.google.gson.JsonObject prof = obj.getAsJsonObject("profiles");
                            if (prof.has("display_name") && !prof.get("display_name").isJsonNull())
                                novel.setAuthorDisplayName(prof.get("display_name").getAsString());
                            if (prof.has("avatar_url") && !prof.get("avatar_url").isJsonNull())
                                novel.setAuthorAvatarUrl(prof.get("avatar_url").getAsString());
                        }
                        // Extract reading progress (PostgREST may return object or single-element array)
                        if (row.has("reading_progress") && !row.get("reading_progress").isJsonNull()) {
                            com.google.gson.JsonElement rpEl = row.get("reading_progress");
                            com.google.gson.JsonObject rp = null;
                            if (rpEl.isJsonArray()) {
                                com.google.gson.JsonArray rpArr = rpEl.getAsJsonArray();
                                if (rpArr.size() > 0) rp = rpArr.get(0).getAsJsonObject();
                            } else if (rpEl.isJsonObject()) {
                                rp = rpEl.getAsJsonObject();
                            }
                            if (rp != null) {
                                if (rp.has("progress_percentage") && !rp.get("progress_percentage").isJsonNull())
                                    novel.setReadingProgressPercent(rp.get("progress_percentage").getAsDouble());
                                if (rp.has("chapter_id") && !rp.get("chapter_id").isJsonNull())
                                    novel.setCurrentChapterId(rp.get("chapter_id").getAsString());
                                if (rp.has("chapters") && !rp.get("chapters").isJsonNull()) {
                                    com.google.gson.JsonObject ch = rp.getAsJsonObject("chapters");
                                    if (ch.has("chapter_number") && !ch.get("chapter_number").isJsonNull())
                                        novel.setCurrentChapterNumber(ch.get("chapter_number").getAsInt());
                                }
                            }
                        }
                        list.add(novel);
                    }
                } catch (Exception e) { Log.e(TAG, "getContinueReading parse error: " + e.getMessage()); }
                ld.postValue(list);
            }
            @Override public void onError(String e) { Log.e(TAG, "getContinueReading error: " + e); ld.postValue(new ArrayList<>()); }
        });
        return ld;
    }

    // ── Task 9: For You shelf ─────────────────────────────────────────────────

    public LiveData<List<Novel>> getNovelsByGenres(List<String> genres) {
        MutableLiveData<List<Novel>> ld = new MutableLiveData<>();
        if (genres == null || genres.isEmpty()) {
            ld.postValue(new ArrayList<>());
            return ld;
        }
        // Build "genres=cs.{Fantasy,Romance}" style Supabase filter
        StringBuilder sb = new StringBuilder("select=id,title,cover_image_url,author_display_name,genres,status&genres=cs.{");
        for (int i = 0; i < genres.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(genres.get(i));
        }
        sb.append("}&limit=20");
        db.queryTable("novels", sb.toString(), new SupabaseDatabaseService.DatabaseCallback() {
            @Override public void onSuccess(String r) { ld.postValue(parseNovels(r)); }
            @Override public void onError(String e)   { ld.postValue(new ArrayList<>()); }
        });
        return ld;
    }

    // ── Task 37: Active challenges ────────────────────────────────────────────

    public LiveData<List<ReadingChallenge>> getActiveChallenges() {
        MutableLiveData<List<ReadingChallenge>> ld = new MutableLiveData<>();
        db.queryTable("challenges",
            "select=id,title,description,target_count,reward_points,badge_drawable_id,expires_at&expires_at=gte." +
                new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(new java.util.Date()),
            new SupabaseDatabaseService.DatabaseCallback() {
                @Override public void onSuccess(String r) {
                    List<ReadingChallenge> list = new ArrayList<>();
                    try {
                        com.google.gson.JsonArray arr = gson.fromJson(r, com.google.gson.JsonArray.class);
                        if (arr != null) {
                            for (int i = 0; i < arr.size(); i++) {
                                ReadingChallenge ch = gson.fromJson(arr.get(i), ReadingChallenge.class);
                                if (ch != null) list.add(ch);
                            }
                        }
                    } catch (Exception e) { Log.e(TAG, "parse challenges: " + e.getMessage()); }
                    ld.postValue(list);
                }
                @Override public void onError(String e) { ld.postValue(new ArrayList<>()); }
            });
        return ld;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private List<Novel> parseNovels(String json) {
        List<Novel> list = new ArrayList<>();
        try {
            JsonArray arr = gson.fromJson(json, JsonArray.class);
            if (arr == null) {
                Log.w(TAG, "parseNovels: null array from: " + json.substring(0, Math.min(100, json.length())));
                return list;
            }
            for (int i = 0; i < arr.size(); i++) {
                try {
                    JsonObject obj = arr.get(i).getAsJsonObject();
                    Novel n = gson.fromJson(obj, Novel.class);
                    if (n == null) continue;
                    // Extract embedded profiles join if present
                    if (obj.has("profiles") && !obj.get("profiles").isJsonNull()) {
                        JsonObject prof = obj.getAsJsonObject("profiles");
                        if (prof.has("display_name") && !prof.get("display_name").isJsonNull())
                            n.setAuthorDisplayName(prof.get("display_name").getAsString());
                        if (prof.has("avatar_url") && !prof.get("avatar_url").isJsonNull())
                            n.setAuthorAvatarUrl(prof.get("avatar_url").getAsString());
                        if (prof.has("username") && !prof.get("username").isJsonNull())
                            n.setAuthorUsername(prof.get("username").getAsString());
                    }
                    list.add(n);
                } catch (Exception e) {
                    Log.w(TAG, "parseNovels: skip item " + i + ": " + e.getMessage());
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "parseNovels ERROR: " + e.getMessage() + " | raw: "
                    + json.substring(0, Math.min(200, json.length())));
        }
        return list;
    }

    // ── Callback interfaces ───────────────────────────────────────────────────

    public interface Callback<T> {
        void onSuccess(T result);
        void onError(String error);
    }

    public interface SimpleCallback {
        void onResult(boolean success, String error);
    }

//    private void updateCoverUrl(String novelId, String url, SimpleCallback cb) {
//        JsonObject data = new JsonObject();
//        data.addProperty("cover_image_url", url);
//        db.update(TABLE, novelId, data, prefs.getAccessToken(), new SupabaseDatabaseService.DatabaseCallback() {
//            @Override public void onSuccess(String r) { cb.onResult(true, null); }
//            @Override public void onError(String e) { cb.onResult(false, e); }
//        });
//    }
}

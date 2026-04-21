package com.novelverse.app.data.repository;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.novelverse.app.data.local.dao.ReadingProgressDao;
import com.novelverse.app.data.local.entities.ReadingProgressEntity;
import com.novelverse.app.data.local.preferences.UserPreferences;
import com.novelverse.app.data.remote.supabase.SupabaseDatabaseService;
import com.novelverse.app.domain.models.ReadingProgress;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Single source of truth for reading progress.
 *
 * Rules enforced here:
 *  Rule 1  – All remote writes go through upsert_reading_progress RPC. Never a direct table write.
 *  Rule 2  – Continue Reading is fetched from the `continue_reading` view only.
 *  Rule 3  – Resume uses chapter_id + scroll_position returned by the view.
 *  Rule 4  – user_library is NOT touched here (that belongs to LibraryRepository).
 *  Rule 7  – is_completed = true when progress_percentage >= 95.
 *  Rule 8  – On RPC failure the row is written locally with needs_sync = true;
 *             syncPendingProgress() replays those rows when connectivity returns.
 *  Rule 10 – No alternate flows. This class is the only place that writes progress.
 */
@Singleton
public class ReadingProgressRepository {

    private static final String TAG = "ProgressRepo";
    private static final String RPC_UPSERT = "upsert_reading_progress";
    private static final String VIEW_CONTINUE = "continue_reading";
    private static final double COMPLETION_THRESHOLD = 95.0;
    private static final SimpleDateFormat ISO =
            new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);

    private final ReadingProgressDao  dao;
    private final SupabaseDatabaseService db;
    private final UserPreferences     prefs;
    private final ExecutorService     executor = Executors.newSingleThreadExecutor();
    private final Handler             mainHandler = new Handler(Looper.getMainLooper());
    private final Gson                gson = new Gson();

    public interface ProgressCallback {
        void onSuccess();
        void onError(String error);
    }

    public interface ResumeCallback {
        /** @param chapterId      last chapter the user was reading
         *  @param scrollPosition pixel offset to restore
         *  @param progressPct    0-100 */
        void onResult(String chapterId, int scrollPosition, double progressPct);
        void onError(String error);
    }

    @Inject
    public ReadingProgressRepository(
            ReadingProgressDao dao,
            SupabaseDatabaseService db,
            UserPreferences prefs) {
        this.dao   = dao;
        this.db    = db;
        this.prefs = prefs;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Public API
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Rule 1 / Rule 7 — Upsert progress via RPC. Falls back to local cache on failure (Rule 8).
     *
     * @param novelId         novel being read
     * @param chapterId       current chapter
     * @param scrollPosition  current pixel offset of the scroll view
     * @param progressPct     0-100 calculated by the ViewModel
     */
    public void upsertProgress(
            String novelId,
            String chapterId,
            int scrollPosition,
            double progressPct,
            ProgressCallback callback) {

        String userId = prefs.getUserId();
        String token  = prefs.getAccessToken();
        if (userId == null || token == null) {
            if (callback != null) callback.onError("User not authenticated");
            return;
        }

        boolean isCompleted = progressPct >= COMPLETION_THRESHOLD;

        JsonObject params = new JsonObject();
        params.addProperty("p_user_id",          userId);
        params.addProperty("p_novel_id",          novelId);
        params.addProperty("p_chapter_id",        chapterId);
        params.addProperty("p_scroll_position",   scrollPosition);
        params.addProperty("p_progress_percentage", progressPct);
        params.addProperty("p_is_completed",      isCompleted);
        params.addProperty("p_last_read_at",      ISO.format(new Date()));

        // Rule 1: always RPC, never direct table write.
        db.callRpc(RPC_UPSERT, params, token, new SupabaseDatabaseService.DatabaseCallback() {
            @Override
            public void onSuccess(String response) {
                // Mark any previously queued offline row as synced.
                executor.execute(() -> dao.markSynced(userId, novelId, new Date()));
                if (callback != null) mainHandler.post(callback::onSuccess);
            }

            @Override
            public void onError(String error) {
                Log.w(TAG, "RPC failed, caching locally: " + error);
                // Rule 8: cache locally so syncPendingProgress() can replay later.
                cacheLocally(userId, novelId, chapterId, scrollPosition, progressPct, isCompleted);
                if (callback != null) mainHandler.post(() -> callback.onError(error));
            }
        });
    }

    /**
     * Rule 2 / Rule 3 — Fetch the user's resume position from the continue_reading view.
     * This is the ONLY place we read progress for "Continue Reading" purposes.
     */
    public void getResumePosition(String novelId, ResumeCallback callback) {
        String userId = prefs.getUserId();
        String token  = prefs.getAccessToken();
        if (userId == null || token == null) {
            callback.onError("User not authenticated");
            return;
        }

        // Rule 2: query the view, not reading_progress table directly.
        String url = db.buildSelectUrl(
                VIEW_CONTINUE,
                "chapter_id,scroll_position,progress_percentage",
                "user_id=eq." + userId + "&novel_id=eq." + novelId,
                null);

        db.rawSelect(url, token, new SupabaseDatabaseService.DatabaseCallback() {
            @Override
            public void onSuccess(String response) {
                try {
                    JsonArray arr = gson.fromJson(response, JsonArray.class);
                    if (arr != null && arr.size() > 0) {
                        JsonObject row     = arr.get(0).getAsJsonObject();
                        // Rule 3: use chapter_id + scroll_position for resume.
                        String chapterId   = getStringOrNull(row, "chapter_id");
                        int    scrollPos   = getIntOrZero(row, "scroll_position");
                        double progressPct = getDoubleOrZero(row, "progress_percentage");
                        mainHandler.post(() -> callback.onResult(chapterId, scrollPos, progressPct));
                    } else {
                        // No prior progress — start from beginning.
                        mainHandler.post(() -> callback.onResult(null, 0, 0));
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Failed to parse continue_reading response", e);
                    mainHandler.post(() -> callback.onError(e.getMessage()));
                }
            }

            @Override
            public void onError(String error) {
                // Rule 8: fall back to local cache if remote fails.
                executor.execute(() -> {
                    ReadingProgressEntity cached = dao.getProgressForNovel(userId, novelId);
                    if (cached != null) {
                        mainHandler.post(() -> callback.onResult(
                                cached.getChapterId(),
                                cached.getScrollPosition() != null ? cached.getScrollPosition() : 0,
                                cached.getProgressPercentage() != null ? cached.getProgressPercentage() : 0));
                    } else {
                        mainHandler.post(() -> callback.onResult(null, 0, 0));
                    }
                });
            }
        });
    }

    /**
     * Rule 8 — Replay all locally-cached rows that haven't reached Supabase yet.
     * Call this when the device comes back online (e.g. from a NetworkCallback).
     */
    public void syncPendingProgress() {
        String token = prefs.getAccessToken();
        if (token == null) return;

        executor.execute(() -> {
            List<ReadingProgressEntity> pending = dao.getPendingSync();
            if (pending == null || pending.isEmpty()) return;
            Log.d(TAG, "Syncing " + pending.size() + " pending progress rows");
            for (ReadingProgressEntity e : pending) {
                JsonObject params = new JsonObject();
                params.addProperty("p_user_id",        e.getUserId());
                params.addProperty("p_novel_id",        e.getNovelId());
                params.addProperty("p_chapter_id",      e.getChapterId());
                params.addProperty("p_scroll_position", e.getScrollPosition() != null ? e.getScrollPosition() : 0);
                params.addProperty("p_progress_percentage", e.getProgressPercentage() != null ? e.getProgressPercentage() : 0.0);
                params.addProperty("p_is_completed",    e.getIsCompleted() != null && e.getIsCompleted());
                params.addProperty("p_last_read_at",    ISO.format(e.getLastReadAt() != null ? e.getLastReadAt() : new Date()));

                db.callRpc(RPC_UPSERT, params, token, new SupabaseDatabaseService.DatabaseCallback() {
                    @Override public void onSuccess(String r) {
                        executor.execute(() -> dao.markSynced(e.getUserId(), e.getNovelId(), new Date()));
                    }
                    @Override public void onError(String err) {
                        Log.w(TAG, "Sync retry failed for novel=" + e.getNovelId() + ": " + err);
                    }
                });
            }
        });
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Private helpers
    // ─────────────────────────────────────────────────────────────────────────

    /** Rule 8 — Write to Room so we can sync later. */
    private void cacheLocally(
            String userId, String novelId, String chapterId,
            int scrollPosition, double progressPct, boolean isCompleted) {
        executor.execute(() -> {
            ReadingProgressEntity existing = dao.getProgressForNovel(userId, novelId);
            ReadingProgressEntity entity   = existing != null ? existing : new ReadingProgressEntity();

            if (entity.getId() == null) entity.setId(UUID.randomUUID().toString());
            entity.setUserId(userId);
            entity.setNovelId(novelId);
            entity.setChapterId(chapterId);
            entity.setScrollPosition(scrollPosition);
            entity.setProgressPercentage(progressPct);
            entity.setIsCompleted(isCompleted);
            entity.setLastReadAt(new Date());
            entity.setNeedsSync(true);
            entity.setIsDirty(true);

            dao.insertOrUpdate(entity);
        });
    }

    private String getStringOrNull(JsonObject obj, String key) {
        JsonElement el = obj.get(key);
        return (el != null && !el.isJsonNull()) ? el.getAsString() : null;
    }

    private int getIntOrZero(JsonObject obj, String key) {
        JsonElement el = obj.get(key);
        return (el != null && !el.isJsonNull()) ? el.getAsInt() : 0;
    }

    private double getDoubleOrZero(JsonObject obj, String key) {
        JsonElement el = obj.get(key);
        return (el != null && !el.isJsonNull()) ? el.getAsDouble() : 0.0;
    }
}

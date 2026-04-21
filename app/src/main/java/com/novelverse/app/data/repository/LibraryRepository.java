package com.novelverse.app.data.repository;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.Transformations;

import com.google.gson.JsonObject;
import com.novelverse.app.data.local.dao.BookmarkDao;
import com.novelverse.app.data.local.dao.ReadingProgressDao;
import com.novelverse.app.data.local.preferences.UserPreferences;
import com.novelverse.app.data.remote.supabase.SupabaseDatabaseService;
import com.novelverse.app.domain.models.Bookmark;
import com.novelverse.app.domain.models.ReadingProgress;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class LibraryRepository {

    public static final String STATUS_READING = "reading";
    public static final String STATUS_COMPLETED = "completed";
    public static final String STATUS_PLAN = "plan_to_read";
    public static final String STATUS_FAVORITES = "favorites";
    public static final String STATUS_DOWNLOADS = "downloads";

    private static final String TABLE_LIBRARY = "user_library";

    private final ReadingProgressDao readingProgressDao;
    private final BookmarkDao bookmarkDao;
    private final SupabaseDatabaseService databaseService;
    private final UserPreferences userPreferences;
    private final ExecutorService executor;

    @Inject
    public LibraryRepository(
            ReadingProgressDao readingProgressDao,
            BookmarkDao bookmarkDao,
            SupabaseDatabaseService databaseService,
            UserPreferences userPreferences) {
        this.readingProgressDao = readingProgressDao;
        this.bookmarkDao = bookmarkDao;
        this.databaseService = databaseService;
        this.userPreferences = userPreferences;
        this.executor = Executors.newFixedThreadPool(2);
    }

    // ── Get reading progress ──────────────────────────────────────────────

    public LiveData<List<ReadingProgress>> getReadingProgress(String userId) {
        return Transformations.map(
                readingProgressDao.getAllProgressForUser(userId),
                entities -> {
                    List<ReadingProgress> list = new java.util.ArrayList<>();
                    if (entities == null) return list;
                    for (com.novelverse.app.data.local.entities.ReadingProgressEntity e :
                            entities) {
                        ReadingProgress rp = new ReadingProgress();
                        rp.setId(e.getId());
                        rp.setUserId(e.getUserId());
                        rp.setNovelId(e.getNovelId());
                        rp.setChapterId(e.getChapterId());
                        rp.setScrollPosition(
                                e.getScrollPosition() != null ? e.getScrollPosition() : 0);
                        rp.setProgressPercentage(
                                e.getProgressPercentage() != null
                                        ? e.getProgressPercentage()
                                        : 0.0);
                        rp.setCompleted(e.getIsCompleted() != null && e.getIsCompleted());
                        rp.setCompletedAt(e.getCompletedAt());
                        rp.setLastReadAt(e.getLastReadAt());
                        rp.setTotalReadingTime(
                                e.getTotalReadingTime() != null ? e.getTotalReadingTime() : 0);
                        rp.setDeviceInfo(e.getDeviceInfo());
                        list.add(rp);
                    }
                    return list;
                });
    }

    public void updateReadingProgress(ReadingProgress progress, SimpleCallback callback) {
        executor.execute(
                () -> {
                    try {
                        callback.onResult(true, null);
                    } catch (Exception e) {
                        callback.onResult(false, e.getMessage());
                    }
                });
    }

    // ── Library add / remove (Supabase user_library table) ───────────────

    /**
     * Adds a novel to the user's library with the given status. Uses an UPSERT so calling it again
     * with a different status updates it.
     *
     * <p>Table schema expected: user_library(id uuid, user_id uuid, novel_id uuid, status text,
     * added_at timestamptz)
     */
    public void addToLibrary(String novelId, String status, SimpleCallback callback) {
        String userId = userPreferences.getUserId();
        String token = userPreferences.getAccessToken();
        if (userId == null) {
            callback.onResult(false, "Not logged in");
            return;
        }
        JsonObject body = new JsonObject();
        body.addProperty("user_id", userId);
        body.addProperty("novel_id", novelId);
        body.addProperty("status", status != null ? status : STATUS_READING);
        // created_at has DEFAULT NOW() — do not send it to avoid schema mismatch

        // Prefer upsert: if a row with same user_id+novel_id exists, update status
        databaseService.upsert(
                TABLE_LIBRARY,
                body,
                "user_id,novel_id",
                token,
                new SupabaseDatabaseService.DatabaseCallback() {
                    @Override
                    public void onSuccess(String r) {
                        callback.onResult(true, null);
                    }

                    @Override
                    public void onError(String e) {
                        callback.onResult(false, e);
                    }
                });
    }

    /** Removes a novel from the user's library. */
    public void removeFromLibrary(String novelId, SimpleCallback callback) {
        String userId = userPreferences.getUserId();
        String token = userPreferences.getAccessToken();
        if (userId == null) {
            callback.onResult(false, "Not logged in");
            return;
        }
        String filter = "user_id=eq." + userId + "&novel_id=eq." + novelId;
        databaseService.deleteWhere(
                TABLE_LIBRARY,
                filter,
                token,
                new SupabaseDatabaseService.DatabaseCallback() {
                    @Override
                    public void onSuccess(String r) {
                        callback.onResult(true, null);
                    }

                    @Override
                    public void onError(String e) {
                        callback.onResult(false, e);
                    }
                });
    }

    /** Checks if a novel is already in the user's library. */
    public void isInLibrary(String novelId, BooleanCallback callback) {
        String userId = userPreferences.getUserId();
        String token = userPreferences.getAccessToken();
        if (userId == null) {
            callback.onResult(false);
            return;
        }
        String filter = "user_id=eq." + userId + "&novel_id=eq." + novelId + "&select=id";
        databaseService.selectWhere(
                TABLE_LIBRARY,
                "id",
                filter,
                null,
                token,
                new SupabaseDatabaseService.DatabaseCallback() {
                    @Override
                    public void onSuccess(String r) {
                        callback.onResult(r != null && r.contains("id"));
                    }

                    @Override
                    public void onError(String e) {
                        callback.onResult(false);
                    }
                });
    }

    // ── Get by status tab ─────────────────────────────────────────────────

    public LiveData<List<Bookmark>> getByStatus(String userId, String status) {
        if (STATUS_FAVORITES.equals(status)) {
            return Transformations.map(
                    bookmarkDao.getBookmarksForUser(userId),
                    entities -> {
                        List<Bookmark> list = new java.util.ArrayList<>();
                        if (entities == null) return list;
                        for (com.novelverse.app.data.local.entities.BookmarkEntity e : entities) {
                            if (Boolean.TRUE.equals(e.getIsFavorite())) {
                                Bookmark b = new Bookmark();
                                b.setId(e.getId());
                                b.setUserId(e.getUserId());
                                b.setNovelId(e.getNovelId());
                                b.setFavorite(true);
                                list.add(b);
                            }
                        }
                        return list;
                    });
        } else {
            return Transformations.map(
                    readingProgressDao.getAllProgressForUser(userId),
                    entities -> {
                        List<Bookmark> list = new java.util.ArrayList<>();
                        if (entities == null) return list;
                        for (com.novelverse.app.data.local.entities.ReadingProgressEntity e :
                                entities) {
                            boolean include;
                            if (STATUS_COMPLETED.equals(status)) {
                                include = Boolean.TRUE.equals(e.getIsCompleted());
                            } else if (STATUS_READING.equals(status)) {
                                include = !Boolean.TRUE.equals(e.getIsCompleted());
                            } else {
                                include = true;
                            }
                            if (include) {
                                Bookmark b = new Bookmark();
                                b.setId(e.getId());
                                b.setUserId(e.getUserId());
                                b.setNovelId(e.getNovelId());
                                list.add(b);
                            }
                        }
                        return list;
                    });
        }
    }

    /**
     * Counts how many novels the user has marked as completed. Used by ProfileFragment to show the
     * "Books Read" stat.
     */
    public void getCompletedCount(String userId, IntCallback callback) {
        executor.execute(
                () -> {
                    int count = 0;
                    try {
                        java.util.List<com.novelverse.app.data.local.entities.ReadingProgressEntity>
                                completed = readingProgressDao.getCompletedNovels(userId);
                        if (completed != null) count = completed.size();
                    } catch (Exception ignored) {
                    }
                    final int result = count;
                    new android.os.Handler(android.os.Looper.getMainLooper())
                            .post(() -> callback.onResult(result));
                });
    }

    // ── Callbacks ─────────────────────────────────────────────────────────

    public interface SimpleCallback {
        void onResult(boolean success, String error);
    }

    public interface BooleanCallback {
        void onResult(boolean inLibrary);
    }

    public interface IntCallback {
        void onResult(int count);
    }
}

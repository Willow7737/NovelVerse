package com.novelverse.app.data.local.dao;

import android.database.Cursor;
import androidx.lifecycle.LiveData;
import androidx.paging.PagingSource;
import androidx.room.EntityDeletionOrUpdateAdapter;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.paging.LimitOffsetPagingSource;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;

import com.novelverse.app.data.local.database.Converters;
import com.novelverse.app.data.local.entities.NovelEntity;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@SuppressWarnings({"unchecked", "deprecation"})
public final class NovelDao_Impl implements NovelDao {

    private final RoomDatabase __db;
    private final EntityInsertionAdapter<NovelEntity> __insertionAdapterOfNovelEntity;
    private final EntityDeletionOrUpdateAdapter<NovelEntity> __deletionAdapterOfNovelEntity;
    private final EntityDeletionOrUpdateAdapter<NovelEntity> __updateAdapterOfNovelEntity;

    public NovelDao_Impl(RoomDatabase db) {
        this.__db = db;
        this.__insertionAdapterOfNovelEntity = new EntityInsertionAdapter<NovelEntity>(db) {
            @Override
            public String createQuery() {
                return "INSERT OR REPLACE INTO `novels` (`id`,`author_id`,`title`,`slug`,`description`,"
                        + "`cover_image_url`,`banner_image_url`,`status`,`visibility`,`price_type`,`price`,"
                        + "`points_cost`,`age_rating`,`content_warnings`,`license_type`,`language`,"
                        + "`total_chapters`,`total_words`,`total_views`,`total_likes`,`total_bookmarks`,"
                        + "`total_comments`,`average_rating`,`rating_count`,`is_published`,`published_at`,"
                        + "`last_updated_at`,`is_featured`,`tags`,`genres`,`author_username`,"
                        + "`author_display_name`,`author_avatar_url`,`is_author_verified`,`moderation_status`,"
                        + "`is_downloaded`,`download_completed_at`,`created_at`,`featured_at`,`completed_at`,"
                        + "`synced_at`,`is_dirty`) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
            }
            @Override
            public void bind(SupportSQLiteStatement stmt, NovelEntity e) {
                bindNovel(stmt, e);
            }
        };
        this.__deletionAdapterOfNovelEntity = new EntityDeletionOrUpdateAdapter<NovelEntity>(db) {
            @Override
            public String createQuery() { return "DELETE FROM `novels` WHERE `id` = ?"; }
            @Override
            public void bind(SupportSQLiteStatement stmt, NovelEntity e) {
                stmt.bindString(1, e.getId());
            }
        };
        this.__updateAdapterOfNovelEntity = new EntityDeletionOrUpdateAdapter<NovelEntity>(db) {
            @Override
            public String createQuery() {
                return "UPDATE OR ABORT `novels` SET `id`=?,`author_id`=?,`title`=?,`slug`=?,`description`=?,"
                        + "`cover_image_url`=?,`banner_image_url`=?,`status`=?,`visibility`=?,`price_type`=?,`price`=?,"
                        + "`points_cost`=?,`age_rating`=?,`content_warnings`=?,`license_type`=?,`language`=?,"
                        + "`total_chapters`=?,`total_words`=?,`total_views`=?,`total_likes`=?,`total_bookmarks`=?,"
                        + "`total_comments`=?,`average_rating`=?,`rating_count`=?,`is_published`=?,`published_at`=?,"
                        + "`last_updated_at`=?,`is_featured`=?,`tags`=?,`genres`=?,`author_username`=?,"
                        + "`author_display_name`=?,`author_avatar_url`=?,`is_author_verified`=?,`moderation_status`=?,"
                        + "`is_downloaded`=?,`download_completed_at`=?,`created_at`=?,`featured_at`=?,`completed_at`=?,"
                        + "`synced_at`=?,`is_dirty`=? WHERE `id`=?";
            }
            @Override
            public void bind(SupportSQLiteStatement stmt, NovelEntity e) {
                bindNovel(stmt, e);
                stmt.bindString(43, e.getId());
            }
        };
    }

    private void bindNovel(SupportSQLiteStatement stmt, NovelEntity e) {
        stmt.bindString(1, e.getId());
        if (e.getAuthorId() == null) stmt.bindNull(2); else stmt.bindString(2, e.getAuthorId());
        if (e.getTitle() == null) stmt.bindNull(3); else stmt.bindString(3, e.getTitle());
        if (e.getSlug() == null) stmt.bindNull(4); else stmt.bindString(4, e.getSlug());
        if (e.getDescription() == null) stmt.bindNull(5); else stmt.bindString(5, e.getDescription());
        if (e.getCoverImageUrl() == null) stmt.bindNull(6); else stmt.bindString(6, e.getCoverImageUrl());
        if (e.getBannerImageUrl() == null) stmt.bindNull(7); else stmt.bindString(7, e.getBannerImageUrl());
        if (e.getStatus() == null) stmt.bindNull(8); else stmt.bindString(8, e.getStatus());
        if (e.getVisibility() == null) stmt.bindNull(9); else stmt.bindString(9, e.getVisibility());
        if (e.getPriceType() == null) stmt.bindNull(10); else stmt.bindString(10, e.getPriceType());
        if (e.getPrice() == null) stmt.bindNull(11); else stmt.bindDouble(11, e.getPrice());
        if (e.getPointsCost() == null) stmt.bindNull(12); else stmt.bindLong(12, e.getPointsCost());
        if (e.getAgeRating() == null) stmt.bindNull(13); else stmt.bindString(13, e.getAgeRating());
        final String _cw = Converters.toStringList(e.getContentWarnings());
        if (_cw == null) stmt.bindNull(14); else stmt.bindString(14, _cw);
        if (e.getLicenseType() == null) stmt.bindNull(15); else stmt.bindString(15, e.getLicenseType());
        if (e.getLanguage() == null) stmt.bindNull(16); else stmt.bindString(16, e.getLanguage());
        if (e.getTotalChapters() == null) stmt.bindNull(17); else stmt.bindLong(17, e.getTotalChapters());
        if (e.getTotalWords() == null) stmt.bindNull(18); else stmt.bindLong(18, e.getTotalWords());
        if (e.getTotalViews() == null) stmt.bindNull(19); else stmt.bindLong(19, e.getTotalViews());
        if (e.getTotalLikes() == null) stmt.bindNull(20); else stmt.bindLong(20, e.getTotalLikes());
        if (e.getTotalBookmarks() == null) stmt.bindNull(21); else stmt.bindLong(21, e.getTotalBookmarks());
        if (e.getTotalComments() == null) stmt.bindNull(22); else stmt.bindLong(22, e.getTotalComments());
        if (e.getAverageRating() == null) stmt.bindNull(23); else stmt.bindDouble(23, e.getAverageRating());
        if (e.getRatingCount() == null) stmt.bindNull(24); else stmt.bindLong(24, e.getRatingCount());
        final Integer _pub = Converters.booleanToInteger(e.getIsPublished());
        if (_pub == null) stmt.bindNull(25); else stmt.bindLong(25, _pub);
        final Long _publishedAt = Converters.dateToTimestamp(e.getPublishedAt());
        if (_publishedAt == null) stmt.bindNull(26); else stmt.bindLong(26, _publishedAt);
        final Long _lastUpdated = Converters.dateToTimestamp(e.getLastUpdatedAt());
        if (_lastUpdated == null) stmt.bindNull(27); else stmt.bindLong(27, _lastUpdated);
        final Integer _featured = Converters.booleanToInteger(e.getIsFeatured());
        if (_featured == null) stmt.bindNull(28); else stmt.bindLong(28, _featured);
        final String _tags = Converters.toStringList(e.getTags());
        if (_tags == null) stmt.bindNull(29); else stmt.bindString(29, _tags);
        final String _genres = Converters.toStringList(e.getGenres());
        if (_genres == null) stmt.bindNull(30); else stmt.bindString(30, _genres);
        if (e.getAuthorUsername() == null) stmt.bindNull(31); else stmt.bindString(31, e.getAuthorUsername());
        if (e.getAuthorDisplayName() == null) stmt.bindNull(32); else stmt.bindString(32, e.getAuthorDisplayName());
        if (e.getAuthorAvatarUrl() == null) stmt.bindNull(33); else stmt.bindString(33, e.getAuthorAvatarUrl());
        final Integer _verified = Converters.booleanToInteger(e.getIsAuthorVerified());
        if (_verified == null) stmt.bindNull(34); else stmt.bindLong(34, _verified);
        if (e.getModerationStatus() == null) stmt.bindNull(35); else stmt.bindString(35, e.getModerationStatus());
        final Integer _downloaded = Converters.booleanToInteger(e.getIsDownloaded());
        if (_downloaded == null) stmt.bindNull(36); else stmt.bindLong(36, _downloaded);
        final Long _dlAt = Converters.dateToTimestamp(e.getDownloadCompletedAt());
        if (_dlAt == null) stmt.bindNull(37); else stmt.bindLong(37, _dlAt);
        final Long _createdAt = Converters.dateToTimestamp(e.getCreatedAt());
        if (_createdAt == null) stmt.bindNull(38); else stmt.bindLong(38, _createdAt);
        final Long _featuredAt = Converters.dateToTimestamp(e.getFeaturedAt());
        if (_featuredAt == null) stmt.bindNull(39); else stmt.bindLong(39, _featuredAt);
        final Long _completedAt = Converters.dateToTimestamp(e.getCompletedAt());
        if (_completedAt == null) stmt.bindNull(40); else stmt.bindLong(40, _completedAt);
        final Long _syncedAt = Converters.dateToTimestamp(e.getSyncedAt());
        if (_syncedAt == null) stmt.bindNull(41); else stmt.bindLong(41, _syncedAt);
        final Integer _dirty = Converters.booleanToInteger(e.getIsDirty());
        if (_dirty == null) stmt.bindNull(42); else stmt.bindLong(42, _dirty);
    }

    private NovelEntity cursorToNovel(Cursor c) {
        NovelEntity e = new NovelEntity();
        e.setId(c.getString(CursorUtil.getColumnIndexOrThrow(c, "id")));
        e.setAuthorId(c.isNull(CursorUtil.getColumnIndexOrThrow(c, "author_id")) ? null : c.getString(CursorUtil.getColumnIndexOrThrow(c, "author_id")));
        e.setTitle(c.isNull(CursorUtil.getColumnIndexOrThrow(c, "title")) ? null : c.getString(CursorUtil.getColumnIndexOrThrow(c, "title")));
        e.setSlug(c.isNull(CursorUtil.getColumnIndexOrThrow(c, "slug")) ? null : c.getString(CursorUtil.getColumnIndexOrThrow(c, "slug")));
        e.setDescription(c.isNull(CursorUtil.getColumnIndexOrThrow(c, "description")) ? null : c.getString(CursorUtil.getColumnIndexOrThrow(c, "description")));
        e.setCoverImageUrl(c.isNull(CursorUtil.getColumnIndexOrThrow(c, "cover_image_url")) ? null : c.getString(CursorUtil.getColumnIndexOrThrow(c, "cover_image_url")));
        e.setBannerImageUrl(c.isNull(CursorUtil.getColumnIndexOrThrow(c, "banner_image_url")) ? null : c.getString(CursorUtil.getColumnIndexOrThrow(c, "banner_image_url")));
        e.setStatus(c.isNull(CursorUtil.getColumnIndexOrThrow(c, "status")) ? null : c.getString(CursorUtil.getColumnIndexOrThrow(c, "status")));
        e.setVisibility(c.isNull(CursorUtil.getColumnIndexOrThrow(c, "visibility")) ? null : c.getString(CursorUtil.getColumnIndexOrThrow(c, "visibility")));
        e.setPriceType(c.isNull(CursorUtil.getColumnIndexOrThrow(c, "price_type")) ? null : c.getString(CursorUtil.getColumnIndexOrThrow(c, "price_type")));
        e.setPrice(c.isNull(CursorUtil.getColumnIndexOrThrow(c, "price")) ? null : c.getDouble(CursorUtil.getColumnIndexOrThrow(c, "price")));
        e.setPointsCost(c.isNull(CursorUtil.getColumnIndexOrThrow(c, "points_cost")) ? null : c.getInt(CursorUtil.getColumnIndexOrThrow(c, "points_cost")));
        e.setAgeRating(c.isNull(CursorUtil.getColumnIndexOrThrow(c, "age_rating")) ? null : c.getString(CursorUtil.getColumnIndexOrThrow(c, "age_rating")));
        e.setContentWarnings(Converters.fromStringList(c.isNull(CursorUtil.getColumnIndexOrThrow(c, "content_warnings")) ? null : c.getString(CursorUtil.getColumnIndexOrThrow(c, "content_warnings"))));
        e.setLicenseType(c.isNull(CursorUtil.getColumnIndexOrThrow(c, "license_type")) ? null : c.getString(CursorUtil.getColumnIndexOrThrow(c, "license_type")));
        e.setLanguage(c.isNull(CursorUtil.getColumnIndexOrThrow(c, "language")) ? null : c.getString(CursorUtil.getColumnIndexOrThrow(c, "language")));
        e.setTotalChapters(c.isNull(CursorUtil.getColumnIndexOrThrow(c, "total_chapters")) ? null : c.getInt(CursorUtil.getColumnIndexOrThrow(c, "total_chapters")));
        e.setTotalWords(c.isNull(CursorUtil.getColumnIndexOrThrow(c, "total_words")) ? null : c.getLong(CursorUtil.getColumnIndexOrThrow(c, "total_words")));
        e.setTotalViews(c.isNull(CursorUtil.getColumnIndexOrThrow(c, "total_views")) ? null : c.getLong(CursorUtil.getColumnIndexOrThrow(c, "total_views")));
        e.setTotalLikes(c.isNull(CursorUtil.getColumnIndexOrThrow(c, "total_likes")) ? null : c.getInt(CursorUtil.getColumnIndexOrThrow(c, "total_likes")));
        e.setTotalBookmarks(c.isNull(CursorUtil.getColumnIndexOrThrow(c, "total_bookmarks")) ? null : c.getInt(CursorUtil.getColumnIndexOrThrow(c, "total_bookmarks")));
        e.setTotalComments(c.isNull(CursorUtil.getColumnIndexOrThrow(c, "total_comments")) ? null : c.getInt(CursorUtil.getColumnIndexOrThrow(c, "total_comments")));
        e.setAverageRating(c.isNull(CursorUtil.getColumnIndexOrThrow(c, "average_rating")) ? null : c.getDouble(CursorUtil.getColumnIndexOrThrow(c, "average_rating")));
        e.setRatingCount(c.isNull(CursorUtil.getColumnIndexOrThrow(c, "rating_count")) ? null : c.getInt(CursorUtil.getColumnIndexOrThrow(c, "rating_count")));
        e.setIsPublished(Converters.fromInteger(c.isNull(CursorUtil.getColumnIndexOrThrow(c, "is_published")) ? null : c.getInt(CursorUtil.getColumnIndexOrThrow(c, "is_published"))));
        e.setPublishedAt(Converters.fromTimestamp(c.isNull(CursorUtil.getColumnIndexOrThrow(c, "published_at")) ? null : c.getLong(CursorUtil.getColumnIndexOrThrow(c, "published_at"))));
        e.setLastUpdatedAt(Converters.fromTimestamp(c.isNull(CursorUtil.getColumnIndexOrThrow(c, "last_updated_at")) ? null : c.getLong(CursorUtil.getColumnIndexOrThrow(c, "last_updated_at"))));
        e.setIsFeatured(Converters.fromInteger(c.isNull(CursorUtil.getColumnIndexOrThrow(c, "is_featured")) ? null : c.getInt(CursorUtil.getColumnIndexOrThrow(c, "is_featured"))));
        e.setTags(Converters.fromStringList(c.isNull(CursorUtil.getColumnIndexOrThrow(c, "tags")) ? null : c.getString(CursorUtil.getColumnIndexOrThrow(c, "tags"))));
        e.setGenres(Converters.fromStringList(c.isNull(CursorUtil.getColumnIndexOrThrow(c, "genres")) ? null : c.getString(CursorUtil.getColumnIndexOrThrow(c, "genres"))));
        e.setAuthorUsername(c.isNull(CursorUtil.getColumnIndexOrThrow(c, "author_username")) ? null : c.getString(CursorUtil.getColumnIndexOrThrow(c, "author_username")));
        e.setAuthorDisplayName(c.isNull(CursorUtil.getColumnIndexOrThrow(c, "author_display_name")) ? null : c.getString(CursorUtil.getColumnIndexOrThrow(c, "author_display_name")));
        e.setAuthorAvatarUrl(c.isNull(CursorUtil.getColumnIndexOrThrow(c, "author_avatar_url")) ? null : c.getString(CursorUtil.getColumnIndexOrThrow(c, "author_avatar_url")));
        e.setIsAuthorVerified(Converters.fromInteger(c.isNull(CursorUtil.getColumnIndexOrThrow(c, "is_author_verified")) ? null : c.getInt(CursorUtil.getColumnIndexOrThrow(c, "is_author_verified"))));
        e.setModerationStatus(c.isNull(CursorUtil.getColumnIndexOrThrow(c, "moderation_status")) ? null : c.getString(CursorUtil.getColumnIndexOrThrow(c, "moderation_status")));
        e.setIsDownloaded(Converters.fromInteger(c.isNull(CursorUtil.getColumnIndexOrThrow(c, "is_downloaded")) ? null : c.getInt(CursorUtil.getColumnIndexOrThrow(c, "is_downloaded"))));
        e.setDownloadCompletedAt(Converters.fromTimestamp(c.isNull(CursorUtil.getColumnIndexOrThrow(c, "download_completed_at")) ? null : c.getLong(CursorUtil.getColumnIndexOrThrow(c, "download_completed_at"))));
        e.setCreatedAt(Converters.fromTimestamp(c.isNull(CursorUtil.getColumnIndexOrThrow(c, "created_at")) ? null : c.getLong(CursorUtil.getColumnIndexOrThrow(c, "created_at"))));
        e.setFeaturedAt(Converters.fromTimestamp(c.isNull(CursorUtil.getColumnIndexOrThrow(c, "featured_at")) ? null : c.getLong(CursorUtil.getColumnIndexOrThrow(c, "featured_at"))));
        e.setCompletedAt(Converters.fromTimestamp(c.isNull(CursorUtil.getColumnIndexOrThrow(c, "completed_at")) ? null : c.getLong(CursorUtil.getColumnIndexOrThrow(c, "completed_at"))));
        e.setSyncedAt(Converters.fromTimestamp(c.isNull(CursorUtil.getColumnIndexOrThrow(c, "synced_at")) ? null : c.getLong(CursorUtil.getColumnIndexOrThrow(c, "synced_at"))));
        e.setIsDirty(Converters.fromInteger(c.isNull(CursorUtil.getColumnIndexOrThrow(c, "is_dirty")) ? null : c.getInt(CursorUtil.getColumnIndexOrThrow(c, "is_dirty"))));
        return e;
    }

    @Override public void insert(NovelEntity novel) { __db.assertNotSuspendingTransaction(); __db.beginTransaction(); try { __insertionAdapterOfNovelEntity.insert(novel); __db.setTransactionSuccessful(); } finally { __db.endTransaction(); } }
    @Override public void insertAll(List<NovelEntity> novels) { __db.assertNotSuspendingTransaction(); __db.beginTransaction(); try { __insertionAdapterOfNovelEntity.insert(novels); __db.setTransactionSuccessful(); } finally { __db.endTransaction(); } }
    @Override public void update(NovelEntity novel) { __db.assertNotSuspendingTransaction(); __db.beginTransaction(); try { __updateAdapterOfNovelEntity.handle(novel); __db.setTransactionSuccessful(); } finally { __db.endTransaction(); } }
    @Override public void delete(NovelEntity novel) { __db.assertNotSuspendingTransaction(); __db.beginTransaction(); try { __deletionAdapterOfNovelEntity.handle(novel); __db.setTransactionSuccessful(); } finally { __db.endTransaction(); } }

    @Override public void deleteById(String novelId) { __db.assertNotSuspendingTransaction(); final SupportSQLiteStatement s = __db.compileStatement("DELETE FROM novels WHERE id = ?"); __db.beginTransaction(); try { s.bindString(1, novelId); s.executeUpdateDelete(); __db.setTransactionSuccessful(); } finally { __db.endTransaction(); } }
    @Override public void updateDownloadStatus(String novelId, boolean isDownloaded, long timestamp) { __db.assertNotSuspendingTransaction(); final SupportSQLiteStatement s = __db.compileStatement("UPDATE novels SET is_downloaded = ?, download_completed_at = ? WHERE id = ?"); __db.beginTransaction(); try { s.bindLong(1, isDownloaded ? 1 : 0); s.bindLong(2, timestamp); s.bindString(3, novelId); s.executeUpdateDelete(); __db.setTransactionSuccessful(); } finally { __db.endTransaction(); } }
    @Override public void markAsSynced(String novelId, long timestamp) { __db.assertNotSuspendingTransaction(); final SupportSQLiteStatement s = __db.compileStatement("UPDATE novels SET is_dirty = 0, synced_at = ? WHERE id = ?"); __db.beginTransaction(); try { s.bindLong(1, timestamp); s.bindString(2, novelId); s.executeUpdateDelete(); __db.setTransactionSuccessful(); } finally { __db.endTransaction(); } }
    @Override public void deleteOldCache(long timestamp) { __db.assertNotSuspendingTransaction(); final SupportSQLiteStatement s = __db.compileStatement("DELETE FROM novels WHERE synced_at < ? AND is_downloaded = 0"); __db.beginTransaction(); try { s.bindLong(1, timestamp); s.executeUpdateDelete(); __db.setTransactionSuccessful(); } finally { __db.endTransaction(); } }
    @Override public void deleteAll() { __db.assertNotSuspendingTransaction(); final SupportSQLiteStatement s = __db.compileStatement("DELETE FROM novels"); __db.beginTransaction(); try { s.executeUpdateDelete(); __db.setTransactionSuccessful(); } finally { __db.endTransaction(); } }

    @Override public NovelEntity getNovelByIdSync(String novelId) { final RoomSQLiteQuery q = RoomSQLiteQuery.acquire("SELECT * FROM novels WHERE id = ?", 1); q.bindString(1, novelId); final Cursor c = DBUtil.query(__db, q, false, null); try { if (c.moveToFirst()) return cursorToNovel(c); return null; } finally { c.close(); q.release(); } }
    @Override public List<NovelEntity> getDirtyNovels() { final RoomSQLiteQuery q = RoomSQLiteQuery.acquire("SELECT * FROM novels WHERE is_dirty = 1", 0); final Cursor c = DBUtil.query(__db, q, false, null); try { List<NovelEntity> r = new ArrayList<>(); while (c.moveToNext()) r.add(cursorToNovel(c)); return r; } finally { c.close(); q.release(); } }
    @Override public int getNovelCount() { final RoomSQLiteQuery q = RoomSQLiteQuery.acquire("SELECT COUNT(*) FROM novels", 0); final Cursor c = DBUtil.query(__db, q, false, null); try { return c.moveToFirst() ? c.getInt(0) : 0; } finally { c.close(); q.release(); } }

    @Override public LiveData<NovelEntity> getNovelById(String novelId) { final RoomSQLiteQuery q = RoomSQLiteQuery.acquire("SELECT * FROM novels WHERE id = ?", 1); q.bindString(1, novelId); return __db.getInvalidationTracker().createLiveData(new String[]{"novels"}, false, () -> { final Cursor c = DBUtil.query(__db, q, false, null); try { return c.moveToFirst() ? cursorToNovel(c) : null; } finally { c.close(); } }); }
    @Override public LiveData<List<NovelEntity>> getAllPublishedNovels() { final RoomSQLiteQuery q = RoomSQLiteQuery.acquire("SELECT * FROM novels WHERE is_published = 1 ORDER BY last_updated_at DESC", 0); return __db.getInvalidationTracker().createLiveData(new String[]{"novels"}, false, () -> { final Cursor c = DBUtil.query(__db, q, false, null); try { List<NovelEntity> r = new ArrayList<>(); while (c.moveToNext()) r.add(cursorToNovel(c)); return r; } finally { c.close(); } }); }
    @Override public LiveData<List<NovelEntity>> getNovelsByAuthor(String authorId) { final RoomSQLiteQuery q = RoomSQLiteQuery.acquire("SELECT * FROM novels WHERE author_id = ? ORDER BY created_at DESC", 1); q.bindString(1, authorId); return __db.getInvalidationTracker().createLiveData(new String[]{"novels"}, false, () -> { final Cursor c = DBUtil.query(__db, q, false, null); try { List<NovelEntity> r = new ArrayList<>(); while (c.moveToNext()) r.add(cursorToNovel(c)); return r; } finally { c.close(); } }); }
    @Override public LiveData<List<NovelEntity>> getFeaturedNovels() { final RoomSQLiteQuery q = RoomSQLiteQuery.acquire("SELECT * FROM novels WHERE is_featured = 1 AND is_published = 1 ORDER BY featured_at DESC", 0); return __db.getInvalidationTracker().createLiveData(new String[]{"novels"}, false, () -> { final Cursor c = DBUtil.query(__db, q, false, null); try { List<NovelEntity> r = new ArrayList<>(); while (c.moveToNext()) r.add(cursorToNovel(c)); return r; } finally { c.close(); } }); }
    @Override public LiveData<List<NovelEntity>> getTrendingNovels(int limit) { final RoomSQLiteQuery q = RoomSQLiteQuery.acquire("SELECT * FROM novels WHERE is_published = 1 ORDER BY total_views DESC LIMIT ?", 1); q.bindLong(1, limit); return __db.getInvalidationTracker().createLiveData(new String[]{"novels"}, false, () -> { final Cursor c = DBUtil.query(__db, q, false, null); try { List<NovelEntity> r = new ArrayList<>(); while (c.moveToNext()) r.add(cursorToNovel(c)); return r; } finally { c.close(); } }); }
    @Override public LiveData<List<NovelEntity>> getNewReleases(int limit) { final RoomSQLiteQuery q = RoomSQLiteQuery.acquire("SELECT * FROM novels WHERE is_published = 1 ORDER BY published_at DESC LIMIT ?", 1); q.bindLong(1, limit); return __db.getInvalidationTracker().createLiveData(new String[]{"novels"}, false, () -> { final Cursor c = DBUtil.query(__db, q, false, null); try { List<NovelEntity> r = new ArrayList<>(); while (c.moveToNext()) r.add(cursorToNovel(c)); return r; } finally { c.close(); } }); }
    @Override public LiveData<List<NovelEntity>> getCompletedNovels(int limit) { final RoomSQLiteQuery q = RoomSQLiteQuery.acquire("SELECT * FROM novels WHERE status = 'completed' AND is_published = 1 ORDER BY completed_at DESC LIMIT ?", 1); q.bindLong(1, limit); return __db.getInvalidationTracker().createLiveData(new String[]{"novels"}, false, () -> { final Cursor c = DBUtil.query(__db, q, false, null); try { List<NovelEntity> r = new ArrayList<>(); while (c.moveToNext()) r.add(cursorToNovel(c)); return r; } finally { c.close(); } }); }
    @Override public LiveData<List<NovelEntity>> getTopRatedNovels(int limit) { final RoomSQLiteQuery q = RoomSQLiteQuery.acquire("SELECT * FROM novels WHERE is_published = 1 ORDER BY average_rating DESC LIMIT ?", 1); q.bindLong(1, limit); return __db.getInvalidationTracker().createLiveData(new String[]{"novels"}, false, () -> { final Cursor c = DBUtil.query(__db, q, false, null); try { List<NovelEntity> r = new ArrayList<>(); while (c.moveToNext()) r.add(cursorToNovel(c)); return r; } finally { c.close(); } }); }
    @Override public LiveData<List<NovelEntity>> getDownloadedNovels() { final RoomSQLiteQuery q = RoomSQLiteQuery.acquire("SELECT * FROM novels WHERE is_downloaded = 1", 0); return __db.getInvalidationTracker().createLiveData(new String[]{"novels"}, false, () -> { final Cursor c = DBUtil.query(__db, q, false, null); try { List<NovelEntity> r = new ArrayList<>(); while (c.moveToNext()) r.add(cursorToNovel(c)); return r; } finally { c.close(); } }); }

    @Override public PagingSource<Integer, NovelEntity> getAllPublishedNovelsPaged() { return new LimitOffsetPagingSource<NovelEntity>(RoomSQLiteQuery.acquire("SELECT * FROM novels WHERE is_published = 1 ORDER BY last_updated_at DESC", 0), __db, "novels") { @Override protected List<NovelEntity> convertRows(Cursor c) { List<NovelEntity> r = new ArrayList<>(); while (c.moveToNext()) r.add(cursorToNovel(c)); return r; } }; }
    @Override public PagingSource<Integer, NovelEntity> searchNovels(String query) { final RoomSQLiteQuery q = RoomSQLiteQuery.acquire("SELECT * FROM novels WHERE title LIKE '%' || ? || '%' OR description LIKE '%' || ? || '%'", 2); q.bindString(1, query); q.bindString(2, query); return new LimitOffsetPagingSource<NovelEntity>(q, __db, "novels") { @Override protected List<NovelEntity> convertRows(Cursor c) { List<NovelEntity> r = new ArrayList<>(); while (c.moveToNext()) r.add(cursorToNovel(c)); return r; } }; }
    @Override public PagingSource<Integer, NovelEntity> getNovelsByGenre(String genre) { final RoomSQLiteQuery q = RoomSQLiteQuery.acquire("SELECT * FROM novels WHERE genres LIKE '%' || ? || '%' AND is_published = 1", 1); q.bindString(1, genre); return new LimitOffsetPagingSource<NovelEntity>(q, __db, "novels") { @Override protected List<NovelEntity> convertRows(Cursor c) { List<NovelEntity> r = new ArrayList<>(); while (c.moveToNext()) r.add(cursorToNovel(c)); return r; } }; }
}

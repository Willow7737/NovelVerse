package com.novelverse.app.data.repository;

import com.novelverse.app.domain.models.TrendingItem;
import com.novelverse.app.presentation.search.SearchFilterBottomSheet;

import java.util.ArrayList;
import java.util.List;

import androidx.lifecycle.LiveData;
import androidx.paging.Pager;
import androidx.paging.PagingConfig;
import androidx.paging.PagingData;
import androidx.paging.PagingLiveData;

import com.novelverse.app.data.local.dao.NovelDao;
import com.novelverse.app.data.remote.supabase.SupabaseDatabaseService;
import com.novelverse.app.domain.models.Novel;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Repository for search operations.
 *
 * FIX: PostgREST ilike uses SQL wildcards (%), NOT glob wildcards (*).
 *      Old:  title=ilike.*query*   → literal asterisk → no results
 *      New:  title=ilike.%25query%25  (URL-encoded %)
 */
@Singleton
public class SearchRepository {

    private final NovelDao novelDao;
    private final SupabaseDatabaseService databaseService;
    private final com.novelverse.app.data.local.preferences.UserPreferences userPreferences;
    private final ExecutorService executor;

    @Inject
    public SearchRepository(NovelDao novelDao,
                            SupabaseDatabaseService databaseService,
                            com.novelverse.app.data.local.preferences.UserPreferences userPreferences) {
        this.novelDao        = novelDao;
        this.databaseService = databaseService;
        this.userPreferences = userPreferences;
        this.executor        = Executors.newFixedThreadPool(2);
    }

    // ── Paged search (used by NovelPagingSource) ──────────────────────────

    public LiveData<PagingData<Novel>> searchNovels(String query) {
        return PagingLiveData.getLiveData(
            new Pager<>(new PagingConfig(20),
                () -> new NovelPagingSource(novelDao, databaseService, query))
        );
    }

    // ── One-shot search called from SearchViewModel ───────────────────────

    /**
     * Searches novels by title OR by genre (array contains).
     *
     * PostgREST wildcard for ilike is % (SQL wildcard), not *.
     * We URL-encode % as %25 since the filter string is appended to a URL.
     */
    public void search(String query, SearchCallback callback) {
        if (query == null || query.trim().isEmpty()) {
            callback.onResults(new ArrayList<>());
            return;
        }
        String token       = userPreferences.getAccessToken();
        String encoded     = query.trim().replace(" ", "%20"); // basic space encoding

        // Build OR filter: match by title OR by genre array-contains
        // PostgREST OR syntax:  or=(title.ilike.%25foo%25,genres.cs.{foo})
        String ilikePart   = "title.ilike.%25" + encoded + "%25";
        String genrePart   = "genres.cs.{" + query.trim() + "}";
        String filter      = "or=(" + ilikePart + "," + genrePart + ")"
                           + "&is_published=eq.true&deleted_at=is.null";

        databaseService.selectWhere("novels", "*", filter, "total_views.desc", token,
            new SupabaseDatabaseService.DatabaseCallback() {
                @Override public void onSuccess(String result) {
                    try {
                        com.google.gson.Gson gson = new com.google.gson.Gson();
                        com.google.gson.JsonArray arr =
                            gson.fromJson(result, com.google.gson.JsonArray.class);
                        List<Novel> novels = new ArrayList<>();
                        if (arr != null) {
                            for (com.google.gson.JsonElement el : arr) {
                                Novel n = gson.fromJson(el, Novel.class);
                                if (n != null) novels.add(n);
                            }
                        }
                        callback.onResults(novels);
                    } catch (Exception e) {
                        callback.onResults(new ArrayList<>());
                    }
                }
                @Override public void onError(String error) {
                    android.util.Log.e("SearchRepository", "search error: " + error);
                    callback.onResults(new ArrayList<>());
                }
            });
    }

    /** Overload with filter (genre/status) support. */
    public void search(String query, SearchFilterBottomSheet.SearchFilter filter,
                       SearchCallback callback) {
        if (filter == null) {
            search(query, callback);
            return;
        }
        String token       = userPreferences.getAccessToken();
        String encoded     = query != null ? query.trim().replace(" ", "%20") : "";
        StringBuilder sb   = new StringBuilder();

        if (!encoded.isEmpty()) {
            sb.append("or=(title.ilike.%25").append(encoded)
              .append("%25,genres.cs.{").append(query.trim()).append("})");
        }

        // Apply status filter (SearchFilter has no genre field; use status for completed/ongoing)
        if (filter.status != null && !"All".equals(filter.status)) {
            if (sb.length() > 0) sb.append("&");
            sb.append("status=eq.").append(filter.status.toLowerCase());
        }
        // Always limit to published, non-deleted
        if (sb.length() > 0) sb.append("&");
        sb.append("is_published=eq.true&deleted_at=is.null");

        databaseService.selectWhere("novels", "*", sb.toString(), "total_views.desc", token,
            new SupabaseDatabaseService.DatabaseCallback() {
                @Override public void onSuccess(String result) {
                    try {
                        com.google.gson.Gson gson = new com.google.gson.Gson();
                        com.google.gson.JsonArray arr =
                            gson.fromJson(result, com.google.gson.JsonArray.class);
                        List<Novel> novels = new ArrayList<>();
                        if (arr != null) {
                            for (com.google.gson.JsonElement el : arr) {
                                Novel n = gson.fromJson(el, Novel.class);
                                if (n != null) novels.add(n);
                            }
                        }
                        callback.onResults(novels);
                    } catch (Exception e) {
                        callback.onResults(new ArrayList<>());
                    }
                }
                @Override public void onError(String error) {
                    callback.onResults(new ArrayList<>());
                }
            });
    }

    public interface SearchCallback {
        void onResults(List<Novel> novels);
    }

    public LiveData<List<String>> getSearchSuggestions(String query) { return null; }
    public LiveData<List<String>> getRecentSearches()               { return null; }
    public void clearRecentSearches() { /* no-op */ }

    // ── Trending ──────────────────────────────────────────────────────────

    private List<TrendingItem> cachedTrending  = new ArrayList<>();
    private long               trendingFetched = 0;
    private static final long  TRENDING_TTL    = 3_600_000L;

    public void getTrendingSearches(TrendingCallback callback) {
        long now = System.currentTimeMillis();
        if (!cachedTrending.isEmpty() && now - trendingFetched < TRENDING_TTL) {
            callback.onResult(cachedTrending);
            return;
        }
        cachedTrending = new ArrayList<>();
        cachedTrending.add(new TrendingItem("Reincarnation Saga", 1, 1));
        cachedTrending.add(new TrendingItem("Magic Academy",      2, 0));
        cachedTrending.add(new TrendingItem("Moon Empress",       3, 1));
        cachedTrending.add(new TrendingItem("CEO Romance",        4, -1));
        cachedTrending.add(new TrendingItem("Sword Saint",        5, 0));
        trendingFetched = now;
        callback.onResult(cachedTrending);
    }

    public interface TrendingCallback { void onResult(List<TrendingItem> items); }
}

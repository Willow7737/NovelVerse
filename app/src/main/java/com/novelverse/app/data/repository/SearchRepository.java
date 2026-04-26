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
     * Searches novels by title OR author_name using the search_novels Postgres RPC.
     * Falls back to ilike PostgREST query if RPC fails.
     */
    public void search(String query, SearchCallback callback) {
        if (query == null || query.trim().isEmpty()) {
            callback.onResults(new ArrayList<>());
            return;
        }
        String token = userPreferences.getAccessToken();
        com.google.gson.JsonObject params = new com.google.gson.JsonObject();
        params.addProperty("p_query",  query.trim());
        params.addProperty("p_limit",  30);
        params.addProperty("p_offset", 0);

        databaseService.callRpc("search_novels", params, token,
            new SupabaseDatabaseService.DatabaseCallback() {
                @Override public void onSuccess(String result) {
                    callback.onResults(parseNovels(result));
                }
                @Override public void onError(String error) {
                    android.util.Log.w("SearchRepository", "RPC search failed, using ilike: " + error);
                    searchViaIlike(query, null, callback);
                }
            });
    }

    /** Overload with filter (genre/status) support. */
    public void search(String query, SearchFilterBottomSheet.SearchFilter filter,
                       SearchCallback callback) {
        if (filter == null) { search(query, callback); return; }

        String token = userPreferences.getAccessToken();
        com.google.gson.JsonObject params = new com.google.gson.JsonObject();
        params.addProperty("p_query",  query != null ? query.trim() : "");
        params.addProperty("p_limit",  30);
        params.addProperty("p_offset", 0);
        if (filter.status != null && !"All".equals(filter.status)) {
            params.addProperty("p_status", filter.status.toLowerCase());
        }

        databaseService.callRpc("search_novels", params, token,
            new SupabaseDatabaseService.DatabaseCallback() {
                @Override public void onSuccess(String result) {
                    callback.onResults(parseNovels(result));
                }
                @Override public void onError(String error) {
                    searchViaIlike(query, filter, callback);
                }
            });
    }

    /** Fallback ilike search used if RPC is unavailable. */
    private void searchViaIlike(String query, SearchFilterBottomSheet.SearchFilter filter,
                                SearchCallback callback) {
        String token   = userPreferences.getAccessToken();
        String encoded = query != null
            ? query.trim().replace(" ", "%20").replace("'", "%27") : "";

        StringBuilder sb = new StringBuilder();
        if (!encoded.isEmpty()) {
            sb.append("or=(title.ilike.%25").append(encoded)
              .append("%25,author_name.ilike.%25").append(encoded).append("%25)");
        }
        if (filter != null && filter.status != null && !"All".equals(filter.status)) {
            if (sb.length() > 0) sb.append("&");
            sb.append("status=eq.").append(filter.status.toLowerCase());
        }
        if (sb.length() > 0) sb.append("&");
        sb.append("is_published=eq.true&deleted_at=is.null");

        databaseService.selectWhere(
            "novels",
            "id,title,author_id,author_name,cover_image_url,total_views,"
                + "total_chapters,average_rating,genres,status,description,is_published",
            sb.toString(), "total_views.desc", token,
            new SupabaseDatabaseService.DatabaseCallback() {
                @Override public void onSuccess(String r) { callback.onResults(parseNovels(r)); }
                @Override public void onError(String e)   { callback.onResults(new ArrayList<>()); }
            });
    }

    private List<Novel> parseNovels(String json) {
        List<Novel> novels = new ArrayList<>();
        try {
            com.google.gson.JsonArray arr =
                new com.google.gson.Gson().fromJson(json, com.google.gson.JsonArray.class);
            if (arr != null) {
                for (com.google.gson.JsonElement el : arr) {
                    Novel n = new com.google.gson.Gson().fromJson(el, Novel.class);
                    if (n != null) novels.add(n);
                }
            }
        } catch (Exception e) {
            android.util.Log.e("SearchRepository", "parse error", e);
        }
        return novels;
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
    private static final long  TRENDING_TTL    = 3_600_000L; // 1 hour

    /**
     * Loads trending novels by total_views from Supabase.
     * Falls back to hardcoded list if the network call fails (e.g. offline).
     */
    public void getTrendingSearches(TrendingCallback callback) {
        long now = System.currentTimeMillis();
        if (!cachedTrending.isEmpty() && now - trendingFetched < TRENDING_TTL) {
            callback.onResult(cachedTrending);
            return;
        }

        String token = userPreferences.getAccessToken();
        String url   = databaseService.buildSelectUrl(
                "novels",
                "title,total_views",
                "is_published=eq.true&deleted_at=is.null&limit=8",
                "total_views.desc");

        databaseService.rawSelect(url, token, new SupabaseDatabaseService.DatabaseCallback() {
            @Override
            public void onSuccess(String result) {
                List<TrendingItem> items = new ArrayList<>();
                try {
                    com.google.gson.JsonArray arr =
                        new com.google.gson.Gson().fromJson(result, com.google.gson.JsonArray.class);
                    if (arr != null) {
                        for (int i = 0; i < arr.size(); i++) {
                            com.google.gson.JsonObject o = arr.get(i).getAsJsonObject();
                            String q     = o.has("title") ? o.get("title").getAsString() : "";
                            long   views = o.has("total_views") ? o.get("total_views").getAsLong() : 0;
                            if (!q.isEmpty()) {
                                items.add(new TrendingItem(q, i + 1, views > 0 ? 1 : 0));
                            }
                        }
                    }
                } catch (Exception ignored) {}

                if (items.isEmpty()) items = fallbackTrending();
                cachedTrending  = items;
                trendingFetched = System.currentTimeMillis();
                callback.onResult(items);
            }

            @Override
            public void onError(String error) {
                android.util.Log.w("SearchRepository", "Trending load failed: " + error);
                callback.onResult(fallbackTrending());
            }
        });
    }

    private List<TrendingItem> fallbackTrending() {
        List<TrendingItem> list = new ArrayList<>();
        list.add(new TrendingItem("Reincarnation Saga", 1, 1));
        list.add(new TrendingItem("Magic Academy",      2, 0));
        list.add(new TrendingItem("Moon Empress",       3, 1));
        list.add(new TrendingItem("CEO Romance",        4, -1));
        list.add(new TrendingItem("Sword Saint",        5, 0));
        return list;
    }

    public interface TrendingCallback { void onResult(List<TrendingItem> items); }
}

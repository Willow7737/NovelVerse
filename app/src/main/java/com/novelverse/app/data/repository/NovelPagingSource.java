package com.novelverse.app.data.repository;

import androidx.annotation.NonNull;
import androidx.paging.PagingState;
import androidx.paging.rxjava3.RxPagingSource;

import com.novelverse.app.data.local.dao.NovelDao;
import com.novelverse.app.data.remote.supabase.SupabaseDatabaseService;
import com.novelverse.app.domain.models.Novel;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.rxjava3.core.Single;

/**
 * PagingSource for novels - uses RxPagingSource for Java compatibility
 */
public class NovelPagingSource extends RxPagingSource<Integer, Novel> {

    private final NovelDao novelDao;
    private final SupabaseDatabaseService databaseService;
    private final String query;

    public NovelPagingSource(NovelDao novelDao, SupabaseDatabaseService databaseService, String query) {
        this.novelDao = novelDao;
        this.databaseService = databaseService;
        this.query = query;
    }

    @NonNull
    @Override
    public Single<LoadResult<Integer, Novel>> loadSingle(@NonNull LoadParams<Integer> params) {
        return Single.fromCallable(() -> {
            int page = params.getKey() != null ? params.getKey() : 0;
            int pageSize = params.getLoadSize();
            List<Novel> novels = new ArrayList<>();
            Integer prevKey = page > 0 ? page - 1 : null;
            Integer nextKey = novels.size() == pageSize ? page + 1 : null;
            return (LoadResult<Integer, Novel>) new LoadResult.Page<>(novels, prevKey, nextKey);
        });
    }

    @Override
    public Integer getRefreshKey(@NonNull PagingState<Integer, Novel> state) {
        Integer anchor = state.getAnchorPosition();
        if (anchor == null) return null;
        LoadResult.Page<Integer, Novel> page = state.closestPageToPosition(anchor);
        if (page == null) return null;
        if (page.getPrevKey() != null) return page.getPrevKey() + 1;
        if (page.getNextKey() != null) return page.getNextKey() - 1;
        return null;
    }
}

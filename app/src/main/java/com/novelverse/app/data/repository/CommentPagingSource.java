package com.novelverse.app.data.repository;

import androidx.annotation.NonNull;
import androidx.paging.PagingState;
import androidx.paging.rxjava3.RxPagingSource;

import com.novelverse.app.data.local.dao.CommentDao;
import com.novelverse.app.data.remote.supabase.SupabaseDatabaseService;
import com.novelverse.app.domain.models.Comment;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.rxjava3.core.Single;

public class CommentPagingSource extends RxPagingSource<Integer, Comment> {

    private final CommentDao commentDao;
    private final SupabaseDatabaseService databaseService;
    private final String novelId;
    private final String chapterId;

    public CommentPagingSource(CommentDao commentDao, SupabaseDatabaseService databaseService,
                               String novelId, String chapterId) {
        this.commentDao = commentDao;
        this.databaseService = databaseService;
        this.novelId = novelId;
        this.chapterId = chapterId;
    }

    @NonNull
    @Override
    public Single<LoadResult<Integer, Comment>> loadSingle(@NonNull LoadParams<Integer> params) {
        return Single.fromCallable(() -> {
            int page = params.getKey() != null ? params.getKey() : 0;
            int pageSize = params.getLoadSize();
            List<Comment> comments = new ArrayList<>();
            Integer prevKey = page > 0 ? page - 1 : null;
            Integer nextKey = comments.size() == pageSize ? page + 1 : null;
            return (LoadResult<Integer, Comment>) new LoadResult.Page<>(comments, prevKey, nextKey);
        });
    }

    @Override
    public Integer getRefreshKey(@NonNull PagingState<Integer, Comment> state) {
        Integer anchor = state.getAnchorPosition();
        if (anchor == null) return null;
        LoadResult.Page<Integer, Comment> page = state.closestPageToPosition(anchor);
        if (page == null) return null;
        if (page.getPrevKey() != null) return page.getPrevKey() + 1;
        if (page.getNextKey() != null) return page.getNextKey() - 1;
        return null;
    }
}

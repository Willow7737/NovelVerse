package com.novelverse.app.presentation.write;

import android.os.Handler;
import android.os.Looper;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.novelverse.app.data.repository.ChapterRepository;
import com.novelverse.app.data.repository.NovelRepository;
import com.novelverse.app.domain.models.Chapter;
import com.novelverse.app.domain.models.Novel;

import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class WriterViewModel extends ViewModel {

    private final NovelRepository   novelRepo;
    private final ChapterRepository chapterRepo;
    private final Handler           mainHandler = new Handler(Looper.getMainLooper());

    // ALL LiveData updates use postValue — safe from any thread
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<String>  error   = new MutableLiveData<>();
    private final MutableLiveData<String>  success = new MutableLiveData<>();

    private final MutableLiveData<List<Novel>>   myNovelsMutable  = new MutableLiveData<>();
    // Keyed chapter LiveData so chapters refresh when a chapter is saved
    private final java.util.Map<String, MutableLiveData<List<Chapter>>> chapterMap
            = new java.util.HashMap<>();

    private boolean initialLoadDone = false;

    @Inject
    public WriterViewModel(NovelRepository novelRepo, ChapterRepository chapterRepo) {
        this.novelRepo   = novelRepo;
        this.chapterRepo = chapterRepo;
    }

    public LiveData<Boolean> isLoading()  { return loading; }
    public LiveData<String>  getError()   { return error; }
    public LiveData<String>  getSuccess() { return success; }

    public LiveData<List<Novel>> getMyNovels() {
        if (!initialLoadDone) { initialLoadDone = true; doFetchNovels(); }
        return myNovelsMutable;
    }

    public void refreshMyNovels() { doFetchNovels(); }

    private void doFetchNovels() {
        // observeForever MUST be called on the main thread
        mainHandler.post(() -> {
            novelRepo.getMyNovels().observeForever(novels -> {
                if (novels != null) myNovelsMutable.postValue(novels);
            });
        });
    }

    public LiveData<Novel> getNovelById(String novelId) {
        return novelRepo.getNovelById(novelId);
    }

    /**
     * Returns a stable cached LiveData for chapters of a given novel.
     * Calling refreshChapters(novelId) posts fresh data into the same instance
     * so existing observers see the update without re-subscribing.
     */
    public LiveData<List<Chapter>> getChapters(String novelId) {
        if (!chapterMap.containsKey(novelId)) {
            MutableLiveData<List<Chapter>> ld = new MutableLiveData<>();
            chapterMap.put(novelId, ld);
            doFetchChapters(novelId);
        }
        return chapterMap.get(novelId);
    }

    public void refreshChapters(String novelId) {
        if (!chapterMap.containsKey(novelId)) {
            chapterMap.put(novelId, new MutableLiveData<>());
        }
        doFetchChapters(novelId);
    }

    private void doFetchChapters(String novelId) {
        mainHandler.post(() ->
            chapterRepo.getChapters(novelId).observeForever(chapters -> {
                MutableLiveData<List<Chapter>> ld = chapterMap.get(novelId);
                if (ld != null && chapters != null) ld.postValue(chapters);
            })
        );
    }

    public void createNovel(Novel novel, NovelRepository.Callback<Novel> cb) {
        loading.postValue(true);  // postValue — safe from any thread
        novelRepo.createNovel(novel, new NovelRepository.Callback<Novel>() {
            @Override public void onSuccess(Novel r) {
                loading.postValue(false);
                mainHandler.post(() -> refreshMyNovels());
                cb.onSuccess(r);
            }
            @Override public void onError(String e) {
                loading.postValue(false);
                error.postValue(e);
                cb.onError(e);
            }
        });
    }

    public void updateNovel(Novel novel, NovelRepository.Callback<Novel> cb) {
        loading.postValue(true);  // FIX: was setValue — crashes when called from OkHttp bg thread
        novelRepo.updateNovel(novel, new NovelRepository.Callback<Novel>() {
            @Override public void onSuccess(Novel r) {
                loading.postValue(false);
                mainHandler.post(() -> refreshMyNovels());
                cb.onSuccess(r);
            }
            @Override public void onError(String e) {
                loading.postValue(false);
                error.postValue(e);
                cb.onError(e);
            }
        });
    }

    public void deleteNovel(String id, NovelRepository.SimpleCallback cb) {
        novelRepo.deleteNovel(id, (success, err) -> {
            if (success) mainHandler.post(() -> refreshMyNovels());
            cb.onResult(success, err);
        });
    }

    public void uploadCover(String novelId, byte[] bytes, NovelRepository.Callback<String> cb) {
        novelRepo.uploadCover(novelId, bytes, cb);
    }

    public void createChapter(Chapter c, NovelRepository.Callback<Chapter> cb) {
        loading.postValue(true);
        chapterRepo.createChapter(c, new NovelRepository.Callback<Chapter>() {
            @Override public void onSuccess(Chapter r) {
                loading.postValue(false);
                // Refresh chapters so the new one appears immediately in the Chapters tab
                if (r.getNovelId() != null) mainHandler.post(() -> refreshChapters(r.getNovelId()));
                cb.onSuccess(r);
            }
            @Override public void onError(String e) {
                loading.postValue(false);
                error.postValue(e);
                cb.onError(e);
            }
        });
    }

    public void updateChapter(Chapter c, NovelRepository.Callback<Chapter> cb) {
        loading.postValue(true);
        chapterRepo.updateChapter(c, new NovelRepository.Callback<Chapter>() {
            @Override public void onSuccess(Chapter r) {
                loading.postValue(false);
                // Refresh chapters so publish status changes show immediately
                if (r.getNovelId() != null) mainHandler.post(() -> refreshChapters(r.getNovelId()));
                else if (c.getNovelId() != null) mainHandler.post(() -> refreshChapters(c.getNovelId()));
                cb.onSuccess(r);
            }
            @Override public void onError(String e) {
                loading.postValue(false);
                error.postValue(e);
                cb.onError(e);
            }
        });
    }

    public void deleteChapter(String id, NovelRepository.SimpleCallback cb) {
        chapterRepo.deleteChapter(id, cb);
    }
}

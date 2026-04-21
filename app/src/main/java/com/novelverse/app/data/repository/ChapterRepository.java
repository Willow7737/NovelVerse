package com.novelverse.app.data.repository;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.novelverse.app.data.local.dao.ChapterDao;
import com.novelverse.app.data.local.preferences.UserPreferences;
import com.novelverse.app.data.remote.supabase.SupabaseDatabaseService;
import com.novelverse.app.domain.models.Chapter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class ChapterRepository {

    private static final String TAG   = "ChapterRepository";
    private static final String TABLE = "chapters";

    private final SupabaseDatabaseService db;
    private final UserPreferences         prefs;
    private final Gson                    gson = new Gson();

    @Inject
    public ChapterRepository(ChapterDao chapterDao,
                             SupabaseDatabaseService db,
                             UserPreferences prefs) {
        this.db    = db;
        this.prefs = prefs;
    }

    public LiveData<List<Chapter>> getChapters(String novelId) {
        MutableLiveData<List<Chapter>> ld = new MutableLiveData<>();
        db.selectWhere(TABLE, "*",
                "novel_id=eq." + novelId + "&deleted_at=is.null",
                "chapter_number.asc", prefs.getAccessToken(),
                new SupabaseDatabaseService.DatabaseCallback() {
                    @Override public void onSuccess(String r) { ld.postValue(parseChapters(r)); }
                    @Override public void onError(String e)   {
                        Log.e(TAG, e); ld.postValue(new ArrayList<>());
                    }
                });
        return ld;
    }

    public LiveData<Chapter> getChapterById(String chapterId) {
        MutableLiveData<Chapter> ld = new MutableLiveData<>();
        db.selectById(TABLE, chapterId, prefs.getAccessToken(),
                new SupabaseDatabaseService.DatabaseCallback() {
                    @Override public void onSuccess(String r) {
                        try {
                            // PostgREST with Accept: application/vnd.pgrst.object+json returns a
                            // single JSON object, but guard against an array response just in case.
                            String trimmed = r.trim();
                            if (trimmed.startsWith("[")) {
                                com.google.gson.JsonArray arr = gson.fromJson(trimmed, com.google.gson.JsonArray.class);
                                if (arr != null && arr.size() > 0) {
                                    ld.postValue(gson.fromJson(arr.get(0), Chapter.class));
                                } else {
                                    ld.postValue(null);
                                }
                            } else {
                                ld.postValue(gson.fromJson(trimmed, Chapter.class));
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "getChapterById parse error: " + e.getMessage());
                            ld.postValue(null);
                        }
                    }
                    @Override public void onError(String e) {
                        Log.e(TAG, "getChapterById error: " + e);
                        ld.postValue(null);
                    }
                });
        return ld;
    }

    public void createChapter(Chapter chapter, NovelRepository.Callback<Chapter> cb) {
        String newId = UUID.randomUUID().toString();
        JsonObject body = new JsonObject();
        body.addProperty("id",             newId);
        body.addProperty("novel_id",       chapter.getNovelId());
        body.addProperty("chapter_number", chapter.getChapterNumber());
        body.addProperty("title",          chapter.getTitle() != null ? chapter.getTitle() : "Untitled");
        body.addProperty("content",        chapter.getContent() != null ? chapter.getContent() : "");
        body.addProperty("word_count",     chapter.getWordCount());
        body.addProperty("is_published",   chapter.isPublished());
        body.addProperty("is_free",        chapter.isFree());

        db.insert(TABLE, body, prefs.getAccessToken(),
                new SupabaseDatabaseService.DatabaseCallback() {
                    @Override public void onSuccess(String r) {
                        chapter.setId(newId);
                        cb.onSuccess(chapter);
                    }
                    @Override public void onError(String e) { cb.onError(e); }
                });
    }

    public void updateChapter(Chapter chapter, NovelRepository.Callback<Chapter> cb) {
        JsonObject body = new JsonObject();
        if (chapter.getTitle()   != null) body.addProperty("title",   chapter.getTitle());
        if (chapter.getContent() != null) body.addProperty("content", chapter.getContent());
        body.addProperty("word_count",   chapter.getWordCount());
        body.addProperty("is_published", chapter.isPublished());
        body.addProperty("is_free",      chapter.isFree());

        db.update(TABLE, chapter.getId(), body, prefs.getAccessToken(),
                new SupabaseDatabaseService.DatabaseCallback() {
                    @Override public void onSuccess(String r) { cb.onSuccess(chapter); }
                    @Override public void onError(String e)   { cb.onError(e); }
                });
    }

    public void deleteChapter(String chapterId, NovelRepository.SimpleCallback cb) {
        db.delete(TABLE, chapterId, prefs.getAccessToken(),
                new SupabaseDatabaseService.DatabaseCallback() {
                    @Override public void onSuccess(String r) { cb.onResult(true, null); }
                    @Override public void onError(String e)   { cb.onResult(false, e); }
                });
    }

    private List<Chapter> parseChapters(String json) {
        List<Chapter> list = new ArrayList<>();
        try {
            JsonArray arr = gson.fromJson(json, JsonArray.class);
            if (arr == null) return list;
            for (int i = 0; i < arr.size(); i++) {
                try { list.add(gson.fromJson(arr.get(i), Chapter.class)); }
                catch (Exception ignored) {}
            }
        } catch (Exception e) { Log.e(TAG, "parse: " + e.getMessage()); }
        return list;
    }
}

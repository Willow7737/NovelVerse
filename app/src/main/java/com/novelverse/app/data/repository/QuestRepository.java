package com.novelverse.app.data.repository;

import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.novelverse.app.data.remote.supabase.SupabaseDatabaseService;
import com.novelverse.app.domain.models.Quest;

import java.util.ArrayList;
import java.util.List;

public class QuestRepository {

    private static final String TAG = "QuestRepository";
    private static final String TABLE_QUESTS = "quests";
    private static final String TABLE_PROGRESS = "user_quest_progress";

    private final SupabaseDatabaseService db;
    private final Gson gson = new Gson();

    public interface QuestCallback {
        void onSuccess(List<Quest> quests);
        void onError(String error);
    }

    public interface ActionCallback {
        void onSuccess();
        void onError(String error);
    }

    public QuestRepository(SupabaseDatabaseService db) {
        this.db = db;
    }

    // ── Load quests with user progress for a given type ──────────────────

    public void loadQuestsWithProgress(String questType, String userId, String token, QuestCallback cb) {
        // Fetch quests left-join with user progress via PostgREST embedded select
        String filter = "quest_type=eq." + questType + "&is_active=eq.true&order=sort_order.asc";
        String select = "*,user_quest_progress!left(current_value,is_completed,reward_claimed,period_start)";
        // Filter user_quest_progress for this user
        String url = filter + "&user_quest_progress.user_id=eq." + userId;

        db.selectWhere(TABLE_QUESTS, select, filter, null, token,
                new SupabaseDatabaseService.DatabaseCallback() {
                    @Override
                    public void onSuccess(String result) {
                        try {
                            JsonArray arr = JsonParser.parseString(result).getAsJsonArray();
                            List<Quest> quests = new ArrayList<>();
                            for (int i = 0; i < arr.size(); i++) {
                                Quest q = parseQuest(arr.get(i).getAsJsonObject(), userId);
                                quests.add(q);
                            }
                            cb.onSuccess(quests);
                        } catch (Exception e) {
                            Log.e(TAG, "Parse error", e);
                            cb.onError(e.getMessage());
                        }
                    }

                    @Override
                    public void onError(String error) {
                        cb.onError(error);
                    }
                });
    }

    // ── Claim reward for a completed quest ────────────────────────────────

    public void claimReward(String questId, String userId, String token, ActionCallback cb) {
        // Find progress row and mark reward_claimed
        String filter = "quest_id=eq." + questId + "&user_id=eq." + userId + "&is_completed=eq.true&reward_claimed=eq.false";
        JsonObject update = new JsonObject();
        update.addProperty("reward_claimed", true);

        // PATCH user_quest_progress WHERE filter
        String url = db.buildFilterUrl(TABLE_PROGRESS, filter);
        db.patchWhere(TABLE_PROGRESS, filter, update, token,
                new SupabaseDatabaseService.DatabaseCallback() {
                    @Override
                    public void onSuccess(String result) {
                        cb.onSuccess();
                    }

                    @Override
                    public void onError(String error) {
                        cb.onError(error);
                    }
                });
    }

    // ── Upsert progress (called after user action) ───────────────────────

    public void upsertProgress(String questId, String userId, int newValue,
                                boolean isCompleted, String token, ActionCallback cb) {
        JsonObject data = new JsonObject();
        data.addProperty("user_id", userId);
        data.addProperty("quest_id", questId);
        data.addProperty("current_value", newValue);
        data.addProperty("is_completed", isCompleted);

        db.upsert(TABLE_PROGRESS, data, token,
                new SupabaseDatabaseService.DatabaseCallback() {
                    @Override
                    public void onSuccess(String result) {
                        cb.onSuccess();
                    }

                    @Override
                    public void onError(String error) {
                        cb.onError(error);
                    }
                });
    }

    // ── Parse ─────────────────────────────────────────────────────────────

    private Quest parseQuest(JsonObject obj, String userId) {
        Quest q = new Quest();
        q.setId(getString(obj, "id"));
        q.setTitle(getString(obj, "title"));
        q.setDescription(getString(obj, "description"));
        q.setTargetValue(getInt(obj, "target_value"));
        q.setRewardInk(getInt(obj, "reward_ink"));
        q.setRewardXp(getInt(obj, "reward_xp"));
        q.setRewardQuill(getInt(obj, "reward_quill"));
        q.setIconName(getString(obj, "icon_name"));
        q.setSortOrder(getInt(obj, "sort_order"));
        q.setActive(getBoolean(obj, "is_active"));

        String typeStr = getString(obj, "quest_type");
        switch (typeStr) {
            case "daily":    q.setQuestType(Quest.Type.DAILY);    break;
            case "weekly":   q.setQuestType(Quest.Type.WEEKLY);   break;
            default:         q.setQuestType(Quest.Type.ONE_TIME); break;
        }

        String ctStr = getString(obj, "challenge_type");
        switch (ctStr) {
            case "read_chapters":  q.setChallengeType(Quest.ChallengeType.READ_CHAPTERS);  break;
            case "watch_ad":       q.setChallengeType(Quest.ChallengeType.WATCH_AD);       break;
            case "read_minutes":   q.setChallengeType(Quest.ChallengeType.READ_MINUTES);   break;
            case "complete_novel": q.setChallengeType(Quest.ChallengeType.COMPLETE_NOVEL); break;
            case "add_to_library": q.setChallengeType(Quest.ChallengeType.ADD_TO_LIBRARY); break;
            case "leave_review":   q.setChallengeType(Quest.ChallengeType.LEAVE_REVIEW);   break;
            case "login":          q.setChallengeType(Quest.ChallengeType.LOGIN);           break;
            default:               q.setChallengeType(Quest.ChallengeType.SHARE);           break;
        }

        // Parse nested progress array (left join)
        if (obj.has("user_quest_progress") && !obj.get("user_quest_progress").isJsonNull()) {
            try {
                JsonArray progressArr = obj.getAsJsonArray("user_quest_progress");
                if (progressArr != null && progressArr.size() > 0) {
                    JsonObject progress = progressArr.get(0).getAsJsonObject();
                    q.setCurrentValue(getInt(progress, "current_value"));
                    q.setCompleted(getBoolean(progress, "is_completed"));
                    q.setRewardClaimed(getBoolean(progress, "reward_claimed"));
                }
            } catch (Exception ignored) {}
        }

        return q;
    }

    private String getString(JsonObject o, String key) {
        return (o.has(key) && !o.get(key).isJsonNull()) ? o.get(key).getAsString() : "";
    }

    private int getInt(JsonObject o, String key) {
        return (o.has(key) && !o.get(key).isJsonNull()) ? o.get(key).getAsInt() : 0;
    }

    private boolean getBoolean(JsonObject o, String key) {
        return (o.has(key) && !o.get(key).isJsonNull()) && o.get(key).getAsBoolean();
    }
}

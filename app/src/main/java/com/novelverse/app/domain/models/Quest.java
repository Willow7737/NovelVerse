package com.novelverse.app.domain.models;

public class Quest {

    public enum Type { DAILY, WEEKLY, ONE_TIME }
    public enum ChallengeType {
        READ_CHAPTERS, WATCH_AD, READ_MINUTES,
        COMPLETE_NOVEL, ADD_TO_LIBRARY, LEAVE_REVIEW, LOGIN, SHARE
    }

    private String id;
    private String title;
    private String description;
    private Type questType;
    private ChallengeType challengeType;
    private int targetValue;
    private int rewardInk;
    private int rewardXp;
    private int rewardQuill;
    private String iconName;
    private int sortOrder;
    private boolean isActive;

    // Progress (from user_quest_progress join)
    private int currentValue;
    private boolean isCompleted;
    private boolean rewardClaimed;

    public Quest() {}

    // ── Getters ──────────────────────────────────────────────────────────

    public String getId()                   { return id; }
    public String getTitle()                { return title; }
    public String getDescription()          { return description; }
    public Type getQuestType()              { return questType; }
    public ChallengeType getChallengeType() { return challengeType; }
    public int getTargetValue()             { return targetValue; }
    public int getRewardInk()               { return rewardInk; }
    public int getRewardXp()                { return rewardXp; }
    public int getRewardQuill()             { return rewardQuill; }
    public String getIconName()             { return iconName; }
    public int getSortOrder()               { return sortOrder; }
    public boolean isActive()               { return isActive; }
    public int getCurrentValue()            { return currentValue; }
    public boolean isCompleted()            { return isCompleted; }
    public boolean isRewardClaimed()        { return rewardClaimed; }

    public float getProgressFraction() {
        if (targetValue == 0) return 0f;
        return Math.min(1f, (float) currentValue / targetValue);
    }

    public int getProgressPercent() {
        return Math.round(getProgressFraction() * 100f);
    }

    // ── Setters ──────────────────────────────────────────────────────────

    public void setId(String id)                             { this.id = id; }
    public void setTitle(String title)                       { this.title = title; }
    public void setDescription(String description)           { this.description = description; }
    public void setQuestType(Type questType)                 { this.questType = questType; }
    public void setChallengeType(ChallengeType ct)           { this.challengeType = ct; }
    public void setTargetValue(int targetValue)              { this.targetValue = targetValue; }
    public void setRewardInk(int rewardInk)                  { this.rewardInk = rewardInk; }
    public void setRewardXp(int rewardXp)                    { this.rewardXp = rewardXp; }
    public void setRewardQuill(int rewardQuill)              { this.rewardQuill = rewardQuill; }
    public void setIconName(String iconName)                 { this.iconName = iconName; }
    public void setSortOrder(int sortOrder)                  { this.sortOrder = sortOrder; }
    public void setActive(boolean active)                    { isActive = active; }
    public void setCurrentValue(int currentValue)            { this.currentValue = currentValue; }
    public void setCompleted(boolean completed)              { isCompleted = completed; }
    public void setRewardClaimed(boolean rewardClaimed)      { this.rewardClaimed = rewardClaimed; }
}

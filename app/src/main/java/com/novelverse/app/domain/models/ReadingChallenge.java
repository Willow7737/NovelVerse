package com.novelverse.app.domain.models;

import java.util.Date;

public class ReadingChallenge {
    private String id, title, description;
    private int targetCount, currentCount, rewardPoints;
    private String badgeDrawableId;
    private Date expiresAt;
    private boolean isCompleted;

    public ReadingChallenge() {}
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String t) { this.title = t; }
    public String getDescription() { return description; }
    public void setDescription(String d) { this.description = d; }
    public int getTargetCount() { return targetCount; }
    public void setTargetCount(int t) { this.targetCount = t; }
    public int getCurrentCount() { return currentCount; }
    public void setCurrentCount(int c) { this.currentCount = c; }
    public int getRewardPoints() { return rewardPoints; }
    public void setRewardPoints(int r) { this.rewardPoints = r; }
    public String getBadgeDrawableId() { return badgeDrawableId; }
    public void setBadgeDrawableId(String b) { this.badgeDrawableId = b; }
    public Date getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Date d) { this.expiresAt = d; }
    public boolean isCompleted() { return isCompleted; }
    public void setCompleted(boolean c) { this.isCompleted = c; }
    public float getProgressFraction() {
        if (targetCount == 0) return 0f;
        return Math.min(1f, (float) currentCount / targetCount);
    }
}

package com.novelverse.app.domain.models;

import java.util.Date;

/**
 * Domain model for Reading Progress
 */
public class ReadingProgress {

    private String id;
    private String userId;
    private String novelId;
    private String chapterId;
    private int scrollPosition;
    private double progressPercentage;
    private boolean isCompleted;
    private Date completedAt;
    private Date lastReadAt;
    private int totalReadingTime;
    private String deviceInfo;

    public ReadingProgress() {
        this.scrollPosition = 0;
        this.progressPercentage = 0.0;
        this.isCompleted = false;
        this.totalReadingTime = 0;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getNovelId() { return novelId; }
    public void setNovelId(String novelId) { this.novelId = novelId; }

    public String getChapterId() { return chapterId; }
    public void setChapterId(String chapterId) { this.chapterId = chapterId; }

    public int getScrollPosition() { return scrollPosition; }
    public void setScrollPosition(int scrollPosition) { this.scrollPosition = scrollPosition; }

    public double getProgressPercentage() { return progressPercentage; }
    public void setProgressPercentage(double progressPercentage) { this.progressPercentage = progressPercentage; }

    public boolean isCompleted() { return isCompleted; }
    public void setCompleted(boolean completed) { isCompleted = completed; }

    public Date getCompletedAt() { return completedAt; }
    public void setCompletedAt(Date completedAt) { this.completedAt = completedAt; }

    public Date getLastReadAt() { return lastReadAt; }
    public void setLastReadAt(Date lastReadAt) { this.lastReadAt = lastReadAt; }

    public int getTotalReadingTime() { return totalReadingTime; }
    public void setTotalReadingTime(int totalReadingTime) { this.totalReadingTime = totalReadingTime; }

    public String getDeviceInfo() { return deviceInfo; }
    public void setDeviceInfo(String deviceInfo) { this.deviceInfo = deviceInfo; }

    /**
     * Get formatted reading time
     */
    public String getFormattedReadingTime() {
        int hours = totalReadingTime / 3600;
        int minutes = (totalReadingTime % 3600) / 60;
        
        if (hours > 0) {
            return hours + "h " + minutes + "m";
        } else {
            return minutes + "m";
        }
    }

    /**
     * Get formatted progress percentage
     */
    public String getFormattedProgress() {
        return (int) progressPercentage + "%";
    }
}

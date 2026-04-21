package com.novelverse.app.data.local.entities;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import java.util.Date;

/**
 * Room entity for Reading Progress (offline tracking)
 */
@Entity(
    tableName = "reading_progress",
    foreignKeys = {
        @ForeignKey(
            entity = NovelEntity.class,
            parentColumns = "id",
            childColumns = "novel_id",
            onDelete = ForeignKey.CASCADE
        ),
        @ForeignKey(
            entity = ChapterEntity.class,
            parentColumns = "id",
            childColumns = "chapter_id",
            onDelete = ForeignKey.CASCADE
        )
    },
    indices = {
        @Index(value = "user_id"),
        @Index(value = "novel_id"),
        @Index(value = "chapter_id"),
        @Index(value = {"user_id", "novel_id"}, unique = true),
        @Index(value = "last_read_at")
    }
)
public class ReadingProgressEntity {

    @PrimaryKey
    @ColumnInfo(name = "id")
    private String id;

    @ColumnInfo(name = "user_id")
    private String userId;

    @ColumnInfo(name = "novel_id")
    private String novelId;

    @ColumnInfo(name = "chapter_id")
    private String chapterId;

    @ColumnInfo(name = "scroll_position")
    private Integer scrollPosition;

    @ColumnInfo(name = "progress_percentage")
    private Double progressPercentage;

    @ColumnInfo(name = "is_completed")
    private Boolean isCompleted;

    @ColumnInfo(name = "completed_at")
    private Date completedAt;

    @ColumnInfo(name = "last_read_at")
    private Date lastReadAt;

    @ColumnInfo(name = "total_reading_time")
    private Integer totalReadingTime; // in seconds

    @ColumnInfo(name = "device_info")
    private String deviceInfo;

    // Sync tracking
    @ColumnInfo(name = "synced_at")
    private Date syncedAt;

    @ColumnInfo(name = "is_dirty")
    private Boolean isDirty;

    @ColumnInfo(name = "needs_sync")
    private Boolean needsSync;

    public ReadingProgressEntity() {
        this.scrollPosition = 0;
        this.progressPercentage = 0.0;
        this.isCompleted = false;
        this.totalReadingTime = 0;
        this.isDirty = false;
        this.needsSync = false;
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

    public Integer getScrollPosition() { return scrollPosition; }
    public void setScrollPosition(Integer scrollPosition) { this.scrollPosition = scrollPosition; }

    public Double getProgressPercentage() { return progressPercentage; }
    public void setProgressPercentage(Double progressPercentage) { this.progressPercentage = progressPercentage; }

    public Boolean getIsCompleted() { return isCompleted; }
    public void setIsCompleted(Boolean isCompleted) { this.isCompleted = isCompleted; }

    public Date getCompletedAt() { return completedAt; }
    public void setCompletedAt(Date completedAt) { this.completedAt = completedAt; }

    public Date getLastReadAt() { return lastReadAt; }
    public void setLastReadAt(Date lastReadAt) { this.lastReadAt = lastReadAt; }

    public Integer getTotalReadingTime() { return totalReadingTime; }
    public void setTotalReadingTime(Integer totalReadingTime) { this.totalReadingTime = totalReadingTime; }

    public String getDeviceInfo() { return deviceInfo; }
    public void setDeviceInfo(String deviceInfo) { this.deviceInfo = deviceInfo; }

    public Date getSyncedAt() { return syncedAt; }
    public void setSyncedAt(Date syncedAt) { this.syncedAt = syncedAt; }

    public Boolean getIsDirty() { return isDirty; }
    public void setIsDirty(Boolean isDirty) { this.isDirty = isDirty; }

    public Boolean getNeedsSync() { return needsSync; }
    public void setNeedsSync(Boolean needsSync) { this.needsSync = needsSync; }
}

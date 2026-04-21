package com.novelverse.app.data.local.entities;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import java.util.Date;

/**
 * Room entity for Chapter data (offline cache)
 */
@Entity(
    tableName = "chapters",
    foreignKeys = @ForeignKey(
        entity = NovelEntity.class,
        parentColumns = "id",
        childColumns = "novel_id",
        onDelete = ForeignKey.CASCADE
    ),
    indices = {
        @Index(value = "novel_id"),
        @Index(value = {"novel_id", "chapter_number"}, unique = true),
        @Index(value = "is_published"),
        @Index(value = "synced_at")
    }
)
public class ChapterEntity {

    @PrimaryKey
    @ColumnInfo(name = "id")
    private String id;

    @ColumnInfo(name = "novel_id")
    private String novelId;

    @ColumnInfo(name = "chapter_number")
    private Integer chapterNumber;

    @ColumnInfo(name = "title")
    private String title;

    @ColumnInfo(name = "slug")
    private String slug;

    @ColumnInfo(name = "content")
    private String content;

    @ColumnInfo(name = "word_count")
    private Integer wordCount;

    @ColumnInfo(name = "is_published")
    private Boolean isPublished;

    @ColumnInfo(name = "is_free")
    private Boolean isFree;

    @ColumnInfo(name = "price")
    private Double price;

    @ColumnInfo(name = "points_cost")
    private Integer pointsCost;

    @ColumnInfo(name = "published_at")
    private Date publishedAt;

    @ColumnInfo(name = "created_at")
    private Date createdAt;

    @ColumnInfo(name = "updated_at")
    private Date updatedAt;

    @ColumnInfo(name = "total_views")
    private Long totalViews;

    @ColumnInfo(name = "total_comments")
    private Integer totalComments;

    @ColumnInfo(name = "average_read_time")
    private Integer averageReadTime; // in seconds

    // Local fields
    @ColumnInfo(name = "is_downloaded")
    private Boolean isDownloaded;

    @ColumnInfo(name = "is_locked")
    private Boolean isLocked; // For purchased content tracking

    @ColumnInfo(name = "synced_at")
    private Date syncedAt;

    @ColumnInfo(name = "is_dirty")
    private Boolean isDirty;

    public ChapterEntity() {
        this.isDownloaded = false;
        this.isLocked = true;
        this.isDirty = false;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getNovelId() { return novelId; }
    public void setNovelId(String novelId) { this.novelId = novelId; }

    public Integer getChapterNumber() { return chapterNumber; }
    public void setChapterNumber(Integer chapterNumber) { this.chapterNumber = chapterNumber; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public Integer getWordCount() { return wordCount; }
    public void setWordCount(Integer wordCount) { this.wordCount = wordCount; }

    public Boolean getIsPublished() { return isPublished; }
    public void setIsPublished(Boolean isPublished) { this.isPublished = isPublished; }

    public Boolean getIsFree() { return isFree; }
    public void setIsFree(Boolean isFree) { this.isFree = isFree; }

    public Double getPrice() { return price; }
    public void setPrice(Double price) { this.price = price; }

    public Integer getPointsCost() { return pointsCost; }
    public void setPointsCost(Integer pointsCost) { this.pointsCost = pointsCost; }

    public Date getPublishedAt() { return publishedAt; }
    public void setPublishedAt(Date publishedAt) { this.publishedAt = publishedAt; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

    public Date getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }

    public Long getTotalViews() { return totalViews; }
    public void setTotalViews(Long totalViews) { this.totalViews = totalViews; }

    public Integer getTotalComments() { return totalComments; }
    public void setTotalComments(Integer totalComments) { this.totalComments = totalComments; }

    public Integer getAverageReadTime() { return averageReadTime; }
    public void setAverageReadTime(Integer averageReadTime) { this.averageReadTime = averageReadTime; }

    public Boolean getIsDownloaded() { return isDownloaded; }
    public void setIsDownloaded(Boolean isDownloaded) { this.isDownloaded = isDownloaded; }

    public Boolean getIsLocked() { return isLocked; }
    public void setIsLocked(Boolean isLocked) { this.isLocked = isLocked; }

    public Date getSyncedAt() { return syncedAt; }
    public void setSyncedAt(Date syncedAt) { this.syncedAt = syncedAt; }

    public Boolean getIsDirty() { return isDirty; }
    public void setIsDirty(Boolean isDirty) { this.isDirty = isDirty; }
}

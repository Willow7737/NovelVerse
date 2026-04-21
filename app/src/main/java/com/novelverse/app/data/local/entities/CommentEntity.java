package com.novelverse.app.data.local.entities;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import java.util.Date;

/**
 * Room entity for Comments (offline cache)
 */
@Entity(
    tableName = "comments",
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
        ),
        @ForeignKey(
            entity = CommentEntity.class,
            parentColumns = "id",
            childColumns = "parent_id",
            onDelete = ForeignKey.CASCADE
        )
    },
    indices = {
        @Index(value = "user_id"),
        @Index(value = "novel_id"),
        @Index(value = "chapter_id"),
        @Index(value = "parent_id"),
        @Index(value = "created_at")
    }
)
public class CommentEntity {

    @PrimaryKey
    @ColumnInfo(name = "id")
    private String id;

    @ColumnInfo(name = "user_id")
    private String userId;

    @ColumnInfo(name = "username")
    private String username;

    @ColumnInfo(name = "user_avatar_url")
    private String userAvatarUrl;

    @ColumnInfo(name = "novel_id")
    private String novelId;

    @ColumnInfo(name = "chapter_id")
    private String chapterId;

    @ColumnInfo(name = "parent_id")
    private String parentId;

    @ColumnInfo(name = "content")
    private String content;

    @ColumnInfo(name = "likes_count")
    private Integer likesCount;

    @ColumnInfo(name = "dislikes_count")
    private Integer dislikesCount;

    @ColumnInfo(name = "replies_count")
    private Integer repliesCount;

    @ColumnInfo(name = "is_edited")
    private Boolean isEdited;

    @ColumnInfo(name = "is_deleted")
    private Boolean isDeleted;

    @ColumnInfo(name = "is_spoiler")
    private Boolean isSpoiler;

    @ColumnInfo(name = "created_at")
    private Date createdAt;

    @ColumnInfo(name = "updated_at")
    private Date updatedAt;

    // Sync tracking
    @ColumnInfo(name = "synced_at")
    private Date syncedAt;

    @ColumnInfo(name = "is_dirty")
    private Boolean isDirty;

    // Local tracking
    @ColumnInfo(name = "is_liked_by_me")
    private Boolean isLikedByMe;

    @ColumnInfo(name = "is_disliked_by_me")
    private Boolean isDislikedByMe;

    public CommentEntity() {
        this.likesCount = 0;
        this.dislikesCount = 0;
        this.repliesCount = 0;
        this.isEdited = false;
        this.isDeleted = false;
        this.isSpoiler = false;
        this.isDirty = false;
        this.isLikedByMe = false;
        this.isDislikedByMe = false;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getUserAvatarUrl() { return userAvatarUrl; }
    public void setUserAvatarUrl(String userAvatarUrl) { this.userAvatarUrl = userAvatarUrl; }

    public String getNovelId() { return novelId; }
    public void setNovelId(String novelId) { this.novelId = novelId; }

    public String getChapterId() { return chapterId; }
    public void setChapterId(String chapterId) { this.chapterId = chapterId; }

    public String getParentId() { return parentId; }
    public void setParentId(String parentId) { this.parentId = parentId; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public Integer getLikesCount() { return likesCount; }
    public void setLikesCount(Integer likesCount) { this.likesCount = likesCount; }

    public Integer getDislikesCount() { return dislikesCount; }
    public void setDislikesCount(Integer dislikesCount) { this.dislikesCount = dislikesCount; }

    public Integer getRepliesCount() { return repliesCount; }
    public void setRepliesCount(Integer repliesCount) { this.repliesCount = repliesCount; }

    public Boolean getIsEdited() { return isEdited; }
    public void setIsEdited(Boolean isEdited) { this.isEdited = isEdited; }

    public Boolean getIsDeleted() { return isDeleted; }
    public void setIsDeleted(Boolean isDeleted) { this.isDeleted = isDeleted; }

    public Boolean getIsSpoiler() { return isSpoiler; }
    public void setIsSpoiler(Boolean isSpoiler) { this.isSpoiler = isSpoiler; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

    public Date getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }

    public Date getSyncedAt() { return syncedAt; }
    public void setSyncedAt(Date syncedAt) { this.syncedAt = syncedAt; }

    public Boolean getIsDirty() { return isDirty; }
    public void setIsDirty(Boolean isDirty) { this.isDirty = isDirty; }

    public Boolean getIsLikedByMe() { return isLikedByMe; }
    public void setIsLikedByMe(Boolean isLikedByMe) { this.isLikedByMe = isLikedByMe; }

    public Boolean getIsDislikedByMe() { return isDislikedByMe; }
    public void setIsDislikedByMe(Boolean isDislikedByMe) { this.isDislikedByMe = isDislikedByMe; }
}

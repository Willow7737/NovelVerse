package com.novelverse.app.domain.models;

import java.util.Date;

/**
 * Domain model for Comment
 */
public class Comment {

    private String id;
    private String userId;
    private String username;
    private String userAvatarUrl;
    private String novelId;
    private String chapterId;
    private String parentId;
    private String content;
    private int likesCount;
    private int dislikesCount;
    private int repliesCount;
    private boolean isEdited;
    private boolean isDeleted;
    private boolean isSpoiler;
    private Date createdAt;
    private Date updatedAt;

    // Local fields
    private boolean isLikedByMe;
    private boolean isDislikedByMe;

    public Comment() {
        this.likesCount = 0;
        this.dislikesCount = 0;
        this.repliesCount = 0;
        this.isEdited = false;
        this.isDeleted = false;
        this.isSpoiler = false;
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

    public int getLikesCount() { return likesCount; }
    public void setLikesCount(int likesCount) { this.likesCount = likesCount; }

    public int getDislikesCount() { return dislikesCount; }
    public void setDislikesCount(int dislikesCount) { this.dislikesCount = dislikesCount; }

    public int getRepliesCount() { return repliesCount; }
    public void setRepliesCount(int repliesCount) { this.repliesCount = repliesCount; }

    public boolean isEdited() { return isEdited; }
    public void setEdited(boolean edited) { isEdited = edited; }

    public boolean isDeleted() { return isDeleted; }
    public void setDeleted(boolean deleted) { isDeleted = deleted; }

    public boolean isSpoiler() { return isSpoiler; }
    public void setSpoiler(boolean spoiler) { isSpoiler = spoiler; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

    public Date getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }

    public boolean isLikedByMe() { return isLikedByMe; }
    public void setLikedByMe(boolean likedByMe) { isLikedByMe = likedByMe; }

    public boolean isDislikedByMe() { return isDislikedByMe; }
    public void setDislikedByMe(boolean dislikedByMe) { isDislikedByMe = dislikedByMe; }

    /**
     * Check if this is a reply
     */
    public boolean isReply() {
        return parentId != null && !parentId.isEmpty();
    }
}

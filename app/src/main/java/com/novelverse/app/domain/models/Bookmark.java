package com.novelverse.app.domain.models;

import java.util.Date;

/**
 * Domain model for Bookmark
 */
public class Bookmark {

    private String id;
    private String userId;
    private String novelId;
    private String chapterId;
    private int position;
    private String note;
    private boolean isFavorite;
    private Date createdAt;
    private Date updatedAt;

    public Bookmark() {
        this.position = 0;
        this.isFavorite = false;
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

    public int getPosition() { return position; }
    public void setPosition(int position) { this.position = position; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    public boolean isFavorite() { return isFavorite; }
    public void setFavorite(boolean favorite) { isFavorite = favorite; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

    public Date getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }
}

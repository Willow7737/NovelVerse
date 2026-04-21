package com.novelverse.app.data.local.entities;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import java.util.Date;

/**
 * Room entity for Bookmarks
 */
@Entity(
    tableName = "bookmarks",
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
        @Index(value = {"user_id", "novel_id", "chapter_id", "position"}, unique = true),
        @Index(value = "created_at")
    }
)
public class BookmarkEntity {

    @PrimaryKey
    @ColumnInfo(name = "id")
    private String id;

    @ColumnInfo(name = "user_id")
    private String userId;

    @ColumnInfo(name = "novel_id")
    private String novelId;

    @ColumnInfo(name = "chapter_id")
    private String chapterId;

    @ColumnInfo(name = "position")
    private Integer position;

    @ColumnInfo(name = "note")
    private String note;

    @ColumnInfo(name = "is_favorite")
    private Boolean isFavorite;

    @ColumnInfo(name = "created_at")
    private Date createdAt;

    @ColumnInfo(name = "updated_at")
    private Date updatedAt;

    // Sync tracking
    @ColumnInfo(name = "synced_at")
    private Date syncedAt;

    @ColumnInfo(name = "is_dirty")
    private Boolean isDirty;

    // Task 38: Paragraph annotation fields
    @ColumnInfo(name = "highlighted_text")
    private String highlightedText;

    @ColumnInfo(name = "note_text")
    private String noteText;

    @ColumnInfo(name = "paragraph_index")
    private Integer paragraphIndex;

    public BookmarkEntity() {
        this.position = 0;
        this.isFavorite = false;
        this.isDirty = false;
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

    public Integer getPosition() { return position; }
    public void setPosition(Integer position) { this.position = position; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    public Boolean getIsFavorite() { return isFavorite; }
    public void setIsFavorite(Boolean isFavorite) { this.isFavorite = isFavorite; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

    public Date getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }

    public Date getSyncedAt() { return syncedAt; }
    public void setSyncedAt(Date syncedAt) { this.syncedAt = syncedAt; }

    public Boolean getIsDirty() { return isDirty; }
    public void setIsDirty(Boolean isDirty) { this.isDirty = isDirty; }

    public String getHighlightedText() { return highlightedText; }
    public void setHighlightedText(String t) { this.highlightedText = t; }
    public String getNoteText() { return noteText; }
    public void setNoteText(String t) { this.noteText = t; }
    public Integer getParagraphIndex() { return paragraphIndex; }
    public void setParagraphIndex(Integer i) { this.paragraphIndex = i; }

}

package com.novelverse.app.domain.models;

import com.google.gson.annotations.SerializedName;
import java.util.Date;

public class Chapter {
    @SerializedName("id")           private String id;
    @SerializedName("novel_id")     private String novelId;
    @SerializedName("chapter_number") private int chapterNumber;
    @SerializedName("title")        private String title;
    @SerializedName("slug")         private String slug;
    @SerializedName("content")      private String content;
    @SerializedName("word_count")   private int wordCount;
    @SerializedName("is_published") private boolean isPublished;
    @SerializedName("is_free")      private boolean isFree;
    @SerializedName("price")        private double price;
    @SerializedName("points_cost")  private int pointsCost;
    @SerializedName("published_at") private Date publishedAt;
    @SerializedName("created_at")   private Date createdAt;
    @SerializedName("updated_at")   private Date updatedAt;
    @SerializedName("total_views")  private long totalViews;
    @SerializedName("total_comments") private int totalComments;
    @SerializedName("average_read_time") private int averageReadTime;

    private transient boolean isDownloaded;
    private transient boolean isLocked;

    public Chapter() {
        this.wordCount = 0; this.isPublished = false; this.isFree = true;
        this.totalViews = 0; this.totalComments = 0;
        this.isDownloaded = false; this.isLocked = true;
    }

    public String getId()                 { return id; }
    public void   setId(String v)         { this.id = v; }
    public String getNovelId()            { return novelId; }
    public void   setNovelId(String v)    { this.novelId = v; }
    public int    getChapterNumber()      { return chapterNumber; }
    public void   setChapterNumber(int v) { this.chapterNumber = v; }
    public String getTitle()              { return title; }
    public void   setTitle(String v)      { this.title = v; }
    public String getSlug()               { return slug; }
    public void   setSlug(String v)       { this.slug = v; }
    public String getContent()            { return content; }
    public void   setContent(String v)    { this.content = v; }
    public int    getWordCount()          { return wordCount; }
    public void   setWordCount(int v)     { this.wordCount = v; }
    public boolean isPublished()          { return isPublished; }
    public void    setPublished(boolean v){ this.isPublished = v; }
    public boolean isFree()               { return isFree; }
    public void    setFree(boolean v)     { this.isFree = v; }
    public double  getPrice()             { return price; }
    public void    setPrice(double v)     { this.price = v; }
    public int     getPointsCost()        { return pointsCost; }
    public void    setPointsCost(int v)   { this.pointsCost = v; }
    public Date    getPublishedAt()       { return publishedAt; }
    public void    setPublishedAt(Date v) { this.publishedAt = v; }
    public Date    getCreatedAt()         { return createdAt; }
    public void    setCreatedAt(Date v)   { this.createdAt = v; }
    public Date    getUpdatedAt()         { return updatedAt; }
    public void    setUpdatedAt(Date v)   { this.updatedAt = v; }
    public long    getTotalViews()        { return totalViews; }
    public void    setTotalViews(long v)  { this.totalViews = v; }
    public int     getTotalComments()     { return totalComments; }
    public void    setTotalComments(int v){ this.totalComments = v; }
    public int     getAverageReadTime()   { return averageReadTime; }
    public void    setAverageReadTime(int v){ this.averageReadTime = v; }
    public boolean isDownloaded()         { return isDownloaded; }
    public void    setDownloaded(boolean v){ this.isDownloaded = v; }
    public boolean isLocked()             { return isLocked; }
    public void    setLocked(boolean v)   { this.isLocked = v; }
    public boolean isPremium()            { return !isFree; }
    public boolean isUnlocked()           { return !isLocked; }
    public String  getFormattedWordCount() { return wordCount + " words"; }
}

package com.novelverse.app.domain.models;

import com.google.gson.annotations.SerializedName;

import java.util.Date;
import java.util.List;

/**
 * Domain model for Novel.
 *
 * CRITICAL FIX: Added @SerializedName on every snake_case field so Gson
 * correctly maps Supabase PostgREST JSON responses (e.g. author_id → authorId).
 * Without these, all fields returned from the DB silently stayed null.
 */
public class Novel {

    @SerializedName("id")
    private String id;

    @SerializedName("author_id")
    private String authorId;

    @SerializedName("title")
    private String title;

    @SerializedName("slug")
    private String slug;

    @SerializedName("description")
    private String description;

    @SerializedName("cover_image_url")
    private String coverImageUrl;

    @SerializedName("banner_image_url")
    private String bannerImageUrl;

    @SerializedName("status")
    private String status;

    @SerializedName("visibility")
    private String visibility;

    @SerializedName("price_type")
    private String priceType;

    @SerializedName("price")
    private double price;

    @SerializedName("points_cost")
    private int pointsCost;

    @SerializedName("age_rating")
    private String ageRating;

    @SerializedName("content_warnings")
    private List<String> contentWarnings;

    @SerializedName("license_type")
    private String licenseType;

    @SerializedName("language")
    private String language;

    @SerializedName("total_chapters")
    private int totalChapters;

    @SerializedName("total_words")
    private long totalWords;

    @SerializedName("total_views")
    private long totalViews;

    @SerializedName("total_likes")
    private int totalLikes;

    @SerializedName("total_bookmarks")
    private int totalBookmarks;

    @SerializedName("total_comments")
    private int totalComments;

    @SerializedName("average_rating")
    private double averageRating;

    @SerializedName("rating_count")
    private int ratingCount;

    @SerializedName("is_published")
    private boolean isPublished;

    @SerializedName("published_at")
    private Date publishedAt;

    @SerializedName("last_updated_at")
    private Date lastUpdatedAt;

    @SerializedName("is_featured")
    private boolean isFeatured;

    @SerializedName("tags")
    private List<String> tags;

    @SerializedName("genres")
    private List<String> genres;

    // Joined from profiles via select=*,profiles(...)
    @SerializedName("author_username")
    private String authorUsername;

    @SerializedName("author_display_name")
    private String authorDisplayName;

    @SerializedName("author_avatar_url")
    private String authorAvatarUrl;

    @SerializedName("author_is_verified")
    private boolean isAuthorVerified;

    // Local-only flags — not from DB
    private transient boolean isDownloaded;
    private transient boolean isInLibrary;
    private transient boolean isPurchased;
    // Reading progress — populated when fetched from user_library or reading_progress
    private transient double  readingProgressPercent;   // 0.0 – 100.0
    private transient int     currentChapterNumber;     // 1-based chapter the user is on
    private transient String  currentChapterId;

    public Novel() {
        this.totalChapters = 0;
        this.totalWords    = 0;
        this.totalViews    = 0;
        this.totalLikes    = 0;
        this.totalBookmarks = 0;
        this.totalComments  = 0;
        this.averageRating  = 0.0;
        this.ratingCount    = 0;
        this.isPublished    = false;
        this.isFeatured     = false;
    }

    // ── Getters & Setters ─────────────────────────────────────────────────────

    public String getId()                           { return id; }
    public void   setId(String id)                  { this.id = id; }

    public String getAuthorId()                     { return authorId; }
    public void   setAuthorId(String v)             { this.authorId = v; }

    public String getTitle()                        { return title; }
    public void   setTitle(String v)                { this.title = v; }

    public String getSlug()                         { return slug; }
    public void   setSlug(String v)                 { this.slug = v; }

    public String getDescription()                  { return description; }
    public void   setDescription(String v)          { this.description = v; }

    public String getCoverImageUrl()                { return coverImageUrl; }
    public void   setCoverImageUrl(String v)        { this.coverImageUrl = v; }

    public String getBannerImageUrl()               { return bannerImageUrl; }
    public void   setBannerImageUrl(String v)       { this.bannerImageUrl = v; }

    public String getStatus()                       { return status; }
    public void   setStatus(String v)               { this.status = v; }

    public String getVisibility()                   { return visibility; }
    public void   setVisibility(String v)           { this.visibility = v; }

    public String getPriceType()                    { return priceType; }
    public void   setPriceType(String v)            { this.priceType = v; }

    public double getPrice()                        { return price; }
    public void   setPrice(double v)                { this.price = v; }

    public int    getPointsCost()                   { return pointsCost; }
    public void   setPointsCost(int v)              { this.pointsCost = v; }

    public String getAgeRating()                    { return ageRating; }
    public void   setAgeRating(String v)            { this.ageRating = v; }

    public List<String> getContentWarnings()        { return contentWarnings; }
    public void   setContentWarnings(List<String> v){ this.contentWarnings = v; }

    public String getLicenseType()                  { return licenseType; }
    public void   setLicenseType(String v)          { this.licenseType = v; }

    public String getLanguage()                     { return language; }
    public void   setLanguage(String v)             { this.language = v; }

    public int    getTotalChapters()                { return totalChapters; }
    public void   setTotalChapters(int v)           { this.totalChapters = v; }

    public long   getTotalWords()                   { return totalWords; }
    public void   setTotalWords(long v)             { this.totalWords = v; }

    public long   getTotalViews()                   { return totalViews; }
    public void   setTotalViews(long v)             { this.totalViews = v; }

    public int    getTotalLikes()                   { return totalLikes; }
    public void   setTotalLikes(int v)              { this.totalLikes = v; }

    public int    getTotalBookmarks()               { return totalBookmarks; }
    public void   setTotalBookmarks(int v)          { this.totalBookmarks = v; }

    public int    getTotalComments()                { return totalComments; }
    public void   setTotalComments(int v)           { this.totalComments = v; }

    public double getAverageRating()                { return averageRating; }
    public void   setAverageRating(double v)        { this.averageRating = v; }

    public int    getRatingCount()                  { return ratingCount; }
    public void   setRatingCount(int v)             { this.ratingCount = v; }

    public boolean isPublished()                    { return isPublished; }
    public void    setPublished(boolean v)          { this.isPublished = v; }

    public Date   getPublishedAt()                  { return publishedAt; }
    public void   setPublishedAt(Date v)            { this.publishedAt = v; }

    public Date   getLastUpdatedAt()                { return lastUpdatedAt; }
    public void   setLastUpdatedAt(Date v)          { this.lastUpdatedAt = v; }

    public boolean isFeatured()                     { return isFeatured; }
    public void    setFeatured(boolean v)           { this.isFeatured = v; }

    public List<String> getTags()                   { return tags; }
    public void   setTags(List<String> v)           { this.tags = v; }

    public List<String> getGenres()                 { return genres; }
    public void   setGenres(List<String> v)         { this.genres = v; }

    public String getAuthorUsername()               { return authorUsername; }
    public void   setAuthorUsername(String v)       { this.authorUsername = v; }

    public String getAuthorDisplayName()            { return authorDisplayName; }
    public void   setAuthorDisplayName(String v)    { this.authorDisplayName = v; }

    public String getAuthorAvatarUrl()              { return authorAvatarUrl; }
    public void   setAuthorAvatarUrl(String v)      { this.authorAvatarUrl = v; }

    public boolean isAuthorVerified()               { return isAuthorVerified; }
    public void    setAuthorVerified(boolean v)     { this.isAuthorVerified = v; }

    public boolean isDownloaded()                   { return isDownloaded; }
    public void    setDownloaded(boolean v)         { this.isDownloaded = v; }

    public boolean isInLibrary()                    { return isInLibrary; }
    public void    setInLibrary(boolean v)          { this.isInLibrary = v; }

    public boolean isPurchased()                    { return isPurchased; }
    public void    setPurchased(boolean v)          { this.isPurchased = v; }

    public double  getReadingProgressPercent()      { return readingProgressPercent; }
    public void    setReadingProgressPercent(double v) { this.readingProgressPercent = v; }

    public int     getCurrentChapterNumber()        { return currentChapterNumber; }
    public void    setCurrentChapterNumber(int v)   { this.currentChapterNumber = v; }

    public String  getCurrentChapterId()            { return currentChapterId; }
    public void    setCurrentChapterId(String v)    { this.currentChapterId = v; }

    public boolean isFree()            { return "free".equals(priceType); }
    public String  getFormattedChapterCount() { return totalChapters + " chapters"; }
    public String  getFormattedRating() { return String.format("%.1f", averageRating); }
}

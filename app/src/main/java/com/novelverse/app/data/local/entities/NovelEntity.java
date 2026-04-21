package com.novelverse.app.data.local.entities;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;

import com.novelverse.app.data.local.database.Converters;

import java.util.Date;
import java.util.List;

/**
 * Room entity for Novel data (offline cache)
 */
@Entity(
    tableName = "novels",
    indices = {
        @Index(value = "author_id"),
        @Index(value = "status"),
        @Index(value = "is_published"),
        @Index(value = "synced_at")
    }
)
@TypeConverters(Converters.class)
public class NovelEntity {

    @PrimaryKey
    @ColumnInfo(name = "id")
    private String id;

    @ColumnInfo(name = "author_id")
    private String authorId;

    @ColumnInfo(name = "title")
    private String title;

    @ColumnInfo(name = "slug")
    private String slug;

    @ColumnInfo(name = "description")
    private String description;

    @ColumnInfo(name = "cover_image_url")
    private String coverImageUrl;

    @ColumnInfo(name = "banner_image_url")
    private String bannerImageUrl;

    @ColumnInfo(name = "status")
    private String status; // ongoing, completed, hiatus, dropped, draft

    @ColumnInfo(name = "visibility")
    private String visibility; // public, private, unlisted

    @ColumnInfo(name = "price_type")
    private String priceType; // free, paid, freemium

    @ColumnInfo(name = "price")
    private Double price;

    @ColumnInfo(name = "points_cost")
    private Integer pointsCost;

    @ColumnInfo(name = "age_rating")
    private String ageRating; // all, teen, mature, adult

    @ColumnInfo(name = "content_warnings")
    private List<String> contentWarnings;

    @ColumnInfo(name = "license_type")
    private String licenseType;

    @ColumnInfo(name = "language")
    private String language;

    @ColumnInfo(name = "total_chapters")
    private Integer totalChapters;

    @ColumnInfo(name = "total_words")
    private Long totalWords;

    @ColumnInfo(name = "total_views")
    private Long totalViews;

    @ColumnInfo(name = "total_likes")
    private Integer totalLikes;

    @ColumnInfo(name = "total_bookmarks")
    private Integer totalBookmarks;

    @ColumnInfo(name = "total_comments")
    private Integer totalComments;

    @ColumnInfo(name = "average_rating")
    private Double averageRating;

    @ColumnInfo(name = "rating_count")
    private Integer ratingCount;

    @ColumnInfo(name = "is_published")
    private Boolean isPublished;

    @ColumnInfo(name = "published_at")
    private Date publishedAt;

    @ColumnInfo(name = "last_updated_at")
    private Date lastUpdatedAt;

    @ColumnInfo(name = "is_featured")
    private Boolean isFeatured;

    @ColumnInfo(name = "tags")
    private List<String> tags;

    @ColumnInfo(name = "genres")
    private List<String> genres;

    @ColumnInfo(name = "author_username")
    private String authorUsername;

    @ColumnInfo(name = "author_display_name")
    private String authorDisplayName;

    @ColumnInfo(name = "author_avatar_url")
    private String authorAvatarUrl;

    @ColumnInfo(name = "is_author_verified")
    private Boolean isAuthorVerified;

    @ColumnInfo(name = "moderation_status")
    private String moderationStatus;

    // Local fields
    @ColumnInfo(name = "is_downloaded")
    private Boolean isDownloaded;

    @ColumnInfo(name = "download_completed_at")
    private Date downloadCompletedAt;

    @ColumnInfo(name = "created_at")
    private Date createdAt;

    @ColumnInfo(name = "featured_at")
    private Date featuredAt;

    @ColumnInfo(name = "completed_at")
    private Date completedAt;

    @ColumnInfo(name = "synced_at")
    private Date syncedAt;

    @ColumnInfo(name = "is_dirty")
    private Boolean isDirty; // For sync tracking

    public NovelEntity() {
        this.isDownloaded = false;
        this.isDirty = false;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getAuthorId() { return authorId; }
    public void setAuthorId(String authorId) { this.authorId = authorId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getCoverImageUrl() { return coverImageUrl; }
    public void setCoverImageUrl(String coverImageUrl) { this.coverImageUrl = coverImageUrl; }

    public String getBannerImageUrl() { return bannerImageUrl; }
    public void setBannerImageUrl(String bannerImageUrl) { this.bannerImageUrl = bannerImageUrl; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getVisibility() { return visibility; }
    public void setVisibility(String visibility) { this.visibility = visibility; }

    public String getPriceType() { return priceType; }
    public void setPriceType(String priceType) { this.priceType = priceType; }

    public Double getPrice() { return price; }
    public void setPrice(Double price) { this.price = price; }

    public Integer getPointsCost() { return pointsCost; }
    public void setPointsCost(Integer pointsCost) { this.pointsCost = pointsCost; }

    public String getAgeRating() { return ageRating; }
    public void setAgeRating(String ageRating) { this.ageRating = ageRating; }

    public List<String> getContentWarnings() { return contentWarnings; }
    public void setContentWarnings(List<String> contentWarnings) { this.contentWarnings = contentWarnings; }

    public String getLicenseType() { return licenseType; }
    public void setLicenseType(String licenseType) { this.licenseType = licenseType; }

    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }

    public Integer getTotalChapters() { return totalChapters; }
    public void setTotalChapters(Integer totalChapters) { this.totalChapters = totalChapters; }

    public Long getTotalWords() { return totalWords; }
    public void setTotalWords(Long totalWords) { this.totalWords = totalWords; }

    public Long getTotalViews() { return totalViews; }
    public void setTotalViews(Long totalViews) { this.totalViews = totalViews; }

    public Integer getTotalLikes() { return totalLikes; }
    public void setTotalLikes(Integer totalLikes) { this.totalLikes = totalLikes; }

    public Integer getTotalBookmarks() { return totalBookmarks; }
    public void setTotalBookmarks(Integer totalBookmarks) { this.totalBookmarks = totalBookmarks; }

    public Integer getTotalComments() { return totalComments; }
    public void setTotalComments(Integer totalComments) { this.totalComments = totalComments; }

    public Double getAverageRating() { return averageRating; }
    public void setAverageRating(Double averageRating) { this.averageRating = averageRating; }

    public Integer getRatingCount() { return ratingCount; }
    public void setRatingCount(Integer ratingCount) { this.ratingCount = ratingCount; }

    public Boolean getIsPublished() { return isPublished; }
    public void setIsPublished(Boolean isPublished) { this.isPublished = isPublished; }

    public Date getPublishedAt() { return publishedAt; }
    public void setPublishedAt(Date publishedAt) { this.publishedAt = publishedAt; }

    public Date getLastUpdatedAt() { return lastUpdatedAt; }
    public void setLastUpdatedAt(Date lastUpdatedAt) { this.lastUpdatedAt = lastUpdatedAt; }

    public Boolean getIsFeatured() { return isFeatured; }
    public void setIsFeatured(Boolean isFeatured) { this.isFeatured = isFeatured; }

    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }

    public List<String> getGenres() { return genres; }
    public void setGenres(List<String> genres) { this.genres = genres; }

    public String getAuthorUsername() { return authorUsername; }
    public void setAuthorUsername(String authorUsername) { this.authorUsername = authorUsername; }

    public String getAuthorDisplayName() { return authorDisplayName; }
    public void setAuthorDisplayName(String authorDisplayName) { this.authorDisplayName = authorDisplayName; }

    public String getAuthorAvatarUrl() { return authorAvatarUrl; }
    public void setAuthorAvatarUrl(String authorAvatarUrl) { this.authorAvatarUrl = authorAvatarUrl; }

    public Boolean getIsAuthorVerified() { return isAuthorVerified; }
    public void setIsAuthorVerified(Boolean isAuthorVerified) { this.isAuthorVerified = isAuthorVerified; }

    public String getModerationStatus() { return moderationStatus; }
    public void setModerationStatus(String moderationStatus) { this.moderationStatus = moderationStatus; }

    public Boolean getIsDownloaded() { return isDownloaded; }
    public void setIsDownloaded(Boolean isDownloaded) { this.isDownloaded = isDownloaded; }

    public Date getDownloadCompletedAt() { return downloadCompletedAt; }
    public void setDownloadCompletedAt(Date downloadCompletedAt) { this.downloadCompletedAt = downloadCompletedAt; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

    public Date getFeaturedAt() { return featuredAt; }
    public void setFeaturedAt(Date featuredAt) { this.featuredAt = featuredAt; }

    public Date getCompletedAt() { return completedAt; }
    public void setCompletedAt(Date completedAt) { this.completedAt = completedAt; }

    public Date getSyncedAt() { return syncedAt; }
    public void setSyncedAt(Date syncedAt) { this.syncedAt = syncedAt; }

    public Boolean getIsDirty() { return isDirty; }
    public void setIsDirty(Boolean isDirty) { this.isDirty = isDirty; }
}

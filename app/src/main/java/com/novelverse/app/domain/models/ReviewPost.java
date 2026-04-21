package com.novelverse.app.domain.models;

/**
 * Represents a single community review post for the social feed
 * and home-screen slideshow.
 */
public class ReviewPost {

    private String id;
    private String userId;
    private String username;
    private String displayName;
    private String userAvatarUrl;

    /** Used to derive ReaderLevel rank — chapters the user has completed */
    private int chaptersCompleted;

    private String novelId;
    private String novelTitle;
    private String novelAuthor;
    private String novelCoverUrl;
    private String novelGenre;
    private int    novelYear;
    private int    novelRecommendations;

    private int    rating;
    private String reviewText;
    private int    likesCount;
    private int    responsesCount;
    private boolean liked;

    /** ISO-8601 string, e.g. "2025-04-17T14:32:00" */
    private String createdAt;

    // ── Constructor ──────────────────────────────────────────────────────

    public ReviewPost() {}

    // Convenience full constructor used for mock data
    public ReviewPost(String id, String userId, String username, String displayName,
                      String userAvatarUrl, int chaptersCompleted,
                      String novelId, String novelTitle, String novelAuthor,
                      String novelCoverUrl, String novelGenre, int novelYear,
                      int novelRecommendations, int rating, String reviewText,
                      int likesCount, int responsesCount, String createdAt) {
        this.id                  = id;
        this.userId              = userId;
        this.username            = username;
        this.displayName         = displayName;
        this.userAvatarUrl       = userAvatarUrl;
        this.chaptersCompleted   = chaptersCompleted;
        this.novelId             = novelId;
        this.novelTitle          = novelTitle;
        this.novelAuthor         = novelAuthor;
        this.novelCoverUrl       = novelCoverUrl;
        this.novelGenre          = novelGenre;
        this.novelYear           = novelYear;
        this.novelRecommendations= novelRecommendations;
        this.rating              = rating;
        this.reviewText          = reviewText;
        this.likesCount          = likesCount;
        this.responsesCount      = responsesCount;
        this.createdAt           = createdAt;
    }

    // ── Derived helpers ───────────────────────────────────────────────────

    /** Derive the reader's rank from chaptersCompleted. */
    public ReaderLevel.Level getRank() {
        return ReaderLevel.fromChaptersCompleted(chaptersCompleted);
    }

    /** Human-readable "time ago" label computed from ISO createdAt string. */
    public String getTimeAgo() {
        if (createdAt == null || createdAt.isEmpty()) return "";
        try {
            java.text.SimpleDateFormat sdf =
                new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.US);
            sdf.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
            java.util.Date then = sdf.parse(createdAt.replace("Z", "").replaceAll("\\.\\d+", ""));
            if (then == null) return "";
            long diff = System.currentTimeMillis() - then.getTime();
            long mins  = diff / 60_000;
            if (mins < 1)  return "just now";
            if (mins < 60) return mins + "m ago";
            long hrs = mins / 60;
            if (hrs < 24) return hrs + "h ago";
            long days = hrs / 24;
            if (days < 7)  return days + "d ago";
            return (days / 7) + "w ago";
        } catch (Exception e) {
            return "";
        }
    }

    /** Format recommendation count: 1200 → "1.2k", 1000000 → "1M" */
    public static String formatCount(int count) {
        if (count >= 1_000_000) return String.format(java.util.Locale.US, "%.1fM", count / 1_000_000f);
        if (count >= 1_000)     return String.format(java.util.Locale.US, "%.1fk", count / 1_000f);
        return String.valueOf(count);
    }

    // ── Getters / Setters ─────────────────────────────────────────────────

    public String getId()                  { return id; }
    public void   setId(String v)          { id = v; }

    public String getUserId()              { return userId; }
    public void   setUserId(String v)      { userId = v; }

    public String getUsername()            { return username; }
    public void   setUsername(String v)    { username = v; }

    public String getDisplayName()         { return displayName; }
    public void   setDisplayName(String v) { displayName = v; }

    public String getUserAvatarUrl()           { return userAvatarUrl; }
    public void   setUserAvatarUrl(String v)   { userAvatarUrl = v; }

    public int  getChaptersCompleted()         { return chaptersCompleted; }
    public void setChaptersCompleted(int v)    { chaptersCompleted = v; }

    public String getNovelId()             { return novelId; }
    public void   setNovelId(String v)     { novelId = v; }

    public String getNovelTitle()          { return novelTitle; }
    public void   setNovelTitle(String v)  { novelTitle = v; }

    public String getNovelAuthor()         { return novelAuthor; }
    public void   setNovelAuthor(String v) { novelAuthor = v; }

    public String getNovelCoverUrl()           { return novelCoverUrl; }
    public void   setNovelCoverUrl(String v)   { novelCoverUrl = v; }

    public String getNovelGenre()          { return novelGenre; }
    public void   setNovelGenre(String v)  { novelGenre = v; }

    public int  getNovelYear()             { return novelYear; }
    public void setNovelYear(int v)        { novelYear = v; }

    public int  getNovelRecommendations()        { return novelRecommendations; }
    public void setNovelRecommendations(int v)   { novelRecommendations = v; }

    public int  getRating()                { return rating; }
    public void setRating(int v)           { rating = v; }

    public String getReviewText()          { return reviewText; }
    public void   setReviewText(String v)  { reviewText = v; }

    public int  getLikesCount()            { return likesCount; }
    public void setLikesCount(int v)       { likesCount = v; }

    public int  getResponsesCount()        { return responsesCount; }
    public void setResponsesCount(int v)   { responsesCount = v; }

    public boolean isLiked()              { return liked; }
    public void    setLiked(boolean v)    { liked = v; }

    public String getCreatedAt()           { return createdAt; }
    public void   setCreatedAt(String v)   { createdAt = v; }
}

package com.novelverse.app.domain.models;

public class Achievement {
    private String id, title, description;
    private int badgeDrawableRes;
    private int pointsAwarded;

    public Achievement() {}
    public Achievement(String id, String title, String description, int badgeDrawableRes, int pointsAwarded) {
        this.id = id; this.title = title; this.description = description;
        this.badgeDrawableRes = badgeDrawableRes; this.pointsAwarded = pointsAwarded;
    }
    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public int getBadgeDrawableRes() { return badgeDrawableRes; }
    public int getPointsAwarded() { return pointsAwarded; }
}

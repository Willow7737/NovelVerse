package com.novelverse.app.domain.models;

/**
 * Domain model for Point Package
 */
public class PointPackage {

    private String id;
    private String googlePlayProductId;
    private String name;
    private String description;
    private int pointsAmount;
    private double price;
    private String currency;
    private int bonusPoints;
    private boolean isActive;

    public PointPackage() {
        this.currency = "USD";
        this.bonusPoints = 0;
        this.isActive = true;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getGooglePlayProductId() { return googlePlayProductId; }
    public void setGooglePlayProductId(String googlePlayProductId) { this.googlePlayProductId = googlePlayProductId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public int getPointsAmount() { return pointsAmount; }
    public void setPointsAmount(int pointsAmount) { this.pointsAmount = pointsAmount; }

    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public int getBonusPoints() { return bonusPoints; }
    public void setBonusPoints(int bonusPoints) { this.bonusPoints = bonusPoints; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }

    /**
     * Get total points (base + bonus)
     */
    public int getTotalPoints() {
        return pointsAmount + bonusPoints;
    }

    /**
     * Get formatted price
     */
    public String getFormattedPrice() {
        return currency + " " + String.format("%.2f", price);
    }
}

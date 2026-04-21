package com.novelverse.app.domain.models;

import java.util.Date;

/**
 * Domain model for Purchase
 */
public class Purchase {

    private String id;
    private String userId;
    private String novelId;
    private String chapterId;
    private String purchaseType; // novel, chapter
    private String paymentMethod; // points, google_play, apple_pay, stripe
    private double amount;
    private int pointsUsed;
    private String currency;
    private String googlePlayOrderId;
    private String googlePlayPurchaseToken;
    private String googlePlayProductId;
    private String status; // pending, completed, failed, refunded, cancelled
    private Date purchasedAt;
    private Date expiresAt;
    private boolean isConsumable;

    public Purchase() {
        this.currency = "USD";
        this.isConsumable = false;
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

    public String getPurchaseType() { return purchaseType; }
    public void setPurchaseType(String purchaseType) { this.purchaseType = purchaseType; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public int getPointsUsed() { return pointsUsed; }
    public void setPointsUsed(int pointsUsed) { this.pointsUsed = pointsUsed; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public String getGooglePlayOrderId() { return googlePlayOrderId; }
    public void setGooglePlayOrderId(String googlePlayOrderId) { this.googlePlayOrderId = googlePlayOrderId; }

    public String getGooglePlayPurchaseToken() { return googlePlayPurchaseToken; }
    public void setGooglePlayPurchaseToken(String googlePlayPurchaseToken) { this.googlePlayPurchaseToken = googlePlayPurchaseToken; }

    public String getGooglePlayProductId() { return googlePlayProductId; }
    public void setGooglePlayProductId(String googlePlayProductId) { this.googlePlayProductId = googlePlayProductId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Date getPurchasedAt() { return purchasedAt; }
    public void setPurchasedAt(Date purchasedAt) { this.purchasedAt = purchasedAt; }

    public Date getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Date expiresAt) { this.expiresAt = expiresAt; }

    public boolean isConsumable() { return isConsumable; }
    public void setConsumable(boolean consumable) { isConsumable = consumable; }

    /**
     * Check if purchase is completed
     */
    public boolean isCompleted() {
        return "completed".equals(status);
    }

    /**
     * Get formatted amount
     */
    public String getFormattedAmount() {
        if (pointsUsed > 0) {
            return pointsUsed + " points";
        } else {
            return currency + " " + String.format("%.2f", amount);
        }
    }
}

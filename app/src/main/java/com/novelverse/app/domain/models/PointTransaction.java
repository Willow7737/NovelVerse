package com.novelverse.app.domain.models;

import java.util.Date;

/**
 * Domain model for Point Transaction
 */
public class PointTransaction {

    private String id;
    private String userId;
    private int amount;
    private String type; // purchase, spend, earn, refund, bonus, tip_given, tip_received, referral, promo_code
    private String description;
    private String referenceType;
    private String referenceId;
    private int balanceAfter;
    private Date createdAt;

    public PointTransaction() {
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public int getAmount() { return amount; }
    public void setAmount(int amount) { this.amount = amount; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getReferenceType() { return referenceType; }
    public void setReferenceType(String referenceType) { this.referenceType = referenceType; }

    public String getReferenceId() { return referenceId; }
    public void setReferenceId(String referenceId) { this.referenceId = referenceId; }

    public int getBalanceAfter() { return balanceAfter; }
    public void setBalanceAfter(int balanceAfter) { this.balanceAfter = balanceAfter; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

    /**
     * Check if this is a credit transaction
     */
    public boolean isCredit() {
        return amount > 0;
    }

    /**
     * Get formatted amount with sign
     */
    public String getFormattedAmount() {
        if (amount > 0) {
            return "+" + amount;
        } else {
            return String.valueOf(amount);
        }
    }
}

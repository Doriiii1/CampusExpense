package com.example.campusexpense.model;

/**
 * Budget - Budget tracking model for expense categories
 * Phase 3: Monthly budget limits with threshold notifications
 *
 * Fields:
 * - id: Unique identifier (database primary key)
 * - userId: User who owns this budget
 * - category: Expense category this budget applies to
 * - limitAmount: Maximum spending limit for the cycle
 * - currentSpent: Amount already spent in current cycle
 * - cycleType: Budget cycle type (MONTHLY, WEEKLY, etc.)
 * - lastReset: Last time the budget was reset (epoch ms)
 * - thresholdPercent: Percentage at which to trigger warning (default 80)
 *
 * Features:
 * - Progress calculation
 * - Over-limit detection
 * - Threshold warning system
 */
public class Budget {

    public static final String CYCLE_MONTHLY = "MONTHLY";
    public static final String CYCLE_WEEKLY = "WEEKLY";
    public static final String CYCLE_DAILY = "DAILY";
    public static final int DEFAULT_THRESHOLD = 80;

    private long id;
    private String userId;
    private String category;
    private double limitAmount;
    private double currentSpent;
    private String cycleType;
    private long lastReset;
    private int thresholdPercent;

    // Constructors
    public Budget() {
        this.id = -1;
        this.cycleType = CYCLE_MONTHLY;
        this.thresholdPercent = DEFAULT_THRESHOLD;
        this.currentSpent = 0.0;
        this.lastReset = System.currentTimeMillis();
    }

    public Budget(String userId, String category, double limitAmount, String cycleType, int thresholdPercent) {
        this.id = -1;
        this.userId = userId;
        this.category = category;
        this.limitAmount = limitAmount;
        this.currentSpent = 0.0;
        this.cycleType = cycleType;
        this.thresholdPercent = thresholdPercent;
        this.lastReset = System.currentTimeMillis();
    }

    // Helper methods

    /**
     * Calculate progress percentage (0-100+)
     * @return Progress as integer percentage
     */
    public int getProgressPercent() {
        if (limitAmount <= 0) {
            return 0;
        }
        return (int) ((currentSpent / limitAmount) * 100);
    }

    /**
     * Check if budget is over limit
     * @return true if current spent exceeds limit
     */
    public boolean isOverLimit() {
        return currentSpent > limitAmount;
    }

    /**
     * Check if budget has reached threshold warning level
     * @return true if progress >= threshold percentage
     */
    public boolean isAtThreshold() {
        return getProgressPercent() >= thresholdPercent;
    }

    /**
     * Get remaining budget amount
     * @return Amount remaining (can be negative if over limit)
     */
    public double getRemainingAmount() {
        return limitAmount - currentSpent;
    }

    /**
     * Check if budget needs to be reset based on cycle
     * @param currentTime Current timestamp
     * @return true if reset is needed
     */
    public boolean needsReset(long currentTime) {
        long cycleMillis;

        switch (cycleType) {
            case CYCLE_DAILY:
                cycleMillis = 24L * 60 * 60 * 1000; // 1 day
                break;
            case CYCLE_WEEKLY:
                cycleMillis = 7L * 24 * 60 * 60 * 1000; // 7 days
                break;
            case CYCLE_MONTHLY:
            default:
                cycleMillis = 30L * 24 * 60 * 60 * 1000; // 30 days (approx)
                break;
        }

        return (currentTime - lastReset) >= cycleMillis;
    }

    /**
     * Reset budget for new cycle
     */
    public void reset() {
        this.currentSpent = 0.0;
        this.lastReset = System.currentTimeMillis();
    }

    // Getters and Setters
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public double getLimitAmount() {
        return limitAmount;
    }

    public void setLimitAmount(double limitAmount) {
        this.limitAmount = limitAmount;
    }

    public double getCurrentSpent() {
        return currentSpent;
    }

    public void setCurrentSpent(double currentSpent) {
        this.currentSpent = currentSpent;
    }

    public String getCycleType() {
        return cycleType;
    }

    public void setCycleType(String cycleType) {
        this.cycleType = cycleType;
    }

    public long getLastReset() {
        return lastReset;
    }

    public void setLastReset(long lastReset) {
        this.lastReset = lastReset;
    }

    public int getThresholdPercent() {
        return thresholdPercent;
    }

    public void setThresholdPercent(int thresholdPercent) {
        this.thresholdPercent = thresholdPercent;
    }

    @Override
    public String toString() {
        return "Budget{" +
                "id=" + id +
                ", userId='" + userId + '\'' +
                ", category='" + category + '\'' +
                ", limitAmount=" + limitAmount +
                ", currentSpent=" + currentSpent +
                ", cycleType='" + cycleType + '\'' +
                ", lastReset=" + lastReset +
                ", thresholdPercent=" + thresholdPercent +
                '}';
    }
}
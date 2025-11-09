package com.example.campusexpense.model;

import java.util.Calendar;

/**
 * RecurringExpense - Model for automatically recurring expenses
 * Phase 3: Scheduled expense generation
 *
 * Fields:
 * - id: Unique identifier
 * - userId: User who owns this recurring expense
 * - category: Expense category
 * - amount: Recurring amount
 * - description: Expense description
 * - startDate: When recurring starts (epoch ms)
 * - endDate: When recurring ends (null for indefinite)
 * - frequency: DAILY, WEEKLY, MONTHLY
 * - nextRun: Next scheduled execution time (epoch ms)
 *
 * Features:
 * - Automatic next run calculation based on frequency
 * - End date handling
 * - Active status checking
 */
public class RecurringExpense {

    public static final String FREQUENCY_DAILY = "DAILY";
    public static final String FREQUENCY_WEEKLY = "WEEKLY";
    public static final String FREQUENCY_MONTHLY = "MONTHLY";

    private long id;
    private String userId;
    private String category;
    private double amount;
    private String description;
    private long startDate;
    private Long endDate; // Nullable - null means indefinite
    private String frequency;
    private long nextRun;

    // Constructors
    public RecurringExpense() {
        this.id = -1;
        this.startDate = System.currentTimeMillis();
        this.nextRun = this.startDate;
        this.frequency = FREQUENCY_MONTHLY;
    }

    public RecurringExpense(String userId, String category, double amount, String description,
                            long startDate, Long endDate, String frequency) {
        this.id = -1;
        this.userId = userId;
        this.category = category;
        this.amount = amount;
        this.description = description;
        this.startDate = startDate;
        this.endDate = endDate;
        this.frequency = frequency;
        this.nextRun = startDate;
    }

    // Helper methods

    /**
     * Compute next run time based on frequency
     * @param fromTs Starting timestamp
     * @return Next run timestamp in milliseconds
     */
    public long computeNextRun(long fromTs) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(fromTs);

        switch (frequency) {
            case FREQUENCY_DAILY:
                calendar.add(Calendar.DAY_OF_MONTH, 1);
                break;
            case FREQUENCY_WEEKLY:
                calendar.add(Calendar.WEEK_OF_YEAR, 1);
                break;
            case FREQUENCY_MONTHLY:
                calendar.add(Calendar.MONTH, 1);
                break;
            default:
                calendar.add(Calendar.MONTH, 1); // Default to monthly
                break;
        }

        return calendar.getTimeInMillis();
    }

    /**
     * Check if recurring expense is still active
     * @param currentTime Current timestamp
     * @return true if active (not past end date)
     */
    public boolean isActive(long currentTime) {
        if (endDate == null) {
            return true; // Indefinite
        }
        return currentTime <= endDate;
    }

    /**
     * Check if this recurring expense is due to run
     * @param currentTime Current timestamp
     * @return true if nextRun <= currentTime and still active
     */
    public boolean isDue(long currentTime) {
        return nextRun <= currentTime && isActive(currentTime);
    }

    /**
     * Update next run time
     */
    public void updateNextRun() {
        this.nextRun = computeNextRun(this.nextRun);
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

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public long getStartDate() {
        return startDate;
    }

    public void setStartDate(long startDate) {
        this.startDate = startDate;
    }

    public Long getEndDate() {
        return endDate;
    }

    public void setEndDate(Long endDate) {
        this.endDate = endDate;
    }

    public String getFrequency() {
        return frequency;
    }

    public void setFrequency(String frequency) {
        this.frequency = frequency;
    }

    public long getNextRun() {
        return nextRun;
    }

    public void setNextRun(long nextRun) {
        this.nextRun = nextRun;
    }

    @Override
    public String toString() {
        return "RecurringExpense{" +
                "id=" + id +
                ", userId='" + userId + '\'' +
                ", category='" + category + '\'' +
                ", amount=" + amount +
                ", description='" + description + '\'' +
                ", startDate=" + startDate +
                ", endDate=" + endDate +
                ", frequency='" + frequency + '\'' +
                ", nextRun=" + nextRun +
                '}';
    }
}
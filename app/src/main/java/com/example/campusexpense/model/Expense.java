package com.example.campusexpense.model;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Expense - Data model for expense records
 * Phase 2: CRUD operations with SQLite
 *
 * Fields:
 * - id: Unique identifier (database primary key)
 * - userId: User who owns this expense (from AuthManager)
 * - category: Expense category (Food, Transport, etc.)
 * - description: Brief description of the expense
 * - amount: Expense amount in local currency
 * - date: Expense date in epoch milliseconds
 * - notes: Optional additional notes
 *
 * Implements Parcelable for passing between activities
 */
public class Expense implements Parcelable {

    private long id;
    private String userId;
    private String category;
    private String description;
    private double amount;
    private long date;
    private String notes;

    // Constructors
    public Expense() {
        this.id = -1;
        this.date = System.currentTimeMillis();
    }

    public Expense(String userId, String category, String description, double amount, long date, String notes) {
        this.id = -1;
        this.userId = userId;
        this.category = category;
        this.description = description;
        this.amount = amount;
        this.date = date;
        this.notes = notes;
    }

    // Parcelable implementation
    protected Expense(Parcel in) {
        id = in.readLong();
        userId = in.readString();
        category = in.readString();
        description = in.readString();
        amount = in.readDouble();
        date = in.readLong();
        notes = in.readString();
    }

    public static final Creator<Expense> CREATOR = new Creator<Expense>() {
        @Override
        public Expense createFromParcel(Parcel in) {
            return new Expense(in);
        }

        @Override
        public Expense[] newArray(int size) {
            return new Expense[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(id);
        dest.writeString(userId);
        dest.writeString(category);
        dest.writeString(description);
        dest.writeDouble(amount);
        dest.writeLong(date);
        dest.writeString(notes);
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public long getDate() {
        return date;
    }

    public void setDate(long date) {
        this.date = date;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    @Override
    public String toString() {
        return "Expense{" +
                "id=" + id +
                ", userId='" + userId + '\'' +
                ", category='" + category + '\'' +
                ", description='" + description + '\'' +
                ", amount=" + amount +
                ", date=" + date +
                ", notes='" + notes + '\'' +
                '}';
    }
}
package com.example.campusexpense.reports;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import com.example.campusexpense.db.DatabaseHelper;
import com.example.campusexpense.model.Expense;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * ReportGenerator - Expense aggregation and CSV export utilities
 * Phase 4: Overview and Reports
 *
 * Features:
 * - Total spending calculation by date range
 * - Category-wise aggregation
 * - Statistical analysis (count, sum, avg, min, max)
 * - CSV export formatting
 *
 * Security: Parameterized queries, no SQL concatenation
 * Threading: All methods designed to run off UI thread
 */
public class ReportGenerator {

    private static final String TAG = "ReportGenerator";

    private final Context context;
    private final DatabaseHelper dbHelper;

    // Database table and column constants (must match DatabaseHelper)
    private static final String TABLE_EXPENSES = "expenses";
    private static final String COL_ID = "id";
    private static final String COL_USER_ID = "user_id";
    private static final String COL_CATEGORY = "category";
    private static final String COL_DESCRIPTION = "description";
    private static final String COL_AMOUNT = "amount";
    private static final String COL_DATE = "date";
    private static final String COL_NOTES = "notes";

    public ReportGenerator(Context context) {
        this.context = context.getApplicationContext();
        this.dbHelper = new DatabaseHelper(context);
    }

    /**
     * Calculate total spending for a date range
     * @param userId User ID
     * @param startDate Start date (epoch ms)
     * @param endDate End date (epoch ms)
     * @return Total amount spent
     */
    public double getTotalSpent(String userId, long startDate, long endDate) {
        SQLiteDatabase db = null;
        Cursor cursor = null;

        try {
            db = dbHelper.getReadableDatabase();

            String selection = COL_USER_ID + " = ? AND " + COL_DATE + " >= ? AND " + COL_DATE + " <= ?";
            String[] selectionArgs = {userId, String.valueOf(startDate), String.valueOf(endDate)};

            cursor = db.query(
                    TABLE_EXPENSES,
                    new String[]{"SUM(" + COL_AMOUNT + ") as total"},
                    selection,
                    selectionArgs,
                    null,
                    null,
                    null
            );

            if (cursor != null && cursor.moveToFirst()) {
                return cursor.getDouble(0);
            }

        } catch (Exception e) {
            Log.e(TAG, "Error calculating total spent", e);
        } finally {
            if (cursor != null && !cursor.isClosed()) {
                cursor.close();
            }
            if (db != null && db.isOpen()) {
                db.close();
            }
        }

        return 0.0;
    }

    /**
     * Get spending totals by category
     * @param userId User ID
     * @param startDate Start date (epoch ms)
     * @param endDate End date (epoch ms)
     * @return Map of category to total amount
     */
    public Map<String, Double> getTotalsByCategory(String userId, long startDate, long endDate) {
        Map<String, Double> categoryTotals = new HashMap<>();
        SQLiteDatabase db = null;
        Cursor cursor = null;

        try {
            db = dbHelper.getReadableDatabase();

            String selection = COL_USER_ID + " = ? AND " + COL_DATE + " >= ? AND " + COL_DATE + " <= ?";
            String[] selectionArgs = {userId, String.valueOf(startDate), String.valueOf(endDate)};

            cursor = db.query(
                    TABLE_EXPENSES,
                    new String[]{COL_CATEGORY, "SUM(" + COL_AMOUNT + ") as total"},
                    selection,
                    selectionArgs,
                    COL_CATEGORY,
                    null,
                    "total DESC"
            );

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    String category = cursor.getString(0);
                    double total = cursor.getDouble(1);
                    categoryTotals.put(category, total);
                } while (cursor.moveToNext());
            }

            Log.d(TAG, "Retrieved totals for " + categoryTotals.size() + " categories");

        } catch (Exception e) {
            Log.e(TAG, "Error getting totals by category", e);
        } finally {
            if (cursor != null && !cursor.isClosed()) {
                cursor.close();
            }
            if (db != null && db.isOpen()) {
                db.close();
            }
        }

        return categoryTotals;
    }

    /**
     * Get detailed statistics for a specific category
     * @param userId User ID
     * @param category Category name
     * @param startDate Start date (epoch ms)
     * @param endDate End date (epoch ms)
     * @return CategoryStats object
     */
    public CategoryStats getStatsForCategory(String userId, String category, long startDate, long endDate) {
        CategoryStats stats = new CategoryStats();
        stats.category = category;

        SQLiteDatabase db = null;
        Cursor cursor = null;

        try {
            db = dbHelper.getReadableDatabase();

            String selection = COL_USER_ID + " = ? AND " + COL_CATEGORY + " = ? AND " +
                    COL_DATE + " >= ? AND " + COL_DATE + " <= ?";
            String[] selectionArgs = {userId, category, String.valueOf(startDate), String.valueOf(endDate)};

            // Get aggregated stats
            cursor = db.query(
                    TABLE_EXPENSES,
                    new String[]{
                            "COUNT(*) as count",
                            "SUM(" + COL_AMOUNT + ") as sum",
                            "AVG(" + COL_AMOUNT + ") as avg",
                            "MIN(" + COL_AMOUNT + ") as min",
                            "MAX(" + COL_AMOUNT + ") as max"
                    },
                    selection,
                    selectionArgs,
                    null,
                    null,
                    null
            );

            if (cursor != null && cursor.moveToFirst()) {
                stats.count = cursor.getInt(0);
                stats.sum = cursor.getDouble(1);
                stats.avg = cursor.getDouble(2);
                stats.min = cursor.getDouble(3);
                stats.max = cursor.getDouble(4);
            }

        } catch (Exception e) {
            Log.e(TAG, "Error getting stats for category", e);
        } finally {
            if (cursor != null && !cursor.isClosed()) {
                cursor.close();
            }
            if (db != null && db.isOpen()) {
                db.close();
            }
        }

        return stats;
    }

    /**
     * Get expenses for a specific category and date range
     * @param userId User ID
     * @param category Category name (null for all)
     * @param startDate Start date (epoch ms)
     * @param endDate End date (epoch ms)
     * @param sortBy Sort field ("date" or "amount")
     * @return List of expenses
     */
    public List<Expense> getExpensesForReport(String userId, String category, long startDate,
                                              long endDate, String sortBy) {
        List<Expense> expenses = new ArrayList<>();
        SQLiteDatabase db = null;
        Cursor cursor = null;

        try {
            db = dbHelper.getReadableDatabase();

            StringBuilder selection = new StringBuilder(COL_USER_ID + " = ? AND " +
                    COL_DATE + " >= ? AND " + COL_DATE + " <= ?");
            List<String> selectionArgs = new ArrayList<>();
            selectionArgs.add(userId);
            selectionArgs.add(String.valueOf(startDate));
            selectionArgs.add(String.valueOf(endDate));

            if (category != null && !category.isEmpty()) {
                selection.append(" AND ").append(COL_CATEGORY).append(" = ?");
                selectionArgs.add(category);
            }

            String orderBy = COL_DATE + " DESC";
            if ("amount".equals(sortBy)) {
                orderBy = COL_AMOUNT + " DESC";
            }

            cursor = db.query(
                    TABLE_EXPENSES,
                    null,
                    selection.toString(),
                    selectionArgs.toArray(new String[0]),
                    null,
                    null,
                    orderBy
            );

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    Expense expense = new Expense();
                    expense.setId(cursor.getLong(cursor.getColumnIndexOrThrow(COL_ID)));
                    expense.setUserId(cursor.getString(cursor.getColumnIndexOrThrow(COL_USER_ID)));
                    expense.setCategory(cursor.getString(cursor.getColumnIndexOrThrow(COL_CATEGORY)));
                    expense.setDescription(cursor.getString(cursor.getColumnIndexOrThrow(COL_DESCRIPTION)));
                    expense.setAmount(cursor.getDouble(cursor.getColumnIndexOrThrow(COL_AMOUNT)));
                    expense.setDate(cursor.getLong(cursor.getColumnIndexOrThrow(COL_DATE)));
                    expense.setNotes(cursor.getString(cursor.getColumnIndexOrThrow(COL_NOTES)));

                    expenses.add(expense);
                } while (cursor.moveToNext());
            }

        } catch (Exception e) {
            Log.e(TAG, "Error getting expenses for report", e);
        } finally {
            if (cursor != null && !cursor.isClosed()) {
                cursor.close();
            }
            if (db != null && db.isOpen()) {
                db.close();
            }
        }

        return expenses;
    }

    /**
     * Generate CSV string from expense list
     * @param expenses List of expenses
     * @return CSV formatted string
     */
    public String generateCsv(List<Expense> expenses) {
        StringBuilder csv = new StringBuilder();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);

        // Header
        csv.append("Date,Category,Description,Amount,Notes\n");

        // Data rows
        for (Expense expense : expenses) {
            csv.append(dateFormat.format(expense.getDate())).append(",");
            csv.append(escapeCsv(expense.getCategory())).append(",");
            csv.append(escapeCsv(expense.getDescription())).append(",");
            csv.append(String.format(Locale.US, "%.2f", expense.getAmount())).append(",");
            csv.append(escapeCsv(expense.getNotes() != null ? expense.getNotes() : "")).append("\n");
        }

        return csv.toString();
    }

    /**
     * Escape CSV special characters
     * @param value String value
     * @return Escaped string
     */
    private String escapeCsv(String value) {
        if (value == null) {
            return "";
        }

        // If contains comma, quote, or newline, wrap in quotes and escape quotes
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }

        return value;
    }

    /**
     * Clean up resources
     */
    public void close() {
        if (dbHelper != null) {
            dbHelper.close();
        }
    }

    /**
     * CategoryStats - Statistics for a category
     */
    public static class CategoryStats {
        public String category;
        public int count;
        public double sum;
        public double avg;
        public double min;
        public double max;

        @Override
        public String toString() {
            return String.format(Locale.US,
                    "CategoryStats{category='%s', count=%d, sum=%.2f, avg=%.2f, min=%.2f, max=%.2f}",
                    category, count, sum, avg, min, max);
        }
    }

    /**
     * CategorySummary - Summary for overview display
     */
    public static class CategorySummary {
        public String category;
        public double total;
        public double percentage;
        public int transactionCount;

        // Budget status (if applicable)
        public boolean hasBudget;
        public double budgetLimit;
        public double budgetSpent;
        public int budgetProgress;

        public CategorySummary(String category, double total, double percentage) {
            this.category = category;
            this.total = total;
            this.percentage = percentage;
        }
    }
}
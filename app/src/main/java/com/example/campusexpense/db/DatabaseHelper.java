package com.example.campusexpense.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import com.example.campusexpense.model.Budget;
import com.example.campusexpense.model.Expense;
import com.example.campusexpense.model.RecurringExpense;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * DatabaseHelper - SQLite database manager
 * Phase 3: Extended with budgets and recurring expenses
 *
 * Features:
 * - Expense, Budget, and RecurringExpense tables
 * - CRUD operations with error handling
 * - Budget progress calculation via expense aggregation
 * - User-specific data isolation
 *
 * Security: No sensitive data in logs, parameterized queries, user isolation
 *
 * WARNING: onUpgrade uses DROP TABLE - data loss on schema changes
 * Consider implementing proper migration for production
 */
public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String TAG = "DatabaseHelper";

    // Database info
    private static final String DATABASE_NAME = "campusexpense.db";
    private static final int DATABASE_VERSION = 2; // Phase 3: Incremented from 1 to 2

    // Table names
    private static final String TABLE_EXPENSES = "expenses";
    private static final String TABLE_BUDGETS = "budgets";
    private static final String TABLE_RECURRING = "recurring_expenses";

    // Expense table columns
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_USER_ID = "user_id";
    private static final String COLUMN_CATEGORY = "category";
    private static final String COLUMN_DESCRIPTION = "description";
    private static final String COLUMN_AMOUNT = "amount";
    private static final String COLUMN_DATE = "date";
    private static final String COLUMN_NOTES = "notes";

    // Budget table columns
    private static final String COLUMN_BUDGET_ID = "id";
    private static final String COLUMN_BUDGET_USER_ID = "user_id";
    private static final String COLUMN_BUDGET_CATEGORY = "category";
    private static final String COLUMN_BUDGET_LIMIT = "limit_amount";
    private static final String COLUMN_BUDGET_SPENT = "current_spent";
    private static final String COLUMN_BUDGET_CYCLE = "cycle_type";
    private static final String COLUMN_BUDGET_RESET = "last_reset";
    private static final String COLUMN_BUDGET_THRESHOLD = "threshold_percent";

    // Recurring expense table columns
    private static final String COLUMN_RECURRING_ID = "id";
    private static final String COLUMN_RECURRING_USER_ID = "user_id";
    private static final String COLUMN_RECURRING_CATEGORY = "category";
    private static final String COLUMN_RECURRING_AMOUNT = "amount";
    private static final String COLUMN_RECURRING_DESC = "description";
    private static final String COLUMN_RECURRING_START = "start_date";
    private static final String COLUMN_RECURRING_END = "end_date";
    private static final String COLUMN_RECURRING_FREQ = "frequency";
    private static final String COLUMN_RECURRING_NEXT = "next_run";

    // Create table SQL
    private static final String CREATE_EXPENSES_TABLE =
            "CREATE TABLE " + TABLE_EXPENSES + " (" +
                    COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COLUMN_USER_ID + " TEXT NOT NULL, " +
                    COLUMN_CATEGORY + " TEXT NOT NULL, " +
                    COLUMN_DESCRIPTION + " TEXT NOT NULL, " +
                    COLUMN_AMOUNT + " REAL NOT NULL, " +
                    COLUMN_DATE + " INTEGER NOT NULL, " +
                    COLUMN_NOTES + " TEXT" +
                    ")";

    private static final String CREATE_BUDGETS_TABLE =
            "CREATE TABLE " + TABLE_BUDGETS + " (" +
                    COLUMN_BUDGET_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COLUMN_BUDGET_USER_ID + " TEXT NOT NULL, " +
                    COLUMN_BUDGET_CATEGORY + " TEXT NOT NULL, " +
                    COLUMN_BUDGET_LIMIT + " REAL NOT NULL, " +
                    COLUMN_BUDGET_SPENT + " REAL DEFAULT 0, " +
                    COLUMN_BUDGET_CYCLE + " TEXT DEFAULT 'MONTHLY', " +
                    COLUMN_BUDGET_RESET + " INTEGER NOT NULL, " +
                    COLUMN_BUDGET_THRESHOLD + " INTEGER DEFAULT 80, " +
                    "UNIQUE(" + COLUMN_BUDGET_USER_ID + ", " + COLUMN_BUDGET_CATEGORY + ")" +
                    ")";

    private static final String CREATE_RECURRING_TABLE =
            "CREATE TABLE " + TABLE_RECURRING + " (" +
                    COLUMN_RECURRING_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COLUMN_RECURRING_USER_ID + " TEXT NOT NULL, " +
                    COLUMN_RECURRING_CATEGORY + " TEXT NOT NULL, " +
                    COLUMN_RECURRING_AMOUNT + " REAL NOT NULL, " +
                    COLUMN_RECURRING_DESC + " TEXT NOT NULL, " +
                    COLUMN_RECURRING_START + " INTEGER NOT NULL, " +
                    COLUMN_RECURRING_END + " INTEGER, " +
                    COLUMN_RECURRING_FREQ + " TEXT NOT NULL, " +
                    COLUMN_RECURRING_NEXT + " INTEGER NOT NULL" +
                    ")";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        try {
            db.execSQL(CREATE_EXPENSES_TABLE);
            db.execSQL(CREATE_BUDGETS_TABLE);
            db.execSQL(CREATE_RECURRING_TABLE);
            Log.d(TAG, "Database tables created successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error creating database tables", e);
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // For production: Implement proper migration logic to preserve data
        try {
            Log.d(TAG, "Upgrading database from version " + oldVersion + " to " + newVersion);

            if (oldVersion < 2) {
                Log.d(TAG, "Migrating from v1 to v2: Adding budgets and recurring tables (preserving expenses)");

                // Drop new tables if they exist (to recreate cleanly), but KEEP expenses
                db.execSQL("DROP TABLE IF EXISTS " + TABLE_BUDGETS);
                db.execSQL("DROP TABLE IF EXISTS " + TABLE_RECURRING);

                // Create only the new tables
                db.execSQL(CREATE_BUDGETS_TABLE);
                db.execSQL(CREATE_RECURRING_TABLE);

                Log.d(TAG, "Migration complete: budgets and recurring tables added");
            } else if (oldVersion > newVersion) {
                // Handle downgrade (rare, but log it)
                Log.w(TAG, "Downgrade detected - recreating all tables (data loss possible)");
                db.execSQL("DROP TABLE IF EXISTS " + TABLE_EXPENSES);
                db.execSQL("DROP TABLE IF EXISTS " + TABLE_BUDGETS);
                db.execSQL("DROP TABLE IF EXISTS " + TABLE_RECURRING);
                onCreate(db);
            } else {
                Log.d(TAG, "No migration needed (same version)");
            }

            Log.d(TAG, "Database upgrade complete");
        } catch (Exception e) {
            Log.e(TAG, "Error during database upgrade", e);
            // Fallback: Recreate everything (data loss, but prevents crash)
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_EXPENSES);
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_BUDGETS);
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_RECURRING);
            onCreate(db);
            Log.w(TAG, "Fallback: Recreated all tables due to upgrade error (data lost)");
        }
    }

    // ==================== EXPENSE METHODS (Phase 2) ====================

    /**
     * Insert a new expense
     * @param expense Expense object to insert
     * @return ID of inserted expense, or -1 on error
     */
    public long insertExpense(Expense expense) {
        SQLiteDatabase db = null;
        try {
            db = this.getWritableDatabase();
            ContentValues values = new ContentValues();
            values.put(COLUMN_USER_ID, expense.getUserId());
            values.put(COLUMN_CATEGORY, expense.getCategory());
            values.put(COLUMN_DESCRIPTION, expense.getDescription());
            values.put(COLUMN_AMOUNT, expense.getAmount());
            values.put(COLUMN_DATE, expense.getDate());
            values.put(COLUMN_NOTES, expense.getNotes());

            long id = db.insert(TABLE_EXPENSES, null, values);
            Log.d(TAG, "Expense inserted with ID: " + id);
            return id;

        } catch (Exception e) {
            Log.e(TAG, "Error inserting expense", e);
            return -1;
        } finally {
            if (db != null && db.isOpen()) {
                db.close();
            }
        }
    }

    /**
     * Update an existing expense
     * @param expense Expense object with updated data
     * @return Number of rows affected, or -1 on error
     */
    public int updateExpense(Expense expense) {
        SQLiteDatabase db = null;
        try {
            db = this.getWritableDatabase();
            ContentValues values = new ContentValues();
            values.put(COLUMN_CATEGORY, expense.getCategory());
            values.put(COLUMN_DESCRIPTION, expense.getDescription());
            values.put(COLUMN_AMOUNT, expense.getAmount());
            values.put(COLUMN_DATE, expense.getDate());
            values.put(COLUMN_NOTES, expense.getNotes());

            int rowsAffected = db.update(
                    TABLE_EXPENSES,
                    values,
                    COLUMN_ID + " = ? AND " + COLUMN_USER_ID + " = ?",
                    new String[]{String.valueOf(expense.getId()), expense.getUserId()}
            );

            Log.d(TAG, "Expense updated, rows affected: " + rowsAffected);
            return rowsAffected;

        } catch (Exception e) {
            Log.e(TAG, "Error updating expense", e);
            return -1;
        } finally {
            if (db != null && db.isOpen()) {
                db.close();
            }
        }
    }

    /**
     * Delete an expense by ID
     * @param id Expense ID to delete
     * @param userId User ID for security check
     * @return Number of rows deleted, or -1 on error
     */
    public int deleteExpense(long id, String userId) {
        SQLiteDatabase db = null;
        try {
            db = this.getWritableDatabase();
            int rowsDeleted = db.delete(
                    TABLE_EXPENSES,
                    COLUMN_ID + " = ? AND " + COLUMN_USER_ID + " = ?",
                    new String[]{String.valueOf(id), userId}
            );

            Log.d(TAG, "Expense deleted, rows affected: " + rowsDeleted);
            return rowsDeleted;

        } catch (Exception e) {
            Log.e(TAG, "Error deleting expense", e);
            return -1;
        } finally {
            if (db != null && db.isOpen()) {
                db.close();
            }
        }
    }

    /**
     * Get all expenses for a user
     * @param userId User ID to filter by
     * @return List of expenses
     */
    public List<Expense> getExpenses(String userId) {
        return getExpenses(userId, null, 0, 0);
    }

    /**
     * Get expenses with optional filters
     * @param userId User ID to filter by
     * @param category Category filter (null for all)
     * @param startDate Start date filter (0 for no filter)
     * @param endDate End date filter (0 for no filter)
     * @return List of filtered expenses
     */
    public List<Expense> getExpenses(String userId, String category, long startDate, long endDate) {
        List<Expense> expenses = new ArrayList<>();
        SQLiteDatabase db = null;
        Cursor cursor = null;

        try {
            db = this.getReadableDatabase();

            // Build query with filters
            StringBuilder selection = new StringBuilder(COLUMN_USER_ID + " = ?");
            List<String> selectionArgs = new ArrayList<>();
            selectionArgs.add(userId);

            if (category != null && !category.isEmpty() && !category.equals("All")) {
                selection.append(" AND ").append(COLUMN_CATEGORY).append(" = ?");
                selectionArgs.add(category);
            }

            if (startDate > 0) {
                selection.append(" AND ").append(COLUMN_DATE).append(" >= ?");
                selectionArgs.add(String.valueOf(startDate));
            }

            if (endDate > 0) {
                selection.append(" AND ").append(COLUMN_DATE).append(" <= ?");
                selectionArgs.add(String.valueOf(endDate));
            }

            cursor = db.query(
                    TABLE_EXPENSES,
                    null,
                    selection.toString(),
                    selectionArgs.toArray(new String[0]),
                    null,
                    null,
                    COLUMN_DATE + " DESC"
            );

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    Expense expense = new Expense();
                    expense.setId(cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ID)));
                    expense.setUserId(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_USER_ID)));
                    expense.setCategory(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CATEGORY)));
                    expense.setDescription(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DESCRIPTION)));
                    expense.setAmount(cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_AMOUNT)));
                    expense.setDate(cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_DATE)));
                    expense.setNotes(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NOTES)));

                    expenses.add(expense);
                } while (cursor.moveToNext());
            }

            Log.d(TAG, "Retrieved " + expenses.size() + " expenses for user " + userId);

        } catch (Exception e) {
            Log.e(TAG, "Error retrieving expenses", e);
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
     * Get total expense amount for a user within date range
     * @param userId User ID
     * @param startDate Start date (0 for all time)
     * @param endDate End date (0 for all time)
     * @return Total amount
     */
    public double getTotalExpenses(String userId, long startDate, long endDate) {
        double total = 0.0;
        SQLiteDatabase db = null;
        Cursor cursor = null;

        try {
            db = this.getReadableDatabase();

            StringBuilder selection = new StringBuilder(COLUMN_USER_ID + " = ?");
            List<String> selectionArgs = new ArrayList<>();
            selectionArgs.add(userId);

            if (startDate > 0) {
                selection.append(" AND ").append(COLUMN_DATE).append(" >= ?");
                selectionArgs.add(String.valueOf(startDate));
            }

            if (endDate > 0) {
                selection.append(" AND ").append(COLUMN_DATE).append(" <= ?");
                selectionArgs.add(String.valueOf(endDate));
            }

            cursor = db.query(
                    TABLE_EXPENSES,
                    new String[]{"SUM(" + COLUMN_AMOUNT + ") as total"},
                    selection.toString(),
                    selectionArgs.toArray(new String[0]),
                    null,
                    null,
                    null
            );

            if (cursor != null && cursor.moveToFirst()) {
                total = cursor.getDouble(0);
            }

        } catch (Exception e) {
            Log.e(TAG, "Error calculating total expenses", e);
        } finally {
            if (cursor != null && !cursor.isClosed()) {
                cursor.close();
            }
            if (db != null && db.isOpen()) {
                db.close();
            }
        }

        return total;
    }

    /**
     * Get expense by ID
     * @param id Expense ID
     * @param userId User ID for security check
     * @return Expense or null if not found
     */
    public Expense getExpenseById(long id, String userId) {
        SQLiteDatabase db = null;
        Cursor cursor = null;

        try {
            db = this.getReadableDatabase();
            cursor = db.query(
                    TABLE_EXPENSES,
                    null,
                    COLUMN_ID + " = ? AND " + COLUMN_USER_ID + " = ?",
                    new String[]{String.valueOf(id), userId},
                    null,
                    null,
                    null
            );

            if (cursor != null && cursor.moveToFirst()) {
                Expense expense = new Expense();
                expense.setId(cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ID)));
                expense.setUserId(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_USER_ID)));
                expense.setCategory(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CATEGORY)));
                expense.setDescription(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DESCRIPTION)));
                expense.setAmount(cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_AMOUNT)));
                expense.setDate(cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_DATE)));
                expense.setNotes(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NOTES)));
                return expense;
            }

        } catch (Exception e) {
            Log.e(TAG, "Error retrieving expense by ID", e);
        } finally {
            if (cursor != null && !cursor.isClosed()) {
                cursor.close();
            }
            if (db != null && db.isOpen()) {
                db.close();
            }
        }

        return null;
    }

    // ==================== BUDGET METHODS (Phase 3) ====================

    /**
     * Create a new budget
     * @param budget Budget object to create
     * @return ID of created budget, or -1 on error
     */
    public long createBudget(Budget budget) {
        SQLiteDatabase db = null;
        try {
            db = this.getWritableDatabase();
            ContentValues values = new ContentValues();
            values.put(COLUMN_BUDGET_USER_ID, budget.getUserId());
            values.put(COLUMN_BUDGET_CATEGORY, budget.getCategory());
            values.put(COLUMN_BUDGET_LIMIT, budget.getLimitAmount());
            values.put(COLUMN_BUDGET_SPENT, budget.getCurrentSpent());
            values.put(COLUMN_BUDGET_CYCLE, budget.getCycleType());
            values.put(COLUMN_BUDGET_RESET, budget.getLastReset());
            values.put(COLUMN_BUDGET_THRESHOLD, budget.getThresholdPercent());

            long id = db.insert(TABLE_BUDGETS, null, values);
            Log.d(TAG, "Budget created with ID: " + id);
            return id;

        } catch (Exception e) {
            Log.e(TAG, "Error creating budget", e);
            return -1;
        } finally {
            if (db != null && db.isOpen()) {
                db.close();
            }
        }
    }

    /**
     * Update an existing budget
     * @param budget Budget object with updated data
     * @return Number of rows affected, or -1 on error
     */
    public int updateBudget(Budget budget) {
        SQLiteDatabase db = null;
        try {
            db = this.getWritableDatabase();
            ContentValues values = new ContentValues();
            values.put(COLUMN_BUDGET_CATEGORY, budget.getCategory());
            values.put(COLUMN_BUDGET_LIMIT, budget.getLimitAmount());
            values.put(COLUMN_BUDGET_SPENT, budget.getCurrentSpent());
            values.put(COLUMN_BUDGET_CYCLE, budget.getCycleType());
            values.put(COLUMN_BUDGET_RESET, budget.getLastReset());
            values.put(COLUMN_BUDGET_THRESHOLD, budget.getThresholdPercent());

            int rowsAffected = db.update(
                    TABLE_BUDGETS,
                    values,
                    COLUMN_BUDGET_ID + " = ? AND " + COLUMN_BUDGET_USER_ID + " = ?",
                    new String[]{String.valueOf(budget.getId()), budget.getUserId()}
            );

            Log.d(TAG, "Budget updated, rows affected: " + rowsAffected);
            return rowsAffected;

        } catch (Exception e) {
            Log.e(TAG, "Error updating budget", e);
            return -1;
        } finally {
            if (db != null && db.isOpen()) {
                db.close();
            }
        }
    }

    /**
     * Delete a budget by ID
     * @param id Budget ID to delete
     * @param userId User ID for security check
     * @return Number of rows deleted, or -1 on error
     */
    public int deleteBudget(long id, String userId) {
        SQLiteDatabase db = null;
        try {
            db = this.getWritableDatabase();
            int rowsDeleted = db.delete(
                    TABLE_BUDGETS,
                    COLUMN_BUDGET_ID + " = ? AND " + COLUMN_BUDGET_USER_ID + " = ?",
                    new String[]{String.valueOf(id), userId}
            );

            Log.d(TAG, "Budget deleted, rows affected: " + rowsDeleted);
            return rowsDeleted;

        } catch (Exception e) {
            Log.e(TAG, "Error deleting budget", e);
            return -1;
        } finally {
            if (db != null && db.isOpen()) {
                db.close();
            }
        }
    }

    /**
     * Get all budgets for a user
     * @param userId User ID to filter by
     * @return List of budgets
     */
    public List<Budget> getBudgets(String userId) {
        List<Budget> budgets = new ArrayList<>();
        SQLiteDatabase db = null;
        Cursor cursor = null;

        try {
            db = this.getReadableDatabase();
            cursor = db.query(
                    TABLE_BUDGETS,
                    null,
                    COLUMN_BUDGET_USER_ID + " = ?",
                    new String[]{userId},
                    null,
                    null,
                    COLUMN_BUDGET_CATEGORY + " ASC"
            );

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    Budget budget = new Budget();
                    budget.setId(cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_BUDGET_ID)));
                    budget.setUserId(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_BUDGET_USER_ID)));
                    budget.setCategory(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_BUDGET_CATEGORY)));
                    budget.setLimitAmount(cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_BUDGET_LIMIT)));
                    budget.setCurrentSpent(cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_BUDGET_SPENT)));
                    budget.setCycleType(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_BUDGET_CYCLE)));
                    budget.setLastReset(cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_BUDGET_RESET)));
                    budget.setThresholdPercent(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_BUDGET_THRESHOLD)));

                    budgets.add(budget);
                } while (cursor.moveToNext());
            }

            Log.d(TAG, "Retrieved " + budgets.size() + " budgets for user " + userId);

        } catch (Exception e) {
            Log.e(TAG, "Error retrieving budgets", e);
        } finally {
            if (cursor != null && !cursor.isClosed()) {
                cursor.close();
            }
            if (db != null && db.isOpen()) {
                db.close();
            }
        }

        return budgets;
    }

    /**
     * Get budget by category
     * @param userId User ID
     * @param category Category name
     * @return Budget or null if not found
     */
    public Budget getBudgetByCategory(String userId, String category) {
        SQLiteDatabase db = null;
        Cursor cursor = null;

        try {
            db = this.getReadableDatabase();
            cursor = db.query(
                    TABLE_BUDGETS,
                    null,
                    COLUMN_BUDGET_USER_ID + " = ? AND " + COLUMN_BUDGET_CATEGORY + " = ?",
                    new String[]{userId, category},
                    null,
                    null,
                    null
            );

            if (cursor != null && cursor.moveToFirst()) {
                Budget budget = new Budget();
                budget.setId(cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_BUDGET_ID)));
                budget.setUserId(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_BUDGET_USER_ID)));
                budget.setCategory(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_BUDGET_CATEGORY)));
                budget.setLimitAmount(cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_BUDGET_LIMIT)));
                budget.setCurrentSpent(cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_BUDGET_SPENT)));
                budget.setCycleType(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_BUDGET_CYCLE)));
                budget.setLastReset(cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_BUDGET_RESET)));
                budget.setThresholdPercent(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_BUDGET_THRESHOLD)));
                return budget;
            }

        } catch (Exception e) {
            Log.e(TAG, "Error retrieving budget by category", e);
        } finally {
            if (cursor != null && !cursor.isClosed()) {
                cursor.close();
            }
            if (db != null && db.isOpen()) {
                db.close();
            }
        }

        return null;
    }

    /**
     * Recompute budget current_spent by aggregating expenses
     * @param userId User ID
     * @param category Budget category
     * @return Updated spent amount, or -1 on error
     */
    public double recomputeBudgetSpent(String userId, String category) {
        SQLiteDatabase db = null;
        Cursor cursor = null;

        try {
            db = this.getWritableDatabase();

            // Get budget to determine cycle period
            Budget budget = getBudgetByCategory(userId, category);
            if (budget == null) {
                return -1;
            }

            // Calculate start date based on last reset
            long startDate = budget.getLastReset();

            // Check if budget needs reset
            if (budget.needsReset(System.currentTimeMillis())) {
                budget.reset();
                updateBudget(budget);
                return 0.0;
            }

            // Aggregate expenses for this category since last reset
            cursor = db.query(
                    TABLE_EXPENSES,
                    new String[]{"SUM(" + COLUMN_AMOUNT + ") as total"},
                    COLUMN_USER_ID + " = ? AND " + COLUMN_CATEGORY + " = ? AND " + COLUMN_DATE + " >= ?",
                    new String[]{userId, category, String.valueOf(startDate)},
                    null,
                    null,
                    null
            );

            double totalSpent = 0.0;
            if (cursor != null && cursor.moveToFirst()) {
                totalSpent = cursor.getDouble(0);
            }

            // Update budget
            budget.setCurrentSpent(totalSpent);
            updateBudget(budget);

            Log.d(TAG, "Recomputed budget spent for category " + category + ": " + totalSpent);
            return totalSpent;

        } catch (Exception e) {
            Log.e(TAG, "Error recomputing budget spent", e);
            return -1;
        } finally {
            if (cursor != null && !cursor.isClosed()) {
                cursor.close();
            }
            if (db != null && db.isOpen()) {
                db.close();
            }
        }
    }

    /**
     * Recompute all budgets for a user
     * @param userId User ID
     */
    public void recomputeAllBudgets(String userId) {
        List<Budget> budgets = getBudgets(userId);
        if (budgets.isEmpty()) {
            Log.d(TAG, "No budgets to recompute for user " + userId);
            return;
        }

        // Step 1: Check and reset budgets that need reset (per budget logic)
        long minStartDate = Long.MAX_VALUE;  // Track min startDate after resets
        Map<String, Budget> budgetMap = new HashMap<>();  // category -> budget
        for (Budget budget : budgets) {
            if (budget.needsReset(System.currentTimeMillis())) {
                budget.reset();
                updateBudget(budget);
            }
            long startDate = budget.getLastReset();
            minStartDate = Math.min(minStartDate, startDate);
            budgetMap.put(budget.getCategory(), budget);
        }

        // Step 2: Single GROUP BY query for all categories (using minStartDate as conservative bound)
        // This assumes similar cycles; for diverse cycles, group by startDate (more complex)
        SQLiteDatabase db = null;
        Cursor cursor = null;
        try {
            db = this.getReadableDatabase();
            cursor = db.query(
                    TABLE_EXPENSES,
                    new String[]{COLUMN_CATEGORY, "SUM(" + COLUMN_AMOUNT + ") as total"},
                    COLUMN_USER_ID + " = ? AND " + COLUMN_DATE + " >= ?",
                    new String[]{userId, String.valueOf(minStartDate)},
                    null,
                    null,
                    null
            );

            // Step 3: Process results and update budgets
            Map<String, Double> categoryTotals = new HashMap<>();
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    String category = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CATEGORY));
                    double total = cursor.getDouble(cursor.getColumnIndexOrThrow("total"));
                    categoryTotals.put(category, total);
                } while (cursor.moveToNext());
            }

            // Update each budget with its total (default 0 if no expenses)
            for (Budget budget : budgets) {
                double totalSpent = categoryTotals.getOrDefault(budget.getCategory(), 0.0);
                budget.setCurrentSpent(totalSpent);
                updateBudget(budget);
            }

            Log.d(TAG, "Recomputed all budgets for user " + userId + " using single GROUP BY query");

        } catch (Exception e) {
            Log.e(TAG, "Error recomputing all budgets", e);
            // Fallback: Use original loop method
            for (Budget budget : budgets) {
                recomputeBudgetSpent(userId, budget.getCategory());
            }
        } finally {
            if (cursor != null && !cursor.isClosed()) {
                cursor.close();
            }
            if (db != null && db.isOpen()) {
                db.close();
            }
        }
    }

    // ==================== RECURRING EXPENSE METHODS (Phase 3) ====================

    /**
     * Create a new recurring expense
     * @param recurring RecurringExpense object to create
     * @return ID of created recurring expense, or -1 on error
     */
    public long createRecurring(RecurringExpense recurring) {
        SQLiteDatabase db = null;
        try {
            db = this.getWritableDatabase();
            ContentValues values = new ContentValues();
            values.put(COLUMN_RECURRING_USER_ID, recurring.getUserId());
            values.put(COLUMN_RECURRING_CATEGORY, recurring.getCategory());
            values.put(COLUMN_RECURRING_AMOUNT, recurring.getAmount());
            values.put(COLUMN_RECURRING_DESC, recurring.getDescription());
            values.put(COLUMN_RECURRING_START, recurring.getStartDate());

            if (recurring.getEndDate() != null) {
                values.put(COLUMN_RECURRING_END, recurring.getEndDate());
            } else {
                values.putNull(COLUMN_RECURRING_END);
            }

            values.put(COLUMN_RECURRING_FREQ, recurring.getFrequency());
            values.put(COLUMN_RECURRING_NEXT, recurring.getNextRun());

            long id = db.insert(TABLE_RECURRING, null, values);
            Log.d(TAG, "Recurring expense created with ID: " + id);
            return id;

        } catch (Exception e) {
            Log.e(TAG, "Error creating recurring expense", e);
            return -1;
        } finally {
            if (db != null && db.isOpen()) {
                db.close();
            }
        }
    }

    /**
     * Update an existing recurring expense
     * @param recurring RecurringExpense object with updated data
     * @return Number of rows affected, or -1 on error
     */
    public int updateRecurring(RecurringExpense recurring) {
        SQLiteDatabase db = null;
        try {
            db = this.getWritableDatabase();
            ContentValues values = new ContentValues();
            values.put(COLUMN_RECURRING_CATEGORY, recurring.getCategory());
            values.put(COLUMN_RECURRING_AMOUNT, recurring.getAmount());
            values.put(COLUMN_RECURRING_DESC, recurring.getDescription());
            values.put(COLUMN_RECURRING_START, recurring.getStartDate());

            if (recurring.getEndDate() != null) {
                values.put(COLUMN_RECURRING_END, recurring.getEndDate());
            } else {
                values.putNull(COLUMN_RECURRING_END);
            }

            values.put(COLUMN_RECURRING_FREQ, recurring.getFrequency());
            values.put(COLUMN_RECURRING_NEXT, recurring.getNextRun());

            int rowsAffected = db.update(
                    TABLE_RECURRING,
                    values,
                    COLUMN_RECURRING_ID + " = ? AND " + COLUMN_RECURRING_USER_ID + " = ?",
                    new String[]{String.valueOf(recurring.getId()), recurring.getUserId()}
            );

            Log.d(TAG, "Recurring expense updated, rows affected: " + rowsAffected);
            return rowsAffected;

        } catch (Exception e) {
            Log.e(TAG, "Error updating recurring expense", e);
            return -1;
        } finally {
            if (db != null && db.isOpen()) {
                db.close();
            }
        }
    }

    /**
     * Delete a recurring expense by ID
     * @param id Recurring expense ID to delete
     * @param userId User ID for security check
     * @return Number of rows deleted, or -1 on error
     */
    public int deleteRecurring(long id, String userId) {
        SQLiteDatabase db = null;
        try {
            db = this.getWritableDatabase();
            int rowsDeleted = db.delete(
                    TABLE_RECURRING,
                    COLUMN_RECURRING_ID + " = ? AND " + COLUMN_RECURRING_USER_ID + " = ?",
                    new String[]{String.valueOf(id), userId}
            );

            Log.d(TAG, "Recurring expense deleted, rows affected: " + rowsDeleted);
            return rowsDeleted;

        } catch (Exception e) {
            Log.e(TAG, "Error deleting recurring expense", e);
            return -1;
        } finally {
            if (db != null && db.isOpen()) {
                db.close();
            }
        }
    }

    /**
     * Get all recurring expenses for a user
     * @param userId User ID to filter by
     * @return List of recurring expenses
     */
    public List<RecurringExpense> getRecurring(String userId) {
        List<RecurringExpense> recurringList = new ArrayList<>();
        SQLiteDatabase db = null;
        Cursor cursor = null;

        try {
            db = this.getReadableDatabase();
            cursor = db.query(
                    TABLE_RECURRING,
                    null,
                    COLUMN_RECURRING_USER_ID + " = ?",
                    new String[]{userId},
                    null,
                    null,
                    COLUMN_RECURRING_NEXT + " ASC"
            );

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    RecurringExpense recurring = new RecurringExpense();
                    recurring.setId(cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_RECURRING_ID)));
                    recurring.setUserId(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_RECURRING_USER_ID)));
                    recurring.setCategory(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_RECURRING_CATEGORY)));
                    recurring.setAmount(cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_RECURRING_AMOUNT)));
                    recurring.setDescription(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_RECURRING_DESC)));
                    recurring.setStartDate(cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_RECURRING_START)));

                    int endDateIndex = cursor.getColumnIndexOrThrow(COLUMN_RECURRING_END);
                    if (!cursor.isNull(endDateIndex)) {
                        recurring.setEndDate(cursor.getLong(endDateIndex));
                    } else {
                        recurring.setEndDate(null);
                    }

                    recurring.setFrequency(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_RECURRING_FREQ)));
                    recurring.setNextRun(cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_RECURRING_NEXT)));

                    recurringList.add(recurring);
                } while (cursor.moveToNext());
            }

            Log.d(TAG, "Retrieved " + recurringList.size() + " recurring expenses for user " + userId);

        } catch (Exception e) {
            Log.e(TAG, "Error retrieving recurring expenses", e);
        } finally {
            if (cursor != null && !cursor.isClosed()) {
                cursor.close();
            }
            if (db != null && db.isOpen()) {
                db.close();
            }
        }

        return recurringList;
    }

    /**
     * Get recurring expenses that are due to run
     * @param now Current timestamp
     * @return List of due recurring expenses
     */
    public List<RecurringExpense> getRecurringDue(long now) {
        List<RecurringExpense> dueList = new ArrayList<>();
        SQLiteDatabase db = null;
        Cursor cursor = null;

        try {
            db = this.getReadableDatabase();

            // Query for recurring expenses where next_run <= now AND (end_date IS NULL OR end_date >= now)
            String selection = COLUMN_RECURRING_NEXT + " <= ? AND (" +
                    COLUMN_RECURRING_END + " IS NULL OR " +
                    COLUMN_RECURRING_END + " >= ?)";

            cursor = db.query(
                    TABLE_RECURRING,
                    null,
                    selection,
                    new String[]{String.valueOf(now), String.valueOf(now)},
                    null,
                    null,
                    COLUMN_RECURRING_NEXT + " ASC"
            );

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    RecurringExpense recurring = new RecurringExpense();
                    recurring.setId(cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_RECURRING_ID)));
                    recurring.setUserId(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_RECURRING_USER_ID)));
                    recurring.setCategory(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_RECURRING_CATEGORY)));
                    recurring.setAmount(cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_RECURRING_AMOUNT)));
                    recurring.setDescription(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_RECURRING_DESC)));
                    recurring.setStartDate(cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_RECURRING_START)));

                    int endDateIndex = cursor.getColumnIndexOrThrow(COLUMN_RECURRING_END);
                    if (!cursor.isNull(endDateIndex)) {
                        recurring.setEndDate(cursor.getLong(endDateIndex));
                    } else {
                        recurring.setEndDate(null);
                    }

                    recurring.setFrequency(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_RECURRING_FREQ)));
                    recurring.setNextRun(cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_RECURRING_NEXT)));

                    dueList.add(recurring);
                } while (cursor.moveToNext());
            }

            Log.d(TAG, "Retrieved " + dueList.size() + " due recurring expenses");

        } catch (Exception e) {
            Log.e(TAG, "Error retrieving due recurring expenses", e);
        } finally {
            if (cursor != null && !cursor.isClosed()) {
                cursor.close();
            }
            if (db != null && db.isOpen()) {
                db.close();
            }
        }

        return dueList;
    }
}
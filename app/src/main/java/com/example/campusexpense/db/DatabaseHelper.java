package com.example.campusexpense.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import com.example.campusexpense.model.Expense;
import java.util.ArrayList;
import java.util.List;

/**
 * DatabaseHelper - SQLite database manager
 * Phase 2: Expense CRUD operations
 *
 * Features:
 * - Expense table creation and management
 * - CRUD operations with error handling
 * - Query filtering by date range and category
 * - User-specific data isolation
 *
 * Security: No sensitive data in logs, user isolation via userId column
 */
public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String TAG = "DatabaseHelper";

    // Database info
    private static final String DATABASE_NAME = "campusexpense.db";
    private static final int DATABASE_VERSION = 1;

    // Table names
    private static final String TABLE_EXPENSES = "expenses";

    // Expense table columns
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_USER_ID = "user_id";
    private static final String COLUMN_CATEGORY = "category";
    private static final String COLUMN_DESCRIPTION = "description";
    private static final String COLUMN_AMOUNT = "amount";
    private static final String COLUMN_DATE = "date";
    private static final String COLUMN_NOTES = "notes";

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

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        try {
            db.execSQL(CREATE_EXPENSES_TABLE);
            Log.d(TAG, "Database tables created successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error creating database tables", e);
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        try {
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_EXPENSES);
            onCreate(db);
            Log.d(TAG, "Database upgraded from version " + oldVersion + " to " + newVersion);
        } catch (Exception e) {
            Log.e(TAG, "Error upgrading database", e);
        }
    }

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
}
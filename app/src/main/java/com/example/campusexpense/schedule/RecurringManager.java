package com.example.campusexpense.schedule;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.campusexpense.R;
import com.example.campusexpense.db.DatabaseHelper;
import com.example.campusexpense.model.Expense;
import com.example.campusexpense.model.RecurringExpense;
import com.example.campusexpense.notifications.NotificationHelper;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * RecurringManager - Manages recurring expense generation
 * Phase 3: WorkManager-based scheduling
 * <p>
 * Features:
 * - Periodic check for due recurring expenses
 * - Automatic expense insertion
 * - Next run calculation and update
 * - End date handling
 * - Testing hook for manual execution
 * <p>
 * Choice: WorkManager (preferred over AlarmManager)
 * Rationale:
 * - Battery-efficient with doze mode support
 * - Survives app restarts
 * - No need for exact timing (daily check is sufficient)
 * - Built-in retry and backoff
 * - Easier testing and debugging
 * <p>
 * Security: No sensitive data in logs, user-isolated operations
 */
public class RecurringManager {

    private static final String TAG = "RecurringManager";
    private static final String WORK_NAME = "RecurringExpenseWork";

    private final Context context;

    public RecurringManager(Context context) {
        this.context = context.getApplicationContext();
    }

    /**
     * Schedule periodic recurring expense checks
     * Runs once per day to process due recurring expenses
     */
    public void schedulePeriodicCheck() {
        try {
            Constraints constraints = new Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.NOT_REQUIRED) // Offline-first
                    .build();

            PeriodicWorkRequest workRequest = new PeriodicWorkRequest.Builder(
                    RecurringWorker.class,
                    24, // Repeat interval
                    TimeUnit.HOURS,
                    15, // Flex interval
                    TimeUnit.MINUTES
            )
                    .setConstraints(constraints)
                    .build();

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                    WORK_NAME,
                    ExistingPeriodicWorkPolicy.KEEP, // Don't restart if already scheduled
                    workRequest
            );

            Log.d(TAG, "Recurring expense check scheduled");

        } catch (Exception e) {
            Log.e(TAG, "Error scheduling recurring check", e);
        }
    }

    /**
     * Cancel scheduled recurring checks
     */
    public void cancelPeriodicCheck() {
        try {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME);
            Log.d(TAG, "Recurring expense check cancelled");
        } catch (Exception e) {
            Log.e(TAG, "Error cancelling recurring check", e);
        }
    }

    /**
     * Run recurring expense check immediately (for testing)
     * This method provides a testing hook to manually trigger the worker logic
     */
    public void runNowForTesting() {
        try {
            long now = System.currentTimeMillis();
            int processedCount = processRecurringExpenses(context, now);

            String message = context.getString(R.string.recurring_processed, processedCount);
            Toast.makeText(context, message, Toast.LENGTH_LONG).show();
            Log.d(TAG, message);

        } catch (Exception e) {
            Log.e(TAG, "Error in manual recurring check", e);
            Toast.makeText(context, "Error processing recurring expenses", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Process all due recurring expenses
     * @param context Application context
     * @param now Current timestamp
     * @return Number of expenses processed
     */
    static int processRecurringExpenses(Context context, long now) {
        DatabaseHelper dbHelper = null;
        int processedCount = 0;

        try {
            dbHelper = new DatabaseHelper(context);

            // Get all due recurring expenses
            List<RecurringExpense> dueRecurring = dbHelper.getRecurringDue(now);

            Log.d(TAG, "Found " + dueRecurring.size() + " due recurring expenses");

            for (RecurringExpense recurring : dueRecurring) {
                try {
                    // Create expense from recurring
                    Expense expense = new Expense();
                    expense.setUserId(recurring.getUserId());
                    expense.setCategory(recurring.getCategory());
                    expense.setDescription(recurring.getDescription() + " (Recurring)");
                    expense.setAmount(recurring.getAmount());
                    expense.setDate(now);
                    expense.setNotes("Auto-generated from recurring expense");

                    // Insert expense
                    long expenseId = dbHelper.insertExpense(expense);

                    if (expenseId > 0) {
                        Log.d(TAG, "Inserted recurring expense: " + recurring.getDescription());

                        // Send notification
                        NotificationHelper.notifyRecurringInserted(
                                context,
                                recurring.getCategory(),
                                recurring.getDescription()
                        );

                        // Update next run
                        recurring.updateNextRun();
                        dbHelper.updateRecurring(recurring);

                        // Recompute budget for this category
                        dbHelper.recomputeBudgetSpent(recurring.getUserId(), recurring.getCategory());

                        processedCount++;
                    } else {
                        Log.e(TAG, "Failed to insert recurring expense: " + recurring.getId());
                    }

                } catch (Exception e) {
                    Log.e(TAG, "Error processing recurring expense " + recurring.getId(), e);
                }
            }

            Log.d(TAG, "Processed " + processedCount + " recurring expenses");

        } catch (Exception e) {
            Log.e(TAG, "Error in processRecurringExpenses", e);
        } finally {
            if (dbHelper != null) {
                dbHelper.close();
            }
        }

        return processedCount;
    }

    /**
     * RecurringWorker - WorkManager worker for periodic execution
     */
    public static class RecurringWorker extends Worker {

        public RecurringWorker(@NonNull Context context, @NonNull WorkerParameters params) {
            super(context, params);
        }

        @NonNull
        @Override
        public Result doWork() {
            try {
                long now = System.currentTimeMillis();
                int processedCount = processRecurringExpenses(getApplicationContext(), now);

                Log.d(TAG, "RecurringWorker completed: " + processedCount + " expenses processed");
                return Result.success();

            } catch (Exception e) {
                Log.e(TAG, "RecurringWorker failed", e);
                return Result.retry();
            }
        }
    }
}
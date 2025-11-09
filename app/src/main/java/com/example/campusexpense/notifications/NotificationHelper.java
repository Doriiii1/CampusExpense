package com.example.campusexpense.notifications;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import com.example.campusexpense.R;
import com.example.campusexpense.ui.BudgetActivity;

/**
 * NotificationHelper - Manages local notifications
 * Phase 3: Budget threshold and recurring expense notifications
 *
 * Features:
 * - Budget threshold warnings
 * - Recurring expense insertion notifications
 * - Notification channels for API 26+
 * - Generic, non-sensitive content
 *
 * Security: No sensitive data exposed in notifications
 */
public class NotificationHelper {

    private static final String TAG = "NotificationHelper";

    // Notification channels
    private static final String CHANNEL_BUDGET = "budget_channel";
    private static final String CHANNEL_RECURRING = "recurring_channel";

    // Notification IDs
    private static final int NOTIFICATION_BUDGET_BASE = 1000;
    private static final int NOTIFICATION_RECURRING_BASE = 2000;

    /**
     * Create notification channels (API 26+)
     * Must be called before sending notifications
     */
    public static void createNotificationChannels(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager manager = context.getSystemService(NotificationManager.class);

            if (manager != null) {
                // Budget channel
                NotificationChannel budgetChannel = new NotificationChannel(
                        CHANNEL_BUDGET,
                        "Budget Alerts",
                        NotificationManager.IMPORTANCE_DEFAULT
                );
                budgetChannel.setDescription("Notifications for budget thresholds and limits");
                manager.createNotificationChannel(budgetChannel);

                // Recurring channel
                NotificationChannel recurringChannel = new NotificationChannel(
                        CHANNEL_RECURRING,
                        "Recurring Expenses",
                        NotificationManager.IMPORTANCE_LOW
                );
                recurringChannel.setDescription("Notifications for automatically added recurring expenses");
                manager.createNotificationChannel(recurringChannel);
            }
        }
    }

    /**
     * Send notification when budget reaches threshold
     * @param context Application context
     * @param category Budget category
     * @param spent Current spent amount
     * @param limit Budget limit
     */
    public static void notifyBudgetThreshold(Context context, String category, double spent, double limit) {
        // Check notification permission for API 33+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context,
                    Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                // Skip notification if permission not granted (request in activity if needed)
                android.util.Log.w(TAG, "POST_NOTIFICATIONS permission not granted, skipping budget notification");
                return;
            }
        }

        try {
            int progress = (int) ((spent / limit) * 100);

            // Create intent to open BudgetActivity
            Intent intent = new Intent(context, BudgetActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(
                    context,
                    0,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            // Build notification
            String title = context.getString(R.string.budget_warning);
            String message = String.format(
                    context.getString(R.string.budget_threshold_message),
                    category,
                    progress
            );

            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_BUDGET)
                    .setSmallIcon(R.drawable.ic_budget)
                    .setContentTitle(title)
                    .setContentText(message)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true);

            // Show notification
            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager != null) {
                int notificationId = NOTIFICATION_BUDGET_BASE + category.hashCode();
                manager.notify(notificationId, builder.build());
            }

        } catch (Exception e) {
            android.util.Log.e(TAG, "Error sending budget notification", e);
        }
    }

    /**
     * Send notification when recurring expense is inserted
     * @param context Application context
     * @param category Expense category
     * @param description Expense description
     */
    public static void notifyRecurringInserted(Context context, String category, String description) {
        // Check notification permission for API 33+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context,
                    Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                // Skip notification if permission not granted (request in activity if needed)
                android.util.Log.w(TAG, "POST_NOTIFICATIONS permission not granted, skipping recurring notification");
                return;
            }
        }

        try {
            // Create intent to open app (generic)
            Intent intent = context.getPackageManager().getLaunchIntentForPackage(context.getPackageName());
            PendingIntent pendingIntent = PendingIntent.getActivity(
                    context,
                    0,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            // Build notification
            String title = context.getString(R.string.recurring_expense_added);
            String message = String.format(
                    context.getString(R.string.recurring_expense_message),
                    category
            );

            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_RECURRING)
                    .setSmallIcon(R.drawable.ic_budget)
                    .setContentTitle(title)
                    .setContentText(message)
                    .setStyle(new NotificationCompat.BigTextStyle().bigText(description))
                    .setPriority(NotificationCompat.PRIORITY_LOW)
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true);

            // Show notification
            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager != null) {
                int notificationId = NOTIFICATION_RECURRING_BASE + (int) System.currentTimeMillis();
                manager.notify(notificationId, builder.build());
            }

        } catch (Exception e) {
            android.util.Log.e(TAG, "Error sending recurring notification", e);
        }
    }
}
package com.example.campusexpense.test;

import android.content.Context;
import com.example.campusexpense.db.DatabaseHelper;
import com.example.campusexpense.notifications.NotificationHelper;

/**
 * TestAppInjector - Test dependency injector
 * Phase 6: Testing
 *
 * Allows tests to inject mock/test implementations of:
 * - DatabaseHelper
 * - NotificationHelper
 * - Current time (for deterministic tests)
 *
 * Production code can check for test overrides via static getters.
 * Only active when explicitly set by tests.
 */
public class TestAppInjector {

    private static DatabaseHelper testDatabaseHelper = null;
    private static NotificationHelper testNotificationHelper = null;
    private static Long overrideNowMillis = null;
    private static boolean testMode = false;

    /**
     * Set test mode - enables all test overrides
     */
    public static void setTestMode(boolean enabled) {
        testMode = enabled;
    }

    /**
     * Check if in test mode
     */
    public static boolean isTestMode() {
        return testMode;
    }

    /**
     * Override DatabaseHelper for tests
     * @param dbHelper Test database helper instance
     */
    public static void setDatabaseHelper(DatabaseHelper dbHelper) {
        testDatabaseHelper = dbHelper;
        testMode = true;
    }

    /**
     * Get DatabaseHelper - returns test instance if set, otherwise creates production instance
     * @param context Application context
     * @return DatabaseHelper instance
     */
    public static DatabaseHelper getDatabaseHelper(Context context) {
        if (testMode && testDatabaseHelper != null) {
            return testDatabaseHelper;
        }
        return DatabaseHelper.getInstance(context);
    }

    /**
     * Override NotificationHelper for tests
     * @param notificationHelper Test notification helper instance
     */
    public static void setNotificationHelper(NotificationHelper notificationHelper) {
        testNotificationHelper = notificationHelper;
        testMode = true;
    }

    /**
     * Get NotificationHelper - returns test instance if set, otherwise null
     * @return NotificationHelper instance or null
     */
    public static NotificationHelper getNotificationHelper() {
        if (testMode && testNotificationHelper != null) {
            return testNotificationHelper;
        }
        return null;
    }

    /**
     * Override current time for deterministic tests
     * @param millis Milliseconds since epoch
     */
    public static void setNowMillis(long millis) {
        overrideNowMillis = millis;
        testMode = true;
    }

    /**
     * Get current time - returns override if set, otherwise system time
     * @return Current time in milliseconds
     */
    public static long getNowMillis() {
        if (testMode && overrideNowMillis != null) {
            return overrideNowMillis;
        }
        return System.currentTimeMillis();
    }

    /**
     * Reset all test overrides
     */
    public static void reset() {
        testDatabaseHelper = null;
        testNotificationHelper = null;
        overrideNowMillis = null;
        testMode = false;
    }

    /**
     * Check if DatabaseHelper is overridden
     */
    public static boolean hasDatabaseHelperOverride() {
        return testMode && testDatabaseHelper != null;
    }

    /**
     * Check if NotificationHelper is overridden
     */
    public static boolean hasNotificationHelperOverride() {
        return testMode && testNotificationHelper != null;
    }

    /**
     * Check if time is overridden
     */
    public static boolean hasTimeOverride() {
        return testMode && overrideNowMillis != null;
    }
}
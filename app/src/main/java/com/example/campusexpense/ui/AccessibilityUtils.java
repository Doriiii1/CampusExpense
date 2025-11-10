package com.example.campusexpense.ui;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;

/**
 * AccessibilityUtils - Accessibility helpers and announcements
 * Phase 5: UI Polish and Accessibility
 *
 * Features:
 * - Accessibility announcements for screen readers
 * - Safe content description setting
 * - Accessibility state checking
 *
 * All methods are safe and handle null inputs gracefully
 */
public class AccessibilityUtils {

    private static final String TAG = "AccessibilityUtils";

    /**
     * Announce message for accessibility services (TalkBack, etc.)
     * @param activity Activity context
     * @param message Message to announce
     */
    public static void announceForAccessibility(Activity activity, String message) {
        if (activity == null || message == null || message.isEmpty()) {
            return;
        }

        try {
            View rootView = activity.getWindow().getDecorView().getRootView();
            if (rootView != null) {
                rootView.announceForAccessibility(message);
                Log.d(TAG, "Announced for accessibility: " + message);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error announcing for accessibility", e);
        }
    }

    /**
     * Safely set content description on a view
     * @param view View to modify
     * @param description Content description
     */
    public static void safeSetContentDescription(View view, String description) {
        if (view == null) {
            return;
        }

        try {
            if (description != null && !description.isEmpty()) {
                view.setContentDescription(description);
                Log.d(TAG, "Set content description: " + description);
            } else {
                // Mark as not important for accessibility if no description
                view.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error setting content description", e);
        }
    }

    /**
     * Check if accessibility services are enabled
     * @param context Context
     * @return true if accessibility services are enabled
     */
    public static boolean isAccessibilityEnabled(Context context) {
        if (context == null) {
            return false;
        }

        try {
            AccessibilityManager am = (AccessibilityManager)
                    context.getSystemService(Context.ACCESSIBILITY_SERVICE);

            if (am != null) {
                boolean enabled = am.isEnabled();
                Log.d(TAG, "Accessibility enabled: " + enabled);
                return enabled;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error checking accessibility state", e);
        }

        return false;
    }

    /**
     * Check if touch exploration (TalkBack) is enabled
     * @param context Context
     * @return true if touch exploration is enabled
     */
    public static boolean isTouchExplorationEnabled(Context context) {
        if (context == null) {
            return false;
        }

        try {
            AccessibilityManager am = (AccessibilityManager)
                    context.getSystemService(Context.ACCESSIBILITY_SERVICE);

            if (am != null) {
                boolean enabled = am.isTouchExplorationEnabled();
                Log.d(TAG, "Touch exploration enabled: " + enabled);
                return enabled;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error checking touch exploration state", e);
        }

        return false;
    }

    /**
     * Send accessibility event
     * @param view View to send event from
     * @param eventType Event type (AccessibilityEvent.TYPE_*)
     */
    public static void sendAccessibilityEvent(View view, int eventType) {
        if (view == null) {
            return;
        }

        try {
            view.sendAccessibilityEvent(eventType);
            Log.d(TAG, "Sent accessibility event: " + eventType);
        } catch (Exception e) {
            Log.e(TAG, "Error sending accessibility event", e);
        }
    }

    /**
     * Request focus for accessibility
     * @param view View to focus
     */
    public static void requestAccessibilityFocus(View view) {
        if (view == null) {
            return;
        }

        try {
            view.performAccessibilityAction(
                    AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED,
                    null
            );
            Log.d(TAG, "Requested accessibility focus");
        } catch (Exception e) {
            Log.e(TAG, "Error requesting accessibility focus", e);
        }
    }

    /**
     * Mark view as heading for accessibility
     * Useful for section headers in lists
     * @param view View to mark as heading
     */
    public static void setAccessibilityHeading(View view) {
        if (view == null) {
            return;
        }

        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                view.setAccessibilityHeading(true);
                Log.d(TAG, "Set accessibility heading");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error setting accessibility heading", e);
        }
    }
}
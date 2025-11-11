package com.example.campusexpense.ui;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import androidx.core.content.ContextCompat;

import com.example.campusexpense.R;
import com.github.mikephil.charting.BuildConfig;

/**
 * UiUtils - UI styling and measurement utilities
 * Phase 5: UI Polish and Accessibility
 *
 * Features:
 * - DP to PX conversion
 * - Minimum touch target enforcement
 * - Button styling helpers
 * - Performance diagnostics (DEBUG only)
 *
 * No heavy operations - all methods are lightweight UI helpers
 */
public class UiUtils {

    private static final String TAG = "UiUtils";
    private static final int MIN_TOUCH_TARGET_DP = 48;

    /**
     * Convert DP to pixels
     * @param context Context
     * @param dp DP value
     * @return Pixels
     */
    public static int dpToPx(Context context, float dp) {
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        return Math.round(dp * metrics.density);
    }

    /**
     * Convert pixels to DP
     * @param context Context
     * @param px Pixel value
     * @return DP
     */
    public static float pxToDp(Context context, int px) {
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        return px / metrics.density;
    }

    /**
     * Ensure view meets minimum touch target size (48dp)
     * @param view View to check and adjust
     */
    public static void ensureMinTouchSize(View view) {
        if (view == null) {
            return;
        }

        try {
            Context context = view.getContext();
            int minSizePx = dpToPx(context, MIN_TOUCH_TARGET_DP);

            ViewGroup.LayoutParams params = view.getLayoutParams();
            if (params != null) {
                boolean changed = false;

                if (params.width > 0 && params.width < minSizePx) {
                    params.width = minSizePx;
                    changed = true;
                }

                if (params.height > 0 && params.height < minSizePx) {
                    params.height = minSizePx;
                    changed = true;
                }

                if (changed) {
                    view.setLayoutParams(params);
                    Log.d(TAG, "Adjusted view to meet minimum touch target: " + view.getClass().getSimpleName());
                }
            }

            // Set minimum width/height as fallback
            view.setMinimumWidth(minSizePx);
            view.setMinimumHeight(minSizePx);

        } catch (Exception e) {
            Log.e(TAG, "Error ensuring minimum touch size", e);
        }
    }

    /**
     * Apply primary button styling with rounded background
     * @param button Button to style
     */
    public static void stylePrimaryButton(Button button) {
        if (button == null) {
            return;
        }

        try {
            Context context = button.getContext();

            // Apply rounded background
            button.setBackgroundResource(R.drawable.bg_button_rounded);

            // Ensure minimum touch target
            ensureMinTouchSize(button);

            // Apply text color
            button.setTextColor(ContextCompat.getColor(context, R.color.on_primary));

            // Add padding if needed
            int paddingPx = dpToPx(context, 16);
            if (button.getPaddingLeft() < paddingPx) {
                button.setPadding(paddingPx, button.getPaddingTop(), paddingPx, button.getPaddingBottom());
            }

            Log.d(TAG, "Styled primary button: " + button.getText());

        } catch (Exception e) {
            Log.e(TAG, "Error styling button", e);
        }
    }

    /**
     * Apply secondary button styling
     * @param button Button to style
     */
    public static void styleSecondaryButton(Button button) {
        if (button == null) {
            return;
        }

        try {
            Context context = button.getContext();

            // Ensure minimum touch target
            ensureMinTouchSize(button);

            // Apply text color
            button.setTextColor(ContextCompat.getColor(context, R.color.primary));

            Log.d(TAG, "Styled secondary button: " + button.getText());

        } catch (Exception e) {
            Log.e(TAG, "Error styling button", e);
        }
    }

    /**
     * Dump performance hints for debugging (DEBUG builds only)
     * @param activity Activity to analyze
     */
    public static void dumpPerformanceHints(Activity activity) {
        if (!BuildConfig.DEBUG) {
            return;
        }

        try {
            View rootView = activity.getWindow().getDecorView().getRootView();
            int[] viewCounts = countViews(rootView);

            Log.d(TAG, "=== Performance Hints ===");
            Log.d(TAG, "Total views: " + viewCounts[0]);
            Log.d(TAG, "Nested depth: " + viewCounts[1]);
            Log.d(TAG, "Activity: " + activity.getClass().getSimpleName());

            // Memory info
            Runtime runtime = Runtime.getRuntime();
            long usedMemory = (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024;
            long maxMemory = runtime.maxMemory() / 1024 / 1024;
            Log.d(TAG, "Memory: " + usedMemory + "MB / " + maxMemory + "MB");

            // Display metrics
            DisplayMetrics metrics = activity.getResources().getDisplayMetrics();
            Log.d(TAG, "Screen: " + metrics.widthPixels + "x" + metrics.heightPixels +
                    " (" + metrics.densityDpi + "dpi)");

            Log.d(TAG, "=========================");

        } catch (Exception e) {
            Log.e(TAG, "Error dumping performance hints", e);
        }
    }

    /**
     * Count views recursively
     * @param view Root view
     * @return [total count, max depth]
     */
    private static int[] countViews(View view) {
        return countViewsRecursive(view, 0);
    }

    private static int[] countViewsRecursive(View view, int depth) {
        int count = 1;
        int maxDepth = depth;

        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                int[] childResult = countViewsRecursive(group.getChildAt(i), depth + 1);
                count += childResult[0];
                maxDepth = Math.max(maxDepth, childResult[1]);
            }
        }

        return new int[]{count, maxDepth};
    }

    /**
     * Set view visibility safely
     * @param view View to modify
     * @param visibility View.VISIBLE, View.GONE, or View.INVISIBLE
     */
    public static void setVisibilitySafe(View view, int visibility) {
        if (view != null) {
            try {
                view.setVisibility(visibility);
            } catch (Exception e) {
                Log.e(TAG, "Error setting visibility", e);
            }
        }
    }

    /**
     * Get screen width in DP
     * @param context Context
     * @return Screen width in DP
     */
    public static float getScreenWidthDp(Context context) {
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        return metrics.widthPixels / metrics.density;
    }

    /**
     * Check if device is in landscape orientation
     * @param context Context
     * @return true if landscape
     */
    public static boolean isLandscape(Context context) {
        return context.getResources().getConfiguration().orientation ==
                android.content.res.Configuration.ORIENTATION_LANDSCAPE;
    }
}
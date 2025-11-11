package com.example.campusexpense.ui;

import android.content.Context;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import androidx.recyclerview.widget.RecyclerView;
import com.example.campusexpense.R;

/**
 * AnimatedRecyclerHelper - RecyclerView item animations controller
 * Phase 5: UI Polish and Accessibility
 *
 * Features:
 * - Optional fade-in animations for RecyclerView items
 * - Automatic disabling when accessibility services active
 * - Manual animation toggle
 * - Safe fallback if animation resource missing
 *
 * Animations respect system accessibility settings and can be manually disabled
 */
public class AnimatedRecyclerHelper {

    private static final String TAG = "AnimatedRecyclerHelper";

    private final Context context;
    private boolean animationsEnabled;
    private int lastPosition = -1;

    public AnimatedRecyclerHelper(Context context) {
        this.context = context.getApplicationContext();

        // Check if accessibility services are enabled
        boolean accessibilityEnabled = AccessibilityUtils.isAccessibilityEnabled(context);

        // Disable animations if accessibility is enabled
        this.animationsEnabled = !accessibilityEnabled;

        Log.d(TAG, "AnimatedRecyclerHelper initialized, animations enabled: " + animationsEnabled);
    }

    /**
     * Enable or disable animations
     * @param enabled true to enable animations
     */
    public void setAnimationsEnabled(boolean enabled) {
        this.animationsEnabled = enabled;
        Log.d(TAG, "Animations manually set to: " + enabled);
    }

    /**
     * Check if animations are enabled
     * @return true if animations are enabled
     */
    public boolean areAnimationsEnabled() {
        return animationsEnabled;
    }

    /**
     * Animate view when it appears in RecyclerView
     * @param view View to animate
     * @param position Item position
     */
    public void animateView(View view, int position) {
        if (!animationsEnabled || view == null) {
            return;
        }

        // Only animate new items that appear
        if (position > lastPosition) {
            try {
                // Try to load animation resource
                Animation animation = AnimationUtils.loadAnimation(context, R.anim.item_fade_in);
                view.startAnimation(animation);
                lastPosition = position;

            } catch (Exception e) {
                // If animation resource doesn't exist, disable animations
                Log.w(TAG, "Animation resource not found, disabling animations", e);
                animationsEnabled = false;
            }
        }
    }

    /**
     * Reset animation position counter
     * Call this when RecyclerView data changes significantly
     */
    public void reset() {
        lastPosition = -1;
        Log.d(TAG, "Animation position reset");
    }

    /**
     * Apply fade-in animation to view (manual)
     * @param view View to animate
     */
    public void applyFadeIn(View view) {
        if (!animationsEnabled || view == null) {
            return;
        }

        try {
            Animation animation = AnimationUtils.loadAnimation(context, R.anim.item_fade_in);
            view.startAnimation(animation);
        } catch (Exception e) {
            Log.w(TAG, "Error applying fade-in animation", e);
        }
    }

    /**
     * Create scroll listener for automatic animations
     * @return RecyclerView.OnScrollListener
     */
    public RecyclerView.OnScrollListener createScrollListener() {
        return new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);

                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    // Reset when scroll stops
                    reset();
                }
            }
        };
    }
}
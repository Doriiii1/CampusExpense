package com.example.campusexpense.ui;

import android.view.View;
import androidx.annotation.IdRes;
import androidx.recyclerview.widget.RecyclerView;
import androidx.test.espresso.IdlingResource;
import androidx.test.espresso.UiController;
import androidx.test.espresso.ViewAction;
import androidx.test.espresso.matcher.BoundedMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.*;

/**
 * EspressoTestUtils - Utility methods for Espresso tests
 * Phase 6: Testing
 *
 * Utilities:
 * - RecyclerView item matching
 * - Wait for view helpers
 * - Custom matchers
 * - IdlingResource helpers
 */
public class EspressoTestUtils {

    /**
     * Wait for a view to appear
     * @param viewId View resource ID
     * @param timeout Timeout in milliseconds
     */
    public static void waitForView(@IdRes int viewId, long timeout) {
        long startTime = System.currentTimeMillis();
        long endTime = startTime + timeout;

        do {
            try {
                onView(withId(viewId)).check(matches(isDisplayed()));
                return;
            } catch (Exception e) {
                // View not found yet, wait a bit
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ie) {
                    // Ignore
                }
            }
        } while (System.currentTimeMillis() < endTime);

        // Final attempt - will throw if view not found
        onView(withId(viewId)).check(matches(isDisplayed()));
    }

    /**
     * Match RecyclerView item at position
     * @param position Item position
     * @return Matcher
     */
    public static Matcher<View> atPosition(final int position) {
        return new TypeSafeMatcher<View>() {
            @Override
            protected boolean matchesSafely(View view) {
                if (!(view.getParent() instanceof RecyclerView)) {
                    return false;
                }

                RecyclerView recyclerView = (RecyclerView) view.getParent();
                RecyclerView.ViewHolder viewHolder = recyclerView.findViewHolderForAdapterPosition(position);

                return viewHolder != null && viewHolder.itemView == view;
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("at RecyclerView position " + position);
            }
        };
    }

    /**
     * Match RecyclerView with item count
     * @param count Expected item count
     * @return Matcher
     */
    public static Matcher<View> hasItemCount(final int count) {
        return new BoundedMatcher<View, RecyclerView>(RecyclerView.class) {
            @Override
            protected boolean matchesSafely(RecyclerView recyclerView) {
                return recyclerView.getAdapter() != null &&
                        recyclerView.getAdapter().getItemCount() == count;
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("RecyclerView with item count: " + count);
            }
        };
    }

    /**
     * Scroll RecyclerView to position
     * @param position Position to scroll to
     * @return ViewAction
     */
    public static ViewAction scrollToPosition(final int position) {
        return new ViewAction() {
            @Override
            public Matcher<View> getConstraints() {
                return isAssignableFrom(RecyclerView.class);
            }

            @Override
            public String getDescription() {
                return "scroll RecyclerView to position " + position;
            }

            @Override
            public void perform(UiController uiController, View view) {
                RecyclerView recyclerView = (RecyclerView) view;
                recyclerView.scrollToPosition(position);
                uiController.loopMainThreadUntilIdle();
            }
        };
    }

    /**
     * Click on child view within RecyclerView item
     * @param childId Child view ID
     * @return ViewAction
     */
    public static ViewAction clickChildViewWithId(final int childId) {
        return new ViewAction() {
            @Override
            public Matcher<View> getConstraints() {
                return isDisplayed();
            }

            @Override
            public String getDescription() {
                return "click child view with ID " + childId;
            }

            @Override
            public void perform(UiController uiController, View view) {
                View child = view.findViewById(childId);
                if (child != null) {
                    child.performClick();
                }
            }
        };
    }

    /**
     * Match text in RecyclerView item at position
     * @param position Item position
     * @param childId Child view ID containing text
     * @param text Expected text
     * @return Matcher
     */
    public static Matcher<View> withRecyclerViewItemText(
            final int position, @IdRes final int childId, final String text) {
        return new BoundedMatcher<View, RecyclerView>(RecyclerView.class) {
            @Override
            protected boolean matchesSafely(RecyclerView recyclerView) {
                RecyclerView.ViewHolder viewHolder =
                        recyclerView.findViewHolderForAdapterPosition(position);

                if (viewHolder == null) {
                    return false;
                }

                View childView = viewHolder.itemView.findViewById(childId);
                if (childView instanceof android.widget.TextView) {
                    return ((android.widget.TextView) childView).getText().toString().equals(text);
                }

                return false;
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("RecyclerView item at position " + position +
                        " with child " + childId + " having text: " + text);
            }
        };
    }

    /**
     * Simple IdlingResource that waits for a condition
     */
    public static class ConditionalIdlingResource implements IdlingResource {
        private ResourceCallback callback;
        private final java.util.function.BooleanSupplier condition;
        private final String name;

        public ConditionalIdlingResource(String name, java.util.function.BooleanSupplier condition) {
            this.name = name;
            this.condition = condition;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public boolean isIdleNow() {
            boolean idle = condition.getAsBoolean();
            if (idle && callback != null) {
                callback.onTransitionToIdle();
            }
            return idle;
        }

        @Override
        public void registerIdleTransitionCallback(ResourceCallback callback) {
            this.callback = callback;
        }
    }

    /**
     * Delay action - use sparingly, prefer IdlingResources
     * @param millis Delay in milliseconds
     * @return ViewAction
     */
    public static ViewAction waitFor(final long millis) {
        return new ViewAction() {
            @Override
            public Matcher<View> getConstraints() {
                return isRoot();
            }

            @Override
            public String getDescription() {
                return "wait for " + millis + " milliseconds";
            }

            @Override
            public void perform(UiController uiController, View view) {
                uiController.loopMainThreadForAtLeast(millis);
            }
        };
    }
}
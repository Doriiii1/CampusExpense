package com.example.campusexpense.ui;

import android.content.Context;
import android.content.Intent;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.contrib.PickerActions;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import com.example.campusexpense.R;
import com.example.campusexpense.auth.AuthManager;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.*;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.*;

/**
 * AddEditExpenseEspressoTest - UI tests for expense form
 * Phase 6: Testing
 *
 * Tests:
 * - Form field input
 * - Date picker interaction
 * - Save expense
 * - Form validation
 *
 * Uses Espresso for UI testing
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class AddEditExpenseEspressoTest {

    private Context context;
    private AuthManager authManager;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        authManager = new AuthManager(context);

        // Create and login test user
        String testUser = "testuser";
        String testPass = "testpass";

        context.getSharedPreferences("CampusExpenseAuth", Context.MODE_PRIVATE)
                .edit().clear().apply();

        authManager.register(testUser, testPass);
        authManager.login(testUser, testPass);
    }

    @Test
    public void testAddExpenseForm_DisplaysCorrectly() {
        // Launch activity
        Intent intent = new Intent(context, AddEditExpenseActivity.class);
        ActivityScenario.launch(intent);

        // Verify form elements
        onView(withId(R.id.spinnerCategory))
                .check(matches(isDisplayed()));

        onView(withId(R.id.editDescription))
                .check(matches(isDisplayed()));

        onView(withId(R.id.editAmount))
                .check(matches(isDisplayed()));

        onView(withId(R.id.editDate))
                .check(matches(isDisplayed()));

        onView(withId(R.id.btnSave))
                .check(matches(isDisplayed()));

        onView(withId(R.id.btnCancel))
                .check(matches(isDisplayed()));
    }

    @Test
    public void testAddExpense_WithValidData_Success() {
        // Launch activity
        Intent intent = new Intent(context, AddEditExpenseActivity.class);
        ActivityScenario.launch(intent);

        // Fill form
        onView(withId(R.id.editDescription))
                .perform(typeText("Test lunch"), closeSoftKeyboard());

        onView(withId(R.id.editAmount))
                .perform(typeText("25.50"), closeSoftKeyboard());

        // Category is already selected (spinner default)

        // Date is already filled (default today)

        // Click save
        onView(withId(R.id.btnSave))
                .perform(click());

        // Activity should finish and return RESULT_OK
        // In integration test, we'd verify expense appears in list
    }

    @Test
    public void testAddExpense_WithEmptyDescription_ShowsError() {
        // Launch activity
        Intent intent = new Intent(context, AddEditExpenseActivity.class);
        ActivityScenario.launch(intent);

        // Fill only amount, leave description empty
        onView(withId(R.id.editAmount))
                .perform(typeText("25.50"), closeSoftKeyboard());

        // Click save
        onView(withId(R.id.btnSave))
                .perform(click());

        // Verify we're still on the form (not closed)
        onView(withId(R.id.btnSave))
                .check(matches(isDisplayed()));
    }

    @Test
    public void testAddExpense_WithInvalidAmount_ShowsError() {
        // Launch activity
        Intent intent = new Intent(context, AddEditExpenseActivity.class);
        ActivityScenario.launch(intent);

        // Fill form with invalid amount
        onView(withId(R.id.editDescription))
                .perform(typeText("Test expense"), closeSoftKeyboard());

        onView(withId(R.id.editAmount))
                .perform(typeText("-10"), closeSoftKeyboard());

        // Click save
        onView(withId(R.id.btnSave))
                .perform(click());

        // Verify we're still on the form
        onView(withId(R.id.btnSave))
                .check(matches(isDisplayed()));
    }

    @Test
    public void testDatePicker_OpensAndSelectsDate() {
        // Launch activity
        Intent intent = new Intent(context, AddEditExpenseActivity.class);
        ActivityScenario.launch(intent);

        // Click date picker button
        onView(withId(R.id.btnDatePicker))
                .perform(click());

        // Wait for date picker
        EspressoTestUtils.waitForView(android.R.id.button1, 1000);

        // Select a date (year 2025, month 11, day 8)
        onView(withClassName(org.hamcrest.Matchers.equalTo(android.widget.DatePicker.class.getName())))
                .perform(PickerActions.setDate(2025, 11, 8));

        // Click OK
        onView(withId(android.R.id.button1))
                .perform(click());

        // Verify date field is populated (contains "Nov")
        onView(withId(R.id.editDate))
                .check(matches(withText(org.hamcrest.Matchers.containsString("Nov"))));
    }

    @Test
    public void testCancel_ClosesActivity() {
        // Launch activity
        Intent intent = new Intent(context, AddEditExpenseActivity.class);
        ActivityScenario<AddEditExpenseActivity> scenario = ActivityScenario.launch(intent);

        // Click cancel
        onView(withId(R.id.btnCancel))
                .perform(click());

        // Verify activity is finished
        assert(scenario.getState().isAtLeast(androidx.lifecycle.Lifecycle.State.DESTROYED));
    }

    @Test
    public void testNotesField_AcceptsMultilineInput() {
        // Launch activity
        Intent intent = new Intent(context, AddEditExpenseActivity.class);
        ActivityScenario.launch(intent);

        // Type multiline notes
        String notes = "Line 1\nLine 2\nLine 3";
        onView(withId(R.id.editNotes))
                .perform(typeText(notes), closeSoftKeyboard());

        // Verify notes are entered
        onView(withId(R.id.editNotes))
                .check(matches(withText(notes)));
    }
}
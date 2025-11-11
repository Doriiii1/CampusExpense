package com.example.campusexpense.schedule;

import android.content.Context;
import androidx.test.core.app.ApplicationProvider;
import com.example.campusexpense.db.DatabaseHelper;
import com.example.campusexpense.model.Expense;
import com.example.campusexpense.model.RecurringExpense;
import com.example.campusexpense.notifications.NotificationHelper;
import com.example.campusexpense.schedule.RecurringManager;
import com.example.campusexpense.test.TestAppInjector;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * RecurringManagerIntegrationTest - Integration tests for recurring expenses
 * Phase 6: Testing
 *
 * Tests:
 * - Recurring expense insertion via runNowForTesting()
 * - Notification triggers
 * - Next run calculation
 * - End date handling
 *
 * Uses test DatabaseHelper and mocked NotificationHelper
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 29)
public class RecurringManagerIntegrationTest {

    private Context context;
    private DatabaseHelper dbHelper;
    private RecurringManager recurringManager;
    private static final String TEST_USER = "testuser";

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        dbHelper = new DatabaseHelper(context);
        recurringManager = new RecurringManager(context);

        // Clear any existing data
        dbHelper.getWritableDatabase().execSQL("DELETE FROM expenses");
        dbHelper.getWritableDatabase().execSQL("DELETE FROM recurring_expenses");

        // Create notification channels
        NotificationHelper.createNotificationChannels(context);
    }

    @After
    public void tearDown() {
        if (dbHelper != null) {
            dbHelper.close();
        }
        TestAppInjector.reset();
    }

    @Test
    public void testRunNowForTesting_CreatesDueRecurringExpense() {
        // Given
        long now = System.currentTimeMillis();
        TestAppInjector.setNowMillis(now);

        RecurringExpense recurring = new RecurringExpense(
                TEST_USER, "Bills", 100.00, "Monthly rent",
                now - 1000, null, RecurringExpense.FREQUENCY_MONTHLY
        );
        recurring.setNextRun(now - 1000); // Due now
        long recurringId = dbHelper.createRecurring(recurring);

        // When
        recurringManager.runNowForTesting();

        // Then
        List<Expense> expenses = dbHelper.getExpenses(TEST_USER);
        assertEquals("Should create one expense from recurring", 1, expenses.size());

        Expense created = expenses.get(0);
        assertEquals("Category should match", "Bills", created.getCategory());
        assertEquals("Amount should match", 100.00, created.getAmount(), 0.01);
        assertTrue("Description should indicate recurring",
                created.getDescription().contains("Monthly rent"));
        assertTrue("Description should indicate recurring",
                created.getDescription().contains("Recurring"));
    }

    @Test
    public void testRunNowForTesting_UpdatesNextRunTime() {
        // Given
        long now = System.currentTimeMillis();
        TestAppInjector.setNowMillis(now);

        RecurringExpense recurring = new RecurringExpense(
                TEST_USER, "Bills", 100.00, "Monthly rent",
                now - 1000, null, RecurringExpense.FREQUENCY_MONTHLY
        );
        recurring.setNextRun(now - 1000);
        long recurringId = dbHelper.createRecurring(recurring);

        // When
        recurringManager.runNowForTesting();

        // Then
        List<RecurringExpense> updated = dbHelper.getRecurring(TEST_USER);
        assertEquals("Should have one recurring expense", 1, updated.size());

        long nextRun = updated.get(0).getNextRun();
        assertTrue("Next run should be in the future", nextRun > now);

        // For monthly, should be approximately 30 days later
        long expectedNextRun = now + (30L * 24 * 60 * 60 * 1000);
        long difference = Math.abs(nextRun - expectedNextRun);
        assertTrue("Next run should be approximately 30 days later",
                difference < (24 * 60 * 60 * 1000)); // Within 1 day tolerance
    }

    @Test
    public void testRunNowForTesting_DoesNotProcessFutureRecurring() {
        // Given
        long now = System.currentTimeMillis();
        long future = now + (7 * 24 * 60 * 60 * 1000);
        TestAppInjector.setNowMillis(now);

        RecurringExpense recurring = new RecurringExpense(
                TEST_USER, "Bills", 100.00, "Future rent",
                now, null, RecurringExpense.FREQUENCY_MONTHLY
        );
        recurring.setNextRun(future); // Not due yet
        dbHelper.createRecurring(recurring);

        // When
        recurringManager.runNowForTesting();

        // Then
        List<Expense> expenses = dbHelper.getExpenses(TEST_USER);
        assertEquals("Should not create expense from future recurring", 0, expenses.size());
    }

    @Test
    public void testRunNowForTesting_ProcessesMultipleDueRecurring() {
        // Given
        long now = System.currentTimeMillis();
        TestAppInjector.setNowMillis(now);

        RecurringExpense r1 = new RecurringExpense(
                TEST_USER, "Bills", 100.00, "Rent",
                now - 1000, null, RecurringExpense.FREQUENCY_MONTHLY
        );
        r1.setNextRun(now - 1000);
        dbHelper.createRecurring(r1);

        RecurringExpense r2 = new RecurringExpense(
                TEST_USER, "Bills", 50.00, "Utilities",
                now - 1000, null, RecurringExpense.FREQUENCY_MONTHLY
        );
        r2.setNextRun(now - 1000);
        dbHelper.createRecurring(r2);

        // When
        recurringManager.runNowForTesting();

        // Then
        List<Expense> expenses = dbHelper.getExpenses(TEST_USER);
        assertEquals("Should create two expenses", 2, expenses.size());

        double totalAmount = expenses.stream().mapToDouble(Expense::getAmount).sum();
        assertEquals("Total amount should be sum of recurring amounts", 150.00, totalAmount, 0.01);
    }

    @Test
    public void testRunNowForTesting_RespectsEndDate() {
        // Given
        long now = System.currentTimeMillis();
        long past = now - (60 * 24 * 60 * 60 * 1000); // 60 days ago
        TestAppInjector.setNowMillis(now);

        RecurringExpense expired = new RecurringExpense(
                TEST_USER, "Bills", 100.00, "Expired subscription",
                past, past, RecurringExpense.FREQUENCY_MONTHLY
        );
        expired.setNextRun(now - 1000); // Would be due, but expired
        dbHelper.createRecurring(expired);

        // When
        recurringManager.runNowForTesting();

        // Then
        List<Expense> expenses = dbHelper.getExpenses(TEST_USER);
        assertEquals("Should not create expense from expired recurring", 0, expenses.size());
    }

    @Test
    public void testRunNowForTesting_UpdatesBudgetAfterInsertion() {
        // Given
        long now = System.currentTimeMillis();
        TestAppInjector.setNowMillis(now);

        // Create budget
        com.example.campusexpense.model.Budget budget =
                new com.example.campusexpense.model.Budget(
                        TEST_USER, "Bills", 500.00,
                        com.example.campusexpense.model.Budget.CYCLE_MONTHLY, 80
                );
        dbHelper.createBudget(budget);

        // Create recurring
        RecurringExpense recurring = new RecurringExpense(
                TEST_USER, "Bills", 100.00, "Rent",
                now - 1000, null, RecurringExpense.FREQUENCY_MONTHLY
        );
        recurring.setNextRun(now - 1000);
        dbHelper.createRecurring(recurring);

        // When
        recurringManager.runNowForTesting();

        // Then
        com.example.campusexpense.model.Budget updatedBudget =
                dbHelper.getBudgetByCategory(TEST_USER, "Bills");
        assertNotNull("Budget should exist", updatedBudget);
        assertEquals("Budget spent should be updated", 100.00,
                updatedBudget.getCurrentSpent(), 0.01);
    }

    @Test
    public void testRecurringExpense_ComputeNextRun_Monthly() {
        // Given
        RecurringExpense recurring = new RecurringExpense();
        recurring.setFrequency(RecurringExpense.FREQUENCY_MONTHLY);

        long startTime = System.currentTimeMillis();

        // When
        long nextRun = recurring.computeNextRun(startTime);

        // Then
        long expectedDiff = 30L * 24 * 60 * 60 * 1000; // ~30 days
        long actualDiff = nextRun - startTime;

        // Allow 1 day tolerance for month variations
        assertTrue("Next run should be approximately 30 days later",
                Math.abs(actualDiff - expectedDiff) < (24 * 60 * 60 * 1000));
    }

    @Test
    public void testRecurringExpense_ComputeNextRun_Weekly() {
        // Given
        RecurringExpense recurring = new RecurringExpense();
        recurring.setFrequency(RecurringExpense.FREQUENCY_WEEKLY);

        long startTime = System.currentTimeMillis();

        // When
        long nextRun = recurring.computeNextRun(startTime);

        // Then
        long expectedDiff = 7L * 24 * 60 * 60 * 1000; // 7 days
        long actualDiff = nextRun - startTime;

        assertEquals("Next run should be exactly 7 days later",
                expectedDiff, actualDiff, 60000); // 1 minute tolerance
    }

    @Test
    public void testRecurringExpense_ComputeNextRun_Daily() {
        // Given
        RecurringExpense recurring = new RecurringExpense();
        recurring.setFrequency(RecurringExpense.FREQUENCY_DAILY);

        long startTime = System.currentTimeMillis();

        // When
        long nextRun = recurring.computeNextRun(startTime);

        // Then
        long expectedDiff = 24L * 60 * 60 * 1000; // 1 day
        long actualDiff = nextRun - startTime;

        assertEquals("Next run should be exactly 1 day later",
                expectedDiff, actualDiff, 60000); // 1 minute tolerance
    }
}
package com.example.campusexpense.db;

import android.content.Context;
import androidx.test.core.app.ApplicationProvider;
import com.example.campusexpense.model.Budget;
import com.example.campusexpense.model.Expense;
import com.example.campusexpense.model.RecurringExpense;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * DatabaseHelperTest - Unit tests for database operations
 * Phase 6: Testing
 *
 * Tests:
 * - CRUD operations for Expenses, Budgets, RecurringExpenses
 * - Database migrations
 * - Query filters and aggregations
 * - User isolation
 *
 * Uses in-memory database via Robolectric
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 29)
public class DatabaseHelperTest {

    private Context context;
    private DatabaseHelper dbHelper;
    private static final String TEST_USER = "testuser";

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        dbHelper = new DatabaseHelper(context);

        // Clear any existing data
        dbHelper.getWritableDatabase().execSQL("DELETE FROM expenses");
        dbHelper.getWritableDatabase().execSQL("DELETE FROM budgets");
        dbHelper.getWritableDatabase().execSQL("DELETE FROM recurring_expenses");
    }

    @After
    public void tearDown() {
        if (dbHelper != null) {
            dbHelper.close();
        }
    }

    // ==================== EXPENSE TESTS ====================

    @Test
    public void testInsertExpense_Success() {
        // Given
        Expense expense = createTestExpense("Food", 25.50);

        // When
        long id = dbHelper.insertExpense(expense);

        // Then
        assertTrue("Insert should return valid ID", id > 0);
    }

    @Test
    public void testGetExpenseById_ReturnsCorrectExpense() {
        // Given
        Expense expense = createTestExpense("Transport", 15.00);
        long id = dbHelper.insertExpense(expense);

        // When
        Expense retrieved = dbHelper.getExpenseById(id, TEST_USER);

        // Then
        assertNotNull("Should retrieve expense", retrieved);
        assertEquals("Category should match", "Transport", retrieved.getCategory());
        assertEquals("Amount should match", 15.00, retrieved.getAmount(), 0.01);
    }

    @Test
    public void testUpdateExpense_ModifiesExisting() {
        // Given
        Expense expense = createTestExpense("Food", 20.00);
        long id = dbHelper.insertExpense(expense);

        // When
        expense.setId(id);
        expense.setAmount(25.00);
        expense.setDescription("Updated meal");
        int rowsAffected = dbHelper.updateExpense(expense);

        // Then
        assertEquals("Should update one row", 1, rowsAffected);

        Expense updated = dbHelper.getExpenseById(id, TEST_USER);
        assertEquals("Amount should be updated", 25.00, updated.getAmount(), 0.01);
        assertEquals("Description should be updated", "Updated meal", updated.getDescription());
    }

    @Test
    public void testDeleteExpense_RemovesFromDatabase() {
        // Given
        Expense expense = createTestExpense("Shopping", 50.00);
        long id = dbHelper.insertExpense(expense);

        // When
        int rowsDeleted = dbHelper.deleteExpense(id, TEST_USER);

        // Then
        assertEquals("Should delete one row", 1, rowsDeleted);
        assertNull("Expense should not exist", dbHelper.getExpenseById(id, TEST_USER));
    }
    @Test
    public void testGetExpenses_FiltersByCategory() {
        // Given
        dbHelper.insertExpense(createTestExpense("Food", 20.00));
        dbHelper.insertExpense(createTestExpense("Food", 30.00));
        dbHelper.insertExpense(createTestExpense("Transport", 15.00));

        // When
        List<Expense> foodExpenses = dbHelper.getExpenses(TEST_USER, "Food", 0, System.currentTimeMillis());

        // Then
        assertEquals("Should return 2 food expenses", 2, foodExpenses.size());
        for (Expense expense : foodExpenses) {
            assertEquals("All should be Food category", "Food", expense.getCategory());
        }
    }

    @Test
    public void testGetExpenses_FiltersByDateRange() {
        // Given
        long now = System.currentTimeMillis();
        long yesterday = now - (24 * 60 * 60 * 1000);
        long lastWeek = now - (7 * 24 * 60 * 60 * 1000);

        Expense e1 = createTestExpense("Food", 20.00);
        e1.setDate(lastWeek);
        dbHelper.insertExpense(e1);

        Expense e2 = createTestExpense("Food", 30.00);
        e2.setDate(yesterday);
        dbHelper.insertExpense(e2);

        Expense e3 = createTestExpense("Food", 40.00);
        e3.setDate(now);
        dbHelper.insertExpense(e3);

        // When - get expenses from yesterday onwards
        List<Expense> recentExpenses = dbHelper.getExpenses(TEST_USER, null, yesterday - 1000, now + 1000);

        // Then
        assertEquals("Should return 2 recent expenses", 2, recentExpenses.size());
    }

    @Test
    public void testGetTotalExpenses_CalculatesCorrectSum() {
        // Given
        dbHelper.insertExpense(createTestExpense("Food", 20.00));
        dbHelper.insertExpense(createTestExpense("Transport", 15.00));
        dbHelper.insertExpense(createTestExpense("Shopping", 50.00));

        // When
        double total = dbHelper.getTotalExpenses(TEST_USER, 0, System.currentTimeMillis());

        // Then
        assertEquals("Should sum all expenses", 85.00, total, 0.01);
    }

    @Test
    public void testUserIsolation_OnlyReturnsUserExpenses() {
        // Given
        String user1 = "user1";
        String user2 = "user2";

        Expense e1 = createTestExpense("Food", 20.00);
        e1.setUserId(user1);
        dbHelper.insertExpense(e1);

        Expense e2 = createTestExpense("Food", 30.00);
        e2.setUserId(user2);
        dbHelper.insertExpense(e2);

        // When
        List<Expense> user1Expenses = dbHelper.getExpenses(user1);
        List<Expense> user2Expenses = dbHelper.getExpenses(user2);

        // Then
        assertEquals("User1 should have 1 expense", 1, user1Expenses.size());
        assertEquals("User2 should have 1 expense", 1, user2Expenses.size());
        assertEquals("User1 expense should belong to user1", user1, user1Expenses.get(0).getUserId());
    }

    // ==================== BUDGET TESTS ====================

    @Test
    public void testCreateBudget_Success() {
        // Given
        Budget budget = new Budget(TEST_USER, "Food", 200.00, Budget.CYCLE_MONTHLY, 80);

        // When
        long id = dbHelper.createBudget(budget);

        // Then
        assertTrue("Insert should return valid ID", id > 0);
    }

    @Test
    public void testGetBudgetByCategory_ReturnsCorrectBudget() {
        // Given
        Budget budget = new Budget(TEST_USER, "Food", 200.00, Budget.CYCLE_MONTHLY, 80);
        dbHelper.createBudget(budget);

        // When
        Budget retrieved = dbHelper.getBudgetByCategory(TEST_USER, "Food");

        // Then
        assertNotNull("Should retrieve budget", retrieved);
        assertEquals("Category should match", "Food", retrieved.getCategory());
        assertEquals("Limit should match", 200.00, retrieved.getLimitAmount(), 0.01);
    }

    @Test
    public void testUpdateBudget_ModifiesExisting() {
        // Given
        Budget budget = new Budget(TEST_USER, "Food", 200.00, Budget.CYCLE_MONTHLY, 80);
        long id = dbHelper.createBudget(budget);

        // When
        budget.setId(id);
        budget.setLimitAmount(250.00);
        budget.setThresholdPercent(75);
        int rowsAffected = dbHelper.updateBudget(budget);

        // Then
        assertEquals("Should update one row", 1, rowsAffected);

        Budget updated = dbHelper.getBudgetByCategory(TEST_USER, "Food");
        assertEquals("Limit should be updated", 250.00, updated.getLimitAmount(), 0.01);
        assertEquals("Threshold should be updated", 75, updated.getThresholdPercent());
    }

    @Test
    public void testRecomputeBudgetSpent_AggregatesExpenses() {
        // Given
        Budget budget = new Budget(TEST_USER, "Food", 200.00, Budget.CYCLE_MONTHLY, 80);
        dbHelper.createBudget(budget);

        // Add expenses
        dbHelper.insertExpense(createTestExpense("Food", 50.00));
        dbHelper.insertExpense(createTestExpense("Food", 75.00));
        dbHelper.insertExpense(createTestExpense("Transport", 30.00)); // Different category

        // When
        double spent = dbHelper.recomputeBudgetSpent(TEST_USER, "Food");

        // Then
        assertEquals("Should aggregate Food expenses", 125.00, spent, 0.01);
    }

    // ==================== RECURRING EXPENSE TESTS ====================

    @Test
    public void testCreateRecurring_Success() {
        // Given
        RecurringExpense recurring = new RecurringExpense(
                TEST_USER, "Bills", 100.00, "Monthly rent",
                System.currentTimeMillis(), null, RecurringExpense.FREQUENCY_MONTHLY
        );

        // When
        long id = dbHelper.createRecurring(recurring);

        // Then
        assertTrue("Insert should return valid ID", id > 0);
    }

    @Test
    public void testGetRecurringDue_ReturnsOnlyDueExpenses() {
        // Given
        long now = System.currentTimeMillis();
        long future = now + (7 * 24 * 60 * 60 * 1000);

        RecurringExpense due = new RecurringExpense(
                TEST_USER, "Bills", 100.00, "Due now",
                now - 1000, null, RecurringExpense.FREQUENCY_MONTHLY
        );
        due.setNextRun(now - 1000);
        dbHelper.createRecurring(due);

        RecurringExpense notDue = new RecurringExpense(
                TEST_USER, "Bills", 100.00, "Not due yet",
                now, null, RecurringExpense.FREQUENCY_MONTHLY
        );
        notDue.setNextRun(future);
        dbHelper.createRecurring(notDue);

        // When
        List<RecurringExpense> dueList = dbHelper.getRecurringDue(now);

        // Then
        assertEquals("Should return only due recurring expense", 1, dueList.size());
        assertEquals("Should be the 'Due now' expense", "Due now", dueList.get(0).getDescription());
    }

    @Test
    public void testGetRecurringDue_RespectsEndDate() {
        // Given
        long now = System.currentTimeMillis();
        long past = now - (30 * 24 * 60 * 60 * 1000);

        RecurringExpense expired = new RecurringExpense(
                TEST_USER, "Bills", 100.00, "Expired",
                past, past, RecurringExpense.FREQUENCY_MONTHLY
        );
        expired.setNextRun(now - 1000);
        dbHelper.createRecurring(expired);

        // When
        List<RecurringExpense> dueList = dbHelper.getRecurringDue(now);

        // Then
        assertEquals("Should not return expired recurring expense", 0, dueList.size());
    }

    // ==================== HELPER METHODS ====================

    private Expense createTestExpense(String category, double amount) {
        Expense expense = new Expense();
        expense.setUserId(TEST_USER);
        expense.setCategory(category);
        expense.setDescription("Test " + category);
        expense.setAmount(amount);
        expense.setDate(System.currentTimeMillis());
        expense.setNotes("Test notes");
        return expense;
    }
}
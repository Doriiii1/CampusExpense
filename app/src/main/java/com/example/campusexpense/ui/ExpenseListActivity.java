package com.example.campusexpense.ui;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.campusexpense.R;
import com.example.campusexpense.auth.AuthManager;
import com.example.campusexpense.db.DatabaseHelper;
import com.example.campusexpense.model.Budget;
import com.example.campusexpense.model.Expense;
import com.example.campusexpense.notifications.NotificationHelper;
import com.github.mikephil.charting.BuildConfig;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/**
 * ExpenseListActivity - Displays list of expenses with CRUD operations
 * Phase 2: RecyclerView with swipe-to-delete and filters
 * <p>
 * Features:
 * - RecyclerView with custom adapter
 * - Swipe-to-delete with UNDO via Snackbar
 * - Filter by category and date range
 * - FloatingActionButton to add new expenses
 * - Empty state when no expenses
 * <p>
 * Lifecycle: Refreshes list on resume to show updated data
 */
public class ExpenseListActivity extends AppCompatActivity {

    private static final String TAG = "ExpenseListActivity";
    private static final int REQUEST_ADD_EXPENSE = 1001;
    private static final int REQUEST_EDIT_EXPENSE = 1002;

    private RecyclerView recyclerView;
    private ExpenseAdapter adapter;
    private FloatingActionButton fabAdd;
    private Spinner spinnerCategory;
    private TextView textDateFilter;
    private View layoutEmptyState;

    private DatabaseHelper dbHelper;
    private AuthManager authManager;
    private List<Expense> expenses;

    private String currentCategory = "All";
    private long startDateFilter = 0;
    private long endDateFilter = 0;
    Button btnSave;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        UiUtils.stylePrimaryButton(btnSave);
        setContentView(R.layout.activity_expense_list);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // Initialize managers
        authManager = new AuthManager(this);
        dbHelper = new DatabaseHelper(this);

        // Check session
        if (!authManager.isSessionValid()) {
            finish();
            return;
        }

        // Initialize views
        initViews();

        // Set up RecyclerView
        setupRecyclerView();

        // Set up filters
        setupFilters();

        // Set up FAB
        setupFab();

        // Load expenses
        loadExpenses();
    }

    private void initViews() {
        recyclerView = findViewById(R.id.recyclerExpenses);
        fabAdd = findViewById(R.id.fabAdd);
        spinnerCategory = findViewById(R.id.spinnerCategory);
        textDateFilter = findViewById(R.id.textDateFilter);
        layoutEmptyState = findViewById(R.id.layoutEmptyState);
    }

    private void setupRecyclerView() {
        expenses = new ArrayList<>();
        adapter = new ExpenseAdapter(expenses, new ExpenseAdapter.OnExpenseClickListener() {
            @Override
            public void onExpenseClick(Expense expense) {
                openEditExpense(expense);
            }
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        // Swipe-to-delete
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                Expense deletedExpense = expenses.get(position);

                // Remove from list
                expenses.remove(position);
                adapter.notifyItemRemoved(position);

                // Show UNDO Snackbar
                Snackbar.make(recyclerView, R.string.expense_deleted, Snackbar.LENGTH_LONG)
                        .setAction(R.string.undo, new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                // Restore expense
                                expenses.add(position, deletedExpense);
                                adapter.notifyItemInserted(position);
                                updateEmptyState();
                            }
                        })
                        .addCallback(new Snackbar.Callback() {
                            @Override
                            public void onDismissed(Snackbar snackbar, int event) {
                                if (event != DISMISS_EVENT_ACTION) {
                                    // Actually delete from database
                                    deleteExpenseFromDb(deletedExpense);
                                }
                            }
                        })
                        .show();

                updateEmptyState();
            }
        });

        itemTouchHelper.attachToRecyclerView(recyclerView);
    }

    private void setupFilters() {
        // Category filter
        String[] categories = getResources().getStringArray(R.array.expense_categories);
        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, categories);
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(categoryAdapter);

        spinnerCategory.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                currentCategory = categories[position];
                loadExpenses();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        // Date filter
        textDateFilter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDateRangePickerDialog();
            }
        });
    }

    private void setupFab() {
        fabAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ExpenseListActivity.this, AddEditExpenseActivity.class);
                startActivityForResult(intent, REQUEST_ADD_EXPENSE);
            }
        });
    }

    private void loadExpenses() {
        String userId = authManager.getCurrentUser();
        if (userId == null) {
            return;
        }

        try {
            expenses.clear();
            List<Expense> loadedExpenses = dbHelper.getExpenses(userId, currentCategory, startDateFilter, endDateFilter);
            expenses.addAll(loadedExpenses);
            adapter.notifyDataSetChanged();
            updateEmptyState();

            Log.d(TAG, "Loaded " + expenses.size() + " expenses");

        } catch (Exception e) {
            Log.e(TAG, "Error loading expenses", e);
            Toast.makeText(this, R.string.error_loading_expenses, Toast.LENGTH_SHORT).show();
        }
    }

    private void updateEmptyState() {
        if (expenses.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            layoutEmptyState.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            layoutEmptyState.setVisibility(View.GONE);
        }
    }

    private void openEditExpense(Expense expense) {
        Intent intent = new Intent(ExpenseListActivity.this, AddEditExpenseActivity.class);
        intent.putExtra("expense", expense);
        startActivityForResult(intent, REQUEST_EDIT_EXPENSE);
    }

    private void deleteExpenseFromDb(Expense expense) {
        String userId = authManager.getCurrentUser();
        if (userId == null) {
            return;
        }

        try {
            int result = dbHelper.deleteExpense(expense.getId(), userId);
            if (result > 0) {
                Log.d(TAG, "Expense deleted successfully");
            } else {
                Toast.makeText(this, R.string.error_deleting_expense, Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error deleting expense", e);
            Toast.makeText(this, R.string.error_deleting_expense, Toast.LENGTH_SHORT).show();
        }
    }

    private void showDateRangePickerDialog() {
        Calendar calendar = Calendar.getInstance();

        // Start date picker
        DatePickerDialog startDatePicker = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    Calendar startCal = Calendar.getInstance();
                    startCal.set(year, month, dayOfMonth, 0, 0, 0);
                    startDateFilter = startCal.getTimeInMillis();

                    // End date picker
                    DatePickerDialog endDatePicker = new DatePickerDialog(
                            this,
                            (view2, year2, month2, dayOfMonth2) -> {
                                Calendar endCal = Calendar.getInstance();
                                endCal.set(year2, month2, dayOfMonth2, 23, 59, 59);
                                endDateFilter = endCal.getTimeInMillis();

                                updateDateFilterText();
                                loadExpenses();
                            },
                            calendar.get(Calendar.YEAR),
                            calendar.get(Calendar.MONTH),
                            calendar.get(Calendar.DAY_OF_MONTH)
                    );
                    endDatePicker.setTitle(R.string.select_end_date);
                    endDatePicker.show();
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );
        startDatePicker.setTitle(R.string.select_start_date);
        startDatePicker.show();
    }

    private void updateDateFilterText() {
        if (startDateFilter > 0 && endDateFilter > 0) {
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd", Locale.getDefault());
            String dateRange = sdf.format(startDateFilter) + " - " + sdf.format(endDateFilter);
            textDateFilter.setText(dateRange);
        } else {
            textDateFilter.setText(R.string.filter_by_date);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_ADD_EXPENSE || requestCode == REQUEST_EDIT_EXPENSE) {
                // Reload expenses after add/edit
                loadExpenses();

                // Phase 3 Integration: Trigger budget recompute and notification (for added/edited expense)**
            String userId = authManager.getCurrentUser();
            if (userId != null && data != null) {
                String category = data.getStringExtra("category"); // Assume passed from AddEditExpenseActivity**
                if (category != null && !category.equals("All")) {
                    // Recompute budget for this category**
                    dbHelper.recomputeBudgetSpent(userId, category);

                    // Check if budget threshold reached**
                    Budget budget = dbHelper.getBudgetByCategory(userId, category);
                    if (budget != null && budget.isAtThreshold()) {
                        NotificationHelper.notifyBudgetThreshold(
                                    this,
                            budget.getCategory(),
                            budget.getCurrentSpent(),
                            budget.getLimitAmount()
                        );
                    }
                } else {
                    // Fallback: Recompute all budgets if no category passed**
                    dbHelper.recomputeAllBudgets(userId);
                }
            }
        }
    }
}

    @Override
    protected void onResume() {
        super.onResume();
        UiUtils.dumpPerformanceHints(this);
        if (BuildConfig.DEBUG) UiUtils.dumpPerformanceHints(this);
        // Update session timestamp
        authManager.updateLastActivityTimestamp();
        // Reload expenses in case data changed
        loadExpenses();
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Future: Stop any animations
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Clean up resources
        if (dbHelper != null) {
            dbHelper.close();
        }
        authManager = null;
        dbHelper = null;
    }

    /**
     * ExpenseAdapter - RecyclerView adapter for expense list
     */
    private static class ExpenseAdapter extends RecyclerView.Adapter<ExpenseAdapter.ExpenseViewHolder> {

        private final List<Expense> expenses;
        private final OnExpenseClickListener listener;

        interface OnExpenseClickListener {
            void onExpenseClick(Expense expense);
        }

        ExpenseAdapter(List<Expense> expenses, OnExpenseClickListener listener) {
            this.expenses = expenses;
            this.listener = listener;
        }

        @NonNull
        @Override
        public ExpenseViewHolder onCreateViewHolder(@NonNull android.view.ViewGroup parent, int viewType) {
            View view = android.view.LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_expense, parent, false);
            return new ExpenseViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ExpenseViewHolder holder, int position) {
            Expense expense = expenses.get(position);
            holder.bind(expense, listener);
        }

        @Override
        public int getItemCount() {
            return expenses.size();
        }

        static class ExpenseViewHolder extends RecyclerView.ViewHolder {

            private final TextView textCategory;
            private final TextView textDescription;
            private final TextView textAmount;
            private final TextView textDate;

            ExpenseViewHolder(@NonNull View itemView) {
                super(itemView);
                textCategory = itemView.findViewById(R.id.textCategory);
                textDescription = itemView.findViewById(R.id.textDescription);
                textAmount = itemView.findViewById(R.id.textAmount);
                textDate = itemView.findViewById(R.id.textDate);
            }

            void bind(Expense expense, OnExpenseClickListener listener) {
                textCategory.setText(expense.getCategory());
                textDescription.setText(expense.getDescription());
                textAmount.setText(String.format(Locale.getDefault(), "$%.2f", expense.getAmount()));

                SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
                textDate.setText(sdf.format(expense.getDate()));

                itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        listener.onExpenseClick(expense);
                    }
                });
            }
        }
    }
}
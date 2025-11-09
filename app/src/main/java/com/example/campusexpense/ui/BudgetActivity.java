package com.example.campusexpense.ui;

import android.app.AlertDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.campusexpense.R;
import com.example.campusexpense.auth.AuthManager;
import com.example.campusexpense.db.DatabaseHelper;
import com.example.campusexpense.model.Budget;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * BudgetActivity - Budget management screen
 * Phase 3: Create, edit, delete budgets with progress tracking
 *
 * Features:
 * - RecyclerView list of budgets
 * - Add/edit budgets via dialog
 * - Color-coded progress bars
 * - Overall budget summary
 * - Force recompute for testing
 *
 * Lifecycle: Refreshes budgets on resume
 */
public class BudgetActivity extends AppCompatActivity {

    private static final String TAG = "BudgetActivity";

    private RecyclerView recyclerBudgets;
    private BudgetAdapter adapter;
    private FloatingActionButton fabAddBudget;
    private TextView textTotalBudget;
    private TextView textTotalSpent;
    private TextView textOverallProgress;

    private DatabaseHelper dbHelper;
    private AuthManager authManager;
    private List<Budget> budgets;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_budget);

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

        // Set up FAB
        setupFab();

        // Load budgets
        loadBudgets();
    }

    private void initViews() {
        recyclerBudgets = findViewById(R.id.recyclerBudgets);
        fabAddBudget = findViewById(R.id.fabAddBudget);
        textTotalBudget = findViewById(R.id.textTotalBudget);
        textTotalSpent = findViewById(R.id.textTotalSpent);
        textOverallProgress = findViewById(R.id.textOverallProgress);
    }

    private void setupRecyclerView() {
        budgets = new ArrayList<>();
        adapter = new BudgetAdapter(budgets, new BudgetAdapter.OnBudgetClickListener() {
            @Override
            public void onBudgetClick(Budget budget) {
                showEditBudgetDialog(budget);
            }

            @Override
            public void onBudgetDelete(Budget budget) {
                deleteBudget(budget);
            }
        });

        recyclerBudgets.setLayoutManager(new LinearLayoutManager(this));
        recyclerBudgets.setAdapter(adapter);
    }

    private void setupFab() {
        fabAddBudget.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAddBudgetDialog();
            }
        });

        // Debug action - long press to force recompute
        fabAddBudget.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                forceRecomputeBudgets();
                return true;
            }
        });
    }

    private void loadBudgets() {
        String userId = authManager.getCurrentUser();
        if (userId == null) {
            return;
        }

        try {
            // Recompute all budgets first
            dbHelper.recomputeAllBudgets(userId);

            // Load budgets
            budgets.clear();
            List<Budget> loadedBudgets = dbHelper.getBudgets(userId);
            budgets.addAll(loadedBudgets);
            adapter.notifyDataSetChanged();

            // Update summary
            updateSummary();

            Log.d(TAG, "Loaded " + budgets.size() + " budgets");

        } catch (Exception e) {
            Log.e(TAG, "Error loading budgets", e);
            Toast.makeText(this, R.string.error_loading_budgets, Toast.LENGTH_SHORT).show();
        }
    }

    private void updateSummary() {
        double totalLimit = 0.0;
        double totalSpent = 0.0;

        for (Budget budget : budgets) {
            totalLimit += budget.getLimitAmount();
            totalSpent += budget.getCurrentSpent();
        }

        textTotalBudget.setText(String.format(Locale.getDefault(), "$%.2f", totalLimit));
        textTotalSpent.setText(String.format(Locale.getDefault(), "$%.2f", totalSpent));

        if (totalLimit > 0) {
            int overallPercent = (int) ((totalSpent / totalLimit) * 100);
            textOverallProgress.setText(String.format(Locale.getDefault(), "%d%%", overallPercent));
        } else {
            textOverallProgress.setText("0%");
        }
    }

    private void showAddBudgetDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_set_budget, null);
        builder.setView(dialogView);

        Spinner spinnerCategory = dialogView.findViewById(R.id.spinnerBudgetCategory);
        EditText editLimitAmount = dialogView.findViewById(R.id.editBudgetAmount);
        EditText editThreshold = dialogView.findViewById(R.id.editThreshold);
        Button btnSave = dialogView.findViewById(R.id.btnSaveBudget);
        Button btnCancel = dialogView.findViewById(R.id.btnCancelBudget);

        // Set up category spinner
        String[] categories = getResources().getStringArray(R.array.expense_categories_no_all);
        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, categories);
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(categoryAdapter);

        // Set default threshold
        editThreshold.setText(String.valueOf(Budget.DEFAULT_THRESHOLD));

        AlertDialog dialog = builder.create();

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String category = spinnerCategory.getSelectedItem().toString();
                String limitStr = editLimitAmount.getText().toString().trim();
                String thresholdStr = editThreshold.getText().toString().trim();

                // Validation
                if (limitStr.isEmpty()) {
                    Toast.makeText(BudgetActivity.this, R.string.error_budget_limit_required, Toast.LENGTH_SHORT).show();
                    return;
                }

                double limit;
                try {
                    limit = Double.parseDouble(limitStr);
                    if (limit <= 0) {
                        Toast.makeText(BudgetActivity.this, R.string.error_budget_limit_positive, Toast.LENGTH_SHORT).show();
                        return;
                    }
                } catch (NumberFormatException e) {
                    Toast.makeText(BudgetActivity.this, R.string.error_invalid_amount, Toast.LENGTH_SHORT).show();
                    return;
                }

                int threshold = Budget.DEFAULT_THRESHOLD;
                if (!thresholdStr.isEmpty()) {
                    try {
                        threshold = Integer.parseInt(thresholdStr);
                        if (threshold < 0 || threshold > 100) {
                            Toast.makeText(BudgetActivity.this, R.string.error_threshold_range, Toast.LENGTH_SHORT).show();
                            return;
                        }
                    } catch (NumberFormatException e) {
                        Toast.makeText(BudgetActivity.this, R.string.error_invalid_threshold, Toast.LENGTH_SHORT).show();
                        return;
                    }
                }

                // Create budget
                String userId = authManager.getCurrentUser();
                Budget budget = new Budget(userId, category, limit, Budget.CYCLE_MONTHLY, threshold);

                long result = dbHelper.createBudget(budget);
                if (result > 0) {
                    Toast.makeText(BudgetActivity.this, R.string.budget_created, Toast.LENGTH_SHORT).show();
                    loadBudgets();
                    dialog.dismiss();
                } else {
                    Toast.makeText(BudgetActivity.this, R.string.error_creating_budget, Toast.LENGTH_SHORT).show();
                }
            }
        });

        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        dialog.show();
    }

    private void showEditBudgetDialog(Budget budget) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_set_budget, null);
        builder.setView(dialogView);

        Spinner spinnerCategory = dialogView.findViewById(R.id.spinnerBudgetCategory);
        EditText editLimitAmount = dialogView.findViewById(R.id.editBudgetAmount);
        EditText editThreshold = dialogView.findViewById(R.id.editThreshold);
        Button btnSave = dialogView.findViewById(R.id.btnSaveBudget);
        Button btnCancel = dialogView.findViewById(R.id.btnCancelBudget);

        // Set up category spinner
        String[] categories = getResources().getStringArray(R.array.expense_categories_no_all);
        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, categories);
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(categoryAdapter);

        // Pre-fill data
        for (int i = 0; i < categories.length; i++) {
            if (categories[i].equals(budget.getCategory())) {
                spinnerCategory.setSelection(i);
                break;
            }
        }
        editLimitAmount.setText(String.format(Locale.getDefault(), "%.2f", budget.getLimitAmount()));
        editThreshold.setText(String.valueOf(budget.getThresholdPercent()));

        // Disable category change (budget already exists for this category)
        spinnerCategory.setEnabled(false);

        AlertDialog dialog = builder.create();

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String limitStr = editLimitAmount.getText().toString().trim();
                String thresholdStr = editThreshold.getText().toString().trim();

                // Validation
                if (limitStr.isEmpty()) {
                    Toast.makeText(BudgetActivity.this, R.string.error_budget_limit_required, Toast.LENGTH_SHORT).show();
                    return;
                }

                double limit;
                try {
                    limit = Double.parseDouble(limitStr);
                    if (limit <= 0) {
                        Toast.makeText(BudgetActivity.this, R.string.error_budget_limit_positive, Toast.LENGTH_SHORT).show();
                        return;
                    }
                } catch (NumberFormatException e) {
                    Toast.makeText(BudgetActivity.this, R.string.error_invalid_amount, Toast.LENGTH_SHORT).show();
                    return;
                }

                int threshold = Budget.DEFAULT_THRESHOLD;
                if (!thresholdStr.isEmpty()) {
                    try {
                        threshold = Integer.parseInt(thresholdStr);
                        if (threshold < 0 || threshold > 100) {
                            Toast.makeText(BudgetActivity.this, R.string.error_threshold_range, Toast.LENGTH_SHORT).show();
                            return;
                        }
                    } catch (NumberFormatException e) {
                        Toast.makeText(BudgetActivity.this, R.string.error_invalid_threshold, Toast.LENGTH_SHORT).show();
                        return;
                    }
                }

                // Update budget
                budget.setLimitAmount(limit);
                budget.setThresholdPercent(threshold);

                int result = dbHelper.updateBudget(budget);
                if (result > 0) {
                    Toast.makeText(BudgetActivity.this, R.string.budget_updated, Toast.LENGTH_SHORT).show();
                    loadBudgets();
                    dialog.dismiss();
                } else {
                    Toast.makeText(BudgetActivity.this, R.string.error_updating_budget, Toast.LENGTH_SHORT).show();
                }
            }
        });

        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        dialog.show();
    }

    private void deleteBudget(Budget budget) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.delete_budget)
                .setMessage(R.string.confirm_delete_budget)
                .setPositiveButton(R.string.delete, (dialog, which) -> {
                    String userId = authManager.getCurrentUser();
                    int result = dbHelper.deleteBudget(budget.getId(), userId);
                    if (result > 0) {
                        Toast.makeText(this, R.string.budget_deleted, Toast.LENGTH_SHORT).show();
                        loadBudgets();
                    } else {
                        Toast.makeText(this, R.string.error_deleting_budget, Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    /**
     * Force recompute all budgets - for testing/debugging
     */
    private void forceRecomputeBudgets() {
        String userId = authManager.getCurrentUser();
        if (userId != null) {
            dbHelper.recomputeAllBudgets(userId);
            loadBudgets();
            Toast.makeText(this, "Budgets recomputed", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        authManager.updateLastActivityTimestamp();
        loadBudgets();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (dbHelper != null) {
            dbHelper.close();
        }
        authManager = null;
        dbHelper = null;
    }

    /**
     * BudgetAdapter - RecyclerView adapter for budget list
     */
    private static class BudgetAdapter extends RecyclerView.Adapter<BudgetAdapter.BudgetViewHolder> {

        private final List<Budget> budgets;
        private final OnBudgetClickListener listener;

        interface OnBudgetClickListener {
            void onBudgetClick(Budget budget);
            void onBudgetDelete(Budget budget);
        }

        BudgetAdapter(List<Budget> budgets, OnBudgetClickListener listener) {
            this.budgets = budgets;
            this.listener = listener;
        }

        @NonNull
        @Override
        public BudgetViewHolder onCreateViewHolder(@NonNull android.view.ViewGroup parent, int viewType) {
            View view = android.view.LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_budget, parent, false);
            return new BudgetViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull BudgetViewHolder holder, int position) {
            Budget budget = budgets.get(position);
            holder.bind(budget, listener);
        }

        @Override
        public int getItemCount() {
            return budgets.size();
        }

        static class BudgetViewHolder extends RecyclerView.ViewHolder {

            private final TextView textCategory;
            private final TextView textAmount;
            private final TextView textProgress;
            private final android.widget.ProgressBar progressBar;
            private final Button btnDelete;

            BudgetViewHolder(@NonNull View itemView) {
                super(itemView);
                textCategory = itemView.findViewById(R.id.textBudgetCategory);
                textAmount = itemView.findViewById(R.id.textBudgetAmount);
                textProgress = itemView.findViewById(R.id.textBudgetProgress);
                progressBar = itemView.findViewById(R.id.progressBudget);
                btnDelete = itemView.findViewById(R.id.btnDeleteBudget);
            }

            void bind(Budget budget, OnBudgetClickListener listener) {
                textCategory.setText(budget.getCategory());
                textAmount.setText(String.format(Locale.getDefault(),
                        "$%.2f / $%.2f", budget.getCurrentSpent(), budget.getLimitAmount()));

                int progress = budget.getProgressPercent();
                textProgress.setText(String.format(Locale.getDefault(), "%d%%", progress));
                progressBar.setProgress(Math.min(progress, 100));

                // Color coding
                int colorResId;
                if (progress >= 100) {
                    colorResId = R.color.danger_red;
                } else if (progress >= budget.getThresholdPercent()) {
                    colorResId = R.color.warning_orange;
                } else {
                    colorResId = R.color.success_green;
                }

                int color = itemView.getContext().getColor(colorResId);
                progressBar.setProgressTintList(android.content.res.ColorStateList.valueOf(color));
                textProgress.setTextColor(color);

                itemView.setOnClickListener(v -> listener.onBudgetClick(budget));
                btnDelete.setOnClickListener(v -> listener.onBudgetDelete(budget));
            }
        }
    }
}
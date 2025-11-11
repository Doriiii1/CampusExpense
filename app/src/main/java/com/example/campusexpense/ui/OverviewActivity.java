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
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.campusexpense.R;
import com.example.campusexpense.auth.AuthManager;
import com.example.campusexpense.db.DatabaseHelper;
import com.example.campusexpense.model.Budget;
import com.example.campusexpense.reports.ReportGenerator;
import com.example.campusexpense.ui.adapters.CategorySummaryAdapter;
import com.github.mikephil.charting.charts.PieChart;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * OverviewActivity - Spending overview and reports
 * Phase 4: Overview and Reports
 *
 * Features:
 * - Total spending display
 * - Category breakdown with percentages
 * - PieChart visualization
 * - Date range filtering
 * - Budget status indicators
 * - Navigation to detail reports
 *
 * Threading: DB queries run on ExecutorService
 * Lifecycle: Executor shutdown in onDestroy
 */
public class OverviewActivity extends AppCompatActivity {

    private static final String TAG = "OverviewActivity";

    private TextView textOverviewTotal;
    private TextView textDateRange;
    private Spinner spinnerRange;
    private Button btnRefresh;
    private PieChart pieChart;
    private RecyclerView rvCategorySummary;

    private AuthManager authManager;
    private DatabaseHelper dbHelper;
    private ReportGenerator reportGenerator;
    private CategorySummaryAdapter adapter;
    private ExecutorService executor;

    private List<ReportGenerator.CategorySummary> summaries;
    private long startDate;
    private long endDate;
    private String selectedRange = "Month";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_overview);

        // Initialize managers
        authManager = new AuthManager(this);
        dbHelper = new DatabaseHelper(this);
        reportGenerator = new ReportGenerator(this);
        executor = Executors.newSingleThreadExecutor();

        // Check session
        if (!authManager.isSessionValid()) {
            finish();
            return;
        }

        // Initialize views
        initViews();

        // Set up spinner
        setupRangeSpinner();

        // Set up RecyclerView
        setupRecyclerView();

        // Set up refresh button
        btnRefresh.setOnClickListener(v -> loadOverviewData());

        // Initial load with default range (Month)
        setDateRange("Month");
        loadOverviewData();
    }

    private void initViews() {
        textOverviewTotal = findViewById(R.id.textOverviewTotal);
        textDateRange = findViewById(R.id.textDateRange);
        spinnerRange = findViewById(R.id.spinnerRange);
        btnRefresh = findViewById(R.id.btnRefreshOverview);
        pieChart = findViewById(R.id.pieChart);
        rvCategorySummary = findViewById(R.id.rvCategorySummary);
    }

    private void setupRangeSpinner() {
        String[] ranges = getResources().getStringArray(R.array.report_ranges);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, ranges);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerRange.setAdapter(adapter);

        spinnerRange.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedRange = ranges[position];
                setDateRange(selectedRange);
                loadOverviewData();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        // Set default to Month
        spinnerRange.setSelection(2);
    }

    private void setupRecyclerView() {
        summaries = new ArrayList<>();
        adapter = new CategorySummaryAdapter(summaries, new CategorySummaryAdapter.OnCategoryClickListener() {
            @Override
            public void onCategoryClick(ReportGenerator.CategorySummary summary) {
                openDetailReport(summary.category);
            }
        });

        rvCategorySummary.setLayoutManager(new LinearLayoutManager(this));
        rvCategorySummary.setAdapter(adapter);
    }

    private void setDateRange(String range) {
        Calendar calendar = Calendar.getInstance();
        endDate = calendar.getTimeInMillis();

        switch (range) {
            case "Today":
                calendar.set(Calendar.HOUR_OF_DAY, 0);
                calendar.set(Calendar.MINUTE, 0);
                calendar.set(Calendar.SECOND, 0);
                calendar.set(Calendar.MILLISECOND, 0);
                startDate = calendar.getTimeInMillis();
                break;

            case "Week":
                calendar.add(Calendar.DAY_OF_MONTH, -7);
                startDate = calendar.getTimeInMillis();
                break;

            case "Month":
                calendar.add(Calendar.MONTH, -1);
                startDate = calendar.getTimeInMillis();
                break;

            case "Year":
                calendar.add(Calendar.YEAR, -1);
                startDate = calendar.getTimeInMillis();
                break;

            case "Custom":
                showCustomDatePicker();
                return;

            default:
                calendar.add(Calendar.MONTH, -1);
                startDate = calendar.getTimeInMillis();
                break;
        }

        updateDateRangeText();
    }

    private void showCustomDatePicker() {
        Calendar calendar = Calendar.getInstance();

        // Start date picker
        DatePickerDialog startPicker = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    Calendar startCal = Calendar.getInstance();
                    startCal.set(year, month, dayOfMonth, 0, 0, 0);
                    startDate = startCal.getTimeInMillis();

                    // End date picker
                    DatePickerDialog endPicker = new DatePickerDialog(
                            this,
                            (view2, year2, month2, dayOfMonth2) -> {
                                Calendar endCal = Calendar.getInstance();
                                endCal.set(year2, month2, dayOfMonth2, 23, 59, 59);
                                endDate = endCal.getTimeInMillis();

                                updateDateRangeText();
                                loadOverviewData();
                            },
                            calendar.get(Calendar.YEAR),
                            calendar.get(Calendar.MONTH),
                            calendar.get(Calendar.DAY_OF_MONTH)
                    );
                    endPicker.setTitle("Select end date");
                    endPicker.show();
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );
        startPicker.setTitle("Select start date");
        startPicker.show();
    }

    private void updateDateRangeText() {
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
        String dateRangeStr = sdf.format(startDate) + " - " + sdf.format(endDate);
        textDateRange.setText(dateRangeStr);
    }

    private void loadOverviewData() {
        String userId = authManager.getCurrentUser();
        if (userId == null) {
            return;
        }

        // Show loading state
        textOverviewTotal.setText("Loading...");

        executor.execute(() -> {
            try {
                // Get total spending
                double totalSpent = reportGenerator.getTotalSpent(userId, startDate, endDate);

                // Get category totals
                Map<String, Double> categoryTotals = reportGenerator.getTotalsByCategory(userId, startDate, endDate);

                // Get budgets
                List<Budget> budgets = dbHelper.getBudgets(userId);
                Map<String, Budget> budgetMap = new java.util.HashMap<>();
                for (Budget budget : budgets) {
                    budgetMap.put(budget.getCategory(), budget);
                }

                // Create summaries
                List<ReportGenerator.CategorySummary> newSummaries = new ArrayList<>();
                for (Map.Entry<String, Double> entry : categoryTotals.entrySet()) {
                    String category = entry.getKey();
                    double total = entry.getValue();
                    double percentage = totalSpent > 0 ? (total / totalSpent) * 100 : 0;

                    ReportGenerator.CategorySummary summary = new ReportGenerator.CategorySummary(category, total, percentage);

                    // Add budget info if exists
                    if (budgetMap.containsKey(category)) {
                        Budget budget = budgetMap.get(category);
                        summary.hasBudget = true;
                        summary.budgetLimit = budget.getLimitAmount();
                        summary.budgetSpent = budget.getCurrentSpent();
                        summary.budgetProgress = budget.getProgressPercent();
                    }

                    newSummaries.add(summary);
                }

                // Update UI on main thread
                runOnUiThread(() -> {
                    textOverviewTotal.setText(String.format(Locale.getDefault(), "$%.2f", totalSpent));

                    summaries.clear();
                    summaries.addAll(newSummaries);
                    adapter.notifyDataSetChanged();

                    // Update PieChart
                    ChartUtils.setupPieChart(pieChart, categoryTotals);

                    Log.d(TAG, "Overview data loaded: $" + totalSpent + ", " + summaries.size() + " categories");
                });

            } catch (Exception e) {
                Log.e(TAG, "Error loading overview data", e);
                runOnUiThread(() -> {
                    Toast.makeText(OverviewActivity.this, R.string.error_loading_overview, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void openDetailReport(String category) {
        Intent intent = new Intent(this, ReportDetailActivity.class);
        intent.putExtra("category", category);
        intent.putExtra("startDate", startDate);
        intent.putExtra("endDate", endDate);
        startActivity(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        authManager.updateLastActivityTimestamp();
        // Reload data to reflect any changes
        loadOverviewData();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Shutdown executor
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
        }

        // Clean up resources
        if (dbHelper != null) {
            dbHelper.close();
        }
        if (reportGenerator != null) {
            reportGenerator.close();
        }

        authManager = null;
        dbHelper = null;
        reportGenerator = null;
    }
}
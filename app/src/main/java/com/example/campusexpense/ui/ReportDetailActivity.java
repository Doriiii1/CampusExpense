package com.example.campusexpense.ui;

import android.content.Intent;
import android.net.Uri;
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
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.campusexpense.R;
import com.example.campusexpense.auth.AuthManager;
import com.example.campusexpense.model.Expense;
import com.example.campusexpense.reports.ReportGenerator;
import com.example.campusexpense.ui.ExpenseListActivity;
import com.example.campusexpense.ui.adapters.ReportExpenseAdapter;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * ReportDetailActivity - Detailed expense report with CSV export
 * Phase 4: Overview and Reports
 *
 * Features:
 * - Expense list for specific category/date range
 * - Statistical summary (count, sum, avg, min, max)
 * - Sort by date or amount
 * - CSV export via FileProvider
 * - Share via ACTION_SEND
 *
 * Threading: DB queries and file I/O run on ExecutorService
 * Security: No external storage, CSV in cache directory
 */
public class ReportDetailActivity extends AppCompatActivity {

    private static final String TAG = "ReportDetailActivity";

    private TextView textReportTitle;
    private TextView textReportSummary;
    private Spinner spinnerSort;
    private Button btnExportCsv;
    private RecyclerView rvExpenses;

    private AuthManager authManager;
    private ReportGenerator reportGenerator;
    private ReportExpenseAdapter adapter;
    private ExecutorService executor;

    private List<Expense> expenses;
    private String category;
    private long startDate;
    private long endDate;
    private String sortBy = "date";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report_detail);

        // Initialize managers
        authManager = new AuthManager(this);
        reportGenerator = new ReportGenerator(this);
        executor = Executors.newSingleThreadExecutor();

        // Check session
        if (!authManager.isSessionValid()) {
            finish();
            return;
        }

        // Get intent extras
        category = getIntent().getStringExtra("category");
        startDate = getIntent().getLongExtra("startDate", 0);
        endDate = getIntent().getLongExtra("endDate", System.currentTimeMillis());

        // Initialize views
        initViews();

        // Set up sort spinner
        setupSortSpinner();

        // Set up RecyclerView
        setupRecyclerView();

        // Set up export button
        btnExportCsv.setOnClickListener(v -> exportCsv());

        // Set title
        setTitle(category != null ? category + " Report" : "Expense Report");
        textReportTitle.setText(category != null ? category : "All Categories");

        // Load data
        loadReportData();
    }

    private void initViews() {
        textReportTitle = findViewById(R.id.textReportTitle);
        textReportSummary = findViewById(R.id.textReportSummary);
        spinnerSort = findViewById(R.id.spinnerSort);
        btnExportCsv = findViewById(R.id.btnExportCsv);
        rvExpenses = findViewById(R.id.rvReportExpenses);
    }

    private void setupSortSpinner() {
        String[] sortOptions = {"Sort by Date", "Sort by Amount"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, sortOptions);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSort.setAdapter(adapter);

        spinnerSort.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                sortBy = position == 0 ? "date" : "amount";
                loadReportData();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private void setupRecyclerView() {
        expenses = new ArrayList<>();
        adapter = new ReportExpenseAdapter(expenses, sortBy);

        rvExpenses.setLayoutManager(new LinearLayoutManager(this));
        rvExpenses.setAdapter(adapter);
    }

    private void loadReportData() {
        String userId = authManager.getCurrentUser();
        if (userId == null) {
            return;
        }

        executor.execute(() -> {
            try {
                // Get expenses
                List<Expense> loadedExpenses = reportGenerator.getExpensesForReport(
                        userId, category, startDate, endDate, sortBy
                );

                // Get statistics
                ReportGenerator.CategoryStats stats = null;
                if (category != null) {
                    stats = reportGenerator.getStatsForCategory(userId, category, startDate, endDate);
                }

                final ReportGenerator.CategoryStats finalStats = stats;

                // Update UI on main thread
                runOnUiThread(() -> {
                    expenses.clear();
                    expenses.addAll(loadedExpenses);
                    adapter.notifyDataSetChanged();

                    // Update summary
                    if (finalStats != null) {
                        String summary = String.format(Locale.getDefault(),
                                "Count: %d | Total: $%.2f | Avg: $%.2f | Min: $%.2f | Max: $%.2f",
                                finalStats.count, finalStats.sum, finalStats.avg, finalStats.min, finalStats.max
                        );
                        textReportSummary.setText(summary);
                    } else {
                        textReportSummary.setText(String.format(Locale.getDefault(),
                                "%d expenses found", loadedExpenses.size()));
                    }

                    Log.d(TAG, "Report data loaded: " + loadedExpenses.size() + " expenses");
                });

            } catch (Exception e) {
                Log.e(TAG, "Error loading report data", e);
                runOnUiThread(() -> {
                    Toast.makeText(ReportDetailActivity.this, R.string.error_loading_report, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void exportCsv() {
        if (expenses.isEmpty()) {
            Toast.makeText(this, R.string.no_data_to_export, Toast.LENGTH_SHORT).show();
            return;
        }

        executor.execute(() -> {
            try {
                // Generate CSV string
                String csvContent = reportGenerator.generateCsv(expenses);

                // Create file in cache directory
                File cacheDir = new File(getCacheDir(), "reports");
                if (!cacheDir.exists()) {
                    cacheDir.mkdirs();
                }

                SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US);
                String filename = "expense_report_" + sdf.format(System.currentTimeMillis()) + ".csv";
                File csvFile = new File(cacheDir, filename);

                // Write CSV to file
                FileOutputStream fos = new FileOutputStream(csvFile);
                fos.write(csvContent.getBytes());
                fos.close();

                // Get URI via FileProvider
                Uri csvUri = FileProvider.getUriForFile(
                        this,
                        getApplicationContext().getPackageName() + ".fileprovider",
                        csvFile
                );

                runOnUiThread(() -> {
                    // Create share intent
                    Intent shareIntent = new Intent(Intent.ACTION_SEND);
                    shareIntent.setType("text/csv");
                    shareIntent.putExtra(Intent.EXTRA_STREAM, csvUri);
                    shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Expense Report - " +
                            (category != null ? category : "All"));
                    shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                    startActivity(Intent.createChooser(shareIntent, "Export CSV Report"));

                    Toast.makeText(this, R.string.csv_exported, Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "CSV exported: " + filename);
                });

            } catch (Exception e) {
                Log.e(TAG, "Error exporting CSV", e);
                runOnUiThread(() -> {
                    Toast.makeText(this, R.string.error_exporting_csv, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        authManager.updateLastActivityTimestamp();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Shutdown executor
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
        }

        // Clean up resources
        if (reportGenerator != null) {
            reportGenerator.close();
        }

        authManager = null;
        reportGenerator = null;
    }
}
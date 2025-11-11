package com.example.campusexpense;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.campusexpense.notifications.NotificationHelper;
import com.example.campusexpense.schedule.RecurringManager;
import com.example.campusexpense.ui.BudgetActivity;
import com.example.campusexpense.ui.ExpenseListActivity;
import com.example.campusexpense.ui.OverviewActivity;

/**
 * MainActivity - Launch activity for CampusExpense Manager
 * Phase 0: Bootstrap skeleton with welcome message
 *
 * Lifecycle: Standard activity lifecycle
 * Phase 2: Added navigation to ExpenseListActivity
 * Phase 3: Added navigation to BudgetActivity, recurring scheduler, and notification channels
 */
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Phase 0: Simple welcome screen
        // Phase 2: Add navigation to ExpenseListActivity
        Button btnExpenses = findViewById(R.id.btnExpenses);
        btnExpenses.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, ExpenseListActivity.class);
            startActivity(intent);
        });

        // Phase 3: Add navigation to BudgetActivity
        Button btnBudgets = findViewById(R.id.btnBudgets);
        btnBudgets.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, BudgetActivity.class);
            startActivity(intent);
        });

        Button btnOverview = findViewById(R.id.btnOverview);
        btnOverview.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, OverviewActivity.class);
            startActivity(intent);
        });

        // Phase 3: Initialize recurring expense scheduler
        RecurringManager recurringManager = new RecurringManager(this);
        recurringManager.schedulePeriodicCheck();

        // Phase 3: Create notification channels
        NotificationHelper.createNotificationChannels(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Future: Stop any animations or timers here
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Future: Clean up resources here
    }
}
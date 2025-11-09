package com.example.campusexpense;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

/**
 * MainActivity - Launch activity for CampusExpense Manager
 * Phase 0: Bootstrap skeleton with welcome message
 *
 * Lifecycle: Standard activity lifecycle
 * No business logic implemented in this phase
 */
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Phase 0: Simple welcome screen
        // Future phases will add navigation to login/expense screens
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
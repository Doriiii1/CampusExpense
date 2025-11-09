package com.example.campusexpense.ui;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.campusexpense.R;
import com.example.campusexpense.auth.AuthManager;
import com.example.campusexpense.db.DatabaseHelper;
import com.example.campusexpense.model.Expense;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

/**
 * AddEditExpenseActivity - Form for creating/editing expenses
 * Phase 2: Validation and database operations
 *
 * Features:
 * - Add new expense or edit existing
 * - Category selection via Spinner
 * - Amount input with validation
 * - Date picker with calendar icon
 * - Save and Cancel actions
 * - Input validation with error messages
 *
 * Lifecycle: Standard activity lifecycle with result passing
 */
public class AddEditExpenseActivity extends AppCompatActivity {

    private static final String TAG = "AddEditExpenseActivity";

    private Spinner spinnerCategory;
    private EditText editDescription;
    private EditText editAmount;
    private EditText editDate;
    private ImageButton btnDatePicker;
    private EditText editNotes;
    private Button btnSave;
    private Button btnCancel;

    private DatabaseHelper dbHelper;
    private AuthManager authManager;
    private Expense existingExpense;
    private long selectedDateMillis;

    private boolean isEditMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit_expense);

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

        // Check if editing existing expense
        checkEditMode();

        // Set up category spinner
        setupCategorySpinner();

        // Set up date picker
        setupDatePicker();

        // Set up buttons
        setupButtons();

        // Set title
        setTitle(isEditMode ? R.string.edit_expense : R.string.add_expense);
    }

    private void initViews() {
        spinnerCategory = findViewById(R.id.spinnerCategory);
        editDescription = findViewById(R.id.editDescription);
        editAmount = findViewById(R.id.editAmount);
        editDate = findViewById(R.id.editDate);
        btnDatePicker = findViewById(R.id.btnDatePicker);
        editNotes = findViewById(R.id.editNotes);
        btnSave = findViewById(R.id.btnSave);
        btnCancel = findViewById(R.id.btnCancel);
    }

    private void checkEditMode() {
        if (getIntent().hasExtra("expense")) {
            existingExpense = getIntent().getParcelableExtra("expense");
            if (existingExpense != null) {
                isEditMode = true;
                populateFields();
            }
        }

        if (!isEditMode) {
            // Set default date to today
            selectedDateMillis = System.currentTimeMillis();
            updateDateField();
        }
    }

    private void setupCategorySpinner() {
        String[] categories = getResources().getStringArray(R.array.expense_categories_no_all);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, categories);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(adapter);
    }

    private void setupDatePicker() {
        // Make EditText non-editable, only via picker
        editDate.setFocusable(false);
        editDate.setClickable(true);

        View.OnClickListener datePickerListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDatePickerDialog();
            }
        };

        editDate.setOnClickListener(datePickerListener);
        btnDatePicker.setOnClickListener(datePickerListener);
    }

    private void setupButtons() {
        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveExpense();
            }
        });

        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    private void populateFields() {
        if (existingExpense == null) {
            return;
        }

        // Set category
        String[] categories = getResources().getStringArray(R.array.expense_categories_no_all);
        for (int i = 0; i < categories.length; i++) {
            if (categories[i].equals(existingExpense.getCategory())) {
                spinnerCategory.setSelection(i);
                break;
            }
        }

        // Set other fields
        editDescription.setText(existingExpense.getDescription());
        editAmount.setText(String.format(Locale.getDefault(), "%.2f", existingExpense.getAmount()));
        editNotes.setText(existingExpense.getNotes());

        selectedDateMillis = existingExpense.getDate();
        updateDateField();
    }

    private void showDatePickerDialog() {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(selectedDateMillis);

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    Calendar selectedCal = Calendar.getInstance();
                    selectedCal.set(year, month, dayOfMonth);
                    selectedDateMillis = selectedCal.getTimeInMillis();
                    updateDateField();
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );

        datePickerDialog.show();
    }

    private void updateDateField() {
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
        editDate.setText(sdf.format(selectedDateMillis));
    }

    private void saveExpense() {
        // Validate inputs
        String category = spinnerCategory.getSelectedItem().toString();
        String description = editDescription.getText().toString().trim();
        String amountStr = editAmount.getText().toString().trim();
        String notes = editNotes.getText().toString().trim();

        // Validation: Description
        if (description.isEmpty()) {
            Toast.makeText(this, R.string.error_description_required, Toast.LENGTH_SHORT).show();
            editDescription.requestFocus();
            return;
        }

        // Validation: Amount
        if (amountStr.isEmpty()) {
            Toast.makeText(this, R.string.error_amount_required, Toast.LENGTH_SHORT).show();
            editAmount.requestFocus();
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(amountStr);
            if (amount <= 0) {
                Toast.makeText(this, R.string.error_amount_must_be_positive, Toast.LENGTH_SHORT).show();
                editAmount.requestFocus();
                return;
            }
        } catch (NumberFormatException e) {
            Toast.makeText(this, R.string.error_invalid_amount, Toast.LENGTH_SHORT).show();
            editAmount.requestFocus();
            return;
        }

        // Get current user
        String userId = authManager.getCurrentUser();
        if (userId == null) {
            Toast.makeText(this, R.string.error_session_expired, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Create or update expense
        Expense expense;
        if (isEditMode && existingExpense != null) {
            expense = existingExpense;
        } else {
            expense = new Expense();
        }

        expense.setUserId(userId);
        expense.setCategory(category);
        expense.setDescription(description);
        expense.setAmount(amount);
        expense.setDate(selectedDateMillis);
        expense.setNotes(notes);

        // Save to database
        try {
            long result;
            if (isEditMode) {
                result = dbHelper.updateExpense(expense);
                if (result > 0) {
                    Toast.makeText(this, R.string.expense_updated, Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "Expense updated successfully");
                } else {
                    Toast.makeText(this, R.string.error_updating_expense, Toast.LENGTH_SHORT).show();
                    return;
                }
            } else {
                result = dbHelper.insertExpense(expense);
                if (result > 0) {
                    Toast.makeText(this, R.string.expense_added, Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "Expense added successfully with ID: " + result);
                } else {
                    Toast.makeText(this, R.string.error_adding_expense, Toast.LENGTH_SHORT).show();
                    return;
                }
            }

            // Return success
            setResult(RESULT_OK);
            finish();

        } catch (Exception e) {
            Log.e(TAG, "Error saving expense", e);
            Toast.makeText(this, R.string.error_saving_expense, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        authManager.updateLastActivityTimestamp();
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
}
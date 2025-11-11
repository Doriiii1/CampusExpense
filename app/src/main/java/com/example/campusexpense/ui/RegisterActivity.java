package com.example.campusexpense.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.campusexpense.R;
import com.example.campusexpense.auth.AuthManager;

/**
 * RegisterActivity - User registration screen
 * Phase 1: New user creation with password confirmation
 *
 * Features:
 * - Username, password, and confirm password input
 * - Password visibility toggles for both fields
 * - Password match validation
 * - Duplicate username detection
 * - SHA-256 password hashing via AuthManager
 * - Navigation to LoginActivity on success
 *
 * Lifecycle: Standard activity lifecycle
 */
public class RegisterActivity extends AppCompatActivity {

    private EditText editUsername;
    private EditText editPassword;
    private EditText editConfirmPassword;
    private ImageButton btnTogglePassword;
    private ImageButton btnToggleConfirmPassword;
    private Button btnRegister;
    private TextView textLogin;

    private AuthManager authManager;
    private boolean isPasswordVisible = false;
    private boolean isConfirmPasswordVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // Initialize AuthManager
        authManager = new AuthManager(this);

        // Initialize views
        initViews();

        // Set up listeners
        setupListeners();
    }

    private void initViews() {
        editUsername = findViewById(R.id.editUsername);
        editPassword = findViewById(R.id.editPassword);
        editConfirmPassword = findViewById(R.id.editConfirmPassword);
        btnTogglePassword = findViewById(R.id.btnTogglePassword);
        btnToggleConfirmPassword = findViewById(R.id.btnToggleConfirmPassword);
        btnRegister = findViewById(R.id.btnRegister);
        textLogin = findViewById(R.id.textLogin);
    }

    private void setupListeners() {
        // Password visibility toggle
        btnTogglePassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                togglePasswordVisibility();
            }
        });

        // Confirm password visibility toggle
        btnToggleConfirmPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleConfirmPasswordVisibility();
            }
        });

        // Register button
        btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                attemptRegister();
            }
        });

        // Navigate to login
        textLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    private void togglePasswordVisibility() {
        if (isPasswordVisible) {
            // Hide password
            editPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            btnTogglePassword.setImageResource(R.drawable.ic_eye_off);
            isPasswordVisible = false;
        } else {
            // Show password
            editPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
            btnTogglePassword.setImageResource(R.drawable.ic_eye);
            isPasswordVisible = true;
        }
        // Move cursor to end
        editPassword.setSelection(editPassword.getText().length());
    }

    private void toggleConfirmPasswordVisibility() {
        if (isConfirmPasswordVisible) {
            // Hide password
            editConfirmPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            btnToggleConfirmPassword.setImageResource(R.drawable.ic_eye_off);
            isConfirmPasswordVisible = false;
        } else {
            // Show password
            editConfirmPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
            btnToggleConfirmPassword.setImageResource(R.drawable.ic_eye);
            isConfirmPasswordVisible = true;
        }
        // Move cursor to end
        editConfirmPassword.setSelection(editConfirmPassword.getText().length());
    }

    private void attemptRegister() {
        hideKeyboard();
        String username = editUsername.getText().toString().trim();
        String password = editPassword.getText().toString();
        String confirmPassword = editConfirmPassword.getText().toString();

        // Validation: Username
        if (username.isEmpty()) {
            Toast.makeText(this, R.string.error_username_required, Toast.LENGTH_SHORT).show();
            editUsername.requestFocus();
            return;
        }

        if (username.length() < 3) {
            Toast.makeText(this, R.string.error_username_too_short, Toast.LENGTH_SHORT).show();
            editUsername.requestFocus();
            return;
        }

        // Validation: Password
        if (password.isEmpty()) {
            Toast.makeText(this, R.string.error_password_required, Toast.LENGTH_SHORT).show();
            editPassword.requestFocus();
            return;
        }

        if (password.length() < 6) {
            Toast.makeText(this, R.string.error_password_too_short, Toast.LENGTH_SHORT).show();
            editPassword.requestFocus();
            return;
        }

        // Validation: Confirm Password
        if (confirmPassword.isEmpty()) {
            Toast.makeText(this, R.string.error_confirm_password_required, Toast.LENGTH_SHORT).show();
            editConfirmPassword.requestFocus();
            return;
        }

        if (!password.equals(confirmPassword)) {
            Toast.makeText(this, R.string.error_passwords_do_not_match, Toast.LENGTH_SHORT).show();
            editConfirmPassword.setText("");
            editConfirmPassword.requestFocus();
            return;
        }

        // Attempt registration via AuthManager
        boolean success = authManager.register(username, password);

        if (success) {
            Toast.makeText(this, R.string.registration_success, Toast.LENGTH_SHORT).show();
            // Navigate back to login
            finish();
        } else {
            Toast.makeText(this, R.string.error_username_exists, Toast.LENGTH_SHORT).show();
            editUsername.requestFocus();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Future: Stop any animations or timers
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Clean up resources
        authManager = null;
    }
    private void hideKeyboard() {
        View view = this.getCurrentFocus();
        if (view != null) {
            android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }
}
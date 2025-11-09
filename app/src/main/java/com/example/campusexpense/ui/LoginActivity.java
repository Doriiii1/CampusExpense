package com.example.campusexpense.ui;

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
import com.example.campusexpense.MainActivity;
import com.example.campusexpense.R;
import com.example.campusexpense.auth.AuthManager;

/**
 * LoginActivity - User authentication screen
 * Phase 1: Password hashing, session management, auto-logout
 *
 * Features:
 * - Username and password input with validation
 * - Password visibility toggle
 * - SHA-256 password hashing via AuthManager
 * - Session persistence with 30-minute auto-logout
 * - Navigation to MainActivity on success
 *
 * Lifecycle: Standard activity lifecycle with session check on resume
 */
public class LoginActivity extends AppCompatActivity {

    private EditText editUsername;
    private EditText editPassword;
    private ImageButton btnTogglePassword;
    private Button btnLogin;
    private TextView textRegister;

    private AuthManager authManager;
    private boolean isPasswordVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Initialize AuthManager
        authManager = new AuthManager(this);

        // Check if user is already logged in
        if (authManager.isSessionValid()) {
            navigateToMain();
            return;
        }

        // Initialize views
        initViews();

        // Set up listeners
        setupListeners();
    }

    private void initViews() {
        editUsername = findViewById(R.id.editUsername);
        editPassword = findViewById(R.id.editPassword);
        btnTogglePassword = findViewById(R.id.btnTogglePassword);
        btnLogin = findViewById(R.id.btnLogin);
        textRegister = findViewById(R.id.textRegister);
    }

    private void setupListeners() {
        // Password visibility toggle
        btnTogglePassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                togglePasswordVisibility();
            }
        });

        // Login button
        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                attemptLogin();
            }
        });

        // Navigate to registration
        textRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
                startActivity(intent);
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

    private void attemptLogin() {
        String username = editUsername.getText().toString().trim();
        String password = editPassword.getText().toString();

        // Validation
        if (username.isEmpty()) {
            Toast.makeText(this, R.string.error_username_required, Toast.LENGTH_SHORT).show();
            editUsername.requestFocus();
            return;
        }

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

        // Attempt login via AuthManager
        boolean success = authManager.login(username, password);

        if (success) {
            Toast.makeText(this, R.string.login_success, Toast.LENGTH_SHORT).show();
            navigateToMain();
        } else {
            Toast.makeText(this, R.string.error_invalid_credentials, Toast.LENGTH_SHORT).show();
            editPassword.setText("");
            editPassword.requestFocus();
        }
    }

    private void navigateToMain() {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Update last activity timestamp
        authManager.updateLastActivityTimestamp();
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
}
package com.example.campusexpense.auth;

import android.content.Context;
import android.content.SharedPreferences;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * AuthManager - Handles authentication and session management
 * Phase 1: Local storage with SHA-256 hashing and auto-logout
 *
 * Features:
 * - User registration with SHA-256 password hashing
 * - Salt generation for each user (random 16-byte salt)
 * - Login validation with hashed password comparison
 * - Session management via SharedPreferences
 * - Auto-logout after 30 minutes of inactivity
 * - Last activity timestamp tracking
 *
 * Storage: SharedPreferences for user credentials and session state
 * Security: Passwords stored as SHA-256 hash with per-user salt
 *
 * Note: This is a local-only implementation for MVP.
 * Future phases may integrate Android Keystore or server-side auth.
 */
public class AuthManager {

    private static final String PREF_NAME = "CampusExpenseAuth";
    private static final String KEY_IS_LOGGED_IN = "is_logged_in";
    private static final String KEY_CURRENT_USER = "current_user";
    private static final String KEY_LAST_ACTIVITY = "last_activity";
    private static final long SESSION_TIMEOUT_MS = 30 * 60 * 1000; // 30 minutes

    private final SharedPreferences preferences;
    private final Context context;

    public AuthManager(Context context) {
        this.context = context.getApplicationContext();
        this.preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    /**
     * Register a new user
     * @param username User's chosen username
     * @param password User's plain-text password
     * @return true if registration successful, false if username already exists
     */
    public boolean register(String username, String password) {
        // Check if username already exists
        if (userExists(username)) {
            return false;
        }

        // Generate a random salt for this user
        String salt = generateSalt();

        // Hash the password with salt
        String hashedPassword = hashPassword(password, salt);

        // Store username, hashed password, and salt
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString("user_" + username + "_password", hashedPassword);
        editor.putString("user_" + username + "_salt", salt);
        editor.putLong("user_" + username + "_created", System.currentTimeMillis());
        editor.apply();

        return true;
    }

    /**
     * Login with username and password
     * @param username User's username
     * @param password User's plain-text password
     * @return true if credentials are valid and login successful
     */
    public boolean login(String username, String password) {
        // Check if user exists
        if (!userExists(username)) {
            return false;
        }

        // Retrieve stored salt and hashed password
        String storedSalt = preferences.getString("user_" + username + "_salt", null);
        String storedHash = preferences.getString("user_" + username + "_password", null);

        if (storedSalt == null || storedHash == null) {
            return false;
        }

        // Hash the provided password with the stored salt
        String hashedPassword = hashPassword(password, storedSalt);

        // Compare hashes
        if (hashedPassword.equals(storedHash)) {
            // Login successful - create session
            SharedPreferences.Editor editor = preferences.edit();
            editor.putBoolean(KEY_IS_LOGGED_IN, true);
            editor.putString(KEY_CURRENT_USER, username);
            editor.putLong(KEY_LAST_ACTIVITY, System.currentTimeMillis());
            editor.apply();
            return true;
        }

        return false;
    }

    /**
     * Logout current user
     */
    public void logout() {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean(KEY_IS_LOGGED_IN, false);
        editor.remove(KEY_CURRENT_USER);
        editor.remove(KEY_LAST_ACTIVITY);
        editor.apply();
    }

    /**
     * Check if user is currently logged in and session is valid
     * @return true if session is valid (within 30-minute timeout)
     */
    public boolean isSessionValid() {
        boolean isLoggedIn = preferences.getBoolean(KEY_IS_LOGGED_IN, false);

        if (!isLoggedIn) {
            return false;
        }

        // Check session timeout
        long lastActivity = preferences.getLong(KEY_LAST_ACTIVITY, 0);
        long currentTime = System.currentTimeMillis();
        long elapsed = currentTime - lastActivity;

        if (elapsed > SESSION_TIMEOUT_MS) {
            // Session expired - auto-logout
            logout();
            return false;
        }

        return true;
    }

    /**
     * Get current logged-in username
     * @return username or null if not logged in
     */
    public String getCurrentUser() {
        if (isSessionValid()) {
            return preferences.getString(KEY_CURRENT_USER, null);
        }
        return null;
    }

    /**
     * Update last activity timestamp to prevent auto-logout
     * Call this in onResume() of activities
     */
    public void updateLastActivityTimestamp() {
        if (preferences.getBoolean(KEY_IS_LOGGED_IN, false)) {
            SharedPreferences.Editor editor = preferences.edit();
            editor.putLong(KEY_LAST_ACTIVITY, System.currentTimeMillis());
            editor.apply();
        }
    }

    /**
     * Check if username already exists
     * @param username Username to check
     * @return true if user exists
     */
    private boolean userExists(String username) {
        return preferences.contains("user_" + username + "_password");
    }

    /**
     * Generate a random salt for password hashing
     * @return Base64-encoded 16-byte salt
     */
    private String generateSalt() {
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[16];
        random.nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }

    /**
     * Hash password using SHA-256 with salt
     * @param password Plain-text password
     * @param salt Base64-encoded salt
     * @return Base64-encoded SHA-256 hash
     */
    private String hashPassword(String password, String salt) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            // Combine password and salt
            String saltedPassword = password + salt;
            byte[] hash = digest.digest(saltedPassword.getBytes(StandardCharsets.UTF_8));

            // Return Base64-encoded hash
            return Base64.getEncoder().encodeToString(hash);

        } catch (NoSuchAlgorithmException e) {
            // SHA-256 should always be available
            throw new RuntimeException("SHA-256 algorithm not found", e);
        }
    }

    /**
     * Get remaining session time in milliseconds
     * @return milliseconds until auto-logout, or 0 if not logged in
     */
    public long getRemainingSessionTime() {
        if (!preferences.getBoolean(KEY_IS_LOGGED_IN, false)) {
            return 0;
        }

        long lastActivity = preferences.getLong(KEY_LAST_ACTIVITY, 0);
        long currentTime = System.currentTimeMillis();
        long elapsed = currentTime - lastActivity;
        long remaining = SESSION_TIMEOUT_MS - elapsed;

        return Math.max(0, remaining);
    }
}
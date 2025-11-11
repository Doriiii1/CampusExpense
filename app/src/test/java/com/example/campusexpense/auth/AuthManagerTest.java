package com.example.campusexpense.auth;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.test.core.app.ApplicationProvider;
import com.example.campusexpense.test.TestAppInjector;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.junit.Assert.*;

/**
 * AuthManagerTest - Unit tests for authentication logic
 * Phase 6: Testing
 *
 * Tests:
 * - User registration with hashing and salt
 * - Login validation
 * - Session management
 * - Session timeout
 * - Password hashing verification
 *
 * Uses Robolectric for Android Context
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 29)
public class AuthManagerTest {

    private Context context;
    private AuthManager authManager;
    private SharedPreferences preferences;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        authManager = new AuthManager(context);
        preferences = context.getSharedPreferences("CampusExpenseAuth", Context.MODE_PRIVATE);

        // Clear any existing data
        preferences.edit().clear().apply();

        // Reset test time
        TestAppInjector.setNowMillis(0);
    }

    @After
    public void tearDown() {
        preferences.edit().clear().apply();
        TestAppInjector.reset();
    }

    @Test
    public void testRegisterNewUser_Success() {
        // Given
        String username = "testuser";
        String password = "password123";

        // When
        boolean result = authManager.register(username, password);

        // Then
        assertTrue("Registration should succeed for new user", result);
        assertTrue("User should exist after registration",
                preferences.contains("user_" + username + "_password"));
        assertTrue("Salt should be generated",
                preferences.contains("user_" + username + "_salt"));
    }

    @Test
    public void testRegisterDuplicateUser_Fails() {
        // Given
        String username = "testuser";
        String password = "password123";
        authManager.register(username, password);

        // When
        boolean result = authManager.register(username, "newpassword");

        // Then
        assertFalse("Registration should fail for duplicate username", result);
    }

    @Test
    public void testLogin_WithValidCredentials_Success() {
        // Given
        String username = "testuser";
        String password = "password123";
        authManager.register(username, password);

        // When
        boolean result = authManager.login(username, password);

        // Then
        assertTrue("Login should succeed with correct credentials", result);
        assertTrue("User should be logged in",
                preferences.getBoolean("is_logged_in", false));
        assertEquals("Current user should be set", username,
                preferences.getString("current_user", null));
    }

    @Test
    public void testLogin_WithInvalidPassword_Fails() {
        // Given
        String username = "testuser";
        String password = "password123";
        authManager.register(username, password);

        // When
        boolean result = authManager.login(username, "wrongpassword");

        // Then
        assertFalse("Login should fail with incorrect password", result);
        assertFalse("User should not be logged in",
                preferences.getBoolean("is_logged_in", false));
    }

    @Test
    public void testLogin_WithNonexistentUser_Fails() {
        // When
        boolean result = authManager.login("nonexistent", "password");

        // Then
        assertFalse("Login should fail for nonexistent user", result);
    }

    @Test
    public void testPasswordHashing_ProducesDifferentHashesForDifferentUsers() {
        // Given
        String username1 = "user1";
        String username2 = "user2";
        String password = "samepassword";

        // When
        authManager.register(username1, password);
        authManager.register(username2, password);

        // Then
        String hash1 = preferences.getString("user_" + username1 + "_password", "");
        String hash2 = preferences.getString("user_" + username2 + "_password", "");
        String salt1 = preferences.getString("user_" + username1 + "_salt", "");
        String salt2 = preferences.getString("user_" + username2 + "_salt", "");

        assertNotEquals("Salts should be different", salt1, salt2);
        assertNotEquals("Hashes should be different due to different salts", hash1, hash2);
    }

    @Test
    public void testSessionTimeout_AfterThirtyMinutes_Invalid() {
        // Given
        String username = "testuser";
        String password = "password123";
        authManager.register(username, password);
        authManager.login(username, password);

        long loginTime = System.currentTimeMillis();
        TestAppInjector.setNowMillis(loginTime);

        // Update session time
        preferences.edit().putLong("last_activity", loginTime).apply();

        // When - advance time by 31 minutes
        TestAppInjector.setNowMillis(loginTime + (31 * 60 * 1000));

        // Then
        assertFalse("Session should be invalid after 30 minutes",
                authManager.isSessionValid());
        assertFalse("User should be logged out",
                preferences.getBoolean("is_logged_in", false));
    }

    @Test
    public void testSessionTimeout_WithinThirtyMinutes_Valid() {
        // Given
        String username = "testuser";
        String password = "password123";
        authManager.register(username, password);
        authManager.login(username, password);

        long loginTime = System.currentTimeMillis();
        TestAppInjector.setNowMillis(loginTime);
        preferences.edit().putLong("last_activity", loginTime).apply();

        // When - advance time by 29 minutes
        TestAppInjector.setNowMillis(loginTime + (29 * 60 * 1000));

        // Then
        assertTrue("Session should be valid within 30 minutes",
                authManager.isSessionValid());
    }

    @Test
    public void testLogout_ClearsSessionData() {
        // Given
        String username = "testuser";
        String password = "password123";
        authManager.register(username, password);
        authManager.login(username, password);

        // When
        authManager.logout();

        // Then
        assertFalse("User should be logged out",
                preferences.getBoolean("is_logged_in", false));
        assertNull("Current user should be null",
                preferences.getString("current_user", null));
        assertFalse("Session should be invalid",
                authManager.isSessionValid());
    }

    @Test
    public void testGetCurrentUser_WhenLoggedIn_ReturnsUsername() {
        // Given
        String username = "testuser";
        String password = "password123";
        authManager.register(username, password);
        authManager.login(username, password);

        // When
        String currentUser = authManager.getCurrentUser();

        // Then
        assertEquals("Should return current logged-in user", username, currentUser);
    }

    @Test
    public void testGetCurrentUser_WhenLoggedOut_ReturnsNull() {
        // When
        String currentUser = authManager.getCurrentUser();

        // Then
        assertNull("Should return null when not logged in", currentUser);
    }

    @Test
    public void testUpdateLastActivityTimestamp_UpdatesTimestamp() {
        // Given
        String username = "testuser";
        String password = "password123";
        authManager.register(username, password);
        authManager.login(username, password);

        long initialTime = System.currentTimeMillis();
        TestAppInjector.setNowMillis(initialTime);
        preferences.edit().putLong("last_activity", initialTime).apply();

        // When - advance time and update
        long newTime = initialTime + 10000;
        TestAppInjector.setNowMillis(newTime);
        authManager.updateLastActivityTimestamp();

        // Then
        long storedTime = preferences.getLong("last_activity", 0);
        assertTrue("Timestamp should be updated", storedTime >= newTime);
    }
}
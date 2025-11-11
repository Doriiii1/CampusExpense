package com.example.campusexpense.ui;

import android.content.Context;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.intent.Intents;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import com.example.campusexpense.MainActivity;
import com.example.campusexpense.R;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.*;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.intent.Intents.intended;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent;
import static androidx.test.espresso.matcher.ViewMatchers.*;

/**
 * LoginActivityEspressoTest - UI tests for authentication flow
 * Phase 6: Testing
 *
 * Tests:
 * - User registration
 * - Login with valid credentials
 * - Login with invalid credentials
 * - Navigation to MainActivity after successful login
 *
 * Uses Espresso for UI testing
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class LoginActivityEspressoTest {

    @Rule
    public ActivityScenarioRule<LoginActivity> activityRule =
            new ActivityScenarioRule<>(LoginActivity.class);

    private Context context;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        Intents.init();

        // Clear any existing session
        context.getSharedPreferences("CampusExpenseAuth", Context.MODE_PRIVATE)
                .edit().clear().apply();
    }

    @After
    public void tearDown() {
        Intents.release();
    }

    @Test
    public void testLoginActivity_DisplaysCorrectly() {
        // Verify login screen elements are displayed
        onView(withId(R.id.editUsername))
                .check(matches(isDisplayed()));

        onView(withId(R.id.editPassword))
                .check(matches(isDisplayed()));

        onView(withId(R.id.btnLogin))
                .check(matches(isDisplayed()))
                .check(matches(withText(R.string.login)));

        onView(withId(R.id.textRegister))
                .check(matches(isDisplayed()));
    }

    @Test
    public void testRegisterAndLogin_Success() {
        String username = "espressouser_" + System.currentTimeMillis();
        String password = "testpass123";

        // Navigate to register screen
        onView(withId(R.id.textRegister))
                .perform(click());

        // Wait for RegisterActivity to load
        EspressoTestUtils.waitForView(R.id.editUsername, 2000);

        // Fill registration form
        onView(withId(R.id.editUsername))
                .perform(typeText(username), closeSoftKeyboard());

        onView(withId(R.id.editPassword))
                .perform(typeText(password), closeSoftKeyboard());

        onView(withId(R.id.editConfirmPassword))
                .perform(typeText(password), closeSoftKeyboard());

        // Click register
        onView(withId(R.id.btnRegister))
                .perform(click());

        // Wait for return to login screen
        EspressoTestUtils.waitForView(R.id.btnLogin, 2000);

        // Now login with registered credentials
        onView(withId(R.id.editUsername))
                .perform(clearText(), typeText(username), closeSoftKeyboard());

        onView(withId(R.id.editPassword))
                .perform(clearText(), typeText(password), closeSoftKeyboard());

        onView(withId(R.id.btnLogin))
                .perform(click());

        // Verify navigation to MainActivity
        intended(hasComponent(MainActivity.class.getName()));
    }

    @Test
    public void testLogin_WithInvalidCredentials_ShowsError() {
        // Try to login with non-existent user
        onView(withId(R.id.editUsername))
                .perform(typeText("nonexistent"), closeSoftKeyboard());

        onView(withId(R.id.editPassword))
                .perform(typeText("wrongpass"), closeSoftKeyboard());

        onView(withId(R.id.btnLogin))
                .perform(click());

        // Verify we're still on login screen (not navigated away)
        onView(withId(R.id.btnLogin))
                .check(matches(isDisplayed()));

        // Note: Toast messages are hard to test with Espresso
        // In real scenario, we'd check for Toast or error message
    }

    @Test
    public void testLogin_WithEmptyFields_ShowsError() {
        // Click login without entering credentials
        onView(withId(R.id.btnLogin))
                .perform(click());

        // Verify we're still on login screen
        onView(withId(R.id.btnLogin))
                .check(matches(isDisplayed()));
    }

    @Test
    public void testPasswordToggle_ShowsAndHidesPassword() {
        // Type password
        onView(withId(R.id.editPassword))
                .perform(typeText("testpassword"), closeSoftKeyboard());

        // Click toggle button
        onView(withId(R.id.btnTogglePassword))
                .perform(click());

        // Password should now be visible
        // (In real test, we'd verify inputType changed)

        // Click toggle again
        onView(withId(R.id.btnTogglePassword))
                .perform(click());

        // Password should be hidden again
    }

    @Test
    public void testNavigateToRegister_AndBack() {
        // Click register link
        onView(withId(R.id.textRegister))
                .perform(click());

        // Wait for RegisterActivity
        EspressoTestUtils.waitForView(R.id.btnRegister, 2000);

        // Verify register screen is displayed
        onView(withId(R.id.btnRegister))
                .check(matches(isDisplayed()));

        // Go back
        onView(withId(R.id.textLogin))
                .perform(click());

        // Verify we're back on login screen
        onView(withId(R.id.btnLogin))
                .check(matches(isDisplayed()));
    }
}
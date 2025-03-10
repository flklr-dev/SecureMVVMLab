package com.example.securemvvm.ui.registration

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.example.securemvvm.MainActivity
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performClick

@HiltAndroidTest
class RegistrationScreenTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setup() {
        hiltRule.inject()
        // Navigate to registration screen
        composeTestRule.onNodeWithText("Don't have an account? Register")
            .performClick()
    }

    @Test
    fun showsPasswordRequirements() {
        composeTestRule.apply {
            // Focus password field
            onNodeWithTag("password_input")
                .performTextInput("a")

            // Verify requirements are shown
            onNodeWithText("At least 8 characters").assertIsDisplayed()
            onNodeWithText("At least one uppercase letter").assertIsDisplayed()
            onNodeWithText("At least one lowercase letter").assertIsDisplayed()
            onNodeWithText("At least one number").assertIsDisplayed()
            onNodeWithText("At least one special character").assertIsDisplayed()
        }
    }

    @Test
    fun passwordMismatchShowsError() {
        composeTestRule.apply {
            // Enter valid email
            onNodeWithTag("email_input")
                .performTextInput("test@example.com")

            // Enter different passwords
            onNodeWithTag("password_input")
                .performTextInput("StrongPass123!")
            onNodeWithTag("confirm_password_input")
                .performTextInput("DifferentPass123!")

            // Click register button
            onNodeWithTag("register_button")
                .performClick()

            // Verify error message
            onNodeWithText("Passwords do not match")
                .assertIsDisplayed()
        }
    }

    @Test
    fun successfulRegistrationFlow() {
        composeTestRule.apply {
            // Enter valid registration data
            onNodeWithTag("email_input")
                .performTextInput("test@example.com")
            onNodeWithTag("password_input")
                .performTextInput("StrongPass123!")
            onNodeWithTag("confirm_password_input")
                .performTextInput("StrongPass123!")

            // Click register button
            onNodeWithTag("register_button")
                .performClick()

            // Verify success and navigation
            onNodeWithTag("login_screen")
                .assertIsDisplayed()
        }
    }
} 
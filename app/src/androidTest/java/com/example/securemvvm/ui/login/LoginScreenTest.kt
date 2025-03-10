package com.example.securemvvm.ui.login

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.example.securemvvm.MainActivity
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@HiltAndroidTest
class LoginScreenTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setup() {
        hiltRule.inject()
    }

    @Test
    fun showsLoadingIndicatorDuringLogin() {
        composeTestRule.apply {
            // Enter valid credentials
            onNodeWithTag("email_input")
                .performTextInput("test@example.com")
            onNodeWithTag("password_input")
                .performTextInput("Password123!")

            // Click login button
            onNodeWithTag("login_button")
                .performClick()

            // Verify loading indicator is shown
            onNodeWithTag("loading_indicator")
                .assertIsDisplayed()
        }
    }

    @Test
    fun invalidEmailShowsError() {
        composeTestRule.apply {
            // Enter invalid email
            onNodeWithTag("email_input")
                .performTextInput("invalid.email")
                .performClick()

            // Verify error message is shown
            onNodeWithText("Please enter a valid email address")
                .assertIsDisplayed()
        }
    }

    @Test
    fun emptyFieldsShowErrors() {
        composeTestRule.apply {
            // Click login without entering data
            onNodeWithTag("login_button").performClick()

            // Verify error messages
            onNodeWithText("Email is required").assertIsDisplayed()
            onNodeWithText("Password is required").assertIsDisplayed()
        }
    }

    @Test
    fun validInputEnablesLoginButton() {
        composeTestRule.apply {
            // Enter valid credentials
            onNodeWithTag("email_input")
                .performTextInput("test@example.com")
            onNodeWithTag("password_input")
                .performTextInput("ValidPass123!")

            // Verify login button is enabled
            onNodeWithTag("login_button")
                .assertIsEnabled()
        }
    }

    @Test
    fun loginFlowWithInvalidCredentials() {
        composeTestRule.apply {
            // Enter invalid credentials
            onNodeWithTag("email_input")
                .performTextInput("invalid@email")
            onNodeWithTag("password_input")
                .performTextInput("weak")

            // Click login button
            onNodeWithTag("login_button")
                .performClick()

            // Verify error state
            onNodeWithText("Invalid email or password")
                .assertIsDisplayed()
        }
    }

    @Test
    fun navigateToRegistrationScreen() {
        composeTestRule.apply {
            // Click on registration link
            onNodeWithText("Don't have an account? Register")
                .performClick()

            // Verify navigation to registration screen
            onNodeWithTag("registration_screen")
                .assertIsDisplayed()
        }
    }

    @Test
    fun passwordVisibilityToggle() {
        composeTestRule.apply {
            // Enter password
            onNodeWithTag("password_input")
                .performTextInput("Password123!")

            // Toggle password visibility
            onNodeWithTag("password_visibility_toggle")
                .performClick()

            // Verify password is visible
            onNodeWithTag("password_input")
                .assertTextEquals("Password123!")
        }
    }

    @Test
    fun loginFlowWithValidCredentials() {
        composeTestRule.apply {
            // Enter valid credentials
            onNodeWithTag("email_input")
                .performTextInput("test@example.com")
            onNodeWithTag("password_input")
                .performTextInput("StrongPass123!")

            // Click login button
            onNodeWithTag("login_button")
                .performClick()

            // Verify successful login (navigation to home screen)
            onNodeWithTag("home_screen")
                .assertIsDisplayed()
        }
    }

    @Test
    fun errorClearingOnInput() {
        composeTestRule.apply {
            // Enter invalid email and submit
            onNodeWithTag("email_input")
                .performTextInput("invalid")
            onNodeWithTag("login_button")
                .performClick()

            // Verify error is shown
            onNodeWithText("Please enter a valid email address")
                .assertIsDisplayed()

            // Enter valid email
            onNodeWithTag("email_input")
                .performTextReplacement("test@example.com")

            // Verify error is cleared
            onNodeWithText("Please enter a valid email address")
                .assertDoesNotExist()
        }
    }

    @Test
    fun biometricPromptDisplayed() {
        composeTestRule.apply {
            // Click biometric login button
            onNodeWithTag("biometric_login_button")
                .performClick()

            // Verify biometric prompt is shown
            onNodeWithTag("biometric_prompt")
                .assertIsDisplayed()
        }
    }
} 
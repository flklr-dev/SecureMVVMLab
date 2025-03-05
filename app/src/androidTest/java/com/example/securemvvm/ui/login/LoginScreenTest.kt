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
} 
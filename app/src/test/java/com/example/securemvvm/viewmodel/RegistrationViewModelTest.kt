package com.example.securemvvm.viewmodel

import android.util.Log
import com.example.securemvvm.model.repository.UserPreferencesRepository
import com.example.securemvvm.model.repository.UserRepository
import com.example.securemvvm.viewmodel.utils.ValidationUtils
import com.google.common.truth.Truth.assertThat
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RegistrationViewModelTest {
    private lateinit var viewModel: RegistrationViewModel
    private lateinit var userRepository: UserRepository
    private lateinit var userPreferencesRepository: UserPreferencesRepository
    private lateinit var testDispatcher: TestDispatcher

    @Before
    fun setup() {
        // Mock Android Log class
        mockkStatic(Log::class)
        every { Log.v(any(), any<String>()) } returns 0
        every { Log.d(any(), any<String>()) } returns 0
        every { Log.i(any(), any<String>()) } returns 0
        every { Log.w(any(), any<String>()) } returns 0
        every { Log.e(any(), any<String>()) } returns 0

        testDispatcher = UnconfinedTestDispatcher()
        Dispatchers.setMain(testDispatcher)
        
        userRepository = mockk(relaxed = true)
        userPreferencesRepository = mockk(relaxed = true)
        
        viewModel = RegistrationViewModel(userRepository, userPreferencesRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkStatic(Log::class)
    }

    @Test
    fun `registration fails with invalid email`() = runTest {
        // When
        viewModel.updateEmail("invalid.email")
        viewModel.updatePassword("StrongPass123!")
        viewModel.updateConfirmPassword("StrongPass123!")
        viewModel.register()

        // Then
        assertThat(viewModel.registrationState.first()).isInstanceOf(RegistrationState.Error::class.java)
    }

    @Test
    fun `registration fails with weak password`() = runTest {
        // When
        viewModel.updateEmail("test@example.com")
        viewModel.updatePassword("weak")
        viewModel.updateConfirmPassword("weak")
        viewModel.register()

        // Then
        assertThat(viewModel.registrationState.first()).isInstanceOf(RegistrationState.Error::class.java)
    }

    @Test
    fun `registration fails with mismatched passwords`() = runTest {
        // When
        viewModel.updateEmail("test@example.com")
        viewModel.updatePassword("StrongPass123!")
        viewModel.updateConfirmPassword("DifferentPass123!")
        viewModel.register()

        // Then
        assertThat(viewModel.registrationState.first()).isInstanceOf(RegistrationState.Error::class.java)
    }

    @Test
    fun `registration succeeds with valid credentials`() = runTest {
        // Given
        coEvery { userRepository.registerUser(any(), any()) } returns Result.success(true)

        // When
        viewModel.updateEmail("test@example.com")
        viewModel.updatePassword("StrongPass123!")
        viewModel.updateConfirmPassword("StrongPass123!")
        viewModel.register()

        // Then
        assertThat(viewModel.registrationState.first()).isInstanceOf(RegistrationState.Success::class.java)
    }

    @Test
    fun `password validation checks all requirements`() {
        // Test minimum length
        viewModel.updatePassword("Short1!")
        assertThat(viewModel.hasMinLength).isFalse()

        // Test uppercase requirement
        viewModel.updatePassword("lowercase123!")
        assertThat(viewModel.hasUppercase).isFalse()

        // Test lowercase requirement
        viewModel.updatePassword("UPPERCASE123!")
        assertThat(viewModel.hasLowercase).isFalse()

        // Test number requirement
        viewModel.updatePassword("NoNumbers!")
        assertThat(viewModel.hasNumber).isFalse()

        // Test special character requirement
        viewModel.updatePassword("NoSpecial123")
        assertThat(viewModel.hasSpecialChar).isFalse()

        // Test valid password
        viewModel.updatePassword("ValidPass123!")
        assertThat(viewModel.isPasswordValid).isTrue()
    }

    @Test
    fun `form validation requires all fields to be valid`() {
        // When
        viewModel.updateEmail("test@example.com")
        viewModel.updatePassword("StrongPass123!")
        viewModel.updateConfirmPassword("StrongPass123!")

        // Then
        assertThat(viewModel.isFormValid).isTrue()

        // When password confirmation doesn't match
        viewModel.updateConfirmPassword("DifferentPass123!")

        // Then
        assertThat(viewModel.isFormValid).isFalse()
    }

    @Test
    fun `registration shows loading state during process`() = runTest {
        // Given
        coEvery { userRepository.registerUser(any(), any()) } coAnswers {
            kotlinx.coroutines.delay(100)
            Result.success(true)
        }

        // When
        viewModel.updateEmail("test@example.com")
        viewModel.updatePassword("StrongPass123!")
        viewModel.updateConfirmPassword("StrongPass123!")
        viewModel.register()

        // Then
        assertThat(viewModel.registrationState.first()).isInstanceOf(RegistrationState.Loading::class.java)
    }

    @Test
    fun `registration handles network error appropriately`() = runTest {
        // Given
        coEvery { userRepository.registerUser(any(), any()) } returns Result.failure(Exception("Network error"))

        // When
        viewModel.updateEmail("test@example.com")
        viewModel.updatePassword("StrongPass123!")
        viewModel.updateConfirmPassword("StrongPass123!")
        viewModel.register()

        // Then
        assertThat(viewModel.registrationState.first()).isInstanceOf(RegistrationState.Error::class.java)
    }

    @Test
    fun `registration handles duplicate email error`() = runTest {
        // Given
        coEvery { userRepository.registerUser(any(), any()) } returns 
            Result.failure(IllegalArgumentException("Email already exists"))

        // When
        viewModel.updateEmail("existing@example.com")
        viewModel.updatePassword("StrongPass123!")
        viewModel.updateConfirmPassword("StrongPass123!")
        viewModel.register()

        // Then
        val state = viewModel.registrationState.first()
        assertThat(state).isInstanceOf(RegistrationState.Error::class.java)
        assertThat((state as RegistrationState.Error).message)
            .contains("email is already registered")
    }
} 
package com.example.securemvvm.viewmodel

import android.util.Log
import androidx.fragment.app.FragmentActivity
import com.example.securemvvm.model.User
import com.example.securemvvm.model.repository.UserPreferencesRepository
import com.example.securemvvm.model.repository.UserRepository
import com.example.securemvvm.model.security.BiometricHelper
import com.example.securemvvm.model.security.SecureStorageManager
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
class LoginViewModelTest {
    private lateinit var viewModel: LoginViewModel
    private lateinit var userRepository: UserRepository
    private lateinit var userPreferencesRepository: UserPreferencesRepository
    private lateinit var biometricHelper: BiometricHelper
    private lateinit var secureStorageManager: SecureStorageManager
    private lateinit var testDispatcher: TestDispatcher
    private lateinit var mockActivity: FragmentActivity

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
        biometricHelper = mockk(relaxed = true)
        secureStorageManager = mockk(relaxed = true)
        mockActivity = mockk(relaxed = true)
        
        viewModel = LoginViewModel(
            userRepository, 
            userPreferencesRepository, 
            biometricHelper,
            secureStorageManager
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkStatic(Log::class)
    }

    @Test
    fun `login fails with invalid email`() = runTest {
        // Given
        coEvery { userRepository.login(any(), any()) } returns Result.failure(Exception("Invalid email"))
        
        // When
        viewModel.updateEmail("invalid.email")
        viewModel.updatePassword("Password123!")
        viewModel.loginWithPassword()

        // Then
        assertThat(viewModel.loginState.first()).isInstanceOf(LoginState.Error::class.java)
    }

    @Test
    fun `login fails with invalid password`() = runTest {
        // When
        viewModel.updateEmail("test@example.com")
        viewModel.updatePassword("weak")
        viewModel.login(mockActivity) {}

        // Then
        assertThat(viewModel.loginState.first()).isInstanceOf(LoginState.Error::class.java)
    }

    @Test
    fun `login succeeds with valid credentials`() = runTest {
        // Given
        val mockUser = mockk<User>(relaxed = true)
        coEvery { 
            userRepository.login(
                any(),
                any()
            ) 
        } returns Result.success(mockUser)

        // When
        viewModel.updateEmail("test@example.com")
        viewModel.updatePassword("Password123!")
        viewModel.loginWithPassword()

        // Then
        assertThat(viewModel.loginState.first()).isInstanceOf(LoginState.Success::class.java)
    }

    @Test
    fun `login shows loading state during authentication`() = runTest {
        // Given
        coEvery { userRepository.login(any(), any()) } coAnswers {
            kotlinx.coroutines.delay(100)
            Result.success(mockk<User>(relaxed = true))
        }

        // When
        viewModel.updateEmail("test@example.com")
        viewModel.updatePassword("Password123!")
        viewModel.loginWithPassword()

        // Then
        assertThat(viewModel.loginState.first()).isInstanceOf(LoginState.Loading::class.java)
    }

    @Test
    fun `login handles network error appropriately`() = runTest {
        // Given
        coEvery { userRepository.login(any(), any()) } returns Result.failure(Exception("Network error"))

        // When
        viewModel.updateEmail("test@example.com")
        viewModel.updatePassword("Password123!")
        viewModel.loginWithPassword()

        // Then
        assertThat(viewModel.loginState.first()).isInstanceOf(LoginState.Error::class.java)
    }

    @Test
    fun `email validation returns false for invalid email`() {
        // Given
        viewModel.updateEmail("invalid.email")

        // Then
        assertThat(ValidationUtils.validateEmail(viewModel.email)).isFalse()
    }

    @Test
    fun `email validation returns true for valid email`() {
        // Given
        viewModel.updateEmail("test@example.com")

        // Then
        assertThat(ValidationUtils.validateEmail(viewModel.email)).isTrue()
    }

    @Test
    fun `password validation fails when empty`() {
        // Given
        viewModel.updatePassword("")

        // Then
        assertThat(ValidationUtils.validatePassword(viewModel.password)).isFalse()
    }

    @Test
    fun `password validation succeeds with valid password`() {
        // Given
        viewModel.updatePassword("StrongPass123!")

        // Then
        assertThat(ValidationUtils.validatePassword(viewModel.password)).isTrue()
    }
} 
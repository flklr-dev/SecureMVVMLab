package com.example.securemvvm.viewmodel

import android.util.Log
import com.example.securemvvm.model.User
import com.example.securemvvm.model.repository.UserPreferencesRepository
import com.example.securemvvm.model.repository.UserRepository
import com.example.securemvvm.model.security.BiometricHelper
import com.example.securemvvm.viewmodel.LoginViewModel
import com.google.common.truth.Truth.assertThat
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
        biometricHelper = mockk(relaxed = true)
        
        viewModel = LoginViewModel(userRepository, userPreferencesRepository, biometricHelper)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkStatic(Log::class)
    }

    @Test
    fun `login fails with invalid email`() = runTest {
        // When
        viewModel.login("invalid.email", "Password123!")

        // Then
        assertThat(viewModel.uiState.value).isInstanceOf(LoginUiState.Error::class.java)
        verify { 
            Log.w(
                any<String>(), 
                any<String>()
            ) 
        }
    }

    @Test
    fun `login fails with invalid password`() = runTest {
        // When
        viewModel.login("test@example.com", "weak")

        // Then
        assertThat(viewModel.uiState.value).isInstanceOf(LoginUiState.Error::class.java)
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
        viewModel.login("test@example.com", "Password123!")

        // Then
        assertThat(viewModel.uiState.value).isInstanceOf(LoginUiState.Success::class.java)
    }

    @Test
    fun `login shows loading state during authentication`() = runTest {
        // Given
        coEvery { userRepository.login(any(), any()) } coAnswers {
            kotlinx.coroutines.delay(100)
            Result.success(mockk<User>(relaxed = true))
        }

        // When
        viewModel.login("test@example.com", "Password123!")

        // Then
        assertThat(viewModel.uiState.value).isInstanceOf(LoginUiState.Loading::class.java)
    }

    @Test
    fun `login handles network error appropriately`() = runTest {
        // Given
        coEvery { userRepository.login(any(), any()) } returns Result.failure(Exception("Network error"))

        // When
        viewModel.login("test@example.com", "Password123!")

        // Then
        assertThat(viewModel.uiState.value).isInstanceOf(LoginUiState.Error::class.java)
    }
} 
package com.example.securemvvm.ui.login

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.securemvvm.viewmodel.LoginViewModel
import com.example.securemvvm.viewmodel.LoginState
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarHost
import kotlinx.coroutines.delay
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.fragment.app.FragmentActivity
import com.example.securemvvm.model.security.BiometricHelper
import com.example.securemvvm.ui.components.OTPDialog
import com.example.securemvvm.navigation.Screen
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.graphics.Color

private object BiometricState {
    var hasPromptedThisSession = false
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    viewModel: LoginViewModel = hiltViewModel(),
    onNavigateToRegister: () -> Unit,
    onLoginSuccess: (String) -> Unit,
    activity: FragmentActivity,
    biometricHelper: BiometricHelper
) {
    var passwordVisible by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val loginState by viewModel.loginState.collectAsState()
    
    var showErrorDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var showOtpDialog by remember { mutableStateOf(false) }
    var remainingTime by remember { mutableStateOf(0L) }
    var canResend by remember { mutableStateOf(false) }
    var showSuccessDialog by remember { mutableStateOf(false) }
    var showFullLoginForm by remember { mutableStateOf(false) }
    var emailError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    
    val lastLoggedInEmail = viewModel.email
    val hasStoredCredentials = viewModel.hasStoredCredentials()

    // Add local state for email
    var emailInput by remember { mutableStateOf("") }
    
    // Modified LaunchedEffect for biometric login
    LaunchedEffect(key1 = Unit) {
        if (hasStoredCredentials && !showFullLoginForm && !BiometricState.hasPromptedThisSession) {
            // Set the flag to true immediately to prevent any possibility of multiple prompts
            BiometricState.hasPromptedThisSession = true
            
            biometricHelper.showBiometricPrompt(
                activity,
                onSuccess = {
                    viewModel.updatePassword("")
                    viewModel.loginWithBiometric { onLoginSuccess(lastLoggedInEmail) }
                },
                onError = { /* Silently fail, let user use password */ }
            )
        }
    }

    // Update email in ViewModel when local state changes
    LaunchedEffect(emailInput) {
        viewModel.updateEmail(emailInput)
    }

    // Clear email when showing full login form
    LaunchedEffect(showFullLoginForm) {
        if (showFullLoginForm) {
            emailInput = ""
            viewModel.updateEmail("")
        }
    }

    // Clear stored credentials when navigating away
    DisposableEffect(Unit) {
        onDispose {
            viewModel.clearStoredCredentials()
            emailInput = ""
            viewModel.updateEmail("")
            viewModel.updatePassword("")
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Login",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(bottom = 24.dp)
            )
            if (!hasStoredCredentials || showFullLoginForm) {
                OutlinedTextField(
                    value = emailInput,
                    onValueChange = { newValue -> 
                        emailInput = newValue
                        emailError = null 
                    },
                    label = { Text("Email") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusEvent { focusState ->
                            if (!focusState.isFocused && emailInput.isNotBlank()) {
                                if (!isValidEmail(emailInput)) {
                                    emailError = "Please enter a valid email address"
                                }
                            }
                        },
                    isError = emailError != null,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next
                    ),
                    supportingText = if (emailError != null) {
                        { Text(text = emailError!!, color = MaterialTheme.colorScheme.error) }
                    } else null,
                    enabled = true
                )

                OutlinedTextField(
                    value = viewModel.password,
                    onValueChange = { newValue -> 
                        viewModel.updatePassword(newValue)
                        passwordError = null
                    },
                    label = { Text("Password") },
                    visualTransformation = if (passwordVisible) 
                        VisualTransformation.None 
                    else 
                        PasswordVisualTransformation(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    isError = passwordError != null,
                    supportingText = if (passwordError != null) {
                        { Text(text = passwordError!!, color = MaterialTheme.colorScheme.error) }
                    } else null,
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) 
                                    Icons.Default.VisibilityOff 
                                else 
                                    Icons.Default.Visibility,
                                contentDescription = if (passwordVisible) 
                                    "Hide password" 
                                else 
                                    "Show password"
                            )
                        }
                    }
                )

                Button(
                    onClick = { 
                        var hasError = false
                        
                        if (emailInput.isBlank()) {
                            emailError = "Email is required"
                            hasError = true
                        } else if (!isValidEmail(emailInput)) {
                            emailError = "Please enter a valid email address"
                            hasError = true
                        }
                        
                        if (viewModel.password.isBlank()) {
                            passwordError = "Password is required"
                            hasError = true
                        }
                        
                        if (!hasError) {
                            viewModel.login(activity) { onLoginSuccess(emailInput) }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                ) {
                    Text("Login")
                }

                TextButton(
                    onClick = onNavigateToRegister,
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Text("Don't have an account? Register")
                }
            } else {
                // Redesigned Quick Login UI
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White // Set pure white background
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "User Account",
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        
                        Text(
                            text = "Continue as",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                        )
                        
                        Text(
                            text = lastLoggedInEmail,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        
                        OutlinedTextField(
                            value = viewModel.password,
                            onValueChange = { newValue -> 
                                viewModel.updatePassword(newValue)
                                passwordError = null
                            },
                            label = { Text("Password") },
                            visualTransformation = if (passwordVisible) 
                                VisualTransformation.None 
                            else 
                                PasswordVisualTransformation(),
                            modifier = Modifier
                                .fillMaxWidth(),
                            isError = passwordError != null,
                            supportingText = if (passwordError != null) {
                                { Text(text = passwordError!!, color = MaterialTheme.colorScheme.error) }
                            } else null,
                            trailingIcon = {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(
                                        imageVector = if (passwordVisible) 
                                            Icons.Default.VisibilityOff 
                                        else 
                                            Icons.Default.Visibility,
                                        contentDescription = if (passwordVisible) 
                                            "Hide password" 
                                        else 
                                            "Show password"
                                    )
                                }
                            },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Done
                            )
                        )
                        
                        Button(
                            onClick = {
                                if (viewModel.password.isBlank()) {
                                    passwordError = "Password is required"
                                } else {
                                    // Quick login doesn't need OTP
                                    viewModel.quickLogin(activity) { 
                                        onLoginSuccess(lastLoggedInEmail)
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp)
                        ) {
                            Text("Login")
                        }
                        
                        TextButton(
                            onClick = { 
                                showFullLoginForm = true
                                viewModel.clearStoredCredentials()
                                viewModel.updateEmail("")
                            },
                            modifier = Modifier.padding(top = 8.dp)
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SwapHoriz,
                                    contentDescription = "Switch Account",
                                    modifier = Modifier.padding(end = 8.dp)
                                )
                                Text("Use different account")
                            }
                        }
                    }
                }
            }
        }
    }

    // Handle OTP Dialog
    if (showOtpDialog) {
        OTPDialog(
            isVisible = showOtpDialog,
            onDismiss = { showOtpDialog = false },
            onVerify = { inputOtp ->
                if (viewModel.verifyOTP(inputOtp)) {
                    showOtpDialog = false
                    showSuccessDialog = true
                }
            },
            onResendOTP = { viewModel.requestOTP() },
            remainingTime = remainingTime,
            attempts = 3
        )
    }

    // Handle login state changes
    LaunchedEffect(loginState) {
        when (loginState) {
            is LoginState.Initial -> {
                // isFirstLaunch = true
            }
            is LoginState.Success -> {
                showSuccessDialog = true
                delay(1000)
                showSuccessDialog = false
                onLoginSuccess(emailInput)
            }
            is LoginState.Error -> {
                errorMessage = (loginState as LoginState.Error).message
                showErrorDialog = true
            }
            is LoginState.OTPRequired -> {
                showOtpDialog = true
                remainingTime = 120000L
            }
            else -> {}
        }
    }

    // Replace the timer countdown effect
    LaunchedEffect(showOtpDialog) {
        while (showOtpDialog && remainingTime > 0) {
            delay(1000) // Add 1 second delay
            remainingTime -= 1000
        }
    }

    // Add error dialog for biometric and other errors
    if (showErrorDialog && loginState is LoginState.Error) {
        ErrorDialog(
            message = (loginState as LoginState.Error).message,
            onDismiss = { showErrorDialog = false }
        )
    }

    // Add success dialog
    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = { 
                showSuccessDialog = false
                onLoginSuccess(emailInput)
            },
            icon = {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Success",
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            title = { Text("Success") },
            text = { Text("Login successful!") },
            confirmButton = {
                TextButton(
                    onClick = { 
                        showSuccessDialog = false
                        onLoginSuccess(emailInput)
                    }
                ) {
                    Text("OK")
                }
            }
        )
    }
}

@Composable
private fun ErrorDialog(
    message: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Error,
                contentDescription = "Error",
                tint = MaterialTheme.colorScheme.error
            )
        },
        title = { Text("Login Error") },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("OK")
            }
        }
    )
}

@Composable
private fun PasswordDialog(
    onDismiss: () -> Unit,
    onSubmit: () -> Unit,
    viewModel: LoginViewModel,
    passwordVisible: Boolean,
    onPasswordVisibilityChange: (Boolean) -> Unit,
    errorMessage: String? = null
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Enter Password") },
        text = {
            Column {
                OutlinedTextField(
                    value = viewModel.password,
                    onValueChange = { viewModel.updatePassword(it) },
                    label = { Text("Password") },
                    isError = errorMessage != null,
                    visualTransformation = if (passwordVisible) 
                        VisualTransformation.None 
                    else 
                        PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { onPasswordVisibilityChange(!passwordVisible) }) {
                            Icon(
                                imageVector = if (passwordVisible) 
                                    Icons.Default.VisibilityOff 
                                else 
                                    Icons.Default.Visibility,
                                contentDescription = if (passwordVisible) 
                                    "Hide password" 
                                else 
                                    "Show password"
                            )
                        }
                    }
                )
                if (errorMessage != null) {
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onSubmit,
                enabled = viewModel.password.isNotBlank()
            ) {
                Text("Login")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private fun isValidEmail(email: String): Boolean {
    val emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}\$"
    return email.matches(emailRegex.toRegex())
} 
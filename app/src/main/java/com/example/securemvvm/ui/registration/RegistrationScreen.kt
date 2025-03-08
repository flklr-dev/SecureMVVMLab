package com.example.securemvvm.ui.registration

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.securemvvm.viewmodel.RegistrationViewModel
import com.example.securemvvm.viewmodel.RegistrationState
import com.example.securemvvm.viewmodel.utils.ValidationUtils
import androidx.compose.ui.focus.onFocusChanged

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegistrationScreen(
    viewModel: RegistrationViewModel = hiltViewModel(),
    onNavigateToLogin: () -> Unit
) {
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    var showErrorDialog by remember { mutableStateOf(false) }
    var showSuccessDialog by remember { mutableStateOf(false) }
    var showPasswordRequirements by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    
    // Add error states and field interaction tracking
    var emailError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var confirmPasswordError by remember { mutableStateOf<String?>(null) }
    
    // Track if fields have been touched/interacted with
    var isEmailTouched by remember { mutableStateOf(false) }
    var isPasswordTouched by remember { mutableStateOf(false) }
    var isConfirmPasswordTouched by remember { mutableStateOf(false) }
    
    val registrationState by viewModel.registrationState.collectAsState()
    
    LaunchedEffect(registrationState) {
        when (registrationState) {
            is RegistrationState.Success -> {
                showSuccessDialog = true
            }
            is RegistrationState.Error -> {
                errorMessage = (registrationState as RegistrationState.Error).message
                showErrorDialog = true
            }
            else -> {}
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Create Account",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        OutlinedTextField(
            value = viewModel.email,
            onValueChange = { 
                viewModel.updateEmail(it)
                if (isEmailTouched) {
                    emailError = null // Clear error only if field has been touched
                }
            },
            label = { Text("Email") },
            isError = emailError != null,
            supportingText = if (emailError != null) {
                { Text(text = emailError!!, color = MaterialTheme.colorScheme.error) }
            } else null,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
                .onFocusChanged { focusState ->
                    if (focusState.isFocused) {
                        isEmailTouched = true
                    } else if (isEmailTouched) {  // Only validate if field has been touched
                        when {
                            viewModel.email.isBlank() -> {
                                emailError = "Email is required"
                            }
                            !ValidationUtils.validateEmail(viewModel.email) -> {
                                emailError = "Invalid email format"
                            }
                        }
                    }
                },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next
            )
        )

        OutlinedTextField(
            value = viewModel.password,
            onValueChange = { 
                viewModel.updatePassword(it)
                if (isPasswordTouched) {
                    passwordError = null
                }
            },
            label = { Text("Password") },
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Next
            ),
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = if (passwordVisible) "Hide password" else "Show password"
                    )
                }
            },
            isError = passwordError != null,
            supportingText = if (passwordError != null) {
                { Text(text = passwordError!!, color = MaterialTheme.colorScheme.error) }
            } else null,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = if (isPasswordTouched && !viewModel.isPasswordValid) 4.dp else 16.dp)
                .onFocusChanged { focusState ->
                    if (focusState.isFocused) {
                        isPasswordTouched = true
                    } else if (isPasswordTouched) {
                        when {
                            viewModel.password.isBlank() -> {
                                passwordError = "Password is required"
                            }
                            !viewModel.isPasswordValid -> {
                                passwordError = "Password does not meet requirements"
                            }
                        }
                    }
                }
        )

        // Show password requirements when password field is touched and requirements aren't met
        if (isPasswordTouched && !viewModel.isPasswordValid) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, bottom = 16.dp)
            ) {
                PasswordRequirement(
                    text = "At least 8 characters",
                    isMet = viewModel.hasMinLength
                )
                PasswordRequirement(
                    text = "Contains uppercase letter",
                    isMet = viewModel.hasUppercase
                )
                PasswordRequirement(
                    text = "Contains lowercase letter",
                    isMet = viewModel.hasLowercase
                )
                PasswordRequirement(
                    text = "Contains number",
                    isMet = viewModel.hasNumber
                )
                PasswordRequirement(
                    text = "Contains special character",
                    isMet = viewModel.hasSpecialChar
                )
            }
        }

        OutlinedTextField(
            value = viewModel.confirmPassword,
            onValueChange = { 
                viewModel.updateConfirmPassword(it)
                if (isConfirmPasswordTouched) {
                    confirmPasswordError = null // Clear error only if field has been touched
                }
            },
            label = { Text("Confirm Password") },
            visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
            ),
            trailingIcon = {
                IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                    Icon(
                        imageVector = if (confirmPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = "Toggle password visibility"
                    )
                }
            },
            isError = confirmPasswordError != null,
            supportingText = if (confirmPasswordError != null) {
                { Text(text = confirmPasswordError!!, color = MaterialTheme.colorScheme.error) }
            } else null,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
                .onFocusChanged { focusState ->
                    if (focusState.isFocused) {
                        isConfirmPasswordTouched = true
                    } else if (isConfirmPasswordTouched) {  // Only validate if field has been touched
                        when {
                            viewModel.confirmPassword.isBlank() -> {
                                confirmPasswordError = "Confirm password is required"
                            }
                            viewModel.password != viewModel.confirmPassword -> {
                                confirmPasswordError = "Passwords do not match"
                            }
                        }
                    }
                }
        )

        Button(
            onClick = { viewModel.register() },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            enabled = viewModel.isFormValid && registrationState !is RegistrationState.Loading
        ) {
            if (registrationState is RegistrationState.Loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text("Register")
            }
        }

        TextButton(
            onClick = onNavigateToLogin,
            modifier = Modifier.padding(top = 16.dp)
        ) {
            Text("Already have an account? Login")
        }
    }

    // Password Requirements Dialog
    if (showPasswordRequirements) {
        AlertDialog(
            onDismissRequest = { showPasswordRequirements = false },
            title = { Text("Password Requirements") },
            text = {
                Column {
                    PasswordRequirement(
                        text = "At least 8 characters",
                        isMet = viewModel.password.length >= 8
                    )
                    PasswordRequirement(
                        text = "Contains uppercase letter",
                        isMet = viewModel.password.any { it.isUpperCase() }
                    )
                    PasswordRequirement(
                        text = "Contains lowercase letter",
                        isMet = viewModel.password.any { it.isLowerCase() }
                    )
                    PasswordRequirement(
                        text = "Contains number",
                        isMet = viewModel.password.any { it.isDigit() }
                    )
                    PasswordRequirement(
                        text = "Contains special character",
                        isMet = viewModel.password.any { !it.isLetterOrDigit() }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showPasswordRequirements = false }) {
                    Text("OK")
                }
            }
        )
    }

    // Error Dialog
    if (showErrorDialog) {
        AlertDialog(
            onDismissRequest = { showErrorDialog = false },
            title = { Text("Registration Error") },
            text = { Text(errorMessage) },
            confirmButton = {
                TextButton(onClick = { showErrorDialog = false }) {
                    Text("OK")
                }
            }
        )
    }

    // Success Dialog
    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = { 
                showSuccessDialog = false
                onNavigateToLogin()
            },
            title = { Text("Registration Successful") },
            text = { Text("Your account has been created successfully. Please login to continue.") },
            confirmButton = {
                TextButton(
                    onClick = { 
                        showSuccessDialog = false
                        onNavigateToLogin()
                    }
                ) {
                    Text("OK")
                }
            }
        )
    }
}

@Composable
private fun PasswordRequirement(
    text: String,
    isMet: Boolean
) {
    Row(
        modifier = Modifier.padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "•",
            color = if (isMet) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            color = if (isMet) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall
        )
    }
} 
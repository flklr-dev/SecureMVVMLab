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
            onValueChange = { viewModel.updateEmail(it) },
            label = { Text("Email") },
            isError = viewModel.email.isNotEmpty() && !ValidationUtils.validateEmail(viewModel.email),
            supportingText = {
                if (viewModel.email.isNotEmpty() && !ValidationUtils.validateEmail(viewModel.email)) {
                    Text(
                        text = "Invalid email format",
                        color = MaterialTheme.colorScheme.error
                    )
                } else if (registrationState is RegistrationState.Error && (registrationState as RegistrationState.Error).message.contains("Email already registered")) {
                    Text(
                        text = "Email already registered",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next
            )
        )

        OutlinedTextField(
            value = viewModel.password,
            onValueChange = { viewModel.updatePassword(it) },
            label = { Text("Password") },
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Next
            ),
            trailingIcon = {
                Row {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (passwordVisible) "Hide password" else "Show password"
                        )
                    }
                    IconButton(onClick = { showPasswordRequirements = true }) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Password requirements"
                        )
                    }
                }
            },
            isError = viewModel.password.isNotEmpty() && !viewModel.isPasswordValid,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        )

        OutlinedTextField(
            value = viewModel.confirmPassword,
            onValueChange = { viewModel.updateConfirmPassword(it) },
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
            isError = viewModel.confirmPassword.isNotEmpty() && viewModel.password != viewModel.confirmPassword,
            supportingText = {
                if (viewModel.confirmPassword.isNotEmpty() && viewModel.password != viewModel.confirmPassword) {
                    Text(
                        text = "Passwords do not match",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
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
        modifier = Modifier.padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Info,
            contentDescription = null,
            tint = if (isMet) Color.Green else Color.Red,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            color = if (isMet) Color.Green else Color.Red
        )
    }
} 
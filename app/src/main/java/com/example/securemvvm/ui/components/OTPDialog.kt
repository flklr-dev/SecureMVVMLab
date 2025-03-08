package com.example.securemvvm.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OTPDialog(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    onVerify: (String) -> Unit,
    onResendOTP: () -> Unit,
    remainingTime: Long,
    attempts: Int
) {
    if (isVisible) {
        var otpValue by remember { mutableStateOf("") }
        var errorMessage by remember { mutableStateOf<String?>(null) }
        val canResend = remainingTime <= 0
        
        AlertDialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(dismissOnClickOutside = false),
            title = { 
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Email,
                        contentDescription = "Email verification",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .size(48.dp)
                            .padding(bottom = 8.dp)
                    )
                    Text(
                        "Email Verification",
                        style = MaterialTheme.typography.headlineSmall,
                        textAlign = TextAlign.Center
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "We've sent a verification code to your email",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                    
                    OutlinedTextField(
                        value = otpValue,
                        onValueChange = { 
                            if (it.length <= 6) {
                                otpValue = it
                                errorMessage = null
                            }
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Done
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Enter 6-digit code") },
                        isError = errorMessage != null
                    )
                    
                    if (errorMessage != null) {
                        Text(
                            text = errorMessage!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (!canResend) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Code expires in: ${remainingTime / 1000} seconds",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            TextButton(
                                onClick = {
                                    onResendOTP()
                                    errorMessage = "New code sent to your email"
                                }
                            ) {
                                Text("Resend Code")
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { 
                        if (otpValue.length == 6) {
                            onVerify(otpValue)
                        } else {
                            errorMessage = "Please enter a valid 6-digit code"
                        }
                    },
                    enabled = otpValue.isNotEmpty()
                ) {
                    Text("Verify")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        )
    }
} 
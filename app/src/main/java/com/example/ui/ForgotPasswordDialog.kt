package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AlertRed
import com.example.ui.theme.NavyDark
import com.example.ui.theme.SafeGreen
import com.example.ui.theme.YellowAccent

@Composable
fun ForgotPasswordDialog(
    authViewModel: AuthViewModel,
    modifier: Modifier = Modifier
) {
    val state by authViewModel.uiState.collectAsState()

    if (!state.showForgotDialog) return

    AlertDialog(
        onDismissRequest = { authViewModel.closeForgotPasswordDialog() },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(YellowAccent.copy(alpha = 0.2f))
                ) {
                    Icon(
                        imageVector = when (state.forgotStep) {
                            1 -> Icons.Default.LockReset
                            2 -> Icons.Default.Key
                            3 -> Icons.Default.Lock
                            else -> Icons.Default.CheckCircle
                        },
                        contentDescription = null,
                        tint = if (state.forgotStep == 4) SafeGreen else YellowAccent,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = when (state.forgotStep) {
                        1 -> "Reset Password"
                        2 -> "Verify Reset Code"
                        3 -> "New Password"
                        else -> "Password Reset"
                    },
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color.White
                )
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                when (state.forgotStep) {
                    1 -> {
                        // STEP 1: Enter email or phone
                        Text(
                            text = "Enter your registered email address or mobile number. We will send a 6-digit OTP code to verify your identity.",
                            fontSize = 13.sp,
                            color = Color.White.copy(alpha = 0.8f),
                            lineHeight = 18.sp
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedTextField(
                            value = state.forgotIdentifier,
                            onValueChange = { authViewModel.onForgotIdentifierChanged(it) },
                            placeholder = { Text("Email or +92 Mobile", color = Color.Gray, fontSize = 13.sp) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Email,
                                    contentDescription = null,
                                    tint = YellowAccent
                                )
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Email,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = { authViewModel.sendForgotOtp() }
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = YellowAccent,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                cursorColor = YellowAccent
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_forgot_identifier")
                        )
                    }

                    2 -> {
                        // STEP 2: Enter 6-digit OTP
                        Text(
                            text = "A 6-digit code was sent to:\n${state.forgotTargetLabel}",
                            fontSize = 13.sp,
                            color = Color.White.copy(alpha = 0.85f),
                            lineHeight = 18.sp
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Test Mode OTP notification banner
                        if (state.forgotGeneratedOtp != null) {
                            Surface(
                                color = YellowAccent.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(8.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, YellowAccent.copy(alpha = 0.4f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "🧪 Test OTP:",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = YellowAccent
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = state.forgotGeneratedOtp!!,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Black,
                                        fontFamily = FontFamily.Monospace,
                                        color = Color.White,
                                        letterSpacing = 2.sp
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = state.forgotOtpEntered,
                            onValueChange = { authViewModel.onForgotOtpEnteredChanged(it) },
                            placeholder = { Text("Enter 6-digit code", color = Color.Gray, fontSize = 14.sp) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Security,
                                    contentDescription = null,
                                    tint = YellowAccent
                                )
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = { authViewModel.verifyForgotOtp() }
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = YellowAccent,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                cursorColor = YellowAccent
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_forgot_otp")
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Didn't receive code?",
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.6f)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            TextButton(onClick = { authViewModel.resendForgotOtp() }) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = null,
                                    tint = YellowAccent,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Resend", fontSize = 12.sp, color = YellowAccent, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    3 -> {
                        // STEP 3: Enter new password
                        Text(
                            text = "Set a new password for your Thief Hunter account (minimum 6 characters).",
                            fontSize = 13.sp,
                            color = Color.White.copy(alpha = 0.8f),
                            lineHeight = 18.sp
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        OutlinedTextField(
                            value = state.forgotNewPassword,
                            onValueChange = { authViewModel.onForgotNewPasswordChanged(it) },
                            placeholder = { Text("New password (min 6 chars)", color = Color.Gray, fontSize = 13.sp) },
                            leadingIcon = {
                                Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = YellowAccent)
                            },
                            trailingIcon = {
                                IconButton(onClick = { authViewModel.toggleForgotNewPasswordVisibility() }) {
                                    Icon(
                                        imageVector = if (state.isForgotNewPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = "Toggle password",
                                        tint = Color.White.copy(alpha = 0.6f)
                                    )
                                }
                            },
                            visualTransformation = if (state.isForgotNewPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Next
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = YellowAccent,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                cursorColor = YellowAccent
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_forgot_new_password")
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = state.forgotConfirmPassword,
                            onValueChange = { authViewModel.onForgotConfirmPasswordChanged(it) },
                            placeholder = { Text("Confirm new password", color = Color.Gray, fontSize = 13.sp) },
                            leadingIcon = {
                                Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = YellowAccent)
                            },
                            trailingIcon = {
                                IconButton(onClick = { authViewModel.toggleForgotConfirmPasswordVisibility() }) {
                                    Icon(
                                        imageVector = if (state.isForgotConfirmPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = "Toggle password",
                                        tint = Color.White.copy(alpha = 0.6f)
                                    )
                                }
                            },
                            visualTransformation = if (state.isForgotConfirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = { authViewModel.resetPassword() }
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = YellowAccent,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                cursorColor = YellowAccent
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_forgot_confirm_password")
                        )
                    }

                    4 -> {
                        // STEP 4: Success confirmation
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(CircleShape)
                                    .background(SafeGreen.copy(alpha = 0.2f))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = SafeGreen,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Password Changed Successfully!",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "You can now log in with your updated credentials.",
                                fontSize = 13.sp,
                                color = Color.White.copy(alpha = 0.7f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                // Error text banner
                if (state.forgotError != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = state.forgotError!!,
                        color = AlertRed,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        },
        confirmButton = {
            when (state.forgotStep) {
                1 -> {
                    Button(
                        onClick = { authViewModel.sendForgotOtp() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = YellowAccent,
                            contentColor = NavyDark
                        ),
                        shape = RoundedCornerShape(8.dp),
                        enabled = !state.isForgotLoading,
                        modifier = Modifier.testTag("btn_send_otp")
                    ) {
                        if (state.isForgotLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = NavyDark,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Send OTP", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
                2 -> {
                    Button(
                        onClick = { authViewModel.verifyForgotOtp() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = YellowAccent,
                            contentColor = NavyDark
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("btn_verify_otp")
                    ) {
                        Text("Verify OTP", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
                3 -> {
                    Button(
                        onClick = { authViewModel.resetPassword() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = YellowAccent,
                            contentColor = NavyDark
                        ),
                        shape = RoundedCornerShape(8.dp),
                        enabled = !state.isForgotLoading,
                        modifier = Modifier.testTag("btn_reset_password")
                    ) {
                        if (state.isForgotLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = NavyDark,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Reset Password", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
                4 -> {
                    Button(
                        onClick = { authViewModel.closeForgotPasswordDialog() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SafeGreen,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("btn_login_now")
                    ) {
                        Text("Log In Now", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        },
        dismissButton = {
            if (state.forgotStep != 4) {
                TextButton(
                    onClick = { authViewModel.closeForgotPasswordDialog() }
                ) {
                    Text("Cancel", color = Color.White.copy(alpha = 0.7f))
                }
            }
        },
        containerColor = NavyDark,
        shape = RoundedCornerShape(20.dp),
        modifier = modifier
    )
}

package com.example.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.Screen
import com.example.ui.theme.AlertRed
import com.example.ui.theme.NavyDark
import com.example.ui.theme.NavyPrimary
import com.example.ui.theme.SafeGreen
import com.example.ui.theme.YellowAccent

/**
 * Facebook-style 3-Step Signup Screen:
 * STEP 1: Registration Form -> [SEND OTP]
 * STEP 2: 6-Digit OTP Verification -> [VERIFY OTP]
 * STEP 3: Account Created Success -> [GO TO HOME]
 */
@Composable
fun SignupScreen(
    viewModel: ThiefHunterViewModel,
    authViewModel: AuthViewModel,
    modifier: Modifier = Modifier
) {
    val state by authViewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

    fun completeAndNavigateHome() {
        val user = state.createdUser
        val email = user?.email?.takeIf { it.isNotBlank() } ?: state.signupEmail.trim()
        val name = user?.fullName?.takeIf { it.isNotBlank() } ?: state.signupFullName.trim()
        viewModel.loginWithGoogle(email = email, displayName = name)
        viewModel.navigateTo(Screen.HOME)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        NavyDark,
                        NavyPrimary,
                        Color(0xFF080C24)
                    )
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Top back button if on Step 1
            if (state.signupStep == 1) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start
                ) {
                    IconButton(
                        onClick = { viewModel.navigateTo(Screen.LOGIN) },
                        modifier = Modifier.testTag("btn_back_to_login")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back to Login",
                            tint = YellowAccent
                        )
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(16.dp))
            }

            // App Brand / Logo
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.08f))
                    .border(2.dp, YellowAccent.copy(alpha = 0.4f), CircleShape)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_thief_hunter_logo),
                    contentDescription = "Thief Hunter Logo",
                    modifier = Modifier.size(46.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Step Indicator Badge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                StepBadge(stepNumber = 1, currentStep = state.signupStep, label = "Form")
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .width(20.dp)
                        .height(2.dp)
                        .background(if (state.signupStep >= 2) YellowAccent else Color.White.copy(alpha = 0.2f))
                )
                Spacer(modifier = Modifier.width(8.dp))
                StepBadge(stepNumber = 2, currentStep = state.signupStep, label = "OTP")
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .width(20.dp)
                        .height(2.dp)
                        .background(if (state.signupStep >= 3) SafeGreen else Color.White.copy(alpha = 0.2f))
                )
                Spacer(modifier = Modifier.width(8.dp))
                StepBadge(stepNumber = 3, currentStep = state.signupStep, label = "Done")
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Main Content depending on step
            AnimatedContent(
                targetState = state.signupStep,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "SignupStepTransition"
            ) { step ->
                when (step) {
                    1 -> SignupStep1Form(authViewModel = authViewModel, viewModel = viewModel, state = state)
                    2 -> SignupStep2Otp(authViewModel = authViewModel, state = state)
                    3 -> SignupStep3Success(onGoHome = { completeAndNavigateHome() })
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }

        // Dialog for Rule 3: Where to send OTP? (Email or SMS)
        if (state.showChannelChoiceDialog) {
            AlertDialog(
                onDismissRequest = { authViewModel.dismissChannelChoiceDialog() },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = null,
                            tint = YellowAccent,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Where to send OTP?",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                },
                text = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "You entered both an email and a mobile number. Choose where you would like to receive your 6-digit verification code:",
                            fontSize = 13.sp,
                            color = Color.White.copy(alpha = 0.8f),
                            lineHeight = 18.sp
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        // Email Option
                        Surface(
                            color = Color.White.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, YellowAccent.copy(alpha = 0.5f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { authViewModel.chooseOtpChannel(OtpChannel.EMAIL) }
                                .testTag("btn_choose_email_otp")
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Email,
                                    contentDescription = null,
                                    tint = YellowAccent,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Send to Email",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = Color.White
                                    )
                                    Text(
                                        text = state.signupEmail,
                                        fontSize = 12.sp,
                                        color = YellowAccent
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // SMS Option
                        Surface(
                            color = Color.White.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, YellowAccent.copy(alpha = 0.5f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { authViewModel.chooseOtpChannel(OtpChannel.SMS) }
                                .testTag("btn_choose_sms_otp")
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Sms,
                                    contentDescription = null,
                                    tint = YellowAccent,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Send via SMS",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = Color.White
                                    )
                                    Text(
                                        text = state.signupMobile,
                                        fontSize = 12.sp,
                                        color = YellowAccent
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { authViewModel.dismissChannelChoiceDialog() }) {
                        Text("Cancel", color = Color.White.copy(alpha = 0.7f))
                    }
                },
                containerColor = NavyDark,
                shape = RoundedCornerShape(18.dp)
            )
        }
    }
}

@Composable
private fun StepBadge(stepNumber: Int, currentStep: Int, label: String) {
    val isCompleted = currentStep > stepNumber
    val isCurrent = currentStep == stepNumber
    val bgColor = when {
        isCompleted -> SafeGreen
        isCurrent -> YellowAccent
        else -> Color.White.copy(alpha = 0.15f)
    }
    val textColor = when {
        isCompleted -> Color.White
        isCurrent -> NavyDark
        else -> Color.White.copy(alpha = 0.5f)
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(bgColor)
        ) {
            Text(
                text = if (isCompleted) "✓" else "$stepNumber",
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                color = textColor
            )
        }
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
            color = if (isCurrent) YellowAccent else Color.White.copy(alpha = 0.6f)
        )
    }
}

/**
 * STEP 1: Registration Form
 */
@Composable
private fun SignupStep1Form(
    authViewModel: AuthViewModel,
    viewModel: ThiefHunterViewModel,
    state: AuthUiState
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.08f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "🎯", fontSize = 20.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Create Account",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
            }

            Text(
                text = "Enter your details to generate your verification code",
                fontSize = 12.sp,
                color = YellowAccent,
                modifier = Modifier.padding(top = 2.dp)
            )

            Spacer(modifier = Modifier.height(18.dp))

            // 1. Full Name
            Text(
                text = "Full Name",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White.copy(alpha = 0.9f)
            )
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedTextField(
                value = state.signupFullName,
                onValueChange = { authViewModel.onSignupFullNameChanged(it) },
                placeholder = { Text("e.g. Sajid Malik", color = Color.Gray, fontSize = 13.sp) },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Badge, contentDescription = null, tint = YellowAccent)
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
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
                    .testTag("input_signup_name")
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 2. Mobile Number
            Text(
                text = "Mobile Number",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White.copy(alpha = 0.9f)
            )
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedTextField(
                value = state.signupMobile,
                onValueChange = { authViewModel.onSignupMobileChanged(it) },
                placeholder = { Text("+92 300 1234567", color = Color.Gray, fontSize = 13.sp) },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Phone, contentDescription = null, tint = YellowAccent)
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Phone,
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
                    .testTag("input_signup_mobile")
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 3. Email
            Text(
                text = "Email Address",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White.copy(alpha = 0.9f)
            )
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedTextField(
                value = state.signupEmail,
                onValueChange = { authViewModel.onSignupEmailChanged(it) },
                placeholder = { Text("email@example.com", color = Color.Gray, fontSize = 13.sp) },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Email, contentDescription = null, tint = YellowAccent)
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
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
                    .testTag("input_signup_email")
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 4. Password
            Text(
                text = "Password (min 6 chars)",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White.copy(alpha = 0.9f)
            )
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedTextField(
                value = state.signupPassword,
                onValueChange = { authViewModel.onSignupPasswordChanged(it) },
                placeholder = { Text("••••••••", color = Color.Gray, fontSize = 13.sp) },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = YellowAccent)
                },
                trailingIcon = {
                    IconButton(onClick = { authViewModel.toggleSignupPasswordVisibility() }) {
                        Icon(
                            imageVector = if (state.isSignupPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = "Toggle password",
                            tint = Color.White.copy(alpha = 0.6f)
                        )
                    }
                },
                visualTransformation = if (state.isSignupPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
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
                    .testTag("input_signup_password")
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 5. Confirm Password
            Text(
                text = "Confirm Password",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White.copy(alpha = 0.9f)
            )
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedTextField(
                value = state.signupConfirmPassword,
                onValueChange = { authViewModel.onSignupConfirmPasswordChanged(it) },
                placeholder = { Text("••••••••", color = Color.Gray, fontSize = 13.sp) },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = YellowAccent)
                },
                trailingIcon = {
                    IconButton(onClick = { authViewModel.toggleSignupConfirmPasswordVisibility() }) {
                        Icon(
                            imageVector = if (state.isSignupConfirmPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = "Toggle confirm password",
                            tint = Color.White.copy(alpha = 0.6f)
                        )
                    }
                },
                visualTransformation = if (state.isSignupConfirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = { authViewModel.startSignupOtpFlow() }
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
                    .testTag("input_signup_confirm_password")
            )

            // Error text
            if (state.signupError != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = state.signupError!!,
                    color = AlertRed,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // SEND OTP BUTTON
            Button(
                onClick = { authViewModel.startSignupOtpFlow() },
                enabled = !state.isSignupLoading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = YellowAccent,
                    contentColor = NavyDark
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("btn_send_otp")
            ) {
                if (state.isSignupLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = NavyDark,
                        strokeWidth = 2.5.dp
                    )
                } else {
                    Icon(imageVector = Icons.Default.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "SEND OTP",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Already have an account? Log in
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                TextButton(
                    onClick = { viewModel.navigateTo(Screen.LOGIN) },
                    modifier = Modifier.testTag("btn_already_have_account")
                ) {
                    Text(
                        text = "Already have an account? Log in",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

/**
 * STEP 2: OTP Verification
 */
@Composable
private fun SignupStep2Otp(
    authViewModel: AuthViewModel,
    state: AuthUiState
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.08f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(YellowAccent.copy(alpha = 0.15f))
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = YellowAccent,
                    modifier = Modifier.size(30.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "📱 Verify OTP",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "OTP sent to:",
                fontSize = 13.sp,
                color = Color.White.copy(alpha = 0.7f)
            )

            Text(
                text = if (state.signupOtpChannel == OtpChannel.EMAIL) "📧 ${state.signupOtpTarget}" else "📱 ${state.signupOtpTarget}",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = YellowAccent
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Test Mode Helper Box
            if (state.signupGeneratedOtp != null) {
                Surface(
                    color = YellowAccent.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, YellowAccent.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "🧪 Test OTP:",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = YellowAccent
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = state.signupGeneratedOtp!!,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            color = Color.White,
                            letterSpacing = 3.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 6-digit OTP Box Visual Display
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                for (i in 0 until 6) {
                    val char = state.signupOtpEntered.getOrNull(i)?.toString() ?: ""
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.White.copy(alpha = 0.1f))
                            .border(
                                width = 1.5.dp,
                                color = if (char.isNotEmpty()) YellowAccent else Color.White.copy(alpha = 0.25f),
                                shape = RoundedCornerShape(10.dp)
                            )
                    ) {
                        Text(
                            text = char,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Hidden or Formatted Actual Input Field
            OutlinedTextField(
                value = state.signupOtpEntered,
                onValueChange = { authViewModel.onSignupOtpEnteredChanged(it) },
                placeholder = { Text("Enter 6-digit OTP", color = Color.Gray, fontSize = 14.sp) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = { authViewModel.verifySignupOtp() }
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
                    .testTag("input_signup_otp")
            )

            // Error
            if (state.signupError != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = state.signupError!!,
                    color = AlertRed,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // VERIFY OTP BUTTON
            Button(
                onClick = { authViewModel.verifySignupOtp() },
                enabled = !state.isSignupLoading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = SafeGreen,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("btn_verify_otp")
            ) {
                if (state.isSignupLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.5.dp
                    )
                } else {
                    Icon(imageVector = Icons.Default.VerifiedUser, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "VERIFY OTP",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Resend row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Didn't receive code?",
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.width(4.dp))
                TextButton(
                    onClick = { authViewModel.resendSignupOtp() },
                    modifier = Modifier.testTag("btn_resend_otp")
                ) {
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = null, tint = YellowAccent, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Resend",
                        color = YellowAccent,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

/**
 * STEP 3: Success Screen
 */
@Composable
private fun SignupStep3Success(
    onGoHome: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.08f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, SafeGreen.copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(26.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(76.dp)
                    .clip(CircleShape)
                    .background(SafeGreen.copy(alpha = 0.2f))
                    .border(2.dp, SafeGreen, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Success",
                    tint = SafeGreen,
                    modifier = Modifier.size(46.dp)
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "✅ Account Created!",
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                color = Color.White,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Your identity is verified and your Thief Hunter perimeter shield is now active. Anti-theft sensors and intruder camera are ready.",
                fontSize = 13.sp,
                color = Color.White.copy(alpha = 0.8f),
                textAlign = TextAlign.Center,
                lineHeight = 19.sp
            )

            Spacer(modifier = Modifier.height(26.dp))

            // GO TO HOME BUTTON
            Button(
                onClick = onGoHome,
                colors = ButtonDefaults.buttonColors(
                    containerColor = SafeGreen,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("btn_go_to_home")
            ) {
                Text(
                    text = "GO TO HOME",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}

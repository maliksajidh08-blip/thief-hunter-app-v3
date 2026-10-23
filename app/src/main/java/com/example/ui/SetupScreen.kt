package com.example.ui

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.example.security.BiometricHelper
import com.example.ui.theme.AlertRed
import com.example.ui.theme.NavyCardDark
import com.example.ui.theme.NavyDark
import com.example.ui.theme.NavyPrimary
import com.example.ui.theme.SafeGreen
import com.example.ui.theme.YellowAccent

/**
 * First Launch Registration & Setup Screen.
 * Collects Owner Name, Phone (+92), Email, and 4-digit Master PIN.
 * Prompts for Fingerprint (Biometric) enrollment before proceeding to Home.
 */
@Composable
fun SetupScreen(
    viewModel: ThiefHunterViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val scrollState = rememberScrollState()

    var ownerName by remember { mutableStateOf("Sajid") }
    var ownerPhone by remember { mutableStateOf("+92 300 4589211") }
    var ownerEmail by remember { mutableStateOf("sajidhr905@gmail.com") }
    var masterPin by remember { mutableStateOf("9051") }

    var nameError by remember { mutableStateOf<String?>(null) }
    var phoneError by remember { mutableStateOf<String?>(null) }
    var emailError by remember { mutableStateOf<String?>(null) }
    var pinError by remember { mutableStateOf<String?>(null) }

    var showFingerprintPrompt by remember { mutableStateOf(false) }

    fun validateInputs(): Boolean {
        var valid = true
        if (ownerName.trim().length < 2) {
            nameError = "Please enter your name (at least 2 letters)"
            valid = false
        } else {
            nameError = null
        }

        if (ownerPhone.trim().length < 7) {
            phoneError = "Please enter a valid phone number"
            valid = false
        } else {
            phoneError = null
        }

        if (!ownerEmail.contains("@") || !ownerEmail.contains(".")) {
            emailError = "Please enter a valid Gmail / Email address"
            valid = false
        } else {
            emailError = null
        }

        if (masterPin.trim().length != 4 || !masterPin.all { it.isDigit() }) {
            pinError = "PIN must be exactly 4 digits (e.g., 9051)"
            valid = false
        } else {
            pinError = null
        }
        return valid
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(NavyDark, NavyPrimary, Color(0xFF090D24))
                )
            )
            .imePadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Brand Icon & Header
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(68.dp)
                    .clip(CircleShape)
                    .background(NavyCardDark)
                    .border(2.dp, YellowAccent, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = null,
                    tint = YellowAccent,
                    modifier = Modifier.size(36.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Device Registration",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Text(
                text = "Set up your owner profile and master security PIN to protect your phone.",
                fontSize = 13.sp,
                color = Color.White.copy(alpha = 0.8f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Form Card
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = NavyCardDark),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // 1. Owner Name
                    Column {
                        Text(
                            text = "Owner Name",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = YellowAccent
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = ownerName,
                            onValueChange = {
                                ownerName = it
                                if (nameError != null) nameError = null
                            },
                            placeholder = { Text("e.g. Sajid", color = Color.Gray) },
                            leadingIcon = {
                                Icon(Icons.Default.Person, contentDescription = null, tint = YellowAccent)
                            },
                            singleLine = true,
                            isError = nameError != null,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = YellowAccent,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_setup_name")
                        )
                        if (nameError != null) {
                            Text(text = nameError!!, color = AlertRed, fontSize = 11.sp, modifier = Modifier.padding(top = 2.dp))
                        }
                    }

                    // 2. Phone Number (+92)
                    Column {
                        Text(
                            text = "Owner Phone Number",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = YellowAccent
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = ownerPhone,
                            onValueChange = {
                                ownerPhone = it
                                if (phoneError != null) phoneError = null
                            },
                            placeholder = { Text("+92 300 1234567", color = Color.Gray) },
                            leadingIcon = {
                                Icon(Icons.Default.Phone, contentDescription = null, tint = YellowAccent)
                            },
                            singleLine = true,
                            isError = phoneError != null,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = YellowAccent,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next),
                            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_setup_phone")
                        )
                        if (phoneError != null) {
                            Text(text = phoneError!!, color = AlertRed, fontSize = 11.sp, modifier = Modifier.padding(top = 2.dp))
                        }
                    }

                    // 3. Gmail / Email
                    Column {
                        Text(
                            text = "Gmail / Email Address",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = YellowAccent
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = ownerEmail,
                            onValueChange = {
                                ownerEmail = it
                                if (emailError != null) emailError = null
                            },
                            placeholder = { Text("you@gmail.com", color = Color.Gray) },
                            leadingIcon = {
                                Icon(Icons.Default.Email, contentDescription = null, tint = YellowAccent)
                            },
                            singleLine = true,
                            isError = emailError != null,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = YellowAccent,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_setup_email")
                        )
                        if (emailError != null) {
                            Text(text = emailError!!, color = AlertRed, fontSize = 11.sp, modifier = Modifier.padding(top = 2.dp))
                        }
                    }

                    // 4. Master PIN (4 Digits)
                    Column {
                        Text(
                            text = "Master PIN (4 Digits to Disarm)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = YellowAccent
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = masterPin,
                            onValueChange = {
                                if (it.length <= 4 && it.all { ch -> ch.isDigit() }) {
                                    masterPin = it
                                    if (pinError != null) pinError = null
                                }
                            },
                            placeholder = { Text("4 Digits (e.g. 9051)", color = Color.Gray) },
                            leadingIcon = {
                                Icon(Icons.Default.Lock, contentDescription = null, tint = YellowAccent)
                            },
                            visualTransformation = PasswordVisualTransformation(),
                            singleLine = true,
                            isError = pinError != null,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = YellowAccent,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = {
                                focusManager.clearFocus()
                                if (validateInputs()) {
                                    showFingerprintPrompt = true
                                }
                            }),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_setup_pin")
                        )
                        if (pinError != null) {
                            Text(text = pinError!!, color = AlertRed, fontSize = 11.sp, modifier = Modifier.padding(top = 2.dp))
                        } else {
                            Text(
                                text = "This PIN stops sirens and disarms all sensors.",
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 11.sp,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Continue Button
                    Button(
                        onClick = {
                            focusManager.clearFocus()
                            if (validateInputs()) {
                                showFingerprintPrompt = true
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = YellowAccent,
                            contentColor = NavyDark
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("btn_setup_continue")
                    ) {
                        Text(
                            text = "Continue",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(30.dp))
        }

        // FINGERPRINT / BIOMETRIC SETUP PROMPT DIALOG
        if (showFingerprintPrompt) {
            AlertDialog(
                onDismissRequest = {
                    // Default to enabling and save
                    viewModel.saveInitialSetup(
                        name = ownerName,
                        phone = ownerPhone,
                        email = ownerEmail,
                        pin = masterPin,
                        enableBiometric = true
                    )
                    showFingerprintPrompt = false
                },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Fingerprint,
                            contentDescription = null,
                            tint = YellowAccent,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Enable Fingerprint?",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "Use your device fingerprint sensor for instant 1-touch alarm disarming.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Surface(
                            color = SafeGreen.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = SafeGreen, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Touch sensor anytime to stop sirens instantly.",
                                    fontSize = 12.sp,
                                    color = SafeGreen,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val activity = context as? FragmentActivity
                            if (activity != null && BiometricHelper.checkIfBiometricAvailable(context)) {
                                BiometricHelper.authenticate(
                                    activity = activity,
                                    title = "Verify Fingerprint",
                                    subtitle = "Test your fingerprint sensor to complete setup",
                                    callback = { success ->
                                        if (success) {
                                            Toast.makeText(context, "Fingerprint verified!", Toast.LENGTH_SHORT).show()
                                        }
                                        viewModel.saveInitialSetup(
                                            name = ownerName,
                                            phone = ownerPhone,
                                            email = ownerEmail,
                                            pin = masterPin,
                                            enableBiometric = true
                                        )
                                        showFingerprintPrompt = false
                                    }
                                )
                            } else {
                                viewModel.saveInitialSetup(
                                    name = ownerName,
                                    phone = ownerPhone,
                                    email = ownerEmail,
                                    pin = masterPin,
                                    enableBiometric = true
                                )
                                showFingerprintPrompt = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = YellowAccent,
                            contentColor = NavyDark
                        ),
                        modifier = Modifier.testTag("btn_enable_fingerprint_setup")
                    ) {
                        Text("Enable Fingerprint", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            viewModel.saveInitialSetup(
                                name = ownerName,
                                phone = ownerPhone,
                                email = ownerEmail,
                                pin = masterPin,
                                enableBiometric = false
                            )
                            showFingerprintPrompt = false
                        },
                        modifier = Modifier.testTag("btn_skip_fingerprint_setup")
                    ) {
                        Text("Skip for Now")
                    }
                }
            )
        }
    }
}

package com.example.ui

import android.app.Application
import android.content.Context
import android.util.Patterns
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import com.example.data.AppPreferences
import com.example.data.UserPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.random.Random

data class OnboardingUiState(
    val email: String = "",
    val emailError: String? = null,
    val password: String = "",
    val confirmPassword: String = "",
    val passwordError: String? = null,
    val profileName: String = "",
    val profileNameError: String? = null,
    val phoneNumber: String = "+92 ",
    val phoneError: String? = null,
    val isOtpSent: Boolean = false,
    val generatedOtp: String = "",
    val enteredOtp: String = "",
    val otpError: String? = null,
    val biometricEnabled: Boolean = true,
    val loginPasswordInput: String = "",
    val loginPasswordError: String? = null,
    val isExistingAccount: Boolean = false
)

class OnboardingViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    init {
        val app = getApplication<Application>()
        val savedEmail = UserPreferences.getOwnerEmail(app)
        val savedName = UserPreferences.getOwnerName(app)
        val savedPhone = UserPreferences.getOwnerPhone(app)
        val hasCompleted = UserPreferences.isOnboardingCompleted(app)
        val bio = UserPreferences.isBiometricEnabled(app)

        _uiState.update {
            it.copy(
                email = savedEmail,
                profileName = savedName,
                phoneNumber = if (savedPhone.isNotBlank()) savedPhone else "+92 ",
                biometricEnabled = bio,
                isExistingAccount = hasCompleted
            )
        }
    }

    fun onEmailChanged(value: String) {
        _uiState.update { it.copy(email = value, emailError = null) }
    }

    fun validateAndProceedEmail(): Boolean {
        val email = _uiState.value.email.trim()
        if (email.isEmpty()) {
            _uiState.update { it.copy(emailError = "Email address cannot be empty") }
            return false
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _uiState.update { it.copy(emailError = "Please enter a valid Gmail / Email address") }
            return false
        }
        val app = getApplication<Application>()
        UserPreferences.setOwnerEmail(app, email)
        return true
    }

    fun onPasswordChanged(value: String) {
        _uiState.update { it.copy(password = value, passwordError = null) }
    }

    fun onConfirmPasswordChanged(value: String) {
        _uiState.update { it.copy(confirmPassword = value, passwordError = null) }
    }

    fun validateAndProceedPassword(): Boolean {
        val pass = _uiState.value.password
        val confirm = _uiState.value.confirmPassword

        if (pass.length < 6) {
            _uiState.update { it.copy(passwordError = "Password must be at least 6 characters") }
            return false
        }
        if (pass != confirm) {
            _uiState.update { it.copy(passwordError = "Passwords do not match") }
            return false
        }

        val app = getApplication<Application>()
        UserPreferences.setOwnerPassword(app, pass)
        return true
    }

    fun onProfileNameChanged(value: String) {
        _uiState.update { it.copy(profileName = value, profileNameError = null) }
    }

    fun validateAndProceedProfileName(): Boolean {
        val name = _uiState.value.profileName.trim()
        if (name.length < 2) {
            _uiState.update { it.copy(profileNameError = "Please enter your name (min 2 characters)") }
            return false
        }
        val app = getApplication<Application>()
        UserPreferences.setOwnerName(app, name)
        return true
    }

    fun onPhoneNumberChanged(value: String) {
        _uiState.update { it.copy(phoneNumber = value, phoneError = null) }
    }

    fun onEnteredOtpChanged(value: String) {
        if (value.length <= 6) {
            _uiState.update { it.copy(enteredOtp = value, otpError = null) }
        }
    }

    fun sendVerificationCode(context: Context): Boolean {
        val rawPhone = _uiState.value.phoneNumber.trim()
        val digitsOnly = rawPhone.filter { it.isDigit() }
        if (digitsOnly.length < 9) {
            _uiState.update { it.copy(phoneError = "Please enter a valid phone number (e.g. +92 300 1234567)") }
            return false
        }

        // Generate random 6-digit fake OTP
        val code = (100000..999999).random().toString()
        _uiState.update {
            it.copy(
                generatedOtp = code,
                isOtpSent = true,
                otpError = null,
                enteredOtp = ""
            )
        }

        // Display in Toast for easy testing as requested
        Toast.makeText(
            context,
            "Verification Code Sent: $code",
            Toast.LENGTH_LONG
        ).show()
        return true
    }

    fun verifyCodeAndProceed(context: Context): Boolean {
        val entered = _uiState.value.enteredOtp.trim()
        val generated = _uiState.value.generatedOtp

        if (entered.isEmpty()) {
            _uiState.update { it.copy(otpError = "Please enter the 6-digit code") }
            return false
        }

        // Accepts the generated code or fallback test code 123456
        if (entered != generated && entered != "123456") {
            _uiState.update { it.copy(otpError = "Incorrect verification code. Please check and retry.") }
            return false
        }

        UserPreferences.setOwnerPhone(context, _uiState.value.phoneNumber.trim(), verified = true)
        Toast.makeText(context, "✓ Phone verified successfully!", Toast.LENGTH_SHORT).show()
        return true
    }

    fun setBiometricChoice(enabled: Boolean, context: Context) {
        _uiState.update { it.copy(biometricEnabled = enabled) }
        UserPreferences.setBiometricEnabled(context, enabled)
    }

    fun completeOnboarding(context: Context) {
        val email = _uiState.value.email.ifBlank { UserPreferences.getOwnerEmail(context) }
        val name = _uiState.value.profileName.ifBlank { UserPreferences.getOwnerName(context) }
        val phone = _uiState.value.phoneNumber.ifBlank { UserPreferences.getOwnerPhone(context) }

        UserPreferences.setOwnerEmail(context, email)
        UserPreferences.setOwnerName(context, name)
        UserPreferences.setOwnerPhone(context, phone, verified = true)
        UserPreferences.setLoggedIn(context, true)
        UserPreferences.setOnboardingCompleted(context, true)
        AppPreferences.setSetupCompleted(context, true)
        AppPreferences.setUserEmail(context, email)
        AppPreferences.setUserDisplayName(context, name)
        AppPreferences.setEmergencyPhone(context, phone)
    }

    fun onLoginPasswordChanged(value: String) {
        _uiState.update { it.copy(loginPasswordInput = value, loginPasswordError = null) }
    }

    fun verifyReturningLogin(context: Context): Boolean {
        val input = _uiState.value.loginPasswordInput
        if (input.isEmpty()) {
            _uiState.update { it.copy(loginPasswordError = "Please enter your password") }
            return false
        }
        val isCorrect = UserPreferences.verifyPassword(context, input)
        if (!isCorrect) {
            _uiState.update { it.copy(loginPasswordError = "Incorrect password. Please try again.") }
            return false
        }
        UserPreferences.setLoggedIn(context, true)
        return true
    }
}

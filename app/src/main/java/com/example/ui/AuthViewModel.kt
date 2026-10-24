package com.example.ui

import android.app.Application
import android.util.Patterns
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.AppPreferences
import com.example.data.UserEntity
import com.example.data.UserPreferences
import com.example.security.PasswordUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class OtpChannel {
    EMAIL,
    SMS
}

data class AuthUiState(
    // Login
    val loginIdentifier: String = "",
    val loginPassword: String = "",
    val isLoginPasswordVisible: Boolean = false,
    val isLoginLoading: Boolean = false,
    val loginError: String? = null,
    val isAccountNotFound: Boolean = false,

    // Signup Steps: 1 = Form, 2 = Verify OTP, 3 = Success
    val signupStep: Int = 1,
    val signupFullName: String = "",
    val signupMobile: String = "+92 ",
    val signupEmail: String = "",
    val signupPassword: String = "",
    val signupConfirmPassword: String = "",
    val isSignupPasswordVisible: Boolean = false,
    val isSignupConfirmPasswordVisible: Boolean = false,
    val isSignupLoading: Boolean = false,
    val signupError: String? = null,

    // Signup OTP
    val signupOtpEntered: String = "",
    val signupGeneratedOtp: String? = null,
    val signupOtpExpiry: Long = 0L,
    val signupOtpChannel: OtpChannel = OtpChannel.EMAIL,
    val signupOtpTarget: String = "",
    val showChannelChoiceDialog: Boolean = false,
    val createdUser: UserEntity? = null,

    // Forgot Password Flow: 1 = Enter contact, 2 = Enter OTP, 3 = New Password, 4 = Success
    val showForgotDialog: Boolean = false,
    val forgotStep: Int = 1,
    val forgotIdentifier: String = "",
    val forgotError: String? = null,
    val forgotGeneratedOtp: String? = null,
    val forgotOtpEntered: String = "",
    val forgotOtpExpiry: Long = 0L,
    val forgotTargetUserId: Long? = null,
    val forgotTargetLabel: String = "",
    val forgotNewPassword: String = "",
    val forgotConfirmPassword: String = "",
    val isForgotNewPasswordVisible: Boolean = false,
    val isForgotConfirmPasswordVisible: Boolean = false,
    val isForgotLoading: Boolean = false,
    val forgotSuccessMessage: String? = null
)

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val userDao = AppDatabase.getDatabase(application).userDao()

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        val app = getApplication<Application>()
        val savedEmail = UserPreferences.getOwnerEmail(app)
        if (savedEmail.isNotBlank()) {
            _uiState.update { it.copy(loginIdentifier = savedEmail) }
        }
    }

    // ==========================================
    // LOGIN HANDLERS
    // ==========================================
    fun onLoginIdentifierChanged(value: String) {
        _uiState.update { it.copy(loginIdentifier = value, loginError = null, isAccountNotFound = false) }
    }

    fun onLoginPasswordChanged(value: String) {
        _uiState.update { it.copy(loginPassword = value, loginError = null) }
    }

    fun toggleLoginPasswordVisibility() {
        _uiState.update { it.copy(isLoginPasswordVisible = !it.isLoginPasswordVisible) }
    }

    fun login(onSuccess: (UserEntity) -> Unit) {
        val identifier = _uiState.value.loginIdentifier.trim()
        val password = _uiState.value.loginPassword

        if (identifier.isEmpty()) {
            _uiState.update { it.copy(loginError = "Please enter your mobile number or email") }
            return
        }

        if (password.isEmpty()) {
            _uiState.update { it.copy(loginError = "Please enter your password") }
            return
        }

        _uiState.update { it.copy(isLoginLoading = true, loginError = null) }

        viewModelScope.launch {
            try {
                val user = withContext(Dispatchers.IO) {
                    userDao.findUserByEmailOrMobile(identifier)
                        ?: userDao.findUserByEmail(identifier)
                        ?: userDao.findUserByMobile(identifier)
                }

                if (user == null) {
                    _uiState.update {
                        it.copy(
                            isLoginLoading = false,
                            loginError = "Account not found. Create one to protect your mobile.",
                            isAccountNotFound = true
                        )
                    }
                    return@launch
                }

                val matches = PasswordUtils.verifyPassword(password, user.password)
                if (!matches) {
                    _uiState.update {
                        it.copy(
                            isLoginLoading = false,
                            loginError = "Wrong password. Please try again or use Forgot password."
                        )
                    }
                    return@launch
                }

                // Password verified successfully!
                val app = getApplication<Application>()
                UserPreferences.setUserId(app, user.id)
                UserPreferences.setOwnerEmail(app, user.email)
                UserPreferences.setOwnerName(app, user.fullName)
                UserPreferences.setOwnerPhone(app, user.mobileNumber, verified = true)
                UserPreferences.setBiometricEnabled(app, user.fingerprintEnabled)
                UserPreferences.setLoggedIn(app, true)
                UserPreferences.setOnboardingCompleted(app, true)

                // Sync with legacy AppPreferences
                AppPreferences.setUserEmail(app, user.email)
                AppPreferences.setUserDisplayName(app, user.fullName)
                AppPreferences.setEmergencyPhone(app, user.mobileNumber)

                _uiState.update { it.copy(isLoginLoading = false, loginError = null) }
                onSuccess(user)
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoginLoading = false,
                        loginError = "Login error: ${e.localizedMessage ?: "Unknown error"}"
                    )
                }
            }
        }
    }

    // ==========================================
    // SIGNUP HANDLERS & 3-STEP OTP FLOW
    // ==========================================
    fun resetSignup() {
        _uiState.update {
            it.copy(
                signupStep = 1,
                signupFullName = "",
                signupMobile = "+92 ",
                signupEmail = "",
                signupPassword = "",
                signupConfirmPassword = "",
                isSignupPasswordVisible = false,
                isSignupConfirmPasswordVisible = false,
                isSignupLoading = false,
                signupError = null,
                signupOtpEntered = "",
                signupGeneratedOtp = null,
                showChannelChoiceDialog = false,
                createdUser = null
            )
        }
    }

    fun onSignupFullNameChanged(value: String) {
        _uiState.update { it.copy(signupFullName = value, signupError = null) }
    }

    fun onSignupMobileChanged(value: String) {
        _uiState.update { it.copy(signupMobile = value, signupError = null) }
    }

    fun onSignupEmailChanged(value: String) {
        _uiState.update { it.copy(signupEmail = value, signupError = null) }
    }

    fun onSignupPasswordChanged(value: String) {
        _uiState.update { it.copy(signupPassword = value, signupError = null) }
    }

    fun onSignupConfirmPasswordChanged(value: String) {
        _uiState.update { it.copy(signupConfirmPassword = value, signupError = null) }
    }

    fun toggleSignupPasswordVisibility() {
        _uiState.update { it.copy(isSignupPasswordVisible = !it.isSignupPasswordVisible) }
    }

    fun toggleSignupConfirmPasswordVisibility() {
        _uiState.update { it.copy(isSignupConfirmPasswordVisible = !it.isSignupConfirmPasswordVisible) }
    }

    fun onSignupOtpEnteredChanged(value: String) {
        if (value.length <= 6 && value.all { it.isDigit() }) {
            _uiState.update { it.copy(signupOtpEntered = value, signupError = null) }
        }
    }

    fun dismissChannelChoiceDialog() {
        _uiState.update { it.copy(showChannelChoiceDialog = false) }
    }

    /**
     * Step 1: User clicks [SEND OTP].
     * Evaluates Email & Phone routing rules:
     * Rule 1: Only Email -> Email OTP
     * Rule 2: Only Phone -> SMS OTP
     * Rule 3: Both -> Show Dialog to choose Email or SMS
     * Rule 4: Neither -> Error
     */
    fun startSignupOtpFlow() {
        val name = _uiState.value.signupFullName.trim()
        val mobile = _uiState.value.signupMobile.trim()
        val email = _uiState.value.signupEmail.trim()
        val password = _uiState.value.signupPassword
        val confirmPassword = _uiState.value.signupConfirmPassword

        if (name.length < 2) {
            _uiState.update { it.copy(signupError = "Please enter your full name (at least 2 letters)") }
            return
        }

        if (password.length < 6) {
            _uiState.update { it.copy(signupError = "Password must be at least 6 characters") }
            return
        }

        if (password != confirmPassword) {
            _uiState.update { it.copy(signupError = "Passwords do not match") }
            return
        }

        val digits = mobile.filter { it.isDigit() }
        // Clean check: user entered more than default "+92" (which has 2 digits)
        val hasPhone = digits.length >= 7
        val hasEmail = email.isNotEmpty() && Patterns.EMAIL_ADDRESS.matcher(email).matches()

        // Rule 4: If neither valid email nor phone
        if (!hasEmail && !hasPhone) {
            _uiState.update { it.copy(signupError = "Please enter a valid email address or phone number") }
            return
        }

        // Rule 3: If BOTH email and phone provided -> prompt user to pick
        if (hasEmail && hasPhone) {
            _uiState.update { it.copy(showChannelChoiceDialog = true, signupError = null) }
            return
        }

        // Rule 1: Only email
        if (hasEmail) {
            dispatchSignupOtp(OtpChannel.EMAIL)
            return
        }

        // Rule 2: Only phone
        dispatchSignupOtp(OtpChannel.SMS)
    }

    fun chooseOtpChannel(channel: OtpChannel) {
        _uiState.update { it.copy(showChannelChoiceDialog = false) }
        dispatchSignupOtp(channel)
    }

    private fun dispatchSignupOtp(channel: OtpChannel) {
        _uiState.update { it.copy(isSignupLoading = true, signupError = null) }

        viewModelScope.launch {
            try {
                val email = _uiState.value.signupEmail.trim()
                val mobile = _uiState.value.signupMobile.trim()

                // Check uniqueness in Room
                if (email.isNotEmpty()) {
                    val existing = withContext(Dispatchers.IO) { userDao.findUserByEmail(email) }
                    if (existing != null) {
                        _uiState.update {
                            it.copy(
                                isSignupLoading = false,
                                signupError = "An account with this email already exists. Please log in."
                            )
                        }
                        return@launch
                    }
                }

                val digits = mobile.filter { it.isDigit() }
                if (digits.length >= 7) {
                    val existing = withContext(Dispatchers.IO) { userDao.findUserByMobile(mobile) }
                    if (existing != null) {
                        _uiState.update {
                            it.copy(
                                isSignupLoading = false,
                                signupError = "An account with this mobile number already exists."
                            )
                        }
                        return@launch
                    }
                }

                // Generate 6-digit random code
                val otp = (100000..999999).random().toString()
                val expiry = System.currentTimeMillis() + (5 * 60 * 1000) // 5 minutes
                val target = if (channel == OtpChannel.EMAIL) email else mobile

                _uiState.update {
                    it.copy(
                        isSignupLoading = false,
                        signupStep = 2,
                        signupGeneratedOtp = otp,
                        signupOtpExpiry = expiry,
                        signupOtpChannel = channel,
                        signupOtpTarget = target,
                        signupOtpEntered = "",
                        signupError = null
                    )
                }

                val toastMsg = if (channel == OtpChannel.EMAIL) {
                    "OTP sent to your email ($target)\nYour OTP is: $otp"
                } else {
                    "OTP sent via SMS ($target)\nYour OTP is: $otp"
                }

                withContext(Dispatchers.Main) {
                    Toast.makeText(getApplication(), toastMsg, Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isSignupLoading = false,
                        signupError = "Failed to send OTP: ${e.localizedMessage ?: "Unknown error"}"
                    )
                }
            }
        }
    }

    fun resendSignupOtp() {
        val channel = _uiState.value.signupOtpChannel
        val otp = (100000..999999).random().toString()
        val expiry = System.currentTimeMillis() + (5 * 60 * 1000)
        val target = _uiState.value.signupOtpTarget

        _uiState.update {
            it.copy(
                signupGeneratedOtp = otp,
                signupOtpExpiry = expiry,
                signupOtpEntered = "",
                signupError = null
            )
        }

        val toastMsg = if (channel == OtpChannel.EMAIL) {
            "New OTP sent to your email ($target)\nYour OTP is: $otp"
        } else {
            "New OTP sent via SMS ($target)\nYour OTP is: $otp"
        }

        Toast.makeText(getApplication(), toastMsg, Toast.LENGTH_LONG).show()
    }

    /**
     * Step 2: User taps [VERIFY OTP].
     */
    fun verifySignupOtp() {
        val entered = _uiState.value.signupOtpEntered.trim()
        val generated = _uiState.value.signupGeneratedOtp
        val expiry = _uiState.value.signupOtpExpiry

        if (entered.length != 6) {
            _uiState.update { it.copy(signupError = "Please enter the complete 6-digit OTP") }
            return
        }

        if (System.currentTimeMillis() > expiry) {
            _uiState.update { it.copy(signupError = "OTP has expired. Tap Resend to get a new code.") }
            return
        }

        if (entered != generated) {
            _uiState.update { it.copy(signupError = "Invalid OTP code. Please check and try again.") }
            return
        }

        // Verified! Save to Room DB and move to Step 3 (Success)
        _uiState.update { it.copy(isSignupLoading = true, signupError = null) }

        viewModelScope.launch {
            try {
                val name = _uiState.value.signupFullName.trim()
                val mobile = _uiState.value.signupMobile.trim()
                val email = _uiState.value.signupEmail.trim()
                val password = _uiState.value.signupPassword

                val hashedPassword = PasswordUtils.hashPassword(password)
                val newUser = UserEntity(
                    fullName = name,
                    mobileNumber = mobile,
                    email = email,
                    password = hashedPassword,
                    isVerified = true,
                    fingerprintEnabled = true,
                    createdAt = System.currentTimeMillis()
                )

                val generatedId = withContext(Dispatchers.IO) {
                    userDao.insertUser(newUser)
                }
                val savedUser = newUser.copy(id = generatedId)

                // Save session in SharedPreferences
                val app = getApplication<Application>()
                UserPreferences.setUserId(app, generatedId)
                UserPreferences.setOwnerName(app, name)
                UserPreferences.setOwnerPhone(app, mobile, verified = true)
                UserPreferences.setOwnerEmail(app, email)
                UserPreferences.setOwnerPassword(app, password)
                UserPreferences.setBiometricEnabled(app, true)
                UserPreferences.setLoggedIn(app, true)
                UserPreferences.setOnboardingCompleted(app, true)

                // Sync with legacy AppPreferences
                AppPreferences.setUserEmail(app, email)
                AppPreferences.setUserDisplayName(app, name)
                AppPreferences.setEmergencyPhone(app, mobile)
                AppPreferences.setSetupCompleted(app, true)

                _uiState.update {
                    it.copy(
                        isSignupLoading = false,
                        signupStep = 3,
                        createdUser = savedUser,
                        signupError = null
                    )
                }

                withContext(Dispatchers.Main) {
                    Toast.makeText(getApplication(), "✅ Account verified & created!", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isSignupLoading = false,
                        signupError = "Error saving account: ${e.localizedMessage ?: "Unknown error"}"
                    )
                }
            }
        }
    }

    // ==========================================
    // FORGOT PASSWORD FLOW (4 STEPS)
    // ==========================================
    fun openForgotPasswordDialog() {
        val currentIdentifier = _uiState.value.loginIdentifier.trim()
        _uiState.update {
            it.copy(
                showForgotDialog = true,
                forgotStep = 1,
                forgotIdentifier = currentIdentifier,
                forgotError = null,
                forgotGeneratedOtp = null,
                forgotOtpEntered = "",
                forgotNewPassword = "",
                forgotConfirmPassword = "",
                isForgotNewPasswordVisible = false,
                isForgotConfirmPasswordVisible = false,
                isForgotLoading = false,
                forgotSuccessMessage = null
            )
        }
    }

    fun closeForgotPasswordDialog() {
        _uiState.update { it.copy(showForgotDialog = false, forgotError = null) }
    }

    fun onForgotIdentifierChanged(value: String) {
        _uiState.update { it.copy(forgotIdentifier = value, forgotError = null) }
    }

    fun onForgotOtpEnteredChanged(value: String) {
        if (value.length <= 6 && value.all { it.isDigit() }) {
            _uiState.update { it.copy(forgotOtpEntered = value, forgotError = null) }
        }
    }

    fun onForgotNewPasswordChanged(value: String) {
        _uiState.update { it.copy(forgotNewPassword = value, forgotError = null) }
    }

    fun onForgotConfirmPasswordChanged(value: String) {
        _uiState.update { it.copy(forgotConfirmPassword = value, forgotError = null) }
    }

    fun toggleForgotNewPasswordVisibility() {
        _uiState.update { it.copy(isForgotNewPasswordVisible = !it.isForgotNewPasswordVisible) }
    }

    fun toggleForgotConfirmPasswordVisibility() {
        _uiState.update { it.copy(isForgotConfirmPasswordVisible = !it.isForgotConfirmPasswordVisible) }
    }

    /**
     * Forgot Password Step 1: Send OTP to email or phone.
     */
    fun sendForgotOtp() {
        val identifier = _uiState.value.forgotIdentifier.trim()
        if (identifier.isEmpty()) {
            _uiState.update { it.copy(forgotError = "Please enter your registered email or phone") }
            return
        }

        _uiState.update { it.copy(isForgotLoading = true, forgotError = null) }

        viewModelScope.launch {
            try {
                val user = withContext(Dispatchers.IO) {
                    userDao.findUserByEmailOrMobile(identifier)
                        ?: userDao.findUserByEmail(identifier)
                        ?: userDao.findUserByMobile(identifier)
                }

                if (user == null) {
                    _uiState.update {
                        it.copy(
                            isForgotLoading = false,
                            forgotError = "No account found for $identifier. Please check or create an account."
                        )
                    }
                    return@launch
                }

                val otp = (100000..999999).random().toString()
                val expiry = System.currentTimeMillis() + (5 * 60 * 1000)
                val isEmail = identifier.contains("@")
                val targetLabel = if (isEmail && user.email.isNotBlank()) user.email else user.mobileNumber

                _uiState.update {
                    it.copy(
                        isForgotLoading = false,
                        forgotStep = 2,
                        forgotGeneratedOtp = otp,
                        forgotOtpExpiry = expiry,
                        forgotTargetUserId = user.id,
                        forgotTargetLabel = targetLabel,
                        forgotOtpEntered = "",
                        forgotError = null
                    )
                }

                val toastMsg = if (isEmail) {
                    "Reset code sent to your email ($targetLabel)\nYour OTP is: $otp"
                } else {
                    "Reset code sent via SMS ($targetLabel)\nYour OTP is: $otp"
                }

                withContext(Dispatchers.Main) {
                    Toast.makeText(getApplication(), toastMsg, Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isForgotLoading = false,
                        forgotError = "Failed to send reset code: ${e.localizedMessage ?: "Unknown error"}"
                    )
                }
            }
        }
    }

    fun resendForgotOtp() {
        val otp = (100000..999999).random().toString()
        val expiry = System.currentTimeMillis() + (5 * 60 * 1000)
        val targetLabel = _uiState.value.forgotTargetLabel

        _uiState.update {
            it.copy(
                forgotGeneratedOtp = otp,
                forgotOtpExpiry = expiry,
                forgotOtpEntered = "",
                forgotError = null
            )
        }

        val toastMsg = "New reset code sent ($targetLabel)\nYour OTP is: $otp"
        Toast.makeText(getApplication(), toastMsg, Toast.LENGTH_LONG).show()
    }

    /**
     * Forgot Password Step 2: Verify the 6-digit OTP code.
     */
    fun verifyForgotOtp() {
        val entered = _uiState.value.forgotOtpEntered.trim()
        val generated = _uiState.value.forgotGeneratedOtp
        val expiry = _uiState.value.forgotOtpExpiry

        if (entered.length != 6) {
            _uiState.update { it.copy(forgotError = "Please enter the 6-digit verification code") }
            return
        }

        if (System.currentTimeMillis() > expiry) {
            _uiState.update { it.copy(forgotError = "Verification code expired. Tap Resend.") }
            return
        }

        if (entered != generated) {
            _uiState.update { it.copy(forgotError = "Invalid verification code. Please check and try again.") }
            return
        }

        // Move to Step 3: Set New Password
        _uiState.update {
            it.copy(
                forgotStep = 3,
                forgotError = null
            )
        }
    }

    /**
     * Forgot Password Step 3: Save new password to Room DB.
     */
    fun resetPassword() {
        val newPass = _uiState.value.forgotNewPassword
        val confirm = _uiState.value.forgotConfirmPassword
        val userId = _uiState.value.forgotTargetUserId

        if (newPass.length < 6) {
            _uiState.update { it.copy(forgotError = "Password must be at least 6 characters") }
            return
        }

        if (newPass != confirm) {
            _uiState.update { it.copy(forgotError = "Passwords do not match") }
            return
        }

        if (userId == null) {
            _uiState.update { it.copy(forgotError = "Session error. Please restart reset.") }
            return
        }

        _uiState.update { it.copy(isForgotLoading = true, forgotError = null) }

        viewModelScope.launch {
            try {
                val hashedPassword = PasswordUtils.hashPassword(newPass)
                withContext(Dispatchers.IO) {
                    userDao.updatePasswordById(userId, hashedPassword)
                }

                val app = getApplication<Application>()
                if (UserPreferences.getUserId(app) == userId) {
                    UserPreferences.setOwnerPassword(app, newPass)
                }

                // Pre-fill login identifier for quick login
                val verifiedTarget = _uiState.value.forgotTargetLabel
                _uiState.update {
                    it.copy(
                        isForgotLoading = false,
                        forgotStep = 4,
                        forgotSuccessMessage = "Password reset successfully! You can now log in.",
                        loginIdentifier = verifiedTarget,
                        loginPassword = "",
                        forgotError = null
                    )
                }

                withContext(Dispatchers.Main) {
                    Toast.makeText(getApplication(), "✅ Password reset successfully!", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isForgotLoading = false,
                        forgotError = "Failed to update password: ${e.localizedMessage ?: "Unknown error"}"
                    )
                }
            }
        }
    }
}

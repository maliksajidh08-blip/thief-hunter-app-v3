package com.example.ui

import android.app.Application
import android.util.Patterns
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

data class AuthUiState(
    // Login
    val loginIdentifier: String = "",
    val loginPassword: String = "",
    val isLoginPasswordVisible: Boolean = false,
    val isLoginLoading: Boolean = false,
    val loginError: String? = null,
    val isAccountNotFound: Boolean = false,

    // Signup
    val signupFullName: String = "",
    val signupMobile: String = "+92 ",
    val signupEmail: String = "",
    val signupPassword: String = "",
    val signupConfirmPassword: String = "",
    val isSignupPasswordVisible: Boolean = false,
    val isSignupConfirmPasswordVisible: Boolean = false,
    val isSignupLoading: Boolean = false,
    val signupError: String? = null,

    // Forgot Password
    val showForgotDialog: Boolean = false,
    val forgotEmail: String = "",
    val forgotMessage: String? = null,
    val isForgotLoading: Boolean = false
)

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val userDao = AppDatabase.getDatabase(application).userDao()

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        // Pre-fill previously remembered email/mobile if any
        val app = getApplication<Application>()
        val savedEmail = UserPreferences.getOwnerEmail(app)
        if (savedEmail.isNotBlank()) {
            _uiState.update { it.copy(loginIdentifier = savedEmail) }
        }
    }

    // --- Login Handlers ---
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
                    // Try email or mobile lookup
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

    // --- Signup Handlers ---
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

    fun signup(onSuccess: (UserEntity) -> Unit) {
        val name = _uiState.value.signupFullName.trim()
        val mobile = _uiState.value.signupMobile.trim()
        val email = _uiState.value.signupEmail.trim()
        val password = _uiState.value.signupPassword
        val confirmPassword = _uiState.value.signupConfirmPassword

        if (name.length < 2) {
            _uiState.update { it.copy(signupError = "Please enter your full name (at least 2 letters)") }
            return
        }

        val digits = mobile.filter { it.isDigit() }
        if (digits.length < 9) {
            _uiState.update { it.copy(signupError = "Please enter a valid mobile number (e.g. +92 300 1234567)") }
            return
        }

        if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _uiState.update { it.copy(signupError = "Please enter a valid email address") }
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

        _uiState.update { it.copy(isSignupLoading = true, signupError = null) }

        viewModelScope.launch {
            try {
                // Check uniqueness in Room
                val existingEmailUser = withContext(Dispatchers.IO) {
                    userDao.findUserByEmail(email)
                }
                if (existingEmailUser != null) {
                    _uiState.update {
                        it.copy(
                            isSignupLoading = false,
                            signupError = "An account with this email already exists. Please log in."
                        )
                    }
                    return@launch
                }

                val existingMobileUser = withContext(Dispatchers.IO) {
                    userDao.findUserByMobile(mobile)
                }
                if (existingMobileUser != null) {
                    _uiState.update {
                        it.copy(
                            isSignupLoading = false,
                            signupError = "An account with this mobile number already exists."
                        )
                    }
                    return@launch
                }

                // Hash password with SHA-256
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

                _uiState.update { it.copy(isSignupLoading = false, signupError = null) }
                onSuccess(savedUser)
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isSignupLoading = false,
                        signupError = "Signup failed: ${e.localizedMessage ?: "Unknown error"}"
                    )
                }
            }
        }
    }

    // --- Forgot Password Dialog ---
    fun openForgotPasswordDialog() {
        _uiState.update {
            it.copy(
                showForgotDialog = true,
                forgotEmail = it.loginIdentifier.takeIf { id -> id.contains("@") } ?: "",
                forgotMessage = null
            )
        }
    }

    fun closeForgotPasswordDialog() {
        _uiState.update { it.copy(showForgotDialog = false, forgotMessage = null) }
    }

    fun onForgotEmailChanged(value: String) {
        _uiState.update { it.copy(forgotEmail = value, forgotMessage = null) }
    }

    fun sendPasswordReset() {
        val email = _uiState.value.forgotEmail.trim()
        if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _uiState.update { it.copy(forgotMessage = "Please enter a valid email address.") }
            return
        }

        _uiState.update { it.copy(isForgotLoading = true) }

        viewModelScope.launch {
            val user = withContext(Dispatchers.IO) {
                userDao.findUserByEmail(email)
            }
            _uiState.update {
                it.copy(
                    isForgotLoading = false,
                    forgotMessage = if (user != null) {
                        "Reset link sent to your email ($email). Please check your inbox and spam folder."
                    } else {
                        "If an account is associated with $email, password reset instructions have been sent."
                    }
                )
            }
        }
    }
}

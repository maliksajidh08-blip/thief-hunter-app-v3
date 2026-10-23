package com.example.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import java.security.MessageDigest

/**
 * SharedPreferences helper specifically managing User Onboarding and Authentication state:
 * - owner_email
 * - owner_password (securely hashed)
 * - owner_name
 * - owner_phone
 * - biometric_enabled
 * - is_logged_in
 * - is_onboarding_completed
 */
object UserPreferences {
    private const val PREFS_NAME = "thief_hunter_user_prefs"

    const val KEY_OWNER_EMAIL = "owner_email"
    const val KEY_OWNER_PASSWORD = "owner_password"
    const val KEY_OWNER_NAME = "owner_name"
    const val KEY_OWNER_PHONE = "owner_phone"
    const val KEY_PHONE_VERIFIED = "phone_verified"
    const val KEY_BIOMETRIC_ENABLED = "biometric_enabled"
    const val KEY_IS_LOGGED_IN = "is_logged_in"
    const val KEY_IS_ONBOARDING_COMPLETED = "is_onboarding_completed"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun hashPassword(password: String): String {
        return try {
            val md = MessageDigest.getInstance("SHA-256")
            val digest = md.digest(password.toByteArray(Charsets.UTF_8))
            Base64.encodeToString(digest, Base64.NO_WRAP)
        } catch (_: Exception) {
            password
        }
    }

    fun verifyPassword(context: Context, entered: String): Boolean {
        val storedHash = getOwnerPassword(context)
        if (storedHash.isBlank()) return true
        val enteredHash = hashPassword(entered)
        return enteredHash == storedHash || entered == storedHash
    }

    fun getOwnerEmail(context: Context): String {
        return getPrefs(context).getString(KEY_OWNER_EMAIL, "") ?: ""
    }

    fun setOwnerEmail(context: Context, email: String) {
        getPrefs(context).edit().putString(KEY_OWNER_EMAIL, email.trim()).apply()
        // Sync with legacy AppPreferences for background services
        AppPreferences.setOwnerEmail(context, email.trim())
    }

    fun getOwnerPassword(context: Context): String {
        return getPrefs(context).getString(KEY_OWNER_PASSWORD, "") ?: ""
    }

    fun setOwnerPassword(context: Context, rawPassword: String) {
        val hashed = hashPassword(rawPassword)
        getPrefs(context).edit().putString(KEY_OWNER_PASSWORD, hashed).apply()
    }

    fun getOwnerName(context: Context): String {
        return getPrefs(context).getString(KEY_OWNER_NAME, "") ?: ""
    }

    fun setOwnerName(context: Context, name: String) {
        getPrefs(context).edit().putString(KEY_OWNER_NAME, name.trim()).apply()
        AppPreferences.setOwnerName(context, name.trim())
    }

    fun getOwnerPhone(context: Context): String {
        return getPrefs(context).getString(KEY_OWNER_PHONE, "") ?: ""
    }

    fun setOwnerPhone(context: Context, phone: String, verified: Boolean = true) {
        getPrefs(context).edit()
            .putString(KEY_OWNER_PHONE, phone.trim())
            .putBoolean(KEY_PHONE_VERIFIED, verified)
            .apply()
        AppPreferences.setOwnerPhone(context, phone.trim())
    }

    fun isPhoneVerified(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_PHONE_VERIFIED, false)
    }

    fun isBiometricEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_BIOMETRIC_ENABLED, true)
    }

    fun setBiometricEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_BIOMETRIC_ENABLED, enabled).apply()
        AppPreferences.setBiometricEnabled(context, enabled)
    }

    fun isLoggedIn(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_IS_LOGGED_IN, false)
    }

    fun setLoggedIn(context: Context, loggedIn: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_IS_LOGGED_IN, loggedIn).apply()
    }

    fun setUserId(context: Context, id: Long) {
        getPrefs(context).edit().putLong("user_id", id).apply()
    }

    fun getUserId(context: Context): Long {
        return getPrefs(context).getLong("user_id", -1L)
    }

    fun isOnboardingCompleted(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_IS_ONBOARDING_COMPLETED, false)
    }

    fun setOnboardingCompleted(context: Context, completed: Boolean) {
        getPrefs(context).edit()
            .putBoolean(KEY_IS_ONBOARDING_COMPLETED, completed)
            .apply()
        AppPreferences.setSetupCompleted(context, completed)
    }

    fun logout(context: Context) {
        getPrefs(context).edit().putBoolean(KEY_IS_LOGGED_IN, false).apply()
        AppPreferences.setUserEmail(context, null)
    }

    fun resetAll(context: Context) {
        getPrefs(context).edit().clear().apply()
        AppPreferences.clearUserSession(context)
    }
}

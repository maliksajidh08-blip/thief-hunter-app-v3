package com.example.security

import android.util.Base64
import java.security.MessageDigest

object PasswordUtils {
    /**
     * Hashes password using SHA-256 with Base64 encoding.
     */
    fun hashPassword(password: String): String {
        return try {
            val md = MessageDigest.getInstance("SHA-256")
            val digest = md.digest(password.toByteArray(Charsets.UTF_8))
            Base64.encodeToString(digest, Base64.NO_WRAP)
        } catch (_: Exception) {
            password
        }
    }

    /**
     * Verifies raw password against stored hash.
     */
    fun verifyPassword(rawPassword: String, storedHash: String): Boolean {
        if (storedHash.isBlank()) return false
        val computed = hashPassword(rawPassword)
        return computed == storedHash || rawPassword == storedHash
    }
}

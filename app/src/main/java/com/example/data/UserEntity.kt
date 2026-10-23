package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val fullName: String,
    val mobileNumber: String,
    val email: String,
    val password: String,  // Hashed with SHA-256
    val isVerified: Boolean = false,
    val fingerprintEnabled: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

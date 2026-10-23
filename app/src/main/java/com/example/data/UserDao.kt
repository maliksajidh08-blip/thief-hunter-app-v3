package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity): Long

    @Query("SELECT * FROM users WHERE LOWER(email) = LOWER(:email) LIMIT 1")
    suspend fun findUserByEmail(email: String): UserEntity?

    @Query("SELECT * FROM users WHERE mobileNumber = :mobile LIMIT 1")
    suspend fun findUserByMobile(mobile: String): UserEntity?

    @Query("SELECT * FROM users WHERE LOWER(email) = LOWER(:input) OR mobileNumber = :input LIMIT 1")
    suspend fun findUserByEmailOrMobile(input: String): UserEntity?

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    suspend fun getUserById(id: Long): UserEntity?

    @Query("UPDATE users SET fingerprintEnabled = :enabled WHERE id = :id")
    suspend fun updateFingerprint(id: Long, enabled: Boolean)

    @Query("SELECT * FROM users ORDER BY id DESC")
    fun getAllUsers(): Flow<List<UserEntity>>

    @Query("UPDATE users SET password = :newPassword WHERE LOWER(email) = LOWER(:email)")
    suspend fun updatePasswordByEmail(email: String, newPassword: String): Int
}

package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "users",
    indices = [
        Index(value = ["email"], unique = true),
        Index(value = ["employee_id"], unique = true)
    ]
)
data class UserEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val full_name: String,
    val email: String,
    val employee_id: String,
    val mobile: String,
    val department: String,
    val role: String, // "NURSE" or "DOCTOR"
    val password_hash: String,
    val status: String = "ACTIVE", // "ACTIVE" or "INACTIVE"
    val created_at: Long = System.currentTimeMillis(),
    val updated_at: Long = System.currentTimeMillis()
)

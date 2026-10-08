package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "audit_logs",
    indices = [
        Index(value = ["user_id"]),
        Index(value = ["action"]),
        Index(value = ["timestamp"])
    ]
)
data class AuditLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val user_id: Long,
    val user_name: String,
    val user_role: String,
    val action: String, // "LOGIN", "REGISTER", "PATIENT_CREATED", "PATIENT_EDITED", "PATIENT_COPIED", "PATIENT_DELETED", "EXCEL_EXPORT", "USER_STATUS_CHANGED", "SETTINGS_UPDATED"
    val record_id: Long? = null,
    val details: String,
    val timestamp: Long = System.currentTimeMillis(),
    val device_info: String = "Hospital Clinical Terminal"
)

package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "hospital_settings")
data class HospitalSettingEntity(
    @PrimaryKey
    val key: String,
    val category: String, // "SPECIALTY", "DIAGNOSIS", "TRANSFER", "TAEI", "MEDICOLEGAL"
    val value: String
)

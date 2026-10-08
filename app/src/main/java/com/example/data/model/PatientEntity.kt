package com.example.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "patients",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["entered_by"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index(value = ["serial_no"]),
        Index(value = ["ip_no"]),
        Index(value = ["entered_by"]),
        Index(value = ["status"]),
        Index(value = ["admission_datetime"])
    ]
)
data class PatientEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val serial_no: Long,
    val ip_no: String,
    val name: String,
    val age: Int,
    val sex: String, // "Male", "Female", "Other"
    val admission_datetime: String, // YYYY-MM-DD HH:mm
    val patient_received_time: String, // HH:mm or YYYY-MM-DD HH:mm
    val broad_speciality_category: String,
    val diagnosis: String,
    val age_interval: String, // "Below 12", "12-60", "Above 60"
    val taei_category: String, // "TAEI Pillar", "TAEI Non Pillar"
    val medicolegal_category: String, // "MLC", "Non-MLC"
    val transferred_out: String, // "Yes", "No", or Ward/Unit
    val transferred_out_time: String = "",
    val emergency_response_time: String = "Immediate (<1 min)",
    val entered_by: Long,
    val entered_by_name: String,
    val created_at: Long = System.currentTimeMillis(),
    val updated_at: Long = System.currentTimeMillis(),
    val updated_by: Long? = null,
    val updated_by_name: String? = null,
    val deleted_at: Long? = null,
    val deleted_by: Long? = null,
    val deleted_by_name: String? = null,
    val status: String = "ACTIVE" // "ACTIVE" or "DELETED"
)

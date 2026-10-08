package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.dao.AuditLogDao
import com.example.data.dao.HospitalSettingDao
import com.example.data.dao.PatientDao
import com.example.data.dao.UserDao
import com.example.data.model.AuditLogEntity
import com.example.data.model.HospitalSettingEntity
import com.example.data.model.PatientEntity
import com.example.data.model.UserEntity
import com.example.data.security.SecurityUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        UserEntity::class,
        PatientEntity::class,
        AuditLogEntity::class,
        HospitalSettingEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class HospitalDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun patientDao(): PatientDao
    abstract fun auditLogDao(): AuditLogDao
    abstract fun settingDao(): HospitalSettingDao

    companion object {
        @Volatile
        private var INSTANCE: HospitalDatabase? = null

        fun getDatabase(context: Context): HospitalDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    HospitalDatabase::class.java,
                    "hospital_pdms_db"
                )
                    .addCallback(DatabaseCallback())
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    CoroutineScope(Dispatchers.IO).launch {
                        seedDatabase(database)
                    }
                }
            }
        }

        suspend fun seedDatabase(db: HospitalDatabase) {
            val userDao = db.userDao()
            val patientDao = db.patientDao()
            val auditLogDao = db.auditLogDao()
            val settingDao = db.settingDao()

            // 1. Seed Doctor/Admin
            val doctorId = userDao.insertUser(
                UserEntity(
                    full_name = "Dr. Sarah Mitchell, MD",
                    email = "doctor@hospital.org",
                    employee_id = "DOC-1001",
                    mobile = "+1 555-019-2834",
                    department = "Emergency & Critical Care",
                    role = "DOCTOR",
                    password_hash = SecurityUtils.hashPassword("Doctor@123"),
                    status = "ACTIVE"
                )
            )

            // 2. Seed Nurses
            val nurse1Id = userDao.insertUser(
                UserEntity(
                    full_name = "Nurse Clara Barton, RN",
                    email = "clara.barton@hospital.org",
                    employee_id = "NUR-4021",
                    mobile = "+1 555-014-9912",
                    department = "Emergency Trauma Unit",
                    role = "NURSE",
                    password_hash = SecurityUtils.hashPassword("Nurse@123"),
                    status = "ACTIVE"
                )
            )

            val nurse2Id = userDao.insertUser(
                UserEntity(
                    full_name = "Nurse Florence Hayes, BSN",
                    email = "florence.hayes@hospital.org",
                    employee_id = "NUR-4022",
                    mobile = "+1 555-018-7733",
                    department = "Trauma Resuscitation",
                    role = "NURSE",
                    password_hash = SecurityUtils.hashPassword("Nurse@123"),
                    status = "ACTIVE"
                )
            )

            // 3. Seed Configurable Settings
            val settings = listOf(
                HospitalSettingEntity("spec_1", "SPECIALTY", "Trauma / Surgery"),
                HospitalSettingEntity("spec_2", "SPECIALTY", "Emergency Medicine"),
                HospitalSettingEntity("spec_3", "SPECIALTY", "Cardiology / CCU"),
                HospitalSettingEntity("spec_4", "SPECIALTY", "Neurology / Stroke"),
                HospitalSettingEntity("spec_5", "SPECIALTY", "Pediatric Emergency"),
                HospitalSettingEntity("spec_6", "SPECIALTY", "Orthopedics"),
                HospitalSettingEntity("spec_7", "SPECIALTY", "Toxicology & Poison"),
                HospitalSettingEntity("spec_8", "SPECIALTY", "General Medicine"),

                HospitalSettingEntity("diag_1", "DIAGNOSIS", "Acute Myocardial Infarction (STEMI)"),
                HospitalSettingEntity("diag_2", "DIAGNOSIS", "Polytrauma with Femur Fracture"),
                HospitalSettingEntity("diag_3", "DIAGNOSIS", "Acute Appendicitis with Perforation"),
                HospitalSettingEntity("diag_4", "DIAGNOSIS", "Acute Ischemic Stroke"),
                HospitalSettingEntity("diag_5", "DIAGNOSIS", "Severe Respiratory Distress (Asthma/COPD)"),
                HospitalSettingEntity("diag_6", "DIAGNOSIS", "Traumatic Brain Injury (Subdural Hematoma)"),
                HospitalSettingEntity("diag_7", "DIAGNOSIS", "Diabetic Ketoacidosis (DKA)"),
                HospitalSettingEntity("diag_8", "DIAGNOSIS", "Severe Sepsis secondary to UTI"),
                HospitalSettingEntity("diag_9", "DIAGNOSIS", "Organophosphorus Poisoning"),
                HospitalSettingEntity("diag_10", "DIAGNOSIS", "Acute Pancreatitis"),

                HospitalSettingEntity("taei_1", "TAEI", "TAEI Pillar"),
                HospitalSettingEntity("taei_2", "TAEI", "TAEI Non Pillar"),

                HospitalSettingEntity("mlc_1", "MEDICOLEGAL", "MLC"),
                HospitalSettingEntity("mlc_2", "MEDICOLEGAL", "Non-MLC"),

                HospitalSettingEntity("trans_1", "TRANSFER", "No"),
                HospitalSettingEntity("trans_2", "TRANSFER", "Yes - Emergency OT"),
                HospitalSettingEntity("trans_3", "TRANSFER", "Yes - Intensive Care Unit (ICU)"),
                HospitalSettingEntity("trans_4", "TRANSFER", "Yes - Coronary Care Unit (CCU)"),
                HospitalSettingEntity("trans_5", "TRANSFER", "Yes - Tertiary Trauma Center"),
                HospitalSettingEntity("trans_6", "TRANSFER", "Yes - Inpatient Ward")
            )

            settings.forEach { settingDao.insertOrUpdateSetting(it) }

            // 4. Seed Initial Patient Records with exact required columns
            val samplePatients = listOf(
                PatientEntity(
                    serial_no = 1,
                    ip_no = "IP-2026-8901",
                    name = "Robert Anderson",
                    age = 54,
                    sex = "Male",
                    admission_datetime = "2026-08-11 08:30",
                    patient_received_time = "08:32",
                    broad_speciality_category = "Cardiology / CCU",
                    diagnosis = "Acute Myocardial Infarction (STEMI)",
                    age_interval = "12-60",
                    taei_category = "TAEI Pillar",
                    medicolegal_category = "Non-MLC",
                    transferred_out = "Yes - Coronary Care Unit (CCU)",
                    transferred_out_time = "09:45",
                    emergency_response_time = "Immediate (<1 min)",
                    entered_by = nurse1Id,
                    entered_by_name = "Nurse Clara Barton, RN"
                ),
                PatientEntity(
                    serial_no = 2,
                    ip_no = "IP-2026-8902",
                    name = "Maya Sundaram",
                    age = 29,
                    sex = "Female",
                    admission_datetime = "2026-08-11 10:15",
                    patient_received_time = "10:18",
                    broad_speciality_category = "Trauma / Surgery",
                    diagnosis = "Polytrauma with Femur Fracture",
                    age_interval = "12-60",
                    taei_category = "TAEI Pillar",
                    medicolegal_category = "MLC",
                    transferred_out = "Yes - Emergency OT",
                    transferred_out_time = "11:20",
                    emergency_response_time = "3 mins",
                    entered_by = nurse1Id,
                    entered_by_name = "Nurse Clara Barton, RN"
                ),
                PatientEntity(
                    serial_no = 3,
                    ip_no = "IP-2026-8903",
                    name = "David Chen",
                    age = 68,
                    sex = "Male",
                    admission_datetime = "2026-08-12 07:45",
                    patient_received_time = "07:48",
                    broad_speciality_category = "Neurology / Stroke",
                    diagnosis = "Acute Ischemic Stroke",
                    age_interval = "Above 60",
                    taei_category = "TAEI Pillar",
                    medicolegal_category = "Non-MLC",
                    transferred_out = "Yes - Intensive Care Unit (ICU)",
                    transferred_out_time = "08:40",
                    emergency_response_time = "Immediate (<1 min)",
                    entered_by = nurse2Id,
                    entered_by_name = "Nurse Florence Hayes, BSN"
                ),
                PatientEntity(
                    serial_no = 4,
                    ip_no = "IP-2026-8904",
                    name = "Emily Jenkins",
                    age = 8,
                    sex = "Female",
                    admission_datetime = "2026-08-12 14:20",
                    patient_received_time = "14:22",
                    broad_speciality_category = "Pediatric Emergency",
                    diagnosis = "Severe Respiratory Distress (Asthma/COPD)",
                    age_interval = "Below 12",
                    taei_category = "TAEI Non Pillar",
                    medicolegal_category = "Non-MLC",
                    transferred_out = "No",
                    transferred_out_time = "",
                    emergency_response_time = "2 mins",
                    entered_by = nurse1Id,
                    entered_by_name = "Nurse Clara Barton, RN"
                ),
                PatientEntity(
                    serial_no = 5,
                    ip_no = "IP-2026-8905",
                    name = "James Wilson",
                    age = 42,
                    sex = "Male",
                    admission_datetime = "2026-08-13 11:10",
                    patient_received_time = "11:14",
                    broad_speciality_category = "Trauma / Surgery",
                    diagnosis = "Acute Appendicitis with Perforation",
                    age_interval = "12-60",
                    taei_category = "TAEI Non Pillar",
                    medicolegal_category = "Non-MLC",
                    transferred_out = "Yes - Emergency OT",
                    transferred_out_time = "12:15",
                    emergency_response_time = "5 mins",
                    entered_by = nurse2Id,
                    entered_by_name = "Nurse Florence Hayes, BSN"
                ),
                PatientEntity(
                    serial_no = 6,
                    ip_no = "IP-2026-8906",
                    name = "Fatima Al-Hassan",
                    age = 71,
                    sex = "Female",
                    admission_datetime = "2026-08-14 16:30",
                    patient_received_time = "16:35",
                    broad_speciality_category = "General Medicine",
                    diagnosis = "Severe Sepsis secondary to UTI",
                    age_interval = "Above 60",
                    taei_category = "TAEI Non Pillar",
                    medicolegal_category = "Non-MLC",
                    transferred_out = "Yes - Intensive Care Unit (ICU)",
                    transferred_out_time = "17:50",
                    emergency_response_time = "4 mins",
                    entered_by = nurse1Id,
                    entered_by_name = "Nurse Clara Barton, RN"
                )
            )

            samplePatients.forEach { patientDao.insertPatient(it) }

            // 5. Seed Audit Logs
            auditLogDao.insertAuditLog(
                AuditLogEntity(
                    user_id = doctorId,
                    user_name = "Dr. Sarah Mitchell, MD",
                    user_role = "DOCTOR",
                    action = "SYSTEM_INITIALIZED",
                    details = "Hospital PDMS core system provisioned with HIPAA-compliant database schema."
                )
            )
            auditLogDao.insertAuditLog(
                AuditLogEntity(
                    user_id = nurse1Id,
                    user_name = "Nurse Clara Barton, RN",
                    user_role = "NURSE",
                    action = "PATIENT_CREATED",
                    record_id = 1,
                    details = "Emergency admission recorded for IP-2026-8901 (Robert Anderson)."
                )
            )
        }
    }
}

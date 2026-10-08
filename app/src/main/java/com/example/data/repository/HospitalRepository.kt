package com.example.data.repository

import com.example.data.database.HospitalDatabase
import com.example.data.model.AuditLogEntity
import com.example.data.model.HospitalSettingEntity
import com.example.data.model.PatientEntity
import com.example.data.model.UserEntity
import com.example.data.security.SecurityUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class HospitalRepository(private val database: HospitalDatabase) {

    private val userDao = database.userDao()
    private val patientDao = database.patientDao()
    private val auditLogDao = database.auditLogDao()
    private val settingDao = database.settingDao()

    // --- Authentication & User Operations ---

    suspend fun authenticate(email: String, passwordPlain: String): Result<UserEntity> = withContext(Dispatchers.IO) {
        val user = userDao.getUserByEmail(email.trim())
            ?: return@withContext Result.failure(Exception("Invalid email or password."))

        if (user.status == "INACTIVE") {
            return@withContext Result.failure(Exception("Account deactivated by administrator. Please contact your supervisor."))
        }

        if (!SecurityUtils.verifyPassword(passwordPlain, user.password_hash)) {
            return@withContext Result.failure(Exception("Invalid email or password."))
        }

        // Record Audit Log
        auditLogDao.insertAuditLog(
            AuditLogEntity(
                user_id = user.id,
                user_name = user.full_name,
                user_role = user.role,
                action = if (user.role == "DOCTOR") "DOCTOR_LOGIN" else "NURSE_LOGIN",
                details = "User logged in successfully from clinical terminal."
            )
        )

        Result.success(user)
    }

    suspend fun registerNurse(
        fullName: String,
        employeeId: String,
        email: String,
        mobile: String,
        department: String,
        passwordPlain: String
    ): Result<UserEntity> = withContext(Dispatchers.IO) {
        val trimmedEmail = email.trim()
        val trimmedEmpId = employeeId.trim()

        if (userDao.getUserByEmail(trimmedEmail) != null) {
            return@withContext Result.failure(Exception("An account with this email already exists."))
        }

        if (userDao.getUserByEmployeeId(trimmedEmpId) != null) {
            return@withContext Result.failure(Exception("An account with this Employee ID already exists."))
        }

        val newUser = UserEntity(
            full_name = fullName.trim(),
            email = trimmedEmail,
            employee_id = trimmedEmpId,
            mobile = mobile.trim(),
            department = department.trim(),
            role = "NURSE", // Strictly NURSE role only
            password_hash = SecurityUtils.hashPassword(passwordPlain),
            status = "ACTIVE"
        )

        val id = userDao.insertUser(newUser)
        val created = newUser.copy(id = id)

        auditLogDao.insertAuditLog(
            AuditLogEntity(
                user_id = id,
                user_name = created.full_name,
                user_role = "NURSE",
                action = "NURSE_REGISTERED",
                details = "New nurse account registered with Employee ID ${created.employee_id}."
            )
        )

        Result.success(created)
    }

    suspend fun updateUserProfile(user: UserEntity): Result<Unit> = withContext(Dispatchers.IO) {
        userDao.updateUser(user.copy(updated_at = System.currentTimeMillis()))
        auditLogDao.insertAuditLog(
            AuditLogEntity(
                user_id = user.id,
                user_name = user.full_name,
                user_role = user.role,
                action = "PROFILE_UPDATED",
                details = "User profile details updated."
            )
        )
        Result.success(Unit)
    }

    fun getAllUsers(): Flow<List<UserEntity>> = userDao.getAllUsers()

    fun getNurses(): Flow<List<UserEntity>> = userDao.getUsersByRole("NURSE")

    suspend fun updateUserStatus(adminUser: UserEntity, targetUserId: Long, newStatus: String): Result<Unit> = withContext(Dispatchers.IO) {
        if (adminUser.role != "DOCTOR") {
            return@withContext Result.failure(SecurityException("Unauthorized: Only Doctor/Admin can manage user status."))
        }
        val targetUser = userDao.getUserById(targetUserId)
            ?: return@withContext Result.failure(Exception("User not found."))

        userDao.updateUserStatus(targetUserId, newStatus, System.currentTimeMillis())

        auditLogDao.insertAuditLog(
            AuditLogEntity(
                user_id = adminUser.id,
                user_name = adminUser.full_name,
                user_role = adminUser.role,
                action = "USER_STATUS_CHANGED",
                record_id = targetUserId,
                details = "Changed status of ${targetUser.full_name} (${targetUser.employee_id}) to $newStatus."
            )
        )
        Result.success(Unit)
    }

    // --- Patient Operations ---

    fun getAllActivePatients(): Flow<List<PatientEntity>> = patientDao.getAllActivePatients()

    fun getAllPatientsIncludingDeleted(): Flow<List<PatientEntity>> = patientDao.getAllPatientsIncludingDeleted()

    fun getPatientsByNurse(nurseId: Long): Flow<List<PatientEntity>> = patientDao.getPatientsByNurse(nurseId)

    suspend fun getPatientById(id: Long): PatientEntity? = withContext(Dispatchers.IO) {
        patientDao.getPatientById(id)
    }

    suspend fun checkIpExists(ipNo: String): Boolean = withContext(Dispatchers.IO) {
        patientDao.getActivePatientByIpNo(ipNo.trim()) != null
    }

    suspend fun saveNewPatient(
        patient: PatientEntity,
        currentUser: UserEntity
    ): Result<PatientEntity> = withContext(Dispatchers.IO) {
        // Auto-generate S.No if not specified or 0
        val nextSerial = (patientDao.getMaxSerialNo() ?: 0L) + 1L
        val toInsert = patient.copy(
            id = 0,
            serial_no = if (patient.serial_no > 0) patient.serial_no else nextSerial,
            entered_by = currentUser.id,
            entered_by_name = currentUser.full_name,
            created_at = System.currentTimeMillis(),
            updated_at = System.currentTimeMillis(),
            status = "ACTIVE"
        )

        val insertedId = patientDao.insertPatient(toInsert)
        val created = toInsert.copy(id = insertedId)

        auditLogDao.insertAuditLog(
            AuditLogEntity(
                user_id = currentUser.id,
                user_name = currentUser.full_name,
                user_role = currentUser.role,
                action = "PATIENT_CREATED",
                record_id = insertedId,
                details = "Admitted patient ${created.name} (IP: ${created.ip_no}, S.No: ${created.serial_no})."
            )
        )

        Result.success(created)
    }

    suspend fun updatePatient(
        patient: PatientEntity,
        currentUser: UserEntity
    ): Result<Unit> = withContext(Dispatchers.IO) {
        // Only DOCTOR can edit any record, NURSE can only edit their own if permitted
        if (currentUser.role != "DOCTOR" && patient.entered_by != currentUser.id) {
            return@withContext Result.failure(SecurityException("Permission denied: You can only edit records you entered."))
        }

        val updated = patient.copy(
            updated_at = System.currentTimeMillis(),
            updated_by = currentUser.id,
            updated_by_name = currentUser.full_name
        )

        patientDao.updatePatient(updated)

        auditLogDao.insertAuditLog(
            AuditLogEntity(
                user_id = currentUser.id,
                user_name = currentUser.full_name,
                user_role = currentUser.role,
                action = "PATIENT_EDITED",
                record_id = updated.id,
                details = "Updated record for patient ${updated.name} (IP: ${updated.ip_no})."
            )
        )

        Result.success(Unit)
    }

    suspend fun softDeletePatient(
        patientId: Long,
        currentUser: UserEntity
    ): Result<Unit> = withContext(Dispatchers.IO) {
        if (currentUser.role != "DOCTOR") {
            return@withContext Result.failure(SecurityException("Unauthorized: Only Doctor/Admin can delete patient records."))
        }

        val existing = patientDao.getPatientById(patientId)
            ?: return@withContext Result.failure(Exception("Patient record not found."))

        patientDao.softDeletePatient(
            id = patientId,
            deletedAt = System.currentTimeMillis(),
            deletedBy = currentUser.id,
            deletedByName = currentUser.full_name
        )

        auditLogDao.insertAuditLog(
            AuditLogEntity(
                user_id = currentUser.id,
                user_name = currentUser.full_name,
                user_role = currentUser.role,
                action = "PATIENT_DELETED",
                record_id = patientId,
                details = "Soft deleted record for ${existing.name} (IP: ${existing.ip_no})."
            )
        )

        Result.success(Unit)
    }

    suspend fun recordExcelExportAudit(
        currentUser: UserEntity,
        fileName: String,
        recordCount: Int
    ) = withContext(Dispatchers.IO) {
        auditLogDao.insertAuditLog(
            AuditLogEntity(
                user_id = currentUser.id,
                user_name = currentUser.full_name,
                user_role = currentUser.role,
                action = "EXCEL_EXPORT",
                details = "Exported $recordCount patient records to Excel spreadsheet: $fileName"
            )
        )
    }

    // --- Audit Logs ---

    fun getAllAuditLogs(): Flow<List<AuditLogEntity>> = auditLogDao.getAllAuditLogs()

    // --- Settings ---

    fun getAllSettings(): Flow<List<HospitalSettingEntity>> = settingDao.getAllSettings()

    suspend fun saveSetting(
        currentUser: UserEntity,
        setting: HospitalSettingEntity
    ): Result<Unit> = withContext(Dispatchers.IO) {
        if (currentUser.role != "DOCTOR") {
            return@withContext Result.failure(SecurityException("Unauthorized: Only Doctor/Admin can change clinical settings."))
        }
        settingDao.insertOrUpdateSetting(setting)
        auditLogDao.insertAuditLog(
            AuditLogEntity(
                user_id = currentUser.id,
                user_name = currentUser.full_name,
                user_role = currentUser.role,
                action = "SETTINGS_UPDATED",
                details = "Configured ${setting.category} option: ${setting.value}"
            )
        )
        Result.success(Unit)
    }

    suspend fun deleteSetting(
        currentUser: UserEntity,
        key: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        if (currentUser.role != "DOCTOR") {
            return@withContext Result.failure(SecurityException("Unauthorized: Only Doctor/Admin can delete clinical settings."))
        }
        settingDao.deleteSetting(key)
        Result.success(Unit)
    }
}

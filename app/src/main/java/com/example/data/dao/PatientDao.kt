package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.PatientEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PatientDao {
    @Query("SELECT * FROM patients WHERE status = 'ACTIVE' ORDER BY admission_datetime DESC, serial_no DESC")
    fun getAllActivePatients(): Flow<List<PatientEntity>>

    @Query("SELECT * FROM patients ORDER BY admission_datetime DESC, serial_no DESC")
    fun getAllPatientsIncludingDeleted(): Flow<List<PatientEntity>>

    @Query("SELECT * FROM patients WHERE entered_by = :nurseId AND status = 'ACTIVE' ORDER BY admission_datetime DESC, serial_no DESC")
    fun getPatientsByNurse(nurseId: Long): Flow<List<PatientEntity>>

    @Query("SELECT * FROM patients WHERE id = :id LIMIT 1")
    suspend fun getPatientById(id: Long): PatientEntity?

    @Query("SELECT * FROM patients WHERE ip_no = :ipNo AND status = 'ACTIVE' LIMIT 1")
    suspend fun getActivePatientByIpNo(ipNo: String): PatientEntity?

    @Query("SELECT MAX(serial_no) FROM patients")
    suspend fun getMaxSerialNo(): Long?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPatient(patient: PatientEntity): Long

    @Update
    suspend fun updatePatient(patient: PatientEntity)

    @Query("UPDATE patients SET status = 'DELETED', deleted_at = :deletedAt, deleted_by = :deletedBy, deleted_by_name = :deletedByName, updated_at = :deletedAt WHERE id = :id")
    suspend fun softDeletePatient(id: Long, deletedAt: Long, deletedBy: Long, deletedByName: String)

    @Query("SELECT COUNT(*) FROM patients WHERE status = 'ACTIVE'")
    suspend fun getActivePatientCount(): Int

    @Query("SELECT COUNT(*) FROM patients WHERE entered_by = :nurseId AND status = 'ACTIVE'")
    suspend fun getNursePatientCount(nurseId: Long): Int

    @Query("SELECT COUNT(*) FROM patients WHERE entered_by = :nurseId AND status = 'ACTIVE' AND admission_datetime LIKE :datePrefix || '%'")
    suspend fun getNurseTodayPatientCount(nurseId: Long, datePrefix: String): Int
}

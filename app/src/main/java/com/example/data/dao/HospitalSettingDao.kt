package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.HospitalSettingEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HospitalSettingDao {
    @Query("SELECT * FROM hospital_settings ORDER BY category ASC, value ASC")
    fun getAllSettings(): Flow<List<HospitalSettingEntity>>

    @Query("SELECT * FROM hospital_settings WHERE category = :category ORDER BY value ASC")
    fun getSettingsByCategory(category: String): Flow<List<HospitalSettingEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateSetting(setting: HospitalSettingEntity)

    @Query("DELETE FROM hospital_settings WHERE `key` = :key")
    suspend fun deleteSetting(key: String)
}

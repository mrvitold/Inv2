package com.example.inv2.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.inv2.model.ScanEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ScanDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(scan: ScanEntity)

    @Query("SELECT * FROM scans ORDER BY id DESC")
    fun getAll(): Flow<List<ScanEntity>>

    @Query("DELETE FROM scans WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Int>)

    @Query("UPDATE scans SET uploadStatus = :status WHERE id = :id")
    suspend fun updateUploadStatus(id: Int, status: String)

    @Query("SELECT * FROM scans WHERE uploadStatus = 'pending'")
    suspend fun getPendingScans(): List<ScanEntity>

    @Query("UPDATE scans SET uploadStatus = 'pending' WHERE uploadStatus = 'uploading'")
    suspend fun resetUploadingToPending()

    @Query("UPDATE scans SET uploadStatus = 'pending' WHERE uploadStatus = 'failed'")
    suspend fun resetFailedToPending()
} 
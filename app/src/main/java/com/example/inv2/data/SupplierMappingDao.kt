package com.example.inv2.data

import androidx.room.*
import com.example.inv2.model.SupplierMappingEntity

@Dao
interface SupplierMappingDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(mapping: SupplierMappingEntity)

    @Query("SELECT * FROM supplier_mappings WHERE scannedSupplierName = :scannedName LIMIT 1")
    suspend fun getMappingByScannedName(scannedName: String): SupplierMappingEntity?

    @Query("SELECT * FROM supplier_mappings")
    suspend fun getAllMappings(): List<SupplierMappingEntity>

    @Delete
    suspend fun delete(mapping: SupplierMappingEntity)
} 
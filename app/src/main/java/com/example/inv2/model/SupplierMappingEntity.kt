package com.example.inv2.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "supplier_mappings")
data class SupplierMappingEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val scannedSupplierName: String,
    val canonicalSupplierName: String,
    val companyRegNumber: String?,
    val companyVatNumber: String?
) 
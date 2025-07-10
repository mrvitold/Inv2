package com.example.inv2.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.inv2.model.InvoiceEntity

@Dao
interface InvoiceDao {
    // Insert a new invoice
    @Insert
    suspend fun insert(invoice: InvoiceEntity)

    // Get all invoices
    @Query("SELECT * FROM invoices")
    suspend fun getAll(): List<InvoiceEntity>
} 
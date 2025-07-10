package com.example.inv2.model

import androidx.room.Entity
import androidx.room.PrimaryKey

// Room entity for an invoice
@Entity(tableName = "invoices")
data class InvoiceEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val date: String,
    val invoiceNumber: String,
    val supplierName: String,
    val amountNoVat: String,
    val vatAmount: String,
    val companyRegNumber: String,
    val companyVatNumber: String,
    val vatPercent: String
) 
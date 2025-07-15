package com.example.inv2.repository

import com.example.inv2.data.InvoiceDao
import com.example.inv2.data.SupplierMappingDao
import com.example.inv2.model.InvoiceEntity
import com.example.inv2.model.SupplierMappingEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class InvoiceRepository(
    private val invoiceDao: InvoiceDao,
    private val supplierMappingDao: SupplierMappingDao
) {
    suspend fun getAllInvoices(): List<InvoiceEntity> = withContext(Dispatchers.IO) {
        invoiceDao.getAll()
    }

    suspend fun getMonthlySummary(): List<MonthlySummary> = withContext(Dispatchers.IO) {
        val invoices = invoiceDao.getAll()
        val mappings = supplierMappingDao.getAllMappings()
        val mappingMap = mappings.associateBy { it.scannedSupplierName }
        // Group by month (YYYY-MM) if possible, else 'Unknown'
        val grouped = invoices.groupBy {
            val date = it.date
            if (date.length >= 7) date.substring(0, 7) else "Unknown"
        }
        grouped.map { (month, list) ->
            val vatSum = list.sumOf { it.vatAmount.toDoubleOrNull() ?: 0.0 }
            val totalInvoices = list.size
            val successCount = list.count { invoice ->
                invoice.date.isNotBlank() && invoice.invoiceNumber.isNotBlank() && invoice.supplierName.isNotBlank() && invoice.amountNoVat.isNotBlank() && invoice.vatAmount.isNotBlank() && invoice.companyRegNumber.isNotBlank() && invoice.companyVatNumber.isNotBlank() && invoice.vatPercent.isNotBlank()
            }
            val needsMappingCount = list.count { invoice ->
                mappingMap[invoice.supplierName] == null
            }
            MonthlySummary(
                month = month,
                vatSum = vatSum,
                totalInvoices = totalInvoices,
                successCount = successCount,
                needsMappingCount = needsMappingCount
            )
        }.sortedByDescending { it.month }
    }

    suspend fun getInvoicesForMonth(month: String): List<InvoiceEntity> = withContext(Dispatchers.IO) {
        invoiceDao.getAll().filter { it.date.startsWith(month) }
    }

    suspend fun getInvoicesForSupplier(supplier: String): List<InvoiceEntity> = withContext(Dispatchers.IO) {
        invoiceDao.getAll().filter { it.supplierName == supplier }
    }

    suspend fun insertSupplierMapping(mapping: SupplierMappingEntity) = withContext(Dispatchers.IO) {
        supplierMappingDao.insert(mapping)
    }

    suspend fun getSupplierMapping(scannedName: String): SupplierMappingEntity? = withContext(Dispatchers.IO) {
        supplierMappingDao.getMappingByScannedName(scannedName)
    }
}

data class MonthlySummary(
    val month: String,
    val vatSum: Double,
    val totalInvoices: Int,
    val successCount: Int,
    val needsMappingCount: Int
) 
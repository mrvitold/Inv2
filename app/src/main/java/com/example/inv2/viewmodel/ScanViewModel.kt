package com.example.inv2.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.inv2.data.InvoiceDatabase
import com.example.inv2.model.ScanEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import android.content.Context
import kotlinx.coroutines.delay
import com.example.inv2.repository.InvoiceRepository
import com.example.inv2.data.InvoiceDao
import com.example.inv2.data.SupplierMappingDao
import com.example.inv2.model.InvoiceEntity
import com.example.inv2.model.SupplierMappingEntity
import com.example.inv2.repository.MonthlySummary

class ScanViewModel(app: Application) : AndroidViewModel(app) {
    private val db = InvoiceDatabase.getInstance(app)
    private val scanDao = db.scanDao()
    private val invoiceDao: InvoiceDao = db.invoiceDao()
    private val supplierMappingDao: SupplierMappingDao = db.supplierMappingDao()
    private val repository = InvoiceRepository(invoiceDao, supplierMappingDao)

    private val _isLoading = MutableStateFlow(true)
    val isLoading = _isLoading.asStateFlow()

    val scans: StateFlow<List<ScanEntity>> = scanDao.getAll().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _monthlySummary = MutableStateFlow<List<MonthlySummary>>(emptyList())
    val monthlySummary = _monthlySummary.asStateFlow()

    private val _filteredInvoices = MutableStateFlow<List<InvoiceEntity>>(emptyList())
    val filteredInvoices = _filteredInvoices.asStateFlow()

    private val _supplierMapping = MutableStateFlow<SupplierMappingEntity?>(null)
    val supplierMapping = _supplierMapping.asStateFlow()

    private val _allInvoices = MutableStateFlow<List<InvoiceEntity>>(emptyList())
    val allInvoices = _allInvoices.asStateFlow()

    init {
        viewModelScope.launch {
            _isLoading.value = true
            scanDao.getAll().collect {
                _isLoading.value = false
            }
            refreshMonthlySummary()
        }
    }

    fun refreshMonthlySummary() {
        viewModelScope.launch {
            _monthlySummary.value = repository.getMonthlySummary()
        }
    }

    fun filterInvoicesByMonth(month: String) {
        viewModelScope.launch {
            _filteredInvoices.value = repository.getInvoicesForMonth(month)
        }
    }

    fun filterInvoicesBySupplier(supplier: String) {
        viewModelScope.launch {
            _filteredInvoices.value = repository.getInvoicesForSupplier(supplier)
        }
    }

    fun addSupplierMapping(mapping: SupplierMappingEntity) {
        viewModelScope.launch {
            repository.insertSupplierMapping(mapping)
            refreshMonthlySummary()
        }
    }

    fun getSupplierMapping(scannedName: String) {
        viewModelScope.launch {
            _supplierMapping.value = repository.getSupplierMapping(scannedName)
        }
    }

    fun addScan(scan: ScanEntity) {
        viewModelScope.launch {
            scanDao.insert(scan)
        }
    }

    fun deleteScansByIds(ids: List<Int>) {
        viewModelScope.launch {
            scanDao.deleteByIds(ids)
        }
    }

    fun updateUploadStatus(id: Int, status: String) {
        viewModelScope.launch {
            scanDao.updateUploadStatus(id, status)
        }
    }

    fun uploadPendingScans(context: Context) {
        viewModelScope.launch {
            scanDao.resetUploadingToPending()
            scanDao.resetFailedToPending()
            val pendingScans = scanDao.getPendingScans()
            for (scan in pendingScans) {
                updateUploadStatus(scan.id, "uploading")
                try {
                    // TODO: Replace with your real upload logic
                    delay(1000) // Simulate upload
                    updateUploadStatus(scan.id, "uploaded")
                } catch (e: Exception) {
                    updateUploadStatus(scan.id, "failed")
                }
            }
        }
    }

    fun refreshAllInvoices() {
        viewModelScope.launch {
            _allInvoices.value = repository.getAllInvoices()
        }
    }

    fun saveInvoice(invoice: InvoiceEntity) {
        viewModelScope.launch {
            invoiceDao.insert(invoice)
            refreshAllInvoices()
            refreshMonthlySummary()
        }
    }
} 
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

class ScanViewModel(app: Application) : AndroidViewModel(app) {
    private val scanDao = InvoiceDatabase.getInstance(app).scanDao()

    private val _isLoading = MutableStateFlow(true)
    val isLoading = _isLoading.asStateFlow()

    val scans: StateFlow<List<ScanEntity>> = scanDao.getAll().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    init {
        viewModelScope.launch {
            _isLoading.value = true
            scanDao.getAll().collect {
                _isLoading.value = false
            }
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
} 
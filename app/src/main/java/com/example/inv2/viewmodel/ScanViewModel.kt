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
} 
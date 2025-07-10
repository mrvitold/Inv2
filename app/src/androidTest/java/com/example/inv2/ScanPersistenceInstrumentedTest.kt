package com.example.inv2

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.inv2.data.InvoiceDatabase
import com.example.inv2.data.ScanDao
import com.example.inv2.model.ScanEntity
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ScanPersistenceInstrumentedTest {
    private lateinit var db: InvoiceDatabase
    private lateinit var dao: ScanDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, InvoiceDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.scanDao()
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun insertAndGetScan() = runBlocking {
        val scan = ScanEntity(uri = "test_uri", uploadDate = "2024-01-01", hash = "abc123")
        dao.insert(scan)
        val allScans = dao.getAll().first()
        assertEquals(1, allScans.size)
        assertEquals("test_uri", allScans[0].uri)
        assertEquals("2024-01-01", allScans[0].uploadDate)
        assertEquals("abc123", allScans[0].hash)
    }
} 
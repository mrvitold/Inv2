package com.example.inv2.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.inv2.model.InvoiceEntity
import com.example.inv2.model.ScanEntity

@Database(entities = [InvoiceEntity::class, ScanEntity::class], version = 1)
abstract class InvoiceDatabase : RoomDatabase() {
    abstract fun invoiceDao(): InvoiceDao
    abstract fun scanDao(): ScanDao

    companion object {
        @Volatile
        private var INSTANCE: InvoiceDatabase? = null

        fun getInstance(context: Context): InvoiceDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    InvoiceDatabase::class.java,
                    "invoice_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
} 
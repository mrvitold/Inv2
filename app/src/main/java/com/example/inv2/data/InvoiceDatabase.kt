package com.example.inv2.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.inv2.model.InvoiceEntity
import com.example.inv2.model.ScanEntity
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.inv2.model.SupplierMappingEntity

@Database(entities = [InvoiceEntity::class, ScanEntity::class, SupplierMappingEntity::class], version = 3)
abstract class InvoiceDatabase : RoomDatabase() {
    abstract fun invoiceDao(): InvoiceDao
    abstract fun scanDao(): ScanDao
    abstract fun supplierMappingDao(): SupplierMappingDao

    companion object {
        @Volatile
        private var INSTANCE: InvoiceDatabase? = null

        fun getInstance(context: Context): InvoiceDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    InvoiceDatabase::class.java,
                    "invoice_database"
                )
                // Add migration from 1 to 2
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .build()
                INSTANCE = instance
                instance
            }
        }

        // Migration from version 1 to 2: add uploadStatus column to scans
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE scans ADD COLUMN uploadStatus TEXT NOT NULL DEFAULT 'pending'")
            }
        }
        // Migration from version 2 to 3: create supplier_mappings table
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS supplier_mappings (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        scannedSupplierName TEXT NOT NULL,
                        canonicalSupplierName TEXT NOT NULL,
                        companyRegNumber TEXT,
                        companyVatNumber TEXT
                    )
                """)
            }
        }
    }
} 
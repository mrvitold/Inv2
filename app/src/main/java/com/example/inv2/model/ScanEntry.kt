package com.example.inv2.model

import androidx.room.Entity
import androidx.room.PrimaryKey

// Represents a scan entry for the gallery
// uri: image URI as string
// uploadDate: date/time of upload
// hash: hash of image content for duplicate detection

@Entity(tableName = "scans")
data class ScanEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val uri: String,
    val uploadDate: String,
    val hash: String,
    val uploadStatus: String = "pending"
) 
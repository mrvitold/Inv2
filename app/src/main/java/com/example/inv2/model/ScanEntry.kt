package com.example.inv2.model

// Represents a scan entry for the gallery
// uri: image URI as string
// uploadDate: date/time of upload
// hash: hash of image content for duplicate detection

data class ScanEntry(
    val uri: String,
    val uploadDate: String,
    val hash: String
) 
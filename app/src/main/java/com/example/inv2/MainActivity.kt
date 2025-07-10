package com.example.inv2

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.inv2.ui.theme.Inv2Theme
import androidx.compose.ui.tooling.preview.Preview
import android.net.Uri
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import java.io.InputStream
import android.graphics.BitmapFactory
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import androidx.compose.runtime.LaunchedEffect
import com.google.android.gms.tasks.Task
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import androidx.exifinterface.media.ExifInterface
import android.graphics.Matrix
import android.graphics.Bitmap
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import com.example.inv2.data.InvoiceDatabase
import com.example.inv2.model.InvoiceEntity
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.example.inv2.model.ScanEntry
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Inv2Theme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen()
                }
            }
        }
    }
}

// Field mapping: maps possible field names (English, Lithuanian, variations) to standard keys
val FIELD_KEYWORDS = mapOf(
    "date" to listOf("date", "data", "išrašymo data", "data issued", "invoice date"),
    "invoice_number" to listOf("invoice number", "no.", "inv. no.", "nr", "sąskaitos nr", "sąskaita nr", "sąskaita faktūra nr", "faktūros nr"),
    "supplier_name" to listOf("supplier", "supplier name", "pardavėjas", "tiekėjas", "company name", "įmonės pavadinimas"),
    "amount_no_vat" to listOf("amount without vat", "be pvm", "suma be pvm", "neto suma", "net amount", "be mokesčių"),
    "vat_amount" to listOf("vat amount", "pvm suma", "pvm", "value-added tax", "tax amount"),
    "company_reg_number" to listOf("company register number", "įmonės kodas", "company code", "reg. nr", "į.k."),
    "company_vat_number" to listOf("company vat number", "pvm kodas", "vat code", "vat no", "pvm mokėtojo kodas"),
    "vat_percent" to listOf("vat, %", "vat %", "pvm %", "pvm tarifas", "tax rate")
)

// Extract fields from OCR text using the mapping
fun extractInvoiceFields(ocrText: String): Pair<Map<String, String>, List<String>> {
    val result = mutableMapOf<String, String>()
    val missing = mutableListOf<String>()
    val lines = ocrText.lines().map { it.trim() }
    val used = mutableSetOf<Int>()
    for ((key, keywords) in FIELD_KEYWORDS) {
        var found = false
        for ((i, line) in lines.withIndex()) {
            for (kw in keywords) {
                val regex = Regex("""^$kw[:\-\s]*(.+)""", RegexOption.IGNORE_CASE)
                val match = regex.find(line)
                if (match != null) {
                    result[key] = match.groupValues[1].trim()
                    used.add(i)
                    found = true
                    break
                }
            }
            if (found) break
        }
        if (!found) missing.add(key)
    }
    return result to missing
}

// Helper to load and rotate bitmap according to EXIF orientation
fun loadBitmapWithCorrectOrientation(context: android.content.Context, uri: Uri): android.graphics.Bitmap? {
    val inputStream = context.contentResolver.openInputStream(uri) ?: return null
    val bitmap = inputStream.use { BitmapFactory.decodeStream(it) } ?: return null
    // Re-open stream for EXIF (must be a fresh stream)
    val exifStream = context.contentResolver.openInputStream(uri) ?: return bitmap
    val exif = ExifInterface(exifStream)
    val orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
    val matrix = Matrix()
    when (orientation) {
        ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
        ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
        ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
        ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.preScale(-1f, 1f)
        ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.preScale(1f, -1f)
        else -> {}
    }
    return if (orientation != ExifInterface.ORIENTATION_NORMAL) {
        Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    } else {
        bitmap
    }
}

@Composable
fun MainScreen() {
    val context = LocalContext.current
    val scanList = remember { mutableStateListOf<ScanEntry>() }
    val showGallery = remember { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris: List<Uri> ->
        // For each selected image, add to scanList
        uris.forEach { uri ->
            val bitmap = loadBitmapWithCorrectOrientation(context, uri)
            if (bitmap != null) {
                val hash = bitmapHash(bitmap)
                val date = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
                scanList.add(ScanEntry(uri.toString(), date, hash))
            }
        }
    }

    if (showGallery.value) {
        ScansGalleryScreen(scanList = scanList, onBack = { showGallery.value = false })
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(onClick = {
                launcher.launch("image/*")
            }) {
                Text("Add Invoice Photos")
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = {
                showGallery.value = true
            }) {
                Text("Scans")
            }
        }
    }
}

// Helper to compute a hash of a bitmap (for duplicate detection)
fun bitmapHash(bitmap: Bitmap): String {
    val buffer = java.nio.ByteBuffer.allocate(bitmap.byteCount)
    bitmap.copyPixelsToBuffer(buffer)
    val bytes = buffer.array()
    val md = MessageDigest.getInstance("SHA-256")
    val digest = md.digest(bytes)
    return digest.joinToString("") { "%02x".format(it) }
}

// Updated PhotoPickerScreen to accept onBack and onScanAdded
@Composable
fun PhotoPickerScreen(onBack: () -> Unit, onScanAdded: (ScanEntry) -> Unit) {
    val context = LocalContext.current
    val imageUris = remember { mutableStateOf<List<Uri>>(emptyList()) }
    val ocrResults = remember { mutableStateOf<Map<Uri, String>>(emptyMap()) }
    val saveStatus = remember { mutableStateOf<Map<Uri, String>>(emptyMap()) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris: List<Uri> ->
        imageUris.value = uris
    }
    val db = remember { InvoiceDatabase.getInstance(context) }

    // OCR: Run text recognition when imageUris changes
    LaunchedEffect(imageUris.value) {
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        val results = mutableMapOf<Uri, String>()
        val status = mutableMapOf<Uri, String>()
        for (uri in imageUris.value) {
            val bitmap = loadBitmapWithCorrectOrientation(context, uri)
            if (bitmap != null) {
                val image = InputImage.fromBitmap(bitmap, 0)
                val result = recognizer.process(image).await()
                results[uri] = result.text
                val (fields, missing) = extractInvoiceFields(result.text)
                if (missing.isEmpty()) {
                    val entity = InvoiceEntity(
                        date = fields["date"] ?: "",
                        invoiceNumber = fields["invoice_number"] ?: "",
                        supplierName = fields["supplier_name"] ?: "",
                        amountNoVat = fields["amount_no_vat"] ?: "",
                        vatAmount = fields["vat_amount"] ?: "",
                        companyRegNumber = fields["company_reg_number"] ?: "",
                        companyVatNumber = fields["company_vat_number"] ?: "",
                        vatPercent = fields["vat_percent"] ?: ""
                    )
                    kotlinx.coroutines.GlobalScope.launch {
                        db.invoiceDao().insert(entity)
                    }
                    status[uri] = "Saved!"
                } else {
                    status[uri] = "Missing fields, not saved"
                }
                // Add scan entry for gallery
                val hash = bitmapHash(bitmap)
                val date = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
                onScanAdded(ScanEntry(uri.toString(), date, hash))
            } else {
                results[uri] = "Could not load image."
                status[uri] = "Could not load image."
            }
        }
        ocrResults.value = results
        saveStatus.value = status
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row {
            Button(onClick = { launcher.launch("image/*") }) {
                Text("Add Invoice Photos")
            }
            Spacer(modifier = Modifier.width(16.dp))
            Button(onClick = onBack) {
                Text("Back")
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        if (imageUris.value.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                imageUris.value.forEach { uri ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        val bitmap = remember(uri) {
                            loadBitmapWithCorrectOrientation(context, uri)
                        }
                        bitmap?.let {
                            Image(
                                bitmap = it.asImageBitmap(),
                                contentDescription = "Selected Invoice Photo",
                                modifier = Modifier
                                    .size(160.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color.LightGray),
                                contentScale = ContentScale.Crop
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        val ocrText = ocrResults.value[uri]
                        if (ocrText == null) {
                            Text("Recognizing...", style = MaterialTheme.typography.bodySmall, modifier = Modifier.width(160.dp))
                        } else if (ocrText == "Could not load image.") {
                            Text("Could not load image.", color = Color.Red, modifier = Modifier.width(160.dp))
                        } else {
                            val (fields, missing) = extractInvoiceFields(ocrText)
                            fields.forEach { (key, value) ->
                                val label = when (key) {
                                    "date" -> "Date"
                                    "invoice_number" -> "Invoice Number"
                                    "supplier_name" -> "Supplier Name"
                                    "amount_no_vat" -> "Amount without VAT"
                                    "vat_amount" -> "VAT Amount"
                                    "company_reg_number" -> "Company Reg. Number"
                                    "company_vat_number" -> "Company VAT Number"
                                    "vat_percent" -> "VAT, %"
                                    else -> key
                                }
                                Text("$label: $value", style = MaterialTheme.typography.bodySmall, modifier = Modifier.width(160.dp))
                            }
                            if (missing.isNotEmpty()) {
                                Text(
                                    text = "Missing: " + missing.joinToString { key ->
                                        when (key) {
                                            "date" -> "Date"
                                            "invoice_number" -> "Invoice Number"
                                            "supplier_name" -> "Supplier Name"
                                            "amount_no_vat" -> "Amount without VAT"
                                            "vat_amount" -> "VAT Amount"
                                            "company_reg_number" -> "Company Reg. Number"
                                            "company_vat_number" -> "Company VAT Number"
                                            "vat_percent" -> "VAT, %"
                                            else -> key
                                        }
                                    },
                                    color = Color.Red,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.width(160.dp)
                                )
                            }
                        }
                        val status = saveStatus.value[uri]
                        if (status != null) {
                            Text(status, color = if (status == "Saved!") Color(0xFF388E3C) else Color.Red, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}

// Scans gallery screen with Back button at top left
@Composable
fun ScansGalleryScreen(scanList: List<ScanEntry>, onBack: () -> Unit) {
    val duplicates = scanList.groupBy { it.hash }.filter { it.value.size > 1 }.flatMap { it.value.map { entry -> entry.uri } }.toSet()
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 24.dp, start = 16.dp, end = 16.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top bar with Back button at left and title centered
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onBack,
                    modifier = Modifier.height(36.dp)
                ) {
                    Text("Back")
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    "Scans Gallery",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            if (scanList.isEmpty()) {
                Text("No scans yet.")
            } else {
                LazyColumn {
                    items(scanList) { entry ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                        ) {
                            val uri = Uri.parse(entry.uri)
                            val context = LocalContext.current
                            val bitmap = remember(uri) { loadBitmapWithCorrectOrientation(context, uri) }
                            bitmap?.let {
                                Image(
                                    bitmap = it.asImageBitmap(),
                                    contentDescription = "Scan thumbnail",
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color.LightGray),
                                    contentScale = ContentScale.Crop
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Uploaded: ${entry.uploadDate}", style = MaterialTheme.typography.bodySmall)
                                if (entry.uri in duplicates) {
                                    Text("duplicated", color = Color.Red, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// Helper to use ML Kit Task with coroutines
suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { cont ->
    addOnSuccessListener { result ->
        cont.resume(result)
    }
    addOnFailureListener { exception ->
        cont.resumeWithException(exception)
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    Inv2Theme {
        Greeting("Android")
    }
}
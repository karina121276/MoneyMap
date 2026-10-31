package com.thecorporatedeveloper.moneymap.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import coil.compose.AsyncImage
import com.thecorporatedeveloper.moneymap.core.model.CurrencyFormatter
import com.thecorporatedeveloper.moneymap.core.model.TransactionType
import com.thecorporatedeveloper.moneymap.data.entity.AccountEntity
import com.thecorporatedeveloper.moneymap.data.entity.CategoryEntity
import com.thecorporatedeveloper.moneymap.data.entity.TransactionEntity
import com.thecorporatedeveloper.moneymap.data.repository.MoneyMapRepository
import com.thecorporatedeveloper.moneymap.importer.*
import com.thecorporatedeveloper.moneymap.ui.components.getCategoryIcon
import com.thecorporatedeveloper.moneymap.ui.components.parseColorHex
import com.thecorporatedeveloper.moneymap.ui.theme.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.activity.result.PickVisualMediaRequest
import androidx.compose.foundation.BorderStroke
import com.thecorporatedeveloper.moneymap.receipt.ReceiptStorageManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartEntryMenuScreen(
    onBack: () -> Unit,
    onNavigateToManualAdd: () -> Unit,
    onImageSelectedForOcr: (Uri) -> Unit,
    onPdfSelectedForImport: (Uri) -> Unit
) {
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { onImageSelectedForOcr(it) }
    }

    val docPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { onPdfSelectedForImport(it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Smart Entry", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            item {
                Text(
                    text = "Add transactions in seconds",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }

            // Option 1: Add Manually
            item {
                SmartEntryOptionCard(
                    title = "Add Manually",
                    subtitle = "Enter income, expense or transfer",
                    icon = Icons.Default.Edit,
                    iconTint = MoneyMapBlue,
                    onClick = onNavigateToManualAdd
                )
            }

            // Option 2: From Screenshot
            item {
                SmartEntryOptionCard(
                    title = "From Screenshot",
                    subtitle = "Share UPI payment screenshot from any app",
                    icon = Icons.Default.Image,
                    iconTint = MoneyMapPurple,
                    onClick = { photoPickerLauncher.launch("image/*") }
                )
            }

            // Option 3: Scan Receipt
            item {
                SmartEntryOptionCard(
                    title = "Scan Receipt",
                    subtitle = "Take a photo or choose from gallery",
                    icon = Icons.Default.PhotoCamera,
                    iconTint = MoneyMapGreen,
                    onClick = { photoPickerLauncher.launch("image/*") }
                )
            }

            // Option 4: Import PDF Statement
            item {
                SmartEntryOptionCard(
                    title = "Import PDF Statement",
                    subtitle = "Import bank statement (PDF)",
                    icon = Icons.Default.PictureAsPdf,
                    iconTint = MoneyMapRed,
                    onClick = { docPickerLauncher.launch("application/pdf") }
                )
            }

            // Privacy Guarantee Badge
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFFEFF6FF)
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Security, contentDescription = null, tint = MoneyMapBlue, modifier = Modifier.size(32.dp))
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text("Your data stays on your device", fontWeight = FontWeight.Bold, color = MoneyMapNavy, fontSize = 14.sp)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                "We process screenshots, receipts and statements locally. No bank logins, no SMS access, no data uploaded.",
                                color = TextSecondary,
                                fontSize = 12.sp,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }

            // Tips Card
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Tips for best results", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("• Use clear screenshots or receipts with visible amounts", color = TextSecondary, fontSize = 12.sp)
                        Text("• Ensure the full transaction reference is visible", color = TextSecondary, fontSize = 12.sp)
                        Text("• Always review and confirm details before saving", color = TextSecondary, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun SmartEntryOptionCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .testTag("option_${title.lowercase().replace(" ", "_")}"),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        shape = RoundedCornerShape(18.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(iconTint.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = title, tint = iconTint, modifier = Modifier.size(24.dp))
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Spacer(modifier = Modifier.height(2.dp))
                Text(subtitle, color = TextSecondary, fontSize = 12.sp)
            }

            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.LightGray)
        }
    }
}

sealed class OcrUiState {
    object Idle : OcrUiState()
    data class Processing(val stepMessage: String) : OcrUiState()
    data class Ready(
        val extracted: ExtractedTransaction,
        val rawText: String,
        val duplicateCandidate: TransactionEntity? = null
    ) : OcrUiState()
    data class Error(val message: String, val canRetry: Boolean) : OcrUiState()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OcrReviewScreen(
    initialExtracted: ExtractedTransaction? = null,
    imageUri: Uri?,
    initialPendingFiles: List<File> = emptyList(),
    accounts: List<AccountEntity>,
    categories: List<CategoryEntity>,
    repository: MoneyMapRepository,
    onSave: (TransactionEntity) -> Unit = {},
    onSaveWithReceipts: ((TransactionEntity, List<File>) -> Unit)? = null,
    onNavigateToManualAdd: ((ExtractedTransaction?, List<File>) -> Unit)? = null,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var currentImageUri by remember(imageUri) { mutableStateOf(imageUri) }
    var uiState by remember { mutableStateOf<OcrUiState>(OcrUiState.Idle) }

    var pendingReceiptFiles by remember(initialPendingFiles, imageUri) {
        val initial = if (initialPendingFiles.isNotEmpty()) {
            initialPendingFiles
        } else if (imageUri != null && imageUri.scheme == "file") {
            val f = imageUri.path?.let { File(it) }
            if (f != null && f.exists()) listOf(f) else emptyList()
        } else {
            emptyList()
        }
        mutableStateOf<List<File>>(initial)
    }

    var showAddReceiptSheet by remember { mutableStateOf(false) }
    var cropFileToProcess by remember { mutableStateOf<File?>(null) }
    var tempCameraFile by remember { mutableStateOf<File?>(null) }
    var isDocumentScanMode by remember { mutableStateOf(false) }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && tempCameraFile != null && tempCameraFile!!.exists()) {
            if (isDocumentScanMode) {
                coroutineScope.launch {
                    val enhanced = ReceiptStorageManager.enhanceDocumentImage(tempCameraFile!!)
                    cropFileToProcess = enhanced ?: tempCameraFile
                }
            } else {
                cropFileToProcess = tempCameraFile
            }
        }
    }

    val pickMediaLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 10)
    ) { uris ->
        if (uris.isNotEmpty()) {
            coroutineScope.launch {
                val copiedFiles = mutableListOf<File>()
                for (uri in uris) {
                    val temp = ReceiptStorageManager.copyUriToTempFile(context, uri)
                    if (temp != null) copiedFiles.add(temp)
                }
                if (copiedFiles.size == 1) {
                    cropFileToProcess = copiedFiles.first()
                } else if (copiedFiles.isNotEmpty()) {
                    pendingReceiptFiles = pendingReceiptFiles + copiedFiles
                }
            }
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { pickedUri: Uri? ->
        if (pickedUri != null) {
            currentImageUri = pickedUri
            coroutineScope.launch {
                val copied = ReceiptStorageManager.copyUriToTempFile(context, pickedUri)
                if (copied != null) {
                    pendingReceiptFiles = listOf(copied)
                }
            }
        }
    }

    // Trigger on-device OCR automatically whenever currentImageUri changes
    LaunchedEffect(currentImageUri) {
        val uri = currentImageUri
        if (uri != null) {
            uiState = OcrUiState.Processing("Reading shared screenshot...")
            val result = OcrProcessor.processImageUri(context, uri, accounts, categories)
            when (result) {
                is OcrResult.Success -> {
                    val candidate = repository.checkDuplicate(
                        referenceId = result.extracted.referenceId.value,
                        amountPaise = result.extracted.amountPaise.value,
                        dateMillis = result.extracted.dateEpochMillis.value
                    )
                    uiState = OcrUiState.Ready(
                        extracted = result.extracted,
                        rawText = result.rawText,
                        duplicateCandidate = candidate
                    )
                }
                is OcrResult.Error -> {
                    uiState = OcrUiState.Error(
                        message = result.message,
                        canRetry = result.canRetry
                    )
                }
            }
        } else if (initialExtracted != null) {
            uiState = OcrUiState.Ready(
                extracted = initialExtracted,
                rawText = initialExtracted.rawText,
                duplicateCandidate = null
            )
        } else {
            uiState = OcrUiState.Idle
        }
    }

    if (cropFileToProcess != null) {
        CropAndAdjustScreen(
            imageFile = cropFileToProcess!!,
            onCancel = { cropFileToProcess = null },
            onDone = { confirmedFile ->
                val existingIndex = pendingReceiptFiles.indexOf(cropFileToProcess)
                if (existingIndex != -1) {
                    pendingReceiptFiles = pendingReceiptFiles.toMutableList().apply { set(existingIndex, confirmedFile) }
                } else if (!pendingReceiptFiles.contains(confirmedFile)) {
                    pendingReceiptFiles = pendingReceiptFiles + confirmedFile
                }
                cropFileToProcess = null
            }
        )
        return
    }

    if (showAddReceiptSheet) {
        AddReceiptBottomSheet(
            onDismiss = { showAddReceiptSheet = false },
            onTakePhoto = {
                showAddReceiptSheet = false
                isDocumentScanMode = false
                val temp = ReceiptStorageManager.createTempReceiptFile(context)
                tempCameraFile = temp
                val uri = ReceiptStorageManager.getFileProviderUri(context, temp)
                try {
                    cameraLauncher.launch(uri)
                } catch (e: Exception) {
                    android.widget.Toast.makeText(context, "Cannot open camera: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                }
            },
            onChooseGallery = {
                showAddReceiptSheet = false
                try {
                    pickMediaLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                } catch (e: Exception) {
                    android.widget.Toast.makeText(context, "Cannot open gallery: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                }
            },
            onScanDocument = {
                showAddReceiptSheet = false
                isDocumentScanMode = true
                val temp = ReceiptStorageManager.createTempReceiptFile(context)
                tempCameraFile = temp
                val uri = ReceiptStorageManager.getFileProviderUri(context, temp)
                try {
                    cameraLauncher.launch(uri)
                } catch (e: Exception) {
                    android.widget.Toast.makeText(context, "Cannot open camera: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Smart Entry Review", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { photoPickerLauncher.launch("image/*") }) {
                        Icon(Icons.Default.AddPhotoAlternate, contentDescription = "Pick Another Image")
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (val state = uiState) {
                is OcrUiState.Idle -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.Image,
                            contentDescription = null,
                            tint = MoneyMapBlue,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("No Screenshot Selected", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Share a payment receipt from Google Pay, PhonePe, or Paytm, or pick an image below.",
                            color = TextSecondary,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = { photoPickerLauncher.launch("image/*") },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Pick Screenshot from Gallery")
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedButton(
                            onClick = { onNavigateToManualAdd?.invoke(null, pendingReceiptFiles) },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Enter Transaction Manually")
                        }
                    }
                }

                is OcrUiState.Processing -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            color = MoneyMapBlue,
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Text(
                            text = state.stepMessage,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Extracting amount, merchant, and reference ID on-device. No data is uploaded.",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }
                }

                is OcrUiState.Error -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.ErrorOutline,
                            contentDescription = null,
                            tint = MoneyMapRed,
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Could Not Read Transaction",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = state.message,
                            fontSize = 13.sp,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        if (state.canRetry && currentImageUri != null) {
                            Button(
                                onClick = {
                                    val temp = currentImageUri
                                    currentImageUri = null
                                    currentImageUri = temp
                                },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Retry OCR")
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        OutlinedButton(
                            onClick = { photoPickerLauncher.launch("image/*") },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Choose Another Image")
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(onClick = { onNavigateToManualAdd?.invoke(null, pendingReceiptFiles) }) {
                            Text("Enter Transaction Manually")
                        }
                    }
                }

                is OcrUiState.Ready -> {
                    OcrReviewForm(
                        extracted = state.extracted,
                        imageUri = currentImageUri,
                        pendingReceiptFiles = pendingReceiptFiles,
                        onPendingReceiptFilesChange = { pendingReceiptFiles = it },
                        onAddMoreReceipts = { showAddReceiptSheet = true },
                        onReceiptClick = { index ->
                            if (index in pendingReceiptFiles.indices) {
                                cropFileToProcess = pendingReceiptFiles[index]
                            }
                        },
                        duplicateCandidate = state.duplicateCandidate,
                        accounts = accounts,
                        categories = categories,
                        onSave = onSave,
                        onSaveWithReceipts = onSaveWithReceipts,
                        onNavigateToManualAdd = {
                            onNavigateToManualAdd?.invoke(state.extracted, pendingReceiptFiles)
                        },
                        onPickAnotherImage = { photoPickerLauncher.launch("image/*") }
                    )
                }
            }
        }
    }
}

@Composable
private fun OcrReviewForm(
    extracted: ExtractedTransaction,
    imageUri: Uri?,
    pendingReceiptFiles: List<File>,
    onPendingReceiptFilesChange: (List<File>) -> Unit,
    onAddMoreReceipts: () -> Unit,
    onReceiptClick: (Int) -> Unit,
    duplicateCandidate: TransactionEntity?,
    accounts: List<AccountEntity>,
    categories: List<CategoryEntity>,
    onSave: (TransactionEntity) -> Unit,
    onSaveWithReceipts: ((TransactionEntity, List<File>) -> Unit)? = null,
    onNavigateToManualAdd: (() -> Unit)? = null,
    onPickAnotherImage: () -> Unit
) {
    var txType by remember(extracted) { mutableStateOf(extracted.transactionType.value) }
    var amountInput by remember(extracted) {
        mutableStateOf(
            if (extracted.amountPaise.value > 0) {
                if (extracted.amountPaise.value % 100 == 0L) {
                    (extracted.amountPaise.value / 100).toString()
                } else {
                    String.format(Locale.ENGLISH, "%.2f", extracted.amountPaise.value / 100.0)
                }
            } else ""
        )
    }
    var merchant by remember(extracted) { mutableStateOf(extracted.merchantOrPayee.value) }
    var referenceId by remember(extracted) { mutableStateOf(extracted.referenceId.value ?: "") }
    var note by remember(extracted) { mutableStateOf(extracted.note.value) }
    var selectedAccountId by remember(extracted, accounts) {
        mutableStateOf(extracted.suggestedAccountId ?: accounts.firstOrNull()?.id ?: 0L)
    }

    val typeFilteredCategories = remember(txType, categories) {
        categories.filter { it.type == txType && !it.isArchived }
    }
    var selectedCategoryId by remember(txType, extracted, typeFilteredCategories) {
        mutableStateOf(
            extracted.suggestedCategoryId?.takeIf { id -> typeFilteredCategories.any { it.id == id } }
                ?: typeFilteredCategories.firstOrNull()?.id
        )
    }

    var showDuplicateConfirmDialog by remember { mutableStateOf(false) }

    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.ENGLISH) }
    val formattedDate = remember(extracted.dateEpochMillis.value) {
        dateFormat.format(Date(extracted.dateEpochMillis.value))
    }

    if (showDuplicateConfirmDialog && duplicateCandidate != null) {
        AlertDialog(
            onDismissRequest = { showDuplicateConfirmDialog = false },
            title = { Text("Save Possible Duplicate?") },
            text = {
                val candidateDate = dateFormat.format(Date(duplicateCandidate.dateEpochMillis))
                Text(
                    "An existing transaction of ₹${duplicateCandidate.amountPaise / 100} (${duplicateCandidate.merchantOrPayee}) on $candidateDate was already recorded.\n\nDo you want to save this transaction anyway?"
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDuplicateConfirmDialog = false
                        val paise = CurrencyFormatter.parseInputToPaise(amountInput)
                        val tx = TransactionEntity(
                            type = txType,
                            amountPaise = paise,
                            categoryId = selectedCategoryId,
                            accountId = selectedAccountId,
                            dateEpochMillis = extracted.dateEpochMillis.value,
                            merchantOrPayee = merchant.ifBlank { "UPI Transaction" },
                            note = note,
                            referenceId = referenceId.ifBlank { null },
                            isImported = true
                        )
                        if (onSaveWithReceipts != null) {
                            onSaveWithReceipts(tx, pendingReceiptFiles)
                        } else {
                            onSave(tx)
                        }
                    }
                ) {
                    Text("Save Anyway")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDuplicateConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 48.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Receipt Attachment Section (Screen 1 & Screen 6: Attached Receipts Row)
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (pendingReceiptFiles.isNotEmpty()) "Attached Receipts (${pendingReceiptFiles.size})" else "Receipt Attachment",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                    if (onNavigateToManualAdd != null) {
                        TextButton(
                            onClick = onNavigateToManualAdd,
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(13.dp), tint = MoneyMapBlue)
                            Spacer(modifier = Modifier.width(3.dp))
                            Text("Full Add Screen", fontSize = 11.sp, color = MoneyMapBlue, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))

                if (pendingReceiptFiles.isEmpty()) {
                    Surface(
                        onClick = onAddMoreReceipts,
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = BorderStroke(1.dp, BorderSubtle.copy(alpha = 0.6f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("add_receipt_button_card")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MoneyMapBlueLight),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Receipt,
                                    contentDescription = null,
                                    tint = MoneyMapBlue,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Add Receipt (Optional)",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Take a photo or choose from gallery",
                                    fontSize = 12.sp,
                                    color = TextSecondary
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = TextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = BorderStroke(1.dp, BorderSubtle.copy(alpha = 0.6f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("attached_receipts_card")
                    ) {
                        Box(modifier = Modifier.padding(14.dp)) {
                            AttachedReceiptsRow(
                                receiptFiles = pendingReceiptFiles,
                                receiptEntities = emptyList(),
                                onAddMore = onAddMoreReceipts,
                                onReceiptClick = onReceiptClick,
                                onRemoveReceipt = { index ->
                                    if (index in pendingReceiptFiles.indices) {
                                        val removed = pendingReceiptFiles[index]
                                        val updated = pendingReceiptFiles.toMutableList().apply { removeAt(index) }
                                        onPendingReceiptFilesChange(updated)
                                        ReceiptStorageManager.deleteTempFile(removed)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }

        // Payment Status Warning Banner
        if (!extracted.paymentStatusWarning.isNullOrBlank()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFEE2E2))
                ) {
                    Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = MoneyMapRed)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Payment Status Warning", fontWeight = FontWeight.Bold, color = MoneyMapRed, fontSize = 13.sp)
                            Text(extracted.paymentStatusWarning, fontSize = 12.sp, color = MoneyMapRed)
                        }
                    }
                }
            }
        }

        // Duplicate Warning Banner
        if (duplicateCandidate != null) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF3C7))
                ) {
                    Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFD97706))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Possible Duplicate Detected", fontWeight = FontWeight.Bold, color = Color(0xFF92400E), fontSize = 13.sp)
                            Text(
                                "A transaction of ₹${duplicateCandidate.amountPaise / 100} exists on ${dateFormat.format(Date(duplicateCandidate.dateEpochMillis))}. You can still save if this was a separate payment.",
                                fontSize = 12.sp,
                                color = Color(0xFF92400E)
                            )
                        }
                    }
                }
            }
        }

        // Extraction Confidence Summary
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Detected Details", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SuggestionChip(
                            onClick = {},
                            label = { Text("Amount: ₹${if (extracted.amountPaise.value > 0) extracted.amountPaise.value / 100 else "?"} (${extracted.amountPaise.confidence}%)", fontSize = 11.sp) }
                        )
                        SuggestionChip(
                            onClick = {},
                            label = { Text("Type: ${extracted.transactionType.value.name} (${extracted.transactionType.confidence}%)", fontSize = 11.sp) }
                        )
                        if (extracted.merchantOrPayee.value.isNotBlank()) {
                            SuggestionChip(
                                onClick = {},
                                label = { Text("Party: ${extracted.merchantOrPayee.value}", fontSize = 11.sp) }
                            )
                        }
                        if (extracted.referenceId.value != null) {
                            SuggestionChip(
                                onClick = {},
                                label = { Text("Ref: ${extracted.referenceId.value}", fontSize = 11.sp) }
                            )
                        }
                    }
                }
            }
        }

        // Transaction Type Selector
        item {
            Column {
                Text("Transaction Type", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = TextSecondary)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(TransactionType.EXPENSE, TransactionType.INCOME, TransactionType.TRANSFER).forEach { t ->
                        val isSelected = txType == t
                        FilterChip(
                            selected = isSelected,
                            onClick = { txType = t },
                            label = { Text(t.displayName, fontSize = 13.sp) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                if (extracted.isTypeUncertain) {
                    Text(
                        "Please verify whether this was an expense, income, or self transfer.",
                        fontSize = 11.sp,
                        color = MoneyMapNavy,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }

        // Amount Input
        item {
            OutlinedTextField(
                value = amountInput,
                onValueChange = { amountInput = it.filter { ch -> ch.isDigit() || ch == '.' } },
                label = { Text("Amount (₹)") },
                prefix = { Text("₹ ", fontWeight = FontWeight.Bold) },
                isError = amountInput.isBlank() || amountInput == "0",
                supportingText = {
                    if (amountInput.isBlank() || amountInput == "0") {
                        Text("Please enter a valid amount", color = MoneyMapRed)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
        }

        // Merchant / Payee
        item {
            OutlinedTextField(
                value = merchant,
                onValueChange = { merchant = it },
                label = { Text(if (txType == TransactionType.INCOME) "Sender / Payer" else "Merchant / Payee") },
                placeholder = { Text("e.g. ABC Store, Zomato, Salary") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
        }

        // Account Selector
        item {
            Column {
                Text("Account", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = TextSecondary)
                Spacer(modifier = Modifier.height(6.dp))
                if (accounts.isEmpty()) {
                    Text("No accounts found. Please create an account first.", color = MoneyMapRed, fontSize = 12.sp)
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        accounts.forEach { acc ->
                            val isSel = selectedAccountId == acc.id
                            FilterChip(
                                selected = isSel,
                                onClick = { selectedAccountId = acc.id },
                                label = { Text(acc.name, fontSize = 12.sp) }
                            )
                        }
                    }
                }
            }
        }

        // Category Selector (if EXPENSE or INCOME)
        if (txType != TransactionType.TRANSFER && typeFilteredCategories.isNotEmpty()) {
            item {
                Column {
                    Text("Category", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = TextSecondary)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        typeFilteredCategories.forEach { cat ->
                            val isSel = selectedCategoryId == cat.id
                            FilterChip(
                                selected = isSel,
                                onClick = { selectedCategoryId = cat.id },
                                label = { Text(cat.name, fontSize = 12.sp) }
                            )
                        }
                    }
                }
            }
        }

        // Date & Time
        item {
            OutlinedTextField(
                value = formattedDate,
                onValueChange = {},
                readOnly = true,
                label = { Text("Transaction Date & Time") },
                leadingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null) },
                supportingText = {
                    if (!extracted.isDateDetected) {
                        Text("Date was not detected in screenshot (defaulted to now).", color = TextSecondary)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
        }

        // UPI Reference ID
        item {
            OutlinedTextField(
                value = referenceId,
                onValueChange = { referenceId = it },
                label = { Text("UPI Ref No / Transaction ID (Optional)") },
                placeholder = { Text("e.g. 614057380921") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
        }

        // Note
        item {
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Notes (Optional)") },
                placeholder = { Text("Add any extra notes") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
        }

        // Confirm & Save Button
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = {
                    val paise = CurrencyFormatter.parseInputToPaise(amountInput)
                    if (paise <= 0) return@Button

                    if (duplicateCandidate != null) {
                        showDuplicateConfirmDialog = true
                    } else {
                        val tx = TransactionEntity(
                            type = txType,
                            amountPaise = paise,
                            categoryId = selectedCategoryId,
                            accountId = selectedAccountId,
                            dateEpochMillis = extracted.dateEpochMillis.value,
                            merchantOrPayee = merchant.ifBlank { "UPI Transaction" },
                            note = note,
                            referenceId = referenceId.ifBlank { null },
                            isImported = true
                        )
                        if (onSaveWithReceipts != null) {
                            onSaveWithReceipts(tx, pendingReceiptFiles)
                        } else {
                            onSave(tx)
                        }
                    }
                },
                enabled = amountInput.isNotBlank() && CurrencyFormatter.parseInputToPaise(amountInput) > 0,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("save_reviewed_transaction"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MoneyMapNavy)
            ) {
                Text("Confirm & Save Transaction", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfStatementImportScreen(
    candidates: List<StatementCandidate>,
    onImportSelected: (List<StatementCandidate>) -> Unit,
    onNavigateToDuplicateResolution: (StatementCandidate) -> Unit,
    onBack: () -> Unit
) {
    val candidateList = remember { mutableStateListOf(*candidates.toTypedArray()) }
    val selectedCount = candidateList.count { it.isSelected }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Import Preview", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding(),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("$selectedCount selected", fontWeight = FontWeight.Bold)
                        Text("Ready to import", fontSize = 12.sp, color = TextSecondary)
                    }

                    Button(
                        onClick = {
                            val selected = candidateList.filter { it.isSelected }
                            onImportSelected(selected)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MoneyMapBlue),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Import Selected", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // File Info Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF))
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = MoneyMapRed, modifier = Modifier.size(36.dp))
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text("HDFC_Bank_Statement_May2026.pdf", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("12 pages • 1.8 MB • 14 extracted", fontSize = 12.sp, color = TextSecondary)
                        }
                    }
                }
            }

            // Candidate Rows with Checkboxes
            items(candidateList, key = { it.tempId }) { item ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp)),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 1.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = item.isSelected,
                            onCheckedChange = { checked ->
                                val idx = candidateList.indexOf(item)
                                if (idx >= 0) {
                                    candidateList[idx] = item.copy(isSelected = checked)
                                }
                            }
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(item.merchantOrDescription, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                if (item.status == CandidateStatus.POSSIBLE_DUPLICATE) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        "Duplicate",
                                        color = MoneyMapOrange,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(MoneyMapOrangeLight)
                                            .clickable { onNavigateToDuplicateResolution(item) }
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text("${item.dateFormatted} • ${item.categoryName}", fontSize = 12.sp, color = TextSecondary)
                        }

                        val amtFormatted = CurrencyFormatter.formatRupees(item.amountPaise)
                        Text(
                            text = if (item.transactionType == TransactionType.INCOME) "+$amtFormatted" else "-$amtFormatted",
                            fontWeight = FontWeight.Bold,
                            color = if (item.transactionType == TransactionType.INCOME) MoneyMapGreen else MoneyMapRed
                        )
                    }
                }
            }
        }
    }
}

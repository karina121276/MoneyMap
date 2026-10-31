package com.thecorporatedeveloper.moneymap.ui.screens

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.RectF
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.thecorporatedeveloper.moneymap.data.entity.ReceiptEntity
import com.thecorporatedeveloper.moneymap.receipt.ReceiptStorageManager
import com.thecorporatedeveloper.moneymap.ui.theme.*
import kotlinx.coroutines.launch
import java.io.File

/**
 * Bottom Sheet presenting options to capture, select, or scan a receipt (Screen 2).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddReceiptBottomSheet(
    onDismiss: () -> Unit,
    onTakePhoto: () -> Unit,
    onChooseGallery: () -> Unit,
    onScanDocument: () -> Unit = {}
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = {
            BottomSheetDefaults.DragHandle(color = Color.LightGray)
        },
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .padding(bottom = 32.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Add Receipt",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(36.dp).testTag("close_add_receipt_sheet")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Option 1: Take Photo
            ReceiptOptionCard(
                icon = Icons.Default.PhotoCamera,
                iconTint = MoneyMapBlue,
                iconBg = MoneyMapBlueLight,
                title = "Take Photo",
                subtitle = "Capture receipt with camera",
                testTag = "option_take_photo",
                onClick = {
                    onDismiss()
                    onTakePhoto()
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Option 2: Choose from Gallery
            ReceiptOptionCard(
                icon = Icons.Default.Image,
                iconTint = MoneyMapGreen,
                iconBg = MoneyMapGreenLight,
                title = "Choose from Gallery",
                subtitle = "Select image from gallery",
                testTag = "option_choose_gallery",
                onClick = {
                    onDismiss()
                    onChooseGallery()
                }
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Info hint
            Text(
                text = "You can add multiple images for one transaction.",
                fontSize = 12.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun ReceiptOptionCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    iconBg: Color,
    title: String,
    subtitle: String,
    showChevron: Boolean = false,
    testTag: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = BorderStroke(1.dp, BorderSubtle.copy(alpha = 0.6f)),
        modifier = Modifier.fillMaxWidth().testTag(testTag)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }
            if (showChevron) {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

/**
 * Screen for cropping, rotating, and adjusting/enhancing receipt photos (Screen 4).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CropAndAdjustScreen(
    imageFile: File,
    onDone: (File) -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var currentFile by remember { mutableStateOf<File>(imageFile) }
    var fileUpdateKey by remember { mutableIntStateOf(0) }
    var isProcessing by remember { mutableStateOf(false) }

    // Normalized crop rectangle: 0f..1f
    var cropLeft by remember { mutableFloatStateOf(0.05f) }
    var cropTop by remember { mutableFloatStateOf(0.08f) }
    var cropRight by remember { mutableFloatStateOf(0.95f) }
    var cropBottom by remember { mutableFloatStateOf(0.92f) }

    BackHandler { onCancel() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Crop & Adjust", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onCancel, modifier = Modifier.testTag("crop_back_btn")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
                border = BorderStroke(1.dp, BorderSubtle.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    // Tool buttons row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CropToolButton(
                            icon = Icons.Default.RotateLeft,
                            label = "Rotate Left",
                            enabled = !isProcessing,
                            testTag = "btn_rotate_left",
                            onClick = {
                                coroutineScope.launch {
                                    isProcessing = true
                                    ReceiptStorageManager.rotateImageFile(currentFile, -90f)
                                    fileUpdateKey++
                                    isProcessing = false
                                }
                            }
                        )

                        CropToolButton(
                            icon = Icons.Default.RotateRight,
                            label = "Rotate Right",
                            enabled = !isProcessing,
                            testTag = "btn_rotate_right",
                            onClick = {
                                coroutineScope.launch {
                                    isProcessing = true
                                    ReceiptStorageManager.rotateImageFile(currentFile, 90f)
                                    fileUpdateKey++
                                    isProcessing = false
                                }
                            }
                        )

                        CropToolButton(
                            icon = Icons.Default.Crop,
                            label = "Reset Crop",
                            enabled = !isProcessing,
                            testTag = "btn_reset_crop",
                            onClick = {
                                cropLeft = 0.05f
                                cropTop = 0.08f
                                cropRight = 0.95f
                                cropBottom = 0.92f
                            }
                        )

                        CropToolButton(
                            icon = Icons.Default.AutoFixHigh,
                            label = "Enhance",
                            enabled = !isProcessing,
                            testTag = "btn_enhance",
                            onClick = {
                                coroutineScope.launch {
                                    isProcessing = true
                                    val enhanced = ReceiptStorageManager.enhanceDocumentImage(currentFile)
                                    if (enhanced != null) {
                                        currentFile = enhanced
                                        fileUpdateKey++
                                        Toast.makeText(context, "Enhanced contrast & clarity", Toast.LENGTH_SHORT).show()
                                    }
                                    isProcessing = false
                                }
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Use this image primary button
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                isProcessing = true
                                val cropRect = RectF(cropLeft, cropTop, cropRight, cropBottom)
                                val finalCropped = ReceiptStorageManager.cropImageFile(currentFile, cropRect)
                                isProcessing = false
                                if (finalCropped != null) {
                                    onDone(finalCropped)
                                } else {
                                    onDone(currentFile)
                                }
                            }
                        },
                        enabled = !isProcessing,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("use_this_image_btn"),
                        colors = ButtonDefaults.buttonColors(containerColor = MoneyMapBlue),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        if (isProcessing) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                        } else {
                            Text("Use This Image", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFF1E293B)),
            contentAlignment = Alignment.Center
        ) {
            // Preview Image with crop overlays
            key(fileUpdateKey) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(currentFile)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Receipt Image",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp)
                )
            }

            // Visual Crop Box Frame
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                // Interactive Crop Overlay Frame
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction = (cropRight - cropLeft).coerceIn(0.2f, 1f))
                        .fillMaxHeight(fraction = (cropBottom - cropTop).coerceIn(0.2f, 1f))
                        .align(Alignment.Center)
                        .border(2.dp, Color.White, RoundedCornerShape(8.dp))
                ) {
                    // Corner grips
                    Box(modifier = Modifier.size(16.dp).background(MoneyMapBlue, CircleShape).align(Alignment.TopStart))
                    Box(modifier = Modifier.size(16.dp).background(MoneyMapBlue, CircleShape).align(Alignment.TopEnd))
                    Box(modifier = Modifier.size(16.dp).background(MoneyMapBlue, CircleShape).align(Alignment.BottomStart))
                    Box(modifier = Modifier.size(16.dp).background(MoneyMapBlue, CircleShape).align(Alignment.BottomEnd))
                }
            }

            if (isProcessing) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color.White)
                }
            }
        }
    }
}

@Composable
private fun CropToolButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    enabled: Boolean,
    testTag: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .testTag(testTag)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (enabled) MaterialTheme.colorScheme.onSurface else TextSecondary.copy(alpha = 0.5f),
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            color = if (enabled) MaterialTheme.colorScheme.onSurface else TextSecondary.copy(alpha = 0.5f)
        )
    }
}

/**
 * Fullscreen Receipt Viewer with Zoom, Pan, Rotation, Counter, and Overflow Menu (Screen 8).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceiptViewerScreen(
    receipts: List<ReceiptEntity>,
    initialReceiptId: Long? = null,
    onBack: () -> Unit,
    onDeleteReceipt: (ReceiptEntity) -> Unit,
    onShareReceipt: (ReceiptEntity) -> Unit,
    onDownloadReceipt: (ReceiptEntity) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var currentIndex by remember {
        val initialIdx = receipts.indexOfFirst { it.id == initialReceiptId }
        mutableIntStateOf(if (initialIdx >= 0) initialIdx else 0)
    }

    val currentReceipt = receipts.getOrNull(currentIndex)
    var showOverflowMenu by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    // Zoom & pan states
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var rotationAngle by remember { mutableFloatStateOf(0f) }

    // Reset transform on page change
    LaunchedEffect(currentIndex) {
        scale = 1f
        offset = Offset.Zero
        rotationAngle = 0f
    }

    BackHandler { onBack() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (receipts.isNotEmpty()) {
                        Text(
                            text = "${currentIndex + 1} / ${receipts.size}",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp,
                            color = Color.White
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("viewer_back_btn")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showOverflowMenu = true },
                        modifier = Modifier.testTag("viewer_overflow_menu_btn")
                    ) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Options", tint = Color.White)
                    }
                    DropdownMenu(
                        expanded = showOverflowMenu,
                        onDismissRequest = { showOverflowMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Share") },
                            leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) },
                            onClick = {
                                showOverflowMenu = false
                                currentReceipt?.let { onShareReceipt(it) }
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Download") },
                            leadingIcon = { Icon(Icons.Default.Download, contentDescription = null) },
                            onClick = {
                                showOverflowMenu = false
                                currentReceipt?.let { onDownloadReceipt(it) }
                            }
                        )
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text("Delete Receipt", color = MoneyMapRed) },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MoneyMapRed) },
                            onClick = {
                                showOverflowMenu = false
                                showDeleteConfirmDialog = true
                            }
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black.copy(alpha = 0.85f)
                )
            )
        },
        bottomBar = {
            Surface(
                color = Color.Black.copy(alpha = 0.85f),
                contentColor = Color.White
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Share
                    ViewerBottomAction(
                        icon = Icons.Default.Share,
                        label = "Share",
                        testTag = "viewer_action_share",
                        onClick = { currentReceipt?.let { onShareReceipt(it) } }
                    )

                    // Zoom toggle
                    ViewerBottomAction(
                        icon = if (scale > 1f) Icons.Default.ZoomOut else Icons.Default.ZoomIn,
                        label = if (scale > 1f) "Reset" else "Zoom",
                        testTag = "viewer_action_zoom",
                        onClick = {
                            if (scale > 1f) {
                                scale = 1f
                                offset = Offset.Zero
                            } else {
                                scale = 2.5f
                            }
                        }
                    )

                    // Rotate
                    ViewerBottomAction(
                        icon = Icons.Default.RotateRight,
                        label = "Rotate",
                        testTag = "viewer_action_rotate",
                        onClick = {
                            rotationAngle = (rotationAngle + 90f) % 360f
                        }
                    )

                    // Delete
                    ViewerBottomAction(
                        icon = Icons.Default.Delete,
                        label = "Delete",
                        tint = MoneyMapRed,
                        testTag = "viewer_action_delete",
                        onClick = { showDeleteConfirmDialog = true }
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            if (currentReceipt != null) {
                val receiptFile = ReceiptStorageManager.getReceiptFile(context, currentReceipt)

                if (receiptFile.exists()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(Unit) {
                                detectTransformGestures { _, pan, zoom, _ ->
                                    scale = (scale * zoom).coerceIn(1f, 5f)
                                    if (scale > 1f) {
                                        val maxOffsetX = (size.width * (scale - 1)) / 2
                                        val maxOffsetY = (size.height * (scale - 1)) / 2
                                        offset = Offset(
                                            x = (offset.x + pan.x).coerceIn(-maxOffsetX, maxOffsetX),
                                            y = (offset.y + pan.y).coerceIn(-maxOffsetY, maxOffsetY)
                                        )
                                    } else {
                                        offset = Offset.Zero
                                    }
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(receiptFile)
                                .crossfade(true)
                                .build(),
                            contentDescription = "Full receipt view",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer(
                                    scaleX = scale,
                                    scaleY = scale,
                                    translationX = offset.x,
                                    translationY = offset.y,
                                    rotationZ = rotationAngle
                                )
                        )
                    }
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Icon(Icons.Default.BrokenImage, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(64.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Receipt image file is not found on device storage.",
                            color = Color.White,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // Previous / Next navigational chevron overlays if multiple receipts
                if (receipts.size > 1) {
                    if (currentIndex > 0) {
                        IconButton(
                            onClick = { currentIndex-- },
                            modifier = Modifier
                                .align(Alignment.CenterStart)
                                .padding(start = 12.dp)
                                .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous", tint = Color.White)
                        }
                    }
                    if (currentIndex < receipts.size - 1) {
                        IconButton(
                            onClick = { currentIndex++ },
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .padding(end = 12.dp)
                                .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                        ) {
                            Icon(Icons.Default.ChevronRight, contentDescription = "Next", tint = Color.White)
                        }
                    }
                }
            } else {
                Text("Receipt not found", color = Color.White)
            }
        }
    }

    if (showDeleteConfirmDialog && currentReceipt != null) {
        RemoveReceiptDialog(
            onDismiss = { showDeleteConfirmDialog = false },
            onConfirm = {
                showDeleteConfirmDialog = false
                val toDelete = currentReceipt
                if (receipts.size <= 1) {
                    onDeleteReceipt(toDelete)
                    onBack()
                } else {
                    if (currentIndex >= receipts.size - 1) {
                        currentIndex = receipts.size - 2
                    }
                    onDeleteReceipt(toDelete)
                }
            }
        )
    }
}

@Composable
private fun ViewerBottomAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: Color = Color.White,
    testTag: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .testTag(testTag)
    ) {
        Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.height(4.dp))
        Text(label, color = tint, fontSize = 11.sp)
    }
}

/**
 * Multiple Receipts Screen (Screen 9): Displays grid of all attached receipts with badges and "+ Add More".
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MultipleReceiptsScreen(
    receipts: List<ReceiptEntity>,
    onBack: () -> Unit,
    onAddMore: () -> Unit,
    onSelectReceipt: (ReceiptEntity) -> Unit,
    onDeleteReceipt: (ReceiptEntity) -> Unit
) {
    val context = LocalContext.current
    var isEditMode by remember { mutableStateOf(false) }
    var receiptToDelete by remember { mutableStateOf<ReceiptEntity?>(null) }

    BackHandler { onBack() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Multiple Receipts", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(
                        onClick = { isEditMode = !isEditMode },
                        modifier = Modifier.testTag("toggle_receipts_edit_mode")
                    ) {
                        Text(
                            text = if (isEditMode) "Done" else "Edit",
                            color = MoneyMapBlue,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                itemsIndexed(receipts, key = { _, r -> r.id }) { index, receipt ->
                    ReceiptGridCard(
                        receipt = receipt,
                        index = index,
                        isEditMode = isEditMode,
                        onClick = { onSelectReceipt(receipt) },
                        onRemove = { receiptToDelete = receipt }
                    )
                }

                // "+ Add More" card
                item {
                    AddMoreReceiptCard(onClick = onAddMore)
                }
            }

            // Bottom note
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "You can add multiple images for one transaction. Useful for split bills or multiple receipts.",
                    fontSize = 12.sp,
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }
    }

    receiptToDelete?.let { receipt ->
        RemoveReceiptDialog(
            onDismiss = { receiptToDelete = null },
            onConfirm = {
                onDeleteReceipt(receipt)
                receiptToDelete = null
            }
        )
    }
}

@Composable
private fun ReceiptGridCard(
    receipt: ReceiptEntity,
    index: Int,
    isEditMode: Boolean,
    onClick: () -> Unit,
    onRemove: () -> Unit
) {
    val context = LocalContext.current
    val thumbFile = ReceiptStorageManager.getReceiptThumbnailFile(context, receipt)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("receipt_grid_item_$index")
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFFF1F5F9))
                .border(1.dp, BorderSubtle, RoundedCornerShape(16.dp))
        ) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(thumbFile)
                    .crossfade(true)
                    .build(),
                contentDescription = "Receipt $index",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Remove button on top-right
            Box(
                modifier = Modifier
                    .padding(8.dp)
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.65f))
                    .clickable(onClick = onRemove)
                    .align(Alignment.TopEnd)
                    .testTag("remove_receipt_badge_$index"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Remove receipt",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Receipt ${index + 1}",
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurface
        )

        if (receipt.isPrimary) {
            Surface(
                color = MoneyMapBlueLight,
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier.padding(top = 4.dp)
            ) {
                Text(
                    text = "Main Receipt",
                    color = MoneyMapBlue,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}

@Composable
private fun AddMoreReceiptCard(onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFFF8FAFC),
        border = BorderStroke(1.5.dp, BorderSubtle),
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .testTag("add_more_receipt_card")
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MoneyMapBlueLight),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add More",
                    tint = MoneyMapBlue,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Add More",
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                color = MoneyMapBlue
            )
        }
    }
}

/**
 * Receipt Options Bottom Sheet (Screen 10): Shows options for a selected receipt.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceiptOptionsBottomSheet(
    receipt: ReceiptEntity,
    receiptIndex: Int = 1,
    onDismiss: () -> Unit,
    onView: () -> Unit,
    onDownload: () -> Unit,
    onShare: () -> Unit,
    onReplace: () -> Unit,
    onRemove: () -> Unit
) {
    val context = LocalContext.current
    val thumbFile = ReceiptStorageManager.getReceiptThumbnailFile(context, receipt)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle(color = Color.LightGray) },
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .padding(bottom = 32.dp)
        ) {
            // Header with thumbnail & title
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFE2E8F0))
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(thumbFile)
                            .crossfade(true)
                            .build(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Text(
                    text = "Receipt $receiptIndex",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    modifier = Modifier.weight(1f)
                )

                IconButton(onClick = onDismiss, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = BorderSubtle.copy(alpha = 0.6f))
            Spacer(modifier = Modifier.height(8.dp))

            // 1. View Image
            ReceiptOptionRow(
                icon = Icons.Default.Visibility,
                title = "View Image",
                onClick = {
                    onDismiss()
                    onView()
                }
            )

            // 2. Download
            ReceiptOptionRow(
                icon = Icons.Default.Download,
                title = "Download",
                onClick = {
                    onDismiss()
                    onDownload()
                }
            )

            // 3. Share
            ReceiptOptionRow(
                icon = Icons.Default.Share,
                title = "Share",
                onClick = {
                    onDismiss()
                    onShare()
                }
            )

            // 4. Replace Image
            ReceiptOptionRow(
                icon = Icons.Default.Sync,
                title = "Replace Image",
                onClick = {
                    onDismiss()
                    onReplace()
                }
            )

            // 5. Remove Image
            ReceiptOptionRow(
                icon = Icons.Default.Delete,
                title = "Remove Image",
                tint = MoneyMapRed,
                onClick = {
                    onDismiss()
                    onRemove()
                }
            )
        }
    }
}

@Composable
private fun ReceiptOptionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = tint,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = title,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = tint
        )
    }
}

/**
 * Remove Receipt Confirmation Dialog (Screen 11).
 */
@Composable
fun RemoveReceiptDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(24.dp),
        icon = {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(MoneyMapRedLight),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    tint = MoneyMapRed,
                    modifier = Modifier.size(28.dp)
                )
            }
        },
        title = {
            Text(
                text = "Remove Receipt?",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Text(
                text = "This will remove the receipt from this transaction. This action cannot be undone.",
                fontSize = 13.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = MoneyMapRed),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("confirm_remove_receipt_btn")
            ) {
                Text("Remove", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("cancel_remove_receipt_btn")
            ) {
                Text("Cancel", color = TextSecondary)
            }
        }
    )
}

/**
 * Compact horizontal row showing attached receipts (Screens 6 & 7).
 */
@Composable
fun AttachedReceiptsRow(
    receiptFiles: List<File> = emptyList(),
    receiptEntities: List<ReceiptEntity> = emptyList(),
    onAddMore: () -> Unit,
    onReceiptClick: (Int) -> Unit,
    onRemoveReceipt: (Int) -> Unit
) {
    val context = LocalContext.current
    val totalCount = maxOf(receiptFiles.size, receiptEntities.size)

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Attached Receipts ($totalCount)",
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            TextButton(onClick = onAddMore) {
                Text("+ Add More", color = MoneyMapBlue, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            if (receiptFiles.isNotEmpty()) {
                itemsIndexed(receiptFiles) { index, file ->
                    AttachedThumbnailCard(
                        model = file,
                        index = index,
                        onClick = { onReceiptClick(index) },
                        onRemove = { onRemoveReceipt(index) }
                    )
                }
            } else if (receiptEntities.isNotEmpty()) {
                itemsIndexed(receiptEntities) { index, entity ->
                    val thumbFile = ReceiptStorageManager.getReceiptThumbnailFile(context, entity)
                    AttachedThumbnailCard(
                        model = thumbFile,
                        index = index,
                        onClick = { onReceiptClick(index) },
                        onRemove = { onRemoveReceipt(index) }
                    )
                }
            }
        }
    }
}

@Composable
private fun AttachedThumbnailCard(
    model: Any,
    index: Int,
    onClick: () -> Unit,
    onRemove: () -> Unit
) {
    val context = LocalContext.current
    Box(
        modifier = Modifier
            .size(width = 80.dp, height = 80.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFE2E8F0))
            .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .testTag("attached_receipt_thumb_$index")
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(model)
                .crossfade(true)
                .build(),
            contentDescription = "Receipt $index",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Close 'X' button
        Box(
            modifier = Modifier
                .padding(4.dp)
                .size(20.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.65f))
                .clickable(onClick = onRemove)
                .align(Alignment.TopEnd)
                .testTag("remove_attached_thumb_$index"),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Remove",
                tint = Color.White,
                modifier = Modifier.size(12.dp)
            )
        }
    }
}

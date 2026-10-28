package com.thecorporatedeveloper.moneymap.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.core.content.FileProvider
import com.thecorporatedeveloper.moneymap.backup.BackupManager
import com.thecorporatedeveloper.moneymap.data.entity.AccountEntity
import com.thecorporatedeveloper.moneymap.data.entity.BudgetEntity
import com.thecorporatedeveloper.moneymap.data.entity.CategoryEntity
import com.thecorporatedeveloper.moneymap.data.entity.TransactionEntity
import com.thecorporatedeveloper.moneymap.data.preferences.UserPreferences
import com.thecorporatedeveloper.moneymap.ui.components.parseColorHex
import com.thecorporatedeveloper.moneymap.ui.theme.*
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    preferences: UserPreferences,
    onSetThemeMode: (String) -> Unit,
    onSetLanguage: (String) -> Unit = {},
    onNavigateToCategories: () -> Unit = {},
    onNavigateToBackup: () -> Unit,
    onNavigateToPrivacy: () -> Unit,
    onNavigateToWidgetSettings: () -> Unit = {},
    onBack: () -> Unit
) {
    val currentAccent = parseColorHex(preferences.accentColorHex, MoneyMapBlue)
    val context = LocalContext.current

    var showPrivacyDialog by remember { mutableStateOf(false) }
    var showTermsDialog by remember { mutableStateOf(false) }
    var showDataSafetyDialog by remember { mutableStateOf(false) }
    var showSupportDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold) },
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
            // Appearance Section
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text("Theme Mode", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Spacer(modifier = Modifier.height(10.dp))

                        // Theme Mode Selector: Light, Dark, System
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            listOf("Light" to Icons.Default.LightMode, "Dark" to Icons.Default.DarkMode, "System" to Icons.Default.Smartphone).forEach { (mode, icon) ->
                                val isSel = preferences.themeMode.equals(mode, ignoreCase = true)
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(14.dp))
                                        .clickable { onSetThemeMode(mode.uppercase()) }
                                        .border(
                                            width = if (isSel) 2.dp else 1.dp,
                                            color = if (isSel) currentAccent else BorderSubtle,
                                            shape = RoundedCornerShape(14.dp)
                                        ),
                                    color = if (isSel) currentAccent.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(icon, contentDescription = mode, tint = if (isSel) currentAccent else TextSecondary, modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(mode, fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium, fontSize = 12.sp)
                                    }
                                }
                            }
                        }

                    }
                }
            }

            // Management Navigation Section
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column {
                        SettingsRow(icon = Icons.Default.Category, title = "Category Management", onClick = onNavigateToCategories)
                        HorizontalDivider(color = BorderSubtle.copy(alpha = 0.5f))
                        SettingsRow(icon = Icons.Default.CloudSync, title = "Backup & Restore", onClick = onNavigateToBackup)
                        HorizontalDivider(color = BorderSubtle.copy(alpha = 0.5f))
                        SettingsRow(icon = Icons.Default.Lock, title = "App Lock & Security", onClick = onNavigateToPrivacy)
                    }
                }
            }

            // Legal & Compliance Section
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column {
                        SettingsRow(
                            icon = Icons.Default.PrivacyTip,
                            title = "Privacy Policy",
                            onClick = {
                                val url = "https://doc-hosting.flycricket.io/moneymap-privacy-policy/01d50b25-5834-403b-be59-1c0967a0c3d4/privacy"
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "No web browser available to open Privacy Policy", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                        HorizontalDivider(color = BorderSubtle.copy(alpha = 0.5f))
                        SettingsRow(icon = Icons.Default.Description, title = "Terms of Service", onClick = { showTermsDialog = true })
                        HorizontalDivider(color = BorderSubtle.copy(alpha = 0.5f))
                        SettingsRow(icon = Icons.Default.VerifiedUser, title = "Data Safety Disclosure", onClick = { showDataSafetyDialog = true })
                        HorizontalDivider(color = BorderSubtle.copy(alpha = 0.5f))
                        SettingsRow(icon = Icons.Default.HelpCenter, title = "Help & Support / FAQ", onClick = { showSupportDialog = true })
                        HorizontalDivider(color = BorderSubtle.copy(alpha = 0.5f))
                        SettingsRow(icon = Icons.Default.Info, title = "About & Licenses", onClick = { showAboutDialog = true })
                    }
                }
            }

            // About MoneyMap
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("MoneyMap v1.0", fontWeight = FontWeight.Bold, color = TextSecondary, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text("Offline-first Personal Finance • Track • Plan • Grow", color = TextSecondary, fontSize = 11.sp)
                }
            }
        }
    }

    // Dialogs
    if (showPrivacyDialog) {
        AlertDialog(
            onDismissRequest = { showPrivacyDialog = false },
            title = { Text("Privacy Policy", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("MoneyMap is built with a strict 100% offline-first privacy architecture.")
                    Text("• All accounts, transactions, categories, and budgets are stored locally on your device in an encrypted Room SQLite database.")
                    Text("• We do not collect, upload, transmit, or store any personal financial data on external servers.")
                    Text("• No third-party tracking, analytics, or advertising SDKs are included.")
                }
            },
            confirmButton = {
                Button(onClick = { showPrivacyDialog = false }, shape = RoundedCornerShape(10.dp)) {
                    Text("Got it")
                }
            }
        )
    }

    if (showTermsDialog) {
        AlertDialog(
            onDismissRequest = { showTermsDialog = false },
            title = { Text("Terms of Service", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("By using MoneyMap, you agree to use this application for personal financial tracking purposes.")
                    Text("• MoneyMap is provided on an 'as is' basis without warranties of any kind.")
                    Text("• Because all data is stored locally on your device, you are responsible for regularly exporting backups using the Backup & Restore feature to prevent data loss upon device damage or factory reset.")
                }
            },
            confirmButton = {
                Button(onClick = { showTermsDialog = false }, shape = RoundedCornerShape(10.dp)) {
                    Text("Accept")
                }
            }
        )
    }

    if (showDataSafetyDialog) {
        AlertDialog(
            onDismissRequest = { showDataSafetyDialog = false },
            title = { Text("Data Safety Disclosure", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Google Play Data Safety Summary:")
                    Text("• Data Collection & Sharing: 0 bytes collected or shared with third parties.")
                    Text("• Security Practices: Data is encrypted at rest in local SQLite database storage.")
                    Text("• User Controls: You can export your data to JSON or wipe all stored records at any time.")
                }
            },
            confirmButton = {
                Button(onClick = { showDataSafetyDialog = false }, shape = RoundedCornerShape(10.dp)) {
                    Text("Close")
                }
            }
        )
    }

    if (showSupportDialog) {
        AlertDialog(
            onDismissRequest = { showSupportDialog = false },
            title = { Text("Help & Support / FAQ", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Frequently Asked Questions:")
                    Text("1. How do I backup my financial data?\nGo to Settings > Backup & Restore to export your ledger JSON file.")
                    Text("2. What if I forget my App Lock PIN?\nPINs are securely hashed on device. If forgotten, you can restore from a previous backup file.")
                    Text("Support Email: support@moneymap.app")
                }
            },
            confirmButton = {
                Button(onClick = { showSupportDialog = false }, shape = RoundedCornerShape(10.dp)) {
                    Text("Close")
                }
            }
        )
    }

    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = { Text("About MoneyMap", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("MoneyMap v1.0.0 (Build 100)")
                    Text("Tagline: Track • Plan • Grow")
                    Text("Built with native Android Jetpack Compose and Kotlin.")
                    Text("Copyright © 2026 MoneyMap. All rights reserved.")
                }
            },
            confirmButton = {
                Button(onClick = { showAboutDialog = false }, shape = RoundedCornerShape(10.dp)) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
fun SettingsRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MoneyMapNavy, modifier = Modifier.size(22.dp))
        Spacer(modifier = Modifier.width(14.dp))
        Text(title, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.LightGray)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupRestoreScreen(
    accounts: List<AccountEntity>,
    categories: List<CategoryEntity>,
    transactions: List<TransactionEntity>,
    budgets: List<BudgetEntity>,
    onRestore: (List<AccountEntity>, List<CategoryEntity>, List<TransactionEntity>) -> Unit,
    onClearAllData: (onFinished: () -> Unit) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var showRestoreConfirmDialog by remember { mutableStateOf(false) }
    var showClearAllDialog by remember { mutableStateOf(false) }
    var pendingRestoreData by remember { mutableStateOf<Triple<List<AccountEntity>, List<CategoryEntity>, List<TransactionEntity>>?>(null) }

    // SAF file launchers
    val createJsonDocLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        uri?.let {
            val json = BackupManager.createJsonBackup(accounts, categories, transactions, budgets)
            val result = BackupManager.writeToUriSafely(context, it, json)
            if (result.isSuccess) {
                Toast.makeText(context, "Backup saved successfully!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Backup failed because the file could not be written. Your existing data has not been intentionally deleted.", Toast.LENGTH_LONG).show()
            }
        }
    }

    val createCsvDocLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri: Uri? ->
        uri?.let {
            val accMap = accounts.associateBy { it.id }
            val catMap = categories.associateBy { it.id }
            val csv = BackupManager.createCsvExport(transactions, accMap, catMap)
            val success = BackupManager.writeToUri(context, it, csv)
            Toast.makeText(context, if (success) "CSV exported successfully!" else "Failed to export CSV", Toast.LENGTH_SHORT).show()
        }
    }

    val openRestoreDocLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val json = BackupManager.readFromUri(context, it)
            if (json != null) {
                val result = BackupManager.parseJsonBackup(json)
                if (result.isSuccess) {
                    pendingRestoreData = result.getOrNull()
                    showRestoreConfirmDialog = true
                } else {
                    Toast.makeText(context, "Error validating backup: ${result.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Backup & Restore", fontWeight = FontWeight.Bold) },
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
            // Guarantee Card
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFFF0FDF4)
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = MoneyMapGreen, modifier = Modifier.size(36.dp))
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text("Your data stays on your device", fontWeight = FontWeight.Bold, color = Color(0xFF065F46))
                            Text("No cloud, no bank logins, no data upload. You are in full control.", color = Color(0xFF166534), fontSize = 12.sp)
                        }
                    }
                }
            }

            // Backup & Restore Actions
            item {
                Text("Backup & Restore", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Column {
                        SettingsRow(
                            icon = Icons.Default.CloudUpload,
                            title = "Create Backup (JSON)",
                            onClick = {
                                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ENGLISH).format(Date())
                                createJsonDocLauncher.launch("MoneyMap_Backup_$timestamp.json")
                            }
                        )
                        HorizontalDivider(color = BorderSubtle)
                        SettingsRow(
                            icon = Icons.Default.CloudDownload,
                            title = "Restore Backup",
                            onClick = { openRestoreDocLauncher.launch("application/json") }
                        )
                    }
                }
            }

            // Export Data Actions
            item {
                Text("Export Data", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Column {
                        SettingsRow(
                            icon = Icons.Default.TableChart,
                            title = "Export Transactions (CSV)",
                            onClick = {
                                val timestamp = SimpleDateFormat("yyyyMMdd", Locale.ENGLISH).format(Date())
                                createCsvDocLauncher.launch("MoneyMap_Transactions_$timestamp.csv")
                            }
                        )
                        HorizontalDivider(color = BorderSubtle)
                        SettingsRow(
                            icon = Icons.Default.Code,
                            title = "Export Full Data (JSON)",
                            onClick = {
                                val timestamp = SimpleDateFormat("yyyyMMdd", Locale.ENGLISH).format(Date())
                                createJsonDocLauncher.launch("MoneyMap_FullExport_$timestamp.json")
                            }
                        )
                    }
                }
            }

            // Clear All Data Option
            item {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showClearAllDialog = true }
                                .padding(16.dp)
                                .testTag("clear_all_data_row"),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.DeleteForever, contentDescription = null, tint = MoneyMapRed, modifier = Modifier.size(22.dp))
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Clear All Data", fontWeight = FontWeight.SemiBold, color = MoneyMapRed)
                                Text("Permanently delete all locally stored MoneyMap data from this device.", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                            }
                            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.LightGray)
                        }
                    }
                }
            }

            // Info Card
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("What's included in backup?", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("Accounts, transactions, categories, budgets, settings, and all your personal finance history.", color = TextSecondary, fontSize = 12.sp)
                    }
                }
            }
        }
    }

    if (showRestoreConfirmDialog && pendingRestoreData != null) {
        val (accs, cats, txs) = pendingRestoreData!!
        AlertDialog(
            onDismissRequest = { showRestoreConfirmDialog = false },
            title = { Text("Confirm Database Restore") },
            text = {
                Text("Found ${accs.size} accounts, ${cats.size} categories, and ${txs.size} transactions. Restoring will safely update your database.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        onRestore(accs, cats, txs)
                        showRestoreConfirmDialog = false
                        Toast.makeText(context, "Database restored successfully!", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("Restore")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showClearAllDialog) {
        var confirmInput by remember { mutableStateOf("") }
        var isClearing by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { if (!isClearing) showClearAllDialog = false },
            title = { Text("Clear all data?", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("This will permanently delete all your MoneyMap accounts, transactions, categories, budgets, favorites, and app data stored locally. This action cannot be undone. Export a backup first if you want to keep your records.", fontSize = 13.sp)
                    Text("Type DELETE below to confirm:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MoneyMapRed)
                    OutlinedTextField(
                        value = confirmInput,
                        onValueChange = { confirmInput = it },
                        placeholder = { Text("DELETE") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("clear_all_confirm_input")
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (confirmInput.trim() == "DELETE" && !isClearing) {
                            isClearing = true
                            onClearAllData {
                                isClearing = false
                                showClearAllDialog = false
                            }
                        }
                    },
                    enabled = confirmInput.trim() == "DELETE" && !isClearing,
                    modifier = Modifier.testTag("clear_all_confirm_button")
                ) {
                    if (isClearing) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else {
                        Text("Clear All Data", fontWeight = FontWeight.Bold, color = MoneyMapRed)
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showClearAllDialog = false },
                    enabled = !isClearing
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppLockPrivacyScreen(
    preferences: UserPreferences,
    onRequestStartPinSetup: () -> Unit = {},
    onChangePinRequested: () -> Unit = {},
    onDisableAppLockConfirmed: () -> Unit = {},
    onVerifyCurrentPinForDisable: suspend (String) -> Boolean = { true },
    onSetAutoLockMinutes: (Int) -> Unit = {},
    onSetHideAmounts: (Boolean) -> Unit = {},
    onSetBlockScreenshots: (Boolean) -> Unit = {},
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var showDisableConfirmDialog by remember { mutableStateOf(false) }
    var disablePinInput by remember { mutableStateOf("") }
    var disablePinError by remember { mutableStateOf<String?>(null) }
    var isVerifyingDisable by remember { mutableStateOf(false) }

    var autoLockMenuExpanded by remember { mutableStateOf(false) }


    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("App Lock & Privacy", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("app_lock_back_button")) {
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
            // Privacy Banner
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFFEFF6FF)
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = MoneyMapBlue, modifier = Modifier.size(36.dp))
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text("Your privacy matters", fontWeight = FontWeight.Bold, color = MoneyMapNavy)
                            Text("All your data is stored locally on your device. No bank logins, no cloud upload, no SMS access.", color = TextSecondary, fontSize = 12.sp)
                        }
                    }
                }
            }

            // App Lock Options
            item {
                Text("App Lock", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        // Enable App Lock Switch
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Enable App Lock", fontWeight = FontWeight.SemiBold)
                                Text("Require 6-digit PIN or biometrics to open MoneyMap", fontSize = 12.sp, color = TextSecondary)
                            }
                            Switch(
                                checked = preferences.appLockEnabled,
                                onCheckedChange = { checked ->
                                    if (checked) {
                                        onRequestStartPinSetup()
                                    } else {
                                        showDisableConfirmDialog = true
                                    }
                                },
                                modifier = Modifier.testTag("app_lock_enable_switch")
                            )
                        }

                        if (preferences.appLockEnabled) {
                            HorizontalDivider(color = BorderSubtle.copy(alpha = 0.5f))

                            // Change PIN
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(onClick = onChangePinRequested)
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Change PIN", fontWeight = FontWeight.SemiBold)
                                    Text("Update your 6-digit app lock PIN", fontSize = 12.sp, color = TextSecondary)
                                }
                                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.LightGray)
                            }

                            HorizontalDivider(color = BorderSubtle.copy(alpha = 0.5f))
                            HorizontalDivider(color = BorderSubtle.copy(alpha = 0.5f))
                            // Auto Lock Timeout
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Auto-Lock Timeout", fontWeight = FontWeight.SemiBold)
                                    Text("Require authentication after backgrounding", fontSize = 12.sp, color = TextSecondary)
                                }
                                Box {
                                    OutlinedButton(
                                        onClick = { autoLockMenuExpanded = true },
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.testTag("autolock_timeout_button")
                                    ) {
                                        Text(
                                            when (preferences.autoLockMinutes) {
                                                0 -> "Immediately"
                                                1 -> "After 1 min"
                                                5 -> "After 5 min"
                                                else -> "After 15 min"
                                            },
                                            fontSize = 12.sp
                                        )
                                    }
                                    DropdownMenu(
                                        expanded = autoLockMenuExpanded,
                                        onDismissRequest = { autoLockMenuExpanded = false }
                                    ) {
                                        listOf(
                                            0 to "Immediately",
                                            1 to "After 1 minute",
                                            5 to "After 5 minutes",
                                            15 to "After 15 minutes"
                                        ).forEach { (mins, label) ->
                                            DropdownMenuItem(
                                                text = { Text(label) },
                                                onClick = {
                                                    onSetAutoLockMinutes(mins)
                                                    autoLockMenuExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Privacy Options
            item {
                Text("Privacy Options", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Hide Amounts", fontWeight = FontWeight.SemiBold)
                                Text("Show ••••• instead of real amounts", fontSize = 12.sp, color = TextSecondary)
                            }
                            Switch(
                                checked = preferences.hideAmounts,
                                onCheckedChange = onSetHideAmounts
                            )
                        }

                        HorizontalDivider(color = BorderSubtle.copy(alpha = 0.5f))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Block Screenshots", fontWeight = FontWeight.SemiBold)
                                Text("Prevent screenshots and hide preview in recent apps", fontSize = 12.sp, color = TextSecondary)
                            }
                            Switch(
                                checked = preferences.blockScreenshots,
                                onCheckedChange = onSetBlockScreenshots
                            )
                        }
                    }
                }
            }

            // Security Highlights Card
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFFF8FAFC)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Data Security", fontWeight = FontWeight.Bold)
                        Text("✔ All data stored locally on your device", fontSize = 12.sp, color = TextSecondary)
                        Text("✔ Cryptographic PBKDF2 salted hash PIN verifier", fontSize = 12.sp, color = TextSecondary)
                        Text("✔ AndroidX BiometricPrompt system authentication", fontSize = 12.sp, color = TextSecondary)
                        Text("✔ Automatic lock enforcement when backgrounded", fontSize = 12.sp, color = TextSecondary)
                    }
                }
            }
        }
    }

    // Disable App Lock Confirmation Dialog
    if (showDisableConfirmDialog) {
        AlertDialog(
            onDismissRequest = {
                showDisableConfirmDialog = false
                disablePinInput = ""
                disablePinError = null
            },
            title = { Text("Disable App Lock?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Enter your current PIN to confirm disabling App Lock. MoneyMap will no longer require authentication to open.")

                    OutlinedTextField(
                        value = disablePinInput,
                        onValueChange = { if (it.length <= 6) disablePinInput = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Current 6-digit PIN") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (!disablePinError.isNullOrBlank()) {
                        Text(disablePinError!!, color = MoneyMapRed, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (disablePinInput.length == 6 && !isVerifyingDisable) {
                            isVerifyingDisable = true
                            val candidate = disablePinInput
                            coroutineScope.launch {
                                val isValid = onVerifyCurrentPinForDisable(candidate)
                                isVerifyingDisable = false
                                if (isValid) {
                                    onDisableAppLockConfirmed()
                                    showDisableConfirmDialog = false
                                    disablePinInput = ""
                                    disablePinError = null
                                } else {
                                    disablePinError = "Incorrect PIN. Try again."
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MoneyMapRed),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("confirm_disable_app_lock_button")
                ) {
                    Text("Disable Lock")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDisableConfirmDialog = false
                    disablePinInput = ""
                    disablePinError = null
                }) {
                    Text("Cancel")
                }
            }
        )
    }
}

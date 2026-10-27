package com.thecorporatedeveloper.moneymap.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thecorporatedeveloper.moneymap.core.model.AccountType
import com.thecorporatedeveloper.moneymap.core.model.CurrencyFormatter
import com.thecorporatedeveloper.moneymap.core.model.TransactionType
import com.thecorporatedeveloper.moneymap.data.entity.AccountEntity
import com.thecorporatedeveloper.moneymap.data.entity.CategoryEntity
import com.thecorporatedeveloper.moneymap.data.entity.TransactionEntity
import com.thecorporatedeveloper.moneymap.ui.components.TransactionItemRow
import com.thecorporatedeveloper.moneymap.ui.components.MoneySproutIllustration
import com.thecorporatedeveloper.moneymap.ui.components.PennyIllustration
import com.thecorporatedeveloper.moneymap.ui.components.getCategoryIcon
import com.thecorporatedeveloper.moneymap.ui.components.parseColorHex
import com.thecorporatedeveloper.moneymap.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditTransactionScreen(
    initialTransaction: TransactionEntity? = null,
    preselectedType: TransactionType? = null,
    accounts: List<AccountEntity>,
    categories: List<CategoryEntity>,
    onSave: suspend (TransactionEntity) -> Boolean,
    onNavigateToAddAccount: () -> Unit = {},
    onNavigateToCategories: () -> Unit = {},
    onBack: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var saveErrorMsg by remember { mutableStateOf<String?>(null) }
    var insufficientBalanceErrorMsg by remember { mutableStateOf<String?>(null) }
    var showPennySuccess by remember { mutableStateOf(false) }
    var type by remember { mutableStateOf(initialTransaction?.type ?: preselectedType ?: TransactionType.EXPENSE) }
    var amountInput by remember {
        mutableStateOf(if (initialTransaction != null) (initialTransaction.amountPaise / 100.0).toString() else "")
    }
    var note by remember { mutableStateOf(initialTransaction?.note ?: "") }
    var paymentMethod by remember { mutableStateOf(initialTransaction?.paymentMethod ?: "UPI") }

    var selectedEpochMillis by remember(initialTransaction) {
        mutableLongStateOf(initialTransaction?.dateEpochMillis ?: System.currentTimeMillis())
    }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    var selectedAccountId by remember {
        mutableStateOf(initialTransaction?.accountId ?: accounts.firstOrNull()?.id ?: 0L)
    }
    var selectedToAccountId by remember {
        mutableStateOf(initialTransaction?.toAccountId ?: accounts.getOrNull(1)?.id ?: 0L)
    }

    LaunchedEffect(accounts) {
        if (selectedAccountId == 0L || accounts.none { it.id == selectedAccountId }) {
            selectedAccountId = accounts.firstOrNull()?.id ?: 0L
        }
        if (selectedToAccountId == 0L || accounts.none { it.id == selectedToAccountId }) {
            selectedToAccountId = accounts.firstOrNull { it.id != selectedAccountId }?.id ?: 0L
        }
    }

    val filteredCategories = remember(categories, type, initialTransaction) {
        categories.filter {
            it.type == type && (!it.isArchived || it.id == initialTransaction?.categoryId) && (it.isFavorite || it.id == initialTransaction?.categoryId)
        }
    }
    var selectedCategoryId by remember {
        mutableStateOf(initialTransaction?.categoryId ?: filteredCategories.firstOrNull()?.id)
    }

    LaunchedEffect(type) {
        val cats = categories.filter {
            it.type == type && (!it.isArchived || it.id == initialTransaction?.categoryId) && (it.isFavorite || it.id == initialTransaction?.categoryId)
        }
        if (selectedCategoryId == null || cats.none { it.id == selectedCategoryId }) {
            selectedCategoryId = cats.firstOrNull()?.id
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (initialTransaction == null) "Add Transaction" else "Edit Transaction", fontWeight = FontWeight.Bold) },
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
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Type Selector
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    listOf(
                        TransactionType.EXPENSE to "↓ Expense",
                        TransactionType.INCOME to "↑ Income",
                        TransactionType.TRANSFER to "⇄ Transfer"
                    ).forEach { (t, label) ->
                        val isSel = type == t
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (isSel) {
                                        when (t) {
                                            TransactionType.EXPENSE -> MoneyMapRedLight
                                            TransactionType.INCOME -> MoneyMapGreenLight
                                            TransactionType.TRANSFER -> MoneyMapBlueLight
                                        }
                                    } else Color.Transparent
                                )
                                .clickable { type = t }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSel) {
                                    when (t) {
                                        TransactionType.EXPENSE -> MoneyMapRed
                                        TransactionType.INCOME -> MoneyMapGreen
                                        TransactionType.TRANSFER -> MoneyMapNavy
                                    }
                                } else Color.Gray,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }

            // Big Amount Input
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text("₹", fontSize = 36.sp, fontWeight = FontWeight.Bold, color = MoneyMapNavy)
                    Spacer(modifier = Modifier.width(6.dp))
                    OutlinedTextField(
                        value = amountInput,
                        onValueChange = { amountInput = it.filter { ch -> ch.isDigit() || ch == '.' } },
                        textStyle = LocalTextStyle.current.copy(
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        placeholder = { Text("0", fontSize = 36.sp, fontWeight = FontWeight.Bold) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .width(240.dp)
                            .testTag("amount_input_field"),
                        shape = RoundedCornerShape(14.dp)
                    )
                }
            }

            // Quick Amount Suggestion Pills
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    listOf("100", "500", "1,000", "2,000", "5,000").forEach { quick ->
                        val plainNum = quick.replace(",", "")
                        FilterChip(
                            selected = amountInput == plainNum,
                            onClick = { amountInput = plainNum },
                            label = { Text("₹$quick", fontSize = 12.sp) }
                        )
                    }
                }
            }

            // Account Validation Banner if no accounts exist
            if (accounts.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("no_accounts_warning_card"),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                "No Accounts Found",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Please add a bank, cash, or wallet account first to record transactions.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = onNavigateToAddAccount,
                                colors = ButtonDefaults.buttonColors(containerColor = MoneyMapNavy),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Add Account")
                            }
                        }
                    }
                }
            }

            // Category (for Expense and Income)
            if (type != TransactionType.TRANSFER) {
                item {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Category", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            TextButton(
                                onClick = onNavigateToCategories,
                                modifier = Modifier.testTag("manage_categories_button")
                            ) {
                                Text("Manage", fontSize = 12.sp, color = MoneyMapBlue)
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        if (filteredCategories.isEmpty()) {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable(onClick = onNavigateToCategories)
                                    .padding(vertical = 4.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        modifier = Modifier.weight(1f),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.StarBorder,
                                            contentDescription = null,
                                            tint = MoneyMapBlue,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            "No favourite ${type.name.lowercase()} categories yet.",
                                            color = MoneyMapNavy,
                                            fontSize = 13.sp
                                        )
                                    }
                                    TextButton(
                                        onClick = onNavigateToCategories,
                                        modifier = Modifier.testTag("empty_state_manage_categories_button")
                                    ) {
                                        Text("Manage Categories", fontSize = 12.sp, color = MoneyMapBlue)
                                    }
                                }
                            }
                        } else {
                            androidx.compose.foundation.lazy.LazyRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(filteredCategories, key = { it.id }) { cat ->
                                    val isSel = selectedCategoryId == cat.id
                                    val catCol = parseColorHex(cat.colorHex, MoneyMapBlue)
                                    FilterChip(
                                        selected = isSel,
                                        onClick = { selectedCategoryId = cat.id },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = getCategoryIcon(cat.iconName),
                                                contentDescription = null,
                                                tint = if (isSel) Color.White else catCol,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        },
                                        label = { Text(cat.name, fontSize = 12.sp) }
                                    )
                                }
                                item {
                                    AssistChip(
                                        onClick = onNavigateToCategories,
                                        leadingIcon = {
                                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                                        },
                                        label = { Text("+ Category", fontSize = 12.sp) }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Account Selection (From Account)
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(if (type == TransactionType.TRANSFER) "From Account" else "Account", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    androidx.compose.foundation.lazy.LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(accounts, key = { it.id }) { acc ->
                            val isSel = selectedAccountId == acc.id
                            FilterChip(
                                selected = isSel,
                                onClick = { selectedAccountId = acc.id },
                                label = { Text(acc.name, fontSize = 12.sp) }
                            )
                        }
                        item {
                            AssistChip(
                                onClick = onNavigateToAddAccount,
                                leadingIcon = { Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp)) },
                                label = { Text("+ Account", fontSize = 12.sp) }
                            )
                        }
                    }
                }
            }

            // To Account (for transfer)
            if (type == TransactionType.TRANSFER) {
                item {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text("To Account (Destination)", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        androidx.compose.foundation.lazy.LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val destinationCandidates = accounts.filter { it.id != selectedAccountId }
                            items(destinationCandidates, key = { it.id }) { acc ->
                                val isSel = selectedToAccountId == acc.id
                                FilterChip(
                                    selected = isSel,
                                    onClick = { selectedToAccountId = acc.id },
                                    label = { Text(acc.name, fontSize = 12.sp) }
                                )
                            }
                        }
                    }
                }
            }

            // Note (Optional)
            item {
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Note / Description (Optional)") },
                    placeholder = { Text("e.g. Dinner with friends, tax deduction, monthly bill") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("transaction_note_field"),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            // Date & Time Selector
            item {
                val dateFmt = remember { SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH) }
                val timeFmt = remember { SimpleDateFormat("h:mm a", Locale.ENGLISH) }

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp)),
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("Transaction Date & Time", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = dateFmt.format(Date(selectedEpochMillis)),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = MoneyMapNavy
                                )
                                Text(
                                    text = timeFmt.format(Date(selectedEpochMillis)),
                                    fontSize = 13.sp,
                                    color = TextSecondary
                                )
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = { showDatePicker = true },
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    modifier = Modifier.testTag("date_picker_button")
                                ) {
                                    Icon(Icons.Default.CalendarToday, contentDescription = "Select Date", modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Date", fontSize = 12.sp)
                                }

                                OutlinedButton(
                                    onClick = { showTimePicker = true },
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    modifier = Modifier.testTag("time_picker_button")
                                ) {
                                    Icon(Icons.Default.Schedule, contentDescription = "Select Time", modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Time", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }

            // Save Button
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        val paise = CurrencyFormatter.parseInputToPaise(amountInput)
                        if (paise <= 0L) return@Button
                        if (accounts.isEmpty() || selectedAccountId == 0L) return@Button

                        val selectedCat = categories.find { it.id == selectedCategoryId }
                        val defaultTitle = when (type) {
                            TransactionType.TRANSFER -> "Transfer"
                            TransactionType.INCOME -> selectedCat?.name ?: "Income"
                            TransactionType.EXPENSE -> selectedCat?.name ?: "Expense"
                        }

                        val tx = (initialTransaction ?: TransactionEntity(
                            type = type,
                            amountPaise = paise,
                            accountId = selectedAccountId,
                            dateEpochMillis = selectedEpochMillis,
                            merchantOrPayee = defaultTitle
                        )).copy(
                            type = type,
                            amountPaise = paise,
                            categoryId = if (type == TransactionType.TRANSFER) null else selectedCategoryId,
                            accountId = selectedAccountId,
                            toAccountId = if (type == TransactionType.TRANSFER) selectedToAccountId else null,
                            merchantOrPayee = defaultTitle,
                            note = note,
                            paymentMethod = paymentMethod,
                            dateEpochMillis = selectedEpochMillis
                        )
                        coroutineScope.launch {
                            try {
                                val success = onSave(tx)
                                if (success) {
                                    showPennySuccess = true
                                    kotlinx.coroutines.delay(700)
                                    onBack()
                                } else {
                                    saveErrorMsg = "Your transaction was not saved. Your entered details are still available to retry."
                                }
                            } catch (e: Exception) {
                                val msg = e.message ?: ""
                                if (msg.contains("Insufficient balance")) {
                                    insufficientBalanceErrorMsg = msg
                                } else {
                                    saveErrorMsg = if (com.thecorporatedeveloper.moneymap.core.util.StorageSafety.isStorageOrDatabaseError(e)) {
                                        "Not enough storage to save this transaction. Free some device space and try again."
                                    } else {
                                        "Your transaction was not saved. Your entered details are still available to retry."
                                    }
                                }
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("save_transaction_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MoneyMapNavy),
                    enabled = accounts.isNotEmpty()
                ) {
                    Text("Save Transaction", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    if (showPennySuccess) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.3f)),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    PennyIllustration(size = 64.dp)
                    Text("Saved Successfully!", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MoneyMapNavy)
                }
            }
        }
    }

    if (insufficientBalanceErrorMsg != null) {
        AlertDialog(
            containerColor = MaterialTheme.colorScheme.surface,
            onDismissRequest = { insufficientBalanceErrorMsg = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = MoneyMapRed)
                    Text("Insufficient Balance", fontWeight = FontWeight.Bold)
                }
            },
            text = { Text(insufficientBalanceErrorMsg!!) },
            confirmButton = {
                TextButton(
                    onClick = { insufficientBalanceErrorMsg = null },
                    modifier = Modifier.testTag("insufficient_balance_change_account")
                ) {
                    Text("Change Account", fontWeight = FontWeight.Bold, color = MoneyMapNavy)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { insufficientBalanceErrorMsg = null },
                    modifier = Modifier.testTag("insufficient_balance_not_now")
                ) {
                    Text("Not Now", color = TextSecondary)
                }
            }
        )
    }

    if (saveErrorMsg != null) {
        AlertDialog(
            containerColor = MaterialTheme.colorScheme.surface,
            onDismissRequest = { saveErrorMsg = null },
            title = { Text("Save Failed", fontWeight = FontWeight.Bold) },
            text = { Text(saveErrorMsg!!) },
            confirmButton = {
                TextButton(
                    onClick = {
                        val errorMsg = saveErrorMsg
                        saveErrorMsg = null
                        coroutineScope.launch {
                            try {
                                val paise = CurrencyFormatter.parseInputToPaise(amountInput)
                                val selectedCat = categories.find { it.id == selectedCategoryId }
                                val defaultTitle = when (type) {
                                    TransactionType.TRANSFER -> "Transfer"
                                    TransactionType.INCOME -> selectedCat?.name ?: "Income"
                                    TransactionType.EXPENSE -> selectedCat?.name ?: "Expense"
                                }
                                val tx = (initialTransaction ?: TransactionEntity(
                                    type = type,
                                    amountPaise = paise,
                                    accountId = selectedAccountId,
                                    dateEpochMillis = selectedEpochMillis,
                                    merchantOrPayee = defaultTitle
                                )).copy(
                                    type = type,
                                    amountPaise = paise,
                                    categoryId = if (type == TransactionType.TRANSFER) null else selectedCategoryId,
                                    accountId = selectedAccountId,
                                    toAccountId = if (type == TransactionType.TRANSFER) selectedToAccountId else null,
                                    merchantOrPayee = defaultTitle,
                                    note = note,
                                    paymentMethod = paymentMethod,
                                    dateEpochMillis = selectedEpochMillis
                                )
                                val success = onSave(tx)
                                if (success) {
                                    onBack()
                                } else {
                                    saveErrorMsg = errorMsg
                                }
                            } catch (e: Exception) {
                                saveErrorMsg = errorMsg
                            }
                        }
                    },
                    modifier = Modifier.testTag("retry_save_button")
                ) {
                    Text("Retry", fontWeight = FontWeight.Bold, color = MoneyMapNavy)
                }
            },
            dismissButton = {
                TextButton(onClick = { saveErrorMsg = null }) {
                    Text("Not now")
                }
            }
        )
    }

    if (showDatePicker) {
        val initialUtc = remember(selectedEpochMillis) {
            val localCal = Calendar.getInstance().apply { timeInMillis = selectedEpochMillis }
            val utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                clear()
                set(localCal.get(Calendar.YEAR), localCal.get(Calendar.MONTH), localCal.get(Calendar.DAY_OF_MONTH))
            }
            utcCal.timeInMillis
        }
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialUtc)

        DatePickerDialog(
            colors = DatePickerDefaults.colors(containerColor = MaterialTheme.colorScheme.surface),
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { utcMillis ->
                            val utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                                timeInMillis = utcMillis
                            }
                            val year = utcCal.get(Calendar.YEAR)
                            val month = utcCal.get(Calendar.MONTH)
                            val day = utcCal.get(Calendar.DAY_OF_MONTH)

                            val updatedCal = Calendar.getInstance().apply {
                                timeInMillis = selectedEpochMillis
                                set(Calendar.YEAR, year)
                                set(Calendar.MONTH, month)
                                set(Calendar.DAY_OF_MONTH, day)
                            }
                            selectedEpochMillis = updatedCal.timeInMillis
                        }
                        showDatePicker = false
                    },
                    modifier = Modifier.testTag("date_picker_confirm_button")
                ) {
                    Text("OK", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showTimePicker) {
        val localCal = remember(selectedEpochMillis) {
            Calendar.getInstance().apply { timeInMillis = selectedEpochMillis }
        }
        val timePickerState = rememberTimePickerState(
            initialHour = localCal.get(Calendar.HOUR_OF_DAY),
            initialMinute = localCal.get(Calendar.MINUTE),
            is24Hour = false
        )

        AlertDialog(
            containerColor = MaterialTheme.colorScheme.surface,
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val updatedCal = Calendar.getInstance().apply {
                            timeInMillis = selectedEpochMillis
                            set(Calendar.HOUR_OF_DAY, timePickerState.hour)
                            set(Calendar.MINUTE, timePickerState.minute)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }
                        selectedEpochMillis = updatedCal.timeInMillis
                        showTimePicker = false
                    },
                    modifier = Modifier.testTag("time_picker_confirm_button")
                ) {
                    Text("OK", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text("Cancel")
                }
            },
            title = { Text("Select Time", fontSize = 16.sp, fontWeight = FontWeight.Bold) },
            text = {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    TimePicker(state = timePickerState)
                }
            }
        )
    }
}

@Composable
fun TransactionHistoryScreen(
    transactions: List<TransactionEntity>,
    categories: List<CategoryEntity>,
    accounts: List<AccountEntity>,
    hideAmounts: Boolean,
    isDataLoaded: Boolean = true,
    onNavigateToDetails: (Long) -> Unit,
    onNavigateToAdd: () -> Unit,
    onNavigateToFavorites: () -> Unit = {},
    onToggleFavorite: ((TransactionEntity) -> Unit)? = null
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedTypeFilter by remember { mutableStateOf<TransactionType?>(null) }

    val categoryMap = remember(categories) { categories.associateBy { it.id } }
    val accountMap = remember(accounts) { accounts.associateBy { it.id } }

    val filteredList = remember(transactions, searchQuery, selectedTypeFilter, categoryMap, accountMap) {
        transactions.filter { tx ->
            val matchesType = selectedTypeFilter == null || tx.type == selectedTypeFilter
            val matchesQuery = if (searchQuery.isBlank()) {
                true
            } else {
                val q = searchQuery.trim().lowercase(Locale.ENGLISH)
                val matchesMerchant = tx.merchantOrPayee.lowercase(Locale.ENGLISH).contains(q)
                val matchesNote = tx.note.lowercase(Locale.ENGLISH).contains(q)
                val catName = categoryMap[tx.categoryId]?.name?.lowercase(Locale.ENGLISH) ?: ""
                val matchesCat = catName.contains(q)
                val accName = accountMap[tx.accountId]?.name?.lowercase(Locale.ENGLISH) ?: ""
                val matchesAcc = accName.contains(q)

                val cleanQ = q.replace("₹", "").replace("rs", "").replace(",", "").replace(" ", "").trim()
                val qAmount = cleanQ.toDoubleOrNull()
                val matchesAmount = if (qAmount != null) {
                    val txRupees = tx.amountPaise / 100.0
                    kotlin.math.abs(txRupees - qAmount) < 0.01 || tx.amountPaise.toString().contains(cleanQ)
                } else {
                    tx.amountPaise.toString().contains(cleanQ) || (tx.amountPaise / 100.0).toString().contains(cleanQ)
                }

                val txDate = Date(tx.dateEpochMillis)
                val fmt1 = SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH).format(txDate).lowercase(Locale.ENGLISH)
                val fmt2 = SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH).format(txDate)
                val fmt3 = SimpleDateFormat("dd-MM-yyyy", Locale.ENGLISH).format(txDate)
                val fmt4 = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).format(txDate)
                val matchesDate = fmt1.contains(q) || fmt2.contains(q) || fmt3.contains(q) || fmt4.contains(q)

                matchesMerchant || matchesNote || matchesCat || matchesAcc || matchesAmount || matchesDate
            }
            matchesType && matchesQuery
        }
    }

    data class TransactionDayGroup(
        val headingText: String,
        val transactions: List<TransactionEntity>,
        val dayNetPaise: Long
    )

    val groupedGroups = remember(filteredList) {
        val today = LocalDate.now()
        val yesterday = today.minusDays(1)
        val dateFormatter = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH)

        filteredList
            .sortedByDescending { it.dateEpochMillis }
            .groupBy { tx ->
                Instant.ofEpochMilli(tx.dateEpochMillis)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate()
            }
            .toSortedMap(comparator = Comparator.reverseOrder())
            .map { (localDate, txs) ->
                val formattedDate = localDate.format(dateFormatter)
                val heading = when (localDate) {
                    today -> "Today — $formattedDate"
                    yesterday -> "Yesterday — $formattedDate"
                    else -> formattedDate
                }
                val expenseSum = txs.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amountPaise }
                val incomeSum = txs.filter { it.type == TransactionType.INCOME }.sumOf { it.amountPaise }
                val net = incomeSum - expenseSum
                TransactionDayGroup(
                    headingText = heading,
                    transactions = txs.sortedByDescending { it.dateEpochMillis },
                    dayNetPaise = if (expenseSum > 0 && incomeSum == 0L) -expenseSum else net
                )
            }
    }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Transaction History",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )

                    IconButton(
                        onClick = onNavigateToFavorites,
                        modifier = Modifier.testTag("history_favorites_button")
                    ) {
                        Icon(Icons.Default.Star, contentDescription = "Favorites", tint = Color(0xFFFFB800))
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search a Transaction") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear search")
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("history_search_field"),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Filter Pills: All, Income, Expense, Transfer
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedTypeFilter == null,
                        onClick = { selectedTypeFilter = null },
                        label = { Text("All", fontSize = 12.sp) }
                    )
                    FilterChip(
                        selected = selectedTypeFilter == TransactionType.EXPENSE,
                        onClick = { selectedTypeFilter = TransactionType.EXPENSE },
                        label = { Text("Expenses", fontSize = 12.sp) }
                    )
                    FilterChip(
                        selected = selectedTypeFilter == TransactionType.INCOME,
                        onClick = { selectedTypeFilter = TransactionType.INCOME },
                        label = { Text("Income", fontSize = 12.sp) }
                    )
                    FilterChip(
                        selected = selectedTypeFilter == TransactionType.TRANSFER,
                        onClick = { selectedTypeFilter = TransactionType.TRANSFER },
                        label = { Text("Transfers", fontSize = 12.sp) }
                    )
                }
            }
        }
    ) { innerPadding ->
        if (!isDataLoaded) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MoneyMapBlue)
            }
        } else if (groupedGroups.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MoneySproutIllustration(size = 72.dp)
                    Text("No transactions found", color = TextSecondary, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 8.dp, bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                groupedGroups.forEach { group ->
                    item(key = "header_${group.headingText}") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = group.headingText,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = TextSecondary
                            )
                            if (group.dayNetPaise != 0L) {
                                val formattedTotal = CurrencyFormatter.formatRupees(
                                    paise = kotlin.math.abs(group.dayNetPaise),
                                    showSign = true,
                                    hideAmounts = hideAmounts
                                )
                                Text(
                                    text = if (group.dayNetPaise < 0) "-$formattedTotal" else "+$formattedTotal",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (group.dayNetPaise < 0) MoneyMapRed else MoneyMapGreen
                                )
                            }
                        }
                    }

                    items(group.transactions, key = { it.id }) { tx ->
                        val cat = categoryMap[tx.categoryId]
                        val acc = accountMap[tx.accountId]
                        TransactionItemRow(
                            transaction = tx,
                            category = cat,
                            accountName = acc?.name ?: "Account",
                            hideAmounts = hideAmounts,
                            onToggleFavorite = onToggleFavorite,
                            onClick = { onNavigateToDetails(tx.id) }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionDetailsScreen(
    transaction: TransactionEntity,
    category: CategoryEntity?,
    account: AccountEntity?,
    toAccount: AccountEntity?,
    hideAmounts: Boolean,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
    onToggleFavorite: () -> Unit = {}
) {
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    val catColor = category?.let { parseColorHex(it.colorHex, MoneyMapBlue) } ?: MoneyMapNavy
    val icon = category?.iconName?.let { getCategoryIcon(it) } ?: Icons.Default.Receipt
    val dateFmt = SimpleDateFormat("dd MMM yyyy • h:mm a", Locale.ENGLISH)
    val dateStr = dateFmt.format(Date(transaction.dateEpochMillis))

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Transaction Details", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = onToggleFavorite,
                        modifier = Modifier.testTag("details_favorite_button")
                    ) {
                        Icon(
                            imageVector = if (transaction.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = if (transaction.isFavorite) "Remove from Favorites" else "Mark as Favorite",
                            tint = if (transaction.isFavorite) Color(0xFFFFB800) else TextSecondary
                        )
                    }
                    IconButton(onClick = { showDeleteConfirmDialog = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MoneyMapRed)
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Hero Header Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = catColor.copy(alpha = 0.08f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(catColor.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(icon, contentDescription = null, tint = catColor, modifier = Modifier.size(28.dp))
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = transaction.merchantOrPayee,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = category?.name ?: transaction.type.displayName,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        val amtFormatted = CurrencyFormatter.formatRupees(
                            paise = transaction.amountPaise,
                            showSign = true,
                            hideAmounts = hideAmounts
                        )
                        Text(
                            text = if (transaction.type == TransactionType.EXPENSE) "-$amtFormatted" else amtFormatted,
                            style = MaterialTheme.typography.displayLarge,
                            fontWeight = FontWeight.Bold,
                            color = when (transaction.type) {
                                TransactionType.INCOME -> MoneyMapGreen
                                TransactionType.EXPENSE -> MoneyMapRed
                                TransactionType.TRANSFER -> MoneyMapBlue
                            }
                        )
                    }
                }

                // Attributes List
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        DetailRow("Account", account?.name ?: "Account")
                        if (transaction.type == TransactionType.TRANSFER && toAccount != null) {
                            DetailRow("Transferred To", toAccount.name)
                        }
                        DetailRow("Category", category?.name ?: "General")
                        DetailRow("Date & Time", dateStr)
                        if (transaction.paymentMethod.isNotBlank()) {
                            DetailRow("Payment Method", transaction.paymentMethod)
                        }
                        if (!transaction.referenceId.isNullOrBlank()) {
                            DetailRow("Reference ID", transaction.referenceId)
                        }
                        if (transaction.note.isNotBlank()) {
                            DetailRow("Note", transaction.note)
                        }
                    }
                }
            }

            // Bottom Actions: Edit, Duplicate
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = onEdit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MoneyMapNavy)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Edit Transaction", fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = onDuplicate,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Duplicate", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }

    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Delete Transaction?") },
            text = { Text("This will permanently remove this record and update your account balance.") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirmDialog = false
                    onDelete()
                }) {
                    Text("Delete", color = MoneyMapRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.model.AccountType
import com.example.core.model.CurrencyFormatter
import com.example.data.entity.AccountEntity
import com.example.data.entity.CategoryEntity
import com.example.data.entity.TransactionEntity
import com.example.domain.FinancialCalculator
import com.example.ui.components.TransactionItemRow
import com.example.ui.components.MoneySproutIllustration
import com.example.ui.components.getCategoryIcon
import com.example.ui.components.parseColorHex
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AccountsOverviewScreen(
    accounts: List<AccountEntity>,
    transactions: List<TransactionEntity>,
    hideAmounts: Boolean,
    isDataLoaded: Boolean = true,
    onNavigateToAddAccount: () -> Unit,
    onNavigateToAccountDetails: (Long) -> Unit,
    onNavigateToBudgets: () -> Unit
) {
    val netWorthPaise = remember(accounts, transactions) {
        FinancialCalculator.calculateNetWorth(accounts, transactions)
    }

    val savingsBalancePaise = remember(accounts, transactions) {
        FinancialCalculator.calculateSavingsBalance(accounts, transactions)
    }

    val savingsAccountsCount = remember(accounts) {
        accounts.count { it.isSavingsAccount }
    }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Accounts",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )

                IconButton(
                    onClick = onNavigateToAddAccount,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MoneyMapBlueLight)
                        .testTag("add_account_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Account", tint = MoneyMapBlue)
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Net Worth & Savings Hero Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(Color(0xFFDBEAFE), Color(0xFFBFDBFE))
                                )
                            )
                            .padding(20.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Net Worth",
                                    color = Color(0xFF1E3A8A),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )

                                if (accounts.isNotEmpty()) {
                                    Row(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(Color(0x3310B981))
                                            .padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.ArrowUpward,
                                            contentDescription = null,
                                            tint = Color(0xFF065F46),
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Spacer(modifier = Modifier.width(2.dp))
                                        Text(
                                            text = "+8%",
                                            color = Color(0xFF065F46),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = CurrencyFormatter.formatRupees(netWorthPaise, hideAmounts = hideAmounts),
                                color = Color(0xFF0F172A),
                                style = MaterialTheme.typography.displayLarge,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            // Decorative line chart wave matching reference
                            androidx.compose.foundation.Canvas(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(36.dp)
                            ) {
                                val width = size.width
                                val height = size.height
                                val path = androidx.compose.ui.graphics.Path().apply {
                                    moveTo(0f, height * 0.7f)
                                    cubicTo(width * 0.25f, height * 0.2f, width * 0.5f, height * 0.9f, width * 0.75f, height * 0.4f)
                                    cubicTo(width * 0.85f, height * 0.2f, width * 0.95f, height * 0.3f, width, height * 0.1f)
                                    lineTo(width, height)
                                    lineTo(0f, height)
                                    close()
                                }
                                drawPath(
                                    path = path,
                                    brush = Brush.verticalGradient(
                                        colors = listOf(Color(0x333B82F6), Color(0x003B82F6))
                                    )
                                )
                                val strokePath = androidx.compose.ui.graphics.Path().apply {
                                    moveTo(0f, height * 0.7f)
                                    cubicTo(width * 0.25f, height * 0.2f, width * 0.5f, height * 0.9f, width * 0.75f, height * 0.4f)
                                    cubicTo(width * 0.85f, height * 0.2f, width * 0.95f, height * 0.3f, width, height * 0.1f)
                                }
                                drawPath(
                                    path = strokePath,
                                    color = Color(0xFF2563EB),
                                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3.dp.toPx())
                                )
                                drawCircle(
                                    color = Color(0xFF10B981),
                                    radius = 5.dp.toPx(),
                                    center = androidx.compose.ui.geometry.Offset(width * 0.95f, height * 0.3f)
                                )
                            }

                            if (savingsAccountsCount > 0) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color.White.copy(alpha = 0.7f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                Icons.Default.Savings,
                                                contentDescription = null,
                                                tint = Color(0xFF047857),
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "Total Savings ($savingsAccountsCount account${if (savingsAccountsCount > 1) "s" else ""})",
                                                color = Color(0xFF1E3A8A),
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                        Text(
                                            text = CurrencyFormatter.formatRupees(savingsBalancePaise, hideAmounts = hideAmounts),
                                            color = Color(0xFF047857),
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Accounts List Header
            item {
                Text(
                    text = "Your Accounts",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            if (!isDataLoaded) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = MoneyMapBlue)
                    }
                }
            } else if (accounts.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("empty_accounts_card"),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            MoneySproutIllustration(size = 72.dp)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("No accounts yet", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                "Add your bank accounts, credit cards, or cash to start tracking your net worth.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = onNavigateToAddAccount,
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MoneyMapNavy)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Add Account", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Account Rows
            items(accounts, key = { it.id }) { acc ->
                val balancePaise = remember(acc, transactions) {
                    FinancialCalculator.calculateAccountBalance(acc, transactions)
                }
                val accColor = parseColorHex(acc.colorHex, MoneyMapBlue)
                val icon = getCategoryIcon(acc.iconName)

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .clickable { onNavigateToAccountDetails(acc.id) }
                        .testTag("account_card_${acc.id}"),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 1.dp,
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(CircleShape)
                                .background(accColor.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(icon, contentDescription = acc.name, tint = accColor, modifier = Modifier.size(24.dp))
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = acc.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                if (acc.isSavingsAccount) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = Color(0x3310B981)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                Icons.Default.Savings,
                                                contentDescription = null,
                                                tint = Color(0xFF047857),
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text(
                                                text = "Savings",
                                                color = Color(0xFF047857),
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            val typeLabel = if (acc.accountNumberMasked.isNotBlank()) {
                                "${acc.type.displayName} • ${acc.accountNumberMasked}"
                            } else {
                                acc.type.displayName
                            }
                            Text(
                                text = typeLabel,
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                        }

                        Text(
                            text = CurrencyFormatter.formatRupees(balancePaise, hideAmounts = hideAmounts),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (acc.type.isLiability) MoneyMapRed else TextPrimary
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.LightGray)
                    }
                }
            }

            // Budget Tip Banner
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .clickable(onClick = onNavigateToBudgets),
                    color = Color(0xFFF0FDF4),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Spa, contentDescription = null, tint = MoneyMapGreen, modifier = Modifier.size(32.dp))
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Save more, dream bigger!",
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF065F46)
                            )
                            Text(
                                text = "Set a monthly budget",
                                color = Color(0xFF166534),
                                fontSize = 12.sp
                            )
                        }
                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MoneyMapGreen)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountDetailsScreen(
    account: AccountEntity,
    transactions: List<TransactionEntity>,
    categories: List<CategoryEntity>,
    hideAmounts: Boolean,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onNavigateToAddMoney: () -> Unit,
    onNavigateToTransfer: () -> Unit,
    onNavigateToStatement: () -> Unit = {},
    onNavigateToTransactionDetails: (Long) -> Unit
) {
    val categoryMap = remember(categories) { categories.associateBy { it.id } }
    val balancePaise = remember(account, transactions) {
        FinancialCalculator.calculateAccountBalance(account, transactions)
    }

    val accTransactions = remember(transactions, account.id) {
        transactions.filter { it.accountId == account.id || it.toAccountId == account.id }
    }

    val accColor = parseColorHex(account.colorHex, MoneyMapBlue)
    val icon = getCategoryIcon(account.iconName)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(account.name, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(onClick = onEdit) {
                        Text("Edit", color = MoneyMapBlue, fontWeight = FontWeight.Bold)
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
            // Balance Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF))
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text("Current Balance", color = TextSecondary, fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = CurrencyFormatter.formatRupees(balancePaise, hideAmounts = hideAmounts),
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = MoneyMapNavy
                        )
                        if (account.isSavingsAccount) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0x3310B981))
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Savings, contentDescription = null, tint = Color(0xFF047857), modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Savings Account", color = Color(0xFF047857), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            // Quick Actions: Add Money, Transfer
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onNavigateToAddMoney,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = MoneyMapGreen, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Add Money", fontSize = 13.sp)
                    }

                    OutlinedButton(
                        onClick = onNavigateToTransfer,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.SwapHoriz, contentDescription = null, tint = MoneyMapBlue, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Transfer", fontSize = 13.sp)
                    }
                }
            }

            // Account Details Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Account Details", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Spacer(modifier = Modifier.height(12.dp))

                        DetailRow("Account Type", account.type.displayName)
                        DetailRow("Savings Account", if (account.isSavingsAccount) "Enabled (Tracked in Savings)" else "Disabled")
                        if (account.bankName.isNotBlank()) DetailRow("Bank Name", account.bankName)
                        if (account.accountNumberMasked.isNotBlank()) DetailRow("Account Number", account.accountNumberMasked)
                        if (account.ifscCode.isNotBlank()) DetailRow("IFSC Code", account.ifscCode)
                        if (account.accountHolder.isNotBlank()) DetailRow("Account Holder", account.accountHolder)
                        DetailRow("Opening Balance", CurrencyFormatter.formatRupees(account.openingBalancePaise))
                        DetailRow("Current Balance", CurrencyFormatter.formatRupees(balancePaise))
                    }
                }
            }

            // Transactions Header & List
            item {
                Text("Recent Transactions", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }

            if (accTransactions.isEmpty()) {
                item {
                    Text("No transactions for this account yet.", color = TextSecondary, fontSize = 13.sp)
                }
            } else {
                items(accTransactions, key = { it.id }) { tx ->
                    val cat = categoryMap[tx.categoryId]
                    TransactionItemRow(
                        transaction = tx,
                        category = cat,
                        accountName = account.name,
                        hideAmounts = hideAmounts,
                        onClick = { onNavigateToTransactionDetails(tx.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = TextSecondary, fontSize = 13.sp)
        Text(value, fontWeight = FontWeight.Medium, fontSize = 13.sp)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditAccountScreen(
    initialAccount: AccountEntity? = null,
    onSave: (AccountEntity) -> Unit,
    onDelete: ((AccountEntity) -> Unit)? = null,
    onBack: () -> Unit
) {
    var type by remember { mutableStateOf(initialAccount?.type ?: AccountType.BANK) }
    var isSavings by remember {
        mutableStateOf(initialAccount?.isSavings ?: (initialAccount?.type == AccountType.SAVINGS))
    }
    var name by remember { mutableStateOf(initialAccount?.name ?: "") }
    var bankName by remember { mutableStateOf(initialAccount?.bankName ?: "HDFC Bank") }
    var accountNumber by remember { mutableStateOf(initialAccount?.accountNumberMasked ?: "") }
    var ifscCode by remember { mutableStateOf(initialAccount?.ifscCode ?: "") }
    var openingBalanceInput by remember {
        mutableStateOf(if (initialAccount != null) (initialAccount.openingBalancePaise / 100).toString() else "0")
    }
    var selectedColor by remember { mutableStateOf(initialAccount?.colorHex ?: "#1E56F0") }
    var selectedIcon by remember { mutableStateOf(initialAccount?.iconName ?: "bank") }

    val colorsList = listOf("#1E56F0", "#10B981", "#EF4444", "#F59E0B", "#8B5CF6", "#06B6D4", "#F43F5E")
    val iconsList = listOf("bank", "payments", "wallet", "credit_card")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (initialAccount == null) "Add Account" else "Edit Account", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (initialAccount != null && onDelete != null) {
                        IconButton(onClick = { onDelete(initialAccount) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MoneyMapRed)
                        }
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
            // Account Type Selector
            item {
                Column {
                    Text(
                        text = "Account Type",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AccountType.entries.forEach { at ->
                            val isSel = type == at
                            FilterChip(
                                selected = isSel,
                                onClick = {
                                    type = at
                                    if (at == AccountType.SAVINGS) {
                                        isSavings = true
                                    }
                                    if (name.isBlank() || AccountType.entries.any { name == it.displayName }) {
                                        name = when (at) {
                                            AccountType.BANK -> "Bank Account"
                                            AccountType.CASH -> "Cash Wallet"
                                            AccountType.WALLET -> "UPI Wallet"
                                            AccountType.CREDIT_CARD -> "Credit Card"
                                            AccountType.SAVINGS -> "Savings Account"
                                        }
                                    }
                                },
                                label = { Text(at.displayName, fontSize = 12.sp) }
                            )
                        }
                    }
                }
            }

            // Name
            item {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Account Name") },
                    placeholder = { Text("e.g. HDFC Bank, Emergency Fund, Cash") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            // Bank Name (if BANK, SAVINGS, or CREDIT_CARD)
            if (type == AccountType.BANK || type == AccountType.SAVINGS || type == AccountType.CREDIT_CARD) {
                item {
                    OutlinedTextField(
                        value = bankName,
                        onValueChange = { bankName = it },
                        label = { Text("Bank / Institution Name") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                item {
                    OutlinedTextField(
                        value = accountNumber,
                        onValueChange = { accountNumber = it },
                        label = { Text("Account Number / Masked (Optional)") },
                        placeholder = { Text("e.g. **** 5678") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                item {
                    OutlinedTextField(
                        value = ifscCode,
                        onValueChange = { ifscCode = it },
                        label = { Text("IFSC Code (Optional)") },
                        placeholder = { Text("e.g. HDFC0001234") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            // Savings Account Classification Toggle (for non-credit-card accounts)
            if (!type.isLiability) {
                item {
                    val activeSavings = isSavings || type == AccountType.SAVINGS
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (activeSavings) MoneyMapBlue.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surfaceVariant
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.Savings,
                                        contentDescription = null,
                                        tint = if (activeSavings) MoneyMapBlue else TextSecondary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        "Mark as Savings Account",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    "Count towards your total savings balance (e.g. Emergency Fund, FD, Goal savings)",
                                    fontSize = 12.sp,
                                    color = TextSecondary
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Switch(
                                checked = activeSavings,
                                onCheckedChange = { checked ->
                                    if (type == AccountType.SAVINGS) {
                                        if (!checked) {
                                            type = AccountType.BANK
                                            isSavings = false
                                        }
                                    } else {
                                        isSavings = checked
                                    }
                                }
                            )
                        }
                    }
                }
            }

            // Opening Balance
            item {
                OutlinedTextField(
                    value = openingBalanceInput,
                    onValueChange = { openingBalanceInput = it.filter { ch -> ch.isDigit() || ch == '.' } },
                    label = { Text("Opening Balance (₹)") },
                    supportingText = { Text("Opening balance is initial capital, not an income transaction.") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            // Color Selector
            item {
                Text("Color & Accent", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    colorsList.forEach { hex ->
                        val col = parseColorHex(hex)
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(col)
                                .border(
                                    width = if (selectedColor == hex) 3.dp else 0.dp,
                                    color = if (selectedColor == hex) TextPrimary else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable { selectedColor = hex }
                        )
                    }
                }
            }

            // Save Button
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        val paise = CurrencyFormatter.parseInputToPaise(openingBalanceInput)
                        val effectiveIsSavings = (isSavings || type == AccountType.SAVINGS) && !type.isLiability
                        val account = (initialAccount ?: AccountEntity(
                            name = name.ifBlank { type.displayName },
                            type = type,
                            isSavings = effectiveIsSavings
                        )).copy(
                            name = name.ifBlank { type.displayName },
                            type = type,
                            isSavings = effectiveIsSavings,
                            bankName = bankName,
                            accountNumberMasked = accountNumber,
                            ifscCode = ifscCode,
                            openingBalancePaise = paise,
                            colorHex = selectedColor,
                            iconName = selectedIcon
                        )
                        onSave(account)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("save_account_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MoneyMapNavy)
                ) {
                    Text("Save Account", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

package com.thecorporatedeveloper.moneymap.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
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
import com.thecorporatedeveloper.moneymap.core.model.CurrencyFormatter
import com.thecorporatedeveloper.moneymap.core.model.TransactionType
import com.thecorporatedeveloper.moneymap.data.entity.AccountEntity
import com.thecorporatedeveloper.moneymap.data.entity.BudgetEntity
import com.thecorporatedeveloper.moneymap.data.entity.CategoryEntity
import com.thecorporatedeveloper.moneymap.data.entity.TransactionEntity
import com.thecorporatedeveloper.moneymap.domain.FinancialCalculator
import com.thecorporatedeveloper.moneymap.ui.components.*
import com.thecorporatedeveloper.moneymap.ui.theme.*
import androidx.compose.foundation.BorderStroke
import java.text.SimpleDateFormat
import java.util.Date
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun ReportsScreen(
    transactions: List<TransactionEntity>,
    categories: List<CategoryEntity>,
    hideAmounts: Boolean,
    firstDayOfWeek: String = "Monday",
    onNavigateToCategoryBreakdown: () -> Unit,
    onNavigateToCalendar: () -> Unit,
    onNavigateToCategoryTransactions: (categoryId: Long, startEpoch: Long, endEpoch: Long, periodLabel: String) -> Unit = { _, _, _, _ -> }
) {
    var selectedPeriodTab by remember { mutableStateOf("Month") }

    val categoryMap = remember(categories) { categories.associateBy { it.id } }

    val (reportStartEpoch, reportEndEpoch, reportPeriodLabel) = remember(selectedPeriodTab, firstDayOfWeek) {
        val now = LocalDate.now()
        val zone = ZoneId.systemDefault()
        when (selectedPeriodTab) {
            "Day" -> {
                val start = now.atStartOfDay(zone).toInstant().toEpochMilli()
                val end = now.atTime(23, 59, 59).atZone(zone).toInstant().toEpochMilli()
                Triple(start, end, "Today")
            }
            "Week" -> {
                val dayOfWeekTarget = when (firstDayOfWeek.uppercase()) {
                    "SUNDAY" -> java.time.DayOfWeek.SUNDAY
                    "SATURDAY" -> java.time.DayOfWeek.SATURDAY
                    else -> java.time.DayOfWeek.MONDAY
                }
                val startOfWeek = now.with(java.time.temporal.TemporalAdjusters.previousOrSame(dayOfWeekTarget))
                val start = startOfWeek.atStartOfDay(zone).toInstant().toEpochMilli()
                val end = startOfWeek.plusDays(6).atTime(23, 59, 59).atZone(zone).toInstant().toEpochMilli()
                Triple(start, end, "This Week")
            }
            "Month" -> {
                val ym = YearMonth.now()
                val start = ym.atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
                val end = ym.atEndOfMonth().atTime(23, 59, 59).atZone(zone).toInstant().toEpochMilli()
                Triple(start, end, ym.format(DateTimeFormatter.ofPattern("MMM yyyy", Locale.ENGLISH)))
            }
            "Year" -> {
                val start = now.withDayOfYear(1).atStartOfDay(zone).toInstant().toEpochMilli()
                val end = now.withDayOfYear(now.lengthOfYear()).atTime(23, 59, 59).atZone(zone).toInstant().toEpochMilli()
                Triple(start, end, "${now.year}")
            }
            else -> Triple(0L, Long.MAX_VALUE, "All Time")
        }
    }

    val filteredTransactions = remember(transactions, reportStartEpoch, reportEndEpoch) {
        transactions.filter { it.dateEpochMillis in reportStartEpoch..reportEndEpoch }
    }

    val periodSummary = remember(filteredTransactions) {
        FinancialCalculator.calculatePeriodSummary(filteredTransactions)
    }

    val categoryBreakdown = remember(filteredTransactions, categoryMap) {
        FinancialCalculator.calculateCategoryBreakdown(filteredTransactions, categoryMap)
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
                Text("Reports", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                IconButton(onClick = onNavigateToCalendar) {
                    Icon(Icons.Default.CalendarMonth, contentDescription = "Calendar", tint = MoneyMapBlue)
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Period Tabs: Day, Week, Month, Year
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(4.dp)
                ) {
                    listOf("Day", "Week", "Month", "Year").forEach { tab ->
                        val isSel = selectedPeriodTab == tab
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSel) MoneyMapBlue else Color.Transparent)
                                .clickable { selectedPeriodTab = tab }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = tab,
                                color = if (isSel) Color.White else TextSecondary,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }

            // Summary Tiles: Income, Expense, Savings
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MetricSummaryCard(
                        modifier = Modifier.weight(1f),
                        title = "Income",
                        amountPaise = periodSummary.totalIncomePaise,
                        color = MoneyMapGreen,
                        icon = Icons.Default.ArrowUpward,
                        hideAmounts = hideAmounts,
                        subtitle = null
                    )
                    MetricSummaryCard(
                        modifier = Modifier.weight(1f),
                        title = "Expenses",
                        amountPaise = periodSummary.totalExpensePaise,
                        color = MoneyMapRed,
                        icon = Icons.Default.ArrowDownward,
                        hideAmounts = hideAmounts,
                        subtitle = null
                    )
                    MetricSummaryCard(
                        modifier = Modifier.weight(1f),
                        title = "Net Flow",
                        amountPaise = periodSummary.netCashFlowPaise,
                        color = MoneyMapNavy,
                        icon = Icons.Default.AccountBalanceWallet,
                        hideAmounts = hideAmounts,
                        subtitle = null
                    )
                }
            }

            // Donut Chart & Spending Breakdown
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Spending Breakdown",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            modifier = Modifier.align(Alignment.Start)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        if (categoryBreakdown.isEmpty()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(60.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.surfaceVariant),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.PieChartOutline,
                                        contentDescription = null,
                                        tint = TextSecondary,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                Text("No Expense Data", fontWeight = FontWeight.Bold, color = TextPrimary)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    "No expenses recorded for this period.",
                                    fontSize = 12.sp,
                                    color = TextSecondary
                                )
                            }
                        } else {
                            Box(contentAlignment = Alignment.Center) {
                                val slices = categoryBreakdown.map {
                                    it.percentage.toFloat() to parseColorHex(it.colorHex, MoneyMapBlue)
                                }
                                DonutChart(slices = slices, modifier = Modifier.size(170.dp), strokeWidth = 24.dp)

                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Total Spent", fontSize = 11.sp, color = TextSecondary)
                                    Text(
                                        text = CurrencyFormatter.formatRupees(periodSummary.totalExpensePaise, hideAmounts = hideAmounts),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp,
                                        color = MoneyMapNavy
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Category List
                            categoryBreakdown.take(5).forEach { cat ->
                                val col = parseColorHex(cat.colorHex, MoneyMapBlue)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .clip(CircleShape)
                                                .background(col)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(cat.categoryName, fontSize = 13.sp)
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            CurrencyFormatter.formatRupees(cat.totalSpentPaise, hideAmounts = hideAmounts),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            String.format(Locale.ENGLISH, "%.0f%%", cat.percentage),
                                            fontSize = 11.sp,
                                            color = TextSecondary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Top Categories Progress Bars
            if (categoryBreakdown.isNotEmpty()) {
                item {
                    Text("Top Spending Categories", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }

                items(categoryBreakdown.take(4)) { cat ->
                    val col = parseColorHex(cat.colorHex, MoneyMapBlue)
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable {
                                cat.categoryId?.let { catId ->
                                    onNavigateToCategoryTransactions(catId, reportStartEpoch, reportEndEpoch, reportPeriodLabel)
                                }
                            }
                            .testTag("top_category_item_${cat.categoryId}"),
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, BorderSubtle.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(col.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = getCategoryIcon(cat.iconName),
                                            contentDescription = null,
                                            tint = col,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(cat.categoryName, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        CurrencyFormatter.formatRupees(cat.totalSpentPaise, hideAmounts = hideAmounts),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.Default.ChevronRight,
                                        contentDescription = "View category transactions",
                                        tint = TextSecondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            LinearProgressIndicator(
                                progress = { (cat.percentage / 100f).toFloat().coerceIn(0f, 1f) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = col,
                                trackColor = BorderSubtle
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    transactions: List<TransactionEntity>,
    categories: List<CategoryEntity>,
    accounts: List<AccountEntity> = emptyList(),
    hideAmounts: Boolean,
    onBack: () -> Unit,
    onNavigateToTransactionDetails: (Long) -> Unit
) {
    val currentMonth = remember { YearMonth.now() }
    val today = remember { LocalDate.now() }
    var selectedDay by remember { mutableIntStateOf(today.dayOfMonth) }

    val categoryMap = remember(categories) { categories.associateBy { it.id } }
    val accountMap = remember(accounts) { accounts.associateBy { it.id } }

    val daysOfWeek = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")

    val zone = ZoneId.systemDefault()
    val monthStart = currentMonth.atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
    val monthEnd = currentMonth.atEndOfMonth().atTime(23, 59, 59).atZone(zone).toInstant().toEpochMilli()

    val monthTransactions = remember(transactions, currentMonth) {
        transactions.filter { it.dateEpochMillis in monthStart..monthEnd }
    }

    val monthIncome = remember(monthTransactions) {
        monthTransactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amountPaise }
    }
    val monthExpense = remember(monthTransactions) {
        monthTransactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amountPaise }
    }
    val monthNet = monthIncome - monthExpense

    val selectedDayTransactions = remember(monthTransactions, selectedDay) {
        monthTransactions.filter { tx ->
            val date = Instant.ofEpochMilli(tx.dateEpochMillis).atZone(zone).toLocalDate()
            date.dayOfMonth == selectedDay
        }
    }

    val dayExpense = remember(selectedDayTransactions) {
        selectedDayTransactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amountPaise }
    }

    val daysWithTx = remember(monthTransactions) {
        monthTransactions.map {
            Instant.ofEpochMilli(it.dateEpochMillis).atZone(zone).toLocalDate().dayOfMonth
        }.toSet()
    }

    val daysInMonth = currentMonth.lengthOfMonth()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Calendar", fontWeight = FontWeight.Bold) },
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
            // Month Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        currentMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH)),
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
            }

            // Month Overview Badges (Real Database Calculations)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Income", color = TextSecondary, fontSize = 11.sp)
                        Text(
                            CurrencyFormatter.formatRupees(monthIncome, hideAmounts = hideAmounts),
                            fontWeight = FontWeight.Bold,
                            color = MoneyMapGreen,
                            fontSize = 15.sp
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Expense", color = TextSecondary, fontSize = 11.sp)
                        Text(
                            CurrencyFormatter.formatRupees(monthExpense, hideAmounts = hideAmounts),
                            fontWeight = FontWeight.Bold,
                            color = MoneyMapRed,
                            fontSize = 15.sp
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Net", color = TextSecondary, fontSize = 11.sp)
                        Text(
                            CurrencyFormatter.formatRupees(monthNet, hideAmounts = hideAmounts),
                            fontWeight = FontWeight.Bold,
                            color = if (monthNet >= 0) MoneyMapBlue else MoneyMapRed,
                            fontSize = 15.sp
                        )
                    }
                }
            }

            // Calendar Grid Days of Week
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    daysOfWeek.forEach { day ->
                        Text(day, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = TextSecondary)
                    }
                }
            }

            // Calendar Days Grid
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    val daysList = (1..daysInMonth).toList().chunked(7)
                    daysList.forEach { week ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            week.forEach { dayNumber ->
                                val isSelected = selectedDay == dayNumber
                                val hasTx = daysWithTx.contains(dayNumber)
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isSelected) MoneyMapBlue else Color.Transparent)
                                        .clickable { selectedDay = dayNumber }
                                        .padding(4.dp)
                                ) {
                                    Text(
                                        text = "$dayNumber",
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) Color.White else TextPrimary,
                                        fontSize = 13.sp
                                    )
                                    if (hasTx) {
                                        Box(
                                            modifier = Modifier
                                                .size(4.dp)
                                                .clip(CircleShape)
                                                .background(if (isSelected) Color.White else MoneyMapBlue)
                                        )
                                    }
                                }
                            }
                            if (week.size < 7) {
                                for (i in 0 until (7 - week.size)) {
                                    Spacer(modifier = Modifier.size(40.dp))
                                }
                            }
                        }
                    }
                }
            }

            // Transactions on Selected Day Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Transactions on $selectedDay ${currentMonth.month.name.lowercase().replaceFirstChar { it.uppercase() }}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    if (dayExpense > 0L) {
                        Text(
                            "-${CurrencyFormatter.formatRupees(dayExpense, hideAmounts = hideAmounts)}",
                            fontWeight = FontWeight.Bold,
                            color = MoneyMapRed,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            if (selectedDayTransactions.isEmpty()) {
                item {
                    Text(
                        "No transactions recorded for this day.",
                        color = TextSecondary,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                }
            } else {
                items(selectedDayTransactions, key = { it.id }) { tx ->
                    val cat = categoryMap[tx.categoryId]
                    val acc = accountMap[tx.accountId]
                    TransactionItemRow(
                        transaction = tx,
                        category = cat,
                        accountName = acc?.name ?: "Account",
                        hideAmounts = hideAmounts,
                        onClick = { onNavigateToTransactionDetails(tx.id) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetsScreen(
    budgets: List<BudgetEntity> = emptyList(),
    accounts: List<AccountEntity> = emptyList(),
    categories: List<CategoryEntity> = emptyList(),
    transactions: List<TransactionEntity> = emptyList(),
    hideAmounts: Boolean,
    onSaveBudget: (BudgetEntity) -> Unit = {},
    onUpdateBudget: (BudgetEntity) -> Unit = {},
    onDeleteBudget: (BudgetEntity) -> Unit = {},
    onBack: () -> Unit
) {
    var showAddEditDialog by remember { mutableStateOf(false) }
    var budgetToEdit by remember { mutableStateOf<BudgetEntity?>(null) }
    var budgetToDelete by remember { mutableStateOf<BudgetEntity?>(null) }

    val currentMonthKey = remember { YearMonth.now().toString() }

    // Account & Category maps
    val accountMap = remember(accounts) { accounts.associateBy { it.id } }
    val categoryMap = remember(categories) { categories.associateBy { it.id } }
    val expenseCategories = remember(categories) { categories.filter { it.type == TransactionType.EXPENSE && !it.isArchived } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Budget Management", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            budgetToEdit = null
                            showAddEditDialog = true
                        },
                        modifier = Modifier.testTag("add_budget_top_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add Budget", tint = MoneyMapBlue)
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
            if (budgets.isEmpty()) {
                // Empty State Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.Savings, contentDescription = null, tint = MoneyMapBlue, modifier = Modifier.size(52.dp))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("No Budgets Set", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                "Create monthly spending limits for all accounts, individual bank accounts, or specific expense categories.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(18.dp))
                            Button(
                                onClick = {
                                    budgetToEdit = null
                                    showAddEditDialog = true
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MoneyMapNavy),
                                modifier = Modifier.testTag("create_budget_empty_button")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("+ Create Budget")
                            }
                        }
                    }
                }
            } else {
                items(budgets, key = { it.id }) { b ->
                    val spendingResult = remember(transactions, b) {
                        FinancialCalculator.calculateBudgetSpending(b, transactions)
                    }

                    val limitPaise = spendingResult.limitPaise
                    val eligibleSpentPaise = spendingResult.spentPaise
                    val remainingPaise = spendingResult.remainingPaise
                    val usageFraction = spendingResult.usageFraction
                    val usagePct = spendingResult.usagePercentage
                    val isOverBudget = spendingResult.isOverBudget
                    val isApproaching = spendingResult.isApproaching
                    val status = spendingResult.status // "UPCOMING", "ACTIVE", "COMPLETED"

                    val progressColor = when {
                        isOverBudget -> MoneyMapRed
                        isApproaching -> Color(0xFFF59E0B) // Amber
                        else -> MoneyMapBlue
                    }

                    val scopeLabel = when (b.scope) {
                        "ACCOUNT" -> accountMap[b.accountId]?.name ?: "Bank Account"
                        "CATEGORY" -> categoryMap[b.categoryId]?.name ?: "Category"
                        "ACCOUNT_CATEGORY" -> "${accountMap[b.accountId]?.name ?: "Account"} • ${categoryMap[b.categoryId]?.name ?: "Category"}"
                        else -> "All Accounts"
                    }

                    val (statusBg, statusFg) = when (status) {
                        "ACTIVE" -> Color(0xFFDCFCE7) to Color(0xFF16A34A)
                        "UPCOMING" -> Color(0xFFFEF3C7) to Color(0xFFD97706)
                        else -> Color(0xFFF1F5F9) to Color(0xFF64748B)
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("budget_card_${b.id}"),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            // Header: Name + Status Badge & Scope Badge
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = b.name.ifBlank { "Monthly Budget" },
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        maxLines = 1
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = spendingResult.dateRangeLabel,
                                        fontSize = 12.sp,
                                        color = TextSecondary
                                    )
                                }

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Status Badge
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = statusBg
                                    ) {
                                        Text(
                                            text = status.lowercase().replaceFirstChar { it.uppercase() },
                                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = statusFg
                                        )
                                    }

                                    // Scope Badge
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MoneyMapBlue.copy(alpha = 0.12f)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = when (b.scope) {
                                                    "ACCOUNT" -> Icons.Default.AccountBalance
                                                    "CATEGORY" -> Icons.Default.Category
                                                    "ACCOUNT_CATEGORY" -> Icons.Default.Layers
                                                    else -> Icons.Default.Public
                                                },
                                                contentDescription = null,
                                                tint = MoneyMapBlue,
                                                modifier = Modifier.size(13.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = scopeLabel,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MoneyMapBlue
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Spent vs Limit Numbers
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Bottom
                            ) {
                                Column {
                                    Text("Spent", fontSize = 11.sp, color = TextSecondary)
                                    Text(
                                        text = CurrencyFormatter.formatRupees(eligibleSpentPaise, hideAmounts = hideAmounts),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 20.sp,
                                        color = if (isOverBudget) MoneyMapRed else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Text(
                                    text = "Limit: " + CurrencyFormatter.formatRupees(limitPaise, hideAmounts = hideAmounts),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = TextSecondary
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Progress Bar
                            LinearProgressIndicator(
                                progress = { usageFraction.coerceIn(0f, 1f) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = progressColor,
                                trackColor = BorderSubtle.copy(alpha = 0.6f)
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            // Status text + Actions Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (isOverBudget) {
                                        "Over budget by " + CurrencyFormatter.formatRupees(-remainingPaise, hideAmounts = hideAmounts) + " ($usagePct%)"
                                    } else {
                                        "Remaining: " + CurrencyFormatter.formatRupees(remainingPaise, hideAmounts = hideAmounts) + " ($usagePct%)"
                                    },
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isOverBudget) MoneyMapRed else if (isApproaching) Color(0xFFD97706) else MoneyMapGreen
                                )

                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    IconButton(
                                        onClick = {
                                            budgetToEdit = b
                                            showAddEditDialog = true
                                        },
                                        modifier = Modifier
                                            .size(32.dp)
                                            .testTag("edit_budget_button_${b.id}")
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = "Edit Budget", tint = MoneyMapBlue, modifier = Modifier.size(18.dp))
                                    }

                                    IconButton(
                                        onClick = { budgetToDelete = b },
                                        modifier = Modifier
                                            .size(32.dp)
                                            .testTag("delete_budget_button_${b.id}")
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete Budget", tint = MoneyMapRed, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Add / Edit Budget Dialog
    if (showAddEditDialog) {
        BudgetFormDialog(
            initialBudget = budgetToEdit,
            accounts = accounts,
            categories = expenseCategories,
            onDismiss = { showAddEditDialog = false },
            onConfirm = { budget ->
                if (budgetToEdit == null) {
                    onSaveBudget(budget)
                } else {
                    onUpdateBudget(budget)
                }
                showAddEditDialog = false
            }
        )
    }

    // Delete Confirmation Dialog
    if (budgetToDelete != null) {
        val target = budgetToDelete!!
        AlertDialog(
            onDismissRequest = { budgetToDelete = null },
            title = { Text("Delete Budget") },
            text = {
                Text("Are you sure you want to delete budget '${target.name.ifBlank { "Monthly Budget" }}'?\n\nYour saved transactions and account records will remain intact.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteBudget(target)
                        budgetToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MoneyMapRed),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("confirm_delete_budget_button")
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { budgetToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun BudgetFormDialog(
    initialBudget: BudgetEntity?,
    accounts: List<AccountEntity>,
    categories: List<CategoryEntity>,
    onDismiss: () -> Unit,
    onConfirm: (BudgetEntity) -> Unit
) {
    var nameInput by remember { mutableStateOf(initialBudget?.name ?: "Monthly Budget") }
    var limitInput by remember {
        mutableStateOf(
            if (initialBudget != null && initialBudget.effectiveLimitPaise > 0L)
                (initialBudget.effectiveLimitPaise / 100L).toString()
            else ""
        )
    }
    var selectedScope by remember { mutableStateOf(initialBudget?.scope ?: "ALL_ACCOUNTS") }
    var selectedAccountId by remember { mutableStateOf(initialBudget?.accountId ?: accounts.firstOrNull()?.id) }
    var selectedCategoryId by remember { mutableStateOf(initialBudget?.categoryId ?: categories.firstOrNull()?.id) }
    var selectedMonthYear by remember { mutableStateOf(initialBudget?.monthYear ?: YearMonth.now().toString()) }
    var startDateEpochMillis by remember {
        mutableStateOf(
            if (initialBudget != null && initialBudget.startDateEpochMillis > 0L) {
                initialBudget.startDateEpochMillis
            } else {
                val ym = try { YearMonth.parse(initialBudget?.monthYear ?: YearMonth.now().toString()) } catch (e: Exception) { YearMonth.now() }
                ym.atDay(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            }
        )
    }
    var endDateEpochMillis by remember {
        mutableStateOf(
            if (initialBudget != null && initialBudget.endDateEpochMillis > 0L) {
                initialBudget.endDateEpochMillis
            } else {
                val ym = try { YearMonth.parse(initialBudget?.monthYear ?: YearMonth.now().toString()) } catch (e: Exception) { YearMonth.now() }
                ym.atEndOfMonth().atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            }
        )
    }

    var accountExpanded by remember { mutableStateOf(false) }
    var categoryExpanded by remember { mutableStateOf(false) }
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }

    val dateFormatter = remember { DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH) }
    val startDateString = remember(startDateEpochMillis) {
        Instant.ofEpochMilli(startDateEpochMillis).atZone(ZoneId.systemDefault()).toLocalDate().format(dateFormatter)
    }
    val endDateString = remember(endDateEpochMillis) {
        Instant.ofEpochMilli(endDateEpochMillis).atZone(ZoneId.systemDefault()).toLocalDate().format(dateFormatter)
    }
    val isDateRangeValid = endDateEpochMillis >= startDateEpochMillis

    AlertDialog(
        containerColor = MaterialTheme.colorScheme.surface,
        onDismissRequest = onDismiss,
        title = { Text(if (initialBudget == null) "Create Budget" else "Edit Budget", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Name Field
                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it },
                    label = { Text("Budget Name") },
                    placeholder = { Text("e.g. Monthly Limit") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("budget_name_input")
                )

                // Limit Field
                OutlinedTextField(
                    value = limitInput,
                    onValueChange = { limitInput = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Spending Limit (₹)") },
                    placeholder = { Text("e.g. 20000") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("budget_limit_input")
                )

                // Budget Dates: Start & End Date Cards
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedCard(
                        onClick = { showStartDatePicker = true },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("budget_start_date_card")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Start Date", fontSize = 11.sp, color = TextSecondary)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(startDateString, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1)
                            }
                            Icon(Icons.Default.CalendarMonth, contentDescription = "Select Start Date", tint = MoneyMapBlue, modifier = Modifier.size(18.dp))
                        }
                    }

                    OutlinedCard(
                        onClick = { showEndDatePicker = true },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("budget_end_date_card")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("End Date", fontSize = 11.sp, color = TextSecondary)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(endDateString, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1)
                            }
                            Icon(Icons.Default.CalendarMonth, contentDescription = "Select End Date", tint = MoneyMapBlue, modifier = Modifier.size(18.dp))
                        }
                    }
                }

                if (!isDateRangeValid) {
                    Text(
                        text = "End date must be on or after start date",
                        color = MoneyMapRed,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Scope Selector Chips with FlowRow for wrapping
                Text("Budget Scope", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        "ALL_ACCOUNTS" to "All Accounts",
                        "ACCOUNT" to "Bank Account",
                        "CATEGORY" to "Category",
                        "ACCOUNT_CATEGORY" to "Bank + Category"
                    ).forEach { (scopeKey, label) ->
                        val isSel = selectedScope == scopeKey
                        FilterChip(
                            selected = isSel,
                            onClick = { selectedScope = scopeKey },
                            label = { Text(label, fontSize = 12.sp) }
                        )
                    }
                }

                // Account Selector Dropdown
                if (selectedScope == "ACCOUNT" || selectedScope == "ACCOUNT_CATEGORY") {
                    val selAcc = accounts.find { it.id == selectedAccountId }
                    OutlinedButton(
                        onClick = { accountExpanded = true },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Account: ${selAcc?.name ?: "Select Account"}", fontSize = 13.sp)
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                        }
                    }
                    DropdownMenu(
                        expanded = accountExpanded,
                        onDismissRequest = { accountExpanded = false }
                    ) {
                        accounts.forEach { acc ->
                            DropdownMenuItem(
                                text = { Text(acc.name) },
                                onClick = {
                                    selectedAccountId = acc.id
                                    accountExpanded = false
                                }
                            )
                        }
                    }
                }

                // Category Selector Dropdown
                if (selectedScope == "CATEGORY" || selectedScope == "ACCOUNT_CATEGORY") {
                    val selCat = categories.find { it.id == selectedCategoryId }
                    OutlinedButton(
                        onClick = { categoryExpanded = true },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Category: ${selCat?.name ?: "Select Category"}", fontSize = 13.sp)
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                        }
                    }
                    DropdownMenu(
                        expanded = categoryExpanded,
                        onDismissRequest = { categoryExpanded = false }
                    ) {
                        categories.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat.name) },
                                onClick = {
                                    selectedCategoryId = cat.id
                                    categoryExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            val parsedLimit = CurrencyFormatter.parseInputToPaise(limitInput)
            val canSave = parsedLimit > 0L && isDateRangeValid
            Button(
                onClick = {
                    if (canSave) {
                        val budget = BudgetEntity(
                            id = initialBudget?.id ?: 0L,
                            name = nameInput.ifBlank { "Monthly Budget" },
                            monthYear = selectedMonthYear,
                            period = "MONTHLY",
                            amountLimitPaise = parsedLimit,
                            scope = selectedScope,
                            accountId = if (selectedScope == "ACCOUNT" || selectedScope == "ACCOUNT_CATEGORY") selectedAccountId else null,
                            categoryId = if (selectedScope == "CATEGORY" || selectedScope == "ACCOUNT_CATEGORY") selectedCategoryId else null,
                            startDateEpochMillis = startDateEpochMillis,
                            endDateEpochMillis = endDateEpochMillis,
                            createdAt = initialBudget?.createdAt ?: System.currentTimeMillis(),
                            updatedAt = System.currentTimeMillis(),
                            totalLimitPaise = parsedLimit
                        )
                        onConfirm(budget)
                    }
                },
                enabled = canSave,
                colors = ButtonDefaults.buttonColors(containerColor = MoneyMapNavy),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("save_budget_dialog_button")
            ) {
                Text("Save Budget")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )

    if (showStartDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = startDateEpochMillis)
        DatePickerDialog(
            onDismissRequest = { showStartDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            startDateEpochMillis = millis
                        }
                        showStartDatePicker = false
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showStartDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showEndDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = endDateEpochMillis)
        DatePickerDialog(
            onDismissRequest = { showEndDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            endDateEpochMillis = millis
                        }
                        showEndDatePicker = false
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEndDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

/**
 * Filtered Category Transactions List Screen shown when tapping a category card in Reports (Top Spending Categories).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryTransactionsScreen(
    categoryId: Long,
    categoryName: String,
    periodLabel: String,
    startEpochMillis: Long,
    endEpochMillis: Long,
    transactions: List<TransactionEntity>,
    categories: List<CategoryEntity>,
    accounts: List<AccountEntity>,
    budgets: List<BudgetEntity> = emptyList(),
    hideAmounts: Boolean,
    onBack: () -> Unit,
    onNavigateToDetails: (Long) -> Unit,
    onNavigateToEditTransaction: ((Long) -> Unit)? = null,
    onDeleteTransaction: ((TransactionEntity) -> Unit)? = null
) {
    var isNewestFirst by remember { mutableStateOf(true) }
    var showSortMenu by remember { mutableStateOf(false) }
    var transactionToDelete by remember { mutableStateOf<TransactionEntity?>(null) }

    val category = remember(categories, categoryId) { categories.find { it.id == categoryId } }
    val catColor = category?.let { parseColorHex(it.colorHex, MoneyMapBlue) } ?: MoneyMapNavy
    val catIcon = category?.iconName?.let { getCategoryIcon(it) } ?: Icons.Default.Category
    val accountMap = remember(accounts) { accounts.associateBy { it.id } }

    val filteredTransactions = remember(transactions, categoryId, startEpochMillis, endEpochMillis) {
        transactions.filter { tx ->
            tx.categoryId == categoryId &&
            tx.type == TransactionType.EXPENSE &&
            tx.dateEpochMillis in startEpochMillis..endEpochMillis
        }
    }

    val sortedTransactions = remember(filteredTransactions, isNewestFirst) {
        if (isNewestFirst) {
            filteredTransactions.sortedByDescending { it.dateEpochMillis }
        } else {
            filteredTransactions.sortedBy { it.dateEpochMillis }
        }
    }

    val totalSpentPaise = remember(filteredTransactions) {
        filteredTransactions.sumOf { it.amountPaise }
    }

    val averageSpendPaise = remember(filteredTransactions, totalSpentPaise) {
        if (filteredTransactions.isNotEmpty()) totalSpentPaise / filteredTransactions.size else 0L
    }

    // Check budget for this category
    val matchingBudget = remember(budgets, categoryId) {
        budgets.find { b ->
            (b.scope == "CATEGORY" || b.scope == "ACCOUNT_CATEGORY") && b.categoryId == categoryId
        }
    }

    val budgetLimitPaise = matchingBudget?.amountLimitPaise ?: 0L
    val usagePercentage = if (budgetLimitPaise > 0) {
        ((totalSpentPaise.toDouble() / budgetLimitPaise.toDouble()) * 100).toInt().coerceIn(0, 100)
    } else {
        0
    }

    // Date grouping
    val groupedTransactions = remember(sortedTransactions) {
        val fmt = SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH)
        sortedTransactions.groupBy { fmt.format(Date(it.dateEpochMillis)) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "$categoryName Transactions",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("category_tx_back_btn")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MoneyMapBlue.copy(alpha = 0.12f),
                        modifier = Modifier.padding(end = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.CalendarMonth,
                                contentDescription = null,
                                tint = MoneyMapBlue,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = periodLabel,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MoneyMapBlue
                            )
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
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Hero Category Summary Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = catColor.copy(alpha = 0.08f)),
                    border = BorderStroke(1.dp, catColor.copy(alpha = 0.2f))
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        // Top row: Icon, Category name, Expenses tag
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(catColor.copy(alpha = 0.18f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = catIcon,
                                        contentDescription = null,
                                        tint = catColor,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = categoryName,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Expense Category",
                                        fontSize = 12.sp,
                                        color = TextSecondary
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MoneyMapRed.copy(alpha = 0.12f)
                            ) {
                                Text(
                                    text = "↓ Expenses",
                                    color = MoneyMapRed,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // 3 Metrics Row: Total Spent | Transactions | Average Spend
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Total Spent", fontSize = 11.sp, color = TextSecondary)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = CurrencyFormatter.formatRupees(totalSpentPaise, hideAmounts = hideAmounts),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = MoneyMapRed
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Transactions", fontSize = 11.sp, color = TextSecondary)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${filteredTransactions.size}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Average Spend", fontSize = 11.sp, color = TextSecondary)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = CurrencyFormatter.formatRupees(averageSpendPaise, hideAmounts = hideAmounts),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Progress Bar & Usage Label
                        val progressFraction = if (budgetLimitPaise > 0) {
                            (totalSpentPaise.toFloat() / budgetLimitPaise.toFloat()).coerceIn(0f, 1f)
                        } else {
                            1f
                        }
                        LinearProgressIndicator(
                            progress = { progressFraction },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = if (usagePercentage > 90) MoneyMapRed else catColor,
                            trackColor = BorderSubtle
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = if (budgetLimitPaise > 0) "$usagePercentage% used" else "Period Total",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                            Text(
                                text = if (budgetLimitPaise > 0) {
                                    "${CurrencyFormatter.formatRupees(totalSpentPaise, hideAmounts = hideAmounts)} / ${CurrencyFormatter.formatRupees(budgetLimitPaise, hideAmounts = hideAmounts)}"
                                } else {
                                    CurrencyFormatter.formatRupees(totalSpentPaise, hideAmounts = hideAmounts)
                                },
                                fontSize = 11.sp,
                                color = TextSecondary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // Transactions Header Row with Count & Sort Toggle
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Transactions (${filteredTransactions.size})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )

                    Box {
                        TextButton(
                            onClick = { showSortMenu = true },
                            modifier = Modifier.testTag("category_tx_sort_button")
                        ) {
                            Icon(
                                Icons.Default.SwapVert,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MoneyMapBlue
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isNewestFirst) "Newest First" else "Oldest First",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MoneyMapBlue
                            )
                        }

                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Newest First") },
                                leadingIcon = { Icon(Icons.Default.ArrowDownward, contentDescription = null) },
                                onClick = {
                                    isNewestFirst = true
                                    showSortMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Oldest First") },
                                leadingIcon = { Icon(Icons.Default.ArrowUpward, contentDescription = null) },
                                onClick = {
                                    isNewestFirst = false
                                    showSortMenu = false
                                }
                            )
                        }
                    }
                }
            }

            // Grouped Transaction List
            if (sortedTransactions.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.ReceiptLong,
                                contentDescription = null,
                                tint = TextSecondary,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "No Transactions Found",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "No $categoryName expense transactions recorded for $periodLabel.",
                                fontSize = 12.sp,
                                color = TextSecondary,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                groupedTransactions.forEach { (dateStr, txList) ->
                    val daySumPaise = txList.sumOf { it.amountPaise }
                    item(key = "header_$dateStr") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp, bottom = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = dateStr,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = CurrencyFormatter.formatRupees(daySumPaise, hideAmounts = hideAmounts),
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                        }
                    }

                    txList.forEach { tx ->
                        item(key = "tx_${tx.id}") {
                            val acc = accountMap[tx.accountId]
                            TransactionItemRow(
                                transaction = tx,
                                category = category,
                                accountName = acc?.name ?: "",
                                hideAmounts = hideAmounts,
                                onClick = { onNavigateToDetails(tx.id) }
                            )
                        }
                    }
                }
            }
        }
    }

    if (transactionToDelete != null && onDeleteTransaction != null) {
        DeleteTransactionConfirmationDialog(
            transaction = transactionToDelete,
            onDismiss = { transactionToDelete = null },
            onConfirm = { tx -> onDeleteTransaction(tx) }
        )
    }
}

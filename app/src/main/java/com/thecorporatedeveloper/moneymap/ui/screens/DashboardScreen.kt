package com.thecorporatedeveloper.moneymap.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thecorporatedeveloper.moneymap.core.model.CurrencyFormatter
import com.thecorporatedeveloper.moneymap.data.entity.AccountEntity
import com.thecorporatedeveloper.moneymap.data.entity.CategoryEntity
import com.thecorporatedeveloper.moneymap.data.entity.TransactionEntity
import com.thecorporatedeveloper.moneymap.domain.FinancialCalculator
import com.thecorporatedeveloper.moneymap.ui.components.CircularProgressRing
import com.thecorporatedeveloper.moneymap.ui.components.MetricSummaryCard
import com.thecorporatedeveloper.moneymap.data.entity.BudgetEntity
import com.thecorporatedeveloper.moneymap.ui.components.TransactionItemRow
import com.thecorporatedeveloper.moneymap.ui.components.MoneySproutIllustration
import com.thecorporatedeveloper.moneymap.ui.theme.*
import java.time.YearMonth

@Composable
fun DashboardScreen(
    accounts: List<AccountEntity>,
    categories: List<CategoryEntity>,
    transactions: List<TransactionEntity>,
    budgets: List<BudgetEntity> = emptyList(),
    userName: String = "",
    hideAmounts: Boolean,
    isDataLoaded: Boolean = true,
    onToggleHideAmounts: () -> Unit,
    onNavigateToAddTransaction: () -> Unit,
    onNavigateToAddAccount: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToTransactionDetails: (Long) -> Unit,
    onNavigateToBudgets: () -> Unit,
    onNavigateToSmartEntry: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val categoryMap = remember(categories) { categories.associateBy { it.id } }
    val accountMap = remember(accounts) { accounts.associateBy { it.id } }

    val totalBalancePaise = remember(accounts, transactions) {
        FinancialCalculator.calculateTotalAssetBalance(accounts, transactions)
    }

    val periodSummary = remember(transactions) {
        FinancialCalculator.calculatePeriodSummary(transactions)
    }

    val savingsBalancePaise = remember(accounts, transactions) {
        FinancialCalculator.calculateSavingsBalance(accounts, transactions)
    }

    val recentTransactions = remember(transactions) {
        transactions.take(5)
    }

    val currentMonthBudget = remember(budgets) {
        val currentMonthKey = YearMonth.now().toString()
        budgets.find { it.monthYear == currentMonthKey }
    }

    Scaffold { innerPadding ->
        val currentHour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        val (greetingText, greetingEmoji) = when (currentHour) {
            in 5..11 -> "Good morning" to "☀️"
            in 12..16 -> "Good afternoon" to "🌤️"
            in 17..21 -> "Good evening" to "🌆"
            else -> "Good night" to "🌙"
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Header: Greeting & Action Icons
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = greetingText,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = MoneyMapNavy
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = greetingEmoji, fontSize = 20.sp)
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Have a great day!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconButton(
                            onClick = onNavigateToSmartEntry,
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surface)
                                .testTag("smart_entry_icon_button")
                        ) {
                            Icon(Icons.Default.QrCodeScanner, contentDescription = "Scan Screenshot", tint = MoneyMapNavy)
                        }

                        IconButton(
                            onClick = onNavigateToSettings,
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surface)
                                .testTag("dashboard_settings_button")
                        ) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings", tint = MoneyMapNavy)
                        }
                    }
                }
            }

            // If data is loading or no accounts exist yet
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
                            .testTag("add_first_account_card"),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(CircleShape)
                                    .background(MoneyMapBlueLight),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.AccountBalance,
                                    contentDescription = null,
                                    tint = MoneyMapBlue,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Welcome to MoneyMap",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MoneyMapNavy
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Add your first bank account, cash wallet, or UPI account to begin managing your finances.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Button(
                                onClick = onNavigateToAddAccount,
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MoneyMapNavy),
                                modifier = Modifier.testTag("add_first_account_button")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Add Your First Account", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // 2. Hero Total Balance Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("total_balance_card"),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(Color(0xFF0F2B48), Color(0xFF1B4965))
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
                                    text = "Total Balance",
                                    color = Color(0xFF94A3B8),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                IconButton(
                                    onClick = onToggleHideAmounts,
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = if (hideAmounts) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = "Toggle Balance",
                                        tint = Color.White
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = CurrencyFormatter.formatRupees(totalBalancePaise, hideAmounts = hideAmounts),
                                color = Color.White,
                                style = MaterialTheme.typography.displayLarge,
                                fontWeight = FontWeight.Bold
                            )

                            if (accounts.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(Color(0x3310B981))
                                        .padding(horizontal = 10.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.AccountBalance,
                                        contentDescription = null,
                                        tint = MoneyMapGreen,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "${accounts.size} active account${if (accounts.size > 1) "s" else ""}",
                                        color = MoneyMapGreen,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 3. Metric Summary Cards: Income, Expense, Savings (real figures, no fake percentages)
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
                        title = "Savings Balance",
                        amountPaise = savingsBalancePaise,
                        color = MoneyMapPurple,
                        icon = Icons.Default.Savings,
                        hideAmounts = hideAmounts,
                        subtitle = null
                    )
                }
            }

            // Optional Net Cash Flow indicator (Income - Expenses in period)
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp)),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 1.dp,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.SwapVert,
                                contentDescription = null,
                                tint = MoneyMapNavy,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Net Cash Flow",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = TextSecondary
                            )
                        }
                        Text(
                            text = CurrencyFormatter.formatRupees(
                                paise = periodSummary.netCashFlowPaise,
                                showSign = true,
                                hideAmounts = hideAmounts
                            ),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (periodSummary.netCashFlowPaise >= 0) MoneyMapGreen else MoneyMapRed
                        )
                    }
                }
            }

            // 4. Spending Goal Progress Card (real budget calculation)
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .clickable(onClick = onNavigateToBudgets),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 2.dp,
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (currentMonthBudget != null && currentMonthBudget.effectiveLimitPaise > 0L) {
                            val ratio = (periodSummary.totalExpensePaise.toFloat() / currentMonthBudget.effectiveLimitPaise).coerceIn(0f, 1f)
                            val pct = (ratio * 100).toInt()
                            val progressCol = if (ratio > 0.9f) MoneyMapRed else if (ratio > 0.75f) MoneyMapOrange else MoneyMapGreen

                            CircularProgressRing(
                                percentage = ratio,
                                modifier = Modifier.size(54.dp),
                                progressColor = progressCol,
                                strokeWidth = 6.dp
                            )

                            Spacer(modifier = Modifier.width(16.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (ratio >= 1f) "Budget Exceeded" else "Monthly Spending Budget",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "$pct% used (${CurrencyFormatter.formatRupees(periodSummary.totalExpensePaise, hideAmounts)} / ${CurrencyFormatter.formatRupees(currentMonthBudget.effectiveLimitPaise, hideAmounts)})",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary
                                )
                            }
                        } else {
                            CircularProgressRing(
                                percentage = 0f,
                                modifier = Modifier.size(54.dp),
                                progressColor = MoneyMapBlue,
                                strokeWidth = 6.dp
                            )

                            Spacer(modifier = Modifier.width(16.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Monthly Budget",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Tap to set monthly spending limits",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))
                        MoneySproutIllustration(size = 38.dp)

                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            Icons.Default.ChevronRight,
                            contentDescription = "View Budgets",
                            tint = TextSecondary
                        )
                    }
                }
            }

            // 5. Recent Transactions Header & List
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recent Transactions",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    TextButton(onClick = onNavigateToHistory) {
                        Text("See All", color = MoneyMapBlue, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            if (recentTransactions.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No transactions yet. Tap + to add one!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                }
            } else {
                items(recentTransactions, key = { it.id }) { tx ->
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

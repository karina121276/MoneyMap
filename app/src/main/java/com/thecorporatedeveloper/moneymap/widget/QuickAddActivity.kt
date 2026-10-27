package com.thecorporatedeveloper.moneymap.widget

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.thecorporatedeveloper.moneymap.core.model.AccountType
import com.thecorporatedeveloper.moneymap.core.model.CurrencyFormatter
import com.thecorporatedeveloper.moneymap.core.model.TransactionType
import com.thecorporatedeveloper.moneymap.data.database.MoneyMapDatabase
import com.thecorporatedeveloper.moneymap.data.entity.AccountEntity
import com.thecorporatedeveloper.moneymap.data.entity.CategoryEntity
import com.thecorporatedeveloper.moneymap.data.entity.TransactionEntity
import com.thecorporatedeveloper.moneymap.data.repository.MoneyMapRepository
import com.thecorporatedeveloper.moneymap.ui.theme.MoneyMapTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class QuickAddActivity : ComponentActivity() {

    private lateinit var repository: MoneyMapRepository

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val db = MoneyMapDatabase.getDatabase(this)
        repository = MoneyMapRepository(db, this)

        setContent {
            MoneyMapTheme {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = {
                                Text(
                                    "Quick Add from Widget",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            },
                            navigationIcon = {
                                IconButton(onClick = { finish() }) {
                                    Icon(Icons.Default.Close, contentDescription = "Close")
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        )
                    }
                ) { innerPadding ->
                    QuickAddContent(
                        modifier = Modifier.padding(innerPadding),
                        repository = repository,
                        onSaved = {
                            Toast.makeText(this@QuickAddActivity, "Transaction saved!", Toast.LENGTH_SHORT).show()
                            finish()
                        },
                        onCancel = { finish() }
                    )
                }
            }
        }
    }
}

@Composable
fun QuickAddContent(
    modifier: Modifier = Modifier,
    repository: MoneyMapRepository,
    onSaved: () -> Unit,
    onCancel: () -> Unit
) {
    var txType by remember { mutableStateOf(TransactionType.EXPENSE) }
    var amountInput by remember { mutableStateOf("") }
    var noteInput by remember { mutableStateOf("") }
    var payeeInput by remember { mutableStateOf("") }

    val accounts by repository.allAccounts.collectAsState(initial = emptyList())
    val categories by repository.allCategories.collectAsState(initial = emptyList())

    var selectedAccountId by remember { mutableStateOf<Long?>(null) }
    var selectedCategoryId by remember { mutableStateOf<Long?>(null) }

    LaunchedEffect(accounts) {
        if (selectedAccountId == null && accounts.isNotEmpty()) {
            selectedAccountId = accounts.first().id
        }
    }

    LaunchedEffect(categories, txType) {
        val matching = categories.filter { it.type == txType }
        if (matching.isNotEmpty()) {
            selectedCategoryId = matching.first().id
        }
    }

    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Type Selector: Expense, Income, Transfer
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            listOf(
                TransactionType.EXPENSE to "↓ Expense",
                TransactionType.INCOME to "↑ Income",
                TransactionType.TRANSFER to "⇄ Transfer"
            ).forEach { (type, label) ->
                val isSelected = txType == type
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (isSelected) {
                                when (type) {
                                    TransactionType.EXPENSE -> Color(0xFFFEE2E2)
                                    TransactionType.INCOME -> Color(0xFFDCFCE7)
                                    TransactionType.TRANSFER -> Color(0xFFE0E7FF)
                                }
                            } else Color.Transparent
                        )
                        .clickable { txType = type }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) {
                            when (type) {
                                TransactionType.EXPENSE -> Color(0xFFDC2626)
                                TransactionType.INCOME -> Color(0xFF16A34A)
                                TransactionType.TRANSFER -> Color(0xFF4338CA)
                            }
                        } else Color.Gray,
                        fontSize = 13.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Large Amount Display
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = "₹",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(4.dp))
            OutlinedTextField(
                value = amountInput,
                onValueChange = { amountInput = it.filter { ch -> ch.isDigit() || ch == '.' } },
                textStyle = LocalTextStyle.current.copy(
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold
                ),
                placeholder = { Text("0", fontSize = 32.sp, fontWeight = FontWeight.Bold) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.width(220.dp),
                shape = RoundedCornerShape(12.dp)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Quick Amount Suggestions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            listOf("100", "500", "1000", "2000").forEach { quick ->
                FilterChip(
                    selected = amountInput == quick,
                    onClick = { amountInput = quick },
                    label = { Text("₹$quick", fontSize = 12.sp) }
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Payee / Description
        OutlinedTextField(
            value = payeeInput,
            onValueChange = { payeeInput = it },
            label = { Text("Title / Description (Optional)") },
            placeholder = { Text("e.g. Zomato, Dinner, Groceries") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Category Selection
        val filteredCats = categories.filter { it.type == txType }
        if (filteredCats.isNotEmpty() && txType != TransactionType.TRANSFER) {
            Text(
                text = "Category",
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            )
            androidx.compose.foundation.lazy.LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredCats.size) { index ->
                    val cat = filteredCats[index]
                    val isSel = selectedCategoryId == cat.id
                    FilterChip(
                        selected = isSel,
                        onClick = { selectedCategoryId = cat.id },
                        label = { Text(cat.name, fontSize = 11.sp) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Account Selection
        if (accounts.isNotEmpty()) {
            Text(
                text = "Account",
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            )
            androidx.compose.foundation.lazy.LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(accounts.size) { index ->
                    val acc = accounts[index]
                    val isSel = selectedAccountId == acc.id
                    FilterChip(
                        selected = isSel,
                        onClick = { selectedAccountId = acc.id },
                        label = { Text(acc.name, fontSize = 11.sp) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Note
        OutlinedTextField(
            value = noteInput,
            onValueChange = { noteInput = it },
            label = { Text("Note (Optional)") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(28.dp))

        // Save Button
        Button(
            onClick = {
                val paise = CurrencyFormatter.parseInputToPaise(amountInput)
                if (paise <= 0L) return@Button
                val accId = selectedAccountId ?: accounts.firstOrNull()?.id ?: return@Button
                val catId = if (txType == TransactionType.TRANSFER) null else selectedCategoryId
                val cat = categories.find { it.id == catId }

                val defaultTitle = when (txType) {
                    TransactionType.TRANSFER -> "Transfer"
                    TransactionType.INCOME -> cat?.name ?: "Income"
                    TransactionType.EXPENSE -> cat?.name ?: "Expense"
                }

                val transaction = TransactionEntity(
                    type = txType,
                    amountPaise = paise,
                    categoryId = catId,
                    accountId = accId,
                    dateEpochMillis = System.currentTimeMillis(),
                    merchantOrPayee = payeeInput.ifBlank { defaultTitle },
                    note = noteInput
                )

                coroutineScope.launch {
                    repository.insertTransaction(transaction)
                    onSaved()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0C2B8A)),
            enabled = accounts.isNotEmpty()
        ) {
            Text("Save Transaction", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}

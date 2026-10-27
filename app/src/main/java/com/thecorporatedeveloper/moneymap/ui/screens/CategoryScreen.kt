package com.thecorporatedeveloper.moneymap.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thecorporatedeveloper.moneymap.core.model.TransactionType
import com.thecorporatedeveloper.moneymap.data.entity.CategoryEntity
import com.thecorporatedeveloper.moneymap.data.entity.TransactionEntity
import com.thecorporatedeveloper.moneymap.ui.components.getCategoryIcon
import com.thecorporatedeveloper.moneymap.ui.components.parseColorHex
import com.thecorporatedeveloper.moneymap.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryManagementScreen(
    categories: List<CategoryEntity>,
    transactions: List<TransactionEntity>,
    onBack: () -> Unit,
    onAddCategory: (CategoryEntity) -> Unit,
    onUpdateCategory: (CategoryEntity) -> Unit,
    onDeleteCategory: (CategoryEntity, Long?) -> Unit,
    onArchiveCategory: (Long) -> Unit = {},
    onToggleFavoriteCategory: (CategoryEntity) -> Unit = {}
) {
    var selectedType by remember { mutableStateOf(TransactionType.EXPENSE) }
    var favoriteFilter by remember { mutableStateOf("ALL") } // "ALL", "FAVORITES", "NON_FAVORITES"
    var showAddDialog by remember { mutableStateOf(false) }
    var categoryToEdit by remember { mutableStateOf<CategoryEntity?>(null) }
    var categoryToDelete by remember { mutableStateOf<CategoryEntity?>(null) }
    var showArchived by remember { mutableStateOf(false) }

    val activeCatsForType = remember(categories, selectedType, favoriteFilter) {
        val active = categories.filter { it.type == selectedType && !it.isArchived }
        when (favoriteFilter) {
            "FAVORITES" -> active.filter { it.isFavorite }
            "NON_FAVORITES" -> active.filter { !it.isFavorite }
            else -> active.sortedWith(compareByDescending<CategoryEntity> { it.isFavorite }.thenBy { it.name })
        }
    }
    val archivedCatsForType = remember(categories, selectedType) {
        categories.filter { it.type == selectedType && it.isArchived }
    }

    val defaultCats = remember(activeCatsForType) {
        activeCatsForType.filter { !it.isCustom }.sortedWith(compareByDescending<CategoryEntity> { it.isFavorite }.thenBy { it.name })
    }
    val customCats = remember(activeCatsForType) {
        activeCatsForType.filter { it.isCustom }.sortedWith(compareByDescending<CategoryEntity> { it.isFavorite }.thenBy { it.name })
    }

    val selectableIcons = listOf(
        "restaurant", "shopping_cart", "shopping_bag", "directions_car",
        "local_gas_station", "build", "receipt_long", "bolt",
        "phone_android", "home", "school", "local_hospital",
        "sports_esports", "spa", "flight", "roofing",
        "card_giftcard", "payments", "work", "business",
        "trending_up", "account_balance", "savings", "percent",
        "apartment", "subscriptions", "devices", "fitness_center",
        "local_cafe", "pets", "more_horiz"
    )

    val selectableColors = listOf(
        "#EF4444", "#F59E0B", "#10B981", "#3B82F6",
        "#8B5CF6", "#06B6D4", "#EC4899", "#6366F1",
        "#14B8A6", "#F97316", "#84CC16", "#64748B"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Categories", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("categories_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showAddDialog = true },
                        modifier = Modifier.testTag("add_category_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add Category", tint = MoneyMapBlue)
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MoneyMapBlue,
                contentColor = Color.White,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Add Category", fontWeight = FontWeight.Bold) },
                modifier = Modifier.testTag("fab_add_category")
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 90.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Segmented Control: Expense, Income
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(4.dp)
                ) {
                    val expCount = categories.count { it.type == TransactionType.EXPENSE && !it.isArchived }
                    val incCount = categories.count { it.type == TransactionType.INCOME && !it.isArchived }

                    listOf(
                        TransactionType.EXPENSE to "Expense ($expCount)",
                        TransactionType.INCOME to "Income ($incCount)"
                    ).forEach { (type, label) ->
                        val isSel = selectedType == type
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSel) MoneyMapBlue else Color.Transparent)
                                .clickable { selectedType = type }
                                .padding(vertical = 10.dp)
                                .testTag("category_tab_${type.name.lowercase()}"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                color = if (isSel) Color.White else TextSecondary,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }

            // Filter Pills: All, Favourites, Non-favourites
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = favoriteFilter == "ALL",
                        onClick = { favoriteFilter = "ALL" },
                        label = { Text("All", fontSize = 12.sp) }
                    )
                    FilterChip(
                        selected = favoriteFilter == "FAVORITES",
                        onClick = { favoriteFilter = "FAVORITES" },
                        label = { Text("Favourites", fontSize = 12.sp) },
                        leadingIcon = { Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFB800), modifier = Modifier.size(14.dp)) }
                    )
                    FilterChip(
                        selected = favoriteFilter == "NON_FAVORITES",
                        onClick = { favoriteFilter = "NON_FAVORITES" },
                        label = { Text("Non-favourites", fontSize = 12.sp) }
                    )
                }
            }

            // Empty State
            if (activeCatsForType.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp)
                            .testTag("empty_categories_card"),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.Category, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = if (favoriteFilter == "FAVORITES") "No favourite ${selectedType.name.lowercase()} categories yet" else "No ${selectedType.name.lowercase()} categories",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = if (favoriteFilter == "FAVORITES") "Tap the star on any category to add it to favourites." else "Add a new category to organize your transactions.",
                                color = TextSecondary,
                                fontSize = 13.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { showAddDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = MoneyMapNavy)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Add Category")
                            }
                        }
                    }
                }
            }

            // Custom Categories Section (if any)
            if (customCats.isNotEmpty()) {
                item {
                    Text(
                        text = "Custom Categories (${customCats.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                items(customCats, key = { it.id }) { cat ->
                    CategoryRowItem(
                        category = cat,
                        onEdit = { categoryToEdit = cat },
                        onDelete = { categoryToDelete = cat },
                        onToggleFavorite = { onToggleFavoriteCategory(cat) }
                    )
                }
            }

            // Standard Categories Section
            if (defaultCats.isNotEmpty()) {
                item {
                    Text(
                        text = "Standard Categories (${defaultCats.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                items(defaultCats, key = { it.id }) { cat ->
                    CategoryRowItem(
                        category = cat,
                        onEdit = { categoryToEdit = cat },
                        onDelete = { categoryToDelete = cat },
                        onToggleFavorite = { onToggleFavoriteCategory(cat) }
                    )
                }
            }

            // Archived Categories Toggle & List (if any)
            if (archivedCatsForType.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showArchived = !showArchived }
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Archived Categories (${archivedCatsForType.size})",
                            style = MaterialTheme.typography.titleSmall,
                            color = TextSecondary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Icon(
                            imageVector = if (showArchived) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = "Toggle Archived",
                            tint = TextSecondary
                        )
                    }
                }

                if (showArchived) {
                    items(archivedCatsForType, key = { it.id }) { cat ->
                        CategoryRowItem(
                            category = cat,
                            onEdit = { categoryToEdit = cat },
                            onDelete = { categoryToDelete = cat }
                        )
                    }
                }
            }
        }
    }

    // Add Category Dialog
    if (showAddDialog) {
        var catName by remember { mutableStateOf("") }
        var catType by remember { mutableStateOf(selectedType) }
        var catIcon by remember { mutableStateOf(if (catType == TransactionType.EXPENSE) "shopping_cart" else "payments") }
        var catColor by remember { mutableStateOf(if (catType == TransactionType.EXPENSE) "#EF4444" else "#10B981") }
        var errorMessage by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add Category", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Type selector in dialog
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(3.dp)
                    ) {
                        listOf(TransactionType.EXPENSE to "Expense", TransactionType.INCOME to "Income").forEach { (t, lbl) ->
                            val isSel = catType == t
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSel) MoneyMapBlue else Color.Transparent)
                                    .clickable {
                                        catType = t
                                        errorMessage = null
                                    }
                                    .padding(vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = lbl,
                                    color = if (isSel) Color.White else TextSecondary,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = catName,
                        onValueChange = {
                            catName = it
                            errorMessage = null
                        },
                        label = { Text("Category Name") },
                        placeholder = { Text("e.g. Groceries, Gym, Dividends") },
                        isError = errorMessage != null,
                        supportingText = errorMessage?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("category_name_input")
                    )

                    Text("Select Icon", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(selectableIcons) { iconKey ->
                            val isSel = catIcon == iconKey
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(if (isSel) MoneyMapBlue else MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable { catIcon = iconKey }
                                    .testTag("icon_choice_$iconKey"),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    getCategoryIcon(iconKey),
                                    contentDescription = iconKey,
                                    tint = if (isSel) Color.White else TextPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    Text("Accent Color", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(selectableColors) { hex ->
                            val col = parseColorHex(hex)
                            val isSel = catColor.equals(hex, ignoreCase = true)
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(col)
                                    .border(if (isSel) 3.dp else 0.dp, Color.Black, CircleShape)
                                    .clickable { catColor = hex }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val trimmed = catName.trim()
                        if (trimmed.isBlank()) {
                            errorMessage = "Category name cannot be empty"
                            return@Button
                        }
                        // Case-insensitive duplicate validation within same type
                        val isDuplicate = categories.any {
                            it.type == catType && it.name.trim().equals(trimmed, ignoreCase = true)
                        }
                        if (isDuplicate) {
                            errorMessage = "A category named \"$trimmed\" already exists for ${catType.name.lowercase()}"
                            return@Button
                        }

                        onAddCategory(
                            CategoryEntity(
                                name = trimmed,
                                type = catType,
                                iconName = catIcon,
                                colorHex = catColor,
                                isCustom = true
                            )
                        )
                        showAddDialog = false
                    },
                    modifier = Modifier.testTag("save_category_button")
                ) {
                    Text("Save Category")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Edit Category Dialog
    categoryToEdit?.let { cat ->
        var editName by remember { mutableStateOf(cat.name) }
        var editIcon by remember { mutableStateOf(cat.iconName) }
        var editColor by remember { mutableStateOf(cat.colorHex) }
        var errorMessage by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { categoryToEdit = null },
            title = { Text("Edit Category", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = editName,
                        onValueChange = {
                            editName = it
                            errorMessage = null
                        },
                        label = { Text("Category Name") },
                        isError = errorMessage != null,
                        supportingText = errorMessage?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("edit_category_name_input")
                    )

                    Text("Select Icon", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(selectableIcons) { iconKey ->
                            val isSel = editIcon == iconKey
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(if (isSel) MoneyMapBlue else MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable { editIcon = iconKey },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    getCategoryIcon(iconKey),
                                    contentDescription = iconKey,
                                    tint = if (isSel) Color.White else TextPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    Text("Accent Color", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(selectableColors) { hex ->
                            val col = parseColorHex(hex)
                            val isSel = editColor.equals(hex, ignoreCase = true)
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(col)
                                    .border(if (isSel) 3.dp else 0.dp, Color.Black, CircleShape)
                                    .clickable { editColor = hex }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val trimmed = editName.trim()
                        if (trimmed.isBlank()) {
                            errorMessage = "Category name cannot be empty"
                            return@Button
                        }
                        val isDuplicate = categories.any {
                            it.id != cat.id && it.type == cat.type && it.name.trim().equals(trimmed, ignoreCase = true)
                        }
                        if (isDuplicate) {
                            errorMessage = "A category named \"$trimmed\" already exists for ${cat.type.name.lowercase()}"
                            return@Button
                        }

                        onUpdateCategory(
                            cat.copy(
                                name = trimmed,
                                iconName = editIcon,
                                colorHex = editColor
                            )
                        )
                        categoryToEdit = null
                    },
                    modifier = Modifier.testTag("update_category_button")
                ) {
                    Text("Save Changes")
                }
            },
            dismissButton = {
                TextButton(onClick = { categoryToEdit = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Delete / Reassign / Archive Dialog
    categoryToDelete?.let { cat ->
        val associatedTxs = remember(transactions, cat) {
            transactions.filter { it.categoryId == cat.id }
        }
        val compatibleCategories = remember(categories, cat) {
            categories.filter { it.id != cat.id && it.type == cat.type && !it.isArchived }
        }
        var selectedReassignId by remember {
            mutableStateOf(compatibleCategories.firstOrNull()?.id)
        }
        var resolutionMode by remember { mutableStateOf<String>("reassign") } // "reassign" or "archive"

        AlertDialog(
            onDismissRequest = { categoryToDelete = null },
            title = {
                Text(
                    text = if (associatedTxs.isEmpty()) "Delete Category" else "Category in Use",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (associatedTxs.isEmpty()) {
                        Text("Are you sure you want to delete \"${cat.name}\"? This action cannot be undone.")
                    } else {
                        Text(
                            text = "\"${cat.name}\" is referenced by ${associatedTxs.size} transaction(s). Choose a safe resolution:",
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp
                        )

                        // Option 1: Reassign
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .border(
                                    width = if (resolutionMode == "reassign") 2.dp else 1.dp,
                                    color = if (resolutionMode == "reassign") MoneyMapBlue else BorderSubtle,
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .clickable { resolutionMode = "reassign" }
                                .padding(12.dp),
                            color = MaterialTheme.colorScheme.surface
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    RadioButton(
                                        selected = resolutionMode == "reassign",
                                        onClick = { resolutionMode = "reassign" }
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Reassign transactions", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                                if (resolutionMode == "reassign" && compatibleCategories.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text("Reassign all ${associatedTxs.size} transactions to:", fontSize = 12.sp, color = TextSecondary)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    LazyRow(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        items(compatibleCategories, key = { it.id }) { target ->
                                            val isSel = selectedReassignId == target.id
                                            FilterChip(
                                                selected = isSel,
                                                onClick = { selectedReassignId = target.id },
                                                label = { Text(target.name, fontSize = 11.sp) }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Option 2: Archive
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .border(
                                    width = if (resolutionMode == "archive") 2.dp else 1.dp,
                                    color = if (resolutionMode == "archive") MoneyMapBlue else BorderSubtle,
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .clickable { resolutionMode = "archive" }
                                .padding(12.dp),
                            color = MaterialTheme.colorScheme.surface
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(
                                    selected = resolutionMode == "archive",
                                    onClick = { resolutionMode = "archive" }
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Text("Archive category", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text("Hides from new transactions while preserving all historical data.", fontSize = 11.sp, color = TextSecondary)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                if (associatedTxs.isEmpty()) {
                    Button(
                        onClick = {
                            onDeleteCategory(cat, null)
                            categoryToDelete = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MoneyMapRed),
                        modifier = Modifier.testTag("confirm_delete_category_button")
                    ) {
                        Text("Delete")
                    }
                } else {
                    Button(
                        onClick = {
                            if (resolutionMode == "archive") {
                                onArchiveCategory(cat.id)
                            } else {
                                onDeleteCategory(cat, selectedReassignId)
                            }
                            categoryToDelete = null
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (resolutionMode == "archive") MoneyMapNavy else MoneyMapRed
                        ),
                        modifier = Modifier.testTag("confirm_resolve_category_button")
                    ) {
                        Text(if (resolutionMode == "archive") "Archive Category" else "Reassign & Delete")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { categoryToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun CategoryRowItem(
    category: CategoryEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggleFavorite: () -> Unit = {}
) {
    val col = parseColorHex(category.colorHex, MoneyMapBlue)
    val icon = getCategoryIcon(category.iconName)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .testTag("category_row_${category.id}"),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(col.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = col, modifier = Modifier.size(22.dp))
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = category.name,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = if (category.isCustom) "Custom" else "Standard",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                    if (category.isFavorite) {
                        Text(
                            text = "• Favourite",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFFFB800),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                    if (category.isArchived) {
                        Text(
                            text = "• Archived",
                            style = MaterialTheme.typography.bodySmall,
                            color = MoneyMapOrange,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            // Star Favorite Action
            IconButton(
                onClick = onToggleFavorite,
                modifier = Modifier.testTag("favorite_category_${category.id}")
            ) {
                Icon(
                    imageVector = if (category.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                    contentDescription = if (category.isFavorite) "Remove from Favourites" else "Mark as Favourite",
                    tint = if (category.isFavorite) Color(0xFFFFB800) else TextSecondary.copy(alpha = 0.5f),
                    modifier = Modifier.size(22.dp)
                )
            }

            // Edit Action
            IconButton(
                onClick = onEdit,
                modifier = Modifier.testTag("edit_category_${category.id}")
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit ${category.name}",
                    tint = MoneyMapNavy,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Delete Action
            IconButton(
                onClick = onDelete,
                modifier = Modifier.testTag("delete_category_${category.id}")
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete ${category.name}",
                    tint = MoneyMapRed,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

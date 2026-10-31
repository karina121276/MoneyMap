package com.thecorporatedeveloper.moneymap.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thecorporatedeveloper.moneymap.core.model.AccountType
import com.thecorporatedeveloper.moneymap.core.model.CurrencyFormatter
import com.thecorporatedeveloper.moneymap.core.model.TransactionType
import com.thecorporatedeveloper.moneymap.data.entity.AccountEntity
import com.thecorporatedeveloper.moneymap.data.entity.CategoryEntity
import com.thecorporatedeveloper.moneymap.data.entity.TransactionEntity
import com.thecorporatedeveloper.moneymap.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Returns an ImageVector based on icon name string
 */
fun getCategoryIcon(name: String): ImageVector {
    return when (name.lowercase(Locale.ENGLISH)) {
        // Food & Groceries
        "fish", "set_meal" -> Icons.Default.SetMeal
        "chicken", "meat", "kebab_dining" -> Icons.Default.KebabDining
        "vegetables", "veggies", "eco" -> Icons.Default.Eco
        "fruits", "local_florist" -> Icons.Default.LocalFlorist
        "grocery_basket", "shopping_basket", "basket" -> Icons.Default.ShoppingBasket
        "shopping_cart", "groceries" -> Icons.Default.ShoppingCart
        "restaurant", "food", "dining" -> Icons.Default.Restaurant
        "local_cafe", "cafe", "coffee" -> Icons.Default.LocalCafe
        "cake", "bakery" -> Icons.Default.Cake
        "fastfood", "fast_food", "burger" -> Icons.Default.Fastfood

        // Shopping & Lifestyle
        "checkroom", "clothes", "clothing", "tshirt", "t-shirt" -> Icons.Default.Checkroom
        "shopping_bag", "shopping" -> Icons.Default.ShoppingBag
        "brush", "cosmetics", "beauty", "makeup" -> Icons.Default.Brush
        "watch", "jewelry", "accessories" -> Icons.Default.Watch
        "devices", "electronics", "gadgets" -> Icons.Default.Devices
        "chair", "furniture", "decor" -> Icons.Default.Chair
        "card_giftcard", "gift", "gifts", "donations" -> Icons.Default.CardGiftcard
        "spa", "personal", "personal care" -> Icons.Default.Spa

        // Transport & Utilities
        "directions_car", "car", "transport", "cab" -> Icons.Default.DirectionsCar
        "two_wheeler", "motorcycle", "bike", "scooter" -> Icons.Default.TwoWheeler
        "directions_bus", "bus", "public_transport" -> Icons.Default.DirectionsBus
        "train", "metro", "subway" -> Icons.Default.Train
        "local_gas_station", "fuel", "petrol", "diesel" -> Icons.Default.LocalGasStation
        "bolt", "electricity", "power" -> Icons.Default.Bolt
        "water_drop", "water", "utility_water" -> Icons.Default.WaterDrop
        "phone_android", "mobile", "internet", "mobile & internet" -> Icons.Default.PhoneAndroid
        "wifi", "broadband", "network" -> Icons.Default.Wifi
        "home", "roofing", "rent", "housing" -> Icons.Default.Home
        "receipt_long", "receipt", "bills & utilities", "bills", "utilities" -> Icons.Default.ReceiptLong
        "build", "vehicle", "service", "vehicle servicing", "maintenance" -> Icons.Default.Build

        // Income & Finance
        "payments", "salary", "paycheck" -> Icons.Default.Payments
        "account_balance_wallet", "wallet", "cash" -> Icons.Default.AccountBalanceWallet
        "account_balance", "interest", "bank" -> Icons.Default.AccountBalance
        "business", "company" -> Icons.Default.Business
        "work", "freelance", "gig", "consulting" -> Icons.Default.Work
        "trending_up", "investments", "stocks", "mutual_funds" -> Icons.Default.TrendingUp
        "percent", "grants", "cashback", "dividends" -> Icons.Default.Percent
        "redeem", "gift income", "reward" -> Icons.Default.Redeem
        "savings", "other income", "piggy" -> Icons.Default.Savings
        "credit_card" -> Icons.Default.CreditCard

        // Health, Leisure & Education
        "local_hospital", "favorite", "health", "medical", "hospital", "pharmacy" -> Icons.Default.LocalHospital
        "school", "education", "books", "course" -> Icons.Default.School
        "flight", "travel", "vacation" -> Icons.Default.Flight
        "sports_esports", "entertainment", "gaming" -> Icons.Default.SportsEsports
        "fitness_center", "gym", "workout" -> Icons.Default.FitnessCenter
        "subscriptions", "ott", "streaming" -> Icons.Default.Subscriptions
        "pets", "pet_care" -> Icons.Default.Pets
        "family_restroom", "family" -> Icons.Default.FamilyRestroom
        "apartment", "rental" -> Icons.Default.Apartment

        else -> Icons.Default.MoreHoriz
    }
}

fun parseColorHex(hex: String, fallback: Color = MoneyMapBlue): Color {
    return try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (e: Exception) {
        fallback
    }
}

/**
 * Transaction Item Row matching Design Showcase 04 & 05
 */
@Composable
fun TransactionItemRow(
    transaction: TransactionEntity,
    category: CategoryEntity?,
    accountName: String = "",
    hideAmounts: Boolean = false,
    onToggleFavorite: ((TransactionEntity) -> Unit)? = null,
    onClick: () -> Unit
) {
    val dateFmt = SimpleDateFormat("h:mm a", Locale.ENGLISH)
    val timeStr = dateFmt.format(Date(transaction.dateEpochMillis))
    val catColor = category?.let { parseColorHex(it.colorHex, MoneyMapBlue) } ?: MoneyMapNavy
    val icon = category?.iconName?.let { getCategoryIcon(it) } ?: when (transaction.type) {
        TransactionType.INCOME -> Icons.Default.ArrowUpward
        TransactionType.EXPENSE -> Icons.Default.ArrowDownward
        TransactionType.TRANSFER -> Icons.Default.SwapHoriz
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .testTag("transaction_row_${transaction.id}"),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Category Icon Badge
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(catColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = category?.name ?: "Category",
                    tint = catColor,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Merchant / Description
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transaction.merchantOrPayee.ifBlank { category?.name ?: "Transaction" },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                val trimmedNote = transaction.note.trim()
                val subtitle = if (trimmedNote.isNotBlank()) {
                    "$trimmedNote • $timeStr"
                } else {
                    "${category?.name ?: "General"} • $timeStr"
                }
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Amount
            val amountFormatted = CurrencyFormatter.formatTransactionAmount(
                paise = transaction.amountPaise,
                type = transaction.type,
                hideAmounts = hideAmounts
            )
            val amountColor = when (transaction.type) {
                TransactionType.INCOME -> MoneyMapGreen
                TransactionType.EXPENSE -> MoneyMapRed
                TransactionType.TRANSFER -> MoneyMapBlue
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = amountFormatted,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = amountColor
                )
            }
        }
    }
}



/**
 * Reusable Confirmation Dialog before deleting a transaction
 */
@Composable
fun DeleteTransactionConfirmationDialog(
    transaction: TransactionEntity?,
    onDismiss: () -> Unit,
    onConfirm: (TransactionEntity) -> Unit
) {
    if (transaction == null) return
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete Transaction?", fontWeight = FontWeight.Bold) },
        text = { Text("Are you sure you want to delete this transaction? This action cannot be undone.") },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(transaction)
                    onDismiss()
                },
                modifier = Modifier.testTag("confirm_delete_dialog_button")
            ) {
                Text("Delete", color = MoneyMapRed, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("cancel_delete_dialog_button")
            ) {
                Text("Cancel", color = TextSecondary)
            }
        }
    )
}

/**
 * Metric summary card (Income, Expense, Savings)
 */
@Composable
fun MetricSummaryCard(
    modifier: Modifier = Modifier,
    title: String,
    amountPaise: Long,
    color: Color,
    icon: ImageVector,
    hideAmounts: Boolean = false,
    subtitle: String? = null
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp)),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = color,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = CurrencyFormatter.formatRupees(amountPaise, hideAmounts = hideAmounts),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )

            if (subtitle != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    fontSize = 10.sp
                )
            }
        }
    }
}

/**
 * Circular progress ring for spending goal
 */
@Composable
fun CircularProgressRing(
    percentage: Float,
    modifier: Modifier = Modifier,
    strokeWidth: Dp = 10.dp,
    progressColor: Color = MoneyMapBlue,
    backgroundColor: Color = BorderSubtle
) {
    val animatedProgress by animateFloatAsState(targetValue = percentage.coerceIn(0f, 1f), label = "progress")

    Canvas(modifier = modifier) {
        val stroke = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
        val diameter = size.minDimension - strokeWidth.toPx()
        val topLeft = Offset((size.width - diameter) / 2, (size.height - diameter) / 2)
        val arcSize = Size(diameter, diameter)

        // Background ring
        drawArc(
            color = backgroundColor,
            startAngle = -90f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = stroke
        )

        // Progress arc
        drawArc(
            color = progressColor,
            startAngle = -90f,
            sweepAngle = 360f * animatedProgress,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = stroke
        )
    }
}

/**
 * Donut chart for category spending breakdown
 */
@Composable
fun DonutChart(
    slices: List<Pair<Float, Color>>, // percentage (0..100) to Color
    modifier: Modifier = Modifier,
    strokeWidth: Dp = 26.dp
) {
    Canvas(modifier = modifier) {
        val stroke = Stroke(width = strokeWidth.toPx())
        val diameter = size.minDimension - strokeWidth.toPx()
        val topLeft = Offset((size.width - diameter) / 2, (size.height - diameter) / 2)
        val arcSize = Size(diameter, diameter)

        var currentAngle = -90f

        if (slices.isEmpty()) {
            drawArc(
                color = BorderSubtle,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = stroke
            )
            return@Canvas
        }

        for ((percent, color) in slices) {
            val sweep = (percent / 100f) * 360f
            drawArc(
                color = color,
                startAngle = currentAngle,
                sweepAngle = sweep,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = stroke
            )
            currentAngle += sweep
        }
    }
}

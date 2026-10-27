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
        "restaurant", "food", "dining" -> Icons.Default.Restaurant
        "shopping_bag", "shopping" -> Icons.Default.ShoppingBag
        "shopping_cart", "groceries" -> Icons.Default.ShoppingCart
        "directions_car", "transport", "cab" -> Icons.Default.DirectionsCar
        "local_gas_station", "fuel", "petrol" -> Icons.Default.LocalGasStation
        "build", "vehicle", "service", "vehicle servicing" -> Icons.Default.Build
        "receipt_long", "receipt", "bills & utilities", "bills", "utilities" -> Icons.Default.ReceiptLong
        "bolt", "electricity" -> Icons.Default.Bolt
        "phone_android", "mobile", "internet", "mobile & internet" -> Icons.Default.PhoneAndroid
        "sports_esports", "entertainment" -> Icons.Default.SportsEsports
        "local_hospital", "favorite", "health", "medical", "hospital" -> Icons.Default.LocalHospital
        "school", "education" -> Icons.Default.School
        "flight", "travel" -> Icons.Default.Flight
        "roofing", "home", "rent" -> Icons.Default.Home
        "spa", "personal", "personal care" -> Icons.Default.Spa
        "card_giftcard", "gifts", "donations" -> Icons.Default.CardGiftcard
        "family_restroom", "family" -> Icons.Default.FamilyRestroom
        "devices", "electronics" -> Icons.Default.Devices
        "trending_up", "investments" -> Icons.Default.TrendingUp
        "subscriptions" -> Icons.Default.Subscriptions
        "payments", "salary" -> Icons.Default.Payments
        "work", "freelance" -> Icons.Default.Work
        "business" -> Icons.Default.Business
        "apartment", "rental" -> Icons.Default.Apartment
        "percent", "grants", "cashback" -> Icons.Default.Percent
        "savings", "other income" -> Icons.Default.Savings
        "account_balance", "interest", "bank" -> Icons.Default.AccountBalance
        "credit_card" -> Icons.Default.CreditCard
        "account_balance_wallet", "wallet" -> Icons.Default.AccountBalanceWallet
        "fitness_center", "gym" -> Icons.Default.FitnessCenter
        "local_cafe", "cafe", "coffee" -> Icons.Default.LocalCafe
        "pets" -> Icons.Default.Pets
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

package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.view.View
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import com.example.core.model.CurrencyFormatter
import com.example.core.model.TransactionType
import com.example.data.database.MoneyMapDatabase
import com.example.data.entity.AccountEntity
import com.example.data.entity.CategoryEntity
import com.example.data.entity.TransactionEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class RecentTransactionsWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        updateWidgets(context, appWidgetManager, appWidgetIds)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == "com.example.ACTION_MONEYMAP_DATA_CHANGED" ||
            intent.action == AppWidgetManager.ACTION_APPWIDGET_UPDATE
        ) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val ids = appWidgetManager.getAppWidgetIds(
                ComponentName(context, RecentTransactionsWidgetProvider::class.java)
            )
            updateWidgets(context, appWidgetManager, ids)
        }
    }

    private fun updateWidgets(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = MoneyMapDatabase.getDatabase(context)
                val accounts = db.accountDao().getAllAccountsList()
                val categories = db.categoryDao().getAllCategoriesList()
                val recentTxList = db.transactionDao().getAllTransactionsList().take(3)

                val accountsMap = accounts.associateBy { it.id }
                val categoriesMap = categories.associateBy { it.id }

                for (widgetId in appWidgetIds) {
                    val views = RemoteViews(context.packageName, R.layout.widget_recent_transactions)

                    // 1. Root Header click -> Home
                    val homeIntent = Intent(context, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                        putExtra("route", "home")
                    }
                    val homePendingIntent = PendingIntent.getActivity(
                        context, widgetId * 100 + 1, homeIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    views.setOnClickPendingIntent(R.id.widget_recent_root, homePendingIntent)

                    // 2. See All click -> History Screen
                    val seeAllIntent = Intent(context, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                        putExtra("route", "history")
                    }
                    val seeAllPendingIntent = PendingIntent.getActivity(
                        context, widgetId * 100 + 2, seeAllIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    views.setOnClickPendingIntent(R.id.btn_widget_recent_see_all, seeAllPendingIntent)

                    // 3. Handle Empty vs Populated State
                    if (recentTxList.isEmpty()) {
                        views.setViewVisibility(R.id.widget_recent_empty_state, View.VISIBLE)
                        views.setViewVisibility(R.id.widget_recent_list, View.GONE)

                        val addIntent = Intent(context, MainActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                            putExtra("route", "add_transaction")
                        }
                        val addPendingIntent = PendingIntent.getActivity(
                            context, widgetId * 100 + 3, addIntent,
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        )
                        views.setOnClickPendingIntent(R.id.btn_widget_empty_add, addPendingIntent)
                    } else {
                        views.setViewVisibility(R.id.widget_recent_empty_state, View.GONE)
                        views.setViewVisibility(R.id.widget_recent_list, View.VISIBLE)

                        bindRow(context, views, widgetId, 1, R.id.widget_tx_row_1, R.id.tx_1_title, R.id.tx_1_subtitle, recentTxList.getOrNull(0), accountsMap, categoriesMap)
                        bindRow(context, views, widgetId, 2, R.id.widget_tx_row_2, R.id.tx_2_title, R.id.tx_2_subtitle, recentTxList.getOrNull(1), accountsMap, categoriesMap)
                        bindRow(context, views, widgetId, 3, R.id.widget_tx_row_3, R.id.tx_3_title, R.id.tx_3_subtitle, recentTxList.getOrNull(2), accountsMap, categoriesMap)
                    }

                    appWidgetManager.updateAppWidget(widgetId, views)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun bindRow(
        context: Context,
        views: RemoteViews,
        widgetId: Int,
        rowIndex: Int,
        rowId: Int,
        titleId: Int,
        subtitleId: Int,
        tx: TransactionEntity?,
        accountsMap: Map<Long, AccountEntity>,
        categoriesMap: Map<Long, CategoryEntity>
    ) {
        if (tx == null) {
            views.setViewVisibility(rowId, View.GONE)
            return
        }

        views.setViewVisibility(rowId, View.VISIBLE)

        val catName = tx.categoryId?.let { categoriesMap[it]?.name }
        val title = when {
            !tx.merchantOrPayee.isNullOrBlank() -> tx.merchantOrPayee
            !catName.isNullOrBlank() -> catName
            tx.type == TransactionType.TRANSFER -> "Transfer"
            tx.type == TransactionType.INCOME -> "Income"
            else -> "Expense"
        }

        val accName = accountsMap[tx.accountId]?.name ?: "Account"
        val dateFormatted = formatDateShort(tx.dateEpochMillis)
        val subtitle = "• $accName • $dateFormatted"

        views.setTextViewText(titleId, title)
        views.setTextViewText(subtitleId, subtitle)

        // Row Click -> Open Transaction Details Screen
        val rowIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("route", "transaction_details")
            putExtra("txId", tx.id)
        }
        val rowPendingIntent = PendingIntent.getActivity(
            context, widgetId * 1000 + rowIndex * 10, rowIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(rowId, rowPendingIntent)
    }

    private fun formatDateShort(millis: Long): String {
        return try {
            val date = Instant.ofEpochMilli(millis)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
            val today = LocalDate.now()
            when {
                date == today -> "Today"
                date == today.minusDays(1) -> "Yesterday"
                else -> date.format(DateTimeFormatter.ofPattern("dd MMM"))
            }
        } catch (e: Exception) {
            "Recent"
        }
    }
}

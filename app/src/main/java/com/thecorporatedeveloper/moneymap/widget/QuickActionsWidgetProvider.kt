package com.thecorporatedeveloper.moneymap.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.thecorporatedeveloper.moneymap.MainActivity
import com.thecorporatedeveloper.moneymap.R

class QuickActionsWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        updateWidgets(context, appWidgetManager, appWidgetIds)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == "com.thecorporatedeveloper.moneymap.ACTION_MONEYMAP_DATA_CHANGED" ||
            intent.action == AppWidgetManager.ACTION_APPWIDGET_UPDATE
        ) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val ids = appWidgetManager.getAppWidgetIds(
                ComponentName(context, QuickActionsWidgetProvider::class.java)
            )
            updateWidgets(context, appWidgetManager, ids)
        }
    }

    private fun updateWidgets(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (widgetId in appWidgetIds) {
            val views = RemoteViews(context.packageName, R.layout.widget_quick_actions)

            // 1. Root / Brand click -> Home
            val homeIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra("route", "home")
            }
            val homePendingIntent = PendingIntent.getActivity(
                context, widgetId * 10 + 1, homeIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_quick_root, homePendingIntent)
            views.setOnClickPendingIntent(R.id.widget_quick_brand, homePendingIntent)

            // 2. Add Income -> Add Transaction prefilled with Income
            val incomeIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra("route", "add_transaction")
                putExtra("type", "INCOME")
            }
            val incomePendingIntent = PendingIntent.getActivity(
                context, widgetId * 10 + 2, incomeIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.btn_widget_add_income, incomePendingIntent)

            // 3. Add Expense -> Add Transaction prefilled with Expense
            val expenseIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra("route", "add_transaction")
                putExtra("type", "EXPENSE")
            }
            val expensePendingIntent = PendingIntent.getActivity(
                context, widgetId * 10 + 3, expenseIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.btn_widget_add_expense, expensePendingIntent)

            // 4. Add Screenshot / Smart Entry -> Smart Entry screen
            val screenshotIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra("route", "smart_entry")
            }
            val screenshotPendingIntent = PendingIntent.getActivity(
                context, widgetId * 10 + 4, screenshotIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.btn_widget_add_screenshot, screenshotPendingIntent)

            // 5. Add Transfer -> Add Transaction prefilled with Transfer
            val transferIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra("route", "add_transaction")
                putExtra("type", "TRANSFER")
            }
            val transferPendingIntent = PendingIntent.getActivity(
                context, widgetId * 10 + 5, transferIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.btn_widget_add_transfer, transferPendingIntent)

            // 6. View Transactions -> History screen
            val transactionsIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra("route", "history")
            }
            val transactionsPendingIntent = PendingIntent.getActivity(
                context, widgetId * 10 + 6, transactionsIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.btn_widget_view_transactions, transactionsPendingIntent)

            // 7. Settings -> Settings Screen
            val settingsIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra("route", "settings")
            }
            val settingsPendingIntent = PendingIntent.getActivity(
                context, widgetId * 10 + 7, settingsIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.btn_widget_quick_settings, settingsPendingIntent)

            appWidgetManager.updateAppWidget(widgetId, views)
        }
    }
}

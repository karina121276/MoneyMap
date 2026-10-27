package com.thecorporatedeveloper.moneymap

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thecorporatedeveloper.moneymap.ui.components.parseColorHex
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.thecorporatedeveloper.moneymap.core.model.AccountType
import com.thecorporatedeveloper.moneymap.core.model.TransactionType
import com.thecorporatedeveloper.moneymap.data.database.MoneyMapDatabase
import com.thecorporatedeveloper.moneymap.data.entity.AccountEntity
import com.thecorporatedeveloper.moneymap.data.entity.BudgetEntity
import com.thecorporatedeveloper.moneymap.data.entity.CategoryEntity
import com.thecorporatedeveloper.moneymap.data.entity.TransactionEntity
import com.thecorporatedeveloper.moneymap.data.preferences.AppPreferences
import com.thecorporatedeveloper.moneymap.data.preferences.UserPreferences
import com.thecorporatedeveloper.moneymap.data.repository.MoneyMapRepository
import com.thecorporatedeveloper.moneymap.importer.ExtractedTransaction
import com.thecorporatedeveloper.moneymap.importer.PdfStatementParser
import com.thecorporatedeveloper.moneymap.importer.SmartEntryParser
import com.thecorporatedeveloper.moneymap.security.PinSecurityManager
import com.thecorporatedeveloper.moneymap.ui.screens.*
import com.thecorporatedeveloper.moneymap.ui.theme.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var repository: MoneyMapRepository
    private lateinit var appPreferences: AppPreferences
    private lateinit var pinSecurityManager: PinSecurityManager

    private val widgetRouteState = mutableStateOf<String?>(null)
    private val widgetTypeExtraState = mutableStateOf<String?>(null)
    private val widgetTxIdExtraState = mutableStateOf<Long?>(null)
    private val sharedImageUriState = mutableStateOf<Uri?>(null)

    private val isAppLockedState = mutableStateOf(false)
    private var lastBackgroundTimeMillis: Long = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val db = MoneyMapDatabase.getDatabase(this)
        repository = MoneyMapRepository(db, this)
        appPreferences = AppPreferences(this)
        pinSecurityManager = PinSecurityManager(this)

        extractImageUri(intent)
        extractWidgetExtras(intent)

        lifecycleScope.launch {
            if (pinSecurityManager.isPinSet()) {
                val prefs = appPreferences.userPreferencesFlow.first()
                if (prefs.appLockEnabled) {
                    isAppLockedState.value = true
                }
            }
        }

        setContent {
            val userPrefs by appPreferences.userPreferencesFlow.collectAsState(initial = UserPreferences())
            val isLocked by remember { isAppLockedState }

            // Apply FLAG_SECURE if screenshot blocking or app lock is active
            LaunchedEffect(userPrefs.blockScreenshots, isLocked, userPrefs.appLockEnabled) {
                if (userPrefs.blockScreenshots || (userPrefs.appLockEnabled && isLocked)) {
                    window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
                } else {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                }
            }

            val isDark = when (userPrefs.themeMode) {
                "DARK" -> true
                "LIGHT" -> false
                else -> androidx.compose.foundation.isSystemInDarkTheme()
            }

            val accentColor = remember(userPrefs.accentColorHex) {
                parseColorHex(userPrefs.accentColorHex, MoneyMapBlue)
            }

            val currentWidgetRoute by remember { widgetRouteState }
            val currentWidgetType by remember { widgetTypeExtraState }
            val currentWidgetTxId by remember { widgetTxIdExtraState }
            val currentSharedImageUri by remember { sharedImageUriState }

            MoneyMapTheme(darkTheme = isDark, accentColor = accentColor) {
                if (userPrefs.appLockEnabled && isLocked) {
                    AppUnlockScreen(
                        biometricsEnabled = userPrefs.biometricsEnabled,
                        deviceCredentialEnabled = userPrefs.deviceCredentialEnabled,
                        onVerifyPin = { pin -> pinSecurityManager.verifyPin(pin) },
                        onUnlockSuccess = { isAppLockedState.value = false }
                    )
                } else {
                    MoneyMapApp(
                        repository = repository,
                        appPreferences = appPreferences,
                        userPreferences = userPrefs,
                        pinSecurityManager = pinSecurityManager,
                        incomingImageUri = currentSharedImageUri,
                        onClearSharedImageUri = { sharedImageUriState.value = null },
                        widgetRoute = currentWidgetRoute,
                        widgetType = currentWidgetType,
                        widgetTxId = currentWidgetTxId,
                        onClearWidgetRoute = {
                            widgetRouteState.value = null
                            widgetTypeExtraState.value = null
                            widgetTxIdExtraState.value = null
                        }
                    )
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        lastBackgroundTimeMillis = android.os.SystemClock.elapsedRealtime()
    }

    override fun onStart() {
        super.onStart()
        if (lastBackgroundTimeMillis > 0L) {
            val elapsed = android.os.SystemClock.elapsedRealtime() - lastBackgroundTimeMillis
            lifecycleScope.launch {
                val prefs = appPreferences.userPreferencesFlow.first()
                if (prefs.appLockEnabled && pinSecurityManager.isPinSet()) {
                    val timeoutMillis = prefs.autoLockMinutes * 60 * 1000L
                    if (prefs.autoLockMinutes == 0 || elapsed >= timeoutMillis) {
                        isAppLockedState.value = true
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        extractImageUri(intent)
        extractWidgetExtras(intent)
    }

    private fun extractWidgetExtras(intent: Intent?) {
        val route = intent?.getStringExtra("route")
        if (!route.isNullOrBlank()) {
            widgetRouteState.value = route
        }
        val typeExtra = intent?.getStringExtra("type")
        if (!typeExtra.isNullOrBlank()) {
            widgetTypeExtraState.value = typeExtra
        }
        if (intent?.hasExtra("txId") == true) {
            val id = intent.getLongExtra("txId", -1L)
            if (id != -1L) {
                widgetTxIdExtraState.value = id
            }
        }
    }

    private fun extractImageUri(intent: Intent?) {
        if (intent == null) return
        val action = intent.action
        val type = intent.type

        val isSendAction = action == Intent.ACTION_SEND || action == Intent.ACTION_SEND_MULTIPLE
        val isImageType = type == null || type.startsWith("image/") || type == "*/*"

        if (!isSendAction || !isImageType) return

        var extractedUri: Uri? = null

        // 1. Try EXTRA_STREAM parcelable single
        try {
            extractedUri = intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
        } catch (e: Exception) {
            // Ignore
        }

        // 2. Try EXTRA_STREAM parcelable list (ACTION_SEND_MULTIPLE)
        if (extractedUri == null) {
            try {
                val list = intent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM)
                extractedUri = list?.firstOrNull()
            } catch (e: Exception) {
                // Ignore
            }
        }

        // 3. Try ClipData
        if (extractedUri == null && intent.clipData != null) {
            val clipData = intent.clipData
            if (clipData != null && clipData.itemCount > 0) {
                for (i in 0 until clipData.itemCount) {
                    val uri = clipData.getItemAt(i).uri
                    if (uri != null) {
                        extractedUri = uri
                        break
                    }
                }
            }
        }

        // 4. Try intent.data
        if (extractedUri == null) {
            extractedUri = intent.data
        }

        if (extractedUri != null) {
            val safeUri = copySharedUriToCache(extractedUri) ?: extractedUri
            sharedImageUriState.value = safeUri

            // Clear intent action to prevent re-triggering on activity recreation/rotation
            intent.action = null
            intent.removeExtra(Intent.EXTRA_STREAM)
            intent.clipData = null
        }
    }

    private fun copySharedUriToCache(sourceUri: Uri): Uri? {
        return try {
            if (sourceUri.scheme == "file") return sourceUri

            val contentResolver = applicationContext.contentResolver
            val mimeType = contentResolver.getType(sourceUri) ?: ""
            val ext = if (mimeType.contains("png")) "png" else "jpg"
            val cacheFile = java.io.File(cacheDir, "shared_import_${System.currentTimeMillis()}.$ext")

            contentResolver.openInputStream(sourceUri)?.use { input ->
                java.io.FileOutputStream(cacheFile).use { output ->
                    input.copyTo(output)
                }
            }
            if (cacheFile.exists() && cacheFile.length() > 0) {
                Uri.fromFile(cacheFile)
            } else null
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Error copying shared URI", e)
            sourceUri
        }
    }
}

sealed class Screen(val route: String, val title: String = "", val icon: ImageVector? = null) {
    object Splash : Screen("splash")
    object Home : Screen("home", "Home", Icons.Default.Home)
    object Accounts : Screen("accounts", "Accounts", Icons.Default.AccountBalance)
    object Add : Screen("add", "Add", Icons.Default.Add)
    object History : Screen("history", "History", Icons.Default.History)
    object Reports : Screen("reports", "Reports", Icons.Default.BarChart)

    object AccountDetails : Screen("account_details/{accountId}") {
        fun createRoute(id: Long) = "account_details/$id"
    }
    object AddAccount : Screen("add_account")
    object EditAccount : Screen("edit_account/{accountId}") {
        fun createRoute(id: Long) = "edit_account/$id"
    }

    object AddTransaction : Screen("add_transaction?type={type}") {
        fun createRoute(type: String? = null) = if (!type.isNullOrBlank()) "add_transaction?type=$type" else "add_transaction"
    }
    object EditTransaction : Screen("edit_transaction/{txId}") {
        fun createRoute(id: Long) = "edit_transaction/$id"
    }
    object TransactionDetails : Screen("transaction_details/{txId}") {
        fun createRoute(id: Long) = "transaction_details/$id"
    }

    object Favorites : Screen("favorites", "Favorites", Icons.Default.Star)
    object Categories : Screen("categories")
    object Calendar : Screen("calendar")
    object Budgets : Screen("budgets")
    object SmartEntry : Screen("smart_entry")
    object OcrReview : Screen("ocr_review")
    object PdfImport : Screen("pdf_import")
    object Settings : Screen("settings")
    object Backup : Screen("backup")
    object Privacy : Screen("privacy")
    object PinSetup : Screen("pin_setup")
    object ChangePin : Screen("change_pin")
}

@Composable
fun MoneyMapApp(
    repository: MoneyMapRepository,
    appPreferences: AppPreferences,
    userPreferences: UserPreferences,
    pinSecurityManager: PinSecurityManager,
    incomingImageUri: Uri?,
    onClearSharedImageUri: () -> Unit = {},
    widgetRoute: String?,
    widgetType: String? = null,
    widgetTxId: Long? = null,
    onClearWidgetRoute: () -> Unit
) {
    val navController = rememberNavController()
    val coroutineScope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current

    val accounts by repository.allAccounts.collectAsState(initial = emptyList())
    val categories by repository.allCategories.collectAsState(initial = emptyList())
    val transactions by repository.allTransactions.collectAsState(initial = emptyList())
    val favoriteTransactions by repository.favoriteTransactions.collectAsState(initial = emptyList())
    val budgets by repository.allBudgets.collectAsState(initial = emptyList())

    var isDataLoaded by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                repository.allAccounts.first()
            } catch (e: Exception) {}
        }
        isDataLoaded = true
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }

    // Handle initial incoming shared image or widget deep link
    LaunchedEffect(incomingImageUri) {
        if (incomingImageUri != null) {
            selectedImageUri = incomingImageUri
            navController.navigate(Screen.OcrReview.route) {
                popUpTo(Screen.Home.route) { saveState = true }
            }
        }
    }

    LaunchedEffect(widgetRoute, widgetType, widgetTxId) {
        if (!widgetRoute.isNullOrBlank()) {
            when (widgetRoute) {
                "home" -> navController.navigate(Screen.Home.route) {
                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                    launchSingleTop = true
                }
                "add_transaction", "add" -> {
                    val targetRoute = if (!widgetType.isNullOrBlank()) {
                        Screen.AddTransaction.createRoute(widgetType)
                    } else {
                        Screen.AddTransaction.createRoute()
                    }
                    navController.navigate(targetRoute)
                }
                "transaction_details" -> {
                    if (widgetTxId != null && widgetTxId != -1L) {
                        navController.navigate(Screen.TransactionDetails.createRoute(widgetTxId))
                    } else {
                        navController.navigate(Screen.History.route)
                    }
                }
                "history" -> navController.navigate(Screen.History.route) {
                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                    launchSingleTop = true
                }
                "reports" -> navController.navigate(Screen.Reports.route) {
                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                    launchSingleTop = true
                }
                "settings" -> navController.navigate(Screen.Settings.route)
            }
            onClearWidgetRoute()
        }
    }

    val bottomNavRoutes = listOf(
        Screen.Home,
        Screen.Accounts,
        Screen.Add,
        Screen.History,
        Screen.Reports
    )

    val showBottomBar = currentRoute in bottomNavRoutes.map { it.route }

    var activeExtractedTx by remember {
        mutableStateOf(
            SmartEntryParser.parseText(
                "Paid to Zomato ₹637\nPayment successful\n20 May 2026, 2:14 PM\nUPI Ref No. 614057380921\nFood order with friends"
            )
        )
    }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                MoneyMapBottomBar(
                    currentRoute = currentRoute ?: Screen.Home.route,
                    onNavigate = { screen ->
                        if (screen == Screen.Add) {
                            navController.navigate(Screen.AddTransaction.route)
                        } else {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Splash.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            // Splash Screen
            composable(Screen.Splash.route) {
                SplashScreen(
                    onTimeout = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Splash.route) { inclusive = true }
                        }
                    }
                )
            }

            // Home / Dashboard
            composable(Screen.Home.route) {
                DashboardScreen(
                    accounts = accounts,
                    categories = categories,
                    transactions = transactions,
                    budgets = budgets,
                    userName = userPreferences.userName,
                    hideAmounts = userPreferences.hideAmounts,
                    isDataLoaded = isDataLoaded,
                    onToggleHideAmounts = {
                        coroutineScope.launch {
                            appPreferences.setHideAmounts(!userPreferences.hideAmounts)
                        }
                    },
                    onNavigateToAddTransaction = { navController.navigate(Screen.AddTransaction.route) },
                    onNavigateToAddAccount = { navController.navigate(Screen.AddAccount.route) },
                    onNavigateToHistory = { navController.navigate(Screen.History.route) },
                    onNavigateToTransactionDetails = { id ->
                        navController.navigate(Screen.TransactionDetails.createRoute(id))
                    },
                    onNavigateToBudgets = { navController.navigate(Screen.Budgets.route) },
                    onNavigateToSmartEntry = { navController.navigate(Screen.SmartEntry.route) },
                    onNavigateToSettings = { navController.navigate(Screen.Settings.route) }
                )
            }

            // Accounts Overview
            composable(Screen.Accounts.route) {
                AccountsOverviewScreen(
                    accounts = accounts,
                    transactions = transactions,
                    hideAmounts = userPreferences.hideAmounts,
                    isDataLoaded = isDataLoaded,
                    onNavigateToAddAccount = { navController.navigate(Screen.AddAccount.route) },
                    onNavigateToAccountDetails = { id ->
                        navController.navigate(Screen.AccountDetails.createRoute(id))
                    },
                    onNavigateToBudgets = { navController.navigate(Screen.Budgets.route) }
                )
            }

            // Account Details
            composable(
                route = Screen.AccountDetails.route,
                arguments = listOf(navArgument("accountId") { type = NavType.LongType })
            ) { backStackEntry ->
                val accId = backStackEntry.arguments?.getLong("accountId") ?: 0L
                val account = accounts.find { it.id == accId }
                if (account != null) {
                    AccountDetailsScreen(
                        account = account,
                        transactions = transactions,
                        categories = categories,
                        hideAmounts = userPreferences.hideAmounts,
                        onBack = { navController.popBackStack() },
                        onEdit = { navController.navigate(Screen.EditAccount.createRoute(accId)) },
                        onNavigateToAddMoney = { navController.navigate(Screen.AddTransaction.route) },
                        onNavigateToTransfer = { navController.navigate(Screen.AddTransaction.route) },
                        onNavigateToStatement = { navController.navigate(Screen.PdfImport.route) },
                        onNavigateToTransactionDetails = { txId ->
                            navController.navigate(Screen.TransactionDetails.createRoute(txId))
                        }
                    )
                }
            }

            // Add Account
            composable(Screen.AddAccount.route) {
                AddEditAccountScreen(
                    initialAccount = null,
                    onSave = { newAccount ->
                        coroutineScope.launch {
                            repository.insertAccount(newAccount)
                            navController.popBackStack()
                        }
                    },
                    onBack = { navController.popBackStack() }
                )
            }

            // Edit Account
            composable(
                route = Screen.EditAccount.route,
                arguments = listOf(navArgument("accountId") { type = NavType.LongType })
            ) { backStackEntry ->
                val accId = backStackEntry.arguments?.getLong("accountId") ?: 0L
                val account = accounts.find { it.id == accId }
                AddEditAccountScreen(
                    initialAccount = account,
                    onSave = { updated ->
                        coroutineScope.launch {
                            repository.updateAccount(updated)
                            navController.popBackStack()
                        }
                    },
                    onDelete = { toDelete ->
                        coroutineScope.launch {
                            repository.deleteAccount(toDelete)
                            navController.navigate(Screen.Accounts.route) {
                                popUpTo(Screen.Accounts.route) { inclusive = true }
                            }
                        }
                    },
                    onBack = { navController.popBackStack() }
                )
            }

            // Add Transaction
            composable(
                route = Screen.AddTransaction.route,
                arguments = listOf(navArgument("type") { nullable = true; defaultValue = null })
            ) { backStackEntry ->
                val typeParam = backStackEntry.arguments?.getString("type")
                val preselectedType = when (typeParam?.uppercase()) {
                    "INCOME" -> TransactionType.INCOME
                    "TRANSFER" -> TransactionType.TRANSFER
                    "EXPENSE" -> TransactionType.EXPENSE
                    else -> null
                }
                AddEditTransactionScreen(
                    initialTransaction = null,
                    preselectedType = preselectedType,
                    accounts = accounts,
                    categories = categories,
                    onSave = { newTx ->
                        try {
                            repository.insertTransaction(newTx)
                            true
                        } catch (e: Exception) {
                            false
                        }
                    },
                    onNavigateToAddAccount = { navController.navigate(Screen.AddAccount.route) },
                    onNavigateToCategories = { navController.navigate(Screen.Categories.route) },
                    onBack = { navController.popBackStack() }
                )
            }

            // Edit Transaction
            composable(
                route = Screen.EditTransaction.route,
                arguments = listOf(navArgument("txId") { type = NavType.LongType })
            ) { backStackEntry ->
                val txId = backStackEntry.arguments?.getLong("txId") ?: 0L
                val tx = transactions.find { it.id == txId }
                AddEditTransactionScreen(
                    initialTransaction = tx,
                    accounts = accounts,
                    categories = categories,
                    onSave = { updated ->
                        try {
                            repository.updateTransaction(updated)
                            true
                        } catch (e: Exception) {
                            false
                        }
                    },
                    onNavigateToAddAccount = { navController.navigate(Screen.AddAccount.route) },
                    onNavigateToCategories = { navController.navigate(Screen.Categories.route) },
                    onBack = { navController.popBackStack() }
                )
            }

            // History
            composable(Screen.History.route) {
                TransactionHistoryScreen(
                    transactions = transactions,
                    categories = categories,
                    accounts = accounts,
                    hideAmounts = userPreferences.hideAmounts,
                    isDataLoaded = isDataLoaded,
                    onNavigateToDetails = { id ->
                        navController.navigate(Screen.TransactionDetails.createRoute(id))
                    },
                    onNavigateToAdd = { navController.navigate(Screen.AddTransaction.route) },
                    onNavigateToFavorites = { navController.navigate(Screen.Favorites.route) },
                    onToggleFavorite = { tx ->
                        coroutineScope.launch { repository.toggleFavorite(tx) }
                    }
                )
            }

            // Favorites
            composable(Screen.Favorites.route) {
                FavoritesScreen(
                    favoriteTransactions = favoriteTransactions,
                    categories = categories,
                    accounts = accounts,
                    hideAmounts = userPreferences.hideAmounts,
                    onToggleFavorite = { tx ->
                        coroutineScope.launch { repository.toggleFavorite(tx) }
                    },
                    onNavigateToDetails = { id ->
                        navController.navigate(Screen.TransactionDetails.createRoute(id))
                    },
                    onNavigateToHistory = {
                        navController.navigate(Screen.History.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        }
                    },
                    onBack = { navController.popBackStack() }
                )
            }

            // Transaction Details
            composable(
                route = Screen.TransactionDetails.route,
                arguments = listOf(navArgument("txId") { type = NavType.LongType })
            ) { backStackEntry ->
                val txId = backStackEntry.arguments?.getLong("txId") ?: 0L
                val tx = transactions.find { it.id == txId }
                if (tx != null) {
                    val cat = categories.find { it.id == tx.categoryId }
                    val acc = accounts.find { it.id == tx.accountId }
                    val toAcc = tx.toAccountId?.let { targetId -> accounts.find { it.id == targetId } }

                    TransactionDetailsScreen(
                        transaction = tx,
                        category = cat,
                        account = acc,
                        toAccount = toAcc,
                        hideAmounts = userPreferences.hideAmounts,
                        onBack = { navController.popBackStack() },
                        onEdit = { navController.navigate(Screen.EditTransaction.createRoute(txId)) },
                        onDuplicate = {
                            coroutineScope.launch {
                                repository.insertTransaction(
                                    tx.copy(id = 0, dateEpochMillis = System.currentTimeMillis())
                                )
                                navController.popBackStack()
                            }
                        },
                        onDelete = {
                            coroutineScope.launch {
                                repository.deleteTransaction(tx)
                                navController.popBackStack()
                            }
                        },
                        onToggleFavorite = {
                            coroutineScope.launch {
                                repository.toggleFavorite(tx)
                            }
                        }
                    )
                }
            }

            // Reports
            composable(Screen.Reports.route) {
                ReportsScreen(
                    transactions = transactions,
                    categories = categories,
                    hideAmounts = userPreferences.hideAmounts,
                    firstDayOfWeek = userPreferences.firstDayOfWeek,
                    onNavigateToCategoryBreakdown = { navController.navigate(Screen.Categories.route) },
                    onNavigateToCalendar = { navController.navigate(Screen.Calendar.route) }
                )
            }

            // Calendar
            composable(Screen.Calendar.route) {
                CalendarScreen(
                    transactions = transactions,
                    categories = categories,
                    accounts = accounts,
                    hideAmounts = userPreferences.hideAmounts,
                    onBack = { navController.popBackStack() },
                    onNavigateToTransactionDetails = { id ->
                        navController.navigate(Screen.TransactionDetails.createRoute(id))
                    }
                )
            }

            // Budgets
            composable(Screen.Budgets.route) {
                BudgetsScreen(
                    budgets = budgets,
                    accounts = accounts,
                    categories = categories,
                    transactions = transactions,
                    hideAmounts = userPreferences.hideAmounts,
                    onSaveBudget = { newBudget ->
                        coroutineScope.launch {
                            repository.insertBudget(newBudget)
                        }
                    },
                    onUpdateBudget = { updatedBudget ->
                        coroutineScope.launch {
                            repository.updateBudget(updatedBudget)
                        }
                    },
                    onDeleteBudget = { budgetToDelete ->
                        coroutineScope.launch {
                            repository.deleteBudget(budgetToDelete)
                        }
                    },
                    onBack = { navController.popBackStack() }
                )
            }

            // Categories
            composable(Screen.Categories.route) {
                CategoryManagementScreen(
                    categories = categories,
                    transactions = transactions,
                    onBack = { navController.popBackStack() },
                    onAddCategory = { newCat ->
                        coroutineScope.launch {
                            repository.insertCategory(newCat)
                        }
                    },
                    onUpdateCategory = { updatedCat ->
                        coroutineScope.launch {
                            repository.updateCategory(updatedCat)
                        }
                    },
                    onDeleteCategory = { catToDelete, reassignToId ->
                        coroutineScope.launch {
                            repository.deleteCategory(catToDelete, reassignToId)
                        }
                    },
                    onArchiveCategory = { catId ->
                        coroutineScope.launch {
                            repository.archiveCategory(catId)
                        }
                    },
                    onToggleFavoriteCategory = { cat ->
                        coroutineScope.launch {
                            repository.toggleCategoryFavorite(cat)
                        }
                    }
                )
            }

            // Smart Entry Menu
            composable(Screen.SmartEntry.route) {
                SmartEntryMenuScreen(
                    onBack = { navController.popBackStack() },
                    onNavigateToManualAdd = { navController.navigate(Screen.AddTransaction.route) },
                    onImageSelectedForOcr = { uri ->
                        selectedImageUri = uri
                        navController.navigate(Screen.OcrReview.route)
                    },
                    onPdfSelectedForImport = { uri ->
                        navController.navigate(Screen.PdfImport.route)
                    }
                )
            }

            // OCR Review
            composable(Screen.OcrReview.route) {
                OcrReviewScreen(
                    initialExtracted = null,
                    imageUri = selectedImageUri ?: incomingImageUri,
                    accounts = accounts,
                    categories = categories,
                    repository = repository,
                    onSave = { tx ->
                        coroutineScope.launch {
                            repository.insertTransaction(tx)
                            selectedImageUri = null
                            onClearSharedImageUri()
                            navController.navigate(Screen.History.route) {
                                popUpTo(Screen.Home.route)
                            }
                        }
                    },
                    onNavigateToManualAdd = { navController.navigate(Screen.AddTransaction.route) },
                    onBack = {
                        selectedImageUri = null
                        onClearSharedImageUri()
                        navController.popBackStack()
                    }
                )
            }

            // PDF Import
            composable(Screen.PdfImport.route) {
                val parseResult = remember(transactions) {
                    PdfStatementParser.parseStatementText("HDFC_Bank_Statement_May2026.pdf", "", transactions)
                }
                PdfStatementImportScreen(
                    candidates = parseResult.candidates,
                    onImportSelected = { selectedCandidates ->
                        val hdfcId = accounts.firstOrNull()?.id ?: 1L
                        val txList = selectedCandidates.map { cand ->
                            TransactionEntity(
                                type = cand.transactionType,
                                amountPaise = cand.amountPaise,
                                accountId = hdfcId,
                                dateEpochMillis = cand.dateEpochMillis,
                                merchantOrPayee = cand.merchantOrDescription,
                                referenceId = cand.referenceId,
                                isImported = true
                            )
                        }
                        coroutineScope.launch {
                            repository.insertTransactions(txList)
                            navController.navigate(Screen.History.route) {
                                popUpTo(Screen.Home.route)
                            }
                        }
                    },
                    onNavigateToDuplicateResolution = { candidate ->
                        // Can view duplicate
                    },
                    onBack = { navController.popBackStack() }
                )
            }

            // Settings
            composable(Screen.Settings.route) {
                SettingsScreen(
                    preferences = userPreferences,
                    onSetThemeMode = { mode ->
                        coroutineScope.launch { appPreferences.setThemeMode(mode) }
                    },
                    onSetLanguage = { lang ->
                        coroutineScope.launch {
                            appPreferences.setLanguage(lang)
                            try {
                                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                                    val localeManager = context.getSystemService(android.app.LocaleManager::class.java)
                                    localeManager?.applicationLocales = android.os.LocaleList.forLanguageTags(lang)
                                } else {
                                    val loc = java.util.Locale(lang)
                                    java.util.Locale.setDefault(loc)
                                    val config = context.resources.configuration
                                    config.setLocale(loc)
                                    @Suppress("DEPRECATION")
                                    context.resources.updateConfiguration(config, context.resources.displayMetrics)
                                }
                            } catch (e: Exception) {
                                java.util.Locale.setDefault(java.util.Locale(lang))
                            }
                        }
                    },
                    onNavigateToCategories = { navController.navigate(Screen.Categories.route) },
                    onNavigateToBackup = { navController.navigate(Screen.Backup.route) },
                    onNavigateToPrivacy = { navController.navigate(Screen.Privacy.route) },
                    onNavigateToWidgetSettings = { navController.navigate(Screen.Settings.route) },
                    onBack = { navController.popBackStack() }
                )
            }

            // Backup & Restore
            composable(Screen.Backup.route) {
                BackupRestoreScreen(
                    accounts = accounts,
                    categories = categories,
                    transactions = transactions,
                    budgets = budgets,
                    onRestore = { restoredAccs, restoredCats, restoredTxs ->
                        coroutineScope.launch {
                            repository.restoreAllData(restoredAccs, restoredCats, restoredTxs, emptyList())
                        }
                    },
                    onClearAllData = { onFinished ->
                        coroutineScope.launch {
                            repository.clearAllData()
                            onFinished()
                        }
                    },
                    onBack = { navController.popBackStack() }
                )
            }

            // Privacy & App Lock
            composable(Screen.Privacy.route) {
                AppLockPrivacyScreen(
                    preferences = userPreferences,
                    onRequestStartPinSetup = {
                        coroutineScope.launch {
                            if (pinSecurityManager.isPinSet()) {
                                appPreferences.setAppLockEnabled(true)
                            } else {
                                navController.navigate(Screen.PinSetup.route)
                            }
                        }
                    },
                    onChangePinRequested = {
                        navController.navigate(Screen.ChangePin.route)
                    },
                    onDisableAppLockConfirmed = {
                        coroutineScope.launch {
                            appPreferences.setAppLockEnabled(false)
                        }
                    },
                    onVerifyCurrentPinForDisable = { pin ->
                        pinSecurityManager.verifyPin(pin)
                    },
                    onSetAutoLockMinutes = { mins ->
                        coroutineScope.launch { appPreferences.setAutoLockMinutes(mins) }
                    },
                    onSetHideAmounts = { hide ->
                        coroutineScope.launch { appPreferences.setHideAmounts(hide) }
                    },
                    onSetBlockScreenshots = { block ->
                        coroutineScope.launch { appPreferences.setBlockScreenshots(block) }
                    },
                    onBack = { navController.popBackStack() }
                )
            }

            // PIN Setup Flow
            composable(Screen.PinSetup.route) {
                PinSetupScreen(
                    onPinSetupComplete = { newPin ->
                        coroutineScope.launch {
                            pinSecurityManager.savePin(newPin)
                            appPreferences.setAppLockEnabled(true)
                            navController.popBackStack()
                        }
                    },
                    onBack = { navController.popBackStack() }
                )
            }

            // Change PIN Flow
            composable(Screen.ChangePin.route) {
                ChangePinScreen(
                    onVerifyCurrentPin = { pin -> pinSecurityManager.verifyPin(pin) },
                    onChangePinComplete = { newPin ->
                        coroutineScope.launch {
                            pinSecurityManager.savePin(newPin)
                            navController.popBackStack()
                        }
                    },
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}

@Composable
fun MoneyMapBottomBar(
    currentRoute: String,
    onNavigate: (Screen) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Home
            BottomNavItem(
                screen = Screen.Home,
                isSelected = currentRoute == Screen.Home.route,
                onClick = { onNavigate(Screen.Home) }
            )

            // Accounts
            BottomNavItem(
                screen = Screen.Accounts,
                isSelected = currentRoute == Screen.Accounts.route,
                onClick = { onNavigate(Screen.Accounts) }
            )

            // Central Floating Add '+' Button
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(MoneyMapBlue)
                    .clickable { onNavigate(Screen.Add) }
                    .testTag("bottom_nav_add_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Add Transaction",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }

            // History
            BottomNavItem(
                screen = Screen.History,
                isSelected = currentRoute == Screen.History.route,
                onClick = { onNavigate(Screen.History) }
            )

            // Reports
            BottomNavItem(
                screen = Screen.Reports,
                isSelected = currentRoute == Screen.Reports.route,
                onClick = { onNavigate(Screen.Reports) }
            )
        }
    }
}

@Composable
fun BottomNavItem(
    screen: Screen,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .testTag("nav_${screen.route}"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        screen.icon?.let { icon ->
            Icon(
                imageVector = icon,
                contentDescription = screen.title,
                tint = if (isSelected) MoneyMapBlue else TextSecondary,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = screen.title,
            color = if (isSelected) MoneyMapBlue else TextSecondary,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            fontSize = 10.sp
        )
    }
}

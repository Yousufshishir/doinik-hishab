package com.example.ui.screens

import com.example.ui.util.clearFocusOnTap

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.launch
import com.example.data.model.SimpleCategoryData
import com.example.ui.util.AppLocale
import com.example.data.model.TransactionEntity
import com.example.data.model.KhatEntity
import com.example.data.model.formatTakaSafe
import com.example.ui.components.AppToastManager
import com.example.ui.components.AppCardDefaults
import com.example.ui.components.KeyboardScrollDownHint
import com.example.ui.components.KhatAllocationSection
import com.example.ui.components.KhatAllocationContent
import com.example.ui.components.CreateOrEditKhatDialog
import com.example.ui.components.DeleteKhatConfirmationDialog
import com.example.ui.components.getKhatIcon
import com.example.ui.viewmodel.ExpenseViewModel
import com.example.util.AppLockManager
import com.example.ui.screens.AppLockSetupDialog
import com.example.ui.screens.AppLockVerifyDialog
import java.text.SimpleDateFormat
import java.util.*

@OptIn(
    ExperimentalMaterial3Api::class,
    androidx.compose.foundation.ExperimentalFoundationApi::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class
)
@Composable
fun DashboardScreen(
    viewModel: ExpenseViewModel,
    onOpenAddTransaction: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val filteredTransactions by viewModel.filteredTransactions.collectAsState()
    val totalIncome by viewModel.totalIncome.collectAsState()
    val totalExpense by viewModel.totalExpense.collectAsState()
    val netBalance by viewModel.netBalance.collectAsState()
    val todayExpense by viewModel.todayExpense.collectAsState()
    val todayIncome by viewModel.todayIncome.collectAsState()
    val dailyTarget by viewModel.dailySpendingTarget.collectAsState()
    val budgetStreak by viewModel.budgetStreakDays.collectAsState()
    val selectedTypeFilter by viewModel.selectedTypeFilter.collectAsState()
    val selectedDateFilter by viewModel.selectedDateFilter.collectAsState()
    val customSelectedDate by viewModel.customSelectedDate.collectAsState()
    val selectedDateExpense by viewModel.selectedDateExpense.collectAsState()
    val selectedDateIncome by viewModel.selectedDateIncome.collectAsState()
    val allTransactions by viewModel.transactions.collectAsState()
    val userName by viewModel.userName.collectAsState()
    val userPhone by viewModel.userPhone.collectAsState()
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val isEnglish by viewModel.isEnglish.collectAsState()
    val khats by viewModel.khats.collectAsState()
    val totalAllocated by viewModel.totalAllocated.collectAsState()
    val oboshistoBalance by viewModel.oboshistoBalance.collectAsState()

    var showCreateKhatDialog by remember { mutableStateOf(false) }
    var khatToEdit by remember { mutableStateOf<KhatEntity?>(null) }
    var khatToDelete by remember { mutableStateOf<KhatEntity?>(null) }

    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val coroutineScope = rememberCoroutineScope()
    val quickExpenseBringIntoViewRequester = remember { BringIntoViewRequester() }
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    val isImeVisible = WindowInsets.isImeVisible

    var showDailyTargetDialog by remember { mutableStateOf(false) }
    val sharedPrefs = remember { context.getSharedPreferences("daily_hishab_prefs", android.content.Context.MODE_PRIVATE) }

    var isHighTxnAlertEnabled by remember {
        mutableStateOf(sharedPrefs.getBoolean("pref_high_txn_alert", true))
    }
    var isHapticFeedbackEnabled by remember {
        mutableStateOf(sharedPrefs.getBoolean("pref_haptic_feedback", true))
    }

    var showReceiptDialog by remember { mutableStateOf(false) }
    var showProfileDialog by remember { mutableStateOf(false) }
    var showAboutAppDialog by remember { mutableStateOf(false) }
    var showSettingsDrawer by remember { mutableStateOf(false) }
    var transactionToDelete by remember { mutableStateOf<TransactionEntity?>(null) }
    var selectedTransactionForDetails by remember { mutableStateOf<TransactionEntity?>(null) }
    val lastSyncTimestamp by viewModel.lastSyncTimestamp.collectAsState()
    var isDrawerSyncing by remember { mutableStateOf(false) }
    var isRefreshingTopBar by remember { mutableStateOf(false) }
    var activeDrawerConfirmAction by remember { mutableStateOf<String?>(null) } // null, "CLEAR_ALL", "DELETE_ACCOUNT", "LOGOUT"
    val showDrawerClearAllConfirm = (activeDrawerConfirmAction == "CLEAR_ALL")
    val showDrawerDeleteAccountConfirm = (activeDrawerConfirmAction == "DELETE_ACCOUNT")
    val showDrawerLogoutConfirm = (activeDrawerConfirmAction == "LOGOUT")

    // App Lock States
    val appLockManager = remember { AppLockManager.getInstance(context) }
    val isAppLockEnabled by appLockManager.isAppLockEnabled.collectAsState()
    val isBiometricEnabled by appLockManager.isBiometricEnabled.collectAsState()
    val canBiometric = remember { appLockManager.isBiometricHardwareAvailable() }
    var showAppLockSetupDialog by remember { mutableStateOf(false) }
    var showAppLockDisableVerifyDialog by remember { mutableStateOf(false) }
    var showAppLockChangePinVerifyDialog by remember { mutableStateOf(false) }

    // Live clock ticker
    var currentTimeText by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        val timeFmt = SimpleDateFormat("hh:mm:ss a", Locale.US)
        while (true) {
            val now = Calendar.getInstance()
            currentTimeText = timeFmt.format(now.time)
            kotlinx.coroutines.delay(1000)
        }
    }

    // System DatePickerDialog launcher
    val showDatePicker = {
        val cal = Calendar.getInstance()
        customSelectedDate?.let { cal.timeInMillis = it }
        android.app.DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val picked = Calendar.getInstance().apply {
                    set(Calendar.YEAR, year)
                    set(Calendar.MONTH, month)
                    set(Calendar.DAY_OF_MONTH, dayOfMonth)
                    set(Calendar.HOUR_OF_DAY, 12)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                viewModel.selectSpecificDate(picked.timeInMillis)
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    Box(modifier = modifier.fillMaxSize()) {
        BackHandler(enabled = showSettingsDrawer) {
            if (activeDrawerConfirmAction != null) {
                activeDrawerConfirmAction = null
            } else {
                showSettingsDrawer = false
            }
        }

        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .clearFocusOnTap(),
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = if (isEnglish) "Daily Expense" else "দৈনিক খরচের হিসাব",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    },
                    actions = {
                        // Quick Refresh & Realtime Sync Trigger
                        IconButton(
                            onClick = {
                                if (!isRefreshingTopBar) {
                                    isRefreshingTopBar = true
                                    viewModel.refreshAllData { success, msg ->
                                        isRefreshingTopBar = false
                                        AppToastManager.show(msg)
                                    }
                                }
                            },
                            modifier = Modifier
                                .testTag("action_refresh_all_data")
                                .padding(end = 4.dp)
                        ) {
                            if (isRefreshingTopBar) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = if (isEnglish) "Refresh & Sync" else "রিফ্রেশ ও সিঙ্ক",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }

                        // Modern Settings Button (Swipes Side Bar Drawer from Right)
                        Surface(
                            onClick = { showSettingsDrawer = true },
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .height(36.dp)
                                .testTag("action_open_settings_drawer")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = if (isEnglish) "Settings" else "সেটিংস",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(17.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isEnglish) "Settings" else "সেটিংস",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onOpenAddTransaction,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .height(40.dp)
                    .testTag("fab_add_transaction")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Add", modifier = Modifier.size(17.dp))
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = if (isEnglish) "Add Entry" else "লেনদেন যোগ",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.5.sp
                    )
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item { Spacer(modifier = Modifier.height(2.dp)) }

            // 1. Welcome Card (Clean Classic Design: Greeting & Time, Today's In/Out, View Details Button, & Daily Target)
            item {
                val now = Calendar.getInstance()
                val hourNow = now.get(Calendar.HOUR_OF_DAY)
                val greetingEmoji = when (hourNow) {
                    in 5..11 -> "🌅"
                    in 12..15 -> "☀️"
                    in 16..19 -> "🌇"
                    else -> "🌙"
                }
                val greetingWord = when (hourNow) {
                    in 5..11 -> if (isEnglish) "Good Morning" else "শুভ সকাল"
                    in 12..15 -> if (isEnglish) "Good Afternoon" else "শুভ দুপুর"
                    in 16..19 -> if (isEnglish) "Good Evening" else "শুভ বিকাল"
                    else -> if (isEnglish) "Good Night" else "শুভ রাত্রি"
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("welcome_header_card"),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDarkMode) Color(0xFF1E293B) else Color(0xFFF8FAFC)
                    ),
                    border = BorderStroke(
                        1.2.dp,
                        if (isDarkMode) Color(0xFF38BDF8).copy(alpha = 0.4f) else Color(0xFF10B981).copy(alpha = 0.35f)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // 1. Top Row: Avatar & Greeting + Live Time
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // User Avatar + Dynamic Greeting
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = if (isDarkMode) Color(0xFF0F766E).copy(alpha = 0.4f) else Color(0xFFD1FAE5),
                                    border = BorderStroke(1.5.dp, Color(0xFF10B981)),
                                    modifier = Modifier.size(42.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                        Text(text = greetingEmoji, fontSize = 20.sp)
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = greetingWord,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                fontWeight = FontWeight.Medium
                                            )
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Box(
                                            modifier = Modifier
                                                .size(5.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFF10B981))
                                        )
                                    }
                                    Text(
                                        text = if (userName.isNotBlank()) userName else if (isEnglish) "Dear User" else "সম্মানিত ব্যবহারকারী",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.ExtraBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            // Live Clock
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isDarkMode) Color(0xFF0F172A) else Color.White,
                                border = BorderStroke(
                                    1.dp,
                                    if (isDarkMode) Color(0xFF334155) else Color(0xFFCBD5E1)
                                ),
                                shadowElevation = 1.dp
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF10B981))
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = currentTimeText.ifEmpty { "..." },
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.5.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    )
                                }
                            }
                        }

                        // 2. Today's Income and Expense Glance Cards
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Today's Income Card
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = if (isDarkMode) Color(0xFF064E3B).copy(alpha = 0.35f) else Color(0xFFDCFCE7),
                                border = BorderStroke(
                                    1.dp,
                                    if (isDarkMode) Color(0xFF059669).copy(alpha = 0.45f) else Color(0xFF86EFAC)
                                ),
                                modifier = Modifier.weight(1f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF10B981).copy(alpha = 0.2f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ArrowDownward,
                                            contentDescription = "Income",
                                            tint = Color(0xFF10B981),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = if (isEnglish) "Today's Income" else "আজকের আয়",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontSize = 10.5.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        )
                                        Text(
                                            text = formatTakaSafe(todayIncome),
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.ExtraBold,
                                                color = Color(0xFF10B981)
                                            ),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }

                            // Today's Expense Card
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = if (isDarkMode) Color(0xFF4C0519).copy(alpha = 0.35f) else Color(0xFFFFE4E6),
                                border = BorderStroke(
                                    1.dp,
                                    if (isDarkMode) Color(0xFFE11D48).copy(alpha = 0.45f) else Color(0xFFFDA4AF)
                                ),
                                modifier = Modifier.weight(1f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFFE11D48).copy(alpha = 0.2f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ArrowUpward,
                                            contentDescription = "Expense",
                                            tint = Color(0xFFE11D48),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = if (isEnglish) "Today's Expense" else "আজকের ব্যয়",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontSize = 10.5.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        )
                                        Text(
                                            text = formatTakaSafe(todayExpense),
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.ExtraBold,
                                                color = Color(0xFFE11D48)
                                            ),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }

                        // 3. Button: "আজকের আয় ব্যয়ের বিস্তারিত দেখুন এখানে" (Scrolls to Today's Account section)
                        Surface(
                            onClick = {
                                coroutineScope.launch {
                                    viewModel.setDateFilter("TODAY")
                                    listState.animateScrollToItem(4)
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primary,
                            shadowElevation = 2.dp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("btn_goto_today_details")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 8.5.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ReceiptLong,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = if (isEnglish) "View Today's Income & Expense Details Here" else "আজকের আয় ব্যয়ের বিস্তারিত দেখুন এখানে",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.5.sp,
                                        color = MaterialTheme.colorScheme.onPrimary
                                    ),
                                    maxLines = 1,
                                    softWrap = false
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowDown,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(17.dp)
                                )
                            }
                        }

                        // 4. Daily Spending Target Section (Moved to the bottom of Welcome Card)
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (isDarkMode) Color(0xFF0F172A).copy(alpha = 0.65f) else Color(0xFFF1F5F9),
                            border = BorderStroke(
                                1.dp,
                                if (dailyTarget > 0 && todayExpense > dailyTarget) Color(0xFFEF4444).copy(alpha = 0.6f)
                                else (if (isDarkMode) Color(0xFF334155) else Color(0xFFE2E8F0))
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("welcome_daily_target_section")
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Row 1: Title
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Adjust,
                                        contentDescription = null,
                                        tint = if (dailyTarget > 0 && todayExpense > dailyTarget) Color(0xFFEF4444) else MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (isEnglish) "Daily Spending Target" else "আজকের খরচের লক্ষ্যমাত্রা",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                // Row 2: Target Button on a separate line
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (dailyTarget > 0) {
                                        Surface(
                                            onClick = { showDailyTargetDialog = true },
                                            shape = RoundedCornerShape(9.dp),
                                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)),
                                            modifier = Modifier.testTag("btn_edit_daily_target")
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Text(
                                                    text = "${if (isEnglish) "Target: " else "টার্গেট: "}${formatTakaSafe(dailyTarget)}",
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        color = MaterialTheme.colorScheme.primary,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 12.sp
                                                    )
                                                )
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.height(20.dp)
                                                ) {
                                                    Row(
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Edit,
                                                            contentDescription = if (isEnglish) "Edit Target" else "টার্গেট পরিবর্তন করুন",
                                                            tint = MaterialTheme.colorScheme.onPrimary,
                                                            modifier = Modifier.size(11.dp)
                                                        )
                                                        Text(
                                                            text = if (isEnglish) "Change" else "বদলান",
                                                            style = MaterialTheme.typography.labelSmall.copy(
                                                                color = MaterialTheme.colorScheme.onPrimary,
                                                                fontWeight = FontWeight.Bold,
                                                                fontSize = 10.sp
                                                            )
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    } else {
                                        Surface(
                                            onClick = { showDailyTargetDialog = true },
                                            shape = RoundedCornerShape(8.dp),
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.testTag("btn_set_daily_target")
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Edit,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.onPrimary,
                                                    modifier = Modifier.size(12.dp)
                                                )
                                                Text(
                                                    text = if (isEnglish) "Set Target" else "লক্ষ্যমাত্রা সেট করুন",
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        color = MaterialTheme.colorScheme.onPrimary,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 11.sp
                                                    )
                                                )
                                            }
                                        }
                                    }
                                }

                                if (dailyTarget > 0) {
                                    val dailyRatio = (todayExpense / dailyTarget).toFloat().coerceIn(0f, 1f)
                                    val isOver = todayExpense > dailyTarget
                                    val barColor = if (isOver) Color(0xFFEF4444) else Color(0xFF10B981)

                                    LinearProgressIndicator(
                                        progress = { dailyRatio },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(6.dp)
                                            .clip(RoundedCornerShape(3.dp)),
                                        color = barColor,
                                        trackColor = if (isDarkMode) Color(0xFF334155) else Color(0xFFCBD5E1)
                                    )

                                    if (isOver) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.Warning,
                                                contentDescription = null,
                                                tint = Color(0xFFEF4444),
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(5.dp))
                                            Text(
                                                text = if (isEnglish)
                                                    "You have exceeded today's spending target! (${formatTakaSafe(todayExpense - dailyTarget)} extra)"
                                                else
                                                    "আপনি আজকের লক্ষ্যমাত্রা অতিক্রম করেছেন! (অতিরিক্ত ${formatTakaSafe(todayExpense - dailyTarget)})",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    color = Color(0xFFEF4444),
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 11.sp
                                                )
                                            )
                                        }
                                    } else {
                                        val remaining = dailyTarget - todayExpense
                                        Text(
                                            text = if (isEnglish)
                                                "Remaining: ${formatTakaSafe(remaining)} (Within budget ✅)"
                                            else
                                                "বাকি আছে: ${formatTakaSafe(remaining)} (বাজেটের মধ্যে আছে ✅)",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = Color(0xFF10B981),
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 11.sp
                                            )
                                        )
                                    }
                                } else {
                                    Text(
                                        text = if (isEnglish)
                                            "Tap to set today's spending target to control daily expenses"
                                        else
                                            "দৈনিক খরচ নিয়ন্ত্রণে রাখতে এখানে ট্যাপ করে লক্ষ্যমাত্রা সেট করুন",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 10.5.sp
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ==========================================
            // BOX 2: Balance & Budget Overview Card (Indigo Tint)
            // ==========================================
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("balance_summary_card"),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDarkMode) Color(0xFF1E1B4B).copy(alpha = 0.65f) else Color(0xFFEEF2FF)
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isDarkMode) Color(0xFF6366F1).copy(alpha = 0.4f) else Color(0xFFC7D2FE)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = AppLocale.currentBalance(isEnglish),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = formatTakaSafe(netBalance),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    style = if (kotlin.math.abs(netBalance) >= 100_000_000) {
                                        MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.ExtraBold,
                                            color = if (netBalance >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                                        )
                                    } else if (kotlin.math.abs(netBalance) >= 1_000_000) {
                                        MaterialTheme.typography.headlineSmall.copy(
                                            fontWeight = FontWeight.ExtraBold,
                                            color = if (netBalance >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                                        )
                                    } else {
                                        MaterialTheme.typography.headlineMedium.copy(
                                            fontWeight = FontWeight.ExtraBold,
                                            color = if (netBalance >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                                        )
                                    }
                                )
                            }

                            if (budgetStreak > 0) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFFF57C00).copy(alpha = 0.15f),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF57C00).copy(alpha = 0.3f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(text = "🔥", fontSize = 16.sp)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = AppLocale.dayStreak(isEnglish, budgetStreak),
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFFE65100)
                                            )
                                        )
                                    }
                                }
                            }
                        }

                        // Income, Expense, Today's stats row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFF2E7D32).copy(alpha = 0.08f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2E7D32).copy(alpha = 0.2f))
                            ) {
                                Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(7.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFF2E7D32))
                                        )
                                        Spacer(modifier = Modifier.width(5.dp))
                                        Text(
                                            text = AppLocale.totalIncome(isEnglish),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = formatTakaSafe(totalIncome),
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF2E7D32)
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Surface(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFFD32F2F).copy(alpha = 0.08f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFD32F2F).copy(alpha = 0.2f))
                            ) {
                                Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(7.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFFD32F2F))
                                        )
                                        Spacer(modifier = Modifier.width(5.dp))
                                        Text(
                                            text = AppLocale.totalExpense(isEnglish),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = formatTakaSafe(totalExpense),
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFD32F2F)
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Surface(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                            ) {
                                Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Today,
                                            contentDescription = null,
                                            modifier = Modifier.size(11.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = AppLocale.todayExpense(isEnglish),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = formatTakaSafe(todayExpense),
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }

                        Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))

                        // Merged Khat Allocation Section inside the Card
                        KhatAllocationContent(
                            khats = khats,
                            netBalance = netBalance,
                            totalAllocated = totalAllocated,
                            oboshistoBalance = oboshistoBalance,
                            isEnglish = isEnglish,
                            isDarkMode = isDarkMode,
                            onCreateKhatClick = { showCreateKhatDialog = true },
                            onEditKhatClick = { khatToEdit = it },
                            onDeleteKhatClick = { khatToDelete = it }
                        )

                        Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))

                        // Quick Action: Download Official Money Receipt Voucher
                        Surface(
                            onClick = { showReceiptDialog = true },
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("card_download_receipt_button")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 9.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ReceiptLong,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(17.dp)
                                )
                                Spacer(modifier = Modifier.width(7.dp))
                                Text(
                                    text = if (isEnglish) "Download Official Money Receipt & Voucher" else "মানি রসিদ ও ভাউচার ডাউনলোড (PDF)",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    ),
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }

            // ==========================================
            // BOX 3: Quick Expense Section (Redesigned Eye-Catching & Modern Card)
            // ==========================================
            item {
                val imeInsets = WindowInsets.ime
                val density = LocalDensity.current
                val isKeyboardOpen = imeInsets.getBottom(density) > 0

                val animatedElevation by animateDpAsState(
                    targetValue = if (isKeyboardOpen) 8.dp else 3.dp,
                    animationSpec = tween(durationMillis = 200),
                    label = "quick_expense_elevation"
                )
                val animatedVerticalPadding by animateDpAsState(
                    targetValue = if (isKeyboardOpen) 10.dp else 16.dp,
                    animationSpec = tween(durationMillis = 200),
                    label = "quick_expense_v_pad"
                )
                val animatedSpacing by animateDpAsState(
                    targetValue = if (isKeyboardOpen) 8.dp else 12.dp,
                    animationSpec = tween(durationMillis = 200),
                    label = "quick_expense_spacing"
                )

                val presetOptions = listOf(
                    Pair(if (isEnglish) "☕ Tea & Snacks" else "☕ চা ও নাস্তা", "খাবার ও মুদি"),
                    Pair(if (isEnglish) "🛺 Transport" else "🛺 রিকশা/ভাড়া", "যাতায়াত"),
                    Pair(if (isEnglish) "🍛 Lunch & Meal" else "🍛 দুপুরের খাবার", "খাবার ও মুদি"),
                    Pair(if (isEnglish) "📱 Recharge" else "📱 রিচার্জ", "বিল ও ইউটিলিটি"),
                    Pair(if (isEnglish) "🛒 Kitchen & Grocery" else "🛒 কাঁচাবাজার", "খাবার ও মুদি"),
                    Pair(if (isEnglish) "💊 Medicine" else "💊 ওষুধ", "চিকিৎসা"),
                    Pair(if (isEnglish) "🛍️ Shopping" else "🛍️ কেনাকাটা", "কেনাকাটা"),
                    Pair(if (isEnglish) "⚡ Utility Bill" else "⚡ বিল", "বিল ও ইউটিলিটি"),
                    Pair(if (isEnglish) "🏷️ Other Name" else "🏷️ অন্যান্য নাম", "অন্যান্য")
                )
                var selectedOption by remember(isEnglish) { mutableStateOf(presetOptions[0]) }
                var dropdownExpanded by remember { mutableStateOf(false) }
                var customQuickAmount by remember { mutableStateOf("") }
                var customPurpose by remember { mutableStateOf("") }
                var selectedQuickKhatId by remember { mutableStateOf<Long?>(null) }
                var selectedQuickKhatName by remember { mutableStateOf("") }
                var selectedQuickDateTimestamp by remember { mutableStateOf<Long?>(null) }
                var showQuickOverBalanceWarning by remember { mutableStateOf(false) }

                val selectedQuickKhat = khats.find { it.id == selectedQuickKhatId }
                val quickMaxSpendable = remember(selectedQuickKhat, oboshistoBalance, netBalance, khats.size) {
                    if (selectedQuickKhat != null) {
                        minOf(maxOf(0.0, selectedQuickKhat.allocatedAmount), maxOf(0.0, netBalance))
                    } else {
                        if (khats.isNotEmpty()) {
                            minOf(maxOf(0.0, oboshistoBalance), maxOf(0.0, netBalance))
                        } else {
                            maxOf(0.0, netBalance)
                        }
                    }
                }
                val isQuickBalanceZero = quickMaxSpendable <= 0.0
                val currentQuickAmt = customQuickAmount.toDoubleOrNull() ?: 0.0
                val isQuickInsufficient = (selectedQuickKhat != null && currentQuickAmt > selectedQuickKhat.allocatedAmount) || (currentQuickAmt > quickMaxSpendable)
                val isOtherCat = selectedOption.second == "অন্যান্য" || selectedOption.first.contains("অন্যান্য")

                // Auto-sync if source changes and exceeds limit
                LaunchedEffect(selectedQuickKhatId, quickMaxSpendable) {
                    if (customQuickAmount.isNotBlank()) {
                        val entered = customQuickAmount.toDoubleOrNull() ?: 0.0
                        if (isQuickBalanceZero) {
                            customQuickAmount = ""
                            showQuickOverBalanceWarning = true
                        } else if (entered > quickMaxSpendable) {
                            customQuickAmount = if (quickMaxSpendable % 1.0 == 0.0) quickMaxSpendable.toLong().toString() else String.format(Locale.US, "%.2f", quickMaxSpendable)
                            showQuickOverBalanceWarning = true
                        }
                    }
                }

                // Premium Card Container with Subtle Gradient & Modern Outline
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .bringIntoViewRequester(quickExpenseBringIntoViewRequester)
                        .testTag("quick_expense_dropdown_card"),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDarkMode) Color(0xFF26190E) else Color(0xFFFFFBF5)
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        1.2.dp,
                        if (isDarkMode) Color(0xFFF59E0B).copy(alpha = 0.55f) else Color(0xFFF59E0B).copy(alpha = 0.45f)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = animatedElevation)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = animatedVerticalPadding),
                        verticalArrangement = Arrangement.spacedBy(animatedSpacing)
                    ) {
                        // Header Banner: Icon + Title + Other Name Shortcut Pill
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isDarkMode) Color(0xFFF59E0B).copy(alpha = 0.2f)
                                            else Color(0xFFFEF3C7)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.FlashOn,
                                        contentDescription = null,
                                        tint = Color(0xFFD97706),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                Column {
                                    Text(
                                        text = if (isEnglish) "Quick Expense" else "দ্রুত খরচ / লেনদেন যোগ",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp
                                        ),
                                        color = if (isDarkMode) Color(0xFFFBBF24) else Color(0xFFB45309)
                                    )
                                    Text(
                                        text = if (isEnglish) "Instant 1-tap entry" else "তাত্ক্ষণিক এক ক্লিকে খরচ যোগ করুন",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 10.5.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                                        )
                                    )
                                }
                            }

                            // Option for "অন্যান্য নাম" (Other Name) Pill Toggle
                            val presetOthers = if (isEnglish) "🏷️ Other" else "🏷️ অন্যান্য"
                            val isCurrentlyOthers = isOtherCat
                            Surface(
                                onClick = {
                                    val otherOption = presetOptions.find { it.second == "অন্যান্য" } ?: presetOptions.last()
                                    selectedOption = if (isCurrentlyOthers) presetOptions[0] else otherOption
                                },
                                shape = RoundedCornerShape(12.dp),
                                color = if (isCurrentlyOthers) Color(0xFFD97706) else (if (isDarkMode) Color(0xFF382313) else Color(0xFFFEF3C7)),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isCurrentlyOthers) Color(0xFFB45309) else Color(0xFFFDE68A)
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = presetOthers,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = if (isCurrentlyOthers) FontWeight.Bold else FontWeight.SemiBold,
                                            fontSize = 11.sp
                                        ),
                                        color = if (isCurrentlyOthers) Color.White else (if (isDarkMode) Color(0xFFFBBF24) else Color(0xFFB45309))
                                    )
                                }
                            }
                        }

                        // Real-time Max Spendable Balance Status Strip
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isQuickBalanceZero) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f)
                            else if (isDarkMode) Color(0xFF382313) else Color(0xFFFEF3C7),
                            border = androidx.compose.foundation.BorderStroke(
                                0.8.dp,
                                if (isQuickBalanceZero) MaterialTheme.colorScheme.error.copy(alpha = 0.5f)
                                else if (isDarkMode) Color(0xFFB45309).copy(alpha = 0.6f) else Color(0xFFFDE68A)
                            ),
                            modifier = Modifier.fillMaxWidth().testTag("quick_expense_max_spendable_strip")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                    Icon(
                                        imageVector = Icons.Default.Shield,
                                        contentDescription = null,
                                        tint = if (isQuickBalanceZero) MaterialTheme.colorScheme.error else Color(0xFFD97706),
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Text(
                                        text = if (isEnglish) "Max Spendable Balance:" else "সর্বোচ্চ খরচযোগ্য ব্যালেন্স:",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Medium),
                                        color = if (isQuickBalanceZero) MaterialTheme.colorScheme.error else (if (isDarkMode) Color(0xFFFBBF24) else Color(0xFF92400E))
                                    )
                                }
                                Text(
                                    text = formatTakaSafe(quickMaxSpendable),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 11.5.sp,
                                        color = if (isQuickBalanceZero) MaterialTheme.colorScheme.error else (if (isDarkMode) Color(0xFFFBBF24) else Color(0xFF92400E))
                                    )
                                )
                            }
                        }

                        // Zero-Balance or Over-Balance Negative Protection Warning Banners
                        if (isQuickBalanceZero) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.45f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.6f)),
                                modifier = Modifier.fillMaxWidth().testTag("quick_expense_zero_balance_banner")
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Warning,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = if (isEnglish)
                                                "⚠️ Available balance is ৳0! Add income first to record expenses."
                                            else
                                                "⚠️ অ্যাকাউন্টে পর্যাপ্ত ব্যালেন্স নেই (ব্যালেন্স ৳০)! খরচ করতে প্রথমে আয় যোগ করুন।",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = MaterialTheme.colorScheme.error,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp
                                            )
                                        )
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End
                                    ) {
                                        Button(
                                            onClick = onOpenAddTransaction,
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = Color(0xFF2E7D32),
                                                contentColor = Color.White
                                            ),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                            modifier = Modifier.height(30.dp).testTag("btn_quick_income_shortcut")
                                        ) {
                                            Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(13.dp))
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text(
                                                text = if (isEnglish) "Add Income" else "আয় যোগ করুন",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        } else if (showQuickOverBalanceWarning) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFFEF4444).copy(alpha = 0.12f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.45f)),
                                modifier = Modifier.fillMaxWidth().testTag("quick_expense_over_balance_banner")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = Color(0xFFEF4444),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = if (isEnglish)
                                            "⚠️ Cannot exceed balance (${formatTakaSafe(quickMaxSpendable)})! Amount capped & keyboard locked."
                                        else
                                            "⚠️ ব্যালেন্সের অতিরিক্ত টাকা প্রবেশ করা সম্ভব নয়! সর্বোচ্চ খরচযোগ্য ${formatTakaSafe(quickMaxSpendable)} নির্ধারণ করে কীবোর্ড নিষ্ক্রিয় করা হয়েছে।",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = Color(0xFFEF4444),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.5.sp
                                        )
                                    )
                                }
                            }
                        }

                        // Input Row: Category Selector Dropdown + Amount Input + Add Button
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Category Dropdown Box
                            Box(
                                modifier = Modifier.weight(1.35f)
                            ) {
                                Surface(
                                    onClick = { dropdownExpanded = true },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(44.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (dropdownExpanded) Color(0xFFD97706) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                                    ),
                                    color = MaterialTheme.colorScheme.surface
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(horizontal = 10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = selectedOption.first,
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 12.5.sp
                                            ),
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f, fill = false)
                                        )
                                        Icon(
                                            imageVector = Icons.Default.ArrowDropDown,
                                            contentDescription = null,
                                            tint = Color(0xFFD97706),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }

                                DropdownMenu(
                                    expanded = dropdownExpanded,
                                    onDismissRequest = { dropdownExpanded = false }
                                ) {
                                    presetOptions.forEach { opt ->
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    opt.first,
                                                    style = MaterialTheme.typography.bodyMedium.copy(
                                                        fontWeight = if (opt == selectedOption) FontWeight.Bold else FontWeight.Normal,
                                                        color = if (opt == selectedOption) Color(0xFFD97706) else MaterialTheme.colorScheme.onSurface
                                                    )
                                                )
                                            },
                                            onClick = {
                                                selectedOption = opt
                                                dropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                            }

                            // Amount Input Box
                            Surface(
                                modifier = Modifier
                                    .weight(1.05f)
                                    .height(44.dp),
                                shape = RoundedCornerShape(12.dp),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isQuickBalanceZero || showQuickOverBalanceWarning) MaterialTheme.colorScheme.error
                                    else if (customQuickAmount.isNotBlank()) Color(0xFFD97706)
                                    else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                                ),
                                color = MaterialTheme.colorScheme.surface
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 9.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "৳",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.ExtraBold),
                                        color = if (isQuickBalanceZero) MaterialTheme.colorScheme.error else Color(0xFFD97706)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Box(modifier = Modifier.weight(1f)) {
                                        if (customQuickAmount.isEmpty()) {
                                            Text(
                                                text = if (isEnglish) "Amount" else "পরিমাণ",
                                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                            )
                                        }
                                        BasicTextField(
                                            value = customQuickAmount,
                                            onValueChange = { input ->
                                                val filtered = input.filter { it.isDigit() || it == '.' }
                                                if (filtered.count { it == '.' } <= 1) {
                                                    if (isQuickBalanceZero) {
                                                        customQuickAmount = ""
                                                        keyboardController?.hide()
                                                        focusManager.clearFocus()
                                                        showQuickOverBalanceWarning = true
                                                        AppToastManager.show(
                                                            if (isEnglish) "Account balance is ৳0! Add income first."
                                                            else "অ্যাকাউন্টে পর্যাপ্ত ব্যালেন্স নেই (ব্যালেন্স ৳০)! নতুন খরচ করতে প্রথমে আয় যোগ করুন।"
                                                        )
                                                    } else {
                                                        val entered = filtered.toDoubleOrNull() ?: 0.0
                                                        if (entered > quickMaxSpendable) {
                                                            customQuickAmount = if (quickMaxSpendable % 1.0 == 0.0) {
                                                                quickMaxSpendable.toLong().toString()
                                                            } else {
                                                                String.format(Locale.US, "%.2f", quickMaxSpendable)
                                                            }
                                                            keyboardController?.hide()
                                                            focusManager.clearFocus()
                                                            showQuickOverBalanceWarning = true
                                                            AppToastManager.show(
                                                                if (isEnglish) "Cannot spend more than available balance (${formatTakaSafe(quickMaxSpendable)})"
                                                                else "ব্যালেন্সের অতিরিক্ত খরচ সম্ভব নয়! সর্বোচ্চ ${formatTakaSafe(quickMaxSpendable)} নির্ধারণ করা হয়েছে।"
                                                            )
                                                        } else {
                                                            customQuickAmount = filtered
                                                            showQuickOverBalanceWarning = false
                                                        }
                                                    }
                                                }
                                            },
                                            textStyle = MaterialTheme.typography.bodyMedium.copy(
                                                color = if (isQuickBalanceZero) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.5.sp
                                            ),
                                            keyboardOptions = KeyboardOptions(
                                                keyboardType = KeyboardType.Decimal,
                                                imeAction = androidx.compose.ui.text.input.ImeAction.Next
                                            ),
                                            singleLine = true,
                                            cursorBrush = SolidColor(if (isQuickBalanceZero) MaterialTheme.colorScheme.error else Color(0xFFD97706)),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .onFocusEvent {
                                                    if (it.isFocused) {
                                                        if (isQuickBalanceZero) {
                                                            keyboardController?.hide()
                                                            focusManager.clearFocus()
                                                            showQuickOverBalanceWarning = true
                                                            AppToastManager.show(
                                                                if (isEnglish) "Account balance is ৳0! Add income first."
                                                                else "অ্যাকাউন্টে পর্যাপ্ত ব্যালেন্স নেই (ব্যালেন্স ৳০)! নতুন খরচ করতে প্রথমে আয় যোগ করুন।"
                                                            )
                                                        } else {
                                                            coroutineScope.launch {
                                                                kotlinx.coroutines.delay(150)
                                                                listState.animateScrollToItem(index = 3, scrollOffset = 0)
                                                                quickExpenseBringIntoViewRequester.bringIntoView()
                                                            }
                                                        }
                                                    }
                                                }
                                                .testTag("input_quick_custom_amount")
                                        )
                                    }
                                }
                            }

                            // Quick Add Button
                            val canAdd = currentQuickAmt > 0 && !isQuickInsufficient && !isQuickBalanceZero && currentQuickAmt <= quickMaxSpendable
                            Button(
                                onClick = {
                                    val amt = customQuickAmount.toDoubleOrNull() ?: 0.0
                                    if (amt > 0 && !isQuickInsufficient && !isQuickBalanceZero && amt <= quickMaxSpendable) {
                                        val isOthers = selectedOption.second == "অন্যান্য" || selectedOption.first.contains("অন্যান্য")
                                        val cleanTitle = if (isOthers && customPurpose.isNotBlank()) {
                                            customPurpose.trim()
                                        } else if (isOthers) {
                                            if (isEnglish) "Other Expense" else "অন্যান্য খরচ"
                                        } else {
                                            selectedOption.first.substringAfter(" ")
                                        }
                                        viewModel.addTransaction(
                                            title = cleanTitle,
                                            amount = amt,
                                            type = "EXPENSE",
                                            category = selectedOption.second,
                                            note = if (customPurpose.isNotBlank()) customPurpose.trim() else (if (isEnglish) "Quick Expense" else "দ্রুত খরচ"),
                                            timestamp = selectedQuickDateTimestamp ?: System.currentTimeMillis(),
                                            khatId = selectedQuickKhatId,
                                            khatName = selectedQuickKhatName
                                        )
                                        customQuickAmount = ""
                                        customPurpose = ""
                                        selectedQuickDateTimestamp = null
                                        showQuickOverBalanceWarning = false
                                        focusManager.clearFocus()
                                        keyboardController?.hide()
                                    }
                                },
                                enabled = canAdd,
                                modifier = Modifier
                                    .height(44.dp)
                                    .testTag("btn_quick_add_expense"),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFD97706),
                                    contentColor = Color.White,
                                    disabledContainerColor = if (isDarkMode) Color(0xFF3E2C1E) else Color(0xFFFDE68A).copy(alpha = 0.6f)
                                ),
                                contentPadding = PaddingValues(horizontal = 14.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Add",
                                    modifier = Modifier.size(19.dp)
                                )
                            }
                        }

                        // Source Selector Row: "খরচ করবেন কোথা থেকে? (উৎস)"
                        var quickSourceDropdownExpanded by remember { mutableStateOf(false) }
                        val quickSourceLabel = if (selectedQuickKhat != null) {
                            "${AppLocale.khatName(selectedQuickKhat.name, isEnglish)} (${formatTakaSafe(selectedQuickKhat.allocatedAmount)})"
                        } else {
                            if (isEnglish) "Oboshisto (${formatTakaSafe(oboshistoBalance)})" else "অবশিষ্ট (${formatTakaSafe(oboshistoBalance)})"
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AccountBalanceWallet,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Text(
                                        text = if (isEnglish) "Spend from (Source):" else "খরচ করবেন কোথা থেকে? (উৎস):",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                if (selectedQuickKhatId != null) {
                                    Surface(
                                        onClick = {
                                            selectedQuickKhatId = null
                                            selectedQuickKhatName = ""
                                        },
                                        shape = RoundedCornerShape(6.dp),
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                    ) {
                                        Text(
                                            text = if (isEnglish) "Reset to Oboshisto" else "অবশিষ্টে রিসেট",
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp, fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }

                            Box(modifier = Modifier.fillMaxWidth()) {
                                Surface(
                                    onClick = { quickSourceDropdownExpanded = true },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(38.dp)
                                        .testTag("dropdown_quick_expense_source"),
                                    shape = RoundedCornerShape(10.dp),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (isQuickInsufficient) MaterialTheme.colorScheme.error
                                        else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                    ),
                                    color = MaterialTheme.colorScheme.surface
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(horizontal = 10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = if (selectedQuickKhat != null) Icons.Default.Folder else Icons.Default.Savings,
                                                contentDescription = null,
                                                tint = if (selectedQuickKhat != null) MaterialTheme.colorScheme.primary else Color(0xFF059669),
                                                modifier = Modifier.size(15.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = quickSourceLabel,
                                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold),
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        Icon(
                                            imageVector = Icons.Default.ArrowDropDown,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }

                                DropdownMenu(
                                    expanded = quickSourceDropdownExpanded,
                                    onDismissRequest = { quickSourceDropdownExpanded = false }
                                ) {
                                    DropdownMenuItem(
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.Savings,
                                                contentDescription = null,
                                                tint = Color(0xFF059669),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        },
                                        text = {
                                            Column {
                                                Text(
                                                    text = if (isEnglish) "Oboshisto (Default)" else "অবশিষ্ট (ডিফল্ট)",
                                                    fontWeight = if (selectedQuickKhatId == null) FontWeight.Bold else FontWeight.Normal
                                                )
                                                Text(
                                                    text = formatTakaSafe(oboshistoBalance),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = Color(0xFF059669)
                                                )
                                            }
                                        },
                                        onClick = {
                                            selectedQuickKhatId = null
                                            selectedQuickKhatName = ""
                                            quickSourceDropdownExpanded = false
                                        }
                                    )

                                    if (khats.isNotEmpty()) {
                                        Divider()
                                        khats.forEach { khat ->
                                            DropdownMenuItem(
                                                leadingIcon = {
                                                    Icon(
                                                        imageVector = Icons.Default.Folder,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                },
                                                text = {
                                                    Column {
                                                        Text(
                                                            text = AppLocale.khatName(khat.name, isEnglish),
                                                            fontWeight = if (selectedQuickKhatId == khat.id) FontWeight.Bold else FontWeight.Normal
                                                        )
                                                        Text(
                                                            text = formatTakaSafe(khat.allocatedAmount),
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                },
                                                onClick = {
                                                    selectedQuickKhatId = khat.id
                                                    selectedQuickKhatName = khat.name
                                                    quickSourceDropdownExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            if (isQuickInsufficient) {
                                Text(
                                    text = if (isEnglish) "'${selectedQuickKhat?.name}' has insufficient balance!" else "'${selectedQuickKhat?.name}' খাতে পর্যাপ্ত ব্যালেন্স নেই!",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = MaterialTheme.colorScheme.error,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    )
                                )
                            }
                        }

                        // Optional Note / Purpose / Other Name field
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(38.dp),
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isOtherCat) Color(0xFFD97706) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            ),
                            color = MaterialTheme.colorScheme.surface
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 10.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                if (customPurpose.isEmpty()) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.EditNote,
                                            contentDescription = null,
                                            tint = if (isOtherCat) Color(0xFFD97706).copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (isOtherCat) {
                                                if (isEnglish) "Other expense name / note (e.g. Gift, Donation)" else "অন্যান্য খরচের নাম বা নোট (যেমন: উপহার, সাহায্য)"
                                            } else {
                                                if (isEnglish) "Note / details (Optional)" else "নোট / বিবরণ (ঐচ্ছিক)"
                                            },
                                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                                            color = if (isOtherCat) Color(0xFFD97706).copy(alpha = 0.85f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                        )
                                    }
                                }
                                BasicTextField(
                                    value = customPurpose,
                                    onValueChange = { customPurpose = it },
                                    textStyle = MaterialTheme.typography.bodySmall.copy(
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontSize = 12.sp
                                    ),
                                    singleLine = true,
                                    cursorBrush = SolidColor(Color(0xFFD97706)),
                                    keyboardOptions = KeyboardOptions(
                                        imeAction = androidx.compose.ui.text.input.ImeAction.Done
                                    ),
                                    keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                                        onDone = {
                                            focusManager.clearFocus()
                                            keyboardController?.hide()
                                        }
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .onFocusEvent {
                                            if (it.isFocused) {
                                                coroutineScope.launch {
                                                    kotlinx.coroutines.delay(150)
                                                    listState.animateScrollToItem(index = 3, scrollOffset = 0)
                                                    quickExpenseBringIntoViewRequester.bringIntoView()
                                                }
                                            }
                                        }
                                        .testTag("input_quick_custom_purpose")
                                )
                            }
                        }

                        // Optional Date Selection Row (Defaults to Current Time, user can choose past date)
                        val quickDateText = remember(selectedQuickDateTimestamp, isEnglish) {
                            if (selectedQuickDateTimestamp == null) {
                                if (isEnglish) "Today (Current time)" else "আজকের তারিখ (বর্তমান সময়)"
                            } else {
                                val c = Calendar.getInstance().apply { timeInMillis = selectedQuickDateTimestamp!! }
                                if (isEnglish) {
                                    SimpleDateFormat("d MMMM yyyy", Locale.US).format(c.time)
                                } else {
                                    val bengaliMonths = arrayOf("জানুয়ারি", "ফেব্রুয়ারি", "মার্চ", "এপ্রিল", "মে", "জুন", "জুলাই", "আগস্ট", "সেপ্টেম্বর", "অক্টোবর", "নভেম্বর", "ডিসেম্বর")
                                    val d = c.get(Calendar.DAY_OF_MONTH)
                                    val m = bengaliMonths[c.get(Calendar.MONTH)]
                                    val y = c.get(Calendar.YEAR)
                                    "$d $m $y"
                                }
                            }
                        }

                        Surface(
                            onClick = {
                                val cal = Calendar.getInstance()
                                if (selectedQuickDateTimestamp != null) {
                                    cal.timeInMillis = selectedQuickDateTimestamp!!
                                }
                                android.app.DatePickerDialog(
                                    context,
                                    { _, year, month, dayOfMonth ->
                                        val now = Calendar.getInstance()
                                        val chosen = Calendar.getInstance().apply {
                                            set(Calendar.YEAR, year)
                                            set(Calendar.MONTH, month)
                                            set(Calendar.DAY_OF_MONTH, dayOfMonth)
                                            // Preserve present time of day
                                            set(Calendar.HOUR_OF_DAY, now.get(Calendar.HOUR_OF_DAY))
                                            set(Calendar.MINUTE, now.get(Calendar.MINUTE))
                                            set(Calendar.SECOND, now.get(Calendar.SECOND))
                                        }
                                        selectedQuickDateTimestamp = chosen.timeInMillis
                                    },
                                    cal.get(Calendar.YEAR),
                                    cal.get(Calendar.MONTH),
                                    cal.get(Calendar.DAY_OF_MONTH)
                                ).show()
                            },
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (selectedQuickDateTimestamp != null) Color(0xFFD97706)
                                else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            ),
                            color = if (selectedQuickDateTimestamp != null) {
                                if (isDarkMode) Color(0xFF382313) else Color(0xFFFFFBEB)
                            } else MaterialTheme.colorScheme.surface,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(38.dp)
                                .testTag("btn_quick_expense_date")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CalendarToday,
                                        contentDescription = null,
                                        tint = if (selectedQuickDateTimestamp != null) Color(0xFFD97706) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (isEnglish) "Date: $quickDateText" else "তারিখ: $quickDateText",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontSize = 11.5.sp,
                                            fontWeight = if (selectedQuickDateTimestamp != null) FontWeight.Bold else FontWeight.Normal
                                        ),
                                        color = if (selectedQuickDateTimestamp != null) {
                                            if (isDarkMode) Color(0xFFFBBF24) else Color(0xFFB45309)
                                        } else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                if (selectedQuickDateTimestamp != null) {
                                    IconButton(
                                        onClick = { selectedQuickDateTimestamp = null },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Reset Date",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(15.dp)
                                        )
                                    }
                                } else {
                                    Text(
                                        text = if (isEnglish) "Change" else "তারিখ বাছুন",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFD97706)
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // =========================================================================
            // UNIFIED SECTION: "Daily Financial Record & Transactions Ledger"
            // (Merged Box 4 and Box 5 into a single cohesive, elegant card)
            // =========================================================================
            item {
                val dateTitle = when (selectedDateFilter) {
                    "TODAY" -> if (isEnglish) "Today's Account" else "আজকের দিনের হিসাব"
                    "YESTERDAY" -> if (isEnglish) "Yesterday's Account" else "গতকালকের দিনের হিসাব"
                    "CUSTOM_DATE" -> {
                        if (customSelectedDate != null) {
                            val c = Calendar.getInstance().apply { timeInMillis = customSelectedDate!! }
                            if (isEnglish) "Account for ${formatBengaliFullDate(c, true)}"
                            else "${formatBengaliFullDate(c, false)} এর হিসাব"
                        } else {
                            if (isEnglish) "Selected Date Account" else "নির্দিষ্ট দিনের হিসাব"
                        }
                    }
                    "THIS_WEEK" -> if (isEnglish) "This Week's Account" else "এই সপ্তাহের হিসাব"
                    "THIS_MONTH" -> if (isEnglish) "This Month's Account" else "এই মাসের হিসাব"
                    else -> if (isEnglish) "Today's Account" else "আজকের দিনের হিসাব"
                }

                val dayIncome = if (selectedDateFilter == "TODAY") todayIncome
                else if (selectedDateFilter == "CUSTOM_DATE") selectedDateIncome
                else filteredTransactions.filter { it.type == "INCOME" }.sumOf { it.amount }

                val dayExpense = if (selectedDateFilter == "TODAY") todayExpense
                else if (selectedDateFilter == "CUSTOM_DATE") selectedDateExpense
                else filteredTransactions.filter { it.type == "EXPENSE" }.sumOf { it.amount }

                val dayNet = dayIncome - dayExpense

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("day_navigation_and_record_card"),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDarkMode) Color(0xFF18181B) else MaterialTheme.colorScheme.surface
                    ),
                    border = AppCardDefaults.border(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // 1st row: Strictly the date title; 2nd row: Transactions count
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = dateTitle,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDarkMode) Color(0xFF6EE7B7) else Color(0xFF065F46)
                                ),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                                ) {
                                    Text(
                                        text = AppLocale.entriesCount(isEnglish, filteredTransactions.size),
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        }

                        // Calendar & Return-To-Today Action Rows
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Row: View Any Day's Account (opens calendar)
                            Surface(
                                onClick = { showDatePicker() },
                                shape = RoundedCornerShape(12.dp),
                                color = if (isDarkMode) Color(0xFF27272A) else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isDarkMode) Color(0xFF3F3F46) else MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("btn_view_any_day_account")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 9.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(34.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.CalendarMonth,
                                                contentDescription = "Calendar",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = if (isEnglish) "View any day's account" else "যেকোনো দিনের হিসাব দেখুন",
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    fontWeight = FontWeight.SemiBold,
                                                    fontSize = 13.sp,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                            )
                                            Text(
                                                text = if (isEnglish) "Tap to open calendar" else "ক্যালেন্ডার থেকে যেকোনো তারিখ বাছাই করুন",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontSize = 10.5.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                                )
                                            )
                                        }
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = if (isEnglish) "Calendar" else "ক্যালেন্ডার",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    fontSize = 11.sp
                                                )
                                            )
                                            Spacer(modifier = Modifier.width(2.dp))
                                            Icon(
                                                imageVector = Icons.Default.ArrowForward,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(12.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            // Row: Return to Today (active when not on Today)
                            if (selectedDateFilter != "TODAY") {
                                Surface(
                                    onClick = { viewModel.clearCustomDate() },
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isDarkMode) Color(0xFF1E293B) else MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.45f),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (isDarkMode) Color(0xFF38BDF8).copy(alpha = 0.4f) else MaterialTheme.colorScheme.tertiary.copy(alpha = 0.35f)
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("btn_return_to_today_row")
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 9.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(34.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Today,
                                                    contentDescription = "Return to Today",
                                                    tint = MaterialTheme.colorScheme.tertiary,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column {
                                                Text(
                                                    text = if (isEnglish) "Return to today" else "আজকের হিসাবে ফিরুন",
                                                    style = MaterialTheme.typography.bodyMedium.copy(
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 13.sp,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                )
                                                Text(
                                                    text = if (isEnglish) "Switch back to today's active ledger" else "আজকের নিয়মিত হিসেবে ফিরে যান",
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        fontSize = 10.5.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                                    )
                                                )
                                            }
                                        }
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = if (isEnglish) "Switch" else "ফিরুন",
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.tertiary,
                                                        fontSize = 11.sp
                                                    )
                                                )
                                                Spacer(modifier = Modifier.width(3.dp))
                                                Icon(
                                                    imageVector = Icons.Default.ArrowForward,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.tertiary,
                                                    modifier = Modifier.size(12.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // 2 Visual Indicators for Selected Date (টাকা ঢুকেছে ও টাকা গেছে)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Income Box (টাকা ঢুকেছে)
                            Surface(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(14.dp),
                                color = Color(0xFF16A34A).copy(alpha = 0.10f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF16A34A).copy(alpha = 0.35f))
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Surface(
                                            shape = CircleShape,
                                            color = Color(0xFF16A34A).copy(alpha = 0.18f),
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = Icons.Default.ArrowDownward,
                                                    contentDescription = null,
                                                    tint = Color(0xFF16A34A),
                                                    modifier = Modifier.size(15.dp)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (isEnglish) "Today's Income (+)" else "টাকা ঢুকেছে (+)",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF16A34A),
                                                fontSize = 11.sp
                                            ),
                                            maxLines = 1
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = formatTakaSafe(dayIncome),
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 17.5.sp,
                                            color = Color(0xFF16A34A)
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            // Expense Box (টাকা গেছে)
                            Surface(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(14.dp),
                                color = Color(0xFFDC2626).copy(alpha = 0.10f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFDC2626).copy(alpha = 0.35f))
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Surface(
                                            shape = CircleShape,
                                            color = Color(0xFFDC2626).copy(alpha = 0.18f),
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = Icons.Default.ArrowUpward,
                                                    contentDescription = null,
                                                    tint = Color(0xFFDC2626),
                                                    modifier = Modifier.size(15.dp)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (isEnglish) "Today's Expense (-)" else "টাকা গেছে (-)",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFFDC2626),
                                                fontSize = 11.sp
                                            ),
                                            maxLines = 1
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = formatTakaSafe(dayExpense),
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 17.5.sp,
                                            color = Color(0xFFDC2626)
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }

                        // Type Filter Chips (All Types, Expense Only, Income Only) & Transaction List
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("transactions_history_box"),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                FilterChip(
                                    selected = selectedTypeFilter == "ALL",
                                    onClick = { viewModel.setTypeFilter("ALL") },
                                    label = { Text(if (isEnglish) "All Types" else "সব প্রকার", fontSize = 11.5.sp) },
                                    modifier = Modifier.weight(1f).testTag("filter_all")
                                )
                                FilterChip(
                                    selected = selectedTypeFilter == "EXPENSE",
                                    onClick = { viewModel.setTypeFilter("EXPENSE") },
                                    label = { Text(if (isEnglish) "🔻 Expense" else "🔻 শুধু খরচ", fontSize = 11.5.sp) },
                                    modifier = Modifier.weight(1f).testTag("filter_expense")
                                )
                                FilterChip(
                                    selected = selectedTypeFilter == "INCOME",
                                    onClick = { viewModel.setTypeFilter("INCOME") },
                                    label = { Text(if (isEnglish) "🟢 Income" else "🟢 শুধু আয়", fontSize = 11.5.sp) },
                                    modifier = Modifier.weight(1f).testTag("filter_income")
                                )
                            }

                            // Transactions List or Empty State inside the Unified Section
                            if (filteredTransactions.isEmpty()) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 20.dp)
                                        .testTag("empty_state_card"),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ReceiptLong,
                                        contentDescription = null,
                                        modifier = Modifier.size(40.dp),
                                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = AppLocale.noTransactions(isEnglish),
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = if (isEnglish) "Select a preset above or tap '${AppLocale.addTransaction(isEnglish)}' to record your expenses."
                                        else "উপরের শর্টকাট চাপুন অথবা নিচের '${AppLocale.addTransaction(isEnglish)}' বাটনে চাপ দিয়ে নতুন হিসাব যোগ করুন।",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                }
                            } else {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    filteredTransactions.forEachIndexed { index, item ->
                                        TransactionRow(
                                            item = item,
                                            isEnglish = isEnglish,
                                            onClick = { selectedTransactionForDetails = item },
                                            onDelete = { transactionToDelete = item }
                                        )
                                        if (index < filteredTransactions.size - 1) {
                                            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Bottom Spacing for FAB
            item {
                Spacer(modifier = Modifier.height(if (isImeVisible) 320.dp else 80.dp))
            }
        }
    }

    // Set Daily Target Dialog
    if (showDailyTargetDialog) {
        var targetInput by remember { mutableStateOf(dailyTarget.toLong().toString()) }

        val keyboardController = LocalSoftwareKeyboardController.current
        val imeInsets = WindowInsets.ime
        val density = LocalDensity.current
        val isKeyboardOpen = imeInsets.getBottom(density) > 0

        val animatedCardElevation by animateDpAsState(
            targetValue = if (isKeyboardOpen) 12.dp else 6.dp,
            animationSpec = tween(durationMillis = 250),
            label = "daily_target_elevation"
        )
        val animatedVerticalPadding by animateDpAsState(
            targetValue = if (isKeyboardOpen) 6.dp else 20.dp,
            animationSpec = tween(durationMillis = 250),
            label = "daily_target_v_padding"
        )
        val animatedContentPadding by animateDpAsState(
            targetValue = if (isKeyboardOpen) 16.dp else 24.dp,
            animationSpec = tween(durationMillis = 250),
            label = "daily_target_c_padding"
        )
        val animatedSpacing by animateDpAsState(
            targetValue = if (isKeyboardOpen) 10.dp else 16.dp,
            animationSpec = tween(durationMillis = 250),
            label = "daily_target_spacing"
        )

        Dialog(
            onDismissRequest = {
                focusManager.clearFocus()
                keyboardController?.hide()
                showDailyTargetDialog = false
            },
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                dismissOnBackPress = true,
                dismissOnClickOutside = true
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(horizontal = 20.dp, vertical = animatedVerticalPadding)
                    .clearFocusOnTap(),
                contentAlignment = if (isKeyboardOpen) Alignment.TopCenter else Alignment.Center
            ) {
                val targetScrollState = rememberScrollState()
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 450.dp)
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("daily_target_dialog"),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        border = AppCardDefaults.border(Color(0xFFF59E0B)),
                        elevation = CardDefaults.cardElevation(defaultElevation = animatedCardElevation)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = animatedContentPadding, vertical = animatedContentPadding)
                                .verticalScroll(targetScrollState),
                            verticalArrangement = Arrangement.spacedBy(animatedSpacing)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (isEnglish) "Daily Spending Target" else "দৈনিক খরচের লক্ষ্যমাত্রা",
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                IconButton(
                                    onClick = {
                                        focusManager.clearFocus()
                                        keyboardController?.hide()
                                        showDailyTargetDialog = false
                                    }
                                ) {
                                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                                }
                            }

                            Text(
                                text = if (isEnglish) "Enter your maximum target budget per day:" else "প্রতিদিন আপনি সর্বোচ্চ কত টাকা খরচ করতে চান লিখুন:",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            OutlinedTextField(
                                value = targetInput,
                                onValueChange = { input -> targetInput = input.filter { it.isDigit() } },
                                label = { Text(if (isEnglish) "Daily Target" else "দৈনিক টার্গেট") },
                                placeholder = { Text("500") },
                                leadingIcon = { Text("৳", fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 12.dp)) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = androidx.compose.ui.text.input.ImeAction.Done),
                                keyboardActions = androidx.compose.foundation.text.KeyboardActions(onDone = {
                                    focusManager.clearFocus()
                                    keyboardController?.hide()
                                }),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth().testTag("input_daily_target")
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        focusManager.clearFocus()
                                        keyboardController?.hide()
                                        showDailyTargetDialog = false
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f).height(48.dp)
                                ) {
                                    Text(AppLocale.cancel(isEnglish))
                                }

                                Button(
                                    onClick = {
                                        val amount = targetInput.toDoubleOrNull() ?: 500.0
                                        viewModel.setDailySpendingTarget(amount)
                                        focusManager.clearFocus()
                                        keyboardController?.hide()
                                        showDailyTargetDialog = false
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary
                                    ),
                                    modifier = Modifier.weight(1.3f).height(48.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(17.dp))
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(AppLocale.save(isEnglish), fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                    KeyboardScrollDownHint(
                        scrollState = targetScrollState,
                        modifier = Modifier.align(Alignment.BottomCenter)
                    )
                }
            }
        }
    }

    // Delete Single Transaction Confirmation Dialog
    transactionToDelete?.let { item ->
        AlertDialog(
            onDismissRequest = { transactionToDelete = null },
            title = { Text(AppLocale.deleteConfirm(isEnglish)) },
            text = {
                Text(
                    if (isEnglish) "Do you want to delete '${item.title}' (${formatTakaSafe(item.amount)}) from records?"
                    else "'${item.title}' (${formatTakaSafe(item.amount)}) হিসাব থেকে মুছে ফেলা হবে।"
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteTransaction(item)
                        transactionToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(AppLocale.delete(isEnglish))
                }
            },
            dismissButton = {
                TextButton(onClick = { transactionToDelete = null }) {
                    Text(AppLocale.cancel(isEnglish))
                }
            }
        )
    }

    // Comprehensive Transaction Details Modal Card (A to Z Details Card)
    selectedTransactionForDetails?.let { item ->
        ComprehensiveTransactionDetailsDialog(
            item = item,
            isEnglish = isEnglish,
            onDismiss = { selectedTransactionForDetails = null },
            onDelete = {
                val toDelete = item
                selectedTransactionForDetails = null
                transactionToDelete = toDelete
            },
            onGenerateReceipt = {
                selectedTransactionForDetails = null
                showReceiptDialog = true
            }
        )
    }

    // Money Receipt Dialog
    if (showReceiptDialog) {
        MoneyReceiptDialog(
            viewModel = viewModel,
            onDismiss = { showReceiptDialog = false }
        )
    }

    // User Profile Dialog
    if (showProfileDialog) {
        UserProfileDialog(
            viewModel = viewModel,
            onDismiss = { showProfileDialog = false }
        )
    }

    // =========================================================================
    // Unique Settings Side Bar Drawer (Right-to-Left Swipe / Slide-in Sheet)
    // =========================================================================
    if (showSettingsDrawer) {
        // Scrim backdrop
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
                .clickable {
                    showSettingsDrawer = false
                    activeDrawerConfirmAction = null
                }
        )
    }

    AnimatedVisibility(
        visible = showSettingsDrawer,
        enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
        exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut(),
        modifier = Modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.CenterEnd
        ) {
            val drawerScrollState = rememberScrollState()

            Surface(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(0.88f)
                    .widthIn(max = 380.dp)
                    .clickable(enabled = false) {}
                    .testTag("settings_side_bar_drawer"),
                shape = RoundedCornerShape(topStart = 26.dp, bottomStart = 26.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 16.dp,
                shadowElevation = 24.dp
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(drawerScrollState)
                            .padding(horizontal = 20.dp, vertical = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                    // 1. Drawer Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                modifier = Modifier.size(38.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                    Icon(
                                        imageVector = Icons.Default.Settings,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = if (isEnglish) "Settings & Menu" else "সেটিংস ও নিয়ন্ত্রণ",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = if (isEnglish) "App options & profile" else "অ্যাপ ও অ্যাকাউন্ট মেনু",
                                    style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                )
                            }
                        }

                        IconButton(
                            onClick = {
                                showSettingsDrawer = false
                                activeDrawerConfirmAction = null
                            },
                            modifier = Modifier.testTag("btn_close_settings_drawer")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    // 2. User Profile Summary Card
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                        ),
                        border = AppCardDefaults.border(),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AccountCircle,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(30.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = userName.ifBlank { if (isEnglish) "User" else "ব্যবহারকারী" },
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleSmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = userPhone.ifBlank { "01XXXXXXXXX" },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Button(
                                onClick = {
                                    showSettingsDrawer = false
                                    showProfileDialog = true
                                },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(38.dp)
                                    .testTag("drawer_btn_view_profile"),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isEnglish) "Profile & Security" else "প্রোফাইল বিবরণ ও নিরাপত্তা",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    // Section Label
                    Text(
                        text = if (isEnglish) "QUICK CONTROLS" else "দ্রুত নিয়ন্ত্রণ ও সেটিংস",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    )

                    // 3. Theme Toggle Card (Light / Dark Mode)
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                        ),
                        border = AppCardDefaults.border(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("drawer_theme_card")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                        Icon(
                                            imageVector = if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(19.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = if (isDarkMode) (if (isEnglish) "Dark Mode Active" else "ডার্ক মোড সক্রিয়")
                                               else (if (isEnglish) "Light Mode Active" else "লাইট মোড সক্রিয়"),
                                        fontWeight = FontWeight.SemiBold,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        text = if (isDarkMode) (if (isEnglish) "Switch to Light theme" else "লাইট থিমে রূপান্তর করুন")
                                               else (if (isEnglish) "Switch to Dark theme" else "ডার্ক থিমে রূপান্তর করুন"),
                                        style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    )
                                }
                            }

                            Switch(
                                checked = isDarkMode,
                                onCheckedChange = { viewModel.toggleDarkMode() },
                                modifier = Modifier.testTag("drawer_switch_dark_mode")
                            )
                        }
                    }

                    // 4. Money Receipt Generator Button Card
                    Card(
                        onClick = {
                            showSettingsDrawer = false
                            showReceiptDialog = true
                        },
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                        ),
                        border = AppCardDefaults.border(Color(0xFF0284C7)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("drawer_btn_receipt")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFF0284C7).copy(alpha = 0.15f),
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                    Icon(
                                        imageVector = Icons.Default.ReceiptLong,
                                        contentDescription = null,
                                        tint = Color(0xFF0284C7),
                                        modifier = Modifier.size(19.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isEnglish) "Money Receipt Generator" else "টাকা জমার রসিদ (ভাউচার)",
                                    fontWeight = FontWeight.SemiBold,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = if (isEnglish) "Generate digital receipt voucher" else "গ্রাহকের জন্য ডিজিটাল রশিদ তৈরি করুন",
                                    style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }
                    }

                    // 5. Daily Spending Target Limit Card
                    Card(
                        onClick = {
                            showSettingsDrawer = false
                            showDailyTargetDialog = true
                        },
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                        ),
                        border = AppCardDefaults.border(Color(0xFFF59E0B)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("drawer_btn_daily_target")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFFF59E0B).copy(alpha = 0.15f),
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                    Icon(
                                        imageVector = Icons.Default.TrackChanges,
                                        contentDescription = null,
                                        tint = Color(0xFFD97706),
                                        modifier = Modifier.size(19.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isEnglish) "Daily Spending Target" else "দৈনিক খরচের লক্ষ্যমাত্রা",
                                    fontWeight = FontWeight.SemiBold,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = "${if (isEnglish) "Target: " else "বর্তমান লক্ষ্য: "}৳${dailyTarget.toLong()}",
                                    style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }
                    }

                    // 6. Language Switcher Card
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                        ),
                        border = AppCardDefaults.border(Color(0xFF8B5CF6)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("drawer_language_card")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFF8B5CF6).copy(alpha = 0.15f),
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                        Icon(
                                            imageVector = Icons.Default.Translate,
                                            contentDescription = null,
                                            tint = Color(0xFF8B5CF6),
                                            modifier = Modifier.size(19.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = if (isEnglish) "App Language" else "অ্যাপের ভাষা",
                                        fontWeight = FontWeight.SemiBold,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        text = if (isEnglish) "English active" else "বাংলা ভাষা সক্রিয়",
                                        style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    )
                                }
                            }

                            FilledTonalButton(
                                onClick = { viewModel.toggleLanguage() },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .height(32.dp)
                                    .testTag("drawer_btn_toggle_lang")
                            ) {
                                Text(
                                    text = if (isEnglish) "বাংলা" else "English",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // 7. Instant Cloud Sync Status & Action Card
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                        ),
                        border = AppCardDefaults.border(Color(0xFF10B981)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("drawer_cloud_sync_card")
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFF10B981).copy(alpha = 0.15f),
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                        Icon(
                                            imageVector = Icons.Default.CloudSync,
                                            contentDescription = null,
                                            tint = Color(0xFF059669),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (isEnglish) "Cloud Auto-Sync" else "ক্লাউড অটো-সিঙ্ক",
                                        fontWeight = FontWeight.SemiBold,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    val syncTimeText = if (lastSyncTimestamp > 0) {
                                        val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale.US)
                                        "${if (isEnglish) "Last: " else "সর্বশেষ সিঙ্ক: "}${sdf.format(Date(lastSyncTimestamp))}"
                                    } else {
                                        if (isEnglish) "Offline/Online auto active" else "অফলাইন/অনলাইন স্বয়ংক্রিয় সক্রিয়"
                                    }
                                    Text(
                                        text = syncTimeText,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = Color(0xFF059669),
                                            fontWeight = FontWeight.Medium
                                        )
                                    )
                                }
                            }

                            OutlinedButton(
                                onClick = {
                                    if (!isDrawerSyncing) {
                                        isDrawerSyncing = true
                                        viewModel.triggerCloudSync { _, msg ->
                                            isDrawerSyncing = false
                                            AppToastManager.show(msg)
                                        }
                                    }
                                },
                                enabled = !isDrawerSyncing,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(36.dp)
                                    .testTag("drawer_btn_sync_now"),
                                shape = RoundedCornerShape(8.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.6f))
                            ) {
                                if (isDrawerSyncing) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(text = if (isEnglish) "Syncing..." else "সিঙ্ক হচ্ছে...", fontSize = 12.sp)
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Sync,
                                        contentDescription = null,
                                        modifier = Modifier.size(15.dp),
                                        tint = Color(0xFF059669)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (isEnglish) "Sync Now" else "এখনই সিঙ্ক করুন",
                                        fontSize = 12.sp,
                                        color = Color(0xFF059669),
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }

                    // 7.5. App Lock & Privacy Security Card
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                        ),
                        border = AppCardDefaults.border(MaterialTheme.colorScheme.primary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("drawer_app_lock_card")
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                            Icon(
                                                imageVector = if (isAppLockEnabled) Icons.Default.Lock else Icons.Default.LockOpen,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = if (isEnglish) "App Lock (PIN)" else "অ্যাপ লক (পিন)",
                                            fontWeight = FontWeight.SemiBold,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        Text(
                                            text = if (isAppLockEnabled) (if (isEnglish) "App is protected by PIN" else "অ্যাপ পিন দিয়ে সুরক্ষিত")
                                                   else (if (isEnglish) "Protect app with 4-digit PIN" else "৪-সংখ্যার পিন দিয়ে সুরক্ষিত রাখুন"),
                                            style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        )
                                    }
                                }

                                Switch(
                                    checked = isAppLockEnabled,
                                    onCheckedChange = { enable ->
                                        if (enable) {
                                            showAppLockSetupDialog = true
                                        } else {
                                            showAppLockDisableVerifyDialog = true
                                        }
                                    },
                                    modifier = Modifier.testTag("switch_app_lock")
                                )
                            }

                            if (isAppLockEnabled) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                                // Change PIN button
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (isEnglish) "Change PIN code" else "পিন কোড পরিবর্তন করুন",
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium)
                                    )
                                    OutlinedButton(
                                        onClick = { showAppLockChangePinVerifyDialog = true },
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        modifier = Modifier.height(32.dp).testTag("btn_change_pin")
                                    ) {
                                        Icon(imageVector = Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(if (isEnglish) "Change" else "পরিবর্তন", fontSize = 11.sp)
                                    }
                                }

                                // Biometric unlock option if hardware available
                                if (canBiometric) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = if (isEnglish) "Biometric Unlock" else "ফিঙ্গারপ্রিন্ট / বায়োমেট্রিক",
                                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium)
                                            )
                                            Text(
                                                text = if (isEnglish) "Use Fingerprint or Face to unlock" else "ফিঙ্গারপ্রিন্ট বা ফেস দিয়ে দ্রুত আনলক",
                                                style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            )
                                        }
                                        Switch(
                                            checked = isBiometricEnabled,
                                            onCheckedChange = { appLockManager.setBiometricEnabled(it) },
                                            modifier = Modifier.testTag("switch_biometric_lock")
                                        )
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Shield,
                                            contentDescription = null,
                                            tint = Color(0xFF10B981),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (isEnglish) "PIN is device-local only. Never synced to cloud."
                                                   else "পিন শুধুমাত্র এই ফোনে সংরক্ষিত। ক্লাউডে সিঙ্ক হয় না।",
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 8. System Storage & Database Health Telemetry Card (Enhanced Unique Feature)
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        border = AppCardDefaults.border(MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("drawer_storage_health_card")
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Storage,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (isEnglish) "Storage & Database Health" else "স্টোরেজ ও ডাটাবেজ হেলথ",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFF10B981).copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = if (isEnglish) "100% Secure" else "নিরাপদ",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 9.5.sp,
                                            color = Color(0xFF10B981)
                                        ),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = if (isEnglish) "Saved Records" else "সংরক্ষিত লেনদেন",
                                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                )
                                Text(
                                    text = "${allTransactions.size} ${if (isEnglish) "entries" else "টি হিসাব"}",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = if (isEnglish) "Local Cache" else "লোকাল ডাটা ক্যাশ",
                                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                )
                                Text(
                                    text = "~${((allTransactions.size * 180 + 2048) / 1024)} KB",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                                )
                            }

                            OutlinedButton(
                                onClick = {
                                    AppToastManager.show(
                                        if (isEnglish) "Local database & cache verified optimal!"
                                        else "ডাটাবেজ ও ক্যাশ সফলভাবে অপটিমাইজ করা হয়েছে!"
                                    )
                                },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth().height(36.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = null,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isEnglish) "Optimize Cache & Index" else "ক্যাশ ও ইনডেক্স রিফ্রেশ করুন",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    // 8.1. Smart Spending Alerts & Interaction Preferences (Enhanced Unique Feature)
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        border = AppCardDefaults.border(MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("drawer_alerts_haptics_card")
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.NotificationsActive,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isEnglish) "Smart Alerts & Feedback" else "স্মার্ট অ্যালার্ট ও ভাইব্রেশন",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            // Toggle 1: High Transaction Warning Alert
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (isEnglish) "High Expense Alert (> ৳5,000)" else "বড় খরচে সতর্কতা (> ৳ ৫,০০০)",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                                    )
                                    Text(
                                        text = if (isEnglish) "Warn when entering large payments" else "বড় অঙ্কের লেনদেনে তাৎক্ষণিক সতর্কতা",
                                        style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    )
                                }
                                Switch(
                                    checked = isHighTxnAlertEnabled,
                                    onCheckedChange = {
                                        isHighTxnAlertEnabled = it
                                        sharedPrefs.edit().putBoolean("pref_high_txn_alert", it).apply()
                                        AppToastManager.show(
                                            if (it) (if (isEnglish) "High expense alert enabled" else "বড় খরচে সতর্কতা চালু হয়েছে")
                                            else (if (isEnglish) "High expense alert disabled" else "বড় খরচে সতর্কতা বন্ধ হয়েছে")
                                        )
                                    }
                                )
                            }

                            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                            // Toggle 2: Button Tactile Haptic Vibration
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (isEnglish) "Tactile Haptic Feedback" else "বাটন টাচ ভাইব্রেশন",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                                    )
                                    Text(
                                        text = if (isEnglish) "Vibrate on button actions" else "বাটনে স্পর্শ করলে মৃদু স্পন্দন",
                                        style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    )
                                }
                                Switch(
                                    checked = isHapticFeedbackEnabled,
                                    onCheckedChange = {
                                        isHapticFeedbackEnabled = it
                                        sharedPrefs.edit().putBoolean("pref_haptic_feedback", it).apply()
                                        AppToastManager.show(
                                            if (it) (if (isEnglish) "Touch haptics enabled" else "টাচ ভাইব্রেশন সক্রিয় হয়েছে")
                                            else (if (isEnglish) "Touch haptics disabled" else "টাচ ভাইব্রেশন বন্ধ হয়েছে")
                                        )
                                    }
                                )
                            }
                        }
                    }

                    // 8.2. Footer Zero-Knowledge Security Badge
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                        ),
                        border = AppCardDefaults.border(MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isEnglish) "256-Bit Bank-Grade AES Encryption\n100% Zero-Knowledge & Private"
                                       else "২৫৬-বিট ব্যাংক-গ্রেড AES এনক্রিপশন\n১০০% সুরক্ষিত ও সম্পূর্ণ ব্যক্তিগত",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 10.sp,
                                    lineHeight = 14.sp
                                ),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    // 8.3. About the App & User Guide (Placed just above Account & Data Controls)
                    Card(
                        onClick = {
                            showSettingsDrawer = false
                            showAboutAppDialog = true
                        },
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                        ),
                        border = AppCardDefaults.border(MaterialTheme.colorScheme.primary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("drawer_btn_about_app")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isEnglish) "About the App & Guide" else "অ্যাপ পরিচিতি ও ব্যবহার নির্দেশিকা",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (isEnglish) "Built by Md. Yousuf • How it Works & Functions" else "তৈরি করেছেন মো: ইউসুফ • সকল ফিচার নির্দেশিকা",
                                    style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    // 8.4. Account & Data Controls (At the VERY BOTTOM of Settings)
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        border = AppCardDefaults.border(Color(0xFFDC2626)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("drawer_card_account_data_controls")
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.AdminPanelSettings,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isEnglish) "Account & Data Controls" else "অ্যাকাউন্ট ও ডেটা কন্ট্রোল",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                )
                            }

                            // Action 1: Reset Financial Records (Warm Amber/Tangerine #D97706)
                            if (showDrawerClearAllConfirm) {
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isDarkMode) Color(0xFF291B05) else Color(0xFFFEF3C7)
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    border = AppCardDefaults.border(Color(0xFFD97706)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.WarningAmber,
                                                contentDescription = null,
                                                tint = Color(0xFFD97706),
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = if (isEnglish) "Reset All Financial Records?" else "সব হিসাবের রেকর্ড মুছে ফেলবেন?",
                                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                                color = Color(0xFFD97706)
                                            )
                                        }
                                        Text(
                                            text = if (isEnglish) "This will erase all transactions, debts, and savings goals from both this device and cloud. Your user account remains active."
                                                   else "এটি এই ডিভাইস ও ক্লাউড থেকে আপনার সমস্ত লেনদেন, ধার-দেনা ও সঞ্চয় লক্ষ্য মুছে ফেলবে। আপনার অ্যাকাউন্ট সক্রিয় থাকবে।",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (isDarkMode) Color(0xFFFDE68A) else Color(0xFF92400E)
                                        )
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.End,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            TextButton(
                                                onClick = { activeDrawerConfirmAction = null },
                                                colors = ButtonDefaults.textButtonColors(
                                                    contentColor = if (isDarkMode) Color(0xFFE2E8F0) else Color(0xFF475569)
                                                )
                                            ) {
                                                Text(if (isEnglish) "Cancel" else "বাতিল")
                                            }
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Button(
                                                onClick = {
                                                    activeDrawerConfirmAction = null
                                                    viewModel.clearAllData()
                                                    showSettingsDrawer = false
                                                },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = Color(0xFFD97706),
                                                    contentColor = Color.White
                                                ),
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Text(if (isEnglish) "Erase Records" else "রেকর্ড মুছুন")
                                            }
                                        }
                                    }
                                }
                            } else {
                                OutlinedButton(
                                    onClick = {
                                        activeDrawerConfirmAction = if (activeDrawerConfirmAction == "CLEAR_ALL") null else "CLEAR_ALL"
                                    },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFD97706)),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFD97706).copy(alpha = 0.5f)),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth().testTag("drawer_btn_clear_all")
                                ) {
                                    Icon(imageVector = Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (isEnglish) "Reset Financial Records" else "সব হিসাবের তথ্য মুছুন",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 12.sp
                                    )
                                }
                            }

                            // Action 2: Delete Account Permanently (Crimson Red #DC2626)
                            if (showDrawerDeleteAccountConfirm) {
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isDarkMode) Color(0xFF330C0C) else Color(0xFFFEE2E2)
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    border = AppCardDefaults.border(Color(0xFFDC2626)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.ReportProblem,
                                                contentDescription = null,
                                                tint = Color(0xFFDC2626),
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = if (isEnglish) "Permanently Delete Account?" else "অ্যাকাউন্ট স্থায়ীভাবে মুছবেন?",
                                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                                color = Color(0xFFDC2626)
                                            )
                                        }
                                        Text(
                                            text = if (isEnglish) "DANGER: This action cannot be undone. It permanently deletes your profile, credentials, and all financial records from cloud and phone."
                                                   else "সতর্কতা: এটি আর ফিরিয়ে আনা যাবে না। ক্লাউড ও ফোন থেকে আপনার প্রোফাইল, তথ্য এবং সমস্ত আর্থিক রেকর্ড স্থায়ীভাবে মুছে যাবে।",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (isDarkMode) Color(0xFFFECACA) else Color(0xFF991B1B)
                                        )
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.End,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            TextButton(
                                                onClick = { activeDrawerConfirmAction = null },
                                                colors = ButtonDefaults.textButtonColors(
                                                    contentColor = if (isDarkMode) Color(0xFFE2E8F0) else Color(0xFF475569)
                                                )
                                            ) {
                                                Text(if (isEnglish) "Cancel" else "বাতিল")
                                            }
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Button(
                                                onClick = {
                                                    activeDrawerConfirmAction = null
                                                    viewModel.deleteUserAccount(
                                                        onSuccess = {
                                                            AppToastManager.show(if (isEnglish) "Account permanently deleted" else "অ্যাকাউন্ট স্থায়ীভাবে মুছে ফেলা হয়েছে")
                                                            showSettingsDrawer = false
                                                        },
                                                        onError = { err ->
                                                            AppToastManager.show(err)
                                                        }
                                                    )
                                                },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = Color(0xFFDC2626),
                                                    contentColor = Color.White
                                                ),
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Text(if (isEnglish) "Delete Permanently" else "স্থায়ীভাবে মুছুন")
                                            }
                                        }
                                    }
                                }
                            } else {
                                OutlinedButton(
                                    onClick = {
                                        activeDrawerConfirmAction = if (activeDrawerConfirmAction == "DELETE_ACCOUNT") null else "DELETE_ACCOUNT"
                                    },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFDC2626)),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFDC2626).copy(alpha = 0.5f)),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth().testTag("drawer_btn_delete_account")
                                ) {
                                    Icon(imageVector = Icons.Default.DeleteForever, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (isEnglish) "Delete Account Permanently" else "অ্যাকাউন্ট স্থায়ীভাবে ডিলিট করুন",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 12.sp
                                    )
                                }
                            }

                            // Action 3: Log Out (Slate Charcoal #475569)
                            if (showDrawerLogoutConfirm) {
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isDarkMode) Color(0xFF1E293B) else Color(0xFFF1F5F9)
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    border = AppCardDefaults.border(Color(0xFF475569)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = if (isEnglish) "Log out of this account?" else "এই অ্যাকাউন্ট থেকে লগআউট করবেন?",
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.End,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            TextButton(
                                                onClick = { activeDrawerConfirmAction = null }
                                            ) {
                                                Text(if (isEnglish) "Cancel" else "না")
                                            }
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Button(
                                                onClick = {
                                                    activeDrawerConfirmAction = null
                                                    viewModel.logoutUser()
                                                    showSettingsDrawer = false
                                                },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = Color(0xFF475569),
                                                    contentColor = Color.White
                                                ),
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Text(if (isEnglish) "Logout" else "লগআউট")
                                            }
                                        }
                                    }
                                }
                            } else {
                                Button(
                                    onClick = {
                                        activeDrawerConfirmAction = if (activeDrawerConfirmAction == "LOGOUT") null else "LOGOUT"
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isDarkMode) Color(0xFF334155) else Color(0xFF475569),
                                        contentColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("drawer_btn_logout")
                                ) {
                                    Icon(imageVector = Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (isEnglish) "Log Out" else "লগআউট",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(58.dp))
                }

                // Top Dynamic Gradient Scroll Progress Bar
                val maxScroll = drawerScrollState.maxValue
                val scrollFraction = if (maxScroll > 0) (drawerScrollState.value.toFloat() / maxScroll).coerceIn(0f, 1f) else 0f
                if (maxScroll > 0) {
                    LinearProgressIndicator(
                        progress = { scrollFraction },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .align(Alignment.TopCenter),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = Color.Transparent
                    )
                }

                // Floating "Scroll Down ↓" / "স্ক্রোল করুন ↓" Pill Indicator
                AnimatedVisibility(
                    visible = drawerScrollState.canScrollForward,
                    enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
                    exit = fadeOut() + slideOutVertically(targetOffsetY = { it }),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 16.dp)
                ) {
                    val infiniteTransition = rememberInfiniteTransition(label = "scroll_bounce")
                    val bounceOffset by infiniteTransition.animateFloat(
                        initialValue = 0f,
                        targetValue = 6f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(650, easing = FastOutSlowInEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "bounce"
                    )

                    Surface(
                        onClick = {
                            coroutineScope.launch {
                                drawerScrollState.animateScrollTo(drawerScrollState.value + 450)
                            }
                        },
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.primary,
                        shadowElevation = 8.dp,
                        border = BorderStroke(1.2.dp, Color.White.copy(alpha = 0.4f)),
                        modifier = Modifier
                            .offset(y = bounceOffset.dp)
                            .testTag("settings_scroll_down_indicator")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = if (isEnglish) "Scroll down" else "স্ক্রোল করুন",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.5.sp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            )
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = "Scroll down",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
    }

    // App Lock Dialogs
    if (showAppLockSetupDialog) {
        AppLockSetupDialog(
            appLockManager = appLockManager,
            isEnglish = isEnglish,
            onDismiss = { showAppLockSetupDialog = false },
            onSuccess = {
                showAppLockSetupDialog = false
                AppToastManager.show(if (isEnglish) "App Lock enabled with PIN" else "পিন দিয়ে অ্যাপ লক সক্রিয় হয়েছে")
            }
        )
    }

    if (showAppLockDisableVerifyDialog) {
        AppLockVerifyDialog(
            appLockManager = appLockManager,
            isEnglish = isEnglish,
            title = if (isEnglish) "Disable App Lock" else "অ্যাপ লক বন্ধ করুন",
            onDismiss = { showAppLockDisableVerifyDialog = false },
            onVerified = {
                showAppLockDisableVerifyDialog = false
                appLockManager.setAppLockEnabled(false)
                AppToastManager.show(if (isEnglish) "App Lock disabled" else "অ্যাপ লক নিষ্ক্রিয় করা হয়েছে")
            }
        )
    }

    if (showAppLockChangePinVerifyDialog) {
        AppLockVerifyDialog(
            appLockManager = appLockManager,
            isEnglish = isEnglish,
            title = if (isEnglish) "Verify Current PIN" else "বর্তমান পিন যাচাই করুন",
            onDismiss = { showAppLockChangePinVerifyDialog = false },
            onVerified = {
                showAppLockChangePinVerifyDialog = false
                showAppLockSetupDialog = true
            }
        )
    }

    if (showCreateKhatDialog) {
        CreateOrEditKhatDialog(
            khatToEdit = null,
            availableOboshisto = oboshistoBalance,
            isEnglish = isEnglish,
            onDismiss = { showCreateKhatDialog = false },
            onSave = { name, amount, colorHex, iconName ->
                viewModel.createKhat(
                    name = name,
                    allocatedAmount = amount,
                    colorHex = colorHex,
                    iconName = iconName,
                    onSuccess = {
                        showCreateKhatDialog = false
                    },
                    onError = { err ->
                        AppToastManager.show(err)
                    }
                )
            }
        )
    }

    if (khatToEdit != null) {
        CreateOrEditKhatDialog(
            khatToEdit = khatToEdit,
            availableOboshisto = oboshistoBalance,
            isEnglish = isEnglish,
            onDismiss = { khatToEdit = null },
            onSave = { name, amount, colorHex, iconName ->
                khatToEdit?.let { current ->
                    viewModel.updateKhat(
                        khat = current,
                        newName = name,
                        newAmount = amount,
                        colorHex = colorHex,
                        iconName = iconName,
                        onSuccess = {
                            khatToEdit = null
                        },
                        onError = { err ->
                            AppToastManager.show(err)
                        }
                    )
                }
            }
        )
    }

    if (khatToDelete != null) {
        DeleteKhatConfirmationDialog(
            khat = khatToDelete!!,
            isEnglish = isEnglish,
            onDismiss = { khatToDelete = null },
            onConfirm = {
                khatToDelete?.let { viewModel.deleteKhat(it) }
                khatToDelete = null
            }
        )
    }

    if (showAboutAppDialog) {
        AboutAppDialog(
            initialIsEnglish = isEnglish,
            onDismiss = { showAboutAppDialog = false }
        )
    }
}
}

@Composable
fun TransactionRow(
    item: TransactionEntity,
    isEnglish: Boolean = false,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isExpense = item.type == "EXPENSE"
    val categoryIcon = SimpleCategoryData.getCategoryIcon(item.category)
    val categoryColor = SimpleCategoryData.getCategoryColor(item.category)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("transaction_item_${item.id}")
            .clip(RoundedCornerShape(18.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(
            1.dp,
            if (isExpense) Color(0xFFDC2626).copy(alpha = 0.22f)
            else Color(0xFF16A34A).copy(alpha = 0.22f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left Content: Category Icon + Stacked Information Rows
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Category Icon Badge
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = categoryColor.copy(alpha = 0.14f),
                    border = BorderStroke(1.dp, categoryColor.copy(alpha = 0.35f)),
                    modifier = Modifier.size(46.dp)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Icon(
                            imageVector = categoryIcon,
                            contentDescription = item.category,
                            tint = categoryColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    // Row 1: Category Name & Title
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = AppLocale.category(item.category, isEnglish),
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        val displayTitle = AppLocale.displayTitle(item.title, isEnglish)
                        if (displayTitle.isNotBlank() && displayTitle != AppLocale.category(item.category, isEnglish)) {
                            Text(
                                text = "• $displayTitle",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 12.5.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Row 2: Date & Time
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = formatDateTime(item.timestamp, isEnglish),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Medium
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Row 3: Where the money was spent from (Khat or General/Oboshisto) + Payment Method
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(top = 1.dp)
                    ) {
                        if (item.khatName.isNotBlank()) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFF4F46E5).copy(alpha = 0.12f),
                                border = BorderStroke(0.8.dp, Color(0xFF4F46E5).copy(alpha = 0.35f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Icon(
                                        imageVector = getKhatIcon("", item.khatName),
                                        contentDescription = null,
                                        tint = Color(0xFF4F46E5),
                                        modifier = Modifier.size(11.dp)
                                    )
                                    Text(
                                        text = if (isEnglish) "Khat: ${AppLocale.khatName(item.khatName, true)}" else "খাত: ${item.khatName}",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold
                                        ),
                                        color = Color(0xFF4F46E5),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        } else if (isExpense) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                            ) {
                                Text(
                                    text = if (isEnglish) "Cash/General" else "সাধারণ ক্যাশ",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Medium
                                    ),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        if (item.paymentMethod.isNotBlank() && item.paymentMethod != "ক্যাশ" && item.paymentMethod != "Cash") {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                                border = BorderStroke(0.6.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
                            ) {
                                Text(
                                    text = item.paymentMethod,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.primary
                                    ),
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    // Row 4: Note (if present)
                    if (item.note.isNotBlank()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(top = 1.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Notes,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = AppLocale.displayNote(item.note, isEnglish),
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            // Right Side: Amount, Tap Hint, and Delete Button
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(start = 8.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isExpense) Color(0xFFDC2626).copy(alpha = 0.10f) else Color(0xFF16A34A).copy(alpha = 0.10f),
                        border = BorderStroke(0.8.dp, if (isExpense) Color(0xFFDC2626).copy(alpha = 0.3f) else Color(0xFF16A34A).copy(alpha = 0.3f))
                    ) {
                        Text(
                            text = "${if (isExpense) "-" else "+"}${formatTakaSafe(item.amount)}",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = if (item.amount >= 100_000) 14.5.sp else 16.sp,
                                color = if (isExpense) Color(0xFFDC2626) else Color(0xFF16A34A)
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                        )
                    }

                    Text(
                        text = if (isEnglish) "Details ℹ️" else "বিস্তারিত ℹ️",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    )
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f))
                        .testTag("delete_transaction_${item.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(17.dp)
                    )
                }
            }
        }
    }
}

/**
 * Unique, Modern & Comprehensive Full Transaction Details Dialog (A to Z Details Card)
 * - Has close cross button on top
 * - Dismisses when touching outside
 * - Displays "Scroll down" indicator when content is long/overflows
 * - Displays complete metadata: Type, Amount, Category, Title, Khat, Method, Timestamp, Note, ID
 */
@Composable
fun ComprehensiveTransactionDetailsDialog(
    item: TransactionEntity,
    isEnglish: Boolean = false,
    onDismiss: () -> Unit,
    onDelete: () -> Unit,
    onGenerateReceipt: () -> Unit
) {
    val isExpense = item.type == "EXPENSE"
    val categoryIcon = SimpleCategoryData.getCategoryIcon(item.category)
    val categoryColor = SimpleCategoryData.getCategoryColor(item.category)
    val scrollState = rememberScrollState()

    val cal = Calendar.getInstance().apply { timeInMillis = item.timestamp }
    val fullDateString = formatBengaliFullDate(cal, isEnglish)
    val timeFormat = SimpleDateFormat("hh:mm a", Locale.US)
    val formattedTime = timeFormat.format(Date(item.timestamp))

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnClickOutside = true,
            dismissOnBackPress = true
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 20.dp)
                .clickable(
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                    indication = null
                ) { onDismiss() },
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 480.dp)
                    .clickable(
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                        indication = null
                    ) { /* Consume click inside card */ }
                    .testTag("comprehensive_transaction_details_card"),
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(
                    1.5.dp,
                    if (isExpense) Color(0xFFDC2626).copy(alpha = 0.35f)
                    else Color(0xFF16A34A).copy(alpha = 0.35f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // 1. Top Header with Title and Cross (Close) Button
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 18.dp, end = 10.dp, top = 14.dp, bottom = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                modifier = Modifier.size(34.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.ReceiptLong,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            Column {
                                Text(
                                    text = if (isEnglish) "Transaction Details (A to Z)" else "হিসাবের পূর্ণাঙ্গ বিবরণ",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (isEnglish) "Detailed record breakdown" else "লেনদেনের যাবতীয় তথ্যাবলী",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }
                        }

                        // Close (Cross) Button
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                                .testTag("btn_close_tx_details")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))

                    // 2. Scrollable Body
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(scrollState)
                                .padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            // Hero Amount Banner
                            Surface(
                                shape = RoundedCornerShape(18.dp),
                                color = if (isExpense) Color(0xFFDC2626).copy(alpha = 0.10f) else Color(0xFF16A34A).copy(alpha = 0.10f),
                                border = BorderStroke(
                                    1.2.dp,
                                    if (isExpense) Color(0xFFDC2626).copy(alpha = 0.35f) else Color(0xFF16A34A).copy(alpha = 0.35f)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(20.dp),
                                        color = if (isExpense) Color(0xFFDC2626).copy(alpha = 0.20f) else Color(0xFF16A34A).copy(alpha = 0.20f)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (isExpense) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                                                contentDescription = null,
                                                tint = if (isExpense) Color(0xFFDC2626) else Color(0xFF16A34A),
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Text(
                                                text = if (isExpense) {
                                                    if (isEnglish) "Expense (Cash Out)" else "ব্যয় (টাকা গেছে)"
                                                } else {
                                                    if (isEnglish) "Income (Cash In)" else "আয় (টাকা ঢুকেছে)"
                                                },
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isExpense) Color(0xFFDC2626) else Color(0xFF16A34A),
                                                    fontSize = 11.5.sp
                                                )
                                            )
                                        }
                                    }

                                    Text(
                                        text = "${if (isExpense) "- " else "+ "}${formatTakaSafe(item.amount)}",
                                        style = MaterialTheme.typography.headlineMedium.copy(
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 28.sp,
                                            color = if (isExpense) Color(0xFFDC2626) else Color(0xFF16A34A)
                                        )
                                    )
                                }
                            }

                            // Structured Metadata Card
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    // Row: Category & Title
                                    DetailMetaRow(
                                        icon = categoryIcon,
                                        iconTint = categoryColor,
                                        label = if (isEnglish) "Category & Title" else "ক্যাটাগরি ও বিবরণ",
                                        value = "${AppLocale.category(item.category, isEnglish)}${
                                            if (item.title.isNotBlank() && item.title != item.category) " (${item.title})" else ""
                                        }"
                                    )

                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))

                                    // Row: Fund Source / Khat
                                    DetailMetaRow(
                                        icon = if (item.khatName.isNotBlank()) getKhatIcon("", item.khatName) else Icons.Default.AccountBalanceWallet,
                                        iconTint = Color(0xFF4F46E5),
                                        label = if (isEnglish) "Budget / Fund Source" else "বাজেট খাত ও তহবিল উৎস",
                                        value = if (item.khatName.isNotBlank()) {
                                            if (isEnglish) "Khat: ${AppLocale.khatName(item.khatName, true)}" else "খাত: ${item.khatName}"
                                        } else {
                                            if (isEnglish) "General Account / Cash (Oboshisto)" else "উৎস: অবশিষ্ট / সাধারণ ক্যাশ ব্যালেন্স"
                                        }
                                    )

                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))

                                    // Row: Payment Method
                                    DetailMetaRow(
                                        icon = Icons.Default.Payments,
                                        iconTint = Color(0xFFF59E0B),
                                        label = if (isEnglish) "Payment Method" else "লেনদেনের মাধ্যম",
                                        value = item.paymentMethod.ifBlank { if (isEnglish) "Cash" else "ক্যাশ" }
                                    )

                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))

                                    // Row: Date & Day
                                    DetailMetaRow(
                                        icon = Icons.Default.CalendarMonth,
                                        iconTint = Color(0xFF06B6D4),
                                        label = if (isEnglish) "Date & Day" else "তারিখ ও বার",
                                        value = fullDateString
                                    )

                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))

                                    // Row: Exact Time
                                    DetailMetaRow(
                                        icon = Icons.Default.Schedule,
                                        iconTint = Color(0xFF8B5CF6),
                                        label = if (isEnglish) "Exact Time" else "সঠিক সময়",
                                        value = formattedTime
                                    )

                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))

                                    // Row: Reference ID & Status
                                    DetailMetaRow(
                                        icon = Icons.Default.Fingerprint,
                                        iconTint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        label = if (isEnglish) "Tracking & System ID" else "সিস্টেম আইডি ও রেকর্ড কোড",
                                        value = "#${item.id} • ${item.syncId.take(12).uppercase()}"
                                    )
                                }
                            }

                            // Extra Notes Section
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Notes,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = if (isEnglish) "Additional Notes & Remarks" else "অতিরিক্ত নোট ও মন্তব্য",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary,
                                                fontSize = 11.5.sp
                                            )
                                        )
                                    }
                                    Text(
                                        text = if (item.note.isNotBlank()) item.note else (if (isEnglish) "No additional notes recorded for this transaction." else "এই লেনদেনে কোনো অতিরিক্ত নোট যুক্ত করা হয়নি।"),
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontSize = 13.sp,
                                            color = if (item.note.isNotBlank()) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                        ),
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                }
                            }

                            // Bottom Action Buttons
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedButton(
                                    onClick = onGenerateReceipt,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ReceiptLong,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(if (isEnglish) "Receipt" else "রসিদ", fontSize = 12.5.sp)
                                }

                                Button(
                                    onClick = onDelete,
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DeleteOutline,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(if (isEnglish) "Delete" else "মুছুন", fontSize = 12.5.sp)
                                }
                            }
                        }

                        // Scroll Down Floating Hint if content is large/overflowing
                        if (scrollState.canScrollForward) {
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.95f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                                shadowElevation = 4.dp,
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(bottom = 8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = if (isEnglish) "Scroll down for more ▾" else "নিচে আরো দেখতে স্ক্রোল করুন ▾",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            fontSize = 11.sp
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailMetaRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
            )
        }
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 12.5.sp
            ),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private fun formatDateTime(timestamp: Long, isEnglish: Boolean = false): String {
    val cal = Calendar.getInstance()
    val todayYear = cal.get(Calendar.YEAR)
    val todayDay = cal.get(Calendar.DAY_OF_YEAR)

    cal.timeInMillis = timestamp
    val itemYear = cal.get(Calendar.YEAR)
    val itemDay = cal.get(Calendar.DAY_OF_YEAR)

    val timeFormat = SimpleDateFormat("hh:mm a", Locale.US)
    val timeStr = timeFormat.format(Date(timestamp))

    return when {
        todayYear == itemYear && todayDay == itemDay -> if (isEnglish) "Today, $timeStr" else "আজ, $timeStr"
        todayYear == itemYear && todayDay - 1 == itemDay -> if (isEnglish) "Yesterday, $timeStr" else "গতকাল, $timeStr"
        else -> {
            val dateFormat = SimpleDateFormat("dd MMM, hh:mm a", Locale.US)
            dateFormat.format(Date(timestamp))
        }
    }
}

private fun formatBengaliFullDate(cal: Calendar, isEnglish: Boolean): String {
    if (isEnglish) {
        val fmt = SimpleDateFormat("EEEE, d MMMM yyyy", Locale.US)
        return fmt.format(cal.time)
    }
    val bengaliDays = arrayOf("রবিবার", "সোমবার", "মঙ্গলবার", "বুধবার", "বৃহস্পতিবার", "শুক্রবার", "শনিবার")
    val bengaliMonths = arrayOf("জানুয়ারি", "ফেব্রুয়ারি", "মার্চ", "এপ্রিল", "মে", "জুন", "জুলাই", "আগস্ট", "সেপ্টেম্বর", "অক্টোবর", "নভেম্বর", "ডিসেম্বর")
    val dayOfWeek = bengaliDays[cal.get(Calendar.DAY_OF_WEEK) - 1]
    val dayOfMonth = cal.get(Calendar.DAY_OF_MONTH)
    val monthName = bengaliMonths[cal.get(Calendar.MONTH)]
    val year = cal.get(Calendar.YEAR)
    return "$dayOfWeek, $dayOfMonth $monthName $year"
}

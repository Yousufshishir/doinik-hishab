package com.example.ui.screens

import android.content.Intent
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import java.util.Calendar
import java.text.SimpleDateFormat
import java.util.Locale
import android.app.DatePickerDialog
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.DebtEntity
import com.example.ui.components.getKhatIcon
import com.example.ui.util.AppLocale
import com.example.ui.util.clearFocusOnTap
import com.example.data.model.formatTakaSafe
import com.example.ui.components.AppCardDefaults
import com.example.ui.components.KeyboardScrollDownHint
import com.example.ui.viewmodel.ExpenseViewModel
import kotlinx.coroutines.launch

@OptIn(
    ExperimentalMaterial3Api::class,
    androidx.compose.foundation.ExperimentalFoundationApi::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class
)
@Composable
fun DebtsScreen(
    viewModel: ExpenseViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val debts by viewModel.debts.collectAsState()
    val totalReceivable by viewModel.totalReceivable.collectAsState()
    val totalPayable by viewModel.totalPayable.collectAsState()
    val allKhats by viewModel.khats.collectAsState()
    val netBalance by viewModel.netBalance.collectAsState()
    val oboshistoBalance by viewModel.oboshistoBalance.collectAsState()
    val isEnglish by viewModel.isEnglish.collectAsState()

    var selectedTab by remember { mutableStateOf("ALL") } // "ALL", "RECEIVE", "PAY", "SETTLED"
    var showAddDialog by remember { mutableStateOf(false) }
    var debtToDelete by remember { mutableStateOf<DebtEntity?>(null) }
    var debtForPayment by remember { mutableStateOf<DebtEntity?>(null) }
    var debtForReceipt by remember { mutableStateOf<DebtEntity?>(null) }

    val filteredDebts = remember(debts, selectedTab) {
        when (selectedTab) {
            "RECEIVE" -> debts.filter { it.type == "RECEIVE" && !it.isSettled }
            "PAY" -> debts.filter { it.type == "PAY" && !it.isSettled }
            "SETTLED" -> debts.filter { it.isSettled }
            else -> debts.filter { !it.isSettled }
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .clearFocusOnTap(),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.SwapHoriz,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = if (isEnglish) "Debts & Loans" else "ধার-দেনা খাতা",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = (-0.3).sp
                                )
                            )
                            Text(
                                text = if (isEnglish) "Receivables, payables & settlement" else "কার কাছে কত পাবেন ও দেবেন তার হিসাব",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    val activeCount = debts.count { !it.isSettled }
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                        modifier = Modifier.padding(end = 12.dp)
                    ) {
                        Text(
                            text = if (isEnglish) "$activeCount Active" else "$activeCount টি বাকি",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(16.dp),
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp),
                modifier = Modifier
                    .height(46.dp)
                    .testTag("fab_add_debt")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Add Debt", modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isEnglish) "New Debt Entry" else "নতুন ধার হিসাব",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.5.sp
                    )
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item { Spacer(modifier = Modifier.height(4.dp)) }

            // High-End Fintech Portfolio Summary Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("debts_summary_card"),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        width = 1.2.dp,
                        brush = Brush.linearGradient(
                            listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f),
                                MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f)
                            )
                        )
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Net Balance Status Header Bar
                        val netBalance = totalReceivable - totalPayable
                        val isNetPositive = netBalance > 0.001
                        val isNetNegative = netBalance < -0.001
                        val netColor = if (isNetPositive) Color(0xFF10B981) else if (isNetNegative) Color(0xFFEF4444) else MaterialTheme.colorScheme.onSurfaceVariant

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.AccountBalanceWallet,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isEnglish) "Net Position" else "নিট দেনা-পাওনা স্থিতি",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = netColor.copy(alpha = 0.12f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, netColor.copy(alpha = 0.35f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (isNetPositive) Icons.Default.ArrowDownward else if (isNetNegative) Icons.Default.ArrowUpward else Icons.Default.Check,
                                        contentDescription = null,
                                        tint = netColor,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (isNetPositive) {
                                            if (isEnglish) "Net Receivable: +${formatTakaSafe(netBalance)}" else "পাওনা বেশি: +${formatTakaSafe(netBalance)}"
                                        } else if (isNetNegative) {
                                            if (isEnglish) "Net Payable: -${formatTakaSafe(kotlin.math.abs(netBalance))}" else "দেনা বেশি: -${formatTakaSafe(kotlin.math.abs(netBalance))}"
                                        } else {
                                            if (isEnglish) "Balanced (৳0)" else "সমান সমান স্থিতি (৳০)"
                                        },
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.ExtraBold,
                                            color = netColor
                                        )
                                    )
                                }
                            }
                        }

                        // Twin Metric Cards for Receivable & Payable
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Column 1: Receivables (পাব)
                            Surface(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(16.dp),
                                color = Color(0xFF10B981).copy(alpha = 0.08f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.3f))
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = if (isEnglish) "Receivable" else "মোট পাব",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF047857)
                                            )
                                        )
                                        Surface(
                                            shape = CircleShape,
                                            color = Color(0xFF10B981).copy(alpha = 0.2f),
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = Icons.Default.ArrowDownward,
                                                    contentDescription = null,
                                                    tint = Color(0xFF047857),
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                        }
                                    }
                                    Text(
                                        text = formatTakaSafe(totalReceivable),
                                        style = MaterialTheme.typography.titleLarge.copy(
                                            fontWeight = FontWeight.Black,
                                            color = Color(0xFF047857)
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = if (isEnglish) "${debts.count { it.type == "RECEIVE" && !it.isSettled }} people owe you"
                                        else "${debts.count { it.type == "RECEIVE" && !it.isSettled }} জন মানুষ দেবেন",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            // Column 2: Payables (দেব)
                            Surface(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(16.dp),
                                color = Color(0xFFEF4444).copy(alpha = 0.08f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.3f))
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = if (isEnglish) "Payable" else "মোট দেব",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFFB91C1C)
                                            )
                                        )
                                        Surface(
                                            shape = CircleShape,
                                            color = Color(0xFFEF4444).copy(alpha = 0.2f),
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = Icons.Default.ArrowUpward,
                                                    contentDescription = null,
                                                    tint = Color(0xFFB91C1C),
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                        }
                                    }
                                    Text(
                                        text = formatTakaSafe(totalPayable),
                                        style = MaterialTheme.typography.titleLarge.copy(
                                            fontWeight = FontWeight.Black,
                                            color = Color(0xFFB91C1C)
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = if (isEnglish) "${debts.count { it.type == "PAY" && !it.isSettled }} people to repay"
                                        else "${debts.count { it.type == "PAY" && !it.isSettled }} জনকে ফেরত দেবেন",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Modern Styled Filter Tabs with Counts
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val countAll = debts.count { !it.isSettled }
                    val countReceive = debts.count { it.type == "RECEIVE" && !it.isSettled }
                    val countPay = debts.count { it.type == "PAY" && !it.isSettled }
                    val countSettled = debts.count { it.isSettled }

                    // Row 1: Active 3 tabs
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = selectedTab == "ALL",
                            onClick = { selectedTab = "ALL" },
                            label = {
                                Text(
                                    text = if (isEnglish) "All ($countAll)" else "সব বাকি ($countAll)",
                                    maxLines = 1,
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.Center,
                                    fontWeight = if (selectedTab == "ALL") FontWeight.Bold else FontWeight.Medium
                                )
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("debt_filter_all")
                        )
                        FilterChip(
                            selected = selectedTab == "RECEIVE",
                            onClick = { selectedTab = "RECEIVE" },
                            label = {
                                Text(
                                    text = if (isEnglish) "🟢 Receive ($countReceive)" else "🟢 পাব ($countReceive)",
                                    maxLines = 1,
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.Center,
                                    fontWeight = if (selectedTab == "RECEIVE") FontWeight.Bold else FontWeight.Medium
                                )
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("debt_filter_receive")
                        )
                        FilterChip(
                            selected = selectedTab == "PAY",
                            onClick = { selectedTab = "PAY" },
                            label = {
                                Text(
                                    text = if (isEnglish) "🔴 Pay ($countPay)" else "🔴 দেব ($countPay)",
                                    maxLines = 1,
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.Center,
                                    fontWeight = if (selectedTab == "PAY") FontWeight.Bold else FontWeight.Medium
                                )
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("debt_filter_pay")
                        )
                    }

                    // Row 2: Settled records
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start
                    ) {
                        FilterChip(
                            selected = selectedTab == "SETTLED",
                            onClick = { selectedTab = "SETTLED" },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = if (selectedTab == "SETTLED") MaterialTheme.colorScheme.primary else Color(0xFF10B981)
                                )
                            },
                            label = {
                                Text(
                                    text = if (isEnglish) "Settled Records ($countSettled)" else "পরিশোধিত খতিয়ান ($countSettled)",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                                )
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("debt_filter_settled")
                        )
                    }
                }
            }

            // Debt List
            if (filteredDebts.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(30.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                                modifier = Modifier.size(64.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Handshake,
                                        contentDescription = null,
                                        modifier = Modifier.size(34.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            Text(
                                text = AppLocale.noDebts(isEnglish),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = if (isEnglish) "Record money lent or borrowed easily. Tap the 'New Debt Entry' button below."
                                else "কাউকে টাকা ধার দিলে বা কারো কাছ থেকে ধার নিলে 'নতুন ধার হিসাব' বাটন দিয়ে সহজেই লিখে রাখুন।",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                lineHeight = 18.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Button(
                                onClick = { showAddDialog = true },
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isEnglish) "Add First Debt" else "প্রথম ধার হিসাব যোগ করুন",
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            } else {
                items(filteredDebts, key = { it.id }) { item ->
                    DebtCardItem(
                        item = item,
                        netBalance = netBalance,
                        isEnglish = isEnglish,
                        onRecordPayment = {
                            debtForPayment = item
                        },
                        onUnsettle = {
                            viewModel.unsettleDebt(item, isEnglish)
                        },
                        onShowReceipt = {
                            debtForReceipt = item
                        },
                        onDelete = { debtToDelete = item }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }

    // Add Debt Dialog (Floating Card with Interactive Calendar DatePicker)
    if (showAddDialog) {
        var personName by remember { mutableStateOf("") }
        var amountText by remember { mutableStateOf("") }
        var debtType by remember { mutableStateOf("RECEIVE") } // "RECEIVE" or "PAY"
        var dueDate by remember { mutableStateOf("") }
        var note by remember { mutableStateOf("") }
        var selectedSourceType by remember { mutableStateOf("NONE") } // "NONE", "MAIN", "KHAT"
        var selectedKhatId by remember { mutableStateOf<Long?>(null) }
        var selectedKhatName by remember { mutableStateOf("") }

        val focusManager = LocalFocusManager.current
        val keyboardController = LocalSoftwareKeyboardController.current

        val imeInsets = WindowInsets.ime
        val density = LocalDensity.current
        val isKeyboardOpen = imeInsets.getBottom(density) > 0

        val animatedCardElevation by animateDpAsState(
            targetValue = if (isKeyboardOpen) 14.dp else 8.dp,
            animationSpec = tween(durationMillis = 250),
            label = "debt_card_elevation"
        )
        val animatedVerticalPadding by animateDpAsState(
            targetValue = if (isKeyboardOpen) 6.dp else 20.dp,
            animationSpec = tween(durationMillis = 250),
            label = "debt_vertical_padding"
        )
        val animatedContentPadding by animateDpAsState(
            targetValue = if (isKeyboardOpen) 16.dp else 22.dp,
            animationSpec = tween(durationMillis = 250),
            label = "debt_content_padding"
        )
        val animatedSpacing by animateDpAsState(
            targetValue = if (isKeyboardOpen) 10.dp else 14.dp,
            animationSpec = tween(durationMillis = 250),
            label = "debt_spacing"
        )

        // DatePickerDialog Launcher for Expected Return Date
        val openDatePicker = {
            val cal = Calendar.getInstance()
            DatePickerDialog(
                context,
                { _, year, month, dayOfMonth ->
                    val picked = Calendar.getInstance().apply {
                        set(Calendar.YEAR, year)
                        set(Calendar.MONTH, month)
                        set(Calendar.DAY_OF_MONTH, dayOfMonth)
                    }
                    val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                    dueDate = sdf.format(picked.time)
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        // Quick date preset setter
        val setQuickDate = { daysToAdd: Int ->
            val cal = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, daysToAdd)
            }
            val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
            dueDate = sdf.format(cal.time)
        }

        val setEndOfMonth = {
            val cal = Calendar.getInstance().apply {
                set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
            }
            val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
            dueDate = sdf.format(cal.time)
        }

        Dialog(
            onDismissRequest = {
                focusManager.clearFocus()
                keyboardController?.hide()
                showAddDialog = false
            },
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false,
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
                    .padding(horizontal = 18.dp, vertical = animatedVerticalPadding)
                    .clearFocusOnTap(),
                contentAlignment = if (isKeyboardOpen) Alignment.TopCenter else Alignment.Center
            ) {
                val addDebtScrollState = rememberScrollState()

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 460.dp)
                        .testTag("add_debt_dialog"),
                    shape = RoundedCornerShape(26.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        width = 1.2.dp,
                        brush = Brush.linearGradient(
                            if (debtType == "RECEIVE") listOf(Color(0xFF10B981), MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                            else listOf(Color(0xFFEF4444), MaterialTheme.colorScheme.error.copy(alpha = 0.5f))
                        )
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = animatedCardElevation)
                ) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = animatedContentPadding, vertical = animatedContentPadding)
                                .verticalScroll(addDebtScrollState),
                            verticalArrangement = Arrangement.spacedBy(animatedSpacing)
                        ) {
                            // Dialog Header
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        shape = CircleShape,
                                        color = if (debtType == "RECEIVE") Color(0xFF10B981).copy(alpha = 0.15f) else Color(0xFFEF4444).copy(alpha = 0.15f),
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = if (debtType == "RECEIVE") Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                                                contentDescription = null,
                                                tint = if (debtType == "RECEIVE") Color(0xFF047857) else Color(0xFFB91C1C),
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = if (isEnglish) "New Debt / Loan Entry" else "নতুন ধার-দেনা এন্ট্রি",
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = if (debtType == "RECEIVE") (if (isEnglish) "You lent money to someone" else "আপনি কাউকে টাকা ধার দিয়েছেন")
                                            else (if (isEnglish) "You borrowed money from someone" else "আপনি কারো থেকে ধার নিয়েছেন"),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                IconButton(
                                    onClick = {
                                        focusManager.clearFocus()
                                        keyboardController?.hide()
                                        showAddDialog = false
                                    },
                                    modifier = Modifier.testTag("dialog_close_debt")
                                ) {
                                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                                }
                            }

                            // Distinct Visual Type Selector Cards
                            val isReceive = debtType == "RECEIVE"
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // Option 1: Receivable (আমি পাব)
                                Surface(
                                    onClick = { debtType = "RECEIVE" },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(14.dp),
                                    color = if (isReceive) Color(0xFF10B981).copy(alpha = 0.14f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                    border = androidx.compose.foundation.BorderStroke(
                                        width = if (isReceive) 1.5.dp else 1.dp,
                                        color = if (isReceive) Color(0xFF10B981) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier.padding(10.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = if (isReceive) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                                contentDescription = null,
                                                tint = if (isReceive) Color(0xFF047857) else MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(15.dp)
                                            )
                                            Spacer(modifier = Modifier.width(5.dp))
                                            Text(
                                                text = if (isEnglish) "I Will Receive" else "আমি পাব",
                                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                                color = if (isReceive) Color(0xFF047857) else MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                        Text(
                                            text = if (isEnglish) "Lent money" else "টাকা দিয়েছি",
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                // Option 2: Payable (আমি দেব)
                                Surface(
                                    onClick = { debtType = "PAY" },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(14.dp),
                                    color = if (!isReceive) Color(0xFFEF4444).copy(alpha = 0.14f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                    border = androidx.compose.foundation.BorderStroke(
                                        width = if (!isReceive) 1.5.dp else 1.dp,
                                        color = if (!isReceive) Color(0xFFEF4444) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier.padding(10.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = if (!isReceive) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                                contentDescription = null,
                                                tint = if (!isReceive) Color(0xFFB91C1C) else MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(15.dp)
                                            )
                                            Spacer(modifier = Modifier.width(5.dp))
                                            Text(
                                                text = if (isEnglish) "I Will Pay" else "আমি দেব",
                                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                                color = if (!isReceive) Color(0xFFB91C1C) else MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                        Text(
                                            text = if (isEnglish) "Borrowed money" else "টাকা নিয়েছি",
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            // Person Name Field
                            OutlinedTextField(
                                value = personName,
                                onValueChange = { personName = it },
                                label = { Text(if (isEnglish) "Person Name" else "ব্যক্তির নাম") },
                                placeholder = { Text(if (isEnglish) "e.g., Rahim, John" else "যেমন: রহিম সাহেব, সাজিদ") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(16.dp),
                                keyboardOptions = KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Next),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("input_debt_name")
                            )

                            // Amount Field with Taka symbol
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                OutlinedTextField(
                                    value = amountText,
                                    onValueChange = { input ->
                                        amountText = input.filter { it.isDigit() || it == '.' }
                                    },
                                    label = { Text(if (isEnglish) "Amount (৳)" else "টাকার পরিমাণ (৳)") },
                                    leadingIcon = {
                                        Text(
                                            text = "৳",
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.Black,
                                                color = MaterialTheme.colorScheme.primary
                                            ),
                                            modifier = Modifier.padding(start = 14.dp)
                                        )
                                    },
                                    shape = RoundedCornerShape(16.dp),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = androidx.compose.ui.text.input.ImeAction.Next),
                                    singleLine = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("input_debt_amount")
                                )

                                // Quick Amount Preset Chips
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    listOf(500, 1000, 2000, 5000).forEach { preset ->
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                                            modifier = Modifier
                                                .weight(1f)
                                                .clickable {
                                                    val current = amountText.toDoubleOrNull() ?: 0.0
                                                    amountText = (current + preset).toInt().toString()
                                                }
                                        ) {
                                            Text(
                                                text = "+৳$preset",
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                textAlign = TextAlign.Center,
                                                modifier = Modifier.padding(vertical = 5.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            // ================= CALENDAR DATE PICKER (FEROTER SHOMVABBO TARIKH) =================
                            // Replaces the old plain text field with an elegant interactive Calendar component
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = if (isEnglish) "Expected Return Date (Optional)" else "ফেরতের সম্ভাব্য তারিখ (ঐচ্ছিক)",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Surface(
                                    onClick = { openDatePicker() },
                                    shape = RoundedCornerShape(16.dp),
                                    color = if (dueDate.isNotBlank()) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                    border = androidx.compose.foundation.BorderStroke(
                                        width = if (dueDate.isNotBlank()) 1.2.dp else 1.dp,
                                        color = if (dueDate.isNotBlank()) MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                                        else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 14.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f, fill = false)
                                        ) {
                                            Surface(
                                                shape = CircleShape,
                                                color = if (dueDate.isNotBlank()) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                                                else MaterialTheme.colorScheme.surfaceVariant,
                                                modifier = Modifier.size(34.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(
                                                        imageVector = Icons.Default.CalendarMonth,
                                                        contentDescription = "Calendar",
                                                        tint = if (dueDate.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column {
                                                if (dueDate.isNotBlank()) {
                                                    Text(
                                                        text = dueDate,
                                                        style = MaterialTheme.typography.bodyMedium.copy(
                                                            fontWeight = FontWeight.Bold,
                                                            color = MaterialTheme.colorScheme.primary
                                                        )
                                                    )
                                                    Text(
                                                        text = if (isEnglish) "Tap to change date" else "তারিখ পরিবর্তন করতে ট্যাপ করুন",
                                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                } else {
                                                    Text(
                                                        text = if (isEnglish) "Select date from calendar" else "ক্যালেন্ডার থেকে তারিখ বাছুন",
                                                        style = MaterialTheme.typography.bodyMedium.copy(
                                                            fontWeight = FontWeight.Medium,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    )
                                                    Text(
                                                        text = if (isEnglish) "Optional - tap to open calendar" else "ঐচ্ছিক - ট্যাপ করলে ক্যালেন্ডার খুলবে",
                                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                                        color = MaterialTheme.colorScheme.outline
                                                    )
                                                }
                                            }
                                        }

                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            if (dueDate.isNotBlank()) {
                                                // Clear Date Button (keeps it optional!)
                                                IconButton(
                                                    onClick = { dueDate = "" },
                                                    modifier = Modifier.size(30.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Close,
                                                        contentDescription = "Clear date",
                                                        tint = MaterialTheme.colorScheme.error,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                                Spacer(modifier = Modifier.width(4.dp))
                                            }

                                            Surface(
                                                shape = RoundedCornerShape(10.dp),
                                                color = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.clickable { openDatePicker() }
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.CalendarMonth,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.onPrimary,
                                                        modifier = Modifier.size(13.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(
                                                        text = if (isEnglish) "Calendar" else "ক্যালেন্ডার",
                                                        style = MaterialTheme.typography.labelSmall.copy(
                                                            fontWeight = FontWeight.Bold,
                                                            color = MaterialTheme.colorScheme.onPrimary
                                                        )
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                // Convenient Quick Date Shortcut Chips
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                        border = androidx.compose.foundation.BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)),
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { setQuickDate(7) }
                                    ) {
                                        Text(
                                            text = if (isEnglish) "+7 Days" else "+৭ দিন",
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.padding(vertical = 4.dp)
                                        )
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                        border = androidx.compose.foundation.BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)),
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { setQuickDate(15) }
                                    ) {
                                        Text(
                                            text = if (isEnglish) "+15 Days" else "+১৫ দিন",
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.padding(vertical = 4.dp)
                                        )
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                        border = androidx.compose.foundation.BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)),
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { setQuickDate(30) }
                                    ) {
                                        Text(
                                            text = if (isEnglish) "+1 Month" else "+১ মাস",
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.padding(vertical = 4.dp)
                                        )
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                        border = androidx.compose.foundation.BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)),
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { setEndOfMonth() }
                                    ) {
                                        Text(
                                            text = if (isEnglish) "Month End" else "মাস শেষ",
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.padding(vertical = 4.dp)
                                        )
                                    }
                                }
                            }

                            // ================= BALANCE & KHAT FUND SOURCE SELECTION (OPTIONAL) =================
                            Surface(
                                shape = RoundedCornerShape(18.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                border = androidx.compose.foundation.BorderStroke(
                                    width = 1.dp,
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    // Section Header
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Surface(
                                            shape = CircleShape,
                                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = Icons.Default.AccountBalanceWallet,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                text = AppLocale.debtFundSourceTitle(isEnglish),
                                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = if (isReceive) AppLocale.debtDeductSourceSubtitle(isEnglish)
                                                else AppLocale.debtAddSourceSubtitle(isEnglish),
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    // Option 1: Do Not Adjust (Record Only) - Default
                                    val isNoneSelected = selectedSourceType == "NONE"
                                    Surface(
                                        onClick = {
                                            selectedSourceType = "NONE"
                                            selectedKhatId = null
                                            selectedKhatName = ""
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (isNoneSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                                        else MaterialTheme.colorScheme.surface,
                                        border = androidx.compose.foundation.BorderStroke(
                                            width = if (isNoneSelected) 1.5.dp else 0.8.dp,
                                            color = if (isNoneSelected) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 12.dp, vertical = 9.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = if (isNoneSelected) Icons.Default.RadioButtonChecked else Icons.Default.RadioButtonUnchecked,
                                                contentDescription = null,
                                                tint = if (isNoneSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = if (isReceive) AppLocale.debtSourceNone(isEnglish) else AppLocale.debtSourceNonePay(isEnglish),
                                                    style = MaterialTheme.typography.bodySmall.copy(
                                                        fontWeight = if (isNoneSelected) FontWeight.Bold else FontWeight.Medium
                                                    ),
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Text(
                                                    text = if (isEnglish) "Only record in debt ledger (balances unchanged)"
                                                    else "শুধুমাত্র ধার খাতায় রেকর্ড থাকবে (ব্যালেন্সে পরিবর্তন হবে না)",
                                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp),
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }

                                    if (allKhats.isEmpty()) {
                                        // Option 2: মূল ক্যাশ ব্যালেন্স (Main Cash Balance) - Only shown when NO custom khats are created
                                        val isMainSelected = selectedSourceType == "MAIN"
                                        val enteredAmt = amountText.toDoubleOrNull() ?: 0.0
                                        val isInsufficientMain = isReceive && isMainSelected && enteredAmt > netBalance

                                        Surface(
                                            onClick = {
                                                selectedSourceType = "MAIN"
                                                selectedKhatId = null
                                                selectedKhatName = ""
                                            },
                                            shape = RoundedCornerShape(12.dp),
                                            color = if (isMainSelected) (if (isReceive) Color(0xFFEF4444).copy(alpha = 0.12f) else Color(0xFF10B981).copy(alpha = 0.12f))
                                            else MaterialTheme.colorScheme.surface,
                                            border = androidx.compose.foundation.BorderStroke(
                                                width = if (isMainSelected) 1.5.dp else 0.8.dp,
                                                color = if (isMainSelected) (if (isInsufficientMain) MaterialTheme.colorScheme.error else (if (isReceive) Color(0xFFDC2626) else Color(0xFF059669)))
                                                else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                                            ),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 12.dp, vertical = 9.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = if (isMainSelected) Icons.Default.RadioButtonChecked else Icons.Default.RadioButtonUnchecked,
                                                    contentDescription = null,
                                                    tint = if (isMainSelected) (if (isReceive) Color(0xFFDC2626) else Color(0xFF059669)) else MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        modifier = Modifier.fillMaxWidth()
                                                    ) {
                                                        Text(
                                                            text = AppLocale.debtSourceMain(isEnglish),
                                                            style = MaterialTheme.typography.bodySmall.copy(
                                                                fontWeight = if (isMainSelected) FontWeight.Bold else FontWeight.Medium
                                                            ),
                                                            color = MaterialTheme.colorScheme.onSurface
                                                        )
                                                        Surface(
                                                            shape = RoundedCornerShape(6.dp),
                                                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                                                        ) {
                                                            Text(
                                                                text = "${if (isEnglish) "Bal: " else "স্থিতি: "}${formatTakaSafe(netBalance)}",
                                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp, fontWeight = FontWeight.Bold),
                                                                color = MaterialTheme.colorScheme.primary,
                                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                            )
                                                        }
                                                    }
                                                    Text(
                                                        text = if (isReceive) AppLocale.debtSourceMainDeductDesc(isEnglish)
                                                        else AppLocale.debtSourceMainAddDesc(isEnglish),
                                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp),
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                        }

                                        // Insufficient balance warning for Main Cash Balance
                                        if (isReceive && isMainSelected && enteredAmt > netBalance) {
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f),
                                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Warning,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.error,
                                                        modifier = Modifier.size(15.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(
                                                        text = AppLocale.insufficientMainBalance(isEnglish, formatTakaSafe(netBalance)),
                                                        style = MaterialTheme.typography.labelSmall.copy(
                                                            fontWeight = FontWeight.Bold,
                                                            color = MaterialTheme.colorScheme.error
                                                        )
                                                    )
                                                }
                                            }
                                        }
                                    } else {
                                        // When custom khats ARE created: Show scrollable list with Oboshisto Balance and all custom Khats
                                        Text(
                                            text = AppLocale.allKhatsScrollHint(isEnglish),
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )

                                        val khatScrollState = rememberScrollState()
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .heightIn(max = 240.dp)
                                                .verticalScroll(khatScrollState)
                                        ) {
                                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                                // 1. অবশিষ্ট ব্যালেন্স (Oboshisto Balance - Available system fund)
                                                val isOboshistoSelected = selectedSourceType == "OBOSHISTO"
                                                val enteredAmt = amountText.toDoubleOrNull() ?: 0.0
                                                val isInsufficientOboshisto = isReceive && isOboshistoSelected && enteredAmt > oboshistoBalance
                                                val oboshistoColor = Color(0xFF059669)

                                                Surface(
                                                    onClick = {
                                                        selectedSourceType = "OBOSHISTO"
                                                        selectedKhatId = null
                                                        selectedKhatName = if (isEnglish) "Oboshisto" else "অবশিষ্ট ব্যালেন্স"
                                                    },
                                                    shape = RoundedCornerShape(12.dp),
                                                    color = if (isOboshistoSelected) (if (isReceive) Color(0xFFEF4444).copy(alpha = 0.12f) else Color(0xFF10B981).copy(alpha = 0.12f))
                                                    else MaterialTheme.colorScheme.surface,
                                                    border = androidx.compose.foundation.BorderStroke(
                                                        width = if (isOboshistoSelected) 1.5.dp else 0.8.dp,
                                                        color = if (isOboshistoSelected) (if (isInsufficientOboshisto) MaterialTheme.colorScheme.error else (if (isReceive) Color(0xFFDC2626) else oboshistoColor))
                                                        else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                                                    ),
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .padding(horizontal = 12.dp, vertical = 9.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Icon(
                                                            imageVector = if (isOboshistoSelected) Icons.Default.RadioButtonChecked else Icons.Default.RadioButtonUnchecked,
                                                            contentDescription = null,
                                                            tint = if (isOboshistoSelected) (if (isReceive) Color(0xFFDC2626) else oboshistoColor) else MaterialTheme.colorScheme.onSurfaceVariant,
                                                            modifier = Modifier.size(18.dp)
                                                        )
                                                        Spacer(modifier = Modifier.width(8.dp))
                                                        Surface(
                                                            shape = CircleShape,
                                                            color = oboshistoColor.copy(alpha = 0.15f),
                                                            modifier = Modifier.size(24.dp)
                                                        ) {
                                                            Box(contentAlignment = Alignment.Center) {
                                                                Icon(
                                                                    imageVector = Icons.Default.Savings,
                                                                    contentDescription = null,
                                                                    tint = oboshistoColor,
                                                                    modifier = Modifier.size(13.dp)
                                                                )
                                                            }
                                                        }
                                                        Spacer(modifier = Modifier.width(8.dp))
                                                        Column(modifier = Modifier.weight(1f)) {
                                                            Row(
                                                                verticalAlignment = Alignment.CenterVertically,
                                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                                modifier = Modifier.fillMaxWidth()
                                                            ) {
                                                                Text(
                                                                    text = AppLocale.debtSourceOboshisto(isEnglish),
                                                                    style = MaterialTheme.typography.bodySmall.copy(
                                                                        fontWeight = if (isOboshistoSelected) FontWeight.Bold else FontWeight.Medium
                                                                    ),
                                                                    color = MaterialTheme.colorScheme.onSurface
                                                                )
                                                                Surface(
                                                                    shape = RoundedCornerShape(6.dp),
                                                                    color = oboshistoColor.copy(alpha = 0.12f)
                                                                ) {
                                                                    Text(
                                                                        text = formatTakaSafe(oboshistoBalance),
                                                                        style = MaterialTheme.typography.labelSmall.copy(
                                                                            fontSize = 10.sp,
                                                                            fontWeight = FontWeight.Bold,
                                                                            color = oboshistoColor
                                                                        ),
                                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                                    )
                                                                }
                                                            }
                                                            Text(
                                                                text = if (isReceive) AppLocale.debtSourceOboshistoDeductDesc(isEnglish)
                                                                else AppLocale.debtSourceOboshistoAddDesc(isEnglish),
                                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                                            )
                                                        }
                                                    }
                                                }

                                                if (isReceive && isOboshistoSelected && enteredAmt > oboshistoBalance) {
                                                    Surface(
                                                        shape = RoundedCornerShape(8.dp),
                                                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f),
                                                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                                                        modifier = Modifier.fillMaxWidth()
                                                    ) {
                                                        Row(
                                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Default.Warning,
                                                                contentDescription = null,
                                                                tint = MaterialTheme.colorScheme.error,
                                                                modifier = Modifier.size(15.dp)
                                                            )
                                                            Spacer(modifier = Modifier.width(6.dp))
                                                            Text(
                                                                text = AppLocale.insufficientMainBalance(isEnglish, formatTakaSafe(oboshistoBalance)),
                                                                style = MaterialTheme.typography.labelSmall.copy(
                                                                    fontWeight = FontWeight.Bold,
                                                                    color = MaterialTheme.colorScheme.error
                                                                )
                                                            )
                                                        }
                                                    }
                                                }

                                                // 2. Custom User Khats
                                                allKhats.forEach { khat ->
                                                    val isThisKhat = (selectedSourceType == "KHAT" && selectedKhatId == khat.id)
                                                    val kColor = try {
                                                        Color(android.graphics.Color.parseColor(khat.colorHex))
                                                    } catch (_: Exception) {
                                                        MaterialTheme.colorScheme.primary
                                                    }
                                                    val isInsufficientForKhat = isReceive && isThisKhat && enteredAmt > khat.allocatedAmount

                                                    Surface(
                                                        onClick = {
                                                            selectedSourceType = "KHAT"
                                                            selectedKhatId = khat.id
                                                            selectedKhatName = khat.name
                                                        },
                                                        shape = RoundedCornerShape(12.dp),
                                                        color = if (isThisKhat) kColor.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface,
                                                        border = androidx.compose.foundation.BorderStroke(
                                                            width = if (isThisKhat) 1.5.dp else 0.8.dp,
                                                            color = if (isThisKhat) (if (isInsufficientForKhat) MaterialTheme.colorScheme.error else kColor)
                                                            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                                                        ),
                                                        modifier = Modifier.fillMaxWidth()
                                                    ) {
                                                        Row(
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .padding(horizontal = 12.dp, vertical = 8.dp),
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Icon(
                                                                imageVector = if (isThisKhat) Icons.Default.RadioButtonChecked else Icons.Default.RadioButtonUnchecked,
                                                                contentDescription = null,
                                                                tint = if (isThisKhat) kColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                                                modifier = Modifier.size(18.dp)
                                                            )
                                                            Spacer(modifier = Modifier.width(8.dp))
                                                            Surface(
                                                                shape = CircleShape,
                                                                color = kColor.copy(alpha = 0.15f),
                                                                modifier = Modifier.size(24.dp)
                                                            ) {
                                                                Box(contentAlignment = Alignment.Center) {
                                                                    Icon(
                                                                        imageVector = getKhatIcon(khat.iconName, khat.name),
                                                                        contentDescription = null,
                                                                        tint = kColor,
                                                                        modifier = Modifier.size(13.dp)
                                                                    )
                                                                }
                                                            }
                                                            Spacer(modifier = Modifier.width(8.dp))
                                                            Column(modifier = Modifier.weight(1f)) {
                                                                Row(
                                                                    verticalAlignment = Alignment.CenterVertically,
                                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                                    modifier = Modifier.fillMaxWidth()
                                                                ) {
                                                                    Text(
                                                                        text = AppLocale.khatName(khat.name, isEnglish),
                                                                        style = MaterialTheme.typography.bodySmall.copy(
                                                                            fontWeight = if (isThisKhat) FontWeight.Bold else FontWeight.Medium
                                                                        ),
                                                                        color = MaterialTheme.colorScheme.onSurface
                                                                    )
                                                                    Surface(
                                                                        shape = RoundedCornerShape(6.dp),
                                                                        color = kColor.copy(alpha = 0.12f)
                                                                    ) {
                                                                        Text(
                                                                            text = formatTakaSafe(khat.allocatedAmount),
                                                                            style = MaterialTheme.typography.labelSmall.copy(
                                                                                fontSize = 10.sp,
                                                                                fontWeight = FontWeight.Bold,
                                                                                color = kColor
                                                                            ),
                                                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                                        )
                                                                    }
                                                                }
                                                                Text(
                                                                    text = if (isReceive) {
                                                                        if (isEnglish) "Deduct from '${AppLocale.khatName(khat.name, isEnglish)}'"
                                                                        else "'${AppLocale.khatName(khat.name, isEnglish)}' খাতের বরাদ্দ থেকে বাদ যাবে"
                                                                    } else {
                                                                        if (isEnglish) "Add to '${AppLocale.khatName(khat.name, isEnglish)}'"
                                                                        else "'${AppLocale.khatName(khat.name, isEnglish)}' খাতের বরাদ্দে যোগ হবে"
                                                                    },
                                                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }

                                        // Insufficient balance warning for Main Balance
                                        val entered = amountText.toDoubleOrNull() ?: 0.0
                                        if (isReceive && selectedSourceType == "MAIN" && entered > netBalance) {
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f),
                                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Warning,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.error,
                                                        modifier = Modifier.size(15.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(
                                                        text = if (isEnglish) "Insufficient main balance! Available: ${formatTakaSafe(netBalance)}"
                                                        else "মূল অ্যাকাউন্টে পর্যাপ্ত ব্যালেন্স নেই! বর্তমান ব্যালেন্স: ${formatTakaSafe(netBalance)}",
                                                        style = MaterialTheme.typography.labelSmall.copy(
                                                            fontWeight = FontWeight.Bold,
                                                            color = MaterialTheme.colorScheme.error
                                                        )
                                                    )
                                                }
                                            }
                                        }

                                        // Insufficient balance warning for Oboshisto Balance
                                        if (isReceive && selectedSourceType == "OBOSHISTO" && entered > oboshistoBalance) {
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f),
                                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Warning,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.error,
                                                        modifier = Modifier.size(15.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(
                                                        text = AppLocale.insufficientOboshistoBalance(isEnglish, formatTakaSafe(oboshistoBalance)),
                                                        style = MaterialTheme.typography.labelSmall.copy(
                                                            fontWeight = FontWeight.Bold,
                                                            color = MaterialTheme.colorScheme.error
                                                        )
                                                    )
                                                }
                                            }
                                        }

                                        // Insufficient balance warning for selected Khat
                                        if (isReceive && selectedSourceType == "KHAT") {
                                            val sKhat = allKhats.find { it.id == selectedKhatId }
                                            if (sKhat != null && entered > sKhat.allocatedAmount) {
                                                Surface(
                                                    shape = RoundedCornerShape(8.dp),
                                                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f),
                                                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Row(
                                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Warning,
                                                            contentDescription = null,
                                                            tint = MaterialTheme.colorScheme.error,
                                                            modifier = Modifier.size(15.dp)
                                                        )
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        Text(
                                                            text = AppLocale.insufficientKhatBalance(
                                                                isEnglish,
                                                                AppLocale.khatName(sKhat.name, isEnglish),
                                                                formatTakaSafe(sKhat.allocatedAmount)
                                                            ),
                                                            style = MaterialTheme.typography.labelSmall.copy(
                                                                fontWeight = FontWeight.Bold,
                                                                color = MaterialTheme.colorScheme.error
                                                            )
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // Note / Description Field
                            OutlinedTextField(
                                value = note,
                                onValueChange = { note = it },
                                label = { Text(if (isEnglish) "Note / Description (Optional)" else "নোট বা বিবরণ (ঐচ্ছিক)") },
                                placeholder = { Text(if (isEnglish) "e.g., Borrowed for shopping" else "যেমন: বাজার খরচের জন্য দেওয়া") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Notes,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(16.dp),
                                keyboardOptions = KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Done),
                                keyboardActions = androidx.compose.foundation.text.KeyboardActions(onDone = {
                                    focusManager.clearFocus()
                                    keyboardController?.hide()
                                }),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            // Action Buttons (Cancel and Save)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        focusManager.clearFocus()
                                        keyboardController?.hide()
                                        showAddDialog = false
                                    },
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                ) {
                                    Text(AppLocale.cancel(isEnglish))
                                }

                                val amount = amountText.toDoubleOrNull() ?: 0.0
                                val selectedKhat = if (selectedSourceType == "KHAT") allKhats.find { it.id == selectedKhatId } else null
                                val isInsufficientKhat = isReceive && selectedSourceType == "KHAT" && selectedKhat != null && amount > selectedKhat.allocatedAmount
                                val isInsufficientOboshistoAmt = isReceive && selectedSourceType == "OBOSHISTO" && amount > oboshistoBalance
                                val isInsufficientMainAmt = isReceive && selectedSourceType == "MAIN" && amount > netBalance
                                val valid = personName.isNotBlank() && amount > 0 && !isInsufficientKhat && !isInsufficientOboshistoAmt && !isInsufficientMainAmt
                                Button(
                                    onClick = {
                                        viewModel.addDebt(
                                            personName = personName.trim(),
                                            amount = amount,
                                            type = debtType,
                                            dueDate = dueDate.trim(),
                                            note = note.trim(),
                                            deductFromMain = (selectedSourceType == "OBOSHISTO" || selectedSourceType == "MAIN"),
                                            khatId = if (selectedSourceType == "KHAT") selectedKhatId else null,
                                            khatName = when (selectedSourceType) {
                                                "KHAT" -> (selectedKhat?.name ?: selectedKhatName)
                                                "OBOSHISTO" -> if (isEnglish) "Oboshisto" else "অবশিষ্ট ব্যালেন্স"
                                                "MAIN" -> ""
                                                else -> ""
                                            },
                                            onSuccess = {
                                                focusManager.clearFocus()
                                                keyboardController?.hide()
                                                showAddDialog = false
                                            },
                                            onError = { err ->
                                                viewModel.showToast(err)
                                            }
                                        )
                                    },
                                    enabled = valid,
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (debtType == "RECEIVE") Color(0xFF10B981) else MaterialTheme.colorScheme.primary
                                    ),
                                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 3.dp),
                                    modifier = Modifier
                                        .weight(1.3f)
                                        .height(48.dp)
                                        .testTag("btn_save_debt")
                                ) {
                                    Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (isEnglish) "Save Debt Record" else "ধার সংরক্ষণ করুন",
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        KeyboardScrollDownHint(
                            scrollState = addDebtScrollState,
                            isKeyboardOpen = isKeyboardOpen,
                            forceShowWhenScrollable = true,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 6.dp),
                            label = if (isEnglish) "Scroll down" else "নিচে স্ক্রল করুন"
                        )
                    }
                }
            }
        }
    }

    // Delete Confirmation Dialog
    debtToDelete?.let { item ->
        AlertDialog(
            onDismissRequest = { debtToDelete = null },
            title = { Text(AppLocale.deleteConfirm(isEnglish)) },
            text = {
                val adjustHint = if (isEnglish) {
                    if (item.khatName.isNotBlank()) " Deleting will adjust the amount back to '${item.khatName}' and remove it from home page history."
                    else if (item.deductedFromMain) " Deleting will adjust the amount back to main balance and remove it from home page history."
                    else ""
                } else {
                    if (item.khatName.isNotBlank()) " এই ধার ডিলিট করলে '${item.khatName}' খাতের বরাদ্দে টাকা পুনরায় সমন্বয় হবে এবং হোমপেজের ট্রানজেকশন থেকেও মুছে যাবে।"
                    else if (item.deductedFromMain) " এই ধার ডিলিট করলে মূল ব্যালেন্সে টাকা পুনরায় সমন্বয় হবে এবং হোমপেজের ট্রানজেকশন থেকেও মুছে যাবে।"
                    else ""
                }
                Text(
                    (if (isEnglish) "Do you want to delete debt record of ${item.personName} (${formatTakaSafe(item.amount)})?"
                    else "${item.personName}-এর ${formatTakaSafe(item.amount)} হিসাব মুছে ফেলতে চান?") + adjustHint
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteDebt(item)
                        debtToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(AppLocale.delete(isEnglish))
                }
            },
            dismissButton = {
                TextButton(onClick = { debtToDelete = null }) {
                    Text(AppLocale.cancel(isEnglish))
                }
            }
        )
    }

    // Debt Settlement & Installment Payment Dialog
    debtForPayment?.let { debt ->
        DebtPaymentDialog(
            debt = debt,
            netBalance = netBalance,
            isEnglish = isEnglish,
            onDismiss = { debtForPayment = null },
            onConfirm = { paymentAmount, isFull, adjustMainAccount, note ->
                viewModel.recordDebtPayment(
                    debt = debt,
                    paymentAmount = paymentAmount,
                    isFullSettlement = isFull,
                    adjustMainAccount = adjustMainAccount,
                    note = note,
                    isEnglish = isEnglish
                )
                debtForPayment = null
            }
        )
    }

    // Individual Debt Memo & Receipt Voucher Dialog
    debtForReceipt?.let { debt ->
        DebtReceiptDialog(
            debt = debt,
            isEnglish = isEnglish,
            onDismiss = { debtForReceipt = null }
        )
    }
}

@Composable
fun DebtPaymentDialog(
    debt: DebtEntity,
    netBalance: Double,
    isEnglish: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (paymentAmount: Double, isFullSettlement: Boolean, adjustMainAccount: Boolean, note: String) -> Unit
) {
    val isPay = debt.type == "PAY"
    val accentColor = if (isPay) Color(0xFFDC2626) else Color(0xFF059669)
    val remaining = debt.remainingAmount

    var adjustMainAccount by remember { mutableStateOf(true) }

    val maxBalancePayable = if (isPay && adjustMainAccount) maxOf(0.0, netBalance) else remaining
    val canPayFullFromBalance = if (isPay && adjustMainAccount) (netBalance >= remaining - 0.001 && netBalance > 0.0) else true
    val maxPossiblePay = minOf(remaining, maxBalancePayable)

    // Automatically default to PARTIAL if account balance is less than full debt
    var paymentMode by remember { mutableStateOf(if (isPay && adjustMainAccount && !canPayFullFromBalance) "PARTIAL" else "FULL") }
    var partialAmountText by remember {
        mutableStateOf(
            if (isPay && adjustMainAccount && !canPayFullFromBalance && netBalance > 0.0) {
                if (maxPossiblePay == maxPossiblePay.toLong().toDouble()) maxPossiblePay.toLong().toString()
                else String.format(Locale.US, "%.2f", maxPossiblePay)
            } else ""
        )
    }
    var noteText by remember { mutableStateOf("") }

    val enteredAmount = partialAmountText.toDoubleOrNull() ?: 0.0
    val isExceedingRemaining = paymentMode == "PARTIAL" && enteredAmount > remaining + 0.0001
    val isExceedingBalance = paymentMode == "PARTIAL" && isPay && adjustMainAccount && enteredAmount > netBalance + 0.0001
    val isExceeding = isExceedingRemaining || isExceedingBalance
    val isZeroOrNegative = paymentMode == "PARTIAL" && enteredAmount <= 0.0
    val isFullThroughPartial = paymentMode == "PARTIAL" && Math.abs(enteredAmount - remaining) < 0.01

    val isConfirmEnabled = when (paymentMode) {
        "FULL" -> remaining > 0.0 && canPayFullFromBalance
        "PARTIAL" -> !isZeroOrNegative && !isExceeding && (!isPay || !adjustMainAccount || (netBalance > 0.0 && enteredAmount <= netBalance + 0.0001))
        else -> false
    }

    val imeInsets = WindowInsets.ime
    val density = LocalDensity.current
    val isKeyboardOpen = imeInsets.getBottom(density) > 0

    val animatedCardElevation by animateDpAsState(
        targetValue = if (isKeyboardOpen) 14.dp else 8.dp,
        animationSpec = tween(durationMillis = 250),
        label = "debt_pay_card_elevation"
    )
    val animatedVerticalPadding by animateDpAsState(
        targetValue = if (isKeyboardOpen) 6.dp else 20.dp,
        animationSpec = tween(durationMillis = 250),
        label = "debt_pay_vertical_padding"
    )
    val animatedContentPadding by animateDpAsState(
        targetValue = if (isKeyboardOpen) 16.dp else 20.dp,
        animationSpec = tween(durationMillis = 250),
        label = "debt_pay_content_padding"
    )
    val animatedSpacing by animateDpAsState(
        targetValue = if (isKeyboardOpen) 10.dp else 14.dp,
        animationSpec = tween(durationMillis = 250),
        label = "debt_pay_spacing"
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 18.dp, vertical = animatedVerticalPadding)
                .clearFocusOnTap(),
            contentAlignment = if (isKeyboardOpen) Alignment.TopCenter else Alignment.Center
        ) {
            val scrollState = rememberScrollState()

            Card(
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = animatedCardElevation),
                border = androidx.compose.foundation.BorderStroke(
                    width = 1.2.dp,
                    brush = Brush.linearGradient(
                        if (isPay) listOf(Color(0xFFEF4444), MaterialTheme.colorScheme.error.copy(alpha = 0.5f))
                        else listOf(Color(0xFF10B981), MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                    )
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 480.dp)
                    .testTag("debt_payment_dialog")
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = if (isKeyboardOpen) 420.dp else 640.dp)
                            .padding(horizontal = animatedContentPadding, vertical = animatedContentPadding)
                            .verticalScroll(scrollState),
                        verticalArrangement = Arrangement.spacedBy(animatedSpacing)
                    ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = accentColor.copy(alpha = 0.15f),
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Payments,
                                    contentDescription = null,
                                    tint = accentColor,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = AppLocale.recordPaymentTitle(isEnglish),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (isEnglish) "Record full payment or installment" else "সম্পূর্ণ পরিশোধ বা কিস্তির হিসাব",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.outline
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))

                // Person Info & Direction Card
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (isPay) Color(0xFFEF4444).copy(alpha = 0.08f) else Color(0xFF10B981).copy(alpha = 0.08f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, accentColor.copy(alpha = 0.25f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = debt.personName,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (isEnglish) {
                                    if (isPay) "Payable • You are returning money" else "Receivable • You are collecting money"
                                } else {
                                    if (isPay) "দেনা পরিশোধ • আপনি টাকা ফেরত দিচ্ছেন" else "পাওনা আদায় • আপনি টাকা গ্রহণ করছেন"
                                },
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                color = accentColor
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = accentColor.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = if (isPay) (if (isEnglish) "PAY" else "দেনা") else (if (isEnglish) "RECEIVE" else "পাওনা"),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = accentColor
                                ),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                // Financial Breakdown (Total, Paid, Remaining) in distinct clean rows
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    border = androidx.compose.foundation.BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Row 1: Total & Already Paid
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = AppLocale.totalDebtAmount(isEnglish),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = formatTakaSafe(debt.amount),
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        if (debt.paidAmount > 0.0) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = AppLocale.alreadyPaid(isEnglish),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = formatTakaSafe(debt.effectivePaidAmount),
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = Color(0xFF047857)
                                )
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))

                        // Row 2: Current Remaining Balance (Highlighted)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = AppLocale.currentRemaining(isEnglish),
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = accentColor.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = formatTakaSafe(remaining),
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        color = accentColor
                                    ),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }
                }

                // Account Balance Status Card (Prominent & Real-time)
                if (isPay && adjustMainAccount) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = when {
                            netBalance <= 0.0 -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)
                            !canPayFullFromBalance -> Color(0xFFF59E0B).copy(alpha = 0.12f)
                            else -> Color(0xFF10B981).copy(alpha = 0.12f)
                        },
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            when {
                                netBalance <= 0.0 -> MaterialTheme.colorScheme.error.copy(alpha = 0.5f)
                                !canPayFullFromBalance -> Color(0xFFD97706).copy(alpha = 0.4f)
                                else -> Color(0xFF059669).copy(alpha = 0.4f)
                            }
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = when {
                                    netBalance <= 0.0 -> Icons.Default.Cancel
                                    !canPayFullFromBalance -> Icons.Default.Warning
                                    else -> Icons.Default.AccountBalanceWallet
                                },
                                contentDescription = null,
                                tint = when {
                                    netBalance <= 0.0 -> MaterialTheme.colorScheme.error
                                    !canPayFullFromBalance -> Color(0xFFB45309)
                                    else -> Color(0xFF047857)
                                },
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = AppLocale.accountBalanceStatus(isEnglish, formatTakaSafe(netBalance)),
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = when {
                                        netBalance <= 0.0 -> MaterialTheme.colorScheme.error
                                        !canPayFullFromBalance -> Color(0xFF92400E)
                                        else -> Color(0xFF047857)
                                    }
                                )
                                Text(
                                    text = when {
                                        netBalance <= 0.0 -> AppLocale.zeroMainBalanceError(isEnglish)
                                        !canPayFullFromBalance -> AppLocale.insufficientMainBalanceForDebt(isEnglish, formatTakaSafe(maxPossiblePay), formatTakaSafe(netBalance))
                                        else -> if (isEnglish) "Sufficient balance available for full repayment."
                                        else "সম্পূর্ণ দেনা পরিশোধের পর্যাপ্ত টাকা অ্যাকাউন্টে রয়েছে।"
                                    },
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Section Label: Payment Mode Selection
                Text(
                    text = if (isEnglish) "Select Payment Option:" else "পরিশোধের বিকল্প বেছে নিন:",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Option 1: সম্পূর্ণ টাকা পরিশোধিত (Full Payment) - Clean Dedicated Row
                val isFullSelected = paymentMode == "FULL"
                Surface(
                    onClick = {
                        if (canPayFullFromBalance) {
                            paymentMode = "FULL"
                        }
                    },
                    shape = RoundedCornerShape(16.dp),
                    color = if (isFullSelected) Color(0xFF10B981).copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface,
                    border = androidx.compose.foundation.BorderStroke(
                        width = if (isFullSelected) 1.8.dp else 0.8.dp,
                        color = if (isFullSelected) Color(0xFF059669) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isFullSelected,
                                onClick = {
                                    if (canPayFullFromBalance) {
                                        paymentMode = "FULL"
                                    }
                                },
                                enabled = canPayFullFromBalance,
                                colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF059669))
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = AppLocale.fullPaymentOption(isEnglish),
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            fontWeight = if (isFullSelected) FontWeight.Bold else FontWeight.SemiBold
                                        ),
                                        color = if (canPayFullFromBalance) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = if (canPayFullFromBalance) Color(0xFF10B981).copy(alpha = 0.15f) else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                                    ) {
                                        Text(
                                            text = formatTakaSafe(remaining),
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = if (canPayFullFromBalance) Color(0xFF047857) else MaterialTheme.colorScheme.error
                                            ),
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = AppLocale.fullPaymentOptionDesc(isEnglish, formatTakaSafe(remaining)),
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Warning when account balance cannot cover full payment
                        if (isPay && adjustMainAccount && !canPayFullFromBalance) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFFF59E0B).copy(alpha = 0.12f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFD97706).copy(alpha = 0.35f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(
                                        text = if (netBalance <= 0.0) {
                                            if (isEnglish) "Account balance is ৳0! Full repayment not possible."
                                            else "⚠️ অ্যাকাউন্টে কোনো ব্যালেন্স নেই (৳০)! সম্পূর্ণ পরিশোধ সম্ভব নয়।"
                                        } else {
                                            if (isEnglish) "Insufficient balance for full repayment (Available: ${formatTakaSafe(netBalance)})"
                                            else "⚠️ সম্পূর্ণ পরিশোধের পর্যাপ্ত ব্যালেন্স নেই (আছে মাত্র ${formatTakaSafe(netBalance)})"
                                        },
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = if (netBalance <= 0.0) MaterialTheme.colorScheme.error else Color(0xFFB45309)
                                        )
                                    )
                                    if (netBalance > 0.0) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        FilledTonalButton(
                                            onClick = {
                                                paymentMode = "PARTIAL"
                                                partialAmountText = maxPossiblePay.toLong().toString()
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                text = if (isEnglish) "Pay max available ${formatTakaSafe(maxPossiblePay)} in installment"
                                                else "👉 সর্বোচ্চ প্রাপ্ত ${formatTakaSafe(maxPossiblePay)} কিস্তিতে পরিশোধ করুন",
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Option 2: কিছু টাকা পরিশোধিত / কিস্তি (Partial Payment) - Clean Dedicated Row
                val isPartialSelected = paymentMode == "PARTIAL"
                Surface(
                    onClick = {
                        paymentMode = "PARTIAL"
                    },
                    shape = RoundedCornerShape(16.dp),
                    color = if (isPartialSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.10f) else MaterialTheme.colorScheme.surface,
                    border = androidx.compose.foundation.BorderStroke(
                        width = if (isPartialSelected) 1.8.dp else 0.8.dp,
                        color = if (isPartialSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isPartialSelected,
                                onClick = { paymentMode = "PARTIAL" }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = AppLocale.partialPaymentOption(isEnglish),
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = if (isPartialSelected) FontWeight.Bold else FontWeight.SemiBold
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = AppLocale.partialPaymentOptionDesc(isEnglish),
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // When Partial is selected, show Amount input & Quick Chips
                        if (isPartialSelected) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))

                            // Amount Input Field
                            OutlinedTextField(
                                value = partialAmountText,
                                onValueChange = { input ->
                                    if (input.all { it.isDigit() || it == '.' }) {
                                        partialAmountText = input
                                    }
                                },
                                label = { Text(AppLocale.enterPaidAmount(isEnglish)) },
                                placeholder = { Text(if (isEnglish) "e.g. 5000" else "যেমন: ৫০০০") },
                                leadingIcon = {
                                    Text(
                                        text = "৳",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(start = 12.dp, end = 4.dp)
                                    )
                                },
                                trailingIcon = {
                                    if (partialAmountText.isNotEmpty()) {
                                        IconButton(onClick = { partialAmountText = "" }) {
                                            Icon(Icons.Default.Clear, contentDescription = "Clear", modifier = Modifier.size(18.dp))
                                        }
                                    }
                                },
                                isError = isExceeding,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("input_partial_amount")
                            )

                            // Quick Helper Chips (including max payable from balance)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                if (isPay && adjustMainAccount && netBalance < remaining && netBalance > 0.0) {
                                    SuggestionChip(
                                        onClick = {
                                            partialAmountText = maxPossiblePay.toLong().toString()
                                        },
                                        label = {
                                            Text(
                                                text = AppLocale.maxPayableChipLabel(isEnglish, formatTakaSafe(maxPossiblePay)),
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                                                color = Color(0xFFB45309)
                                            )
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        colors = SuggestionChipDefaults.suggestionChipColors(containerColor = Color(0xFFF59E0B).copy(alpha = 0.15f)),
                                        border = SuggestionChipDefaults.suggestionChipBorder(borderColor = Color(0xFFD97706).copy(alpha = 0.4f), enabled = true),
                                        modifier = Modifier.weight(1.2f)
                                    )
                                }

                                val fractions = listOf(0.25 to "25%", 0.50 to "50%", 1.0 to (if (isEnglish) "All" else "সব"))
                                fractions.forEach { (fraction, label) ->
                                    val calcVal = (remaining * fraction).toLong()
                                    SuggestionChip(
                                        onClick = {
                                            partialAmountText = calcVal.toString()
                                        },
                                        label = {
                                            Text(
                                                text = "$label (${formatTakaSafe(calcVal.toDouble())})",
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp)
                                            )
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }

                            // Dynamic Validation & Preview Message Boxes
                            if (isExceeding) {
                                val errText = if (isExceedingBalance) {
                                    AppLocale.partialPayExceedsBalance(isEnglish, formatTakaSafe(netBalance), formatTakaSafe(maxPossiblePay))
                                } else {
                                    AppLocale.amountExceedsError(isEnglish, formatTakaSafe(remaining))
                                }
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Warning,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = errText,
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                color = MaterialTheme.colorScheme.error,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        )
                                    }
                                }
                            } else if (isFullThroughPartial) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFF10B981).copy(alpha = 0.12f),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF059669).copy(alpha = 0.35f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = Color(0xFF047857),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (isEnglish) "This amount covers the full remaining debt and will close it."
                                            else "✓ এটি সম্পূর্ণ অবশিষ্ট টাকা পরিশোধ করবে এবং হিসাবটি সম্পূর্ণ পরিশোধিত হিসেবে ক্লোজ হবে।",
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                color = Color(0xFF047857),
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        )
                                    }
                                }
                            } else if (enteredAmount > 0.0) {
                                val remAfter = maxOf(0.0, remaining - enteredAmount)
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Info,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = AppLocale.remainingAfterPayment(isEnglish, formatTakaSafe(remAfter)),
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                color = MaterialTheme.colorScheme.primary,
                                                fontWeight = FontWeight.Bold
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Optional Note Field (Separate clean row)
                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    label = { Text(AppLocale.installmentNoteHint(isEnglish)) },
                    placeholder = { Text(if (isEnglish) "e.g. 1st installment, via Cash/bKash" else "যেমন: ১ম কিস্তি, ক্যাশ / বিকাশ মারফত") },
                    leadingIcon = {
                        Icon(Icons.Default.EditNote, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                // Cash Balance Adjustment Switch (Separate clean row)
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    border = androidx.compose.foundation.BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isEnglish) "Cash Ledger Adjustment" else "মূল ক্যাশ ব্যালেন্স সমন্বয়",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (adjustMainAccount) {
                                    if (isPay) {
                                        if (isEnglish) "Deduct paid money from cash balance as expense" else "টাকা মূল ব্যালেন্স থেকে খরচ হিসেবে কাটা হবে ও হোম পেজে দেখাবে"
                                    } else {
                                        if (isEnglish) "Add collected money to cash balance as income" else "টাকা মূল ব্যালেন্সে আয় হিসেবে যোগ হবে ও হোম পেজে দেখাবে"
                                    }
                                } else {
                                    if (isEnglish) "Do not affect cash balance (Record in debt ledger only)" else "ব্যালেন্সে কোনো প্রভাব পড়বে না (শুধুমাত্র ধার খাতায় রেকর্ড থাকবে)"
                                },
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Switch(
                            checked = adjustMainAccount,
                            onCheckedChange = { adjustMainAccount = it }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Action Buttons (Separate Rows for maximum touch target and clarity)
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val payAmount = if (paymentMode == "FULL") remaining else enteredAmount
                    val btnLabel = when {
                        isPay && adjustMainAccount && netBalance <= 0.0 -> if (isEnglish) "Insufficient Balance (৳0)" else "পরিশোধ সম্ভব নয় (ব্যালেন্স ৳০)"
                        paymentMode == "FULL" && isPay && adjustMainAccount && !canPayFullFromBalance -> if (isEnglish) "Insufficient Balance" else "ব্যালেন্স অপর্যাপ্ত (আছে ${formatTakaSafe(netBalance)})"
                        paymentMode == "FULL" -> if (isEnglish) "Confirm Full Payment (${formatTakaSafe(remaining)})" else "সম্পূর্ণ পরিশোধ নিশ্চিত করুন (${formatTakaSafe(remaining)})"
                        enteredAmount > 0.0 && !isExceeding -> if (isEnglish) "Confirm Installment (${formatTakaSafe(enteredAmount)})" else "কিস্তি জমা নিশ্চিত করুন (${formatTakaSafe(enteredAmount)})"
                        else -> AppLocale.confirmPayment(isEnglish)
                    }

                    Button(
                        onClick = {
                            val isFull = (paymentMode == "FULL") || isFullThroughPartial
                            onConfirm(payAmount, isFull, adjustMainAccount, noteText.trim())
                        },
                        enabled = isConfirmEnabled,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isPay) Color(0xFFDC2626) else Color(0xFF059669)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("btn_confirm_debt_payment")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = btnLabel,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Text(
                            text = AppLocale.cancel(isEnglish),
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                    }
                }
            }

            KeyboardScrollDownHint(
                scrollState = scrollState,
                isKeyboardOpen = isKeyboardOpen,
                forceShowWhenScrollable = true,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 6.dp),
                label = if (isEnglish) "Scroll down" else "নিচে স্ক্রল করুন"
            )
        }
    }
}
}
}

@Composable
fun DebtCardItem(
    item: DebtEntity,
    netBalance: Double = 0.0,
    isEnglish: Boolean = false,
    onRecordPayment: () -> Unit,
    onUnsettle: () -> Unit,
    onShowReceipt: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isReceive = item.type == "RECEIVE"
    val accentColor = if (isReceive) Color(0xFF059669) else Color(0xFFDC2626)
    val accentBg = if (isReceive) Color(0xFF10B981).copy(alpha = 0.12f) else Color(0xFFEF4444).copy(alpha = 0.12f)
    var showHistory by remember { mutableStateOf(false) }
    val paymentHistory = remember(item.paymentHistoryJson) { item.parsePaymentHistory() }

    val canPayFullFromBalance = netBalance >= item.remainingAmount - 0.001 && netBalance > 0.0
    val maxPayableFromBalance = minOf(item.remainingAmount, maxOf(0.0, netBalance))
    val balanceDeficit = maxOf(0.0, item.remainingAmount - maxPayableFromBalance)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("debt_item_${item.id}"),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (item.isSettled) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            else MaterialTheme.colorScheme.surface
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = if (item.isSettled) 1.dp else 1.2.dp,
            brush = if (item.isSettled) {
                Brush.linearGradient(
                    listOf(
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
                    )
                )
            } else {
                Brush.linearGradient(
                    listOf(
                        accentColor.copy(alpha = 0.55f),
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f),
                        accentColor.copy(alpha = 0.2f)
                    )
                )
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (item.isSettled) 0.dp else 2.5.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Row 1: Avatar badge + Person Details + Direction + Source + Delete
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Stylish Avatar with initial letter
                val initialLetter = item.personName.trim().take(1).uppercase()
                Surface(
                    shape = CircleShape,
                    color = if (item.isSettled) MaterialTheme.colorScheme.surfaceVariant else accentBg,
                    border = androidx.compose.foundation.BorderStroke(
                        width = 1.2.dp,
                        color = if (item.isSettled) MaterialTheme.colorScheme.outline.copy(alpha = 0.4f) else accentColor.copy(alpha = 0.4f)
                    ),
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = if (initialLetter.isNotEmpty()) initialLetter else "?",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Black,
                                color = if (item.isSettled) MaterialTheme.colorScheme.onSurfaceVariant else accentColor
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Person Name & Direction Badge
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = item.personName,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                textDecoration = if (item.isSettled) androidx.compose.ui.text.style.TextDecoration.LineThrough else null
                            ),
                            color = if (item.isSettled) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface,
                            softWrap = true,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (item.isSettled) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFF10B981).copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = if (isEnglish) "Settled" else "পরিশোধিত",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF047857)
                                    ),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(3.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (isReceive) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                            contentDescription = null,
                            tint = if (item.isSettled) MaterialTheme.colorScheme.onSurfaceVariant else accentColor,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = if (isEnglish) (if (isReceive) "Receivable (Others owe you)" else "Payable (You owe)")
                            else (if (isReceive) "পাব (অন্যরা দেবে)" else "দেব (ফেরত দিতে হবে)"),
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = if (item.isSettled) MaterialTheme.colorScheme.onSurfaceVariant else accentColor,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }

                    // Source Indicator Badge (Khat or Main Balance)
                    if (item.khatName.isNotBlank()) {
                        val isOboshisto = item.khatName.contains("অবশিষ্ট") || item.khatName.contains("Oboshisto") || item.khatName.contains("Remaining")
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 2.dp)) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (isOboshisto) Color(0xFF10B981).copy(alpha = 0.15f) else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                border = androidx.compose.foundation.BorderStroke(0.6.dp, (if (isOboshisto) Color(0xFF10B981) else MaterialTheme.colorScheme.primary).copy(alpha = 0.35f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (isOboshisto) Icons.Default.Savings else Icons.Default.Folder,
                                        contentDescription = null,
                                        tint = if (isOboshisto) Color(0xFF047857) else MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(10.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = AppLocale.sourceKhatBadge(isEnglish, AppLocale.khatName(item.khatName, isEnglish)),
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isOboshisto) Color(0xFF047857) else MaterialTheme.colorScheme.primary
                                        )
                                    )
                                }
                            }
                        }
                    } else if (item.deductedFromMain) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 2.dp)) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                                border = androidx.compose.foundation.BorderStroke(0.6.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AccountBalanceWallet,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(10.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = AppLocale.sourceMainBadge(isEnglish),
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Receipt Voucher Button
                IconButton(
                    onClick = onShowReceipt,
                    modifier = Modifier
                        .size(32.dp)
                        .testTag("receipt_debt_${item.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.ReceiptLong,
                        contentDescription = if (isEnglish) "Debt Receipt Voucher" else "ধার রসিদ ও ভাউচার",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(19.dp)
                    )
                }

                Spacer(modifier = Modifier.width(2.dp))

                // Delete Button
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .size(32.dp)
                        .testTag("delete_debt_${item.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.7f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Financial Summary Section on Distinct Separate Rows
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                border = androidx.compose.foundation.BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Row 1: Total Debt Amount
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isEnglish) "Total Debt:" else "মোট ধারের পরিমাণ:",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = formatTakaSafe(item.amount),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                textDecoration = if (item.isSettled) androidx.compose.ui.text.style.TextDecoration.LineThrough else null
                            )
                        )
                    }

                    // Row 2: Paid and Remaining status
                    if (!item.isSettled) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Paid chip
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFF10B981).copy(alpha = 0.12f),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Text(
                                        text = if (isEnglish) "Paid So Far" else "পরিশোধ হয়েছে",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                        color = Color(0xFF047857)
                                    )
                                    Text(
                                        text = formatTakaSafe(item.effectivePaidAmount),
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF047857)
                                        )
                                    )
                                }
                            }

                            // Remaining chip
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = accentColor.copy(alpha = 0.12f),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Text(
                                        text = if (isEnglish) "Current Due" else "অবশিষ্ট বাকি",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                        color = accentColor
                                    )
                                    Text(
                                        text = formatTakaSafe(item.remainingAmount),
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.ExtraBold,
                                            color = accentColor
                                        )
                                    )
                                }
                            }
                        }

                        // Progress Bar if partial payment has occurred
                        if (item.paidAmount > 0.0) {
                            val progress = (item.effectivePaidAmount / item.amount).toFloat().coerceIn(0f, 1f)
                            val percent = (progress * 100).toInt()
                            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = if (isEnglish) "$percent% paid" else "$percent% পরিশোধ সম্পন্ন",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp, fontWeight = FontWeight.Bold),
                                        color = Color(0xFF047857)
                                    )
                                    Text(
                                        text = if (isEnglish) "Remaining: ${formatTakaSafe(item.remainingAmount)}" else "বাকি: ${formatTakaSafe(item.remainingAmount)}",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp, fontWeight = FontWeight.Bold),
                                        color = accentColor
                                    )
                                }
                                LinearProgressIndicator(
                                    progress = { progress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(5.dp)
                                        .clip(RoundedCornerShape(3.dp)),
                                    color = if (isReceive) Color(0xFF059669) else Color(0xFFDC2626),
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            }
                        }

                        // Account Balance & Repayment Capability Status (strictly synchronized with main account)
                        if (!isReceive) {
                            // PAY Debt: Repayment leaves user's account
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = when {
                                    netBalance <= 0.0 -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.45f)
                                    !canPayFullFromBalance -> Color(0xFFF59E0B).copy(alpha = 0.12f)
                                    else -> Color(0xFF10B981).copy(alpha = 0.12f)
                                },
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    when {
                                        netBalance <= 0.0 -> MaterialTheme.colorScheme.error.copy(alpha = 0.5f)
                                        !canPayFullFromBalance -> Color(0xFFD97706).copy(alpha = 0.4f)
                                        else -> Color(0xFF059669).copy(alpha = 0.35f)
                                    }
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = when {
                                            netBalance <= 0.0 -> Icons.Default.Cancel
                                            !canPayFullFromBalance -> Icons.Default.Warning
                                            else -> Icons.Default.CheckCircle
                                        },
                                        contentDescription = null,
                                        tint = when {
                                            netBalance <= 0.0 -> MaterialTheme.colorScheme.error
                                            !canPayFullFromBalance -> Color(0xFFB45309)
                                            else -> Color(0xFF047857)
                                        },
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = when {
                                                netBalance <= 0.0 -> AppLocale.debtCardZeroBalance(isEnglish)
                                                !canPayFullFromBalance -> AppLocale.debtCardInsufficientBalance(
                                                    isEnglish,
                                                    formatTakaSafe(maxPayableFromBalance),
                                                    formatTakaSafe(balanceDeficit),
                                                    formatTakaSafe(netBalance)
                                                )
                                                else -> AppLocale.debtCardReadyForRepay(isEnglish, formatTakaSafe(netBalance))
                                            },
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp,
                                                color = when {
                                                    netBalance <= 0.0 -> MaterialTheme.colorScheme.error
                                                    !canPayFullFromBalance -> Color(0xFF92400E)
                                                    else -> Color(0xFF047857)
                                                }
                                            )
                                        )
                                        if (netBalance > 0.0 && !canPayFullFromBalance) {
                                            Text(
                                                text = if (isEnglish) "Account has only ${formatTakaSafe(netBalance)}. Balance cannot be negative!"
                                                else "অ্যাকাউন্টে আছে মাত্র ${formatTakaSafe(netBalance)}। মূল ব্যালেন্স মাইনাস হতে পারবে না!",
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        } else {
                            // RECEIVE Debt: Money collected enters user's account
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                border = androidx.compose.foundation.BorderStroke(0.6.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Savings,
                                        contentDescription = null,
                                        tint = Color(0xFF059669),
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = AppLocale.debtReceiveAccountSyncHint(isEnglish),
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    } else {
                        // Fully Settled banner
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF10B981).copy(alpha = 0.15f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF047857),
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isEnglish) "Fully Settled ✓ (${formatTakaSafe(item.amount)})" else "সম্পূর্ণ পরিশোধ সম্পন্ন হয়েছে ✓ (${formatTakaSafe(item.amount)})",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF047857)
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // Row 3: Due Date & Note (if present)
            if (item.dueDate.isNotBlank() || item.note.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.38f),
                    border = androidx.compose.foundation.BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (item.dueDate.isNotBlank()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                    modifier = Modifier.size(20.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.CalendarMonth,
                                            contentDescription = null,
                                            modifier = Modifier.size(12.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isEnglish) "Expected Due Date: ${item.dueDate}" else "ফেরতের সম্ভাব্য তারিখ: ${item.dueDate}",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    softWrap = true
                                )
                            }
                        }
                        if (item.note.isNotBlank()) {
                            Row(verticalAlignment = Alignment.Top) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f),
                                    modifier = Modifier.size(20.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Notes,
                                            contentDescription = null,
                                            modifier = Modifier.size(12.dp),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = item.note,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    softWrap = true
                                )
                            }
                        }
                    }
                }
            }

            // Row 4: Installment History List (if any payments recorded)
            if (paymentHistory.isNotEmpty()) {
                Surface(
                    onClick = { showHistory = !showHistory },
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = androidx.compose.foundation.BorderStroke(0.6.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.History,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Text(
                                        text = if (isEnglish) "Installment History (${paymentHistory.size} payments)"
                                        else "কিস্তির ইতিহাস (${paymentHistory.size} টি জমা)",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    if (!isReceive && !item.isSettled) {
                                        Text(
                                            text = AppLocale.debtMaxRepayableCapability(isEnglish, formatTakaSafe(maxPayableFromBalance)),
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontSize = 9.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (netBalance <= 0.0) MaterialTheme.colorScheme.error else Color(0xFFB45309)
                                            )
                                        )
                                    }
                                }
                            }
                            Icon(
                                imageVector = if (showHistory) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (showHistory) {
                            Spacer(modifier = Modifier.height(8.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
                            Spacer(modifier = Modifier.height(6.dp))

                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                paymentHistory.forEachIndexed { idx, pRec ->
                                    val dateStr = try {
                                        val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
                                        sdf.format(pRec.timestamp)
                                    } catch (_: Exception) {
                                        ""
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = "${idx + 1}. ${pRec.note.ifBlank { if (isEnglish) "Installment" else "কিস্তি জমা" }}",
                                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            if (dateStr.isNotBlank()) {
                                                Text(
                                                    text = dateStr,
                                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(
                                                text = "+${formatTakaSafe(pRec.amount)}",
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF047857)
                                                )
                                            )
                                            Text(
                                                text = "${if (isEnglish) "Due: " else "বাকি: "}${formatTakaSafe(pRec.remainingAfter)}",
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Row 5: Action Bottom Button (Replaces the old "পরিশোধিত হিসেবে চিহ্নিত করুন")
            if (item.isSettled) {
                // Settled state: Clean status with reopen button
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFF10B981).copy(alpha = 0.12f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = Color(0xFF047857)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isEnglish) "Fully Settled ✓" else "পরিশোধ সম্পন্ন হয়েছে ✓",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF047857)
                                )
                            )
                        }

                        TextButton(
                            onClick = onUnsettle,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (isEnglish) "Reopen" else "পুনরায় চালু",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            } else {
                // Active state: Prominent and beautiful "টাকা পরিশোধ বা কিস্তি জমা দিন" Button
                val isPayNoBalance = !isReceive && netBalance <= 0.0
                val isPayPartialBalance = !isReceive && !canPayFullFromBalance && netBalance > 0.0

                Surface(
                    onClick = onRecordPayment,
                    shape = RoundedCornerShape(14.dp),
                    color = if (isReceive) Color(0xFF059669)
                            else if (isPayNoBalance) Color(0xFFEF4444)
                            else Color(0xFFDC2626),
                    shadowElevation = 1.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("debt_pay_action_${item.id}")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = if (isPayNoBalance) Icons.Default.Cancel else Icons.Default.Payments,
                            contentDescription = null,
                            modifier = Modifier.size(19.dp),
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = if (isPayNoBalance) {
                                    if (isEnglish) "Repayment Blocked (Account ৳0)" else "পরিশোধ বন্ধ (অ্যাকাউন্টে টাকা নেই)"
                                } else if (isPayPartialBalance) {
                                    if (isEnglish) "Pay Installment (Max ${formatTakaSafe(maxPayableFromBalance)})" else "কিস্তি পরিশোধ (সর্বোচ্চ ${formatTakaSafe(maxPayableFromBalance)})"
                                } else {
                                    AppLocale.recordPaymentButton(isEnglish)
                                },
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            )
                            Text(
                                text = if (isPayNoBalance) {
                                    if (isEnglish) "Main balance cannot be negative • Add money first" else "ব্যালেন্স মাইনাস হতে পারবে না • অ্যাকাউন্টে টাকা যোগ করুন"
                                } else if (isPayPartialBalance) {
                                    if (isEnglish) "Account has ${formatTakaSafe(netBalance)} • Tap to pay installment" else "অ্যাকাউন্টে আছে মাত্র ${formatTakaSafe(netBalance)} • সর্বোচ্চ এই পরিমাণ দেওয়া যাবে"
                                } else {
                                    if (isEnglish) "Choose Full Payment or Partial Installment" else "সম্পূর্ণ বা কিস্তিতে পরিশোধ নির্বাচন করুন"
                                },
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 10.sp,
                                    color = Color.White.copy(alpha = 0.85f)
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

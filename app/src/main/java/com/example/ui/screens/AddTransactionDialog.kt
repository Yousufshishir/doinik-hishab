package com.example.ui.screens

import com.example.ui.util.AppLocale
import com.example.ui.util.clearFocusOnTap

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.KhatEntity
import com.example.data.model.SimpleCategoryData
import com.example.data.model.formatTakaSafe
import com.example.ui.components.getKhatIcon
import com.example.ui.components.AppCardDefaults
import com.example.ui.components.AppToastManager
import com.example.ui.components.KeyboardScrollDownHint
import kotlinx.coroutines.launch

import android.app.DatePickerDialog
import androidx.compose.ui.platform.LocalContext
import java.util.Calendar
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(
    androidx.compose.foundation.ExperimentalFoundationApi::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class
)
@Composable
fun AddTransactionDialog(
    isEnglish: Boolean = false,
    khats: List<KhatEntity> = emptyList(),
    oboshistoBalance: Double = 0.0,
    netBalance: Double = 0.0,
    onDismiss: () -> Unit,
    onSave: (title: String, amount: Double, type: String, category: String, note: String, khatId: Long?, khatName: String, timestamp: Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var transactionType by remember { mutableStateOf("EXPENSE") } // "EXPENSE" or "INCOME"
    var amountText by remember { mutableStateOf("") }
    var titleText by remember { mutableStateOf("") }
    var noteText by remember { mutableStateOf("") }
    var selectedKhatId by remember { mutableStateOf<Long?>(null) }
    var selectedKhatName by remember { mutableStateOf("") }
    var selectedCustomTimestamp by remember { mutableStateOf<Long?>(null) }
    var showOverBalanceWarning by remember { mutableStateOf(false) }

    val selectedKhat = khats.find { it.id == selectedKhatId }
    val isExpense = transactionType == "EXPENSE"
    val maxSpendable = remember(isExpense, selectedKhat, oboshistoBalance, netBalance, khats.size) {
        if (!isExpense) {
            Double.MAX_VALUE
        } else if (selectedKhat != null) {
            minOf(maxOf(0.0, selectedKhat.allocatedAmount), maxOf(0.0, netBalance))
        } else {
            if (khats.isNotEmpty()) {
                minOf(maxOf(0.0, oboshistoBalance), maxOf(0.0, netBalance))
            } else {
                maxOf(0.0, netBalance)
            }
        }
    }
    val isSourceBalanceZero = isExpense && maxSpendable <= 0.0

    val currentCategories = if (transactionType == "EXPENSE") {
        SimpleCategoryData.expenseCategories
    } else {
        SimpleCategoryData.incomeCategories
    }

    var selectedCategoryName by remember(transactionType) {
        mutableStateOf(currentCategories.first().name)
    }

    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    fun addToAmount(addValue: Double) {
        val current = amountText.toDoubleOrNull() ?: 0.0
        val sum = current + addValue
        if (isExpense) {
            if (isSourceBalanceZero) {
                amountText = ""
                keyboardController?.hide()
                focusManager.clearFocus()
                showOverBalanceWarning = true
                return
            }
            if (sum > maxSpendable) {
                amountText = if (maxSpendable % 1.0 == 0.0) maxSpendable.toLong().toString() else String.format(Locale.US, "%.2f", maxSpendable)
                keyboardController?.hide()
                focusManager.clearFocus()
                showOverBalanceWarning = true
            } else {
                amountText = if (sum % 1.0 == 0.0) sum.toLong().toString() else String.format(Locale.US, "%.2f", sum)
                showOverBalanceWarning = false
            }
        } else {
            amountText = if (sum % 1.0 == 0.0) sum.toLong().toString() else String.format(Locale.US, "%.2f", sum)
            showOverBalanceWarning = false
        }
    }

    // Auto-adjust if source changed and amount exceeds new limit
    LaunchedEffect(selectedKhatId, maxSpendable, transactionType) {
        if (isExpense && amountText.isNotBlank()) {
            val entered = amountText.toDoubleOrNull() ?: 0.0
            if (isSourceBalanceZero) {
                amountText = ""
                showOverBalanceWarning = true
            } else if (entered > maxSpendable) {
                amountText = if (maxSpendable % 1.0 == 0.0) maxSpendable.toLong().toString() else String.format(Locale.US, "%.2f", maxSpendable)
                showOverBalanceWarning = true
            }
        }
    }

    val imeInsets = WindowInsets.ime
    val density = LocalDensity.current
    val isKeyboardOpen = imeInsets.getBottom(density) > 0

    val animatedCardElevation by animateDpAsState(
        targetValue = if (isKeyboardOpen) 12.dp else 6.dp,
        animationSpec = tween(durationMillis = 250),
        label = "trans_card_elevation"
    )
    val animatedVerticalPadding by animateDpAsState(
        targetValue = if (isKeyboardOpen) 6.dp else 20.dp,
        animationSpec = tween(durationMillis = 250),
        label = "trans_vertical_padding"
    )
    val animatedContentPadding by animateDpAsState(
        targetValue = if (isKeyboardOpen) 16.dp else 24.dp,
        animationSpec = tween(durationMillis = 250),
        label = "trans_content_padding"
    )
    val animatedSpacing by animateDpAsState(
        targetValue = if (isKeyboardOpen) 10.dp else 16.dp,
        animationSpec = tween(durationMillis = 250),
        label = "trans_spacing"
    )

    Dialog(
        onDismissRequest = {
            focusManager.clearFocus()
            keyboardController?.hide()
            onDismiss()
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
                .padding(horizontal = 20.dp, vertical = animatedVerticalPadding)
                .clearFocusOnTap(),
            contentAlignment = if (isKeyboardOpen) Alignment.TopCenter else Alignment.Center
        ) {
            val scrollState = rememberScrollState()

            Card(
                modifier = modifier
                    .fillMaxWidth()
                    .widthIn(max = 450.dp)
                    .testTag("add_transaction_dialog"),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                border = if (transactionType == "EXPENSE") AppCardDefaults.expenseBorder() else AppCardDefaults.incomeBorder(),
                elevation = CardDefaults.cardElevation(defaultElevation = animatedCardElevation)
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = animatedContentPadding, vertical = animatedContentPadding)
                            .verticalScroll(scrollState),
                        verticalArrangement = Arrangement.spacedBy(animatedSpacing)
                    ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isEnglish) "Add Transaction" else "নতুন লেনদেন যোগ করুন",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        IconButton(
                            onClick = {
                                focusManager.clearFocus()
                                keyboardController?.hide()
                                onDismiss()
                            },
                            modifier = Modifier.testTag("dialog_close_button")
                        ) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                        }
                    }
                // Type Segmented Toggle (Expense vs Income)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(4.dp)
                ) {
                    val isExpense = transactionType == "EXPENSE"
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isExpense) Color(0xFFD32F2F) else Color.Transparent)
                            .clickable {
                                transactionType = "EXPENSE"
                                val entered = amountText.toDoubleOrNull() ?: 0.0
                                if (isSourceBalanceZero) {
                                    amountText = ""
                                    showOverBalanceWarning = true
                                    keyboardController?.hide()
                                    focusManager.clearFocus()
                                } else if (entered > maxSpendable) {
                                    amountText = if (maxSpendable % 1.0 == 0.0) maxSpendable.toLong().toString() else String.format(Locale.US, "%.2f", maxSpendable)
                                    showOverBalanceWarning = true
                                    keyboardController?.hide()
                                    focusManager.clearFocus()
                                }
                            }
                            .padding(vertical = 10.dp)
                            .testTag("toggle_expense"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isEnglish) "🔻 Expense" else "🔻 খরচ",
                            fontWeight = FontWeight.Bold,
                            color = if (isExpense) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (!isExpense) Color(0xFF2E7D32) else Color.Transparent)
                            .clickable {
                                transactionType = "INCOME"
                                showOverBalanceWarning = false
                            }
                            .padding(vertical = 10.dp)
                            .testTag("toggle_income"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isEnglish) "🟢 Income" else "🟢 আয়",
                            fontWeight = FontWeight.Bold,
                            color = if (!isExpense) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Amount Input Field
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { input ->
                            val filtered = input.filter { it.isDigit() || it == '.' }
                            if (filtered.count { it == '.' } <= 1) {
                                if (isExpense) {
                                    if (isSourceBalanceZero) {
                                        amountText = ""
                                        keyboardController?.hide()
                                        focusManager.clearFocus()
                                        showOverBalanceWarning = true
                                        AppToastManager.show(
                                            if (isEnglish) "Insufficient balance (৳0)! Add income first."
                                            else "অ্যাকাউন্টে পর্যাপ্ত ব্যালেন্স নেই (ব্যালেন্স ৳০)! খরচ করতে প্রথমে অ্যাকাউন্টে টাকা যোগ করুন।"
                                        )
                                    } else {
                                        val entered = filtered.toDoubleOrNull() ?: 0.0
                                        if (entered > maxSpendable) {
                                            amountText = if (maxSpendable % 1.0 == 0.0) maxSpendable.toLong().toString() else String.format(Locale.US, "%.2f", maxSpendable)
                                            keyboardController?.hide()
                                            focusManager.clearFocus()
                                            showOverBalanceWarning = true
                                            AppToastManager.show(
                                                if (isEnglish) "Cannot spend more than available balance (${formatTakaSafe(maxSpendable)})"
                                                else "ব্যালেন্সের অতিরিক্ত খরচ সম্ভব নয়! সর্বোচ্চ ${formatTakaSafe(maxSpendable)} নির্ধারণ করা হয়েছে।"
                                            )
                                        } else {
                                            amountText = filtered
                                            showOverBalanceWarning = false
                                        }
                                    }
                                } else {
                                    amountText = filtered
                                    showOverBalanceWarning = false
                                }
                            }
                        },
                        label = { Text(if (isEnglish) "Amount" else "টাকার পরিমাণ") },
                        placeholder = { Text("0.00") },
                        leadingIcon = {
                            Text(
                                text = "৳",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSourceBalanceZero) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                ),
                                modifier = Modifier.padding(start = 12.dp)
                            )
                        },
                        trailingIcon = {
                            if (amountText.isNotBlank()) {
                                IconButton(onClick = {
                                    amountText = ""
                                    showOverBalanceWarning = false
                                }) {
                                    Icon(imageVector = Icons.Default.Close, contentDescription = "Clear")
                                }
                            }
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Decimal,
                            imeAction = androidx.compose.ui.text.input.ImeAction.Next
                        ),
                        shape = RoundedCornerShape(14.dp),
                        singleLine = true,
                        isError = isSourceBalanceZero || showOverBalanceWarning,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("transaction_amount_input")
                    )

                    // Balance Limit & Warnings
                    if (isExpense) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSourceBalanceZero) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f)
                            else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                            border = androidx.compose.foundation.BorderStroke(
                                0.8.dp,
                                if (isSourceBalanceZero) MaterialTheme.colorScheme.error.copy(alpha = 0.5f)
                                else MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                    Icon(
                                        imageVector = Icons.Default.Shield,
                                        contentDescription = null,
                                        tint = if (isSourceBalanceZero) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = if (isEnglish) "Available to spend (Max):" else "সর্বোচ্চ খরচযোগ্য ব্যালেন্স:",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Medium),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    text = formatTakaSafe(maxSpendable),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 11.5.sp,
                                        color = if (isSourceBalanceZero) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                    )
                                )
                            }
                        }

                        if (isSourceBalanceZero) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.6f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
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
                                            "⚠️ Balance is ৳0! Add income first to record expenses."
                                        else
                                            "⚠️ অ্যাকাউন্টে পর্যাপ্ত ব্যালেন্স নেই (ব্যালেন্স ৳০)! খরচ যোগ করতে প্রথমে অ্যাকাউন্টে টাকা যোগ করুন।",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = MaterialTheme.colorScheme.error,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.5.sp
                                        )
                                    )
                                }
                            }
                        } else if (showOverBalanceWarning) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFEF4444).copy(alpha = 0.12f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.45f)),
                                modifier = Modifier.fillMaxWidth()
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
                                            "⚠️ Cannot spend more than available balance (${formatTakaSafe(maxSpendable)})."
                                        else
                                            "⚠️ ব্যালেন্সের চেয়ে অতিরিক্ত খরচ সম্ভব নয়! সর্বোচ্চ খরচযোগ্য ${formatTakaSafe(maxSpendable)} নির্ধারণ করা হয়েছে।",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = Color(0xFFEF4444),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.5.sp
                                        )
                                    )
                                }
                            }
                        }
                    }

                    // Quick Math Add Buttons (+10, +50, +100, +500, +1000)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(10.0, 50.0, 100.0, 500.0, 1000.0).forEach { addVal ->
                            val currentAmt = amountText.toDoubleOrNull() ?: 0.0
                            val wouldExceed = isExpense && (isSourceBalanceZero || currentAmt + addVal > maxSpendable)
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (wouldExceed) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { addToAmount(addVal) }
                            ) {
                                Box(
                                    modifier = Modifier.padding(vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "+%.0f".format(addVal),
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = if (wouldExceed) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                            else MaterialTheme.colorScheme.onSurface
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                // Category Selection
                Column {
                    Text(
                        text = if (isEnglish) "Category:" else "ক্যাটাগরি:",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        currentCategories.forEach { cat ->
                            val isSelected = selectedCategoryName == cat.name
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { selectedCategoryName = cat.name }
                                    .testTag("category_${cat.id}")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = cat.icon,
                                        contentDescription = cat.name,
                                        tint = if (isSelected) MaterialTheme.colorScheme.primary else cat.color,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = com.example.ui.util.AppLocale.category(cat.name, isEnglish),
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                // Source Selection: "খরচ করবেন কোথা থেকে? (উৎস)"
                val currentEnteredAmount = amountText.toDoubleOrNull() ?: 0.0
                val selectedKhat = khats.find { it.id == selectedKhatId }
                val isInsufficientKhatBalance = transactionType == "EXPENSE" && selectedKhat != null && currentEnteredAmount > selectedKhat.allocatedAmount

                if (transactionType == "EXPENSE") {
                    var sourceDropdownExpanded by remember { mutableStateOf(false) }
                    val selectedSourceText = if (selectedKhat != null) {
                        "${AppLocale.khatName(selectedKhat.name, isEnglish)} (${formatTakaSafe(selectedKhat.allocatedAmount)})"
                    } else {
                        if (isEnglish) "Oboshisto (${formatTakaSafe(oboshistoBalance)})" else "অবশিষ্ট (${formatTakaSafe(oboshistoBalance)})"
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = if (isEnglish) "Spend from (Source):" else "খরচ করবেন কোথা থেকে? (উৎস):",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Box(modifier = Modifier.fillMaxWidth()) {
                            Surface(
                                onClick = { sourceDropdownExpanded = true },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("dropdown_transaction_source"),
                                shape = RoundedCornerShape(12.dp),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isInsufficientKhatBalance) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outlineVariant
                                ),
                                color = MaterialTheme.colorScheme.surface
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = if (selectedKhat != null) getKhatIcon(selectedKhat.iconName, selectedKhat.name) else Icons.Default.Savings,
                                            contentDescription = null,
                                            tint = if (selectedKhat != null) {
                                                try {
                                                    Color(android.graphics.Color.parseColor(selectedKhat.colorHex))
                                                } catch (_: Exception) {
                                                    MaterialTheme.colorScheme.primary
                                                }
                                            } else Color(0xFF059669),
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = selectedSourceText,
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            DropdownMenu(
                                expanded = sourceDropdownExpanded,
                                onDismissRequest = { sourceDropdownExpanded = false },
                                modifier = Modifier.fillMaxWidth(0.85f)
                            ) {
                                // 1. Oboshisto (Default)
                                DropdownMenuItem(
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Savings,
                                            contentDescription = null,
                                            tint = Color(0xFF059669),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    },
                                    text = {
                                        Column {
                                            Text(
                                                text = if (isEnglish) "Oboshisto (Remaining)" else "অবশিষ্ট (ডিফল্ট)",
                                                fontWeight = if (selectedKhatId == null) FontWeight.Bold else FontWeight.Normal
                                            )
                                            Text(
                                                text = formatTakaSafe(oboshistoBalance),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color(0xFF059669)
                                            )
                                        }
                                    },
                                    onClick = {
                                        selectedKhatId = null
                                        selectedKhatName = ""
                                        sourceDropdownExpanded = false
                                    }
                                )

                                // 2. Custom Khats
                                if (khats.isNotEmpty()) {
                                    Divider()
                                    khats.forEach { khat ->
                                        val kColor = try {
                                            Color(android.graphics.Color.parseColor(khat.colorHex))
                                        } catch (_: Exception) {
                                            MaterialTheme.colorScheme.primary
                                        }
                                        DropdownMenuItem(
                                            leadingIcon = {
                                                Icon(
                                                    imageVector = getKhatIcon(khat.iconName, khat.name),
                                                    contentDescription = null,
                                                    tint = kColor,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            },
                                            text = {
                                                Column {
                                                    Text(
                                                        text = AppLocale.khatName(khat.name, isEnglish),
                                                        fontWeight = if (selectedKhatId == khat.id) FontWeight.Bold else FontWeight.Normal
                                                    )
                                                    Text(
                                                        text = formatTakaSafe(khat.allocatedAmount),
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            },
                                            onClick = {
                                                selectedKhatId = khat.id
                                                selectedKhatName = khat.name
                                                sourceDropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        if (isInsufficientKhatBalance) {
                            val displayKName = AppLocale.khatName(selectedKhat?.name ?: "", isEnglish)
                            Text(
                                text = if (isEnglish) {
                                    "Insufficient balance in '$displayKName'! (Available: ${formatTakaSafe(selectedKhat?.allocatedAmount ?: 0.0)})"
                                } else {
                                    "'$displayKName' খাতে পর্যাপ্ত ব্যালেন্স নেই! (আছে: ${formatTakaSafe(selectedKhat?.allocatedAmount ?: 0.0)})"
                                },
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = MaterialTheme.colorScheme.error,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }
                } else {
                    // Income info
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF2E7D32).copy(alpha = 0.08f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2E7D32).copy(alpha = 0.25f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = Color(0xFF2E7D32),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isEnglish) "Deposits will be added to Oboshisto automatically" else "নতুন আয় স্বয়ংক্রিয়ভাবে 'অবশিষ্ট' ব্যালেন্সে যোগ হবে",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                                color = Color(0xFF2E7D32)
                            )
                        }
                    }
                }

                // Date Selection (Optional: default is current time, or pick any past/custom date)
                val displayDateText = remember(selectedCustomTimestamp, isEnglish) {
                    if (selectedCustomTimestamp == null) {
                        if (isEnglish) "Today (Present time)" else "আজকের তারিখ (বর্তমান সময়)"
                    } else {
                        val c = Calendar.getInstance().apply { timeInMillis = selectedCustomTimestamp!! }
                        if (isEnglish) {
                            SimpleDateFormat("dd MMMM yyyy", Locale.US).format(c.time)
                        } else {
                            val bengaliMonths = arrayOf("জানুয়ারি", "ফেব্রুয়ারি", "মার্চ", "এপ্রিল", "মে", "জুন", "জুলাই", "আগস্ট", "সেপ্টেম্বর", "অক্টোবর", "নভেম্বর", "ডিসেম্বর")
                            val day = c.get(Calendar.DAY_OF_MONTH)
                            val m = bengaliMonths[c.get(Calendar.MONTH)]
                            val y = c.get(Calendar.YEAR)
                            "$day $m $y"
                        }
                    }
                }

                Surface(
                    onClick = {
                        val cal = Calendar.getInstance()
                        if (selectedCustomTimestamp != null) {
                            cal.timeInMillis = selectedCustomTimestamp!!
                        }
                        val dp = DatePickerDialog(
                            context,
                            { _, y, m, d ->
                                val now = Calendar.getInstance()
                                val chosen = Calendar.getInstance().apply {
                                    set(Calendar.YEAR, y)
                                    set(Calendar.MONTH, m)
                                    set(Calendar.DAY_OF_MONTH, d)
                                    // Preserve current time-of-day
                                    set(Calendar.HOUR_OF_DAY, now.get(Calendar.HOUR_OF_DAY))
                                    set(Calendar.MINUTE, now.get(Calendar.MINUTE))
                                    set(Calendar.SECOND, now.get(Calendar.SECOND))
                                }
                                selectedCustomTimestamp = chosen.timeInMillis
                            },
                            cal.get(Calendar.YEAR),
                            cal.get(Calendar.MONTH),
                            cal.get(Calendar.DAY_OF_MONTH)
                        )
                        dp.show()
                    },
                    shape = RoundedCornerShape(14.dp),
                    color = if (selectedCustomTimestamp != null) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (selectedCustomTimestamp != null) MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                        else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("btn_select_transaction_date")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarToday,
                                contentDescription = null,
                                tint = if (selectedCustomTimestamp != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                            Column {
                                Text(
                                    text = if (isEnglish) "Date (Optional):" else "তারিখ (ঐচ্ছিক):",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 10.5.sp
                                    )
                                )
                                Text(
                                    text = displayDateText,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = if (selectedCustomTimestamp != null) FontWeight.Bold else FontWeight.Medium,
                                        color = if (selectedCustomTimestamp != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                        fontSize = 13.sp
                                    )
                                )
                            }
                        }

                        if (selectedCustomTimestamp != null) {
                            IconButton(
                                onClick = { selectedCustomTimestamp = null },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Reset Date",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        } else {
                            Text(
                                text = if (isEnglish) "Change" else "পরিবর্তন",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }
                }

                // Description / Title Input
                OutlinedTextField(
                    value = titleText,
                    onValueChange = { titleText = it },
                    label = { Text(if (isEnglish) "Title / Purpose (Optional)" else "বিবরণ / শিরোনাম (ঐচ্ছিক)") },
                    placeholder = { Text(com.example.ui.util.AppLocale.category(selectedCategoryName, isEnglish)) },
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Next),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("transaction_title_input")
                )

                // Note Input
                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    label = { Text(if (isEnglish) "Note (Optional)" else "অতিরিক্ত নোট (ঐচ্ছিক)") },
                    placeholder = { Text(if (isEnglish) "e.g. details, location, etc." else "যেমন: কার সাথে, কি উদ্দেশ্যে ইত্যাদি") },
                    shape = RoundedCornerShape(14.dp),
                    maxLines = 2,
                    keyboardOptions = KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Done),
                    keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                        onDone = {
                            focusManager.clearFocus()
                            keyboardController?.hide()
                        }
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("transaction_note_input")
                )
                    Spacer(modifier = Modifier.height(4.dp))

                    // Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                focusManager.clearFocus()
                                keyboardController?.hide()
                                onDismiss()
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                        ) {
                            Text(if (isEnglish) "Cancel" else "বাতিল")
                        }

                        val validAmount = (amountText.toDoubleOrNull() ?: 0.0) > 0 &&
                            (!isExpense || (!isSourceBalanceZero && (amountText.toDoubleOrNull() ?: 0.0) <= maxSpendable)) &&
                            !isInsufficientKhatBalance
                        Button(
                            onClick = {
                                val amount = amountText.toDoubleOrNull() ?: 0.0
                                if (amount > 0 && (!isExpense || (!isSourceBalanceZero && amount <= maxSpendable)) && !isInsufficientKhatBalance) {
                                    val finalTitle = if (titleText.isNotBlank()) {
                                        titleText.trim()
                                    } else {
                                        com.example.ui.util.AppLocale.category(selectedCategoryName, isEnglish)
                                    }
                                    val finalTimestamp = selectedCustomTimestamp ?: System.currentTimeMillis()
                                    onSave(
                                        finalTitle,
                                        amount,
                                        transactionType,
                                        selectedCategoryName,
                                        noteText.trim(),
                                        if (transactionType == "EXPENSE") selectedKhatId else null,
                                        if (transactionType == "EXPENSE") selectedKhatName else "",
                                        finalTimestamp
                                    )
                                    focusManager.clearFocus()
                                    keyboardController?.hide()
                                    onDismiss()
                                }
                            },
                            enabled = validAmount,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier
                                .weight(1.3f)
                                .height(48.dp)
                                .testTag("save_transaction_button")
                        ) {
                            Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (isEnglish) "Save" else "সংরক্ষণ করুন", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                KeyboardScrollDownHint(
                    scrollState = scrollState,
                    isKeyboardOpen = isKeyboardOpen,
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

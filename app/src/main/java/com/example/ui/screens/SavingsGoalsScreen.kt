package com.example.ui.screens

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Color
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
import com.example.data.model.SavingsGoalEntity
import com.example.ui.util.AppLocale
import com.example.ui.util.clearFocusOnTap
import com.example.data.model.formatTakaSafe
import com.example.ui.components.AppCardDefaults
import com.example.ui.components.AppToastManager
import com.example.ui.components.KeyboardScrollDownHint
import com.example.ui.viewmodel.ExpenseViewModel
import java.util.*
import kotlin.math.max
import kotlin.math.min

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavingsGoalsScreen(
    viewModel: ExpenseViewModel,
    modifier: Modifier = Modifier
) {
    val goals by viewModel.savingsGoals.collectAsState()
    val isEnglish by viewModel.isEnglish.collectAsState()
    val netBalance by viewModel.netBalance.collectAsState()
    val focusManager = LocalFocusManager.current

    var showAddDialog by remember { mutableStateOf(false) }
    var goalForDeposit by remember { mutableStateOf<SavingsGoalEntity?>(null) }
    var goalForWithdraw by remember { mutableStateOf<SavingsGoalEntity?>(null) }
    var goalForHistory by remember { mutableStateOf<SavingsGoalEntity?>(null) }
    var goalToDelete by remember { mutableStateOf<SavingsGoalEntity?>(null) }

    val totalSaved = remember(goals) { goals.sumOf { it.savedAmount } }
    val totalTarget = remember(goals) { goals.sumOf { it.targetAmount } }
    val overallRatio = if (totalTarget > 0) (totalSaved / totalTarget).toFloat().coerceIn(0f, 1f) else 0f

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .clearFocusOnTap(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (isEnglish) "Savings Goals" else "সঞ্চয় ও আর্থিক লক্ষ্য",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = if (isEnglish) "Future financial security & savings" else "ভবিষ্যতের আর্থিক নিরাপত্তা ও সঞ্চয় ট্র্যাকার",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
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
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .height(40.dp)
                    .testTag("fab_add_savings_goal")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Default.AddCircle, contentDescription = "Add Goal", modifier = Modifier.size(17.dp))
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = if (isEnglish) "Add Goal" else "নতুন লক্ষ্য যোগ",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.5.sp
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
            item { Spacer(modifier = Modifier.height(2.dp)) }

            // Overview Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("goals_overview_card"),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    border = AppCardDefaults.border(),
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
                            Column(modifier = Modifier.weight(1f, fill = false)) {
                                Text(
                                    text = AppLocale.totalSaved(isEnglish),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = formatTakaSafe(totalSaved),
                                    style = if (totalSaved >= 100_000_000) MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.primary
                                    ) else if (totalSaved >= 1_000_000) MaterialTheme.typography.headlineSmall.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.primary
                                    ) else MaterialTheme.typography.headlineMedium.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.primary
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AccountBalance,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                        }

                        if (totalTarget > 0) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${if (isEnglish) "Total Target: " else "মোট লক্ষ্য: "}${formatTakaSafe(totalTarget)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "${String.format(Locale.US, "%.0f", overallRatio * 100)}%",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        ),
                                        maxLines = 1
                                    )
                                }

                                LinearProgressIndicator(
                                    progress = { overallRatio },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(4.dp)),
                                    color = MaterialTheme.colorScheme.primary,
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Goals List Header
            item {
                Text(
                    text = if (isEnglish) "Savings Goals (${goals.size})" else "সঞ্চয়ের লক্ষ্যসমূহ (${goals.size}টি)",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }

            // Goals items
            if (goals.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountBalance,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = AppLocale.noSavingsGoals(isEnglish),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = if (isEnglish) "Set savings targets for gadgets, trips, or emergency funds and save step by step."
                                else "ফোন কেনা, বাইক, ট্যুর বা জরুরি ফান্ডের জন্য সঞ্চয় লক্ষ্য সেট করুন ও একটু একটু করে টাকা জমান।",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(goals, key = { it.id }) { goal ->
                    GoalCardItem(
                        goal = goal,
                        isEnglish = isEnglish,
                        onAddDeposit = { goalForDeposit = goal },
                        onWithdraw = { goalForWithdraw = goal },
                        onViewHistory = { goalForHistory = goal },
                        onDelete = { goalToDelete = goal }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }

    // Add Goal Dialog
    if (showAddDialog) {
        var title by remember { mutableStateOf("") }
        var targetAmountText by remember { mutableStateOf("") }
        var note by remember { mutableStateOf("") }

        val keyboardController = LocalSoftwareKeyboardController.current
        val imeInsets = WindowInsets.ime
        val density = LocalDensity.current
        val isKeyboardOpen = imeInsets.getBottom(density) > 0

        val animatedCardElevation by animateDpAsState(
            targetValue = if (isKeyboardOpen) 12.dp else 6.dp,
            animationSpec = tween(durationMillis = 250),
            label = "goal_card_elevation"
        )
        val animatedVerticalPadding by animateDpAsState(
            targetValue = if (isKeyboardOpen) 6.dp else 20.dp,
            animationSpec = tween(durationMillis = 250),
            label = "goal_vertical_padding"
        )
        val animatedContentPadding by animateDpAsState(
            targetValue = if (isKeyboardOpen) 16.dp else 24.dp,
            animationSpec = tween(durationMillis = 250),
            label = "goal_content_padding"
        )
        val animatedSpacing by animateDpAsState(
            targetValue = if (isKeyboardOpen) 10.dp else 16.dp,
            animationSpec = tween(durationMillis = 250),
            label = "goal_spacing"
        )

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
                    .padding(horizontal = 20.dp, vertical = animatedVerticalPadding)
                    .clearFocusOnTap(),
                contentAlignment = if (isKeyboardOpen) Alignment.TopCenter else Alignment.Center
            ) {
                val addGoalScrollState = rememberScrollState()

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 450.dp)
                        .testTag("add_goal_dialog"),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    border = AppCardDefaults.border(),
                    elevation = CardDefaults.cardElevation(defaultElevation = animatedCardElevation)
                ) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = animatedContentPadding, vertical = animatedContentPadding)
                                .verticalScroll(addGoalScrollState),
                            verticalArrangement = Arrangement.spacedBy(animatedSpacing)
                        ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isEnglish) "New Savings Goal" else "নতুন সঞ্চয় লক্ষ্য",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            IconButton(
                                onClick = {
                                    focusManager.clearFocus()
                                    keyboardController?.hide()
                                    showAddDialog = false
                                },
                                modifier = Modifier.testTag("dialog_close_goal")
                            ) {
                                Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                            }
                        }

                        OutlinedTextField(
                            value = title,
                            onValueChange = { title = it },
                            label = { Text(if (isEnglish) "Goal Name" else "লক্ষ্যের নাম") },
                            placeholder = { Text(if (isEnglish) "e.g., New Laptop, Emergency Fund" else "যেমন: নতুন ল্যাপটপ, জরুরি ফান্ড") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            keyboardOptions = KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Next),
                            modifier = Modifier.fillMaxWidth().testTag("input_goal_title")
                        )

                        OutlinedTextField(
                            value = targetAmountText,
                            onValueChange = { input ->
                                targetAmountText = input.filter { it.isDigit() || it == '.' }
                            },
                            label = { Text(if (isEnglish) "Target Amount to Save?" else "কত টাকা জমাতে চান?") },
                            placeholder = { Text("0.00") },
                            leadingIcon = { Text("৳", fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 12.dp)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = androidx.compose.ui.text.input.ImeAction.Next),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().testTag("input_goal_amount")
                        )

                        OutlinedTextField(
                            value = note,
                            onValueChange = { note = it },
                            label = { Text(if (isEnglish) "Purpose or Note (Optional)" else "উদ্দেশ্য বা নোট (ঐচ্ছিক)") },
                            placeholder = { Text(if (isEnglish) "e.g., Target completion by December" else "যেমন: আগামী ডিসেম্বরের মধ্যে পূরণ করতে চাই") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            keyboardOptions = KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Done),
                            keyboardActions = androidx.compose.foundation.text.KeyboardActions(onDone = {
                                focusManager.clearFocus()
                                keyboardController?.hide()
                            }),
                            modifier = Modifier.fillMaxWidth()
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
                                    showAddDialog = false
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f).height(48.dp)
                            ) {
                                Text(AppLocale.cancel(isEnglish))
                            }

                            val valid = title.isNotBlank() && (targetAmountText.toDoubleOrNull() ?: 0.0) > 0
                            Button(
                                onClick = {
                                    val amount = targetAmountText.toDoubleOrNull() ?: 0.0
                                    viewModel.addSavingsGoal(
                                        title = title.trim(),
                                        targetAmount = amount,
                                        note = note.trim()
                                    )
                                    focusManager.clearFocus()
                                    keyboardController?.hide()
                                    showAddDialog = false
                                },
                                enabled = valid,
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                ),
                                modifier = Modifier.weight(1.3f).height(48.dp).testTag("btn_save_goal")
                            ) {
                                Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(17.dp))
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(AppLocale.save(isEnglish), fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    KeyboardScrollDownHint(
                        scrollState = addGoalScrollState,
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

    // Add Deposit to Goal Dialog
    goalForDeposit?.let { goal ->
        var depositAmountText by remember { mutableStateOf("") }
        var depositNote by remember { mutableStateOf("") }
        val isAccountBalanceZero = (netBalance <= 0.0)
        var adjustMainAccount by remember { mutableStateOf(!isAccountBalanceZero) }

        val keyboardController = LocalSoftwareKeyboardController.current
        val imeInsets = WindowInsets.ime
        val density = LocalDensity.current
        val isKeyboardOpen = imeInsets.getBottom(density) > 0

        val animatedCardElevation by animateDpAsState(
            targetValue = if (isKeyboardOpen) 12.dp else 6.dp,
            animationSpec = tween(durationMillis = 250),
            label = "deposit_card_elevation"
        )
        val animatedVerticalPadding by animateDpAsState(
            targetValue = if (isKeyboardOpen) 6.dp else 20.dp,
            animationSpec = tween(durationMillis = 250),
            label = "deposit_vertical_padding"
        )
        val animatedContentPadding by animateDpAsState(
            targetValue = if (isKeyboardOpen) 16.dp else 24.dp,
            animationSpec = tween(durationMillis = 250),
            label = "deposit_content_padding"
        )
        val animatedSpacing by animateDpAsState(
            targetValue = if (isKeyboardOpen) 10.dp else 16.dp,
            animationSpec = tween(durationMillis = 250),
            label = "deposit_spacing"
        )

        val remaining = max(0.0, goal.targetAmount - goal.savedAmount)
        val maxAllowedDeposit = if (adjustMainAccount) minOf(remaining, max(0.0, netBalance)) else remaining
        val enteredAmount = depositAmountText.toDoubleOrNull() ?: 0.0
        val exceedsRemaining = enteredAmount > remaining
        val exceedsAccountBalance = adjustMainAccount && enteredAmount > maxAllowedDeposit
        var showDepositOverBalanceWarning by remember { mutableStateOf(false) }
        val isValid = enteredAmount > 0.0 && !exceedsRemaining && (!adjustMainAccount || (netBalance > 0.0 && enteredAmount <= netBalance)) && remaining > 0.0

        Dialog(
            onDismissRequest = {
                focusManager.clearFocus()
                keyboardController?.hide()
                goalForDeposit = null
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
                val depositScrollState = rememberScrollState()

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 450.dp)
                        .testTag("deposit_dialog"),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    border = AppCardDefaults.incomeBorder(),
                    elevation = CardDefaults.cardElevation(defaultElevation = animatedCardElevation)
                ) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = animatedContentPadding, vertical = animatedContentPadding)
                                .verticalScroll(depositScrollState),
                            verticalArrangement = Arrangement.spacedBy(animatedSpacing)
                        ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isEnglish) "Deposit into '${goal.title}'" else "'${goal.title}'-এ টাকা জমা",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = {
                                    focusManager.clearFocus()
                                    keyboardController?.hide()
                                    goalForDeposit = null
                                }
                            ) {
                                Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                            }
                        }

                        // Target, Current Saved, and Remaining Amount Summary
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(if (isEnglish) "Target" else "টার্গেট", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(formatTakaSafe(goal.targetAmount), style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                                }
                                Column {
                                    Text(if (isEnglish) "Already Saved" else "জমা হয়েছে", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(formatTakaSafe(goal.savedAmount), style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(if (isEnglish) "Remaining" else "বাকি আছে", style = MaterialTheme.typography.labelSmall, color = Color(0xFF10B981))
                                    Text(formatTakaSafe(remaining), style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold, color = Color(0xFF059669)))
                                }
                            }
                        }

                        if (adjustMainAccount && isAccountBalanceZero) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.45f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = if (isEnglish)
                                            "⚠️ Account balance is ৳0! Add money first, or turn off balance deduction."
                                        else
                                            "⚠️ অ্যাকাউন্টে পর্যাপ্ত ব্যালেন্স নেই (ব্যালেন্স ৳০)। অ্যাকাউন্টে টাকা যোগ করুন অথবা অ্যাকাউন্ট থেকে কর্তন বন্ধ রাখুন।",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = MaterialTheme.colorScheme.error,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp
                                        )
                                    )
                                }
                            }
                        }

                        OutlinedTextField(
                            value = depositAmountText,
                            onValueChange = { input ->
                                val filtered = input.filter { it.isDigit() || it == '.' }
                                if (filtered.count { it == '.' } <= 1) {
                                    if (adjustMainAccount) {
                                        if (isAccountBalanceZero) {
                                            depositAmountText = ""
                                            keyboardController?.hide()
                                            focusManager.clearFocus()
                                            showDepositOverBalanceWarning = true
                                            AppToastManager.show(
                                                if (isEnglish) "Available balance is ৳0! Add money first."
                                                else "অ্যাকাউন্টে পর্যাপ্ত ব্যালেন্স নেই (ব্যালেন্স ৳০)!"
                                            )
                                        } else {
                                            val entered = filtered.toDoubleOrNull() ?: 0.0
                                            if (entered > maxAllowedDeposit) {
                                                depositAmountText = if (maxAllowedDeposit % 1.0 == 0.0) maxAllowedDeposit.toLong().toString() else "%.2f".format(Locale.US, maxAllowedDeposit)
                                                keyboardController?.hide()
                                                focusManager.clearFocus()
                                                showDepositOverBalanceWarning = true
                                                AppToastManager.show(
                                                    if (isEnglish) "Cannot deposit more than available balance (${formatTakaSafe(maxAllowedDeposit)})"
                                                    else "ব্যালেন্সের অতিরিক্ত জমা সম্ভব নয়! সর্বোচ্চ ${formatTakaSafe(maxAllowedDeposit)} নির্ধারণ করা হয়েছে।"
                                                )
                                            } else {
                                                depositAmountText = filtered
                                                showDepositOverBalanceWarning = false
                                            }
                                        }
                                    } else {
                                        val entered = filtered.toDoubleOrNull() ?: 0.0
                                        if (entered > remaining) {
                                            depositAmountText = if (remaining % 1.0 == 0.0) remaining.toLong().toString() else "%.2f".format(Locale.US, remaining)
                                            keyboardController?.hide()
                                            focusManager.clearFocus()
                                            showDepositOverBalanceWarning = false
                                        } else {
                                            depositAmountText = filtered
                                            showDepositOverBalanceWarning = false
                                        }
                                    }
                                }
                            },
                            label = { Text(if (isEnglish) "Deposit Amount" else "জমার পরিমাণ") },
                            placeholder = { Text("0.00") },
                            leadingIcon = { Text("৳", fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 12.dp)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = androidx.compose.ui.text.input.ImeAction.Next),
                            singleLine = true,
                            isError = exceedsRemaining || exceedsAccountBalance,
                            shape = RoundedCornerShape(12.dp),
                            supportingText = {
                                if (adjustMainAccount && isAccountBalanceZero) {
                                    Text(
                                        text = if (isEnglish) "Available balance: ৳0" else "অ্যাকাউন্ট ব্যালেন্স: ৳০",
                                        color = MaterialTheme.colorScheme.error,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                } else if (exceedsAccountBalance) {
                                    Text(
                                        text = if (isEnglish) "Cannot exceed account balance (${formatTakaSafe(maxAllowedDeposit)})"
                                        else "অ্যাকাউন্ট ব্যালেন্সের (${formatTakaSafe(maxAllowedDeposit)}) বেশি জমা সম্ভব নয়",
                                        color = MaterialTheme.colorScheme.error,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                } else if (exceedsRemaining) {
                                    Text(
                                        text = if (isEnglish) "Deposit cannot exceed remaining amount (${formatTakaSafe(remaining)})"
                                        else "ডিপোজিট বাকি থাকা পরিমাণের (${formatTakaSafe(remaining)}) বেশি হতে পারবে না",
                                        color = MaterialTheme.colorScheme.error,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                } else {
                                    Text(
                                        text = if (adjustMainAccount) {
                                            if (isEnglish) "Max deposit from account: ${formatTakaSafe(maxAllowedDeposit)}"
                                            else "অ্যাকাউন্ট থেকে সর্বোচ্চ জমা: ${formatTakaSafe(maxAllowedDeposit)}"
                                        } else {
                                            if (isEnglish) "Max deposit: ${formatTakaSafe(remaining)}"
                                            else "সর্বোচ্চ জমা: ${formatTakaSafe(remaining)}"
                                        },
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth().testTag("input_deposit_amount")
                        )

                        OutlinedTextField(
                            value = depositNote,
                            onValueChange = { depositNote = it },
                            label = { Text(if (isEnglish) "Note / Source (Optional)" else "নোট / উৎস (ঐচ্ছিক)") },
                            placeholder = { Text(if (isEnglish) "e.g., Monthly bonus, Pocket money" else "যেমন: মাসিক বোনাস, পকেট মানি") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            keyboardOptions = KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Done),
                            keyboardActions = androidx.compose.foundation.text.KeyboardActions(onDone = {
                                focusManager.clearFocus()
                                keyboardController?.hide()
                            }),
                            modifier = Modifier.fillMaxWidth().testTag("input_deposit_note")
                        )

                        // Quick buttons: +100, +500, +1000, +5000 (auto-capped to max allowed)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(100.0, 500.0, 1000.0, 5000.0).forEach { addVal ->
                                val limit = if (adjustMainAccount) maxAllowedDeposit else remaining
                                val isChipDisabled = (limit <= 0.0)
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isChipDisabled) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                    else MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            if (limit <= 0.0) {
                                                depositAmountText = ""
                                                keyboardController?.hide()
                                                focusManager.clearFocus()
                                                AppToastManager.show(
                                                    if (isEnglish) "No balance available to deposit."
                                                    else "জমা করার মতো পর্যাপ্ত ব্যালেন্স নেই।"
                                                )
                                            } else {
                                                val current = depositAmountText.toDoubleOrNull() ?: 0.0
                                                val sum = current + addVal
                                                val finalVal = minOf(limit, sum)
                                                depositAmountText = if (finalVal % 1.0 == 0.0) finalVal.toLong().toString() else "%.2f".format(Locale.US, finalVal)
                                                if (finalVal >= limit) {
                                                    keyboardController?.hide()
                                                    focusManager.clearFocus()
                                                }
                                            }
                                        }
                                ) {
                                    Box(
                                        modifier = Modifier.padding(vertical = 6.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "+%.0f".format(addVal),
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = if (isChipDisabled) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                                else MaterialTheme.colorScheme.onSurface
                                            )
                                        )
                                    }
                                }
                            }
                        }

                        // Opinion section: Adjust with main balance?
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (adjustMainAccount) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (adjustMainAccount) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                                else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (isAccountBalanceZero && !adjustMainAccount) {
                                        AppToastManager.show(
                                            if (isEnglish) "Insufficient account balance (৳0)"
                                            else "অ্যাকাউন্টে পর্যাপ্ত ব্যালেন্স নেই (ব্যালেন্স ৳০)"
                                        )
                                    } else {
                                        val nextState = !adjustMainAccount
                                        adjustMainAccount = nextState
                                        val limit = if (nextState) minOf(remaining, max(0.0, netBalance)) else remaining
                                        val cur = depositAmountText.toDoubleOrNull() ?: 0.0
                                        if (cur > limit) {
                                            depositAmountText = if (limit % 1.0 == 0.0) limit.toLong().toString() else "%.2f".format(Locale.US, limit)
                                            keyboardController?.hide()
                                            focusManager.clearFocus()
                                        }
                                    }
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = AppLocale.savingsDeductSwitchTitle(isEnglish),
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = if (adjustMainAccount) AppLocale.savingsDeductActive(isEnglish)
                                        else AppLocale.savingsDeductInactive(isEnglish),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (adjustMainAccount) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Switch(
                                    checked = adjustMainAccount,
                                    enabled = !isAccountBalanceZero,
                                    onCheckedChange = { nextState ->
                                        if (isAccountBalanceZero && nextState) {
                                            AppToastManager.show(
                                                if (isEnglish) "Insufficient account balance (৳0)"
                                                else "অ্যাকাউন্টে পর্যাপ্ত ব্যালেন্স নেই (ব্যালেন্স ৳০)"
                                            )
                                        } else {
                                            adjustMainAccount = nextState
                                            val limit = if (nextState) minOf(remaining, max(0.0, netBalance)) else remaining
                                            val cur = depositAmountText.toDoubleOrNull() ?: 0.0
                                            if (cur > limit) {
                                                depositAmountText = if (limit % 1.0 == 0.0) limit.toLong().toString() else "%.2f".format(Locale.US, limit)
                                                keyboardController?.hide()
                                                focusManager.clearFocus()
                                            }
                                        }
                                    },
                                    modifier = Modifier.testTag("switch_adjust_savings_balance")
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    focusManager.clearFocus()
                                    keyboardController?.hide()
                                    goalForDeposit = null
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f).height(48.dp)
                            ) {
                                Text(AppLocale.cancel(isEnglish))
                            }

                            Button(
                                onClick = {
                                    val amount = depositAmountText.toDoubleOrNull() ?: 0.0
                                    if (amount <= 0 || amount > remaining) return@Button
                                    if (adjustMainAccount && (isAccountBalanceZero || amount > netBalance)) {
                                        AppToastManager.show(
                                            if (isEnglish) "Insufficient account balance!" else "অ্যাকাউন্টে পর্যাপ্ত ব্যালেন্স নেই!"
                                        )
                                        return@Button
                                    }
                                    viewModel.addDepositToGoalWithAdjustment(
                                        goal = goal,
                                        depositAmount = amount,
                                        adjustMainAccount = adjustMainAccount,
                                        note = depositNote.trim(),
                                        isEnglish = isEnglish
                                    )
                                    focusManager.clearFocus()
                                    keyboardController?.hide()
                                    goalForDeposit = null
                                },
                                enabled = isValid,
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                ),
                                modifier = Modifier.weight(1.3f).height(48.dp).testTag("btn_confirm_deposit")
                            ) {
                                Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(17.dp))
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(if (isEnglish) "Deposit" else "জমা করুন", fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    KeyboardScrollDownHint(
                        scrollState = depositScrollState,
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

    // Withdraw from Goal Dialog
    goalForWithdraw?.let { goal ->
        var withdrawAmountText by remember { mutableStateOf("") }
        var withdrawNote by remember { mutableStateOf("") }
        var adjustMainAccount by remember { mutableStateOf(true) }

        val keyboardController = LocalSoftwareKeyboardController.current
        val imeInsets = WindowInsets.ime
        val density = LocalDensity.current
        val isKeyboardOpen = imeInsets.getBottom(density) > 0

        val animatedCardElevation by animateDpAsState(
            targetValue = if (isKeyboardOpen) 12.dp else 6.dp,
            animationSpec = tween(durationMillis = 250),
            label = "withdraw_card_elevation"
        )
        val animatedVerticalPadding by animateDpAsState(
            targetValue = if (isKeyboardOpen) 6.dp else 20.dp,
            animationSpec = tween(durationMillis = 250),
            label = "withdraw_vertical_padding"
        )
        val animatedContentPadding by animateDpAsState(
            targetValue = if (isKeyboardOpen) 16.dp else 24.dp,
            animationSpec = tween(durationMillis = 250),
            label = "withdraw_content_padding"
        )
        val animatedSpacing by animateDpAsState(
            targetValue = if (isKeyboardOpen) 10.dp else 16.dp,
            animationSpec = tween(durationMillis = 250),
            label = "withdraw_spacing"
        )

        val amt = withdrawAmountText.toDoubleOrNull() ?: 0.0
        val valid = amt > 0 && amt <= goal.savedAmount

        Dialog(
            onDismissRequest = {
                focusManager.clearFocus()
                keyboardController?.hide()
                goalForWithdraw = null
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
                val withdrawScrollState = rememberScrollState()

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 450.dp)
                        .testTag("withdraw_dialog"),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    border = AppCardDefaults.expenseBorder(),
                    elevation = CardDefaults.cardElevation(defaultElevation = animatedCardElevation)
                ) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = animatedContentPadding, vertical = animatedContentPadding)
                                .verticalScroll(withdrawScrollState),
                            verticalArrangement = Arrangement.spacedBy(animatedSpacing)
                        ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = AppLocale.withdrawTitle(isEnglish, goal.title),
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = {
                                    focusManager.clearFocus()
                                    keyboardController?.hide()
                                    goalForWithdraw = null
                                }
                            ) {
                                Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                            }
                        }

                        Text(
                            text = "${if (isEnglish) "Available savings: " else "বর্তমান জমা আছে: "}${formatTakaSafe(goal.savedAmount)}",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.primary
                        )

                        OutlinedTextField(
                            value = withdrawAmountText,
                            onValueChange = { input ->
                                val filtered = input.filter { it.isDigit() || it == '.' }
                                if (filtered.count { it == '.' } <= 1) {
                                    val entered = filtered.toDoubleOrNull() ?: 0.0
                                    if (entered > goal.savedAmount) {
                                        withdrawAmountText = if (goal.savedAmount % 1.0 == 0.0) goal.savedAmount.toLong().toString() else "%.2f".format(Locale.US, goal.savedAmount)
                                        keyboardController?.hide()
                                        focusManager.clearFocus()
                                        AppToastManager.show(
                                            if (isEnglish) "Maximum withdrawal: ${formatTakaSafe(goal.savedAmount)}"
                                            else "সর্বোচ্চ উত্তোলনের পরিমাণ: ${formatTakaSafe(goal.savedAmount)}"
                                        )
                                    } else {
                                        withdrawAmountText = filtered
                                    }
                                }
                            },
                            label = { Text(AppLocale.withdrawAmount(isEnglish)) },
                            placeholder = { Text("0.00") },
                            leadingIcon = { Text("৳", fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 12.dp)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = androidx.compose.ui.text.input.ImeAction.Next),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            supportingText = {
                                Text(
                                    text = if (isEnglish) "Max withdrawal: ${formatTakaSafe(goal.savedAmount)}"
                                    else "সর্বোচ্চ উত্তোলন: ${formatTakaSafe(goal.savedAmount)}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            },
                            modifier = Modifier.fillMaxWidth().testTag("input_withdraw_amount")
                        )

                        // Quick withdrawal buttons: +100, +500, +1000, All
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(100.0, 500.0, 1000.0).forEach { addVal ->
                                val limit = goal.savedAmount
                                val isChipDisabled = (limit <= 0.0)
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isChipDisabled) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                    else MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            if (limit > 0.0) {
                                                val current = withdrawAmountText.toDoubleOrNull() ?: 0.0
                                                val sum = current + addVal
                                                val finalVal = minOf(limit, sum)
                                                withdrawAmountText = if (finalVal % 1.0 == 0.0) finalVal.toLong().toString() else "%.2f".format(Locale.US, finalVal)
                                                if (finalVal >= limit) {
                                                    keyboardController?.hide()
                                                    focusManager.clearFocus()
                                                }
                                            }
                                        }
                                ) {
                                    Box(
                                        modifier = Modifier.padding(vertical = 6.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "+%.0f".format(addVal),
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = if (isChipDisabled) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                                else MaterialTheme.colorScheme.onSurface
                                            )
                                        )
                                    }
                                }
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                                modifier = Modifier
                                    .weight(1.2f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        if (goal.savedAmount > 0.0) {
                                            withdrawAmountText = if (goal.savedAmount % 1.0 == 0.0) goal.savedAmount.toLong().toString() else "%.2f".format(Locale.US, goal.savedAmount)
                                            keyboardController?.hide()
                                            focusManager.clearFocus()
                                        }
                                    }
                            ) {
                                Box(
                                    modifier = Modifier.padding(vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (isEnglish) "Withdraw All" else "সব উত্তোলন",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    )
                                }
                            }
                        }

                        OutlinedTextField(
                            value = withdrawNote,
                            onValueChange = { withdrawNote = it },
                            label = { Text(if (isEnglish) "Reason / Note (Optional)" else "উত্তোলনের কারণ / নোট (ঐচ্ছিক)") },
                            placeholder = { Text(if (isEnglish) "e.g., Emergency expense, Purchased item" else "যেমন: জরুরি প্রয়োজন, কেনাকাটা") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            keyboardOptions = KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Done),
                            keyboardActions = androidx.compose.foundation.text.KeyboardActions(onDone = {
                                focusManager.clearFocus()
                                keyboardController?.hide()
                            }),
                            modifier = Modifier.fillMaxWidth().testTag("input_withdraw_note")
                        )

                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (adjustMainAccount) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (adjustMainAccount) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                                else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { adjustMainAccount = !adjustMainAccount }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = AppLocale.savingsWithdrawAddSwitchTitle(isEnglish),
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = if (adjustMainAccount) AppLocale.savingsWithdrawActive(isEnglish)
                                        else AppLocale.savingsWithdrawInactive(isEnglish),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (adjustMainAccount) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Switch(
                                    checked = adjustMainAccount,
                                    onCheckedChange = { adjustMainAccount = it },
                                    modifier = Modifier.testTag("switch_adjust_withdraw_balance")
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    focusManager.clearFocus()
                                    keyboardController?.hide()
                                    goalForWithdraw = null
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f).height(48.dp)
                            ) {
                                Text(AppLocale.cancel(isEnglish))
                            }

                            Button(
                                onClick = {
                                    viewModel.withdrawFromGoalWithAdjustment(
                                        goal = goal,
                                        withdrawAmount = amt,
                                        adjustMainAccount = adjustMainAccount,
                                        note = withdrawNote.trim(),
                                        isEnglish = isEnglish
                                    )
                                    focusManager.clearFocus()
                                    keyboardController?.hide()
                                    goalForWithdraw = null
                                },
                                enabled = valid,
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                ),
                                modifier = Modifier.weight(1.3f).height(48.dp).testTag("btn_confirm_withdraw")
                            ) {
                                Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(17.dp))
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(AppLocale.withdrawSavings(isEnglish), fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    KeyboardScrollDownHint(
                        scrollState = withdrawScrollState,
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

    // Delete Confirmation Dialog
    goalToDelete?.let { goal ->
        AlertDialog(
            onDismissRequest = { goalToDelete = null },
            title = { Text(AppLocale.deleteConfirm(isEnglish)) },
            text = {
                Text(
                    if (goal.savedAmount > 0.0) {
                        if (isEnglish) "'${goal.title}' has ${formatTakaSafe(goal.savedAmount)} saved. Deleting this goal will return this amount to your main account."
                        else "'${goal.title}' লক্ষ্যটিতে ${formatTakaSafe(goal.savedAmount)} সঞ্চিত রয়েছে। লক্ষ্য মুছে ফেললে এই টাকা আপনার মূল অ্যাকাউন্টে ফেরত যোগ হবে।"
                    } else {
                        if (isEnglish) "Do you want to delete savings goal '${goal.title}'?"
                        else "'${goal.title}' সঞ্চয় লক্ষ্যটি মুছে ফেলতে চান?"
                    }
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteSavingsGoal(goal, refundToMain = true)
                        goalToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(AppLocale.delete(isEnglish))
                }
            },
            dismissButton = {
                TextButton(onClick = { goalToDelete = null }) {
                    Text(AppLocale.cancel(isEnglish))
                }
            }
        )
    }

    // Savings Goal History Dialog
    goalForHistory?.let { goal ->
        SavingsGoalHistoryDialog(
            goal = goal,
            isEnglish = isEnglish,
            onDismiss = { goalForHistory = null }
        )
    }
}

@Composable
fun GoalCardItem(
    goal: SavingsGoalEntity,
    isEnglish: Boolean = false,
    onAddDeposit: () -> Unit,
    onWithdraw: () -> Unit = {},
    onViewHistory: () -> Unit = {},
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val progress = if (goal.targetAmount > 0) (goal.savedAmount / goal.targetAmount).toFloat().coerceIn(0f, 1f) else 0f
    val isCompleted = goal.isCompleted || (goal.targetAmount > 0 && goal.savedAmount >= goal.targetAmount)
    val isClosed = isCompleted && goal.savedAmount <= 0.0

    val progressColor = when {
        isClosed -> Color(0xFF10B981) // Emerald Green - Fully Finished & Achieved
        isCompleted -> Color(0xFF10B981) // Emerald Green
        progress > 0.6f -> MaterialTheme.colorScheme.primary
        else -> Color(0xFFF57C00)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("goal_item_${goal.id}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = if (isClosed || isCompleted) AppCardDefaults.incomeBorder()
        else AppCardDefaults.border(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(progressColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isClosed || isCompleted) Icons.Default.CheckCircle else Icons.Default.AccountBalance,
                            contentDescription = null,
                            tint = progressColor,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = goal.title,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (goal.note.isNotBlank()) {
                            Text(
                                text = goal.note,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                if (isClosed || isCompleted) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF10B981).copy(alpha = 0.15f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.4f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF047857),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isEnglish) "Goal Achieved" else "লক্ষ্য অর্জিত",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF047857)
                                )
                            )
                        }
                    }
                }
            }

            // Progress Bar & Figures
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isClosed) {
                        // Closed / Withdrawn state: "jomeche part ta bad jabe...lokkho te joto takar lokkho seta uthbe and 100% likha thakbe pashe"
                        Text(
                            text = "${if (isEnglish) "Target: " else "লক্ষ্য: "}${formatTakaSafe(goal.targetAmount)}",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isEnglish) "100% Achieved" else "১০০% অর্জিত",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF047857)
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    } else {
                        Text(
                            text = "${if (isEnglish) "Saved: " else "জমেছে: "}${formatTakaSafe(goal.savedAmount)}",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${if (isEnglish) "Target: " else "লক্ষ্য: "}${formatTakaSafe(goal.targetAmount)} (${String.format(Locale.US, "%.0f", progress * 100)}%)",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = progressColor
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                LinearProgressIndicator(
                    progress = { if (isClosed) 1f else progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(5.dp)),
                    color = progressColor,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isClosed) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF10B981).copy(alpha = 0.15f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.4f)),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = Color(0xFF047857)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isEnglish) "Goal Achieved" else "লক্ষ্য অর্জিত",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.5.sp,
                                    color = Color(0xFF047857)
                                )
                            }
                        }
                    } else if (!isCompleted) {
                        Button(
                            onClick = onAddDeposit,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = progressColor),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier.height(34.dp).testTag("btn_deposit_${goal.id}")
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(if (isEnglish) "Deposit" else "জমা", fontWeight = FontWeight.Bold, fontSize = 11.5.sp)
                        }
                    } else {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF10B981).copy(alpha = 0.15f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.4f)),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = Color(0xFF047857)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isEnglish) "Goal Locked" else "লক্ষ্য অর্জিত",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.5.sp,
                                    color = Color(0xFF047857)
                                )
                            }
                        }
                    }

                    if (!isClosed && goal.savedAmount > 0) {
                        OutlinedButton(
                            onClick = onWithdraw,
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier.height(34.dp).testTag("btn_withdraw_${goal.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.RemoveCircleOutline,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = if (isEnglish) "Withdraw" else "উত্তোলন",
                                fontSize = 11.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = onViewHistory,
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                        modifier = Modifier.height(34.dp).testTag("btn_history_${goal.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = if (isEnglish) "History" else "ইতিহাস",
                            fontSize = 11.5.sp,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(34.dp).testTag("btn_delete_goal_${goal.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun SavingsGoalHistoryDialog(
    goal: SavingsGoalEntity,
    isEnglish: Boolean,
    onDismiss: () -> Unit
) {
    val historyItems = remember(goal.historyJson) {
        goal.parseHistory().sortedByDescending { it.timestamp }
    }
    val dateFormat = remember { java.text.SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()) }
    val isCompleted = goal.isCompleted || (goal.targetAmount > 0 && goal.savedAmount >= goal.targetAmount)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = goal.title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = if (isEnglish) "Transaction History" else "জমা ও উত্তোলনের ইতিহাস",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (isCompleted) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF10B981).copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = if (isEnglish) "Completed" else "সম্পন্ন",
                            color = Color(0xFF047857),
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Summary bar
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = if (isEnglish) "Total Saved" else "মোট সঞ্চয়",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = formatTakaSafe(goal.savedAmount),
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = if (isEnglish) "Target Goal" else "লক্ষ্যমাত্রা",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = formatTakaSafe(goal.targetAmount),
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }

                if (historyItems.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 28.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.outlineVariant,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = if (isEnglish) "No transactions recorded yet" else "এখনো কোনো জমার ইতিহাস নেই",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(historyItems, key = { it.id }) { item ->
                            val isDeposit = item.type == "DEPOSIT"
                            val badgeColor = if (isDeposit) Color(0xFF10B981) else Color(0xFFF59E0B)

                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(32.dp)
                                                .clip(CircleShape)
                                                .background(badgeColor.copy(alpha = 0.15f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = if (isDeposit) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                                                contentDescription = null,
                                                tint = badgeColor,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = if (isDeposit) (if (isEnglish) "Deposit" else "টাকা জমা")
                                                       else (if (isEnglish) "Withdrawal" else "টাকা উত্তোলন"),
                                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = dateFormat.format(Date(item.timestamp)),
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            if (!item.note.isNullOrBlank()) {
                                                Text(
                                                    text = item.note,
                                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "${if (isDeposit) "+" else "-"}${formatTakaSafe(item.amount)}",
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = badgeColor
                                            )
                                        )
                                        Text(
                                            text = "${if (isEnglish) "Bal: " else "অবশিষ্ট: "}${formatTakaSafe(item.balanceAfter)}",
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(if (isEnglish) "Close" else "বন্ধ করুন")
            }
        }
    )
}

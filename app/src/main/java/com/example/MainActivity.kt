package com.example

import android.app.Activity
import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.AppToastManager
import com.example.ui.components.PremiumToastItem
import com.example.ui.components.ToastEvent
import com.example.ui.util.clearFocusOnTap
import com.example.ui.screens.AddTransactionDialog
import com.example.ui.screens.AppLockScreen
import com.example.ui.screens.AppSplashScreen
import com.example.ui.screens.CalculatorScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.DebtsScreen
import com.example.ui.screens.MonthlyOverviewScreen
import com.example.ui.screens.SavingsGoalsScreen
import com.example.ui.screens.SignInScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.ExpenseViewModel
import com.example.util.AppLockManager

class MainActivity : FragmentActivity() {

    override fun onStop() {
        super.onStop()
        AppLockManager.getInstance(this).lock()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val expenseViewModel: ExpenseViewModel = viewModel()
            val feedbackMessage by expenseViewModel.feedbackMessage.collectAsState()
            val currentNavIndex by expenseViewModel.currentNavIndex.collectAsState()
            val isUserLoggedIn by expenseViewModel.isUserLoggedIn.collectAsState()
            val currentUserId by expenseViewModel.currentUserId.collectAsState()
            val userName by expenseViewModel.userName.collectAsState()
            val userPhone by expenseViewModel.userPhone.collectAsState()
            val isDarkMode by expenseViewModel.isDarkMode.collectAsState()
            val isEnglish by expenseViewModel.isEnglish.collectAsState()
            val isAppLoading by expenseViewModel.isAppLoading.collectAsState()
            val context = LocalContext.current
            val focusManager = LocalFocusManager.current
            val appLockManager = remember { AppLockManager.getInstance(context) }
            val isAppLockActive by appLockManager.isLocked.collectAsState()

            // Keep App Lock account isolation synchronized with the active session
            LaunchedEffect(currentUserId, isUserLoggedIn) {
                if (isUserLoggedIn && currentUserId > 0) {
                    appLockManager.setActiveUser(currentUserId)
                } else {
                    appLockManager.setActiveUser(-1L)
                }
            }

            var showSplashScreen by remember { mutableStateOf(true) }
            var showAddTransactionDialog by remember { mutableStateOf(false) }
            var showExitConfirmationDialog by remember { mutableStateOf(false) }
            val snackbarHostState = remember { SnackbarHostState() }

            // Back navigation: handle splash screen, lock screen, reversely traverse visited screens, or ask before exiting app
            BackHandler {
                if (showSplashScreen) {
                    // Do nothing while splash screen is displaying
                } else if (isAppLockActive) {
                    (context as? Activity)?.finish()
                } else if (showAddTransactionDialog) {
                    showAddTransactionDialog = false
                } else if (isUserLoggedIn) {
                    val navigatedBack = expenseViewModel.popNavBackStack()
                    if (!navigatedBack) {
                        showExitConfirmationDialog = true
                    }
                } else {
                    showExitConfirmationDialog = true
                }
            }

            var currentToast by remember { mutableStateOf<ToastEvent?>(null) }

            // Listen to singleton AppToastManager events
            LaunchedEffect(Unit) {
                AppToastManager.toastEvents.collect { event ->
                    currentToast = event
                }
            }

            // Listen to ViewModel feedbackMessage and forward to premium toast
            LaunchedEffect(feedbackMessage) {
                feedbackMessage?.let { msg ->
                    val showUndo = msg.contains("যোগ হয়েছে") || msg.contains("added") || msg.contains("New expense") || msg.contains("New income")
                    currentToast = ToastEvent(
                        message = msg,
                        actionLabel = if (showUndo) (if (isEnglish) "Undo" else "বাতিল") else null,
                        onAction = if (showUndo) { { expenseViewModel.undoLastAddedTransaction() } } else null
                    )
                    expenseViewModel.clearFeedback()
                }
            }

            // Play notification sound and auto-dismiss after standard 3600ms
            LaunchedEffect(currentToast) {
                if (currentToast != null) {
                    com.example.ui.util.ToastSoundPlayer.playNotificationSound(context)
                    kotlinx.coroutines.delay(3600)
                    currentToast = null
                }
            }

            MyApplicationTheme(darkTheme = isDarkMode) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(currentToast) {
                            if (currentToast != null) {
                                awaitPointerEventScope {
                                    while (true) {
                                        val event = awaitPointerEvent(PointerEventPass.Initial)
                                        if (event.changes.any { it.pressed }) {
                                            // Pressing anywhere on screen dismisses toast immediately!
                                            currentToast = null
                                            break
                                        }
                                    }
                                }
                            }
                        }
                ) {
                    // Global wrapper: Dismiss soft keyboard on outside tap
                    Surface(
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                focusManager.clearFocus()
                            },
                        color = MaterialTheme.colorScheme.background
                    ) {
                    if (isAppLockActive) {
                        AppLockScreen(
                            appLockManager = appLockManager,
                            isEnglish = isEnglish,
                            onUnlocked = {
                                appLockManager.unlock()
                            }
                        )
                    } else if (!isUserLoggedIn) {
                        SignInScreen(
                            viewModel = expenseViewModel
                        )
                    } else {
                        Scaffold(
                            modifier = Modifier
                                .fillMaxSize()
                                .clearFocusOnTap(),
                            snackbarHost = { },
                    bottomBar = {
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surface,
                            tonalElevation = 6.dp,
                            modifier = Modifier.testTag("main_navigation_bar")
                        ) {
                            // 0: দৈনিক / Daily
                            NavigationBarItem(
                                selected = currentNavIndex == 0,
                                onClick = { expenseViewModel.setNavIndex(0) },
                                icon = {
                                    Icon(imageVector = Icons.Default.ReceiptLong, contentDescription = if (isEnglish) "Daily" else "দৈনিক")
                                },
                                label = {
                                    Text(
                                        text = if (isEnglish) "Daily" else "দৈনিক",
                                        fontWeight = if (currentNavIndex == 0) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 11.sp
                                    )
                                },
                                modifier = Modifier.testTag("nav_daily_tab")
                            )

                            // 1: ধার-দেনা / Debts
                            NavigationBarItem(
                                selected = currentNavIndex == 1,
                                onClick = { expenseViewModel.setNavIndex(1) },
                                icon = {
                                    Icon(imageVector = Icons.Default.Handshake, contentDescription = if (isEnglish) "Debts" else "ধার-দেনা")
                                },
                                label = {
                                    Text(
                                        text = if (isEnglish) "Debts" else "ধার-দেনা",
                                        fontWeight = if (currentNavIndex == 1) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 11.sp
                                    )
                                },
                                modifier = Modifier.testTag("nav_debts_tab")
                            )

                            // 2: ক্যালকুলেটর / Calculator
                            NavigationBarItem(
                                selected = currentNavIndex == 2,
                                onClick = { expenseViewModel.setNavIndex(2) },
                                icon = {
                                    Icon(imageVector = Icons.Default.Calculate, contentDescription = if (isEnglish) "Calculator" else "ক্যালকুলেটর")
                                },
                                label = {
                                    Text(
                                        text = if (isEnglish) "Calc" else "হিসাব",
                                        fontWeight = if (currentNavIndex == 2) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 11.sp
                                    )
                                },
                                modifier = Modifier.testTag("nav_calculator_tab")
                            )

                            // 3: সঞ্চয় লক্ষ্য / Savings Goals
                            NavigationBarItem(
                                selected = currentNavIndex == 3,
                                onClick = { expenseViewModel.setNavIndex(3) },
                                icon = {
                                    Icon(imageVector = Icons.Default.AccountBalance, contentDescription = if (isEnglish) "Savings" else "সঞ্চয় লক্ষ্য")
                                },
                                label = {
                                    Text(
                                        text = if (isEnglish) "Savings" else "সঞ্চয় লক্ষ্য",
                                        fontWeight = if (currentNavIndex == 3) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 11.sp,
                                        maxLines = 1
                                    )
                                },
                                modifier = Modifier.testTag("nav_savings_tab")
                            )

                            // 4: মাসিক বিশ্লেষণ / Monthly Overview
                            NavigationBarItem(
                                selected = currentNavIndex == 4,
                                onClick = { expenseViewModel.setNavIndex(4) },
                                icon = {
                                    Icon(imageVector = Icons.Default.Insights, contentDescription = if (isEnglish) "Monthly" else "মাসিক")
                                },
                                label = {
                                    Text(
                                        text = if (isEnglish) "Monthly" else "মাসিক",
                                        fontWeight = if (currentNavIndex == 4) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 11.sp
                                    )
                                },
                                modifier = Modifier.testTag("nav_monthly_tab")
                            )
                        }
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .clearFocusOnTap()
                    ) {
                        when (currentNavIndex) {
                            0 -> DashboardScreen(
                                viewModel = expenseViewModel,
                                onOpenAddTransaction = { showAddTransactionDialog = true }
                            )
                            1 -> DebtsScreen(
                                viewModel = expenseViewModel
                            )
                            2 -> CalculatorScreen(
                                viewModel = expenseViewModel,
                                onAddExpenseFromCalc = { amount ->
                                    expenseViewModel.quickAddFromCalculator(amount, "EXPENSE")
                                },
                                onAddIncomeFromCalc = { amount ->
                                    expenseViewModel.quickAddFromCalculator(amount, "INCOME")
                                }
                            )
                            3 -> SavingsGoalsScreen(
                                viewModel = expenseViewModel
                            )
                            4 -> MonthlyOverviewScreen(
                                viewModel = expenseViewModel
                            )
                        }

                        if (showAddTransactionDialog) {
                            val currentKhats by expenseViewModel.khats.collectAsState()
                            val currentOboshisto by expenseViewModel.oboshistoBalance.collectAsState()
                            val currentNetBalance by expenseViewModel.netBalance.collectAsState()
                            AddTransactionDialog(
                                isEnglish = isEnglish,
                                khats = currentKhats,
                                oboshistoBalance = currentOboshisto,
                                netBalance = currentNetBalance,
                                onDismiss = { showAddTransactionDialog = false },
                                onSave = { title, amount, type, category, note, khatId, khatName, timestamp ->
                                    expenseViewModel.addTransaction(
                                        title = title,
                                        amount = amount,
                                        type = type,
                                        category = category,
                                        note = note,
                                        timestamp = timestamp,
                                        khatId = khatId,
                                        khatName = khatName
                                    )
                                }
                            )
                        }
                    }
                }
            }

            // App Exit Confirmation Dialog
            if (showExitConfirmationDialog) {
                AlertDialog(
                    onDismissRequest = { showExitConfirmationDialog = false },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.ExitToApp,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                    },
                    title = {
                        Text(
                            text = if (isEnglish) "Exit the app?" else "অ্যাপ থেকে প্রস্থান করবেন?",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                    },
                    text = {
                        Text(
                            text = if (isEnglish) "Are you sure you want to exit the app?" else "আপনি কি নিশ্চিত যে আপনি অ্যাপ থেকে বের হতে চান?",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                showExitConfirmationDialog = false
                                (context as? Activity)?.finish()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            ),
                            modifier = Modifier.testTag("btn_confirm_exit")
                        ) {
                            Text(if (isEnglish) "Yes, Exit" else "হ্যাঁ, বের হন")
                        }
                    },
                    dismissButton = {
                        OutlinedButton(
                            onClick = { showExitConfirmationDialog = false },
                            modifier = Modifier.testTag("btn_cancel_exit")
                        ) {
                            Text(if (isEnglish) "No, Stay" else "না, থাকুন")
                        }
                    }
                )
            }
        }

                    // Floating Premium Toast HUD at top
                    AnimatedVisibility(
                        visible = currentToast != null,
                        enter = slideInVertically(initialOffsetY = { -it }, animationSpec = tween(220)) + fadeIn(animationSpec = tween(200)),
                        exit = slideOutVertically(targetOffsetY = { -it }, animationSpec = tween(180)) + fadeOut(animationSpec = tween(160)),
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .statusBarsPadding()
                            .padding(top = 10.dp)
                            .zIndex(9999f)
                    ) {
                        currentToast?.let { toast ->
                            PremiumToastItem(
                                message = toast.message,
                                actionLabel = toast.actionLabel,
                                onAction = toast.onAction,
                                onDismiss = { currentToast = null }
                            )
                        }
                    }

                    // App Launch Splash Animation Overlay
                    AnimatedVisibility(
                        visible = showSplashScreen,
                        enter = fadeIn(animationSpec = tween(150)),
                        exit = fadeOut(animationSpec = tween(400)) +
                                scaleOut(targetScale = 1.05f, animationSpec = tween(400)),
                        modifier = Modifier
                            .fillMaxSize()
                            .zIndex(900f)
                    ) {
                        AppSplashScreen(
                            isAppLoading = isAppLoading,
                            isDarkMode = isDarkMode,
                            isEnglish = isEnglish,
                            onAnimationFinished = {
                                showSplashScreen = false
                            }
                        )
                    }
                }
            }
        }
    }
}

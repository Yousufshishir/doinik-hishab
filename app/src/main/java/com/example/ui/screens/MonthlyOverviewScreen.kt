package com.example.ui.screens

import com.example.ui.util.clearFocusOnTap

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import com.example.ui.components.AppToastManager
import com.example.ui.components.AppCardDefaults
import com.example.ui.components.KeyboardScrollDownHint
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.SimpleCategoryData
import com.example.data.model.TransactionEntity
import com.example.data.model.formatTakaCompact
import com.example.data.model.formatTakaSafe
import com.example.ui.viewmodel.ExpenseViewModel
import java.util.*
import kotlin.math.max

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonthlyOverviewScreen(
    viewModel: ExpenseViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val selectedMonthCalendar by viewModel.selectedMonthCalendar.collectAsState()
    val monthlyIncome by viewModel.monthlyIncome.collectAsState()
    val monthlyExpense by viewModel.monthlyExpense.collectAsState()
    val monthlySavings by viewModel.monthlySavings.collectAsState()
    val monthlySavingsRate by viewModel.monthlySavingsRate.collectAsState()
    val monthlyDailyAverage by viewModel.monthlyDailyAverage.collectAsState()
    val categoryBreakdown by viewModel.monthlyCategoryBreakdown.collectAsState()
    val monthlyBudget by viewModel.monthlyBudget.collectAsState()
    val safeDailySpending by viewModel.safeDailySpending.collectAsState()
    val weeklyTrend by viewModel.weeklyTrend.collectAsState()
    val isEnglish by viewModel.isEnglish.collectAsState()

    val previousMonthExpense by viewModel.previousMonthExpense.collectAsState()
    val monthOverMonthDiff by viewModel.monthOverMonthDiff.collectAsState()
    val monthOverMonthPercentage by viewModel.monthOverMonthPercentage.collectAsState()
    val projectedMonthExpense by viewModel.projectedMonthExpense.collectAsState()
    val peakSpendingDay by viewModel.peakSpendingDay.collectAsState()
    val monthDailyExpenseMap by viewModel.monthDailyExpenseMap.collectAsState()
    val financialHealthScore by viewModel.financialHealthScore.collectAsState()
    val allTransactions by viewModel.transactions.collectAsState()

    var showBudgetDialog by remember { mutableStateOf(false) }
    var showMonthPickerModal by remember { mutableStateOf(false) }
    var selectedCalendarDayExpense by remember { mutableStateOf<Pair<Int, Double>?>(null) }

    val monthNamesBn = arrayOf(
        "জানুয়ারি", "ফেব্রুয়ারি", "মার্চ", "এপ্রিল", "মে", "জুন",
        "জুলাই", "আগস্ট", "সেপ্টেম্বর", "অক্টোবর", "নভেম্বর", "ডিসেম্বর"
    )
    val monthNamesEn = arrayOf(
        "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December"
    )
    val monthYearText = if (isEnglish) {
        "${monthNamesEn[selectedMonthCalendar.get(Calendar.MONTH)]} ${selectedMonthCalendar.get(Calendar.YEAR)}"
    } else {
        "${monthNamesBn[selectedMonthCalendar.get(Calendar.MONTH)]} ${selectedMonthCalendar.get(Calendar.YEAR)}"
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .clearFocusOnTap(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (isEnglish) "Monthly Overview" else "মাসিক পর্যালোচনা",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = if (isEnglish) "Comprehensive budget & expense analysis" else "বাজেট ও খরচের সামগ্রিক বিশ্লেষণ",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(2.dp)) }

            // Month Navigator Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("month_selector_card"),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    border = AppCardDefaults.border(MaterialTheme.colorScheme.primary)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        IconButton(
                            onClick = { viewModel.previousMonth() },
                            modifier = Modifier.testTag("btn_prev_month")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = if (isEnglish) "Previous Month" else "পূর্ববর্তী মাস"
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Surface(
                                onClick = { showMonthPickerModal = true },
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                                modifier = Modifier.testTag("btn_open_month_picker")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CalendarMonth,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = monthYearText,
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = "Select Month",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            val now = Calendar.getInstance()
                            val isCurrentMonth = selectedMonthCalendar.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
                                selectedMonthCalendar.get(Calendar.MONTH) == now.get(Calendar.MONTH)

                            if (!isCurrentMonth) {
                                Text(
                                    text = if (isEnglish) "Back to Current Month" else "চলতি মাসে ফিরুন",
                                    style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold),
                                    modifier = Modifier
                                        .clickable { viewModel.resetToCurrentMonth() }
                                        .padding(top = 4.dp)
                                )
                            }
                        }

                        IconButton(
                            onClick = { viewModel.nextMonth() },
                            modifier = Modifier.testTag("btn_next_month")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = if (isEnglish) "Next Month" else "পরবর্তী মাস"
                            )
                        }
                    }
                }
            }

            // 4-Quadrant Monthly Summary Grid
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Monthly Income Card
                        Card(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFF2E7D32).copy(alpha = 0.12f)
                            ),
                            border = AppCardDefaults.incomeBorder()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.TrendingUp,
                                        contentDescription = null,
                                        tint = Color(0xFF2E7D32),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (isEnglish) "Total Income" else "মোট আয়",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = Color(0xFF2E7D32),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = formatTakaSafe(monthlyIncome),
                                    style = if (monthlyIncome >= 10_000_000) MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFF1B5E20)
                                    ) else MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFF1B5E20)
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        // Monthly Expense Card
                        Card(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFFD32F2F).copy(alpha = 0.12f)
                            ),
                            border = AppCardDefaults.expenseBorder()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.TrendingDown,
                                        contentDescription = null,
                                        tint = Color(0xFFD32F2F),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (isEnglish) "Total Expense" else "মোট খরচ",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = Color(0xFFD32F2F),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = formatTakaSafe(monthlyExpense),
                                    style = if (monthlyExpense >= 10_000_000) MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFFB71C1C)
                                    ) else MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFFB71C1C)
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Monthly Net Savings
                        Card(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                            ),
                            border = AppCardDefaults.border(Color(0xFF0284C7))
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = if (isEnglish) "Net Savings" else "নিট সঞ্চয় (Savings)",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = formatTakaSafe(monthlySavings),
                                    style = if (kotlin.math.abs(monthlySavings) >= 10_000_000) MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (monthlySavings >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                                    ) else MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (monthlySavings >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (isEnglish) "Savings Rate: ${String.format(Locale.US, "%.1f", monthlySavingsRate)}%" else "সঞ্চয়ের হার: ${String.format(Locale.US, "%.1f", monthlySavingsRate)}%",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Daily Average Expense
                        Card(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            ),
                            border = AppCardDefaults.border(Color(0xFFF59E0B))
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = if (isEnglish) "Daily Avg. Expense" else "দৈনিক গড় খরচ",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = formatTakaSafe(monthlyDailyAverage),
                                    style = if (monthlyDailyAverage >= 10_000_000) MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    ) else MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (isEnglish) "Daily average spending" else "প্রতিদিনের গড় ব্যয়",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // 1. Financial Health Scorecard (0-100 Score with Visual Meter)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("financial_health_card"),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = AppCardDefaults.border(Color(0xFF10B981)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier.size(36.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(imageVector = Icons.Default.Speed, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = if (isEnglish) "Financial Health Score" else "আর্থিক শৃঙ্খলা ও স্বাস্থ্য স্কোর",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Text(
                                        text = if (isEnglish) "Monthly habit & budget evaluation" else "বাজেট অনুশাসন ও অভ্যাস মূল্যায়ন",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            val hasNoData = monthlyIncome <= 0.0 && monthlyExpense <= 0.0
                            if (hasNoData) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                ) {
                                    Text(
                                        text = if (isEnglish) "Awaiting Data" else "ডাটা অপেক্ষমান",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant),
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            } else {
                                // Score pill badge
                                val scoreColor = when {
                                    financialHealthScore >= 80 -> Color(0xFF2E7D32)
                                    financialHealthScore >= 60 -> Color(0xFFF57C00)
                                    else -> MaterialTheme.colorScheme.error
                                }
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = scoreColor.copy(alpha = 0.12f),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, scoreColor.copy(alpha = 0.4f))
                                ) {
                                    Text(
                                        text = "$financialHealthScore / 100",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold, color = scoreColor),
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }

                        val hasNoData = monthlyIncome <= 0.0 && monthlyExpense <= 0.0
                        if (hasNoData) {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.EmojiObjects,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = if (isEnglish) "Start Tracking to Unlock Your Score" else "হিসাব রাখা শুরু করলেই স্কোর পাবেন",
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    Text(
                                        text = if (isEnglish)
                                            "Your financial health score evaluates your savings discipline and budget adherence. Once you record your income and expenses, your personalized score will appear here automatically."
                                        else
                                            "আপনার খরচ নিয়ন্ত্রণ ও সঞ্চয় অভ্যাসের ওপর ভিত্তি করে এই স্কোর হিসাব করা হয়। হোম পেজ থেকে দৈনন্দিন আয় ও খরচের হিসাব এন্ট্রি করলেই এখানে স্বয়ংক্রিয়ভাবে আপনার স্বাস্থ্য স্কোর ও পরামর্শ দেখতে পাবেন।",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        lineHeight = 18.sp
                                    )
                                    HorizontalDivider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text("1️⃣", fontSize = 12.sp)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = if (isEnglish) "Log daily expenses & income on Home" else "হোম পেজ থেকে নিয়মিত আয় ও ব্যয় এন্ট্রি করুন",
                                                style = MaterialTheme.typography.labelMedium
                                            )
                                        }
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text("2️⃣", fontSize = 12.sp)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = if (isEnglish) "Set a monthly budget to control outflow" else "মাসিক বাজেট নির্ধারণ করে অপচয় কমান",
                                                style = MaterialTheme.typography.labelMedium
                                            )
                                        }
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text("3️⃣", fontSize = 12.sp)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = if (isEnglish) "Maintain healthy savings to boost score" else "নিয়মিত সঞ্চয় বজায় রেখে স্কোর বৃদ্ধি করুন",
                                                style = MaterialTheme.typography.labelMedium
                                            )
                                        }
                                    }
                                }
                            }
                        } else {
                            // Linear progress with meter
                            LinearProgressIndicator(
                                progress = { (financialHealthScore / 100f).coerceIn(0f, 1f) },
                                modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp)),
                                color = when {
                                    financialHealthScore >= 80 -> Color(0xFF2E7D32)
                                    financialHealthScore >= 60 -> Color(0xFFF57C00)
                                    else -> MaterialTheme.colorScheme.error
                                },
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )

                            // Meaning label
                            val healthStatus = when {
                                financialHealthScore >= 80 -> if (isEnglish) "🌟 Excellent Financial Discipline! You are managing savings and budget exceptionally well." else "🌟 চমৎকার আর্থিক শৃঙ্খলা! সঞ্চয় ও খরচের চমৎকার ভারসাম্য বজায় রয়েছে।"
                                financialHealthScore >= 60 -> if (isEnglish) "👍 Good Standing. Keep monitoring daily micro-expenses to improve savings further." else "👍 সন্তোষজনক অবস্থা। ছোট ছোট খরচে রাশ টানলে সঞ্চয় আরও বৃদ্ধি পাবে।"
                                else -> if (isEnglish) "⚠️ Needs Caution. Expenses are approaching limits or exceeding income. Consider reviewing non-essentials." else "⚠️ সতর্ক থাকুন! খরচের পরিমাণ আয় ও বাজেটের সীমার কাছাকাছি বা ছাড়িয়ে গেছে।"
                            }
                            Text(
                                text = healthStatus,
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            // 2. Month-over-Month Comparison & Trend Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("mom_comparison_card"),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = AppCardDefaults.border(Color(0xFF8B5CF6)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier.size(36.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(imageVector = Icons.Default.CompareArrows, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = if (isEnglish) "Month-over-Month Comparison" else "আগের মাসের সাথে তুলনা",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Text(
                                        text = if (isEnglish) "Comparison against previous month" else "পূর্ববর্তী মাসের মোট খরচের সাথে তুলনা",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        // Comparison Stats Box
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = if (isEnglish) "Previous Month Total" else "পূর্ববর্তী মাসের মোট খরচ",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = formatTakaSafe(previousMonthExpense),
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                        )
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = if (isEnglish) "This Month So Far" else "চলতি মাসে এ পর্যন্ত",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = formatTakaSafe(monthlyExpense),
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                                        )
                                    }
                                }

                                // Comparative visual bars
                                val maxComp = max(1.0, max(previousMonthExpense, monthlyExpense))
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = if (isEnglish) "Prev" else "আগে",
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                            modifier = Modifier.width(32.dp),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        LinearProgressIndicator(
                                            progress = { (previousMonthExpense / maxComp).toFloat().coerceIn(0f, 1f) },
                                            modifier = Modifier.weight(1f).height(7.dp).clip(RoundedCornerShape(4.dp)),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                                        )
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = if (isEnglish) "Now" else "এখন",
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                                            modifier = Modifier.width(32.dp),
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        LinearProgressIndicator(
                                            progress = { (monthlyExpense / maxComp).toFloat().coerceIn(0f, 1f) },
                                            modifier = Modifier.weight(1f).height(7.dp).clip(RoundedCornerShape(4.dp)),
                                            color = MaterialTheme.colorScheme.primary,
                                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                                        )
                                    }
                                }

                                // Change summary pill
                                if (previousMonthExpense > 0) {
                                    val isLess = monthOverMonthDiff <= 0
                                    val badgeColor = if (isLess) Color(0xFF2E7D32) else Color(0xFFD32F2F)
                                    val diffText = if (isLess) {
                                        if (isEnglish) "🎉 ৳%.0f less spent than last month (-%.1f%%)".format(Locale.US, -monthOverMonthDiff, kotlin.math.abs(monthOverMonthPercentage))
                                        else "🎉 গত মাসের চেয়ে ${formatTakaSafe(-monthOverMonthDiff)} কম ব্যয় হয়েছে (-%.1f%%)".format(Locale.US, kotlin.math.abs(monthOverMonthPercentage))
                                    } else {
                                        if (isEnglish) "⚠️ ৳%.0f more spent than last month (+%.1f%%)".format(Locale.US, monthOverMonthDiff, monthOverMonthPercentage)
                                        else "⚠️ গত মাসের চেয়ে ${formatTakaSafe(monthOverMonthDiff)} বেশি ব্যয় হয়েছে (+%.1f%%)".format(Locale.US, monthOverMonthPercentage)
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = badgeColor.copy(alpha = 0.12f),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, badgeColor.copy(alpha = 0.3f))
                                    ) {
                                        Text(
                                            text = diffText,
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = badgeColor),
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                } else {
                                    Text(
                                        text = if (isEnglish) "ℹ️ First month recording comparison data." else "ℹ️ এই মাসেই প্রথম তুলনামূলক তথ্য রেকর্ড করা হচ্ছে।",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        // Projected Month End Spending & Peak Spending Day
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Projected Month End
                            Surface(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(imageVector = Icons.Default.Timeline, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(15.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = if (isEnglish) "Projected Total" else "মাস শেষে সম্ভাব্য খরচ",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.5.sp),
                                            color = MaterialTheme.colorScheme.primary,
                                            maxLines = 1
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = formatTakaSafe(projectedMonthExpense),
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = if (isEnglish) "Based on daily burn rate" else "বর্তমান খরচের গতির ভিত্তিতে",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            // Peak Spending Day
                            Surface(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFFF57C00).copy(alpha = 0.08f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF57C00).copy(alpha = 0.25f))
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(imageVector = Icons.Default.Whatshot, contentDescription = null, tint = Color(0xFFF57C00), modifier = Modifier.size(15.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = if (isEnglish) "Peak Spend Day" else "সর্বোচ্চ খরচের দিন",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.5.sp),
                                            color = Color(0xFFF57C00),
                                            maxLines = 1
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    if (peakSpendingDay != null && peakSpendingDay!!.second > 0) {
                                        Text(
                                            text = "${peakSpendingDay!!.first} ${monthNamesBn[selectedMonthCalendar.get(Calendar.MONTH)]}: ${formatTakaSafe(peakSpendingDay!!.second)}",
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = Color(0xFFF57C00), fontSize = 11.5.sp),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = if (isEnglish) "Highest single-day spike" else "মাসের সবচেয়ে বেশি ব্যয়ের দিন",
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    } else {
                                        Text(
                                            text = if (isEnglish) "No expense spikes" else "কোনো স্পাইক নেই",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 3. Full Month Daily Spending Heatmap & No-Spend Day Tracker
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("month_daily_calendar_heatmap_card"),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = AppCardDefaults.border(Color(0xFF3B82F6)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier.size(36.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(imageVector = Icons.Default.CalendarMonth, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = if (isEnglish) "Monthly Spending Calendar" else "পুরো মাসের খরচের ক্যালেন্ডার",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Text(
                                        text = if (isEnglish) "Daily expense heatmap & zero-spend days" else "প্রতিদিনের খরচের তীব্রতা ও শূন্য-খরচ দিন",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        val maxDaysInMonth = selectedMonthCalendar.getActualMaximum(Calendar.DAY_OF_MONTH)
                        val zeroSpendDays = (1..maxDaysInMonth).count { day ->
                            (monthDailyExpenseMap[day] ?: 0.0) == 0.0
                        }

                        // Zero-Spend Achievement Banner
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF2E7D32).copy(alpha = 0.1f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2E7D32).copy(alpha = 0.25f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(text = "🎯", fontSize = 16.sp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (isEnglish) "No-Spend Days Achieved:" else "খরচহীন দিন সাফল্য (No-Spend):",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                                    )
                                }
                                Text(
                                    text = if (isEnglish) "$zeroSpendDays Days" else "$zeroSpendDays দিন",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold, color = Color(0xFF2E7D32))
                                )
                            }
                        }

                        // Calendar Weekday Headers
                        val weekdaysBn = arrayOf("রবি", "সোম", "মঙ্গল", "বুধ", "বৃহঃ", "শুক্র", "শনি")
                        val weekdaysEn = arrayOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                            for (i in 0..6) {
                                Text(
                                    text = if (isEnglish) weekdaysEn[i] else weekdaysBn[i],
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (i == 5) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 11.sp
                                    ),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        // Month Grid Generation
                        val firstDayCal = (selectedMonthCalendar.clone() as Calendar).apply {
                            set(Calendar.DAY_OF_MONTH, 1)
                        }
                        val firstDayOfWeek = firstDayCal.get(Calendar.DAY_OF_WEEK) - 1
                        val totalCells = firstDayOfWeek + maxDaysInMonth
                        val numRows = (totalCells + 6) / 7

                        val avgExpense = if (monthlyDailyAverage > 0) monthlyDailyAverage else 500.0

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            for (rowIndex in 0 until numRows) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    for (colIndex in 0..6) {
                                        val cellIndex = rowIndex * 7 + colIndex
                                        val dayNumber = cellIndex - firstDayOfWeek + 1

                                        if (dayNumber in 1..maxDaysInMonth) {
                                            val daySpent = monthDailyExpenseMap[dayNumber] ?: 0.0
                                            val isSelected = selectedCalendarDayExpense?.first == dayNumber

                                            val cellBg = when {
                                                daySpent == 0.0 -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                                daySpent > avgExpense * 1.5 -> Color(0xFFD32F2F).copy(alpha = 0.25f)
                                                daySpent > avgExpense * 0.8 -> Color(0xFFF57C00).copy(alpha = 0.22f)
                                                else -> Color(0xFF2E7D32).copy(alpha = 0.20f)
                                            }

                                            val cellBorder = when {
                                                isSelected -> MaterialTheme.colorScheme.primary
                                                daySpent == 0.0 -> Color.Transparent
                                                daySpent > avgExpense * 1.5 -> Color(0xFFD32F2F).copy(alpha = 0.5f)
                                                else -> Color(0xFF2E7D32).copy(alpha = 0.4f)
                                            }

                                            Surface(
                                                onClick = {
                                                    selectedCalendarDayExpense = if (isSelected) null else Pair(dayNumber, daySpent)
                                                },
                                                modifier = Modifier.weight(1f).height(38.dp),
                                                shape = RoundedCornerShape(8.dp),
                                                color = cellBg,
                                                border = androidx.compose.foundation.BorderStroke(if (isSelected) 2.dp else 1.dp, cellBorder)
                                            ) {
                                                Column(
                                                    modifier = Modifier.fillMaxSize().padding(2.dp),
                                                    horizontalAlignment = Alignment.CenterHorizontally,
                                                    verticalArrangement = Arrangement.Center
                                                ) {
                                                    Text(
                                                        text = "$dayNumber",
                                                        style = MaterialTheme.typography.labelSmall.copy(
                                                            fontWeight = if (isSelected || daySpent > 0) FontWeight.Bold else FontWeight.Normal,
                                                            fontSize = 11.sp,
                                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                                        )
                                                    )
                                                    if (daySpent > 0) {
                                                        Box(
                                                            modifier = Modifier.size(4.dp).clip(CircleShape).background(
                                                                if (daySpent > avgExpense * 1.5) Color(0xFFD32F2F) else Color(0xFF2E7D32)
                                                            )
                                                        )
                                                    }
                                                }
                                            }
                                        } else {
                                            Spacer(modifier = Modifier.weight(1f).height(38.dp))
                                        }
                                    }
                                }
                            }
                        }

                        // Selected Calendar Day Inspector Callout
                        AnimatedVisibility(visible = selectedCalendarDayExpense != null) {
                            if (selectedCalendarDayExpense != null) {
                                val (dayNum, spentAmt) = selectedCalendarDayExpense!!
                                val mName = if (isEnglish) monthNamesEn[selectedMonthCalendar.get(Calendar.MONTH)] else monthNamesBn[selectedMonthCalendar.get(Calendar.MONTH)]

                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = "📅 $dayNum $mName ${selectedMonthCalendar.get(Calendar.YEAR)}",
                                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                            )
                                            Text(
                                                text = if (spentAmt == 0.0) (if (isEnglish) "🎯 No expenditure on this day!" else "🎯 এই দিনে কোনো খরচ হয়নি (নো-স্পেন্ড ডে)!")
                                                else (if (isEnglish) "Total Spent: ${formatTakaSafe(spentAmt)}" else "ওই দিনের মোট খরচ: ${formatTakaSafe(spentAmt)}"),
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = if (spentAmt == 0.0) Color(0xFF2E7D32) else Color(0xFFD32F2F)
                                                )
                                            )
                                        }

                                        IconButton(onClick = { selectedCalendarDayExpense = null }, modifier = Modifier.size(24.dp)) {
                                            Icon(imageVector = Icons.Default.Close, contentDescription = "Close", modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }
                        }

                        // Heatmap Legend
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(10.dp).clip(RoundedCornerShape(2.dp)).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (isEnglish) "৳0 (No spend)" else "৳০ (ব্যয়হীন)", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(10.dp).clip(RoundedCornerShape(2.dp)).background(Color(0xFF2E7D32).copy(alpha = 0.35f)))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (isEnglish) "Normal" else "স্বাভাবিক", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(10.dp).clip(RoundedCornerShape(2.dp)).background(Color(0xFFD32F2F).copy(alpha = 0.4f)))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (isEnglish) "High" else "অধিক খরচ", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }

            // Monthly Budget Tracker Card with Safe Daily Allowance
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("budget_tracker_card"),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    border = AppCardDefaults.border(Color(0xFF10B981)),
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
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.TrackChanges,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = if (isEnglish) "Monthly Budget Target" else "মাসিক বাজেট লক্ষ্যমাত্রা",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Text(
                                        text = if (monthlyBudget > 0) (if (isEnglish) "Target: ${formatTakaSafe(monthlyBudget)}" else "নির্ধারিত: ${formatTakaSafe(monthlyBudget)}") else (if (isEnglish) "No budget set yet" else "কোনো বাজেট নির্ধারণ করা হয়নি"),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            TextButton(
                                onClick = { showBudgetDialog = true },
                                modifier = Modifier.testTag("btn_set_budget")
                            ) {
                                Text(if (monthlyBudget > 0) (if (isEnglish) "Change" else "পরিবর্তন") else (if (isEnglish) "Set Budget" else "সেট করুন"))
                            }
                        }

                        if (monthlyBudget > 0) {
                            val budgetUsageRatio = (monthlyExpense / monthlyBudget).toFloat().coerceIn(0f, 1f)
                            val isOverBudget = monthlyExpense > monthlyBudget
                            val progressColor = when {
                                isOverBudget -> MaterialTheme.colorScheme.error
                                budgetUsageRatio > 0.85f -> Color(0xFFF57C00)
                                else -> Color(0xFF2E7D32)
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = if (isEnglish) "Spent: ${formatTakaSafe(monthlyExpense)}" else "খরচ হয়েছে: ${formatTakaSafe(monthlyExpense)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "${String.format(Locale.US, "%.0f", (monthlyExpense / monthlyBudget) * 100)}%",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = progressColor
                                    )
                                }

                                LinearProgressIndicator(
                                    progress = { budgetUsageRatio },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(10.dp)
                                        .clip(RoundedCornerShape(5.dp)),
                                    color = progressColor,
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                                )

                                val remainingBudget = monthlyBudget - monthlyExpense
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = if (remainingBudget >= 0) {
                                            if (isEnglish) "Remaining: ${formatTakaSafe(remainingBudget)}" else "অবশিষ্ট বাজেট: ${formatTakaSafe(remainingBudget)}"
                                        } else {
                                            if (isEnglish) "Over Budget: ${formatTakaSafe(-remainingBudget)}" else "বাজেট অতিক্রম: ${formatTakaSafe(-remainingBudget)}"
                                        },
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (remainingBudget >= 0) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error
                                        )
                                    )
                                }
                            }

                            // Unique feature: Safe Daily Spending Allowance
                            if (safeDailySpending > 0) {
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Lightbulb,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = if (isEnglish) "Safe Daily Spending Limit" else "নিরাপদ দৈনিক খরচের সীমা",
                                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                                color = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                            Text(
                                                text = if (isEnglish) "To stay within budget, you can spend up to ${formatTakaSafe(safeDailySpending)} per day." else "বাজেটের ভেতরে থাকতে আপনি প্রতিদিন গড়ে সর্বোচ্চ ${formatTakaSafe(safeDailySpending)} খরচ করতে পারবেন।",
                                                style = MaterialTheme.typography.bodySmall,
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

            // Interactive Daily Spending Trend (7-Day / Monthly / Lifetime)
            item {
                InteractiveSpendingTrendSection(
                    allTransactions = allTransactions,
                    selectedMonthCalendar = selectedMonthCalendar,
                    isEnglish = isEnglish,
                    onPreviousMonth = { viewModel.previousMonth() },
                    onNextMonth = { viewModel.nextMonth() },
                    onOpenMonthPicker = { showMonthPickerModal = true },
                    monthNamesBn = monthNamesBn,
                    monthNamesEn = monthNamesEn
                )
            }

            // Category-wise Breakdown
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("category_breakdown_card"),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    border = AppCardDefaults.border(Color(0xFFEC4899)),
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
                            Text(
                                text = if (isEnglish) "Category Spending Breakdown" else "খাতভিত্তিক ব্যয়ের বিশ্লেষণ",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = if (isEnglish) "${categoryBreakdown.size} Categories" else "${categoryBreakdown.size}টি খাত",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (categoryBreakdown.isEmpty()) {
                            Text(
                                text = if (isEnglish) "No expenses recorded this month yet." else "এই মাসে এখনো কোনো ব্যয়ের হিসাব নেই।",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 12.dp)
                            )
                        } else {
                            categoryBreakdown.forEach { item ->
                                val icon = SimpleCategoryData.getCategoryIcon(item.category)
                                val color = SimpleCategoryData.getCategoryColor(item.category)

                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(28.dp)
                                                    .clip(CircleShape)
                                                    .background(color.copy(alpha = 0.15f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = icon,
                                                    contentDescription = item.category,
                                                    tint = color,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = if (isEnglish) com.example.ui.util.AppLocale.category(item.category, true) else item.category,
                                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                                            )
                                            Text(
                                                text = if (isEnglish) " (${item.count})" else " (${item.count}টি)",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = formatTakaSafe(item.amount),
                                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "${String.format(Locale.US, "%.1f", item.percentage * 100)}%",
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                color = color
                                            )
                                        }
                                    }

                                    LinearProgressIndicator(
                                        progress = { item.percentage },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(6.dp)
                                            .clip(RoundedCornerShape(3.dp)),
                                        color = color,
                                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Smart Financial Insights Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("financial_insights_card"),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                    ),
                    border = AppCardDefaults.border(Color(0xFFEAB308))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isEnglish) "Smart Financial Insights" else "স্মার্ট ফাইন্যান্সিয়াল ইনসাইটস",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        val topCategory = categoryBreakdown.firstOrNull()
                        val adviceText = if (isEnglish) {
                            when {
                                monthlyIncome <= 0 && monthlyExpense > 0 ->
                                    "No income recorded this month. Add your salary or income to see an accurate savings rate."
                                monthlyExpense > monthlyIncome && monthlyIncome > 0 ->
                                    "Warning: Your monthly expenses exceed your income. Consider reducing non-essential spending."
                                monthlySavingsRate >= 20.0 ->
                                    "Awesome! Your savings rate is ${String.format(Locale.US, "%.1f", monthlySavingsRate)}%. Maintaining this rate will accelerate your financial goals."
                                topCategory != null && topCategory.percentage > 0.4f ->
                                    "The largest portion of your spending (${String.format(Locale.US, "%.0f", topCategory.percentage * 100)}%) went to '${com.example.ui.util.AppLocale.category(topCategory.category, true)}'."
                                else ->
                                    "Record your daily expenses diligently. Consistent tracking helps curb waste and build lasting wealth."
                            }
                        } else {
                            when {
                                monthlyIncome <= 0 && monthlyExpense > 0 ->
                                    "এই মাসে আপনার আয়ের হিসাব যোগ করা হয়নি। সঠিক সঞ্চয় হিসাব পাওয়ার জন্য বেতন বা আয়ের বিবরণ যুক্ত করুন।"
                                monthlyExpense > monthlyIncome && monthlyIncome > 0 ->
                                    "সতর্কতা: চলতি মাসে আপনার খরচ আয়ের চেয়ে বেশি হয়ে গেছে। অপ্রয়োজনীয় কেনাকাটা নিয়ন্ত্রণ করুন।"
                                monthlySavingsRate >= 20.0 ->
                                    "অসাধারণ! আপনার সঞ্চয়ের হার ${String.format(Locale.US, "%.1f", monthlySavingsRate)}%। নিয়মিত এই হার বজায় রাখলে আর্থিক লক্ষ্য অর্জন সহজ হবে।"
                                topCategory != null && topCategory.percentage > 0.4f ->
                                    "আপনার মোট ব্যয়ের সিংহভাগ (${String.format(Locale.US, "%.0f", topCategory.percentage * 100)}%) যাচ্ছে '${topCategory.category}' খাতে।"
                                else ->
                                    "প্রতিদিনের খরচের হিসাব সঠিকভাবে রেকর্ড করুন। মাস শেষে এটি আপনার অপচয় কমাতে বড় ভূমিকা রাখবে।"
                            }
                        }

                        Text(
                            text = adviceText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // 50/30/20 Rule Smart Financial Analysis Card (Unique Feature)
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("fifty_thirty_twenty_card"),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = AppCardDefaults.border(Color(0xFF8B5CF6)),
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
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.secondaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PieChart,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = if (isEnglish) "50/30/20 Financial Rule" else "৫০/৩০/২০ আর্থিক শৃঙ্খলা বিশ্লেষণ",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Text(
                                        text = if (isEnglish) "Needs (50%) • Wants (30%) • Savings (20%)" else "প্রয়োজন (৫০%) • ইচ্ছে (৩০%) • সঞ্চয় (২০%)",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        // Needs and Wants Computation
                        val needsCategories = setOf("খাবার", "বাজার", "চিকিৎসা", "বিল", "শিক্ষা", "যাতায়াত", "Food", "Groceries", "Medical", "Bills", "Education", "Transport", "Rent", "Utilities")
                        val needsAmount = categoryBreakdown.filter { cat ->
                            needsCategories.any { key -> cat.category.contains(key, ignoreCase = true) }
                        }.sumOf { it.amount }
                        val wantsAmount = (monthlyExpense - needsAmount).coerceAtLeast(0.0)
                        val savingsAmount = monthlySavings.coerceAtLeast(0.0)

                        val baseTotal = if (monthlyIncome > 0) monthlyIncome else max(1.0, monthlyExpense)
                        val needsRatio = ((needsAmount / baseTotal) * 100).toFloat().coerceIn(0f, 100f)
                        val wantsRatio = ((wantsAmount / baseTotal) * 100).toFloat().coerceIn(0f, 100f)
                        val savingsRatio = if (monthlyIncome > 0) ((savingsAmount / baseTotal) * 100).toFloat().coerceIn(0f, 100f) else 0f

                        // Segmented bar
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(14.dp)
                                    .clip(RoundedCornerShape(10.dp))
                            ) {
                                if (needsRatio > 0) {
                                    Box(
                                        modifier = Modifier
                                            .weight(needsRatio.coerceAtLeast(1f))
                                            .fillMaxHeight()
                                            .background(Color(0xFF1976D2))
                                    )
                                }
                                if (wantsRatio > 0) {
                                    Box(
                                        modifier = Modifier
                                            .weight(wantsRatio.coerceAtLeast(1f))
                                            .fillMaxHeight()
                                            .background(Color(0xFFF57C00))
                                    )
                                }
                                if (savingsRatio > 0) {
                                    Box(
                                        modifier = Modifier
                                            .weight(savingsRatio.coerceAtLeast(1f))
                                            .fillMaxHeight()
                                            .background(Color(0xFF2E7D32))
                                    )
                                }
                            }
                        }

                        // Legend row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.size(9.dp).clip(CircleShape).background(Color(0xFF1976D2)))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (isEnglish) "Needs (50%)" else "প্রয়োজন (৫০%)",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                                    )
                                }
                                Text(
                                    text = "${formatTakaSafe(needsAmount)} (%.0f%%)".format(Locale.US, needsRatio),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF1976D2)
                                )
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.size(9.dp).clip(CircleShape).background(Color(0xFFF57C00)))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (isEnglish) "Wants (30%)" else "ইচ্ছে (৩০%)",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                                    )
                                }
                                Text(
                                    text = "${formatTakaSafe(wantsAmount)} (%.0f%%)".format(Locale.US, wantsRatio),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFFF57C00)
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.size(9.dp).clip(CircleShape).background(Color(0xFF2E7D32)))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (isEnglish) "Savings (20%)" else "সঞ্চয় (২০%)",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                                    )
                                }
                                Text(
                                    text = "${formatTakaSafe(savingsAmount)} (%.0f%%)".format(Locale.US, savingsRatio),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF2E7D32)
                                )
                            }
                        }

                        // Guidance note
                        val ruleFeedback = when {
                            savingsRatio >= 20f && needsRatio <= 60f ->
                                if (isEnglish) "🎯 Excellent balance! You are meeting the golden 50/30/20 benchmark with strong savings."
                                else "🎯 আদর্শ ভারসাম্য! আপনার সঞ্চয় ও খরচের অনুপাত ৫০/৩০/২০ নিয়মের সাথে চমৎকারভাবে মিলছে।"
                            needsRatio > 70f ->
                                if (isEnglish) "⚠️ Essential needs consume over 70% of your outflow. Focus on streamlining utility or grocery costs."
                                else "⚠️ প্রয়োজনীয় ব্যয়ের হার ৭০% ছাড়িয়েছে। বাজার বা নিয়মিত ইউটিলিটি খরচে নজর দেওয়া প্রয়োজন।"
                            wantsRatio > 40f ->
                                if (isEnglish) "💡 Lifestyle & wants exceed 40%. Consider deferring non-urgent shopping to maximize savings."
                                else "💡 শপিং ও লাইফস্টাইল খরচ ৪০% ছাড়িয়েছে। অপ্রয়োজনীয় খরচ কমালে সঞ্চয় দ্রুত বৃদ্ধি পাবে।"
                            else ->
                                if (isEnglish) "Keep categorizing each entry to refine your 50/30/20 balance over time."
                                else "সঠিক খাতে খরচ এন্ট্রি করতে থাকুন, এটি আপনার ৫০/৩০/২০ অনুশাসন মজবুত করবে।"
                        }
                        Text(
                            text = ruleFeedback,
                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                    }
                }
            }

            // Monthly Summary Share & Export Card (Unique Feature)
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("monthly_export_share_card"),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = AppCardDefaults.border(Color(0xFF06B6D4)),
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
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.tertiaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Share,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.tertiary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = if (isEnglish) "Monthly Summary Statement" else "মাসিক বিবরণী শেয়ার ও কপি",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Text(
                                        text = if (isEnglish) "One-tap export for personal records" else "এক ক্লিকে সামগ্রিক হিসাব সংগ্রহ বা শেয়ার",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        // Summary Text Builder
                        val summaryText = buildString {
                            appendLine(if (isEnglish) "📊 Monthly Financial Summary - $monthYearText" else "📊 মাসিক আর্থিক বিবরণী - $monthYearText")
                            appendLine("─────────────────────────")
                            appendLine(if (isEnglish) "💰 Total Income: ${formatTakaSafe(monthlyIncome)}" else "💰 মোট আয়: ${formatTakaSafe(monthlyIncome)}")
                            appendLine(if (isEnglish) "🔻 Total Expense: ${formatTakaSafe(monthlyExpense)}" else "🔻 মোট খরচ: ${formatTakaSafe(monthlyExpense)}")
                            appendLine(if (isEnglish) "💎 Net Savings: ${formatTakaSafe(monthlySavings)} (Rate: %.1f%%)".format(Locale.US, monthlySavingsRate) else "💎 নিট সঞ্চয়: ${formatTakaSafe(monthlySavings)} (হার: %.1f%%)".format(Locale.US, monthlySavingsRate))
                            if (monthlyBudget > 0) {
                                appendLine(if (isEnglish) "🎯 Budget Limit: ${formatTakaSafe(monthlyBudget)}" else "🎯 মাসিক বাজেট: ${formatTakaSafe(monthlyBudget)}")
                            }
                            if (categoryBreakdown.isNotEmpty()) {
                                appendLine(if (isEnglish) "🏆 Top Category: ${categoryBreakdown.first().category} (${formatTakaSafe(categoryBreakdown.first().amount)})" else "🏆 প্রধান ব্যয়ের খাত: ${categoryBreakdown.first().category} (${formatTakaSafe(categoryBreakdown.first().amount)})")
                            }
                            appendLine("─────────────────────────")
                            appendLine(if (isEnglish) "Generated with Daily Hishab App" else "দৈনিক হিসাব অ্যাপ থেকে তৈরি")
                        }

                        // Preview box
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = summaryText,
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, lineHeight = 16.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(12.dp)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText("Monthly Summary", summaryText)
                                    clipboard.setPrimaryClip(clip)
                                    AppToastManager.show(if (isEnglish) "Statement copied to clipboard!" else "মাসিক বিবরণী কপি করা হয়েছে!")
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .testTag("btn_copy_monthly_summary")
                            ) {
                                Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (isEnglish) "Copy" else "কপি করুন")
                            }

                            Button(
                                onClick = {
                                    val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_SUBJECT, if (isEnglish) "Monthly Summary - $monthYearText" else "মাসিক হিসাব বিবরণী - $monthYearText")
                                        putExtra(Intent.EXTRA_TEXT, summaryText)
                                    }
                                    val shareIntent = Intent.createChooser(sendIntent, if (isEnglish) "Share Monthly Statement" else "মাসিক হিসাব শেয়ার করুন")
                                    context.startActivity(shareIntent)
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1.2f)
                                    .height(44.dp)
                                    .testTag("btn_share_monthly_summary")
                            ) {
                                Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (isEnglish) "Share Report" else "রিপোর্ট শেয়ার", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(20.dp)) }
        }
    }

    // Set Budget Dialog (Floating Card)
    if (showBudgetDialog) {
        var budgetInput by remember { mutableStateOf(if (monthlyBudget > 0) String.format(Locale.US, "%.0f", monthlyBudget) else "") }
        val budgetDialogScrollState = rememberScrollState()

        Dialog(
            onDismissRequest = { showBudgetDialog = false },
            properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = true)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .padding(horizontal = 8.dp, vertical = 16.dp)
                    .imePadding()
                    .testTag("budget_dialog_card"),
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = AppCardDefaults.dialogBorder(),
                elevation = CardDefaults.cardElevation(defaultElevation = 14.dp)
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(budgetDialogScrollState)
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isEnglish) "Set Monthly Budget" else "মাসিক বাজেট নির্ধারণ",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            IconButton(onClick = { showBudgetDialog = false }) {
                                Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                            }
                        }

                        Text(
                            text = if (isEnglish) "Enter the maximum amount you plan to spend this month:" else "এই মাসে আপনি সর্বোচ্চ কত টাকা খরচ করতে চান তা লিখুন:",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        OutlinedTextField(
                            value = budgetInput,
                            onValueChange = { input ->
                                budgetInput = input.filter { it.isDigit() }
                            },
                            label = { Text(if (isEnglish) "Budget Amount" else "বাজেটের পরিমাণ") },
                            placeholder = { Text(if (isEnglish) "e.g. 25000" else "যেমন: ২৫০০০") },
                            leadingIcon = { Text("৳", fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 12.dp)) },
                            shape = RoundedCornerShape(12.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("input_budget_amount")
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = { showBudgetDialog = false },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f).height(46.dp)
                            ) {
                                Text(if (isEnglish) "Cancel" else "বাতিল")
                            }

                            Button(
                                onClick = {
                                    val amount = budgetInput.toDoubleOrNull() ?: 0.0
                                    viewModel.setMonthlyBudget(amount)
                                    showBudgetDialog = false
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1.3f).height(46.dp).testTag("btn_save_budget")
                            ) {
                                Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(17.dp))
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(if (isEnglish) "Save Budget" else "বাজেট সেট করুন", fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    KeyboardScrollDownHint(
                        scrollState = budgetDialogScrollState,
                        modifier = Modifier.align(Alignment.BottomCenter)
                    )
                }
            }
        }
    }

    // Interactive Month & Year Quick Jump Dialog
    if (showMonthPickerModal) {
        var pickerYear by remember { mutableStateOf(selectedMonthCalendar.get(Calendar.YEAR)) }

        AlertDialog(
            onDismissRequest = { showMonthPickerModal = false },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isEnglish) "Select Month & Year" else "মাস ও বছর নির্বাচন করুন",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    IconButton(onClick = { showMonthPickerModal = false }, modifier = Modifier.size(24.dp)) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    // Year selector row with prev/next arrows
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
                        shape = RoundedCornerShape(12.dp),
                        border = AppCardDefaults.border(MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { pickerYear-- }) {
                                Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Prev Year")
                            }
                            Text(
                                text = "$pickerYear",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                            )
                            IconButton(onClick = { pickerYear++ }) {
                                Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next Year")
                            }
                        }
                    }

                    // 12 Months Grid (3 columns x 4 rows)
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        val currentMonthIdx = selectedMonthCalendar.get(Calendar.MONTH)
                        val isSameYear = selectedMonthCalendar.get(Calendar.YEAR) == pickerYear

                        for (row in 0..3) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                for (col in 0..2) {
                                    val monthIdx = row * 3 + col
                                    val isSelectedMonth = isSameYear && currentMonthIdx == monthIdx
                                    val mName = if (isEnglish) monthNamesEn[monthIdx] else monthNamesBn[monthIdx]

                                    Surface(
                                        onClick = {
                                            viewModel.setYearAndMonth(pickerYear, monthIdx)
                                            showMonthPickerModal = false
                                        },
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (isSelectedMonth) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                        border = androidx.compose.foundation.BorderStroke(
                                            1.dp,
                                            if (isSelectedMonth) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                                        ),
                                        modifier = Modifier.weight(1f).height(38.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = mName,
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontWeight = if (isSelectedMonth) FontWeight.ExtraBold else FontWeight.Medium,
                                                    color = if (isSelectedMonth) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                                    fontSize = 11.5.sp
                                                ),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showMonthPickerModal = false }) {
                    Text(if (isEnglish) "Close" else "বন্ধ করুন")
                }
            }
        )
    }
}

enum class TrendScope {
    LAST_7_DAYS,
    SELECTED_MONTH,
    LIFETIME
}

data class SpendingTrendDay(
    val id: String,
    val timestamp: Long,
    val dayOfMonth: Int,
    val month: Int,
    val year: Int,
    val dayNameBn: String,
    val dayNameEn: String,
    val dateTextBn: String,
    val dateTextEn: String,
    val fullDateBn: String,
    val fullDateEn: String,
    val amount: Double,
    val incomeAmount: Double,
    val count: Int,
    val isToday: Boolean,
    val isPeak: Boolean,
    val isFuture: Boolean
)

@Composable
fun InteractiveSpendingTrendSection(
    allTransactions: List<TransactionEntity>,
    selectedMonthCalendar: Calendar,
    isEnglish: Boolean,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onOpenMonthPicker: () -> Unit,
    monthNamesBn: Array<String>,
    monthNamesEn: Array<String>,
    modifier: Modifier = Modifier
) {
    var currentScope by remember { mutableStateOf(TrendScope.SELECTED_MONTH) }
    var selectedDayId by remember { mutableStateOf<String?>(null) }
    val lazyListState = rememberLazyListState()

    val bengaliDaysShort = remember { arrayOf("রবি", "সোম", "মঙ্গল", "বুধ", "বৃহঃ", "শুক্র", "শনি") }
    val englishDaysShort = remember { arrayOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat") }
    val bengaliDaysFull = remember { arrayOf("রবিবার", "সোমবার", "মঙ্গলবার", "বুধবার", "বৃহস্পতিবার", "শুক্রবার", "শনিবার") }
    val englishDaysFull = remember { arrayOf("Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday") }
    val monthNamesBnShort = remember { arrayOf("জানু", "ফেব্রু", "মার্চ", "এপ্রিল", "মে", "জুন", "জুলাই", "আগস্ট", "সেপ্টে", "অক্টো", "নভে", "ডিসে") }
    val monthNamesEnShort = remember { arrayOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec") }

    val todayCal = Calendar.getInstance()
    val todayYear = todayCal.get(Calendar.YEAR)
    val todayMonth = todayCal.get(Calendar.MONTH)
    val todayDay = todayCal.get(Calendar.DAY_OF_MONTH)
    val todayKey = String.format(Locale.US, "%04d-%02d-%02d", todayYear, todayMonth + 1, todayDay)

    val expenseTransactions = remember(allTransactions) {
        allTransactions.filter { !it.isDeleted && it.type == "EXPENSE" }
    }
    val earliestExpenseTimestamp = remember(expenseTransactions) {
        expenseTransactions.minOfOrNull { it.timestamp }
    }

    val dayAggregates = remember(allTransactions) {
        val activeTx = allTransactions.filter { !it.isDeleted }
        val map = mutableMapOf<String, Triple<Double, Double, Int>>()
        val cal = Calendar.getInstance()
        activeTx.forEach { tx ->
            cal.timeInMillis = tx.timestamp
            val key = String.format(Locale.US, "%04d-%02d-%02d", cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH))
            val current = map[key] ?: Triple(0.0, 0.0, 0)
            if (tx.type == "EXPENSE") {
                map[key] = Triple(current.first + tx.amount, current.second, current.third + 1)
            } else if (tx.type == "INCOME") {
                map[key] = Triple(current.first, current.second + tx.amount, current.third + 1)
            }
        }
        map
    }

    val trendDays = remember(currentScope, selectedMonthCalendar, dayAggregates) {
        val list = mutableListOf<SpendingTrendDay>()
        when (currentScope) {
            TrendScope.LAST_7_DAYS -> {
                // Generates exact 7 days ending today (last 7 days)
                for (offset in 6 downTo 0) {
                    val c = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -offset) }
                    val y = c.get(Calendar.YEAR)
                    val m = c.get(Calendar.MONTH)
                    val d = c.get(Calendar.DAY_OF_MONTH)
                    val dow = c.get(Calendar.DAY_OF_WEEK) - 1
                    val key = String.format(Locale.US, "%04d-%02d-%02d", y, m + 1, d)
                    val data = dayAggregates[key] ?: Triple(0.0, 0.0, 0)
                    val isToday = (key == todayKey)
                    list.add(
                        SpendingTrendDay(
                            id = key,
                            timestamp = c.timeInMillis,
                            dayOfMonth = d,
                            month = m,
                            year = y,
                            dayNameBn = bengaliDaysShort[dow],
                            dayNameEn = englishDaysShort[dow],
                            dateTextBn = "$d ${monthNamesBnShort[m]}",
                            dateTextEn = "$d ${monthNamesEnShort[m]}",
                            fullDateBn = "$d ${monthNamesBn[m]} $y, ${bengaliDaysFull[dow]}",
                            fullDateEn = "$d ${monthNamesEn[m]} $y, ${englishDaysFull[dow]}",
                            amount = data.first,
                            incomeAmount = data.second,
                            count = data.third,
                            isToday = isToday,
                            isPeak = false,
                            isFuture = false
                        )
                    )
                }
            }
            TrendScope.SELECTED_MONTH -> {
                val year = selectedMonthCalendar.get(Calendar.YEAR)
                val month = selectedMonthCalendar.get(Calendar.MONTH)
                val cal = Calendar.getInstance().apply {
                    set(Calendar.YEAR, year)
                    set(Calendar.MONTH, month)
                    set(Calendar.DAY_OF_MONTH, 1)
                }
                val maxDays = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
                for (d in 1..maxDays) {
                    cal.set(Calendar.DAY_OF_MONTH, d)
                    val dow = cal.get(Calendar.DAY_OF_WEEK) - 1
                    val key = String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, d)
                    val data = dayAggregates[key] ?: Triple(0.0, 0.0, 0)
                    val isToday = (key == todayKey)
                    val isFuture = cal.after(todayCal) && !isToday
                    list.add(
                        SpendingTrendDay(
                            id = key,
                            timestamp = cal.timeInMillis,
                            dayOfMonth = d,
                            month = month,
                            year = year,
                            dayNameBn = bengaliDaysShort[dow],
                            dayNameEn = englishDaysShort[dow],
                            dateTextBn = "$d ${monthNamesBnShort[month]}",
                            dateTextEn = "$d ${monthNamesEnShort[month]}",
                            fullDateBn = "$d ${monthNamesBn[month]} $year, ${bengaliDaysFull[dow]}",
                            fullDateEn = "$d ${monthNamesEn[month]} $year, ${englishDaysFull[dow]}",
                            amount = data.first,
                            incomeAmount = data.second,
                            count = data.third,
                            isToday = isToday,
                            isPeak = false,
                            isFuture = isFuture
                        )
                    )
                }
            }
            TrendScope.LIFETIME -> {
                val earliestTimestamp = earliestExpenseTimestamp
                    ?: (System.currentTimeMillis() - 29L * 24 * 3600 * 1000)

                val startCal = Calendar.getInstance().apply {
                    timeInMillis = earliestTimestamp
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val endCal = Calendar.getInstance().apply {
                    timeInMillis = System.currentTimeMillis()
                    set(Calendar.HOUR_OF_DAY, 23)
                    set(Calendar.MINUTE, 59)
                    set(Calendar.SECOND, 59)
                }

                val curCal = Calendar.getInstance().apply { timeInMillis = startCal.timeInMillis }
                while (!curCal.after(endCal)) {
                    val y = curCal.get(Calendar.YEAR)
                    val m = curCal.get(Calendar.MONTH)
                    val d = curCal.get(Calendar.DAY_OF_MONTH)
                    val dow = curCal.get(Calendar.DAY_OF_WEEK) - 1
                    val key = String.format(Locale.US, "%04d-%02d-%02d", y, m + 1, d)
                    val data = dayAggregates[key] ?: Triple(0.0, 0.0, 0)
                    val isToday = (key == todayKey)
                    list.add(
                        SpendingTrendDay(
                            id = key,
                            timestamp = curCal.timeInMillis,
                            dayOfMonth = d,
                            month = m,
                            year = y,
                            dayNameBn = bengaliDaysShort[dow],
                            dayNameEn = englishDaysShort[dow],
                            dateTextBn = "$d ${monthNamesBnShort[m]}",
                            dateTextEn = "$d ${monthNamesEnShort[m]}",
                            fullDateBn = "$d ${monthNamesBn[m]} $y, ${bengaliDaysFull[dow]}",
                            fullDateEn = "$d ${monthNamesEn[m]} $y, ${englishDaysFull[dow]}",
                            amount = data.first,
                            incomeAmount = data.second,
                            count = data.third,
                            isToday = isToday,
                            isPeak = false,
                            isFuture = false
                        )
                    )
                    curCal.add(Calendar.DAY_OF_YEAR, 1)
                }
            }
        }
        val maxAmt = list.filter { it.amount > 0 }.maxOfOrNull { it.amount } ?: 0.0
        if (maxAmt > 0) {
            list.map { if (it.amount == maxAmt && it.amount > 0) it.copy(isPeak = true) else it }
        } else {
            list
        }
    }

    val totalExpense = remember(trendDays) { trendDays.sumOf { it.amount } }
    val rawMaxExpense = remember(trendDays) { trendDays.maxOfOrNull { it.amount } ?: 0.0 }
    val peakDay = remember(trendDays) { trendDays.filter { it.amount > 0 }.maxByOrNull { it.amount } }
    val zeroSpendDays = remember(trendDays) { trendDays.count { it.amount == 0.0 && !it.isFuture } }
    val activeDaysCount = remember(trendDays) { trendDays.count { !it.isFuture } }
    val dailyAverage = remember(totalExpense, activeDaysCount) {
        if (activeDaysCount > 0) totalExpense / activeDaysCount else 0.0
    }
    val chartCeiling = remember(rawMaxExpense) {
        if (rawMaxExpense > 0.0) rawMaxExpense else 0.0
    }

    val currentSelectedYearMonth = "${selectedMonthCalendar.get(Calendar.YEAR)}-${selectedMonthCalendar.get(Calendar.MONTH)}"
    var lastObservedMonthKey by remember { mutableStateOf(currentSelectedYearMonth) }

    // Auto-sync scope to SELECTED_MONTH whenever user changes the month in the top bar
    LaunchedEffect(currentSelectedYearMonth) {
        if (lastObservedMonthKey != currentSelectedYearMonth) {
            lastObservedMonthKey = currentSelectedYearMonth
            currentScope = TrendScope.SELECTED_MONTH
        }
    }

    // Auto-scroll to today / active day when scope or month changes
    LaunchedEffect(currentScope, currentSelectedYearMonth, trendDays.size) {
        if (trendDays.isNotEmpty()) {
            val todayIdx = trendDays.indexOfFirst { it.isToday }
            val targetIdx = if (todayIdx >= 0) todayIdx else 0
            lazyListState.scrollToItem((targetIdx - 4).coerceAtLeast(0))
            if (selectedDayId == null || trendDays.none { it.id == selectedDayId }) {
                selectedDayId = if (todayIdx >= 0) trendDays[todayIdx].id else trendDays.first().id
            }
        }
    }

    val activeDay = trendDays.find { it.id == selectedDayId }
        ?: trendDays.find { it.isToday }
        ?: trendDays.firstOrNull()

    // Detect if the selected month is in the future or before user started using the app
    val selectedYear = selectedMonthCalendar.get(Calendar.YEAR)
    val selectedMonth = selectedMonthCalendar.get(Calendar.MONTH)
    val isSelectedMonthFuture = (selectedYear > todayYear) || (selectedYear == todayYear && selectedMonth > todayMonth)

    val earliestUsedCal = remember(earliestExpenseTimestamp) {
        if (earliestExpenseTimestamp != null) {
            Calendar.getInstance().apply { timeInMillis = earliestExpenseTimestamp }
        } else null
    }
    val isSelectedMonthBeforeApp = remember(selectedYear, selectedMonth, earliestUsedCal, isSelectedMonthFuture) {
        if (isSelectedMonthFuture || earliestUsedCal == null) false
        else {
            val earYear = earliestUsedCal.get(Calendar.YEAR)
            val earMonth = earliestUsedCal.get(Calendar.MONTH)
            (selectedYear < earYear) || (selectedYear == earYear && selectedMonth < earMonth)
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("spending_trend_card"),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = AppCardDefaults.border(Color(0xFF6366F1)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Row 1: Title and Subtitle (Full Width, never truncated by chips)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF6366F1).copy(alpha = 0.12f),
                    modifier = Modifier.size(38.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.TrendingUp,
                            contentDescription = null,
                            tint = Color(0xFF6366F1),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = when (currentScope) {
                            TrendScope.LAST_7_DAYS -> if (isEnglish) "7-Day Spending Trend" else "গত ৭ দিনের খরচের ধারা"
                            TrendScope.SELECTED_MONTH -> if (isEnglish) "Monthly Spending Trend" else "মাসিক খরচের ধারা"
                            TrendScope.LIFETIME -> if (isEnglish) "Lifetime Spending Trend" else "লাইফটাইম খরচের ধারা"
                        },
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (isEnglish) "Slide left/right to inspect any day" else "বামে-ডানে স্লাইড করে যেকোনো দিনের ব্যয় দেখুন",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Row 2: Scope Selector Chips (Separate row with equal distribution so no text gets cut off)
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    TrendScopeChip(
                        label = if (isEnglish) "7 Days" else "৭ দিন",
                        isSelected = currentScope == TrendScope.LAST_7_DAYS,
                        onClick = { currentScope = TrendScope.LAST_7_DAYS },
                        testTag = "tab_trend_7days",
                        modifier = Modifier.weight(1f)
                    )
                    TrendScopeChip(
                        label = if (isEnglish) "Monthly" else "মাসিক",
                        isSelected = currentScope == TrendScope.SELECTED_MONTH,
                        onClick = { currentScope = TrendScope.SELECTED_MONTH },
                        testTag = "tab_trend_month",
                        modifier = Modifier.weight(1f)
                    )
                    TrendScopeChip(
                        label = if (isEnglish) "Lifetime" else "লাইফটাইম",
                        isSelected = currentScope == TrendScope.LIFETIME,
                        onClick = { currentScope = TrendScope.LIFETIME },
                        testTag = "tab_trend_lifetime",
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Special Informational Banner for Future Month or Past Before-App Month
            if (currentScope == TrendScope.SELECTED_MONTH && (isSelectedMonthFuture || isSelectedMonthBeforeApp)) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSelectedMonthFuture) {
                        Color(0xFF6366F1).copy(alpha = 0.08f)
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                    },
                    border = BorderStroke(
                        1.dp,
                        if (isSelectedMonthFuture) Color(0xFF6366F1).copy(alpha = 0.25f)
                        else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = if (isSelectedMonthFuture) Icons.Default.Info else Icons.Default.History,
                            contentDescription = null,
                            tint = if (isSelectedMonthFuture) Color(0xFF6366F1) else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = if (isSelectedMonthFuture) {
                                if (isEnglish) "Your selected month is in the future. No spending recorded yet."
                                else "আপনার সিলেক্ট করা মাসটি ভবিষ্যৎ সময়ের। এখনো কোনো খরচ যোগ করা হয়নি।"
                            } else {
                                if (isEnglish) "You selected a past month from before you started using this app."
                                else "আপনি যে মাস সিলেক্ট করেছেন তা অতীত সময়ের যখন আপনি এই অ্যাপটি ব্যবহার করতেন না।"
                            },
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Medium,
                                color = if (isSelectedMonthFuture) Color(0xFF4338CA) else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp
                            )
                        )
                    }
                }
            }

            // Month Stepper (Visible when Monthly scope is chosen)
            AnimatedVisibility(visible = currentScope == TrendScope.SELECTED_MONTH) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onPreviousMonth,
                            modifier = Modifier.size(34.dp).testTag("btn_trend_prev_month")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Previous Month",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        Surface(
                            onClick = onOpenMonthPicker,
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                            modifier = Modifier.testTag("btn_trend_month_picker")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CalendarMonth,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(15.dp)
                                )
                                val curMonth = selectedMonthCalendar.get(Calendar.MONTH)
                                val curYear = selectedMonthCalendar.get(Calendar.YEAR)
                                val monthText = if (isEnglish) "${monthNamesEn[curMonth]} $curYear" else "${monthNamesBn[curMonth]} ${toBengaliNum(curYear.toString())}"
                                Text(
                                    text = monthText,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontSize = 12.sp
                                    )
                                )
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        IconButton(
                            onClick = onNextMonth,
                            modifier = Modifier.size(34.dp).testTag("btn_trend_next_month")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "Next Month",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            // Selected Day Inspection Banner (Dynamic & Interactive)
            if (activeDay != null) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("spending_trend_inspector")
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                        verticalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.weight(1f, fill = false)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CalendarMonth,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = if (isEnglish) activeDay.fullDateEn else activeDay.fullDateBn,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontSize = 12.sp
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            // Status Tag
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = when {
                                    activeDay.isPeak && activeDay.amount > 0 -> Color(0xFFEF4444).copy(alpha = 0.15f)
                                    activeDay.isToday -> Color(0xFF10B981).copy(alpha = 0.15f)
                                    activeDay.amount == 0.0 -> Color(0xFF00897B).copy(alpha = 0.15f)
                                    else -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = when {
                                            activeDay.isPeak && activeDay.amount > 0 -> if (isEnglish) "🔥 Peak Expense" else "🔥 সর্বোচ্চ ব্যয়"
                                            activeDay.isToday -> if (isEnglish) "⭐ Today" else "⭐ আজকের দিন"
                                            activeDay.amount == 0.0 -> if (isEnglish) "🌿 No-Spend" else "🌿 খরচহীন দিন"
                                            else -> if (isEnglish) "💳 Daily Spend" else "💳 মোট খরচ"
                                        },
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp,
                                            color = when {
                                                activeDay.isPeak && activeDay.amount > 0 -> Color(0xFFEF4444)
                                                activeDay.isToday -> Color(0xFF10B981)
                                                activeDay.amount == 0.0 -> Color(0xFF00897B)
                                                else -> MaterialTheme.colorScheme.primary
                                            }
                                        )
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.Bottom,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = formatTakaSafe(activeDay.amount),
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 17.sp,
                                        color = if (activeDay.amount > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                                if (activeDay.incomeAmount > 0) {
                                    Text(
                                        text = "(+${formatTakaSafe(activeDay.incomeAmount)})",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.5.sp,
                                            color = Color(0xFF10B981)
                                        ),
                                        modifier = Modifier.padding(bottom = 2.dp)
                                    )
                                }
                            }

                            Text(
                                text = if (activeDay.count > 0) {
                                    if (isEnglish) "${activeDay.count} transactions" else "${toBengaliNum(activeDay.count.toString())} টি লেনদেন"
                                } else {
                                    if (isEnglish) "No expense" else "কোনো খরচ নেই"
                                },
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            )
                        }
                    }
                }
            }

            // =========================================================================
            // Brand New Scratch-Built Daily Spending Progress Meters Showcase
            // =========================================================================
            var dailySpendingFilter by remember { mutableStateOf("EXPENSE_ONLY") } // "EXPENSE_ONLY", "SORT_HIGHEST"

            val displayDays = remember(trendDays, dailySpendingFilter) {
                when (dailySpendingFilter) {
                    "SORT_HIGHEST" -> {
                        val spent = trendDays.filter { it.amount > 0 }.sortedByDescending { it.amount }
                        if (spent.isNotEmpty()) spent else trendDays
                    }
                    else -> {
                        val spent = trendDays.filter { it.amount > 0 }
                        if (spent.isNotEmpty()) spent else trendDays
                    }
                }
            }

            val progressScrollState = rememberScrollState()

            // Isolate nested scrolling: Consumes all leftover scroll deltas and flings so parent page never scrolls!
            val isolateProgressScrollConnection = remember {
                object : NestedScrollConnection {
                    override fun onPostScroll(
                        consumed: Offset,
                        available: Offset,
                        source: NestedScrollSource
                    ): Offset {
                        // Consume all unconsumed scroll deltas so outer page doesn't scroll when list reaches top or bottom
                        return available
                    }

                    override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                        // Consume unconsumed fling velocity to prevent parent page from scrolling at scroll boundaries
                        return available
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Section Title Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF6366F1).copy(alpha = 0.14f),
                        modifier = Modifier.size(28.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.LinearScale,
                                contentDescription = null,
                                tint = Color(0xFF6366F1),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    Column {
                        Text(
                            text = if (isEnglish) "Daily Spending Progress Meter" else "দৈনিক খরচের প্রগ্রেস হিসাব",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (isEnglish) "Day-by-day expenditure meter & ratio" else "দিনভিত্তিক ব্যয়ের পরিমাপক ও অনুপাত",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }

                // Dedicated Separate Filter Bar: Only "খরচ" and "শীর্ষ" (Separated with clean margins)
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            onClick = { dailySpendingFilter = "EXPENSE_ONLY" },
                            shape = RoundedCornerShape(9.dp),
                            color = if (dailySpendingFilter == "EXPENSE_ONLY") MaterialTheme.colorScheme.primary else Color.Transparent,
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 7.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (isEnglish) "🔻 Spent Days" else "🔻 খরচের দিন",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = if (dailySpendingFilter == "EXPENSE_ONLY") FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 11.5.sp,
                                        color = if (dailySpendingFilter == "EXPENSE_ONLY") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }
                        }

                        Surface(
                            onClick = { dailySpendingFilter = "SORT_HIGHEST" },
                            shape = RoundedCornerShape(9.dp),
                            color = if (dailySpendingFilter == "SORT_HIGHEST") MaterialTheme.colorScheme.primary else Color.Transparent,
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 7.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (isEnglish) "🔥 Highest Spend" else "🔥 শীর্ষ খরচ",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = if (dailySpendingFilter == "SORT_HIGHEST") FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 11.5.sp,
                                        color = if (dailySpendingFilter == "SORT_HIGHEST") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                // Scrollable Daily Spending Meters List (Independent Isolated Scrolling)
                if (displayDays.isEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp)
                    ) {
                        Text(
                            text = if (isEnglish) "No expense recorded for this filter." else "এই ফিল্টারে কোনো খরচ নেই।",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 330.dp)
                            .nestedScroll(isolateProgressScrollConnection)
                            .verticalScroll(progressScrollState),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        displayDays.forEach { day ->
                            val isSelected = (day.id == selectedDayId)
                            val progressRatio = if (rawMaxExpense > 0.0 && day.amount > 0.0) {
                                (day.amount / rawMaxExpense).toFloat().coerceIn(0f, 1f)
                            } else 0f

                            Surface(
                                onClick = { selectedDayId = day.id },
                                shape = RoundedCornerShape(14.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.09f)
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                border = BorderStroke(
                                    if (isSelected) 1.5.dp else 0.8.dp,
                                    if (isSelected) MaterialTheme.colorScheme.primary
                                    else if (day.isPeak && day.amount > 0) Color(0xFFEF4444).copy(alpha = 0.4f)
                                    else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    // Row 1: Date & Badges (Left) + Amount (Right)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        // Left: Date Badge + Name
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Surface(
                                                shape = RoundedCornerShape(10.dp),
                                                color = when {
                                                    day.isToday -> Color(0xFF10B981).copy(alpha = 0.18f)
                                                    day.isPeak && day.amount > 0 -> Color(0xFFEF4444).copy(alpha = 0.18f)
                                                    else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                                },
                                                border = BorderStroke(
                                                    0.8.dp,
                                                    when {
                                                        day.isToday -> Color(0xFF10B981).copy(alpha = 0.4f)
                                                        day.isPeak && day.amount > 0 -> Color(0xFFEF4444).copy(alpha = 0.4f)
                                                        else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                                                    }
                                                ),
                                                modifier = Modifier.size(38.dp)
                                            ) {
                                                Column(
                                                    modifier = Modifier.fillMaxSize(),
                                                    horizontalAlignment = Alignment.CenterHorizontally,
                                                    verticalArrangement = Arrangement.Center
                                                ) {
                                                    Text(
                                                        text = if (isEnglish) day.dayOfMonth.toString() else toBengaliNum(day.dayOfMonth.toString()),
                                                        style = MaterialTheme.typography.titleSmall.copy(
                                                            fontWeight = FontWeight.ExtraBold,
                                                            fontSize = 13.sp,
                                                            color = when {
                                                                day.isToday -> Color(0xFF10B981)
                                                                day.isPeak && day.amount > 0 -> Color(0xFFEF4444)
                                                                else -> MaterialTheme.colorScheme.primary
                                                            }
                                                        )
                                                    )
                                                    Text(
                                                        text = if (isEnglish) day.dayNameEn else day.dayNameBn,
                                                        style = MaterialTheme.typography.labelSmall.copy(
                                                            fontSize = 8.5.sp,
                                                            fontWeight = FontWeight.Medium,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    )
                                                }
                                            }

                                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                                ) {
                                                    Text(
                                                        text = if (isEnglish) day.dateTextEn else day.dateTextBn,
                                                        style = MaterialTheme.typography.bodyMedium.copy(
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 13.5.sp
                                                        ),
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )

                                                    if (day.isToday) {
                                                        Surface(
                                                            shape = RoundedCornerShape(6.dp),
                                                            color = Color(0xFF10B981).copy(alpha = 0.15f)
                                                        ) {
                                                            Text(
                                                                text = if (isEnglish) "Today" else "আজ",
                                                                style = MaterialTheme.typography.labelSmall.copy(
                                                                    fontSize = 9.5.sp,
                                                                    fontWeight = FontWeight.Bold,
                                                                    color = Color(0xFF10B981)
                                                                ),
                                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                                            )
                                                        }
                                                    } else if (day.isPeak && day.amount > 0) {
                                                        Surface(
                                                            shape = RoundedCornerShape(6.dp),
                                                            color = Color(0xFFEF4444).copy(alpha = 0.15f)
                                                        ) {
                                                            Text(
                                                                text = if (isEnglish) "🔥 Peak" else "🔥 শীর্ষ",
                                                                style = MaterialTheme.typography.labelSmall.copy(
                                                                    fontSize = 9.5.sp,
                                                                    fontWeight = FontWeight.Bold,
                                                                    color = Color(0xFFEF4444)
                                                                ),
                                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                                            )
                                                        }
                                                    } else if (day.amount == 0.0 && !day.isFuture) {
                                                        Surface(
                                                            shape = RoundedCornerShape(6.dp),
                                                            color = Color(0xFF059669).copy(alpha = 0.12f)
                                                        ) {
                                                            Text(
                                                                text = if (isEnglish) "No Spend" else "খরচহীন",
                                                                style = MaterialTheme.typography.labelSmall.copy(
                                                                    fontSize = 9.5.sp,
                                                                    fontWeight = FontWeight.Medium,
                                                                    color = Color(0xFF059669)
                                                                ),
                                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                                            )
                                                        }
                                                    }
                                                }

                                                Text(
                                                    text = if (day.count > 0) {
                                                        if (isEnglish) "${day.count} transactions" else "${toBengaliNum(day.count.toString())} টি হিসাব"
                                                    } else {
                                                        if (isEnglish) "No expense" else "কোনো খরচ নেই"
                                                    },
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        fontSize = 11.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                )
                                            }
                                        }

                                        // Right: Formatted Daily Expense Amount
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(
                                                text = formatTakaSafe(day.amount),
                                                style = MaterialTheme.typography.titleMedium.copy(
                                                    fontWeight = FontWeight.ExtraBold,
                                                    fontSize = 15.5.sp,
                                                    color = if (day.amount > 0) Color(0xFFDC2626) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                                )
                                            )
                                            if (day.incomeAmount > 0) {
                                                Text(
                                                    text = "+${formatTakaSafe(day.incomeAmount)}",
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        fontSize = 10.5.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = Color(0xFF16A34A)
                                                    )
                                                )
                                            }
                                        }
                                    }

                                    // Row 2: Proportional Spending Progress Bar Meter
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(9.dp)
                                                .clip(RoundedCornerShape(5.dp))
                                                .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                                        ) {
                                            if (progressRatio > 0f) {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxHeight()
                                                        .fillMaxWidth(progressRatio)
                                                        .clip(RoundedCornerShape(5.dp))
                                                        .background(
                                                            when {
                                                                day.isPeak -> Brush.horizontalGradient(listOf(Color(0xFFFF3D00), Color(0xFFFF9100)))
                                                                day.isToday -> Brush.horizontalGradient(listOf(Color(0xFF059669), Color(0xFF10B981)))
                                                                day.amount >= dailyAverage && dailyAverage > 0 -> Brush.horizontalGradient(listOf(Color(0xFF6366F1), Color(0xFF8B5CF6)))
                                                                else -> Brush.horizontalGradient(listOf(Color(0xFF3B82F6), Color(0xFF60A5FA)))
                                                            }
                                                        )
                                                )
                                            }
                                        }

                                        Text(
                                            text = if (day.amount > 0 && rawMaxExpense > 0) "${(progressRatio * 100).toInt()}%" else "০%",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (day.amount > 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                            ),
                                            modifier = Modifier.width(32.dp),
                                            textAlign = TextAlign.End
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Summary Metrics 4-Column Grid
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                SummaryMetricMiniCard(
                    title = if (isEnglish) "Total" else "মোট ব্যয়",
                    value = formatTakaCompact(totalExpense),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                SummaryMetricMiniCard(
                    title = if (isEnglish) "Daily Avg" else "দৈনিক গড়",
                    value = formatTakaCompact(dailyAverage),
                    color = Color(0xFF6366F1),
                    modifier = Modifier.weight(1f)
                )
                SummaryMetricMiniCard(
                    title = if (isEnglish) "Peak Day" else "শীর্ষ ব্যয়",
                    value = if (peakDay != null) formatTakaCompact(peakDay.amount) else "৳০",
                    color = Color(0xFFEF4444),
                    modifier = Modifier.weight(1f)
                )
                SummaryMetricMiniCard(
                    title = if (isEnglish) "No Spend" else "খরচহীন",
                    value = if (isEnglish) "$zeroSpendDays d" else "${toBengaliNum(zeroSpendDays.toString())} দিন",
                    color = Color(0xFF10B981),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun TrendScopeChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
        modifier = modifier.testTag(testTag)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 11.5.sp,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp)
            )
        }
    }
}

@Composable
private fun SummaryMetricMiniCard(
    title: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = color.copy(alpha = 0.08f),
        border = BorderStroke(0.5.dp, color.copy(alpha = 0.22f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 5.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 9.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(1.5.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = color
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private fun calculateChartCeiling(maxAmount: Double, dailyAverage: Double): Double {
    return if (maxAmount > 0.0) maxAmount else 0.0
}

private fun formatDayAmount(amount: Double, isEnglish: Boolean): String {
    if (amount <= 0) return if (isEnglish) "৳0" else "৳০"
    val formatted = when {
        amount >= 100_000 -> {
            val lk = amount / 100_000.0
            if (isEnglish) String.format(Locale.US, "%.1fL", lk) else "${toBengaliNum(String.format(Locale.US, "%.1f", lk))}লা"
        }
        amount >= 1_000 -> {
            val k = amount / 1_000.0
            if (isEnglish) String.format(Locale.US, "%.1fk", k) else "${toBengaliNum(String.format(Locale.US, "%.1f", k))}কে"
        }
        else -> {
            if (isEnglish) String.format(Locale.US, "%.0f", amount) else toBengaliNum(String.format(Locale.US, "%.0f", amount))
        }
    }
    return "৳$formatted"
}

private fun toBengaliNum(input: String): String {
    val bnDigits = charArrayOf('০', '১', '২', '৩', '৪', '৫', '৬', '৭', '৮', '৯')
    val sb = StringBuilder()
    for (ch in input) {
        if (ch in '0'..'9') {
            sb.append(bnDigits[ch - '0'])
        } else {
            sb.append(ch)
        }
    }
    return sb.toString()
}

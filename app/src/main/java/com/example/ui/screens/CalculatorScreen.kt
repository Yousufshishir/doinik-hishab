package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.SimpleCategoryData
import com.example.data.model.formatTakaSafe
import com.example.ui.components.AppCardDefaults
import com.example.ui.viewmodel.ExpenseViewModel
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalculatorScreen(
    viewModel: ExpenseViewModel,
    onAddExpenseFromCalc: (amount: Double) -> Unit,
    onAddIncomeFromCalc: (amount: Double) -> Unit,
    modifier: Modifier = Modifier
) {
    val netBalance by viewModel.netBalance.collectAsState()
    val todayExpense by viewModel.todayExpense.collectAsState()
    val todayIncome by viewModel.todayIncome.collectAsState()
    val transactions by viewModel.transactions.collectAsState()
    val isEnglish by viewModel.isEnglish.collectAsState()

    // Memory storage for calculator
    var memoryValue by remember { mutableStateOf(0.0) }
    val calculationHistory = remember { mutableStateListOf<Pair<String, String>>() }
    var showBudgetRuleDialog by remember { mutableStateOf(false) }
    var showHistoryDialog by remember { mutableStateOf(false) }

    // Recent 5 transactions for the compact reference feed
    val recentItems = remember(transactions) {
        transactions.take(5)
    }

    var expression by remember { mutableStateOf("") }
    var resultText by remember { mutableStateOf("0") }

    fun evaluateExpression(expr: String): String {
        if (expr.isBlank()) return "0"
        return try {
            val sanitized = expr.replace("×", "*").replace("÷", "/")
            val res = evaluateSimpleMath(sanitized)
            if (res % 1.0 == 0.0) res.toLong().toString() else "%.2f".format(res)
        } catch (e: Exception) {
            resultText
        }
    }

    fun applyDirectMath(transform: (Double) -> Double) {
        val current = resultText.toDoubleOrNull() ?: 0.0
        val transformed = transform(current)
        val formatted = if (transformed % 1.0 == 0.0) transformed.toLong().toString() else "%.2f".format(transformed)
        if (expression.isNotEmpty()) {
            calculationHistory.add(0, expression to formatted)
        }
        resultText = formatted
        expression = formatted
    }

    fun onKeyClick(key: String) {
        when (key) {
            "C" -> {
                expression = ""
                resultText = "0"
            }
            "⌫" -> {
                if (expression.isNotEmpty()) {
                    expression = expression.dropLast(1)
                    resultText = if (expression.isNotEmpty()) evaluateExpression(expression) else "0"
                }
            }
            "=" -> {
                val eval = evaluateExpression(expression)
                if (expression.isNotBlank() && expression != eval) {
                    calculationHistory.add(0, expression to eval)
                    if (calculationHistory.size > 20) calculationHistory.removeLast()
                }
                resultText = eval
                expression = eval
            }
            "+", "-", "×", "÷", "%" -> {
                if (expression.isNotEmpty()) {
                    val lastChar = expression.last()
                    if (lastChar in listOf('+', '-', '×', '÷', '%')) {
                        expression = expression.dropLast(1) + key
                    } else {
                        expression += key
                    }
                }
            }
            else -> {
                // Digits or dot
                expression += key
                resultText = evaluateExpression(expression)
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (isEnglish) "Smart Calculator" else "স্মার্ট ক্যালকুলেটর",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = if (isEnglish) "Quick financial math & direct logging" else "হিসাব করুন ও সরাসরি খরচে যোগ করুন",
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Mini Financial Context Strip (Present hisab compact preview)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("calc_present_summary_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                border = AppCardDefaults.border(Color(0xFF6366F1))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isEnglish) "📊 Current Financial Overview (Tap to use in calculation):" else "📊 বর্তমান হিসাবের একনজর (ট্যাপ করে সংখ্যা নিন):",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    // 3 Metric Pills
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    val amt = if (netBalance % 1.0 == 0.0) netBalance.toLong().toString() else "%.2f".format(netBalance)
                                    expression += amt
                                    resultText = evaluateExpression(expression)
                                }
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text(if (isEnglish) "Balance" else "ব্যালেন্স", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(formatTakaSafe(netBalance), style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    val amt = if (todayExpense % 1.0 == 0.0) todayExpense.toLong().toString() else "%.2f".format(todayExpense)
                                    expression += amt
                                    resultText = evaluateExpression(expression)
                                }
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text(if (isEnglish) "Today Expense" else "আজকের খরচ", style = MaterialTheme.typography.labelSmall, color = Color(0xFFD32F2F))
                                Text(formatTakaSafe(todayExpense), style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = Color(0xFFD32F2F)))
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    val amt = if (todayIncome % 1.0 == 0.0) todayIncome.toLong().toString() else "%.2f".format(todayIncome)
                                    expression += amt
                                    resultText = evaluateExpression(expression)
                                }
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text(if (isEnglish) "Today Income" else "আজকের আয়", style = MaterialTheme.typography.labelSmall, color = Color(0xFF2E7D32))
                                Text(formatTakaSafe(todayIncome), style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32)))
                            }
                        }
                    }

                    // Recent mini transactions row
                    if (recentItems.isNotEmpty()) {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(recentItems) { item ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            val amt = if (item.amount % 1.0 == 0.0) item.amount.toLong().toString() else "%.2f".format(item.amount)
                                            expression += if (expression.isEmpty() || expression.last() in listOf('+', '-', '×', '÷')) amt else "+$amt"
                                            resultText = evaluateExpression(expression)
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        val displayTitle = if (isEnglish) com.example.ui.util.AppLocale.category(item.category, true) else item.title.take(10)
                                        Text(
                                            text = "$displayTitle: ",
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                        Text(
                                            text = formatTakaSafe(item.amount),
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Display Screen Box
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("calc_display_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                border = AppCardDefaults.border(MaterialTheme.colorScheme.primary),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = if (expression.isEmpty()) "0" else expression,
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontFamily = FontFamily.Monospace
                        ),
                        maxLines = 2,
                        textAlign = TextAlign.End
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.End
                    ) {
                        Text(
                            text = "৳ ",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        )
                        Text(
                            text = resultText,
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                letterSpacing = 1.sp
                            ),
                            maxLines = 1,
                            textAlign = TextAlign.End
                        )
                    }

                    // Direct Action Buttons: "খরচে যোগ করুন" & "আয়ে যোগ করুন"
                    val parsedResult = resultText.toDoubleOrNull() ?: 0.0
                    if (parsedResult > 0) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 2.dp),
                            horizontalArrangement = Arrangement.End
                        ) {
                            FilledTonalButton(
                                onClick = { onAddExpenseFromCalc(parsedResult) },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = Color(0xFFD32F2F).copy(alpha = 0.12f),
                                    contentColor = Color(0xFFD32F2F)
                                ),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Icon(imageVector = Icons.Default.TrendingDown, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (isEnglish) "Add to Expense" else "খরচে যোগ", fontWeight = FontWeight.Bold, fontSize = 11.5.sp)
                            }

                            Spacer(modifier = Modifier.width(6.dp))

                            FilledTonalButton(
                                onClick = { onAddIncomeFromCalc(parsedResult) },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = Color(0xFF2E7D32).copy(alpha = 0.12f),
                                    contentColor = Color(0xFF2E7D32)
                                ),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Icon(imageVector = Icons.Default.TrendingUp, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (isEnglish) "Add to Income" else "আয়ে যোগ", fontWeight = FontWeight.Bold, fontSize = 11.5.sp)
                            }
                        }
                    }
                }
            }

            // Financial Quick Utility Bar (VAT, Discounts, Split Bill, 50/30/20, Round, History)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // History Button
                Surface(
                    onClick = { showHistoryDialog = true },
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                    modifier = Modifier.height(26.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.History, contentDescription = null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = if (isEnglish) "History (${calculationHistory.size})" else "ইতিহাস (${calculationHistory.size})",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp, color = MaterialTheme.colorScheme.primary)
                        )
                    }
                }

                // 50/30/20 Budget Rule
                val parsedForBudget = resultText.toDoubleOrNull() ?: 0.0
                Surface(
                    onClick = { showBudgetRuleDialog = true },
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f),
                    modifier = Modifier.height(26.dp)
                ) {
                    Box(modifier = Modifier.padding(horizontal = 7.dp), contentAlignment = Alignment.Center) {
                        Text(
                            text = if (isEnglish) "50/30/20 Rule" else "৫০/৩০/২০ নিয়ম",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp, color = MaterialTheme.colorScheme.secondary)
                        )
                    }
                }

                // Round to nearest 10
                Surface(
                    onClick = {
                        applyDirectMath { kotlin.math.round(it / 10.0) * 10.0 }
                    },
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.height(26.dp)
                ) {
                    Box(modifier = Modifier.padding(horizontal = 7.dp), contentAlignment = Alignment.Center) {
                        Text(
                            text = if (isEnglish) "Round ৳10" else "রাউন্ড ৳১০",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp)
                        )
                    }
                }

                // +15% VAT
                Surface(
                    onClick = { applyDirectMath { it * 1.15 } },
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    modifier = Modifier.height(26.dp)
                ) {
                    Box(modifier = Modifier.padding(horizontal = 7.dp), contentAlignment = Alignment.Center) {
                        Text(if (isEnglish) "+15% VAT" else "+১৫% ভ্যাট", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp, color = MaterialTheme.colorScheme.primary))
                    }
                }

                // +5% Tax
                Surface(
                    onClick = { applyDirectMath { it * 1.05 } },
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                    modifier = Modifier.height(26.dp)
                ) {
                    Box(modifier = Modifier.padding(horizontal = 7.dp), contentAlignment = Alignment.Center) {
                        Text(if (isEnglish) "+5% Tax" else "+৫% ট্যাক্স", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp, color = MaterialTheme.colorScheme.primary))
                    }
                }

                // -10% Discount
                Surface(
                    onClick = { applyDirectMath { it * 0.90 } },
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF2E7D32).copy(alpha = 0.12f),
                    modifier = Modifier.height(26.dp)
                ) {
                    Box(modifier = Modifier.padding(horizontal = 7.dp), contentAlignment = Alignment.Center) {
                        Text(if (isEnglish) "-10% Disc" else "-১০% ছাড়", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp, color = Color(0xFF2E7D32)))
                    }
                }

                // -20% Discount
                Surface(
                    onClick = { applyDirectMath { it * 0.80 } },
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF2E7D32).copy(alpha = 0.12f),
                    modifier = Modifier.height(26.dp)
                ) {
                    Box(modifier = Modifier.padding(horizontal = 7.dp), contentAlignment = Alignment.Center) {
                        Text(if (isEnglish) "-20% Disc" else "-২০% ছাড়", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp, color = Color(0xFF2E7D32)))
                    }
                }

                // ÷2 Split Bill
                Surface(
                    onClick = { applyDirectMath { it / 2.0 } },
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.height(26.dp)
                ) {
                    Box(modifier = Modifier.padding(horizontal = 7.dp), contentAlignment = Alignment.Center) {
                        Text(if (isEnglish) "÷2 Split" else "÷২ ভাগ", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp))
                    }
                }

                // ÷3 Split Bill
                Surface(
                    onClick = { applyDirectMath { it / 3.0 } },
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.height(26.dp)
                ) {
                    Box(modifier = Modifier.padding(horizontal = 7.dp), contentAlignment = Alignment.Center) {
                        Text(if (isEnglish) "÷3 Split" else "÷৩ ভাগ", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp))
                    }
                }

                // ÷4 Split Bill
                Surface(
                    onClick = { applyDirectMath { it / 4.0 } },
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.height(26.dp)
                ) {
                    Box(modifier = Modifier.padding(horizontal = 7.dp), contentAlignment = Alignment.Center) {
                        Text(if (isEnglish) "÷4 Split" else "÷৪ ভাগ", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp))
                    }
                }

                // ± Negate
                Surface(
                    onClick = { applyDirectMath { it * -1.0 } },
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.height(26.dp)
                ) {
                    Box(modifier = Modifier.padding(horizontal = 7.dp), contentAlignment = Alignment.Center) {
                        Text("±", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp))
                    }
                }

                // M+ Memory Save
                Surface(
                    onClick = {
                        val cur = resultText.toDoubleOrNull() ?: 0.0
                        memoryValue += cur
                    },
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                    modifier = Modifier.height(26.dp)
                ) {
                    Box(modifier = Modifier.padding(horizontal = 7.dp), contentAlignment = Alignment.Center) {
                        Text(if (isEnglish) "M+ Save" else "M+ জমা", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 9.5.sp))
                    }
                }

                // MR Memory Recall
                if (memoryValue != 0.0) {
                    Surface(
                        onClick = {
                            val memStr = if (memoryValue % 1.0 == 0.0) memoryValue.toLong().toString() else "%.2f".format(memoryValue)
                            expression += memStr
                            resultText = evaluateExpression(expression)
                        },
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        modifier = Modifier.height(26.dp)
                    ) {
                        Box(modifier = Modifier.padding(horizontal = 7.dp), contentAlignment = Alignment.Center) {
                            Text("MR (${formatTakaSafe(memoryValue)})", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 9.5.sp))
                        }
                    }

                    Surface(
                        onClick = { memoryValue = 0.0 },
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                        modifier = Modifier.height(26.dp)
                    ) {
                        Box(modifier = Modifier.padding(horizontal = 7.dp), contentAlignment = Alignment.Center) {
                            Text("MC", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 9.5.sp, color = MaterialTheme.colorScheme.error))
                        }
                    }
                }
            }

            // Keypad Grid with reduced font size (14sp) and tighter padding/spacing (4dp)
            val buttons = listOf(
                listOf("C", "%", "⌫", "÷"),
                listOf("7", "8", "9", "×"),
                listOf("4", "5", "6", "-"),
                listOf("1", "2", "3", "+"),
                listOf("0", "00", ".", "=")
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                buttons.forEach { row ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        row.forEach { key ->
                            val isOperator = key in listOf("÷", "×", "-", "+")
                            val isAction = key in listOf("C", "⌫", "%")
                            val isEquals = key == "="

                            val containerColor = when {
                                isEquals -> MaterialTheme.colorScheme.primary
                                isOperator -> MaterialTheme.colorScheme.primaryContainer
                                isAction -> MaterialTheme.colorScheme.surfaceVariant
                                else -> MaterialTheme.colorScheme.surface
                            }

                            val contentColor = when {
                                isEquals -> MaterialTheme.colorScheme.onPrimary
                                isOperator -> MaterialTheme.colorScheme.onPrimaryContainer
                                isAction -> MaterialTheme.colorScheme.onSurfaceVariant
                                else -> MaterialTheme.colorScheme.onSurface
                            }

                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { onKeyClick(key) }
                                    .testTag("calc_key_$key"),
                                shape = RoundedCornerShape(8.dp),
                                color = containerColor,
                                tonalElevation = if (isEquals || isOperator) 2.dp else 1.dp
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    Text(
                                        text = key,
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = if (isEquals || isOperator) FontWeight.ExtraBold else FontWeight.SemiBold,
                                            color = contentColor,
                                            fontSize = if (key.length > 1) 12.5.sp else 14.5.sp
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Calculation History Dialog
            if (showHistoryDialog) {
                AlertDialog(
                    onDismissRequest = { showHistoryDialog = false },
                    title = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isEnglish) "Calculation History" else "পূর্ববর্তী হিসাবের ইতিহাস",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            if (calculationHistory.isNotEmpty()) {
                                IconButton(
                                    onClick = { calculationHistory.clear() },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.DeleteOutline, contentDescription = "Clear", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    },
                    text = {
                        if (calculationHistory.isEmpty()) {
                            Text(
                                text = if (isEnglish) "No calculations recorded yet. Equals (=) results will appear here." else "এখনও কোনো হিসাব রেকর্ড হয়নি। সমান (=) চাপলে এখানে জমা হবে।",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 280.dp)
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                calculationHistory.forEach { (expr, res) ->
                                    Surface(
                                        onClick = {
                                            expression = res
                                            resultText = res
                                            showHistoryDialog = false
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 12.dp, vertical = 8.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "$expr =",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = "৳ $res",
                                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showHistoryDialog = false }) {
                            Text(if (isEnglish) "Close" else "বন্ধ করুন")
                        }
                    }
                )
            }

            // 50/30/20 Budget Breakdown Dialog
            if (showBudgetRuleDialog) {
                val currentAmount = resultText.toDoubleOrNull() ?: 0.0
                AlertDialog(
                    onDismissRequest = { showBudgetRuleDialog = false },
                    title = {
                        Text(
                            text = if (isEnglish) "50/30/20 Budget Rule" else "৫০/৩০/২০ বাজেট নিয়ম",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    },
                    text = {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = if (isEnglish)
                                    "Breakdown for ৳${formatTakaSafe(currentAmount)}:"
                                else
                                    "বর্তমান ৳${formatTakaSafe(currentAmount)} এর জন্য বিভাজন:",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                            )

                            // 50% Needs
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(
                                        text = if (isEnglish) "50% Needs (Rent, Food, Utility)" else "৫০% জরুরি প্রয়োজন (বাসাভাড়া, খাবার, বিল)",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Text(
                                        text = "৳ ${formatTakaSafe(currentAmount * 0.50)}",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                    )
                                }
                            }

                            // 30% Wants
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(
                                        text = if (isEnglish) "30% Wants (Shopping, Entertainment)" else "৩০% শখ ও বিনোদন (কেনাকাটা, ঘোরাঘুরি)",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Text(
                                        text = "৳ ${formatTakaSafe(currentAmount * 0.30)}",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                                    )
                                }
                            }

                            // 20% Savings
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFF2E7D32).copy(alpha = 0.12f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(
                                        text = if (isEnglish) "20% Savings & Debt Payment" else "২০% সঞ্চয় ও ঋণ পরিশোধ",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                                    )
                                    Text(
                                        text = "৳ ${formatTakaSafe(currentAmount * 0.20)}",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                                    )
                                }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showBudgetRuleDialog = false }) {
                            Text(if (isEnglish) "Got it" else "ঠিক আছে")
                        }
                    }
                )
            }
        }
    }
}

// Simple Math Evaluator supporting +, -, *, /, %
private fun evaluateSimpleMath(expression: String): Double {
    val tokens = mutableListOf<String>()
    var currentNumber = StringBuilder()

    for (ch in expression) {
        if (ch.isDigit() || ch == '.') {
            currentNumber.append(ch)
        } else if (ch in listOf('+', '-', '*', '/', '%')) {
            if (currentNumber.isNotEmpty()) {
                tokens.add(currentNumber.toString())
                currentNumber = StringBuilder()
            }
            tokens.add(ch.toString())
        }
    }
    if (currentNumber.isNotEmpty()) {
        tokens.add(currentNumber.toString())
    }

    if (tokens.isEmpty()) return 0.0

    // First pass: *, /, %
    val pass1 = mutableListOf<String>()
    var i = 0
    while (i < tokens.size) {
        val token = tokens[i]
        if (token in listOf("*", "/", "%") && i > 0 && i + 1 < tokens.size) {
            val left = pass1.removeAt(pass1.size - 1).toDoubleOrNull() ?: 0.0
            val right = tokens[i + 1].toDoubleOrNull() ?: 1.0
            val res = when (token) {
                "*" -> left * right
                "/" -> if (right != 0.0) left / right else 0.0
                "%" -> (left * right) / 100.0
                else -> left
            }
            pass1.add(res.toString())
            i += 2
        } else {
            pass1.add(token)
            i++
        }
    }

    // Second pass: +, -
    var finalResult = pass1.getOrNull(0)?.toDoubleOrNull() ?: 0.0
    var j = 1
    while (j < pass1.size) {
        val op = pass1[j]
        val right = pass1.getOrNull(j + 1)?.toDoubleOrNull() ?: 0.0
        when (op) {
            "+" -> finalResult += right
            "-" -> finalResult -= right
        }
        j += 2
    }

    return finalResult
}

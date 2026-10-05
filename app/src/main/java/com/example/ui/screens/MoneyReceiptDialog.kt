package com.example.ui.screens

import com.example.ui.util.clearFocusOnTap

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.example.ui.components.AppToastManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.data.model.DebtEntity
import com.example.data.model.KhatEntity
import com.example.data.model.SavingsGoalEntity
import com.example.data.model.TransactionEntity
import com.example.data.model.formatTakaSafe
import com.example.ui.components.AppCardDefaults
import com.example.ui.viewmodel.ExpenseViewModel
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.max

@Composable
fun MoneyReceiptDialog(
    viewModel: ExpenseViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val userName by viewModel.userName.collectAsState()
    val userPhone by viewModel.userPhone.collectAsState()
    val isEnglish by viewModel.isEnglish.collectAsState()

    val allTransactions by viewModel.transactions.collectAsState()
    val allDebts by viewModel.debts.collectAsState()
    val allSavingsGoals by viewModel.savingsGoals.collectAsState()
    val allKhats by viewModel.khats.collectAsState()

    val monthlyBudget by viewModel.monthlyBudget.collectAsState()
    val monthlyTotalExpense by viewModel.monthlyExpense.collectAsState()
    val monthlyTotalIncome by viewModel.monthlyIncome.collectAsState()
    val monthlyNetBalance by viewModel.monthlySavings.collectAsState()

    // Filter selector: "ALL" (All-Time), "MONTH" (Selected Month/Year), "DATE" (Selected Date)
    var receiptFilter by remember { mutableStateOf("ALL") }
    // View Mode: "COMPACT" (Executive Overview) vs "FULL_TABLE" (Every Row on Screen)
    var viewMode by remember { mutableStateOf("FULL_TABLE") }
    // Optional search in table preview
    var searchKeyword by remember { mutableStateOf("") }

    val now = Calendar.getInstance()
    val todayYear = now.get(Calendar.YEAR)
    val todayMonth = now.get(Calendar.MONTH) // 0-based
    val todayDay = now.get(Calendar.DAY_OF_MONTH)

    var selectedYear by remember { mutableStateOf(todayYear) }
    var selectedMonth by remember { mutableStateOf(todayMonth) }
    var selectedDay by remember { mutableStateOf(todayDay) }

    var downloadedFile by remember { mutableStateOf<File?>(null) }
    var publicSavedPath by remember { mutableStateOf<String?>(null) }
    var isGeneratingPdf by remember { mutableStateOf(false) }

    val monthNamesEn = remember {
        listOf("January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December")
    }
    val monthNamesBn = remember {
        listOf("জানুয়ারি", "ফেব্রুয়ারি", "মার্চ", "এপ্রিল", "মে", "জুন", "জুলাই", "আগস্ট", "সেপ্টেম্বর", "অক্টোবর", "নভেম্বর", "ডিসেম্বর")
    }

    val openMonthPicker = {
        val dp = android.app.DatePickerDialog(
            context,
            { _, year, month, _ ->
                selectedYear = year
                selectedMonth = month
                receiptFilter = "MONTH"
                downloadedFile = null
            },
            selectedYear,
            selectedMonth,
            1
        )
        dp.setTitle(if (isEnglish) "Select Month & Year" else "মাস ও বছর নির্বাচন করুন")
        dp.show()
    }

    val openDatePicker = {
        val dp = android.app.DatePickerDialog(
            context,
            { _, year, month, day ->
                selectedYear = year
                selectedMonth = month
                selectedDay = day
                receiptFilter = "DATE"
                downloadedFile = null
            },
            selectedYear,
            selectedMonth,
            selectedDay
        )
        dp.setTitle(if (isEnglish) "Select Date" else "তারিখ নির্বাচন করুন")
        dp.show()
    }

    val displayMonthTitle = if (isEnglish) "${monthNamesEn[selectedMonth]} $selectedYear" else "${monthNamesBn[selectedMonth]} $selectedYear"

    val displayDateTitle = remember(selectedYear, selectedMonth, selectedDay, isEnglish) {
        val cal = Calendar.getInstance().apply { set(selectedYear, selectedMonth, selectedDay) }
        if (isEnglish) {
            SimpleDateFormat("dd MMM yyyy", Locale.US).format(cal.time)
        } else {
            "$selectedDay ${monthNamesBn[selectedMonth]} $selectedYear"
        }
    }

    val itemCal = Calendar.getInstance()
    val filteredList = remember(allTransactions, receiptFilter, selectedYear, selectedMonth, selectedDay, searchKeyword) {
        val base = allTransactions.filter { item ->
            itemCal.timeInMillis = item.timestamp
            when (receiptFilter) {
                "DATE" -> itemCal.get(Calendar.YEAR) == selectedYear &&
                          itemCal.get(Calendar.MONTH) == selectedMonth &&
                          itemCal.get(Calendar.DAY_OF_MONTH) == selectedDay
                "MONTH" -> itemCal.get(Calendar.YEAR) == selectedYear &&
                           itemCal.get(Calendar.MONTH) == selectedMonth
                else -> true // "ALL"
            }
        }
        if (searchKeyword.isBlank()) {
            base
        } else {
            val kw = searchKeyword.lowercase().trim()
            base.filter {
                it.title.lowercase().contains(kw) ||
                        it.category.lowercase().contains(kw) ||
                        it.note.lowercase().contains(kw)
            }
        }
    }

    val totalIncome = remember(filteredList) {
        filteredList.filter { it.type == "INCOME" }.sumOf { it.amount }
    }
    val totalExpense = remember(filteredList) {
        filteredList.filter { it.type == "EXPENSE" }.sumOf { it.amount }
    }
    val netBalance = totalIncome - totalExpense

    val pendingReceivable = remember(allDebts) {
        allDebts.filter { it.type == "RECEIVE" && !it.isSettled }.sumOf { it.amount }
    }
    val pendingPayable = remember(allDebts) {
        allDebts.filter { it.type == "PAY" && !it.isSettled }.sumOf { it.amount }
    }
    val totalSavedInGoals = remember(allSavingsGoals) {
        allSavingsGoals.sumOf { goal ->
            val isGoalCompleted = goal.isCompleted || (goal.targetAmount > 0 && goal.savedAmount >= goal.targetAmount)
            if (isGoalCompleted) goal.targetAmount else goal.savedAmount
        }
    }

    // Top Expense Categories for Charts
    val topExpenseCategories = remember(filteredList) {
        filteredList.filter { it.type == "EXPENSE" }
            .groupBy { it.category }
            .mapValues { entry -> entry.value.sumOf { it.amount } }
            .toList()
            .sortedByDescending { it.second }
            .take(5)
    }

    val receiptDateStr = remember {
        SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.US).format(Date())
    }
    val receiptNumber = remember {
        val datePart = SimpleDateFormat("yyMMdd", Locale.US).format(Date())
        val randomPart = (1000..9999).random()
        "$datePart-$randomPart"
    }

    val scopeText = if (isEnglish) {
        when (receiptFilter) {
            "DATE" -> "Daily Statement: $displayDateTitle"
            "MONTH" -> "Monthly Statement: $displayMonthTitle"
            else -> "All-Time Statement"
        }
    } else {
        when (receiptFilter) {
            "DATE" -> "দৈনিক খতিয়ান: $displayDateTitle"
            "MONTH" -> "মাসিক খতিয়ান: $displayMonthTitle"
            else -> "সকল লেনদেনের খতিয়ান"
        }
    }

    // Notification permission launcher for Android 13+
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    /**
     * UNLIMITED MULTI-PAGE PDF GENERATOR:
     * Generates a luxury, eye-catching financial statement with proper pagination.
     * Can print any number of transactions from 1 to thousands across multiple A4 pages without limit!
     */
    fun generateUnlimitedMultiPagePdf(pdfIsEnglish: Boolean = isEnglish): PdfDocument {
        val pageWidth = 595 // Standard A4 width in points
        val pageHeight = 842 // Standard A4 height in points
        val leftMargin = 28f
        val rightMargin = pageWidth - 28f
        val maxContentY = 788f // Bottom margin threshold before new page

        val document = PdfDocument()
        val paint = Paint().apply { isAntiAlias = true }
        val dateFormat = SimpleDateFormat("dd/MM/yy hh:mm a", Locale.US)
        val rowDateFormat = SimpleDateFormat("dd MMM yyyy", Locale.US)
        val rowTimeFormat = SimpleDateFormat("hh:mm a", Locale.US)

        var pageNumber = 1
        var currentPageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
        var currentPage = document.startPage(currentPageInfo)
        var canvas = currentPage.canvas

        // Helper to draw common footer on a page
        fun drawPageFooter(c: android.graphics.Canvas, pNum: Int) {
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 0.6f
            paint.color = android.graphics.Color.parseColor("#CBD5E1")
            c.drawLine(leftMargin, 804f, rightMargin, 804f, paint)

            paint.style = Paint.Style.FILL
            paint.textSize = 7.5f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            paint.color = android.graphics.Color.parseColor("#64748B")
            val footerL = if (pdfIsEnglish) {
                "Daily Expense Tracker • Certified Digital Financial Statement • Confidential"
            } else {
                "দৈনিক খরচের হিসাব • ডিজিটাল আর্থিক বিবরণী ও খতিয়ান • গোপনীয় নথিপত্র"
            }
            c.drawText(footerL, leftMargin, 818f, paint)

            val pageLabel = if (pdfIsEnglish) "Page $pNum" else "পৃষ্ঠা $pNum"
            val textW = paint.measureText(pageLabel)
            c.drawText(pageLabel, rightMargin - textW, 818f, paint)
        }

        // Helper to start a brand-new page with continuous header
        fun startNextPage(): android.graphics.Canvas {
            drawPageFooter(canvas, pageNumber)
            document.finishPage(currentPage)
            pageNumber++
            currentPageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
            currentPage = document.startPage(currentPageInfo)
            val newCanvas = currentPage.canvas

            // Running header on page 2+
            paint.style = Paint.Style.FILL
            paint.color = android.graphics.Color.parseColor("#0F172A")
            newCanvas.drawRect(leftMargin, 20f, rightMargin, 38f, paint)

            paint.color = android.graphics.Color.parseColor("#F59E0B") // Amber gold trim
            newCanvas.drawRect(leftMargin, 38f, rightMargin, 40f, paint)

            paint.color = android.graphics.Color.WHITE
            paint.textSize = 8.5f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            val runTitle = if (pdfIsEnglish) {
                "DAILY EXPENSE TRACKER — Statement #$receiptNumber (Continued)"
            } else {
                "দৈনিক খরচের হিসাব — আর্থিক খতিয়ান #$receiptNumber (চলমান অংশ)"
            }
            newCanvas.drawText(runTitle, leftMargin + 10f, 32f, paint)

            val pText = if (pdfIsEnglish) "Page $pageNumber" else "পৃষ্ঠা $pageNumber"
            newCanvas.drawText(pText, rightMargin - 50f, 32f, paint)

            return newCanvas
        }

        // =========================================================================
        // PAGE 1: LUXURY EXECUTIVE BANNER & OFFICIAL SEAL
        // =========================================================================
        // 1. Dark Obsidian Navy Top Banner
        paint.color = android.graphics.Color.parseColor("#0A1128")
        canvas.drawRect(0f, 0f, pageWidth.toFloat(), 88f, paint)

        // Luxury Gold Accent Trim Bar
        paint.color = android.graphics.Color.parseColor("#D4AF37")
        canvas.drawRect(0f, 88f, pageWidth.toFloat(), 91f, paint)

        // Title & Branding
        paint.color = android.graphics.Color.parseColor("#F1F5F9")
        paint.textSize = 19f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(if (pdfIsEnglish) "Daily Expense Tracker" else "দৈনিক খরচের হিসাব", leftMargin, 36f, paint)

        paint.textSize = 8.5f
        paint.color = android.graphics.Color.parseColor("#F59E0B")
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(if (pdfIsEnglish) "★ OFFICIAL FINANCIAL STATEMENT & AUDIT LEDGER ★" else "★ অফিসিয়াল আর্থিক বিবরণী ও অডিট খতিয়ান ★", leftMargin, 52f, paint)

        paint.textSize = 7.5f
        paint.color = android.graphics.Color.parseColor("#94A3B8")
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText(if (pdfIsEnglish) "COMPREHENSIVE CASH FLOW • DEBTS • SAVINGS • AUDIT STATEMENT" else "সমন্বিত নগদ প্রবাহ • ধার-দেনা • সঞ্চয় অডিট খতিয়ান", leftMargin, 66f, paint)

        // Circular Digital Stamp / Seal on Top Right
        val stampCenterX = rightMargin - 40f
        val stampCenterY = 44f
        paint.color = android.graphics.Color.parseColor("#D4AF37")
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1.2f
        canvas.drawCircle(stampCenterX, stampCenterY, 32f, paint)
        paint.strokeWidth = 0.6f
        canvas.drawCircle(stampCenterX, stampCenterY, 28f, paint)

        paint.style = Paint.Style.FILL
        paint.textSize = 5.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.color = android.graphics.Color.parseColor("#F59E0B")
        canvas.drawText("★ CERTIFIED ★", stampCenterX - 18f, stampCenterY - 10f, paint)
        paint.textSize = 7f
        paint.color = android.graphics.Color.WHITE
        canvas.drawText("VERIFIED", stampCenterX - 16f, stampCenterY + 1f, paint)
        paint.textSize = 5.5f
        paint.color = android.graphics.Color.parseColor("#94A3B8")
        canvas.drawText("AUDIT SECURE", stampCenterX - 18f, stampCenterY + 11f, paint)

        // 2. Account Holder & Statement Metadata Box
        var currentY = 104f
        val userCardRect = RectF(leftMargin, currentY, rightMargin, currentY + 62f)
        paint.color = android.graphics.Color.parseColor("#F8FAFC")
        canvas.drawRoundRect(userCardRect, 6f, 6f, paint)
        paint.color = android.graphics.Color.parseColor("#CBD5E1")
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 0.8f
        canvas.drawRoundRect(userCardRect, 6f, 6f, paint)
        paint.style = Paint.Style.FILL

        // Left Metadata
        val displayName = if (userName.isNotBlank()) userName else (if (pdfIsEnglish) "Account Holder" else "হিসাবধারী")
        val displayPhone = if (userPhone.isNotBlank()) userPhone else (if (pdfIsEnglish) "Unspecified" else "অনুল্লেখিত")

        paint.color = android.graphics.Color.parseColor("#0F172A")
        paint.textSize = 9.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(if (pdfIsEnglish) "Account Holder: $displayName" else "হিসাবধারী: $displayName", leftMargin + 12f, currentY + 18f, paint)

        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 8.5f
        paint.color = android.graphics.Color.parseColor("#475569")
        canvas.drawText(if (pdfIsEnglish) "Mobile / ID: $displayPhone" else "মোবাইল / আইডি: $displayPhone", leftMargin + 12f, currentY + 34f, paint)

        paint.color = android.graphics.Color.parseColor("#0284C7")
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(if (pdfIsEnglish) "Receipt ID: #$receiptNumber" else "রিসিট নং: #$receiptNumber", leftMargin + 12f, currentY + 50f, paint)

        // Right Metadata
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.color = android.graphics.Color.parseColor("#475569")
        canvas.drawText(if (pdfIsEnglish) "Generated: $receiptDateStr" else "প্রস্তুতের সময়: $receiptDateStr", 300f, currentY + 18f, paint)
        canvas.drawText(if (pdfIsEnglish) "Scope: $scopeText" else "আওতা: $scopeText", 300f, currentY + 34f, paint)

        paint.color = android.graphics.Color.parseColor("#15803D")
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(
            if (pdfIsEnglish) "Records: ${filteredList.size} items" else "মোট রেকর্ড: ${filteredList.size} টি",
            300f,
            currentY + 50f,
            paint
        )

        // 3. Four Executive Financial Summary Cards (Income, Expense, Balance, Debts)
        currentY += 72f
        val gap = 6f
        val cardW = (rightMargin - leftMargin - (gap * 3)) / 4f
        val cardH = 46f

        // Card 1: Total Inflow (Income)
        val r1 = RectF(leftMargin, currentY, leftMargin + cardW, currentY + cardH)
        paint.color = android.graphics.Color.parseColor("#F0FDF4")
        canvas.drawRoundRect(r1, 5f, 5f, paint)
        paint.color = android.graphics.Color.parseColor("#15803D")
        paint.textSize = 8f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText(if (pdfIsEnglish) "TOTAL INFLOW (INCOME)" else "মোট জমা (আয়)", leftMargin + 8f, currentY + 14f, paint)
        paint.textSize = 10.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(formatTakaSafe(totalIncome), leftMargin + 8f, currentY + 34f, paint)

        // Card 2: Total Outflow (Expense)
        val x2 = leftMargin + cardW + gap
        val r2 = RectF(x2, currentY, x2 + cardW, currentY + cardH)
        paint.color = android.graphics.Color.parseColor("#FEF2F2")
        canvas.drawRoundRect(r2, 5f, 5f, paint)
        paint.color = android.graphics.Color.parseColor("#B91C1C")
        paint.textSize = 8f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText(if (pdfIsEnglish) "TOTAL OUTFLOW (EXP)" else "মোট খরচ (ব্যয়)", x2 + 8f, currentY + 14f, paint)
        paint.textSize = 10.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(formatTakaSafe(totalExpense), x2 + 8f, currentY + 34f, paint)

        // Card 3: Net Cash Balance
        val x3 = x2 + cardW + gap
        val r3 = RectF(x3, currentY, x3 + cardW, currentY + cardH)
        paint.color = android.graphics.Color.parseColor("#EFF6FF")
        canvas.drawRoundRect(r3, 5f, 5f, paint)
        paint.color = if (netBalance >= 0) android.graphics.Color.parseColor("#1D4ED8") else android.graphics.Color.parseColor("#DC2626")
        paint.textSize = 8f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText(if (pdfIsEnglish) "NET BALANCE" else "নিট ব্যালেন্স", x3 + 8f, currentY + 14f, paint)
        paint.textSize = 10.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(formatTakaSafe(netBalance), x3 + 8f, currentY + 34f, paint)

        // Card 4: Debts Equilibrium
        val x4 = x3 + cardW + gap
        val r4 = RectF(x4, currentY, x4 + cardW, currentY + cardH)
        paint.color = android.graphics.Color.parseColor("#FFFBEB")
        canvas.drawRoundRect(r4, 5f, 5f, paint)
        paint.color = android.graphics.Color.parseColor("#B45309")
        paint.textSize = 8f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText(if (pdfIsEnglish) "DUE STATUS" else "ধার-দেনা স্থিতি", x4 + 8f, currentY + 14f, paint)
        paint.textSize = 8.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        val pLabel = if (pdfIsEnglish) "Get: " else "পাব: "
        val dLabel = if (pdfIsEnglish) "Give: " else "দেনা: "
        canvas.drawText("$pLabel${formatTakaSafe(pendingReceivable)}", x4 + 8f, currentY + 28f, paint)
        paint.color = android.graphics.Color.parseColor("#DC2626")
        canvas.drawText("$dLabel${formatTakaSafe(pendingPayable)}", x4 + 8f, currentY + 40f, paint)

        // 4. Visual Cashflow Ratio Bar
        currentY += cardH + 10f
        val totalFlow = totalIncome + totalExpense
        val incRatio = if (totalFlow > 0) (totalIncome / totalFlow).toFloat() else 0.5f
        val barW = rightMargin - leftMargin
        val barH = 10f

        paint.color = android.graphics.Color.parseColor("#16A34A")
        val incW = barW * incRatio
        canvas.drawRoundRect(RectF(leftMargin, currentY, leftMargin + incW, currentY + barH), 3f, 3f, paint)
        paint.color = android.graphics.Color.parseColor("#DC2626")
        canvas.drawRoundRect(RectF(leftMargin + incW, currentY, rightMargin, currentY + barH), 3f, 3f, paint)

        currentY += barH + 10f
        paint.textSize = 7.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.color = android.graphics.Color.parseColor("#15803D")
        val incPct = (incRatio * 100).toInt()
        val expPct = 100 - incPct
        canvas.drawText(if (pdfIsEnglish) "■ Income: $incPct% (${formatTakaSafe(totalIncome)})" else "■ আয়: $incPct% (${formatTakaSafe(totalIncome)})", leftMargin, currentY, paint)

        paint.color = android.graphics.Color.parseColor("#B91C1C")
        canvas.drawText(if (pdfIsEnglish) "■ Expense: $expPct% (${formatTakaSafe(totalExpense)})" else "■ ব্যয়: $expPct% (${formatTakaSafe(totalExpense)})", 300f, currentY, paint)

        // Visual Top Spending Category Pills (Premium Executive Representation)
        if (topExpenseCategories.isNotEmpty() && totalExpense > 0) {
            currentY += 14f
            paint.textSize = 7f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.color = android.graphics.Color.parseColor("#475569")
            val catHeader = if (pdfIsEnglish) "TOP EXPENSE CATEGORIES:" else "শীর্ষ ব্যয়ের খাতসমূহ:"
            canvas.drawText(catHeader, leftMargin, currentY, paint)

            var pillX = leftMargin + (if (pdfIsEnglish) 110f else 96f)
            topExpenseCategories.take(4).forEach { (catName, catAmt) ->
                val pct = ((catAmt / totalExpense) * 100).toInt()
                val catTrans = com.example.ui.util.AppLocale.category(catName, pdfIsEnglish)
                val pillText = "$catTrans: $pct% (${formatTakaSafe(catAmt)})"
                paint.textSize = 6.5f
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                val textW = paint.measureText(pillText)
                val pillW = textW + 12f

                if (pillX + pillW < rightMargin) {
                    paint.color = android.graphics.Color.parseColor("#F1F5F9")
                    paint.style = Paint.Style.FILL
                    canvas.drawRoundRect(RectF(pillX, currentY - 8f, pillX + pillW, currentY + 4f), 3f, 3f, paint)
                    paint.color = android.graphics.Color.parseColor("#CBD5E1")
                    paint.style = Paint.Style.STROKE
                    paint.strokeWidth = 0.5f
                    canvas.drawRoundRect(RectF(pillX, currentY - 8f, pillX + pillW, currentY + 4f), 3f, 3f, paint)

                    paint.style = Paint.Style.FILL
                    paint.color = android.graphics.Color.parseColor("#334155")
                    canvas.drawText(pillText, pillX + 6f, currentY, paint)
                    pillX += pillW + 6f
                }
            }
        }

        // =========================================================================
        // TABLE 1: MASTER TRANSACTIONS LEDGER (UNIQUE & PREMIUM PRESENTATION)
        // =========================================================================
        currentY += 16f

        // Table 1 Column Positions:
        // SL (24), Date (74), Title (170), Category (95), Type (65), Amount (111) => Total 539
        val colSl = leftMargin
        val colDate = leftMargin + 24f
        val colTitle = colDate + 74f
        val colCat = colTitle + 170f
        val colType = colCat + 95f
        val colAmt = colType + 65f

        fun drawTransactionTableHeader(c: android.graphics.Canvas, y: Float) {
            paint.color = android.graphics.Color.parseColor("#0F172A")
            c.drawRect(leftMargin, y, rightMargin, y + 20f, paint)

            paint.color = android.graphics.Color.WHITE
            paint.textSize = 8.5f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            val secLabel = if (pdfIsEnglish) {
                "1. MASTER TRANSACTION LEDGER (${filteredList.size} ITEMS)"
            } else {
                "১. আয়-ব্যয় খতিয়ান (${filteredList.size} টি রেকর্ড)"
            }
            c.drawText(secLabel, leftMargin + 8f, y + 14f, paint)

            // Column Names Subheader
            val subY = y + 20f
            paint.color = android.graphics.Color.parseColor("#F1F5F9")
            c.drawRect(leftMargin, subY, rightMargin, subY + 18f, paint)

            paint.color = android.graphics.Color.parseColor("#334155")
            paint.textSize = 8f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            c.drawText("#", colSl + 4f, subY + 12f, paint)
            c.drawText(if (pdfIsEnglish) "Date / Time" else "তারিখ ও সময়", colDate + 4f, subY + 12f, paint)
            c.drawText(if (pdfIsEnglish) "Description / Title" else "বিবরণ / শিরোনাম", colTitle + 4f, subY + 12f, paint)
            c.drawText(if (pdfIsEnglish) "Category" else "খাত / ক্যাটাগরি", colCat + 4f, subY + 12f, paint)
            c.drawText(if (pdfIsEnglish) "Type" else "ধরন", colType + 4f, subY + 12f, paint)

            val amtHeader = if (pdfIsEnglish) "Amount" else "টাকার পরিমাণ"
            val amtHW = paint.measureText(amtHeader)
            c.drawText(amtHeader, (rightMargin - 6f) - amtHW, subY + 12f, paint)
        }

        drawTransactionTableHeader(canvas, currentY)
        currentY += 38f
        val rowHeight = 25f

        if (filteredList.isEmpty()) {
            paint.color = android.graphics.Color.parseColor("#94A3B8")
            paint.textSize = 8.5f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            canvas.drawText(
                if (pdfIsEnglish) "No transactions found in this period." else "এই সময়কালের মধ্যে কোনো লেনদেন পাওয়া যায়নি।",
                leftMargin + 10f,
                currentY + 14f,
                paint
            )
            currentY += 20f
        } else {
            filteredList.forEachIndexed { index, item ->
                // Check if we need to paginate to next page
                if (currentY + rowHeight > maxContentY) {
                    canvas = startNextPage()
                    currentY = 52f
                    drawTransactionTableHeader(canvas, currentY)
                    currentY += 38f
                }

                // Zebra striping
                paint.color = if (index % 2 == 1) android.graphics.Color.parseColor("#F8FAFC") else android.graphics.Color.WHITE
                canvas.drawRect(leftMargin, currentY, rightMargin, currentY + rowHeight, paint)

                val isExp = item.type == "EXPENSE"

                // Left visual indicator strip (Emerald green for Income, Crimson for Expense)
                paint.color = if (isExp) android.graphics.Color.parseColor("#EF4444") else android.graphics.Color.parseColor("#10B981")
                canvas.drawRect(leftMargin, currentY, leftMargin + 3f, currentY + rowHeight, paint)

                // Serial
                paint.textSize = 7.5f
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                paint.color = android.graphics.Color.parseColor("#64748B")
                canvas.drawText("${index + 1}", colSl + 6f, currentY + 15f, paint)

                // Date & Time (2 lines)
                val dStr = rowDateFormat.format(Date(item.timestamp))
                val tStr = rowTimeFormat.format(Date(item.timestamp))
                paint.textSize = 7f
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                paint.color = android.graphics.Color.parseColor("#0F172A")
                canvas.drawText(dStr, colDate + 4f, currentY + 10.5f, paint)
                paint.textSize = 6f
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                paint.color = android.graphics.Color.parseColor("#64748B")
                canvas.drawText(tStr, colDate + 4f, currentY + 19.5f, paint)

                // Title & Note (2 lines)
                val safeTitle = if (item.title.length > 25) item.title.take(23) + ".." else item.title
                paint.color = android.graphics.Color.parseColor("#0F172A")
                paint.textSize = 8f
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                canvas.drawText(safeTitle, colTitle + 4f, currentY + 10.5f, paint)

                val noteText = if (item.note.isNotBlank()) {
                    if (item.note.length > 26) item.note.take(24) + ".." else item.note
                } else {
                    "-"
                }
                paint.color = android.graphics.Color.parseColor("#64748B")
                paint.textSize = 6.5f
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
                canvas.drawText(noteText, colTitle + 4f, currentY + 19.5f, paint)

                // Category (Inside a sleek rounded capsule)
                val transCat = com.example.ui.util.AppLocale.category(item.category, pdfIsEnglish)
                val safeCat = if (transCat.length > 14) transCat.take(12) + ".." else transCat
                val catRect = RectF(colCat + 2f, currentY + 4f, colCat + 88f, currentY + 20f)
                paint.style = Paint.Style.FILL
                paint.color = android.graphics.Color.parseColor("#F1F5F9")
                canvas.drawRoundRect(catRect, 3.5f, 3.5f, paint)
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 0.5f
                paint.color = android.graphics.Color.parseColor("#CBD5E1")
                canvas.drawRoundRect(catRect, 3.5f, 3.5f, paint)

                paint.style = Paint.Style.FILL
                paint.color = android.graphics.Color.parseColor("#334155")
                paint.textSize = 7f
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                canvas.drawText(safeCat, colCat + 6f, currentY + 14.5f, paint)

                // Type Capsule (Inside a colored badge)
                val typeRect = RectF(colType + 2f, currentY + 4f, colType + 58f, currentY + 20f)
                paint.style = Paint.Style.FILL
                paint.color = if (isExp) android.graphics.Color.parseColor("#FEE2E2") else android.graphics.Color.parseColor("#DCFCE7")
                canvas.drawRoundRect(typeRect, 3.5f, 3.5f, paint)
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 0.5f
                paint.color = if (isExp) android.graphics.Color.parseColor("#FECACA") else android.graphics.Color.parseColor("#BBF7D0")
                canvas.drawRoundRect(typeRect, 3.5f, 3.5f, paint)

                paint.style = Paint.Style.FILL
                paint.color = if (isExp) android.graphics.Color.parseColor("#B91C1C") else android.graphics.Color.parseColor("#15803D")
                paint.textSize = 6.5f
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                val typeLabel = if (isExp) (if (pdfIsEnglish) "EXPENSE" else "ব্যয়") else (if (pdfIsEnglish) "INCOME" else "আয়")
                canvas.drawText(typeLabel, colType + 6f, currentY + 14.5f, paint)

                // Amount (Right-aligned bold text)
                val sign = if (isExp) "(-)" else "(+)"
                val amtStr = "$sign ${formatTakaSafe(item.amount)}"
                paint.color = if (isExp) android.graphics.Color.parseColor("#DC2626") else android.graphics.Color.parseColor("#16A34A")
                paint.textSize = 9.5f
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                val amtW = paint.measureText(amtStr)
                canvas.drawText(amtStr, (rightMargin - 6f) - amtW, currentY + 15.5f, paint)

                // Micro-divider line at bottom of row
                paint.color = android.graphics.Color.parseColor("#E2E8F0")
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 0.4f
                canvas.drawLine(leftMargin, currentY + rowHeight, rightMargin, currentY + rowHeight, paint)
                paint.style = Paint.Style.FILL

                currentY += rowHeight
            }
        }

        // =========================================================================
        // TABLE 2: DEBTS & RECEIVABLES (ধার-দেনা ও বাকি খাতা)
        // =========================================================================
        if (allDebts.isNotEmpty()) {
            if (currentY + 80f > maxContentY) {
                canvas = startNextPage()
                currentY = 52f
            } else {
                currentY += 14f
            }

            paint.color = android.graphics.Color.parseColor("#1E293B")
            canvas.drawRect(leftMargin, currentY, rightMargin, currentY + 18f, paint)
            paint.color = android.graphics.Color.WHITE
            paint.textSize = 8.5f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            val dTitle = if (pdfIsEnglish) {
                "2. DEBTS & BORROWINGS STATUS (${allDebts.size} RECORDS)"
            } else {
                "২. ধার-দেনা ও বাকি হিসাবের স্থিতি (${allDebts.size} টি রেকর্ড)"
            }
            canvas.drawText(dTitle, leftMargin + 8f, currentY + 13f, paint)

            currentY += 18f
            paint.color = android.graphics.Color.parseColor("#F1F5F9")
            canvas.drawRect(leftMargin, currentY, rightMargin, currentY + 16f, paint)
            paint.color = android.graphics.Color.parseColor("#475569")
            paint.textSize = 8f
            canvas.drawText("#", leftMargin + 4f, currentY + 11f, paint)
            canvas.drawText(if (pdfIsEnglish) "Person Name" else "ব্যক্তির নাম", leftMargin + 26f, currentY + 11f, paint)
            canvas.drawText(if (pdfIsEnglish) "Type" else "ধরন", leftMargin + 180f, currentY + 11f, paint)
            canvas.drawText(if (pdfIsEnglish) "Amount" else "পরিমাণ", leftMargin + 280f, currentY + 11f, paint)
            canvas.drawText(if (pdfIsEnglish) "Due Date / Source" else "তারিখ / উৎস", leftMargin + 370f, currentY + 11f, paint)
            canvas.drawText(if (pdfIsEnglish) "Status" else "স্ট্যাটাস", leftMargin + 470f, currentY + 11f, paint)

            currentY += 16f
            allDebts.forEachIndexed { dIdx, debt ->
                if (currentY + rowHeight > maxContentY) {
                    canvas = startNextPage()
                    currentY = 52f
                }

                if (dIdx % 2 == 1) {
                    paint.color = android.graphics.Color.parseColor("#F8FAFC")
                    canvas.drawRect(leftMargin, currentY, rightMargin, currentY + rowHeight, paint)
                }

                val isRec = debt.type == "RECEIVE"
                paint.color = android.graphics.Color.parseColor("#64748B")
                paint.textSize = 7.5f
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                canvas.drawText("${dIdx + 1}", leftMargin + 4f, currentY + 12f, paint)

                paint.color = android.graphics.Color.parseColor("#0F172A")
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                canvas.drawText(debt.personName.take(20), leftMargin + 26f, currentY + 12f, paint)

                paint.color = if (isRec) android.graphics.Color.parseColor("#15803D") else android.graphics.Color.parseColor("#DC2626")
                val debtLabel = if (isRec) (if (pdfIsEnglish) "Receivable (Get)" else "পাওনা (আমি পাব)") else (if (pdfIsEnglish) "Payable (Give)" else "দেনা (আমি দেব)")
                canvas.drawText(debtLabel, leftMargin + 180f, currentY + 12f, paint)

                paint.color = android.graphics.Color.parseColor("#0F172A")
                canvas.drawText(formatTakaSafe(debt.amount), leftMargin + 280f, currentY + 12f, paint)

                paint.color = android.graphics.Color.parseColor("#64748B")
                val sourceTag = when {
                    debt.khatName.isNotBlank() -> if (pdfIsEnglish) "Khat: ${debt.khatName}" else "খাত: ${debt.khatName}"
                    debt.deductedFromMain -> if (pdfIsEnglish) "Main Bal" else "মূল ব্যালেন্স"
                    else -> ""
                }
                val dateOrNote = when {
                    sourceTag.isNotBlank() && debt.dueDate.isNotBlank() -> "$sourceTag • ${debt.dueDate}"
                    sourceTag.isNotBlank() -> sourceTag
                    debt.dueDate.isNotBlank() -> debt.dueDate
                    debt.note.isNotBlank() -> debt.note
                    else -> "-"
                }
                canvas.drawText(dateOrNote.take(16), leftMargin + 370f, currentY + 12f, paint)

                paint.color = if (debt.isSettled) android.graphics.Color.parseColor("#15803D") else android.graphics.Color.parseColor("#B45309")
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                val stLabel = if (debt.isSettled) (if (pdfIsEnglish) "Settled" else "পরিশোধিত") else (if (pdfIsEnglish) "Pending" else "বাকি আছে")
                canvas.drawText(stLabel, leftMargin + 470f, currentY + 12f, paint)

                currentY += rowHeight

                // Sub-row: Loan Date, Due Date, Paid Amount, Remaining Balance
                if (currentY + 12f > maxContentY) {
                    canvas = startNextPage()
                    currentY = 52f
                }
                val loanTakenDateStr = try {
                    SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(debt.timestamp))
                } catch (_: Exception) {
                    "-"
                }
                val dueStr = if (debt.dueDate.isNotBlank()) debt.dueDate else "-"
                paint.color = android.graphics.Color.parseColor("#475569")
                paint.textSize = 7f
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                val detailLine = if (pdfIsEnglish) {
                    "  Taken: $loanTakenDateStr • Due: $dueStr • Paid: ${formatTakaSafe(debt.effectivePaidAmount)} • Due Bal: ${formatTakaSafe(debt.remainingAmount)}"
                } else {
                    "  নেওয়া: $loanTakenDateStr • ফেরত: $dueStr • পরিশোধ: ${formatTakaSafe(debt.effectivePaidAmount)} • অবশিষ্ট বাকি: ${formatTakaSafe(debt.remainingAmount)}"
                }
                canvas.drawText(detailLine, leftMargin + 26f, currentY + 9f, paint)
                currentY += 12f

                // Installments breakdown in PDF (if any payments recorded)
                val pHistory = debt.parsePaymentHistory()
                if (pHistory.isNotEmpty()) {
                    pHistory.forEachIndexed { pIdx, pRec ->
                        if (currentY + 11f > maxContentY) {
                            canvas = startNextPage()
                            currentY = 52f
                        }
                        val pDateStr = try {
                            SimpleDateFormat("dd/MM/yy", Locale.getDefault()).format(Date(pRec.timestamp))
                        } catch (_: Exception) {
                            "-"
                        }
                        paint.color = android.graphics.Color.parseColor("#047857")
                        paint.textSize = 6.5f
                        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                        val pNoteStr = if (pRec.note.isNotBlank()) " (${pRec.note.take(15)})" else ""
                        val instLine = if (pdfIsEnglish) {
                            "     ↳ Inst #${pIdx + 1}: $pDateStr • +${formatTakaSafe(pRec.amount)} (Bal: ${formatTakaSafe(pRec.remainingAfter)})$pNoteStr"
                        } else {
                            "     ↳ কিস্তি #${pIdx + 1}: $pDateStr • +${formatTakaSafe(pRec.amount)} (বাকি: ${formatTakaSafe(pRec.remainingAfter)})$pNoteStr"
                        }
                        canvas.drawText(instLine, leftMargin + 26f, currentY + 8f, paint)
                        currentY += 11f
                    }
                }
            }
        }

        // =========================================================================
        // TABLE 3: SAVINGS GOALS (সঞ্চয় লক্ষ্যমাত্রা)
        // =========================================================================
        if (allSavingsGoals.isNotEmpty()) {
            if (currentY + 70f > maxContentY) {
                canvas = startNextPage()
                currentY = 52f
            } else {
                currentY += 14f
            }

            paint.color = android.graphics.Color.parseColor("#1E293B")
            canvas.drawRect(leftMargin, currentY, rightMargin, currentY + 18f, paint)
            paint.color = android.graphics.Color.WHITE
            paint.textSize = 8.5f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            val sTitle = if (pdfIsEnglish) {
                "3. SAVINGS GOALS PORTFOLIO (${allSavingsGoals.size} GOALS)"
            } else {
                "৩. সঞ্চয় লক্ষ্যমাত্রা ও অর্জনের স্থিতি (${allSavingsGoals.size} টি লক্ষ্য)"
            }
            canvas.drawText(sTitle, leftMargin + 8f, currentY + 13f, paint)

            currentY += 18f
            paint.color = android.graphics.Color.parseColor("#F1F5F9")
            canvas.drawRect(leftMargin, currentY, rightMargin, currentY + 16f, paint)
            paint.color = android.graphics.Color.parseColor("#475569")
            paint.textSize = 8f
            canvas.drawText("#", leftMargin + 4f, currentY + 11f, paint)
            canvas.drawText(if (pdfIsEnglish) "Goal Title" else "লক্ষ্যের নাম", leftMargin + 24f, currentY + 11f, paint)
            canvas.drawText(if (pdfIsEnglish) "Target" else "টার্গেট", leftMargin + 180f, currentY + 11f, paint)
            canvas.drawText(if (pdfIsEnglish) "Saved" else "মোট জমা", leftMargin + 270f, currentY + 11f, paint)
            canvas.drawText(if (pdfIsEnglish) "Status" else "স্ট্যাটাস", leftMargin + 370f, currentY + 11f, paint)
            canvas.drawText(if (pdfIsEnglish) "Progress" else "অগ্রগতি", leftMargin + 460f, currentY + 11f, paint)

            currentY += 16f
            allSavingsGoals.forEachIndexed { sIdx, goal ->
                val isGoalCompleted = goal.isCompleted || (goal.targetAmount > 0 && goal.savedAmount >= goal.targetAmount)
                val displaySaved = if (isGoalCompleted) goal.targetAmount else goal.savedAmount
                val displayProgress = if (isGoalCompleted) 100 else if (goal.targetAmount > 0) ((goal.savedAmount / goal.targetAmount) * 100).toInt() else 0
                val history = goal.parseHistory()

                if (currentY + rowHeight > maxContentY) {
                    canvas = startNextPage()
                    currentY = 52f
                }

                if (sIdx % 2 == 1) {
                    paint.color = android.graphics.Color.parseColor("#F8FAFC")
                    canvas.drawRect(leftMargin, currentY, rightMargin, currentY + rowHeight, paint)
                }

                paint.color = android.graphics.Color.parseColor("#64748B")
                paint.textSize = 7.5f
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                canvas.drawText("${sIdx + 1}", leftMargin + 4f, currentY + 12f, paint)

                paint.color = android.graphics.Color.parseColor("#0F172A")
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                canvas.drawText(goal.title.take(22), leftMargin + 24f, currentY + 12f, paint)

                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                paint.color = android.graphics.Color.parseColor("#475569")
                canvas.drawText(formatTakaSafe(goal.targetAmount), leftMargin + 180f, currentY + 12f, paint)

                paint.color = android.graphics.Color.parseColor("#15803D")
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                canvas.drawText(formatTakaSafe(displaySaved), leftMargin + 270f, currentY + 12f, paint)

                if (isGoalCompleted) {
                    paint.color = android.graphics.Color.parseColor("#047857")
                    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    canvas.drawText(if (pdfIsEnglish) "[COMPLETED]" else "[সম্পন্ন]", leftMargin + 370f, currentY + 12f, paint)
                } else {
                    paint.color = android.graphics.Color.parseColor("#D97706")
                    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                    canvas.drawText(if (pdfIsEnglish) "Active" else "চলমান", leftMargin + 370f, currentY + 12f, paint)
                }

                paint.color = if (isGoalCompleted) android.graphics.Color.parseColor("#047857") else android.graphics.Color.parseColor("#0284C7")
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                canvas.drawText("$displayProgress%", leftMargin + 460f, currentY + 12f, paint)

                currentY += rowHeight

                // Sub-rows: Print recent transaction history of the goal
                if (history.isNotEmpty()) {
                    val recentHistory = history.takeLast(4)
                    recentHistory.forEach { hItem ->
                        if (currentY + 13f > maxContentY) {
                            canvas = startNextPage()
                            currentY = 52f
                        }
                        paint.color = android.graphics.Color.parseColor("#94A3B8")
                        paint.textSize = 6.5f
                        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
                        val hTypeStr = if (hItem.type == "DEPOSIT") (if (pdfIsEnglish) "+Dep:" else "+জমা:") else (if (pdfIsEnglish) "-Wd:" else "-উত্তোলন:")
                        val hDateStr = SimpleDateFormat("dd/MM/yy", Locale.getDefault()).format(Date(hItem.timestamp))
                        val hNoteStr = if (!hItem.note.isNullOrBlank()) " (${hItem.note.take(15)})" else ""
                        val hText = "  • $hDateStr $hTypeStr ${formatTakaSafe(hItem.amount)}$hNoteStr | Bal: ${formatTakaSafe(hItem.balanceAfter)}"
                        canvas.drawText(hText, leftMargin + 24f, currentY + 9.5f, paint)
                        currentY += 13f
                    }
                }
            }
        }

        // =========================================================================
        // TABLE 4: KHAT DISTRIBUTION & ALLOCATION (খাত ভিত্তিক বণ্টন ও স্থিতি)
        // =========================================================================
        if (allKhats.isNotEmpty()) {
            if (currentY + 70f > maxContentY) {
                canvas = startNextPage()
                currentY = 52f
            } else {
                currentY += 14f
            }

            paint.color = android.graphics.Color.parseColor("#1E293B")
            canvas.drawRect(leftMargin, currentY, rightMargin, currentY + 18f, paint)
            paint.color = android.graphics.Color.WHITE
            paint.textSize = 8.5f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            val kTitle = if (pdfIsEnglish) {
                "4. KHAT BUDGET & EXPENSE DISTRIBUTION (${allKhats.size} KHATS)"
            } else {
                "৪. খাত ভিত্তিক বণ্টন ও খরচের স্থিতি (${allKhats.size} টি খাত)"
            }
            canvas.drawText(kTitle, leftMargin + 8f, currentY + 13f, paint)

            currentY += 18f
            paint.color = android.graphics.Color.parseColor("#F1F5F9")
            canvas.drawRect(leftMargin, currentY, rightMargin, currentY + 16f, paint)
            paint.color = android.graphics.Color.parseColor("#475569")
            paint.textSize = 8f
            canvas.drawText("#", leftMargin + 4f, currentY + 11f, paint)
            canvas.drawText(if (pdfIsEnglish) "Khat Name" else "খাতের নাম", leftMargin + 24f, currentY + 11f, paint)
            canvas.drawText(if (pdfIsEnglish) "Allocated / Balance" else "বরাদ্দ / অবশিষ্ট স্থিতি", leftMargin + 190f, currentY + 11f, paint)
            canvas.drawText(if (pdfIsEnglish) "Total Spent" else "মোট ব্যয়", leftMargin + 320f, currentY + 11f, paint)
            canvas.drawText(if (pdfIsEnglish) "Transactions" else "লেনদেন সংখ্যা", leftMargin + 430f, currentY + 11f, paint)

            currentY += 16f
            allKhats.forEachIndexed { kIdx, khat ->
                val khatTxs = filteredList.filter { it.khatId == khat.id || it.khatName.equals(khat.name, ignoreCase = true) }
                val khatSpent = khatTxs.filter { it.type == "EXPENSE" }.sumOf { it.amount }

                if (currentY + rowHeight > maxContentY) {
                    canvas = startNextPage()
                    currentY = 52f
                }

                if (kIdx % 2 == 1) {
                    paint.color = android.graphics.Color.parseColor("#F8FAFC")
                    canvas.drawRect(leftMargin, currentY, rightMargin, currentY + rowHeight, paint)
                }

                paint.color = android.graphics.Color.parseColor("#64748B")
                paint.textSize = 7.5f
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                canvas.drawText("${kIdx + 1}", leftMargin + 4f, currentY + 12f, paint)

                paint.color = android.graphics.Color.parseColor("#0F172A")
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                canvas.drawText(khat.name.take(24), leftMargin + 24f, currentY + 12f, paint)

                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                paint.color = android.graphics.Color.parseColor("#2563EB")
                canvas.drawText(formatTakaSafe(khat.allocatedAmount), leftMargin + 190f, currentY + 12f, paint)

                paint.color = android.graphics.Color.parseColor("#DC2626")
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                canvas.drawText(formatTakaSafe(khatSpent), leftMargin + 320f, currentY + 12f, paint)

                paint.color = android.graphics.Color.parseColor("#475569")
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                canvas.drawText("${khatTxs.size} entries", leftMargin + 430f, currentY + 12f, paint)

                currentY += rowHeight
            }
        }

        // =========================================================================
        // FINAL SECTION: MONTHLY PERFORMANCE AUDIT & OFFICIAL AUTHENTICATION
        // =========================================================================
        if (currentY + 120f > maxContentY) {
            canvas = startNextPage()
            currentY = 52f
        } else {
            currentY += 16f
        }

        // Monthly Summary Pill Card
        val auditCardRect = RectF(leftMargin, currentY, rightMargin, currentY + 44f)
        paint.color = android.graphics.Color.parseColor("#F8FAFC")
        canvas.drawRoundRect(auditCardRect, 6f, 6f, paint)
        paint.color = android.graphics.Color.parseColor("#CBD5E1")
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 0.8f
        canvas.drawRoundRect(auditCardRect, 6f, 6f, paint)
        paint.style = Paint.Style.FILL

        paint.textSize = 8f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.color = android.graphics.Color.parseColor("#1E293B")
        val mLabel1 = if (pdfIsEnglish) "Monthly Income: " else "চলতি মাসের আয়: "
        val mLabel2 = if (pdfIsEnglish) "Monthly Expense: " else "চলতি মাসের ব্যয়: "
        val mLabel3 = if (pdfIsEnglish) "Monthly Net: " else "মাসিক নিট স্থিতি: "
        val mLabel4 = if (pdfIsEnglish) "Budget Target: " else "বাজেট টার্গেট: "
        canvas.drawText("$mLabel1${formatTakaSafe(monthlyTotalIncome)}", leftMargin + 12f, currentY + 16f, paint)
        canvas.drawText("$mLabel2${formatTakaSafe(monthlyTotalExpense)}", leftMargin + 12f, currentY + 32f, paint)

        canvas.drawText("$mLabel3${formatTakaSafe(monthlyNetBalance)}", 300f, currentY + 16f, paint)
        val budStr = if (monthlyBudget > 0) formatTakaSafe(monthlyBudget) else (if (pdfIsEnglish) "Not set" else "নির্ধারিত নেই")
        canvas.drawText("$mLabel4$budStr", 300f, currentY + 32f, paint)

        // Authentication & Signature Block
        currentY += 56f
        paint.color = android.graphics.Color.parseColor("#0F172A")
        paint.textSize = 8.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("✓ VERIFIED DIGITAL FINANCIAL VOUCHER & STATEMENT", leftMargin, currentY, paint)

        paint.color = android.graphics.Color.parseColor("#64748B")
        paint.textSize = 7.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText(
            if (pdfIsEnglish) "Generated automatically by Daily Expense Tracker. Official financial audit statement."
            else "দৈনিক খরচের হিসাব অ্যাপ থেকে সংকলিত। অফিসিয়াল অডিট বিবরণী।",
            leftMargin,
            currentY + 12f,
            paint
        )

        // Simulated Barcode Pattern (High-craft official finish)
        val barCodeY = currentY + 22f
        val barCodeX = leftMargin
        val pattern = listOf(2f, 1f, 3f, 1f, 2f, 4f, 1f, 2f, 1f, 3f, 2f, 1f, 4f, 2f, 1f, 3f, 1f, 2f, 3f, 1f, 2f, 4f, 1f, 3f, 2f, 1f)
        var curBx = barCodeX
        paint.color = android.graphics.Color.parseColor("#0F172A")
        paint.style = Paint.Style.FILL
        pattern.forEachIndexed { pIdx, w ->
            if (pIdx % 2 == 0) {
                canvas.drawRect(curBx, barCodeY, curBx + w, barCodeY + 16f, paint)
            }
            curBx += w + 1f
        }
        paint.textSize = 6f
        paint.color = android.graphics.Color.parseColor("#64748B")
        paint.typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
        canvas.drawText("* EXP-REC-$receiptNumber *", barCodeX, barCodeY + 24f, paint)

        // Account holder signature placeholder
        paint.color = android.graphics.Color.parseColor("#1E293B")
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 8f
        val sigLabel = if (pdfIsEnglish) "Account Holder Signature: __________________" else "হিসাবধারীর স্বাক্ষর: __________________"
        canvas.drawText(sigLabel, 300f, currentY + 24f, paint)

        // Finish last page
        drawPageFooter(canvas, pageNumber)
        document.finishPage(currentPage)
        return document
    }

    /**
     * Saves Unlimited Multi-Page PDF to phone's public Downloads directory.
     */
    fun savePdfToPhoneStorage(): Pair<File, String>? {
        return try {
            val fileName = "HishabStatement_${receiptNumber}.pdf"
            val document = generateUnlimitedMultiPagePdf(isEnglish)

            // 1. Direct write to phone's public Downloads directory
            val publicDownloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (!publicDownloadsDir.exists()) {
                publicDownloadsDir.mkdirs()
            }
            val targetFile = File(publicDownloadsDir, fileName)

            FileOutputStream(targetFile).use { out ->
                document.writeTo(out)
                out.flush()
            }

            // 2. Insert into MediaStore.Downloads (so Android Download Manager picks it up on Android 10+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                try {
                    val values = ContentValues().apply {
                        put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                        put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                        put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                    }
                    val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                    if (uri != null) {
                        context.contentResolver.openOutputStream(uri)?.use { out ->
                            document.writeTo(out)
                            out.flush()
                        }
                    }
                } catch (ignored: Exception) {
                    // Fallback
                }
            }

            document.close()

            // 3. Inform MediaScanner so file is indexed immediately
            MediaScannerConnection.scanFile(
                context,
                arrayOf(targetFile.absolutePath),
                arrayOf("application/pdf"),
                null
            )

            val displayLocation = if (isEnglish) "Phone's 'Downloads' folder" else "ফোনের 'Downloads' (ডাউনলোড) ফোল্ডার"
            Pair(targetFile, displayLocation)
        } catch (e: Exception) {
            // Fallback to app-specific download folder if external storage threw permission error
            try {
                val fileName = "HishabStatement_${receiptNumber}.pdf"
                val document = generateUnlimitedMultiPagePdf(isEnglish)
                val fallbackDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.filesDir
                val fallbackFile = File(fallbackDir, fileName)
                FileOutputStream(fallbackFile).use { out ->
                    document.writeTo(out)
                    out.flush()
                }
                document.close()
                Pair(fallbackFile, fallbackFile.absolutePath)
            } catch (err: Exception) {
                err.printStackTrace()
                null
            }
        }
    }

    fun showDownloadNotification(file: File) {
        try {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channelId = "receipt_downloads_channel"

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    channelId,
                    if (isEnglish) "Receipt Downloads" else "রিসিট ডাউনলোড",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = if (isEnglish) "Money receipt & cash memo download notifications" else "মানি রিসিট ও ক্যাশ মেমো ডাউনলোড নোটিফিকেশন"
                    enableVibration(true)
                }
                notificationManager.createNotificationChannel(channel)
            }

            val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
            val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            val pendingIntent = PendingIntent.getActivity(
                context,
                System.currentTimeMillis().toInt(),
                viewIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notifTitle = if (isEnglish) "✓ Financial Statement Download Complete" else "✓ আর্থিক রিসিট ডাউনলোড সম্পন্ন"
            val notifSub = if (isEnglish) "${file.name} • Tap to view all records" else "${file.name} • দেখতে এখানে ট্যাপ করুন"
            val userLabel = if (userName.isNotBlank()) userName else (if (isEnglish) "User" else "ব্যবহারকারী")
            val notifBigText = if (isEnglish) {
                "File: ${file.name}\nAccount: $userLabel\nScope: $scopeText\nTotal Records: ${filteredList.size}\nTap to open multi-page PDF."
            } else {
                "ফাইল: ${file.name}\nহিসাবধারী: $userLabel\nআওতা: $scopeText\nমোট রেকর্ড: ${filteredList.size} টি\nপিডিএফ দেখতে এখানে ট্যাপ করুন।"
            }

            val notification = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(android.R.drawable.stat_sys_download_done)
                .setContentTitle(notifTitle)
                .setContentText(notifSub)
                .setStyle(NotificationCompat.BigTextStyle().bigText(notifBigText))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .build()

            notificationManager.notify(1001, notification)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun openPdfFile(file: File) {
        try {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
            val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(viewIntent)
        } catch (e: Exception) {
            AppToastManager.show(if (isEnglish) "No app found to open PDF" else "পিডিএফ দেখার জন্য কোনো অ্যাপ পাওয়া যায়নি")
        }
    }

    fun sharePdfFile(file: File) {
        try {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, if (isEnglish) "Financial Statement - $scopeText" else "আর্থিক বিবরণী - $scopeText")
                putExtra(Intent.EXTRA_TEXT, if (isEnglish) "Official financial statement generated from Daily Expense Tracker." else "দৈনিক খরচের হিসাব অ্যাপ থেকে সংকলিত অফিসিয়াল আর্থিক বিবরণী।")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, if (isEnglish) "Share PDF" else "পিডিএফ শেয়ার করুন"))
        } catch (e: Exception) {
            AppToastManager.show(if (isEnglish) "Failed to share" else "শেয়ার করতে সমস্যা হয়েছে")
        }
    }

    fun downloadPdfReceipt() {
        isGeneratingPdf = true
        try {
            val result = savePdfToPhoneStorage()
            if (result != null) {
                val (file, location) = result
                downloadedFile = file
                publicSavedPath = location
                showDownloadNotification(file)
                AppToastManager.show(
                    if (isEnglish) "✓ Full Statement (${filteredList.size} records) saved to Downloads folder!" else "✓ সম্পূর্ণ বিবরণী (${filteredList.size} টি রেকর্ড) Downloads ফোল্ডারে সংরক্ষিত হয়েছে!"
                )
            } else {
                AppToastManager.show(if (isEnglish) "Failed to generate PDF" else "পিডিএফ তৈরিতে ব্যর্থ হয়েছে")
            }
        } catch (e: Exception) {
            AppToastManager.show(if (isEnglish) "Error: ${e.message}" else "ত্রুটি: ${e.message}")
        } finally {
            isGeneratingPdf = false
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.94f)
                .clip(RoundedCornerShape(24.dp))
                .testTag("money_receipt_dialog"),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Top Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF0F172A)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ReceiptLong,
                                contentDescription = null,
                                tint = Color(0xFFF59E0B),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = if (isEnglish) "Executive Money Receipt & Ledger" else "এক্সিকিউটিভ মানি রিসিট ও খতিয়ান",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = Color(0xFF0284C7).copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = if (isEnglish) "OFFICIAL LEDGER" else "অফিসিয়াল খতিয়ান",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 9.sp,
                                            color = Color(0xFF0284C7)
                                        ),
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isEnglish) "${filteredList.size} Records" else "${filteredList.size} টি রেকর্ড",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = if (isEnglish) "Close" else "বন্ধ")
                    }
                }

                // Scope Selector Filter (All Time, Monthly with Calendar, Daily with Calendar)
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(3.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val filters = listOf(
                            Triple("ALL", if (isEnglish) "All Time" else "সকল সময়", Icons.Default.AllInclusive),
                            Triple("MONTH", if (isEnglish) "Monthly" else "মাসভিত্তিক", Icons.Default.CalendarMonth),
                            Triple("DATE", if (isEnglish) "Daily" else "তারিখভিত্তিক", Icons.Default.Today)
                        )

                        filters.forEach { (key, label, icon) ->
                            val isSelected = receiptFilter == key
                            Button(
                                onClick = {
                                    receiptFilter = key
                                    downloadedFile = null
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSelected) Color(0xFF0F172A) else Color.Transparent,
                                    contentColor = if (isSelected) Color(0xFFF59E0B) else MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
                                elevation = if (isSelected) ButtonDefaults.buttonElevation(defaultElevation = 2.dp) else null
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(13.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            fontSize = 10.5.sp
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }

                // Dynamic Calendar Picker Bar for Month or Date
                if (receiptFilter == "MONTH") {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            IconButton(
                                onClick = {
                                    if (selectedMonth == 0) {
                                        selectedMonth = 11
                                        selectedYear -= 1
                                    } else {
                                        selectedMonth -= 1
                                    }
                                    downloadedFile = null
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = if (isEnglish) "Previous Month" else "পূর্ববর্তী মাস",
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }

                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surface)
                                    .clickable { openMonthPicker() }
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CalendarMonth,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(17.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = displayMonthTitle,
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.EditCalendar,
                                    contentDescription = if (isEnglish) "Pick Month" else "মাস বাছাই করুন",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(14.dp)
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (selectedMonth != todayMonth || selectedYear != todayYear) {
                                    TextButton(
                                        onClick = {
                                            selectedYear = todayYear
                                            selectedMonth = todayMonth
                                            downloadedFile = null
                                        },
                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = if (isEnglish) "This Month" else "চলতি মাস",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }

                                IconButton(
                                    onClick = {
                                        if (selectedMonth == 11) {
                                            selectedMonth = 0
                                            selectedYear += 1
                                        } else {
                                            selectedMonth += 1
                                        }
                                        downloadedFile = null
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                        contentDescription = if (isEnglish) "Next Month" else "পরবর্তী মাস",
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                } else if (receiptFilter == "DATE") {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            IconButton(
                                onClick = {
                                    val cal = Calendar.getInstance().apply {
                                        set(selectedYear, selectedMonth, selectedDay)
                                        add(Calendar.DAY_OF_MONTH, -1)
                                    }
                                    selectedYear = cal.get(Calendar.YEAR)
                                    selectedMonth = cal.get(Calendar.MONTH)
                                    selectedDay = cal.get(Calendar.DAY_OF_MONTH)
                                    downloadedFile = null
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = if (isEnglish) "Previous Day" else "পূর্ববর্তী দিন",
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }

                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surface)
                                    .clickable { openDatePicker() }
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Today,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(17.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = displayDateTitle,
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.EditCalendar,
                                    contentDescription = if (isEnglish) "Pick Date" else "তারিখ বাছাই করুন",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(14.dp)
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (selectedDay != todayDay || selectedMonth != todayMonth || selectedYear != todayYear) {
                                    TextButton(
                                        onClick = {
                                            selectedYear = todayYear
                                            selectedMonth = todayMonth
                                            selectedDay = todayDay
                                            downloadedFile = null
                                        },
                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = if (isEnglish) "Today" else "আজকে",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }

                                IconButton(
                                    onClick = {
                                        val cal = Calendar.getInstance().apply {
                                            set(selectedYear, selectedMonth, selectedDay)
                                            add(Calendar.DAY_OF_MONTH, 1)
                                        }
                                        selectedYear = cal.get(Calendar.YEAR)
                                        selectedMonth = cal.get(Calendar.MONTH)
                                        selectedDay = cal.get(Calendar.DAY_OF_MONTH)
                                        downloadedFile = null
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                        contentDescription = if (isEnglish) "Next Day" else "পরবর্তী দিন",
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }

                // Search & View Mode Switch Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = searchKeyword,
                        onValueChange = { searchKeyword = it },
                        placeholder = {
                            Text(
                                if (isEnglish) "Search records..." else "রেকর্ড খুঁজুন...",
                                style = MaterialTheme.typography.labelSmall
                            )
                        },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        trailingIcon = {
                            if (searchKeyword.isNotBlank()) {
                                IconButton(onClick = { searchKeyword = "" }) {
                                    Icon(imageVector = Icons.Default.Close, contentDescription = null, modifier = Modifier.size(14.dp))
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        textStyle = MaterialTheme.typography.bodySmall
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    // View Mode Toggle (Full Table vs Compact)
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                viewMode = if (viewMode == "FULL_TABLE") "COMPACT" else "FULL_TABLE"
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (viewMode == "FULL_TABLE") Icons.Default.TableChart else Icons.Default.ViewCompact,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (viewMode == "FULL_TABLE") (if (isEnglish) "Table View" else "টেবিল ভিউ") else (if (isEnglish) "Compact" else "সংক্ষিপ্ত"),
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                // =========================================================================
                // MAIN EYE-CATCHING VOUCHER RECEIPT CONTAINER
                // =========================================================================
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                    ),
                    border = AppCardDefaults.border(Color(0xFF0284C7))
                ) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Official Cash Memo Voucher Header Banner
                        item {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFF0F172A)
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(imageVector = Icons.Default.Verified, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (isEnglish) "OFFICIAL FINANCIAL STATEMENT & LEDGER" else "অফিসিয়াল আর্থিক খতিয়ান ও ক্যাশ ভাউচার",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.ExtraBold,
                                                letterSpacing = 1.sp,
                                                color = Color(0xFFF59E0B)
                                            )
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = if (isEnglish) "Daily Expense Tracker" else "দৈনিক খরচের হিসাব",
                                        style = MaterialTheme.typography.titleLarge.copy(
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color.White
                                        )
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "VOUCHER #$receiptNumber • $receiptDateStr",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = Color(0xFF94A3B8),
                                            fontSize = 9.5.sp
                                        )
                                    )
                                }
                            }
                        }

                        // Account Holder & Statement Scope Bar
                        item {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = androidx.compose.foundation.BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = if (isEnglish) "ACCOUNT HOLDER" else "হিসাবধারী:",
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.5.sp, color = MaterialTheme.colorScheme.outline)
                                        )
                                        Text(
                                            text = if (userName.isNotBlank()) userName else (if (isEnglish) "Valued User" else "সম্মানিত ব্যবহারকারী"),
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                        )
                                        Text(
                                            text = if (userPhone.isNotBlank()) userPhone else (if (isEnglish) "Mobile Unspecified" else "মোবাইল অনুল্লেখিত"),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = if (isEnglish) "STATEMENT SCOPE" else "বিবরণীর আওতা:",
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.5.sp, color = MaterialTheme.colorScheme.outline)
                                        )
                                        Text(
                                            text = scopeText,
                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                        )
                                        Text(
                                            text = if (isEnglish) "${filteredList.size} Total Items" else "সর্বমোট ${filteredList.size} টি হিসাব",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold, color = Color(0xFF059669))
                                        )
                                    }
                                }
                            }
                        }

                        // 4 Executive Financial Metric Cards (Income, Expense, Net, Debts)
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                // Income
                                Card(
                                    modifier = Modifier.weight(1f),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF2E7D32).copy(alpha = 0.12f)),
                                    shape = RoundedCornerShape(10.dp),
                                    border = AppCardDefaults.incomeBorder()
                                ) {
                                    Column(modifier = Modifier.padding(8.dp)) {
                                        Text(if (isEnglish) "Total Inflow" else "মোট আয়", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, color = Color(0xFF2E7D32)))
                                        Text(formatTakaSafe(totalIncome), style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = Color(0xFF1B5E20)))
                                    }
                                }

                                // Expense
                                Card(
                                    modifier = Modifier.weight(1f),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFD32F2F).copy(alpha = 0.12f)),
                                    shape = RoundedCornerShape(10.dp),
                                    border = AppCardDefaults.expenseBorder()
                                ) {
                                    Column(modifier = Modifier.padding(8.dp)) {
                                        Text(if (isEnglish) "Total Outflow" else "মোট ব্যয়", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, color = Color(0xFFD32F2F)))
                                        Text(formatTakaSafe(totalExpense), style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = Color(0xFFB71C1C)))
                                    }
                                }

                                // Net
                                Card(
                                    modifier = Modifier.weight(1f),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                                    shape = RoundedCornerShape(10.dp),
                                    border = AppCardDefaults.border(MaterialTheme.colorScheme.primary)
                                ) {
                                    Column(modifier = Modifier.padding(8.dp)) {
                                        Text(if (isEnglish) "Net Reserve" else "নিট স্থিতি", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, color = MaterialTheme.colorScheme.primary))
                                        Text(formatTakaSafe(netBalance), style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                                    }
                                }

                                // Debts
                                Card(
                                    modifier = Modifier.weight(1f),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF59E0B).copy(alpha = 0.12f)),
                                    shape = RoundedCornerShape(10.dp),
                                    border = AppCardDefaults.border(Color(0xFFF59E0B))
                                ) {
                                    Column(modifier = Modifier.padding(8.dp)) {
                                        Text(if (isEnglish) "Debts Net" else "ধার-দেনা", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, color = Color(0xFFB45309)))
                                        Text(formatTakaSafe(pendingReceivable - pendingPayable), style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = Color(0xFF78350F)))
                                    }
                                }
                            }
                        }

                        // Visual Income vs Expense Ratio Bar
                        item {
                            val totalFlow = totalIncome + totalExpense
                            val incRatio = if (totalFlow > 0) (totalIncome / totalFlow).toFloat() else 0.5f
                            val incPct = (incRatio * 100).toInt()
                            val expPct = 100 - incPct

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surface,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text(
                                            text = if (isEnglish) "Cash Flow Ratio" else "আয় বনাম ব্যয় অনুপাত",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold)
                                        )
                                        Text(
                                            text = if (isEnglish) "Income $incPct% • Expense $expPct%" else "আয় $incPct% • ব্যয় $expPct%",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                        )
                                    }
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(10.dp)
                                            .clip(RoundedCornerShape(5.dp))
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .weight(max(0.01f, incRatio))
                                                .fillMaxHeight()
                                                .background(Color(0xFF16A34A))
                                        )
                                        Box(
                                            modifier = Modifier
                                                .weight(max(0.01f, 1f - incRatio))
                                                .fillMaxHeight()
                                                .background(Color(0xFFDC2626))
                                        )
                                    }
                                }
                            }
                        }

                        // =========================================================================
                        // TABLE 1: MASTER TRANSACTIONS TABLE (FULL ROWS AND COLUMNS!)
                        // =========================================================================
                        item {
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (isEnglish) "1. Master Transaction Ledger (${filteredList.size})" else "১. সর্বমোট আয়-ব্যয় খতিয়ান (${filteredList.size} টি)",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = if (isEnglish) "All Storage Records" else "সকল রেকর্ড প্রদর্শিত",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, color = MaterialTheme.colorScheme.outline)
                                )
                            }
                        }

                        // Table Column Headers
                        item {
                            Surface(
                                shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp),
                                color = Color(0xFF0F172A),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 8.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("#", style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFFF59E0B), fontWeight = FontWeight.Bold), modifier = Modifier.width(24.dp))
                                    Text(if (isEnglish) "Date" else "তারিখ", style = MaterialTheme.typography.labelSmall.copy(color = Color.White, fontWeight = FontWeight.Bold), modifier = Modifier.width(72.dp))
                                    Text(if (isEnglish) "Description / Title" else "বিবরণ / শিরোনাম", style = MaterialTheme.typography.labelSmall.copy(color = Color.White, fontWeight = FontWeight.Bold), modifier = Modifier.weight(1f))
                                    Text(if (isEnglish) "Category" else "খাত", style = MaterialTheme.typography.labelSmall.copy(color = Color.White, fontWeight = FontWeight.Bold), modifier = Modifier.width(80.dp))
                                    Text(if (isEnglish) "Amount" else "টাকা", style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFFF59E0B), fontWeight = FontWeight.Bold, textAlign = TextAlign.End), modifier = Modifier.width(75.dp))
                                }
                            }
                        }

                        if (filteredList.isEmpty()) {
                            item {
                                Surface(
                                    shape = RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = if (isEnglish) "No transactions recorded yet." else "কোনো লেনদেনের রেকর্ড পাওয়া যায়নি।",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.outline,
                                        modifier = Modifier.padding(16.dp),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        } else {
                            val itemsToShow = if (viewMode == "COMPACT") filteredList.take(10) else filteredList
                            val dateFormatSimple = SimpleDateFormat("dd/MM/yy", Locale.US)

                            itemsIndexed(itemsToShow) { idx, item ->
                                val isExp = item.type == "EXPENSE"
                                val isEven = idx % 2 == 0
                                val itemCat = com.example.ui.util.AppLocale.category(item.category, isEnglish)

                                Surface(
                                    color = if (isEven) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                    shape = if (idx == itemsToShow.lastIndex) RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp) else RoundedCornerShape(0.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 8.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "${idx + 1}",
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, color = MaterialTheme.colorScheme.outline),
                                            modifier = Modifier.width(24.dp)
                                        )
                                        Text(
                                            text = dateFormatSimple.format(Date(item.timestamp)),
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp),
                                            modifier = Modifier.width(72.dp)
                                        )
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = item.title,
                                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold, fontSize = 11.5.sp),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            if (item.note.isNotBlank()) {
                                                Text(
                                                    text = item.note,
                                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, color = MaterialTheme.colorScheme.outline),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
                                            modifier = Modifier.width(80.dp)
                                        ) {
                                            Text(
                                                text = itemCat,
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Medium),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                            )
                                        }
                                        Text(
                                            text = "${if (isExp) "(-)" else "(+)"}${formatTakaSafe(item.amount)}",
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp,
                                                color = if (isExp) Color(0xFFDC2626) else Color(0xFF16A34A),
                                                textAlign = TextAlign.End
                                            ),
                                            modifier = Modifier.width(75.dp)
                                        )
                                    }
                                }
                            }

                            if (viewMode == "COMPACT" && filteredList.size > 10) {
                                item {
                                    TextButton(
                                        onClick = { viewMode = "FULL_TABLE" },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = if (isEnglish) "+ View all ${filteredList.size} records directly on screen" else "+ সম্পূর্ণ ${filteredList.size} টি রেকর্ড সরাসরি স্ক্রিনে দেখুন",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                                        )
                                    }
                                }
                            }
                        }

                        // =========================================================================
                        // TABLE 2: DEBTS & RECEIVABLES TABLE
                        // =========================================================================
                        if (allDebts.isNotEmpty()) {
                            item {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = if (isEnglish) "2. Debts & Borrowings Ledger (${allDebts.size})" else "২. ধার-দেনা ও বাকি খাতা (${allDebts.size} টি)",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            item {
                                Surface(
                                    shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp),
                                    color = Color(0xFF1E293B),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 8.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(if (isEnglish) "Person" else "ব্যক্তির নাম", style = MaterialTheme.typography.labelSmall.copy(color = Color.White, fontWeight = FontWeight.Bold), modifier = Modifier.weight(1f))
                                        Text(if (isEnglish) "Type" else "ধরন", style = MaterialTheme.typography.labelSmall.copy(color = Color.White, fontWeight = FontWeight.Bold), modifier = Modifier.width(80.dp))
                                        Text(if (isEnglish) "Amount" else "পরিমাণ", style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFFF59E0B), fontWeight = FontWeight.Bold, textAlign = TextAlign.End), modifier = Modifier.width(70.dp))
                                        Text(if (isEnglish) "Status" else "স্ট্যাটাস", style = MaterialTheme.typography.labelSmall.copy(color = Color.White, fontWeight = FontWeight.Bold, textAlign = TextAlign.End), modifier = Modifier.width(65.dp))
                                    }
                                }
                            }
                            itemsIndexed(allDebts) { dIdx, debt ->
                                val isRec = debt.type == "RECEIVE"
                                Surface(
                                    color = if (dIdx % 2 == 0) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                    shape = if (dIdx == allDebts.lastIndex) RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp) else RoundedCornerShape(0.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 8.dp, vertical = 6.dp),
                                        verticalArrangement = Arrangement.spacedBy(3.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(debt.personName, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold))
                                                if (debt.khatName.isNotBlank()) {
                                                    Text(
                                                        text = "🏷️ ${com.example.ui.util.AppLocale.sourceKhatBadge(isEnglish, com.example.ui.util.AppLocale.khatName(debt.khatName, isEnglish))}",
                                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, color = MaterialTheme.colorScheme.primary),
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                } else if (debt.deductedFromMain) {
                                                    Text(
                                                        text = "💳 ${com.example.ui.util.AppLocale.sourceMainBadge(isEnglish)}",
                                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant),
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                            }
                                            Text(
                                                text = if (isRec) (if (isEnglish) "Get" else "পাওনা") else (if (isEnglish) "Give" else "দেনা"),
                                                style = MaterialTheme.typography.labelSmall.copy(color = if (isRec) Color(0xFF16A34A) else Color(0xFFDC2626), fontWeight = FontWeight.Bold),
                                                modifier = Modifier.width(80.dp)
                                            )
                                            Text(formatTakaSafe(debt.amount), style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, textAlign = TextAlign.End), modifier = Modifier.width(70.dp))
                                            Text(
                                                text = if (debt.isSettled) (if (isEnglish) "Settled" else "পরিশোধ") else (if (isEnglish) "Pending" else "বাকি"),
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    color = if (debt.isSettled) Color(0xFF15803D) else Color(0xFFB45309),
                                                    fontWeight = FontWeight.Bold,
                                                    textAlign = TextAlign.End
                                                ),
                                                modifier = Modifier.width(65.dp)
                                            )
                                        }

                                        // Loan Dates & Balances
                                        val lDate = try {
                                            SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(debt.timestamp))
                                        } catch (_: Exception) {
                                            "-"
                                        }
                                        val dDate = if (debt.dueDate.isNotBlank()) debt.dueDate else "-"
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = if (isEnglish) "Taken: $lDate • Due: $dDate" else "নেওয়া: $lDate • ফেরত: $dDate",
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            )
                                            Text(
                                                text = if (isEnglish) "Paid: ${formatTakaSafe(debt.effectivePaidAmount)} • Due: ${formatTakaSafe(debt.remainingAmount)}"
                                                else "পরিশোধ: ${formatTakaSafe(debt.effectivePaidAmount)} • বাকি: ${formatTakaSafe(debt.remainingAmount)}",
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold, color = if (debt.isSettled) Color(0xFF047857) else Color(0xFFB45309))
                                            )
                                        }

                                        // Installment History list if any
                                        val pList = debt.parsePaymentHistory()
                                        if (pList.isNotEmpty()) {
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                                                    .padding(6.dp),
                                                verticalArrangement = Arrangement.spacedBy(2.dp)
                                            ) {
                                                Text(
                                                    text = if (isEnglish) "Installment History (${pList.size}):" else "কিস্তি জমার বিবরণ (${pList.size} টি):",
                                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.5.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                                )
                                                pList.forEachIndexed { pIdx, pRec ->
                                                    val pD = try {
                                                        SimpleDateFormat("dd/MM/yy", Locale.getDefault()).format(Date(pRec.timestamp))
                                                    } catch (_: Exception) {
                                                        "-"
                                                    }
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween
                                                    ) {
                                                        Text(
                                                            text = "#${pIdx + 1} $pD ${if (pRec.note.isNotBlank()) "(${pRec.note})" else ""}",
                                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                        )
                                                        Text(
                                                            text = "+${formatTakaSafe(pRec.amount)} (${if (isEnglish) "Bal" else "বাকি"}: ${formatTakaSafe(pRec.remainingAfter)})",
                                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF047857))
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // =========================================================================
                        // TABLE 3: SAVINGS TARGETS
                        // =========================================================================
                        if (allSavingsGoals.isNotEmpty()) {
                            item {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = if (isEnglish) "3. Savings Portfolio (${allSavingsGoals.size})" else "৩. সঞ্চয় লক্ষ্যমাত্রা পোর্টফোলিও (${allSavingsGoals.size} টি)",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            itemsIndexed(allSavingsGoals) { sIdx, goal ->
                                val isGoalCompleted = goal.isCompleted || (goal.targetAmount > 0 && goal.savedAmount >= goal.targetAmount)
                                val displaySaved = if (isGoalCompleted) goal.targetAmount else goal.savedAmount
                                val displayProgress = if (isGoalCompleted) 100 else if (goal.targetAmount > 0) ((goal.savedAmount / goal.targetAmount) * 100).toInt() else 0
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                    border = androidx.compose.foundation.BorderStroke(
                                        0.8.dp,
                                        if (isGoalCompleted) Color(0xFF10B981).copy(alpha = 0.5f)
                                        else MaterialTheme.colorScheme.outlineVariant
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                                Text(goal.title, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                                                if (isGoalCompleted) {
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Surface(
                                                        shape = RoundedCornerShape(4.dp),
                                                        color = Color(0xFF10B981).copy(alpha = 0.15f)
                                                    ) {
                                                        Text(
                                                            text = if (isEnglish) "Completed" else "সম্পন্ন",
                                                            color = Color(0xFF047857),
                                                            fontSize = 9.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                        )
                                                    }
                                                }
                                            }
                                            Text(
                                                text = "${formatTakaSafe(displaySaved)} / ${formatTakaSafe(goal.targetAmount)} ($displayProgress%)",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isGoalCompleted) Color(0xFF10B981) else Color(0xFF0284C7)
                                                )
                                            )
                                        }
                                        LinearProgressIndicator(
                                            progress = { (displayProgress / 100f).coerceIn(0f, 1f) },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(6.dp)
                                                .clip(RoundedCornerShape(3.dp)),
                                            color = if (isGoalCompleted) Color(0xFF10B981) else Color(0xFF0284C7),
                                            trackColor = if (isGoalCompleted) Color(0xFF10B981).copy(alpha = 0.2f) else Color(0xFF0284C7).copy(alpha = 0.2f)
                                        )
                                    }
                                }
                            }
                        }

                        // =========================================================================
                        // TABLE 4: KHAT DISTRIBUTION & ALLOCATION (খাত বণ্টন ও স্থিতি)
                        // =========================================================================
                        if (allKhats.isNotEmpty()) {
                            item {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = if (isEnglish) "4. Khat Distribution & Expenses (${allKhats.size})" else "৪. খাত ভিত্তিক বণ্টন ও খরচের স্থিতি (${allKhats.size} টি খাত)",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            itemsIndexed(allKhats) { _, khat ->
                                val khatTxs = filteredList.filter { it.khatId == khat.id || it.khatName.equals(khat.name, ignoreCase = true) }
                                val khatSpent = khatTxs.filter { it.type == "EXPENSE" }.sumOf { it.amount }

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                    border = androidx.compose.foundation.BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(khat.name, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                                            Text(
                                                text = "${if (isEnglish) "Allocated: " else "বরাদ্দ: "}${formatTakaSafe(khat.allocatedAmount)}",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            )
                                        }
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "${if (isEnglish) "Spent: " else "ব্যয়িত: "}${formatTakaSafe(khatSpent)} (${khatTxs.size} ${if (isEnglish) "entries" else "টি লেনদেন"})",
                                                style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFDC2626))
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Official Security Verification Footer
                        item {
                            Spacer(modifier = Modifier.height(6.dp))
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = androidx.compose.foundation.BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Security,
                                        contentDescription = null,
                                        tint = Color(0xFF10B981),
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = if (isEnglish) "VERIFIED DIGITAL FINANCIAL STATEMENT" else "ডিজিটালভাবে পরীক্ষিত ও সত্যায়িত খতিয়ান",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                                        )
                                        Text(
                                            text = if (isEnglish) "Automated digital financial record • Verified cryptographic checksum" else "স্বয়ংক্রিয় ডিজিটাল আর্থিক হিসাব • ভেরিফাইড অডিট স্টেটমেন্ট",
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.5.sp, color = MaterialTheme.colorScheme.outline)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Download notification banner if saved
                if (downloadedFile != null) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF2E7D32).copy(alpha = 0.12f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2E7D32).copy(alpha = 0.4f))
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF2E7D32),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isEnglish) "✓ PDF saved to phone successfully!" else "✓ সম্পূর্ণ পিডিএফ রিসিট ফোনে সংরক্ষিত হয়েছে!",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF1B5E20)
                                    )
                                )
                            }
                            Text(
                                text = "📁 ${publicSavedPath ?: "Downloads"} • ${downloadedFile?.name}",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = Color(0xFF1B5E20),
                                    fontSize = 10.sp
                                )
                            )
                        }
                    }
                }

                // Action Buttons Row (Download / Open / Share / Close)
                if (downloadedFile != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(if (isEnglish) "Close" else "বন্ধ")
                        }

                        FilledTonalButton(
                            onClick = { sharePdfFile(downloadedFile!!) },
                            modifier = Modifier
                                .weight(1.2f)
                                .height(46.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (isEnglish) "Share" else "শেয়ার", fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { openPdfFile(downloadedFile!!) },
                            modifier = Modifier
                                .weight(1.4f)
                                .height(46.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A))
                        ) {
                            Icon(imageVector = Icons.Default.Visibility, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (isEnglish) "Open PDF" else "ওপেন করুন", fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                } else {
                    Button(
                        onClick = { downloadPdfReceipt() },
                        enabled = !isGeneratingPdf,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("btn_direct_download_receipt"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF0F172A)
                        )
                    ) {
                        if (isGeneratingPdf) {
                            CircularProgressIndicator(color = Color(0xFFF59E0B), modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isEnglish) "Generating Multi-Page PDF..." else "সম্পূর্ণ পিডিএফ তৈরি হচ্ছে...",
                                color = Color.White,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                        } else {
                            Icon(imageVector = Icons.Default.Download, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isEnglish) "Download Financial Statement PDF (${filteredList.size} Items)" else "আর্থিক বিবরণী পিডিএফ ডাউনলোড (${filteredList.size} টি রেকর্ড)",
                                color = Color.White,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }
            }
        }
    }
}

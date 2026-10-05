package com.example.ui.screens

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import com.example.data.model.DebtEntity
import com.example.data.model.formatTakaSafe
import com.example.ui.components.KeyboardScrollDownHint
import com.example.ui.util.AppLocale
import com.example.ui.util.clearFocusOnTap
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DebtReceiptDialog(
    debt: DebtEntity,
    isEnglish: Boolean,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val isPay = debt.type == "PAY"
    val accentColor = if (isPay) Color(0xFFDC2626) else Color(0xFF059669)
    val accentBg = if (isPay) Color(0xFFEF4444).copy(alpha = 0.12f) else Color(0xFF10B981).copy(alpha = 0.12f)

    val installments = remember(debt.paymentHistoryJson) { debt.parsePaymentHistory() }
    val receiptId = remember(debt.syncId, debt.id) {
        "REC-DEBT-${debt.id}-${debt.syncId.take(6).uppercase(Locale.getDefault())}"
    }

    val loanDateStr = remember(debt.timestamp) {
        try {
            val sdf = SimpleDateFormat("dd MMMM yyyy, hh:mm a", Locale.getDefault())
            sdf.format(Date(debt.timestamp))
        } catch (_: Exception) {
            "-"
        }
    }

    val generatedDateStr = remember {
        try {
            val sdf = SimpleDateFormat("dd MMMM yyyy, hh:mm a", Locale.getDefault())
            sdf.format(Date())
        } catch (_: Exception) {
            "-"
        }
    }

    val imeInsets = WindowInsets.ime
    val density = LocalDensity.current
    val isKeyboardOpen = imeInsets.getBottom(density) > 0

    val animatedCardElevation by animateDpAsState(
        targetValue = if (isKeyboardOpen) 14.dp else 8.dp,
        animationSpec = tween(durationMillis = 250),
        label = "debt_receipt_card_elevation"
    )
    val animatedVerticalPadding by animateDpAsState(
        targetValue = if (isKeyboardOpen) 6.dp else 16.dp,
        animationSpec = tween(durationMillis = 250),
        label = "debt_receipt_vertical_padding"
    )

    // Helper to generate text memo for sharing
    fun shareReceiptAsText() {
        try {
            val sb = StringBuilder()
            sb.appendLine("==========================================")
            sb.appendLine(if (isEnglish) "★ OFFICIAL DEBT & INSTALLMENT MEMO ★" else "★ অফিসিয়াল ধার-দেনা ও কিস্তি রসিদ ★")
            sb.appendLine(if (isEnglish) "Receipt ID: #$receiptId" else "রসিদ নং: #$receiptId")
            sb.appendLine(if (isEnglish) "Date: $generatedDateStr" else "তারিখ: $generatedDateStr")
            sb.appendLine("==========================================")
            sb.appendLine("${if (isEnglish) "Person Name" else "ব্যক্তির নাম"}: ${debt.personName}")
            sb.appendLine("${if (isEnglish) "Category" else "লেনদেনের ধরন"}: ${if (isPay) (if (isEnglish) "Payable (You owe)" else "দেনা (আমি দেব)") else (if (isEnglish) "Receivable (They owe)" else "পাওনা (আমি পাব)")}")
            sb.appendLine("${if (isEnglish) "Loan Taken Date" else "ঋণ গ্রহণের তারিখ"}: $loanDateStr")
            sb.appendLine("${if (isEnglish) "Expected Due Date" else "পরিশোধের সম্ভাব্য তারিখ"}: ${debt.dueDate.ifBlank { if (isEnglish) "Not Specified" else "নির্দিষ্ট করা নেই" }}")
            if (debt.khatName.isNotBlank()) {
                sb.appendLine("${if (isEnglish) "Source Khat" else "যুক্ত খাত"}: ${debt.khatName}")
            } else if (debt.deductedFromMain) {
                sb.appendLine("${if (isEnglish) "Source" else "উৎস"}: ${if (isEnglish) "Main Balance" else "মূল ক্যাশ ব্যালেন্স"}")
            }
            if (debt.note.isNotBlank()) {
                sb.appendLine("${if (isEnglish) "Note" else "মন্তব্য"}: ${debt.note}")
            }
            sb.appendLine("------------------------------------------")
            sb.appendLine("${if (isEnglish) "Total Debt Amount" else "মোট ধারের পরিমাণ"}: ${formatTakaSafe(debt.amount)}")
            sb.appendLine("${if (isEnglish) "Total Paid So Far" else "ইতোমধ্যে পরিশোধিত"}: ${formatTakaSafe(debt.effectivePaidAmount)}")
            sb.appendLine("${if (isEnglish) "Current Due Balance" else "বর্তমান অবশিষ্ট বাকি"}: ${formatTakaSafe(debt.remainingAmount)}")
            sb.appendLine("${if (isEnglish) "Status" else "বর্তমান স্থিতি"}: ${if (debt.isSettled) (if (isEnglish) "FULLY SETTLED ✓" else "সম্পূর্ণ পরিশোধিত ✓") else (if (isEnglish) "ACTIVE (DUE)" else "সক্রিয় বাকি")}")

            if (installments.isNotEmpty()) {
                sb.appendLine("------------------------------------------")
                sb.appendLine(if (isEnglish) "INSTALLMENT PAYMENT RECORDS (${installments.size}):" else "কিস্তি জমার বিস্তারিত খতিয়ান (${installments.size} টি):")
                installments.forEachIndexed { i, inst ->
                    val instDate = try {
                        val sdf = SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.getDefault())
                        sdf.format(Date(inst.timestamp))
                    } catch (_: Exception) {
                        "-"
                    }
                    val noteStr = if (inst.note.isNotBlank()) " [${inst.note}]" else ""
                    sb.appendLine("${i + 1}. $instDate | +${formatTakaSafe(inst.amount)} | ${if (isEnglish) "Due" else "বাকি"}: ${formatTakaSafe(inst.remainingAfter)}$noteStr")
                }
            }
            sb.appendLine("==========================================")
            sb.appendLine(if (isEnglish) "Generated via Daily Expense Tracker" else "দৈনিক খরচের হিসাব অ্যাপ থেকে প্রস্তুতকৃত")

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, if (isEnglish) "Debt Voucher: ${debt.personName}" else "ধার রসিদ: ${debt.personName}")
                putExtra(Intent.EXTRA_TEXT, sb.toString())
            }
            context.startActivity(Intent.createChooser(intent, if (isEnglish) "Share Receipt via" else "রসিদ শেয়ার করুন"))
        } catch (e: Exception) {
            Toast.makeText(context, e.localizedMessage ?: "Failed to share", Toast.LENGTH_SHORT).show()
        }
    }

    // Helper to generate and save PDF receipt
    fun downloadDebtReceiptPdf() {
        try {
            val pdfDoc = PdfDocument()
            val pageWidth = 595
            val pageHeight = 842
            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
            val page = pdfDoc.startPage(pageInfo)
            val canvas = page.canvas
            val paint = Paint().apply { isAntiAlias = true }

            val leftMargin = 36f
            val rightMargin = pageWidth - 36f
            var currentY = 40f

            // 1. Header Banner
            paint.color = if (isPay) AndroidColor.parseColor("#B91C1C") else AndroidColor.parseColor("#047857")
            canvas.drawRoundRect(leftMargin, currentY, rightMargin, currentY + 68f, 10f, 10f, paint)

            paint.color = AndroidColor.WHITE
            paint.textSize = 15f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            val headerTitle = if (isEnglish) "DEBT & INSTALLMENT OFFICIAL VOUCHER" else "ডিজিটাল ধার-দেনা ও কিস্তি রসিদ"
            canvas.drawText(headerTitle, leftMargin + 16f, currentY + 28f, paint)

            paint.textSize = 9.5f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            val subHeader = if (isEnglish) "Voucher ID: #$receiptId • Generated: $generatedDateStr" else "রসিদ আইডি: #$receiptId • প্রস্তুত: $generatedDateStr"
            canvas.drawText(subHeader, leftMargin + 16f, currentY + 48f, paint)

            currentY += 82f

            // 2. Person & Direction Card
            paint.color = AndroidColor.parseColor("#F8FAFC")
            canvas.drawRoundRect(leftMargin, currentY, rightMargin, currentY + 90f, 8f, 8f, paint)
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 1f
            paint.color = AndroidColor.parseColor("#E2E8F0")
            canvas.drawRoundRect(leftMargin, currentY, rightMargin, currentY + 90f, 8f, 8f, paint)
            paint.style = Paint.Style.FILL

            paint.color = AndroidColor.parseColor("#0F172A")
            paint.textSize = 13f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText(debt.personName, leftMargin + 14f, currentY + 24f, paint)

            paint.textSize = 9f
            paint.color = if (isPay) AndroidColor.parseColor("#DC2626") else AndroidColor.parseColor("#059669")
            val directionText = if (isPay) (if (isEnglish) "PAYABLE (You have borrowed / Need to return)" else "দেনা (আমি নিয়েছি / ফেরত দিতে হবে)")
            else (if (isEnglish) "RECEIVABLE (You have lent / Others owe you)" else "পাওনা (আমি দিয়েছি / অন্যরা ফেরত দেবে)")
            canvas.drawText(directionText, leftMargin + 14f, currentY + 40f, paint)

            paint.color = AndroidColor.parseColor("#475569")
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            val loanDateLine = if (isEnglish) "Loan Taken Date: $loanDateStr" else "ধার নেওয়ার তারিখ: $loanDateStr"
            canvas.drawText(loanDateLine, leftMargin + 14f, currentY + 58f, paint)

            val dueLine = if (isEnglish) "Expected Due Date: ${debt.dueDate.ifBlank { "Not Specified" }}" else "পরিশোধের সম্ভাব্য তারিখ: ${debt.dueDate.ifBlank { "নির্দিষ্ট করা নেই" }}"
            canvas.drawText(dueLine, leftMargin + 14f, currentY + 74f, paint)

            val sourceTag = when {
                debt.khatName.isNotBlank() -> if (isEnglish) "Khat: ${debt.khatName}" else "খাত: ${debt.khatName}"
                debt.deductedFromMain -> if (isEnglish) "Source: Main Balance" else "উৎস: মূল ক্যাশ ব্যালেন্স"
                else -> ""
            }
            if (sourceTag.isNotBlank()) {
                canvas.drawText(sourceTag, leftMargin + 320f, currentY + 58f, paint)
            }

            val statusText = if (debt.isSettled) (if (isEnglish) "Status: FULLY SETTLED ✓" else "স্থিতি: সম্পূর্ণ পরিশোধিত ✓")
            else (if (isEnglish) "Status: ACTIVE (DUE)" else "স্থিতি: সক্রিয় বাকি")
            paint.color = if (debt.isSettled) AndroidColor.parseColor("#047857") else AndroidColor.parseColor("#B45309")
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText(statusText, leftMargin + 320f, currentY + 74f, paint)

            currentY += 104f

            // 3. Financial Summary Box (3 Columns)
            val boxWidth = (rightMargin - leftMargin - 16f) / 3f

            // Box 1: Total Amount
            paint.color = AndroidColor.parseColor("#F1F5F9")
            canvas.drawRoundRect(leftMargin, currentY, leftMargin + boxWidth, currentY + 46f, 6f, 6f, paint)
            paint.color = AndroidColor.parseColor("#64748B")
            paint.textSize = 8.5f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            canvas.drawText(if (isEnglish) "Total Debt" else "মোট ধারের পরিমাণ", leftMargin + 10f, currentY + 16f, paint)
            paint.color = AndroidColor.parseColor("#0F172A")
            paint.textSize = 12f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText(formatTakaSafe(debt.amount), leftMargin + 10f, currentY + 34f, paint)

            // Box 2: Paid Amount
            val b2X = leftMargin + boxWidth + 8f
            paint.color = AndroidColor.parseColor("#ECFDF5")
            canvas.drawRoundRect(b2X, currentY, b2X + boxWidth, currentY + 46f, 6f, 6f, paint)
            paint.color = AndroidColor.parseColor("#047857")
            paint.textSize = 8.5f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            canvas.drawText(if (isEnglish) "Paid So Far" else "পরিশোধ হয়েছে", b2X + 10f, currentY + 16f, paint)
            paint.textSize = 12f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText(formatTakaSafe(debt.effectivePaidAmount), b2X + 10f, currentY + 34f, paint)

            // Box 3: Remaining Due
            val b3X = b2X + boxWidth + 8f
            paint.color = if (isPay) AndroidColor.parseColor("#FEF2F2") else AndroidColor.parseColor("#EFF6FF")
            canvas.drawRoundRect(b3X, currentY, rightMargin, currentY + 46f, 6f, 6f, paint)
            paint.color = if (isPay) AndroidColor.parseColor("#DC2626") else AndroidColor.parseColor("#1D4ED8")
            paint.textSize = 8.5f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            canvas.drawText(if (isEnglish) "Current Due" else "অবশিষ্ট বাকি", b3X + 10f, currentY + 16f, paint)
            paint.textSize = 12f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText(formatTakaSafe(debt.remainingAmount), b3X + 10f, currentY + 34f, paint)

            currentY += 58f

            // 4. Installments Table Header
            paint.color = AndroidColor.parseColor("#1E293B")
            canvas.drawRoundRect(leftMargin, currentY, rightMargin, currentY + 22f, 4f, 4f, paint)
            paint.color = AndroidColor.WHITE
            paint.textSize = 9.5f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            val instHeader = if (isEnglish) "INSTALLMENT & PAYMENT BREAKDOWN (${installments.size} RECORDS)" else "কিস্তি জমার বিস্তারিত খতিয়ান (${installments.size} টি রেকর্ড)"
            canvas.drawText(instHeader, leftMargin + 10f, currentY + 15f, paint)

            currentY += 24f

            // Table Columns Header
            paint.color = AndroidColor.parseColor("#E2E8F0")
            canvas.drawRect(leftMargin, currentY, rightMargin, currentY + 18f, paint)
            paint.color = AndroidColor.parseColor("#334155")
            paint.textSize = 8f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("#", leftMargin + 6f, currentY + 12f, paint)
            canvas.drawText(if (isEnglish) "Date & Time" else "তারিখ ও সময়", leftMargin + 26f, currentY + 12f, paint)
            canvas.drawText(if (isEnglish) "Paid Amount" else "পরিশোধিত টাকা", leftMargin + 170f, currentY + 12f, paint)
            canvas.drawText(if (isEnglish) "Remaining Balance" else "অবশিষ্ট বাকি", leftMargin + 280f, currentY + 12f, paint)
            canvas.drawText(if (isEnglish) "Note / Remark" else "মন্তব্য / বিবরণ", leftMargin + 400f, currentY + 12f, paint)

            currentY += 18f

            if (installments.isEmpty()) {
                paint.color = AndroidColor.parseColor("#F8FAFC")
                canvas.drawRect(leftMargin, currentY, rightMargin, currentY + 30f, paint)
                paint.color = AndroidColor.parseColor("#64748B")
                paint.textSize = 9f
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                canvas.drawText(if (isEnglish) "No installments paid yet. Debt is currently full." else "এখনও কোনো কিস্তি পরিশোধ করা হয়নি। ধারের সম্পূর্ণ টাকা বাকি রয়েছে।", leftMargin + 12f, currentY + 18f, paint)
                currentY += 34f
            } else {
                installments.forEachIndexed { idx, item ->
                    if (idx % 2 == 1) {
                        paint.color = AndroidColor.parseColor("#F8FAFC")
                        canvas.drawRect(leftMargin, currentY, rightMargin, currentY + 18f, paint)
                    }

                    val dateStr = try {
                        val sdf = SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.getDefault())
                        sdf.format(Date(item.timestamp))
                    } catch (_: Exception) {
                        "-"
                    }

                    paint.color = AndroidColor.parseColor("#64748B")
                    paint.textSize = 8f
                    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                    canvas.drawText("${idx + 1}", leftMargin + 6f, currentY + 12f, paint)

                    paint.color = AndroidColor.parseColor("#0F172A")
                    canvas.drawText(dateStr, leftMargin + 26f, currentY + 12f, paint)

                    paint.color = AndroidColor.parseColor("#047857")
                    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    canvas.drawText("+${formatTakaSafe(item.amount)}", leftMargin + 170f, currentY + 12f, paint)

                    paint.color = if (isPay) AndroidColor.parseColor("#DC2626") else AndroidColor.parseColor("#1D4ED8")
                    canvas.drawText(formatTakaSafe(item.remainingAfter), leftMargin + 280f, currentY + 12f, paint)

                    paint.color = AndroidColor.parseColor("#475569")
                    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                    canvas.drawText(item.note.ifBlank { if (isEnglish) "Installment" else "কিস্তি জমা" }.take(20), leftMargin + 400f, currentY + 12f, paint)

                    currentY += 18f
                }
            }

            // 5. Footer Signatures & Disclaimer
            currentY += 30f
            paint.color = AndroidColor.parseColor("#94A3B8")
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 0.8f
            canvas.drawLine(leftMargin + 10f, currentY + 40f, leftMargin + 160f, currentY + 40f, paint)
            canvas.drawLine(rightMargin - 160f, currentY + 40f, rightMargin - 10f, currentY + 40f, paint)
            paint.style = Paint.Style.FILL

            paint.color = AndroidColor.parseColor("#475569")
            paint.textSize = 8f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText(if (isEnglish) "Borrower / Lender Signature" else "গ্রহীতা / দাতার স্বাক্ষর", leftMargin + 20f, currentY + 54f, paint)
            canvas.drawText(if (isEnglish) "Authorized Signatory" else "অনুমোদিত কর্মকর্তা / সংরক্ষণকারী", rightMargin - 150f, currentY + 54f, paint)

            paint.color = AndroidColor.parseColor("#94A3B8")
            paint.textSize = 7.5f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
            canvas.drawText(if (isEnglish) "This is a computer generated digital ledger voucher from Daily Expense Tracker." else "এটি দৈনিক খরচের হিসাব অ্যাপ থেকে স্বয়ংক্রিয়ভাবে প্রস্তুতকৃত ডিজিটাল খতিয়ান ও রসিদ ভাউচার।", leftMargin + 10f, pageHeight - 30f, paint)

            pdfDoc.finishPage(page)

            // Save PDF File
            val fileName = "DebtVoucher_${debt.personName.replace("\\s+".toRegex(), "_")}_${debt.id}.pdf"
            val publicDownloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (!publicDownloadsDir.exists()) {
                publicDownloadsDir.mkdirs()
            }
            val targetFile = File(publicDownloadsDir, fileName)
            FileOutputStream(targetFile).use { out ->
                pdfDoc.writeTo(out)
                out.flush()
            }

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
                            pdfDoc.writeTo(out)
                            out.flush()
                        }
                    }
                } catch (_: Exception) {}
            }

            pdfDoc.close()

            MediaScannerConnection.scanFile(
                context,
                arrayOf(targetFile.absolutePath),
                arrayOf("application/pdf"),
                null
            )

            // Notification
            try {
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                val channelId = "debt_receipt_downloads"
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val channel = NotificationChannel(
                        channelId,
                        if (isEnglish) "Debt Vouchers" else "ধার-দেনা রসিদ ডাউনলোড",
                        NotificationManager.IMPORTANCE_HIGH
                    ).apply {
                        enableVibration(true)
                    }
                    notificationManager.createNotificationChannel(channel)
                }
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", targetFile)
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
                val notif = androidx.core.app.NotificationCompat.Builder(context, channelId)
                    .setSmallIcon(android.R.drawable.stat_sys_download_done)
                    .setContentTitle(if (isEnglish) "✓ Debt Voucher Downloaded" else "✓ ধার-দেনার রসিদ ডাউনলোড সম্পন্ন")
                    .setContentText(if (isEnglish) "${targetFile.name} • Tap to view" else "${targetFile.name} • দেখতে এখানে ট্যাপ করুন")
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true)
                    .build()
                notificationManager.notify(System.currentTimeMillis().toInt(), notif)
            } catch (_: Exception) {}

            Toast.makeText(
                context,
                if (isEnglish) "✓ PDF Saved to Downloads folder (${targetFile.name})" else "✓ রসিদ PDF সফলভাবে Downloads ফোল্ডারে সংরক্ষিত হয়েছে",
                Toast.LENGTH_LONG
            ).show()
        } catch (e: Exception) {
            Toast.makeText(context, e.localizedMessage ?: "Failed to generate PDF", Toast.LENGTH_SHORT).show()
        }
    }

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
                .padding(horizontal = 16.dp, vertical = animatedVerticalPadding)
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
                    .widthIn(max = 500.dp)
                    .testTag("debt_receipt_dialog")
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = if (isKeyboardOpen) 440.dp else 680.dp)
                            .padding(20.dp)
                            .verticalScroll(scrollState),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
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
                                            imageVector = Icons.Default.ReceiptLong,
                                            contentDescription = null,
                                            tint = accentColor,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }
                                Column {
                                    Text(
                                        text = if (isEnglish) "Debt Memo & Receipt" else "ধার-দেনা রসিদ ও চুক্তিপত্র",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "#$receiptId",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = accentColor
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

                        // Person Details & Direction Card
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = accentBg,
                            border = androidx.compose.foundation.BorderStroke(1.dp, accentColor.copy(alpha = 0.25f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = debt.personName,
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = accentColor.copy(alpha = 0.18f)
                                    ) {
                                        Text(
                                            text = if (isPay) (if (isEnglish) "PAYABLE (YOU OWE)" else "দেনা (আমি দেব)")
                                            else (if (isEnglish) "RECEIVABLE (GET)" else "পাওনা (আমি পাব)"),
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = accentColor
                                            ),
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.CalendarToday,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (isEnglish) "Loan Date: $loanDateStr" else "ঋণ নেওয়ার তারিখ: $loanDateStr",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Event,
                                        contentDescription = null,
                                        tint = accentColor,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (isEnglish) "Due Date: ${debt.dueDate.ifBlank { "Not Specified" }}"
                                        else "পরিশোধের সম্ভাব্য তারিখ: ${debt.dueDate.ifBlank { "নির্দিষ্ট করা নেই" }}",
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                if (debt.khatName.isNotBlank() || debt.deductedFromMain) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Folder,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        val src = if (debt.khatName.isNotBlank()) debt.khatName else (if (isEnglish) "Main Balance" else "মূল ক্যাশ ব্যালেন্স")
                                        Text(
                                            text = if (isEnglish) "Linked Fund: $src" else "যুক্ত তহবিল / খাত: $src",
                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }

                                if (debt.note.isNotBlank()) {
                                    Row(verticalAlignment = Alignment.Top) {
                                        Icon(
                                            imageVector = Icons.Default.Notes,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = debt.note,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }

                        // Financial Status Card (3 distinct clean rows)
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
                                // Row 1: Total Debt
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (isEnglish) "Total Debt Amount" else "মোট ধারের পরিমাণ",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = formatTakaSafe(debt.amount),
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                // Row 2: Paid So Far
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (isEnglish) "Paid So Far" else "ইতোমধ্যে পরিশোধিত",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = formatTakaSafe(debt.effectivePaidAmount),
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF047857)
                                        )
                                    )
                                }

                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))

                                // Row 3: Current Remaining Due (Highlighted)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (isEnglish) "Current Due Balance" else "বর্তমান অবশিষ্ট বাকি",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = accentColor.copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            text = formatTakaSafe(debt.remainingAmount),
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.ExtraBold,
                                                color = accentColor
                                            ),
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                                        )
                                    }
                                }

                                // Status Badge
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (isEnglish) "Status" else "বর্তমান স্থিতি",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = if (debt.isSettled) Color(0xFF10B981).copy(alpha = 0.18f) else Color(0xFFF59E0B).copy(alpha = 0.18f)
                                    ) {
                                        Text(
                                            text = if (debt.isSettled) (if (isEnglish) "FULLY SETTLED ✓" else "সম্পূর্ণ পরিশোধিত ✓")
                                            else (if (isEnglish) "ACTIVE (DUE)" else "সক্রিয় বাকি"),
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = if (debt.isSettled) Color(0xFF047857) else Color(0xFFB45309)
                                            ),
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Section 4: Installment History List
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isEnglish) "Installment History (${installments.size})" else "কিস্তি জমার বিস্তারিত ইতিহাস (${installments.size} টি)",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        if (installments.isEmpty()) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                border = androidx.compose.foundation.BorderStroke(0.6.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (isEnglish) "No installments paid yet. Complete debt is pending." else "এখনও কোনো কিস্তি পরিশোধ করা হয়নি। ধারের সম্পূর্ণ টাকা বাকি রয়েছে।",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        } else {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                border = androidx.compose.foundation.BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    installments.forEachIndexed { idx, pRec ->
                                        val dateStr = try {
                                            val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
                                            sdf.format(Date(pRec.timestamp))
                                        } catch (_: Exception) {
                                            "-"
                                        }

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                                Surface(
                                                    shape = CircleShape,
                                                    color = Color(0xFF10B981).copy(alpha = 0.15f),
                                                    modifier = Modifier.size(24.dp)
                                                ) {
                                                    Box(contentAlignment = Alignment.Center) {
                                                        Text(
                                                            text = "${idx + 1}",
                                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = Color(0xFF047857))
                                                        )
                                                    }
                                                }
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Column {
                                                    Text(
                                                        text = pRec.note.ifBlank { if (isEnglish) "Installment Payment" else "কিস্তি জমা" },
                                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
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
                                                    style = MaterialTheme.typography.bodyMedium.copy(
                                                        fontWeight = FontWeight.ExtraBold,
                                                        color = Color(0xFF047857)
                                                    )
                                                )
                                                Text(
                                                    text = "${if (isEnglish) "Due: " else "বাকি: "}${formatTakaSafe(pRec.remainingAfter)}",
                                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp, fontWeight = FontWeight.SemiBold),
                                                    color = accentColor
                                                )
                                            }
                                        }

                                        if (idx < installments.lastIndex) {
                                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // Action Buttons: Share, Download PDF, Close
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { downloadDebtReceiptPdf() },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("btn_download_debt_pdf")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Download,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (isEnglish) "PDF Receipt" else "PDF রসিদ",
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                                    )
                                }

                                OutlinedButton(
                                    onClick = { shareReceiptAsText() },
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("btn_share_debt_receipt")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Share,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (isEnglish) "Share Memo" else "মেমো শেয়ার",
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                                    )
                                }
                            }

                            TextButton(
                                onClick = onDismiss,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(42.dp)
                            ) {
                                Text(
                                    text = AppLocale.cancel(isEnglish),
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
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

package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.components.AppCardDefaults
import com.example.ui.components.AppToastManager
import com.example.ui.components.KeyboardScrollDownHint

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutAppDialog(
    initialIsEnglish: Boolean = false,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var isEnglish by remember { mutableStateOf(initialIsEnglish) }
    val scrollState = rememberScrollState()

    val developerName = "Md.Yousuf"
    val developerDegree = "B.Sc in CSE from North South University"
    val developerEmail = "md.yousuf01@northsouth.edu"

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.92f)
                .clip(RoundedCornerShape(24.dp))
                .testTag("about_app_dialog"),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            border = AppCardDefaults.dialogBorder(),
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 18.dp, vertical = 16.dp)
            ) {
                // Top Action Bar with Language Toggle and Close Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = if (isEnglish) "About Daily Hishab" else "অ্যাপ পরিচিতি ও নির্দেশিকা",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (isEnglish) "User Guide & Developer Profile" else "ব্যবহার বিধি ও ডেভেলপার পরিচিতি",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Language Segmented Pill
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        ) {
                            Row(modifier = Modifier.padding(3.dp)) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (!isEnglish) MaterialTheme.colorScheme.primary else Color.Transparent,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable { isEnglish = false }
                                ) {
                                    Text(
                                        text = "বাংলা",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = if (!isEnglish) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isEnglish) MaterialTheme.colorScheme.primary else Color.Transparent,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable { isEnglish = true }
                                ) {
                                    Text(
                                        text = "EN",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = if (isEnglish) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 12.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                )

                // Scrollable Content wrapped in Box with KeyboardScrollDownHint
                Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(scrollState),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // =========================================================================
                        // 1. DEVELOPER HERO PROFILE CARD (MANDATORY REQUIREMENT)
                        // =========================================================================
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("card_developer_profile"),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                            ),
                            border = AppCardDefaults.border(Color(0xFF6366F1))
                        ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                // Developer Avatar / Crest
                                Box(
                                    modifier = Modifier
                                        .size(54.dp)
                                        .clip(CircleShape)
                                        .background(
                                            Brush.linearGradient(
                                                listOf(Color(0xFF4F46E5), Color(0xFF7C3AED))
                                            )
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.School,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(14.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            text = if (isEnglish) "CREATOR & ARCHITECT" else "সৃষ্টিকর্তা ও সফটওয়্যার প্রকৌশলী",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 9.5.sp,
                                                color = MaterialTheme.colorScheme.primary
                                            ),
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Text(
                                        text = "Built by $developerName",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 17.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    )
                                    Text(
                                        text = developerDegree,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    )
                                }
                            }

                            // Email Pill & Action Buttons
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Email,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = developerEmail,
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        )
                                    }

                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        // Copy Email Button
                                        OutlinedButton(
                                            onClick = {
                                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                val clip = ClipData.newPlainText("Developer Email", developerEmail)
                                                clipboard.setPrimaryClip(clip)
                                                AppToastManager.show(
                                                    if (isEnglish) "Email copied to clipboard!" else "ইমেইল অ্যাড্রেস কপি করা হয়েছে!"
                                                )
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                            modifier = Modifier.height(28.dp)
                                        ) {
                                            Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(12.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(if (isEnglish) "Copy" else "কপি", fontSize = 11.sp)
                                        }

                                        // Send Email Button
                                        Button(
                                            onClick = {
                                                try {
                                                    val intent = Intent(Intent.ACTION_SENDTO).apply {
                                                        data = Uri.parse("mailto:$developerEmail")
                                                        putExtra(Intent.EXTRA_SUBJECT, "Daily Hishab App Feedback & Query")
                                                    }
                                                    context.startActivity(Intent.createChooser(intent, "Send Email"))
                                                } catch (_: Exception) {
                                                    AppToastManager.show(developerEmail)
                                                }
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                            modifier = Modifier.height(28.dp)
                                        ) {
                                            Icon(imageVector = Icons.Default.Send, contentDescription = null, modifier = Modifier.size(12.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(if (isEnglish) "Mail" else "মেইল", fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // =========================================================================
                    // 2. APP OVERVIEW & HIGHLIGHTS
                    // =========================================================================
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                        ),
                        border = AppCardDefaults.border(Color(0xFFF59E0B)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Lightbulb,
                                    contentDescription = null,
                                    tint = Color(0xFFF59E0B),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isEnglish) "What is Daily Hishab?" else "Daily Hishab কী এবং কেন ব্যবহার করবেন?",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Text(
                                text = if (isEnglish) {
                                    "Daily Hishab is an ultra-secure, bank-grade personal finance and bookkeeping companion built for individuals, students, and businesses. It operates completely offline first, keeping all your data instantly responsive, while seamlessly syncing with Firebase Cloud when connected."
                                } else {
                                    "Daily Hishab হলো ব্যক্তিগত, শিক্ষার্থী ও ব্যবসা প্রতিষ্ঠানের জন্য একটি পূর্ণাঙ্গ, সহজ এবং ব্যাংক-গ্রেড এনক্রিপ্টযুক্ত ডিজিটাল হিসাব খাতা। ইন্টারনেট ছাড়াও অ্যাপটি ১০০% অফলাইনে কাজ করে এবং ইন্টারনেট পেলেই স্বয়ংক্রিয়ভাবে ক্লাউডে নিরাপদ ব্যাকআপ নিয়ে নেয়।"
                                },
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    lineHeight = 19.sp
                                )
                            )
                        }
                    }

                    // Section Heading: HOW TO USE THE APP (Functions & Step-by-Step Guide)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MenuBook,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isEnglish) "HOW TO USE EVERY FEATURE" else "অ্যাপের সকল ফিচার ও ব্যবহার নির্দেশিকা",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        )
                    }

                    // =========================================================================
                    // 3. STEP-BY-STEP FUNCTION GUIDES
                    // =========================================================================

                    // Function 1: Daily Transactions (Expenses & Incomes)
                    FeatureGuideCard(
                        number = "১",
                        titleBn = "দৈনিক আয় ও ব্যয় হিসাব রাখা (Expenses & Incomes)",
                        titleEn = "Daily Income & Expense Tracking",
                        icon = Icons.Default.ReceiptLong,
                        accentColor = Color(0xFF10B981),
                        steps = if (isEnglish) listOf(
                            "Tap the floating (+) button at bottom or use the Quick Add bar on the daily screen.",
                            "Select whether it is an Expense (ব্যয়) or Income (আয়).",
                            "Enter the amount in Taka, select a Category (e.g. Food, Groceries, Rent, Salary), and pick a Payment Method (Cash, bKash, Nagad, Bank, Card).",
                            "Optionally select a specific 'Khat' (Envelope) to deduct funds from, or add a short note.",
                            "Tap Save. An instant Undo button appears on screen in case you made a mistake!"
                        ) else listOf(
                            "হোম স্ক্রিনের নিচে গোল ভাসমান (+) বোতাম চাপুন অথবা কুইক অ্যাড বার ব্যবহার করুন।",
                            "লেনদেনটি 'খরচ' (Expense) নাকি 'আয়' (Income) তা নির্বাচন করুন।",
                            "টাকার পরিমাণ লিখুন, ক্যাটাগরি (খাবার, বাজার, ভাড়া, বেতন ইত্যাদি) ও পেমেন্ট মাধ্যম (ক্যাশ, বিকাশ, নগদ, ব্যাংক) সিলেক্ট করুন।",
                            "নির্দিষ্ট কোনো 'খাত' থেকে খরচ হলে সেই খাতটি বেছে নিতে পারেন। প্রয়োজনে ছোট নোট যোগ করুন।",
                            "'সংরক্ষণ করুন' চাপুন। ভুল হলে সাথে সাথে আসা টোস্টের 'Undo/বাতিল' চেপে ফেরত নিতে পারবেন।"
                        )
                    )

                    // Function 2: Envelope Budgeting / Khats
                    FeatureGuideCard(
                        number = "২",
                        titleBn = "বাজেট ও খাত ব্যবস্থাপনা (Khat / Envelope Budgeting)",
                        titleEn = "Khat & Envelope Budgeting System",
                        icon = Icons.Default.AccountBalanceWallet,
                        accentColor = Color(0xFF6366F1),
                        steps = if (isEnglish) listOf(
                            "The Khat system lets you allocate your total balance into separate envelopes (e.g., Groceries, Rent, Bills, Education).",
                            "Scroll to the 'Khat Allocation' section on the daily screen and tap '+ New Khat'.",
                            "Name your Khat and assign an allocated budget from your available unallocated cash (অবশিষ্ট ব্যালেন্স).",
                            "When adding an expense, select that Khat — the money will be cleanly deducted from that envelope with live balance tracking."
                        ) else listOf(
                            "খাত বা Envelope পদ্ধতি আপনাকে আপনার মোট টাকা বিভিন্ন খাতে (যেমন: বাজার, বাড়ি ভাড়া, বিদ্যুৎ বিল, পড়াশোনা) ভাগ করে রাখতে সাহায্য করে।",
                            "হোম স্ক্রিনের 'খাত অনুযায়ী বরাদ্দ' সেকশনে গিয়ে '+ নতুন খাত' বোতাম চাপুন।",
                            "খাতের নাম দিন এবং অবশিষ্ট ব্যালেন্স থেকে নির্দিষ্ট পরিমাণ টাকা এই খাতে বরাদ্দ দিন।",
                            "খরচ করার সময় উক্ত খাত সিলেক্ট করলে সেই খাতের বরাদ্দকৃত টাকা থেকে স্বয়ংক্রিয়ভাবে খরচ কাটা হবে।"
                        )
                    )

                    // Function 3: Daily Target & Streak
                    FeatureGuideCard(
                        number = "৩",
                        titleBn = "দৈনিক খরচের লক্ষ্য ও স্ট্রিক (Daily Target & Streak)",
                        titleEn = "Daily Spending Limit & Budget Streak",
                        icon = Icons.Default.TrackChanges,
                        accentColor = Color(0xFFF59E0B),
                        steps = if (isEnglish) listOf(
                            "Open Settings Drawer -> tap 'Daily Spending Target' to set your maximum planned spend per day (e.g. ৳500).",
                            "The top card on the daily screen displays your today's spending progress against this target.",
                            "If you stay under your target, your consecutive 'Budget Streak' builds up day by day!"
                        ) else listOf(
                            "সেটিংস মেনু খুলে 'দৈনিক খরচের লক্ষ্যমাত্রা' নির্বাচন করুন এবং আপনার প্রতিদিনের খরচের সীমা (যেমন: ৫০০ টাকা) নির্ধারণ করুন।",
                            "হোম স্ক্রিনের ওপরের কার্ডে আজকের মোট খরচ ও এই সীমার সাথে তুলনামূলক অগ্রগতি দেখতে পাবেন।",
                            "প্রতিদিন সীমার মধ্যে খরচ রাখলে আপনার ধারাবাহিক 'বাজেট স্ট্রিক' দিন দিন বৃদ্ধি পাবে।"
                        )
                    )

                    // Function 4: Debts & Loans (দেনা-পাওনা)
                    FeatureGuideCard(
                        number = "৪",
                        titleBn = "ধার-দেনা ও দেনা-পাওনা হিসাব (Debts & Loans)",
                        titleEn = "Debts & Loans Tracker",
                        icon = Icons.Default.Handshake,
                        accentColor = Color(0xFFEC4899),
                        steps = if (isEnglish) listOf(
                            "Tap the 'Debts' tab (ধার-দেনা) from the bottom navigation bar.",
                            "Track both 'I Owe' (দেনা - money you must pay back) and 'Owed to Me' (পাওনা - money someone owes you).",
                            "Enter the person's name, phone number, amount, and due return date.",
                            "You can record partial payments, call/SMS the person directly, or mark the record as fully settled with one tap."
                        ) else listOf(
                            "নিচের নেভিগেশন বারের 'ধার-দেনা' ট্যাবে প্রবেশ করুন।",
                            "এখানে 'আমি পাব' (পাওনা) এবং 'আমার কাছে পাবে' (দেনা) উভয় হিসাব আলাদাভাবে সংরক্ষণ করুন।",
                            "ব্যক্তির নাম, ফোন নম্বর, টাকার পরিমাণ ও ফেরত দেওয়ার তারিখ উল্লেখ করুন।",
                            "আংশিক কিস্তি পরিশোধ যোগ করতে পারবেন অথবা এক ক্লিকেই সম্পূর্ণ পরিশোধ বা কল/এসএমএস করতে পারবেন।"
                        )
                    )

                    // Function 5: Savings Goals (সঞ্চয় লক্ষ্যমাত্রা)
                    FeatureGuideCard(
                        number = "৫",
                        titleBn = "ভবিষ্যৎ সঞ্চয় লক্ষ্যমাত্রা (Savings Goals)",
                        titleEn = "Future Savings Goals & Targets",
                        icon = Icons.Default.Savings,
                        accentColor = Color(0xFF06B6D4),
                        steps = if (isEnglish) listOf(
                            "Tap the 'Savings' tab (সঞ্চয়) on the bottom navigation bar.",
                            "Create dreams and targets like 'Emergency Fund', 'New Laptop', 'Eid Shopping', or 'Hajj'.",
                            "Set target amounts and target completion dates.",
                            "Add deposits whenever you save money, and watch your animated percentage progress bar reach 100%!"
                        ) else listOf(
                            "নিচের নেভিগেশন বারে 'সঞ্চয়' ট্যাবে যান।",
                            "আপনার স্বপ্ন বা লক্ষ্যের নাম দিন (যেমন: ইমার্জেন্সি ফান্ড, নতুন ল্যাপটপ, ঈদ শপিং বা বাইক কেনা)।",
                            "মোট কত টাকা লাগবে এবং কোন তারিখের মধ্যে জমাতে চান তা নির্ধারণ করুন।",
                            "টাকা জমার সাথে সাথে ডিপোজিট করুন এবং আপনার শতকরা সঞ্চয় বার পূরণ হতে দেখুন।"
                        )
                    )

                    // Function 6: Money Receipt / Voucher Generator
                    FeatureGuideCard(
                        number = "৬",
                        titleBn = "ডিজিটাল টাকা জমার রসিদ ও ভাউচার (Money Receipts)",
                        titleEn = "Digital Money Receipt Voucher Generator",
                        icon = Icons.Default.Receipt,
                        accentColor = Color(0xFF0284C7),
                        steps = if (isEnglish) listOf(
                            "Open the Settings Drawer -> tap 'Money Receipt Generator'.",
                            "Enter payer name, phone, purpose, amount, and payment method.",
                            "Instantly generate a clean, official digital invoice/voucher.",
                            "Export as a high-quality PDF or share directly to WhatsApp, Messenger, or Email!"
                        ) else listOf(
                            "সেটিংস মেনু থেকে 'টাকা জমার রসিদ (ভাউচার)' অপশনটিতে ক্লিক করুন।",
                            "গ্রাহকের নাম, মোবাইল, গ্রহণের কারণ, টাকার পরিমাণ ও পেমেন্ট মেথড পূরণ করুন।",
                            "একটি সম্পূর্ণ ডিজিটাল ক্যাশ মেমো / মানি রিসিট স্বয়ংক্রিয়ভাবে তৈরি হবে।",
                            "সরাসরি প্রিন্ট বা হাই-কোয়ালিটি পিডিএফ আকারে হোয়াটসঅ্যাপ, মেসেঞ্জার বা ইমেইলে শেয়ার করতে পারবেন।"
                        )
                    )

                    // Function 7: Cloud Auto-Sync & Multi-Device Login
                    FeatureGuideCard(
                        number = "৭",
                        titleBn = "ক্লাউড অটো-সিঙ্ক ও একাধিক ডিভাইসে ব্যবহার (Cloud Sync)",
                        titleEn = "Encrypted Cloud Sync & Multi-Device Sync",
                        icon = Icons.Default.CloudSync,
                        accentColor = Color(0xFF14B8A6),
                        steps = if (isEnglish) listOf(
                            "Your account is connected to a secure cloud database powered by Google Firebase Firestore.",
                            "Works 100% offline! When you are without internet, transactions save locally, then sync automatically once online.",
                            "Log in with the same mobile number and password on any other smartphone or tablet — your entire account syncs in real-time.",
                            "You can also export an offline encrypted JSON backup file anytime from Settings."
                        ) else listOf(
                            "আপনার অ্যাকাউন্টটি গুগল ফায়ারবেস ফায়ারস্টোর ডাটাবেসের সাথে সার্বক্ষণিক যুক্ত।",
                            "সম্পূর্ণ অফলাইনে কাজ করে! ইন্টারনেট না থাকলেও নিশ্চিন্তে হিসাব লিখুন, নেট পাওয়া মাত্রই তা ক্লাউডে স্বয়ংক্রিয় সিঙ্ক হয়ে যাবে।",
                            "অন্য যেকোনো ফোনে একই মোবাইল নম্বর ও পাসওয়ার্ড দিয়ে সাইন ইন করলে সব হিসাব সাথে সাথে চলে আসবে।",
                            "এছাড়া সেটিংস থেকে অফলাইন এনক্রিপ্টযুক্ত ব্যাকআপ ফাইল সেভ করে মেমোরিতেও রাখতে পারেন।"
                        )
                    )

                    // Function 8: App Lock & Biometrics
                    FeatureGuideCard(
                        number = "৮",
                        titleBn = "পিন লক ও ফিঙ্গারপ্রিন্ট নিরাপত্তা (App Lock)",
                        titleEn = "PIN & Biometric Security Lock",
                        icon = Icons.Default.Fingerprint,
                        accentColor = Color(0xFF8B5CF6),
                        steps = if (isEnglish) listOf(
                            "Open Settings Drawer -> scroll to App Lock section -> tap 'Set up PIN'.",
                            "Choose a secure 4-digit PIN to lock your personal financial records.",
                            "Enable Fingerprint / Face unlock if your device supports biometric hardware.",
                            "100% Zero-Knowledge: your PIN is stored strictly on your phone hardware and never uploaded to any cloud server."
                        ) else listOf(
                            "সেটিংস মেনু থেকে 'অ্যাপ লক' কার্ডে গিয়ে 'পিন সেটআপ করুন' চাপুন।",
                            "আপনার ব্যক্তিগত হিসাব সুরক্ষিত রাখতে ৪ ডিজিটের একটি পিন কোড নির্ধারণ করুন।",
                            "ফোনে ফিঙ্গারপ্রিন্ট সেন্সর থাকলে 'বায়োমেট্রিক আনলক' সক্রিয় করে এক স্পর্শেই দ্রুত আনলক করুন।",
                            "জিরো-নলেজ প্রাইভেসি: আপনার পিন শুধুমাত্র আপনার এই ফোনে এনক্রিপ্ট হয়ে থাকে, সার্ভারে পাঠানো হয় না।"
                        )
                    )

                    // Function 9: Monthly Overview & Date Filtering
                    FeatureGuideCard(
                        number = "৯",
                        titleBn = "ফিল্টার ও মাসিক বিবরণী (Monthly Overview & Filters)",
                        titleEn = "Filtering & Monthly Analytics",
                        icon = Icons.Default.BarChart,
                        accentColor = Color(0xFF3B82F6),
                        steps = if (isEnglish) listOf(
                            "On the daily screen, tap 'ক্যালেন্ডার' (Calendar) to view transactions for any specific past or future date.",
                            "Use the filter chips: 'সব' (All), 'খরচ' (Expenses), 'আয়' (Income).",
                            "Tap the 'Monthly' tab at bottom to see comprehensive category-wise pie charts, month-by-month cashflow, and text statement exports."
                        ) else listOf(
                            "হোম স্ক্রিনের ক্যালেন্ডার বোতাম চেপে যেকোনো অতীত বা ভবিষ্যতের নির্দিষ্ট দিনের হিসাব এক ক্লিকে দেখুন।",
                            "ফিল্টার চিপ দিয়ে সব, শুধুমাত্র খরচ অথবা শুধুমাত্র আয়ের তালিকা আলাদা করুন।",
                            "নিচের 'মাসিক' ট্যাবে গিয়ে সম্পূর্ণ মাসের ক্যাটাগরি পাই-চার্ট এবং বিস্তারিত স্টেটমেন্ট দেখতে পারেন।"
                        )
                    )

                    // =========================================================================
                    // 4. FOOTER CREDITS & APPRECIATION
                    // =========================================================================
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                        ),
                        border = AppCardDefaults.border(MaterialTheme.colorScheme.primary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Favorite,
                                contentDescription = null,
                                tint = Color(0xFFEF4444),
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = if (isEnglish) "Crafted with dedication for your financial peace of mind" else "আপনার আর্থিক স্বচ্ছতা ও হিসাবের নিরাপত্তার জন্য আন্তরিকভাবে তৈরি",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Built by $developerName | North South University",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Button(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("btn_close_about_dialog")
                    ) {
                        Text(
                            text = if (isEnglish) "Close Guide" else "বুঝেছি, বন্ধ করুন",
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                }

                KeyboardScrollDownHint(
                    scrollState = scrollState,
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }
    }
}
}

@Composable
private fun FeatureGuideCard(
    number: String,
    titleBn: String,
    titleEn: String,
    icon: ImageVector,
    accentColor: Color,
    steps: List<String>
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = AppCardDefaults.border(accentColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = accentColor.copy(alpha = 0.15f),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = titleBn,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = titleEn,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 4.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                steps.forEachIndexed { index, step ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = accentColor.copy(alpha = 0.2f),
                            modifier = Modifier
                                .size(18.dp)
                                .padding(top = 1.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                Text(
                                    text = "${index + 1}",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = accentColor
                                    )
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = step,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 12.5.sp,
                                lineHeight = 18.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

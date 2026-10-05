package com.example.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.ui.viewmodel.ExpenseViewModel

object AppLocale {
    /**
     * Translates a string based on whether English is currently active.
     */
    fun t(isEnglish: Boolean, bn: String, en: String): String = if (isEnglish) en else bn

    // Navigation Labels
    fun navDaily(isEnglish: Boolean) = t(isEnglish, "দৈনিক", "Daily")
    fun navDebts(isEnglish: Boolean) = t(isEnglish, "ধার-দেনা", "Debts")
    fun navCalc(isEnglish: Boolean) = t(isEnglish, "হিসাব", "Calc")
    fun navGoals(isEnglish: Boolean) = t(isEnglish, "সঞ্চয় লক্ষ্য", "Savings")
    fun navMonthly(isEnglish: Boolean) = t(isEnglish, "মাসিক", "Monthly")

    // Common Buttons
    fun addTransaction(isEnglish: Boolean) = t(isEnglish, "লেনদেন যোগ", "Add Entry")
    fun addDebt(isEnglish: Boolean) = t(isEnglish, "ধার হিসাব যোগ", "Add Debt")
    fun addGoal(isEnglish: Boolean) = t(isEnglish, "নতুন লক্ষ্য যোগ", "Add Goal")
    fun downloadReceipt(isEnglish: Boolean) = t(isEnglish, "রিসিট", "Receipt")
    fun profile(isEnglish: Boolean) = t(isEnglish, "প্রোফাইল", "Profile")
    fun theme(isEnglish: Boolean) = t(isEnglish, "থিম", "Theme")
    fun lightMode(isEnglish: Boolean) = t(isEnglish, "লাইট", "Light")
    fun darkMode(isEnglish: Boolean) = t(isEnglish, "ডার্ক", "Dark")

    // Titles
    fun dailyTitle(isEnglish: Boolean) = t(isEnglish, "দৈনিক খরচের হিসাব", "Daily Expense Tracker")
    fun dailySubtitle(isEnglish: Boolean) = t(isEnglish, "সহজ ও নির্ভরযোগ্য ব্যক্তিগত হিসাব", "Simple & reliable expense manager")
    fun debtsTitle(isEnglish: Boolean) = t(isEnglish, "ধার-দেনা খাতা", "Debt & Loan Manager")
    fun debtsSubtitle(isEnglish: Boolean) = t(isEnglish, "পাওনা ও দেনার নির্ভরযোগ্য হিসাব", "Receivables & payables ledger")
    fun goalsTitle(isEnglish: Boolean) = t(isEnglish, "সঞ্চয় ও আর্থিক লক্ষ্য", "Savings & Financial Goals")
    fun goalsSubtitle(isEnglish: Boolean) = t(isEnglish, "ভবিষ্যতের আর্থিক নিরাপত্তা ও সঞ্চয় ট্র্যাকার", "Future security & savings tracker")
    fun calcTitle(isEnglish: Boolean) = t(isEnglish, "স্মার্ট ক্যালকুলেটর", "Smart Calculator")
    fun calcSubtitle(isEnglish: Boolean) = t(isEnglish, "হিসাব করুন ও সরাসরি খরচে যোগ করুন", "Quick financial math & direct logging")
    fun monthlyTitle(isEnglish: Boolean) = t(isEnglish, "মাসিক বিশ্লেষণ ও বাজেট", "Monthly Overview & Budget")
    fun monthlySubtitle(isEnglish: Boolean) = t(isEnglish, "মাসিক আয়, ব্যয় ও ক্যাটাগরি বিশ্লেষণ", "Monthly income, expense & category insights")

    // Common Financial Strings
    fun currentBalance(isEnglish: Boolean) = t(isEnglish, "বর্তমান ব্যালেন্স", "Net Balance")
    fun totalExpense(isEnglish: Boolean) = t(isEnglish, "মোট খরচ", "Total Expense")
    fun totalIncome(isEnglish: Boolean) = t(isEnglish, "মোট আয়", "Total Income")
    fun netBalance(isEnglish: Boolean) = t(isEnglish, "ব্যালেন্স", "Net Balance")
    fun todayExpense(isEnglish: Boolean) = t(isEnglish, "আজকের খরচ", "Today's Expense")
    fun todayIncome(isEnglish: Boolean) = t(isEnglish, "আজকের আয়", "Today's Income")
    fun dayStreak(isEnglish: Boolean, count: Int) = if (isEnglish) "$count Day Streak" else "$count দিনের স্ট্রিক"
    fun downloadReceiptPdf(isEnglish: Boolean) = t(isEnglish, "ক্যাশ মেমো ও হিসাবের রিসিট ডাউনলোড (PDF)", "Download Cash Memo & Receipt (PDF)")

    // Daily Targets / Budget
    fun dailyBudget(isEnglish: Boolean) = t(isEnglish, "আজকের খরচের সীমা", "Today's Budget")
    fun remaining(isEnglish: Boolean) = t(isEnglish, "বাকি আছে", "Remaining")
    fun budgetExceeded(isEnglish: Boolean) = t(isEnglish, "সীমা ছাড়িয়েছে", "Budget Exceeded")
    fun setBudget(isEnglish: Boolean) = t(isEnglish, "বাজেট নির্ধারণ", "Set Budget")

    // Filters
    fun searchPlaceholder(isEnglish: Boolean) = t(isEnglish, "বিবরণ বা ক্যাটাগরি খুঁজুন...", "Search title or category...")
    fun filterAll(isEnglish: Boolean) = t(isEnglish, "সব", "All")
    fun filterExpense(isEnglish: Boolean) = t(isEnglish, "খরচ", "Expense")
    fun filterIncome(isEnglish: Boolean) = t(isEnglish, "আয়", "Income")
    fun filterToday(isEnglish: Boolean) = t(isEnglish, "আজ", "Today")
    fun filterYesterday(isEnglish: Boolean) = t(isEnglish, "গতকাল", "Yesterday")
    fun filterThisWeek(isEnglish: Boolean) = t(isEnglish, "এই সপ্তাহ", "This Week")
    fun filterThisMonth(isEnglish: Boolean) = t(isEnglish, "এই মাস", "This Month")

    // Lists & Empty states
    fun transactionList(isEnglish: Boolean) = t(isEnglish, "লেনদেনের তালিকা", "Transaction History")
    fun entriesCount(isEnglish: Boolean, count: Int) = if (isEnglish) "$count entries" else "$count টি লেনদেন"
    fun noTransactions(isEnglish: Boolean) = t(isEnglish, "কোনো লেনদেন পাওয়া যায়নি", "No transactions found")
    fun tapToAdd(isEnglish: Boolean) = t(isEnglish, "নতুন লেনদেন যোগ করতে নিচের বাটনে চাপ দিন", "Tap the button below to add an entry")

    // Debts
    fun totalReceivable(isEnglish: Boolean) = t(isEnglish, "মোট পাওনা (Receivable)", "Total Receivable")
    fun totalPayable(isEnglish: Boolean) = t(isEnglish, "মোট দেনা (Payable)", "Total Payable")
    fun tabReceivable(isEnglish: Boolean) = t(isEnglish, "পাবো (পাওনা)", "Receivable")
    fun tabPayable(isEnglish: Boolean) = t(isEnglish, "দেবো (দেনা)", "Payable")
    fun willReceive(isEnglish: Boolean) = t(isEnglish, "পাবো (পাওনা)", "Will Receive")
    fun willPay(isEnglish: Boolean) = t(isEnglish, "দেবো (দেনা)", "Will Pay")
    fun tabSettled(isEnglish: Boolean) = t(isEnglish, "পরিশোধিত", "Settled")
    fun noDebts(isEnglish: Boolean) = t(isEnglish, "কোনো ধার বা দেনার হিসাব নেই", "No debts or loans found")
    fun noDebtsSubtitle(isEnglish: Boolean) = t(isEnglish, "নতুন ধার বা দেনা রেকর্ড করতে যোগ করুন", "Add a record to track receivables & payables")
    fun settleDebt(isEnglish: Boolean) = t(isEnglish, "পরিশোধ করুন", "Settle")
    fun call(isEnglish: Boolean) = t(isEnglish, "কল", "Call")
    fun sms(isEnglish: Boolean) = t(isEnglish, "মেসেজ", "SMS")
    fun delete(isEnglish: Boolean) = t(isEnglish, "মুছুন", "Delete")
    fun cancel(isEnglish: Boolean) = t(isEnglish, "বাতিল", "Cancel")
    fun confirm(isEnglish: Boolean) = t(isEnglish, "নিশ্চিত করুন", "Confirm")
    fun save(isEnglish: Boolean) = t(isEnglish, "সংরক্ষণ করুন", "Save")

    // Savings Goals
    fun totalSaved(isEnglish: Boolean) = t(isEnglish, "মোট সঞ্চিত টাকা", "Total Saved")
    fun totalTarget(isEnglish: Boolean) = t(isEnglish, "মোট লক্ষ্য", "Total Target")
    fun noGoals(isEnglish: Boolean) = t(isEnglish, "কোনো সঞ্চয় লক্ষ্য তৈরি করা হয়নি", "No savings goals created yet")
    fun noSavingsGoals(isEnglish: Boolean) = t(isEnglish, "কোনো সঞ্চয় লক্ষ্য তৈরি করা হয়নি", "No savings goals created yet")
    fun noGoalsSubtitle(isEnglish: Boolean) = t(isEnglish, "আপনার স্বপ্নের জন্য নতুন সঞ্চয় লক্ষ্য শুরু করুন", "Start a new savings goal for your future")
    fun deposit(isEnglish: Boolean) = t(isEnglish, "টাকা জমা দিন", "Deposit")
    fun completed(isEnglish: Boolean) = t(isEnglish, "সম্পন্ন", "Completed")

    // Category translation helper (Bidirectional)
    fun category(cat: String, isEnglish: Boolean): String {
        val trimmed = cat.trim()
        if (isEnglish) {
            return when (trimmed) {
                // Expense Categories
                "খাবার ও বাজার", "খাবার ও মুদি", "খাবার" -> "Food & Grocery"
                "যাতায়াত" -> "Transport"
                "বিল ও রিচার্জ", "বিল ও ইউটিলিটি", "বিল" -> "Bills & Recharge"
                "কেনাকাটা" -> "Shopping"
                "চিকিৎসা ও ওষুধ", "চিকিৎসা" -> "Medical & Health"
                "পরিবার ও সংসার", "পরিবার" -> "Family & Home"
                "শিক্ষা" -> "Education"
                "অন্যান্য খরচ" -> "Other Expense"
                "অন্যান্য" -> "Other"
                "বিনোদন" -> "Entertainment"
                "ব্যক্তিগত" -> "Personal"
                // Income Categories
                "বেতন (Salary)", "বেতন" -> "Salary"
                "ব্যবসা ও বিক্রয়", "ব্যবসা ও বিক্রয়", "ব্যবসায়িক আয়" -> "Business & Sales"
                "উপহার / হাদিয়া", "উপহার / হাদিয়া", "উপহার" -> "Gift & Bonus"
                "ফ্রিল্যান্সিং" -> "Freelancing"
                "অন্যান্য আয়", "অন্যান্য আয়" -> "Other Income"
                "বিনিয়োগ", "বিনিয়োগ" -> "Investment"
                // Debt & Savings Categories
                "ধার প্রদান" -> "Loan Given"
                "ধার গ্রহণ" -> "Loan Received"
                "ধার পরিশোধ" -> "Debt Repayment"
                "ধার আদায়", "ধার আদায়" -> "Debt Collection"
                "সঞ্চয় জমা", "সঞ্চয় জমা" -> "Savings Deposit"
                "সঞ্চয় উত্তোলন", "সঞ্চয় উত্তোলন" -> "Savings Withdrawal"
                "সঞ্চয়", "সঞ্চয়" -> "Savings"
                else -> trimmed
            }
        } else {
            return when (trimmed) {
                // English to Bengali
                "Loan Given" -> "ধার প্রদান"
                "Loan Received" -> "ধার গ্রহণ"
                "Food & Grocery", "Food & Market", "Food" -> "খাবার ও বাজার"
                "Transport" -> "যাতায়াত"
                "Bills & Recharge", "Bills & Utility", "Bills" -> "বিল ও রিচার্জ"
                "Shopping" -> "কেনাকাটা"
                "Medical & Health", "Medical & Healthcare", "Medical" -> "চিকিৎসা ও ওষুধ"
                "Family & Home", "Family & Household", "Family" -> "পরিবার ও সংসার"
                "Education" -> "শিক্ষা"
                "Other Expense", "Other Expenses" -> "অন্যান্য খরচ"
                "Other" -> "অন্যান্য"
                "Entertainment" -> "বিনোদন"
                "Personal" -> "ব্যক্তিগত"
                "Salary" -> "বেতন (Salary)"
                "Business & Sales", "Business Income", "Business" -> "ব্যবসা ও বিক্রয়"
                "Gift & Bonus", "Gift / Bonus", "Gift" -> "উপহার / হাদিয়া"
                "Freelancing" -> "ফ্রিল্যান্সিং"
                "Other Income" -> "অন্যান্য আয়"
                "Investment" -> "বিনিয়োগ"
                "Debt Repayment" -> "ধার পরিশোধ"
                "Debt Collection" -> "ধার আদায়"
                "Savings Deposit" -> "সঞ্চয় জমা"
                "Savings Withdrawal" -> "সঞ্চয় উত্তোলন"
                "Savings" -> "সঞ্চয়"
                else -> trimmed
            }
        }
    }

    // Title translation helper (Translates presets and system titles dynamically based on active language)
    fun displayTitle(title: String, isEnglish: Boolean): String {
        val t = title.trim()
        if (isEnglish) {
            return when (t) {
                "চা ও নাস্তা" -> "Tea & Snacks"
                "রিকশা/ভাড়া" -> "Transport"
                "দুপুরের খাবার" -> "Lunch & Meal"
                "রিচার্জ" -> "Mobile Recharge"
                "কাঁচাবাজার" -> "Kitchen & Grocery"
                "ওষুধ" -> "Medicine"
                "কেনাকাটা" -> "Shopping"
                "বিল" -> "Utility Bill"
                "ক্যালকুলেটর থেকে হিসাব" -> "Calculator Entry"
                "ক্যালকুলেটর খরচ" -> "Calculator Expense"
                "ক্যালকুলেটর আয়" -> "Calculator Income"
                "ধার পরিশোধ" -> "Debt Repayment"
                "ধার আদায়" -> "Debt Collection"
                "সঞ্চয় জমা" -> "Savings Deposit"
                "সঞ্চয় উত্তোলন" -> "Savings Withdrawal"
                "কুইক খরচ" -> "Quick Expense"
                "দ্রুত খরচ" -> "Quick Expense"
                else -> {
                    if (t.startsWith("সঞ্চয় জমা: ")) {
                        "Savings Deposit: " + t.removePrefix("সঞ্চয় জমা: ")
                    } else if (t.startsWith("সঞ্চয় উত্তোলন: ")) {
                        "Savings Withdrawal: " + t.removePrefix("সঞ্চয় উত্তোলন: ")
                    } else if (t.startsWith("ধার দেওয়া হয়েছে: ")) {
                        "Loan Given: " + t.removePrefix("ধার দেওয়া হয়েছে: ")
                    } else if (t.startsWith("ধার নেওয়া হয়েছে: ")) {
                        "Loan Borrowed: " + t.removePrefix("ধার নেওয়া হয়েছে: ")
                    } else if (t.startsWith("ক্যালকুলেটর: ")) {
                        "Calculator: " + t.removePrefix("ক্যালকুলেটর: ")
                    } else {
                        t
                    }
                }
            }
        } else {
            return when (t) {
                "Tea & Snacks" -> "চা ও নাস্তা"
                "Transport" -> "রিকশা/ভাড়া"
                "Lunch & Meal" -> "দুপুরের খাবার"
                "Mobile Recharge" -> "রিচার্জ"
                "Kitchen & Grocery" -> "কাঁচাবাজার"
                "Medicine" -> "ওষুধ"
                "Shopping" -> "কেনাকাটা"
                "Utility Bill" -> "বিল"
                "Calculator Entry" -> "ক্যালকুলেটর থেকে হিসাব"
                "Calculator Expense" -> "ক্যালকুলেটর খরচ"
                "Calculator Income" -> "ক্যালকুলেটর আয়"
                "Debt Repayment" -> "ধার পরিশোধ"
                "Debt Collection" -> "ধার আদায়"
                "Loan Given" -> "ধার দেওয়া হয়েছে"
                "Loan Borrowed" -> "ধার নেওয়া হয়েছে"
                "Savings Deposit" -> "সঞ্চয় জমা"
                "Savings Withdrawal" -> "সঞ্চয় উত্তোলন"
                "Quick Expense" -> "দ্রুত খরচ"
                else -> {
                    if (t.startsWith("Savings Deposit: ")) {
                        "সঞ্চয় জমা: " + t.removePrefix("Savings Deposit: ")
                    } else if (t.startsWith("Savings Withdrawal: ")) {
                        "সঞ্চয় উত্তোলন: " + t.removePrefix("Savings Withdrawal: ")
                    } else if (t.startsWith("Loan Given: ")) {
                        "ধার দেওয়া হয়েছে: " + t.removePrefix("Loan Given: ")
                    } else if (t.startsWith("Loan Borrowed: ")) {
                        "ধার নেওয়া হয়েছে: " + t.removePrefix("Loan Borrowed: ")
                    } else if (t.startsWith("Calculator: ")) {
                        "ক্যালকুলেটর: " + t.removePrefix("Calculator: ")
                    } else {
                        t
                    }
                }
            }
        }
    }

    // Note translation helper
    fun displayNote(note: String, isEnglish: Boolean): String {
        val n = note.trim()
        if (n.isBlank()) return ""
        if (isEnglish) {
            if (n == "কুইক খরচ" || n == "দ্রুত খরচ") return "Quick Expense"
            if (n.startsWith("দ্রুত খরচ: ")) return "Quick Expense: " + n.removePrefix("দ্রুত খরচ: ")
            if (n.startsWith("সঞ্চয় লক্ষ্য '") && n.endsWith("'-এ জমা স্থানান্তর")) {
                val goalName = n.removePrefix("সঞ্চয় লক্ষ্য '").removeSuffix("'-এ জমা স্থানান্তর")
                return "Transferred to savings goal '$goalName'"
            }
            if (n.startsWith("সঞ্চয় লক্ষ্য '") && n.endsWith("' থেকে উত্তোলন")) {
                val goalName = n.removePrefix("সঞ্চয় লক্ষ্য '").removeSuffix("' থেকে উত্তোলন")
                return "Withdrawn from savings goal '$goalName'"
            }
        } else {
            if (n == "Quick Expense") return "দ্রুত খরচ"
            if (n.startsWith("Quick: ")) return "দ্রুত খরচ: " + n.removePrefix("Quick: ")
            if (n.startsWith("Quick Expense: ")) return "দ্রুত খরচ: " + n.removePrefix("Quick Expense: ")
            if (n.startsWith("Transferred to savings goal '") && n.endsWith("'")) {
                val goalName = n.removePrefix("Transferred to savings goal '").removeSuffix("'")
                return "সঞ্চয় লক্ষ্য '$goalName'-এ জমা স্থানান্তর"
            }
            if (n.startsWith("Withdrawn from savings goal '") && n.endsWith("'")) {
                val goalName = n.removePrefix("Withdrawn from savings goal '").removeSuffix("'")
                return "সঞ্চয় লক্ষ্য '$goalName' থেকে উত্তোলন"
            }
        }
        return n
    }

    // Khat name translation helper (Translates preset & common khat names dynamically)
    fun khatName(name: String, isEnglish: Boolean): String {
        val k = name.trim()
        if (k.isBlank()) return ""
        if (isEnglish) {
            return when (k) {
                "খাবার ও বাজার", "খাবার ও মুদি", "বাজার" -> "Groceries"
                "বাড়িভাড়া", "বাড়িভাড়া", "বাসাভাড়া", "বাসাভাড়া" -> "House Rent"
                "শিক্ষা খরচ", "শিক্ষা" -> "Education"
                "ওষুধ ও চিকিৎসা", "চিকিৎসা", "ওষুধ" -> "Medical"
                "যাতায়াত ও ভাড়া", "যাতায়াত ও ভাড়া", "যাতায়াত", "যাতায়াত" -> "Transport"
                "শপিং ও পোশাক", "শপিং", "কেনাকাটা" -> "Shopping"
                "গ্যাস ও বিদ্যুৎ বিল", "বিল ও ইউটিলিটি", "বিল" -> "Bills & Utilities"
                "জরুরি তহবিল", "জরুরি" -> "Emergency Fund"
                "নাস্তা ও বিনোদন", "বিনোদন", "নাস্তা" -> "Snacks & Dine"
                "সঞ্চয় ও বিনিয়োগ", "সঞ্চয়", "সঞ্চয়" -> "Savings"
                "মোবাইল ও নেট", "মোবাইল", "রিচার্জ" -> "Mobile & Internet"
                "ভ্রমণ" -> "Travel"
                "উপহার" -> "Gift"
                "ফিটনেস", "জিম" -> "Fitness"
                "পোষা প্রাণী" -> "Pets"
                "অন্যান্য খরচ", "অন্যান্য" -> "Other Expenses"
                "অবশিষ্ট ব্যালেন্স", "অবশিষ্ট" -> "Remaining Balance"
                else -> k
            }
        } else {
            return when (k) {
                "Groceries", "Grocery", "Food" -> "খাবার ও বাজার"
                "House Rent", "Rent", "Home" -> "বাড়িভাড়া"
                "Education" -> "শিক্ষা খরচ"
                "Medical", "Healthcare" -> "ওষুধ ও চিকিৎসা"
                "Transport" -> "যাতায়াত ও ভাড়া"
                "Shopping" -> "শপিং ও পোশাক"
                "Bills & Utilities", "Bills" -> "গ্যাস ও বিদ্যুৎ বিল"
                "Emergency Fund", "Emergency" -> "জরুরি তহবিল"
                "Snacks & Dine", "Dine" -> "নাস্তা ও বিনোদন"
                "Savings" -> "সঞ্চয় ও বিনিয়োগ"
                "Mobile & Internet", "Mobile", "Phone" -> "মোবাইল ও নেট"
                "Travel" -> "ভ্রমণ"
                "Gift" -> "উপহার"
                "Fitness" -> "ফিটনেস"
                "Pets" -> "পোষা প্রাণী"
                "Other Expenses", "Other" -> "অন্যান্য খরচ"
                "Remaining Balance", "Oboshisto" -> "অবশিষ্ট ব্যালেন্স"
                else -> k
            }
        }
    }

    // Month translation helper
    fun monthName(monthIndex: Int, isEnglish: Boolean): String {
        val bnMonths = listOf("জানুয়ারি", "ফেব্রুয়ারি", "মার্চ", "এপ্রিল", "মে", "জুন", "জুলাই", "আগস্ট", "সেপ্টেম্বর", "অক্টোবর", "নভেম্বর", "ডিসেম্বর")
        val enMonths = listOf("January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December")
        val idx = monthIndex.coerceIn(0, 11)
        return if (isEnglish) enMonths[idx] else bnMonths[idx]
    }

    // Exit Dialog
    fun exitTitle(isEnglish: Boolean) = t(isEnglish, "অ্যাপ থেকে প্রস্থান করবেন?", "Exit application?")
    fun exitMessage(isEnglish: Boolean) = t(isEnglish, "আপনি কি নিশ্চিত যে আপনি অ্যাপ থেকে বের হতে চান?", "Are you sure you want to exit the app?")
    fun exitConfirm(isEnglish: Boolean) = t(isEnglish, "হ্যাঁ, বের হন", "Yes, Exit")
    fun exitDismiss(isEnglish: Boolean) = t(isEnglish, "না, থাকুন", "No, Stay")

    // Additional Screen Helpers
    fun addTransactionTitle(isEnglish: Boolean) = t(isEnglish, "নতুন লেনদেন যোগ করুন", "Add New Transaction")
    fun expense(isEnglish: Boolean) = t(isEnglish, "খরচ", "Expense")
    fun income(isEnglish: Boolean) = t(isEnglish, "আয়", "Income")
    fun amount(isEnglish: Boolean) = t(isEnglish, "টাকার পরিমাণ", "Amount (৳)")
    fun titleOrNote(isEnglish: Boolean) = t(isEnglish, "বিবরণ / শিরোনাম", "Title / Note")
    fun categoryLabel(isEnglish: Boolean) = t(isEnglish, "ক্যাটাগরি নির্বাচন করুন", "Select Category")
    fun noteOptional(isEnglish: Boolean) = t(isEnglish, "অতিরিক্ত নোট (ঐচ্ছিক)", "Additional Note (Optional)")
    fun recentTransactions(isEnglish: Boolean) = t(isEnglish, "সাম্প্রতিক লেনদেন", "Recent Transactions")
    fun viewAll(isEnglish: Boolean) = t(isEnglish, "সব দেখুন", "View All")
    fun monthlyBudgetUsage(isEnglish: Boolean) = t(isEnglish, "মাসিক বাজেট ব্যবহার", "Monthly Budget Usage")
    fun spent(isEnglish: Boolean) = t(isEnglish, "খরচ", "Spent")
    fun deleteConfirm(isEnglish: Boolean) = t(isEnglish, "মুছবেন?", "Delete?")
    fun deleteItemQuestion(isEnglish: Boolean, title: String) = t(isEnglish, "'$title' মুছে ফেলতে চান?", "Do you want to delete '$title'?")
    fun iWillReceive(isEnglish: Boolean) = t(isEnglish, "আমি পাব", "Receivable")
    fun iWillPay(isEnglish: Boolean) = t(isEnglish, "আমি দেব", "Payable")
    fun othersOweMe(isEnglish: Boolean) = t(isEnglish, "অন্যরা দেবে", "Others owe me")
    fun iOweOthers(isEnglish: Boolean) = t(isEnglish, "আমার দেনা", "I owe others")
    fun personName(isEnglish: Boolean) = t(isEnglish, "ব্যক্তির নাম", "Person Name")
    fun mobileOptional(isEnglish: Boolean) = t(isEnglish, "মোবাইল নম্বর (ঐচ্ছিক)", "Mobile Number (Optional)")
    fun dueDateOptional(isEnglish: Boolean) = t(isEnglish, "পরিশোধের তারিখ (ঐচ্ছিক)", "Due Date (Optional)")
    fun debtType(isEnglish: Boolean) = t(isEnglish, "লেনদেনের ধরন", "Transaction Type")
    fun addDebtTitle(isEnglish: Boolean) = t(isEnglish, "নতুন ধার-দেনা যোগ", "Add Debt Record")
    fun settleDebtConfirm(isEnglish: Boolean, name: String) = t(isEnglish, "$name-এর হিসাব সম্পূর্ণ পরিশোধ হিসেবে চিহ্নিত করবেন?", "Mark debt with $name as fully settled?")
    fun addGoalTitle(isEnglish: Boolean) = t(isEnglish, "নতুন সঞ্চয় লক্ষ্য যোগ", "Add Savings Goal")
    fun goalName(isEnglish: Boolean) = t(isEnglish, "লক্ষ্যের নাম", "Goal Title")
    fun targetAmountLabel(isEnglish: Boolean) = t(isEnglish, "টার্গেট বা কাঙ্ক্ষিত পরিমাণ", "Target Amount (৳)")
    fun initialDeposit(isEnglish: Boolean) = t(isEnglish, "প্রাথমিক জমা (ঐচ্ছিক)", "Initial Deposit (Optional)")
    fun savedSoFar(isEnglish: Boolean) = t(isEnglish, "জমেছে", "Saved")
    fun targetLabel(isEnglish: Boolean) = t(isEnglish, "লক্ষ্য", "Target")
    fun goalAchieved(isEnglish: Boolean) = t(isEnglish, "🎉 লক্ষ্য অর্জিত!", "🎉 Goal Achieved!")
    fun depositToGoalTitle(isEnglish: Boolean, title: String) = t(isEnglish, "'$title' এ নতুন জমা", "Deposit to '$title'")
    fun depositAmount(isEnglish: Boolean) = t(isEnglish, "জমার পরিমাণ (টাকা)", "Deposit Amount (৳)")
    fun confirmDeposit(isEnglish: Boolean) = t(isEnglish, "জমা করুন", "Deposit")
    fun quickGlance(isEnglish: Boolean) = t(isEnglish, "📊 বর্তমান হিসাবের একনজর (ট্যাপ করে সংখ্যা নিন):", "📊 Current glance (Tap to insert number):")
    fun addToExpense(isEnglish: Boolean) = t(isEnglish, "খরচে যোগ", "Add to Expense")
    fun addToIncome(isEnglish: Boolean) = t(isEnglish, "আয়ে যোগ", "Add to Income")
    fun monthlyNetSavings(isEnglish: Boolean) = t(isEnglish, "নেট সঞ্চয়", "Net Savings")
    fun dailyAvgExpense(isEnglish: Boolean) = t(isEnglish, "দৈনিক গড় খরচ", "Daily Average")
    fun budgetStatus(isEnglish: Boolean) = t(isEnglish, "বাজেটের অবস্থা", "Budget Status")
    fun backToCurrentMonth(isEnglish: Boolean) = t(isEnglish, "চলতি মাসে ফিরুন", "Back to Current Month")
    fun categorySpending(isEnglish: Boolean) = t(isEnglish, "ক্যাটাগরি অনুযায়ী খরচ", "Spending by Category")
    fun weeklySpendingTrend(isEnglish: Boolean) = t(isEnglish, "সাপ্তাহিক খরচের ট্রেন্ড", "Weekly Spending Trend")
    fun userProfileTitle(isEnglish: Boolean) = t(isEnglish, "ব্যবহারকারী প্রোফাইল", "User Profile")
    fun receiptHeading(isEnglish: Boolean) = t(isEnglish, "সার্বজনীন আর্থিক রিসিট ও চার্ট", "Universal Financial Statement & Charts")
    fun receiptSubheading(isEnglish: Boolean) = t(isEnglish, "লেনদেন, গ্রাফ-চার্ট, ধার-দেনা ও সঞ্চয়সহ সম্পূর্ণ বিবরণী", "Complete statement with transactions, charts, debts & goals")
    fun downloadPdfButton(isEnglish: Boolean) = t(isEnglish, "পিডিএফ ডাউনলোড", "Download PDF")
    fun sharePdfButton(isEnglish: Boolean) = t(isEnglish, "শেয়ার", "Share")
    fun openPdfButton(isEnglish: Boolean) = t(isEnglish, "ওপেন করুন", "Open PDF")

    // Debt Settlement & Savings Opinion
    fun debtSettlementOpinionTitle(isEnglish: Boolean) = t(isEnglish, "ধার-দেনা নিষ্পত্তি ও ব্যালেন্স সমন্বয়", "Debt Settlement & Balance Adjustment")
    fun debtPayOpinionQuestion(isEnglish: Boolean, name: String, amount: String) = t(
        isEnglish,
        "আপনি $name-কে $amount পরিশোধ করছেন। এই পরিশোধিত টাকা কি আপনার মূল ক্যাশ ব্যালেন্স/হিসাব থেকে খরচ (Expense) হিসেবে কেটে নেওয়া হবে?",
        "You are repaying $amount to $name. Should this repayment be deducted from your main cash balance as an Expense?"
    )
    fun debtReceiveOpinionQuestion(isEnglish: Boolean, name: String, amount: String) = t(
        isEnglish,
        "আপনি $name-এর কাছ থেকে $amount আদায় করছেন। এই আদায়কৃত টাকা কি আপনার মূল ক্যাশ ব্যালেন্সে আয় (Income) হিসেবে যোগ করা হবে?",
        "You are collecting $amount from $name. Should this collected amount be added to your main cash balance as Income?"
    )
    fun yesDeductBalance(isEnglish: Boolean) = t(isEnglish, "হ্যাঁ, ব্যালেন্স থেকে বাদ দিন", "Yes, Deduct from Balance")
    fun yesAddBalance(isEnglish: Boolean) = t(isEnglish, "হ্যাঁ, ব্যালেন্সে যোগ করুন", "Yes, Add to Balance")
    fun noMarkSettledOnly(isEnglish: Boolean) = t(isEnglish, "না, শুধু পরিশোধিত চিহ্নিত করুন", "No, Mark Settled Only")
    fun savingsDeductSwitchTitle(isEnglish: Boolean) = t(isEnglish, "মূল ক্যাশ ব্যালেন্স থেকে কাটা হবে?", "Deduct from Main Cash Balance?")
    fun savingsDeductActive(isEnglish: Boolean) = t(isEnglish, "✓ হ্যাঁ, মূল হিসাব থেকে খরচ হিসেবে বাদ যাবে ও হোম পেজে দেখাবে", "✓ Yes, deduct as an expense and show on Home Page")
    fun savingsDeductInactive(isEnglish: Boolean) = t(isEnglish, "✗ না, শুধু সঞ্চয় লক্ষ্যে যোগ হবে (মূল ব্যালেন্স অপরিবর্তিত)", "✗ No, update goal only (main balance unchanged)")
    fun withdrawSavings(isEnglish: Boolean) = t(isEnglish, "টাকা উত্তোলন", "Withdraw")
    fun withdrawTitle(isEnglish: Boolean, goal: String) = t(isEnglish, "'$goal' থেকে টাকা উত্তোলন", "Withdraw from '$goal'")
    fun withdrawAmount(isEnglish: Boolean) = t(isEnglish, "উত্তোলনের পরিমাণ", "Withdrawal Amount")
    fun savingsWithdrawAddSwitchTitle(isEnglish: Boolean) = t(isEnglish, "উত্তোলিত টাকা মূল ব্যালেন্সে যোগ হবে?", "Add withdrawn money to Main Balance?")
    fun savingsWithdrawActive(isEnglish: Boolean) = t(isEnglish, "✓ হ্যাঁ, মূল হিসাবে আয় হিসেবে যোগ হবে", "✓ Yes, add as income to main balance")
    fun savingsWithdrawInactive(isEnglish: Boolean) = t(isEnglish, "✗ না, শুধু সঞ্চয় লক্ষ্য থেকে কমবে", "✗ No, decrease goal only")

    // Daily Page Calendar & Uneducated User Support
    fun openCalendar(isEnglish: Boolean) = t(isEnglish, "ক্যালেন্ডার", "Calendar")
    fun selectAnyDate(isEnglish: Boolean) = t(isEnglish, "যেকোনো তারিখের হিসাব দেখুন", "View Any Date's Ledger")
    fun backToToday(isEnglish: Boolean) = t(isEnglish, "আজকের দিনে ফিরুন", "Back to Today")
    fun showingLedgerForDate(isEnglish: Boolean, date: String) = t(isEnglish, "📅 $date -এর হিসাব", "📅 Ledger for $date")
    fun easyGuideGreen(isEnglish: Boolean) = t(isEnglish, "সবুজ মানে টাকা আসছে (+) এবং লাল মানে টাকা খরচ হয়েছে (-)", "Green = Money In (+), Red = Money Spent (-)")
    fun cashInHand(isEnglish: Boolean) = t(isEnglish, "হাতে ক্যাশ টাকা আছে", "Cash in Hand")
    fun moneyIn(isEnglish: Boolean) = t(isEnglish, "টাকা ঢুকছে (আয়)", "Money In (Income)")
    fun moneyOut(isEnglish: Boolean) = t(isEnglish, "টাকা গেছে (খরচ)", "Money Out (Expense)")
    fun selectDateModalTitle(isEnglish: Boolean) = t(isEnglish, "হিসাব দেখতে দিন নির্বাচন করুন", "Select Date to View Accounts")

    // Monthly Page Unique Analytics
    fun vsLastMonth(isEnglish: Boolean) = t(isEnglish, "গত মাসের চেয়ে", "vs Last Month")
    fun projectedSpend(isEnglish: Boolean) = t(isEnglish, "মাস শেষে সম্ভাব্য খরচ", "Projected Month-End")
    fun peakExpenseDay(isEnglish: Boolean) = t(isEnglish, "সর্বোচ্চ খরচের দিন", "Peak Spending Day")
    fun financialHealth(isEnglish: Boolean) = t(isEnglish, "আর্থিক শৃঙ্খলা স্কোর", "Financial Health Score")
    fun monthlyDistribution(isEnglish: Boolean) = t(isEnglish, "খরচের খাতভিত্তিক অনুপাত", "Spending Distribution")
    fun monthDailyCalendar(isEnglish: Boolean) = t(isEnglish, "দৈনিক খরচের ক্যালেন্ডার চার্ট", "Daily Spending Calendar")
    fun jumpMonthYear(isEnglish: Boolean) = t(isEnglish, "মাস ও সাল বাছাই", "Jump to Month & Year")
    fun noSpendDay(isEnglish: Boolean) = t(isEnglish, "খরচহীন দিন", "No Spend Day")

    // Khat / Category Translation
    fun khatWord(isEnglish: Boolean): String = if (isEnglish) "Khat / Fund" else "খাত"

    // Dhar-Dena Khat & Fund Source Strings
    fun debtFundSourceTitle(isEnglish: Boolean) = t(isEnglish, "ব্যালেন্স / খাত সমন্বয় (ঐচ্ছিক)", "Balance / Fund Adjustment (Optional)")
    fun debtDeductSourceSubtitle(isEnglish: Boolean) = t(isEnglish, "এই ধারটি দেওয়ার টাকা কোথা থেকে কাটা হবে তা বেছে নিন", "Select where the lent money will be deducted from")
    fun debtAddSourceSubtitle(isEnglish: Boolean) = t(isEnglish, "ধার নেওয়া টাকা কোথায় যোগ করতে চান তা বেছে নিন", "Select where the borrowed money will be added")
    fun debtSourceNone(isEnglish: Boolean) = t(isEnglish, "ব্যালেন্স থেকে কাটবেন না (শুধুমাত্র রেকর্ড)", "Do not deduct (Record debt only)")
    fun debtSourceNonePay(isEnglish: Boolean) = t(isEnglish, "ব্যালেন্সে যোগ করবেন না (শুধুমাত্র রেকর্ড)", "Do not add (Record debt only)")
    fun debtSourceMain(isEnglish: Boolean) = t(isEnglish, "মূল ক্যাশ ব্যালেন্স", "Main Cash Balance")
    fun debtSourceMainDeductDesc(isEnglish: Boolean) = t(isEnglish, "মূল ব্যালেন্স থেকে খরচ হিসেবে কাটা হবে ও হোম পেজে দেখাবে", "Deduct from main balance as an expense and show on Home")
    fun debtSourceMainAddDesc(isEnglish: Boolean) = t(isEnglish, "মূল ব্যালেন্সে আয় হিসেবে যোগ হবে ও হোম পেজে দেখাবে", "Add to main balance as income and show on Home")
    fun insufficientMainBalance(isEnglish: Boolean, available: String) = t(
        isEnglish,
        "মূল ক্যাশ ব্যালেন্সে পর্যাপ্ত টাকা নেই (আছে: $available)",
        "Insufficient main cash balance (Available: $available)"
    )
    fun debtSourceOboshisto(isEnglish: Boolean) = t(isEnglish, "অবশিষ্ট ব্যালেন্স", "Remaining Balance (Oboshisto)")
    fun debtSourceOboshistoDeductDesc(isEnglish: Boolean) = t(isEnglish, "অবশিষ্ট ব্যালেন্স থেকে খরচ হিসেবে কাটা হবে ও হোম পেজে দেখাবে", "Deduct from remaining balance as expense & show on Home")
    fun debtSourceOboshistoAddDesc(isEnglish: Boolean) = t(isEnglish, "অবশিষ্ট ব্যালেন্সে আয় হিসেবে যোগ হবে ও হোম পেজে দেখাবে", "Add to remaining balance as income & show on Home")
    fun insufficientOboshistoBalance(isEnglish: Boolean, available: String) = t(
        isEnglish,
        "অবশিষ্ট ব্যালেন্সে পর্যাপ্ত টাকা নেই (আছে: $available)",
        "Insufficient remaining balance (Available: $available)"
    )
    fun sourceOboshistoBadge(isEnglish: Boolean) = t(isEnglish, "খাত: অবশিষ্ট ব্যালেন্স", "Khat: Remaining Balance")
    fun allKhatsScrollHint(isEnglish: Boolean) = t(isEnglish, "সকল খাত (অবশিষ্ট ব্যালেন্স সহ স্ক্রল করে বেছে নিন):", "All Khats (including Remaining Balance - scroll to choose):")
    fun debtSourceKhatsSection(isEnglish: Boolean) = t(isEnglish, "তৈরি করা খাতসমূহ থেকে বেছে নিন:", "Or choose from your created Khats:")
    fun noKhatsCreatedHint(isEnglish: Boolean) = t(isEnglish, "হোম পেজে নতুন খাত তৈরি করে রাখলে এখান থেকে সরাসরি নির্দিষ্ট খাত বেছে নিতে পারবেন।", "Create custom khats on the Home Page to choose specific funds directly from here.")
    fun insufficientKhatBalance(isEnglish: Boolean, khatName: String, available: String) = t(
        isEnglish,
        "'$khatName' খাতে পর্যাপ্ত ব্যালেন্স নেই (আছে: $available)",
        "Insufficient balance in '$khatName' (Available: $available)"
    )
    fun sourceKhatBadge(isEnglish: Boolean, khatName: String) = t(isEnglish, "খাত: $khatName", "Khat: $khatName")
    fun sourceMainBadge(isEnglish: Boolean) = t(isEnglish, "মূল ব্যালেন্স", "Main Balance")
    fun createdKhatsScrollHint(isEnglish: Boolean) = t(isEnglish, "আপনার তৈরি খাতসমূহ (স্ক্রল করে নির্বাচন করুন):", "Your Created Khats (scroll to choose):")
    fun noKhatsMainOnlyHint(isEnglish: Boolean) = t(
        isEnglish,
        "কোনো খাত তৈরি করা নেই। তাই মূল ব্যালেন্স থেকে সমন্বয় করার সুযোগ রয়েছে।",
        "No custom khats created yet. You can adjust with Main Balance."
    )

    // Debt Installment & Partial Settlement Strings
    fun fullPaymentOption(isEnglish: Boolean) = t(isEnglish, "সম্পূর্ণ টাকা পরিশোধিত", "Full Payment / Completely Settled")
    fun fullPaymentOptionDesc(isEnglish: Boolean, remaining: String) = t(
        isEnglish,
        "অবশিষ্ট পুরো $remaining টাকা একবারে পরিশোধ বা আদায় সম্পন্ন হবে এবং এই ধারটির হিসাব ক্লোজ হবে।",
        "The remaining entire $remaining will be fully settled at once and closed."
    )
    fun partialPaymentOption(isEnglish: Boolean) = t(isEnglish, "কিছু টাকা পরিশোধিত (কিস্তি)", "Partial Payment (Installment)")
    fun partialPaymentOptionDesc(isEnglish: Boolean) = t(
        isEnglish,
        "নির্দিষ্ট কিছু টাকা এখন পরিশোধ বা আদায় হবে, বাকি টাকা পরবর্তীতে দেওয়ার জন্য হিসেবে থাকবে।",
        "Pay or collect a partial amount now, keeping the remaining balance active."
    )
    fun enterPaidAmount(isEnglish: Boolean) = t(isEnglish, "পরিশোধিত টাকার পরিমাণ লিখুন", "Enter Amount Paid")
    fun remainingAfterPayment(isEnglish: Boolean, remaining: String) = t(
        isEnglish,
        "এই কিস্তির পর বাকি থাকবে: $remaining",
        "Remaining balance after this payment: $remaining"
    )
    fun amountExceedsError(isEnglish: Boolean, remaining: String) = t(
        isEnglish,
        "টাকার পরিমাণ বাকি টাকার ($remaining)-এর চেয়ে বেশি হতে পারবে না!",
        "Amount cannot exceed the remaining balance of $remaining!"
    )
    fun alreadyPaid(isEnglish: Boolean) = t(isEnglish, "ইতোমধ্যে পরিশোধিত", "Already Paid")
    fun currentRemaining(isEnglish: Boolean) = t(isEnglish, "বর্তমান অবশিষ্ট বাকি", "Current Remaining")
    fun totalDebtAmount(isEnglish: Boolean) = t(isEnglish, "মোট ধারের পরিমাণ", "Total Debt Amount")
    fun recordPaymentButton(isEnglish: Boolean) = t(isEnglish, "টাকা পরিশোধ বা কিস্তি জমা দিন", "Record Payment / Installment")
    fun recordPaymentTitle(isEnglish: Boolean) = t(isEnglish, "ধার পরিশোধ ও কিস্তি হিসাব", "Debt Settlement & Installment")
    fun paymentHistory(isEnglish: Boolean) = t(isEnglish, "কিস্তি পরিশোধের ইতিহাস", "Payment & Installment History")
    fun noPaymentHistory(isEnglish: Boolean) = t(isEnglish, "এখনও কোনো কিস্তি পরিশোধ করা হয়নি", "No installments recorded yet")
    fun confirmPayment(isEnglish: Boolean) = t(isEnglish, "পরিশোধ নিশ্চিত করুন", "Confirm Payment")
    fun installmentNoteHint(isEnglish: Boolean) = t(isEnglish, "মন্তব্য / নোট (ঐচ্ছিক)", "Note / Remark (Optional)")
    fun insufficientMainBalanceForDebt(isEnglish: Boolean, maxAmount: String, currentBal: String) = t(
        isEnglish,
        "অ্যাকাউন্টে পর্যাপ্ত ব্যালেন্স নেই! বর্তমান ব্যালেন্স $currentBal, আপনি সর্বোচ্চ $maxAmount পরিশোধ করতে পারবেন।",
        "Insufficient account balance! Current balance is $currentBal, you can pay at most $maxAmount."
    )
    fun zeroMainBalanceError(isEnglish: Boolean) = t(
        isEnglish,
        "অ্যাকাউন্টে কোনো ব্যালেন্স নেই (৳০)! ব্যালেন্স মাইনাস হতে পারবে না, প্রথমে অ্যাকাউন্টে টাকা যোগ করুন।",
        "No account balance (৳0)! Add money to account first to avoid negative balance."
    )
    fun maxPayableChipLabel(isEnglish: Boolean, maxAmount: String) = t(
        isEnglish,
        "সর্বোচ্চ সম্ভব ($maxAmount)",
        "Max Possible ($maxAmount)"
    )
    fun accountBalanceStatus(isEnglish: Boolean, balance: String) = t(
        isEnglish,
        "বর্তমান মূল ব্যালেন্স: $balance",
        "Current Main Balance: $balance"
    )
    fun partialPayExceedsBalance(isEnglish: Boolean, currentBal: String, maxAmount: String) = t(
        isEnglish,
        "ব্যালেন্সের বেশি টাকা পরিশোধ করা যাবে না! অ্যাকাউন্টে আছে $currentBal। সর্বোচ্চ $maxAmount পরিশোধ করতে পারবেন।",
        "Cannot pay more than account balance! Available: $currentBal. You can pay at most $maxAmount."
    )
    fun debtCardInsufficientBalance(isEnglish: Boolean, maxPayable: String, deficit: String, currentBal: String) = t(
        isEnglish,
        "⚠️ অ্যাকাউন্টে পর্যাপ্ত টাকা নেই (আছে $currentBal)। আপনি সর্বোচ্চ $maxPayable পরিশোধ করতে পারবেন (ঘাটতি: $deficit)।",
        "⚠️ Insufficient balance (available $currentBal). You can only pay up to $maxPayable (Deficit: $deficit)."
    )
    fun debtCardZeroBalance(isEnglish: Boolean) = t(
        isEnglish,
        "🚫 অ্যাকাউন্টে কোনো ব্যালেন্স নেই (৳০)! ব্যালেন্স মাইনাস হতে পারবে না, তাই পরিশোধ সম্ভব নয়।",
        "🚫 No account balance (৳0)! Balance cannot be negative, repayment disabled."
    )
    fun debtCardReadyForRepay(isEnglish: Boolean, balance: String) = t(
        isEnglish,
        "✓ অ্যাকাউন্টে পর্যাপ্ত ব্যালেন্স রয়েছে ($balance)। সম্পূর্ণ দেনা পরিশোধ সম্ভব।",
        "✓ Sufficient account balance available ($balance). Full repayment possible."
    )
    fun debtReceiveAccountSyncHint(isEnglish: Boolean) = t(
        isEnglish,
        "💡 টাকা আদায় করা হলে তা স্বয়ংক্রিয়ভাবে মূল অ্যাকাউন্টে যুক্ত হবে।",
        "💡 Money collected will automatically be added to your main account."
    )
    fun debtMaxRepayableCapability(isEnglish: Boolean, maxAmount: String) = t(
        isEnglish,
        "পরিশোধ সক্ষমতা: সর্বোচ্চ $maxAmount",
        "Repayment Capacity: Max $maxAmount"
    )
}

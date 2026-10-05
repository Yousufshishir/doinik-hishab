package com.example.data.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import java.util.Locale

data class SimpleCategory(
    val id: String,
    val name: String,
    val icon: ImageVector,
    val color: Color
)

object SimpleCategoryData {
    val expenseCategories = listOf(
        SimpleCategory("food", "খাবার ও বাজার", Icons.Default.Restaurant, Color(0xFF2E7D32)),
        SimpleCategory("transport", "যাতায়াত", Icons.Default.DirectionsBus, Color(0xFF0288D1)),
        SimpleCategory("bills", "বিল ও রিচার্জ", Icons.Default.Receipt, Color(0xFFF57C00)),
        SimpleCategory("shopping", "কেনাকাটা", Icons.Default.ShoppingCart, Color(0xFFE91E63)),
        SimpleCategory("medical", "চিকিৎসা ও ওষুধ", Icons.Default.LocalHospital, Color(0xFFD32F2F)),
        SimpleCategory("family", "পরিবার ও সংসার", Icons.Default.Home, Color(0xFF7B1FA2)),
        SimpleCategory("education", "শিক্ষা", Icons.Default.School, Color(0xFF5D4037)),
        SimpleCategory("other_exp", "অন্যান্য খরচ", Icons.Default.MoreHoriz, Color(0xFF607D8B))
    )

    val incomeCategories = listOf(
        SimpleCategory("salary", "বেতন (Salary)", Icons.Default.Work, Color(0xFF1B5E20)),
        SimpleCategory("business", "ব্যবসা ও বিক্রয়", Icons.Default.Store, Color(0xFF0D47A1)),
        SimpleCategory("gift", "উপহার / হাদিয়া", Icons.Default.CardGiftcard, Color(0xFFE65100)),
        SimpleCategory("freelance", "ফ্রিল্যান্সিং", Icons.Default.AttachMoney, Color(0xFF6A1B9A)),
        SimpleCategory("other_inc", "অন্যান্য আয়", Icons.Default.Payments, Color(0xFF37474F))
    )

    fun getCategoryIcon(categoryName: String): ImageVector {
        val lower = categoryName.lowercase().trim()
        return when {
            lower.contains("food") || lower.contains("খাবার") || lower.contains("মুদি") || lower.contains("বাজার") || lower.contains("grocery") -> Icons.Default.Restaurant
            lower.contains("transport") || lower.contains("যাতায়াত") || lower.contains("রিকশা") || lower.contains("ভাড়া") || lower.contains("গাড়ি") -> Icons.Default.DirectionsBus
            lower.contains("bill") || lower.contains("বিল") || lower.contains("ইউটিলিটি") || lower.contains("রিচার্জ") || lower.contains("recharge") -> Icons.Default.Receipt
            lower.contains("shop") || lower.contains("কেনাকাটা") || lower.contains("মার্কেট") -> Icons.Default.ShoppingCart
            lower.contains("medic") || lower.contains("চিকিৎসা") || lower.contains("ওষুধ") || lower.contains("ডাক্তার") || lower.contains("hospital") -> Icons.Default.LocalHospital
            lower.contains("family") || lower.contains("পরিবার") || lower.contains("সংসার") || lower.contains("বাসা") || lower.contains("home") -> Icons.Default.Home
            lower.contains("edu") || lower.contains("শিক্ষা") || lower.contains("পড়া") || lower.contains("বই") || lower.contains("school") -> Icons.Default.School
            lower.contains("salary") || lower.contains("বেতন") -> Icons.Default.Work
            lower.contains("business") || lower.contains("ব্যবসা") || lower.contains("বিক্রয়") -> Icons.Default.Store
            lower.contains("gift") || lower.contains("উপহার") || lower.contains("হাদিয়া") -> Icons.Default.CardGiftcard
            lower.contains("freelance") || lower.contains("ফ্রিল্যান্সিং") -> Icons.Default.AttachMoney
            lower.contains("debt") || lower.contains("ধার") || lower.contains("কর্জ") || lower.contains("ঋণ") -> Icons.Default.AccountBalanceWallet
            lower.contains("saving") || lower.contains("সঞ্চয়") || lower.contains("জমা") -> Icons.Default.Savings
            else -> {
                val foundExp = expenseCategories.find { it.name.equals(categoryName, ignoreCase = true) }
                if (foundExp != null) return foundExp.icon
                val foundInc = incomeCategories.find { it.name.equals(categoryName, ignoreCase = true) }
                if (foundInc != null) return foundInc.icon
                Icons.Default.Category
            }
        }
    }

    fun getCategoryColor(categoryName: String): Color {
        val lower = categoryName.lowercase().trim()
        return when {
            lower.contains("food") || lower.contains("খাবার") || lower.contains("মুদি") || lower.contains("বাজার") || lower.contains("grocery") -> Color(0xFF2E7D32)
            lower.contains("transport") || lower.contains("যাতায়াত") || lower.contains("রিকশা") || lower.contains("ভাড়া") -> Color(0xFF0288D1)
            lower.contains("bill") || lower.contains("বিল") || lower.contains("ইউটিলিটি") || lower.contains("রিচার্জ") -> Color(0xFFF57C00)
            lower.contains("shop") || lower.contains("কেনাকাটা") -> Color(0xFFE91E63)
            lower.contains("medic") || lower.contains("চিকিৎসা") || lower.contains("ওষুধ") -> Color(0xFFD32F2F)
            lower.contains("family") || lower.contains("পরিবার") || lower.contains("সংসার") -> Color(0xFF7B1FA2)
            lower.contains("edu") || lower.contains("শিক্ষা") -> Color(0xFF5D4037)
            lower.contains("salary") || lower.contains("বেতন") -> Color(0xFF1B5E20)
            lower.contains("business") || lower.contains("ব্যবসা") -> Color(0xFF0D47A1)
            lower.contains("gift") || lower.contains("উপহার") -> Color(0xFFE65100)
            lower.contains("freelance") || lower.contains("ফ্রিল্যান্সিং") -> Color(0xFF6A1B9A)
            lower.contains("debt") || lower.contains("ধার") -> Color(0xFFC2185B)
            lower.contains("saving") || lower.contains("সঞ্চয়") -> Color(0xFF00897B)
            else -> {
                val foundExp = expenseCategories.find { it.name.equals(categoryName, ignoreCase = true) }
                if (foundExp != null) return foundExp.color
                val foundInc = incomeCategories.find { it.name.equals(categoryName, ignoreCase = true) }
                if (foundInc != null) return foundInc.color
                Color(0xFF546E7A)
            }
        }
    }
}

fun formatTakaSafe(amount: Double): String {
    val isNegative = amount < 0
    val absVal = kotlin.math.abs(amount)
    val prefix = if (isNegative) "-৳ " else "৳ "
    val formatted = if (absVal % 1.0 == 0.0) {
        String.format(Locale.US, "%,.0f", absVal)
    } else {
        String.format(Locale.US, "%,.2f", absVal)
    }
    return "$prefix$formatted"
}

/**
 * Compact formatting for large sums (লাখ / কোটি) to prevent interface overflow.
 */
fun formatTakaCompact(amount: Double): String {
    val isNegative = amount < 0
    val absVal = kotlin.math.abs(amount)
    val prefix = if (isNegative) "-৳ " else "৳ "

    return when {
        absVal >= 10_000_000 -> { // 1 কোটি বা তার বেশি
            val cr = absVal / 10_000_000.0
            val crStr = if (cr % 1.0 == 0.0) String.format(Locale.US, "%.0f", cr) else String.format(Locale.US, "%.2f", cr)
            "$prefix$crStr কোটি"
        }
        absVal >= 100_000 -> { // 1 লাখ বা তার বেশি
            val lk = absVal / 100_000.0
            val lkStr = if (lk % 1.0 == 0.0) String.format(Locale.US, "%.0f", lk) else String.format(Locale.US, "%.2f", lk)
            "$prefix$lkStr লাখ"
        }
        absVal % 1.0 == 0.0 -> "$prefix${String.format(Locale.US, "%,.0f", absVal)}"
        else -> "$prefix${String.format(Locale.US, "%,.2f", absVal)}"
    }
}

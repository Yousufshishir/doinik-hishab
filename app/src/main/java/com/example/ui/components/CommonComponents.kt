package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

fun formatTaka(amount: Double, hide: Boolean = false): String {
    if (hide) return "৳ ••••••"
    return "৳ %,.2f".format(amount)
}

fun getCategoryIcon(iconName: String): ImageVector {
    return when (iconName) {
        "shopping_cart" -> Icons.Default.ShoppingCart
        "home" -> Icons.Default.Home
        "bolt" -> Icons.Default.Bolt
        "directions_bus" -> Icons.Default.DirectionsBus
        "restaurant" -> Icons.Default.Restaurant
        "local_hospital" -> Icons.Default.LocalHospital
        "school" -> Icons.Default.School
        "wifi" -> Icons.Default.Wifi
        "payments" -> Icons.Default.Payments
        "checkroom" -> Icons.Default.Checkroom
        "volunteer_activism" -> Icons.Default.VolunteerActivism
        "favorite" -> Icons.Default.Favorite
        "account_balance_wallet" -> Icons.Default.AccountBalanceWallet
        "store" -> Icons.Default.Store
        "laptop_mac" -> Icons.Default.Computer
        "card_giftcard" -> Icons.Default.CardGiftcard
        "trending_up" -> Icons.Default.TrendingUp
        else -> Icons.Default.AttachMoney
    }
}

fun getPaymentMethodColor(method: String): Color {
    val lower = method.lowercase()
    return when {
        lower.contains("bkash") || lower.contains("বিকাশ") -> BKashPink
        lower.contains("nagad") || lower.contains("নগদ") -> NagadOrange
        lower.contains("rocket") || lower.contains("রকেট") -> RocketPurple
        lower.contains("dbbl") || lower.contains("nexus") -> DBBLNavy
        lower.contains("brac") -> BracBankBlue
        else -> CashGreen
    }
}

@Composable
fun BangladeshCurrencyTag(
    modifier: Modifier = Modifier,
    fontSize: Int = 18,
    color: Color = MaterialTheme.colorScheme.primary
) {
    Text(
        text = "৳",
        style = MaterialTheme.typography.titleLarge.copy(
            fontWeight = FontWeight.Bold,
            fontSize = fontSize.sp,
            color = color
        ),
        modifier = modifier
    )
}

@Composable
fun SecurityBadge(
    isEncrypted: Boolean,
    modifier: Modifier = Modifier
) {
    if (isEncrypted) {
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            shape = RoundedCornerShape(12.dp),
            modifier = modifier
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "AES-256 Encrypted",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = "AES-256",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 10.sp
                    )
                )
            }
        }
    }
}

package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.KhatEntity
import com.example.data.model.formatTakaSafe
import com.example.ui.util.AppLocale
import com.example.ui.util.clearFocusOnTap

data class KhatIconItem(
    val id: String,
    val icon: ImageVector,
    val nameBn: String,
    val nameEn: String
)

val availableKhatIconItems = listOf(
    KhatIconItem("shopping_cart", Icons.Default.ShoppingCart, "বাজার", "Groceries"),
    KhatIconItem("home", Icons.Default.Home, "বাসা", "Rent"),
    KhatIconItem("school", Icons.Default.School, "শিক্ষা", "Education"),
    KhatIconItem("medical_services", Icons.Default.MedicalServices, "চিকিৎসা", "Medical"),
    KhatIconItem("directions_car", Icons.Default.DirectionsCar, "যাতায়াত", "Transport"),
    KhatIconItem("shopping_bag", Icons.Default.ShoppingBag, "পোশাক", "Shopping"),
    KhatIconItem("bolt", Icons.Default.Bolt, "বিদ্যুৎ/বিল", "Bills"),
    KhatIconItem("shield", Icons.Default.Shield, "জরুরি", "Emergency"),
    KhatIconItem("restaurant", Icons.Default.Restaurant, "খাবার", "Dine"),
    KhatIconItem("savings", Icons.Default.Savings, "সঞ্চয়", "Savings"),
    KhatIconItem("smartphone", Icons.Default.Smartphone, "মোবাইল", "Phone"),
    KhatIconItem("flight", Icons.Default.Flight, "ভ্রমণ", "Travel"),
    KhatIconItem("card_giftcard", Icons.Default.CardGiftcard, "উপহার", "Gift"),
    KhatIconItem("fitness_center", Icons.Default.FitnessCenter, "ফিটনেস", "Fitness"),
    KhatIconItem("pets", Icons.Default.Pets, "পোষা প্রাণী", "Pets"),
    KhatIconItem("folder", Icons.Default.Folder, "অন্যান্য", "Other")
)

fun getKhatIcon(iconName: String, khatName: String = ""): ImageVector {
    val matched = availableKhatIconItems.firstOrNull { it.id.equals(iconName, ignoreCase = true) }
    if (matched != null && matched.id != "folder") {
        return matched.icon
    }
    val lower = khatName.lowercase()
    return when {
        lower.contains("বাজার") || lower.contains("খাবার") || lower.contains("grocer") || lower.contains("food") -> Icons.Default.ShoppingCart
        lower.contains("বাড়ি") || lower.contains("বাড়ি") || lower.contains("ভাড়া") || lower.contains("ভাড়া") || lower.contains("rent") || lower.contains("home") || lower.contains("বাসা") -> Icons.Default.Home
        lower.contains("শিক্ষা") || lower.contains("বই") || lower.contains("পড়া") || lower.contains("school") || lower.contains("educat") || lower.contains("টিউশন") -> Icons.Default.School
        lower.contains("ওষুধ") || lower.contains("চিকিৎসা") || lower.contains("হাসপাতাল") || lower.contains("doctor") || lower.contains("medic") -> Icons.Default.MedicalServices
        lower.contains("যাতায়াত") || lower.contains("যাতায়াত") || lower.contains("গাড়ি") || lower.contains("বাস") || lower.contains("car") || lower.contains("transport") -> Icons.Default.DirectionsCar
        lower.contains("শপিং") || lower.contains("পোশাক") || lower.contains("কাপড়") || lower.contains("shop") || lower.contains("cloth") -> Icons.Default.ShoppingBag
        lower.contains("বিল") || lower.contains("বিদ্যুৎ") || lower.contains("কারেন্ট") || lower.contains("গ্যাস") || lower.contains("পানি") || lower.contains("bill") -> Icons.Default.Bolt
        lower.contains("জরুরি") || lower.contains("তহবিল") || lower.contains("emergency") -> Icons.Default.Shield
        lower.contains("রেস্তোরাঁ") || lower.contains("নাস্তা") || lower.contains("হোটেল") || lower.contains("dine") || lower.contains("cafe") -> Icons.Default.Restaurant
        lower.contains("সঞ্চয়") || lower.contains("সঞ্চয়") || lower.contains("ডিপিএস") || lower.contains("saving") -> Icons.Default.Savings
        lower.contains("মোবাইল") || lower.contains("নেট") || lower.contains("রিচার্জ") || lower.contains("phone") -> Icons.Default.Smartphone
        lower.contains("ভ্রমণ") || lower.contains("ট্যুর") || lower.contains("travel") || lower.contains("tour") -> Icons.Default.Flight
        lower.contains("উপহার") || lower.contains("দান") || lower.contains("যাকাত") || lower.contains("gift") -> Icons.Default.CardGiftcard
        lower.contains("জিম") || lower.contains("খেলা") || lower.contains("gym") || lower.contains("fit") -> Icons.Default.FitnessCenter
        lower.contains("বিড়াল") || lower.contains("প্রাণী") || lower.contains("pet") -> Icons.Default.Pets
        else -> matched?.icon ?: Icons.Default.Folder
    }
}

@Composable
fun KhatAllocationContent(
    khats: List<KhatEntity>,
    netBalance: Double,
    totalAllocated: Double,
    oboshistoBalance: Double,
    isEnglish: Boolean,
    isDarkMode: Boolean,
    onCreateKhatClick: () -> Unit,
    onEditKhatClick: (KhatEntity) -> Unit,
    onDeleteKhatClick: (KhatEntity) -> Unit,
    modifier: Modifier = Modifier,
    initiallyExpanded: Boolean = true
) {
    var isExpanded by remember { mutableStateOf(initiallyExpanded) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("khat_allocation_card")
            .animateContentSize(animationSpec = tween(200)),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Explanatory Tagline requested by user
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.primary.copy(alpha = if (isDarkMode) 0.12f else 0.07f),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = AppLocale.t(
                        isEnglish,
                        "আপনার মূল ব্যালেন্স ${formatTakaSafe(netBalance)}, সেটাকে আপনি বিভিন্ন খাতে ভাগ করে রাখতে পারবেন আপনার সুবিধার জন্য।",
                        "Your total balance is ${formatTakaSafe(netBalance)}, which you can allocate across different funds for your convenience."
                    ),
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 11.5.sp,
                        lineHeight = 16.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Header Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .weight(1f)
                    .clickable { isExpanded = !isExpanded }
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF4F46E5).copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PieChart,
                        contentDescription = null,
                        tint = Color(0xFF4F46E5),
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = AppLocale.t(isEnglish, "খাত বণ্টন (অ্যালোকেশন)", "Balance Allocation (Khats)"),
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = AppLocale.t(
                            isEnglish,
                            "বরাদ্দ: ${formatTakaSafe(totalAllocated)} • অবশিষ্ট: ${formatTakaSafe(oboshistoBalance)}",
                            "Allocated: ${formatTakaSafe(totalAllocated)} • Remaining: ${formatTakaSafe(oboshistoBalance)}"
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Add Khat Button
                FilledTonalButton(
                    onClick = onCreateKhatClick,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    modifier = Modifier
                        .height(34.dp)
                        .testTag("btn_create_khat")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = AppLocale.t(isEnglish, "নতুন খাত", "New Khat"),
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }

                IconButton(
                    onClick = { isExpanded = !isExpanded },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = "Expand/Collapse",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        AnimatedVisibility(visible = isExpanded) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Fixed Oboshisto Section (Auto Remaining)
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (oboshistoBalance >= 0) {
                        if (isDarkMode) Color(0xFF064E3B).copy(alpha = 0.45f) else Color(0xFFECFDF5)
                    } else {
                        if (isDarkMode) Color(0xFF7F1D1D).copy(alpha = 0.45f) else Color(0xFFFEF2F2)
                    },
                    border = androidx.compose.foundation.BorderStroke(
                        1.2.dp,
                        if (oboshistoBalance >= 0) {
                            if (isDarkMode) Color(0xFF059669).copy(alpha = 0.5f) else Color(0xFFA7F3D0)
                        } else {
                            if (isDarkMode) Color(0xFFDC2626).copy(alpha = 0.5f) else Color(0xFFFECACA)
                        }
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("khat_oboshisto_card")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (oboshistoBalance >= 0) Color(0xFF10B981).copy(alpha = 0.2f)
                                        else Color(0xFFEF4444).copy(alpha = 0.2f)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (oboshistoBalance >= 0) Icons.Default.Savings else Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = if (oboshistoBalance >= 0) Color(0xFF10B981) else Color(0xFFEF4444),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = AppLocale.t(isEnglish, "অবশিষ্ট ব্যালেন্স", "Oboshisto (Remaining)"),
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = if (oboshistoBalance >= 0) Color(0xFF10B981).copy(alpha = 0.15f) else Color(0xFFEF4444).copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            text = AppLocale.t(isEnglish, "স্বয়ংক্রিয়", "Fixed / Auto"),
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (oboshistoBalance >= 0) Color(0xFF059669) else Color(0xFFDC2626)
                                            ),
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = AppLocale.t(
                                        isEnglish,
                                        "নতুন আয় ও অবরাদ্দকৃত টাকা সরাসরি এখানে থাকে",
                                        "Unallocated funds & incoming deposits automatically stay here"
                                    ),
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Text(
                            text = formatTakaSafe(oboshistoBalance),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = if (oboshistoBalance >= 0) Color(0xFF059669) else Color(0xFFDC2626)
                            )
                        )
                    }
                }

                // List of Custom Khats
                if (khats.isEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp, horizontal = 14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = AppLocale.t(
                                    isEnglish,
                                    "এখনো কোনো নির্দিষ্ট খাত তৈরি করা হয়নি।",
                                    "No custom khats created yet."
                                ),
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = AppLocale.t(
                                    isEnglish,
                                    "আপনার ব্যালেন্স বিভিন্ন প্রয়োজনে (যেমন: ভাড়া, বাজার, ভ্রমণ) ভাগ করতে '+ নতুন খাত' চাপুন।",
                                    "Tap '+ New Khat' to allocate your balance into funds like Rent, Groceries, Travel, etc."
                                ),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                        }
                    }
                } else {
                    khats.forEach { khat ->
                        KhatItemRow(
                            khat = khat,
                            isEnglish = isEnglish,
                            isDarkMode = isDarkMode,
                            onEdit = { onEditKhatClick(khat) },
                            onDelete = { onDeleteKhatClick(khat) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun KhatAllocationSection(
    khats: List<KhatEntity>,
    netBalance: Double,
    totalAllocated: Double,
    oboshistoBalance: Double,
    isEnglish: Boolean,
    isDarkMode: Boolean,
    onCreateKhatClick: () -> Unit,
    onEditKhatClick: (KhatEntity) -> Unit,
    onDeleteKhatClick: (KhatEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("khat_allocation_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDarkMode) Color(0xFF1E1E2E) else Color(0xFFF8FAFC)
        ),
        border = AppCardDefaults.border(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        KhatAllocationContent(
            khats = khats,
            netBalance = netBalance,
            totalAllocated = totalAllocated,
            oboshistoBalance = oboshistoBalance,
            isEnglish = isEnglish,
            isDarkMode = isDarkMode,
            onCreateKhatClick = onCreateKhatClick,
            onEditKhatClick = onEditKhatClick,
            onDeleteKhatClick = onDeleteKhatClick,
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Composable
fun KhatItemRow(
    khat: KhatEntity,
    isEnglish: Boolean,
    isDarkMode: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val khatColor = try {
        Color(android.graphics.Color.parseColor(khat.colorHex))
    } catch (_: Exception) {
        Color(0xFF4F46E5)
    }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("khat_item_${khat.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(khatColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = getKhatIcon(khat.iconName, khat.name),
                        contentDescription = null,
                        tint = khatColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = AppLocale.khatName(khat.name, isEnglish),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = AppLocale.t(isEnglish, "বরাদ্দ ব্যালেন্স", "Allocated Fund"),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = formatTakaSafe(khat.allocatedAmount),
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = khatColor
                    ),
                    modifier = Modifier.padding(end = 6.dp)
                )

                IconButton(
                    onClick = onEdit,
                    modifier = Modifier
                        .size(32.dp)
                        .testTag("btn_edit_khat_${khat.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Khat",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(17.dp)
                    )
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .size(32.dp)
                        .testTag("btn_delete_khat_${khat.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Delete Khat",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(17.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun CreateOrEditKhatDialog(
    khatToEdit: KhatEntity? = null,
    availableOboshisto: Double,
    isEnglish: Boolean,
    onDismiss: () -> Unit,
    onSave: (name: String, amount: Double, colorHex: String, iconName: String) -> Unit
) {
    var nameText by remember { mutableStateOf(khatToEdit?.name ?: "") }
    var amountText by remember {
        mutableStateOf(
            if (khatToEdit != null) {
                if (khatToEdit.allocatedAmount % 1.0 == 0.0) khatToEdit.allocatedAmount.toLong().toString()
                else "%.2f".format(khatToEdit.allocatedAmount)
            } else ""
        )
    }

    val colorOptions = listOf(
        "#4F46E5", // Indigo
        "#059669", // Emerald
        "#D97706", // Amber
        "#E11D48", // Rose
        "#7C3AED", // Violet
        "#0891B2", // Cyan
        "#EA580C", // Orange
        "#2563EB", // Blue
        "#D946EF", // Fuchsia
        "#475569"  // Slate
    )
    var selectedColor by remember { mutableStateOf(khatToEdit?.colorHex ?: colorOptions[0]) }
    var selectedIcon by remember { mutableStateOf(khatToEdit?.iconName ?: "folder") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    data class PresetKhat(
        val bn: String,
        val en: String,
        val emoji: String,
        val defaultColor: String,
        val iconId: String
    )

    val presetList = listOf(
        PresetKhat("খাবার ও বাজার", "Groceries", "🛒", "#059669", "shopping_cart"),
        PresetKhat("বাড়িভাড়া", "House Rent", "🏠", "#4F46E5", "home"),
        PresetKhat("শিক্ষা খরচ", "Education", "🎓", "#7C3AED", "school"),
        PresetKhat("ওষুধ ও চিকিৎসা", "Medical", "💊", "#E11D48", "medical_services"),
        PresetKhat("যাতায়াত ও ভাড়া", "Transport", "🚗", "#D97706", "directions_car"),
        PresetKhat("শপিং ও পোশাক", "Shopping", "🛍️", "#0891B2", "shopping_bag"),
        PresetKhat("গ্যাস ও বিদ্যুৎ বিল", "Bills & Utilities", "⚡", "#EA580C", "bolt"),
        PresetKhat("জরুরি তহবিল", "Emergency Fund", "🛡️", "#2563EB", "shield"),
        PresetKhat("নাস্তা ও বিনোদন", "Snacks & Dine", "☕", "#D946EF", "restaurant"),
        PresetKhat("সঞ্চয় ও বিনিয়োগ", "Savings", "💰", "#10B981", "savings"),
        PresetKhat("মোবাইল ও নেট", "Mobile & Internet", "📱", "#6366F1", "smartphone"),
        PresetKhat("অন্যান্য খরচ", "Other Expenses", "🏷️", "#475569", "folder")
    )

    // Calculate maximum allowed allocation
    val maxAllowed = if (khatToEdit != null) {
        maxOf(0.0, availableOboshisto + khatToEdit.allocatedAmount)
    } else {
        maxOf(0.0, availableOboshisto)
    }

    val activeColor = try {
        Color(android.graphics.Color.parseColor(selectedColor))
    } catch (_: Exception) {
        MaterialTheme.colorScheme.primary
    }

    val density = LocalDensity.current
    val imeInsets = WindowInsets.ime
    val isKeyboardOpen = imeInsets.getBottom(density) > 0

    Dialog(
        onDismissRequest = onDismiss,
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
                .padding(horizontal = 16.dp, vertical = if (isKeyboardOpen) 6.dp else 24.dp)
                .clearFocusOnTap(),
            contentAlignment = if (isKeyboardOpen) Alignment.TopCenter else Alignment.Center
        ) {
            val scrollState = rememberScrollState()
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 440.dp)
            ) {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    border = AppCardDefaults.border(activeColor),
                    elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateContentSize()
                        .testTag("dialog_create_edit_khat")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(scrollState)
                            .padding(horizontal = 20.dp, vertical = if (isKeyboardOpen) 14.dp else 20.dp),
                        verticalArrangement = Arrangement.spacedBy(if (isKeyboardOpen) 10.dp else 14.dp)
                    ) {
                    // Header with Badge and Close button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = activeColor.copy(alpha = 0.16f),
                                modifier = Modifier.size(44.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                    Icon(
                                        imageVector = getKhatIcon(selectedIcon, nameText),
                                        contentDescription = null,
                                        tint = activeColor,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                            Column {
                                Text(
                                    text = if (khatToEdit == null) {
                                        AppLocale.t(isEnglish, "নতুন বাজেট খাত", "Create New Khat")
                                    } else {
                                        AppLocale.t(isEnglish, "খাত সম্পাদনা করুন", "Edit Khat")
                                    },
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 17.sp
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = AppLocale.t(
                                        isEnglish,
                                        "নির্দিষ্ট ব্যয়ের জন্য আলাদা খামে টাকা রাখুন",
                                        "Set aside money for planned categories"
                                    ),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))

                    // Available Oboshisto Balance & Quick Allocation Chips
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.AccountBalanceWallet,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = AppLocale.t(isEnglish, "বরাদ্দযোগ্য অবশিষ্ট:", "Available Balance:"),
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    text = formatTakaSafe(maxAllowed),
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (maxAllowed > 0) Color(0xFF059669) else MaterialTheme.colorScheme.error
                                    )
                                )
                            }

                            if (maxAllowed > 0) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    val percentages = listOf(
                                        Pair("২৫%", 0.25),
                                        Pair("৫০%", 0.50),
                                        Pair("৭৫%", 0.75),
                                        Pair("১০০%", 1.00)
                                    )
                                    percentages.forEach { (label, frac) ->
                                        val calc = (maxAllowed * frac).toLong()
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                                            border = androidx.compose.foundation.BorderStroke(
                                                0.8.dp,
                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                                            ),
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(8.dp))
                                                .clickable {
                                                    amountText = calc.toString()
                                                    errorMessage = null
                                                }
                                        ) {
                                            Text(
                                                text = label,
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontWeight = FontWeight.SemiBold,
                                                    fontSize = 11.sp
                                                ),
                                                color = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.padding(vertical = 5.dp),
                                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                            )
                                        }
                                    }
                                }
                            } else {
                                Text(
                                    text = AppLocale.t(
                                        isEnglish,
                                        "💡 অবশিষ্ট ব্যালেন্স ০ হলেও আপনি ৳০ বরাদ্দ দিয়ে এখনই খাতটি তৈরি করে রাখতে পারেন।",
                                        "💡 Balance is ৳0, but you can create this Khat with ৳0 and add funds anytime."
                                    ),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }
                        }
                    }

                    // Preset Suggestions Chips
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = AppLocale.t(isEnglish, "প্রস্তাবিত খাতের তালিকা (ট্যাপ করুন):", "Quick Categories (Tap to choose):"),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            presetList.forEach { preset ->
                                val label = if (isEnglish) preset.en else preset.bn
                                val isSel = nameText.trim() == label.trim()
                                val chipColor = try {
                                    Color(android.graphics.Color.parseColor(preset.defaultColor))
                                } catch (_: Exception) {
                                    MaterialTheme.colorScheme.primary
                                }

                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isSel) chipColor else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (isSel) chipColor else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                                    ),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable {
                                            nameText = label
                                            selectedColor = preset.defaultColor
                                            selectedIcon = preset.iconId
                                            errorMessage = null
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(text = preset.emoji, fontSize = 12.sp)
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                                color = if (isSel) Color.White else MaterialTheme.colorScheme.onSurface
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Khat Name Input
                    OutlinedTextField(
                        value = nameText,
                        onValueChange = {
                            nameText = it
                            errorMessage = null
                        },
                        label = { Text(AppLocale.t(isEnglish, "খাতের নাম", "Khat Name")) },
                        placeholder = { Text(AppLocale.t(isEnglish, "যেমন: খাবার ও বাজার, বাড়িভাড়া...", "e.g. Groceries, Rent...")) },
                        leadingIcon = {
                            Icon(
                                imageVector = getKhatIcon(selectedIcon, nameText),
                                contentDescription = null,
                                tint = activeColor,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        shape = RoundedCornerShape(14.dp),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_khat_name")
                    )

                    // Amount Input
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { input ->
                            val filtered = input.filter { it.isDigit() || it == '.' }
                            if (filtered.count { it == '.' } <= 1) {
                                amountText = filtered
                                errorMessage = null
                            }
                        },
                        label = { Text(AppLocale.t(isEnglish, "বরাদ্দকৃত টাকা (ঐচ্ছিক)", "Allocated Amount (Optional)")) },
                        placeholder = { Text("0") },
                        leadingIcon = {
                            Text(
                                text = "৳",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = activeColor,
                                modifier = Modifier.padding(start = 12.dp)
                            )
                        },
                        trailingIcon = {
                            if (amountText.isNotBlank()) {
                                IconButton(onClick = { amountText = "" }) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Clear",
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        },
                        supportingText = {
                            Text(
                                text = AppLocale.t(
                                    isEnglish,
                                    "চাইলে ০ দিয়েও খুলতে পারেন, পরে বরাদ্দ দেওয়া যাবে",
                                    "Can be 0; you can fund it later"
                                ),
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp)
                            )
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        shape = RoundedCornerShape(14.dp),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_khat_amount")
                    )

                    // Icon Selection Row
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = AppLocale.t(isEnglish, "খাতের আইকন নির্বাচন করুন:", "Category Icon:"),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                            val selectedItem = availableKhatIconItems.firstOrNull { it.id.equals(selectedIcon, ignoreCase = true) }
                            if (selectedItem != null) {
                                Text(
                                    text = if (isEnglish) selectedItem.nameEn else selectedItem.nameBn,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = activeColor
                                    )
                                )
                            }
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            availableKhatIconItems.forEach { iconItem ->
                                val isSelected = selectedIcon.equals(iconItem.id, ignoreCase = true)
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isSelected) activeColor.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                    border = androidx.compose.foundation.BorderStroke(
                                        width = if (isSelected) 1.8.dp else 1.dp,
                                        color = if (isSelected) activeColor else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                                    ),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable {
                                            selectedIcon = iconItem.id
                                        }
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                                    ) {
                                        Icon(
                                            imageVector = iconItem.icon,
                                            contentDescription = if (isEnglish) iconItem.nameEn else iconItem.nameBn,
                                            tint = if (isSelected) activeColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(22.dp)
                                        )
                                        Spacer(modifier = Modifier.height(3.dp))
                                        Text(
                                            text = if (isEnglish) iconItem.nameEn else iconItem.nameBn,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontSize = 10.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                            ),
                                            color = if (isSelected) activeColor else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Color Selection Row
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = AppLocale.t(isEnglish, "খাতের থিম কালার:", "Category Color Theme:"),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            colorOptions.forEach { hex ->
                                val c = try {
                                    Color(android.graphics.Color.parseColor(hex))
                                } catch (_: Exception) {
                                    Color.Blue
                                }
                                val isSelected = selectedColor.equals(hex, ignoreCase = true)
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(c)
                                        .border(
                                            width = if (isSelected) 3.dp else 1.dp,
                                            color = if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                            shape = CircleShape
                                        )
                                        .clickable { selectedColor = hex },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Error Message Notice
                    if (errorMessage != null) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.errorContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ErrorOutline,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = errorMessage ?: "",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                        fontWeight = FontWeight.Medium
                                    ),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    // Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                        ) {
                            Text(
                                text = AppLocale.t(isEnglish, "বাতিল", "Cancel"),
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Button(
                            onClick = {
                                val cleanName = nameText.trim()
                                val amount = amountText.toDoubleOrNull() ?: 0.0
                                if (cleanName.isEmpty()) {
                                    errorMessage = AppLocale.t(isEnglish, "অনুগ্রহ করে খাতের একটি নাম লিখুন", "Please enter a khat name")
                                    return@Button
                                }
                                if (amount < 0) {
                                    errorMessage = AppLocale.t(isEnglish, "বরাদ্দ ঋণাত্মক হতে পারবে না", "Amount cannot be negative")
                                    return@Button
                                }
                                if (amount > 0 && amount > maxAllowed) {
                                    errorMessage = AppLocale.t(
                                        isEnglish,
                                        "বরাদ্দকৃত টাকা (${formatTakaSafe(amount)}) অবশিষ্টের (${formatTakaSafe(maxAllowed)}) চেয়ে বেশি। আপনি চাইলে প্রথমে ০ টাকা দিয়েও খাতটি তৈরি করে রাখতে পারেন।",
                                        "Allocated amount (${formatTakaSafe(amount)}) exceeds available Oboshisto (${formatTakaSafe(maxAllowed)}). You can create with ৳0."
                                    )
                                    return@Button
                                }
                                onSave(cleanName, amount, selectedColor, selectedIcon)
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = activeColor
                            ),
                            modifier = Modifier
                                .weight(1.3f)
                                .height(46.dp)
                                .testTag("btn_save_khat")
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (khatToEdit == null) {
                                    AppLocale.t(isEnglish, "খাত সংরক্ষণ", "Save Khat")
                                } else {
                                    AppLocale.t(isEnglish, "আপডেট করুন", "Update Khat")
                                },
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
            KeyboardScrollDownHint(
                scrollState = scrollState,
                isKeyboardOpen = isKeyboardOpen,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}
}

@Composable
fun DeleteKhatConfirmationDialog(
    khat: KhatEntity,
    isEnglish: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = AppLocale.t(isEnglish, "খাত মুছে ফেলবেন?", "Delete Khat?"),
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = AppLocale.t(
                        isEnglish,
                        "আপনি কি '${khat.name}' খাতটি মুছে ফেলতে চান?",
                        "Are you sure you want to delete '${AppLocale.khatName(khat.name, true)}'?"
                    )
                )
                Text(
                    text = AppLocale.t(
                        isEnglish,
                        "এর বর্তমান ব্যালেন্স (${formatTakaSafe(khat.allocatedAmount)}) স্বয়ংক্রিয়ভাবে মূল 'অবশিষ্ট' ব্যালেন্সে যোগ হবে।",
                        "Its current allocated balance (${formatTakaSafe(khat.allocatedAmount)}) will automatically return to Oboshisto."
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("btn_confirm_delete_khat")
            ) {
                Text(AppLocale.t(isEnglish, "মুছে ফেলুন", "Delete"), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(AppLocale.t(isEnglish, "বাতিল", "Cancel"))
            }
        }
    )
}

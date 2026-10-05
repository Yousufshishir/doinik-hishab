package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Event model for global premium toasts
 */
data class ToastEvent(
    val message: String,
    val actionLabel: String? = null,
    val onAction: (() -> Unit)? = null,
    val id: Long = System.currentTimeMillis()
)

/**
 * Global singleton Toast Dispatcher to replace Android OS toasts with sleek, tap-to-dismiss Compose HUD toasts.
 */
object AppToastManager {
    private val _toastEvents = MutableSharedFlow<ToastEvent>(extraBufferCapacity = 8)
    val toastEvents = _toastEvents.asSharedFlow()

    fun show(message: String, actionLabel: String? = null, onAction: (() -> Unit)? = null) {
        if (message.isBlank()) return
        _toastEvents.tryEmit(ToastEvent(message, actionLabel, onAction))
    }
}

/**
 * Floating Ultra-Premium Toast Capsule with gradient border, animated entry,
 * progress bar timer, high-contrast dark glass styling, and tap-to-dismiss behavior.
 */
@Composable
fun PremiumToastItem(
    message: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val icon = determineToastIcon(message)
    val iconColor = determineIconColor(message)
    val tagLabel = determineTagLabel(message)

    // Smooth progress indicator showing time remaining (standard 3600ms)
    val animatedProgress = remember { Animatable(1f) }
    LaunchedEffect(message) {
        animatedProgress.snapTo(1f)
        animatedProgress.animateTo(
            targetValue = 0f,
            animationSpec = tween(durationMillis = 3600, easing = LinearEasing)
        )
    }

    val gradientBorder = Brush.horizontalGradient(
        colors = listOf(
            iconColor.copy(alpha = 0.85f),
            Color(0xFF38BDF8).copy(alpha = 0.65f),
            Color(0xFF818CF8).copy(alpha = 0.75f)
        )
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .widthIn(max = 450.dp)
            .padding(horizontal = 14.dp)
            .shadow(
                elevation = 18.dp,
                shape = RoundedCornerShape(22.dp),
                spotColor = Color.Black.copy(alpha = 0.55f),
                ambientColor = iconColor.copy(alpha = 0.25f)
            )
            .clip(RoundedCornerShape(22.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                // Tapping directly on the toast dismisses it immediately
                onDismiss()
            }
            .testTag("premium_toast_item"),
        shape = RoundedCornerShape(22.dp),
        color = Color(0xFF0F172A).copy(alpha = 0.96f),
        border = BorderStroke(1.4.dp, gradientBorder),
        tonalElevation = 10.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    // Leading Icon with glowing aura
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(iconColor.copy(alpha = 0.20f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = iconColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(11.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = iconColor.copy(alpha = 0.22f)
                            ) {
                                Text(
                                    text = tagLabel,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = iconColor,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 8.5.sp,
                                        letterSpacing = 0.5.sp
                                    ),
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = message,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.5.sp,
                                lineHeight = 18.sp
                            ),
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                if (actionLabel != null && onAction != null) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            onAction()
                            onDismiss()
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF38BDF8),
                            contentColor = Color(0xFF0F172A)
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 5.dp),
                        modifier = Modifier
                            .height(32.dp)
                            .testTag("toast_action_button")
                    ) {
                        Text(
                            text = actionLabel,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 12.sp
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.width(6.dp))
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(26.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Dismiss",
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Micro duration progress line
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.5.dp)
                    .background(Color(0xFF1E293B))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(animatedProgress.value)
                        .background(
                            Brush.horizontalGradient(
                                listOf(iconColor, Color(0xFF38BDF8))
                            )
                        )
                )
            }
        }
    }
}

private fun determineTagLabel(message: String): String {
    val lower = message.lowercase()
    return when {
        lower.contains("মুছে") || lower.contains("delete") || lower.contains("বাতিল") -> "DELETED"
        lower.contains("সফল") || lower.contains("success") || lower.contains("যোগ") || lower.contains("added") -> "SUCCESS"
        lower.contains("সিঙ্ক") || lower.contains("sync") -> "CLOUD SYNC"
        lower.contains("ত্রুটি") || lower.contains("ভুল") || lower.contains("error") || lower.contains("failed") -> "ALERT"
        lower.contains("লগইন") || lower.contains("login") || lower.contains("signed in") -> "AUTH"
        else -> "NOTICE"
    }
}

private fun determineToastIcon(message: String): ImageVector {
    val lower = message.lowercase()
    return when {
        lower.contains("মুছে") || lower.contains("delete") || lower.contains("বাতিল") -> Icons.Default.DeleteOutline
        lower.contains("স্বাগতম") || lower.contains("welcome") || lower.contains("সফল") || lower.contains("success") || lower.contains("যোগ") || lower.contains("added") -> Icons.Default.CheckCircle
        lower.contains("সিঙ্ক") || lower.contains("sync") -> Icons.Default.CloudDone
        lower.contains("about") || lower.contains("নির্দেশিকা") || lower.contains("দেখুন") -> Icons.Default.Info
        lower.contains("ত্রুটি") || lower.contains("ভুল") || lower.contains("error") || lower.contains("failed") -> Icons.Default.ErrorOutline
        lower.contains("লগইন") || lower.contains("login") -> Icons.Default.Lock
        else -> Icons.Default.Notifications
    }
}

private fun determineIconColor(message: String): Color {
    val lower = message.lowercase()
    return when {
        lower.contains("মুছে") || lower.contains("delete") || lower.contains("error") || lower.contains("failed") -> Color(0xFFEF4444)
        lower.contains("স্বাগতম") || lower.contains("welcome") || lower.contains("সফল") || lower.contains("success") || lower.contains("যোগ") || lower.contains("added") -> Color(0xFF10B981)
        lower.contains("সিঙ্ক") || lower.contains("sync") -> Color(0xFF06B6D4)
        lower.contains("about") || lower.contains("নির্দেশিকা") || lower.contains("দেখুন") -> Color(0xFF818CF8)
        else -> Color(0xFF38BDF8)
    }
}

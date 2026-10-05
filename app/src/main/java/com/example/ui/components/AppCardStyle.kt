package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

/**
 * Standardized global premium card borders and keyboard UX indicators
 * ensuring every card across the app has a distinct, refined, luxury border.
 */
object AppCardDefaults {
    val DefaultShape = RoundedCornerShape(20.dp)
    val CompactShape = RoundedCornerShape(14.dp)
    val LargeShape = RoundedCornerShape(24.dp)

    /**
     * Distinct premium border stroke for general functional cards.
     * Uses an elegant emerald-slate or cyan-accented hairline border in light mode,
     * and a soft luminous emerald glow in dark mode.
     */
    @Composable
    fun border(
        accentColor: Color? = null,
        isDark: Boolean = isSystemInDarkTheme()
    ): BorderStroke {
        val strokeColor = if (accentColor != null) {
            accentColor.copy(alpha = if (isDark) 0.45f else 0.35f)
        } else if (isDark) {
            Color(0xFF2DD4BF).copy(alpha = 0.28f) // Subtle luminous teal/emerald glow
        } else {
            Color(0xFF0F766E).copy(alpha = 0.22f) // Refined deep teal border
        }
        return BorderStroke(1.2.dp, strokeColor)
    }

    /**
     * Green/Emerald border for Income and Savings cards
     */
    @Composable
    fun incomeBorder(isDark: Boolean = isSystemInDarkTheme()): BorderStroke {
        val color = if (isDark) Color(0xFF34D399).copy(alpha = 0.40f) else Color(0xFF10B981).copy(alpha = 0.35f)
        return BorderStroke(1.2.dp, color)
    }

    /**
     * Crimson/Rose border for Expense and Debts (dene) cards
     */
    @Composable
    fun expenseBorder(isDark: Boolean = isSystemInDarkTheme()): BorderStroke {
        val color = if (isDark) Color(0xFFFB7185).copy(alpha = 0.40f) else Color(0xFFE11D48).copy(alpha = 0.30f)
        return BorderStroke(1.2.dp, color)
    }

    /**
     * Amber/Gold border for Debts (paona), Khat allocation and budget cards
     */
    @Composable
    fun warningBorder(isDark: Boolean = isSystemInDarkTheme()): BorderStroke {
        val color = if (isDark) Color(0xFFFBBF24).copy(alpha = 0.42f) else Color(0xFFD97706).copy(alpha = 0.32f)
        return BorderStroke(1.2.dp, color)
    }

    /**
     * Indigo/Purple border for Auth and Security cards
     */
    @Composable
    fun authBorder(isDark: Boolean = isSystemInDarkTheme()): BorderStroke {
        val color = if (isDark) Color(0xFF818CF8).copy(alpha = 0.48f) else Color(0xFF4F46E5).copy(alpha = 0.32f)
        return BorderStroke(1.4.dp, color)
    }

    /**
     * Sky/Cyan border for modal dialogs
     */
    @Composable
    fun dialogBorder(isDark: Boolean = isSystemInDarkTheme()): BorderStroke {
        val color = if (isDark) Color(0xFF38BDF8).copy(alpha = 0.42f) else Color(0xFF0284C7).copy(alpha = 0.32f)
        return BorderStroke(1.2.dp, color)
    }
}

/**
 * An unobtrusive, miniature, gently bouncing down-arrow indicator that automatically appears
 * at the bottom of a card when the on-screen keyboard is open and the card is squeezed,
 * guiding the user that there is more content below.
 */
@Composable
fun KeyboardScrollDownHint(
    scrollState: ScrollState,
    isKeyboardOpen: Boolean = androidx.compose.foundation.layout.WindowInsets.ime.getBottom(androidx.compose.ui.platform.LocalDensity.current) > 0,
    forceShowWhenScrollable: Boolean = false,
    modifier: Modifier = Modifier,
    label: String? = null
) {
    val coroutineScope = rememberCoroutineScope()
    val infiniteTransition = rememberInfiniteTransition(label = "scroll_bounce")
    val bounceOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(
            animation = tween(650, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bounce_offset"
    )

    AnimatedVisibility(
        visible = (forceShowWhenScrollable || isKeyboardOpen) && scrollState.canScrollForward,
        enter = fadeIn(tween(220)) + slideInVertically(tween(220)) { it / 2 },
        exit = fadeOut(tween(180)) + slideOutVertically(tween(180)) { it / 2 },
        modifier = modifier
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.94f),
            shadowElevation = 6.dp,
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.35f)),
            modifier = Modifier
                .offset(y = bounceOffset.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    coroutineScope.launch {
                        scrollState.animateScrollTo(scrollState.value + 240)
                    }
                }
                .testTag("hint_keyboard_scroll_down")
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = "Scroll down indicator",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(15.dp)
                )
                Text(
                    text = label ?: "স্ক্রল করুন",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 9.5.sp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                )
            }
        }
    }
}

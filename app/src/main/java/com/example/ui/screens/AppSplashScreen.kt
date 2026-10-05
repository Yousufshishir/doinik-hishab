package com.example.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * AppSplashScreen:
 * Premium, fluid launch animation screen displaying the app logo, dynamic breathing glow,
 * Bengali & English typography, and animated progress synchronized strictly with the app's
 * background loading and initialization time.
 */
@Composable
fun AppSplashScreen(
    isAppLoading: Boolean,
    isDarkMode: Boolean,
    isEnglish: Boolean,
    onAnimationFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Animation triggers
    val logoScale = remember { Animatable(0.45f) }
    val logoRotation = remember { Animatable(-8f) }
    val contentAlpha = remember { Animatable(0f) }
    val textOffsetY = remember { Animatable(20f) }
    val progressAnim = remember { Animatable(0.15f) }

    // Ambient floating glow effect
    val infiniteTransition = rememberInfiniteTransition(label = "splash_ambient")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.10f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.55f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_alpha"
    )

    // Initial entrance animations (snappy, smooth entrance in ~250ms)
    LaunchedEffect(Unit) {
        launch {
            logoScale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
        }
        launch {
            logoRotation.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
            )
        }
        launch {
            contentAlpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 260, easing = LinearOutSlowInEasing)
            )
        }
        launch {
            textOffsetY.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing)
            )
        }
    }

    // Synchronize progress bar strictly with isAppLoading state
    LaunchedEffect(isAppLoading) {
        if (isAppLoading) {
            // While loading, smoothly progress to ~85%
            progressAnim.animateTo(
                targetValue = 0.85f,
                animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing)
            )
        } else {
            // When app loading finishes, rapidly complete to 100% and transition out immediately
            progressAnim.animateTo(
                targetValue = 1.0f,
                animationSpec = tween(durationMillis = 150, easing = FastOutLinearInEasing)
            )
            // Immediately finish without artificial delay
            onAnimationFinished()
        }
    }

    // Brand color palette
    val bgGradient = if (isDarkMode) {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFF091410),
                Color(0xFF041812),
                Color(0xFF020E0A)
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFFF2FBF7),
                Color(0xFFE6F7F0),
                Color(0xFFD8F3E5)
            )
        )
    }

    val primaryGreen = Color(0xFF059669)
    val accentEmerald = Color(0xFF10B981)
    val lightMint = Color(0xFF6EE7B7)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(bgGradient)
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("app_splash_screen"),
        contentAlignment = Alignment.Center
    ) {
        // Background Ambient Glow Aura
        Box(
            modifier = Modifier
                .size(280.dp)
                .scale(pulseScale)
                .alpha(glowAlpha)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            accentEmerald.copy(alpha = if (isDarkMode) 0.35f else 0.28f),
                            primaryGreen.copy(alpha = if (isDarkMode) 0.15f else 0.12f),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
        )

        // Center Content Column
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
        ) {
            // Animated App Logo Container
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .scale(logoScale.value)
                    .rotate(logoRotation.value)
            ) {
                // Outer glowing gradient ring
                Box(
                    modifier = Modifier
                        .size(118.dp)
                        .scale(pulseScale * 0.98f)
                        .background(
                            brush = Brush.sweepGradient(
                                listOf(
                                    accentEmerald,
                                    Color(0xFF34D399),
                                    Color(0xFF06B6D4),
                                    accentEmerald
                                )
                            ),
                            shape = RoundedCornerShape(32.dp)
                        )
                )

                // Logo Card
                Surface(
                    shape = RoundedCornerShape(28.dp),
                    color = if (isDarkMode) Color(0xFF064E3B) else Color.White,
                    shadowElevation = 16.dp,
                    modifier = Modifier
                        .size(110.dp)
                        .border(
                            width = 2.dp,
                            brush = Brush.linearGradient(
                                listOf(
                                    Color.White.copy(alpha = 0.8f),
                                    accentEmerald.copy(alpha = 0.6f)
                                )
                            ),
                            shape = RoundedCornerShape(28.dp)
                        )
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.hishab_khata_logo_1789834133185),
                            contentDescription = "App Logo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(4.dp)
                                .clip(RoundedCornerShape(24.dp))
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // App Name & Tagline (Animated Slide-up & Fade-in)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .alpha(contentAlpha.value)
                    .offset(y = textOffsetY.value.dp)
            ) {
                // Main App Name
                Text(
                    text = if (isEnglish) "Daily Ledger" else "দৈনিক হিসাব",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.Black,
                        fontSize = 32.sp,
                        letterSpacing = (-0.5).sp
                    ),
                    color = if (isDarkMode) Color(0xFFF0FDF4) else Color(0xFF064E3B),
                    textAlign = TextAlign.Center
                )

                // Subtitle / Tagline
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isDarkMode) Color(0xFF064E3B).copy(alpha = 0.45f) else Color(0xFF059669).copy(alpha = 0.10f),
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    Text(
                        text = if (isEnglish) "Smart & Secure Personal Expense Tracker" else "সহজ ও নির্ভরযোগ্য ব্যক্তিগত হিসাবের খাতা",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        ),
                        color = if (isDarkMode) lightMint else primaryGreen,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(36.dp))

            // Sleek Animated Loading Bar & Status (Synchronized with isAppLoading)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.alpha(contentAlpha.value)
            ) {
                // Progress Track & Fill
                Box(
                    modifier = Modifier
                        .width(140.dp)
                        .height(5.dp)
                        .clip(CircleShape)
                        .background(if (isDarkMode) Color(0xFF1E293B) else Color(0xFFCBD5E1))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(fraction = progressAnim.value.coerceIn(0f, 1f))
                            .background(
                                brush = Brush.horizontalGradient(
                                    listOf(
                                        primaryGreen,
                                        accentEmerald,
                                        Color(0xFF34D399)
                                    )
                                )
                            )
                    )
                }

                Text(
                    text = if (!isAppLoading) {
                        if (isEnglish) "Ready!" else "প্রস্তুত!"
                    } else {
                        if (isEnglish) "Opening your ledger..." else "হিসাবের খাতা লোড হচ্ছে..."
                    },
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    color = if (isDarkMode) Color(0xFF94A3B8) else Color(0xFF64748B)
                )
            }
        }

        // Bottom Trust & Security Badge
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp)
                .alpha(contentAlpha.value)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (isDarkMode) Color.Black.copy(alpha = 0.25f)
                        else Color.White.copy(alpha = 0.6f)
                    )
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = accentEmerald,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isEnglish) "100% Secure • Offline & Cloud Sync" else "১০০% নিরাপদ • অফলাইন ও ক্লাউড সিঙ্ক",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Medium,
                        fontSize = 10.5.sp
                    ),
                    color = if (isDarkMode) Color(0xFFCBD5E1) else Color(0xFF475569)
                )
            }
        }
    }
}

package com.example.ui.screens

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.example.util.AppLockManager
import com.example.util.BiometricHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun AppLockScreen(
    appLockManager: AppLockManager,
    isEnglish: Boolean,
    onUnlocked: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var enteredPin by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    val isBiometricEnabled by appLockManager.isBiometricEnabled.collectAsState()
    val canBiometric = remember { appLockManager.isBiometricHardwareAvailable() }

    val triggerBiometric = {
        if (canBiometric && isBiometricEnabled && context is FragmentActivity) {
            BiometricHelper.showPrompt(
                activity = context,
                title = if (isEnglish) "Unlock App" else "অ্যাপ আনলক করুন",
                subtitle = if (isEnglish) "Confirm your fingerprint or biometric" else "আপনার আঙুলের ছাপ দিয়ে আনলক করুন",
                negativeButtonText = if (isEnglish) "Use PIN" else "পিন ব্যবহার করুন",
                onSuccess = {
                    onUnlocked()
                },
                onError = {
                    // Biometric cancelled or failed, allow PIN entry
                }
            )
        }
    }

    LaunchedEffect(Unit) {
        if (canBiometric && isBiometricEnabled) {
            delay(200)
            triggerBiometric()
        }
    }

    fun vibrateError() {
        try {
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            if (vibrator != null && vibrator.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(150, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(150)
                }
            }
        } catch (_: Exception) {}
    }

    fun onNumberClick(num: String) {
        if (enteredPin.length < 4) {
            val newPin = enteredPin + num
            enteredPin = newPin
            isError = false
            errorMessage = ""
            if (newPin.length == 4) {
                coroutineScope.launch {
                    delay(100)
                    if (appLockManager.verifyPin(newPin)) {
                        onUnlocked()
                    } else {
                        vibrateError()
                        isError = true
                        errorMessage = if (isEnglish) "Incorrect PIN, please try again" else "ভুল পিন! আবার চেষ্টা করুন"
                        delay(500)
                        enteredPin = ""
                    }
                }
            }
        }
    }

    fun onBackspaceClick() {
        if (enteredPin.isNotEmpty()) {
            enteredPin = enteredPin.dropLast(1)
            isError = false
            errorMessage = ""
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .testTag("app_lock_screen"),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header Section
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 24.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Lock",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text = if (isEnglish) "Daily Expense Tracker" else "দৈনিক হিসাব",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp
                    ),
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = if (isEnglish) "Enter 4-digit security PIN" else "৪-সংখ্যার সিকিউরিটি পিন দিন",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(28.dp))

                // PIN dots indicator
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (i in 0 until 4) {
                        val isFilled = i < enteredPin.length
                        val dotColor = if (isError) {
                            MaterialTheme.colorScheme.error
                        } else if (isFilled) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.outlineVariant
                        }

                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .clip(CircleShape)
                                .background(dotColor)
                                .border(
                                    width = 1.5.dp,
                                    color = if (isFilled) dotColor else MaterialTheme.colorScheme.outline,
                                    shape = CircleShape
                                )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                AnimatedVisibility(
                    visible = isError,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Text(
                        text = errorMessage,
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            // Keypad Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                val row1 = listOf("1", "2", "3")
                val row2 = listOf("4", "5", "6")
                val row3 = listOf("7", "8", "9")

                KeypadRow(numbers = row1, onClick = ::onNumberClick)
                KeypadRow(numbers = row2, onClick = ::onNumberClick)
                KeypadRow(numbers = row3, onClick = ::onNumberClick)

                // Bottom row: Biometric, 0, Backspace
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left button: Biometric or empty
                    if (canBiometric && isBiometricEnabled) {
                        Box(
                            modifier = Modifier
                                .size(68.dp)
                                .clip(CircleShape)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = ripple(bounded = true, radius = 34.dp)
                                ) {
                                    triggerBiometric()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Fingerprint,
                                contentDescription = "Biometric",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(34.dp)
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.size(68.dp))
                    }

                    // Center button: 0
                    KeypadButton(text = "0", onClick = { onNumberClick("0") })

                    // Right button: Backspace
                    Box(
                        modifier = Modifier
                            .size(68.dp)
                            .clip(CircleShape)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = ripple(bounded = true, radius = 34.dp)
                            ) {
                                onBackspaceClick()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Backspace,
                            contentDescription = "Backspace",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun KeypadRow(
    numbers: List<String>,
    onClick: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (num in numbers) {
            KeypadButton(text = num, onClick = { onClick(num) })
        }
    }
}

@Composable
private fun KeypadButton(
    text: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .size(68.dp)
            .clip(CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true, radius = 34.dp)
            ) { onClick() },
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
    ) {
        Box(
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Medium,
                    fontSize = 24.sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

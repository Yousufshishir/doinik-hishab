package com.example.ui.screens

import com.example.ui.components.AppCardDefaults
import com.example.ui.components.AppToastManager
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.util.AppLockManager

@Composable
fun AppLockSetupDialog(
    appLockManager: AppLockManager,
    isEnglish: Boolean,
    onDismiss: () -> Unit,
    onSuccess: () -> Unit
) {
    val context = LocalContext.current
    var step by remember { mutableStateOf(1) } // 1: Enter PIN, 2: Confirm PIN
    var firstPin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }

    val currentPin = if (step == 1) firstPin else confirmPin

    fun onNumberClick(num: String) {
        if (currentPin.length < 4) {
            val next = currentPin + num
            errorMessage = ""
            if (step == 1) {
                firstPin = next
                if (next.length == 4) {
                    step = 2
                }
            } else {
                confirmPin = next
                if (next.length == 4) {
                    if (next == firstPin) {
                        appLockManager.setPin(next)
                        AppToastManager.show(
                            if (isEnglish) "App Lock enabled successfully!" else "অ্যাপ লক সফলভাবে চালু হয়েছে!"
                        )
                        onSuccess()
                    } else {
                        errorMessage = if (isEnglish) "PINs do not match. Try again." else "পিন মেলেনি! পুনরায় চেষ্টা করুন।"
                        confirmPin = ""
                    }
                }
            }
        }
    }

    fun onBackspace() {
        if (step == 1) {
            if (firstPin.isNotEmpty()) firstPin = firstPin.dropLast(1)
        } else {
            if (confirmPin.isNotEmpty()) {
                confirmPin = confirmPin.dropLast(1)
            } else {
                step = 1
                firstPin = ""
            }
        }
        errorMessage = ""
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = AppCardDefaults.dialogBorder(),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
                .testTag("app_lock_setup_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isEnglish) "Set 4-Digit Security PIN" else "৪-সংখ্যার সিকিউরিটি পিন সেট করুন",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = if (step == 1) {
                        if (isEnglish) "Step 1: Enter your new 4-digit PIN" else "ধাপ ১: আপনার নতুন ৪-সংখ্যার পিন দিন"
                    } else {
                        if (isEnglish) "Step 2: Confirm your 4-digit PIN" else "ধাপ ২: পিনটি নিশ্চিত করতে পুনরায় দিন"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(18.dp))

                // PIN indicator dots
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    for (i in 0 until 4) {
                        val filled = i < currentPin.length
                        val dotColor = if (errorMessage.isNotEmpty()) MaterialTheme.colorScheme.error else if (filled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .clip(CircleShape)
                                .background(dotColor)
                                .border(1.dp, if (filled) dotColor else MaterialTheme.colorScheme.outline, CircleShape)
                        )
                    }
                }

                if (errorMessage.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Keypad
                SimplePinKeypad(
                    onNumber = ::onNumberClick,
                    onBackspace = ::onBackspace
                )
            }
        }
    }
}

@Composable
fun AppLockVerifyDialog(
    appLockManager: AppLockManager,
    isEnglish: Boolean,
    title: String,
    onDismiss: () -> Unit,
    onVerified: () -> Unit
) {
    var enteredPin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }

    fun onNumberClick(num: String) {
        if (enteredPin.length < 4) {
            val next = enteredPin + num
            enteredPin = next
            errorMessage = ""
            if (next.length == 4) {
                if (appLockManager.verifyPin(next)) {
                    onVerified()
                } else {
                    errorMessage = if (isEnglish) "Incorrect PIN" else "ভুল পিন!"
                    enteredPin = ""
                }
            }
        }
    }

    fun onBackspace() {
        if (enteredPin.isNotEmpty()) {
            enteredPin = enteredPin.dropLast(1)
            errorMessage = ""
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = AppCardDefaults.dialogBorder(),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = if (isEnglish) "Enter your current 4-digit PIN to proceed" else "চালিয়ে যেতে আপনার বর্তমান ৪-সংখ্যার পিন দিন",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(18.dp))

                // PIN dots
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    for (i in 0 until 4) {
                        val filled = i < enteredPin.length
                        val dotColor = if (errorMessage.isNotEmpty()) MaterialTheme.colorScheme.error else if (filled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .clip(CircleShape)
                                .background(dotColor)
                                .border(1.dp, if (filled) dotColor else MaterialTheme.colorScheme.outline, CircleShape)
                        )
                    }
                }

                if (errorMessage.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Keypad
                SimplePinKeypad(
                    onNumber = ::onNumberClick,
                    onBackspace = ::onBackspace
                )
            }
        }
    }
}

@Composable
private fun SimplePinKeypad(
    onNumber: (String) -> Unit,
    onBackspace: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val rows = listOf(
            listOf("1", "2", "3"),
            listOf("4", "5", "6"),
            listOf("7", "8", "9")
        )
        for (r in rows) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                for (num in r) {
                    MiniKeypadButton(text = num, onClick = { onNumber(num) })
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(modifier = Modifier.size(54.dp))
            MiniKeypadButton(text = "0", onClick = { onNumber("0") })
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(bounded = true, radius = 27.dp)
                    ) { onBackspace() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Backspace,
                    contentDescription = "Backspace",
                    modifier = Modifier.size(22.dp),
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun MiniKeypadButton(text: String, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .size(54.dp)
            .clip(CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true, radius = 27.dp)
            ) { onClick() },
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

package com.example.ui.screens

import com.example.ui.util.clearFocusOnTap
import com.example.R

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import com.example.data.sync.SyncState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.example.data.model.formatTakaSafe
import com.example.ui.util.AppLocale
import com.example.ui.viewmodel.ExpenseViewModel
import com.example.ui.components.AppCardDefaults
import com.example.ui.components.KeyboardScrollDownHint

/**
 * Authentication Screen: Unique Sign In & Sign Up for each user.
 * Guarantees each user has their own private session and data.
 */
@Composable
fun SignInScreen(
    viewModel: ExpenseViewModel,
    modifier: Modifier = Modifier
) {
    val isEnglish by viewModel.isEnglish.collectAsState()
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    var isSignUpMode by remember { mutableStateOf(false) }

    // Sign In form fields
    var loginMobile by remember { mutableStateOf("") }
    var loginPassword by remember { mutableStateOf("") }
    var showLoginPassword by remember { mutableStateOf(false) }

    // Sign Up form fields
    var signupName by remember { mutableStateOf("") }
    var signupMobile by remember { mutableStateOf("") }
    var signupPassword by remember { mutableStateOf("") }
    var signupConfirmPassword by remember { mutableStateOf("") }
    var showSignupPassword by remember { mutableStateOf(false) }

    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current

    val imeInsets = WindowInsets.ime
    val density = LocalDensity.current
    val isKeyboardOpen = imeInsets.getBottom(density) > 0

    val animatedCardElevation by animateDpAsState(
        targetValue = if (isKeyboardOpen) 14.dp else 8.dp,
        animationSpec = tween(durationMillis = 250),
        label = "auth_card_elevation"
    )
    val animatedLogoSize by animateDpAsState(
        targetValue = if (isKeyboardOpen) 48.dp else 80.dp,
        animationSpec = tween(durationMillis = 250),
        label = "auth_logo_size"
    )
    val animatedVerticalPadding by animateDpAsState(
        targetValue = if (isKeyboardOpen) 6.dp else 18.dp,
        animationSpec = tween(durationMillis = 250),
        label = "auth_vertical_padding"
    )
    val animatedContentPadding by animateDpAsState(
        targetValue = if (isKeyboardOpen) 16.dp else 22.dp,
        animationSpec = tween(durationMillis = 250),
        label = "auth_content_padding"
    )
    val animatedSpacing by animateDpAsState(
        targetValue = if (isKeyboardOpen) 10.dp else 16.dp,
        animationSpec = tween(durationMillis = 250),
        label = "auth_spacing"
    )

    // Dynamic background gradient based on theme
    val backgroundBrush = if (isDarkMode) {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFF070B14),
                Color(0xFF0F172A),
                Color(0xFF0A1120)
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFFEBF7F2),
                Color(0xFFF6FAF8),
                Color(0xFFEDF5FE)
            )
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundBrush)
            .clearFocusOnTap()
    ) {
        // Decorative ambient gradient orbs with glowing glass effect
        Box(
            modifier = Modifier
                .size(280.dp)
                .align(Alignment.TopEnd)
                .offset(x = 70.dp, y = (-50).dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            if (isDarkMode) Color(0xFF10B981).copy(alpha = 0.22f) else Color(0xFF10B981).copy(alpha = 0.28f),
                            Color.Transparent
                        )
                    )
                )
        )
        Box(
            modifier = Modifier
                .size(260.dp)
                .align(Alignment.BottomStart)
                .offset(x = (-70).dp, y = 70.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            if (isDarkMode) Color(0xFF3B82F6).copy(alpha = 0.20f) else Color(0xFF0EA5E9).copy(alpha = 0.22f),
                            Color.Transparent
                        )
                    )
                )
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 18.dp, vertical = animatedVerticalPadding)
                .clearFocusOnTap(),
            contentAlignment = if (isKeyboardOpen) Alignment.TopCenter else Alignment.Center
        ) {
            val authScrollState = rememberScrollState()

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 460.dp)
                    .testTag("auth_card"),
                shape = RoundedCornerShape(32.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isDarkMode) Color(0xFF131D2E).copy(alpha = 0.95f)
                    else MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)
                ),
                border = androidx.compose.foundation.BorderStroke(
                    width = 1.4.dp,
                    brush = Brush.linearGradient(
                        listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = if (isDarkMode) 0.65f else 0.45f),
                            Color(0xFF10B981).copy(alpha = 0.35f),
                            MaterialTheme.colorScheme.secondary.copy(alpha = if (isDarkMode) 0.4f else 0.25f)
                        )
                    )
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = animatedCardElevation)
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = animatedContentPadding, vertical = animatedContentPadding)
                            .verticalScroll(authScrollState),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(animatedSpacing)
                    ) {
                        // Top Header Action Bar: Theme pill, Security pill, Language pill
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Dark/Light Mode Pill Button
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = if (isDarkMode) Color(0xFF1E293B) else Color(0xFFF1F5F9),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isDarkMode) Color(0xFF334155) else Color(0xFFE2E8F0)
                                ),
                                modifier = Modifier
                                    .clickable { viewModel.toggleDarkMode() }
                                    .testTag("btn_auth_theme_toggle")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                                        contentDescription = if (isDarkMode) "Light Mode" else "Dark Mode",
                                        modifier = Modifier.size(15.dp),
                                        tint = if (isDarkMode) Color(0xFFFBBF24) else MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        text = if (isDarkMode) (if (isEnglish) "Light" else "লাইট") else (if (isEnglish) "Dark" else "ডার্ক"),
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
                                        color = if (isDarkMode) Color(0xFFE2E8F0) else Color(0xFF334155)
                                    )
                                }
                            }

                            // Security Chip
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = Color(0xFF10B981).copy(alpha = if (isDarkMode) 0.15f else 0.12f),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    Color(0xFF10B981).copy(alpha = 0.35f)
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.VerifiedUser,
                                        contentDescription = null,
                                        modifier = Modifier.size(13.dp),
                                        tint = Color(0xFF10B981)
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        text = if (isEnglish) "Private Vault" else "ব্যক্তিগত খাতা",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.5.sp
                                        ),
                                        color = if (isDarkMode) Color(0xFF34D399) else Color(0xFF047857)
                                    )
                                }
                            }

                            // Language Pill Button
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = if (isDarkMode) Color(0xFF1E293B) else Color(0xFFF1F5F9),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isDarkMode) Color(0xFF334155) else Color(0xFFE2E8F0)
                                ),
                                modifier = Modifier
                                    .clickable { viewModel.toggleAppLanguage() }
                                    .testTag("btn_auth_lang_toggle")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Language,
                                        contentDescription = null,
                                        modifier = Modifier.size(15.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        text = if (isEnglish) "বাংলা" else "English",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }

                        // App Hero Logo with Glowing Dual-Ring Frame
                        Surface(
                            shape = RoundedCornerShape(if (isKeyboardOpen) 18.dp else 26.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                            border = androidx.compose.foundation.BorderStroke(
                                width = 2.dp,
                                brush = Brush.linearGradient(
                                    listOf(
                                        MaterialTheme.colorScheme.primary,
                                        Color(0xFF10B981)
                                    )
                                )
                            ),
                            shadowElevation = if (isKeyboardOpen) 4.dp else 10.dp,
                            modifier = Modifier.size(animatedLogoSize)
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.hishab_khata_logo_1789834133185),
                                    contentDescription = "Daily Ledger Logo",
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(if (isKeyboardOpen) 2.dp else 3.dp)
                                        .clip(RoundedCornerShape(if (isKeyboardOpen) 16.dp else 23.dp))
                                )
                            }
                        }

                        // App Title & Tagline
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Text(
                                text = if (isEnglish) "Personal Daily Ledger" else "দৈনিক ব্যক্তিগত হিসাব",
                                style = if (isKeyboardOpen) MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = (-0.3).sp
                                ) else MaterialTheme.typography.headlineSmall.copy(
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = (-0.5).sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center
                            )
                            AnimatedVisibility(visible = !isKeyboardOpen) {
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                                    border = androidx.compose.foundation.BorderStroke(0.8.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                                    modifier = Modifier.padding(top = 2.dp)
                                ) {
                                    Text(
                                        text = if (isEnglish) "★ Smart • Secure • Multi-Device Sync ★" else "★ স্মার্ট • সুরক্ষিত • রিয়েল-টাইম ক্লাউড সিঙ্ক ★",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontSize = 11.sp
                                        ),
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (isSignUpMode) {
                                    if (isEnglish) "Create an account to manage your finances safely" else "নতুন অ্যাকাউন্ট খুলে আপনার আয়-ব্যয় নিরাপদে সংরক্ষণ করুন"
                                } else {
                                    if (isEnglish) "Sign in to access your personal ledger and debts" else "আপনার ব্যক্তিগত হিসাব ও দেনা-পাওনা দেখতে লগইন করুন"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }

                        // Segmented Switcher (Sign In / Sign Up)
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = if (isDarkMode) Color(0xFF1E293B) else Color(0xFFF1F5F9),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isDarkMode) Color(0xFF334155) else Color(0xFFE2E8F0)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(4.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                // Sign In Tab
                                Surface(
                                    onClick = {
                                        isSignUpMode = false
                                        errorMessage = null
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(14.dp),
                                    color = if (!isSignUpMode) MaterialTheme.colorScheme.primary else Color.Transparent,
                                    shadowElevation = if (!isSignUpMode) 3.dp else 0.dp
                                ) {
                                    Row(
                                        modifier = Modifier.padding(vertical = 10.dp),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Login,
                                            contentDescription = null,
                                            modifier = Modifier.size(17.dp),
                                            tint = if (!isSignUpMode) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (isEnglish) "Sign In" else "লগইন",
                                            style = MaterialTheme.typography.labelLarge.copy(
                                                fontWeight = if (!isSignUpMode) FontWeight.ExtraBold else FontWeight.Medium
                                            ),
                                            color = if (!isSignUpMode) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                // Sign Up Tab
                                Surface(
                                    onClick = {
                                        isSignUpMode = true
                                        errorMessage = null
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(14.dp),
                                    color = if (isSignUpMode) MaterialTheme.colorScheme.primary else Color.Transparent,
                                    shadowElevation = if (isSignUpMode) 3.dp else 0.dp
                                ) {
                                    Row(
                                        modifier = Modifier.padding(vertical = 10.dp),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PersonAdd,
                                            contentDescription = null,
                                            modifier = Modifier.size(17.dp),
                                            tint = if (isSignUpMode) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (isEnglish) "Sign Up" else "সাইন আপ",
                                            style = MaterialTheme.typography.labelLarge.copy(
                                                fontWeight = if (isSignUpMode) FontWeight.ExtraBold else FontWeight.Medium
                                            ),
                                            color = if (isSignUpMode) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }

                        // Error Message Display Banner
                        AnimatedVisibility(visible = errorMessage != null) {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.9f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ErrorOutline,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = errorMessage ?: "",
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(
                                        onClick = { errorMessage = null },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Dismiss",
                                            modifier = Modifier.size(16.dp),
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                        }

                        if (!isSignUpMode) {
                            // ================= SIGN IN FORM =================
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                OutlinedTextField(
                                    value = loginMobile,
                                    onValueChange = {
                                        loginMobile = it
                                        errorMessage = null
                                    },
                                    label = { Text(if (isEnglish) "Mobile Number" else "মোবাইল নম্বর") },
                                    placeholder = { Text(if (isEnglish) "e.g. 017XXXXXXXX" else "যেমন: 017XXXXXXXX") },
                                    leadingIcon = {
                                        Surface(
                                            shape = CircleShape,
                                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                            modifier = Modifier.size(34.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = Icons.Default.Phone,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(17.dp),
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                    },
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Phone,
                                        imeAction = androidx.compose.ui.text.input.ImeAction.Next
                                    ),
                                    singleLine = true,
                                    shape = RoundedCornerShape(18.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("input_login_mobile")
                                )

                                OutlinedTextField(
                                    value = loginPassword,
                                    onValueChange = {
                                        loginPassword = it
                                        errorMessage = null
                                    },
                                    label = { Text(if (isEnglish) "Password" else "পাসওয়ার্ড") },
                                    placeholder = { Text(if (isEnglish) "Enter password" else "পাসওয়ার্ড দিন") },
                                    leadingIcon = {
                                        Surface(
                                            shape = CircleShape,
                                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                            modifier = Modifier.size(34.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = Icons.Default.Lock,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(17.dp),
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                    },
                                    trailingIcon = {
                                        IconButton(onClick = { showLoginPassword = !showLoginPassword }) {
                                            Icon(
                                                imageVector = if (showLoginPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                                contentDescription = if (showLoginPassword) "Hide password" else "Show password",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    },
                                    visualTransformation = if (showLoginPassword) VisualTransformation.None else PasswordVisualTransformation(),
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Password,
                                        imeAction = androidx.compose.ui.text.input.ImeAction.Done
                                    ),
                                    keyboardActions = androidx.compose.foundation.text.KeyboardActions(onDone = { focusManager.clearFocus() }),
                                    singleLine = true,
                                    shape = RoundedCornerShape(18.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("input_login_password")
                                )

                                Spacer(modifier = Modifier.height(2.dp))

                                Button(
                                    onClick = {
                                        if (loginMobile.isBlank()) {
                                            errorMessage = if (isEnglish) "Please enter mobile number" else "দয়া করে মোবাইল নম্বর লিখুন"
                                            return@Button
                                        }
                                        if (loginPassword.isBlank()) {
                                            errorMessage = if (isEnglish) "Please enter password" else "দয়া করে পাসওয়ার্ড লিখুন"
                                            return@Button
                                        }
                                        isLoading = true
                                        viewModel.signInUser(
                                            mobile = loginMobile,
                                            password = loginPassword,
                                            onSuccess = { isLoading = false },
                                            onError = {
                                                isLoading = false
                                                errorMessage = it
                                            }
                                        )
                                    },
                                    shape = RoundedCornerShape(18.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary
                                    ),
                                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 5.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(52.dp)
                                        .testTag("btn_login_submit"),
                                    enabled = !isLoading
                                ) {
                                    if (isLoading) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(24.dp),
                                            color = MaterialTheme.colorScheme.onPrimary,
                                            strokeWidth = 2.5.dp
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.Login,
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = if (isEnglish) "Sign In to Ledger" else "লগইন করে হিসাব দেখুন",
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Icon(
                                            imageVector = Icons.Default.ArrowForward,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 2.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (isEnglish) "Don't have an account? " else "এখনও একাউন্ট নেই? ",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = if (isEnglish) "Sign Up Now" else "নতুন অ্যাকাউন্ট খুলুন",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontWeight = FontWeight.ExtraBold,
                                            color = MaterialTheme.colorScheme.primary
                                        ),
                                        modifier = Modifier
                                            .clickable {
                                                isSignUpMode = true
                                                errorMessage = null
                                            }
                                            .padding(4.dp)
                                    )
                                }

                                // 3-Feature Value Grid Cards
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 2.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // Item 1: Offline First
                                    Surface(
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(14.dp),
                                        color = if (isDarkMode) Color(0xFF1E293B).copy(alpha = 0.7f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                        border = androidx.compose.foundation.BorderStroke(
                                            0.8.dp,
                                            if (isDarkMode) Color(0xFF334155) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                                        )
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 10.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.OfflinePin,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(19.dp)
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = if (isEnglish) "100% Offline" else "অফলাইন প্রস্তুত",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 10.5.sp
                                                ),
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    }

                                    // Item 2: Private & Safe
                                    Surface(
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(14.dp),
                                        color = if (isDarkMode) Color(0xFF1E293B).copy(alpha = 0.7f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                        border = androidx.compose.foundation.BorderStroke(
                                            0.8.dp,
                                            if (isDarkMode) Color(0xFF334155) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                                        )
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 10.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Shield,
                                                contentDescription = null,
                                                tint = Color(0xFF10B981),
                                                modifier = Modifier.size(19.dp)
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = if (isEnglish) "Private Data" else "সুরক্ষিত ডাটা",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 10.5.sp
                                                ),
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    }

                                    // Item 3: Multi-device sync
                                    Surface(
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(14.dp),
                                        color = if (isDarkMode) Color(0xFF1E293B).copy(alpha = 0.7f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                        border = androidx.compose.foundation.BorderStroke(
                                            0.8.dp,
                                            if (isDarkMode) Color(0xFF334155) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                                        )
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 10.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.CloudDone,
                                                contentDescription = null,
                                                tint = Color(0xFF0284C7),
                                                modifier = Modifier.size(19.dp)
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = if (isEnglish) "Cloud Backup" else "ক্লাউড সিঙ্ক",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 10.5.sp
                                                ),
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    }
                                }
                            }
                        } else {
                            // ================= SIGN UP FORM =================
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(13.dp)
                            ) {
                                OutlinedTextField(
                                    value = signupName,
                                    onValueChange = {
                                        signupName = it
                                        errorMessage = null
                                    },
                                    label = { Text(if (isEnglish) "Full Name" else "আপনার পূর্ণ নাম") },
                                    placeholder = { Text(if (isEnglish) "e.g. John Doe" else "যেমন: মোঃ রাশেদ") },
                                    leadingIcon = {
                                        Surface(
                                            shape = CircleShape,
                                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                            modifier = Modifier.size(34.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = Icons.Default.Person,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(17.dp),
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                    },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Next),
                                    shape = RoundedCornerShape(18.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("input_signup_name")
                                )

                                OutlinedTextField(
                                    value = signupMobile,
                                    onValueChange = {
                                        signupMobile = it
                                        errorMessage = null
                                    },
                                    label = { Text(if (isEnglish) "Mobile Number" else "মোবাইল নম্বর") },
                                    placeholder = { Text(if (isEnglish) "e.g. 017XXXXXXXX" else "যেমন: 017XXXXXXXX") },
                                    supportingText = {
                                        Text(
                                            text = if (isEnglish) "Unique per user account" else "এক নম্বরে মাত্র একটি একাউন্ট চালানো যাবে",
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    },
                                    leadingIcon = {
                                        Surface(
                                            shape = CircleShape,
                                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                            modifier = Modifier.size(34.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = Icons.Default.Phone,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(17.dp),
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                    },
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Phone,
                                        imeAction = androidx.compose.ui.text.input.ImeAction.Next
                                    ),
                                    singleLine = true,
                                    shape = RoundedCornerShape(18.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("input_signup_mobile")
                                )

                                OutlinedTextField(
                                    value = signupPassword,
                                    onValueChange = {
                                        signupPassword = it
                                        errorMessage = null
                                    },
                                    label = { Text(if (isEnglish) "Password" else "পাসওয়ার্ড") },
                                    placeholder = { Text(if (isEnglish) "At least 4 characters" else "কমপক্ষে ৪ সংখ্যার পাসওয়ার্ড দিন") },
                                    leadingIcon = {
                                        Surface(
                                            shape = CircleShape,
                                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                            modifier = Modifier.size(34.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = Icons.Default.Lock,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(17.dp),
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                    },
                                    trailingIcon = {
                                        IconButton(onClick = { showSignupPassword = !showSignupPassword }) {
                                            Icon(
                                                imageVector = if (showSignupPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                                contentDescription = if (showSignupPassword) "Hide password" else "Show password",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    },
                                    visualTransformation = if (showSignupPassword) VisualTransformation.None else PasswordVisualTransformation(),
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Password,
                                        imeAction = androidx.compose.ui.text.input.ImeAction.Next
                                    ),
                                    singleLine = true,
                                    shape = RoundedCornerShape(18.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("input_signup_password")
                                )

                                OutlinedTextField(
                                    value = signupConfirmPassword,
                                    onValueChange = {
                                        signupConfirmPassword = it
                                        errorMessage = null
                                    },
                                    label = { Text(if (isEnglish) "Confirm Password" else "পাসওয়ার্ড নিশ্চিত করুন") },
                                    placeholder = { Text(if (isEnglish) "Re-enter password" else "একই পাসওয়ার্ড পুনরায় লিখুন") },
                                    leadingIcon = {
                                        Surface(
                                            shape = CircleShape,
                                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                            modifier = Modifier.size(34.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = Icons.Default.LockReset,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(17.dp),
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                    },
                                    visualTransformation = if (showSignupPassword) VisualTransformation.None else PasswordVisualTransformation(),
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Password,
                                        imeAction = androidx.compose.ui.text.input.ImeAction.Done
                                    ),
                                    keyboardActions = androidx.compose.foundation.text.KeyboardActions(onDone = { focusManager.clearFocus() }),
                                    singleLine = true,
                                    shape = RoundedCornerShape(18.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("input_signup_confirm_password")
                                )

                                Spacer(modifier = Modifier.height(2.dp))

                                Button(
                                    onClick = {
                                        val trimmedName = signupName.trim()
                                        val trimmedMobile = signupMobile.trim()
                                        val trimmedPass = signupPassword.trim()
                                        val trimmedConfirm = signupConfirmPassword.trim()

                                        if (trimmedName.isEmpty()) {
                                            errorMessage = if (isEnglish) "Please enter your name" else "অনুগ্রহ করে আপনার নাম লিখুন"
                                            return@Button
                                        }
                                        if (trimmedMobile.isEmpty() || trimmedMobile.length < 5) {
                                            errorMessage = if (isEnglish) "Please enter a valid mobile number" else "সঠিক মোবাইল নম্বর দিন"
                                            return@Button
                                        }
                                        if (trimmedPass.length < 4) {
                                            errorMessage = if (isEnglish) "Password must be at least 4 characters" else "পাসওয়ার্ড কমপক্ষে ৪ অক্ষরের হতে হবে"
                                            return@Button
                                        }
                                        if (trimmedPass != trimmedConfirm) {
                                            errorMessage = if (isEnglish) "Passwords do not match" else "দুটি পাসওয়ার্ড একই হতে হবে"
                                            return@Button
                                        }

                                        isLoading = true
                                        viewModel.signUpUser(
                                            name = trimmedName,
                                            mobile = trimmedMobile,
                                            password = trimmedPass,
                                            onSuccess = { isLoading = false },
                                            onError = {
                                                isLoading = false
                                                errorMessage = it
                                            }
                                        )
                                    },
                                    shape = RoundedCornerShape(18.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary
                                    ),
                                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 5.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(52.dp)
                                        .testTag("btn_signup_submit"),
                                    enabled = !isLoading
                                ) {
                                    if (isLoading) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(24.dp),
                                            color = MaterialTheme.colorScheme.onPrimary,
                                            strokeWidth = 2.5.dp
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.PersonAdd,
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = if (isEnglish) "Create Account & Start" else "অ্যাকাউন্ট খুলুন ও শুরু করুন",
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Icon(
                                            imageVector = Icons.Default.ArrowForward,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 2.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (isEnglish) "Already have an account? " else "ইতিমধ্যে অ্যাকাউন্ট আছে? ",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = if (isEnglish) "Sign In" else "লগইন করুন",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontWeight = FontWeight.ExtraBold,
                                            color = MaterialTheme.colorScheme.primary
                                        ),
                                        modifier = Modifier
                                            .clickable {
                                                isSignUpMode = false
                                                errorMessage = null
                                            }
                                            .padding(4.dp)
                                    )
                                }
                            }
                        }

                        // Privacy assurance footer note
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (isDarkMode) Color(0xFF1E293B).copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            border = androidx.compose.foundation.BorderStroke(
                                0.8.dp,
                                if (isDarkMode) Color(0xFF334155).copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isEnglish) "100% private session. Your financial records are encrypted." else "১০০% প্রাইভেট সেশন। আপনার হিসাব অন্য কেউ দেখতে পারবে না।",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium
                                    ),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }

                    // Keyboard squished indicator directing user downwards
                    KeyboardScrollDownHint(
                        scrollState = authScrollState,
                        isKeyboardOpen = isKeyboardOpen,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 6.dp),
                        label = if (isEnglish) "Scroll down" else "নিচে স্ক্রল করুন"
                    )
                }
            }
        }
    }
}

/**
 * Redesigned User Profile Dialog:
 * Features a modern, banking-grade aesthetic with user credentials, quick financial summary,
 * interactive Language selector (Bangla/English), Theme mode selector (Light/Dark),
 * profile editing, clear data reset, and logout options.
 */
@Composable
fun UserProfileDialog(
    viewModel: ExpenseViewModel,
    onDismiss: () -> Unit
) {
    val currentName by viewModel.userName.collectAsState()
    val currentPhone by viewModel.userPhone.collectAsState()
    val isEnglish by viewModel.isEnglish.collectAsState()

    var isEditing by remember { mutableStateOf(false) }
    var editName by remember { mutableStateOf(currentName) }
    var editPhone by remember { mutableStateOf(currentPhone) }
    var validationError by remember { mutableStateOf<String?>(null) }
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight()
                .imePadding()
                .clip(RoundedCornerShape(28.dp))
                .testTag("user_profile_dialog"),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                // Fixed Header Row with Close button
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AccountCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = if (isEnglish) "User Profile" else "ব্যবহারকারী প্রোফাইল",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (isEnglish) "Account details & identity" else "প্রোফাইল বিবরণ ও তথ্য",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // Scrollable Body Content
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Profile Identity Hero Card
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(22.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // User Avatar
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (currentName.isNotBlank()) currentName.take(1).uppercase() else "P",
                                    style = MaterialTheme.typography.headlineMedium.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                )
                            }

                            // User Name & Phone
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = currentName.ifBlank { if (isEnglish) "Personal User" else "ব্যক্তিগত ইউজার" },
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Phone,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Spacer(modifier = Modifier.width(5.dp))
                                        Text(
                                            text = currentPhone.ifBlank { if (isEnglish) "No Mobile" else "মোবাইল নম্বর নেই" },
                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            // Active Account Badge
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.VerifiedUser,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (isEnglish) "Verified Account • Private Ledger" else "যাচাইকৃত অ্যাকাউন্ট • ব্যক্তিগত খাতা",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    )
                                }
                            }
                        }
                    }

                    // PROFILE DETAILS & EDIT SECTION
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.ManageAccounts,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (isEnglish) "Profile Details" else "প্রোফাইল বিবরণ",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                    )
                                }

                                if (!isEditing) {
                                    FilledTonalButton(
                                        onClick = {
                                            editName = currentName
                                            editPhone = currentPhone
                                            isEditing = true
                                        },
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.height(32.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(13.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(text = if (isEnglish) "Edit" else "সম্পাদনা", fontSize = 12.sp)
                                    }
                                }
                            }

                            if (isEditing) {
                                if (validationError != null) {
                                    Text(
                                        text = validationError!!,
                                        color = MaterialTheme.colorScheme.error,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }

                                OutlinedTextField(
                                    value = editName,
                                    onValueChange = {
                                        editName = it
                                        validationError = null
                                    },
                                    label = { Text(if (isEnglish) "Full Name" else "পুরো নাম") },
                                    leadingIcon = { Icon(imageVector = Icons.Default.Person, contentDescription = null) },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Next),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                OutlinedTextField(
                                    value = editPhone,
                                    onValueChange = {
                                        editPhone = it
                                        validationError = null
                                    },
                                    label = { Text(if (isEnglish) "Mobile Number" else "মোবাইল নম্বর") },
                                    leadingIcon = { Icon(imageVector = Icons.Default.Phone, contentDescription = null) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = androidx.compose.ui.text.input.ImeAction.Done),
                                    keyboardActions = androidx.compose.foundation.text.KeyboardActions(onDone = { focusManager.clearFocus() }),
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            isEditing = false
                                            editName = currentName
                                            editPhone = currentPhone
                                        },
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(if (isEnglish) "Cancel" else "বাতিল")
                                    }

                                    Button(
                                        onClick = {
                                            val trimmedName = editName.trim()
                                            val trimmedPhone = editPhone.trim()
                                            if (trimmedName.isEmpty()) {
                                                validationError = if (isEnglish) "Name cannot be empty" else "নাম খালি রাখা যাবে না"
                                            } else if (trimmedPhone.isEmpty()) {
                                                validationError = if (isEnglish) "Phone is required" else "মোবাইল নম্বর প্রয়োজন"
                                            } else {
                                                viewModel.saveUserProfile(trimmedName, trimmedPhone)
                                                isEditing = false
                                            }
                                        },
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.weight(1.2f)
                                    ) {
                                        Text(if (isEnglish) "Save Changes" else "সংরক্ষণ করুন")
                                    }
                                }
                            } else {
                                // Read-only info cards
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = if (isEnglish) "Full Name" else "পুরো নাম:",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = currentName.ifBlank { "-" },
                                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                                            )
                                        }
                                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = if (isEnglish) "Registered Mobile" else "মোবাইল নম্বর:",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = currentPhone.ifBlank { "-" },
                                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    var showAboutInProfile by remember { mutableStateOf(false) }

                    OutlinedButton(
                        onClick = { showAboutInProfile = true },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("btn_profile_about_app")
                    ) {
                        Icon(imageVector = Icons.Default.Info, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isEnglish) "About App & Usage Guide" else "অ্যাপ পরিচিতি ও ব্যবহার নির্দেশিকা",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                    }

                    if (showAboutInProfile) {
                        AboutAppDialog(
                            initialIsEnglish = isEnglish,
                            onDismiss = { showAboutInProfile = false }
                        )
                    }

                    // Security & Privacy Note
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isEnglish) "Profile data is stored locally and securely synced with end-to-end encryption."
                                       else "প্রোফাইল তথ্য লোকাল স্টোরেজে সংরক্ষিত এবং এনক্রিপশনের সাথে সুরক্ষিতভাবে সিঙ্ক হয়।",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 11.sp,
                                    lineHeight = 15.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
        }
    }
}

package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.AppDatabase
import com.example.data.model.DebtEntity
import com.example.data.model.KhatEntity
import com.example.data.model.SavingsGoalEntity
import com.example.data.model.TransactionEntity
import com.example.data.model.UserEntity
import com.example.data.model.formatTakaSafe
import com.example.data.repository.ExpenseRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.data.sync.CloudSyncManager
import com.example.data.sync.SyncState
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.max

data class CategorySpendingItem(
    val category: String,
    val amount: Double,
    val percentage: Float,
    val count: Int
)

data class DaySpendingItem(
    val dayName: String,
    val dateText: String,
    val amount: Double,
    val isToday: Boolean
)

data class QuickExpensePreset(
    val title: String,
    val amount: Double,
    val category: String,
    val emoji: String
)

class ExpenseViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("daily_hishab_prefs", Context.MODE_PRIVATE)
    private val database = AppDatabase.getDatabase(application)
    val repository = ExpenseRepository(
        database.transactionDao(),
        database.accountDao()
    )
    private val debtDao = database.debtDao()
    private val savingsGoalDao = database.savingsGoalDao()
    private val khatDao = database.khatDao()
    private val userDao = database.userDao()
    private val accountDao = database.accountDao()

    // Cloud Sync Manager (Firebase Firestore & Portable Backup Engine)
    val cloudSyncManager = CloudSyncManager.getInstance(application)
    val syncState: StateFlow<SyncState> = cloudSyncManager.syncState
    val lastSyncTimestamp: StateFlow<Long> = cloudSyncManager.lastSyncTimestamp

    fun isFirebaseConfigured(): Boolean = cloudSyncManager.isFirebaseInitialized()

    private var autoSyncJob: Job? = null
    private var silentReconcileJob: Job? = null

    fun triggerBackgroundAutoSync() {
        val uid = _currentUserId.value
        if (uid > 0 && cloudSyncManager.isFirebaseInitialized()) {
            autoSyncJob?.cancel()
            autoSyncJob = viewModelScope.launch {
                delay(1200) // Debounce rapid consecutive edits into a single batched cloud sync
                try {
                    val user = userDao.getUserById(uid) ?: return@launch
                    cloudSyncManager.syncAllUserDataToCloud(
                        userId = uid,
                        user = user,
                        monthlyBudget = _monthlyBudget.value,
                        dailyTarget = _dailySpendingTarget.value
                    )
                } catch (_: Exception) {
                    // background sync fails silently without interrupting user
                }
            }
        }
    }

    fun reconcileCloudSyncSilently() {
        val uid = _currentUserId.value
        if (uid > 0 && cloudSyncManager.isFirebaseInitialized()) {
            silentReconcileJob?.cancel()
            silentReconcileJob = viewModelScope.launch {
                try {
                    val user = userDao.getUserById(uid) ?: return@launch
                    cloudSyncManager.reconcileAndSync(
                        userId = uid,
                        user = user,
                        monthlyBudget = _monthlyBudget.value,
                        dailyTarget = _dailySpendingTarget.value
                    )
                    loadUserSettingsFor(uid)
                } catch (_: Exception) {
                    // silent
                }
            }
        }
    }

    // Loading State for App Launch & Cold Start synchronization
    private val _isAppLoading = MutableStateFlow(true)
    val isAppLoading: StateFlow<Boolean> = _isAppLoading.asStateFlow()

    // Current logged-in User ID session
    private val _currentUserId = MutableStateFlow(prefs.getLong("current_user_id", -1L))
    val currentUserId: StateFlow<Long> = _currentUserId.asStateFlow()

    // User Profile Information
    private val _userName = MutableStateFlow(prefs.getString("user_profile_name", "") ?: "")
    val userName: StateFlow<String> = _userName.asStateFlow()

    private val _userPhone = MutableStateFlow(prefs.getString("user_profile_phone", "") ?: "")
    val userPhone: StateFlow<String> = _userPhone.asStateFlow()

    private val _isUserLoggedIn = MutableStateFlow(prefs.getBoolean("is_user_logged_in", false))
    val isUserLoggedIn: StateFlow<Boolean> = _isUserLoggedIn.asStateFlow()

    // Theme Mode (false = Light, true = Dark)
    private val _isDarkMode = MutableStateFlow(prefs.getBoolean("pref_is_dark_mode", false))
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    fun toggleDarkMode() {
        val next = !_isDarkMode.value
        _isDarkMode.value = next
        prefs.edit().putBoolean("pref_is_dark_mode", next).apply()
    }

    fun setDarkMode(dark: Boolean) {
        _isDarkMode.value = dark
        prefs.edit().putBoolean("pref_is_dark_mode", dark).apply()
    }

    // App Language ("bn" = Bangla, "en" = English)
    private val _appLanguage = MutableStateFlow(prefs.getString("pref_app_language", "bn") ?: "bn")
    val appLanguage: StateFlow<String> = _appLanguage.asStateFlow()
    private val _isEnglish = MutableStateFlow(prefs.getString("pref_app_language", "bn") == "en")
    val isEnglish: StateFlow<Boolean> = _isEnglish.asStateFlow()

    fun setAppLanguage(lang: String) {
        val validated = if (lang == "en") "en" else "bn"
        _appLanguage.value = validated
        _isEnglish.value = (validated == "en")
        prefs.edit().putString("pref_app_language", validated).apply()
    }

    fun toggleAppLanguage() {
        val next = if (_appLanguage.value == "en") "bn" else "en"
        setAppLanguage(next)
    }

    fun toggleLanguage() {
        toggleAppLanguage()
    }

    init {
        // Register network online callback for automatic background silent reconciliation
        cloudSyncManager.onNetworkAvailableSync = {
            if (!cloudSyncManager.isRealtimeListenerActive()) {
                reconcileCloudSyncSilently()
            }
        }

        // Ensure user session integrity & launch real-time sync listener
        viewModelScope.launch {
            try {
                val savedUid = prefs.getLong("current_user_id", -1L)
                if (savedUid > 0) {
                    val user = userDao.getUserById(savedUid)
                    if (user != null) {
                        _currentUserId.value = user.id
                        _userName.value = user.name
                        _userPhone.value = user.mobile
                        _isUserLoggedIn.value = true
                        loadUserSettingsFor(user.id)
                        startCloudRealtimeSyncFor(user)
                    } else {
                        logoutUser()
                    }
                } else if (_isUserLoggedIn.value && _userPhone.value.isNotBlank()) {
                    val user = userDao.getUserByMobile(_userPhone.value)
                    if (user != null) {
                        _currentUserId.value = user.id
                        prefs.edit().putLong("current_user_id", user.id).apply()
                        loadUserSettingsFor(user.id)
                        startCloudRealtimeSyncFor(user)
                    }
                }
            } catch (_: Exception) {
                // Ignore initialization hiccups
            } finally {
                _isAppLoading.value = false
            }
        }
    }

    /**
     * Starts listening to Firestore document changes in real time.
     * When any other phone logged into the same account adds, edits, or deletes an entry,
     * this listener reconciles Room and immediately updates all UI state flows silently.
     */
    fun startCloudRealtimeSyncFor(user: UserEntity) {
        if (!cloudSyncManager.isFirebaseInitialized()) return
        cloudSyncManager.startRealtimeSyncListener(
            userMobile = user.mobile,
            userPasswordProvider = { user.password },
            onDataUpdatedRemotely = {
                viewModelScope.launch {
                    val uid = _currentUserId.value
                    if (uid > 0) {
                        val updatedUser = userDao.getUserById(uid)
                        if (updatedUser != null) {
                            _userName.value = updatedUser.name
                        }
                        loadUserSettingsFor(uid)
                    }
                }
            },
            onAccountDeletedRemotely = {
                viewModelScope.launch {
                    val uid = _currentUserId.value
                    cloudSyncManager.stopRealtimeSyncListener()
                    if (uid > 0) {
                        repository.clearAllForUser(uid)
                        debtDao.clearAllForUser(uid)
                        savingsGoalDao.clearAllForUser(uid)
                        userDao.deleteUserById(uid)
                        com.example.util.AppLockManager.getInstance(getApplication()).clearAccountLock(uid)
                    }
                    logoutUser()
                }
            }
        )
    }

    /**
     * Actively pulls the authoritative server vault from Firestore and reconciles local storage.
     */
    fun refreshAllData(onResult: ((Boolean, String) -> Unit)? = null) {
        val uid = _currentUserId.value
        val phone = _userPhone.value.trim()
        if (uid <= 0 || phone.isBlank()) {
            onResult?.invoke(false, if (_isEnglish.value) "Please sign in first" else "প্রথমে সাইন ইন করুন")
            return
        }
        viewModelScope.launch {
            try {
                val user = userDao.getUserById(uid)
                if (user == null) {
                    onResult?.invoke(false, if (_isEnglish.value) "User not found" else "ব্যবহারকারী পাওয়া যায়নি")
                    return@launch
                }
                val result = cloudSyncManager.pullLatestFromCloud(user.mobile, user.password)
                if (result.isSuccess) {
                    _userName.value = prefs.getString("user_profile_name", user.name) ?: user.name
                    _monthlyBudget.value = prefs.getFloat("pref_monthly_budget_user_$uid", 0f).toDouble()
                    _dailySpendingTarget.value = prefs.getFloat("pref_daily_target_user_$uid", 500f).toDouble()
                    onResult?.invoke(true, if (_isEnglish.value) "Refreshed & synced with cloud" else "ক্লাউডের সাথে সব ডেটা সফলভাবে রিফ্রেশ হয়েছে")
                } else {
                    val err = result.exceptionOrNull()?.message ?: ""
                    if (err.contains("ACCOUNT_NOT_FOUND_OR_DELETED", ignoreCase = true)) {
                        logoutUser()
                        onResult?.invoke(false, if (_isEnglish.value) "Account has been deleted from another device" else "এই অ্যাকাউন্টটি অন্য ডিভাইস থেকে মুছে ফেলা হয়েছে")
                    } else {
                        onResult?.invoke(false, if (_isEnglish.value) "Cloud refresh completed" else "ক্লাউড রিফ্রেশ সম্পন্ন")
                    }
                }
            } catch (e: Exception) {
                onResult?.invoke(false, e.localizedMessage ?: "Sync error")
            }
        }
    }

    private fun loadUserSettingsFor(userId: Long) {
        val budget = prefs.getFloat("pref_monthly_budget_user_$userId", 0f).toDouble()
        val target = prefs.getFloat("pref_daily_target_user_$userId", 500f).toDouble()
        _monthlyBudget.value = budget
        _dailySpendingTarget.value = target
    }

    fun signUpUser(
        name: String,
        mobile: String,
        password: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val cleanName = name.trim()
        val cleanMobile = mobile.trim()
        val cleanPassword = password.trim()

        if (cleanName.isEmpty()) {
            onError("অনুগ্রহ করে আপনার পূর্ণ নাম লিখুন")
            return
        }
        if (cleanMobile.length < 5) {
            onError("সঠিক মোবাইল নম্বর প্রদান করুন")
            return
        }
        if (cleanPassword.length < 4) {
            onError("পাসওয়ার্ড কমপক্ষে ৪ অক্ষরের হতে হবে")
            return
        }

        viewModelScope.launch {
            try {
                val existing = userDao.getUserByMobile(cleanMobile)
                if (existing != null) {
                    onError(if (_isEnglish.value) "An account with this phone number already exists! Please log in." else "এই মোবাইল নম্বর দিয়ে ইতিমধ্যে অ্যাকাউন্ট রয়েছে! দয়া করে সাইন ইন করুন।")
                    return@launch
                }
                if (cloudSyncManager.isFirebaseInitialized()) {
                    val existsInCloud = cloudSyncManager.doesPhoneExistInCloud(cleanMobile)
                    if (existsInCloud) {
                        onError(if (_isEnglish.value) "An account with this phone number already exists! Please log in." else "এই মোবাইল নম্বর দিয়ে ইতিমধ্যে অ্যাকাউন্ট রয়েছে! দয়া করে সাইন ইন করুন।")
                        return@launch
                    }
                }
                val newUserId = userDao.insertUser(
                    UserEntity(
                        name = cleanName,
                        mobile = cleanMobile,
                        password = cleanPassword
                    )
                )

                // Register account in Cloud first BEFORE starting listener,
                // so snapshot listener doesn't see non-existent document and falsely log user out
                if (cloudSyncManager.isFirebaseInitialized()) {
                    cloudSyncManager.registerNewAccountInCloud(
                        mobile = cleanMobile,
                        name = cleanName,
                        password = cleanPassword,
                        monthlyBudget = _monthlyBudget.value,
                        dailyTarget = _dailySpendingTarget.value
                    )
                }

                prefs.edit()
                    .putLong("current_user_id", newUserId)
                    .putString("user_profile_name", cleanName)
                    .putString("user_profile_phone", cleanMobile)
                    .putBoolean("is_user_logged_in", true)
                    .apply()

                _currentUserId.value = newUserId
                _userName.value = cleanName
                _userPhone.value = cleanMobile
                _isUserLoggedIn.value = true
                com.example.util.AppLockManager.getInstance(getApplication()).setActiveUser(newUserId)
                loadUserSettingsFor(newUserId)

                val newUser = userDao.getUserById(newUserId) ?: UserEntity(id = newUserId, name = cleanName, mobile = cleanMobile, password = cleanPassword)
                startCloudRealtimeSyncFor(newUser)

                _feedbackMessage.value = if (_isEnglish.value) {
                    "Please visit the 'About the App' section in Settings to see full details on how the app works."
                } else {
                    "আপনি About সেকশনে গিয়ে অ্যাপটি কীভাবে কাজ করে তা বিস্তারিত দেখে নিন।"
                }
                prefs.edit().putBoolean("is_new_signup_pending_guide", true).apply()
                onSuccess()
            } catch (e: Exception) {
                onError(if (_isEnglish.value) "Failed to create account: ${e.localizedMessage}" else "অ্যাকাউন্ট তৈরি করতে ব্যর্থ: ${e.localizedMessage}")
            }
        }
    }

    fun signInUser(
        mobile: String,
        password: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val cleanMobile = mobile.trim()
        val cleanPassword = password.trim()

        if (cleanMobile.isEmpty()) {
            onError(if (_isEnglish.value) "Please enter mobile number" else "মোবাইল নম্বর লিখুন")
            return
        }
        if (cleanPassword.isEmpty()) {
            onError(if (_isEnglish.value) "Please enter password" else "পাসওয়ার্ড লিখুন")
            return
        }

        viewModelScope.launch {
            try {
                var user = userDao.getUserByMobile(cleanMobile)

                // If user is not found on this phone (e.g. phone lost or new phone):
                if (user == null) {
                    if (cloudSyncManager.isFirebaseInitialized()) {
                        val cloudResult = cloudSyncManager.tryCloudLogin(cleanMobile, cleanPassword)
                        if (cloudResult.isSuccess) {
                            user = cloudResult.getOrNull()
                        } else {
                            val err = cloudResult.exceptionOrNull()?.message ?: ""
                            when (err) {
                                "INCORRECT_PASSWORD" -> {
                                    onError(if (_isEnglish.value) "Incorrect password! Please check your credentials." else "ভুল পাসওয়ার্ড! ক্লাউড অ্যাকাউন্টের সঠিক পাসওয়ার্ড দিন।")
                                    return@launch
                                }
                                "USER_NOT_FOUND" -> {
                                    onError(if (_isEnglish.value) "No account found locally or in Cloud! Please Sign Up or restore from backup file." else "এই নম্বরে কোনো লোকাল বা ক্লাউড অ্যাকাউন্ট পাওয়া যায়নি! নিচে 'সাইন আপ' করুন অথবা ব্যাকআপ ফাইল থাকলে রিস্টোর করুন।")
                                    return@launch
                                }
                                else -> {
                                    val isOffline = err.contains("offline", ignoreCase = true) || err.contains("unavailable", ignoreCase = true) || err.contains("timed out", ignoreCase = true)
                                    val displayErr = if (isOffline) {
                                        if (_isEnglish.value) "Unable to connect to database server. Please check internet connection."
                                        else "ডাটাবেস সার্ভারের সাথে সংযোগ করা যাচ্ছে না। আপনার ইন্টারনেট সংযোগ নিশ্চিত করে পুনরায় চেষ্টা করুন।"
                                    } else {
                                        err
                                    }
                                    onError(if (_isEnglish.value) "Cloud sync login failed: $displayErr" else "ক্লাউড পুনরুদ্ধার ব্যর্থ: $displayErr")
                                    return@launch
                                }
                            }
                        }
                    } else {
                        onError(
                            if (_isEnglish.value)
                                "No account found on this phone. If you switched phones, tap 'Restore Backup File' below or Sign Up."
                            else
                                "এই নতুন ফোনে কোনো অ্যাকাউন্ট পাওয়া যায়নি। আপনি যদি ফোন পরিবর্তন করে থাকেন, তবে নিচে 'ব্যাকআপ ফাইল থেকে রিস্টোর' বোতাম চাপুন অথবা নতুন অ্যাকাউন্ট খুলুন।"
                        )
                        return@launch
                    }
                }

                if (user == null) {
                    onError(if (_isEnglish.value) "Login failed. Please try again." else "লগইন ব্যর্থ হয়েছে। পুনরায় চেষ্টা করুন।")
                    return@launch
                }

                if (user.password != cleanPassword) {
                    onError(if (_isEnglish.value) "Incorrect password! Please try again." else "ভুল পাসওয়ার্ড! সঠিক পাসওয়ার্ড দিয়ে পুনরায় চেষ্টা করুন।")
                    return@launch
                }

                prefs.edit()
                    .putLong("current_user_id", user.id)
                    .putString("user_profile_name", user.name)
                    .putString("user_profile_phone", user.mobile)
                    .putBoolean("is_user_logged_in", true)
                    .apply()

                _currentUserId.value = user.id
                _userName.value = user.name
                _userPhone.value = user.mobile
                _isUserLoggedIn.value = true
                com.example.util.AppLockManager.getInstance(getApplication()).setActiveUser(user.id)
                loadUserSettingsFor(user.id)

                startCloudRealtimeSyncFor(user)

                val isNewSignupPending = prefs.getBoolean("is_new_signup_pending_guide", false)
                val hasSeenGuide = prefs.getBoolean("has_seen_about_guide_${user.id}", false)
                if (isNewSignupPending || !hasSeenGuide) {
                    prefs.edit()
                        .putBoolean("is_new_signup_pending_guide", false)
                        .putBoolean("has_seen_about_guide_${user.id}", true)
                        .apply()
                    _feedbackMessage.value = if (_isEnglish.value) {
                        "Please visit the 'About the App' section in Settings to see full details on how the app works."
                    } else {
                        "আপনি About সেকশনে গিয়ে অ্যাপটি কীভাবে কাজ করে তা বিস্তারিত দেখে নিন।"
                    }
                } else {
                    _feedbackMessage.value = if (_isEnglish.value) {
                        "Welcome, ${user.name}! Logged in successfully."
                    } else {
                        "স্বাগতম, ${user.name}! সফলভাবে প্রবেশ করেছেন।"
                    }
                }

                onSuccess()
            } catch (e: Exception) {
                onError(if (_isEnglish.value) "Login error: ${e.localizedMessage}" else "লগইন ত্রুটি: ${e.localizedMessage}")
            }
        }
    }

    fun triggerCloudSync(onResult: ((Boolean, String) -> Unit)? = null) {
        val uid = _currentUserId.value
        if (uid <= 0) {
            onResult?.invoke(false, if (_isEnglish.value) "Please sign in first" else "প্রথমে সাইন ইন করুন")
            return
        }
        viewModelScope.launch {
            val user = userDao.getUserById(uid)
            if (user == null) {
                onResult?.invoke(false, if (_isEnglish.value) "User not found" else "ব্যবহারকারী পাওয়া যায়নি")
                return@launch
            }
            val res = cloudSyncManager.reconcileAndSync(
                userId = uid,
                user = user,
                monthlyBudget = _monthlyBudget.value,
                dailyTarget = _dailySpendingTarget.value
            )
            res.onSuccess {
                loadUserSettingsFor(uid)
                val msg = if (_isEnglish.value) "Cloud sync completed successfully!" else "ক্লাউড সিঙ্ক সফলভাবে সম্পন্ন হয়েছে!"
                _feedbackMessage.value = msg
                onResult?.invoke(true, msg)
            }.onFailure { err ->
                val msg = if (err.message == "FIREBASE_NOT_CONFIGURED") {
                    if (_isEnglish.value) "Online cloud requires Firebase config. You can also export a full backup file below." else "অনলাইন রিয়েলটাইম সিঙ্কের জন্য ফায়ারবেস কনফিগার প্রয়োজন। তবে আপনি নিচের বোতাম দিয়ে যেকোনো সময় ব্যাকআপ ফাইল সেভ করতে পারেন।"
                } else {
                    err.localizedMessage ?: "Sync error"
                }
                _feedbackMessage.value = msg
                onResult?.invoke(false, msg)
            }
        }
    }

    fun updateUserProfile(
        name: String,
        phone: String,
        password: String? = null,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        val uid = _currentUserId.value
        val cleanName = name.trim()
        val cleanPhone = phone.trim()
        if (cleanName.isEmpty()) {
            onError(if (_isEnglish.value) "Name cannot be empty" else "নাম খালি রাখা যাবে না")
            return
        }
        if (cleanPhone.length < 5) {
            onError(if (_isEnglish.value) "Valid phone number required" else "সঠিক মোবাইল নম্বর প্রয়োজন")
            return
        }

        viewModelScope.launch {
            try {
                if (uid > 0) {
                    val user = userDao.getUserById(uid)
                    if (user != null) {
                        val oldPhone = user.mobile.trim()
                        val isPhoneChanged = (cleanPhone != oldPhone)

                        if (isPhoneChanged) {
                            // Enforce uniqueness: check local and cloud
                            val existingLocal = userDao.getUserByMobile(cleanPhone)
                            if (existingLocal != null && existingLocal.id != uid) {
                                onError(if (_isEnglish.value) "This phone number is already registered to another account!" else "এই মোবাইল নম্বরটি ইতিমধ্যে অন্য একটি অ্যাকাউন্টে ব্যবহৃত হচ্ছে!")
                                return@launch
                            }
                            if (cloudSyncManager.isFirebaseInitialized()) {
                                val existsInCloud = cloudSyncManager.doesPhoneExistInCloud(cleanPhone)
                                if (existsInCloud) {
                                    onError(if (_isEnglish.value) "This phone number is already registered to another account!" else "এই মোবাইল নম্বরটি ইতিমধ্যে অন্য একটি অ্যাকাউন্টে ব্যবহৃত হচ্ছে!")
                                    return@launch
                                }
                            }
                        }

                        // Local update: Preserve user ID, transactions, debts, savings, AppLock
                        val updated = user.copy(
                            name = cleanName,
                            mobile = cleanPhone,
                            password = if (!password.isNullOrBlank()) password.trim() else user.password
                        )
                        userDao.updateUser(updated)

                        if (isPhoneChanged && cloudSyncManager.isFirebaseInitialized()) {
                            // 1. Immediately STOP real-time listener on old phone to prevent deletion triggers
                            cloudSyncManager.stopRealtimeSyncListener()

                            // 2. Upload full user data under new phone number to cloud
                            cloudSyncManager.syncAllUserDataToCloud(
                                userId = uid,
                                user = updated,
                                monthlyBudget = _monthlyBudget.value,
                                dailyTarget = _dailySpendingTarget.value
                            )

                            // 3. Delete old phone record from cloud WITHOUT setting isDeleted=true
                            cloudSyncManager.removeOldPhoneDocWithoutDeletion(oldPhone)

                            // 4. Start real-time sync listener on the new phone number
                            startCloudRealtimeSyncFor(updated)
                        } else if (!isPhoneChanged) {
                            triggerBackgroundAutoSync()
                        }
                    }
                }
                prefs.edit()
                    .putString("user_profile_name", cleanName)
                    .putString("user_profile_phone", cleanPhone)
                    .apply()
                _userName.value = cleanName
                _userPhone.value = cleanPhone
                _feedbackMessage.value = if (_isEnglish.value) "Profile updated successfully" else "প্রোফাইল তথ্য সফলভাবে আপডেট হয়েছে"
                onSuccess()
            } catch (e: Exception) {
                onError(if (_isEnglish.value) "Failed to update: ${e.localizedMessage}" else "আপডেট করতে ব্যর্থ: ${e.localizedMessage}")
            }
        }
    }

    fun saveUserProfile(name: String, phone: String) {
        updateUserProfile(name, phone)
    }

    /**
     * Permanent Account Deletion:
     * Completely wipes out all user data both from the local Room database
     * and the remote Firestore cloud vault (100% data eradication).
     */
    fun deleteUserAccount(
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val uid = _currentUserId.value
        val phone = _userPhone.value.trim()
        if (uid <= 0) {
            onError(if (_isEnglish.value) "No active user account found" else "কোনো সক্রিয় অ্যাকাউন্ট পাওয়া যায়নি")
            return
        }
        autoSyncJob?.cancel()
        autoSyncJob = null
        silentReconcileJob?.cancel()
        silentReconcileJob = null

        viewModelScope.launch {
            try {
                // 0. Stop listening to remote events
                cloudSyncManager.stopRealtimeSyncListener()

                // 1. Wipe out cloud data completely
                if (cloudSyncManager.isFirebaseInitialized() && phone.isNotBlank()) {
                    cloudSyncManager.deleteUserCloudData(phone)
                }

                // 2. Wipe out local Room database entries for this user
                repository.clearAllForUser(uid)
                debtDao.clearAllForUser(uid)
                savingsGoalDao.clearAllForUser(uid)
                khatDao.clearAllForUser(uid)
                accountDao.clearAll()
                userDao.deleteUserById(uid)

                // Clear account-scoped App Lock credentials and settings completely
                com.example.util.AppLockManager.getInstance(getApplication()).clearAccountLock(uid)

                // 3. Clear session and user preferences
                prefs.edit()
                    .remove("current_user_id")
                    .remove("user_profile_name")
                    .remove("user_profile_phone")
                    .putBoolean("is_user_logged_in", false)
                    .remove("pref_monthly_budget_user_$uid")
                    .remove("pref_daily_target_user_$uid")
                    .remove("pref_all_data_cleared_at_user_$uid")
                    .remove("pref_settings_updated_at_user_$uid")
                    .apply()

                _currentUserId.value = -1L
                _userName.value = ""
                _userPhone.value = ""
                _isUserLoggedIn.value = false
                resetNavStack()

                _feedbackMessage.value = if (_isEnglish.value) {
                    "Account and all data wiped out permanently."
                } else {
                    "অ্যাকাউন্ট এবং সমস্ত অনলাইন ও অফলাইন হিসাব স্থায়ীভাবে মুছে ফেলা হয়েছে।"
                }
                onSuccess()
            } catch (e: Exception) {
                onError(if (_isEnglish.value) "Failed to delete account: ${e.localizedMessage}" else "অ্যাকাউন্ট মুছতে ব্যর্থ: ${e.localizedMessage}")
            }
        }
    }

    fun logoutUser() {
        autoSyncJob?.cancel()
        autoSyncJob = null
        silentReconcileJob?.cancel()
        silentReconcileJob = null
        cloudSyncManager.stopRealtimeSyncListener()
        prefs.edit()
            .remove("current_user_id")
            .putBoolean("is_user_logged_in", false)
            .apply()
        _currentUserId.value = -1L
        _isUserLoggedIn.value = false
        _userName.value = ""
        _userPhone.value = ""
        com.example.util.AppLockManager.getInstance(getApplication()).setActiveUser(-1L)
        resetNavStack()
        _feedbackMessage.value = if (_isEnglish.value) "Logged out successfully" else "লগআউট সফল হয়েছে"
    }

    // Navigation Tab & History Stack: 0 = Daily, 1 = Debts, 2 = Calculator, 3 = Goals, 4 = Monthly
    private val _currentNavIndex = MutableStateFlow(0)
    val currentNavIndex: StateFlow<Int> = _currentNavIndex.asStateFlow()
    private val _navBackStack = MutableStateFlow(listOf(0))
    val navBackStack: StateFlow<List<Int>> = _navBackStack.asStateFlow()

    fun resetNavStack() {
        _currentNavIndex.value = 0
        _navBackStack.value = listOf(0)
    }

    fun quickAddFromCalculator(amount: Double, type: String) {
        val title = if (type == "EXPENSE") "ক্যালকুলেটর খরচ" else "ক্যালকুলেটর আয়"
        val category = if (type == "EXPENSE") "সাধারণ" else "অন্যান্য"
        addTransaction(
            title = title,
            amount = amount,
            type = type,
            category = category,
            note = "ক্যালকুলেটর থেকে হিসাবকৃত"
        )
    }

    // Filter & Search state for Daily screen
    private val _selectedTypeFilter = MutableStateFlow("ALL") // "ALL", "EXPENSE", "INCOME"
    val selectedTypeFilter: StateFlow<String> = _selectedTypeFilter.asStateFlow()

    private val _selectedDateFilter = MutableStateFlow("TODAY") // "TODAY", "YESTERDAY", "THIS_WEEK", "THIS_MONTH", "CUSTOM_DATE", "ALL"
    val selectedDateFilter: StateFlow<String> = _selectedDateFilter.asStateFlow()

    private val _customSelectedDate = MutableStateFlow<Long?>(null)
    val customSelectedDate: StateFlow<Long?> = _customSelectedDate.asStateFlow()

    fun selectSpecificDate(timestampMillis: Long) {
        val pickedCal = Calendar.getInstance().apply { timeInMillis = timestampMillis }
        val todayCal = Calendar.getInstance()
        val isToday = pickedCal.get(Calendar.YEAR) == todayCal.get(Calendar.YEAR) &&
                pickedCal.get(Calendar.DAY_OF_YEAR) == todayCal.get(Calendar.DAY_OF_YEAR)

        if (isToday) {
            clearCustomDate()
        } else {
            _customSelectedDate.value = timestampMillis
            _selectedDateFilter.value = "CUSTOM_DATE"
        }
    }

    fun clearCustomDate() {
        _customSelectedDate.value = null
        _selectedDateFilter.value = "TODAY"
    }

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _feedbackMessage = MutableStateFlow<String?>(null)
    val feedbackMessage: StateFlow<String?> = _feedbackMessage.asStateFlow()

    fun showToast(message: String) {
        _feedbackMessage.value = message
        com.example.ui.components.AppToastManager.show(message)
    }

    // Last added transaction for quick Undo
    private var lastAddedTransactionId: Long? = null

    // Daily spending target (default 500 or custom)
    private val _dailySpendingTarget = MutableStateFlow(prefs.getFloat("pref_daily_target", 500f).toDouble())
    val dailySpendingTarget: StateFlow<Double> = _dailySpendingTarget.asStateFlow()

    // Monthly Overview: Selected Month
    private val _selectedMonthCalendar = MutableStateFlow(Calendar.getInstance())
    val selectedMonthCalendar: StateFlow<Calendar> = _selectedMonthCalendar.asStateFlow()

    // Monthly Budget
    private val _monthlyBudget = MutableStateFlow(prefs.getFloat("pref_monthly_budget", 0f).toDouble())
    val monthlyBudget: StateFlow<Double> = _monthlyBudget.asStateFlow()

    // Quick Expense Presets
    val quickPresets = listOf(
        QuickExpensePreset("চা ও নাস্তা", 20.0, "খাবার ও বাজার", "☕"),
        QuickExpensePreset("রিকশা ভাড়া", 40.0, "যাতায়াত ও ভ্রমণ", "🛺"),
        QuickExpensePreset("দুপুরের খাবার", 120.0, "খাবার ও বাজার", "🍛"),
        QuickExpensePreset("মোবাইল রিচার্জ", 50.0, "মোবাইল রিচার্জ", "📱"),
        QuickExpensePreset("কাঁচাবাজার", 450.0, "খাবার ও বাজার", "🛒"),
        QuickExpensePreset("ওষুধ", 150.0, "চিকিৎসা ও স্বাস্থ্য", "💊")
    )

    // All Transactions Stream from Room (Scoped strictly per unique user)
    @OptIn(ExperimentalCoroutinesApi::class)
    val transactions: StateFlow<List<TransactionEntity>> = _currentUserId.flatMapLatest { uid ->
        if (uid > 0) repository.getTransactionsByUser(uid)
        else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // All Debts Stream from Room (Scoped strictly per unique user)
    @OptIn(ExperimentalCoroutinesApi::class)
    val debts: StateFlow<List<DebtEntity>> = _currentUserId.flatMapLatest { uid ->
        if (uid > 0) debtDao.getDebtsByUser(uid)
        else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // All Savings Goals Stream from Room (Scoped strictly per unique user)
    @OptIn(ExperimentalCoroutinesApi::class)
    val savingsGoals: StateFlow<List<SavingsGoalEntity>> = _currentUserId.flatMapLatest { uid ->
        if (uid > 0) savingsGoalDao.getGoalsByUser(uid)
        else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Filtered Transactions for Daily Screen
    val filteredTransactions: StateFlow<List<TransactionEntity>> = combine(
        transactions,
        _selectedTypeFilter,
        _selectedDateFilter,
        _customSelectedDate,
        _searchQuery
    ) { list, typeFilter, dateFilter, customDate, query ->
        val nowCal = Calendar.getInstance()
        val currentYear = nowCal.get(Calendar.YEAR)
        val currentDayOfYear = nowCal.get(Calendar.DAY_OF_YEAR)
        val currentWeek = nowCal.get(Calendar.WEEK_OF_YEAR)
        val currentMonth = nowCal.get(Calendar.MONTH)

        val itemCal = Calendar.getInstance()

        list.filter { item ->
            // Type check
            val matchesType = when (typeFilter) {
                "EXPENSE" -> item.type == "EXPENSE"
                "INCOME" -> item.type == "INCOME"
                else -> true
            }

            // Date check
            itemCal.timeInMillis = item.timestamp
            val matchesDate = when (dateFilter) {
                "TODAY" -> {
                    itemCal.get(Calendar.YEAR) == currentYear &&
                        itemCal.get(Calendar.DAY_OF_YEAR) == currentDayOfYear
                }
                "YESTERDAY" -> {
                    val yestCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
                    itemCal.get(Calendar.YEAR) == yestCal.get(Calendar.YEAR) &&
                        itemCal.get(Calendar.DAY_OF_YEAR) == yestCal.get(Calendar.DAY_OF_YEAR)
                }
                "THIS_WEEK" -> {
                    itemCal.get(Calendar.YEAR) == currentYear &&
                        itemCal.get(Calendar.WEEK_OF_YEAR) == currentWeek
                }
                "THIS_MONTH" -> {
                    itemCal.get(Calendar.YEAR) == currentYear &&
                        itemCal.get(Calendar.MONTH) == currentMonth
                }
                "CUSTOM_DATE" -> {
                    if (customDate != null) {
                        val cCal = Calendar.getInstance().apply { timeInMillis = customDate }
                        itemCal.get(Calendar.YEAR) == cCal.get(Calendar.YEAR) &&
                            itemCal.get(Calendar.DAY_OF_YEAR) == cCal.get(Calendar.DAY_OF_YEAR)
                    } else true
                }
                else -> true
            }

            // Query check
            val matchesQuery = query.isBlank() ||
                item.title.contains(query, ignoreCase = true) ||
                item.category.contains(query, ignoreCase = true) ||
                item.note.contains(query, ignoreCase = true)

            matchesType && matchesDate && matchesQuery
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Expense on the currently selected date/filter
    val selectedDateExpense: StateFlow<Double> = combine(
        transactions,
        _selectedDateFilter,
        _customSelectedDate
    ) { list, dateFilter, customDate ->
        val nowCal = Calendar.getInstance()
        val currentYear = nowCal.get(Calendar.YEAR)
        val currentDayOfYear = nowCal.get(Calendar.DAY_OF_YEAR)
        val itemCal = Calendar.getInstance()

        list.filter { item ->
            if (item.type != "EXPENSE") return@filter false
            itemCal.timeInMillis = item.timestamp
            when (dateFilter) {
                "YESTERDAY" -> {
                    val yestCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
                    itemCal.get(Calendar.YEAR) == yestCal.get(Calendar.YEAR) &&
                        itemCal.get(Calendar.DAY_OF_YEAR) == yestCal.get(Calendar.DAY_OF_YEAR)
                }
                "CUSTOM_DATE" -> {
                    if (customDate != null) {
                        val cCal = Calendar.getInstance().apply { timeInMillis = customDate }
                        itemCal.get(Calendar.YEAR) == cCal.get(Calendar.YEAR) &&
                            itemCal.get(Calendar.DAY_OF_YEAR) == cCal.get(Calendar.DAY_OF_YEAR)
                    } else true
                }
                else -> {
                    itemCal.get(Calendar.YEAR) == currentYear &&
                        itemCal.get(Calendar.DAY_OF_YEAR) == currentDayOfYear
                }
            }
        }.sumOf { it.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // Income on the currently selected date/filter
    val selectedDateIncome: StateFlow<Double> = combine(
        transactions,
        _selectedDateFilter,
        _customSelectedDate
    ) { list, dateFilter, customDate ->
        val nowCal = Calendar.getInstance()
        val currentYear = nowCal.get(Calendar.YEAR)
        val currentDayOfYear = nowCal.get(Calendar.DAY_OF_YEAR)
        val itemCal = Calendar.getInstance()

        list.filter { item ->
            if (item.type != "INCOME") return@filter false
            itemCal.timeInMillis = item.timestamp
            when (dateFilter) {
                "YESTERDAY" -> {
                    val yestCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
                    itemCal.get(Calendar.YEAR) == yestCal.get(Calendar.YEAR) &&
                        itemCal.get(Calendar.DAY_OF_YEAR) == yestCal.get(Calendar.DAY_OF_YEAR)
                }
                "CUSTOM_DATE" -> {
                    if (customDate != null) {
                        val cCal = Calendar.getInstance().apply { timeInMillis = customDate }
                        itemCal.get(Calendar.YEAR) == cCal.get(Calendar.YEAR) &&
                            itemCal.get(Calendar.DAY_OF_YEAR) == cCal.get(Calendar.DAY_OF_YEAR)
                    } else true
                }
                else -> {
                    itemCal.get(Calendar.YEAR) == currentYear &&
                        itemCal.get(Calendar.DAY_OF_YEAR) == currentDayOfYear
                }
            }
        }.sumOf { it.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // Total Overall Income
    val totalIncome: StateFlow<Double> = transactions.combine(_selectedTypeFilter) { list, _ ->
        list.filter { it.type == "INCOME" }.sumOf { it.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // Total Overall Expense
    val totalExpense: StateFlow<Double> = transactions.combine(_selectedTypeFilter) { list, _ ->
        list.filter { it.type == "EXPENSE" }.sumOf { it.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // Net Overall Balance
    val netBalance: StateFlow<Double> = combine(totalIncome, totalExpense) { inc, exp ->
        inc - exp
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // All Khats Stream from Room (Scoped strictly per unique user)
    @OptIn(ExperimentalCoroutinesApi::class)
    val khats: StateFlow<List<KhatEntity>> = _currentUserId.flatMapLatest { uid ->
        if (uid > 0) khatDao.getKhatsByUser(uid)
        else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Total Allocated across all custom khats
    val totalAllocated: StateFlow<Double> = khats.map { list ->
        list.sumOf { it.allocatedAmount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // Oboshisto (Remaining unallocated balance): Net Balance - Total Allocated
    val oboshistoBalance: StateFlow<Double> = combine(netBalance, totalAllocated) { net, allocated ->
        net - allocated
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // Today's Expense
    val todayExpense: StateFlow<Double> = transactions.combine(_selectedTypeFilter) { list, _ ->
        val cal = Calendar.getInstance()
        val currentYear = cal.get(Calendar.YEAR)
        val currentDayOfYear = cal.get(Calendar.DAY_OF_YEAR)

        list.filter { item ->
            if (item.type != "EXPENSE") return@filter false
            cal.timeInMillis = item.timestamp
            cal.get(Calendar.YEAR) == currentYear && cal.get(Calendar.DAY_OF_YEAR) == currentDayOfYear
        }.sumOf { it.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // Today's Income
    val todayIncome: StateFlow<Double> = transactions.combine(_selectedTypeFilter) { list, _ ->
        val cal = Calendar.getInstance()
        val currentYear = cal.get(Calendar.YEAR)
        val currentDayOfYear = cal.get(Calendar.DAY_OF_YEAR)

        list.filter { item ->
            if (item.type != "INCOME") return@filter false
            cal.timeInMillis = item.timestamp
            cal.get(Calendar.YEAR) == currentYear && cal.get(Calendar.DAY_OF_YEAR) == currentDayOfYear
        }.sumOf { it.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // Spending Streak (How many consecutive days stayed under daily target or zero excess)
    val budgetStreakDays: StateFlow<Int> = combine(transactions, _dailySpendingTarget) { list, target ->
        val expenseList = list.filter { it.type == "EXPENSE" }
        if (expenseList.isEmpty()) return@combine 0

        var streak = 0
        val cal = Calendar.getInstance()
        val itemCal = Calendar.getInstance()

        // Check backwards from today for up to 30 days
        for (i in 0..30) {
            val checkCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -i) }
            val cYear = checkCal.get(Calendar.YEAR)
            val cDay = checkCal.get(Calendar.DAY_OF_YEAR)

            val dayExpense = expenseList.filter {
                itemCal.timeInMillis = it.timestamp
                itemCal.get(Calendar.YEAR) == cYear && itemCal.get(Calendar.DAY_OF_YEAR) == cDay
            }.sumOf { it.amount }

            // If day has expense and stayed within target, or 0 expense
            if (dayExpense <= target) {
                streak++
            } else {
                // If today itself exceeded, streak breaks. If yesterday exceeded, breaks.
                break
            }
        }
        streak
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // Debts summary
    val totalReceivable: StateFlow<Double> = debts.combine(_selectedTypeFilter) { list, _ ->
        list.filter { it.type == "RECEIVE" && !it.isSettled }.sumOf { it.remainingAmount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val totalPayable: StateFlow<Double> = debts.combine(_selectedTypeFilter) { list, _ ->
        list.filter { it.type == "PAY" && !it.isSettled }.sumOf { it.remainingAmount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // Monthly Transactions
    val monthlyTransactions: StateFlow<List<TransactionEntity>> = combine(
        transactions,
        _selectedMonthCalendar
    ) { list, cal ->
        val targetYear = cal.get(Calendar.YEAR)
        val targetMonth = cal.get(Calendar.MONTH)
        val itemCal = Calendar.getInstance()

        list.filter { item ->
            itemCal.timeInMillis = item.timestamp
            itemCal.get(Calendar.YEAR) == targetYear && itemCal.get(Calendar.MONTH) == targetMonth
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Monthly Income
    val monthlyIncome: StateFlow<Double> = monthlyTransactions.combine(_selectedMonthCalendar) { list, _ ->
        list.filter { it.type == "INCOME" }.sumOf { it.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // Monthly Expense
    val monthlyExpense: StateFlow<Double> = monthlyTransactions.combine(_selectedMonthCalendar) { list, _ ->
        list.filter { it.type == "EXPENSE" }.sumOf { it.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // Monthly Savings
    val monthlySavings: StateFlow<Double> = combine(monthlyIncome, monthlyExpense) { inc, exp ->
        inc - exp
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // Monthly Savings Rate (%)
    val monthlySavingsRate: StateFlow<Double> = combine(monthlyIncome, monthlyExpense) { inc, exp ->
        if (inc > 0) {
            val rate = ((inc - exp) / inc) * 100.0
            max(-100.0, rate)
        } else {
            0.0
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // Daily Average Expense in selected month
    val monthlyDailyAverage: StateFlow<Double> = combine(monthlyExpense, _selectedMonthCalendar) { exp, cal ->
        val now = Calendar.getInstance()
        val isCurrentMonth = cal.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
            cal.get(Calendar.MONTH) == now.get(Calendar.MONTH)

        val daysCount = if (isCurrentMonth) {
            max(1, now.get(Calendar.DAY_OF_MONTH))
        } else {
            cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        }
        exp / daysCount
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // Category Breakdown for selected month
    val monthlyCategoryBreakdown: StateFlow<List<CategorySpendingItem>> = combine(
        monthlyTransactions,
        monthlyExpense
    ) { list, totalExp ->
        val expenseList = list.filter { it.type == "EXPENSE" }
        val grouped = expenseList.groupBy { it.category }
        grouped.map { (cat, items) ->
            val sum = items.sumOf { it.amount }
            val pct = if (totalExp > 0) (sum / totalExp).toFloat() else 0f
            CategorySpendingItem(
                category = cat,
                amount = sum,
                percentage = pct,
                count = items.size
            )
        }.sortedByDescending { it.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 7-Day Expense Trend
    val weeklyTrend: StateFlow<List<DaySpendingItem>> = transactions.combine(_selectedTypeFilter) { list, _ ->
        val result = mutableListOf<DaySpendingItem>()
        val bengaliDays = arrayOf("রবি", "সোম", "মঙ্গল", "বুধ", "বৃহঃ", "শুক্র", "শনি")
        val dateFormat = SimpleDateFormat("dd MMM", Locale.US)

        for (i in 6 downTo 0) {
            val targetCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -i) }
            val tYear = targetCal.get(Calendar.YEAR)
            val tDay = targetCal.get(Calendar.DAY_OF_YEAR)
            val dayOfWeek = targetCal.get(Calendar.DAY_OF_WEEK) - 1

            val dayTotal = list.filter { item ->
                if (item.type != "EXPENSE") return@filter false
                val itemCal = Calendar.getInstance().apply { timeInMillis = item.timestamp }
                itemCal.get(Calendar.YEAR) == tYear && itemCal.get(Calendar.DAY_OF_YEAR) == tDay
            }.sumOf { it.amount }

            result.add(
                DaySpendingItem(
                    dayName = bengaliDays[dayOfWeek],
                    dateText = dateFormat.format(targetCal.time),
                    amount = dayTotal,
                    isToday = (i == 0)
                )
            )
        }
        result
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Safe Daily Spending Allowance based on Budget
    val safeDailySpending: StateFlow<Double> = combine(
        monthlyBudget,
        monthlyExpense,
        _selectedMonthCalendar
    ) { budget, expense, cal ->
        if (budget <= 0) return@combine 0.0

        val now = Calendar.getInstance()
        val isCurrentMonth = cal.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
            cal.get(Calendar.MONTH) == now.get(Calendar.MONTH)

        if (!isCurrentMonth) return@combine 0.0

        val totalDays = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val passedDays = now.get(Calendar.DAY_OF_MONTH)
        val remainingDays = max(1, totalDays - passedDays + 1)
        val remainingBudget = budget - expense

        if (remainingBudget > 0) remainingBudget / remainingDays else 0.0
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // Previous month expense for comparison
    val previousMonthExpense: StateFlow<Double> = combine(
        transactions,
        _selectedMonthCalendar
    ) { list, cal ->
        val prevCal = Calendar.getInstance().apply {
            timeInMillis = cal.timeInMillis
            add(Calendar.MONTH, -1)
        }
        val targetYear = prevCal.get(Calendar.YEAR)
        val targetMonth = prevCal.get(Calendar.MONTH)
        val itemCal = Calendar.getInstance()
        list.filter { item ->
            if (item.type != "EXPENSE") return@filter false
            itemCal.timeInMillis = item.timestamp
            itemCal.get(Calendar.YEAR) == targetYear && itemCal.get(Calendar.MONTH) == targetMonth
        }.sumOf { it.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // Month-over-Month difference and percentage
    val monthOverMonthDiff: StateFlow<Double> = combine(
        monthlyExpense,
        previousMonthExpense
    ) { current, previous ->
        current - previous
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val monthOverMonthPercentage: StateFlow<Double> = combine(
        monthlyExpense,
        previousMonthExpense
    ) { current, previous ->
        if (previous > 0) {
            ((current - previous) / previous) * 100.0
        } else 0.0
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // Projected Month-End Expense
    val projectedMonthExpense: StateFlow<Double> = combine(
        monthlyExpense,
        _selectedMonthCalendar
    ) { exp, cal ->
        val now = Calendar.getInstance()
        val isCurrentMonth = cal.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
            cal.get(Calendar.MONTH) == now.get(Calendar.MONTH)
        if (!isCurrentMonth) {
            exp
        } else {
            val currentDay = max(1, now.get(Calendar.DAY_OF_MONTH))
            val totalDays = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
            (exp / currentDay) * totalDays
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // Highest spending day in the selected month: Pair(DayOfMonth, Amount)
    val peakSpendingDay: StateFlow<Pair<Int, Double>?> = monthlyTransactions.map { list ->
        val expenseList = list.filter { it.type == "EXPENSE" }
        if (expenseList.isEmpty()) return@map null
        val itemCal = Calendar.getInstance()
        val dayGroups = expenseList.groupBy { item ->
            itemCal.timeInMillis = item.timestamp
            itemCal.get(Calendar.DAY_OF_MONTH)
        }
        val maxDayEntry = dayGroups.maxByOrNull { entry -> entry.value.sumOf { it.amount } }
        if (maxDayEntry != null) {
            val totalForDay = maxDayEntry.value.sumOf { it.amount }
            if (totalForDay > 0) {
                Pair(maxDayEntry.key, totalForDay)
            } else null
        } else null
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Monthly daily distribution map: DayOfMonth (1..31) -> total expense
    val monthDailyExpenseMap: StateFlow<Map<Int, Double>> = monthlyTransactions.map { list ->
        val itemCal = Calendar.getInstance()
        val expenseList = list.filter { it.type == "EXPENSE" }
        val map = mutableMapOf<Int, Double>()
        expenseList.forEach { item ->
            itemCal.timeInMillis = item.timestamp
            val day = itemCal.get(Calendar.DAY_OF_MONTH)
            map[day] = (map[day] ?: 0.0) + item.amount
        }
        map
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    // Financial Health Score (0 to 100)
    val financialHealthScore: StateFlow<Int> = combine(
        monthlyIncome,
        monthlyExpense,
        monthlyBudget,
        monthlySavingsRate
    ) { income, expense, budget, rate ->
        var score = 65 // Baseline
        if (income > 0) {
            if (rate >= 30.0) score += 20
            else if (rate >= 15.0) score += 10
            else if (rate >= 0.0) score += 5
            else score -= 15
        }
        if (budget > 0) {
            if (expense <= budget) score += 15
            else score -= 20
        }
        score.coerceIn(10, 100)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 75)

    // Navigation Tab & Back Stack Operations
    fun setNavIndex(index: Int) {
        if (_currentNavIndex.value == index) return
        _currentNavIndex.value = index
        val updated = _navBackStack.value.toMutableList()
        updated.add(index)
        _navBackStack.value = updated
    }

    /**
     * Pops the current page and returns to the previous page in history.
     * Returns true if a previous page was navigated to, false if the stack had only 1 page.
     */
    fun popNavBackStack(): Boolean {
        val currentList = _navBackStack.value.toMutableList()
        if (currentList.size > 1) {
            currentList.removeAt(currentList.lastIndex) // remove current page
            val prevIndex = currentList.last()
            _navBackStack.value = currentList
            _currentNavIndex.value = prevIndex
            return true
        }
        return false
    }

    fun setTypeFilter(filter: String) {
        _selectedTypeFilter.value = filter
    }

    fun setDateFilter(filter: String) {
        if (filter != "CUSTOM_DATE") {
            _customSelectedDate.value = null
        }
        _selectedDateFilter.value = filter
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun previousMonth() {
        val newCal = Calendar.getInstance().apply {
            timeInMillis = _selectedMonthCalendar.value.timeInMillis
            add(Calendar.MONTH, -1)
        }
        _selectedMonthCalendar.value = newCal
    }

    fun nextMonth() {
        val newCal = Calendar.getInstance().apply {
            timeInMillis = _selectedMonthCalendar.value.timeInMillis
            add(Calendar.MONTH, 1)
        }
        _selectedMonthCalendar.value = newCal
    }

    fun resetToCurrentMonth() {
        _selectedMonthCalendar.value = Calendar.getInstance()
    }

    fun setYearAndMonth(year: Int, month: Int) {
        val newCal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, 1)
        }
        _selectedMonthCalendar.value = newCal
    }

    fun setMonthlyBudget(amount: Double) {
        _monthlyBudget.value = amount
        val uid = _currentUserId.value
        val now = System.currentTimeMillis()
        if (uid > 0) {
            prefs.edit()
                .putFloat("pref_monthly_budget_user_$uid", amount.toFloat())
                .putLong("pref_settings_updated_at_user_$uid", now)
                .apply()
        } else {
            prefs.edit().putFloat("pref_monthly_budget", amount.toFloat()).apply()
        }
        _feedbackMessage.value = if (_isEnglish.value) {
            "Monthly budget set to: ${formatTakaSafe(amount)}"
        } else {
            "মাসিক বাজেট নির্ধারণ করা হয়েছে: ${formatTakaSafe(amount)}"
        }
        triggerBackgroundAutoSync()
    }

    fun setDailySpendingTarget(target: Double) {
        _dailySpendingTarget.value = target
        val uid = _currentUserId.value
        val now = System.currentTimeMillis()
        if (uid > 0) {
            prefs.edit()
                .putFloat("pref_daily_target_user_$uid", target.toFloat())
                .putLong("pref_settings_updated_at_user_$uid", now)
                .apply()
        } else {
            prefs.edit().putFloat("pref_daily_target", target.toFloat()).apply()
        }
        _feedbackMessage.value = if (_isEnglish.value) {
            "Daily spending target: ${formatTakaSafe(target)}"
        } else {
            "দৈনিক খরচের লক্ষ্যমাত্রা: ${formatTakaSafe(target)}"
        }
        triggerBackgroundAutoSync()
    }

    fun addTransaction(
        title: String,
        amount: Double,
        type: String,
        category: String,
        note: String = "",
        timestamp: Long = System.currentTimeMillis(),
        khatId: Long? = null,
        khatName: String = "",
        onSuccess: (() -> Unit)? = null,
        onError: ((String) -> Unit)? = null
    ) {
        val uid = _currentUserId.value
        viewModelScope.launch {
            // Negative Balance Protection & Account Sync for Expenses
            if (type == "EXPENSE") {
                val currentNet = netBalance.value
                if (currentNet <= 0.0) {
                    val errMsg = if (_isEnglish.value) {
                        "Account balance is ৳0! Please add income first before spending."
                    } else {
                        "অ্যাকাউন্টে পর্যাপ্ত ব্যালেন্স নেই (ব্যালেন্স ৳০)! খরচ করতে প্রথমে অ্যাকাউন্টে আয় বা টাকা যোগ করুন।"
                    }
                    onError?.invoke(errMsg)
                    _feedbackMessage.value = errMsg
                    return@launch
                }
                if (amount > currentNet) {
                    val errMsg = if (_isEnglish.value) {
                        "Expense exceeds total available balance (Available: ${formatTakaSafe(currentNet)})"
                    } else {
                        "খরচের পরিমাণ মোট ব্যালেন্স অতিক্রম করেছে (আছে: ${formatTakaSafe(currentNet)})"
                    }
                    onError?.invoke(errMsg)
                    _feedbackMessage.value = errMsg
                    return@launch
                }

                // If spending from a specific custom khat, deduct from its allocated amount
                if (khatId != null && khatId > 0L) {
                    val khat = khatDao.getKhatById(khatId)
                    if (khat != null) {
                        if (amount > khat.allocatedAmount) {
                            val errMsg = if (_isEnglish.value) {
                                "Insufficient balance in khat '${khat.name}' (Available: ${formatTakaSafe(khat.allocatedAmount)})"
                            } else {
                                "'${khat.name}' খাতে পর্যাপ্ত ব্যালেন্স নেই (আছে: ${formatTakaSafe(khat.allocatedAmount)})"
                            }
                            onError?.invoke(errMsg)
                            _feedbackMessage.value = errMsg
                            return@launch
                        }
                        val newAlloc = maxOf(0.0, khat.allocatedAmount - amount)
                        khatDao.updateKhat(khat.copy(allocatedAmount = newAlloc, updatedAt = System.currentTimeMillis()))
                    }
                } else if (khats.value.isNotEmpty()) {
                    // Spending from unallocated Oboshisto balance
                    val currentOboshisto = oboshistoBalance.value
                    if (amount > currentOboshisto) {
                        val errMsg = if (_isEnglish.value) {
                            "Expense exceeds unallocated balance (Available: ${formatTakaSafe(currentOboshisto)})"
                        } else {
                            "খরচের পরিমাণ অবশিষ্ট ব্যালেন্স অতিক্রম করেছে (আছে: ${formatTakaSafe(currentOboshisto)})"
                        }
                        onError?.invoke(errMsg)
                        _feedbackMessage.value = errMsg
                        return@launch
                    }
                }
            }

            val entity = TransactionEntity(
                userId = if (uid > 0) uid else 0L,
                title = title.ifBlank { category },
                amount = amount,
                type = type,
                category = category,
                note = note,
                khatId = khatId,
                khatName = khatName,
                timestamp = timestamp
            )
            val id = database.transactionDao().insertTransaction(entity)
            lastAddedTransactionId = id
            triggerBackgroundAutoSync()
            _feedbackMessage.value = if (_isEnglish.value) {
                val actionEn = if (type == "EXPENSE") "expense" else "income"
                "New $actionEn added: ${formatTakaSafe(amount)}"
            } else {
                val actionBn = if (type == "EXPENSE") "খরচ" else "আয়"
                "নতুন $actionBn যোগ হয়েছে: ${formatTakaSafe(amount)}"
            }
            onSuccess?.invoke()
        }
    }

    fun addQuickPreset(preset: QuickExpensePreset) {
        addTransaction(
            title = preset.title,
            amount = preset.amount,
            type = "EXPENSE",
            category = preset.category,
            note = if (_isEnglish.value) "Quick Expense" else "দ্রুত খরচ"
        )
    }

    fun undoLastAddedTransaction() {
        val id = lastAddedTransactionId ?: return
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val tx = database.transactionDao().getTransactionById(id)
            if (tx != null && tx.type == "EXPENSE" && tx.khatId != null && tx.khatId > 0L) {
                val khat = khatDao.getKhatById(tx.khatId)
                if (khat != null) {
                    khatDao.updateKhat(khat.copy(allocatedAmount = khat.allocatedAmount + tx.amount, updatedAt = now))
                }
            }
            database.transactionDao().markDeleted(id, now)
            lastAddedTransactionId = null
            triggerBackgroundAutoSync()
            _feedbackMessage.value = if (_isEnglish.value) {
                "Last transaction undone"
            } else {
                "শেষ লেনদেনটি বাতিল করা হয়েছে"
            }
        }
    }

    fun deleteTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            if (transaction.type == "EXPENSE" && transaction.khatId != null && transaction.khatId > 0L) {
                val khat = khatDao.getKhatById(transaction.khatId)
                if (khat != null) {
                    val now = System.currentTimeMillis()
                    khatDao.updateKhat(khat.copy(allocatedAmount = khat.allocatedAmount + transaction.amount, updatedAt = now))
                }
            }
            repository.deleteTransaction(transaction)
            triggerBackgroundAutoSync()
            _feedbackMessage.value = if (_isEnglish.value) {
                "Transaction deleted"
            } else {
                "লেনদেন মুছে ফেলা হয়েছে"
            }
        }
    }

    // ==========================================
    // KHAT & ALLOCATION SYSTEM CRUD
    // ==========================================

    fun createKhat(
        name: String,
        allocatedAmount: Double,
        colorHex: String = "#4F46E5",
        iconName: String = "folder",
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        val cleanName = name.trim()
        if (cleanName.isEmpty()) {
            onError(if (_isEnglish.value) "Khat name cannot be empty" else "খাতের নাম খালি রাখা যাবে না")
            return
        }
        if (allocatedAmount < 0) {
            onError(if (_isEnglish.value) "Allocated amount cannot be negative" else "বরাদ্দকৃত টাকা ঋণাত্মক হতে পারবে না")
            return
        }
        val currentOboshisto = oboshistoBalance.value
        if (allocatedAmount > 0.0 && allocatedAmount > currentOboshisto) {
            val err = if (_isEnglish.value) {
                "Allocated amount (${formatTakaSafe(allocatedAmount)}) exceeds available Oboshisto (${formatTakaSafe(currentOboshisto)})"
            } else {
                "বরাদ্দকৃত টাকা (${formatTakaSafe(allocatedAmount)}) অবশিষ্ট ব্যালেন্সের (${formatTakaSafe(currentOboshisto)}) চেয়ে বেশি হতে পারবে না"
            }
            onError(err)
            return
        }

        val uid = _currentUserId.value
        viewModelScope.launch {
            try {
                val entity = KhatEntity(
                    userId = if (uid > 0) uid else 0L,
                    name = cleanName,
                    allocatedAmount = allocatedAmount,
                    colorHex = colorHex,
                    iconName = iconName,
                    timestamp = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
                khatDao.insertKhat(entity)
                triggerBackgroundAutoSync()
                _feedbackMessage.value = if (_isEnglish.value) {
                    "New khat '$cleanName' created with ${formatTakaSafe(allocatedAmount)}"
                } else {
                    "নতুন খাত '$cleanName' তৈরি হয়েছে: ${formatTakaSafe(allocatedAmount)}"
                }
                onSuccess()
            } catch (e: Exception) {
                onError(e.localizedMessage ?: "Failed to create khat")
            }
        }
    }

    fun updateKhat(
        khat: KhatEntity,
        newName: String = khat.name,
        newAmount: Double = khat.allocatedAmount,
        colorHex: String = khat.colorHex,
        iconName: String = khat.iconName,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        val cleanName = newName.trim()
        if (cleanName.isEmpty()) {
            onError(if (_isEnglish.value) "Khat name cannot be empty" else "খাতের নাম খালি রাখা যাবে না")
            return
        }
        if (newAmount < 0) {
            onError(if (_isEnglish.value) "Allocated amount cannot be negative" else "বরাদ্দকৃত টাকা ঋণাত্মক হতে পারবে না")
            return
        }

        val diff = newAmount - khat.allocatedAmount
        val currentOboshisto = oboshistoBalance.value
        if (diff > 0 && diff > currentOboshisto) {
            val err = if (_isEnglish.value) {
                "Increased allocation (${formatTakaSafe(diff)}) exceeds available Oboshisto (${formatTakaSafe(currentOboshisto)})"
            } else {
                "বরাদ্দ বৃদ্ধির পরিমাণ (${formatTakaSafe(diff)}) অবশিষ্ট ব্যালেন্সের (${formatTakaSafe(currentOboshisto)}) চেয়ে বেশি হতে পারবে না"
            }
            onError(err)
            return
        }

        viewModelScope.launch {
            try {
                val updated = khat.copy(
                    name = cleanName,
                    allocatedAmount = newAmount,
                    colorHex = colorHex,
                    iconName = iconName,
                    updatedAt = System.currentTimeMillis()
                )
                khatDao.updateKhat(updated)
                triggerBackgroundAutoSync()
                _feedbackMessage.value = if (_isEnglish.value) {
                    "Khat '$cleanName' updated"
                } else {
                    "খাত '$cleanName' আপডেট সম্পন্ন হয়েছে"
                }
                onSuccess()
            } catch (e: Exception) {
                onError(e.localizedMessage ?: "Failed to update khat")
            }
        }
    }

    fun updateKhat(khat: KhatEntity) {
        updateKhat(khat, khat.name, khat.allocatedAmount, khat.colorHex, khat.iconName)
    }

    fun deleteKhat(khat: KhatEntity) {
        viewModelScope.launch {
            try {
                val now = System.currentTimeMillis()
                khatDao.markDeleted(khat.id, now)
                triggerBackgroundAutoSync()
                _feedbackMessage.value = if (_isEnglish.value) {
                    "Khat '${khat.name}' deleted. Remaining balance returned to Oboshisto."
                } else {
                    "খাত '${khat.name}' মুছে ফেলা হয়েছে। ব্যালেন্স অবশিষ্টে যোগ হয়েছে।"
                }
            } catch (e: Exception) {
                _feedbackMessage.value = e.localizedMessage ?: "Failed to delete khat"
            }
        }
    }

    fun clearAllData(onComplete: (() -> Unit)? = null) {
        // 1. Cancel background auto sync or silent reconcile jobs so stale data is not pushed
        autoSyncJob?.cancel()
        autoSyncJob = null
        silentReconcileJob?.cancel()
        silentReconcileJob = null

        val uid = _currentUserId.value
        val now = System.currentTimeMillis()

        viewModelScope.launch {
            if (uid > 0) {
                val user = userDao.getUserById(uid)
                val cleanMobile = user?.mobile?.trim() ?: ""

                // 2. Wipe all local Room tables for this user and guest 0L
                repository.clearAllForUser(uid)
                debtDao.clearAllForUser(uid)
                savingsGoalDao.clearAllForUser(uid)
                khatDao.clearAllForUser(uid)
                accountDao.clearAll()

                repository.clearAllForUser(0L)
                debtDao.clearAllForUser(0L)
                savingsGoalDao.clearAllForUser(0L)
                khatDao.clearAllForUser(0L)

                // 3. Reset local preferences and set anti-resurrection watermark with commit()
                prefs.edit()
                    .putFloat("pref_monthly_budget_user_$uid", 0f)
                    .putFloat("pref_daily_target_user_$uid", 500f)
                    .putLong("pref_settings_updated_at_user_$uid", now)
                    .putLong("pref_all_data_cleared_at_user_$uid", now)
                    .putLong("pref_all_data_cleared_at_mobile_$cleanMobile", now)
                    .putLong("pref_last_cloud_sync_time", now)
                    .commit()

                // 4. Wipe cloud Firestore database so remote has empty vault with allDataClearedAt
                if (user != null) {
                    try {
                        cloudSyncManager.clearAllUserFinancialCloudData(user)
                    } catch (e: Exception) {
                        Log.e("ExpenseViewModel", "Error clearing user cloud data: ${e.localizedMessage}")
                    }
                }
            } else {
                repository.clearAllTransactions()
                debtDao.clearAll()
                savingsGoalDao.clearAll()
                khatDao.clearAll()
                accountDao.clearAll()
                repository.clearAllForUser(0L)
                debtDao.clearAllForUser(0L)
                savingsGoalDao.clearAllForUser(0L)
                khatDao.clearAllForUser(0L)
                prefs.edit()
                    .putFloat("pref_monthly_budget", 0f)
                    .putFloat("pref_daily_target", 500f)
                    .putLong("pref_all_data_cleared_guest", now)
                    .commit()
            }

            // 5. Reset UI and state
            _monthlyBudget.value = 0.0
            _dailySpendingTarget.value = 500.0
            _feedbackMessage.value = if (_isEnglish.value) {
                "All account data permanently cleared from device and cloud."
            } else {
                "সব হিসাবের তথ্য ফোন ও ক্লাউড থেকে স্থায়ীভাবে মুছে ফেলা হয়েছে।"
            }
            onComplete?.invoke()
        }
    }

    fun clearFeedback() {
        _feedbackMessage.value = null
    }

    // --- DEBTS (ধার-দেনা খাতা) ACTIONS ---
    fun addDebt(
        personName: String,
        amount: Double,
        type: String,
        dueDate: String = "",
        note: String = "",
        deductFromMain: Boolean = false,
        khatId: Long? = null,
        khatName: String = "",
        onSuccess: (() -> Unit)? = null,
        onError: ((String) -> Unit)? = null
    ) {
        val uid = _currentUserId.value
        viewModelScope.launch {
            try {
                val cleanPerson = personName.trim()
                val cleanDue = dueDate.trim()
                val cleanNote = note.trim()
                val now = System.currentTimeMillis()
                val debtSyncId = java.util.UUID.randomUUID().toString()
                val txRef = "DEBT_$debtSyncId"

                // If user chose to deduct from/add to a specific custom Khat
                if (khatId != null && khatId > 0L) {
                    val khat = khatDao.getKhatById(khatId)
                    if (khat != null) {
                        if (type == "RECEIVE") {
                            // Lending money: money leaves this Khat
                            if (amount > khat.allocatedAmount) {
                                val err = com.example.ui.util.AppLocale.insufficientKhatBalance(
                                    _isEnglish.value,
                                    khat.name,
                                    formatTakaSafe(khat.allocatedAmount)
                                )
                                onError?.invoke(err)
                                _feedbackMessage.value = err
                                return@launch
                            }
                            val newAlloc = maxOf(0.0, khat.allocatedAmount - amount)
                            khatDao.updateKhat(khat.copy(allocatedAmount = newAlloc, updatedAt = now))

                            val txTitle = if (_isEnglish.value) "Loan Given: $cleanPerson" else "ধার দেওয়া হয়েছে: $cleanPerson"
                            val txNote = if (cleanNote.isNotBlank()) {
                                if (_isEnglish.value) "Deducted from khat '${khat.name}' ($cleanNote)" else "'${khat.name}' খাত থেকে কর্তন ($cleanNote)"
                            } else {
                                if (_isEnglish.value) "Deducted from khat '${khat.name}' for debt" else "'${khat.name}' খাত থেকে ধারের জন্য কর্তন"
                            }
                            val tx = TransactionEntity(
                                userId = if (uid > 0) uid else 0L,
                                title = txTitle,
                                amount = amount,
                                type = "EXPENSE",
                                category = "ধার প্রদান",
                                note = txNote,
                                transactionRef = txRef,
                                khatId = khat.id,
                                khatName = khat.name,
                                timestamp = now
                            )
                            database.transactionDao().insertTransaction(tx)
                        } else {
                            // Borrowing money: money enters this Khat
                            val newAlloc = khat.allocatedAmount + amount
                            khatDao.updateKhat(khat.copy(allocatedAmount = newAlloc, updatedAt = now))

                            val txTitle = if (_isEnglish.value) "Loan Borrowed: $cleanPerson" else "ধার নেওয়া হয়েছে: $cleanPerson"
                            val txNote = if (cleanNote.isNotBlank()) {
                                if (_isEnglish.value) "Added to khat '${khat.name}' ($cleanNote)" else "'${khat.name}' খাতে যোগ ($cleanNote)"
                            } else {
                                if (_isEnglish.value) "Added to khat '${khat.name}' from debt" else "ধার থেকে '${khat.name}' খাতে যোগ"
                            }
                            val tx = TransactionEntity(
                                userId = if (uid > 0) uid else 0L,
                                title = txTitle,
                                amount = amount,
                                type = "INCOME",
                                category = "ধার গ্রহণ",
                                note = txNote,
                                transactionRef = txRef,
                                khatId = khat.id,
                                khatName = khat.name,
                                timestamp = now
                            )
                            database.transactionDao().insertTransaction(tx)
                        }
                    }
                } else if (deductFromMain) {
                    val isOboshisto = khatName.contains("অবশিষ্ট") || khatName.contains("Oboshisto")
                    val availableBal = if (isOboshisto) oboshistoBalance.value else netBalance.value
                    if (type == "RECEIVE") {
                        if (amount > availableBal + 0.001) {
                            val err = if (_isEnglish.value) "Insufficient account balance! Available: ${formatTakaSafe(availableBal)}"
                            else "অ্যাকাউন্টে পর্যাপ্ত ব্যালেন্স নেই! বর্তমান ব্যালেন্স: ${formatTakaSafe(availableBal)}"
                            onError?.invoke(err)
                            _feedbackMessage.value = err
                            return@launch
                        }
                        // Giving loan from Main / Oboshisto Balance (Expense)
                        val txTitle = if (_isEnglish.value) "Loan Given: $cleanPerson" else "ধার দেওয়া হয়েছে: $cleanPerson"
                        val txNote = if (cleanNote.isNotBlank()) {
                            if (isOboshisto) (if (_isEnglish.value) "Deducted from remaining balance ($cleanNote)" else "অবশিষ্ট ব্যালেন্স থেকে কর্তন ($cleanNote)")
                            else (if (_isEnglish.value) "Deducted from main balance ($cleanNote)" else "মূল ব্যালেন্স থেকে কর্তন ($cleanNote)")
                        } else {
                            if (isOboshisto) (if (_isEnglish.value) "Deducted from remaining balance for debt" else "ধার-দেনা: অবশিষ্ট ব্যালেন্স থেকে কর্তন")
                            else (if (_isEnglish.value) "Deducted from main balance for debt" else "ধার-দেনা: মূল ব্যালেন্স থেকে কর্তন")
                        }
                        val tx = TransactionEntity(
                            userId = if (uid > 0) uid else 0L,
                            title = txTitle,
                            amount = amount,
                            type = "EXPENSE",
                            category = "ধার প্রদান",
                            note = txNote,
                            transactionRef = txRef,
                            timestamp = now
                        )
                        database.transactionDao().insertTransaction(tx)
                    } else {
                        // Borrowing money into Main / Oboshisto Balance (Income)
                        val txTitle = if (_isEnglish.value) "Loan Borrowed: $cleanPerson" else "ধার নেওয়া হয়েছে: $cleanPerson"
                        val txNote = if (cleanNote.isNotBlank()) {
                            if (isOboshisto) (if (_isEnglish.value) "Added to remaining balance ($cleanNote)" else "অবশিষ্ট ব্যালেন্সে যোগ ($cleanNote)")
                            else (if (_isEnglish.value) "Added to main balance ($cleanNote)" else "মূল ব্যালেন্সে যোগ ($cleanNote)")
                        } else {
                            if (isOboshisto) (if (_isEnglish.value) "Added to remaining balance from debt" else "ধার-দেনা: অবশিষ্ট ব্যালেন্সে যোগ")
                            else (if (_isEnglish.value) "Added to main balance from debt" else "ধার-দেনা: মূল ব্যালেন্সে যোগ")
                        }
                        val tx = TransactionEntity(
                            userId = if (uid > 0) uid else 0L,
                            title = txTitle,
                            amount = amount,
                            type = "INCOME",
                            category = "ধার গ্রহণ",
                            note = txNote,
                            transactionRef = txRef,
                            timestamp = now
                        )
                        database.transactionDao().insertTransaction(tx)
                    }
                }

                debtDao.insertDebt(
                    DebtEntity(
                        userId = if (uid > 0) uid else 0L,
                        syncId = debtSyncId,
                        personName = cleanPerson,
                        amount = amount,
                        type = type,
                        dueDate = cleanDue,
                        note = cleanNote,
                        khatId = if (khatId != null && khatId > 0L) khatId else null,
                        khatName = if (khatId != null && khatId > 0L) khatName else (if (deductFromMain && khatName.isNotBlank()) khatName else ""),
                        deductedFromMain = deductFromMain,
                        timestamp = now,
                        updatedAt = now
                    )
                )
                triggerBackgroundAutoSync()

                val sourceTag = when {
                    !khatName.isNullOrBlank() -> if (_isEnglish.value) " (Khat: $khatName)" else " (খাত: $khatName)"
                    deductFromMain -> if (_isEnglish.value) " (Main Balance)" else " (মূল ব্যালেন্স)"
                    else -> ""
                }

                _feedbackMessage.value = if (_isEnglish.value) {
                    val typeEn = if (type == "RECEIVE") "receivable" else "payable"
                    "$cleanPerson's $typeEn record saved$sourceTag"
                } else {
                    val typeBn = if (type == "RECEIVE") "পাওনা" else "দেনা"
                    "$cleanPerson-এর $typeBn হিসাব সংরক্ষণ করা হয়েছে$sourceTag"
                }
                onSuccess?.invoke()
            } catch (e: Exception) {
                val err = e.localizedMessage ?: "Failed to save debt"
                onError?.invoke(err)
                _feedbackMessage.value = err
            }
        }
    }

    fun toggleDebtSettled(debt: DebtEntity) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            debtDao.setSettled(debt.id, !debt.isSettled, now)
            triggerBackgroundAutoSync()
            _feedbackMessage.value = if (_isEnglish.value) {
                val stEn = if (!debt.isSettled) "marked as settled" else "marked as active"
                "${debt.personName}'s debt $stEn"
            } else {
                val stBn = if (!debt.isSettled) "পরিশোধিত হিসেবে চিহ্নিত হয়েছে" else "পুনরায় অপূর্ণ চিহ্নিত হয়েছে"
                "${debt.personName}-এর হিসাব $stBn"
            }
        }
    }

    fun recordDebtPayment(
        debt: DebtEntity,
        paymentAmount: Double,
        isFullSettlement: Boolean,
        adjustMainAccount: Boolean,
        note: String = "",
        isEnglish: Boolean
    ) {
        viewModelScope.launch {
            try {
                val now = System.currentTimeMillis()
                val remainingBefore = debt.remainingAmount
                val actualPay = if (isFullSettlement) remainingBefore else minOf(paymentAmount, remainingBefore)

                if (actualPay <= 0.0) {
                    _feedbackMessage.value = if (isEnglish) "Invalid payment amount" else "পরিশোধের পরিমাণ সঠিক নয়"
                    return@launch
                }

                // Balance Guard: When paying off borrowed debt from Main Balance or Khat, ensure balance never goes below zero
                if (adjustMainAccount && debt.type == "PAY") {
                    if (debt.khatId != null && debt.khatId > 0L) {
                        val khat = khatDao.getKhatById(debt.khatId)
                        if (khat != null) {
                            if (khat.allocatedAmount <= 0.0) {
                                val err = if (isEnglish) "Cannot repay: '${khat.name}' khat balance is ৳0!"
                                else "পরিশোধ সম্ভব নয়: '${khat.name}' খাতে কোনো ব্যালেন্স নেই (৳০)! ব্যালেন্স মাইনাস হতে পারবে না।"
                                _feedbackMessage.value = err
                                return@launch
                            }
                            if (actualPay > khat.allocatedAmount + 0.001) {
                                val err = if (isEnglish) "Cannot repay: Insufficient balance in '${khat.name}'! Available: ${formatTakaSafe(khat.allocatedAmount)}"
                                else "পরিশোধ সম্ভব নয়: '${khat.name}' খাতে পর্যাপ্ত টাকা নেই! বর্তমান ব্যালেন্স: ${formatTakaSafe(khat.allocatedAmount)}, আপনি সর্বোচ্চ এই পরিমাণ দিতে পারবেন।"
                                _feedbackMessage.value = err
                                return@launch
                            }
                            val newAlloc = maxOf(0.0, khat.allocatedAmount - actualPay)
                            khatDao.updateKhat(khat.copy(allocatedAmount = newAlloc, updatedAt = now))
                        }
                    } else {
                        val currentMainBal = netBalance.value
                        if (currentMainBal <= 0.0) {
                            val err = if (isEnglish) "Cannot repay: Main account balance is 0 or negative!"
                            else "পরিশোধ সম্ভব নয়: অ্যাকাউন্টে কোনো ব্যালেন্স নেই (৳০)! ব্যালেন্স মাইনাস হতে পারবে না।"
                            _feedbackMessage.value = err
                            return@launch
                        }
                        if (actualPay > currentMainBal + 0.001) {
                            val err = if (isEnglish) "Cannot repay: Insufficient balance! Available: ${formatTakaSafe(currentMainBal)}, attempted: ${formatTakaSafe(actualPay)}"
                            else "পরিশোধ সম্ভব নয়: অ্যাকাউন্টে পর্যাপ্ত টাকা নেই! বর্তমান ব্যালেন্স: ${formatTakaSafe(currentMainBal)}, আপনি সর্বোচ্চ এই পরিমাণ পরিশোধ করতে পারবেন।"
                            _feedbackMessage.value = err
                            return@launch
                        }
                    }
                } else if (adjustMainAccount && debt.type == "RECEIVE" && debt.khatId != null && debt.khatId > 0L) {
                    val khat = khatDao.getKhatById(debt.khatId)
                    if (khat != null) {
                        val newAlloc = khat.allocatedAmount + actualPay
                        khatDao.updateKhat(khat.copy(allocatedAmount = newAlloc, updatedAt = now))
                    }
                }

                val newPaidAmount = if (isFullSettlement) debt.amount else (debt.paidAmount + actualPay)
                val newIsSettled = isFullSettlement || (newPaidAmount >= debt.amount - 0.01)
                val remainingAfter = if (newIsSettled) 0.0 else maxOf(0.0, debt.amount - newPaidAmount)

                val paymentRecord = com.example.data.model.DebtPaymentRecord(
                    amount = actualPay,
                    timestamp = now,
                    note = note.ifBlank {
                        if (newIsSettled) (if (isEnglish) "Full Settlement" else "সম্পূর্ণ পরিশোধ")
                        else (if (isEnglish) "Installment / Partial Payment" else "কিস্তি / আংশিক পরিশোধ")
                    },
                    remainingAfter = remainingAfter
                )
                val updatedHistory = debt.addPaymentRecord(paymentRecord)

                val updatedDebt = debt.copy(
                    paidAmount = newPaidAmount,
                    paymentHistoryJson = updatedHistory,
                    isSettled = newIsSettled,
                    updatedAt = now
                )
                debtDao.updateDebt(updatedDebt)

                // Optional Main Balance / Account Adjustment
                if (adjustMainAccount) {
                    val isPay = debt.type == "PAY" // "PAY" = money goes out (EXPENSE)
                    val txType = if (isPay) "EXPENSE" else "INCOME" // "RECEIVE" = money comes in (INCOME)
                    val category = if (isPay) {
                        if (newIsSettled) (if (isEnglish) "Debt Repayment" else "ধার পরিশোধ")
                        else (if (isEnglish) "Debt Installment" else "ধারের কিস্তি পরিশোধ")
                    } else {
                        if (newIsSettled) (if (isEnglish) "Debt Collection" else "ধার আদায়")
                        else (if (isEnglish) "Debt Installment Received" else "ধারের কিস্তি আদায়")
                    }
                    val title = if (isPay) {
                        if (newIsSettled) (if (isEnglish) "Debt Repaid: ${debt.personName}" else "ধার পরিশোধ: ${debt.personName}")
                        else (if (isEnglish) "Debt Installment: ${debt.personName}" else "ধারের কিস্তি: ${debt.personName}")
                    } else {
                        if (newIsSettled) (if (isEnglish) "Debt Collected: ${debt.personName}" else "ধার আদায়: ${debt.personName}")
                        else (if (isEnglish) "Debt Installment: ${debt.personName}" else "ধারের কিস্তি আদায়: ${debt.personName}")
                    }
                    val txNote = note.ifBlank {
                        if (newIsSettled) (if (isEnglish) "Full settlement for ${debt.personName}" else "ধার-দেনা সম্পূর্ণ নিষ্পত্তি: ${debt.personName}")
                        else (if (isEnglish) "Installment for ${debt.personName} (Remaining: ${formatTakaSafe(remainingAfter)})" else "${debt.personName}-এর কিস্তি (অবশিষ্ট বাকি: ${formatTakaSafe(remainingAfter)})")
                    }

                    val uid = _currentUserId.value
                    val entity = TransactionEntity(
                        userId = if (uid > 0) uid else 0L,
                        title = title,
                        amount = actualPay,
                        type = txType,
                        category = category,
                        note = txNote,
                        transactionRef = "DEBT_PAY_${debt.syncId}_${now}",
                        khatId = debt.khatId,
                        khatName = debt.khatName,
                        timestamp = now
                    )
                    database.transactionDao().insertTransaction(entity)
                }

                triggerBackgroundAutoSync()

                _feedbackMessage.value = if (isEnglish) {
                    if (newIsSettled) "${debt.personName}'s debt fully settled (${formatTakaSafe(actualPay)})"
                    else "Installment of ${formatTakaSafe(actualPay)} recorded for ${debt.personName}. Remaining: ${formatTakaSafe(remainingAfter)}"
                } else {
                    if (newIsSettled) "${debt.personName}-এর সম্পূর্ণ ${formatTakaSafe(actualPay)} পরিশোধ সম্পন্ন হয়েছে"
                    else "${debt.personName}-এর ${formatTakaSafe(actualPay)} কিস্তি সফলভাবে জমা হয়েছে। অবশিষ্ট বাকি: ${formatTakaSafe(remainingAfter)}"
                }
            } catch (e: Exception) {
                _feedbackMessage.value = e.localizedMessage ?: "Failed to record payment"
            }
        }
    }

    fun settleDebtWithAdjustment(
        debt: DebtEntity,
        adjustMainAccount: Boolean,
        isEnglish: Boolean
    ) {
        recordDebtPayment(
            debt = debt,
            paymentAmount = debt.remainingAmount,
            isFullSettlement = true,
            adjustMainAccount = adjustMainAccount,
            note = if (isEnglish) "Full Settlement" else "সম্পূর্ণ পরিশোধ",
            isEnglish = isEnglish
        )
    }

    fun unsettleDebt(debt: DebtEntity, isEnglish: Boolean) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            debtDao.updateDebt(
                debt.copy(
                    isSettled = false,
                    paidAmount = if (debt.paidAmount >= debt.amount) 0.0 else debt.paidAmount,
                    updatedAt = now
                )
            )
            triggerBackgroundAutoSync()
            _feedbackMessage.value = if (isEnglish) {
                "${debt.personName}'s debt marked as active (unsettled)"
            } else {
                "${debt.personName}-এর হিসাব পুনরায় সক্রিয় হিসেবে চিহ্নিত হয়েছে"
            }
        }
    }

    fun deleteDebt(debt: DebtEntity) {
        val uid = _currentUserId.value
        viewModelScope.launch {
            try {
                val now = System.currentTimeMillis()

                // 1. Mark debt as deleted in debtDao
                debtDao.markDeleted(debt.id, now)

                // 2. Adjust Khat balance if this debt was linked to a custom Khat
                if (debt.khatId != null && debt.khatId > 0L) {
                    val khat = khatDao.getKhatById(debt.khatId)
                    if (khat != null) {
                        val restoredAlloc = if (debt.type == "RECEIVE") {
                            // Loan given was deducted from this Khat -> refund/restore it back
                            khat.allocatedAmount + debt.amount
                        } else {
                            // Loan received was added to this Khat -> deduct it back
                            maxOf(0.0, khat.allocatedAmount - debt.amount)
                        }
                        khatDao.updateKhat(khat.copy(allocatedAmount = restoredAlloc, updatedAt = now))
                    }
                }

                // 3. Delete linked transaction history from home page & restore main/khat balance
                // Delete any transaction related to this debt (original loan entry + any installment/settlement transactions)
                val pattern = "%${debt.syncId}%"
                val linkedTxs = database.transactionDao().getTransactionsByRefPattern(pattern)
                if (linkedTxs.isNotEmpty()) {
                    database.transactionDao().markDeletedByRefPattern(pattern, now)
                } else {
                    // Fallback matching for any legacy transaction created without transactionRef:
                    val userTxs = database.transactionDao().getActiveTransactionsByUser(if (uid > 0) uid else debt.userId)
                    val matchingTx = userTxs.firstOrNull { tx ->
                        val catMatches = (tx.category == "ধার প্রদান" || tx.category == "ধার গ্রহণ" ||
                                          tx.category == "Loan Given" || tx.category == "Loan Received")
                        val personMatches = tx.title.contains(debt.personName, ignoreCase = true)
                        val amountMatches = Math.abs(tx.amount - debt.amount) < 0.01
                        val khatMatches = if (debt.khatId != null && debt.khatId > 0L) {
                            tx.khatId == debt.khatId
                        } else {
                            tx.khatId == null && debt.deductedFromMain
                        }
                        catMatches && personMatches && amountMatches && khatMatches
                    }
                    matchingTx?.let { tx ->
                        database.transactionDao().markDeleted(tx.id, now)
                    }
                }

                // Also if this debt had any legacy settlement transaction without syncId in ref, remove it as well
                val userTxs = database.transactionDao().getActiveTransactionsByUser(if (uid > 0) uid else debt.userId)
                val settlementTx = userTxs.firstOrNull { tx ->
                    (tx.category == "ধার পরিশোধ" || tx.category == "ধার আদায়" ||
                     tx.category == "Debt Repayment" || tx.category == "Debt Collection" ||
                     tx.category == "ধারের কিস্তি পরিশোধ" || tx.category == "ধারের কিস্তি আদায়") &&
                    tx.title.contains(debt.personName, ignoreCase = true) &&
                    (tx.transactionRef.isEmpty() || !tx.transactionRef.contains(debt.syncId))
                }
                settlementTx?.let { tx ->
                    database.transactionDao().markDeleted(tx.id, now)
                }

                triggerBackgroundAutoSync()

                val msg = if (_isEnglish.value) {
                    if (debt.khatId != null && debt.khatName.isNotBlank()) {
                        "Debt deleted & ${formatTakaSafe(debt.amount)} adjusted in '${debt.khatName}'"
                    } else if (debt.deductedFromMain) {
                        "Debt deleted & ${formatTakaSafe(debt.amount)} adjusted in main balance"
                    } else {
                        "Debt record deleted"
                    }
                } else {
                    if (debt.khatId != null && debt.khatName.isNotBlank()) {
                        "ধার মুছে ফেলা হয়েছে এবং '${debt.khatName}' খাতে ${formatTakaSafe(debt.amount)} সমন্বয় করা হয়েছে"
                    } else if (debt.deductedFromMain) {
                        "ধার মুছে ফেলা হয়েছে এবং মূল ব্যালেন্সে ${formatTakaSafe(debt.amount)} সমন্বয় করা হয়েছে"
                    } else {
                        "ধার-দেনা হিসাব মুছে ফেলা হয়েছে"
                    }
                }
                _feedbackMessage.value = msg
            } catch (e: Exception) {
                _feedbackMessage.value = e.localizedMessage ?: "Failed to delete debt"
            }
        }
    }

    // --- SAVINGS GOALS (সঞ্চয় লক্ষ্য) ACTIONS ---
    fun addSavingsGoal(
        title: String,
        targetAmount: Double,
        note: String = ""
    ) {
        val uid = _currentUserId.value
        viewModelScope.launch {
            savingsGoalDao.insertGoal(
                SavingsGoalEntity(
                    userId = if (uid > 0) uid else 0L,
                    title = title,
                    targetAmount = targetAmount,
                    savedAmount = 0.0,
                    note = note
                )
            )
            triggerBackgroundAutoSync()
            _feedbackMessage.value = if (_isEnglish.value) "New savings goal '$title' added!" else "নতুন সঞ্চয় লক্ষ্য '$title' যোগ হয়েছে!"
        }
    }

    fun addDepositToGoal(goal: SavingsGoalEntity, depositAmount: Double) {
        addDepositToGoalWithAdjustment(goal, depositAmount, adjustMainAccount = false, note = "", isEnglish = _isEnglish.value)
    }

    fun addDepositToGoalWithAdjustment(
        goal: SavingsGoalEntity,
        depositAmount: Double,
        adjustMainAccount: Boolean,
        note: String = "",
        isEnglish: Boolean = false
    ) {
        if (depositAmount <= 0) return
        if (goal.isCompleted || (goal.targetAmount > 0 && goal.savedAmount >= goal.targetAmount)) {
            _feedbackMessage.value = if (isEnglish) {
                "This savings goal is completed and locked. No further deposits allowed."
            } else {
                "এই সঞ্চয় লক্ষ্যটি ইতিমধ্যে সম্পন্ন এবং লক করা হয়েছে। আর জমা করা সম্ভব নয়।"
            }
            return
        }
        val remaining = max(0.0, goal.targetAmount - goal.savedAmount)
        if (remaining <= 0.0 || depositAmount > remaining) {
            _feedbackMessage.value = if (isEnglish) {
                "Deposit cannot exceed remaining amount (${formatTakaSafe(remaining)})"
            } else {
                "ডিপোজিট বাকি থাকা পরিমাণের (${formatTakaSafe(remaining)}) বেশি হতে পারবে না"
            }
            return
        }
        if (adjustMainAccount) {
            val currentBal = netBalance.value
            if (depositAmount > currentBal + 0.001) {
                _feedbackMessage.value = if (isEnglish) {
                    "Insufficient account balance! Available: ${formatTakaSafe(currentBal)}"
                } else {
                    "অ্যাকাউন্টে পর্যাপ্ত ব্যালেন্স নেই! বর্তমান ব্যালেন্স: ${formatTakaSafe(currentBal)}"
                }
                return
            }
        }
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val newSaved = goal.savedAmount + depositAmount
            val isNowCompleted = (goal.targetAmount > 0 && newSaved >= goal.targetAmount) || goal.isCompleted
            val txRecord = com.example.data.model.SavingsTransaction(
                amount = depositAmount,
                type = "DEPOSIT",
                timestamp = now,
                note = note.trim(),
                balanceAfter = newSaved
            )
            val updatedHistory = goal.addHistoryRecord(txRecord)
            savingsGoalDao.updateSavingsProgress(
                id = goal.id,
                savedAmount = newSaved,
                isCompleted = isNowCompleted,
                historyJson = updatedHistory,
                updatedAt = now
            )

            if (adjustMainAccount) {
                val title = if (isEnglish) "Savings Deposit: ${goal.title}" else "সঞ্চয় জমা: ${goal.title}"
                val category = if (isEnglish) "Savings Deposit" else "সঞ্চয় জমা"
                val txNote = note.ifBlank {
                    if (isEnglish) "Transferred to savings goal '${goal.title}'" else "সঞ্চয় লক্ষ্য '${goal.title}'-এ জমা স্থানান্তর"
                }
                val uid = _currentUserId.value
                val entity = TransactionEntity(
                    userId = if (uid > 0) uid else 0L,
                    title = title,
                    amount = depositAmount,
                    type = "EXPENSE",
                    category = category,
                    note = txNote,
                    timestamp = now
                )
                database.transactionDao().insertTransaction(entity)
            }
            triggerBackgroundAutoSync()

            _feedbackMessage.value = if (isNowCompleted) {
                if (isEnglish) {
                    "🎉 Goal Reached! '${goal.title}' target of ${formatTakaSafe(goal.targetAmount)} completed!"
                } else {
                    "🎉 অভিনন্দন! '${goal.title}' লক্ষ্যমাত্রা ${formatTakaSafe(goal.targetAmount)} সফলভাবে সম্পন্ন হয়েছে!"
                }
            } else {
                if (isEnglish) {
                    "${formatTakaSafe(depositAmount)} added to '${goal.title}'! 🎉"
                } else {
                    "'${goal.title}' লক্ষ্যে ${formatTakaSafe(depositAmount)} জমা হয়েছে! 🎉"
                }
            }
        }
    }

    fun withdrawFromGoalWithAdjustment(
        goal: SavingsGoalEntity,
        withdrawAmount: Double,
        adjustMainAccount: Boolean,
        note: String = "",
        isEnglish: Boolean = false
    ) {
        if (withdrawAmount <= 0) return
        if (withdrawAmount > goal.savedAmount) {
            _feedbackMessage.value = if (isEnglish) {
                "Withdrawal cannot exceed saved balance (${formatTakaSafe(goal.savedAmount)})"
            } else {
                "উত্তোলন মোট সঞ্চিত টাকার (${formatTakaSafe(goal.savedAmount)}) চেয়ে বেশি হতে পারবে না"
            }
            return
        }
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val newSaved = maxOf(0.0, goal.savedAmount - withdrawAmount)
            // Once a goal is completed, it permanently maintains completed status
            val isNowCompleted = goal.isCompleted || (goal.targetAmount > 0 && newSaved >= goal.targetAmount)
            val txRecord = com.example.data.model.SavingsTransaction(
                amount = withdrawAmount,
                type = "WITHDRAWAL",
                timestamp = now,
                note = note.trim(),
                balanceAfter = newSaved
            )
            val updatedHistory = goal.addHistoryRecord(txRecord)
            savingsGoalDao.updateSavingsProgress(
                id = goal.id,
                savedAmount = newSaved,
                isCompleted = isNowCompleted,
                historyJson = updatedHistory,
                updatedAt = now
            )

            if (adjustMainAccount) {
                val title = if (isEnglish) "Savings Withdrawal: ${goal.title}" else "সঞ্চয় উত্তোলন: ${goal.title}"
                val category = if (isEnglish) "Savings Withdrawal" else "সঞ্চয় উত্তোলন"
                val txNote = note.ifBlank {
                    if (isEnglish) "Withdrawn from savings goal '${goal.title}'" else "সঞ্চয় লক্ষ্য '${goal.title}' থেকে উত্তোলন"
                }
                val uid = _currentUserId.value
                val entity = TransactionEntity(
                    userId = if (uid > 0) uid else 0L,
                    title = title,
                    amount = withdrawAmount,
                    type = "INCOME",
                    category = category,
                    note = txNote,
                    timestamp = now
                )
                database.transactionDao().insertTransaction(entity)
            }
            triggerBackgroundAutoSync()

            _feedbackMessage.value = if (newSaved <= 0.0 && isNowCompleted) {
                if (isEnglish) {
                    "Goal '${goal.title}' fully withdrawn. Goal is now closed/read-only."
                } else {
                    "'${goal.title}' সম্পূর্ণ উত্তোলন সম্পন্ন হয়েছে। লক্ষ্যটি এখন সমাপ্ত ও রিড-অনলি।"
                }
            } else {
                if (isEnglish) {
                    "${formatTakaSafe(withdrawAmount)} withdrawn from '${goal.title}'"
                } else {
                    "'${goal.title}' লক্ষ্য থেকে ${formatTakaSafe(withdrawAmount)} উত্তোলন করা হয়েছে"
                }
            }
        }
    }

    fun deleteSavingsGoal(goal: SavingsGoalEntity, refundToMain: Boolean = true) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            if (refundToMain && goal.savedAmount > 0.0) {
                val title = if (_isEnglish.value) "Refund: Goal '${goal.title}'" else "ফেরত: সঞ্চয় লক্ষ্য '${goal.title}'"
                val category = if (_isEnglish.value) "Savings Refund" else "সঞ্চয় ফেরত"
                val txNote = if (_isEnglish.value) "Returned remaining savings of ${formatTakaSafe(goal.savedAmount)} to main account"
                    else "মুছে ফেলা লক্ষ্য থেকে ${formatTakaSafe(goal.savedAmount)} মূল অ্যাকাউন্টে ফেরত"
                val uid = _currentUserId.value
                val entity = TransactionEntity(
                    userId = if (uid > 0) uid else 0L,
                    title = title,
                    amount = goal.savedAmount,
                    type = "INCOME",
                    category = category,
                    note = txNote,
                    timestamp = now
                )
                database.transactionDao().insertTransaction(entity)
            }
            savingsGoalDao.markDeleted(goal.id, now)
            triggerBackgroundAutoSync()
            _feedbackMessage.value = if (_isEnglish.value) "Savings goal deleted" else "সঞ্চয় লক্ষ্য মুছে ফেলা হয়েছে"
        }
    }

    // Reports and Exports
    fun exportAllTransactionsText(): String {
        val list = transactions.value
        val sb = StringBuilder()
        val sdf = SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.US)
        sb.append("📋 দৈনিক হিসাব - সমগ্র লেনদেনের তালিকা\n")
        sb.append("মোট লেনদেন: ${list.size}টি\n")
        sb.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n")
        list.forEach { item ->
            val dateStr = sdf.format(Date(item.timestamp))
            val sign = if (item.type == "EXPENSE") "[-] " else "[+] "
            sb.append("$sign${item.title} - ${formatTakaSafe(item.amount)} (${item.category})\n")
            sb.append("   তারিখ: $dateStr\n")
            if (item.note.isNotBlank()) sb.append("   নোট: ${item.note}\n")
            sb.append("\n")
        }
        sb.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n")
        sb.append("মোট আয়: ${formatTakaSafe(totalIncome.value)}\n")
        sb.append("মোট খরচ: ${formatTakaSafe(totalExpense.value)}\n")
        sb.append("বর্তমান ব্যালেন্স: ${formatTakaSafe(netBalance.value)}\n")
        return sb.toString()
    }

    fun generateMonthlyReportText(): String {
        val cal = _selectedMonthCalendar.value
        val monthNamesBn = arrayOf(
            "জানুয়ারি", "ফেব্রুয়ারি", "মার্চ", "এপ্রিল", "মে", "জুন",
            "জুলাই", "আগস্ট", "সেপ্টেম্বর", "অক্টোবর", "নভেম্বর", "ডিসেম্বর"
        )
        val monthLabel = "${monthNamesBn[cal.get(Calendar.MONTH)]} ${cal.get(Calendar.YEAR)}"

        val inc = monthlyIncome.value
        val exp = monthlyExpense.value
        val sav = monthlySavings.value
        val rate = monthlySavingsRate.value
        val avg = monthlyDailyAverage.value
        val breakdown = monthlyCategoryBreakdown.value

        val sb = StringBuilder()
        sb.append("📊 দৈনিক হিসাব - মাসিক খরচের প্রতিবেদন ($monthLabel)\n")
        sb.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n")
        sb.append("💵 মোট আয়: ${formatTakaSafe(inc)}\n")
        sb.append("🛒 মোট খরচ: ${formatTakaSafe(exp)}\n")
        sb.append("💰 নিট সঞ্চয়: ${formatTakaSafe(sav)} (সঞ্চয়ের হার: ${String.format(Locale.US, "%.1f", rate)}%)\n")
        sb.append("📅 দৈনিক গড় খরচ: ${formatTakaSafe(avg)}\n")
        if (monthlyBudget.value > 0) {
            sb.append("🎯 মাসিক বাজেট: ${formatTakaSafe(monthlyBudget.value)}\n")
        }
        sb.append("\n📌 খাতভিত্তিক ব্যয়ের তালিকা:\n")
        if (breakdown.isEmpty()) {
            sb.append("  (এই মাসে কোনো খরচের হিসাব নেই)\n")
        } else {
            breakdown.forEach { item ->
                val pctStr = String.format(Locale.US, "%.1f", item.percentage * 100)
                sb.append("  • ${item.category}: ${formatTakaSafe(item.amount)} ($pctStr%)\n")
            }
        }
        sb.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n")
        sb.append("সহজ ও নির্ভরযোগ্য দৈনিক ব্যক্তিগত হিসাব")

        return sb.toString()
    }

    override fun onCleared() {
        super.onCleared()
        autoSyncJob?.cancel()
        silentReconcileJob?.cancel()
        cloudSyncManager.stopRealtimeSyncListener()
    }
}

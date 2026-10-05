package com.example.data.sync

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import com.example.data.database.AppDatabase
import com.example.data.model.DebtEntity
import com.example.data.model.KhatEntity
import com.example.data.model.SavingsGoalEntity
import com.example.data.model.TransactionEntity
import com.example.data.model.UserEntity
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

sealed class SyncState {
    object Idle : SyncState()
    object Syncing : SyncState()
    data class Synced(val timestamp: Long) : SyncState()
    data class Error(val message: String) : SyncState()
}

/**
 * CloudSyncManager:
 * - End-to-end Zero-Knowledge AES-256-GCM encryption.
 * - Single-document encrypted vault per user (optimal for Firebase free tier: 1 write per sync, 1 read per login).
 * - Offline-first: local Room database always works seamlessly without internet.
 * - Deterministic, non-destructive entity-level reconciliation by unique syncId and timestamps.
 * - Stale data overwrite prevention: cloud state is checked before uploading.
 * - Real-time synchronization via Firestore snapshot listeners without noisy messages.
 * - Atomic account deletion handling across all devices.
 */
class CloudSyncManager private constructor(private val context: Context) {

    private val database = AppDatabase.getDatabase(context)
    private val transactionDao = database.transactionDao()
    private val debtDao = database.debtDao()
    private val savingsGoalDao = database.savingsGoalDao()
    private val khatDao = database.khatDao()
    private val userDao = database.userDao()
    private val accountDao = database.accountDao()

    private val prefs = context.getSharedPreferences("daily_hishab_prefs", Context.MODE_PRIVATE)

    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    private val _lastSyncTimestamp = MutableStateFlow(prefs.getLong("pref_last_cloud_sync_time", 0L))
    val lastSyncTimestamp: StateFlow<Long> = _lastSyncTimestamp.asStateFlow()

    private val syncMutex = Mutex()

    /**
     * Unique client device identifier to distinguish changes originating from this device
     * versus changes originating from other devices.
     */
    val deviceId: String
        get() {
            var id = prefs.getString("pref_client_device_id", null)
            if (id.isNullOrBlank()) {
                id = UUID.randomUUID().toString()
                prefs.edit().putString("pref_client_device_id", id).apply()
            }
            return id
        }

    private var activeSnapshotListener: ListenerRegistration? = null
    private var currentListeningMobile: String? = null

    @Volatile
    private var lastKnownCloudSnapshot: DocumentSnapshot? = null

    @Volatile
    private var lastSyncedPayloadHash: String? = null

    private var lastNetworkSyncAttempt = 0L

    /**
     * Callback invoked whenever device regains internet access to perform auto-reconciliation.
     */
    var onNetworkAvailableSync: (() -> Unit)? = null

    init {
        registerNetworkMonitoring()
    }

    private fun registerNetworkMonitoring() {
        try {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()

            connectivityManager?.registerNetworkCallback(request, object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    super.onAvailable(network)
                    val now = System.currentTimeMillis()
                    // Throttle: avoid spamming sync on frequent network toggles (min 45s cooldown)
                    if (now - lastNetworkSyncAttempt < 45_000L) {
                        return
                    }
                    lastNetworkSyncAttempt = now
                    Log.d(TAG, "Network connection available, triggering auto-reconciliation")
                    CoroutineScope(Dispatchers.Main).launch {
                        try {
                            onNetworkAvailableSync?.invoke()
                        } catch (e: Exception) {
                            Log.w(TAG, "Network auto-sync invocation error: ${e.localizedMessage}")
                        }
                    }
                }
            })
        } catch (e: Exception) {
            Log.w(TAG, "Failed to register network callback: ${e.localizedMessage}")
        }
    }

    companion object {
        private const val TAG = "CloudSyncManager"

        @Volatile
        private var INSTANCE: CloudSyncManager? = null

        fun getInstance(context: Context): CloudSyncManager {
            return INSTANCE ?: synchronized(this) {
                val instance = CloudSyncManager(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }

    private var cachedFirestore: FirebaseFirestore? = null

    fun isFirebaseInitialized(): Boolean {
        return try {
            if (FirebaseApp.getApps(context).isEmpty()) {
                try {
                    FirebaseApp.initializeApp(context)
                } catch (e: Exception) {
                    val options = com.google.firebase.FirebaseOptions.Builder()
                        .setApplicationId("1:186444542906:android:b0e067d489c7f13cea6383")
                        .setApiKey("AIzaSyA1_n-gixhipVuXcYaTSA5johIIafA1q7c")
                        .setProjectId("daily-hishab-5bac1")
                        .setStorageBucket("daily-hishab-5bac1.firebasestorage.app")
                        .build()
                    FirebaseApp.initializeApp(context, options)
                }
            }
            FirebaseApp.getApps(context).isNotEmpty()
        } catch (e: Exception) {
            Log.e(TAG, "Firebase initialization check failed: ${e.localizedMessage}")
            false
        }
    }

    private fun getFirestoreOrNull(): FirebaseFirestore? {
        if (cachedFirestore != null) return cachedFirestore
        return try {
            if (isFirebaseInitialized()) {
                FirebaseFirestore.getInstance().also { cachedFirestore = it }
            } else {
                null
            }
        } catch (e: Exception) {
            Log.w(TAG, "Firestore not initialized: ${e.localizedMessage}")
            null
        }
    }

    fun isRealtimeListenerActive(): Boolean = activeSnapshotListener != null

    /**
     * Check if a phone number already exists in Firestore.
     * Enforces the 1-account-per-phone rule.
     */
    suspend fun doesPhoneExistInCloud(mobile: String): Boolean = withContext(Dispatchers.IO) {
        val cleanMobile = mobile.trim()
        if (cleanMobile.isEmpty()) return@withContext false
        val firestore = getFirestoreOrNull() ?: return@withContext false
        try {
            kotlinx.coroutines.withTimeout(15000L) {
                val doc = firestore.collection("users").document(cleanMobile).get().await()
                doc.exists() && doc.getBoolean("isDeleted") != true
            }
        } catch (e: Exception) {
            Log.w(TAG, "Check phone exists failed: ${e.localizedMessage}")
            return@withContext false
        }
    }

    /**
     * Registers a new user account in Cloud Firestore before starting sync.
     * Ensures document exists with isDeleted = false.
     */
    suspend fun registerNewAccountInCloud(
        mobile: String,
        name: String,
        password: String,
        monthlyBudget: Double,
        dailyTarget: Double
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val cleanMobile = mobile.trim()
        val firestore = getFirestoreOrNull() ?: return@withContext Result.failure(Exception("FIREBASE_NOT_CONFIGURED"))

        try {
            val root = JSONObject()
            root.put("name", name.trim())
            root.put("mobile", cleanMobile)
            root.put("createdAt", System.currentTimeMillis())
            root.put("monthlyBudget", monthlyBudget)
            root.put("dailyTarget", dailyTarget)
            root.put("settingsUpdatedAt", System.currentTimeMillis())
            root.put("transactions", JSONArray())
            root.put("debts", JSONArray())
            root.put("savingsGoals", JSONArray())

            val plainJson = root.toString()
            val aesKey = CryptoEngine.deriveKey(password, cleanMobile)
            val encryptedVault = CryptoEngine.encrypt(plainJson, aesKey)
            val authHash = CryptoEngine.hashPasswordForAuth(password, cleanMobile)

            val now = System.currentTimeMillis()
            val docData = hashMapOf(
                "authHash" to authHash,
                "encryptedVault" to encryptedVault,
                "lastSyncTime" to now,
                "lastModifiedBy" to deviceId,
                "dataVersion" to 1L,
                "isDeleted" to false
            )

            kotlinx.coroutines.withTimeout(20000L) {
                firestore.collection("users").document(cleanMobile).set(docData).await()
            }
            _lastSyncTimestamp.value = now
            prefs.edit().putLong("pref_last_cloud_sync_time", now).apply()

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "registerNewAccountInCloud error: ${e.localizedMessage}", e)
            Result.failure(e)
        }
    }

    /**
     * Completely and permanently clears all account-owned financial records
     * (transactions, debts, savings goals, budget/target stats) from the Cloud Firestore vault.
     * Keeps the user credentials/auth intact so the user can remain logged in,
     * but writes an empty encrypted vault stamped with allDataClearedAt.
     */
    suspend fun clearAllUserFinancialCloudData(user: UserEntity): Result<Unit> = withContext(Dispatchers.IO) {
        val firestore = getFirestoreOrNull() ?: return@withContext Result.failure(Exception("FIREBASE_NOT_CONFIGURED"))
        val cleanMobile = user.mobile.trim()

        syncMutex.withLock {
            try {
                _syncState.value = SyncState.Syncing
                val now = System.currentTimeMillis()

                val root = JSONObject()
                root.put("name", user.name)
                root.put("mobile", cleanMobile)
                root.put("createdAt", user.createdAt)
                root.put("monthlyBudget", 0.0)
                root.put("dailyTarget", 500.0)
                root.put("settingsUpdatedAt", now)
                root.put("allDataClearedAt", now)
                root.put("transactions", JSONArray())
                root.put("debts", JSONArray())
                root.put("savingsGoals", JSONArray())
                root.put("khats", JSONArray())

                val plainJson = root.toString()
                val aesKey = CryptoEngine.deriveKey(user.password, cleanMobile)
                val encryptedVault = CryptoEngine.encrypt(plainJson, aesKey)
                val authHash = CryptoEngine.hashPasswordForAuth(user.password, cleanMobile)

                var prevVersion = 1L
                try {
                    val cloudDoc = firestore.collection("users").document(cleanMobile).get().await()
                    if (cloudDoc.exists()) {
                        prevVersion = cloudDoc.getLong("dataVersion") ?: 1L
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "cloudDoc get before clear warning: ${e.localizedMessage}")
                }

                val docData = hashMapOf(
                    "authHash" to authHash,
                    "encryptedVault" to encryptedVault,
                    "lastSyncTime" to now,
                    "lastModifiedBy" to deviceId,
                    "dataVersion" to (prevVersion + 1L),
                    "isDeleted" to false,
                    "allDataClearedAt" to now
                )

                firestore.collection("users").document(cleanMobile).set(docData, SetOptions.merge()).await()

                _lastSyncTimestamp.value = now
                prefs.edit()
                    .putLong("pref_last_cloud_sync_time", now)
                    .putLong("pref_all_data_cleared_at_user_${user.id}", now)
                    .apply()

                _syncState.value = SyncState.Synced(now)
                Result.success(Unit)
            } catch (e: Exception) {
                Log.e(TAG, "clearAllUserFinancialCloudData error: ${e.localizedMessage}", e)
                _syncState.value = SyncState.Error(e.localizedMessage ?: "Clear cloud data failed")
                Result.failure(e)
            }
        }
    }

    /**
     * Wipes out all cloud data for the given mobile number.
     * Sets isDeleted = true and clears the vault, then removes document.
     */
    suspend fun deleteUserCloudData(mobile: String): Result<Unit> = withContext(Dispatchers.IO) {
        val cleanMobile = mobile.trim()
        val firestore = getFirestoreOrNull() ?: return@withContext Result.failure(Exception("FIREBASE_NOT_CONFIGURED"))

        try {
            _syncState.value = SyncState.Syncing

            val now = System.currentTimeMillis()
            try {
                firestore.collection("users").document(cleanMobile).set(
                    hashMapOf(
                        "isDeleted" to true,
                        "lastSyncTime" to now,
                        "lastModifiedBy" to deviceId,
                        "encryptedVault" to ""
                    ),
                    SetOptions.merge()
                ).await()
            } catch (e: Exception) {
                Log.w(TAG, "isDeleted flag write warning: ${e.localizedMessage}")
            }

            try {
                firestore.collection("users").document(cleanMobile).delete().await()
            } catch (e: Exception) {
                Log.w(TAG, "Delete doc warning: ${e.localizedMessage}")
            }

            _syncState.value = SyncState.Idle
            Result.success(Unit)
        } catch (e: Exception) {
            _syncState.value = SyncState.Error(e.localizedMessage ?: "Delete failed")
            Result.failure(e)
        }
    }

    /**
     * Safely releases an old phone number document in Firestore when a user updates
     * their profile phone number, WITHOUT setting isDeleted=true so no account deletion
     * is ever triggered on this or any synced device.
     */
    suspend fun removeOldPhoneDocWithoutDeletion(oldMobile: String): Result<Unit> = withContext(Dispatchers.IO) {
        val cleanMobile = oldMobile.trim()
        if (cleanMobile.isBlank()) return@withContext Result.success(Unit)
        val firestore = getFirestoreOrNull() ?: return@withContext Result.failure(Exception("FIREBASE_NOT_CONFIGURED"))
        try {
            firestore.collection("users").document(cleanMobile).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.w(TAG, "removeOldPhoneDocWithoutDeletion warning: ${e.localizedMessage}")
            Result.failure(e)
        }
    }

    /**
     * Try to sign in from Cloud when user is logging in on a new device.
     */
    suspend fun tryCloudLogin(
        mobile: String,
        enteredPassword: String
    ): Result<UserEntity> = withContext(Dispatchers.IO) {
        val cleanMobile = mobile.trim()
        val firestore = getFirestoreOrNull() ?: return@withContext Result.failure(Exception("FIREBASE_NOT_CONFIGURED"))

        try {
            _syncState.value = SyncState.Syncing
            val userDoc = kotlinx.coroutines.withTimeout(20000L) {
                firestore.collection("users").document(cleanMobile).get().await()
            }

            if (!userDoc.exists() || userDoc.getBoolean("isDeleted") == true) {
                _syncState.value = SyncState.Idle
                return@withContext Result.failure(Exception("USER_NOT_FOUND"))
            }

            val storedAuthHash = userDoc.getString("authHash")
            val expectedAuthHash = CryptoEngine.hashPasswordForAuth(enteredPassword, cleanMobile)

            if (storedAuthHash != null) {
                if (storedAuthHash != expectedAuthHash) {
                    _syncState.value = SyncState.Idle
                    return@withContext Result.failure(Exception("INCORRECT_PASSWORD"))
                }
            } else {
                val cloudPassword = userDoc.getString("password") ?: ""
                if (cloudPassword != enteredPassword.trim()) {
                    _syncState.value = SyncState.Idle
                    return@withContext Result.failure(Exception("INCORRECT_PASSWORD"))
                }
            }

            val encryptedVault = userDoc.getString("encryptedVault")
            val aesKey = CryptoEngine.deriveKey(enteredPassword, cleanMobile)

            var userName = "User"
            var createdAt = System.currentTimeMillis()
            var monthlyBudget = 0.0
            var dailyTarget = 500.0
            var remoteDataClearedAt = 0L
            val restoredTransactions = mutableListOf<TransactionEntity>()
            val restoredDebts = mutableListOf<DebtEntity>()
            val restoredGoals = mutableListOf<SavingsGoalEntity>()
            val restoredKhats = mutableListOf<KhatEntity>()

            if (!encryptedVault.isNullOrBlank()) {
                val decryptedJson = CryptoEngine.decrypt(encryptedVault, aesKey)
                if (decryptedJson.isNotBlank()) {
                    val root = JSONObject(decryptedJson)
                    userName = root.optString("name", "User")
                    createdAt = root.optLong("createdAt", System.currentTimeMillis())
                    monthlyBudget = root.optDouble("monthlyBudget", 0.0)
                    dailyTarget = root.optDouble("dailyTarget", 500.0)
                    remoteDataClearedAt = root.optLong("allDataClearedAt", 0L)

                    val txArray = root.optJSONArray("transactions")
                    if (txArray != null) {
                        for (i in 0 until txArray.length()) {
                            val item = txArray.getJSONObject(i)
                            val isDel = item.optBoolean("isDeleted", false)
                            val itemUpdatedAt = item.optLong("updatedAt", item.optLong("timestamp", System.currentTimeMillis()))
                            if (!isDel && (remoteDataClearedAt == 0L || itemUpdatedAt > remoteDataClearedAt)) {
                                val sId = item.optString("syncId").ifBlank { UUID.randomUUID().toString() }
                                restoredTransactions.add(
                                    TransactionEntity(
                                        id = 0,
                                        syncId = sId,
                                        userId = 0,
                                        title = item.optString("title"),
                                        amount = item.optDouble("amount", 0.0),
                                        type = item.optString("type", "EXPENSE"),
                                        category = item.optString("category", "অন্যান্য"),
                                        paymentMethod = item.optString("paymentMethod", "ক্যাশ"),
                                        timestamp = item.optLong("timestamp", System.currentTimeMillis()),
                                        note = item.optString("note", ""),
                                        encryptedNote = item.optString("encryptedNote", ""),
                                        isEncrypted = item.optBoolean("isEncrypted", false),
                                        fee = item.optDouble("fee", 0.0),
                                        transactionRef = item.optString("transactionRef", ""),
                                        khatId = item.optLong("khatId", 0L).let { if (it > 0L) it else null },
                                        khatName = item.optString("khatName", ""),
                                        updatedAt = itemUpdatedAt,
                                        isDeleted = false
                                    )
                                )
                            }
                        }
                    }

                    val khatArray = root.optJSONArray("khats")
                    if (khatArray != null) {
                        for (i in 0 until khatArray.length()) {
                            val item = khatArray.getJSONObject(i)
                            val isDel = item.optBoolean("isDeleted", false)
                            val itemUpdatedAt = item.optLong("updatedAt", item.optLong("timestamp", System.currentTimeMillis()))
                            if (!isDel && (remoteDataClearedAt == 0L || itemUpdatedAt > remoteDataClearedAt)) {
                                val sId = item.optString("syncId").ifBlank { UUID.randomUUID().toString() }
                                restoredKhats.add(
                                    KhatEntity(
                                        id = 0,
                                        syncId = sId,
                                        userId = 0,
                                        name = item.optString("name"),
                                        allocatedAmount = item.optDouble("allocatedAmount", 0.0),
                                        colorHex = item.optString("colorHex", "#4F46E5"),
                                        iconName = item.optString("iconName", "folder"),
                                        timestamp = item.optLong("timestamp", System.currentTimeMillis()),
                                        updatedAt = itemUpdatedAt,
                                        isDeleted = false
                                    )
                                )
                            }
                        }
                    }

                    val debtArray = root.optJSONArray("debts")
                    if (debtArray != null) {
                        for (i in 0 until debtArray.length()) {
                            val item = debtArray.getJSONObject(i)
                            val isDel = item.optBoolean("isDeleted", false)
                            val itemUpdatedAt = item.optLong("updatedAt", item.optLong("timestamp", System.currentTimeMillis()))
                            if (!isDel && (remoteDataClearedAt == 0L || itemUpdatedAt > remoteDataClearedAt)) {
                                val sId = item.optString("syncId").ifBlank { UUID.randomUUID().toString() }
                                restoredDebts.add(
                                    DebtEntity(
                                        id = 0,
                                        syncId = sId,
                                        userId = 0,
                                        personName = item.optString("personName"),
                                        amount = item.optDouble("amount", 0.0),
                                        type = item.optString("type", "RECEIVE"),
                                        dueDate = item.optString("dueDate", ""),
                                        note = item.optString("note", ""),
                                        isSettled = item.optBoolean("isSettled", false),
                                        khatId = item.optLong("khatId", 0L).let { if (it > 0L) it else null },
                                        khatName = item.optString("khatName", ""),
                                        deductedFromMain = item.optBoolean("deductedFromMain", false),
                                        paidAmount = item.optDouble("paidAmount", if (item.optBoolean("isSettled", false)) item.optDouble("amount", 0.0) else 0.0),
                                        paymentHistoryJson = item.optString("paymentHistoryJson", "[]"),
                                        timestamp = item.optLong("timestamp", System.currentTimeMillis()),
                                        updatedAt = itemUpdatedAt,
                                        isDeleted = false
                                    )
                                )
                            }
                        }
                    }

                    val goalArray = root.optJSONArray("savingsGoals")
                    if (goalArray != null) {
                        for (i in 0 until goalArray.length()) {
                            val item = goalArray.getJSONObject(i)
                            val isDel = item.optBoolean("isDeleted", false)
                            val itemUpdatedAt = item.optLong("updatedAt", item.optLong("timestamp", System.currentTimeMillis()))
                            if (!isDel && (remoteDataClearedAt == 0L || itemUpdatedAt > remoteDataClearedAt)) {
                                val sId = item.optString("syncId").ifBlank { UUID.randomUUID().toString() }
                                restoredGoals.add(
                                    SavingsGoalEntity(
                                        id = 0,
                                        syncId = sId,
                                        userId = 0,
                                        title = item.optString("title"),
                                        targetAmount = item.optDouble("targetAmount", 0.0),
                                        savedAmount = item.optDouble("savedAmount", 0.0),
                                        note = item.optString("note", ""),
                                        isCompleted = item.optBoolean("isCompleted", false),
                                        historyJson = item.optString("historyJson", "[]"),
                                        timestamp = item.optLong("timestamp", System.currentTimeMillis()),
                                        updatedAt = itemUpdatedAt,
                                        isDeleted = false
                                    )
                                )
                            }
                        }
                    }
                }
            }

            val localUser = UserEntity(
                id = 0,
                name = userName,
                mobile = cleanMobile,
                password = enteredPassword.trim(),
                createdAt = createdAt
            )
            val newUserId = userDao.upsertUser(localUser)

            prefs.edit()
                .putString("user_profile_name", userName)
                .putFloat("pref_monthly_budget_user_$newUserId", monthlyBudget.toFloat())
                .putFloat("pref_daily_target_user_$newUserId", dailyTarget.toFloat())
                .apply()

            if (remoteDataClearedAt > 0L) {
                prefs.edit().putLong("pref_all_data_cleared_at_user_$newUserId", remoteDataClearedAt).apply()
            }

            if (restoredTransactions.isNotEmpty()) {
                val scopedTx = restoredTransactions.map { it.copy(userId = newUserId) }
                transactionDao.insertAll(scopedTx)
            }
            if (restoredDebts.isNotEmpty()) {
                val scopedDebts = restoredDebts.map { it.copy(userId = newUserId) }
                debtDao.insertAll(scopedDebts)
            }
            if (restoredGoals.isNotEmpty()) {
                val scopedGoals = restoredGoals.map { it.copy(userId = newUserId) }
                savingsGoalDao.insertAll(scopedGoals)
            }
            if (restoredKhats.isNotEmpty()) {
                val scopedKhats = restoredKhats.map { it.copy(userId = newUserId) }
                scopedKhats.forEach { khatDao.insertKhat(it) }
            }

            val finalUser = userDao.getUserById(newUserId) ?: localUser.copy(id = newUserId)
            val now = userDoc.getLong("lastSyncTime") ?: System.currentTimeMillis()
            _lastSyncTimestamp.value = now
            prefs.edit().putLong("pref_last_cloud_sync_time", now).apply()
            _syncState.value = SyncState.Synced(now)

            Result.success(finalUser)
        } catch (e: Exception) {
            _syncState.value = SyncState.Error(e.localizedMessage ?: "Sync failed")
            Result.failure(e)
        }
    }

    /**
     * Deterministically reconciles local Room database from remote decrypted vault.
     * Records are matched by unique syncId.
     * - If remote.updatedAt >= local.updatedAt: remote wins.
     * - If remote.isDeleted: local record is removed.
     * - If local.updatedAt > remote.updatedAt: local record is retained.
     */
    suspend fun reconcileLocalDatabaseFromVault(
        mobile: String,
        password: String,
        encryptedVault: String,
        remoteSyncTime: Long
    ): Boolean = withContext(Dispatchers.IO) {
        syncMutex.withLock {
            try {
                val cleanMobile = mobile.trim()
                val localUser = userDao.getUserByMobile(cleanMobile) ?: return@withContext false
                val currentUserId = localUser.id

                val aesKey = CryptoEngine.deriveKey(password, cleanMobile)
                val decryptedJson = CryptoEngine.decrypt(encryptedVault, aesKey)
                if (decryptedJson.isBlank()) {
                    Log.w(TAG, "Failed to decrypt remote vault for $cleanMobile")
                    return@withContext false
                }

                val root = JSONObject(decryptedJson)
                val remoteName = root.optString("name", localUser.name)
                val monthlyBudget = root.optDouble("monthlyBudget", 0.0)
                val dailyTarget = root.optDouble("dailyTarget", 500.0)
                val settingsUpdatedAt = root.optLong("settingsUpdatedAt", 0L)
                val remoteDataClearedAt = root.optLong("allDataClearedAt", 0L)
                val localDataClearedAt = maxOf(
                    prefs.getLong("pref_all_data_cleared_at_user_$currentUserId", 0L),
                    prefs.getLong("pref_all_data_cleared_at_mobile_$cleanMobile", 0L)
                )
                val effectiveClearedThreshold = maxOf(remoteDataClearedAt, localDataClearedAt)

                if (remoteDataClearedAt > 0L && remoteDataClearedAt >= localDataClearedAt) {
                    transactionDao.clearAllForUser(currentUserId)
                    debtDao.clearAllForUser(currentUserId)
                    savingsGoalDao.clearAllForUser(currentUserId)
                    khatDao.clearAllForUser(currentUserId)
                    accountDao.clearAll()
                    transactionDao.clearAllForUser(0L)
                    debtDao.clearAllForUser(0L)
                    savingsGoalDao.clearAllForUser(0L)
                    khatDao.clearAllForUser(0L)
                    prefs.edit()
                        .putLong("pref_all_data_cleared_at_user_$currentUserId", remoteDataClearedAt)
                        .putLong("pref_all_data_cleared_at_mobile_$cleanMobile", remoteDataClearedAt)
                        .commit()
                } else if (localDataClearedAt > 0L && localDataClearedAt > remoteDataClearedAt) {
                    // Local wipe is newer than remote vault - preserve wipe state!
                    transactionDao.clearAllForUser(currentUserId)
                    debtDao.clearAllForUser(currentUserId)
                    savingsGoalDao.clearAllForUser(currentUserId)
                    khatDao.clearAllForUser(currentUserId)
                    accountDao.clearAll()
                }

                // 1. Reconcile Transactions
                val localTxList = transactionDao.getAllTransactionsIncludingDeleted(currentUserId)
                val localTxMap = localTxList.associateBy { it.syncId }.toMutableMap()
                val txArray = root.optJSONArray("transactions")
                if (txArray != null) {
                    for (i in 0 until txArray.length()) {
                        val item = txArray.getJSONObject(i)
                        val sId = item.optString("syncId").ifBlank {
                            "tx_${item.optLong("timestamp")}_${item.optString("title").hashCode()}"
                        }
                        val rTimestamp = item.optLong("timestamp", 0L)
                        val rUpdatedAt = item.optLong("updatedAt", rTimestamp)
                        val rIsDeleted = item.optBoolean("isDeleted", false)

                        // Anti-resurrection guard: ignore records created/updated before or at data wipe
                        if (effectiveClearedThreshold > 0L && (rUpdatedAt <= effectiveClearedThreshold || rTimestamp <= effectiveClearedThreshold)) {
                            continue
                        }

                        val local = localTxMap[sId]
                        if (local == null) {
                            if (!rIsDeleted) {
                                transactionDao.insertTransaction(
                                    TransactionEntity(
                                        id = 0,
                                        syncId = sId,
                                        userId = currentUserId,
                                        title = item.optString("title"),
                                        amount = item.optDouble("amount", 0.0),
                                        type = item.optString("type", "EXPENSE"),
                                        category = item.optString("category", "অন্যান্য"),
                                        paymentMethod = item.optString("paymentMethod", "ক্যাশ"),
                                        timestamp = if (rTimestamp > 0L) rTimestamp else System.currentTimeMillis(),
                                        note = item.optString("note", ""),
                                        encryptedNote = item.optString("encryptedNote", ""),
                                        isEncrypted = item.optBoolean("isEncrypted", false),
                                        fee = item.optDouble("fee", 0.0),
                                        transactionRef = item.optString("transactionRef", ""),
                                        khatId = item.optLong("khatId", 0L).let { if (it > 0L) it else null },
                                        khatName = item.optString("khatName", ""),
                                        updatedAt = rUpdatedAt,
                                        isDeleted = false
                                    )
                                )
                            }
                        } else {
                            if (rUpdatedAt >= local.updatedAt) {
                                if (rIsDeleted) {
                                    transactionDao.deleteById(local.id)
                                } else {
                                    transactionDao.insertTransaction(
                                        TransactionEntity(
                                            id = local.id,
                                            syncId = sId,
                                            userId = currentUserId,
                                            title = item.optString("title"),
                                            amount = item.optDouble("amount", 0.0),
                                            type = item.optString("type", "EXPENSE"),
                                            category = item.optString("category", "অন্যান্য"),
                                            paymentMethod = item.optString("paymentMethod", "ক্যাশ"),
                                            timestamp = item.optLong("timestamp", local.timestamp),
                                            note = item.optString("note", ""),
                                            encryptedNote = item.optString("encryptedNote", ""),
                                            isEncrypted = item.optBoolean("isEncrypted", false),
                                            fee = item.optDouble("fee", 0.0),
                                            transactionRef = item.optString("transactionRef", ""),
                                            khatId = item.optLong("khatId", 0L).let { if (it > 0L) it else null },
                                            khatName = item.optString("khatName", ""),
                                            updatedAt = rUpdatedAt,
                                            isDeleted = false
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                // 2. Reconcile Debts
                val localDebtList = debtDao.getAllDebtsIncludingDeleted(currentUserId)
                val localDebtMap = localDebtList.associateBy { it.syncId }.toMutableMap()
                val debtArray = root.optJSONArray("debts")
                if (debtArray != null) {
                    for (i in 0 until debtArray.length()) {
                        val item = debtArray.getJSONObject(i)
                        val sId = item.optString("syncId").ifBlank {
                            "debt_${item.optLong("timestamp")}_${item.optString("personName").hashCode()}"
                        }
                        val rTimestamp = item.optLong("timestamp", 0L)
                        val rUpdatedAt = item.optLong("updatedAt", rTimestamp)
                        val rIsDeleted = item.optBoolean("isDeleted", false)

                        // Anti-resurrection guard: ignore records created/updated before or at data wipe
                        if (effectiveClearedThreshold > 0L && (rUpdatedAt <= effectiveClearedThreshold || rTimestamp <= effectiveClearedThreshold)) {
                            continue
                        }

                        val local = localDebtMap[sId]
                        if (local == null) {
                            if (!rIsDeleted) {
                                debtDao.insertDebt(
                                    DebtEntity(
                                        id = 0,
                                        syncId = sId,
                                        userId = currentUserId,
                                        personName = item.optString("personName"),
                                        amount = item.optDouble("amount", 0.0),
                                        type = item.optString("type", "RECEIVE"),
                                        dueDate = item.optString("dueDate", ""),
                                        note = item.optString("note", ""),
                                        isSettled = item.optBoolean("isSettled", false),
                                        khatId = item.optLong("khatId", 0L).let { if (it > 0L) it else null },
                                        khatName = item.optString("khatName", ""),
                                        deductedFromMain = item.optBoolean("deductedFromMain", false),
                                        paidAmount = item.optDouble("paidAmount", if (item.optBoolean("isSettled", false)) item.optDouble("amount", 0.0) else 0.0),
                                        paymentHistoryJson = item.optString("paymentHistoryJson", "[]"),
                                        timestamp = if (rTimestamp > 0L) rTimestamp else System.currentTimeMillis(),
                                        updatedAt = rUpdatedAt,
                                        isDeleted = false
                                    )
                                )
                            }
                        } else {
                            if (rUpdatedAt >= local.updatedAt) {
                                if (rIsDeleted) {
                                    debtDao.deleteDebt(local)
                                } else {
                                    debtDao.insertDebt(
                                        DebtEntity(
                                            id = local.id,
                                            syncId = sId,
                                            userId = currentUserId,
                                            personName = item.optString("personName"),
                                            amount = item.optDouble("amount", 0.0),
                                            type = item.optString("type", "RECEIVE"),
                                            dueDate = item.optString("dueDate", ""),
                                            note = item.optString("note", ""),
                                            isSettled = item.optBoolean("isSettled", false),
                                            khatId = item.optLong("khatId", 0L).let { if (it > 0L) it else null },
                                            khatName = item.optString("khatName", ""),
                                            deductedFromMain = item.optBoolean("deductedFromMain", false),
                                            paidAmount = item.optDouble("paidAmount", if (item.optBoolean("isSettled", false)) item.optDouble("amount", 0.0) else 0.0),
                                            paymentHistoryJson = item.optString("paymentHistoryJson", "[]"),
                                            timestamp = item.optLong("timestamp", local.timestamp),
                                            updatedAt = rUpdatedAt,
                                            isDeleted = false
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                // 3. Reconcile Savings Goals
                val localGoalList = savingsGoalDao.getAllGoalsIncludingDeleted(currentUserId)
                val localGoalMap = localGoalList.associateBy { it.syncId }.toMutableMap()
                val goalArray = root.optJSONArray("savingsGoals")
                if (goalArray != null) {
                    for (i in 0 until goalArray.length()) {
                        val item = goalArray.getJSONObject(i)
                        val sId = item.optString("syncId").ifBlank {
                            "goal_${item.optLong("timestamp")}_${item.optString("title").hashCode()}"
                        }
                        val rTimestamp = item.optLong("timestamp", 0L)
                        val rUpdatedAt = item.optLong("updatedAt", rTimestamp)
                        val rIsDeleted = item.optBoolean("isDeleted", false)
                        val rIsCompleted = item.optBoolean("isCompleted", false)
                        val rHistoryJson = item.optString("historyJson", "[]")

                        // Anti-resurrection guard: ignore records created/updated before or at data wipe
                        if (effectiveClearedThreshold > 0L && (rUpdatedAt <= effectiveClearedThreshold || rTimestamp <= effectiveClearedThreshold)) {
                            continue
                        }

                        val local = localGoalMap[sId]
                        if (local == null) {
                            if (!rIsDeleted) {
                                savingsGoalDao.insertGoal(
                                    SavingsGoalEntity(
                                        id = 0,
                                        syncId = sId,
                                        userId = currentUserId,
                                        title = item.optString("title"),
                                        targetAmount = item.optDouble("targetAmount", 0.0),
                                        savedAmount = item.optDouble("savedAmount", 0.0),
                                        note = item.optString("note", ""),
                                        isCompleted = rIsCompleted,
                                        historyJson = rHistoryJson,
                                        timestamp = if (rTimestamp > 0L) rTimestamp else System.currentTimeMillis(),
                                        updatedAt = rUpdatedAt,
                                        isDeleted = false
                                    )
                                )
                            }
                        } else {
                            if (rUpdatedAt >= local.updatedAt) {
                                if (rIsDeleted) {
                                    savingsGoalDao.deleteGoal(local)
                                } else {
                                    savingsGoalDao.insertGoal(
                                        SavingsGoalEntity(
                                            id = local.id,
                                            syncId = sId,
                                            userId = currentUserId,
                                            title = item.optString("title"),
                                            targetAmount = item.optDouble("targetAmount", 0.0),
                                            savedAmount = item.optDouble("savedAmount", 0.0),
                                            note = item.optString("note", ""),
                                            isCompleted = rIsCompleted,
                                            historyJson = rHistoryJson,
                                            timestamp = item.optLong("timestamp", local.timestamp),
                                            updatedAt = rUpdatedAt,
                                            isDeleted = false
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                // 3.5 Reconcile Khats
                val localKhatList = khatDao.getAllKhatsIncludingDeleted(currentUserId)
                val localKhatMap = localKhatList.associateBy { it.syncId }.toMutableMap()
                val khatArray = root.optJSONArray("khats")
                if (khatArray != null) {
                    for (i in 0 until khatArray.length()) {
                        val item = khatArray.getJSONObject(i)
                        val sId = item.optString("syncId").ifBlank {
                            "khat_${item.optLong("timestamp")}_${item.optString("name").hashCode()}"
                        }
                        val rUpdatedAt = item.optLong("updatedAt", item.optLong("timestamp", System.currentTimeMillis()))
                        val rIsDeleted = item.optBoolean("isDeleted", false)

                        if (effectiveClearedThreshold > 0L && rUpdatedAt <= effectiveClearedThreshold) {
                            continue
                        }

                        val local = localKhatMap[sId]
                        if (local == null) {
                            if (!rIsDeleted) {
                                khatDao.insertKhat(
                                    KhatEntity(
                                        id = 0,
                                        syncId = sId,
                                        userId = currentUserId,
                                        name = item.optString("name"),
                                        allocatedAmount = item.optDouble("allocatedAmount", 0.0),
                                        colorHex = item.optString("colorHex", "#4F46E5"),
                                        iconName = item.optString("iconName", "folder"),
                                        timestamp = item.optLong("timestamp", System.currentTimeMillis()),
                                        updatedAt = rUpdatedAt,
                                        isDeleted = false
                                    )
                                )
                            }
                        } else {
                            if (rUpdatedAt >= local.updatedAt) {
                                if (rIsDeleted) {
                                    khatDao.markDeleted(local.id, rUpdatedAt)
                                } else {
                                    khatDao.updateKhat(
                                        local.copy(
                                            name = item.optString("name"),
                                            allocatedAmount = item.optDouble("allocatedAmount", 0.0),
                                            colorHex = item.optString("colorHex", "#4F46E5"),
                                            iconName = item.optString("iconName", "folder"),
                                            updatedAt = rUpdatedAt,
                                            isDeleted = false
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                // 4. Reconcile Settings
                val localSettingsUpdatedAt = prefs.getLong("pref_settings_updated_at_user_$currentUserId", 0L)
                if (settingsUpdatedAt >= localSettingsUpdatedAt || (remoteDataClearedAt > 0L && remoteDataClearedAt >= localDataClearedAt)) {
                    if (localUser.name != remoteName && remoteName.isNotBlank()) {
                        userDao.updateUser(localUser.copy(name = remoteName))
                    }
                    prefs.edit()
                        .putString("user_profile_name", remoteName)
                        .putFloat("pref_monthly_budget_user_$currentUserId", monthlyBudget.toFloat())
                        .putFloat("pref_daily_target_user_$currentUserId", dailyTarget.toFloat())
                        .putLong("pref_settings_updated_at_user_$currentUserId", settingsUpdatedAt)
                        .apply()
                }

                lastSyncedPayloadHash = CryptoEngine.sha256(decryptedJson)
                _lastSyncTimestamp.value = remoteSyncTime
                prefs.edit().putLong("pref_last_cloud_sync_time", remoteSyncTime).apply()
                _syncState.value = SyncState.Synced(remoteSyncTime)
                true
            } catch (e: Exception) {
                Log.e(TAG, "reconcileLocalDatabaseFromVault error: ${e.localizedMessage}", e)
                false
            }
        }
    }

    /**
     * Push local changes to Cloud with conflict-prevention.
     * Queries Room directly for the current user's records.
     * Before uploading, checks if Cloud has newer data. If so, reconciles first!
     * Optimization: Checks if payload has changed and avoids redundant reads/writes.
     */
    suspend fun syncAllUserDataToCloud(
        userId: Long,
        user: UserEntity,
        monthlyBudget: Double,
        dailyTarget: Double,
        knownCloudDoc: DocumentSnapshot? = null
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val firestore = getFirestoreOrNull() ?: return@withContext Result.failure(Exception("FIREBASE_NOT_CONFIGURED"))

        syncMutex.withLock {
            try {
                _syncState.value = SyncState.Syncing
                val mobile = user.mobile.trim()

                // Optimization: reuse known or cached cloud snapshot to avoid redundant reads
                val cachedSnapshot = lastKnownCloudSnapshot
                val cloudDoc: DocumentSnapshot = knownCloudDoc
                    ?: if (cachedSnapshot != null && cachedSnapshot.id == mobile) {
                        cachedSnapshot
                    } else {
                        val fetched = firestore.collection("users").document(mobile).get().await()
                        lastKnownCloudSnapshot = fetched
                        fetched
                    }

                if (cloudDoc.exists()) {
                    if (cloudDoc.getBoolean("isDeleted") == true) {
                        _syncState.value = SyncState.Idle
                        return@withContext Result.failure(Exception("ACCOUNT_DELETED"))
                    }
                    val remoteSyncTime = cloudDoc.getLong("lastSyncTime") ?: 0L
                    val localSyncTime = _lastSyncTimestamp.value
                    val remoteModifiedBy = cloudDoc.getString("lastModifiedBy") ?: ""

                    // If remote document was updated by another device newer than our last known sync time, reconcile first!
                    if (remoteModifiedBy != deviceId && remoteSyncTime > localSyncTime) {
                        val remoteVault = cloudDoc.getString("encryptedVault")
                        if (!remoteVault.isNullOrBlank()) {
                            reconcileLocalDatabaseFromVault(mobile, user.password, remoteVault, remoteSyncTime)
                        }
                    }
                }

                val localDataClearedAt = prefs.getLong("pref_all_data_cleared_at_user_$userId", 0L)

                // 2. Query the freshest state directly from Room (including tombstones)
                val currentTxs = transactionDao.getAllTransactionsIncludingDeleted(userId)
                    .filter { localDataClearedAt == 0L || it.updatedAt > localDataClearedAt }
                val currentDebts = debtDao.getAllDebtsIncludingDeleted(userId)
                    .filter { localDataClearedAt == 0L || it.updatedAt > localDataClearedAt }
                val currentGoals = savingsGoalDao.getAllGoalsIncludingDeleted(userId)
                    .filter { localDataClearedAt == 0L || it.updatedAt > localDataClearedAt }
                val currentKhats = khatDao.getAllKhatsIncludingDeleted(userId)
                    .filter { localDataClearedAt == 0L || it.updatedAt > localDataClearedAt }
                val settingsUpdatedAt = prefs.getLong("pref_settings_updated_at_user_$userId", System.currentTimeMillis())

                // 3. Build JSON payload
                val root = JSONObject()
                root.put("name", user.name)
                root.put("mobile", mobile)
                root.put("createdAt", user.createdAt)
                root.put("monthlyBudget", monthlyBudget)
                root.put("dailyTarget", dailyTarget)
                root.put("settingsUpdatedAt", settingsUpdatedAt)
                root.put("allDataClearedAt", localDataClearedAt)

                val txArray = JSONArray()
                for (tx in currentTxs) {
                    val item = JSONObject()
                    item.put("syncId", tx.syncId)
                    item.put("title", tx.title)
                    item.put("amount", tx.amount)
                    item.put("type", tx.type)
                    item.put("category", tx.category)
                    item.put("paymentMethod", tx.paymentMethod)
                    item.put("timestamp", tx.timestamp)
                    item.put("note", tx.note)
                    item.put("encryptedNote", tx.encryptedNote)
                    item.put("isEncrypted", tx.isEncrypted)
                    item.put("fee", tx.fee)
                    item.put("transactionRef", tx.transactionRef)
                    item.put("khatId", tx.khatId ?: 0L)
                    item.put("khatName", tx.khatName)
                    item.put("updatedAt", tx.updatedAt)
                    item.put("isDeleted", tx.isDeleted)
                    txArray.put(item)
                }
                root.put("transactions", txArray)

                val debtArray = JSONArray()
                for (d in currentDebts) {
                    val item = JSONObject()
                    item.put("syncId", d.syncId)
                    item.put("personName", d.personName)
                    item.put("amount", d.amount)
                    item.put("type", d.type)
                    item.put("dueDate", d.dueDate)
                    item.put("note", d.note)
                    item.put("isSettled", d.isSettled)
                    item.put("khatId", d.khatId ?: 0L)
                    item.put("khatName", d.khatName)
                    item.put("deductedFromMain", d.deductedFromMain)
                    item.put("paidAmount", d.paidAmount)
                    item.put("paymentHistoryJson", d.paymentHistoryJson)
                    item.put("timestamp", d.timestamp)
                    item.put("updatedAt", d.updatedAt)
                    item.put("isDeleted", d.isDeleted)
                    debtArray.put(item)
                }
                root.put("debts", debtArray)

                val goalArray = JSONArray()
                for (g in currentGoals) {
                    val item = JSONObject()
                    item.put("syncId", g.syncId)
                    item.put("title", g.title)
                    item.put("targetAmount", g.targetAmount)
                    item.put("savedAmount", g.savedAmount)
                    item.put("note", g.note)
                    item.put("isCompleted", g.isCompleted)
                    item.put("historyJson", g.historyJson)
                    item.put("timestamp", g.timestamp)
                    item.put("updatedAt", g.updatedAt)
                    item.put("isDeleted", g.isDeleted)
                    goalArray.put(item)
                }
                root.put("savingsGoals", goalArray)

                val khatArray = JSONArray()
                for (k in currentKhats) {
                    val item = JSONObject()
                    item.put("syncId", k.syncId)
                    item.put("name", k.name)
                    item.put("allocatedAmount", k.allocatedAmount)
                    item.put("colorHex", k.colorHex)
                    item.put("iconName", k.iconName)
                    item.put("timestamp", k.timestamp)
                    item.put("updatedAt", k.updatedAt)
                    item.put("isDeleted", k.isDeleted)
                    khatArray.put(item)
                }
                root.put("khats", khatArray)

                val plainJson = root.toString()
                val currentHash = CryptoEngine.sha256(plainJson)

                // Critical Optimization: Skip Firestore write if data has not changed!
                if (currentHash == lastSyncedPayloadHash && cloudDoc.exists()) {
                    _syncState.value = SyncState.Synced(_lastSyncTimestamp.value)
                    return@withContext Result.success(Unit)
                }

                val aesKey = CryptoEngine.deriveKey(user.password, mobile)
                val encryptedVault = CryptoEngine.encrypt(plainJson, aesKey)
                val authHash = CryptoEngine.hashPasswordForAuth(user.password, mobile)

                val now = System.currentTimeMillis()
                val prevVersion = cloudDoc.getLong("dataVersion") ?: 1L
                val docData = hashMapOf(
                    "authHash" to authHash,
                    "encryptedVault" to encryptedVault,
                    "lastSyncTime" to now,
                    "lastModifiedBy" to deviceId,
                    "dataVersion" to (prevVersion + 1L),
                    "isDeleted" to false,
                    "allDataClearedAt" to localDataClearedAt
                )
                firestore.collection("users").document(mobile).set(docData, SetOptions.merge()).await()

                lastSyncedPayloadHash = currentHash
                _lastSyncTimestamp.value = now
                prefs.edit().putLong("pref_last_cloud_sync_time", now).apply()
                _syncState.value = SyncState.Synced(now)

                Result.success(Unit)
            } catch (e: Exception) {
                _syncState.value = SyncState.Error(e.localizedMessage ?: "Sync failed")
                Result.failure(e)
            }
        }
    }

    /**
     * Reconcile with Cloud: pulls remote changes, merges deterministically,
     * and pushes any newer local changes to Cloud.
     */
    suspend fun reconcileAndSync(
        userId: Long,
        user: UserEntity,
        monthlyBudget: Double,
        dailyTarget: Double
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val mobile = user.mobile.trim()
        val firestore = getFirestoreOrNull() ?: return@withContext Result.failure(Exception("FIREBASE_NOT_CONFIGURED"))

        try {
            val cached = lastKnownCloudSnapshot
            val doc = if (cached != null && cached.id == mobile && cached.exists()) {
                cached
            } else {
                val fetched = firestore.collection("users").document(mobile).get().await()
                lastKnownCloudSnapshot = fetched
                fetched
            }
            if (!doc.exists() || doc.getBoolean("isDeleted") == true) {
                return@withContext Result.failure(Exception("ACCOUNT_NOT_FOUND_OR_DELETED"))
            }

            val remoteVault = doc.getString("encryptedVault")
            val remoteSyncTime = doc.getLong("lastSyncTime") ?: 0L
            if (!remoteVault.isNullOrBlank()) {
                reconcileLocalDatabaseFromVault(mobile, user.password, remoteVault, remoteSyncTime)
            }

            // Push merged state passing doc to eliminate duplicate read!
            syncAllUserDataToCloud(userId, user, monthlyBudget, dailyTarget, knownCloudDoc = doc)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Starts listening to real-time changes on the user's remote document in Firestore.
     */
    fun startRealtimeSyncListener(
        userMobile: String,
        userPasswordProvider: () -> String?,
        onDataUpdatedRemotely: () -> Unit,
        onAccountDeletedRemotely: () -> Unit
    ) {
        val cleanMobile = userMobile.trim()
        if (cleanMobile.isBlank()) return
        val firestore = getFirestoreOrNull() ?: return

        if (currentListeningMobile == cleanMobile && activeSnapshotListener != null) {
            return
        }

        stopRealtimeSyncListener()
        currentListeningMobile = cleanMobile

        activeSnapshotListener = firestore.collection("users").document(cleanMobile)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(TAG, "Real-time snapshot listener error: ${error.localizedMessage}")
                    return@addSnapshotListener
                }
                if (snapshot == null) return@addSnapshotListener

                lastKnownCloudSnapshot = snapshot

                // Check if account is deleted explicitly
                if (snapshot.exists() && snapshot.getBoolean("isDeleted") == true) {
                    Log.d(TAG, "Account deletion detected in real-time for $cleanMobile")
                    stopRealtimeSyncListener()
                    CoroutineScope(Dispatchers.Main).launch {
                        onAccountDeletedRemotely()
                    }
                    return@addSnapshotListener
                }

                val remoteModifiedBy = snapshot.getString("lastModifiedBy") ?: ""
                val remoteSyncTime = snapshot.getLong("lastSyncTime") ?: 0L
                val localLastSyncTime = _lastSyncTimestamp.value

                // Ignore echo from this exact device if not newer
                if (remoteModifiedBy == deviceId && remoteSyncTime <= localLastSyncTime) {
                    return@addSnapshotListener
                }

                // If snapshot has pending local writes, ignore
                if (snapshot.metadata.hasPendingWrites()) {
                    return@addSnapshotListener
                }

                val encryptedVault = snapshot.getString("encryptedVault") ?: return@addSnapshotListener
                val password = userPasswordProvider() ?: return@addSnapshotListener

                CoroutineScope(Dispatchers.IO).launch {
                    val success = reconcileLocalDatabaseFromVault(
                        mobile = cleanMobile,
                        password = password,
                        encryptedVault = encryptedVault,
                        remoteSyncTime = remoteSyncTime
                    )
                    if (success) {
                        withContext(Dispatchers.Main) {
                            onDataUpdatedRemotely()
                        }
                    }
                }
            }
    }

    fun stopRealtimeSyncListener() {
        activeSnapshotListener?.remove()
        activeSnapshotListener = null
        currentListeningMobile = null
        lastKnownCloudSnapshot = null
    }

    /**
     * Pull latest snapshot from Firestore and reconcile.
     */
    suspend fun pullLatestFromCloud(
        mobile: String,
        password: String
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        val cleanMobile = mobile.trim()
        val firestore = getFirestoreOrNull() ?: return@withContext Result.failure(Exception("FIREBASE_NOT_CONFIGURED"))

        try {
            _syncState.value = SyncState.Syncing
            val doc = firestore.collection("users").document(cleanMobile).get().await()
            if (!doc.exists() || doc.getBoolean("isDeleted") == true) {
                _syncState.value = SyncState.Idle
                return@withContext Result.failure(Exception("ACCOUNT_NOT_FOUND_OR_DELETED"))
            }

            val encryptedVault = doc.getString("encryptedVault")
            val remoteSyncTime = doc.getLong("lastSyncTime") ?: System.currentTimeMillis()

            if (!encryptedVault.isNullOrBlank()) {
                val success = reconcileLocalDatabaseFromVault(
                    mobile = cleanMobile,
                    password = password,
                    encryptedVault = encryptedVault,
                    remoteSyncTime = remoteSyncTime
                )
                if (success) {
                    _syncState.value = SyncState.Synced(remoteSyncTime)
                    return@withContext Result.success(true)
                } else {
                    _syncState.value = SyncState.Error("DECRYPT_FAILED")
                    return@withContext Result.failure(Exception("DECRYPT_FAILED"))
                }
            } else {
                _syncState.value = SyncState.Idle
                return@withContext Result.success(false)
            }
        } catch (e: Exception) {
            _syncState.value = SyncState.Error(e.localizedMessage ?: "Sync error")
            return@withContext Result.failure(e)
        }
    }
}

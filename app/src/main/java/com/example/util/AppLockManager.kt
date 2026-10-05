package com.example.util

import android.content.Context
import androidx.biometric.BiometricManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.security.MessageDigest

class AppLockManager private constructor(private val context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // Current active user ID for account-scoped security
    private var currentUserId: Long = -1L

    private val _isAppLockEnabled = MutableStateFlow(false)
    val isAppLockEnabled: StateFlow<Boolean> = _isAppLockEnabled.asStateFlow()

    private val _isBiometricEnabled = MutableStateFlow(true)
    val isBiometricEnabled: StateFlow<Boolean> = _isBiometricEnabled.asStateFlow()

    // Initially locked if lock is enabled for the active account
    private val _isLocked = MutableStateFlow(false)
    val isLocked: StateFlow<Boolean> = _isLocked.asStateFlow()

    init {
        try {
            val dailyPrefs = context.getSharedPreferences("daily_hishab_prefs", Context.MODE_PRIVATE)
            val isLoggedIn = dailyPrefs.getBoolean("is_user_logged_in", false)
            val savedUid = dailyPrefs.getLong("current_user_id", -1L)
            if (isLoggedIn && savedUid > 0) {
                currentUserId = savedUid
            }
            // Purge legacy non-account-scoped global keys if any exist
            if (prefs.contains("key_app_lock_enabled") || prefs.contains("key_pin_hash")) {
                prefs.edit()
                    .remove("key_app_lock_enabled")
                    .remove("key_pin_hash")
                    .remove("key_biometric_enabled")
                    .apply()
            }
        } catch (_: Exception) {}
        refreshState()
    }

    private fun keyLockEnabled(uid: Long = currentUserId): String = "key_app_lock_enabled_user_$uid"
    private fun keyBiometricEnabled(uid: Long = currentUserId): String = "key_biometric_enabled_user_$uid"
    private fun keyPinHash(uid: Long = currentUserId): String = "key_pin_hash_user_$uid"

    fun setActiveUser(userId: Long) {
        if (currentUserId != userId) {
            currentUserId = userId
            refreshState()
        }
    }

    fun getActiveUserId(): Long = currentUserId

    private fun refreshState() {
        if (currentUserId <= 0) {
            _isAppLockEnabled.value = false
            _isBiometricEnabled.value = false
            _isLocked.value = false
            return
        }
        val enabled = prefs.getBoolean(keyLockEnabled(currentUserId), false)
        val biometric = prefs.getBoolean(keyBiometricEnabled(currentUserId), true)
        val hasPin = !prefs.getString(keyPinHash(currentUserId), null).isNullOrBlank()
        _isAppLockEnabled.value = enabled
        _isBiometricEnabled.value = biometric
        _isLocked.value = enabled && hasPin
    }

    fun isBiometricHardwareAvailable(): Boolean {
        val biometricManager = BiometricManager.from(context)
        val authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK
        return biometricManager.canAuthenticate(authenticators) == BiometricManager.BIOMETRIC_SUCCESS
    }

    fun hasPinSet(): Boolean {
        if (currentUserId <= 0) return false
        return !prefs.getString(keyPinHash(currentUserId), null).isNullOrBlank()
    }

    fun verifyPin(pin: String): Boolean {
        if (currentUserId <= 0) return false
        val storedHash = prefs.getString(keyPinHash(currentUserId), null) ?: return false
        val hashed = hashPin(pin)
        return hashed == storedHash
    }

    fun setPin(pin: String) {
        if (currentUserId <= 0) return
        val hashed = hashPin(pin)
        prefs.edit()
            .putString(keyPinHash(currentUserId), hashed)
            .putBoolean(keyLockEnabled(currentUserId), true)
            .commit()
        _isAppLockEnabled.value = true
    }

    fun setAppLockEnabled(enabled: Boolean) {
        if (currentUserId <= 0) return
        prefs.edit().putBoolean(keyLockEnabled(currentUserId), enabled).commit()
        _isAppLockEnabled.value = enabled
        if (!enabled) {
            _isLocked.value = false
        }
    }

    fun setBiometricEnabled(enabled: Boolean) {
        if (currentUserId <= 0) return
        prefs.edit().putBoolean(keyBiometricEnabled(currentUserId), enabled).commit()
        _isBiometricEnabled.value = enabled
    }

    fun lock() {
        if (currentUserId > 0 && _isAppLockEnabled.value && hasPinSet()) {
            _isLocked.value = true
        }
    }

    fun unlock() {
        _isLocked.value = false
    }

    /**
     * Completely and permanently clears all App Lock settings and PIN credentials
     * for the specified account ID upon account deletion.
     * Prevents any leakage or cross-account interference with other accounts on this device.
     */
    fun clearAccountLock(userId: Long) {
        if (userId <= 0) return
        prefs.edit()
            .remove(keyLockEnabled(userId))
            .remove(keyBiometricEnabled(userId))
            .remove(keyPinHash(userId))
            .commit()

        if (currentUserId == userId) {
            currentUserId = -1L
            _isAppLockEnabled.value = false
            _isBiometricEnabled.value = false
            _isLocked.value = false
        }
    }

    private fun hashPin(pin: String): String {
        val salt = "APP_LOCK_LOCAL_SALT_2026_TRACKER"
        val input = salt + pin
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    companion object {
        private const val PREFS_NAME = "device_local_app_lock_prefs"

        @Volatile
        private var INSTANCE: AppLockManager? = null

        fun getInstance(context: Context): AppLockManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AppLockManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}

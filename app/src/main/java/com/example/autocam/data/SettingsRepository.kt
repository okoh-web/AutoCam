
package com.example.autocam.data

import android.content.Context
import android.provider.Settings
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.example.autocam.data.model.ConnectionSettings
import com.example.autocam.data.model.PolicySettings
import com.example.autocam.domain.CaptureController.CaptureSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

private val Context.dataStore by preferencesDataStore(name = "autocam_prefs")

class SettingsRepository(private val context: Context) {

    // === Keys ===
    private object Keys {
        val SERVER = stringPreferencesKey("server")
        val USER   = stringPreferencesKey("user")
        val ALIAS  = stringPreferencesKey("pass_alias")

        val INTERVAL = intPreferencesKey("interval_sec")
        val MP       = intPreferencesKey("target_mp")
        val JPEG     = intPreferencesKey("jpeg_quality")
        val PREVIEW  = booleanPreferencesKey("preview")

        val MAX_FILES = intPreferencesKey("max_files")
        val MAX_BYTES = longPreferencesKey("max_bytes")
        val LOG_DAYS  = intPreferencesKey("log_days")
        val REMOTE_DAYS = intPreferencesKey("remote_days")
    }

    // DeviceId（ANDROID_ID → 12桁大文字）
    val deviceIdFlow: Flow<String> = flow {
        val raw = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
        emit((raw ?: "DEVICE").uppercase().takeLast(12))
    }

    // --- StateHolders ---
    private val _connection = MutableStateFlow(ConnectionSettings())
    val connection: StateFlow<ConnectionSettings> = _connection.asStateFlow()

    private val _capture = MutableStateFlow(
        CaptureSettings(intervalSec = 60, targetMp = 3, jpegQuality = 85, previewEnabled = true)
    )
    val capture: StateFlow<CaptureSettings> = _capture.asStateFlow()

    private val _policy = MutableStateFlow(PolicySettings())
    val policy: StateFlow<PolicySettings> = _policy.asStateFlow()

    private val _remoteDays = MutableStateFlow(0)
    val remoteDaysFlow: StateFlow<Int> = _remoteDays.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.IO)

    init {
        // DataStore → StateFlow への初期ロード＆追従
        scope.launch {
            context.dataStore.data.collect { p ->
                _connection.value = ConnectionSettings(
                    p[Keys.SERVER] ?: "smb://HOST/Share",
                    p[Keys.USER] ?: "",
                    p[Keys.ALIAS] ?: "smb_pass_alias"
                )
                _capture.value = CaptureSettings(
                    intervalSec = p[Keys.INTERVAL] ?: 60,
                    targetMp = p[Keys.MP] ?: 3,
                    jpegQuality = p[Keys.JPEG] ?: 85,
                    previewEnabled = p[Keys.PREVIEW] ?: true
                )
                _policy.value = PolicySettings(
                    maxFiles = p[Keys.MAX_FILES],
                    maxBytes = p[Keys.MAX_BYTES],
                    logRetentionDays = p[Keys.LOG_DAYS] ?: 14
                )
                _remoteDays.value = p[Keys.REMOTE_DAYS] ?: 0
            }
        }
    }

    // --- Setters（保存付き） ---
    fun setConnection(server: String, username: String, passwordAlias: String) {
        require(server.trim().lowercase().startsWith("smb://")) {
            "サーバーは smb://HOST/Share で指定してください"
        }
        val normalized = server.trim().removeSuffix("/")
        val user = username.trim()
        val alias = passwordAlias.trim()

        _connection.value = ConnectionSettings(normalized, user, alias)

        scope.launch {
            context.dataStore.edit { e ->
                e[Keys.SERVER] = normalized
                e[Keys.USER] = user
                e[Keys.ALIAS] = alias
            }
        }
    }

    fun setCapture(cs: CaptureSettings) {
        _capture.value = cs
        scope.launch {
            context.dataStore.edit { e ->
                e[Keys.INTERVAL] = cs.intervalSec
                e[Keys.MP] = cs.targetMp
                e[Keys.JPEG] = cs.jpegQuality
                e[Keys.PREVIEW] = cs.previewEnabled
            }
        }
    }

    fun setPolicy(maxFiles: Int?, maxBytes: Long?, logRetentionDays: Int) {
        _policy.value = PolicySettings(maxFiles, maxBytes, logRetentionDays)
        scope.launch {
            context.dataStore.edit { e ->
                if (maxFiles != null) e[Keys.MAX_FILES] = maxFiles else e.remove(Keys.MAX_FILES)
                if (maxBytes != null) e[Keys.MAX_BYTES] = maxBytes else e.remove(Keys.MAX_BYTES)
                e[Keys.LOG_DAYS] = logRetentionDays
            }
        }
    }

    fun setPolicy(maxFiles: Int?, maxBytes: Long?, logRetentionDays: Int, remoteDays: Int) {
        setPolicy(maxFiles, maxBytes, logRetentionDays)
        val days = remoteDays.coerceAtLeast(0)
        _remoteDays.value = days
        scope.launch { context.dataStore.edit { it[Keys.REMOTE_DAYS] = days } }
    }

    // Worker 互換の Flow
    fun serverFlow(): Flow<String> = connection.map { it.server }
    fun usernameFlow(): Flow<String> = connection.map { it.username }
    fun passwordAliasFlow(): Flow<String> = connection.map { it.passwordAlias }
}

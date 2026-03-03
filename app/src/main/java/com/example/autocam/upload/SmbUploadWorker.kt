
package com.example.autocam.upload

import android.content.Context
import android.net.Uri
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.autocam.data.SettingsRepository
import com.example.autocam.util.AppLogger
import kotlinx.coroutines.flow.first
import kotlin.math.min

class SmbUploadWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {

    companion object {
        private const val MAX_DELAY_SEC = 600 // 10分
        private const val MAX_RETRY = 10
    }

    override suspend fun doWork(): Result {
        val repo = SettingsRepository(applicationContext)

        val contentUri = Uri.parse(inputData.getString("contentUri") ?: return Result.success())
        val displayName = inputData.getString("displayName") ?: return Result.success()
        val dateDir = inputData.getString("dateDir") ?: return Result.success()
        val attempt = inputData.getInt("attempt", 0)

        val server = repo.serverFlow().first()
        val username = repo.usernameFlow().first()
        val passAlias = repo.passwordAliasFlow().first()
        val deviceId = repo.deviceIdFlow.first() // property Flow

        val client = SmbClient(applicationContext)
        try {
            client.connectivityCheck(
                base = server,
                deviceId = deviceId,
                dateDir = dateDir,
                username = username,
                passwordAlias = passAlias
            )
        } catch (e: Throwable) {
            scheduleRetry(contentUri, displayName, dateDir, attempt, e)
            return Result.success()
        }

        try {
            val ok = client.upload(
                contentUri = contentUri,
                displayName = displayName,
                dateDir = dateDir,
                deviceId = deviceId,
                server = server,
                username = username,
                passwordAlias = passAlias
            )
            if (ok) {
                AppLogger.i(
                    """{"event":"upload","status":"success","content_uri":"${contentUri}","display_name":"${displayName}"}"""
                )
                return Result.success()
            } else {
                throw RuntimeException("upload returned false")
            }
        } catch (e: Throwable) {
            scheduleRetry(contentUri, displayName, dateDir, attempt, e)
            return Result.success()
        }
    }

    private suspend fun scheduleRetry(
        contentUri: Uri,
        displayName: String,
        dateDir: String,
        attempt: Int,
        error: Throwable
    ) {
        val next = nextDelaySec(attempt + 1)
        if (attempt + 1 >= MAX_RETRY) {
            AppLogger.e(
                """{"event":"upload_failed_final","content_uri":"${contentUri}","attempt":${attempt},"message":"${error.message}"}"""
            )
        } else {
            AppLogger.i(
                """{"event":"retry_scheduled","content_uri":"${contentUri}","attempt":${attempt},"next_delay_sec":${next}}"""
            )
            UploadEnqueuer.enqueue(
                context = applicationContext,
                contentUri = contentUri,
                displayName = displayName,
                dateDir = dateDir,
                initialDelayMs = next * 1000L,
                attempt = attempt + 1
            )
        }
    }

    private fun nextDelaySec(attempt: Int): Int = when {
        attempt <= 1 -> 10
        attempt == 2 -> 30
        else -> min(MAX_DELAY_SEC, 60 * (1 shl (attempt - 3)))
    }
}

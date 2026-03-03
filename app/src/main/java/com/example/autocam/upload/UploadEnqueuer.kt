
package com.example.autocam.upload

import android.content.Context
import android.net.Uri
import androidx.work.*
import java.util.concurrent.TimeUnit

object UploadEnqueuer {

    private const val UNIQUE_PREFIX = "smb_upload:"

    fun enqueue(
        context: Context,
        contentUri: Uri,
        displayName: String,
        dateDir: String,
        initialDelayMs: Long = 0L,
        attempt: Int = 0
    ) {
        val data = workDataOf(
            "contentUri" to contentUri.toString(),
            "displayName" to displayName,
            "dateDir" to dateDir,
            "attempt" to attempt
        )

        val request = OneTimeWorkRequestBuilder<SmbUploadWorker>()
            .setInputData(data)
            .setInitialDelay(initialDelayMs, TimeUnit.MILLISECONDS)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()

        val unique = UNIQUE_PREFIX + contentUri.toString()
        WorkManager.getInstance(context)
            .enqueueUniqueWork(unique, ExistingWorkPolicy.KEEP, request)
    }
}

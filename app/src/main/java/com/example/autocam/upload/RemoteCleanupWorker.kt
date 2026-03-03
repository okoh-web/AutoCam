package com.example.autocam.upload

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class RemoteCleanupWorker(ctx: Context, params: WorkerParameters): CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        // TODO: implement SMB cleanup
        return Result.success()
    }
}

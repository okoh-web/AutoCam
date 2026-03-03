
package com.example.autocam.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service.START_STICKY
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.example.autocam.R
import com.example.autocam.data.SettingsRepository
import com.example.autocam.domain.CaptureController
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class CaptureService : LifecycleService() {

    companion object {
        private const val CHANNEL_ID = "autocam_capture"
        private const val NOTIF_ID = 1001
        const val ACTION_START = "com.example.autocam.action.START"
        const val ACTION_STOP  = "com.example.autocam.action.STOP"
    }

    private var job: Job? = null
    private lateinit var controller: CaptureController
    private lateinit var settings: SettingsRepository

    override fun onCreate() {
        super.onCreate()
        settings = SettingsRepository(applicationContext)
        controller = CaptureController(
            context = applicationContext,
            lifecycleOwner = this,
        )
        startForeground(NOTIF_ID, buildNotification("待機中"))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        when (intent?.action) {
            ACTION_START -> startLoop()
            ACTION_STOP  -> stopLoop()
        }
        return START_STICKY
    }

    private fun startLoop() {
        if (job?.isActive == true) return
        updateNotification("撮影中")
        job = lifecycleScope.launch {

            controller.startLoop(
                settingsFlow = settings.capture,       // ← StateFlow<CaptureSettings>（FlowとしてOK）
                deviceIdFlow = settings.deviceIdFlow,  // ← こちらは val Flow<String>
                onState = { st -> updateNotification(st) },
            )

        }
    }

    private fun stopLoop() {
        job?.cancel()
        lifecycleScope.launch {
            controller.stop()
            updateNotification("待機中")
        }
    }

    override fun onDestroy() {
        stopLoop()
        super.onDestroy()
    }

    private fun buildNotification(text: String): Notification {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "AutoCam 撮影", NotificationManager.IMPORTANCE_LOW)
            )
        }
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("AutoCam")
            .setContentText(text)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification(text: String) {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIF_ID, buildNotification(text))
    }
}

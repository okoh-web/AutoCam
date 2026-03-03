
package com.example.autocam.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.example.autocam.data.SettingsRepository
import com.example.autocam.domain.CaptureController.CaptureSettings
import com.example.autocam.service.CaptureService

@Composable
fun HomeScreen(nav: NavController) {
    val context = LocalContext.current
    val repo = remember { SettingsRepository(context) }
    // 設定の現在値
    val cap by repo.capture.collectAsState()

    var status by remember { mutableStateOf("待機中") }

    // ---- (1) CAMERA 権限ランチャー ----
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            startCaptureService(context)
            status = "撮影中"
        } else {
            status = "カメラ権限未許可"
        }
    }

    // ---- (2) 通知権限ランチャー（API 33+） ----
    val notifLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted || Build.VERSION.SDK_INT < 33) {
            // 通知OK → 次はカメラ権限
            requestCameraAndStart(context, cameraLauncher, onStatus = { status = it })
        } else {
            status = "通知権限未許可"
        }
    }
    @OptIn(ExperimentalMaterial3Api::class)
    Scaffold(
        topBar = { TopAppBar(title = { Text("AutoCam") }) }
    ) { inner ->
        Column(
            modifier = Modifier
                .padding(inner)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("状態: $status", style = MaterialTheme.typography.bodyLarge)
            Text(
                "間隔:${cap.intervalSec}s / MP:${cap.targetMp} / JPEG:${cap.jpegQuality} / Preview:${if (cap.previewEnabled) "ON" else "OFF"}",
                style = MaterialTheme.typography.bodyLarge
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        // 1) 通知権限（API33+）
                        if (Build.VERSION.SDK_INT >= 33) {
                            val hasNotif = ContextCompat.checkSelfPermission(
                                context, Manifest.permission.POST_NOTIFICATIONS
                            ) == PackageManager.PERMISSION_GRANTED
                            if (!hasNotif) {
                                notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                return@Button
                            }
                        }
                        // 2) カメラ権限へ
                        requestCameraAndStart(context, cameraLauncher, onStatus = { status = it })
                    },
                    modifier = Modifier.height(56.dp)
                ) { Text("撮影スタート") }

                OutlinedButton(
                    onClick = {
                        stopCaptureService(context)
                        status = "待機中"
                    },
                    modifier = Modifier.height(56.dp)
                ) { Text("停止") }
            }
        }
    }
}

// ---- Helper: CAMERA 権限→サービス起動 ----
private fun requestCameraAndStart(
    context: Context,
    cameraLauncher: androidx.activity.result.ActivityResultLauncher<String>,
    onStatus: (String) -> Unit
) {
    val cameraPerm = Manifest.permission.CAMERA
    val hasCam = ContextCompat.checkSelfPermission(
        context, cameraPerm
    ) == PackageManager.PERMISSION_GRANTED
    if (hasCam) {
        startCaptureService(context)
        onStatus("撮影中")
    } else {
        cameraLauncher.launch(cameraPerm)
    }
}

// ---- Helper: サービス起動/停止 ----
private fun startCaptureService(context: Context) {
    val intent = Intent(context, CaptureService::class.java).apply {
        action = CaptureService.ACTION_START
    }
    ContextCompat.startForegroundService(context, intent)
}

private fun stopCaptureService(context: Context) {
    val intent = Intent(context, CaptureService::class.java).apply {
        action = CaptureService.ACTION_STOP
    }
    context.startService(intent)
}

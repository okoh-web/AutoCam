package com.example.autocam.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
//import com.example.autocam.data.CaptureSettings
import com.example.autocam.domain.CaptureController.CaptureSettings
import com.example.autocam.data.SettingsRepository
import kotlinx.coroutines.launch

@Composable
fun CaptureSettingsScreen(nav: NavController) {
    val context = LocalContext.current
    val repo = remember { SettingsRepository(context) }
    val scope = rememberCoroutineScope()

    var interval by remember { mutableStateOf("60") }
    var quality  by remember { mutableStateOf("85") }
    var mp       by remember { mutableStateOf("3") }
    var previewOn by remember { mutableStateOf(true) }

    val current by repo.capture.collectAsState()
    LaunchedEffect(current) {
        interval = current.intervalSec.toString()
        quality  = current.jpegQuality.toString()
        mp       = current.targetMp.toString()
        previewOn = current.previewEnabled
    }

    Column(Modifier.padding(16.dp)) {
        OutlinedTextField(interval, { interval = it }, label = { Text("撮影間隔(秒)") })
        OutlinedTextField(mp, { mp = it }, label = { Text("解像度(MP)") })
        OutlinedTextField(quality, { quality = it }, label = { Text("JPEG品質(0-100)") })
        Row { Text("プレビュー:"); Spacer(Modifier.width(8.dp)); Switch(checked = previewOn, onCheckedChange = { previewOn = it }) }
        Spacer(Modifier.height(8.dp))
        Row {
            Button(onClick = {
                val iv = interval.toIntOrNull()?.coerceIn(1, 86400) ?: 60
                val mpv = mp.toIntOrNull()?.coerceAtLeast(1) ?: 3
                val qv = quality.toIntOrNull()?.coerceIn(0, 100) ?: 85
                scope.launch {
                    repo.setCapture(CaptureSettings(iv, mpv, qv, previewOn))
                    nav.popBackStack("home", false)
                }
            }) { Text("保存") }
            Spacer(Modifier.width(8.dp))
            OutlinedButton(onClick = { nav.popBackStack() }) { Text("キャンセル") }
        }
    }
}

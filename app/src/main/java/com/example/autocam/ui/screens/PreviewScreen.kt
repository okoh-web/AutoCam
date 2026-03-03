
package com.example.autocam.ui.screens

import android.os.Environment
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.compose.ui.viewinterop.AndroidView
import com.example.autocam.camera.CameraXController
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*


@Composable
fun PreviewScreen(nav: NavController) {
    val context = LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    val controller = remember { CameraXController(context, lifecycleOwner) }
    var status by remember { mutableStateOf("待機中") }

    LaunchedEffect(Unit) {
        status = "カメラ初期化中…"
        controller.bind(previewEnabled = true, jpegQuality = 85)
        status = "プレビュー表示中"
    }
    @OptIn(ExperimentalMaterial3Api::class)
    Scaffold(
        topBar = { TopAppBar(title = { Text("プレビュー") }) } // 安定API
    ) { inner ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(inner)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AndroidView(factory = { controller.view() }, modifier = Modifier.weight(1f).fillMaxWidth())

            Text("状態: $status", style = MaterialTheme.typography.titleMedium)

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    modifier = Modifier.weight(1f).height(56.dp),
                    onClick = {
                        scope.launch {
                            status = "撮影中…"
                            val r = controller.takePictureToMediaStore()
                            status = if (r.isSuccess) {
                                val uri = r.getOrNull()
                                "公開Picturesへ保存: $uri"
                            } else {
                                "失敗: ${r.exceptionOrNull()?.message}"
                            }
                        }
                    }
                ) { Text("1枚撮影（公開ピクチャ）", style = MaterialTheme.typography.titleMedium) }

                OutlinedButton(
                    modifier = Modifier.weight(1f).height(56.dp),
                    onClick = { nav.popBackStack() }
                ) { Text("戻る", style = MaterialTheme.typography.titleMedium) }
            }
        }
    }
}

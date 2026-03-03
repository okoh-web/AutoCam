package com.example.autocam.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.autocam.data.SettingsRepository
import kotlinx.coroutines.launch

@Composable
fun PolicySettingsScreen(nav: NavController) {
    val context = LocalContext.current
    val repo = remember { SettingsRepository(context) }
    val scope = rememberCoroutineScope()

    var maxFiles by remember { mutableStateOf("10000") }
    var maxGb    by remember { mutableStateOf("10") }
    var logDays  by remember { mutableStateOf("14") }
    var remoteDays by remember { mutableStateOf("0") }

    Column(Modifier.padding(16.dp)) {
        OutlinedTextField(maxFiles, { maxFiles = it }, label = { Text("最大枚数") })
        OutlinedTextField(maxGb, { maxGb = it }, label = { Text("容量上限(GB)") })
        OutlinedTextField(logDays, { logDays = it }, label = { Text("ログ保存(日)") })
        Divider(Modifier.padding(vertical = 8.dp))
        Text("サーバー側自動削除（SMB）")
        OutlinedTextField(remoteDays, { remoteDays = it }, label = { Text("保持期間(日) 0で無効") })
        Spacer(Modifier.height(8.dp))
        Row {
            Button(onClick = {
                val mf = maxFiles.toIntOrNull()?.takeIf { it > 0 }
                val mb = maxGb.toLongOrNull()?.takeIf { it > 0 }?.let { it * 1024 * 1024 * 1024 }
                val ld = logDays.toIntOrNull()?.coerceIn(1, 3650) ?: 14
                val rd = remoteDays.toIntOrNull()?.coerceAtLeast(0) ?: 0
                scope.launch {
                    repo.setPolicy(mf, mb, ld, rd)
                    nav.popBackStack("home", false)
                }
            }) { Text("保存") }
            Spacer(Modifier.width(8.dp))
            OutlinedButton(onClick = { nav.popBackStack() }) { Text("キャンセル") }
        }
    }
}


package com.example.autocam.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.autocam.data.SettingsRepository
import androidx.compose.runtime.saveable.rememberSaveable


@Composable
fun ConnectionSettingsScreen(nav: NavController) {
    val context = LocalContext.current
    val repo = remember { SettingsRepository(context) }

    // 現在値を監視
    val current by repo.connection.collectAsState()

    // UI 状態（保存可能 / プロセス再生成にも強い）
    var server by rememberSaveable { mutableStateOf("") }
    var user   by rememberSaveable { mutableStateOf("") }
    var alias  by rememberSaveable { mutableStateOf("") }

    // 初回または current の変化時に UI へ反映（初期値流し込み）
    LaunchedEffect(current) {
        if (server.isEmpty() && user.isEmpty() && alias.isEmpty()) {
            server = current.server
            user   = current.username
            alias  = current.passwordAlias
        }
    }

    Column(Modifier.padding(16.dp)) {
        OutlinedTextField(
            value = server,
            onValueChange = { server = it.trim() },
            label = { Text("サーバー（smb://HOST/Share）") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = user,
            onValueChange = { user = it.trim() },
            label = { Text("ユーザー名") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
        )
        OutlinedTextField(
            value = alias,
            onValueChange = { alias = it.trim() },
            label = { Text("パスワード alias（P2でKeystore実装）") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
        )

        Spacer(Modifier.height(12.dp))
        Row {
            Button(
                onClick = {
                    // 永続保存（DataStore）
                    repo.setConnection(server, user, alias)
                    nav.popBackStack("home", false) // ← 戻る（必要に応じて調整）
                }
            ) { Text("保存") }

            Spacer(Modifier.width(8.dp))
            OutlinedButton(onClick = { nav.popBackStack() }) { Text("キャンセル") }
        }
    }
}

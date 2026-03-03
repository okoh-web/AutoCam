package com.example.autocam.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

@Composable
fun LogScreen(nav: NavController) {
    val logs = remember { mutableStateListOf("2026-02-01 10:00 撮影開始", "2026-02-01 10:01 画像転送成功") }
    Column(Modifier.padding(16.dp)) {
        LazyColumn(Modifier.weight(1f)) { items(logs) { Text(it) } }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = { nav.popBackStack() }) { Text("戻る") }
    }
}

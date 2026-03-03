package com.example.autocam.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.autocam.ui.screens.*

object Dest {
    const val HOME = "home"
    const val PREVIEW = "preview"
    const val CONNECTION = "connection"
    const val CAPTURE = "capture"
    const val POLICY = "policy"
    const val LOG = "log"
}

@Composable
fun AppNavHost(navController: NavHostController = rememberNavController()) {

    NavHost(navController, startDestination = Dest.HOME) {
        composable(Dest.HOME) { HomeScreen(navController) }
        composable(Dest.PREVIEW) { PreviewScreen(navController) }
        composable(Dest.CONNECTION) { ConnectionSettingsScreen(navController) }
        composable(Dest.CAPTURE) { CaptureSettingsScreen(navController) }
        composable(Dest.POLICY) { PolicySettingsScreen(navController) }
        composable(Dest.LOG) { LogScreen(navController) }
    }
}

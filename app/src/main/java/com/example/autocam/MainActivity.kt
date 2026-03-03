package com.example.autocam

import android.app.ActivityOptions
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import com.example.autocam.ui.theme.AutoCamTheme
import com.example.autocam.ui.navigation.AppNavHost

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        maybeRelauchOnDefaultDisplay()
        setContent {
            AutoCamTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    AppNavHost()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        maybeRelauchOnDefaultDisplay()
    }

    private fun currentDisplayIdCompat(): Int? {
        return if (Build.VERSION.SDK_INT >= 30) {
            this.display?.displayId
        } else {
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay?.displayId
        }
    }

    private fun maybeRelauchOnDefaultDisplay() {
        val currentId = currentDisplayIdCompat() ?: return
        if (currentId != 0 && Build.VERSION.SDK_INT >= 29) {
            val opts = ActivityOptions.makeBasic().apply {
                try {
                    this.javaClass.getMethod("setLaunchDisplayId", Int::class.java).invoke(this, 0)
                } catch (_: Exception) {}
            }
            val i = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(i, opts.toBundle())
            finish()
        }
    }
}

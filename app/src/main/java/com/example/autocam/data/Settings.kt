package com.example.autocam.data

data class ConnectionSettings(
    val server: String = "",
    val username: String = "",
    val passwordAlias: String = ""
)

data class CaptureSettings(
    val intervalSec: Int = 60,
    val targetMp: Int = 3,
    val jpegQuality: Int = 85,
    val previewEnabled: Boolean = true
)

data class PolicySettings(
    val maxFiles: Int? = 10000,
    val maxBytes: Long? = 10L * 1024 * 1024 * 1024,
    val logRetentionDays: Int = 14,
    val remoteRetentionDays: Int = 0
)

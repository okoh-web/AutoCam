
package com.example.autocam.data.model

data class ConnectionSettings(
    val server: String = "smb://HOST/Share",
    val username: String = "",
    val passwordAlias: String = "smb_pass_alias"
)

data class PolicySettings(
    val maxFiles: Int? = 10_000,
    val maxBytes: Long? = 10L * 1024 * 1024 * 1024, // 10 GB
    val logRetentionDays: Int = 14
)

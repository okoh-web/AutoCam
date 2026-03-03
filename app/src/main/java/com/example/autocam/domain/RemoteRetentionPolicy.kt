package com.example.autocam.domain

class RemoteRetentionPolicy {
    fun shouldDeleteFolder(folderName: String, retentionDays: Int): Boolean {
        // TODO: yyyyMMdd から経過日数を計算
        return false
    }
}

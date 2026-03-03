package com.example.autocam.domain

import java.io.File

class RotationManager(private val rootDir: File) {
    fun onNewFileSaved(file: File) { /* TODO */ }
    fun sweepIfExceeded(maxFiles: Int?, maxBytes: Long?) { /* TODO */ }
}

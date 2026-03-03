package com.example.autocam.camera

import android.content.Context
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.lifecycle.LifecycleOwner

class CameraXController_bak(private val context: Context, private val owner: LifecycleOwner) {
    private var preview: Preview? = null
    private var imageCapture: ImageCapture? = null
    suspend fun bind(previewEnabled: Boolean) {
        val provider = ProcessCameraProvider.getInstance(context).get()
        provider.unbindAll()
        preview = if (previewEnabled) Preview.Builder().build() else null
        imageCapture = ImageCapture.Builder().build()
        // TODO: selector/UseCase 設定
    }
    fun previewView(): PreviewView = PreviewView(context).also { pv -> preview?.setSurfaceProvider(pv.surfaceProvider) }
}

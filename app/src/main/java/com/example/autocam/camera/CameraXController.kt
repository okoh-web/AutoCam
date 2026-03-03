
package com.example.autocam.camera

import android.content.ContentValues
import android.content.Context
import android.provider.MediaStore
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.Executor
import kotlin.coroutines.resume

class CameraXController(
    private val context: Context,
    private val owner: LifecycleOwner
) {
    private var preview: Preview? = null
    private var imageCapture: ImageCapture? = null
    private val mainExecutor: Executor by lazy { ContextCompat.getMainExecutor(context) }
    private val previewView: PreviewView by lazy {
        PreviewView(context).apply { scaleType = PreviewView.ScaleType.FILL_CENTER }
    }

    suspend fun bind(previewEnabled: Boolean, jpegQuality: Int = 85) = withContext(Dispatchers.Main) {
        val provider = ProcessCameraProvider.getInstance(context).get()
        provider.unbindAll()

        val selector = CameraSelector.DEFAULT_BACK_CAMERA

        imageCapture = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .setJpegQuality(jpegQuality)
            .build()

        val cases = mutableListOf<UseCase>()
        imageCapture?.let { cases += it }

        if (previewEnabled) {
            preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }
            cases += preview!!
        }

        provider.bindToLifecycle(owner, selector, *cases.toTypedArray())
    }

    fun view(): PreviewView = previewView

    // 既存のファイル保存版（必要であれば残す）
    suspend fun takePicture(outFile: File): Result<File> = withContext(Dispatchers.Main) {
        val ic = imageCapture ?: return@withContext Result.failure(IllegalStateException("ImageCapture not bound"))
        val options = ImageCapture.OutputFileOptions.Builder(outFile).build()
        suspendCancellableCoroutine { cont ->
            ic.takePicture(options, mainExecutor, object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    if (cont.isActive) cont.resume(Result.failure(exc))
                }
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    if (cont.isActive) cont.resume(Result.success(outFile))
                }
            })
        }
    }

    // ★ MediaStore へ直接保存（公開 Pictures/AutoCam）
    suspend fun takePictureToMediaStore(displayNameNoExt: String = timeStampFileName()): Result<android.net.Uri> =
        withContext(Dispatchers.Main) {
            val ic = imageCapture ?: return@withContext Result.failure(IllegalStateException("ImageCapture not bound"))

            val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, "$displayNameNoExt.jpg")
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                // ギャラリー上で Pictures/AutoCam 配下に入る
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/AutoCam")
            }

            val options = ImageCapture.OutputFileOptions.Builder(
                context.contentResolver,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues
            ).build()

            suspendCancellableCoroutine { cont ->
                ic.takePicture(options, mainExecutor, object : ImageCapture.OnImageSavedCallback {
                    override fun onError(exc: ImageCaptureException) {
                        if (cont.isActive) cont.resume(Result.failure(exc))
                    }
                    override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                        val saved = output.savedUri ?: android.net.Uri.EMPTY
                        if (cont.isActive) cont.resume(Result.success(saved))
                    }
                })
            }
        }

    private fun timeStampFileName(): String {
        val ts = java.text.SimpleDateFormat("yyyyMMdd_HHmmss_SSS", java.util.Locale.US).format(java.util.Date())
        return "IMG_$ts"
    }
}


package com.example.autocam.domain

import android.content.ContentValues
import android.content.Context
import android.provider.MediaStore
import android.util.Size
import androidx.camera.core.ImageCapture
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.example.autocam.util.AppLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.suspendCancellableCoroutine
import java.text.SimpleDateFormat
import java.util.Locale

class CaptureController(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner
) {
    data class CaptureSettings(
        val intervalSec: Int,
        val targetMp: Int,
        val jpegQuality: Int,
        val previewEnabled: Boolean
    )

    private var imageCapture: ImageCapture? = null
    private var running = false

    suspend fun startLoop(
        settingsFlow: Flow<CaptureSettings>,
        deviceIdFlow: Flow<String>,
        onState: (String) -> Unit = {}
    ) {
        running = true
        val provider = ProcessCameraProvider.getInstance(context).get()
        while (running) {
            val s = settingsFlow.first()
            val deviceId = deviceIdFlow.first()
            try {
                bindCamera(provider, s)
                val start = System.currentTimeMillis()
                val uri = takeAndSaveToMediaStore(deviceId = deviceId, s = s)

                // enqueue SMB upload（別途実装済みの前提）
                com.example.autocam.upload.UploadEnqueuer.enqueue(
                    context = context,
                    contentUri = uri,
                    displayName = lastDisplayName!!,
                    dateDir = lastDateDir!!
                )

                AppLogger.i(
                    """{"event":"capture_saved","uri":"${uri}","jpeg_quality":${s.jpegQuality}}"""
                )
                onState("撮影中")
                val elapsed = System.currentTimeMillis() - start
                val period = (s.intervalSec * 1000L).coerceAtLeast(1000L)
                val sleep = period - (elapsed % period)
                delay(sleep)
            } catch (e: Throwable) {
                AppLogger.e(
                    """{"event":"capture_error","message":"${e.message}"}"""
                )
                onState("エラー/次周期で再試行")
                delay(1_000L)
            }
        }
    }

    suspend fun stop() {
        running = false
        withContext(Dispatchers.Main) {
            val provider = ProcessCameraProvider.getInstance(context).get()
            provider.unbindAll()
        }
    }

    private suspend fun bindCamera(provider: ProcessCameraProvider, s: CaptureSettings) =
        withContext(Dispatchers.Main) {
            val selector = androidx.camera.core.CameraSelector.DEFAULT_BACK_CAMERA
            val size = selectResolutionCloseToMp(s.targetMp)

            val resolutionSelector = ResolutionSelector.Builder()
                .setResolutionStrategy(
                    ResolutionStrategy(
                        size,
                        ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER
                    )
                )
                .build()

            val ic = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .setJpegQuality(s.jpegQuality)
                .setResolutionSelector(resolutionSelector)
                .build()

            provider.unbindAll()
            provider.bindToLifecycle(lifecycleOwner, selector, ic)
            imageCapture = ic
        }

    private fun selectResolutionCloseToMp(targetMp: Int): Size = when {
        targetMp <= 4 -> Size(2048, 1536)  // ~3MP
        targetMp <= 8 -> Size(3264, 2448)  // ~8MP
        else -> Size(4000, 3000)           // ~12MP
    }

    private var lastDisplayName: String? = null
    private var lastDateDir: String? = null

    @OptIn(ExperimentalCoroutinesApi::class)
    private suspend fun takeAndSaveToMediaStore(
        deviceId: String,
        @Suppress("UNUSED_PARAMETER") s: CaptureSettings
    ) = withContext(Dispatchers.Main) {
        val ic = requireNotNull(imageCapture) { "ImageCapture not bound" }
        val now = System.currentTimeMillis()
        val dfDate = SimpleDateFormat("yyyyMMdd", Locale.US)
        val dfTime = SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.US)
        val dateDir = dfDate.format(now)
        val displayName = "${deviceId}_${dfTime.format(now)}.jpg"
        lastDisplayName = displayName
        lastDateDir = dateDir

        val resolver = context.contentResolver
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/AutoCam/$dateDir")
        }
        val collection = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        val uri = resolver.insert(collection, contentValues)
            ?: error("Failed to insert into MediaStore")

        val output = ImageCapture.OutputFileOptions
            .Builder(resolver, uri, contentValues)
            .build()

        val executor = ContextCompat.getMainExecutor(context)
        suspendCancellableCoroutine<ImageCapture.OutputFileResults> { cont ->
            ic.takePicture(output, executor, object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: androidx.camera.core.ImageCaptureException) {
                    cont.resumeWith(Result.failure(exc))
                }
                override fun onImageSaved(out: ImageCapture.OutputFileResults) {
                    cont.resume(out, onCancellation = null)
                }
            })
        }
        uri
    }
}

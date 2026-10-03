package com.example.autosim.engine

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.autosim.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ScreenCaptureService : Service() {
    companion object {
        private const val TAG = "ScreenCaptureService"
        private const val CHANNEL_ID = "screen_capture_channel"
        private const val NOTIFICATION_ID = 1001
        
        private var mediaProjection: MediaProjection? = null
        private var imageReader: ImageReader? = null
        private var virtualDisplay: VirtualDisplay? = null
        private var handlerThread: HandlerThread? = null
        private var handler: Handler? = null
        
        private val _latestBitmap = MutableStateFlow<Bitmap?>(null)
        val latestBitmap: StateFlow<Bitmap?> = _latestBitmap
        
        private var captureInterval: Long = 500 // ms
        private var isCapturing = false
        
        fun setMediaProjection(projection: MediaProjection) {
            mediaProjection = projection
        }
        
        fun startCapture(intervalMs: Long = 500) {
            captureInterval = intervalMs
            isCapturing = true
        }
        
        fun stopCapture() {
            isCapturing = false
        }

        fun getLatestBitmap(): Bitmap? = _latestBitmap.value

        fun isServiceActive(): Boolean {
            return mediaProjection != null
        }

        fun cropBitmapForRegion(bitmap: Bitmap, left: Int, top: Int, width: Int, height: Int): Bitmap? {
            return try {
                if (left >= 0 && top >= 0 &&
                    left + width <= bitmap.width &&
                    top + height <= bitmap.height) {
                    Bitmap.createBitmap(bitmap, left, top, width, height)
                } else {
                    null
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error cropping bitmap", e)
                null
            }
        }
    }
    
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())
        setupScreenCapture()
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Screen Capture",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }
    
    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("AutoSim Screen Capture")
            .setContentText("Capturing screen for OCR")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .build()
    }
    
    private fun setupScreenCapture() {
        val metrics = resources.displayMetrics
        val width = metrics.widthPixels
        val height = metrics.heightPixels
        val density = metrics.densityDpi
        
        imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)
        
        handlerThread = HandlerThread("ScreenCapture").apply { start() }
        handler = Handler(handlerThread!!.looper)
        
        imageReader?.setOnImageAvailableListener({ reader ->
            serviceScope.launch {
                reader.acquireLatestImage()?.let { image ->
        val captureRunnable = object : Runnable {
            override fun run() {
                if (isCapturing && handler != null) {
                    // Trigger capture by reading latest image
                    imageReader?.acquireLatestImage()?.let { image ->
                        serviceScope.launch {
                            captureImage(image)
                        }
                    }
                    handler?.postDelayed(this, captureInterval)
                }
            }
        }
        handler?.postDelayed(captureRunnable, captureInterval)
    }
    
    private suspend fun captureImage(image: Image) {
        try {
            val planes = image.planes
            val buffer = planes[0].buffer
            val pixelStride = planes[0].pixelStride
            val rowStride = planes[0].rowStride
            val rowPadding = rowStride - pixelStride * image.width
            
            val bitmap = Bitmap.createBitmap(
                image.width + rowPadding / pixelStride,
                image.height,
                Bitmap.Config.ARGB_8888
            )
            bitmap.copyPixelsFromBuffer(buffer)
            
            // Crop to actual size
            val cropped = Bitmap.createBitmap(bitmap, 0, 0, image.width, image.height)
            bitmap.recycle()
            
            _latestBitmap.value = cropped
            image.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error capturing image", e)
            image.close()
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        stopCapture()
        virtualDisplay?.release()
        imageReader?.close()
        handlerThread?.quitSafely()
        mediaProjection?.stop()
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
}

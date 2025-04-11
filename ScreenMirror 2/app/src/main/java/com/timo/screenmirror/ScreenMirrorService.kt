package com.timo.screenmirror

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.*
import android.util.DisplayMetrics
import android.util.Log
import androidx.core.app.NotificationCompat
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.util.*

class ScreenMirrorService : Service() {
    private val TAG = "ScreenMirrorService"
    private val NOTIFICATION_ID = 1
    private val CHANNEL_ID = "ScreenMirrorChannel"
    
    private lateinit var mediaProjection: MediaProjection
    private lateinit var imageReader: ImageReader
    private lateinit var virtualDisplay: VirtualDisplay
    private lateinit var handler: Handler
    private lateinit var firebaseManager: FirebaseManager
    
    private var screenWidth = 0
    private var screenHeight = 0
    private var screenDensity = 0
    private var isRunning = false
    
    override fun onCreate() {
        super.onCreate()
        // Get screen metrics
        val displayMetrics = DisplayMetrics()
        val windowManager = getSystemService(Context.WINDOW_SERVICE) as android.view.WindowManager
        windowManager.defaultDisplay.getMetrics(displayMetrics)
        
        screenWidth = displayMetrics.widthPixels / 2  // Half resolution for performance
        screenHeight = displayMetrics.heightPixels / 2 // Half resolution for performance
        screenDensity = displayMetrics.densityDpi
        
        // Create handler thread for background processing
        val handlerThread = HandlerThread("ScreenCapture", Process.THREAD_PRIORITY_BACKGROUND)
        handlerThread.start()
        handler = Handler(handlerThread.looper)
        
        // Initialize Firebase Manager
        firebaseManager = FirebaseManager()
        
        createNotificationChannel()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null) {
            return START_NOT_STICKY
        }
        
        if (isRunning) {
            return START_STICKY
        }
        
        val resultCode = intent.getIntExtra("resultCode", Activity.RESULT_CANCELED)
        val data = intent.getParcelableExtra<Intent>("data")
        
        if (resultCode != Activity.RESULT_OK || data == null) {
            Log.e(TAG, "Missing or invalid screen capture permission")
            return START_NOT_STICKY
        }
        
        startForeground(NOTIFICATION_ID, createNotification())
        
        // Initialize MediaProjection
        val projectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        mediaProjection = projectionManager.getMediaProjection(resultCode, data)
        
        // Set up ImageReader
        imageReader = ImageReader.newInstance(
            screenWidth, 
            screenHeight, 
            PixelFormat.RGBA_8888, 
            2
        )
        
        // Create virtual display
        virtualDisplay = mediaProjection.createVirtualDisplay(
            "ScreenCapture",
            screenWidth,
            screenHeight,
            screenDensity,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader.surface,
            null,
            handler
        )
        
        // Start periodic capture
        isRunning = true
        startPeriodicCapture()
        
        return START_STICKY
    }
    
    private fun startPeriodicCapture() {
        handler.post(object : Runnable {
            override fun run() {
                if (isRunning) {
                    captureScreen()
                    // Schedule next capture in 5 seconds
                    handler.postDelayed(this, 5000)
                }
            }
        })
    }
    
    private fun captureScreen() {
        try {
            val image = imageReader.acquireLatestImage()
            if (image != null) {
                val bitmap = imageToBitmap(image)
                image.close()
                
                if (bitmap != null) {
                    // Save bitmap to temporary file
                    val file = File(cacheDir, "screen.jpg")
                    saveBitmapToFile(bitmap, file)
                    
                    // Upload to Firebase
                    firebaseManager.uploadScreenshot(file)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error capturing screen: ${e.message}")
        }
    }
    
    private fun imageToBitmap(image: Image): Bitmap? {
        val planes = image.planes
        val buffer = planes[0].buffer
        val pixelStride = planes[0].pixelStride
        val rowStride = planes[0].rowStride
        val rowPadding = rowStride - pixelStride * screenWidth
        
        // Create bitmap
        val bitmap = Bitmap.createBitmap(
            screenWidth + rowPadding / pixelStride,
            screenHeight,
            Bitmap.Config.ARGB_8888
        )
        
        bitmap.copyPixelsFromBuffer(buffer)
        return bitmap
    }
    
    private fun saveBitmapToFile(bitmap: Bitmap, file: File): Boolean {
        return try {
            val fos = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 70, fos)
            fos.flush()
            fos.close()
            true
        } catch (e: IOException) {
            Log.e(TAG, "Error saving bitmap: ${e.message}")
            false
        }
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Screen Mirror Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Screen mirroring service notification"
            }
            
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    private fun createNotification(): Notification {
        val notificationIntent = Intent(this, SenderActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
        )
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Screen Mirroring Active")
            .setContentText("Timo screen is being mirrored")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pendingIntent)
            .build()
    }
    
    override fun onDestroy() {
        isRunning = false
        
        if (::virtualDisplay.isInitialized) {
            virtualDisplay.release()
        }
        
        if (::imageReader.isInitialized) {
            imageReader.close()
        }
        
        if (::mediaProjection.isInitialized) {
            mediaProjection.stop()
        }
        
        super.onDestroy()
    }
    
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}

package com.timo.screenmirror

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Intent
import android.graphics.Path
import android.util.Log
import android.view.Display
import android.view.accessibility.AccessibilityEvent
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener

class RemoteControlService : AccessibilityService() {
    private val TAG = "RemoteControlService"
    private lateinit var firebaseManager: FirebaseManager
    private var screenWidth = 0
    private var screenHeight = 0
    
    override fun onCreate() {
        super.onCreate()
        firebaseManager = FirebaseManager()
    }
    
    override fun onServiceConnected() {
        super.onServiceConnected()
        
        // Get screen dimensions
        val display = getSystemService(WINDOW_SERVICE) as android.view.WindowManager
        val size = android.graphics.Point()
        display.defaultDisplay.getRealSize(size)
        screenWidth = size.x
        screenHeight = size.y
        
        Log.d(TAG, "RemoteControlService connected: screen size ${screenWidth}x${screenHeight}")
        
        // Start listening for remote touch events
        listenForTouchEvents()
    }
    
    private fun listenForTouchEvents() {
        firebaseManager.getTouchEventReference().addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val touchEvent = snapshot.getValue(TouchEvent::class.java)
                if (touchEvent != null && touchEvent.timestamp > System.currentTimeMillis() - 5000) {
                    // Process only recent touch events (within 5 seconds)
                    performTouch(touchEvent)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Firebase touch event listener cancelled: ${error.message}")
            }
        })
    }
    
    private fun performTouch(touchEvent: TouchEvent) {
        // Convert relative coordinates to screen coordinates
        val x = touchEvent.x * screenWidth
        val y = touchEvent.y * screenHeight
        
        Log.d(TAG, "Performing touch at: $x, $y, action: ${touchEvent.action}")
        
        val path = Path()
        path.moveTo(x.toFloat(), y.toFloat())
        
        val gestureBuilder = GestureDescription.Builder()
        val gesture = gestureBuilder
            .addStroke(GestureDescription.StrokeDescription(path, 0, 100))
            .build()
            
        dispatchGesture(gesture, object : GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription?) {
                super.onCompleted(gestureDescription)
                Log.d(TAG, "Touch gesture completed")
            }

            override fun onCancelled(gestureDescription: GestureDescription?) {
                super.onCancelled(gestureDescription)
                Log.e(TAG, "Touch gesture cancelled")
            }
        }, null)
    }
    
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Not used in this implementation
    }
    
    override fun onInterrupt() {
        Log.d(TAG, "RemoteControlService interrupted")
    }
}

// Data class for touch events stored in Firebase
data class TouchEvent(
    val x: Float = 0f,        // Relative X coordinate (0-1)
    val y: Float = 0f,        // Relative Y coordinate (0-1)
    val action: Int = 0,      // MotionEvent action
    val timestamp: Long = 0   // Event timestamp
)

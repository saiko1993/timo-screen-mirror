package com.timo.screenmirror

import android.net.Uri
import android.util.Log
import android.view.MotionEvent
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import java.io.File

class FirebaseManager {
    private val TAG = "FirebaseManager"
    private val database: FirebaseDatabase = FirebaseDatabase.getInstance()
    private val storage: FirebaseStorage = FirebaseStorage.getInstance()
    
    // Database references
    private val lastUpdateRef: DatabaseReference = database.getReference("lastUpdated")
    private val touchEventRef: DatabaseReference = database.getReference("touchEvent")
    
    // Storage references
    private val screenshotRef: StorageReference = storage.reference.child("screenshots/screen.jpg")
    
    /**
     * Upload screenshot file to Firebase Storage
     */
    fun uploadScreenshot(file: File) {
        val uri = Uri.fromFile(file)
        val uploadTask = screenshotRef.putFile(uri)
        
        uploadTask.addOnSuccessListener {
            // Update timestamp in the database after successful upload
            updateLastUpdateTimestamp()
            Log.d(TAG, "Screenshot uploaded successfully")
        }.addOnFailureListener { exception ->
            Log.e(TAG, "Screenshot upload failed: ${exception.message}")
        }
    }
    
    /**
     * Update the timestamp in Firebase Database
     */
    private fun updateLastUpdateTimestamp() {
        val timestamp = System.currentTimeMillis()
        lastUpdateRef.setValue(timestamp)
            .addOnSuccessListener {
                Log.d(TAG, "Timestamp updated successfully")
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Timestamp update failed: ${exception.message}")
            }
    }
    
    /**
     * Send a touch event to Firebase
     */
    fun sendTouchEvent(x: Float, y: Float, action: Int) {
        val touchEvent = TouchEvent(
            x = x,
            y = y,
            action = action,
            timestamp = System.currentTimeMillis()
        )
        
        touchEventRef.setValue(touchEvent)
            .addOnSuccessListener {
                Log.d(TAG, "Touch event sent: $x, $y, action: $action")
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Touch event failed: ${exception.message}")
            }
    }
    
    /**
     * Get reference to lastUpdated in Firebase Database
     */
    fun getLastUpdateReference(): DatabaseReference {
        return lastUpdateRef
    }
    
    /**
     * Get reference to touchEvent in Firebase Database
     */
    fun getTouchEventReference(): DatabaseReference {
        return touchEventRef
    }
}

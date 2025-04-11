package com.timo.screenmirror

import android.os.Bundle
import android.view.MotionEvent
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import java.text.SimpleDateFormat
import java.util.*

class ReceiverActivity : AppCompatActivity() {

    private lateinit var screenImageView: ImageView
    private lateinit var statusTextView: TextView
    private lateinit var lastUpdateTextView: TextView
    private val firebaseManager = FirebaseManager()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_receiver)
        
        screenImageView = findViewById(R.id.ivScreenshot)
        statusTextView = findViewById(R.id.tvStatus)
        lastUpdateTextView = findViewById(R.id.tvLastUpdate)
        
        setupTouchListener()
        startListeningForUpdates()
    }
    
    private fun setupTouchListener() {
        screenImageView.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE, MotionEvent.ACTION_UP -> {
                    // Calculate relative coordinates (0-1)
                    val relativeX = event.x / screenImageView.width
                    val relativeY = event.y / screenImageView.height
                    
                    // Send touch event to Firebase
                    firebaseManager.sendTouchEvent(relativeX, relativeY, event.action)
                    
                    // Show touch feedback
                    if (event.action == MotionEvent.ACTION_DOWN) {
                        statusTextView.text = "Touch event sent: ${relativeX}, ${relativeY}"
                    }
                    
                    return@setOnTouchListener true
                }
                else -> return@setOnTouchListener false
            }
        }
    }
    
    private fun startListeningForUpdates() {
        statusTextView.text = "Connecting to remote screen..."
        
        // Listen for lastUpdated changes in Firebase
        firebaseManager.getLastUpdateReference().addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val timestamp = snapshot.getValue(Long::class.java)
                if (timestamp != null) {
                    updateScreenshot(timestamp)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                statusTextView.text = "Error: ${error.message}"
            }
        })
    }
    
    private fun updateScreenshot(timestamp: Long) {
        val storageRef = FirebaseStorage.getInstance().reference
        val imageRef = storageRef.child("screenshots/screen.jpg")
        
        // Add a timestamp query parameter to avoid caching
        imageRef.downloadUrl.addOnSuccessListener { uri ->
            // Use Glide to load the image
            Glide.with(this)
                .load("$uri?t=$timestamp")
                .placeholder(R.drawable.ic_loading)
                .error(R.drawable.ic_error)
                .into(screenImageView)
            
            // Update timestamp text
            val dateFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
            val date = Date(timestamp)
            lastUpdateTextView.text = "Last update: ${dateFormat.format(date)}"
            
            statusTextView.text = "Connected to remote screen"
        }.addOnFailureListener {
            statusTextView.text = "Failed to load screenshot: ${it.message}"
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // Remove Firebase listeners
        firebaseManager.getLastUpdateReference().removeEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {}
            override fun onCancelled(error: DatabaseError) {}
        })
    }
}

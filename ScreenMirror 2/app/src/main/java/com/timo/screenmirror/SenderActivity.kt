package com.timo.screenmirror

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class SenderActivity : AppCompatActivity() {
    
    private lateinit var statusTextView: TextView
    private lateinit var startButton: Button
    private lateinit var stopButton: Button
    private lateinit var accessibilityButton: Button
    
    companion object {
        private const val REQUEST_MEDIA_PROJECTION = 1
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sender)
        
        statusTextView = findViewById(R.id.tvStatus)
        startButton = findViewById(R.id.btnStartMirroring)
        stopButton = findViewById(R.id.btnStopMirroring)
        accessibilityButton = findViewById(R.id.btnAccessibilitySettings)
        
        setupButtons()
        updateUIState()
    }
    
    private fun setupButtons() {
        startButton.setOnClickListener {
            requestScreenCapturePermission()
        }
        
        stopButton.setOnClickListener {
            stopScreenMirroring()
        }
        
        accessibilityButton.setOnClickListener {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            startActivity(intent)
        }
    }
    
    private fun requestScreenCapturePermission() {
        val mediaProjectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        startActivityForResult(
            mediaProjectionManager.createScreenCaptureIntent(),
            REQUEST_MEDIA_PROJECTION
        )
    }
    
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        
        if (requestCode == REQUEST_MEDIA_PROJECTION) {
            if (resultCode == Activity.RESULT_OK && data != null) {
                startScreenMirroring(resultCode, data)
            } else {
                Toast.makeText(this, "Screen capture permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun startScreenMirroring(resultCode: Int, data: Intent) {
        val serviceIntent = Intent(this, ScreenMirrorService::class.java).apply {
            putExtra("resultCode", resultCode)
            putExtra("data", data)
        }
        
        startService(serviceIntent)
        updateUIState()
    }
    
    private fun stopScreenMirroring() {
        val serviceIntent = Intent(this, ScreenMirrorService::class.java)
        stopService(serviceIntent)
        updateUIState()
    }
    
    private fun updateUIState() {
        val isServiceRunning = Utils.isServiceRunning(this, ScreenMirrorService::class.java)
        val isAccessibilityEnabled = Utils.isAccessibilityServiceEnabled(this, RemoteControlService::class.java)
        
        startButton.isEnabled = !isServiceRunning
        stopButton.isEnabled = isServiceRunning
        
        if (isServiceRunning) {
            statusTextView.text = "Screen mirroring is active"
        } else {
            statusTextView.text = "Screen mirroring is inactive"
        }
        
        if (!isAccessibilityEnabled) {
            statusTextView.text = "${statusTextView.text}\nAccessibility service is disabled. Remote control won't work."
            accessibilityButton.isEnabled = true
        } else {
            accessibilityButton.isEnabled = false
        }
    }
    
    override fun onResume() {
        super.onResume()
        updateUIState()
    }
}

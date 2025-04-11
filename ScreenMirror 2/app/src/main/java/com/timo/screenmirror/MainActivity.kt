package com.timo.screenmirror

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.FirebaseApp

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        // Initialize Firebase
        FirebaseApp.initializeApp(this)

        // Setup Sender button
        findViewById<Button>(R.id.btnSender).setOnClickListener {
            val intent = Intent(this, SenderActivity::class.java)
            startActivity(intent)
        }

        // Setup Receiver button
        findViewById<Button>(R.id.btnReceiver).setOnClickListener {
            val intent = Intent(this, ReceiverActivity::class.java)
            startActivity(intent)
        }
    }
}

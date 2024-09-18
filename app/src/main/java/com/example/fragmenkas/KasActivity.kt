package com.example.fragmenkas


import android.os.Bundle
import android.widget.Button
import androidx.activity.ComponentActivity


class KasActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_kas)

        val button: Button = findViewById(R.id.button)

        button.setOnClickListener {
            // Kembali ke frame (activity) awal
            finish()
        }
    }
}


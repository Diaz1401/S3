package com.wba

import android.os.Bundle
import android.widget.Button
import androidx.activity.ComponentActivity


class KasActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_kas)

        val button: Button = findViewById(R.id.buttonKasBack)

        button.setOnClickListener {
            // Kembali ke frame (activity) awal
            finish()
        }
    }
}


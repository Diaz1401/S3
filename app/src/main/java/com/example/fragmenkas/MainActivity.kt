package com.example.fragmenkas

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.fragmenkas.ui.theme.FragmenkasTheme
import android.os.Handler
import android.os.Looper
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import android.widget.ImageView
class MainActivity : ComponentActivity() {

    private lateinit var imageView: ImageView
    private val PICK_IMAGE = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.layouttt)
        val imageButton: ImageButton = findViewById(R.id.imageButton)

        imageButton.setOnClickListener {
            // Pindah ke SecondActivity
            val intent = Intent(this, keduaa::class.java)
            startActivity(intent)
        }
        imageView = findViewById(R.id.imageView)

        imageView.setOnClickListener {
            val gallery = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.INTERNAL_CONTENT_URI)
            startActivityForResult(gallery, PICK_IMAGE)
        }

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && requestCode == PICK_IMAGE) {
            val imageUri: Uri? = data?.data
            imageView.setImageURI(imageUri)
        }
    }

}
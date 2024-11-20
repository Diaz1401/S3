package com.siwiba

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.FirebaseApp
import com.siwiba.databinding.ActivityMainBinding
import com.siwiba.databinding.ActivitySignUpBinding
import com.siwiba.wba.SignInActivity

class DaftarAkun: AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_daftar_akun)
        FirebaseApp.initializeApp(this)
    }
}
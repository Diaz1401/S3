package com.siwiba.wba

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatImageView
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.siwiba.MainActivity
import com.siwiba.R
import com.siwiba.databinding.ActivitySignInBinding
import com.siwiba.util.AppMode

class SignInActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySignInBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        val appMode = AppMode(this)
        if (appMode.getAppMode()) {
            setTheme(R.style.Base_Theme_WBA)
        } else {
            setTheme(R.style.Base_Theme_KWI)
        }
        super.onCreate(savedInstanceState)

        auth = FirebaseAuth.getInstance()
        val user = auth.currentUser
        if (user != null) {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }

        binding = ActivitySignInBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firestore = FirebaseFirestore.getInstance()

        binding.btnSignIn.setOnClickListener {
            val email = binding.inputEmailSignIn.text.toString()
            val password = binding.inputPasswordSignIn.text.toString()

            if (isValidSignInDetails(email, password)) {
                signIn()
            }
        }

        binding.imgWBA.setOnClickListener {
            appMode.setAppMode(true)
        }

        binding.imgKWI.setOnClickListener {
            appMode.setAppMode(false)
        }

        if (appMode.getAppMode()) {
            zoomImage(binding.imgKWI, binding.imgWBA )
        } else {
            zoomImage(binding.imgWBA, binding.imgKWI)
        }
    }

    private fun zoomImage(zoomIn: AppCompatImageView, zoomOut: AppCompatImageView) {
        val scaleXsmall = ObjectAnimator.ofFloat(zoomIn, "scaleX", 0.8f)
        val scaleYsmall = ObjectAnimator.ofFloat(zoomIn, "scaleY", 0.8f)
        val scaleXbig = ObjectAnimator.ofFloat(zoomOut, "scaleX", 1.25f)
        val scaleYbig = ObjectAnimator.ofFloat(zoomOut, "scaleY", 1.25f)
        scaleXsmall.duration = 100
        scaleYsmall.duration = 100
        scaleXbig.duration = 100
        scaleYbig.duration = 100

        val animatorSet = AnimatorSet()
        animatorSet.playTogether(scaleXsmall, scaleYsmall, scaleXbig, scaleYbig)
        animatorSet.start()
    }

    private fun isValidSignInDetails(email: String, password: String): Boolean {
        if (email.isEmpty()) {
            binding.inputEmailSignIn.error = "Masukan email"
            binding.inputEmailSignIn.requestFocus()
            return false
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.inputEmailSignIn.error = "Masukan email yang valid"
            binding.inputEmailSignIn.requestFocus()
            return false
        }
        if (password.isEmpty()) {
            binding.inputPasswordSignIn.error = "Masukan password"
            binding.inputPasswordSignIn.requestFocus()
            return false
        }
        return true
    }

    private fun signIn() {
        val email = binding.inputEmailSignIn.text.toString()
        val password = binding.inputPasswordSignIn.text.toString()

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    user?.let {
                        if (it.isEmailVerified) {
                            Toast.makeText(this, "Sign In sukses", Toast.LENGTH_SHORT).show()
                            getUserData(it.uid)
                        } else {
                            Toast.makeText(this, "Verifikasi email terlebih dahulu", Toast.LENGTH_SHORT).show()
                            auth.signOut()
                        }
                    }
                } else {
                    Toast.makeText(this, "Sign In gagal: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun handleSignInResult(task: Task<AuthResult>) {
        if (task.isSuccessful) {
            val user = auth.currentUser
            user?.let {
                getUserData(it.uid)
                Toast.makeText(this, "Sign In sukses", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "Sign In gagal: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getUserData(uid: String) {
        val sharedPref = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            putString("uid", uid)
            apply()
        }

        firestore.collection("users").document(uid).get()
            .addOnSuccessListener { document ->
                val name = document.getString("name") ?: ""
                val email = document.getString("email") ?: ""
                val address = document.getString("address") ?: ""
                val profileImage = document.getString("profileImage") ?: ""
                val isAdmin = document.getBoolean("isAdmin") ?: false
                val jabatan = document.getLong("jabatan")?.toInt() ?: 0

                with(sharedPref.edit()) {
                    putString("name", name)
                    putString("email", email)
                    putString("address", address)
                    putString("profileImage", profileImage)
                    putInt("jabatan", jabatan)
                    putBoolean("isAdmin", isAdmin)
                    apply()
                }

                // Start MainActivity after successfully getting user data
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
                finish()
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Gagal mengambil data pengguna: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
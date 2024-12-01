package com.siwiba.wba

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.OAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.siwiba.MainActivity
import com.siwiba.databinding.ActivitySignInBinding

class SignInActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySignInBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
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

        binding.imgGoogle.setOnClickListener {
            signInWithGoogle()
        }

        binding.txtSignUp.setOnClickListener {
            val intent = Intent(this, SignUpActivity::class.java)
            startActivity(intent)
        }

        binding.txtResetPassword.setOnClickListener {
            val email = binding.inputEmailSignIn.text.toString()
            if (email.isEmpty()) {
                Toast.makeText(this, "Masukan email", Toast.LENGTH_SHORT).show()
                binding.inputEmailSignIn.requestFocus()
            } else {
                auth.sendPasswordResetEmail(email)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Toast.makeText(this, "Link reset password telah dikirim ke email", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(this, "Gagal mengirim link reset password: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
            }
        }
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

    private fun signInWithGoogle() {
        val provider = OAuthProvider.newBuilder("google.com")
        auth.startActivityForSignInWithProvider(this, provider.build())
            .addOnCompleteListener { task ->
                handleSignInResult(task)
            }
    }

    private fun handleSignInResult(task: Task<AuthResult>) {
        if (task.isSuccessful) {
            val user = auth.currentUser
            user?.let {
                getUserData(it.uid)
                Toast.makeText(this, "Sign In sukses", Toast.LENGTH_SHORT).show()
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
                finish()
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
                if (document != null) {
                    val name = document.getString("name")
                    val email = document.getString("email")
                    val address = document.getString("address")
                    val profileImage = document.getString("profileImage")
                    val isAdmin = document.getBoolean("isAdmin") ?: false

                    with(sharedPref.edit()) {
                        putString("name", name)
                        putString("email", email)
                        putString("address", address)
                        putString("profileImage", profileImage)
                        putBoolean("isAdmin", isAdmin)
                        apply()
                    }
                    // Start MainActivity after successfully getting user data
                    val intent = Intent(this, MainActivity::class.java)
                    startActivity(intent)
                    finish()
                } else {
                    Toast.makeText(this, "Data pengguna tidak ditemukan", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Gagal mengambil data pengguna: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
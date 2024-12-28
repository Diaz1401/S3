package com.siwiba.wba

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatImageView
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.siwiba.MainActivity
import com.siwiba.R
import com.siwiba.databinding.ActivitySignInBinding
import com.siwiba.util.AppMode
import com.siwiba.util.EncSharedPref
import com.siwiba.util.RefreshData
import com.siwiba.wba.activity.ManageAccountActivity

class SignInActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySignInBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var refreshData: RefreshData
    private lateinit var sharedPref: SharedPreferences
    private var scopeMode = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        val appMode = AppMode(this)
        if (appMode.getAppMode()) {
            setTheme(R.style.Base_Theme_WBA)
        } else {
            setTheme(R.style.Base_Theme_KWI)
        }
        super.onCreate(savedInstanceState)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        refreshData = RefreshData(this)
        scopeMode = appMode.getScopeMode()
        sharedPref = EncSharedPref(this).getEncSharedPref()
        val user = auth.currentUser

        // Reauthenticate user if already signed in
        if (user != null) {
            val email = sharedPref.getString("email", "") ?: ""
            val password = sharedPref.getString("password", "") ?: ""
            if (email.isNotEmpty() && password.isNotEmpty()) {
                val credential = EmailAuthProvider.getCredential(email, password)
                user.reauthenticate(credential)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            firestore.collection("users").document(user.uid).get().continueWithTask { taskk ->
                                if (taskk.isSuccessful) {
                                    val document = taskk.result
                                    if (document != null) {
                                        val scopeMode = document.getLong("scopeMode")?.toInt() ?: 0
                                        when (scopeMode) {
                                            0 -> {
                                                refreshData.getUserData(user.uid)
                                                openDashboard()
                                            }
                                            1 -> {
                                                if (AppMode(this).getAppMode()) {
                                                    refreshData.getUserData(user.uid)
                                                    openDashboard()
                                                } else {
                                                    auth.signOut()
                                                    Toast.makeText(this, "Anda hanya bisa mengakses WBA", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                            2 -> {
                                                if (!AppMode(this).getAppMode()) {
                                                    refreshData.getUserData(user.uid)
                                                    openDashboard()
                                                } else {
                                                    auth.signOut()
                                                    Toast.makeText(this, "Anda hanya bisa mengakses KWI", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        }
                                    } else {
                                        Toast.makeText(this, "Gagal mengambil data pengguna: ${taskk.exception?.message}", Toast.LENGTH_SHORT).show()
                                    }
                                } else {
                                    Toast.makeText(this, "Gagal mengambil data pengguna: ${taskk.exception?.message}", Toast.LENGTH_SHORT).show()
                                }
                                return@continueWithTask taskk
                            }
                        } else {
                            Toast.makeText(this, "Reautentikasi gagal: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
            }
        }

        binding = ActivitySignInBinding.inflate(layoutInflater)
        setContentView(binding.root)

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

    private fun openDashboard() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
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
                            firestore.collection("users").document(it.uid).get().continueWithTask { taskkk ->
                                if (taskkk.isSuccessful) {
                                    val document = taskkk.result
                                    if (document != null) {
                                        val scopeMode = document.getLong("scopeMode")?.toInt() ?: 0
                                        when (scopeMode) {
                                            0 -> {
                                                refreshData.getUserData(it.uid)
                                                openDashboard()
                                            }
                                            1 -> {
                                                if (AppMode(this).getAppMode()) {
                                                    refreshData.getUserData(it.uid)
                                                    openDashboard()
                                                } else {
                                                    auth.signOut()
                                                    Toast.makeText(this, "Anda hanya bisa mengakses WBA", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                            2 -> {
                                                if (!AppMode(this).getAppMode()) {
                                                    refreshData.getUserData(it.uid)
                                                    openDashboard()
                                                } else {
                                                    auth.signOut()
                                                    Toast.makeText(this, "Anda hanya bisa mengakses KWI", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        }
                                    } else {
                                        Toast.makeText(this, "Gagal mengambil data pengguna: ${taskkk.exception?.message}", Toast.LENGTH_SHORT).show()
                                    }
                                } else {
                                    Toast.makeText(this, "Gagal mengambil data pengguna: ${taskkk.exception?.message}", Toast.LENGTH_SHORT).show()
                                }
                                return@continueWithTask taskkk
                            }
                        } else {
                            Toast.makeText(this, "Email belum diverifikasi", Toast.LENGTH_SHORT).show()
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
                firestore.collection("users").document(it.uid).get().continueWithTask { taskk ->
                    if (taskk.isSuccessful) {
                        val document = taskk.result
                        if (document != null) {
                            val scopeMode = document.getLong("scopeMode")?.toInt() ?: 0
                            when (scopeMode) {
                                0 -> {
                                    refreshData.getUserData(it.uid)
                                    openDashboard()
                                }
                                1 -> {
                                    if (AppMode(this).getAppMode()) {
                                        refreshData.getUserData(it.uid)
                                        openDashboard()
                                    } else {
                                        auth.signOut()
                                        Toast.makeText(this, "Anda hanya bisa mengakses WBA", Toast.LENGTH_SHORT).show()
                                    }
                                }
                                2 -> {
                                    if (!AppMode(this).getAppMode()) {
                                        refreshData.getUserData(it.uid)
                                        openDashboard()
                                    } else {
                                        auth.signOut()
                                        Toast.makeText(this, "Anda hanya bisa mengakses KWI", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        } else {
                            Toast.makeText(this, "Gagal mengambil data pengguna: ${taskk.exception?.message}", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(this, "Gagal mengambil data pengguna: ${taskk.exception?.message}", Toast.LENGTH_SHORT).show()
                    }
                    return@continueWithTask taskk
                }
            }
        } else {
            Toast.makeText(this, "Sign In gagal: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
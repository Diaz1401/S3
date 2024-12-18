package com.siwiba.util

import android.content.Context
import android.widget.Toast
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.FirebaseFirestore

/**
 * A utility class for refreshing user data from Firestore and storing it in shared preferences.
 *
 * @property context The context of the calling component.
 */
class RefreshData(private val context: Context) {
    private val firestore = FirebaseFirestore.getInstance()
    /**
     * Fetches user data from Firestore and stores it in shared preferences.
     *
     * @param uid The unique identifier of the user whose data is to be fetched.
     * @return The scope mode of the user.
     */
    fun getUserData(uid: String): Task<*> {
        // Store the user ID in shared preferences
        val sharedPref = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            putString("uid", uid)
            apply()
        }

        // Fetch user data from Firestore
        val task = firestore.collection("users").document(uid).get().addOnCompleteListener { task ->
            if (task.isComplete) {
                val document = task.result
                if (document != null) {
                    // Retrieve user data from the Firestore document
                    val name = document.getString("name") ?: ""
                    val email = document.getString("email") ?: ""
                    val address = document.getString("address") ?: ""
                    val profileImage = document.getString("profileImage") ?: ""
                    val isAdmin = document.getBoolean("isAdmin") ?: false
                    val jabatan = document.getLong("jabatan")?.toInt() ?: 0
                    val password = document.getString("password") ?: ""
                    val scopeGaji = document.getBoolean("scopeGaji") ?: false
                    val scopePajak = document.getBoolean("scopePajak") ?: false
                    val scopePinjaman = document.getBoolean("scopePinjaman") ?: false
                    val scopeKas = document.getBoolean("scopeKas") ?: false
                    val scopeLogistik = document.getBoolean("scopeLogistik") ?: false
                    val scopeBpjs = document.getBoolean("scopeBpjs") ?: false
                    val scopeTagihan = document.getBoolean("scopeTagihan") ?: false
                    val scopeMode = document.getLong("scopeMode")?.toInt() ?: 0

                    // Store the retrieved user data in shared preferences
                    with(sharedPref.edit()) {
                        putString("name", name)
                        putString("email", email)
                        putString("address", address)
                        putString("profileImage", profileImage)
                        putString("password", password)
                        putInt("jabatan", jabatan)
                        putBoolean("isAdmin", isAdmin)
                        putBoolean("scopeGaji", scopeGaji)
                        putBoolean("scopePajak", scopePajak)
                        putBoolean("scopePinjaman", scopePinjaman)
                        putBoolean("scopeKas", scopeKas)
                        putBoolean("scopeLogistik", scopeLogistik)
                        putBoolean("scopeBpjs", scopeBpjs)
                        putBoolean("scopeTagihan", scopeTagihan)
                        putInt("scopeMode", scopeMode)
                        apply()
                    }
                }
            } else {
                // Show a toast message if fetching user data fails
                Toast.makeText(context, "Gagal mengambil data pengguna: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
            }
        }
        return task
    }
}
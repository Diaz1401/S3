package com.siwiba.util

import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.google.firebase.firestore.FirebaseFirestore
import com.siwiba.MainActivity
import com.siwiba.wba.activity.ManageAccountActivity

/**
 * A utility class for refreshing user data from Firestore and storing it in shared preferences.
 *
 * @property context The context of the calling component.
 * @property firestore The Firestore instance used to fetch user data.
 */
class RefreshData(private val context: Context, private val firestore: FirebaseFirestore) {
    /**
     * Fetches user data from Firestore and stores it in shared preferences.
     *
     * @param uid The unique identifier of the user whose data is to be fetched.
     * @return The scope mode of the user.
     */
    fun getUserData(uid: String) {
        // Store the user ID in shared preferences
        val sharedPref = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            putString("uid", uid)
            apply()
        }

        // Fetch user data from Firestore
        firestore.collection("users").document(uid).get()
            .addOnSuccessListener { document ->
                // Retrieve user data from the Firestore document
                val name = document.getString("name") ?: ""
                val email = document.getString("email") ?: ""
                val address = document.getString("address") ?: ""
                val profileImage = document.getString("profileImage") ?: ""
                val isAdmin = document.getBoolean("isAdmin") ?: false
                val jabatan = document.getLong("jabatan")?.toInt() ?: 0
                val password = document.getString("password") ?: ""
                val saldoGaji = document.getBoolean("saldoGaji") ?: false
                val saldoPajak = document.getBoolean("saldoPajak") ?: false
                val saldoPinjaman = document.getBoolean("saldoPinjaman") ?: false
                val saldoKas = document.getBoolean("saldoKas") ?: false
                val saldoLogistik = document.getBoolean("saldoLogistik") ?: false
                val saldoBpjs = document.getBoolean("saldoBpjs") ?: false
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
                    putBoolean("saldoGaji", saldoGaji)
                    putBoolean("saldoPajak", saldoPajak)
                    putBoolean("saldoPinjaman", saldoPinjaman)
                    putBoolean("saldoKas", saldoKas)
                    putBoolean("saldoLogistik", saldoLogistik)
                    putBoolean("saldoBpjs", saldoBpjs)
                    putInt("scopeMode", scopeMode)
                    apply()
                }
            }
            .addOnFailureListener { exception ->
                // Show a toast message if fetching user data fails
                Toast.makeText(context, "Gagal mengambil data pengguna: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

    fun isAllowedToAccess(appMode: Boolean): Boolean {
        val sharedPref = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        val scopeMode = sharedPref.getInt("scopeMode", 0)
        return if (appMode) {
            scopeMode == 0 || scopeMode == 1
        } else {
            scopeMode == 0 || scopeMode == 2
        }
    }
}
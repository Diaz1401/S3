package com.siwiba.util

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * A utility class for managing encrypted shared preferences.
 *
 * @property context The context of the calling component.
 */
class EncSharedPref(private val context: Context) {

    // MasterKey instance for encrypting the shared preferences
    private val masterKeys: MasterKey = MasterKey.Builder(context, MasterKey.DEFAULT_MASTER_KEY_ALIAS)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    // EncryptedSharedPreferences instance for storing user preferences securely
    private val sharedPreferences: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        "user_prefs",
        masterKeys,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    /**
     * Returns the encrypted shared preferences instance.
     *
     * @return The encrypted shared preferences.
     */
    fun getEncSharedPref(): SharedPreferences {
        return sharedPreferences
    }
}
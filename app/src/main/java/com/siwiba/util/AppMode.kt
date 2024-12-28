package com.siwiba.util

import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity

/**
 * A utility class for managing the application mode (theme) settings.
 *
 * @property context The context of the current activity.
 */
class AppMode(private val context: AppCompatActivity) {

    private val sharedPref = EncSharedPref(context).getEncSharedPref()

    /**
     * Retrieves the current application mode from shared preferences.
     *
     * @return `true` if the app mode is WBA, `false` if the app mode is KWI.
     */
    fun getAppMode(): Boolean {
        return sharedPref.getBoolean("appMode", true)
    }

    /**
     * Sets the application mode and saves it to shared preferences.
     *
     * @param appMode The new application mode to set. `true` for WBA, `false` for KWI.
     */
    fun setAppMode(appMode: Boolean) {
        with(sharedPref.edit()) {
            putBoolean("appMode", appMode)
            apply()
        }
        context.recreate()
    }

    /**
     * Retrieves the current scope mode from shared preferences.
     *
     * @return The current scope mode as an integer.
     */
    fun getScopeMode(): Int {
        return sharedPref.getInt("scopeMode", 0)
    }
}
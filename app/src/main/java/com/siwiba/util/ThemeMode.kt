// ThemeMode.kt
package com.siwiba.util

import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import com.siwiba.R

class ThemeMode(private val context: AppCompatActivity) {

    fun getSavedTheme(): Int {
        val sharedPref: SharedPreferences = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        return sharedPref.getInt("theme", R.style.Base_Theme_WBA) // Default theme
    }

    fun applyTheme(themeName: String) {
        val sharedPref: SharedPreferences = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            putInt("theme", when (themeName) {
                "WBA" -> R.style.Base_Theme_WBA
                "KWI" -> R.style.Base_Theme_KWI
                else -> R.style.Base_Theme_WBA
            })
            apply()
        }
        context.recreate()
    }
}
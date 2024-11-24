package com.siwiba

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.siwiba.databinding.ActivityMainBinding
import com.siwiba.wba.fragment.AbsenFragment
import com.siwiba.wba.fragment.DashboardFragment
import com.siwiba.wba.fragment.KeuanganFragment
import com.siwiba.wba.fragment.AnalisisFragment
import com.siwiba.wba.fragment.ProfilFragment

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupBottomNavigation()
    }

    private fun setupBottomNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_dashboard -> {
                    loadFragment(DashboardFragment())
                    true
                }
                R.id.navigation_keuangan -> {
                    loadFragment(KeuanganFragment())
                    true
                }
                R.id.navigation_absen -> {
                    loadFragment(AbsenFragment())
                    true
                }
                R.id.navigation_analisis -> {
                    loadFragment(AnalisisFragment())
                    true
                }
                R.id.navigation_profil -> {
                    loadFragment(ProfilFragment())
                    true
                }
                else -> false
            }
        }
        binding.bottomNavigation.selectedItemId = R.id.navigation_dashboard
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }
}

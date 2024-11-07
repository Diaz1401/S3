package com.siwiba

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.siwiba.databinding.ActivityMainBinding
import com.siwiba.wba.fragment.DashboardFragment
import com.siwiba.wba.fragment.KeuanganFragment
import com.siwiba.wba.fragment.LogistikFragment
import com.siwiba.wba.fragment.SDMFragment

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
                    loadFragment(LogistikFragment())
                    true
                }
                R.id.navigation_sdm -> {
                    loadFragment(KeuanganFragment())
                    true
                }
                R.id.navigation_logistik -> {
                    loadFragment(SDMFragment())
                    true
                }
                R.id.navigation_profil -> {
                    loadFragment(SDMFragment())
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
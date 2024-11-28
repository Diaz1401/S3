package com.siwiba

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.siwiba.databinding.ActivityMainBinding
import com.siwiba.wba.fragment.AbsenFragment
import com.siwiba.wba.fragment.DashboardFragment
import com.siwiba.wba.fragment.KeuanganFragment
import com.siwiba.wba.fragment.AnalisisFragment
import com.siwiba.wba.fragment.ProfilFragment

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
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
//                R.id.navigation_absen -> {
//                    loadFragment(AbsenFragment())
//                    true
//                }
//                R.id.navigation_analisis -> {
//                    loadFragment(AnalisisFragment())
//                    true
//                }
//                R.id.navigation_profil -> {
//                    loadFragment(ProfilFragment())
//                    true
//                }
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

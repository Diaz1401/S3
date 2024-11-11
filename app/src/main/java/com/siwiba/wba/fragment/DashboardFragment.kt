package com.siwiba.wba.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.firebase.FirebaseApp
import com.siwiba.R

class DashboardFragment: Fragment(R.layout.fragment_dashboard) {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inisialisasi Firebase
        FirebaseApp.initializeApp(requireContext())

        // Return the view for the fragment
        return inflater.inflate(R.layout.fragment_dashboard, container, false)
    }
}

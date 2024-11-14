package com.siwiba.wba.fragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import com.google.firebase.FirebaseApp
import com.siwiba.R
import com.siwiba.KirimUang// Ganti dengan aktivitas tujuan Anda

class DashboardFragment : Fragment(R.layout.fragment_dashboard) {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inisialisasi Firebase
        FirebaseApp.initializeApp(requireContext())

        // Return the view for the fragment
        return inflater.inflate(R.layout.fragment_dashboard, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Inisialisasi tombol dan intent

    }
}

package com.siwiba.wba.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.firebase.FirebaseApp
import com.siwiba.R

class AbsenFragment : Fragment(R.layout.fragment_absen) {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inisialisasi Firebase
        FirebaseApp.initializeApp(requireContext())

        // Return the view for the fragment
        return inflater.inflate(R.layout.fragment_absen, container, false)
    }
}

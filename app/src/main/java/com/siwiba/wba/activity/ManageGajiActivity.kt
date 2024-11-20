package com.siwiba.wba.activity

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore
import com.siwiba.databinding.ActivityManageGajiBinding

class ManageGajiActivity : AppCompatActivity() {

    private lateinit var binding: ActivityManageGajiBinding
    private lateinit var firestore: FirebaseFirestore
    private var mode: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityManageGajiBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize Firestore
        firestore = FirebaseFirestore.getInstance()

        // Get mode from intent
        mode = intent.getIntExtra("mode", 0)

        when (mode) {
            1 -> setupAddMode()
            2 -> setupDeleteMode()
            3 -> setupUpdateMode()
            else -> Toast.makeText(this, "Invalid mode", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupAddMode() {
        binding.btnSave.setOnClickListener {
            val no = binding.etNo.text.toString()
            val karyawan = binding.etKaryawan.text.toString()
            val posisi = binding.etPosisi.text.toString()
            val gaji = binding.etGaji.text.toString()
            val tanggal = binding.etTanggal.text.toString()

            if (no.isEmpty() || karyawan.isEmpty() || posisi.isEmpty() || gaji.isEmpty() || tanggal.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            } else {
                val gajiData = mapOf(
                    "no" to no,
                    "karyawan" to karyawan,
                    "posisi" to posisi,
                    "gaji" to gaji,
                    "tanggal" to tanggal
                )

                firestore.collection("gaji")
                    .add(gajiData)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Data saved successfully", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Failed to save data", Toast.LENGTH_SHORT).show()
                    }
            }
        }
    }

    private fun setupDeleteMode() {
        // Implement delete functionality
    }

    private fun setupUpdateMode() {
        // Implement update functionality
    }
}
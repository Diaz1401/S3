package com.siwiba.wba.activity

import android.app.DatePickerDialog
import android.os.Bundle
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.siwiba.databinding.ActivityManageGajiBinding
import java.util.*

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
            2 -> {
                setupDeleteMode()
                setupUpdateMode()
            }
            else -> {
                Toast.makeText(this, "Invalid mode", Toast.LENGTH_SHORT).show()
                finish()
            }
        }

        val etTanggal: EditText = binding.etTanggal
        etTanggal.setOnClickListener {
            showDatePickerDialog(etTanggal)
        }
    }

    private fun setupAddMode() {
        binding.btnSave.setOnClickListener {
            val karyawan = binding.etKaryawan.text.toString()
            val posisi = binding.etPosisi.text.toString()
            val gaji = binding.etGaji.text.toString()
            val tanggal = binding.etTanggal.text.toString()

            if (karyawan.isEmpty() || posisi.isEmpty() || gaji.isEmpty() || tanggal.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            } else {
                // Fetch the highest current "no" value
                firestore.collection("gaji")
                    .orderBy("no", Query.Direction.DESCENDING)
                    .limit(1)
                    .get()
                    .addOnSuccessListener { documents ->
                        var newNo = 1
                        if (!documents.isEmpty) {
                            val highestNo = documents.documents[0].getLong("no") ?: 0
                            newNo = highestNo.toInt() + 1
                        }

                        val gajiData = mapOf(
                            "no" to newNo,
                            "karyawan" to karyawan,
                            "posisi" to posisi,
                            "gaji" to gaji,
                            "tanggal" to tanggal
                        )

                        // Use the new "no" as the document ID
                        firestore.collection("gaji")
                            .document(newNo.toString())
                            .set(gajiData)
                            .addOnSuccessListener {
                                Toast.makeText(this, "Data saved successfully", Toast.LENGTH_SHORT).show()
                                finish()
                            }
                            .addOnFailureListener {
                                Toast.makeText(this, "Failed to save data", Toast.LENGTH_SHORT).show()
                            }
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Failed to fetch data", Toast.LENGTH_SHORT).show()
                    }
            }
        }
    }

    private fun setupDeleteMode() {
        // Implement delete functionality
        val no = intent.getIntExtra("no", 0)
        val karyawan = intent.getStringExtra("karyawan")
        val posisi = intent.getStringExtra("posisi")
        val gaji = intent.getStringExtra("gaji")
        val tanggal = intent.getStringExtra("tanggal")
        binding.etKaryawan.setText(karyawan)
        binding.etPosisi.setText(posisi)
        binding.etGaji.setText(gaji)
        binding.etTanggal.setText(tanggal)
        binding.btnDelete.setOnClickListener {
            firestore.collection("gaji")
                .document(no.toString())
                .delete()
                .addOnSuccessListener {
                    Toast.makeText(this, "Data deleted successfully", Toast.LENGTH_SHORT).show()
                    finish()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Failed to delete data", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun setupUpdateMode() {
        // Implement update functionality
        val no = intent.getIntExtra("no", 0)
        val karyawan = intent.getStringExtra("karyawan")
        val posisi = intent.getStringExtra("posisi")
        val gaji = intent.getStringExtra("gaji")
        val tanggal = intent.getStringExtra("tanggal")
        binding.etKaryawan.setText(karyawan)
        binding.etPosisi.setText(posisi)
        binding.etGaji.setText(gaji)
        binding.etTanggal.setText(tanggal)
        binding.btnSave.setOnClickListener {
            val newKaryawan = binding.etKaryawan.text.toString()
            val newPosisi = binding.etPosisi.text.toString()
            val newGaji = binding.etGaji.text.toString()
            val newTanggal = binding.etTanggal.text.toString()

            if (newKaryawan.isEmpty() || newPosisi.isEmpty() || newGaji.isEmpty() || newTanggal.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            } else {
                val gajiData = mapOf(
                    "no" to no,
                    "karyawan" to newKaryawan,
                    "posisi" to newPosisi,
                    "gaji" to newGaji,
                    "tanggal" to newTanggal
                )

                firestore.collection("gaji")
                    .document(no.toString())
                    .set(gajiData)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Data updated successfully", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Failed to update data", Toast.LENGTH_SHORT).show()
                    }
            }
        }
    }

    private fun showDatePickerDialog(editText: EditText) {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(
            this,
            { _, selectedYear, selectedMonth, selectedDay ->
                val selectedDate = "$selectedDay/${selectedMonth + 1}/$selectedYear"
                editText.setText(selectedDate)
            },
            year, month, day
        )
        datePickerDialog.show()
    }
}
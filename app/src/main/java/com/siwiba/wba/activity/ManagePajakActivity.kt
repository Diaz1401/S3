package com.siwiba.wba.activity

import android.app.DatePickerDialog
import android.os.Bundle
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.siwiba.databinding.ActivityManagePajakBinding
import java.util.*

class ManagePajakActivity : AppCompatActivity() {

    private lateinit var binding: ActivityManagePajakBinding
    private lateinit var firestore: FirebaseFirestore
    private var mode: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityManagePajakBinding.inflate(layoutInflater)
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
            val jenis = binding.etKaryawan.text.toString()
            val nominal = binding.etPosisi.text.toString()
            val periode = binding.etGaji.text.toString()
            val tanggal = binding.etTanggal.text.toString()

            if (jenis.isEmpty() || nominal.isEmpty() || periode.isEmpty() || tanggal.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            } else {
                // Fetch the highest current "no" value
                firestore.collection("pajak")
                    .orderBy("no", Query.Direction.DESCENDING)
                    .limit(1)
                    .get()
                    .addOnSuccessListener { documents ->
                        var newNo = 1
                        if (!documents.isEmpty) {
                            val highestNo = documents.documents[0].getLong("no") ?: 0
                            newNo = highestNo.toInt() + 1
                        }

                        val pajakData = mapOf(
                            "no" to newNo,
                            "jenis" to jenis,
                            "nominal" to nominal,
                            "periode" to periode,
                            "tanggal" to tanggal
                        )

                        // Use the new "no" as the document ID
                        firestore.collection("pajak")
                            .document(newNo.toString())
                            .set(pajakData)
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
        val jenis = intent.getStringExtra("jenis")
        val nominal = intent.getStringExtra("nominal")
        val periode = intent.getStringExtra("periode")
        val tanggal = intent.getStringExtra("tanggal")
        binding.etKaryawan.setText(jenis)
        binding.etPosisi.setText(nominal)
        binding.etGaji.setText(periode)
        binding.etTanggal.setText(tanggal)
        binding.btnDelete.setOnClickListener {
            firestore.collection("pajak")
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
        val jenis = intent.getStringExtra("jenis")
        val nominal = intent.getStringExtra("nominal")
        val periode = intent.getStringExtra("periode")
        val tanggal = intent.getStringExtra("tanggal")
        binding.etKaryawan.setText(jenis)
        binding.etPosisi.setText(nominal)
        binding.etGaji.setText(periode)
        binding.etTanggal.setText(tanggal)
        binding.btnSave.setOnClickListener {
            val newJenis = binding.etKaryawan.text.toString()
            val newNominal = binding.etPosisi.text.toString()
            val newPeriode = binding.etGaji.text.toString()
            val newTanggal = binding.etTanggal.text.toString()

            if (newJenis.isEmpty() || newNominal.isEmpty() || newPeriode.isEmpty() || newTanggal.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            } else {
                val pajakData = mapOf(
                    "no" to no,
                    "jenis" to newJenis,
                    "nominal" to newNominal,
                    "periode" to newPeriode,
                    "tanggal" to newTanggal
                )

                firestore.collection("pajak")
                    .document(no.toString())
                    .set(pajakData)
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
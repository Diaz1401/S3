package com.siwiba.wba.activity

import android.app.DatePickerDialog
import android.os.Bundle
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.siwiba.databinding.ActivityManagePinjamanBinding
import java.util.*

class ManagePinjamanActivity : AppCompatActivity() {

    private lateinit var binding: ActivityManagePinjamanBinding
    private lateinit var firestore: FirebaseFirestore
    private var mode: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityManagePinjamanBinding.inflate(layoutInflater)
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

        val etTanggalPinjam = binding.etTanggalPinjam
        etTanggalPinjam.setOnClickListener {
            showDatePickerDialog(etTanggalPinjam)
        }

        val etTanggalBayar = binding.etTanggalBayar
        etTanggalBayar.setOnClickListener {
            showDatePickerDialog(etTanggalBayar)
        }
    }

    private fun setupAddMode() {
        binding.btnSave.setOnClickListener {
            val deskripsi = binding.etDeskripsi.text.toString()
            val jumlahPinjaman = binding.etJumlahPinjaman.text.toString()
            val tanggalPinjam = binding.etTanggalPinjam.text.toString()
            val tanggalBayar = binding.etTanggalBayar.text.toString()

            if (deskripsi.isEmpty() || jumlahPinjaman.isEmpty() || tanggalPinjam.isEmpty() || tanggalBayar.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            } else {
                // Fetch the highest current "no" value
                firestore.collection("pinjaman")
                    .orderBy("no", Query.Direction.DESCENDING)
                    .limit(1)
                    .get()
                    .addOnSuccessListener { documents ->
                        var newNo = 1
                        if (!documents.isEmpty) {
                            val highestNo = documents.documents[0].getLong("no") ?: 0
                            newNo = highestNo.toInt() + 1
                        }

                        val formData = mapOf(
                            "no" to newNo,
                            "deskripsi" to deskripsi,
                            "jumlahPinjaman" to jumlahPinjaman,
                            "tanggalPinjam" to tanggalPinjam,
                            "tanggalBayar" to tanggalBayar
                        )

                        // Use the new "no" as the document ID
                        firestore.collection("pinjaman")
                            .document(newNo.toString())
                            .set(formData)
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
        val deskripsi = intent.getStringExtra("deskripsi")
        val jumlahPinjaman = intent.getStringExtra("jumlahPinjaman")
        val tanggalPinjam = intent.getStringExtra("tanggalPinjam")
        val tanggalBayar = intent.getStringExtra("tanggalBayar")
        binding.etDeskripsi.setText(deskripsi)
        binding.etJumlahPinjaman.setText(jumlahPinjaman)
        binding.etTanggalPinjam.setText(tanggalPinjam)
        binding.etTanggalBayar.setText(tanggalBayar)
        binding.btnDelete.setOnClickListener {
            firestore.collection("pinjaman")
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
        val deskripsi = intent.getStringExtra("deskripsi")
        val jumlahPinjaman = intent.getStringExtra("jumlahPinjaman")
        val tanggalPinjam = intent.getStringExtra("tanggalPinjam")
        val tanggalBayar = intent.getStringExtra("tanggalBayar")
        binding.etDeskripsi.setText(deskripsi)
        binding.etJumlahPinjaman.setText(jumlahPinjaman)
        binding.etTanggalPinjam.setText(tanggalPinjam)
        binding.etTanggalBayar.setText(tanggalBayar)
        binding.btnSave.setOnClickListener {
            val newDeskripsi = binding.etDeskripsi.text.toString()
            val newJumlahPinjaman = binding.etJumlahPinjaman.text.toString()
            val newTanggalPinjam = binding.etTanggalPinjam.text.toString()
            val newTanggalBayar = binding.etTanggalBayar.text.toString()

            if (newDeskripsi.isEmpty() || newJumlahPinjaman.isEmpty() || newTanggalPinjam.isEmpty() || newTanggalBayar.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            } else {
                val formData = mapOf(
                    "no" to no,
                    "deskripsi" to newDeskripsi,
                    "jumlahPinjaman" to newJumlahPinjaman,
                    "tanggalPinjam" to newTanggalPinjam,
                    "tanggalBayar" to newTanggalBayar
                )

                firestore.collection("pinjaman")
                    .document(no.toString())
                    .set(formData)
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
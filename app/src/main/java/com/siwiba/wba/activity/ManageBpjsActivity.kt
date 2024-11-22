package com.siwiba.wba.activity

import android.app.DatePickerDialog
import android.os.Bundle
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.siwiba.databinding.ActivityManageBpjsBinding
import java.util.*

class ManageBpjsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityManageBpjsBinding
    private lateinit var firestore: FirebaseFirestore
    private var mode: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityManageBpjsBinding.inflate(layoutInflater)
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
            var nama = binding.etNama.text.toString()
            var jmlPeserta = binding.etJumlahPeserta.text.toString()
            var totalBayar = binding.etTotal.text.toString()
            var tanggal = binding.etTanggal.text.toString()

            if (nama.isEmpty() || jmlPeserta.isEmpty() || totalBayar.isEmpty() || tanggal.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            } else {
                // Fetch the highest current "no" value
                firestore.collection("bpjs")
                    .orderBy("no", Query.Direction.DESCENDING)
                    .limit(1)
                    .get()
                    .addOnSuccessListener { documents ->
                        var newNo = 1
                        if (!documents.isEmpty) {
                            val highestNo = documents.documents[0].getLong("no") ?: 0
                            newNo = highestNo.toInt() + 1
                        }

                        val bpjsData = mapOf(
                            "no" to newNo,
                            "nama" to nama,
                            "jmlPeserta" to jmlPeserta,
                            "totalBayar" to totalBayar,
                            "tanggal" to tanggal
                        )

                        // Use the new "no" as the document ID
                        firestore.collection("bpjs")
                            .document(newNo.toString())
                            .set(bpjsData)
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
        val nama = intent.getStringExtra("nama")
        val jmlPeserta = intent.getStringExtra("jmlPeserta")
        val totalBayar = intent.getStringExtra("totalBayar")
        val tanggal = intent.getStringExtra("tanggal")
        binding.etNama.setText(nama)
        binding.etJumlahPeserta.setText(jmlPeserta)
        binding.etTotal.setText(totalBayar)
        binding.etTanggal.setText(tanggal)
        binding.btnDelete.setOnClickListener {
            firestore.collection("bpjs")
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
        val nama = intent.getStringExtra("nama")
        val jmlPeserta = intent.getStringExtra("jmlPeserta")
        val totalBayar = intent.getStringExtra("totalBayar")
        val tanggal = intent.getStringExtra("tanggal")
        binding.etNama.setText(nama)
        binding.etJumlahPeserta.setText(jmlPeserta)
        binding.etTotal.setText(totalBayar)
        binding.etTanggal.setText(tanggal)
        binding.btnSave.setOnClickListener {
            val newNama = binding.etNama.text.toString()
            val newJmlPeserta = binding.etJumlahPeserta.text.toString()
            val newTotalBayar = binding.etTotal.text.toString()
            val newTanggal = binding.etTanggal.text.toString()

            if (newNama.isEmpty() || newJmlPeserta.isEmpty() || newTotalBayar.isEmpty() || newTanggal.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            } else {
                val bpjsData = mapOf(
                    "no" to no,
                    "nama" to newNama,
                    "jmlPeserta" to newJmlPeserta,
                    "totalBayar" to newTotalBayar,
                    "tanggal" to newTanggal
                )

                firestore.collection("bpjs")
                    .document(no.toString())
                    .set(bpjsData)
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
package com.siwiba.wba.activity

import android.app.DatePickerDialog
import android.os.Bundle
import android.util.Log
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.key
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.siwiba.databinding.ActivityManageLogisticsBinding
import com.siwiba.wba.model.Logistics
import java.util.*

class ManageLogisticsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityManageLogisticsBinding
    private lateinit var firestore: FirebaseFirestore
    private var mode: Int = 0


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityManageLogisticsBinding.inflate(layoutInflater)
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

        val etTanggall: EditText = binding.etTanggall
        etTanggall.setOnClickListener {
            showDatePickerDialog(etTanggall)
        }
    }

    private fun setupAddMode() {
        binding.btSave.setOnClickListener {
            val namaBarang = binding.etNama.text.toString()
            val hargaBarang = binding.etHargaBarang.text.toString()
            val keterangan = binding.etKeterangan.text.toString()
            val tanggal = binding.etTanggall.text.toString()

            if (namaBarang.isEmpty() || hargaBarang.isEmpty() || keterangan.isEmpty() || tanggal.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            } else {
                firestore.collection("logistik")
                    .orderBy("no", Query.Direction.DESCENDING)
                    .limit(1)
                    .get()
                    .addOnSuccessListener { documents ->
                        var newNo = 1
                        if (!documents.isEmpty) {
                            val highestNo = documents.documents[0].getLong("no") ?: 0
                            newNo = highestNo.toInt() + 1
                        }

                        val logisticsData = mapOf(
                            "no" to newNo,
                            "namaBarang" to namaBarang,
                            "hargaBarang" to hargaBarang,
                            "keterangan" to keterangan,
                            "tanggal" to tanggal
                        )

                        firestore.collection("logistik")
                            .document(newNo.toString())
                            .set(logisticsData)
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

    private fun setupUpdateMode() {
        // Implement update functionality
        val no = intent.getIntExtra("no", 0)
        val namaBarang = intent.getStringExtra("namaBarang")
        val hargaBarang = intent.getStringExtra("hargaBarang")
        val keterangan = intent.getStringExtra("keterangan")
        val tanggal = intent.getStringExtra("tanggal")

        binding.etNama.setText(namaBarang)
        binding.etHargaBarang.setText(hargaBarang)
        binding.etKeterangan.setText(keterangan)
        binding.etTanggall.setText(tanggal)

        binding.btSave.setOnClickListener {
            val newNamaBarang = binding.etNama.text.toString()
            val newHargaBarang = binding.etHargaBarang.text.toString()
            val newKeterangan = binding.etKeterangan.text.toString()
            val newTanggal = binding.etTanggall.text.toString()

            if (newNamaBarang.isEmpty() || newHargaBarang.isEmpty() || newKeterangan.isEmpty() || newTanggal.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            } else {
                val logisticsData = mapOf(
                    "no" to no,
                    "namaBarang" to newNamaBarang,
                    "hargaBarang" to newHargaBarang,
                    "keterangan" to newKeterangan,
                    "tanggal" to newTanggal
                )

                firestore.collection("logistik")
                    .document(no.toString())
                    .set(logisticsData)
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

    private fun setupDeleteMode() {
        // Implement delete functionality
        val no = intent.getIntExtra("no", 0)
        val namaBarang = intent.getStringExtra("namaBarang")
        val hargaBarang = intent.getStringExtra("hargaBarang")
        val keterangan = intent.getStringExtra("keterangan")
        val tanggal = intent.getStringExtra("tanggal")
        binding.etNama.setText(namaBarang)
        binding.etHargaBarang.setText(hargaBarang)
        binding.etKeterangan.setText(keterangan)
        binding.etTanggall.setText(tanggal)

        binding.btnDelete.setOnClickListener {
            firestore.collection("logistik")
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






    private fun showDatePickerDialog(editText: EditText) {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(this, { _, selectedYear, selectedMonth, selectedDay ->
            val date = "$selectedDay/${selectedMonth + 1}/$selectedYear"
            editText.setText(date)
        }, year, month, day)

        datePickerDialog.show()
    }
}

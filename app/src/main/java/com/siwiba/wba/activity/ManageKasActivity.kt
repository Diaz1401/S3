package com.siwiba.wba.activity

import android.app.DatePickerDialog
import android.os.Bundle
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.siwiba.databinding.ActivityManageGajiBinding
import com.siwiba.databinding.ActivityManageKasBinding
import java.util.*

class ManageKasActivity : AppCompatActivity() {

    private lateinit var binding: ActivityManageKasBinding
    private lateinit var firestore: FirebaseFirestore
    private var mode: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityManageKasBinding.inflate(layoutInflater)
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
            val id_transaksi = binding.etIdTransaksi.text.toString()
            val tanggal = binding.etTanggal.text.toString()
            val nominal = binding.etNominal.text.toString()
            val keterangan = binding.etKeterangan.text.toString()
            val status = binding.etStatus.text.toString()
            val metode_pembayaran = binding.etMetode.text.toString()

            if (id_transaksi.isEmpty() || tanggal.isEmpty() || nominal.isEmpty() || keterangan.isEmpty()|| status.isEmpty()|| metode_pembayaran.isEmpty()) {
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

                        val kasData = mapOf(
                            "no" to newNo,
                            "id_transaksi" to id_transaksi,
                            "tanggal" to tanggal,
                            "nominal" to nominal,
                            "keterangan" to keterangan,
                            "status" to status,
                            "metode_pembayaran" to metode_pembayaran,
                        )

                        // Use the new "no" as the document ID
                        firestore.collection("kas")
                            .document(newNo.toString())
                            .set(kasData)
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
        val id_transaksi = intent.getStringExtra("id_transaksi").orEmpty()
        val tanggal = intent.getStringExtra("tanggal").orEmpty()
        val nominal = intent.getStringExtra("nominal").orEmpty()
        val keterangan = intent.getStringExtra("keterangan").orEmpty()
        val status = intent.getStringExtra("status").orEmpty()
        val metode_pembayaran = intent.getStringExtra("metode_pembayaran").orEmpty()

        binding.etTanggal.setText(tanggal)
        binding.etKeterangan.setText(keterangan)
        binding.etNominal.setText(nominal)
        binding.etMetode.setText(metode_pembayaran)
        binding.etStatus.setText(status)
        binding.btnDelete.setOnClickListener {
            firestore.collection("kas")
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
        val id_transaksi = intent.getStringExtra("id_transaksi").orEmpty()
        val tanggal = intent.getStringExtra("tanggal").orEmpty()
        val nominal = intent.getStringExtra("nominal").orEmpty()
        val keterangan = intent.getStringExtra("keterangan").orEmpty()
        val status = intent.getStringExtra("status").orEmpty()
        val metode_pembayaran = intent.getStringExtra("metode_pembayaran").orEmpty()

        binding.etTanggal.setText(tanggal)
        binding.etKeterangan.setText(keterangan)
        binding.etNominal.setText(nominal)
        binding.etMetode.setText(metode_pembayaran)
        binding.etStatus.setText(status)

        binding.btnSave.setOnClickListener {
            val newTanggal = binding.etTanggal.text.toString()
            val newKeterangan = binding.etKeterangan.text.toString()
            val newNominal = binding.etNominal.text.toString()
            val newMetodePembayaran = binding.etMetode.text.toString()
            val newStatus = binding.etStatus.text.toString()

            if (newTanggal.isEmpty() || newKeterangan.isEmpty() || newNominal.isEmpty() || newMetodePembayaran.isEmpty() || newStatus.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            } else {
                val kasData = mapOf(
                    "no" to no,
                    "id_transaksi" to id_transaksi,
                    "tanggal" to newTanggal,
                    "nominal" to newNominal,
                    "keterangan" to newKeterangan,
                    "status" to newStatus,
                    "metode_pembayaran" to newMetodePembayaran
                )

                firestore.collection("kas")
                    .document(no.toString())
                    .set(kasData)
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
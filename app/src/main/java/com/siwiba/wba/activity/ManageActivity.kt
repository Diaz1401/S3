package com.siwiba.wba.activity

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.siwiba.databinding.ActivityManageBinding
import com.siwiba.wba.model.Saldo
import java.util.Calendar
import java.text.SimpleDateFormat
import java.util.Locale

class ManageActivity : AppCompatActivity() {

    private lateinit var binding: ActivityManageBinding
    private lateinit var firestore: FirebaseFirestore
    private var mode: Int = 0
    private val debit: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityManageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firestore = FirebaseFirestore.getInstance()

        mode = intent.getIntExtra("mode", 0)
        val saldo = intent.getStringExtra("whichSaldo") ?: ""
        if (saldo.isEmpty()) {
            Toast.makeText(this, "Invalid mode or saldo", Toast.LENGTH_SHORT).show()
            finish()
        }
        when (mode) {
            1 -> {
                setupAddMode(saldo)
            }
            2 -> {
                loadData()
                setupUpdateMode(saldo)
            }
            else -> {
                Toast.makeText(this, "Invalid mode", Toast.LENGTH_SHORT).show()
                finish()
            }
        }

        val calendar = Calendar.getInstance()
        val dateSetListener = DatePickerDialog.OnDateSetListener { _, year, month, dayOfMonth ->
            calendar.set(Calendar.YEAR, year)
            calendar.set(Calendar.MONTH, month)
            calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
            val dateFormat = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
            binding.etTanggal.setText(dateFormat.format(calendar.time))
        }

        val datePickerDialog = DatePickerDialog(
            this,
            dateSetListener,
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )

        binding.etTanggal.setOnClickListener {
            datePickerDialog.show()
        }

        binding.ivTanggal.setOnClickListener {
            datePickerDialog.show()
        }
    }

    private fun setupAddMode(whichSaldo: String) {
        binding.btnSave.setOnClickListener {
            firestore.collection("saldo")
                .document("utama")
                .collection("data")
                .orderBy("no", Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .addOnSuccessListener { document ->
                    val kredit = binding.etKredit.text.toString()
                    var lastSaldoUtama = 0
                    if (!document.isEmpty) {
                        lastSaldoUtama = document.documents[0].getLong("saldo")?.toInt() ?: 0
                    }
                    if (kredit.toInt() > lastSaldoUtama) {
                        Toast.makeText(this, "Kredit tidak boleh lebih besar dari saldo utama", Toast.LENGTH_SHORT ).show()
                        return@addOnSuccessListener
                    }
                    firestore.collection("saldo")
                        .document(whichSaldo)
                        .collection("data")
                        .orderBy("no", Query.Direction.DESCENDING)
                        .limit(1)
                        .get()
                        .addOnSuccessListener { documents ->
                            var lastSaldo = 0
                            var newNo = 1
                            if (!documents.isEmpty) {
                                lastSaldo = documents.documents[0].getLong("saldo")?.toInt() ?: 0
                                newNo = documents.documents[0].getLong("no")?.toInt() ?: 1
                                newNo++
                            }
                            val keterangan = binding.etKeterangan.text.toString()
                            val tanggal = binding.etTanggal.text.toString()
                            val editor = intent.getStringExtra("editor") ?: "Editor tidak diketahui"
                            val saldoToSave = lastSaldo - kredit.toInt()

                            if (keterangan.isEmpty() || kredit.isEmpty() || tanggal.isEmpty()) {
                                Toast.makeText(this, "Lengkapi semua kolom", Toast.LENGTH_SHORT).show()
                            } else {
                                val data = mapOf(
                                    "no" to newNo,
                                    "keterangan" to keterangan,
                                    "debit" to debit,
                                    "kredit" to kredit.toInt(),
                                    "saldo" to saldoToSave,
                                    "editor" to editor,
                                    "tanggal" to tanggal
                                )

                                firestore.collection("saldo")
                                    .document(whichSaldo)
                                    .collection("data")
                                    .document(newNo.toString())
                                    .set(data)
                                    .addOnSuccessListener {
                                        Toast.makeText(
                                            this,
                                            "Sukses menambahkan data",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        finish()
                                    }
                                    .addOnFailureListener {
                                        Toast.makeText(
                                            this,
                                            "Gagal menambahkan data",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                            }

                        }
                        .addOnFailureListener {
                            Toast.makeText(
                                this,
                                "Gagal mengambil saldo terakhir",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Gagal mengambil saldo utama terakhir", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun setupUpdateMode(whichSaldo: String) {
        binding.btnSave.setOnClickListener {
            val no = intent.getIntExtra("no", 0)
            val editor = intent.getStringExtra("editor") ?: "Editor tidak diketahui"
            val kredit = intent.getIntExtra("kredit", 0)
            val debit = intent.getIntExtra("debit", 0)
            val saldo = intent.getIntExtra("saldo", 0)
            val tanggal = intent.getStringExtra("tanggal") ?: ""

            val newKeterangan = binding.etKeterangan.text.toString()
            val newEditor = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
                .getString("name", "Editor tidak diketahui") ?: "Editor tidak diketahui"

            val editorToSave = if (editor != newEditor) "$editor, $newEditor" else editor

            if (newKeterangan.isEmpty()) {
                Toast.makeText(this, "Lengkapi kolom keterangan", Toast.LENGTH_SHORT).show()
            } else {
                val data = mapOf(
                    "no" to no,
                    "keterangan" to newKeterangan,
                    "debit" to debit,
                    "kredit" to kredit,
                    "saldo" to saldo,
                    "editor" to editorToSave,
                    "tanggal" to tanggal
                )

                firestore.collection("saldo")
                    .document(whichSaldo)
                    .collection("data")
                    .document(no.toString())
                    .set(data)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Sukses memperbarui data", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Gagal memperbarui data", Toast.LENGTH_SHORT).show()
                    }
            }
        }
    }

    private fun loadData() {
        val keterangan = intent.getStringExtra("keterangan")
        val kredit = intent.getIntExtra("kredit", 0)
        val tanggal = intent.getStringExtra("tanggal")
        binding.etKeterangan.setText(keterangan)
        // disable kredit and tanggal
        binding.etKredit.setText(kredit.toString())
        binding.etKredit.isEnabled = false
        binding.etTanggal.setText(tanggal)
        binding.etTanggal.isEnabled = false
        binding.ivTanggal.isEnabled = false
    }
}
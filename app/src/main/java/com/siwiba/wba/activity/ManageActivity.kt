package com.siwiba.wba.activity

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.siwiba.databinding.ActivityManageBinding
import com.siwiba.wba.model.Saldo
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class ManageActivity : AppCompatActivity() {

    private lateinit var binding: ActivityManageBinding
    private lateinit var firestore: FirebaseFirestore
    private var mode: Int = 0
    private var isAdmin: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityManageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firestore = FirebaseFirestore.getInstance()
        isAdmin = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
            .getBoolean("isAdmin", false)

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

        if (isAdmin) {
            binding.etDebit.visibility = View.VISIBLE
        } else {
            binding.etDebit.visibility = View.GONE
        }

        // Tombol Delete hanya muncul di mode Update
        if (mode == 2) {
            binding.btnDelete.visibility = View.VISIBLE
            binding.btnDelete.setOnClickListener {
                showDeleteConfirmationDialog(saldo)
            }
        } else {
            binding.btnDelete.visibility = View.GONE
        }
    }

    private fun setupAddMode(whichSaldo: String) {
        binding.btnSave.setOnClickListener {
            if (checkInput()) {
                var debit = 0
                if (isAdmin) {
                    debit = binding.etDebit.text.toString().toInt()
                }
                val kredit = binding.etKredit.text.toString().toInt()
                firestore.collection("saldo")
                    .document("utama")
                    .collection("data")
                    .orderBy("no", Query.Direction.DESCENDING)
                    .limit(1)
                    .get()
                    .addOnSuccessListener { document ->
                        var lastSaldoUtama = 0
                        if (!document.isEmpty) {
                            lastSaldoUtama = document.documents[0].getLong("saldo")?.toInt() ?: 0
                        }
                        if (debit > lastSaldoUtama) {
                            Toast.makeText(
                                this,
                                "Penambahan debit tidak boleh lebih besar dari saldo utama",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
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
                                    if (kredit > lastSaldo) {
                                        Toast.makeText(
                                            this,
                                            "Kredit tidak boleh lebih besar dari saldo $whichSaldo",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    } else {
                                        val keterangan = binding.etKeterangan.text.toString()
                                        val tanggal = binding.etTanggal.text.toString()
                                        val editor = intent.getStringExtra("editor") ?: "Editor tidak diketahui"
                                        val saldoToSave = lastSaldo + debit - kredit

                                        val data = mapOf(
                                            "no" to newNo,
                                            "keterangan" to keterangan,
                                            "debit" to debit,
                                            "kredit" to kredit,
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
                                                    "Sukses menambahkan data pada saldo $whichSaldo",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                                finish()
                                            }
                                            .addOnFailureListener {
                                                Toast.makeText(
                                                    this,
                                                    "Gagal menambahkan data pada saldo $whichSaldo",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                    }
                                }
                        }
                    }
            }
        }
    }

    private fun setupUpdateMode(whichSaldo: String) {
        binding.btnSave.setOnClickListener {
            // Logika update
        }
    }

    private fun showDeleteConfirmationDialog(whichSaldo: String) {
        val no = intent.getIntExtra("no", 0)
        AlertDialog.Builder(this)
            .setTitle("Konfirmasi Hapus")
            .setMessage("Apakah Anda yakin ingin menghapus data ini?")
            .setPositiveButton("Ya") { _, _ ->
                deleteData(whichSaldo, no)
            }
            .setNegativeButton("Tidak", null)
            .show()
    }

    private fun deleteData(whichSaldo: String, no: Int) {
        firestore.collection("saldo")
            .document(whichSaldo)
            .collection("data")
            .document(no.toString())
            .delete()
            .addOnSuccessListener {
                Toast.makeText(this, "Data berhasil dihapus", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Gagal menghapus data", Toast.LENGTH_SHORT).show()
            }
    }

    private fun checkInput(): Boolean {
        if (binding.etKredit.text.isEmpty()) {
            Toast.makeText(this, "Lengkapi kolom kredit", Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }

    private fun loadData() {
        val keterangan = intent.getStringExtra("keterangan")
        val kredit = intent.getIntExtra("kredit", 0)
        binding.etKeterangan.setText(keterangan)
        binding.etKredit.setText(kredit.toString())
        binding.etKredit.isEnabled = false
    }
}

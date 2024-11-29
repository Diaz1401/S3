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
                setupDeleteMode(saldo)
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
            val keterangan = binding.etKeterangan.text.toString()
            val kredit = binding.etKredit.text.toString()
            val tanggal = binding.etTanggal.text.toString()
            val editor = intent.getStringExtra("editor") ?: ""

            if (keterangan.isEmpty() || kredit.isEmpty() || tanggal.isEmpty()) {
                Toast.makeText(this, "Lengkapi semua kolom", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            firestore.collection("saldo")
                .document("utama")
                .get()
                .addOnSuccessListener { document ->
                    val saldoUtama = document.getLong("saldo") ?: 0
                    if (kredit.toInt() > saldoUtama) {
                        Toast.makeText(this, "Kredit tidak boleh lebih besar dari saldo utama", Toast.LENGTH_SHORT).show()
                        return@addOnSuccessListener
                    }
                    firestore.collection("saldo")
                        .document(whichSaldo)
                        .get()
                        .addOnSuccessListener { document ->
                            var saldo = document.getLong("saldo") ?: 0

                            saldo -= kredit.toInt()
//                          saldo += debit.toInt()

                            firestore.collection("saldo")
                                .document(whichSaldo)
                                .collection("data")
                                .orderBy("no", Query.Direction.DESCENDING)
                                .limit(1)
                                .get()
                                .addOnSuccessListener { documents ->
                                    var newNo = 1
                                    if (!documents.isEmpty) {
                                        val highestNo = documents.documents[0].getLong("no") ?: 0
                                        newNo = highestNo.toInt() + 1
                                    }

                                    val data = mapOf(
                                        "no" to newNo,
                                        "keterangan" to keterangan,
                                        "debit" to debit,
                                        "kredit" to kredit.toInt(),
                                        "editor" to editor,
                                        "tanggal" to tanggal
                                    )

                                    firestore.collection("saldo")
                                        .document(whichSaldo)
                                        .collection("data")
                                        .document(newNo.toString())
                                        .set(data)
                                        .addOnSuccessListener {
                                            firestore.collection("saldo")
                                                .document(whichSaldo)
                                                .update("saldo", saldo)
                                                .addOnSuccessListener {
//                                            if (whichSaldo != "utama") {
//                                                updateUtamaSaldo(debit, -kredit.toInt())
//                                            } else {
                                                    Toast.makeText(this, "Sukses menambahkan data", Toast.LENGTH_SHORT).show()
                                                    finish()
//                                            }
                                                }
                                                .addOnFailureListener {
                                                    Toast.makeText(this, "Gagal memperbarui saldo", Toast.LENGTH_SHORT).show()
                                                }
                                        }
                                        .addOnFailureListener {
                                            Toast.makeText(this, "Gagal menyimpan data", Toast.LENGTH_SHORT).show()
                                        }
                                }
                                .addOnFailureListener {
                                    Toast.makeText(this, "Gagal mengambil data", Toast.LENGTH_SHORT).show()
                                }
                        }
                        .addOnFailureListener {
                            Toast.makeText(this, "Gagal mengambil data", Toast.LENGTH_SHORT).show()
                        }
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Gagal mengambil saldo utama", Toast.LENGTH_SHORT).show()
                }


        }
    }

    private fun loadData() {
        val keterangan = intent.getStringExtra("keterangan")
        val kredit = intent.getIntExtra("kredit", 0)
        val tanggal = intent.getStringExtra("tanggal")
        binding.etKeterangan.setText(keterangan)
        binding.etKredit.setText(kredit.toString())
        binding.etTanggal.setText(tanggal)
    }

    private fun setupDeleteMode(whichSaldo: String) {
        binding.btnDelete.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Hapus data")
                .setMessage("Apakah anda yakin ingin menghapus data ini?")
                .setPositiveButton("Ya") { dialog, _ ->
                    val no = intent.getIntExtra("no", 0)
//                    val debit = intent.getIntExtra("debit", 0)
                    val kredit = intent.getIntExtra("kredit", 0)

                    firestore.collection("saldo")
                        .document(whichSaldo)
                        .collection("data")
                        .document(no.toString())
                        .delete()
                        .addOnSuccessListener {
                            firestore.collection("saldo")
                                .document(whichSaldo)
                                .get()
                                .addOnSuccessListener { document ->
                                    var saldo = document.getLong("saldo") ?: 0
//                                    saldo -= debit
                                    saldo += kredit

                                    firestore.collection("saldo")
                                        .document(whichSaldo)
                                        .update("saldo", saldo)
                                        .addOnSuccessListener {
//                                            if (whichSaldo != "utama") {
//                                                updateUtamaSaldo(-debit, kredit)
//                                            } else {
                                                Toast.makeText(this, "Sukses menghapus data", Toast.LENGTH_SHORT).show()
                                                finish()
//                                            }
                                        }
                                        .addOnFailureListener {
                                            Toast.makeText(this, "Gagal memperbarui saldo", Toast.LENGTH_SHORT).show()
                                        }
                                }
                                .addOnFailureListener {
                                    Toast.makeText(this, "Gagal mengambil saldo", Toast.LENGTH_SHORT).show()
                                }
                        }
                        .addOnFailureListener {
                            Toast.makeText(this, "Gagal menghapus data", Toast.LENGTH_SHORT).show()
                        }
                    dialog.dismiss()
                }
                .setNegativeButton("Tidak") { dialog, _ ->
                    dialog.dismiss()
                }
                .create()
                .show()
        }
    }

    private fun setupUpdateMode(whichSaldo: String) {
        binding.btnSave.setOnClickListener {
            val no = intent.getIntExtra("no", 0)
            val kredit = intent.getIntExtra("kredit", 0)
            val editor = intent.getStringExtra("editor") ?: ""

            val newKeterangan = binding.etKeterangan.text.toString()
            val newKredit = binding.etKredit.text.toString().toInt()
//            val newDebit = binding.etDebit.text.toString().toInt()
            val newEditor = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
                .getString("name", "Editor tidak diketahui") ?: "Editor tidak diketahui"
            val newTanggal = binding.etTanggal.text.toString()

            val editorToSave = if (editor != newEditor) "$editor, $newEditor" else editor

            if (newKeterangan.isEmpty() ||  newKredit.toString().isEmpty() || newTanggal.isEmpty()) {
                Toast.makeText(this, "Lengkapi semua kolom", Toast.LENGTH_SHORT).show()
            } else {
                val data = mapOf(
                    "no" to no,
                    "keterangan" to newKeterangan,
//                    "debit" to newDebit,
                    "debit" to debit,
                    "kredit" to newKredit,
                    "editor" to editorToSave,
                    "tanggal" to newTanggal
                )

                firestore.collection("saldo")
                    .document(whichSaldo)
                    .collection("data")
                    .document(no.toString())
                    .set(data)
                    .addOnSuccessListener {
                        firestore.collection("saldo")
                            .document(whichSaldo)
                            .get()
                            .addOnSuccessListener { document ->
                                var saldo = document.getLong("saldo") ?: 0
//                                saldo -= debit
//                                saldo += newDebit
                                saldo += kredit
                                saldo -= newKredit

                                firestore.collection("saldo")
                                    .document(whichSaldo)
                                    .update("saldo", saldo)
                                    .addOnSuccessListener {
//                                        if (whichSaldo != "utama") {
////                                            updateUtamaSaldo(newDebit - debit, -(newKredit - kredit))
//                                            updateUtamaSaldo(debit, -(newKredit - kredit))
//                                        } else {
                                            Toast.makeText(this, "Sukses memperbarui data", Toast.LENGTH_SHORT).show()
                                            finish()
//                                        }
                                    }
                                    .addOnFailureListener {
                                        Toast.makeText(this, "Gagal memperbarui saldo", Toast.LENGTH_SHORT).show()
                                    }
                            }
                            .addOnFailureListener {
                                Toast.makeText(this, "Gagal mengambil saldo", Toast.LENGTH_SHORT).show()
                            }
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Gagal memperbarui data", Toast.LENGTH_SHORT).show()
                    }
            }
        }
    }

//    private fun updateUtamaSaldo(debitChange: Int, kreditChange: Int) {
//        firestore.collection("saldo")
//            .document("utama")
//            .get()
//            .addOnSuccessListener { document ->
//                var saldoUtama = document.getLong("saldo") ?: 0
//                saldoUtama += debitChange
//                saldoUtama += kreditChange
//
//                firestore.collection("saldo")
//                    .document("utama")
//                    .update("saldo", saldoUtama)
//                    .addOnSuccessListener {
//                        Toast.makeText(this, "Sukses memperbarui saldo utama", Toast.LENGTH_SHORT).show()
//                        finish()
//                    }
//                    .addOnFailureListener {
//                        Toast.makeText(this, "Gagal memperbarui saldo utama", Toast.LENGTH_SHORT).show()
//                    }
//            }
//            .addOnFailureListener {
//                Toast.makeText(this, "Gagal mengambil data", Toast.LENGTH_SHORT).show()
//            }
//    }
}
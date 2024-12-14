package com.siwiba.wba.activity

import android.app.DatePickerDialog
import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.siwiba.R
import com.siwiba.databinding.ActivityManageAdminBinding
import com.siwiba.util.AppMode
import java.util.Calendar
import java.text.SimpleDateFormat
import java.util.Locale

class ManageAdminActivity : AppCompatActivity() {

    private lateinit var binding: ActivityManageAdminBinding
    private lateinit var firestore: FirebaseFirestore
    private var mode: Int = 0
    private val whichSaldo: String = "utama"

    override fun onCreate(savedInstanceState: Bundle?) {
        val appMode = AppMode(this)
        if (appMode.getAppMode()) {
            setTheme(R.style.Base_Theme_WBA)
        } else {
            setTheme(R.style.Base_Theme_KWI)
        }
        super.onCreate(savedInstanceState)
        binding = ActivityManageAdminBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firestore = FirebaseFirestore.getInstance()

        mode = intent.getIntExtra("mode", 0)
        when (mode) {
            1 -> {
                setupAddMode()
            }
            2 -> {
                loadData()
                setupUpdateMode()
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

    private fun setupAddMode() {
        binding.btnSave.setOnClickListener {
            firestore.collection("saldo")
                .document(whichSaldo)
                .collection("data")
                .orderBy("no", Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .addOnSuccessListener { document ->
                    val kredit = binding.etKredit.text.toString()
                    var lastSaldo = 0
                    var no = 1
                    if (!document.isEmpty) {
                        lastSaldo = document.documents[0].getLong("saldo")?.toInt() ?: 0
                        no = document.documents[0].getLong("no")?.toInt() ?: 1
                        no++
                    }
                    if (kredit.toInt() > lastSaldo) {
                        Toast.makeText(this, "Kredit tidak boleh lebih besar dari saldo utama", Toast.LENGTH_SHORT ).show()
                    } else {
                        val debit = binding.etDebit.text.toString()
                        val keterangan = binding.etKeterangan.text.toString()
                        val tanggal = binding.etTanggal.text.toString()
                        val editor = intent.getStringExtra("editor") ?: "Editor tidak diketahui"
                        val saldoToSave = lastSaldo + debit.toInt() - kredit.toInt()

                        if (keterangan.isEmpty() || kredit.isEmpty() || debit.isEmpty() || tanggal.isEmpty()) {
                            Toast.makeText(this, "Lengkapi semua kolom", Toast.LENGTH_SHORT).show()
                        } else {
                            val data = mapOf(
                                "no" to no,
                                "keterangan" to keterangan,
                                "debit" to debit.toInt(),
                                "kredit" to kredit.toInt(),
                                "saldo" to saldoToSave,
                                "editor" to editor,
                                "tanggal" to tanggal
                            )

                            firestore.collection("saldo")
                                .document(whichSaldo)
                                .collection("data")
                                .document(no.toString())
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
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Gagal mengambil saldo utama terakhir", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun setupUpdateMode() {
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
        val debit = intent.getIntExtra("debit", 0)
        val tanggal = intent.getStringExtra("tanggal")
        binding.etKeterangan.setText(keterangan)
        // disable kredit, debit and tanggal
        binding.etKredit.setText(kredit.toString())
        binding.etKredit.isEnabled = false
        binding.etDebit.setText(debit.toString())
        binding.etDebit.isEnabled = false
        binding.etTanggal.setText(tanggal)
        binding.etTanggal.isEnabled = false
        binding.ivTanggal.isEnabled = false
    }
}
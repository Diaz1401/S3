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
import java.util.Calendar
import java.text.SimpleDateFormat
import java.util.Locale
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.widget.EditText
import com.siwiba.util.Format

class ManageActivity : AppCompatActivity() {

    private lateinit var binding: ActivityManageBinding
    private lateinit var firestore: FirebaseFirestore
    private var mode: Int = 0
    private val debit: Int = 0
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
        binding.etKredit.addTextChangedListener(Format().createTextWatcher(binding.etKredit))
        binding.etDebit.addTextChangedListener(Format().createTextWatcher(binding.etDebit))
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
                            Toast.makeText(this, "Penambahan debit tidak boleh lebih besar dari saldo utama", Toast.LENGTH_SHORT).show()
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
                                        Toast.makeText(this, "Kredit tidak boleh lebih besar dari saldo $whichSaldo", Toast.LENGTH_SHORT).show()
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
                                                if (debit > 0) {
                                                    // reduce latest saldo utama based on debit addition
                                                    firestore.collection("saldo")
                                                        .document("utama")
                                                        .collection("data")
                                                        .orderBy("no", Query.Direction.DESCENDING)
                                                        .limit(1)
                                                        .get()
                                                        .addOnSuccessListener { document ->
                                                            lastSaldoUtama = 0
                                                            newNo = 1
                                                            val kreditSaldoUtama = debit
                                                            if (!document.isEmpty) {
                                                                lastSaldoUtama = document.documents[0].getLong("saldo")?.toInt() ?: 0
                                                                newNo = document.documents[0].getLong("no")?.toInt() ?: 1
                                                                newNo++
                                                            }
                                                            val newSaldoUtama = lastSaldoUtama - kreditSaldoUtama
                                                            val dataUtama = mapOf(
                                                                "no" to newNo,
                                                                "keterangan" to "Kredit saldo utama ke saldo $whichSaldo",
                                                                "debit" to 0,
                                                                "kredit" to kreditSaldoUtama,
                                                                "saldo" to newSaldoUtama,
                                                                "editor" to editor,
                                                                "tanggal" to tanggal
                                                            )
                                                            firestore.collection("saldo")
                                                                .document("utama")
                                                                .collection("data")
                                                                .document(newNo.toString())
                                                                .set(dataUtama)
                                                                .addOnSuccessListener {
                                                                    Toast.makeText(
                                                                        this,
                                                                        "Sukses mengurangi saldo utama",
                                                                        Toast.LENGTH_SHORT
                                                                    ).show()
                                                                    finish()
                                                                }
                                                                .addOnFailureListener {
                                                                    Toast.makeText(
                                                                        this,
                                                                        "Gagal mengurangi saldo utama",
                                                                        Toast.LENGTH_SHORT
                                                                    ).show()
                                                                }
                                                        }
                                                        .addOnFailureListener {
                                                            Toast.makeText(
                                                                this,
                                                                "Gagal mengambil saldo utama terakhir",
                                                                Toast.LENGTH_SHORT
                                                            ).show()
                                                        }
                                                } else {
                                                    finish()
                                                }
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
                                .addOnFailureListener {
                                    Toast.makeText(
                                        this,
                                        "Gagal mengambil saldo $whichSaldo terakhir",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                        }
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Gagal mengambil saldo utama terakhir", Toast.LENGTH_SHORT).show()
                    }
            }
        }
    }

    private fun setupUpdateMode(whichSaldo: String) {
        binding.btnSave.setOnClickListener {
            if (checkInput()) {
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
    }

    private fun checkInput(): Boolean {
        if (isAdmin) {
            if (binding.etDebit.text.isEmpty()) {
                Toast.makeText(this, "Lengkapi kolom debit", Toast.LENGTH_SHORT).show()
                return false
            }
        }
        if (binding.etKredit.text.isEmpty()) {
            Toast.makeText(this, "Lengkapi kolom kredit", Toast.LENGTH_SHORT).show()
            return false
        }
        if (binding.etKeterangan.text.isEmpty()) {
            Toast.makeText(this, "Lengkapi kolom keterangan", Toast.LENGTH_SHORT).show()
            return false
        }
        if (binding.etTanggal.text.isEmpty()) {
            Toast.makeText(this, "Lengkapi kolom tanggal", Toast.LENGTH_SHORT).show()
            return false
        }
        return true
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
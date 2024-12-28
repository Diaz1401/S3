package com.siwiba.wba.activity

import android.app.DatePickerDialog
import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import android.app.AlertDialog
import android.content.SharedPreferences
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.siwiba.R
import com.siwiba.databinding.ActivityManageSaldoBinding
import java.util.Calendar
import java.text.SimpleDateFormat
import java.util.Locale
import com.siwiba.util.NumberFormat
import com.siwiba.util.AppMode
import com.siwiba.util.EncSharedPref

class ManageSaldoActivity : AppCompatActivity() {

    private lateinit var binding: ActivityManageSaldoBinding
    private lateinit var firestore: FirebaseFirestore
    private lateinit var sharedPref: SharedPreferences
    private var debit: Int = 0
    private var firestoreSaldo: String = ""
    private var isAdmin: Boolean = false
    private var mode: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        val appMode = AppMode(this)
        if (appMode.getAppMode()) {
            setTheme(R.style.Base_Theme_WBA)
            firestoreSaldo = "saldo_wba"
        } else {
            setTheme(R.style.Base_Theme_KWI)
            firestoreSaldo = "saldo_kwi"
        }
        super.onCreate(savedInstanceState)
        binding = ActivityManageSaldoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firestore = FirebaseFirestore.getInstance()
        sharedPref = EncSharedPref(this).getEncSharedPref()
        isAdmin = sharedPref.getBoolean("isAdmin", false)
        mode = intent.getIntExtra("mode", 0)

        val saldo = intent.getStringExtra("whichSaldo") ?: ""
        val utama = saldo == "utama"

        when (mode) {
            1 -> {
                setupAddMode(saldo, utama)
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
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            binding.etTanggal.setText(dateFormat.format(calendar.time))
        }

        val datePickerDialog = DatePickerDialog(
            this,
            dateSetListener,
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )

        binding.ivTanggal.setOnClickListener {
            datePickerDialog.show()
        }

        if (isAdmin) {
            binding.etDebit.visibility = View.VISIBLE
            binding.etDebit.addTextChangedListener(NumberFormat().createTextWatcher(binding.etDebit))
        } else {
            binding.etDebit.visibility = View.GONE
        }

        binding.etKredit.addTextChangedListener(NumberFormat().createTextWatcher(binding.etKredit))
    }

    private fun setupAddMode(whichSaldo: String, utama: Boolean) {
        binding.btnSave.setOnClickListener {
            if (!checkInput()) return@setOnClickListener
            var debit = 0L

            if (isAdmin) {
                val parsedDebit = NumberFormat().parseNumber(binding.etDebit.text.toString())
                debit = parsedDebit?.toLong() ?: 0
            }

            val parsedKredit = NumberFormat().parseNumber(binding.etKredit.text.toString())
            val kredit = parsedKredit?.toLong() ?: 0

            /*
             * Fetch the latest saldo utama
             */
            firestore.collection(firestoreSaldo)
                .document("utama")
                .collection("data")
                .orderBy("no", Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .addOnSuccessListener { document ->
                    var lastSaldoUtama = 0L
                    var lastDate = ""

                    if (!document.isEmpty) {
                        lastSaldoUtama = document.documents[0].getLong("saldo") ?: 0
                        lastDate = document.documents[0].getString("tanggal") ?: ""
                    }

                    if ((debit > lastSaldoUtama) && !utama) {
                        Toast.makeText(this, "Penambahan debit tidak boleh lebih besar dari saldo utama", Toast.LENGTH_SHORT).show()
                        return@addOnSuccessListener
                    }

                    val currentDate = binding.etTanggal.text.toString()
                    if (currentDate < lastDate && lastDate.isNotEmpty()) {
                        Toast.makeText(this, "Tanggal tidak boleh lebih kecil dari tanggal terakhir", Toast.LENGTH_SHORT).show()
                        return@addOnSuccessListener
                    }

                    val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Calendar.getInstance().time)
                    if (currentDate > today) {
                        AlertDialog.Builder(this)
                            .setTitle("Peringatan")
                            .setMessage("Tanggal yang dimasukan lebih baru dari hari ini. Apakah Anda yakin ingin melanjutkan?")
                            .setPositiveButton("Ya") { _, _ ->
                                addToFirestore(whichSaldo, kredit, debit, utama)
                            }
                            .setNegativeButton("Tidak") { dialog, _ ->
                                dialog.dismiss()
                            }
                            .show()
                    } else if (currentDate < today) {
                        AlertDialog.Builder(this)
                            .setTitle("Peringatan")
                            .setMessage("Tanggal yang dimasukan lebih tua dari hari ini. Apakah Anda yakin ingin melanjutkan?")
                            .setPositiveButton("Ya") { _, _ ->
                                addToFirestore(whichSaldo, kredit, debit, utama)
                            }
                            .setNegativeButton("Tidak") { dialog, _ ->
                                dialog.dismiss()
                            }
                            .show()
                    } else {
                        addToFirestore(whichSaldo, kredit, debit, utama)
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Gagal mengambil saldo utama terakhir", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun addToFirestore(whichSaldo: String, kredit: Long, debit: Long, utama: Boolean) {
        /*
         * Fetch the latest selected saldo
         */
        firestore.collection(firestoreSaldo)
            .document(whichSaldo)
            .collection("data")
            .orderBy("no", Query.Direction.DESCENDING)
            .limit(1)
            .get()
            .addOnSuccessListener { documents ->
                var lastSaldo = 0L
                var newNo = 1L
                if (!documents.isEmpty) {
                    lastSaldo = documents.documents[0].getLong("saldo") ?: 0
                    newNo = documents.documents[0].getLong("no") ?: 1
                    newNo++
                }
                if (kredit > lastSaldo) {
                    Toast.makeText(this, "Kredit tidak boleh lebih besar dari saldo $whichSaldo", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }
                val keterangan = binding.etKeterangan.text.toString()
                val tanggal = binding.etTanggal.text.toString()
                val editor = intent.getStringExtra("editor") ?: "Editor tidak diketahui"
                lastSaldo += debit - kredit

                val data = mapOf(
                    "no" to newNo,
                    "keterangan" to keterangan,
                    "debit" to debit,
                    "kredit" to kredit,
                    "saldo" to lastSaldo,
                    "editor" to editor,
                    "tanggal" to tanggal
                )
                /*
                 * Save the data to the selected saldo
                 */
                firestore.collection(firestoreSaldo)
                    .document(whichSaldo)
                    .collection("data")
                    .document(newNo.toString())
                    .set(data)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Sukses menambahkan data pada saldo $whichSaldo", Toast.LENGTH_SHORT).show()
                        if ((debit > 0) && !utama) {
                            /*
                             * Reduce latest saldo utama based on debit addition
                             */
                            firestore.collection(firestoreSaldo)
                                .document("utama")
                                .collection("data")
                                .orderBy("no", Query.Direction.DESCENDING)
                                .limit(1)
                                .get()
                                .addOnSuccessListener { document ->
                                    var lastSaldoUtama = 0L
                                    newNo = 1
                                    val kreditSaldoUtama = debit
                                    if (!document.isEmpty) {
                                        lastSaldoUtama = document.documents[0].getLong("saldo") ?: 0
                                        newNo = document.documents[0].getLong("no") ?: 1
                                        newNo++
                                    }
                                    lastSaldoUtama -= kreditSaldoUtama
                                    val dataUtama = mapOf(
                                        "no" to newNo,
                                        "keterangan" to "Kredit saldo utama ke saldo $whichSaldo",
                                        "debit" to 0,
                                        "kredit" to kreditSaldoUtama,
                                        "saldo" to lastSaldoUtama,
                                        "editor" to editor,
                                        "tanggal" to tanggal
                                    )
                                    /*
                                     * Save the data to saldo utama
                                     */
                                    firestore.collection(firestoreSaldo)
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
            .addOnFailureListener {
                Toast.makeText(
                    this,
                    "Gagal mengambil saldo $whichSaldo terakhir",
                    Toast.LENGTH_SHORT
                ).show()
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
                val newEditor = sharedPref.getString("name", "Editor tidak diketahui") ?: "Editor tidak diketahui"

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
                    firestore.collection(firestoreSaldo)
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
            if (binding.etDebit.text.isNullOrEmpty()) {
                Toast.makeText(this, "Lengkapi kolom debit", Toast.LENGTH_SHORT).show()
                return false
            }
        }
        if (binding.etKredit.text.isNullOrEmpty()) {
            Toast.makeText(this, "Lengkapi kolom kredit", Toast.LENGTH_SHORT).show()
            return false
        }
        if (binding.etKeterangan.text.isNullOrEmpty()) {
            Toast.makeText(this, "Lengkapi kolom keterangan", Toast.LENGTH_SHORT).show()
            return false
        }
        if (binding.etTanggal.text.isNullOrEmpty()) {
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
        binding.etDebit.setText(debit.toString())
        binding.etDebit.isEnabled = false
        binding.etKredit.setText(kredit.toString())
        binding.etKredit.isEnabled = false
        binding.etTanggal.setText(tanggal)
        binding.etTanggal.isEnabled = false
        binding.ivTanggal.isEnabled = false
    }
}
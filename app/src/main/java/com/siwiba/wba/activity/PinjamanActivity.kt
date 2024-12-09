package com.siwiba.wba.activity

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.dewakoding.androiddatatable.data.Column
import com.dewakoding.androiddatatable.listener.OnWebViewComponentClickListener
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.google.gson.Gson
import com.opencsv.CSVReader
import com.opencsv.CSVWriter
import com.siwiba.R
import com.siwiba.databinding.ActivityPinjamanBinding
import com.siwiba.wba.model.Saldo
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.*

class PinjamanActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPinjamanBinding
    private lateinit var firestore: FirebaseFirestore
    private var selectedPeriod: String = "Total"
    private val whichSaldo = "pinjaman"
    private lateinit var sharedPreferences: SharedPreferences
    private var editor: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPinjamanBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize Firestore
        firestore = FirebaseFirestore.getInstance()

        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)

        // Set editor
        editor = sharedPreferences.getString("name", "Editor tidak diketahui") ?: "Editor tidak diketahui"

        binding.tambah.setOnClickListener {
            val intent = Intent(this, ManageActivity::class.java)
            intent.putExtra("mode", 1)
            intent.putExtra("whichSaldo", whichSaldo)
            intent.putExtra("editor", editor)
            startActivity(intent)
        }

        // Set up Spinner
        val periods = arrayOf("Total", "Seminggu", "Sebulan", "Setahun")
        val adapter = ArrayAdapter(this, R.layout.item_spinner_periode, periods)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerPeriode.adapter = adapter
        binding.txtPeriode.text = "Untuk $selectedPeriod Terakhir"

        binding.spinnerPeriode.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                selectedPeriod = periods[position]
                binding.txtPeriode.text = "Untuk $selectedPeriod terakhir"
                fetchSaldoData() // Refresh data based on selected period
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        binding.dokumen.setOnClickListener {
            val isAdmin = sharedPreferences.getBoolean("isAdmin", false)
            AlertDialog.Builder(this)
                .setTitle("Pilih Aksi")
                .setItems(arrayOf("Export Data", "Import Data")) { _, which ->
                    when (which) {
                        0 -> {
                            // Export data
                            val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                                addCategory(Intent.CATEGORY_OPENABLE)
                                type = "text/comma-separated-values"
                                putExtra(Intent.EXTRA_TITLE, "${whichSaldo}.csv")
                            }
                            startActivityForResult(intent, REQUEST_CODE_EXPORT)
                        }
                        1 -> {
                            // Only admin can import data
                            if (isAdmin) {
                                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                                    addCategory(Intent.CATEGORY_OPENABLE)
                                    type = "text/comma-separated-values"
                                }
                                startActivityForResult(intent, REQUEST_CODE_IMPORT)
                            } else {
                                Toast.makeText(
                                    this,
                                    "Anda tidak memiliki akses untuk mengimport data",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                }
                .show()
        }
        binding.btnBack.setOnClickListener {
            finish()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK && data != null) {
            when (requestCode) {
                REQUEST_CODE_EXPORT -> {
                    data.data?.let { uri ->
                        fetchCsvSaldoData().addOnSuccessListener { saldoList ->
                            exportDataToCSV(saldoList, uri)
                        }
                    }
                }
                REQUEST_CODE_IMPORT -> {
                    data.data?.let { uri ->
                        importDataFromCSV(uri)
                    }
                }
            }
        }
    }

    private fun exportDataToCSV(data: List<Saldo>, uri: Uri) {
        contentResolver.openOutputStream(uri)?.use { outputStream ->
            val writer = CSVWriter(OutputStreamWriter(outputStream))
            writer.writeNext(arrayOf("No", "Keterangan", "Debit", "Kredit", "Saldo", "Editor", "Tanggal"))
            for (saldo in data) {
                writer.writeNext(arrayOf(saldo.no.toString(), saldo.keterangan, saldo.debit.toString(), saldo.kredit.toString(), saldo.saldo.toString(), saldo.editor, saldo.tanggal))
            }
            writer.close()
        }
    }

    private fun importDataFromCSV(uri: Uri) {
        val importedData = mutableListOf<Saldo>()

        contentResolver.openInputStream(uri)?.use { inputStream ->
            val reader = CSVReader(InputStreamReader(inputStream))
            reader.readNext() // Skip header
            var nextLine: Array<String>?
            while (reader.readNext().also { nextLine = it } != null) {
                val saldo = Saldo(
                    no = nextLine!![0].toInt(),
                    keterangan = nextLine!![1],
                    debit = nextLine!![2].toInt(),
                    kredit = nextLine!![3].toInt(),
                    saldo = 0,
                    editor = nextLine!![5],
                    tanggal = nextLine!![6]
                )
                importedData.add(saldo)
            }
            reader.close()
        }

        firestore.collection("saldo")
            .document("utama")
            .collection("data")
            .orderBy("no", Query.Direction.DESCENDING)
            .limit(1)
            .get()
            .addOnSuccessListener { document ->
                var lastSaldoUtama = 0
                var newNoUtama = 1
                if (!document.isEmpty) {
                    lastSaldoUtama = document.documents[0].getLong("saldo")?.toInt() ?: 0
                    newNoUtama = document.documents[0].getLong("no")?.toInt() ?: 1
                    newNoUtama++
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

                        val batch = firestore.batch()
                        val collectionRef = firestore.collection("saldo")
                            .document(whichSaldo)
                            .collection("data")

                        for (saldoItem in importedData) {
                            if (saldoItem.debit > lastSaldoUtama) {
                                Toast.makeText(this, "Penambahan debit tidak boleh lebih besar dari saldo utama", Toast.LENGTH_SHORT).show()
                                return@addOnSuccessListener
                            }
                            if (saldoItem.kredit > lastSaldo) {
                                Toast.makeText(this, "Kredit tidak boleh lebih besar dari saldo $whichSaldo", Toast.LENGTH_SHORT).show()
                                return@addOnSuccessListener
                            }

                            lastSaldo += saldoItem.debit - saldoItem.kredit
                            val datas = mapOf(
                                "no" to newNo,
                                "keterangan" to saldoItem.keterangan,
                                "debit" to saldoItem.debit,
                                "kredit" to saldoItem.kredit,
                                "saldo" to lastSaldo,
                                "editor" to saldoItem.editor,
                                "tanggal" to saldoItem.tanggal
                            )
                            val docRef = collectionRef.document(newNo.toString())
                            batch.set(docRef, datas)
                            newNo++

                            if (saldoItem.debit > 0) {
                                lastSaldoUtama -= saldoItem.debit
                                val dataUtama = mapOf(
                                    "no" to newNoUtama,
                                    "keterangan" to "Kredit saldo utama ke saldo $whichSaldo",
                                    "debit" to 0,
                                    "kredit" to saldoItem.debit,
                                    "saldo" to lastSaldoUtama,
                                    "editor" to saldoItem.editor,
                                    "tanggal" to saldoItem.tanggal
                                )
                                val docRefUtama = firestore.collection("saldo")
                                    .document("utama")
                                    .collection("data")
                                    .document(newNoUtama.toString())
                                batch.set(docRefUtama, dataUtama)
                                newNoUtama++
                            }
                        }

                        batch.commit().addOnSuccessListener {
                            Toast.makeText(this, "Berhasil menyimpan data", Toast.LENGTH_SHORT).show()
                        }.addOnFailureListener {
                            Toast.makeText(this, "Gagal menyimpan data", Toast.LENGTH_SHORT).show()
                        }
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Gagal mengambil saldo $whichSaldo terakhir", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Gagal mengambil saldo utama terakhir", Toast.LENGTH_SHORT).show()
            }
    }

    private fun fetchCsvSaldoData(): Task<List<Saldo>> {
        return firestore.collection("saldo")
            .document(whichSaldo)
            .collection("data")
            .get()
            .continueWith { task ->
                if (task.isSuccessful) {
                    task.result?.toObjects(Saldo::class.java) ?: emptyList()
                } else {
                    emptyList()
                }
            }
    }

    private fun fetchSaldoData() {
        firestore.collection("saldo")
            .document(whichSaldo)
            .collection("data")
            .get()
            .addOnSuccessListener { documents ->
                val saldoList = documents.toObjects(Saldo::class.java)
                populateDataTable(saldoList)
                calculateTotalSaldo()
            }
            .addOnFailureListener { exception ->
                // Handle any errors
            }
    }

    private fun populateDataTable(saldoList: List<Saldo>) {
        val columns = ArrayList<Column>()
        columns.add(Column("no", "No."))
        columns.add(Column("keterangan", "Keterangan"))
        columns.add(Column("debit", "Debit"))
        columns.add(Column("kredit", "Kredit"))
        columns.add(Column("saldo", "Saldo"))
        columns.add(Column("editor", "Editor"))
        columns.add(Column("tanggal", "Tanggal"))

        // Clear existing views
        binding.dataTable.removeAllViews()

        binding.dataTable.setTable(columns, saldoList, isActionButtonShow = true)

        binding.dataTable.setOnClickListener(object : OnWebViewComponentClickListener {
            override fun onRowClicked(dataStr: String) {
                val saldoClicked = Gson().fromJson(dataStr, Saldo::class.java)
                val intent = Intent(applicationContext, ManageActivity::class.java)
                intent.putExtra("mode", 2)
                intent.putExtra("whichSaldo", whichSaldo)
                intent.putExtra("no", saldoClicked.no)
                intent.putExtra("keterangan", saldoClicked.keterangan)
                intent.putExtra("debit", saldoClicked.debit)
                intent.putExtra("kredit", saldoClicked.kredit)
                intent.putExtra("saldo", saldoClicked.saldo)
                intent.putExtra("editor", saldoClicked.editor)
                intent.putExtra("tanggal", saldoClicked.tanggal)
                startActivity(intent)
            }
        })
    }

    private fun calculateTotalSaldo() {
        var totalSaldo = 0
        var totalSaldoDebit = 0
        var totalSaldoKredit = 0

        val tasks =  firestore.collection("saldo")
                .document(whichSaldo)
                .collection("data")
                .orderBy("no", Query.Direction.DESCENDING)
                .get()

        Tasks.whenAllComplete(tasks).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val documents = task.result?.map { it.result as QuerySnapshot }?.flatMap { it.documents } ?: emptyList()
                val saldo = documents.firstOrNull()?.getLong("saldo")?.toInt() ?: 0
                totalSaldo += saldo
                documents.forEach { document ->
                    val debit = document.getLong("debit")?.toInt() ?: 0
                    val kredit = document.getLong("kredit")?.toInt() ?: 0

                    totalSaldoDebit += debit
                    totalSaldoKredit += kredit
                }
                // Set formatted total, debit, kredit with "Rp" in front
                binding.txtTotal.text = "Rp ${Format().formatCurrency(totalSaldo.toString())}"
                binding.txtDebit.text = "Rp ${Format().formatCurrency(totalSaldo.toString())}"
                binding.txtKredit.text = "Rp ${Format().formatCurrency(totalSaldo.toString())}"
            }
        }
    }

    override fun onResume() {
        super.onResume()
        fetchSaldoData()
    }

    companion object {
        private const val REQUEST_CODE_EXPORT = 1
        private const val REQUEST_CODE_IMPORT = 2
    }
}
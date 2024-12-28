package com.siwiba.wba.activity

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
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
import com.siwiba.databinding.ActivitySaldoBinding
import com.siwiba.util.NumberFormat
import com.siwiba.util.AppMode
import com.siwiba.util.CsvExportImport
import com.siwiba.util.EncSharedPref
import com.siwiba.wba.model.Saldo
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.util.Locale

class SaldoActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySaldoBinding
    private lateinit var firestore: FirebaseFirestore
    private lateinit var whichSaldo: String
    private lateinit var csvManager: CsvExportImport
    private lateinit var sharedPref: SharedPreferences
    private var selectedPeriod: String = "Total"
    private var editor: String = ""
    private var firestoreSaldo: String = ""

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
        binding = ActivitySaldoBinding.inflate(layoutInflater)
        setContentView(binding.root)
        whichSaldo = intent.getStringExtra("whichSaldo") ?: "utama"

        // Initialize Firestore
        firestore = FirebaseFirestore.getInstance()

        // Set editor
        editor = sharedPref.getString("name", "Editor tidak diketahui") ?: "Editor tidak diketahui"

        sharedPref =  EncSharedPref(this).getEncSharedPref()

        // Initialize CSV manager
        csvManager = CsvExportImport(whichSaldo, firestoreSaldo, this, false)

        binding.tambah.setOnClickListener {
            val intent = Intent(this, ManageSaldoActivity::class.java)
            intent.putExtra("mode", 1)
            intent.putExtra("whichSaldo", whichSaldo)
            intent.putExtra("editor", editor)
            startActivity(intent)
        }

        // Set up Spinner
        val periods = arrayOf("Total", "Seminggu", "Sebulan", "Setahun")
        val adapter = ArrayAdapter(this, R.layout.item_spinner_white, periods)
        adapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item)
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
            val isAdmin = sharedPref.getBoolean("isAdmin", false)
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
        setTitle()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK && data != null) {
            when (requestCode) {
                REQUEST_CODE_EXPORT -> {
                    data.data?.let { uri ->
                        csvManager.fetchCsvSaldoData().addOnSuccessListener { saldoList ->
                            csvManager.exportDataToCSV(saldoList, uri)
                        }
                    }
                }
                REQUEST_CODE_IMPORT -> {
                    data.data?.let { uri ->
                        csvManager.importDataFromCSV(uri)
                    }
                }
            }
        }
    }

    private fun setTitle() {
        val title = if (whichSaldo == "utama") {
            "Saldo Utama"
        } else {
            "Saldo ${whichSaldo.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }}"
        }
        binding.txtTitle.text = title
    }

    private fun fetchSaldoData() {
        firestore.collection(firestoreSaldo)
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
                val intent = Intent(applicationContext, ManageSaldoActivity::class.java)
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
        var totalSaldo = 0L
        var totalSaldoDebit = 0L
        var totalSaldoKredit = 0L

        val tasks =  firestore.collection(firestoreSaldo)
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
                    val debit = document.getLong("debit") ?: 0
                    val kredit = document.getLong("kredit") ?: 0

                    totalSaldoDebit += debit
                    totalSaldoKredit += kredit
                }
                // Set formatted total, debit, kredit with "Rp" in front
                binding.txtTotal.text = "Rp ${NumberFormat().formatCurrency(totalSaldo.toString())}"
                binding.txtDebit.text = "Rp ${NumberFormat().formatCurrency(totalSaldo.toString())}"
                binding.txtKredit.text = "Rp ${NumberFormat().formatCurrency(totalSaldo.toString())}"
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
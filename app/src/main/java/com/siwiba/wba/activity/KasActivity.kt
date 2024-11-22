package com.siwiba.wba.activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import com.dewakoding.androiddatatable.data.Column
import com.dewakoding.androiddatatable.listener.OnWebViewComponentClickListener
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import com.siwiba.R

import com.siwiba.databinding.ActivityKasBinding
import com.siwiba.wba.model.Gaji
import com.siwiba.wba.model.Kas
import java.text.SimpleDateFormat
import java.util.*

class KasActivity : AppCompatActivity() {

    private lateinit var binding: ActivityKasBinding
    private lateinit var firestore: FirebaseFirestore
    private var selectedPeriod: String = "Bulanan"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityKasBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize Firestore
        firestore = FirebaseFirestore.getInstance()

        // Fetch data from Firestore
        fetchKasData()

        binding.tambah.setOnClickListener {
            val intent = Intent(this, ManageKasActivity::class.java)
            intent.putExtra("mode", 1)
            startActivity(intent)
        }

        // Set up Spinner
        val periods = arrayOf("Seminggu", "Sebulan", "Setahun")
        val adapter = ArrayAdapter(this, R.layout.item_spinner, periods)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerPeriode.adapter = adapter
        binding.txtPeriode.text = "Untuk $selectedPeriod Terakhir"

        binding.spinnerPeriode.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                selectedPeriod = periods[position]
                binding.txtPeriode.text = "Untuk $selectedPeriod terakhir"
                fetchKasData() // Refresh data based on selected period
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    private fun fetchKasData() {
        firestore.collection("kas")
            .get()
            .addOnSuccessListener { documents ->
                val kasList = documents.toObjects(Kas::class.java)
                populateDataTable(kasList)
                calculateTotalGaji(kasList)
            }
            .addOnFailureListener { exception ->
                // Handle any errors
            }
    }

    private fun populateDataTable(kasList: List<Kas>) {
        val columns = ArrayList<Column>()
        columns.add(Column("no", "No."))
        columns.add(Column("id_transaksi", "Id Transaksi"))
        columns.add(Column("tanggal", "Tanggal"))
        columns.add(Column("nominal", "Nominal"))
        columns.add(Column("keterangan", "Keterangan"))
        columns.add(Column("status", "Status Transaksi"))
        columns.add(Column("metode_pembayaran", "Metode Pembayaran"))

        // Clear existing views
        binding.dataTable.removeAllViews()

        binding.dataTable.setTable(columns, kasList, isActionButtonShow = true)

        binding.dataTable.setOnClickListener(object : OnWebViewComponentClickListener {
            override fun onRowClicked(dataStr: String) {
                val kasClicked = Gson().fromJson(dataStr, Kas::class.java)
                val intent = Intent(applicationContext, ManageKasActivity::class.java)
                intent.putExtra("mode", 2)
                intent.putExtra("no", kasClicked.no)
                intent.putExtra("id_transaksi", kasClicked.id_transaksi)
                intent.putExtra("tanggal", kasClicked.tanggal)
                intent.putExtra("nominal", kasClicked.nominal)
                intent.putExtra("keterangan", kasClicked.keterangan)
                intent.putExtra("status", kasClicked.status)
                intent.putExtra("metode_pembayaran", kasClicked.metode_pembayaran)
                startActivity(intent)
            }
        })
    }

    private fun calculateTotalGaji(KasList: List<Kas>) {
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val calendar = Calendar.getInstance()
        var totalGaji = 0

        for (kas in KasList) {
            val gajiDate = dateFormat.parse(kas.tanggal)
            if (gajiDate != null) {
                when (selectedPeriod) {
                    "Seminggu" -> {
                        calendar.time = Date()
                        calendar.add(Calendar.DAY_OF_YEAR, -7)
                        if (gajiDate.after(calendar.time)) {
                            totalGaji += kas.nominal.toIntOrNull() ?: 0
                        }
                    }
                    "Sebulan" -> {
                        calendar.time = Date()
                        calendar.add(Calendar.MONTH, -1)
                        if (gajiDate.after(calendar.time)) {
                            totalGaji += kas.nominal.toIntOrNull() ?: 0
                        }
                    }
                    "Setahun" -> {
                        calendar.time = Date()
                        calendar.add(Calendar.YEAR, -1)
                        if (gajiDate.after(calendar.time)) {
                            totalGaji += kas.nominal.toIntOrNull() ?: 0
                        }
                    }
                }
            }
        }
        binding.txtTotal.text = "Rp $totalGaji"
    }
}
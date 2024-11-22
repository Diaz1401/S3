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
import com.siwiba.databinding.ActivityBpjsBinding
import com.siwiba.wba.model.Bpjs
import java.text.SimpleDateFormat
import java.util.*

class BpjsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBpjsBinding
    private lateinit var firestore: FirebaseFirestore
    private var selectedPeriod: String = "Bulanan"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBpjsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize Firestore
        firestore = FirebaseFirestore.getInstance()

        // Fetch data from Firestore
        fetchBpjsData()

        binding.tambah.setOnClickListener {
            val intent = Intent(this, ManageBpjsActivity::class.java)
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
                fetchBpjsData() // Refresh data based on selected period
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    private fun fetchBpjsData() {
        firestore.collection("bpjs")
            .get()
            .addOnSuccessListener { documents ->
                val bpjsList = documents.toObjects(Bpjs::class.java)
                populateDataTable(bpjsList)
                calculateTotalBpjs(bpjsList)
            }
            .addOnFailureListener { exception ->
                // Handle any errors
            }
    }

    private fun populateDataTable(bpjsList: List<Bpjs>) {
        val columns = ArrayList<Column>()
        columns.add(Column("no", "No."))
        columns.add(Column("nama", "Nama"))
        columns.add(Column("jmlPeserta", "Jumlah Peserta"))
        columns.add(Column("totalBayar", "Total Bayar"))
        columns.add(Column("tanggal", "Tanggal"))

        // Clear existing views
        binding.dataTable.removeAllViews()

        binding.dataTable.setTable(columns, bpjsList, isActionButtonShow = true)

        binding.dataTable.setOnClickListener(object : OnWebViewComponentClickListener {
            override fun onRowClicked(dataStr: String) {
                val clicked = Gson().fromJson(dataStr, Bpjs::class.java)
                val intent = Intent(applicationContext, ManageBpjsActivity::class.java)
                intent.putExtra("mode", 2)
                intent.putExtra("no", clicked.no)
                intent.putExtra("nama", clicked.nama)
                intent.putExtra("jmlPeserta", clicked.jmlPeserta)
                intent.putExtra("totalBayar", clicked.totalBayar)
                intent.putExtra("tanggal", clicked.tanggal)
                startActivity(intent)
            }
        })
    }

    private fun calculateTotalBpjs(bpjsList: List<Bpjs>) {
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val calendar = Calendar.getInstance()
        var totalBpjs = 0

        for (bpjs in bpjsList) {
            val bpjsDate = dateFormat.parse(bpjs.tanggal)
            if (bpjsDate != null) {
                when (selectedPeriod) {
                    "Seminggu" -> {
                        calendar.time = Date()
                        calendar.add(Calendar.DAY_OF_YEAR, -7)
                        if (bpjsDate.after(calendar.time)) {
                            totalBpjs += bpjs.totalBayar.toIntOrNull() ?: 0
                        }
                    }
                    "Sebulan" -> {
                        calendar.time = Date()
                        calendar.add(Calendar.MONTH, -1)
                        if (bpjsDate.after(calendar.time)) {
                            totalBpjs += bpjs.totalBayar.toIntOrNull() ?: 0
                        }
                    }
                    "Setahun" -> {
                        calendar.time = Date()
                        calendar.add(Calendar.YEAR, -1)
                        if (bpjsDate.after(calendar.time)) {
                            totalBpjs += bpjs.totalBayar.toIntOrNull() ?: 0
                        }
                    }
                }
            }
        }
        binding.txtTotal.text = "Rp $totalBpjs"
    }
}
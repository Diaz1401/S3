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
import com.siwiba.databinding.ActivityLogisticsBinding
import com.siwiba.wba.model.Logistics
import java.text.SimpleDateFormat
import java.util.*

class LogistikActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLogisticsBinding
    private lateinit var firestore: FirebaseFirestore
    private var selectedPeriod: String = "Bulanan"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLogisticsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize Firestore
        firestore = FirebaseFirestore.getInstance()

        // Fetch data from Firestore
        fetchLogisticsData()

        binding.tambahdata.setOnClickListener {
            val intent = Intent(this, ManageLogisticsActivity::class.java)
            intent.putExtra("mode", 1)
            startActivity(intent)
        }

        // Set up Spinner
        val periods = arrayOf("Seminggu", "Sebulan", "Setahun")
        val adapter = ArrayAdapter(this, R.layout.item_spinner, periods)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerTanggal.adapter = adapter
        binding.txtTanggal.text = "Untuk $selectedPeriod Terakhir"

        binding.spinnerTanggal.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                selectedPeriod = periods[position]
                binding.txtTanggal.text = "Untuk $selectedPeriod terakhir"
                fetchLogisticsData() // Refresh data based on selected period
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    private fun fetchLogisticsData() {
        firestore.collection("logistics")
            .get()
            .addOnSuccessListener { documents ->
                val logisticsList = documents.toObjects(Logistics::class.java)
                populateDataTable(logisticsList)
                calculateTotalLogistics(logisticsList)
            }
            .addOnFailureListener { exception ->
                // Handle any errors
            }
    }

    private fun populateDataTable(logisticsList: List<Logistics>) {
        val columns = ArrayList<Column>()
        columns.add(Column("no", "No."))
        columns.add(Column("namabarang", "NamaBarang"))
        columns.add(Column("hargaBarang", "HargaBarang"))
        columns.add(Column("keterangan", "Keterangan"))
        columns.add(Column("tanggal", "Tanggal"))

        // Clear existing views
        binding.dataTable.removeAllViews()

        binding.dataTable.setTable(columns, logisticsList, isActionButtonShow = true)

        binding.dataTable.setOnClickListener(object : OnWebViewComponentClickListener {
            override fun onRowClicked(dataStr: String) {
                val logisticsClicked = Gson().fromJson(dataStr, Logistics::class.java)
                val intent = Intent(applicationContext, ManageLogisticsActivity::class.java)
                intent.putExtra("mode", 2)
                intent.putExtra("namabarang", logisticsClicked.namabarang)
                intent.putExtra("hargaBarang", logisticsClicked.hargaBarang)
                intent.putExtra("keterangan", logisticsClicked.keterangan)
                intent.putExtra("tanggal", logisticsClicked.tanggal)
                startActivity(intent)
            }
        })
    }

    private fun calculateTotalLogistics(logisticsList: List<Logistics>) {
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val calendar = Calendar.getInstance()
        var totalLogistics = 0

        for (logistics in logisticsList) {
            val logisticsDate = dateFormat.parse(logistics.tanggal)
            if (logisticsDate != null) {
                when (selectedPeriod) {
                    "Seminggu" -> {
                        calendar.time = Date()
                        calendar.add(Calendar.DAY_OF_YEAR, -7)
                        if (logisticsDate.after(calendar.time)) {
                            totalLogistics += logistics.hargaBarang.toIntOrNull() ?: 0
                        }
                    }
                    "Sebulan" -> {
                        calendar.time = Date()
                        calendar.add(Calendar.MONTH, -1)
                        if (logisticsDate.after(calendar.time)) {
                            totalLogistics += logistics.hargaBarang.toIntOrNull() ?: 0
                        }
                    }
                    "Setahun" -> {
                        calendar.time = Date()
                        calendar.add(Calendar.YEAR, -1)
                        if (logisticsDate.after(calendar.time)) {
                            totalLogistics += logistics.hargaBarang.toIntOrNull() ?: 0
                        }
                    }
                }
            }
        }
        binding.txtTot.text = "Rp $totalLogistics"
    }
}

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
import com.siwiba.databinding.ActivityPajakBinding
import com.siwiba.wba.model.Pajak
import java.text.SimpleDateFormat
import java.util.*

class PajakActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPajakBinding
    private lateinit var firestore: FirebaseFirestore
    private var selectedPeriod: String = "Bulanan"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPajakBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize Firestore
        firestore = FirebaseFirestore.getInstance()

        // Fetch data from Firestore
        fetchPajakData()

        binding.tambah.setOnClickListener {
            val intent = Intent(this, ManagePajakActivity::class.java)
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
                fetchPajakData() // Refresh data based on selected period
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    private fun fetchPajakData() {
        firestore.collection("pajak")
            .get()
            .addOnSuccessListener { documents ->
                val pajakList = documents.toObjects(Pajak::class.java)
                populateDataTable(pajakList)
                calculateTotalPajak(pajakList)
            }
            .addOnFailureListener { exception ->
                // Handle any errors
            }
    }

    private fun populateDataTable(pajakList: List<Pajak>) {
        val columns = ArrayList<Column>()
        columns.add(Column("no", "No."))
        columns.add(Column("jenis", "Jenis Pajak"))
        columns.add(Column("nominal", "Nominal Pajak"))
        columns.add(Column("periode", "Periode Pajak"))
        columns.add(Column("tanggal", "Tanggal"))

        // Clear existing views
        binding.dataTable.removeAllViews()

        binding.dataTable.setTable(columns, pajakList, isActionButtonShow = true)

        binding.dataTable.setOnClickListener(object : OnWebViewComponentClickListener {
            override fun onRowClicked(dataStr: String) {
                val pajakClicked = Gson().fromJson(dataStr, Pajak::class.java)
                val intent = Intent(applicationContext, ManagePajakActivity::class.java)
                intent.putExtra("mode", 2)
                intent.putExtra("no", pajakClicked.no)
                intent.putExtra("jenis", pajakClicked.jenis)
                intent.putExtra("nominal", pajakClicked.nominal)
                intent.putExtra("periode", pajakClicked.periode)
                intent.putExtra("tanggal", pajakClicked.tanggal)
                startActivity(intent)
            }
        })
    }

    private fun calculateTotalPajak(pajakList: List<Pajak>) {
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val calendar = Calendar.getInstance()
        var totalPajak = 0

        for (pajak in pajakList) {
            val pajakDate = dateFormat.parse(pajak.tanggal)
            if (pajakDate != null) {
                when (selectedPeriod) {
                    "Seminggu" -> {
                        calendar.time = Date()
                        calendar.add(Calendar.DAY_OF_YEAR, -7)
                        if (pajakDate.after(calendar.time)) {
                            totalPajak += pajak.nominal.toIntOrNull() ?: 0
                        }
                    }
                    "Sebulan" -> {
                        calendar.time = Date()
                        calendar.add(Calendar.MONTH, -1)
                        if (pajakDate.after(calendar.time)) {
                            totalPajak += pajak.nominal.toIntOrNull() ?: 0
                        }
                    }
                    "Setahun" -> {
                        calendar.time = Date()
                        calendar.add(Calendar.YEAR, -1)
                        if (pajakDate.after(calendar.time)) {
                            totalPajak += pajak.nominal.toIntOrNull() ?: 0
                        }
                    }
                }
            }
        }
        binding.txtTotal.text = "Rp $totalPajak"
    }
}
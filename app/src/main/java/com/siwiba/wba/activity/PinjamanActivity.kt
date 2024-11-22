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
import com.siwiba.databinding.ActivityPinjamanBinding
import com.siwiba.wba.model.Pinjaman
import java.text.SimpleDateFormat
import java.util.*

class PinjamanActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPinjamanBinding
    private lateinit var firestore: FirebaseFirestore
    private var selectedPeriod: String = "Bulanan"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPinjamanBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize Firestore
        firestore = FirebaseFirestore.getInstance()

        // Fetch data from Firestore
        fetchData()

        binding.tambah.setOnClickListener {
            val intent = Intent(this, ManagePinjamanActivity::class.java)
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
                fetchData() // Refresh data based on selected period
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    private fun fetchData() {
        firestore.collection("pinjaman")
            .get()
            .addOnSuccessListener { documents ->
                val list = documents.toObjects(Pinjaman::class.java)
                populateDataTable(list)
                calculateTotalPinjaman(list)
            }
            .addOnFailureListener { exception ->
                // Handle any errors
            }
    }

    private fun populateDataTable(pinjamanList: List<Pinjaman>) {
        val columns = ArrayList<Column>()
        columns.add(Column("no", "No."))
        columns.add(Column("deskripsi", "Deskripsi"))
        columns.add(Column("jumlahPinjaman", "Jumlah Pinjaman"))
        columns.add(Column("tanggalPinjam", "Tanggal Pinjam"))
        columns.add(Column("tanggalBayar", "Tanggal Bayar"))

        // Clear existing views
        binding.dataTable.removeAllViews()

        binding.dataTable.setTable(columns, pinjamanList, isActionButtonShow = true)

        binding.dataTable.setOnClickListener(object : OnWebViewComponentClickListener {
            override fun onRowClicked(dataStr: String) {
                val pinjamanClicked = Gson().fromJson(dataStr, Pinjaman::class.java)
                val intent = Intent(applicationContext, ManagePinjamanActivity::class.java)
                intent.putExtra("mode", 2)
                intent.putExtra("no", pinjamanClicked.no)
                intent.putExtra("deskripsi", pinjamanClicked.deskripsi)
                intent.putExtra("jumlahPinjaman", pinjamanClicked.jumlahPinjaman)
                intent.putExtra("tanggalPinjam", pinjamanClicked.tanggalPinjam)
                intent.putExtra("tanggalBayar", pinjamanClicked.tanggalBayar)
                startActivity(intent)
            }
        })
    }

    private fun calculateTotalPinjaman(pinjamanList: List<Pinjaman>) {
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val calendar = Calendar.getInstance()
        var total = 0

        for (pinjaman in pinjamanList) {
            val pinjamanDate = dateFormat.parse(pinjaman.tanggalPinjam)
            if (pinjamanDate != null) {
                when (selectedPeriod) {
                    "Seminggu" -> {
                        calendar.time = Date()
                        calendar.add(Calendar.DAY_OF_YEAR, -7)
                        if (pinjamanDate.after(calendar.time)) {
                            total += pinjaman.jumlahPinjaman.toIntOrNull() ?: 0
                        }
                    }
                    "Sebulan" -> {
                        calendar.time = Date()
                        calendar.add(Calendar.MONTH, -1)
                        if (pinjamanDate.after(calendar.time)) {
                            total += pinjaman.jumlahPinjaman.toIntOrNull() ?: 0
                        }
                    }
                    "Setahun" -> {
                        calendar.time = Date()
                        calendar.add(Calendar.YEAR, -1)
                        if (pinjamanDate.after(calendar.time)) {
                            total += pinjaman.jumlahPinjaman.toIntOrNull() ?: 0
                        }
                    }
                }
            }
        }
        binding.txtTotal.text = "Rp $total"
    }
}
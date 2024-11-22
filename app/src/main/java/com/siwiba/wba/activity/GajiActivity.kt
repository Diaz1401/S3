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
import com.siwiba.databinding.ActivityGajiBinding
import com.siwiba.wba.model.Gaji
import java.text.SimpleDateFormat
import java.util.*

class GajiActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGajiBinding
    private lateinit var firestore: FirebaseFirestore
    private var selectedPeriod: String = "Bulanan"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGajiBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize Firestore
        firestore = FirebaseFirestore.getInstance()

        // Fetch data from Firestore
        fetchGajiData()

        binding.tambah.setOnClickListener {
            val intent = Intent(this, ManageGajiActivity::class.java)
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
                fetchGajiData() // Refresh data based on selected period
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    private fun fetchGajiData() {
        firestore.collection("gaji")
            .get()
            .addOnSuccessListener { documents ->
                val gajiList = documents.toObjects(Gaji::class.java)
                populateDataTable(gajiList)
                calculateTotalGaji(gajiList)
            }
            .addOnFailureListener { exception ->
                // Handle any errors
            }
    }

    private fun populateDataTable(gajiList: List<Gaji>) {
        val columns = ArrayList<Column>()
        columns.add(Column("no", "No."))
        columns.add(Column("karyawan", "Karyawan"))
        columns.add(Column("posisi", "Posisi/Jabatan"))
        columns.add(Column("gaji", "Gaji"))
        columns.add(Column("tanggal", "Tanggal"))

        // Clear existing views
        binding.dataTable.removeAllViews()

        binding.dataTable.setTable(columns, gajiList, isActionButtonShow = true)

        binding.dataTable.setOnClickListener(object : OnWebViewComponentClickListener {
            override fun onRowClicked(dataStr: String) {
                val gajiClicked = Gson().fromJson(dataStr, Gaji::class.java)
                val intent = Intent(applicationContext, ManageGajiActivity::class.java)
                intent.putExtra("mode", 2)
                intent.putExtra("no", gajiClicked.no)
                intent.putExtra("karyawan", gajiClicked.karyawan)
                intent.putExtra("posisi", gajiClicked.posisi)
                intent.putExtra("gaji", gajiClicked.gaji)
                intent.putExtra("tanggal", gajiClicked.tanggal)
                startActivity(intent)
            }
        })
    }

    private fun calculateTotalGaji(gajiList: List<Gaji>) {
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val calendar = Calendar.getInstance()
        var totalGaji = 0

        for (gaji in gajiList) {
            val gajiDate = dateFormat.parse(gaji.tanggal)
            if (gajiDate != null) {
                when (selectedPeriod) {
                    "Seminggu" -> {
                        calendar.time = Date()
                        calendar.add(Calendar.DAY_OF_YEAR, -7)
                        if (gajiDate.after(calendar.time)) {
                            totalGaji += gaji.gaji.toIntOrNull() ?: 0
                        }
                    }
                    "Sebulan" -> {
                        calendar.time = Date()
                        calendar.add(Calendar.MONTH, -1)
                        if (gajiDate.after(calendar.time)) {
                            totalGaji += gaji.gaji.toIntOrNull() ?: 0
                        }
                    }
                    "Setahun" -> {
                        calendar.time = Date()
                        calendar.add(Calendar.YEAR, -1)
                        if (gajiDate.after(calendar.time)) {
                            totalGaji += gaji.gaji.toIntOrNull() ?: 0
                        }
                    }
                }
            }
        }
        binding.txtTotal.text = "Rp $totalGaji"
    }
}
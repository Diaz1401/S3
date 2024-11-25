package com.siwiba.wba.activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.BitmapFactory
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
import com.siwiba.wba.model.Saldo
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Calendar
import java.util.Locale
import android.util.Base64

class PajakActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPajakBinding
    private lateinit var firestore: FirebaseFirestore
    private var selectedPeriod: String = "Bulanan"
    private val whichSaldo = "pajak"
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPajakBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize Firestore
        firestore = FirebaseFirestore.getInstance()

        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)

        // Load profile picture from SharedPreferences
        loadProfilePicture()

        binding.tambah.setOnClickListener {
            val intent = Intent(this, ManageActivity::class.java)
            intent.putExtra("mode", 1)
            intent.putExtra("whichSaldo", whichSaldo)
            startActivity(intent)
        }

        // Set up Spinner
        val periods = arrayOf("Seminggu", "Sebulan", "Setahun")
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
    }

    private fun fetchSaldoData() {
        firestore.collection("saldo")
            .document(whichSaldo)
            .collection("data")
            .get()
            .addOnSuccessListener { documents ->
                val saldoList = documents.toObjects(Saldo::class.java)
                populateDataTable(saldoList)
                calculateTotalSaldo(saldoList)
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
        columns.add(Column("timestamp", "Timestamp"))

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
                startActivity(intent)
            }
        })
    }

    private fun calculateTotalSaldo(saldoList: List<Saldo>) {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())
        val calendar = Calendar.getInstance()
        var totalSaldo = 0

        for (saldo in saldoList) {
            val saldoDate = dateFormat.parse(saldo.timestamp)
            if (saldoDate != null) {
                when (selectedPeriod) {
                    "Seminggu" -> {
                        calendar.time = Date()
                        calendar.add(Calendar.DAY_OF_YEAR, -7)
                        if (saldoDate.after(calendar.time)) {
                            totalSaldo += saldo.debit - saldo.kredit
                        }
                    }
                    "Sebulan" -> {
                        calendar.time = Date()
                        calendar.add(Calendar.MONTH, -1)
                        if (saldoDate.after(calendar.time)) {
                            totalSaldo += saldo.debit - saldo.kredit
                        }
                    }
                    "Setahun" -> {
                        calendar.time = Date()
                        calendar.add(Calendar.YEAR, -1)
                        if (saldoDate.after(calendar.time)) {
                            totalSaldo += saldo.debit - saldo.kredit
                        }
                    }
                }
            }
        }
        binding.txtTotal.text = "Rp $totalSaldo"
    }

    private fun loadProfilePicture() {
        val profileImage = sharedPreferences.getString("profileImage", null)
        if (profileImage != null) {
            val imageBytes = Base64.decode(profileImage, Base64.DEFAULT)
            val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
            binding.imgProfile.setImageBitmap(bitmap)
        }
    }
}
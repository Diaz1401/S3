package com.siwiba.wba.activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.BitmapFactory
import android.net.Uri
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
import com.siwiba.wba.model.Saldo
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Calendar
import java.util.Locale
import android.util.Base64
import android.widget.Toast
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.Query
import com.opencsv.CSVReader
import com.opencsv.CSVWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter

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

        // Load profile picture from SharedPreferences
        loadProfilePicture()

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
                        val importedData = importDataFromCSV(uri)
                        firestore.collection("saldo")
                            .document(whichSaldo)
                            .get()
                            .addOnSuccessListener { document ->
                                var saldo = document.getLong("saldo") ?: 0

                                firestore.collection("saldo")
                                    .document(whichSaldo)
                                    .collection("data")
                                    .orderBy("no", Query.Direction.DESCENDING)
                                    .limit(1)
                                    .get()
                                    .addOnSuccessListener { documents ->
                                        var newNo = 1
                                        if (!documents.isEmpty) {
                                            val highestNo = documents.documents[0].getLong("no") ?: 0
                                            newNo = highestNo.toInt() + 1
                                        }

                                        val batch = firestore.batch()
                                        val collectionRef = firestore.collection("saldo")
                                            .document(whichSaldo)
                                            .collection("data")

                                        importedData.forEach { saldoItem ->
                                            val data = mapOf(
                                                "no" to newNo,
                                                "keterangan" to saldoItem.keterangan,
                                                "debit" to saldoItem.debit,
                                                "kredit" to saldoItem.kredit,
                                                "editor" to saldoItem.editor,
                                                "tanggal" to saldoItem.tanggal
                                            )
                                            saldo -= saldoItem.kredit
                                            val docRef = collectionRef.document(newNo.toString())
                                            batch.set(docRef, data)
                                            newNo++
                                        }

                                        batch.commit().addOnSuccessListener {
                                            firestore.collection("saldo")
                                                .document(whichSaldo)
                                                .update("saldo", saldo)
                                                .addOnSuccessListener {
                                                    Toast.makeText(this, "Sukses menambahkan data", Toast.LENGTH_SHORT).show()
                                                    fetchSaldoData()
                                                }
                                                .addOnFailureListener {
                                                    Toast.makeText(this, "Gagal memperbarui saldo", Toast.LENGTH_SHORT).show()
                                                }
                                        }.addOnFailureListener {
                                            Toast.makeText(this, "Gagal menyimpan data", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                    .addOnFailureListener {
                                        Toast.makeText(this, "Gagal mengambil data", Toast.LENGTH_SHORT).show()
                                    }
                            }
                            .addOnFailureListener {
                                Toast.makeText(this, "Gagal mengambil data", Toast.LENGTH_SHORT).show()
                            }
                    }
                }
            }
        }
    }


    companion object {
        private const val REQUEST_CODE_EXPORT = 1
        private const val REQUEST_CODE_IMPORT = 2
    }

    private fun exportDataToCSV(data: List<Saldo>, uri: Uri) {
        contentResolver.openOutputStream(uri)?.use { outputStream ->
            val writer = CSVWriter(OutputStreamWriter(outputStream))
            writer.writeNext(arrayOf("No", "Keterangan", "Debit", "Kredit", "Editor", "Tanggal"))
            for (saldo in data) {
                writer.writeNext(arrayOf(saldo.no.toString(), saldo.keterangan, saldo.debit.toString(), saldo.kredit.toString(), saldo.editor.toString(), saldo.tanggal))
            }
            writer.close()
        }
    }

    private fun importDataFromCSV(uri: Uri): List<Saldo> {
        val data = mutableListOf<Saldo>()
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
                    editor = nextLine!![4],
                    tanggal = nextLine!![5]
                )
                data.add(saldo)
            }
            reader.close()
        }
        return data
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

    private fun populateDataTable(saldoList: List<Saldo>) {
        val columns = ArrayList<Column>()
        columns.add(Column("no", "No."))
        columns.add(Column("keterangan", "Keterangan"))
        columns.add(Column("debit", "Debit"))
        columns.add(Column("kredit", "Kredit"))
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
                intent.putExtra("editor", saldoClicked.editor)
                intent.putExtra("tanggal", saldoClicked.tanggal)
                startActivity(intent)
            }
        })
    }

    private fun calculateTotalSaldo(saldoList: List<Saldo>) {
        val dateFormat = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
        val calendar = Calendar.getInstance()
        var totalDebit = 0
        var totalKredit = 0

        for (saldo in saldoList) {
            val saldoDate = dateFormat.parse(saldo.tanggal)
            if (saldoDate != null) {
                when (selectedPeriod) {
                    "Total" -> {
                        totalDebit += saldo.debit
                        totalKredit += saldo.kredit
                    }
                    "Seminggu" -> {
                        calendar.time = Date()
                        calendar.add(Calendar.DAY_OF_YEAR, -7)
                        if (saldoDate.after(calendar.time)) {
                            totalDebit += saldo.debit
                            totalKredit += saldo.kredit
                        }
                    }
                    "Sebulan" -> {
                        calendar.time = Date()
                        calendar.add(Calendar.MONTH, -1)
                        if (saldoDate.after(calendar.time)) {
                            totalDebit += saldo.debit
                            totalKredit += saldo.kredit
                        }
                    }
                    "Setahun" -> {
                        calendar.time = Date()
                        calendar.add(Calendar.YEAR, -1)
                        if (saldoDate.after(calendar.time)) {
                            totalDebit += saldo.debit
                            totalKredit += saldo.kredit
                        }
                    }
                }
            }
        }
        binding.txtDebit.text = "Debit Rp $totalDebit"
        binding.txtKredit.text = "Kredit Rp $totalKredit"
        binding.txtTotal.text = "Rp ${totalDebit - totalKredit}"
        if (selectedPeriod == "Total") {
            binding.txtPeriode.text = "Total"
        } else {
            binding.txtPeriode.text = "Untuk $selectedPeriod Terakhir"
        }
    }

    private fun loadProfilePicture() {
        val profileImage = sharedPreferences.getString("profileImage", null)
        if (profileImage != null) {
            val imageBytes = Base64.decode(profileImage, Base64.DEFAULT)
            val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
            binding.imgProfile.setImageBitmap(bitmap)
        }
    }

    override fun onResume() {
        super.onResume()
        fetchSaldoData()
    }
}
package com.siwiba.wba.fragment

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.firebase.firestore.FirebaseFirestore
import com.siwiba.databinding.FragmentKeuanganBinding
import com.siwiba.wba.activity.*
import com.siwiba.MainActivity
import com.siwiba.wba.model.Saldo
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity.RESULT_OK
import com.dewakoding.androiddatatable.data.Column
import com.dewakoding.androiddatatable.listener.OnWebViewComponentClickListener
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.Query
import com.google.gson.Gson
import com.opencsv.CSVReader
import com.opencsv.CSVWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import com.siwiba.R
import com.siwiba.util.NumberFormat

class KeuanganFragment : Fragment() {

    private var _binding: FragmentKeuanganBinding? = null
    private val binding get() = _binding!!
    private lateinit var firestore: FirebaseFirestore
    private lateinit var sharedPreferences: SharedPreferences
    private val whichSaldo = "utama"
    private var editor: String = ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentKeuanganBinding.inflate(inflater, container, false)
        firestore = FirebaseFirestore.getInstance()
        return binding.root
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize SharedPreferences
        sharedPreferences = requireContext().getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        editor = sharedPreferences.getString("name", "Editor tidak diketahui") ?: "Editor tidak diketahui"

        binding.frameGaji.setOnClickListener {
            val intent = Intent(activity, SaldoActivity::class.java)
            intent.putExtra("whichSaldo", "gaji")
            startActivity(intent)
        }

        binding.framePajak.setOnClickListener {
            val intent = Intent(activity, SaldoActivity::class.java)
            intent.putExtra("whichSaldo", "pajak")
            startActivity(intent)
        }

        binding.framePinjaman.setOnClickListener {
            val intent = Intent(activity, SaldoActivity::class.java)
            intent.putExtra("whichSaldo", "pinjaman")
            startActivity(intent)
        }

        binding.frameKas.setOnClickListener {
            val intent = Intent(activity, SaldoActivity::class.java)
            intent.putExtra("whichSaldo", "kas")
            startActivity(intent)
        }

        binding.frameBpjs.setOnClickListener {
            val intent = Intent(activity, SaldoActivity::class.java)
            intent.putExtra("whichSaldo", "bpjs")
            startActivity(intent)
        }

        binding.frameLogistik.setOnClickListener {
            val intent = Intent(activity, SaldoActivity::class.java)
            intent.putExtra("whichSaldo", "logistik")
            startActivity(intent)
        }

        binding.btnTambah.setOnClickListener {
            // if user if admin allow click else show toast not allowed
            val isAdmin = sharedPreferences.getBoolean("isAdmin", false)
            if (isAdmin) {
                val intent = Intent(activity, ManageAdminActivity::class.java)
                intent.putExtra("mode", 1)
                intent.putExtra("whichSaldo", whichSaldo)
                intent.putExtra("editor", editor)
                startActivity(intent)
            } else {
                Toast.makeText(requireContext(), "Anda tidak memiliki akses untuk menambah data", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
        }

        binding.dokumen.setOnClickListener {
            val isAdmin = sharedPreferences.getBoolean("isAdmin", false)
            AlertDialog.Builder(requireContext())
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
                                // Import data
                                val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                                    addCategory(Intent.CATEGORY_OPENABLE)
                                    type = "text/comma-separated-values"
                                }
                                startActivityForResult(intent, REQUEST_CODE_IMPORT)
                            } else {
                                Toast.makeText(
                                    requireContext(),
                                    "Anda tidak memiliki akses untuk mengimport data",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                }
                .show()
        }
        binding.btnBackToHome.setOnClickListener {
            // Navigate back to the dashboard fragment
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, DashboardFragment())
                .addToBackStack(null)
                .commit()
        }
    }

    companion object {
        private const val REQUEST_CODE_EXPORT = 1
        private const val REQUEST_CODE_IMPORT = 2
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
                            .collection("data")
                            .orderBy("no", Query.Direction.DESCENDING)
                            .limit(1)
                            .get()
                            .addOnSuccessListener { document ->
                                var newNo = 1
                                var lastSaldo = 0
                                if (!document.isEmpty) {
                                    val highestNo = document.documents[0].getLong("no") ?: 1
                                    newNo = highestNo.toInt() + 1
                                    lastSaldo = document.documents[0].getLong("saldo")?.toInt() ?: 0
                                }

                                val batch = firestore.batch()
                                val collectionRef = firestore.collection("saldo")
                                    .document(whichSaldo)
                                    .collection("data")

                                importedData.forEach { saldoItem ->
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
                                }

                                batch.commit().addOnSuccessListener {
                                    Toast.makeText(
                                        requireContext(),
                                        "Berhasil menyimpan data",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }.addOnFailureListener {
                                    Toast.makeText(
                                        requireContext(),
                                        "Gagal menyimpan data",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                            .addOnFailureListener {
                                Toast.makeText(requireContext(), "Gagal mengambil data", Toast.LENGTH_SHORT).show()
                            }
                    }
                }
            }
        }
    }

    private fun exportDataToCSV(data: List<Saldo>, uri: Uri) {
        requireContext().contentResolver.openOutputStream(uri)?.use { outputStream ->
            val writer = CSVWriter(OutputStreamWriter(outputStream))
            writer.writeNext(arrayOf("No", "Keterangan", "Debit", "Kredit", "Saldo", "Editor", "Tanggal"))
            for (saldo in data) {
                writer.writeNext(arrayOf(saldo.no.toString(), saldo.keterangan, saldo.debit.toString(), saldo.kredit.toString(), saldo.saldo.toString(), saldo.editor, saldo.tanggal))
            }
            writer.close()
        }
    }

    private fun importDataFromCSV(uri: Uri): List<Saldo> {
        val data = mutableListOf<Saldo>()

        requireContext().contentResolver.openInputStream(uri)?.use { inputStream ->
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
                data.add(saldo)
            }
            reader.close()
        }
        return data
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
        val binding = _binding ?: return // Ensure binding is not null

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
        val customColor = ContextCompat.getColor(requireContext(), R.color.siwiba_light)
        binding.dataTable.setBackgroundColor(customColor)

        binding.dataTable.setTable(columns, saldoList, isActionButtonShow = sharedPreferences.getBoolean("isAdmin", false))

        binding.dataTable.setOnClickListener(object : OnWebViewComponentClickListener {
            override fun onRowClicked(dataStr: String) {
                val saldoClicked = Gson().fromJson(dataStr, Saldo::class.java)
                val intent = Intent(activity, ManageAdminActivity::class.java)
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
        val binding = _binding ?: return // Ensure binding is not null

        var totalSaldo = 0
        var totalSaldoDebit = 0
        var totalSaldoKredit = 0

        firestore.collection("saldo")
            .document(whichSaldo)
            .collection("data")
            .orderBy("no", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { documents ->
                val saldo = documents.firstOrNull()?.getLong("saldo")?.toInt() ?: 0
                totalSaldo += saldo
                documents.forEach { document ->
                    val debit = document.getLong("debit")?.toInt() ?: 0
                    val kredit = document.getLong("kredit")?.toInt() ?: 0

                    totalSaldoDebit += debit
                    totalSaldoKredit += kredit
                }
                // Set formatted total, debit, kredit with "Rp" in front
                binding.txtTotal.text = "Rp ${NumberFormat().formatCurrency(totalSaldo.toString())}"
                binding.txtTotalDebit.text = "Rp ${NumberFormat().formatCurrency(totalSaldoDebit.toString())}"
                binding.txtTotalKredit.text = "Rp ${NumberFormat().formatCurrency(totalSaldoKredit.toString())}"
            }
            .addOnFailureListener { exception ->
                // Handle any errors
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onResume() {
        super.onResume()
        fetchSaldoData()
    }
}
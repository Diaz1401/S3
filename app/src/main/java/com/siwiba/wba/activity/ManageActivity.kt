package com.siwiba.wba.activity

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.siwiba.databinding.ActivityManageBinding
import java.sql.Timestamp
import java.util.*
import java.text.SimpleDateFormat

class ManageActivity : AppCompatActivity() {

    private lateinit var binding: ActivityManageBinding
    private lateinit var firestore: FirebaseFirestore
    private var mode: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityManageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize Firestore
        firestore = FirebaseFirestore.getInstance()

        // Get mode & saldo from intent
        mode = intent.getIntExtra("mode", 0)
        val saldo = intent.getStringExtra("whichSaldo") ?: ""
        if (saldo.isEmpty()) {
            Toast.makeText(this, "Invalid mode or saldo", Toast.LENGTH_SHORT).show()
            finish()
        }
        when (mode) {
            1 -> setupAddMode(saldo)
            2 -> {
                loadData()
                setupDeleteMode(saldo)
                setupUpdateMode(saldo)
            }
            else -> {
                Toast.makeText(this, "Invalid mode", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
        insertPlaceboData("gaji")
        insertPlaceboData("bpjs")
        insertPlaceboData("kas")
        insertPlaceboData("logistik")
        insertPlaceboData("pinjaman")
        insertPlaceboData("pajak")
    }

    private fun setupAddMode(whichSaldo: String) {
        binding.btnSave.setOnClickListener {
            val keterangan = binding.etKeterangan.text.toString()
            val debit = binding.etDebit.text.toString()
            val kredit = binding.etKredit.text.toString()
            val timestamp = Timestamp(System.currentTimeMillis()).toString()
            var saldo: Long = 0

            // Fetch 'saldo' value from the 'whichSaldo' document in the 'saldo' collection
            firestore.collection("saldo")
                .document(whichSaldo)
                .get()
                .addOnSuccessListener { document ->
                    saldo = document.getLong("saldo") ?: 0
                    if (debit.isNotEmpty()) {
                        saldo += debit.toInt()
                    } else if (kredit.isNotEmpty()) {
                        saldo -= kredit.toInt()
                    }

                    if (keterangan.isEmpty() || debit.isEmpty() || kredit.isEmpty()) {
                        Toast.makeText(this, "Lengkapi semua kolom", Toast.LENGTH_SHORT).show()
                    } else {
                        // Fetch the highest current "no" value from the 'data' collection
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

                                val data = mapOf(
                                    "no" to newNo,
                                    "keterangan" to keterangan,
                                    "debit" to debit.toInt(),
                                    "kredit" to kredit.toInt(),
                                    "timestamp" to timestamp
                                )

                                // Store data in the 'data' collection within the 'whichSaldo' document
                                firestore.collection("saldo")
                                    .document(whichSaldo)
                                    .collection("data")
                                    .document(newNo.toString())
                                    .set(data)
                                    .addOnSuccessListener {
                                        // Update the 'saldo' field in the 'whichSaldo' document
                                        firestore.collection("saldo")
                                            .document(whichSaldo)
                                            .update("saldo", saldo)
                                            .addOnSuccessListener {
                                                Toast.makeText(this, "Sukses menyimpan data", Toast.LENGTH_SHORT).show()
                                                finish()
                                            }
                                            .addOnFailureListener {
                                                Toast.makeText(this, "Gagal memperbarui saldo", Toast.LENGTH_SHORT).show()
                                            }
                                    }
                                    .addOnFailureListener {
                                        Toast.makeText(this, "Gagal menyimpan data", Toast.LENGTH_SHORT).show()
                                    }
                            }
                            .addOnFailureListener {
                                Toast.makeText(this, "Gagal mengambil data", Toast.LENGTH_SHORT).show()
                            }
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Gagal mengambil data", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun loadData() {
        val keterangan = intent.getStringExtra("keterangan")
        val debit = intent.getIntExtra("debit", 0)
        val kredit = intent.getIntExtra("kredit", 0)

        binding.etKeterangan.setText(keterangan)
        binding.etDebit.setText(debit.toString())
        binding.etKredit.setText(kredit.toString())
    }

    private fun setupDeleteMode(whichSaldo: String) {
        binding.btnDelete.setOnClickListener {
            val no = intent.getIntExtra("no", 0)
            val debit = intent.getIntExtra("debit", 0)
            val kredit = intent.getIntExtra("kredit", 0)

            firestore.collection("saldo")
                .document(whichSaldo)
                .collection("data")
                .document(no.toString())
                .delete()
                .addOnSuccessListener {
                    // Update saldo after deletion
                    firestore.collection("saldo")
                        .document(whichSaldo)
                        .get()
                        .addOnSuccessListener { document ->
                            var saldo = document.getLong("saldo") ?: 0
                            saldo -= debit
                            saldo += kredit

                            firestore.collection("saldo")
                                .document(whichSaldo)
                                .update("saldo", saldo)
                                .addOnSuccessListener {
                                    Toast.makeText(this, "Sukses menghapus data", Toast.LENGTH_SHORT).show()
                                    finish()
                                }
                                .addOnFailureListener {
                                    Toast.makeText(this, "Gagal memperbarui saldo", Toast.LENGTH_SHORT).show()
                                }
                        }
                        .addOnFailureListener {
                            Toast.makeText(this, "Gagal mengambil saldo", Toast.LENGTH_SHORT).show()
                        }
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Gagal menghapus data", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun setupUpdateMode(whichSaldo: String) {
        binding.btnSave.setOnClickListener {
            val no = intent.getIntExtra("no", 0)
            val debit = intent.getIntExtra("debit", 0)
            val kredit = intent.getIntExtra("kredit", 0)
            val timestamp = Timestamp(System.currentTimeMillis()).toString()

            val newKeterangan = binding.etKeterangan.text.toString()
            val newDebit = binding.etDebit.text.toString().toInt()
            val newKredit = binding.etKredit.text.toString().toInt()

            if (newKeterangan.isEmpty() || newDebit.toString().isEmpty() || newKredit.toString().isEmpty()) {
                Toast.makeText(this, "Lengkapi semua kolom", Toast.LENGTH_SHORT).show()
            } else {
                val data = mapOf(
                    "no" to no,
                    "keterangan" to newKeterangan,
                    "debit" to newDebit,
                    "kredit" to newKredit,
                    "timestamp" to timestamp
                )

                firestore.collection("saldo")
                    .document(whichSaldo)
                    .collection("data")
                    .document(no.toString())
                    .set(data)
                    .addOnSuccessListener {
                        // Update saldo after updating data
                        firestore.collection("saldo")
                            .document(whichSaldo)
                            .get()
                            .addOnSuccessListener { document ->
                                var saldo = document.getLong("saldo") ?: 0
                                saldo -= debit
                                saldo += kredit
                                saldo += newDebit
                                saldo -= newKredit

                                firestore.collection("saldo")
                                    .document(whichSaldo)
                                    .update("saldo", saldo)
                                    .addOnSuccessListener {
                                        Toast.makeText(this, "Sukses memperbarui data", Toast.LENGTH_SHORT).show()
                                        finish()
                                    }
                                    .addOnFailureListener {
                                        Toast.makeText(this, "Gagal memperbarui saldo", Toast.LENGTH_SHORT).show()
                                    }
                            }
                            .addOnFailureListener {
                                Toast.makeText(this, "Gagal mengambil saldo", Toast.LENGTH_SHORT).show()
                            }
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Gagal memperbarui data", Toast.LENGTH_SHORT).show()
                    }
            }
        }
    }
    private fun insertPlaceboData(whichSaldo: String) {
        val firestore = FirebaseFirestore.getInstance()

        // Definisikan data hardcoded untuk setiap tipe saldo
        val sampleData = when (whichSaldo) {
            "gaji" -> listOf(
                mapOf("keterangan" to "Gaji Bulanan", "debit" to 5000000, "kredit" to 0),
                mapOf("keterangan" to "Bonus", "debit" to 1000000, "kredit" to 0),
                mapOf("keterangan" to "Potongan Pinjaman", "debit" to 0, "kredit" to 500000),
                mapOf("keterangan" to "Lembur", "debit" to 200000, "kredit" to 0)
            )
            "bpjs" -> listOf(
                mapOf("keterangan" to "Pembayaran BPJS Kesehatan", "debit" to 0, "kredit" to 150000),
                mapOf("keterangan" to "Pembayaran BPJS Ketenagakerjaan", "debit" to 0, "kredit" to 200000),
                mapOf("keterangan" to "Subsidi BPJS Perusahaan", "debit" to 500000, "kredit" to 0),
                mapOf("keterangan" to "Pembayaran BPJS Pensiun", "debit" to 0, "kredit" to 100000)
            )
            "kas" -> listOf(
                mapOf("keterangan" to "Pendapatan Penjualan", "debit" to 10000000, "kredit" to 0),
                mapOf("keterangan" to "Pengeluaran Operasional", "debit" to 0, "kredit" to 5000000),
                mapOf("keterangan" to "Investasi Masuk", "debit" to 2000000, "kredit" to 0),
                mapOf("keterangan" to "Pengembalian Dana", "debit" to 1000000, "kredit" to 0)
            )
            "logistik" -> listOf(
                mapOf("keterangan" to "Pengadaan Barang", "debit" to 0, "kredit" to 700000),
                mapOf("keterangan" to "Biaya Transportasi", "debit" to 0, "kredit" to 300000),
                mapOf("keterangan" to "Distribusi Barang", "debit" to 0, "kredit" to 500000),
                mapOf("keterangan" to "Pemeliharaan Gudang", "debit" to 0, "kredit" to 200000)
            )
            "pinjaman" -> listOf(
                mapOf("keterangan" to "Pembayaran Cicilan", "debit" to 0, "kredit" to 1000000),
                mapOf("keterangan" to "Pembayaran Bunga", "debit" to 0, "kredit" to 300000),
                mapOf("keterangan" to "Penerimaan Dana Pinjaman", "debit" to 5000000, "kredit" to 0),
                mapOf("keterangan" to "Pengembalian Dana", "debit" to 0, "kredit" to 2000000)
            )
            "pajak" -> listOf(
                mapOf("keterangan" to "Pajak Penghasilan", "debit" to 0, "kredit" to 1000000),
                mapOf("keterangan" to "Pajak PPN", "debit" to 0, "kredit" to 500000),
                mapOf("keterangan" to "Pajak Daerah", "debit" to 0, "kredit" to 300000),
                mapOf("keterangan" to "Pengembalian Pajak", "debit" to 200000, "kredit" to 0)
            )
            else -> emptyList()
        }

        // Format untuk timestamp
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())

        // Hitung tanggal awal (2 tahun yang lalu)
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.YEAR, -2) // Mulai 2 tahun yang lalu

        var currentSaldo = 0L // Simulasi saldo awal

        for (i in 1..104) { // 104 minggu = 2 tahun
            // Pilih 2-3 data secara acak dari sampleData
            val dataCount = (2..3).random()
            val weekData = sampleData.shuffled().take(dataCount)

            weekData.forEach { data ->
                val debit = data["debit"] as Int
                val kredit = data["kredit"] as Int
                currentSaldo += debit - kredit // Hitung saldo baru

                val transactionData = mapOf(
                    "no" to i,
                    "keterangan" to data["keterangan"],
                    "debit" to debit,
                    "kredit" to kredit,
                    "timestamp" to dateFormat.format(calendar.time), // Format timestamp
                    "saldo" to currentSaldo
                )

                // Simpan data ke Firestore
                firestore.collection("saldo")
                    .document(whichSaldo)
                    .collection("data")
                    .document("$i")
                    .set(transactionData)
                    .addOnSuccessListener {
                        println("Data berhasil dimasukkan untuk minggu ke-$i ($whichSaldo)")
                    }
                    .addOnFailureListener {
                        println("Gagal memasukkan data untuk minggu ke-$i ($whichSaldo): ${it.message}")
                    }
            }

            // Tambahkan 1 minggu ke tanggal
            calendar.add(Calendar.WEEK_OF_YEAR, 1)
        }

        // Perbarui saldo terakhir ke dokumen utama
        firestore.collection("saldo")
            .document(whichSaldo)
            .update("saldo", currentSaldo)
            .addOnSuccessListener {
                Toast.makeText(this, "Data placebo berhasil dimasukkan ($whichSaldo)", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Gagal memperbarui saldo ($whichSaldo): ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun deleteAllData() {
        val firestore = FirebaseFirestore.getInstance()

        // Ambil semua dokumen dalam koleksi "saldo"
        firestore.collection("saldo")
            .get()
            .addOnSuccessListener { saldoDocuments ->
                if (!saldoDocuments.isEmpty) {
                    for (saldoDoc in saldoDocuments) {
                        val saldoId = saldoDoc.id

                        // Ambil semua dokumen dalam sub-koleksi "data" untuk setiap "saldo"
                        firestore.collection("saldo")
                            .document(saldoId)
                            .collection("data")
                            .get()
                            .addOnSuccessListener { dataDocuments ->
                                if (!dataDocuments.isEmpty) {
                                    for (dataDoc in dataDocuments) {
                                        // Hapus setiap dokumen dalam sub-koleksi "data"
                                        firestore.collection("saldo")
                                            .document(saldoId)
                                            .collection("data")
                                            .document(dataDoc.id)
                                            .delete()
                                            .addOnSuccessListener {
                                                println("Dokumen ${dataDoc.id} berhasil dihapus dari saldo $saldoId")
                                            }
                                            .addOnFailureListener { e ->
                                                println("Gagal menghapus dokumen ${dataDoc.id} dari saldo $saldoId: ${e.message}")
                                            }
                                    }
                                } else {
                                    println("Sub-koleksi 'data' kosong untuk saldo $saldoId")
                                }
                            }
                            .addOnFailureListener { e ->
                                println("Gagal mengambil sub-koleksi 'data' untuk saldo $saldoId: ${e.message}")
                            }
                    }
                } else {
                    println("Koleksi 'saldo' kosong, tidak ada data untuk dihapus")
                }
            }
            .addOnFailureListener { e ->
                println("Gagal mengambil koleksi 'saldo': ${e.message}")
            }
    }
}
package com.siwiba.wba.activity

import android.annotation.SuppressLint
import android.os.Bundle
import android.widget.*
import android.graphics.Color
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.*
import com.siwiba.R
import com.siwiba.wba.tabledata
import java.text.DecimalFormat

class PajakActivity : AppCompatActivity() {

    private lateinit var database: DatabaseReference
    private lateinit var tableLayout: TableLayout

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pajak)

        // Inisialisasi Firebase
        database = FirebaseDatabase.getInstance().getReference("TabelData")
        tableLayout = findViewById(R.id.tableLayout) // ID dari TableLayout di XML


        // Tombol tambah data
        val btnTambah = findViewById<ImageButton>(R.id.btnpajak)
        btnTambah.setOnClickListener {
            tambahDataBaru()
        }
    }

    // Menambahkan baris data ke TableLayout
    private fun addRowToTable(no: String, keterangan: String, tanggal: String, saldo: String) {
        val tableRow = TableRow(this)

        // No
        val noTextView = TextView(this)
        noTextView.text = no
        noTextView.setTextColor(Color.parseColor("#000000"))  // Mengatur warna teks menjadi hitam
        noTextView.setPadding(8, 8, 8, 8)
        tableRow.addView(noTextView)

        // Keterangan
        val keteranganTextView = TextView(this)
        keteranganTextView.text = keterangan
        keteranganTextView.setTextColor(Color.parseColor("#000000"))  // Mengatur warna teks menjadi hitam
        keteranganTextView.setPadding(8, 8, 8, 8)
        tableRow.addView(keteranganTextView)

        // Tanggal
        val tanggalTextView = TextView(this)
        tanggalTextView.text = tanggal
        tanggalTextView.setTextColor(Color.parseColor("#000000"))  // Mengatur warna teks menjadi hitam
        tanggalTextView.setPadding(8, 8, 8, 8)
        tableRow.addView(tanggalTextView)

        // Saldo
        val saldoTextView = TextView(this)
        val formattedSaldo = formatSaldo(saldo)
        saldoTextView.text = "Rp $formattedSaldo" // Menambahkan "Rp" di depan saldo yang diformat
        saldoTextView.setTextColor(Color.parseColor("#000000"))  // Mengatur warna teks menjadi hitam
        saldoTextView.setPadding(8, 8, 8, 8)
        tableRow.addView(saldoTextView)

        // Menambahkan row ke TableLayout
        tableLayout.addView(tableRow)
    }

    // Fungsi untuk membuka dialog input data
    @SuppressLint("MissingInflatedId")
    private fun tambahDataBaru() {
        // Membuat Layout untuk dialog input
        val dialogView = layoutInflater.inflate(R.layout.inputdata, null)

        // Mengambil reference ke EditText dari layout dialog
        val edtNo = dialogView.findViewById<EditText>(R.id.edtNo)
        val edtKeterangan = dialogView.findViewById<EditText>(R.id.edtKeterangan)
        val edtTanggal = dialogView.findViewById<EditText>(R.id.edtTanggal)
        val edtSaldo = dialogView.findViewById<EditText>(R.id.edtSaldo)

        // Membuat AlertDialog dengan input form
        val dialog = AlertDialog.Builder(this)
            .setTitle("Tambah Data")
            .setView(dialogView)
            .setPositiveButton("Simpan") { _, _ ->
                // Mengambil input dari EditText
                val no = edtNo.text.toString()
                val keterangan = edtKeterangan.text.toString()
                val tanggal = edtTanggal.text.toString()
                val saldo = edtSaldo.text.toString()

                // Validasi input
                if (no.isBlank() || keterangan.isBlank() || tanggal.isBlank() || saldo.isBlank()) {
                    Toast.makeText(this, "Semua kolom harus diisi", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val dataBaru = tabledata(no, keterangan, tanggal, saldo)

                // Menyimpan data ke Firebase
                val newEntry = database.push() // ID unik untuk setiap entri
                newEntry.setValue(dataBaru).addOnSuccessListener {
                    Toast.makeText(this, "Data berhasil ditambahkan", Toast.LENGTH_SHORT).show()
                    addRowToTable(no, keterangan, tanggal, saldo) // Tampilkan di tabel
                }.addOnFailureListener {
                    Toast.makeText(this, "Gagal menambahkan data", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Batal", null)
            .create()

        // Menampilkan dialog
        dialog.show()
    }

    // Fungsi untuk memformat saldo dengan tanda titik sebagai pemisah ribuan
    private fun formatSaldo(saldo: String): String {
        try {
            val cleanedSaldo = saldo.replace("[^\\d]".toRegex(), "") // Hapus selain angka
            val number = cleanedSaldo.toLong() // Konversi string menjadi angka
            val formatter = DecimalFormat("#,###") // Format angka dengan tanda titik
            return formatter.format(number) // Memformat dan mengembalikan sebagai string
        } catch (e: Exception) {
            e.printStackTrace()
            return saldo // Kembalikan saldo asli jika terjadi kesalahan
        }
    }
}


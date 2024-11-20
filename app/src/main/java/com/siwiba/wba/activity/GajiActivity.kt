package com.siwiba.wba.activity

import android.content.Intent
import android.os.Bundle
import android.widget.TableRow
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore
import com.siwiba.databinding.ActivityGajiBinding
import com.siwiba.wba.model.Gaji

class GajiActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGajiBinding
    private lateinit var firestore: FirebaseFirestore

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

        binding.search.setOnClickListener {
            val searchString = binding.search.text.toString()
            searchGajiData(searchString)
        }
    }

    private fun fetchGajiData() {
        firestore.collection("gaji")
            .get()
            .addOnSuccessListener { documents ->
                val gajiList = documents.toObjects(Gaji::class.java)
                addTableRows(gajiList)
                calculateTotalGaji(gajiList)
            }
            .addOnFailureListener { exception ->
                // Handle any errors
            }
    }

    private fun searchGajiData(searchString: String) {
        firestore.collection("gaji")
            .whereGreaterThanOrEqualTo("karyawan", searchString.lowercase())
            .whereLessThanOrEqualTo("karyawan", searchString.uppercase() + "\uf8ff")
            .get()
            .addOnSuccessListener { documents ->
                val gajiList = documents.toObjects(Gaji::class.java)
                addTableRows(gajiList)
                calculateTotalGaji(gajiList)
            }
            .addOnFailureListener { exception ->
                // Handle any errors
            }
    }

    private fun addTableRows(gajiList: List<Gaji>) {
        binding.tableLayout.removeAllViews()
        addHeaderRow()
        for (gaji in gajiList) {
            val tableRow = TableRow(this)
            tableRow.addView(createTextView(gaji.no.toString()))
            tableRow.addView(createTextView(gaji.karyawan))
            tableRow.addView(createTextView(gaji.posisi))
            tableRow.addView(createTextView(gaji.gaji))
            tableRow.addView(createTextView(gaji.tanggal))
            binding.tableLayout.addView(tableRow)
        }
    }

    private fun addHeaderRow() {
        val headerRow = TableRow(this)
        headerRow.addView(createTextView("No.", true))
        headerRow.addView(createTextView("Karyawan", true))
        headerRow.addView(createTextView("Posisi/Jabatan", true))
        headerRow.addView(createTextView("Gaji", true))
        headerRow.addView(createTextView("Tanggal", true))
        binding.tableLayout.addView(headerRow)
    }

    private fun createTextView(text: String, isHeader: Boolean = false): TextView {
        val textView = TextView(this)
        textView.text = text
        textView.setPadding(8, 8, 8, 8)
        if (isHeader) {
            textView.setBackgroundColor(resources.getColor(android.R.color.darker_gray))
            textView.setTextColor(resources.getColor(android.R.color.black))
            textView.setTypeface(null, android.graphics.Typeface.BOLD)
        }
        return textView
    }

    private fun calculateTotalGaji(gajiList: List<Gaji>) {
        var totalGaji = 0
        for (gaji in gajiList) {
            totalGaji += gaji.gaji.toIntOrNull() ?: 0
        }
        binding.txtTotal.text = "Rp $totalGaji"
    }
}
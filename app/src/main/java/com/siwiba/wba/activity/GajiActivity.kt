package com.siwiba.wba.activity

import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore
import com.siwiba.databinding.ActivityGajiBinding
import com.siwiba.wba.model.Gaji
import com.github.zardozz.FixedHeaderTableLayout.FixedHeaderSubTableLayout
import com.github.zardozz.FixedHeaderTableLayout.FixedHeaderTableLayout
import com.github.zardozz.FixedHeaderTableLayout.FixedHeaderTableRow

class GajiActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGajiBinding
    private lateinit var firestore: FirebaseFirestore
    private lateinit var fixedHeaderTableLayout: FixedHeaderTableLayout
    private var selectedRow: View? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGajiBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize Firestore
        firestore = FirebaseFirestore.getInstance()

        // Initialize FixedHeaderTableLayout
        fixedHeaderTableLayout = binding.fixedHeaderTableLayout

        // Set minimum scale to ensure the table does not zoom out smaller than the screen size
        fixedHeaderTableLayout.setMinScale(1.0f)

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
        val mainTable = FixedHeaderSubTableLayout(this)
        val columnHeaderTable = FixedHeaderSubTableLayout(this)
        val rowHeaderTable = FixedHeaderSubTableLayout(this)
        val cornerTable = FixedHeaderSubTableLayout(this)

        // Add header row
        val headerRow = FixedHeaderTableRow(this)
        headerRow.addView(createTextView("ID", true))
        headerRow.addView(createTextView("Karyawan", true))
        headerRow.addView(createTextView("Posisi/Jabatan", true))
        headerRow.addView(createTextView("Gaji", true))
        headerRow.addView(createTextView("Tanggal", true))
        columnHeaderTable.addView(headerRow)

        // Add data rows
        for (gaji in gajiList) {
            val tableRow = FixedHeaderTableRow(this)
            tableRow.addView(createTextView(gaji.no.toString()))
            tableRow.addView(createTextView(gaji.karyawan))
            tableRow.addView(createTextView(gaji.posisi))
            tableRow.addView(createTextView(gaji.gaji))
            tableRow.addView(createTextView(gaji.tanggal))
            tableRow.setOnClickListener {
                selectedRow?.setBackgroundColor(resources.getColor(android.R.color.transparent))
                tableRow.setBackgroundColor(resources.getColor(android.R.color.holo_blue_light))
                selectedRow = tableRow
            }
            mainTable.addView(tableRow)

            // Add row header
            val rowHeader = FixedHeaderTableRow(this)
            rowHeader.addView(createTextView(gaji.no.toString()))
            rowHeaderTable.addView(rowHeader)
        }

        // Add corner header
        val cornerHeader = FixedHeaderTableRow(this)
        cornerHeader.addView(createTextView("No.", true))
        cornerTable.addView(cornerHeader)

        // Add tables to FixedHeaderTableLayout
        fixedHeaderTableLayout.addViews(mainTable, columnHeaderTable, rowHeaderTable, cornerTable)
    }

    private fun createTextView(text: String, isHeader: Boolean = false): TextView {
        val textView = TextView(this)
        textView.text = text
        textView.setPadding(8, 8, 8, 8)
        textView.gravity = Gravity.CENTER
        if (isHeader) {
            textView.setBackgroundColor(resources.getColor(android.R.color.darker_gray))
            textView.setTextColor(resources.getColor(android.R.color.black))
            textView.setTypeface(null, android.graphics.Typeface.BOLD)
            textView.minWidth = resources.displayMetrics.widthPixels / 5 // Ensure minimum width for each header cell
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
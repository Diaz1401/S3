package com.siwiba.wba.activity

import android.content.Intent
import android.os.Bundle
import android.widget.TableRow
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.siwiba.databinding.ActivityGajiBinding

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
                binding.tableLayout.removeAllViews()
                addTableHeader()
                for (document in documents) {
                    addTableRow(document)
                }
            }
            .addOnFailureListener { exception ->
                // Handle any errors
            }
    }

    private fun searchGajiData(searchString: String) {
        firestore.collection("gaji")
            .whereEqualTo("karyawan", searchString)
            .get()
            .addOnSuccessListener { documents ->
                binding.tableLayout.removeAllViews()
                addTableHeader()
                for (document in documents) {
                    addTableRow(document)
                }
            }
            .addOnFailureListener { exception ->
                // Handle any errors
            }
    }

    private fun addTableHeader() {
        val tableRow = TableRow(this)
        tableRow.addView(createTextView("No."))
        tableRow.addView(createTextView("Karyawan"))
        tableRow.addView(createTextView("Posisi/Jabatan"))
        tableRow.addView(createTextView("Gaji"))
        tableRow.addView(createTextView("Tanggal"))
        binding.tableLayout.addView(tableRow)
    }

    private fun addTableRow(document: DocumentSnapshot) {
        val no = document.getString("no") ?: ""
        val karyawan = document.getString("karyawan") ?: ""
        val posisi = document.getString("posisi") ?: ""
        val gaji = document.getString("gaji") ?: ""
        val tanggal = document.getString("tanggal") ?: ""

        val tableRow = TableRow(this)
        tableRow.addView(createTextView(no))
        tableRow.addView(createTextView(karyawan))
        tableRow.addView(createTextView(posisi))
        tableRow.addView(createTextView(gaji))
        tableRow.addView(createTextView(tanggal))

        binding.tableLayout.addView(tableRow)
    }

    private fun createTextView(text: String): TextView {
        val textView = TextView(this)
        textView.text = text
        textView.setPadding(8, 8, 8, 8)
        return textView
    }
}
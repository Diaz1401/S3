package com.example.fragmenkas

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Spinner
import com.example.fragmenkas.databinding.ActivityTaxBinding

class TaxFormActivity : AppCompatActivity() {
    private lateinit var binding: ActivityTaxBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTaxBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Hide the ActionBar if it exists
        supportActionBar?.hide()

        // Initialize views
        val buttonBack: ImageButton = binding.buttonBack
        val buttonSubmit: Button = binding.buttonSubmit

        // Set up back button functionality
        buttonBack.setOnClickListener {
            finish() // Finish activity and go back to previous screen
        }

        // Set up submit button functionality
        buttonSubmit.setOnClickListener {
            // Retrieve input values
            val nama: String = binding.editTextNama.text.toString()
            val jenisPajak: String = binding.spinnerJenisPajak.selectedItem.toString()
            val pajak: String = binding.editTextPajak.text.toString()
            val totalPajak: String = binding.editTextTotalPajak.text.toString()
            val jumlahPembayaran: String = binding.editTextJumlahPembayaran.text.toString()

            // Add validation or processing logic here
        }
    }
}

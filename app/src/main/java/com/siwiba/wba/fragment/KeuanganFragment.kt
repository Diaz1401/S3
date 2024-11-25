package com.siwiba.wba.fragment

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import com.google.firebase.firestore.FirebaseFirestore
import com.siwiba.databinding.FragmentKeuanganBinding
import com.siwiba.wba.activity.*
import com.siwiba.wba.model.Saldo
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Calendar
import java.util.Locale
import android.graphics.Bitmap
import android.util.Base64
import com.dewakoding.androiddatatable.data.Column
import com.dewakoding.androiddatatable.listener.OnWebViewComponentClickListener
import com.google.gson.Gson

class KeuanganFragment : Fragment() {

    private var _binding: FragmentKeuanganBinding? = null
    private val binding get() = _binding!!
    private lateinit var firestore: FirebaseFirestore
    private lateinit var sharedPreferences: SharedPreferences
    private var selectedPeriod: String = "Bulanan"
    private var selectedSaldo: String = "gaji"

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

        binding.frameGaji.setOnClickListener {
            val intent = Intent(activity, GajiActivity::class.java)
            startActivity(intent)
        }

        binding.framePajak.setOnClickListener {
            val intent = Intent(activity, PajakActivity::class.java)
            startActivity(intent)
        }

        binding.framePinjaman.setOnClickListener {
            val intent = Intent(activity, PinjamanActivity::class.java)
            startActivity(intent)
        }

        binding.frameKas.setOnClickListener {
            val intent = Intent(activity, KasActivity::class.java)
            startActivity(intent)
        }

        binding.frameBpjs.setOnClickListener {
            val intent = Intent(activity, BpjsActivity::class.java)
            startActivity(intent)
        }

        binding.frameLogistik.setOnClickListener {
            val intent = Intent(activity, LogistikActivity::class.java)
            startActivity(intent)
        }

        loadProfilePicture()
        setupSpinners()
    }

    private fun loadProfilePicture() {
        val profilePicture = sharedPreferences.getString("profileImage", null)
        if (profilePicture != null) {
            val decodedString = Base64.decode(profilePicture, Base64.DEFAULT)
            val decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size)
            binding.imgProfile.setImageBitmap(decodedByte)
        }
    }

    private fun setupSpinners() {
        val periods = arrayOf("Seminggu", "Sebulan", "Setahun")
        val periodAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, periods)
        periodAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerPeriode.adapter = periodAdapter

        binding.spinnerPeriode.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                selectedPeriod = periods[position]
                fetchSaldoData()
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        val saldos = arrayOf("gaji", "pajak", "pinjaman", "kas", "bpjs", "logistik")
        val saldoAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, saldos)
        saldoAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerSaldo.adapter = saldoAdapter

        binding.spinnerSaldo.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                selectedSaldo = saldos[position]
                fetchSaldoData()
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    private fun fetchSaldoData() {
        firestore.collection("saldo")
            .document(selectedSaldo)
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
                val intent = Intent(activity, ManageActivity::class.java)
                intent.putExtra("mode", 2)
                intent.putExtra("whichSaldo", selectedSaldo)
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
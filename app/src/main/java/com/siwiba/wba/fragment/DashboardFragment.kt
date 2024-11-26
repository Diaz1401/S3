package com.siwiba.wba.fragment

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.utils.ColorTemplate
import com.google.firebase.firestore.FirebaseFirestore
import com.siwiba.R
import com.siwiba.databinding.FragmentDashboardBinding
import com.siwiba.wba.SignInActivity
import com.siwiba.wba.model.Saldo
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!
    private var selectedPeriod: String = "Seminggu"
    private lateinit var firestore: FirebaseFirestore

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        firestore = FirebaseFirestore.getInstance()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        displayUserData()
        setupSpinners()

        binding.spinnerPeriode.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                selectedPeriod = parent.getItemAtPosition(position).toString()
                fetchAllSaldoData()
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        binding.imglogOut.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Sign Out")
                .setMessage("Apakah anda yakin untuk logout?")
                .setPositiveButton("Ya") { dialog, _ ->
                    val sharedPref = requireContext().getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
                    val editor = sharedPref.edit()
                    editor.clear()
                    editor.apply()
                    val intent = Intent(requireContext(), SignInActivity::class.java)
                    startActivity(intent)
                    requireActivity().finish()
                    dialog.dismiss()
                }
                .setNegativeButton("Tidak") { dialog, _ ->
                    dialog.dismiss()
                }
                .create()
                .show()
        }
    }

    private fun displayUserData() {
        val sharedPref = requireContext().getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        val name = sharedPref.getString("name", "Name not found")
        val email = sharedPref.getString("email", "Email not found")
        val address = sharedPref.getString("address", "Address not found")
        val uid = sharedPref.getString("uid", "UID not found")
        val profileImage = sharedPref.getString("profileImage", null)

        binding.txtName.text = name
        binding.txtEmail.text = email
        binding.txtAddress.text = address
        binding.txtId.text = uid

        if (profileImage != null) {
            val imageBytes = Base64.decode(profileImage, Base64.DEFAULT)
            val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
            binding.imgProfile.setImageBitmap(bitmap)
        }
    }

    private fun setupSpinners() {
        val periods = arrayOf("Perminggu", "Perbulan", "Pertahun")
        val periodAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, periods)
        periodAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerPeriode.adapter = periodAdapter

        binding.spinnerPeriode.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                selectedPeriod = periods[position]
                fetchAllSaldoData()
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    private fun fetchAllSaldoData() {
        fetchSaldoData("gaji", binding.lineChartGaji)
        fetchSaldoData("bpjs", binding.lineChartBpjs)
        fetchSaldoData("kas", binding.lineChartKas)
        fetchSaldoData("logistik", binding.lineChartLogistik)
        fetchSaldoData("pajak", binding.lineChartPajak)
        fetchSaldoData("pinjaman", binding.lineChartPinjaman)
    }

    private fun fetchSaldoData(which: String, chart: LineChart) {
        firestore.collection("saldo")
            .document(which)
            .collection("data")
            .get()
            .addOnSuccessListener { documents ->
                val saldoList = documents.toObjects(Saldo::class.java)
                val filteredList = filterDataByPeriod(saldoList, selectedPeriod)
                updateLineChart(filteredList, chart)
            }
            .addOnFailureListener { exception ->
                // Handle any errors
            }
    }

    private fun filterDataByPeriod(dataList: List<Saldo>, period: String): List<Saldo> {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val calendar = Calendar.getInstance()
        val groupedData = mutableMapOf<String, MutableList<Saldo>>()

        for (data in dataList) {
            val dataDate = dateFormat.parse(data.tanggal)
            if (dataDate != null) {
                calendar.time = dataDate
                val key = when (period) {
                    "Perminggu" -> {
                        val weekYear = calendar.get(Calendar.WEEK_OF_YEAR)
                        val year = calendar.get(Calendar.YEAR)
                        "$year-$weekYear"
                    }
                    "Perbulan" -> {
                        val month = calendar.get(Calendar.MONTH) + 1
                        val year = calendar.get(Calendar.YEAR)
                        "$year-$month"
                    }
                    "Pertahun" -> {
                        val year = calendar.get(Calendar.YEAR)
                        "$year"
                    }
                    else -> continue
                }
                groupedData.getOrPut(key) { mutableListOf() }.add(data)
            }
        }

        val aggregatedList = mutableListOf<Saldo>()
        for ((_, groupedSaldo) in groupedData) {
            val totalDebit = groupedSaldo.sumOf { it.debit }
            val totalKredit = groupedSaldo.sumOf { it.kredit }
            val totalSaldo = groupedSaldo.last().saldo // Assume the last saldo is relevant
            val latestTanggal = groupedSaldo.maxByOrNull { dateFormat.parse(it.tanggal)!! }?.tanggal ?: ""
            aggregatedList.add(
                Saldo(
                    no = 0,
                    keterangan = "Aggregated $period",
                    debit = totalDebit,
                    kredit = totalKredit,
                    saldo = totalSaldo,
                    tanggal = latestTanggal
                )
            )
        }

        return aggregatedList.sortedBy { dateFormat.parse(it.tanggal) }
    }

    private fun updateLineChart(dataList: List<Saldo>, chart: LineChart) {

        if (!isAdded) return // Fragment is not attached to activity

        val entries = ArrayList<Entry>()
        val dates = ArrayList<String>()

        for ((index, data) in dataList.withIndex()) {
            entries.add(Entry(index.toFloat(), data.saldo.toFloat()))
            dates.add(data.tanggal)
        }

        val dataSet = LineDataSet(entries, "Saldo Data")
        dataSet.setColors(*ColorTemplate.COLORFUL_COLORS)
        dataSet.setDrawFilled(true)
        dataSet.fillAlpha = 110
        dataSet.fillColor = ColorTemplate.getHoloBlue()
        dataSet.lineWidth = 2f
        dataSet.valueTextSize = 10f
        dataSet.mode = LineDataSet.Mode.CUBIC_BEZIER // Make the line smoother

        // Set gradient fill
        val drawable = ContextCompat.getDrawable(requireContext(), R.drawable.line_chart_gradient)
        dataSet.fillDrawable = drawable

        val lineData = LineData(dataSet)
        chart.data = lineData

        val xAxis: XAxis = chart.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.setDrawGridLines(false)
        xAxis.granularity = 1f
        xAxis.valueFormatter = IndexAxisValueFormatter(dates)

        val yAxisLeft: YAxis = chart.axisLeft
        yAxisLeft.setDrawGridLines(false)

        val yAxisRight: YAxis = chart.axisRight
        yAxisRight.setDrawGridLines(false)

        chart.invalidate() // Refresh chart
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onResume() {
        super.onResume()
        fetchAllSaldoData()
    }
}
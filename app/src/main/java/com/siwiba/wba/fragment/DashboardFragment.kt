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
import android.util.Log
import com.google.firebase.auth.FirebaseAuth

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!
    private var selectedPeriod: String = "Seminggu"
    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        firestore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        displayUserData()
        setupSpinners()

        binding.imglogOut.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Log Out")
                .setMessage("Apakah anda yakin untuk logout?")
                .setPositiveButton("Ya") { dialog, _ ->
                    val sharedPref = requireContext().getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
                    val editor = sharedPref.edit()
                    editor.clear()
                    editor.apply()
                    auth.signOut()
                    val intent = Intent(requireContext(), SignInActivity::class.java)
                    startActivity(intent)
                    requireActivity().finish()
                    dialog.dismiss()
                }
                .setNegativeButton("Tidak", null)
                .create()
                .show()
        }
    }

    private fun displayUserData() {
        val sharedPref = requireContext().getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        val name = sharedPref.getString("name", "Name not found")
        val email = sharedPref.getString("email", "Email not found")
        val address = sharedPref.getString("address", "Address not found")
        val profileImage = sharedPref.getString("profileImage", null)

        binding.txtName.text = name
        binding.txtEmail.text = email
        binding.txtAddress.text = address

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
        fetchSaldoData("utama", binding.lineChartUtama)
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
        val dateFormat = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
        val calendar = Calendar.getInstance()
        val groupedData = dataList.groupBy { data ->
            val dataDate = dateFormat.parse(data.tanggal)
            calendar.time = dataDate
            when (period) {
                "Perminggu" -> "${calendar.get(Calendar.YEAR)}-${calendar.get(Calendar.WEEK_OF_YEAR)}"
                "Perbulan" -> "${calendar.get(Calendar.YEAR)}-${calendar.get(Calendar.MONTH) + 1}"
                "Pertahun" -> "${calendar.get(Calendar.YEAR)}"
                else -> ""
            }
        }

        val aggregatedData = groupedData.map { (key, groupedSaldo) ->
            val totalDebit = groupedSaldo.sumOf { it.debit }
            val totalKredit = groupedSaldo.sumOf { it.kredit }
            val totalSaldo = totalDebit - totalKredit
            val latestTanggal = groupedSaldo.maxByOrNull { dateFormat.parse(it.tanggal)!! }?.tanggal ?: ""
            Saldo(
                no = 0,
                keterangan = "Aggregated $period",
                debit = totalDebit,
                kredit = totalKredit,
                saldo = totalSaldo,
                tanggal = latestTanggal
            )
        }.sortedBy { dateFormat.parse(it.tanggal) }

        return if (aggregatedData.size > 10) aggregatedData.takeLast(10) else aggregatedData
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
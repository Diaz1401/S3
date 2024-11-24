package com.siwiba.wba.fragment

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.fragment.app.Fragment
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.utils.ColorTemplate
import com.siwiba.R
import com.siwiba.databinding.FragmentDashboardBinding

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        displayUserData()
        setupPieChart()
        setupPieChart1()
        setupPieChart2()
        setupPieChart3()
        setupPieChart4()
        setupPieChart5()
        // Akses ImageButton dari View Binding
        val btnAbsen: ImageButton = binding.root.findViewById(R.id.btnAbsen)

        // Perbaiki Intent untuk berpindah ke Activity
        btnAbsen.setOnClickListener {
            val intent = Intent(requireContext(), AbsenFragment::class.java)
            startActivity(intent)
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

        if (profileImage != null) {
            val imageBytes = Base64.decode(profileImage, Base64.DEFAULT)
            val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
            binding.imgProfile.setImageBitmap(bitmap)
        }
    }

    private fun setupPieChart() {
        val pieChart = binding.chart // Asumsikan id dari PieChart adalah `chart` di layout XML

        // Data untuk grafik
        val entries = arrayListOf(
            PieEntry(80f, "Pemasukan"),
            PieEntry(90f, "Pengeluaran"),

        )

        val pieDataSet = PieDataSet(entries, "Subjects").apply {
            colors = ColorTemplate.MATERIAL_COLORS.toList()
        }

        val pieData = PieData(pieDataSet)
        pieChart.data = pieData

        pieChart.description.isEnabled = false
        pieChart.animateY(1000)
        pieChart.invalidate()
    }
    private fun setupPieChart1() {
        val pieChart = binding.chart1 // Asumsikan id dari PieChart adalah `chart` di layout XML

        // Data untuk grafik
        val entries = arrayListOf(
            PieEntry(80f, "Pemasukan"),
            PieEntry(90f, "Pengeluaran"),

        )

        val pieDataSet = PieDataSet(entries, "Subjects").apply {
            colors = ColorTemplate.MATERIAL_COLORS.toList()
        }

        val pieData = PieData(pieDataSet)
        pieChart.data = pieData

        pieChart.description.isEnabled = false
        pieChart.animateY(1000)
        pieChart.invalidate()
    }
    private fun setupPieChart2() {
        val pieChart = binding.chart2 // Asumsikan id dari PieChart adalah `chart` di layout XML

        // Data untuk grafik
        val entries = arrayListOf(
            PieEntry(80f, "Pemasukan"),
            PieEntry(90f, "Pengeluaran"),

            )

        val pieDataSet = PieDataSet(entries, "Subjects").apply {
            colors = ColorTemplate.MATERIAL_COLORS.toList()
        }

        val pieData = PieData(pieDataSet)
        pieChart.data = pieData

        pieChart.description.isEnabled = false
        pieChart.animateY(1000)
        pieChart.invalidate()
    }
    private fun setupPieChart3() {
        val pieChart = binding.chart3 // Asumsikan id dari PieChart adalah `chart` di layout XML

        // Data untuk grafik
        val entries = arrayListOf(
            PieEntry(80f, "Pemasukan"),
            PieEntry(90f, "Pengeluaran"),

            )

        val pieDataSet = PieDataSet(entries, "Subjects").apply {
            colors = ColorTemplate.MATERIAL_COLORS.toList()
        }

        val pieData = PieData(pieDataSet)
        pieChart.data = pieData

        pieChart.description.isEnabled = false
        pieChart.animateY(1000)
        pieChart.invalidate()
    }
    private fun setupPieChart4() {
        val pieChart = binding.chart4 // Asumsikan id dari PieChart adalah `chart` di layout XML

        // Data untuk grafik
        val entries = arrayListOf(
            PieEntry(80f, "Pemasukan"),
            PieEntry(90f, "Pengeluaran"),

            )

        val pieDataSet = PieDataSet(entries, "Subjects").apply {
            colors = ColorTemplate.MATERIAL_COLORS.toList()
        }

        val pieData = PieData(pieDataSet)
        pieChart.data = pieData

        pieChart.description.isEnabled = false
        pieChart.animateY(1000)
        pieChart.invalidate()
    }
    private fun setupPieChart5() {
        val pieChart = binding.chart5 // Asumsikan id dari PieChart adalah `chart` di layout XML

        // Data untuk grafik
        val entries = arrayListOf(
            PieEntry(80f, "Pemasukan"),
            PieEntry(90f, "Pengeluaran"),

            )

        val pieDataSet = PieDataSet(entries, "Subjects").apply {
            colors = ColorTemplate.MATERIAL_COLORS.toList()
        }

        val pieData = PieData(pieDataSet)
        pieChart.data = pieData

        pieChart.description.isEnabled = false
        pieChart.animateY(1000)
        pieChart.invalidate()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

package com.siwiba
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.utils.ColorTemplate
import com.google.firebase.FirebaseApp
import com.siwiba.wba.SignInActivity
import com.siwiba.R


class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        FirebaseApp.initializeApp(this)

        val btnkas = findViewById<ImageButton>(R.id.btnkas)
        val btnbpjs = findViewById<ImageButton>(R.id.btnbpjs)
        val btnkirimuang = findViewById<ImageButton>(R.id.btnkirimuang)
        val btnlogistik = findViewById<ImageButton>(R.id.btnlogistik)
        val btngaji = findViewById<ImageButton>(R.id.btngaji)
        val btnpinjaman = findViewById<ImageButton>(R.id.btnpinjaman)
        val btndaftarabsen = findViewById<ImageButton>(R.id.btndaftarabsen)
        val btnanalisis = findViewById<ImageButton>(R.id.btnanalisis)
        val btndaftarakun = findViewById<ImageButton>(R.id.btndaftarakun)
        val btnpajak = findViewById<ImageButton>(R.id.btnpajak)

        btnkas.setOnClickListener {
            val intent = Intent(this, SignInActivity::class.java)
            startActivity(intent)
        }
        btnbpjs.setOnClickListener {
            val intent = Intent(this, SignInActivity::class.java)
            startActivity(intent)
        }
        btnkirimuang.setOnClickListener {
            val intent = Intent(this, KirimUang::class.java)
            startActivity(intent)
        }
        btnlogistik.setOnClickListener {
            val intent = Intent(this, SignInActivity::class.java)
            startActivity(intent)
        }
        btngaji.setOnClickListener {
            val intent = Intent(this, SignInActivity::class.java)
            startActivity(intent)
        }
        btnpinjaman.setOnClickListener {
            val intent = Intent(this, SignInActivity::class.java)
            startActivity(intent)
        }
        btndaftarabsen.setOnClickListener {
            val intent = Intent(this, DaftarAbsen::class.java)
            startActivity(intent)
        }
        btnanalisis.setOnClickListener {
            val intent = Intent(this, SignInActivity::class.java)
            startActivity(intent)
        }
        btndaftarakun.setOnClickListener {
            val intent = Intent(this, DaftarAkun::class.java)
            startActivity(intent)
        }
        btnpajak.setOnClickListener {
            val intent = Intent(this, SignInActivity::class.java)
            startActivity(intent)
        }
        val barChart: BarChart = findViewById(R.id.chart)
        barChart.getAxisRight().setDrawLabels(false)

        val entries = ArrayList<BarEntry>()
        entries.add(BarEntry(0f, 40000000f)) // X: 0, Y: 45
        entries.add(BarEntry(1f, 35000000f)) // X: 1, Y: 80
        entries.add(BarEntry(2f, 45000000f)) // X: 2, Y: 65
        entries.add(BarEntry(3f, 42000000f)) // X: 3, Y: 38

        val yAxis: YAxis = barChart.axisLeft
        yAxis.axisMinimum = 0f // Mulai dari 0
        yAxis.axisMaximum = 100000000f// Atur sesuai dengan nilai tertinggi yang Anda inginkan
        yAxis.axisLineWidth = 2f
        yAxis.axisLineColor = Color.BLACK
        yAxis.labelCount = 10 // Atur jumlah label pada sumbu Y

        val dataSet = BarDataSet(entries, "Subjects").apply {
            colors = ColorTemplate.MATERIAL_COLORS.toList()
        }

        val barData: BarData = BarData(dataSet)
        barChart.setData(barData)

        barChart.getDescription().setEnabled(false)
        barChart.invalidate()
        val xValues = arrayOf("Subject 1", "Subject 2", "Subject 3", "Subject 4")
        barChart.getXAxis().setValueFormatter(IndexAxisValueFormatter(xValues))
        barChart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM)
        barChart.getXAxis().setGranularity(1f)
        barChart.getXAxis().setGranularityEnabled(true)
    }
}
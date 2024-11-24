package com.siwiba.wba.fragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.siwiba.databinding.FragmentKeuanganBinding
import com.siwiba.wba.activity.GajiActivity
import com.siwiba.wba.activity.PajakActivity
import com.siwiba.wba.activity.PinjamanActivity
import com.siwiba.wba.activity.KasActivity
import com.siwiba.wba.activity.BpjsActivity
import com.siwiba.wba.activity.LogistikActivity
import com.siwiba.wba.model.Saldo
import com.siwiba.wba.model.Pajak
import com.dewakoding.androiddatatable.data.Column

class KeuanganFragment : Fragment() {

    private var _binding: FragmentKeuanganBinding? = null
    private val binding get() = _binding!!
    private lateinit var firestore: FirebaseFirestore

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentKeuanganBinding.inflate(inflater, container, false)
        firestore = FirebaseFirestore.getInstance()
        return binding.root
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

//        fetchGajiData()
    }

//    private fun fetchGajiData() {
//        val columns = arrayListOf(
//            Column("no", "No."),
//            Column("bagian", "Bagian"),
//            Column("jumlah_pengeluaran", "Jumlah Pengeluaran")
//        )
//
//        fetchData("gaji", Saldo::class.java) { gajiList ->
//            populateDataTable(gajiList, columns) { gaji, index ->
//                mapOf(
//                    "no" to (index + 1).toString(),
//                    "bagian" to "Gaji",
//                    "jumlah_pengeluaran" to gaji.gaji
//                )
//            }
//        }
//    }

    private fun fetchPajakData() {
        val columns = arrayListOf(
            Column("no", "No."),
            Column("bagian", "Bagian"),
            Column("jumlah_pengeluaran", "Jumlah Pengeluaran")
        )

        fetchData("pajak", Pajak::class.java) { pajakList ->
            populateDataTable(pajakList, columns) { pajak, index ->
                mapOf(
                    "no" to (index + 1).toString(),
                    "bagian" to "Pajak",
                    "jumlah_pengeluaran" to pajak.nominal
                )
            }
        }
    }

    private fun <T> fetchData(collection: String, clazz: Class<T>, onSuccess: (List<T>) -> Unit) {
        firestore.collection(collection)
            .orderBy("no", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { documents ->
                val dataList = mutableListOf<T>()
                for (document in documents) {
                    val data = document.toObject(clazz)
                    dataList.add(data)
                }
                onSuccess(dataList)
            }
            .addOnFailureListener { exception ->
                // Handle the error
            }
    }

    private fun <T> populateDataTable(dataList: List<T>, columns: java.util.ArrayList<Column>, rowMapper: (T, Int) -> Map<String, String>) {
        // Clear existing views
        binding.dataTable.removeAllViews()

        // Convert dataList to a format suitable for the DataTableView
        val rows = dataList.mapIndexed { index, data ->
            rowMapper(data, index)
        }

        binding.dataTable.setTable(columns, rows, isActionButtonShow = false)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
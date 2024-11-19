package com.siwiba.wba.fragment

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.location.Location
import android.os.Bundle
import android.provider.MediaStore
import android.util.DisplayMetrics
import android.view.*
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.siwiba.R
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.*

class AbsenFragment : Fragment() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var tvTanggalWaktu: TextView
    private lateinit var btnKamera: ImageButton
    private lateinit var btnAbsen: ImageButton

    private var imageBitmap: Bitmap? = null

    private val lokasiKantor = Location("").apply {
        latitude = -7.460280112797017 // Latitude lokasi kantor
        longitude = 112.70801758822674 // Longitude lokasi kantor
    }
    private val MAX_DISTANCE_METERS = 50.0

    private val cameraLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data = result.data
                imageBitmap = data?.extras?.get("data") as? Bitmap
                imageBitmap?.let {
                    btnKamera.setImageBitmap(it)
                }
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_absen, container, false)

        tvTanggalWaktu = view.findViewById(R.id.tvTanggalWaktu)
        btnKamera = view.findViewById(R.id.btnkamera)
        btnAbsen = view.findViewById(R.id.btnAbsen)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext())

        updateTanggalWaktu()

        btnKamera.setOnClickListener { ambilFoto() }
        btnAbsen.setOnClickListener { tampilkanDialogNama() }

        return view
    }

    private fun updateTanggalWaktu() {
        val dateFormat = SimpleDateFormat("EEEE, dd MMM yyyy HH:mm", Locale.getDefault())
        val currentDateTime = dateFormat.format(Date())
        tvTanggalWaktu.text = currentDateTime
    }

    private fun ambilFoto() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        cameraLauncher.launch(intent)
    }

    private fun cekLokasiDanAbsen(nama: String) {
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                REQUEST_LOCATION
            )
            return
        }

        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                val distance = location.distanceTo(lokasiKantor)
                if (distance <= MAX_DISTANCE_METERS) {
                    simpanDataAbsen(nama)
                } else {
                    resetAbsen()
                    tampilkanToast("Absen gagal! Anda terlalu jauh dari lokasi absen.")
                }
            } else {
                resetAbsen()
                tampilkanToast("Gagal mendapatkan lokasi!")
            }
        }
    }

    private fun simpanDataAbsen(nama: String) {
        if (imageBitmap != null) {
            val baos = ByteArrayOutputStream()
            imageBitmap?.compress(Bitmap.CompressFormat.JPEG, 100, baos)
            val imageData = baos.toByteArray()

            val storageRef = FirebaseStorage.getInstance().reference
                .child("absen_images/${UUID.randomUUID()}.jpg")

            storageRef.putBytes(imageData)
                .addOnSuccessListener {
                    storageRef.downloadUrl.addOnSuccessListener { uri ->
                        val imageUrl = uri.toString()
                        val absenData = mapOf(
                            "nama" to nama,
                            "tanggal" to tvTanggalWaktu.text.toString(),
                            "imageUrl" to imageUrl
                        )

                        // Simpan ke Firestore
                        val firestore = FirebaseFirestore.getInstance()
                        firestore.collection("absen")
                            .add(absenData)
                            .addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    resetAbsen()
                                    tampilkanToast("Absen berhasil disimpan.")
                                } else {
                                    tampilkanToast("Gagal menyimpan data absensi ke Firestore.")
                                }
                            }
                    }
                }
                .addOnFailureListener {
                    tampilkanToast("Gagal mengunggah gambar ke Firebase Storage.")
                }
        } else {
            tampilkanToast("Harap ambil foto sebelum absen.")
        }
    }

    private fun resetAbsen() {
        imageBitmap = null
        btnKamera.setImageDrawable(null)
    }

    private fun tampilkanToast(pesan: String) {
        Toast.makeText(requireContext(), pesan, Toast.LENGTH_SHORT).apply {
            setGravity(Gravity.CENTER, 0, -200)
            show()
        }
    }

    private fun tampilkanDialogNama() {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Isi Nama")

        val input = EditText(requireContext())
        input.hint = "Masukkan nama Anda"
        builder.setView(input)

        builder.setPositiveButton("OK") { dialog, _ ->
            val nama = input.text.toString()
            if (nama.isNotEmpty()) {
                cekLokasiDanAbsen(nama)
                dialog.dismiss()
            } else {
                tampilkanToast("Nama tidak boleh kosong!")
            }
        }

        builder.setNegativeButton("Batal") { dialog, _ -> dialog.cancel() }
        builder.show()
    }

    companion object {
        private const val REQUEST_LOCATION = 101
    }
}
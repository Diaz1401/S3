package com.siwiba.util

import android.app.AlertDialog
import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.widget.Toast
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.opencsv.CSVReader
import com.opencsv.CSVWriter
import com.siwiba.wba.model.Saldo
import java.io.InputStreamReader
import java.io.OutputStreamWriter

/**
 * Class for exporting and importing CSV data related to Saldo.
 *
 * @param whichSaldo The specific saldo to be used.
 * @param firestoreSaldo The Firestore collection name for saldo.
 * @param context The context of the application.
 * @param isUtama, whether the saldo is utama or not.
 */
class CsvExportImport(private val whichSaldo: String, private val firestoreSaldo: String, private val context: Context, private val isUtama: Boolean) {
    private val firestore = FirebaseFirestore.getInstance()
    private val contentResolver: ContentResolver = context.contentResolver

    /**
     * Fetches saldo data from Firestore and returns it as a Task.
     *
     * @return Task containing a list of Saldo objects.
     */
    fun fetchCsvSaldoData(): Task<List<Saldo>> {
        return firestore.collection(firestoreSaldo)
            .document(whichSaldo)
            .collection("data")
            .get()
            .continueWith { task ->
                if (task.isSuccessful) {
                    task.result?.toObjects(Saldo::class.java) ?: emptyList()
                } else {
                    emptyList()
                }
            }
    }

    /**
     * Exports the provided saldo data to a CSV file at the given URI.
     *
     * @param data The list of Saldo objects to be exported.
     * @param uri The URI where the CSV file will be saved.
     */
    fun exportDataToCSV(data: List<Saldo>, uri: Uri) {
        contentResolver.openOutputStream(uri)?.use { outputStream ->
            val writer = CSVWriter(OutputStreamWriter(outputStream))
            writer.writeNext(arrayOf("No", "Keterangan", "Debit", "Kredit", "Saldo", "Editor", "Tanggal"))
            for (saldo in data) {
                writer.writeNext(arrayOf(saldo.no.toString(), saldo.keterangan, saldo.debit.toString(), saldo.kredit.toString(), saldo.saldo.toString(), saldo.editor, saldo.tanggal))
            }
            writer.close()
        }
    }

    /**
     * Imports saldo data from a CSV file at the given URI and updates Firestore.
     *
     * @param uri The URI of the CSV file to be imported.
     */
    fun importDataFromCSV(uri: Uri) {
        val importedData = mutableListOf<Saldo>()

        contentResolver.openInputStream(uri)?.use { inputStream ->
            val reader = CSVReader(InputStreamReader(inputStream))
            reader.readNext() // Skip header
            var nextLine: Array<String>?
            while (reader.readNext().also { nextLine = it } != null) {
                val saldo = Saldo(
                    no = nextLine!![0].toLong(),
                    keterangan = nextLine!![1],
                    debit = nextLine!![2].toLong(),
                    kredit = nextLine!![3].toLong(),
                    saldo = 0,
                    editor = nextLine!![5],
                    tanggal = nextLine!![6]
                )
                // check prevent older data than previous data to be added
                if (importedData.isNotEmpty() && saldo.tanggal < importedData[0].tanggal) {
                    // csv, saldo: Prevent older data than previous data to be added
                    Toast.makeText(context, "Data CSV tidak valid", Toast.LENGTH_SHORT).show()
                    Toast.makeText(context, "Tanggal tidak boleh lebih tua dari tanggal terakhir", Toast.LENGTH_SHORT).show()
                    return
                }
                importedData.add(saldo)
            }
            reader.close()
        }

        firestore.collection(firestoreSaldo)
            .document("utama")
            .collection("data")
            .orderBy("no", Query.Direction.DESCENDING)
            .limit(1)
            .get()
            .addOnSuccessListener { document ->
                var lastSaldoUtama = 0L
                var newNoUtama = 1L
                if (!document.isEmpty) {
                    lastSaldoUtama = document.documents[0].getLong("saldo") ?: 0
                    newNoUtama = document.documents[0].getLong("no") ?: 1
                    newNoUtama++
                }
                fbSaldo(importedData, lastSaldoUtama, newNoUtama)
            }
            .addOnFailureListener {
                Toast.makeText(context, "Gagal mengambil saldo utama terakhir", Toast.LENGTH_SHORT).show()
            }
    }

    /**
     * Updates Firestore with the imported saldo data.
     *
     * @param importedData The list of imported Saldo objects.
     * @param lastSaldoUtama The last saldo value of the main saldo.
     * @param newNoUtama The new number for the main saldo.
     */
    private fun fbSaldo(importedData: List<Saldo>, lastSaldoUtama: Long, newNoUtama: Long) {
        var lastSaldoUtamaS = lastSaldoUtama
        var newNoUtamaS = newNoUtama
        firestore.collection(firestoreSaldo)
            .document(whichSaldo)
            .collection("data")
            .orderBy("no", Query.Direction.DESCENDING)
            .limit(1)
            .get()
            .addOnSuccessListener { documents ->
                var lastSaldo = 0L
                var newNo = 1L
                if (!documents.isEmpty) {
                    lastSaldo = documents.documents[0].getLong("saldo") ?: 0
                    newNo = documents.documents[0].getLong("no") ?: 1
                    newNo++
                }

                val batch = firestore.batch()
                val collectionRef = firestore.collection(firestoreSaldo)
                    .document(whichSaldo)
                    .collection("data")

                for (saldoItem in importedData) {
                    if (saldoItem.debit > lastSaldoUtama && !isUtama) {
                        Toast.makeText(context, "Penambahan debit tidak boleh lebih besar dari saldo utama", Toast.LENGTH_SHORT).show()
                        return@addOnSuccessListener
                    }
                    if (saldoItem.kredit > lastSaldo) {
                        Toast.makeText(context, "Kredit tidak boleh lebih besar dari saldo $whichSaldo", Toast.LENGTH_SHORT).show()
                        return@addOnSuccessListener
                    }
                    lastSaldo += saldoItem.debit - saldoItem.kredit
                    val datas = mapOf(
                        "no" to newNo,
                        "keterangan" to saldoItem.keterangan,
                        "debit" to saldoItem.debit,
                        "kredit" to saldoItem.kredit,
                        "saldo" to lastSaldo,
                        "editor" to saldoItem.editor,
                        "tanggal" to saldoItem.tanggal
                    )
                    val docRef = collectionRef.document(newNo.toString())
                    batch.set(docRef, datas)
                    newNo++

                    if (saldoItem.debit > 0 && whichSaldo != "utama") {
                        lastSaldoUtamaS -= saldoItem.debit
                        val dataUtama = mapOf(
                            "no" to newNoUtama,
                            "keterangan" to "Kredit saldo utama ke saldo $whichSaldo",
                            "debit" to 0,
                            "kredit" to saldoItem.debit,
                            "saldo" to lastSaldoUtama,
                            "editor" to saldoItem.editor,
                            "tanggal" to saldoItem.tanggal
                        )
                        val docRefUtama = firestore.collection(firestoreSaldo)
                            .document("utama")
                            .collection("data")
                            .document(newNoUtama.toString())
                        batch.set(docRefUtama, dataUtama)
                        newNoUtamaS++
                    }
                }

                AlertDialog.Builder(context)
                    .setTitle("Konfirmasi")
                    .setMessage("Apakah Anda yakin ingin mengimport data?\nData yang diimport akan melanjutkan data terakhir dari saldo $whichSaldo.")
                    .setPositiveButton("Ya") { _, _ ->
                        batch.commit().addOnSuccessListener {
                            Toast.makeText(context, "Berhasil mengimport data", Toast.LENGTH_SHORT).show()
                        }.addOnFailureListener {
                            Toast.makeText(context, "Gagal mengimport data", Toast.LENGTH_SHORT).show()
                        }
                    }
                    .setNegativeButton("Tidak", null)
                    .show()
            }
            .addOnFailureListener {
                Toast.makeText(context, "Gagal mengambil saldo $whichSaldo terakhir", Toast.LENGTH_SHORT).show()
            }
    }
}
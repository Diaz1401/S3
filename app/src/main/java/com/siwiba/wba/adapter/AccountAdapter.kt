package com.siwiba.wba.adapter

import android.app.Activity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.siwiba.R
import com.siwiba.wba.model.Account
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.CheckBox
import android.widget.Spinner
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.siwiba.wba.SignInActivity

class AccountAdapter(
    private var accounts: List<Account>,
    private val listener: OnAccountClickListener,
    private val context: Context
) : RecyclerView.Adapter<AccountAdapter.AccountViewHolder>() {

    private var filteredAccounts: List<Account> = accounts
    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private var isCancel = false

    interface OnAccountClickListener {
        fun onEditClick(account: Account)
        fun onDeleteClick(account: Account)
        fun onMakeAdminClick(account: Account)
    }

    inner class AccountViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val txtName: TextView = itemView.findViewById(R.id.txtName)
        val txtEmail: TextView = itemView.findViewById(R.id.txtEmail)
        val layoutOptions: LinearLayout = itemView.findViewById(R.id.layoutOptions)
        val layoutShowOptions: LinearLayout = itemView.findViewById(R.id.layoutShowOptions)
        val layoutDelete: LinearLayout = itemView.findViewById(R.id.layoutDelete)
        val spinnerJabatan: Spinner = itemView.findViewById(R.id.spinnerJabatan)
        val spinnerScope: Spinner = itemView.findViewById(R.id.spinnerScope)
        val cbGaji: CheckBox = itemView.findViewById(R.id.cbGaji)
        val cbPajak: CheckBox = itemView.findViewById(R.id.cbPajak)
        val cbPinjaman: CheckBox = itemView.findViewById(R.id.cbPinjaman)
        val cbKas: CheckBox = itemView.findViewById(R.id.cbKas)
        val cbLogistik: CheckBox = itemView.findViewById(R.id.cbLogistik)
        val cbBpjs: CheckBox = itemView.findViewById(R.id.cbBpjs)
        val layoutScope: LinearLayout = itemView.findViewById(R.id.layoutScope)
        val txtScope: TextView = itemView.findViewById(R.id.txtScope)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AccountViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_account, parent, false)
        return AccountViewHolder(view)
    }

    override fun onBindViewHolder(holder: AccountViewHolder, position: Int) {
        val account = filteredAccounts[position]
        val context = holder.itemView.context

        // Setup the spinner jabatan
        val jabatanArray = arrayOf("Direktur", "Direktur Operasional", "General Manager", "Karyawan")
        val adapterJabatan = ArrayAdapter(context, R.layout.item_spinner_black, jabatanArray)
        adapterJabatan.setDropDownViewResource(R.layout.simple_spinner_dropdown_item)
        holder.spinnerJabatan.adapter = adapterJabatan
        holder.spinnerJabatan.setSelection(account.jabatan - 1, false)

        // Setup spinner scopeMode
        val scopeModeArray = arrayOf("WBA", "KWI", "WBA & KWI")
        val adapterScopeMode = ArrayAdapter(context, R.layout.item_spinner_black, scopeModeArray)
        adapterScopeMode.setDropDownViewResource(R.layout.simple_spinner_dropdown_item)
        holder.spinnerScope.adapter = adapterScopeMode
        holder.spinnerScope.setSelection(account.scopeMode, false)

        // Setup the checkboxes
        holder.cbGaji.isChecked = account.scopeGaji
        holder.cbPajak.isChecked = account.scopePajak
        holder.cbPinjaman.isChecked = account.scopePinjaman
        holder.cbKas.isChecked = account.scopeKas
        holder.cbLogistik.isChecked = account.scopeLogistik
        holder.cbBpjs.isChecked = account.scopeBpjs

        holder.txtName.text = account.name
        holder.txtEmail.text = account.email

        firestore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        holder.layoutShowOptions.setOnClickListener {
            holder.layoutOptions.visibility = if (holder.layoutOptions.visibility == View.VISIBLE) View.GONE else View.VISIBLE
            holder.txtScope.visibility = if (holder.txtScope.visibility == View.VISIBLE) View.GONE else View.VISIBLE
            holder.layoutScope.visibility = if (holder.layoutScope.visibility == View.VISIBLE) View.GONE else View.VISIBLE
        }

        holder.layoutDelete.setOnClickListener {
            listener.onDeleteClick(account)
            AlertDialog.Builder(context)
                .setTitle("Delete Account")
                .setMessage("Are you sure you want to delete this account?")
                .setPositiveButton("Yes") { dialog, _ ->
                    var email = account.email
                    var password = account.password
                    auth.signInWithEmailAndPassword(email, password)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                Toast.makeText(context, "Menghapus akun ${account.name}", Toast.LENGTH_SHORT).show()
                                val user = auth.currentUser!!
                                firestore.collection("users").document(account.id).delete()
                                    .continueWithTask { deleteTask ->
                                        if (deleteTask.isSuccessful) {
                                            user.delete()
                                        } else {
                                            throw deleteTask.exception ?: Exception("Failed to delete Firestore document")
                                        }
                                    }
                                    .addOnCompleteListener { deleteUserTask ->
                                        if (deleteUserTask.isSuccessful) {
                                            Toast.makeText(context, "Berhasil menghapus akun ${account.name}", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(context, "Gagal menghapus akun ${account.name}: ${deleteUserTask.exception?.message}", Toast.LENGTH_SHORT).show()
                                        }
                                        auth.signOut()
                                        // clear shared preferences
                                        val sharedPref = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
                                        email = sharedPref.getString("email", "") ?: ""
                                        password = sharedPref.getString("password", "") ?: ""
                                        auth.signInWithEmailAndPassword(email, password)
                                            .addOnCompleteListener { reauthTask ->
                                                if (reauthTask.isSuccessful) {
                                                    Toast.makeText(context, "Berhasil masuk kembali", Toast.LENGTH_SHORT).show()
                                                } else {
                                                    Toast.makeText(context, "Gagal masuk ke akun ${account.name}: ${reauthTask.exception?.message}", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                    }
                            } else {
                                Toast.makeText(context, "Gagal masuk ke akun ${account.name}: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    dialog.dismiss()
                }
                .setNegativeButton("No") { dialog, _ ->
                    dialog.dismiss()
                }
                .create()
                .show()
        }
        holder.spinnerJabatan.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (isCancel) {
                    isCancel = false
                    return
                }
                listener.onMakeAdminClick(account)
                AlertDialog.Builder(context)
                    .setTitle("Ubah Jabatan")
                    .setMessage("Apakah anda yakin ingin mengubah jabatan?")
                    .setPositiveButton("Ya") { dialog, _ ->
                        val correctedPosition = position + 1
                        account.jabatan = correctedPosition
                        firestore.collection("users").document(account.id).update(
                            mapOf(
                                "jabatan" to correctedPosition,
                                "isAdmin" to (correctedPosition < 4)
                            )
                        ).addOnSuccessListener {
                            Toast.makeText(context, "Berhasil mengubah jabatan", Toast.LENGTH_SHORT).show()
                        }.addOnFailureListener { exception ->
                            Toast.makeText(context, "Gagal mengubah jabatan: ${exception.message}", Toast.LENGTH_SHORT).show()
                        }
                        dialog.dismiss()
                    }
                    .setNegativeButton("Tidak") { dialog, _ ->
                        isCancel = true
                        holder.spinnerJabatan.setSelection(account.jabatan - 1, false)
                        dialog.dismiss()
                    }
                    .create()
                    .show()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Do nothing
            }
        }
        holder.spinnerScope.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (isCancel) {
                    isCancel = false
                    return
                }
                listener.onMakeAdminClick(account)
                AlertDialog.Builder(context)
                    .setTitle("Ubah Scope Saldo")
                    .setMessage("Apakah anda yakin ingin mengubah scope saldo?")
                    .setPositiveButton("Ya") { dialog, _ ->
                        account.scopeMode = position
                        firestore.collection("users").document(account.id).update("scopeMode", position)
                            .addOnSuccessListener {
                                Toast.makeText(context, "Berhasil mengubah scope mode", Toast.LENGTH_SHORT).show()
                            }.addOnFailureListener { exception ->
                                Toast.makeText(context, "Gagal mengubah scope mode: ${exception.message}", Toast.LENGTH_SHORT).show()
                            }
                        dialog.dismiss()
                    }
                    .setNegativeButton("Tidak") { dialog, _ ->
                        isCancel = true
                        holder.spinnerScope.setSelection(account.scopeMode, false)
                        dialog.dismiss()
                    }
                    .create()
                    .show()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Do nothing
            }
        }
        holder.cbGaji.setOnClickListener {
            listener.onMakeAdminClick(account)
            AlertDialog.Builder(context)
                .setTitle("Ubah Scope Gaji")
                .setMessage("Apakah anda yakin ingin mengubah scope gaji?")
                .setPositiveButton("Ya") { dialog, _ ->
                    account.scopeGaji = holder.cbGaji.isChecked
                    firestore.collection("users").document(account.id).update("scopeGaji", holder.cbGaji.isChecked)
                        .addOnSuccessListener {
                            Toast.makeText(context, "Berhasil mengubah scope gaji", Toast.LENGTH_SHORT).show()
                        }.addOnFailureListener { exception ->
                            Toast.makeText(context, "Gagal mengubah scope gaji: ${exception.message}", Toast.LENGTH_SHORT).show()
                        }
                    dialog.dismiss()
                }
                .setNegativeButton("Tidak") { dialog, _ ->
                    holder.cbGaji.isChecked = !holder.cbGaji.isChecked
                    dialog.dismiss()
                }
                .create()
                .show()
        }
        holder.cbPajak.setOnClickListener {
            listener.onMakeAdminClick(account)
            AlertDialog.Builder(context)
                .setTitle("Ubah Scope Pajak")
                .setMessage("Apakah anda yakin ingin mengubah scope pajak?")
                .setPositiveButton("Ya") { dialog, _ ->
                    account.scopePajak = holder.cbPajak.isChecked
                    firestore.collection("users").document(account.id).update("scopePajak", holder.cbPajak.isChecked)
                        .addOnSuccessListener {
                            Toast.makeText(context, "Berhasil mengubah scope pajak", Toast.LENGTH_SHORT).show()
                        }.addOnFailureListener { exception ->
                            Toast.makeText(context, "Gagal mengubah scope pajak: ${exception.message}", Toast.LENGTH_SHORT).show()
                        }
                    dialog.dismiss()
                }
                .setNegativeButton("Tidak") { dialog, _ ->
                    holder.cbPajak.isChecked = !holder.cbPajak.isChecked
                    dialog.dismiss()
                }
                .create()
                .show()
        }
        holder.cbPinjaman.setOnClickListener {
            listener.onMakeAdminClick(account)
            AlertDialog.Builder(context)
                .setTitle("Ubah Scope Pinjaman")
                .setMessage("Apakah anda yakin ingin mengubah scope pinjaman?")
                .setPositiveButton("Ya") { dialog, _ ->
                    account.scopePinjaman = holder.cbPinjaman.isChecked
                    firestore.collection("users").document(account.id).update("scopePinjaman", holder.cbPinjaman.isChecked)
                        .addOnSuccessListener {
                            Toast.makeText(context, "Berhasil mengubah scope pinjaman", Toast.LENGTH_SHORT).show()
                        }.addOnFailureListener { exception ->
                            Toast.makeText(context, "Gagal mengubah scope pinjaman: ${exception.message}", Toast.LENGTH_SHORT).show()
                        }
                    dialog.dismiss()
                }
                .setNegativeButton("Tidak") { dialog, _ ->
                    holder.cbPinjaman.isChecked = !holder.cbPinjaman.isChecked
                    dialog.dismiss()
                }
                .create()
                .show()
        }
        holder.cbKas.setOnClickListener {
            listener.onMakeAdminClick(account)
            AlertDialog.Builder(context)
                .setTitle("Ubah Scope Kas")
                .setMessage("Apakah anda yakin ingin mengubah scope kas?")
                .setPositiveButton("Ya") { dialog, _ ->
                    account.scopeKas = holder.cbKas.isChecked
                    firestore.collection("users").document(account.id).update("scopeKas", holder.cbKas.isChecked)
                        .addOnSuccessListener {
                            Toast.makeText(context, "Berhasil mengubah scope kas", Toast.LENGTH_SHORT).show()
                        }.addOnFailureListener { exception ->
                            Toast.makeText(context, "Gagal mengubah scope kas: ${exception.message}", Toast.LENGTH_SHORT).show()
                        }
                    dialog.dismiss()
                }
                .setNegativeButton("Tidak") { dialog, _ ->
                    holder.cbKas.isChecked = !holder.cbKas.isChecked
                    dialog.dismiss()
                }
                .create()
                .show()
        }
        holder.cbLogistik.setOnClickListener {
            listener.onMakeAdminClick(account)
            AlertDialog.Builder(context)
                .setTitle("Ubah Scope Logistik")
                .setMessage("Apakah anda yakin ingin mengubah scope logistik?")
                .setPositiveButton("Ya") { dialog, _ ->
                    account.scopeLogistik = holder.cbLogistik.isChecked
                    firestore.collection("users").document(account.id)
                        .update("scopeLogistik", holder.cbLogistik.isChecked)
                        .addOnSuccessListener {
                            Toast.makeText(
                                context,
                                "Berhasil mengubah scope logistik",
                                Toast.LENGTH_SHORT
                            ).show()
                        }.addOnFailureListener { exception ->
                            Toast.makeText(
                                context,
                                "Gagal mengubah scope logistik: ${exception.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    dialog.dismiss()
                }
                .setNegativeButton("Tidak") { dialog, _ ->
                    holder.cbLogistik.isChecked = !holder.cbLogistik.isChecked
                    dialog.dismiss()
                }
                .create()
                .show()
        }
        holder.cbBpjs.setOnClickListener {
            listener.onMakeAdminClick(account)
            AlertDialog.Builder(context)
                .setTitle("Ubah Scope BPJS")
                .setMessage("Apakah anda yakin ingin mengubah scope BPJS?")
                .setPositiveButton("Ya") { dialog, _ ->
                    account.scopeBpjs = holder.cbBpjs.isChecked
                    firestore.collection("users").document(account.id).update("scopeBpjs", holder.cbBpjs.isChecked)
                        .addOnSuccessListener {
                            Toast.makeText(context, "Berhasil mengubah scope BPJS", Toast.LENGTH_SHORT).show()
                        }.addOnFailureListener { exception ->
                            Toast.makeText(context, "Gagal mengubah scope BPJS: ${exception.message}", Toast.LENGTH_SHORT).show()
                        }
                    dialog.dismiss()
                }
                .setNegativeButton("Tidak") { dialog, _ ->
                    holder.cbBpjs.isChecked = !holder.cbBpjs.isChecked
                    dialog.dismiss()
                }
                .create()
                .show()
        }
    }

    override fun getItemCount(): Int = filteredAccounts.size

    fun filter(query: String) {
        filteredAccounts = if (query.isEmpty()) {
            accounts
        } else {
            accounts.filter {
                it.name.contains(query, ignoreCase = true) || it.email.contains(query, ignoreCase = true)
            }
        }
        notifyDataSetChanged()
    }
}
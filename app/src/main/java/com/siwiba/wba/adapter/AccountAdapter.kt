package com.siwiba.wba.adapter

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
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class AccountAdapter(private var accounts: List<Account>, private val listener: OnAccountClickListener) :
    RecyclerView.Adapter<AccountAdapter.AccountViewHolder>() {

    private var filteredAccounts: List<Account> = accounts
    private var isLoading = false
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
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AccountViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_account, parent, false)
        return AccountViewHolder(view)
    }

    override fun onBindViewHolder(holder: AccountViewHolder, position: Int) {
        val account = filteredAccounts[position]
        val context = holder.itemView.context

        // Setup the spinner
        val jabatanArray = arrayOf("Direktur", "Direktur Operasional", "General Manager", "Karyawan")
        val adapter = ArrayAdapter(context, android.R.layout.simple_spinner_item, jabatanArray)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        holder.spinnerJabatan.adapter = adapter

        holder.spinnerJabatan.setSelection(account.jabatan - 1, false)

        holder.txtName.text = account.name
        holder.txtEmail.text = account.email

        firestore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        holder.layoutShowOptions.setOnClickListener {
            holder.layoutOptions.visibility = if (holder.layoutOptions.visibility == View.VISIBLE) View.GONE else View.VISIBLE
        }

        holder.layoutDelete.setOnClickListener {
            listener.onDeleteClick(account)
            AlertDialog.Builder(context)
                .setTitle("Delete Account")
                .setMessage("Are you sure you want to delete this account?")
                .setPositiveButton("Yes") { dialog, _ ->
                    val email = account.email
                    val password = account.password
                    auth.signInWithEmailAndPassword(email, password)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                Toast.makeText(context, "Masuk ke akun ${account.name}", Toast.LENGTH_SHORT).show()
                                val user = auth.currentUser!!
                                firestore.collection("users").document(account.id).delete()
                                    .addOnCompleteListener { task ->
                                        if (task.isSuccessful) {
                                            Toast.makeText(context, "Berhasil menghapus data akun ${account.name}", Toast.LENGTH_SHORT).show()
                                            user.delete().addOnCompleteListener { task ->
                                                if (task.isSuccessful) {
                                                    Toast.makeText(context, "Berhasil menghapus akun ${account.name}", Toast.LENGTH_SHORT).show()
                                                } else {
                                                    Toast.makeText(context, "Gagal menghapus akun ${account.name}: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        } else {
                                            Toast.makeText(context, "Gagal menghapus data akun ${account.name}: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                            } else {
                                Toast.makeText(context, "Gagal masuk ke akun ${account.name}: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    auth.signOut()
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
                        account.jabatan = position + 1
                        firestore.collection("users").document(account.id).update(
                            mapOf(
                                "jabatan" to position + 1,
                                "isAdmin" to (position + 1 < 4)
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
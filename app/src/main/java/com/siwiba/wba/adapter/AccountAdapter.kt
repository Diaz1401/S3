package com.siwiba.wba.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.CheckBox
import androidx.recyclerview.widget.RecyclerView
import com.siwiba.R
import com.siwiba.wba.model.Account
import android.app.AlertDialog
import android.content.Context
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class AccountAdapter(private var accounts: List<Account>, private val listener: OnAccountClickListener) :
    RecyclerView.Adapter<AccountAdapter.AccountViewHolder>() {

    private var filteredAccounts: List<Account> = accounts
    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

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
        val cbMakeAdmin: CheckBox = itemView.findViewById(R.id.cbMakeAdmin)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AccountViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_account, parent, false)
        return AccountViewHolder(view)
    }

    override fun onBindViewHolder(holder: AccountViewHolder, position: Int) {
        val account = filteredAccounts[position]
        val context = holder.itemView.context
        holder.txtName.text = account.name
        holder.txtEmail.text = account.email
        holder.cbMakeAdmin.isChecked = account.isAdmin
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

        holder.cbMakeAdmin.setOnCheckedChangeListener { _, isChecked ->
            listener.onMakeAdminClick(account)
            AlertDialog.Builder(context)
                .setTitle("Update Admin Status")
                .setMessage("Are you sure you want to update the admin status?")
                .setPositiveButton("Yes") { dialog, _ ->
                    account.isAdmin = isChecked
                    holder.itemView.post {
                        notifyItemChanged(position)
                    }
                    firestore.collection("users").document(account.id).update("isAdmin", isChecked)
                        .addOnSuccessListener {
                            Toast.makeText(context, "Berhasil mengubah status admin", Toast.LENGTH_SHORT).show()
                        }
                        .addOnFailureListener { exception ->
                            Toast.makeText(context, "Gagal mengubah status admin: ${exception.message}", Toast.LENGTH_SHORT).show()
                        }
                    dialog.dismiss()
                }
                .setNegativeButton("No") { dialog, _ ->
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
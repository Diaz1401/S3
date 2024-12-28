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
    private val context: Context,
    private val isOnResume: Boolean
) : RecyclerView.Adapter<AccountAdapter.AccountViewHolder>() {

    private var filteredAccounts: List<Account> = accounts
    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    var isCancel1 = false
    var isCancel2 = false

    interface OnAccountClickListener {
        fun onDeleteClick(account: Account)
        fun onJabatanSelect(account: Account, position: Int)
        fun onScopeModeSelect(account: Account, position: Int)
        fun onCbGajiClick(account: Account, isChecked: Boolean)
        fun onCbPajakClick(account: Account, isChecked: Boolean)
        fun onCbPinjamanClick(account: Account, isChecked: Boolean)
        fun onCbKasClick(account: Account, isChecked: Boolean)
        fun onCbLogistikClick(account: Account, isChecked: Boolean)
        fun onCbBpjsClick(account: Account, isChecked: Boolean)
        fun onCbTagihanClick(account: Account, isChecked: Boolean)
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
        val cbTagihan: CheckBox = itemView.findViewById(R.id.cbTagihan)
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
        val jabatanArray = arrayOf("Direktur", "Direktur Operasional", "General Manager", "Manager Keuangan", "Karyawan")
        val adapterJabatan = ArrayAdapter(context, R.layout.item_spinner_black, jabatanArray)
        adapterJabatan.setDropDownViewResource(R.layout.simple_spinner_dropdown_item)
        holder.spinnerJabatan.adapter = adapterJabatan
        holder.spinnerJabatan.setSelection(account.jabatan - 1, false)

        // Setup spinner scopeMode
        val scopeModeArray = arrayOf("WBA & KWI", "WBA", "KWI")
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
        holder.cbTagihan.isChecked = account.scopeTagihan

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
            AlertDialog.Builder(context)
                .setTitle("Delete Account")
                .setMessage("Are you sure you want to delete this account?")
                .setPositiveButton("Yes") { dialog, _ ->
                    listener.onDeleteClick(account)
                    dialog.dismiss()
                }
                .setNegativeButton("No") { dialog, _ ->
                    dialog.dismiss()
                }
                .create()
                .show()
        }

        holder.spinnerJabatan.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, positionSpinner: Int, id: Long) {
                if (isCancel1 || isOnResume) {
                    isCancel1 = false
                    return
                }
                AlertDialog.Builder(context)
                    .setTitle("Ubah Jabatan")
                    .setMessage("Apakah anda yakin ingin mengubah jabatan?")
                    .setPositiveButton("Ya") { dialog, _ ->
                        listener.onJabatanSelect(account, positionSpinner)
                        dialog.dismiss()
                    }
                    .setNegativeButton("Tidak") { dialog, _ ->
                        isCancel1 = true
                        holder.spinnerJabatan.setSelection(account.jabatan - 1, false)
                        dialog.dismiss()
                    }
                    .create()
                    .show()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) { }
        }

        holder.spinnerScope.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, positionSpinner: Int, id: Long) {
                if (isCancel2 || isOnResume) {
                    isCancel2 = false
                    return
                }
                AlertDialog.Builder(context)
                    .setTitle("Ubah Scope Saldo")
                    .setMessage("Apakah anda yakin ingin mengubah scope saldo?")
                    .setPositiveButton("Ya") { dialog, _ ->
                        listener.onScopeModeSelect(account, positionSpinner)
                        dialog.dismiss()
                    }
                    .setNegativeButton("Tidak") { dialog, _ ->
                        isCancel2 = true
                        holder.spinnerScope.setSelection(account.scopeMode, false)
                        dialog.dismiss()
                    }
                    .create()
                    .show()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) { }
        }

        holder.cbGaji.setOnClickListener {
            AlertDialog.Builder(context)
                .setTitle("Ubah Scope Gaji")
                .setMessage("Apakah anda yakin ingin mengubah scope gaji?")
                .setPositiveButton("Ya") { dialog, _ ->
                    listener.onCbGajiClick(account, holder.cbGaji.isChecked)
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
            AlertDialog.Builder(context)
                .setTitle("Ubah Scope Pajak")
                .setMessage("Apakah anda yakin ingin mengubah scope pajak?")
                .setPositiveButton("Ya") { dialog, _ ->
                    listener.onCbPajakClick(account, holder.cbPajak.isChecked)
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
            AlertDialog.Builder(context)
                .setTitle("Ubah Scope Pinjaman")
                .setMessage("Apakah anda yakin ingin mengubah scope pinjaman?")
                .setPositiveButton("Ya") { dialog, _ ->
                    listener.onCbPinjamanClick(account, holder.cbPinjaman.isChecked)
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
            AlertDialog.Builder(context)
                .setTitle("Ubah Scope Kas")
                .setMessage("Apakah anda yakin ingin mengubah scope kas?")
                .setPositiveButton("Ya") { dialog, _ ->
                    listener.onCbKasClick(account, holder.cbKas.isChecked)
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
            AlertDialog.Builder(context)
                .setTitle("Ubah Scope Logistik")
                .setMessage("Apakah anda yakin ingin mengubah scope logistik?")
                .setPositiveButton("Ya") { dialog, _ ->
                    listener.onCbLogistikClick(account, holder.cbLogistik.isChecked)
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
            AlertDialog.Builder(context)
                .setTitle("Ubah Scope BPJS")
                .setMessage("Apakah anda yakin ingin mengubah scope BPJS?")
                .setPositiveButton("Ya") { dialog, _ ->
                    listener.onCbBpjsClick(account, holder.cbBpjs.isChecked)
                    dialog.dismiss()
                }
                .setNegativeButton("Tidak") { dialog, _ ->
                    holder.cbBpjs.isChecked = !holder.cbBpjs.isChecked
                    dialog.dismiss()
                }
                .create()
                .show()
        }

        holder.cbTagihan.setOnClickListener {
            AlertDialog.Builder(context)
                .setTitle("Ubah Scope Tagihan")
                .setMessage("Apakah anda yakin ingin mengubah scope tagihan?")
                .setPositiveButton("Ya") { dialog, _ ->
                    listener.onCbTagihanClick(account, holder.cbTagihan.isChecked)
                    dialog.dismiss()
                }
                .setNegativeButton("Tidak") { dialog, _ ->
                    holder.cbTagihan.isChecked = !holder.cbTagihan.isChecked
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

    override fun onViewRecycled(holder: AccountViewHolder) {
        super.onViewRecycled(holder)
    }
}
package com.siwiba.wba.activity

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.firestore.FirebaseFirestore
import com.siwiba.R
import com.siwiba.databinding.ActivityManageAccountBinding
import com.siwiba.util.AppMode
import com.siwiba.wba.SignUpActivity
import com.siwiba.wba.adapter.AccountAdapter
import com.siwiba.wba.model.Account

class ManageAccountActivity : AppCompatActivity(), AccountAdapter.OnAccountClickListener {
    private lateinit var binding: ActivityManageAccountBinding
    private lateinit var accountAdapter: AccountAdapter
    private var firestore = FirebaseFirestore.getInstance()
    private val accounts = mutableListOf<Account>()

    override fun onCreate(savedInstanceState: Bundle?) {
        val appMode = AppMode(this)
        if (appMode.getAppMode()) {
            setTheme(R.style.Base_Theme_WBA)
        } else {
            setTheme(R.style.Base_Theme_KWI)
        }
        super.onCreate(savedInstanceState)
        binding = ActivityManageAccountBinding.inflate(layoutInflater)
        firestore = FirebaseFirestore.getInstance()
        setContentView(binding.root)

        accountAdapter = AccountAdapter(accounts, this)
        binding.recyclerViewAccounts.adapter = accountAdapter
        binding.recyclerViewAccounts.layoutManager = LinearLayoutManager(this)

        loadAccounts()

        binding.searchView.setOnQueryTextListener(object : androidx.appcompat.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                accountAdapter.filter(newText ?: "")
                return true
            }
        })

        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.layoutTambah.setOnClickListener() {
            val intent = Intent(this, SignUpActivity::class.java)
            startActivity(intent)
        }
    }

    private fun loadAccounts() {
        // Load accounts from Firestore or any other source
        firestore.collection("users").get()
            .addOnSuccessListener { documents ->
                accounts.clear()
                for (document in documents) {
                    val account = Account(
                        id = document.id,
                        name = document.getString("name") ?: "",
                        jabatan = document.getLong("jabatan")?.toInt() ?: 0,
                        isAdmin = document.getBoolean("isAdmin") ?: false,
                        email = document.getString("email") ?: "",
                        password = document.getString("password") ?: ""
                    )
                    accounts.add(account)
                }
                accountAdapter.notifyDataSetChanged()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to load accounts", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onEditClick(account: Account) {
        // Edit current account
    }

    override fun onDeleteClick(account: Account) {
        // Handle delete account
    }

    override fun onMakeAdminClick(account: Account) {
        // Handle make admin
    }

}
package com.siwiba.wba.activity

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.auth.FirebaseAuth
import com.siwiba.R
import com.siwiba.databinding.ActivityManageAccountBinding
import com.siwiba.util.AppMode
import com.siwiba.wba.SignUpActivity
import com.siwiba.wba.adapter.AccountAdapter
import com.siwiba.wba.model.Account

class ManageAccountActivity : AppCompatActivity(), AccountAdapter.OnAccountClickListener {
    private lateinit var binding: ActivityManageAccountBinding
    private lateinit var accountAdapter: AccountAdapter
    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
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
        auth = FirebaseAuth.getInstance()
        setContentView(binding.root)

        loadAccounts(false)

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

    private fun loadAccounts(isOnResume: Boolean) {
        accountAdapter = AccountAdapter(accounts, this, this, isOnResume)
        binding.recyclerViewAccounts.adapter = accountAdapter
        binding.recyclerViewAccounts.layoutManager = LinearLayoutManager(this)

        // Load accounts from Firestore or any other source
        firestore.collection("users").get()
            .addOnSuccessListener { documents ->
                accounts.clear()
                for (document in documents) {
                    val account = Account(
                        id = document.id,
                        name = document.getString("name") ?: "",
                        jabatan = document.getLong("jabatan")?.toInt() ?: 0,
                        scopeMode = document.getLong("scopeMode")?.toInt() ?: 0,
                        scopeGaji = document.getBoolean("scopeGaji") ?: false,
                        scopePajak = document.getBoolean("scopePajak") ?: false,
                        scopePinjaman = document.getBoolean("scopePinjaman") ?: false,
                        scopeKas = document.getBoolean("scopeKas") ?: false,
                        scopeLogistik = document.getBoolean("scopeLogistik") ?: false,
                        scopeBpjs = document.getBoolean("scopeBpjs") ?: false,
                        scopeTagihan = document.getBoolean("scopeTagihan") ?: false,
                        isAdmin = document.getBoolean("isAdmin") ?: false,
                        email = document.getString("email") ?: "",
                        password = document.getString("password") ?: ""
                    )
                    accounts.add(account)
                }
                accountAdapter.notifyDataSetChanged()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Gagal memuat akun", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onDeleteClick(account: Account) {
        var email = account.email
        var password = account.password
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "Menghapus akun ${account.name}", Toast.LENGTH_SHORT).show()
                    val user = auth.currentUser!!
                    firestore.collection("users").document(account.id).delete()
                        .continueWithTask { deleteTask ->
                            if (deleteTask.isSuccessful) {
                                user.delete()
                            } else {
                                throw deleteTask.exception ?: Exception("Gagal menghapus data akun ${account.name}")
                            }
                        }
                        .addOnCompleteListener { deleteUserTask ->
                            if (deleteUserTask.isSuccessful) {
                                Toast.makeText(this, "Berhasil menghapus akun ${account.name}", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(this, "Gagal menghapus akun ${account.name}: ${deleteUserTask.exception?.message}", Toast.LENGTH_SHORT).show()
                            }
                            auth.signOut()
                            val sharedPref = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
                            email = sharedPref.getString("email", "") ?: ""
                            password = sharedPref.getString("password", "") ?: ""
                            auth.signInWithEmailAndPassword(email, password)
                                .addOnCompleteListener { reauthTask ->
                                    if (reauthTask.isSuccessful) {
                                        Toast.makeText(this, "Berhasil masuk kembali", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(this, "Gagal masuk ke akun ${account.name}: ${reauthTask.exception?.message}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                        }
                    accounts.remove(account)
                    loadAccounts(true)
                } else {
                    Toast.makeText(this, "Gagal masuk ke akun ${account.name}: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    override fun onJabatanSelect(account: Account, position: Int) {
        val correctedPosition = position + 1
        account.jabatan = correctedPosition
        firestore.collection("users").document(account.id).update(
            mapOf(
                "jabatan" to correctedPosition,
                "isAdmin" to (correctedPosition < 4)
            )
        ).addOnSuccessListener {
            Toast.makeText(this, "Berhasil mengubah jabatan", Toast.LENGTH_SHORT).show()
        }.addOnFailureListener { exception ->
            Toast.makeText(this, "Gagal mengubah jabatan: ${exception.message}", Toast.LENGTH_SHORT).show()
        }
        loadAccounts(true)
    }

    override fun onScopeModeSelect(account: Account, position: Int) {
        account.scopeMode = position
        firestore.collection("users").document(account.id).update("scopeMode", position)
            .addOnSuccessListener {
                Toast.makeText(this, "Berhasil mengubah scope mode", Toast.LENGTH_SHORT).show()
            }.addOnFailureListener { exception ->
                Toast.makeText(this, "Gagal mengubah scope mode: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
        loadAccounts(true)
    }

    override fun onCbGajiClick(account: Account, isChecked: Boolean) {
        account.scopeGaji = isChecked
        firestore.collection("users").document(account.id).update("scopeGaji", isChecked)
            .addOnSuccessListener {
                Toast.makeText(this, "Berhasil mengubah scope gaji", Toast.LENGTH_SHORT).show()
            }.addOnFailureListener { exception ->
                Toast.makeText(this, "Gagal mengubah scope gaji: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
        loadAccounts(true)
    }

    override fun onCbPajakClick(account: Account, isChecked: Boolean) {
        account.scopePajak = isChecked
        firestore.collection("users").document(account.id).update("scopePajak", isChecked)
            .addOnSuccessListener {
                Toast.makeText(this, "Berhasil mengubah scope pajak", Toast.LENGTH_SHORT).show()
            }.addOnFailureListener { exception ->
                Toast.makeText(this, "Gagal mengubah scope pajak: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
        loadAccounts(true)
    }

    override fun onCbPinjamanClick(account: Account, isChecked: Boolean) {
        account.scopePinjaman = isChecked
        firestore.collection("users").document(account.id).update("scopePinjaman", isChecked)
            .addOnSuccessListener {
                Toast.makeText(this, "Berhasil mengubah scope pinjaman", Toast.LENGTH_SHORT).show()
            }.addOnFailureListener { exception ->
                Toast.makeText(this, "Gagal mengubah scope pinjaman: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
        loadAccounts(true)
    }

    override fun onCbKasClick(account: Account, isChecked: Boolean) {
        account.scopeKas = isChecked
        firestore.collection("users").document(account.id).update("scopeKas", isChecked)
            .addOnSuccessListener {
                Toast.makeText(this, "Berhasil mengubah scope kas", Toast.LENGTH_SHORT).show()
            }.addOnFailureListener { exception ->
                Toast.makeText(this, "Gagal mengubah scope kas: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
        loadAccounts(true)
    }

    override fun onCbLogistikClick(account: Account, isChecked: Boolean) {
        account.scopeLogistik = isChecked
        firestore.collection("users").document(account.id).update("scopeLogistik", isChecked)
            .addOnSuccessListener {
                Toast.makeText(this, "Berhasil mengubah scope logistik", Toast.LENGTH_SHORT).show()
            }.addOnFailureListener { exception ->
                Toast.makeText(this, "Gagal mengubah scope logistik: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
        loadAccounts(true)
    }

    override fun onCbBpjsClick(account: Account, isChecked: Boolean) {
        account.scopeBpjs = isChecked
        firestore.collection("users").document(account.id).update("scopeBpjs", isChecked)
            .addOnSuccessListener {
                Toast.makeText(this, "Berhasil mengubah scope BPJS", Toast.LENGTH_SHORT).show()
            }.addOnFailureListener { exception ->
                Toast.makeText(this, "Gagal mengubah scope BPJS: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
        loadAccounts(true)
    }

    override fun onCbTagihanClick(account: Account, isChecked: Boolean) {
        account.scopeTagihan = isChecked
        firestore.collection("users").document(account.id).update("scopeTagihan", isChecked)
            .addOnSuccessListener {
                Toast.makeText(this, "Berhasil mengubah scope tagihan", Toast.LENGTH_SHORT).show()
            }.addOnFailureListener { exception ->
                Toast.makeText(this, "Gagal mengubah scope tagihan: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
        loadAccounts(true)
    }

    override fun onResume() {
        super.onResume()
        loadAccounts(true)
    }
}
package com.siwiba.wba

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.util.Patterns
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.OAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.siwiba.databinding.ActivitySignUpBinding
import java.io.ByteArrayOutputStream
import java.io.FileNotFoundException
import android.widget.AdapterView
import com.siwiba.R
import com.siwiba.util.AppMode
import com.siwiba.util.EncSharedPref

class SignUpActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySignUpBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private var encodedImage: String? = null
    private var completeSignUp: Boolean = false
    private lateinit var sharedPref: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        val appMode = AppMode(this)
        if (appMode.getAppMode()) {
            setTheme(R.style.Base_Theme_WBA)
        } else {
            setTheme(R.style.Base_Theme_KWI)
        }
        super.onCreate(savedInstanceState)
        binding = ActivitySignUpBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        sharedPref = EncSharedPref(this).getEncSharedPref()

        completeSignUp = intent.getBooleanExtra("completeSignUp", false)

        if (completeSignUp) {
            binding.txtTitle.text = "Lengkapi Profil"
            binding.btnSignUp.text = "SIMPAN DATA"
            binding.inputEmailSignUp.visibility = View.GONE
            binding.inputPasswordSignUp.visibility = View.GONE
            binding.inputConfirmPasswordSignUp.visibility = View.GONE
        }

        binding.layoutProfile.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            setProfileImage.launch(intent)
        }

        binding.btnSignUp.setOnClickListener {
            if (completeSignUp) {
                if (encodedImage == null) {
                    Toast.makeText(this, "Atur foto profil", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                if (binding.inputNameSignUp.text.toString().isEmpty()) {
                    Toast.makeText(this, "Masukan nama", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                if (binding.inputAlamat.text.toString().isEmpty()) {
                    Toast.makeText(this, "Masukan alamat", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                val user = intent.getParcelableExtra<FirebaseUser>("user")
                saveUserData(user!!)
            } else {
                val name = binding.inputNameSignUp.text.toString()
                val jabatan = binding.spinnerJabatan.selectedItemPosition
                val email = binding.inputEmailSignUp.text.toString()
                val address = binding.inputAlamat.text.toString()
                val password = binding.inputPasswordSignUp.text.toString()
                val confirmPassword = binding.inputConfirmPasswordSignUp.text.toString()

                if (isValidSignUpDetails(name, email, jabatan, address, password, confirmPassword)) {
                    signUpWithEmail(email, password)
                    finish()
                }
            }
        }

        //Setup spinnerScope
        val scopeArray = arrayOf("WBA & KWI", "WBA", "KWI")
        val adapterScope = ArrayAdapter(this, R.layout.item_spinner_black, scopeArray)
        adapterScope.setDropDownViewResource(R.layout.simple_spinner_dropdown_item)
        binding.spinnerScope.adapter = adapterScope

        // Set up the Spinner with the options
        val jabatanArray = arrayOf("Jabatan", "Direktur", "Direktur Operasional", "General Manager", "Manager Keuangan", "Karyawan")
        val adapter = ArrayAdapter(this, R.layout.item_spinner_black, jabatanArray)
        adapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item)
        binding.spinnerJabatan.adapter = adapter

        binding.spinnerJabatan.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                // Display the selected item text on text view
                val selectedItem = parent.getItemAtPosition(position).toString()
                if (selectedItem == "Jabatan") {
                    Toast.makeText(this@SignUpActivity, "Pilih jabatan terlebih dahulu", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                // Do nothing
            }
        }
    }

    private fun signUpWithEmail(email: String, password: String) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    user?.let {
                        it.sendEmailVerification()
                            .addOnCompleteListener { verificationTask ->
                                if (verificationTask.isSuccessful) {
                                    saveUserData(it)
                                    Toast.makeText(this, "Sign Up sukses. Link verifikasi email telah dikirim", Toast.LENGTH_SHORT).show()
                                    finish()
                                } else {
                                    Toast.makeText(this, "Gagal mengirim link verifikasi email: ${verificationTask.exception?.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                    }
                    auth.signOut()
                    val myEmail = sharedPref.getString("email", "") ?: ""
                    val myPassword = sharedPref.getString("password", "") ?: ""
                    auth.signInWithEmailAndPassword(myEmail, myPassword)
                        .addOnCompleteListener { reauthTask ->
                            if (reauthTask.isSuccessful) {
                                Toast.makeText(this, "Berhasil masuk kembali", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(this, "Gagal masuk ke akun ${myEmail}: ${reauthTask.exception?.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                } else {
                    Toast.makeText(this, "Sign Up failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun signInWithGoogle() {
        val provider = OAuthProvider.newBuilder("google.com")
        auth.startActivityForSignInWithProvider(this, provider.build())
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    user?.let {
                        firestore.collection("users").document(it.uid).get()
                            .addOnSuccessListener { document ->
                                if (document.exists()) {
                                    handleSignInResult(task)
                                } else {
                                    Toast.makeText(this, "Lengkapi data terlebih dahulu", Toast.LENGTH_SHORT).show()
                                    auth.signOut()
                                    val intent = Intent(this, SignUpActivity::class.java)
                                    intent.putExtra("completeSignUp", true)
                                    intent.putExtra("user", user)
                                    startActivity(intent)
                                    finish()
                                }
                            }
                            .addOnFailureListener { exception ->
                                Toast.makeText(this, "Gagal memeriksa akun: ${exception.message}", Toast.LENGTH_SHORT).show()
                            }
                    }
                } else {
                    Toast.makeText(this, "Sign In gagal: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun handleSignInResult(task: Task<AuthResult>) {
        if (task.isSuccessful) {
            val user = auth.currentUser
            user?.let {
                saveUserData(it)
                Toast.makeText(this, "Sign Up sukses", Toast.LENGTH_SHORT).show()
                finish()
            }
        } else {
            Toast.makeText(this, "Sign Up gagal: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveUserData(user: FirebaseUser) {
        val userData = hashMapOf(
            "name" to binding.inputNameSignUp.text.toString(),
            "jabatan" to binding.spinnerJabatan.selectedItemPosition,
            "isAdmin" to (binding.spinnerJabatan.selectedItemPosition < 5),
            "scopeMode" to binding.spinnerScope.selectedItemPosition,
            "email" to user.email,
            "address" to binding.inputAlamat.text.toString(),
            "profileImage" to encodedImage,
            "password" to binding.inputPasswordSignUp.text.toString()
        )

        firestore.collection("users").document(user.uid)
            .set(userData)
            .addOnSuccessListener {
                Toast.makeText(this, "Data pengguna berhasil disimpan", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Gagal menyimpan data pengguna: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private val setProfileImage: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            if (result.resultCode == RESULT_OK) {
                if (result.data != null) {
                    val imageUri = result.data?.data
                    try {
                        val inputStream = imageUri?.let { contentResolver.openInputStream(it) }
                        val bitmap = BitmapFactory.decodeStream(inputStream)
                        binding.imgProfile.setImageBitmap(bitmap)
                        binding.txtProfile.visibility = View.GONE
                        encodedImage = encodeImage(bitmap)
                    } catch (e: FileNotFoundException) {
                        e.printStackTrace()
                    }
                }
            }
        }

    private fun encodeImage(bitmap: Bitmap): String {
        val previewWidth = 150
        val previewHeight = bitmap.height * previewWidth / bitmap.width
        val previewBitmap = Bitmap.createScaledBitmap(bitmap, previewWidth, previewHeight, false)
        val byteArrayOutputStream = ByteArrayOutputStream()
        previewBitmap.compress(Bitmap.CompressFormat.JPEG, 90, byteArrayOutputStream)
        val byteArray = byteArrayOutputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.DEFAULT)
    }

    private fun isValidSignUpDetails(name: String, email: String, jabatan: Int, address: String, password: String, confirmPassword: String): Boolean {
        return when {
            name.isEmpty() -> {
                Toast.makeText(this, "Masukan nama", Toast.LENGTH_SHORT).show()
                false
            }
            jabatan == 0 -> {
                Toast.makeText(this, "Pilih jabatan terlebih dahulu", Toast.LENGTH_SHORT).show()
                false
            }
            email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                Toast.makeText(this, "Masukan email yang valid", Toast.LENGTH_SHORT).show()
                false
            }
            address.isEmpty() -> {
                Toast.makeText(this, "Masukan alamat", Toast.LENGTH_SHORT).show()
                false
            }
            password.isEmpty() -> {
                Toast.makeText(this, "Masukan password", Toast.LENGTH_SHORT).show()
                false
            }
            password != confirmPassword -> {
                Toast.makeText(this, "Password tidak cocok", Toast.LENGTH_SHORT).show()
                false
            }

            else -> true
        }
    }
}
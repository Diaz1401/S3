package com.siwiba.wba

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.util.Patterns
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.siwiba.databinding.ActivitySignUpBinding
import java.io.ByteArrayOutputStream
import java.io.FileNotFoundException

class SignUpActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySignUpBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private var encodedImage: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignUpBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        binding.btnSignUp.setOnClickListener {
            if (isValidInput()) {
                signUp()
            }
        }

        binding.layoutProfile.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            setProfileImage.launch(intent)
        }
    }

    private fun signUp() {
        val email = binding.inputEmailSignUp.text.toString()
        val password = binding.inputPasswordSignUp.text.toString()
        val name = binding.inputNameSignUp.text.toString()
        val address = binding.inputAlamat.text.toString()

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    user?.let {
                        val uid = it.uid
                        saveUserData(uid, name, email, address, encodedImage)
                    }
                    Toast.makeText(this, "Sign Up Successful", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    Toast.makeText(this, "Sign Up Failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun saveUserData(uid: String, name: String, email: String, address: String, encodedImage: String?) {
        val user = hashMapOf(
            "name" to name,
            "email" to email,
            "address" to address,
            "profileImage" to encodedImage
        )

        firestore.collection("users").document(uid)
            .set(user)
            .addOnSuccessListener {
                Toast.makeText(this, "User data saved", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error saving user data: ${e.message}", Toast.LENGTH_SHORT).show()
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

    private fun isValidInput(): Boolean {
        if (encodedImage == null) {
            showToast("Select profile image")
            return false
        } else if (binding.inputNameSignUp.text.toString().trim().isEmpty()) {
            showToast("Enter name")
            return false
        } else if (!Patterns.EMAIL_ADDRESS.matcher(binding.inputEmailSignUp.text.toString()).matches()) {
            showToast("Enter valid email")
            return false
        } else if (binding.inputPasswordSignUp.text.toString().trim().isEmpty()) {
            showToast("Enter password")
            return false
        } else if (binding.inputPasswordSignUp.text.toString() != binding.inputConfirmPasswordSignUp.text.toString()) {
            showToast("Password & confirm password must be same")
            return false
        } else {
            return true
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
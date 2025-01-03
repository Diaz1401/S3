package com.siwiba.wba.fragment

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity.RESULT_OK
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.siwiba.databinding.ActivityMainBinding
import com.siwiba.databinding.FragmentProfilBinding
import com.siwiba.wba.SignInActivity
import com.siwiba.MainActivity
import com.siwiba.util.EncSharedPref
import com.siwiba.wba.activity.AboutActivity
import com.siwiba.wba.activity.ManageAccountActivity
import java.io.ByteArrayOutputStream
import java.io.FileNotFoundException

class ProfilFragment : Fragment() {

    private var _binding: FragmentProfilBinding? = null
    private val binding get() = _binding!!
    private var encodedImage: String? = null
    private lateinit var sharedPref: SharedPreferences
    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        auth = FirebaseAuth.getInstance()
        _binding = FragmentProfilBinding.inflate(inflater, container, false)
        firestore = FirebaseFirestore.getInstance()
        sharedPref = EncSharedPref(requireContext()).getEncSharedPref()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val isAdmin = sharedPref.getBoolean("isAdmin", false)

        if (isAdmin) {
            binding.layoutManageAkun.visibility = View.VISIBLE
        }

        binding.layoutProfile.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            setProfileImage.launch(intent)
            binding.btnSaveImg.visibility = View.VISIBLE
        }

        binding.btnSaveImg.setOnClickListener {
            // Save image to firestore and shared prefs
            val editor = sharedPref.edit()
            editor.putString("profileImage", encodedImage)
            editor.apply()
            binding.btnSaveImg.visibility = View.GONE

            val id = sharedPref.getString("uid", "N/A")
            firestore.collection("users").document(id!!).update("profileImage", encodedImage)
                .addOnSuccessListener {
                    Toast.makeText(requireContext(), "Profile image updated", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener {
                    Toast.makeText(requireContext(), "Failed to update profile image", Toast.LENGTH_SHORT).show()
                }
        }

        binding.btnEditName.setOnClickListener {
            binding.txtName.isEnabled = true
            binding.btnSaveName.visibility = View.VISIBLE
        }

        binding.btnSaveName.setOnClickListener {
            val name = binding.txtName.text.toString()
            val editor = sharedPref.edit()
            editor.putString("name", name)
            editor.apply()
            binding.txtName.isEnabled = false
            binding.btnSaveName.visibility = View.GONE

            val id = sharedPref.getString("uid", "N/A")
            firestore.collection("users").document(id!!).update("name", name)
                .addOnSuccessListener {
                    Toast.makeText(requireContext(), "Name updated", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener {
                    Toast.makeText(requireContext(), "Failed to update name", Toast.LENGTH_SHORT).show()
                }
        }

        binding.btnEditAlamat.setOnClickListener {
            binding.txtAlamat.isEnabled = true
            binding.btnSaveAlamat.visibility = View.VISIBLE
        }

        binding.btnSaveAlamat.setOnClickListener {
            val address = binding.txtAlamat.text.toString()
            val editor = sharedPref.edit()
            editor.putString("address", address)
            editor.apply()
            binding.txtAlamat.isEnabled = false
            binding.btnSaveAlamat.visibility = View.GONE

            val id = sharedPref.getString("uid", "N/A")
            firestore.collection("users").document(id!!).update("address", address)
                .addOnSuccessListener {
                    Toast.makeText(requireContext(), "Address updated", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener {
                    Toast.makeText(requireContext(), "Failed to update address", Toast.LENGTH_SHORT).show()
                }
        }

        binding.layoutUID.setOnClickListener {
            val uid = binding.txtId.text.toString()
            val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
            val clip = android.content.ClipData.newPlainText("UID", uid)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(requireContext(), "UID copied to clipboard", Toast.LENGTH_SHORT).show()
        }

        binding.layoutResetPassword.setOnClickListener {
            binding.layoutPassword.visibility = if (binding.layoutPassword.visibility == View.VISIBLE) View.GONE else View.VISIBLE
        }

        binding.layoutLogOut.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Keluar")
                .setMessage("Apakah anda yakin untuk keluar?")
                .setPositiveButton("Ya") { dialog, _ ->
                    val editor = sharedPref.edit()
                    editor.clear()
                    editor.apply()
                    auth.signOut()
                    activity?.let {
                        val intent = Intent(it, SignInActivity::class.java)
                        startActivity(intent)
                        it.finish()
                    }
                    dialog.dismiss()
                }
                .setNegativeButton("Tidak", null)
                .create()
                .show()
        }

        binding.layoutTentangAplikasi.setOnClickListener {
            // Intent to AboutActivity
            activity?.let {
                val intent = Intent(it, AboutActivity::class.java)
                startActivity(intent)
            }
        }

        binding.btnBackToHome.setOnClickListener {
            // Navigate back to the dashboard fragment
            activity?.let {
                val intent = Intent(it, MainActivity::class.java)
                startActivity(intent)
                it.finish()
            }
        }

        binding.layoutManageAkun.setOnClickListener {
            // Navigate to ManageAccountActivity
            activity?.let {
                val intent = Intent(it, ManageAccountActivity::class.java)
                startActivity(intent)
            }
        }

        binding.btnSimpanPassword.setOnClickListener {
            val password = binding.inputPassword.text.toString()
            val confirmPassword = binding.inputConfirmPassword.text.toString()
            if (password != confirmPassword) {
                Toast.makeText(requireContext(), "Password tidak cocok", Toast.LENGTH_SHORT).show()
            } else {
                auth.currentUser?.updatePassword(password)
                    ?.addOnSuccessListener {
                        firestore.collection("users").document(auth.currentUser?.uid!!)
                            .update("password", password)
                            .addOnSuccessListener {
                                Toast.makeText(requireContext(), "Password berhasil diubah", Toast.LENGTH_SHORT).show()
                                Toast.makeText(requireContext(), "Tolong masuk kembali", Toast.LENGTH_SHORT).show()
                                val editor = sharedPref.edit()
                                editor.clear()
                                editor.apply()
                                auth.signOut()
                                activity?.let {
                                    val intent = Intent(it, SignInActivity::class.java)
                                    startActivity(intent)
                                    it.finish()
                                }
                            }
                            .addOnFailureListener {
                                Toast.makeText(requireContext(), "Gagal mengubah password: ${it.message}", Toast.LENGTH_SHORT).show()
                            }
                    }
                    ?.addOnFailureListener {
                        Toast.makeText(requireContext(), "Gagal mengubah password: ${it.message}", Toast.LENGTH_SHORT).show()
                    }
            }
        }

        loadData()
    }

    private val setProfileImage: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            if (result.resultCode == RESULT_OK) {
                if (result.data != null) {
                    val imageUri = result.data?.data
                    try {
                        val inputStream = imageUri?.let { requireContext().contentResolver.openInputStream(it) }
                        val bitmap = BitmapFactory.decodeStream(inputStream)
                        binding.imgProfile.setImageBitmap(bitmap)
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

    private fun loadData() {
        val name = sharedPref.getString("name", "N/A")
        val id = sharedPref.getString("uid", "N/A")
        val email = sharedPref.getString("email", "N/A")
        val address = sharedPref.getString("address", "N/A")
        val profileImage = sharedPref.getString("profileImage", null)

        binding.txtId.text = id
        binding.txtName.setText(name)
        binding.txtName.isEnabled = false
        binding.txtEmail.setText(email)
        binding.txtEmail.isEnabled = false
        binding.txtAlamat.setText(address)
        binding.txtAlamat.isEnabled = false
        if (profileImage != null) {
            val imageBytes = Base64.decode(profileImage, Base64.DEFAULT)
            val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
            binding.imgProfile.setImageBitmap(bitmap)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
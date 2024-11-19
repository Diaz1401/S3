package com.siwiba.wba.fragment

import android.content.Context
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.siwiba.databinding.FragmentDashboardBinding

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        displayUserData()
    }

    private fun displayUserData() {
        val sharedPref = requireContext().getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        val name = sharedPref.getString("name", "Name not found")
        val email = sharedPref.getString("email", "Email not found")
        val address = sharedPref.getString("address", "Address not found")
        val uid = sharedPref.getString("uid", "UID not found")
        val profileImage = sharedPref.getString("profileImage", null)

        binding.txtName.text = name
        binding.txtEmail.text = email
        binding.txtAddress.text = address
        binding.txtId.text = uid

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
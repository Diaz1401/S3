package com.siwiba.wba.fragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.siwiba.databinding.FragmentKeuanganBinding
import com.siwiba.wba.activity.GajiActivity
import com.siwiba.wba.activity.PajakActivity
import com.siwiba.wba.activity.PinjamanActivity
import com.siwiba.wba.activity.KasActivity
import com.siwiba.wba.activity.BpjsActivity
import com.siwiba.wba.activity.LogistikActivity

class KeuanganFragment : Fragment() {

    private var _binding: FragmentKeuanganBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentKeuanganBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnGaji.setOnClickListener {
            val intent = Intent(activity, GajiActivity::class.java)
            startActivity(intent)
        }

        binding.btnPajak.setOnClickListener {
            val intent = Intent(activity, PajakActivity::class.java)
            startActivity(intent)
        }

        binding.btnPinjaman.setOnClickListener {
            val intent = Intent(activity, PinjamanActivity::class.java)
            startActivity(intent)
        }

        binding.btnKas.setOnClickListener {
            val intent = Intent(activity, KasActivity::class.java)
            startActivity(intent)
        }

        binding.btnBpjs.setOnClickListener {
            val intent = Intent(activity, BpjsActivity::class.java)
            startActivity(intent)
        }

        binding.btnLogistik.setOnClickListener {
            val intent = Intent(activity, LogistikActivity::class.java)
            startActivity(intent)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
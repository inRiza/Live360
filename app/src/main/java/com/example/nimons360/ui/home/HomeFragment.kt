package com.example.nimons360.ui.home

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.nimons360.R
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class HomeFragment : Fragment(R.layout.fragment_home) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val btnProfile = view.findViewById<TextView>(R.id.btnProfileHome)

        btnProfile.text = "L" // mock

        btnProfile.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_profile)
        }
    }
}

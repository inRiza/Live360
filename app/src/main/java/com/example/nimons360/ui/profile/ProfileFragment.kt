package com.example.nimons360.ui.profile

import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.nimons360.R
import com.example.nimons360.ui.profile.components.EditNameBottomSheet
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ProfileFragment : Fragment(R.layout.fragment_profile) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val tvName = view.findViewById<TextView>(R.id.tvProfileName)
        val tvEmail = view.findViewById<TextView>(R.id.tvProfileEmail)
        val btnSignOut = view.findViewById<TextView>(R.id.btnSignOut)
        val btnEditProfile = view.findViewById<ImageButton>(R.id.ivEditProfile)

        tvName.text = "Labpro ITB" // mock
        tvEmail.text = "labpro@std.stei.itb.ac.id" // mock

        btnSignOut.setOnClickListener {
            Toast.makeText(requireContext(), "Sign out clicked", Toast.LENGTH_SHORT).show()
        }

        btnEditProfile.setOnClickListener {
            val existing = childFragmentManager.findFragmentByTag("EditNameBottomSheet")
            if (existing == null) {
                EditNameBottomSheet.newInstance(tvName.text.toString())
                    .show(childFragmentManager, "EditNameBottomSheet")
            }
        }
    }
}
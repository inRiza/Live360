package com.example.nimons360.ui.profile

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.nimons360.R
import com.example.nimons360.ui.auth.login.LoginActivity
import com.example.nimons360.ui.profile.components.EditNameBottomSheet
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ProfileFragment : Fragment(R.layout.fragment_profile) {

    private val viewModel: ProfileViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val tvAvatar = view.findViewById<TextView>(R.id.tv_avatar)
        val tvName = view.findViewById<TextView>(R.id.tv_name)
        val tvEmail = view.findViewById<TextView>(R.id.tv_email)
        val btnSignOut = view.findViewById<TextView>(R.id.btn_sign_out)
        val btnEdit = view.findViewById<ImageButton>(R.id.btn_edit)

        btnEdit.setOnClickListener {
            val existing = childFragmentManager.findFragmentByTag("EditNameBottomSheet")
            if (existing == null) {
                EditNameBottomSheet.newInstance(tvName.text.toString()) { newName ->
                    viewModel.updateProfile(newName)
                }.show(childFragmentManager, "EditNameBottomSheet")
            }
        }

        btnSignOut.setOnClickListener {
            showSignOutConfirmationDialog()
        }

        observeViewModel(tvAvatar, tvName, tvEmail)
    }

    private fun observeViewModel(
        tvAvatar: TextView,
        tvName: TextView,
        tvEmail: TextView
    ) {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.user.collect { user ->
                        user ?: return@collect
                        tvName.text = user.fullName ?: "-"
                        tvEmail.text = user.email ?: "-"
                        tvAvatar.text = getInitials(user.fullName)
                    }
                }

                launch {
                    viewModel.event.collect { event ->
                        when (event) {
                            is ProfileEvent.SignedOut -> {
                                startActivity(
                                    Intent(requireContext(), LoginActivity::class.java).apply {
                                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                                                Intent.FLAG_ACTIVITY_CLEAR_TASK
                                    }
                                )
                            }
                            is ProfileEvent.UpdateSuccess -> {
                                Snackbar.make(
                                    requireView(),
                                    "Profile updated",
                                    Snackbar.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                }
            }
        }
    }

    private fun getInitials(name: String?): String {
        if (name.isNullOrBlank()) return "?"
        return name.trim().split(" ")
            .filter { it.isNotEmpty() }
            .map { it[0].uppercaseChar() }
            .take(2)
            .joinToString("")
    }

    private fun showSignOutConfirmationDialog() {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_sign_out_confirmation, null)

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setCancelable(true)
            .create()

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        dialogView.findViewById<TextView>(R.id.tv_cancel_sign_out).setOnClickListener {
            dialog.dismiss()
        }

        dialogView.findViewById<TextView>(R.id.tv_confirm_sign_out).setOnClickListener {
            dialog.dismiss()
            viewModel.signOut()
        }

        dialog.show()
    }
}
package com.example.nimons360.ui.profile

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.bumptech.glide.Glide
import com.bumptech.glide.signature.ObjectKey
import com.example.nimons360.R
import com.example.nimons360.ui.auth.login.LoginActivity
import com.example.nimons360.ui.profile.components.EditNameBottomSheet
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ProfileFragment : Fragment(R.layout.fragment_profile) {

    private val viewModel: ProfileViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val tvAvatar = view.findViewById<TextView>(R.id.tv_avatar)
        val ivAvatar = view.findViewById<ImageView>(R.id.iv_avatar)
        val tvName = view.findViewById<TextView>(R.id.tv_name)
        val tvEmail = view.findViewById<TextView>(R.id.tv_email)
        val btnSignOut = view.findViewById<TextView>(R.id.btn_sign_out)
        val btnEdit = view.findViewById<ImageButton>(R.id.btn_edit)

        val switchNotification = view.findViewById<com.google.android.material.switchmaterial.SwitchMaterial>(R.id.switch_notification)
        val switchLocation = view.findViewById<com.google.android.material.switchmaterial.SwitchMaterial>(R.id.switch_location)

        btnEdit.setOnClickListener {
            val existing = childFragmentManager.findFragmentByTag("EditNameBottomSheet")
            if (existing == null) {
                EditNameBottomSheet.newInstance().show(childFragmentManager, "EditNameBottomSheet")
            }
        }

        btnSignOut.setOnClickListener {
            showSignOutConfirmationDialog()
        }

        observeViewModel(tvAvatar, ivAvatar, tvName, tvEmail, switchNotification, switchLocation)
    }

    private fun observeViewModel(
        tvAvatar: TextView,
        ivAvatar: ImageView,
        tvName: TextView,
        tvEmail: TextView,
        switchNotification: com.google.android.material.switchmaterial.SwitchMaterial,
        switchLocation: com.google.android.material.switchmaterial.SwitchMaterial
    ) {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.user.combine(viewModel.profileUpdatedAt) { user, updatedAt ->
                        user to updatedAt
                    }.collect { (user, updatedAt) ->
                        user ?: return@collect
                        tvName.text = user.fullName ?: "-"
                        tvEmail.text = user.email ?: "-"

                        val imageUrl = if (user.profileImageUrl?.startsWith("http") == true) {
                            user.profileImageUrl
                        } else if (!user.profileImageUrl.isNullOrBlank()) {
                            "${com.example.nimons360.utils.Constants.BASE_URL}${user.profileImageUrl}"
                        } else {
                            null
                        }

                        if (imageUrl != null) {
                            tvAvatar.visibility = View.GONE
                            ivAvatar.visibility = View.VISIBLE
                            Glide.with(this@ProfileFragment)
                                .load(imageUrl)
                                .circleCrop()
                                .skipMemoryCache(true)
                                .diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.NONE)
                                .signature(ObjectKey("$imageUrl-$updatedAt"))
                                .placeholder(R.drawable.profile_bg_avatar_blue)
                                .error(R.drawable.profile_bg_avatar_blue)
                                .into(ivAvatar)
                        } else {
                            tvAvatar.visibility = View.VISIBLE
                            ivAvatar.visibility = View.GONE
                            tvAvatar.text = getInitials(user.fullName)
                        }
                    }
                }

                launch {
                    viewModel.isNotificationEnabled.collect { enabled ->
                        switchNotification.setOnCheckedChangeListener(null)
                        switchNotification.isChecked = enabled
                        switchNotification.setOnCheckedChangeListener { _, isChecked ->
                            viewModel.toggleNotification(isChecked)
                        }
                    }
                }

                launch {
                    viewModel.isLocationSharingEnabled.collect { enabled ->
                        switchLocation.setOnCheckedChangeListener(null)
                        switchLocation.isChecked = enabled
                        switchLocation.setOnCheckedChangeListener { _, isChecked ->
                            viewModel.toggleLocationSharing(isChecked)
                        }
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
                            is ProfileEvent.PhotoUploaded -> {
                                Snackbar.make(
                                    requireView(),
                                    "Photo uploaded",
                                    Snackbar.LENGTH_SHORT
                                ).show()
                            }
                            is ProfileEvent.Error -> {
                                Snackbar.make(
                                    requireView(),
                                    event.message,
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

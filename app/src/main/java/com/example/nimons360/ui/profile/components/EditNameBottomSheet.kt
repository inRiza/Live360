package com.example.nimons360.ui.profile.components

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.viewModels
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.nimons360.R
import com.example.nimons360.ui.profile.ProfileViewModel
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.io.File

class EditNameBottomSheet : BottomSheetDialogFragment() {

	private val viewModel: ProfileViewModel by viewModels(ownerProducer = { requireParentFragment() })
	private var cameraPhotoUri: Uri? = null

	private val pickImageLauncher = registerForActivityResult(
		ActivityResultContracts.GetContent()
	) { uri: Uri? ->
		uri?.let { handleSelectedUri(it) }
	}

	private val takePictureLauncher = registerForActivityResult(
		ActivityResultContracts.TakePicture()
	) { success: Boolean ->
		if (success) {
			cameraPhotoUri?.let { handleSelectedUri(it) }
		}
	}

	private val requestCameraPermissionLauncher = registerForActivityResult(
		ActivityResultContracts.RequestPermission()
	) { granted: Boolean ->
		if (granted) {
			launchCamera()
		} else {
			Toast.makeText(requireContext(), "Izin kamera diperlukan untuk mengambil foto", Toast.LENGTH_SHORT).show()
		}
	}

	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		savedInstanceState: Bundle?
	): View {
		return inflater.inflate(R.layout.bottomsheet_edit_name, container, false)
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)

		val tilName = view.findViewById<TextInputLayout>(R.id.til_edit_name)
		val editName = view.findViewById<TextInputEditText>(R.id.edit_name)
		val btnSave = view.findViewById<Button>(R.id.btnSave)
		val btnCancel = view.findViewById<Button>(R.id.btnCancel)

		val tvDialogAvatar = view.findViewById<TextView>(R.id.tv_dialog_avatar)
		val ivDialogAvatar = view.findViewById<ImageView>(R.id.iv_dialog_avatar)
		val pbDialogUpload = view.findViewById<ProgressBar>(R.id.pb_dialog_upload)
		val btnChangePhoto = view.findViewById<ImageButton>(R.id.btn_change_photo)

		btnCancel.setOnClickListener {
			dismiss()
		}

		btnSave.setOnClickListener {
			val name = editName.text.toString().trim()
			if (name.isEmpty()) {
				tilName?.error = "Name cannot be empty"
				editName.requestFocus()
				return@setOnClickListener
			}
			tilName?.error = null
			viewModel.updateName(name)
			dismiss()
		}

		btnChangePhoto.setOnClickListener {
			val options = arrayOf("Ambil Foto (Kamera)", "Pilih dari Galeri")
			com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
				.setTitle("Ganti Foto Profil")
				.setItems(options) { dialog, which ->
					when (which) {
						0 -> openCamera()
						1 -> openGallery()
					}
					dialog.dismiss()
				}
				.show()
		}

		viewLifecycleOwner.lifecycleScope.launch {
			viewLifecycleOwner.lifecycle.repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
				launch {
					viewModel.user.combine(viewModel.profileUpdatedAt) { user, updatedAt ->
						user to updatedAt
					}.collect { (user, updatedAt) ->
						if (user != null) {
							if (editName.text.isNullOrEmpty()) {
								editName.setText(user.fullName)
							}

							val imageUrl = if (user.profileImageUrl?.startsWith("http") == true) {
								user.profileImageUrl
							} else if (!user.profileImageUrl.isNullOrBlank()) {
								"${com.example.nimons360.utils.Constants.BASE_URL}${user.profileImageUrl}"
							} else {
								null
							}

							if (imageUrl != null) {
								tvDialogAvatar.visibility = View.GONE
								ivDialogAvatar.visibility = View.VISIBLE
								Glide.with(this@EditNameBottomSheet)
									.load(imageUrl)
									.circleCrop()
									.skipMemoryCache(true)
									.diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.NONE)
									.signature(com.bumptech.glide.signature.ObjectKey("$imageUrl-$updatedAt"))
									.placeholder(R.drawable.profile_bg_avatar_blue)
									.into(ivDialogAvatar)
							} else {
								tvDialogAvatar.visibility = View.VISIBLE
								ivDialogAvatar.visibility = View.GONE
								tvDialogAvatar.text = getInitials(user.fullName)
							}
						}
					}
				}

				launch {
					viewModel.uploadState.collect { state ->
						when (state) {
							is com.example.nimons360.utils.Result.Loading -> {
								pbDialogUpload.visibility = View.VISIBLE
								btnChangePhoto.isEnabled = false
								btnSave.isEnabled = false
							}
							is com.example.nimons360.utils.Result.Success -> {
								pbDialogUpload.visibility = View.GONE
								btnChangePhoto.isEnabled = true
								btnSave.isEnabled = true
								Toast.makeText(requireContext(), "Foto profil berhasil diperbarui", Toast.LENGTH_SHORT).show()
							}
							is com.example.nimons360.utils.Result.Error -> {
								pbDialogUpload.visibility = View.GONE
								btnChangePhoto.isEnabled = true
								btnSave.isEnabled = true
								Toast.makeText(requireContext(), "Gagal mengunggah foto: ${state.message}", Toast.LENGTH_SHORT).show()
							}
							else -> {
								pbDialogUpload.visibility = View.GONE
								btnChangePhoto.isEnabled = true
								btnSave.isEnabled = true
							}
                        }
					}
				}
			}
		}
	}

	private fun openCamera() {
		val context = requireContext()
		if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
			== PackageManager.PERMISSION_GRANTED
		) {
			launchCamera()
		} else {
			requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
		}
	}

	private fun launchCamera() {
		val context = requireContext()
		val cacheFile = File(context.cacheDir, "camera_photo_${System.currentTimeMillis()}.jpg")
		try {
			val authority = "${context.packageName}.provider"
			cameraPhotoUri = FileProvider.getUriForFile(context, authority, cacheFile)
			takePictureLauncher.launch(cameraPhotoUri!!)
		} catch (e: Exception) {
			Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
		}
	}

	private fun openGallery() {
		pickImageLauncher.launch("image/*")
	}

	private fun handleSelectedUri(uri: Uri) {
		val context = requireContext()
		viewLifecycleOwner.lifecycleScope.launch {
			try {
				val file = uriToCompressedFile(uri, context)
				viewModel.uploadPhoto(file)
			} catch (e: Exception) {
				Toast.makeText(context, "Gagal memproses gambar: ${e.message}", Toast.LENGTH_SHORT).show()
			}
		}
	}

	private fun uriToFile(uri: Uri, context: Context): File {
		val contentResolver = context.contentResolver
		val tempFile = File(context.cacheDir, "temp_profile_${System.currentTimeMillis()}.jpg")
		contentResolver.openInputStream(uri)?.use { inputStream ->
			tempFile.outputStream().use { outputStream ->
				inputStream.copyTo(outputStream)
			}
		} ?: throw IllegalArgumentException("Tidak dapat membaca data gambar")
		return tempFile
	}

	private fun uriToCompressedFile(uri: Uri, context: Context): File {
		val contentResolver = context.contentResolver
		val tempFile = File(context.cacheDir, "upload_profile_${System.currentTimeMillis()}.jpg")

		// Decode, scale down jika terlalu besar, lalu compress ke JPEG
		val originalBitmap = contentResolver.openInputStream(uri)?.use { inputStream ->
			android.graphics.BitmapFactory.decodeStream(inputStream)
		} ?: throw IllegalArgumentException("Tidak dapat membaca data gambar")

		val maxDimen = 1024
		val scaled = if (originalBitmap.width > maxDimen || originalBitmap.height > maxDimen) {
			val ratio = maxDimen.toFloat() / maxOf(originalBitmap.width, originalBitmap.height)
			val w = (originalBitmap.width * ratio).toInt()
			val h = (originalBitmap.height * ratio).toInt()
			val s = android.graphics.Bitmap.createScaledBitmap(originalBitmap, w, h, true)
			originalBitmap.recycle()
			s
		} else {
			originalBitmap
		}

		tempFile.outputStream().use { out ->
			scaled.compress(android.graphics.Bitmap.CompressFormat.JPEG, 85, out)
		}
		scaled.recycle()

		return tempFile
	}

	private fun getInitials(name: String?): String {
		if (name.isNullOrBlank()) return "?"
		return name.trim().split(" ")
			.filter { it.isNotEmpty() }
			.map { it[0].uppercaseChar() }
			.take(2)
			.joinToString("")
	}

	companion object {
		fun newInstance(): EditNameBottomSheet {
			return EditNameBottomSheet()
		}
	}
}
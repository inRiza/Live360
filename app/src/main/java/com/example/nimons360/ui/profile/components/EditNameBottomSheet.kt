package com.example.nimons360.ui.profile.components

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import com.example.nimons360.R
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class EditNameBottomSheet : BottomSheetDialogFragment() {

	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		savedInstanceState: Bundle?
	): View {
		return inflater.inflate(R.layout.bottomsheet_edit_name, container, false)
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)

		val editName = view.findViewById<EditText>(R.id.edit_name)
		val btnSave = view.findViewById<Button>(R.id.btnSave)
		val btnCancel = view.findViewById<Button>(R.id.btnCancel)

		editName.setText(arguments?.getString(ARG_NAME).orEmpty())

		btnSave.setOnClickListener {
			val name = editName.text.toString().trim()

			if (name.isEmpty()) {
				editName.error = "Nama tidak boleh kosong"
				editName.requestFocus()
				return@setOnClickListener
			}
			Toast.makeText(requireContext(), "Save clicked!", Toast.LENGTH_SHORT).show()
			dismiss()
		}

		btnCancel.setOnClickListener {
			Toast.makeText(requireContext(), "Cancel clicked!", Toast.LENGTH_SHORT).show()
			dismiss()
		}
	}

	companion object {
		private const val ARG_NAME = "arg_name"

		fun newInstance(currentName: String): EditNameBottomSheet {
			val sheet = EditNameBottomSheet()
			sheet.arguments = Bundle().apply {
				putString(ARG_NAME, currentName)
			}
			return sheet
		}
	}
}

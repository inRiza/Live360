package com.example.nimons360.ui.profile.components

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.example.nimons360.R

class EditNameBottomSheet : BottomSheetDialogFragment() {

	private var onSave: ((String) -> Unit)? = null

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

		editName.setText(arguments?.getString(ARG_NAME).orEmpty())

		btnSave.setOnClickListener {
			val name = editName.text.toString().trim()
			if (name.isEmpty()) {
				tilName?.error = "Name cannot be empty"
				editName.requestFocus()
				return@setOnClickListener
			}
			tilName?.error = null
			onSave?.invoke(name)
			dismiss()
		}

		btnCancel.setOnClickListener {
			dismiss()
		}
	}

	companion object {
		private const val ARG_NAME = "arg_name"

		fun newInstance(currentName: String, onSave: (String) -> Unit): EditNameBottomSheet {
			return EditNameBottomSheet().apply {
				arguments = Bundle().apply { putString(ARG_NAME, currentName) }
				this.onSave = onSave
			}
		}
	}
}
package com.pinboard.keyboard.ui

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.pinboard.keyboard.R
import com.pinboard.keyboard.data.Pin
import com.pinboard.keyboard.databinding.DialogEditPinBinding
import com.pinboard.keyboard.util.DEFAULT_CATEGORY

class EditPinDialog(
    private val existingPin: Pin? = null,
    private val onSave: (Pin) -> Unit
) : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val binding = DialogEditPinBinding.inflate(layoutInflater)

        existingPin?.let { pin ->
            binding.editTitle.setText(pin.title)
            binding.editText.setText(pin.text)
            binding.editCategory.setText(pin.category)
            binding.checkFavorite.isChecked = pin.isFavorite
        }

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(if (existingPin == null) R.string.new_pin else R.string.edit_pin)
            .setView(binding.root)
            .setPositiveButton(R.string.save, null)
            .setNegativeButton(R.string.cancel, null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(Dialog.BUTTON_POSITIVE).setOnClickListener {
                val title = binding.editTitle.text?.toString()?.trim().orEmpty()
                val text = binding.editText.text?.toString()?.trim().orEmpty()
                val category = binding.editCategory.text?.toString()?.trim()
                    .takeUnless { it.isNullOrBlank() } ?: DEFAULT_CATEGORY

                if (title.isEmpty()) {
                    binding.layoutTitle.error = getString(R.string.empty_title_error)
                    return@setOnClickListener
                }
                if (text.isEmpty()) {
                    binding.layoutText.error = getString(R.string.empty_text_error)
                    return@setOnClickListener
                }

                val pin = (existingPin ?: Pin(title = title, text = text)).copy(
                    title = title,
                    text = text,
                    category = category,
                    isFavorite = binding.checkFavorite.isChecked
                )
                onSave(pin)
                dismiss()
            }
        }

        return dialog
    }
}

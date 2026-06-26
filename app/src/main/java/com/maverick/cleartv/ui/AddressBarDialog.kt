package com.maverick.cleartv.ui

import android.app.Activity
import android.app.AlertDialog
import android.view.LayoutInflater
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import com.maverick.cleartv.R

object AddressBarDialog {

    fun show(activity: Activity, currentUrl: String, onNavigate: (String) -> Unit) {
        val view = LayoutInflater.from(activity).inflate(R.layout.dialog_address_bar, null)
        val editText = view.findViewById<EditText>(R.id.address_input)

        editText.setText(currentUrl)
        editText.selectAll()

        val dialog = AlertDialog.Builder(activity, R.style.AddressBarDialog)
            .setView(view)
            .create()

        editText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_GO || actionId == EditorInfo.IME_ACTION_DONE) {
                val input = editText.text.toString().trim()
                if (input.isNotEmpty()) {
                    onNavigate(input)
                    dialog.dismiss()
                }
                true
            } else false
        }

        dialog.show()
        editText.requestFocus()
    }
}

package com.maverick.cleartv.ui

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
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

        // Force keyboard to appear when dialog opens
        dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)

        fun navigate() {
            val input = editText.text.toString().trim()
            if (input.isNotEmpty()) {
                onNavigate(input)
                dialog.dismiss()
            }
        }

        // IME action (soft keyboard Go/Done button)
        editText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_GO || actionId == EditorInfo.IME_ACTION_DONE) {
                navigate(); true
            } else false
        }

        // Physical keyboard Enter or D-pad center
        editText.setOnKeyListener { _, keyCode, event ->
            if (event.action == KeyEvent.ACTION_DOWN &&
                (keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER)) {
                navigate(); true
            } else false
        }

        dialog.show()
        editText.requestFocus()

        // Explicitly show IME after layout pass
        val imm = activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        editText.post {
            imm.showSoftInput(editText, InputMethodManager.SHOW_FORCED)
        }
    }
}

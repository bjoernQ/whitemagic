package de.mobilej.ktest

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v7.app.AlertDialog
import android.view.LayoutInflater
import android.widget.EditText

class InputDialogFragment : DialogFragment() {

    interface OnResult {
        fun onResult(result: String)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val inflater = LayoutInflater.from(context)
        val editText = inflater.inflate(R.layout.edittext, null)

        return AlertDialog.Builder(context)
                .setView(editText)
                .setPositiveButton("Ok") { _: DialogInterface, i: Int ->
                    val theActivity = activity
                    if (theActivity is OnResult) {
                        theActivity.onResult((editText.findViewById<EditText>(R.id.edit_text) as EditText).text.toString())
                    }
                }
                .create()
    }
}
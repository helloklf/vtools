package com.omarea.vtools.dialogs

import android.app.AlertDialog
import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import com.omarea.common.ui.DialogHelper
import com.omarea.vtools.R
import java.lang.Exception
import kotlin.math.min

class DialogNumberInput(private val context: Context) {
    interface DialogNumberInputRequest {
        var min: Int
        var max: Int
        var default: Int

        fun onApply(value: Int)
    }

    fun showDialog(dialogRequest: DialogNumberInputRequest) {
        var alertDialog: AlertDialog? = null
        val dialog = LayoutInflater.from(context).inflate(R.layout.dialog_number_input, null)
        val value = dialog.findViewById<TextView>(R.id.number_input_value)
        var current = dialogRequest.default

        dialog.findViewById<ImageButton>(R.id.number_input_minus).setOnClickListener {
            if (current > dialogRequest.min) {
                current-=1
            }
            value.setText(current.toString())
        }
        dialog.findViewById<ImageButton>(R.id.number_input_plus).setOnClickListener {
            if (current < dialogRequest.max) {
                current += 1
            }
            value.setText(current.toString())
        }

        dialog.findViewById<Button>(R.id.number_input_applay).setOnClickListener {
            alertDialog?.dismiss()
            dialogRequest.onApply(current)
        }

        dialog.findViewById<Button>(R.id.number_input_cancel).setOnClickListener {
            alertDialog?.dismiss()
        }

        value.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                if (Regex("^[-0-9]{1,10}").matches(value.text.toString())) {
                    try {
                        val intValue = value.text.toString().toInt()
                        if (intValue >= dialogRequest.min && intValue <= dialogRequest.max) {
                            current = intValue
                            return
                        }
                    } catch (ex: Exception) {
                    }
                }
                value.text = current.toString()
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

            }
        })

        value.setText(dialogRequest.default.toString())
        alertDialog = DialogHelper.animDialog(AlertDialog.Builder(context).setView(dialog))
    }
}
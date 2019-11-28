package com.omarea.krscript.ui

import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import com.omarea.common.ui.DialogHelper
import com.omarea.krscript.R
import com.omarea.krscript.model.ActionParamInfo

class ParamsColorPicker(private val actionParamInfo: ActionParamInfo, private val context: Context) {
    fun render(): View {
        val layout = LayoutInflater.from(context).inflate(R.layout.kr_param_color, null)
        val textView = layout.findViewById<EditText>(R.id.kr_param_color_text)
        val invalidView = layout.findViewById<ImageView>(R.id.kr_param_color_invalid)
        val preview = layout.findViewById<View>(R.id.kr_param_color_preview)
        textView.tag = actionParamInfo.name
        textView.addTextChangedListener(object : TextWatcher {
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun afterTextChanged(s: Editable?) {
                updateColorPreview(textView, invalidView, preview, s!!.toString())
            }
        })
        if (actionParamInfo.valueFromShell != null) {
            textView.setText(actionParamInfo.valueFromShell!!)
        } else if (actionParamInfo.value != null) {
            textView.setText(actionParamInfo.value!!)
        }

        updateColorPreview(textView, invalidView, preview, textView.text.toString())
        layout.findViewById<View>(R.id.kr_param_color_picker).setOnClickListener {
            openColorPicker(textView, invalidView, preview)
        }

        return layout
    }

    private fun updateColorPreview(textView: TextView, invalidView: ImageView, preview: View, colorStr: String): Boolean {
        try {
            val color = Color.parseColor(colorStr)
            // textView.setBackgroundColor(Color.TRANSPARENT)
            invalidView.visibility = View.GONE
            preview.visibility = View.VISIBLE
            preview.background = ColorDrawable(color)
            return true
        } catch (ex: Exception) {
            // textView.setBackgroundColor(Color.RED)
            invalidView.visibility = View.VISIBLE
            preview.visibility = View.GONE
            return false
        }
    }

    private fun currentColor(colorStr: CharSequence?): Int {
        if (colorStr != null && colorStr.isNotEmpty()) {
            try {
                return Color.parseColor(colorStr.toString())
            } catch (ex: Exception) {
            }
        }
        return (0xff000000).toInt()
    }

    private fun openColorPicker(textView: TextView, invalidView: ImageView, preview: View) {
        val view = LayoutInflater.from(context).inflate(R.layout.kr_color_picker, null)
        val defValue = currentColor(textView.text)

        val alphaBar = view.findViewById<SeekBar>(R.id.color_alpha)
        val redBar = view.findViewById<SeekBar>(R.id.color_red)
        val greenBar = view.findViewById<SeekBar>(R.id.color_green)
        val blueBar = view.findViewById<SeekBar>(R.id.color_blue)
        val colorPreview = view.findViewById<Button>(R.id.color_preview)
        val colorPreviewText = view.findViewById<TextView>(R.id.color_preview_text)

        alphaBar.progress = Color.alpha(defValue)
        redBar.progress = Color.red(defValue)
        greenBar.progress = Color.green(defValue)
        blueBar.progress = Color.blue(defValue)
        colorPreview.setBackgroundColor(defValue)
        colorPreviewText.text = parseHexStr(alphaBar.progress, redBar.progress, greenBar.progress, blueBar.progress)

        val listener = object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                val color = Color.argb(alphaBar.progress, redBar.progress, greenBar.progress, blueBar.progress)
                colorPreview.setBackgroundColor(color)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {}

            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        }
        alphaBar.setOnSeekBarChangeListener(listener)
        redBar.setOnSeekBarChangeListener(listener)
        greenBar.setOnSeekBarChangeListener(listener)
        blueBar.setOnSeekBarChangeListener(listener)

        DialogHelper.animDialog(AlertDialog.Builder(context)
                .setTitle(context.getString(R.string.kr_color_picker))
                .setView(view)
                .setPositiveButton(context.getString(R.string.btn_confirm)) { _, which ->
                    val color = Color.argb(alphaBar.progress, redBar.progress, greenBar.progress, blueBar.progress)
                    colorPreview.setBackgroundColor(color)
                    try {
                        textView.text = parseHexStr(alphaBar.progress, redBar.progress, greenBar.progress, blueBar.progress)
                        invalidView.visibility = View.GONE
                        preview.background = ColorDrawable(color)
                    } catch (ex: Exception) {
                    }
                    // Integer.toHexString(color) // "argb(${alphaBar.progress}, ${redBar.progress}, ${greenBar.progress}, ${blueBar.progress}, )"
                }
                .setNegativeButton(context.getString(R.string.btn_cancel)) { _, _ -> })
    }

    private fun parseHexStr(a: Int, r: Int, g: Int, b: Int): String {
        return String.format("#%02x%02x%02x%02x", a, r, g, b)
    }
}

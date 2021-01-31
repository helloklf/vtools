package com.omarea.krscript.ui

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageButton
import android.widget.SeekBar
import android.widget.TextView
import com.omarea.krscript.R
import com.omarea.krscript.model.ActionParamInfo

class ParamsSeekBar(private var actionParamInfo: ActionParamInfo, private var context: Context) {
    fun render(): View {
        val layout = LayoutInflater.from(context).inflate(R.layout.kr_param_seekbar, null)
        val seekbar = layout.findViewById<SeekBar>(R.id.kr_param_seekbar)

        seekbar.max = actionParamInfo.max
        seekbar.max = actionParamInfo.max - actionParamInfo.min

        try {
            if (actionParamInfo.valueFromShell != null)
                seekbar.progress = (actionParamInfo.valueFromShell)!!.toInt() - actionParamInfo.min
            else if (actionParamInfo.value != null)
                seekbar.progress = (actionParamInfo.value)!!.toInt() - actionParamInfo.min
        } catch (ex: Exception) {
        }

        seekbar.tag = actionParamInfo.name

        val minus = layout.findViewById<ImageButton>(R.id.kr_param_seekbar_minus)
        val plus = layout.findViewById<ImageButton>(R.id.kr_param_seekbar_plus)
        val textView = layout.findViewById<TextView>(R.id.kr_param_seekbar_value)
        textView.text = (seekbar.progress + actionParamInfo.min).toString()
        seekbar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                textView.text = (progress + actionParamInfo.min).toString()
            }
        })
        minus.setOnClickListener {
            if (seekbar.progress > 0) {
                seekbar.progress -= 1
            }
        }
        plus.setOnClickListener {
            if (seekbar.progress < seekbar.max) {
                seekbar.progress += 1
            }
        }

        return layout
    }
}

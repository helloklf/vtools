package com.omarea.krscript.ui

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import com.omarea.krscript.R
import com.omarea.krscript.model.ActionParamInfo

class ParamsFileChooserRender(private var actionParamInfo: ActionParamInfo, private var context: Context, private var fileChooser: FileChooserInterface?) {
    interface FileChooserInterface {
        fun openFileChooser(fileSelectedInterface: FileSelectedInterface): Boolean
    }

    interface FileSelectedInterface {
        companion object {
            val TYPE_FILE: Int
                get() = 0
            val TYPE_FOLDER: Int
                get() = 1
        }

        fun onFileSelected(path: String?)
        fun mimeType():String?
        fun suffix():String?
        fun type(): Int
    }

    fun render(): View {
        val layout = LayoutInflater.from(context).inflate(R.layout.kr_param_file, null)
        val textView = layout.findViewById<TextView>(R.id.kr_param_file_text)
        val pathView = layout.findViewById<TextView>(R.id.kr_param_file_path)
        val btn = layout.findViewById<ImageButton>(R.id.kr_param_file_btn)

        btn.setOnClickListener {
            fileChooser?.openFileChooser(object : FileSelectedInterface {
                override fun onFileSelected(path: String?) {
                    if (path.isNullOrEmpty()) {
                        if (type() == FileSelectedInterface.TYPE_FOLDER) {
                            textView.text = context.getString(R.string.kr_please_choose_folder)
                        } else {
                            textView.text = context.getString(R.string.kr_please_choose_file)
                        }
                        pathView.text = ""
                    } else {
                        textView.text = path
                        pathView.text = path
                    }
                }

                override fun mimeType(): String? {
                    if (actionParamInfo.mime.isNotEmpty()) {
                        return actionParamInfo.mime
                    }
                    return null
                }

                override fun suffix(): String? {
                    if (actionParamInfo.suffix.isNotEmpty()) {
                        return actionParamInfo.suffix
                    }
                    return null
                }

                override fun type(): Int {
                    return when(actionParamInfo.type) {
                        "folder" -> FileSelectedInterface.TYPE_FOLDER
                        else -> FileSelectedInterface.TYPE_FILE
                    }
                }
            })
        }

        if (actionParamInfo.valueFromShell != null) {
            textView.text = actionParamInfo.valueFromShell
            pathView.text = actionParamInfo.valueFromShell
        } else if (!actionParamInfo.value.isNullOrEmpty()) {
            textView.text = actionParamInfo.value
            pathView.text = actionParamInfo.value
        }

        pathView.tag = actionParamInfo.name

        return layout
    }
}

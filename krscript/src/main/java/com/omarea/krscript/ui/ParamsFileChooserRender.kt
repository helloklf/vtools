package com.omarea.krscript.ui

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
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


    fun setEditTextReadOnly(view: TextView) {
        // view.setTextColor(R.color.read_only_color) //设置只读时的文字颜色
        if (view is EditText) {
            view.setCursorVisible(false) //设置输入框中的光标不可见
            view.setFocusable(false) //无焦点
            view.setFocusableInTouchMode(false) //触摸时也得不到焦点
        }
    }

    fun render(): View {
        val layout = LayoutInflater.from(context).inflate(R.layout.kr_param_file, null)
        val textView = layout.findViewById<TextView>(R.id.kr_param_file_text)
        val pathView = layout.findViewById<EditText>(R.id.kr_param_file_path)
        val btn = layout.findViewById<View>(R.id.kr_param_file_btn)

        if (actionParamInfo.editable) {
            textView.visibility = View.GONE
            pathView.visibility = View.VISIBLE
            pathView.hint = if (actionParamInfo.type == "folder") {
                context.getString(R.string.kr_please_choose_folder)
            } else {
                context.getString(R.string.kr_please_choose_file)
            }
        } else {
            setEditTextReadOnly(pathView)
        }

        btn.setOnClickListener {
            fileChooser?.openFileChooser(object : FileSelectedInterface {
                override fun onFileSelected(path: String?) {
                    if (path.isNullOrEmpty()) {
                        if (type() == FileSelectedInterface.TYPE_FOLDER) {
                            textView.text = context.getString(R.string.kr_please_choose_folder)
                        } else {
                            textView.text = context.getString(R.string.kr_please_choose_file)
                        }
                        pathView.setText("")
                    } else {
                        textView.text = path
                        pathView.setText(path)
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
            pathView.setText(actionParamInfo.valueFromShell)
        } else if (!actionParamInfo.value.isNullOrEmpty()) {
            textView.text = actionParamInfo.value
            pathView.setText(actionParamInfo.value)
        }

        pathView.tag = actionParamInfo.name

        return layout
    }
}

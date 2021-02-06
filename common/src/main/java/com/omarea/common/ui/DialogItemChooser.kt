package com.omarea.common.ui

import android.content.DialogInterface
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.AbsListView
import android.widget.EditText
import android.widget.Filterable
import com.omarea.common.R
import com.omarea.common.model.SelectItem

class DialogItemChooser(
        private val darkMode: Boolean,
        private var items: ArrayList<SelectItem>,
        private val multiple: Boolean = false,
        private var callback: Callback? = null) : DialogFullScreen(R.layout.dialog_item_chooser, darkMode) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val absListView = view.findViewById<AbsListView>(R.id.item_list)
        setup(absListView)

        view.findViewById<View>(R.id.btn_cancel).setOnClickListener {
            dismiss()
        }
        view.findViewById<View>(R.id.btn_confirm).setOnClickListener {
            this.onConfirm(absListView)
        }
        val clearBtn = view.findViewById<View>(R.id.search_box_clear)
        val searchBox = view.findViewById<EditText>(R.id.search_box).apply {
            addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    if (s != null) {
                        clearBtn.visibility = if (s.length > 0) View.VISIBLE else View.GONE
                    }
                }
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    (absListView.adapter as Filterable).getFilter().filter(if (s == null) "" else s.toString())
                }
            })
        }
        clearBtn.visibility = if (searchBox.text.isNullOrEmpty()) View.GONE else View.VISIBLE
        clearBtn.setOnClickListener {
            searchBox.text = null
        }
    }

    private fun setup(gridView: AbsListView) {
        gridView.adapter = AdapterItemChooser(gridView.context, items, multiple)
    }

    interface Callback {
        fun onConfirm(selected: List<SelectItem>, status: BooleanArray)
    }

    private fun onConfirm(gridView: AbsListView) {
        val adapter = (gridView.adapter as AdapterItemChooser)
        val items = adapter.getSelectedItems()
        val status = adapter.getSelectStatus()

        callback?.onConfirm(items, status)

        this.dismiss()
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
    }
}

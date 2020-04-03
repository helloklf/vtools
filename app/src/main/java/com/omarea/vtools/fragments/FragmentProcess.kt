package com.omarea.vtools.fragments

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.EditText
import com.omarea.shell_utils.ProcessUtils
import com.omarea.ui.ProcessAdapter
import com.omarea.vtools.R
import kotlinx.android.synthetic.main.fragment_process.*
import kotlinx.android.synthetic.main.nav_item.*

class FragmentProcess : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? =
            inflater.inflate(R.layout.fragment_process, container, false)

    private val processUtils = ProcessUtils()
    private val supported = processUtils.supported()
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        if (supported) {
            process_unsupported.visibility = View.GONE
            process_view.visibility = View.VISIBLE
        } else {
            process_unsupported.visibility = View.VISIBLE
            process_view.visibility = View.GONE
        }

        if (supported) {
            process_list.adapter = ProcessAdapter(this.context!!, processUtils.allProcess, "")
        }

        // 搜索关键字
        process_search.addTextChangedListener(object : TextWatcher{
            override fun afterTextChanged(s: Editable?) {
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                s?.run {
                    (process_list.adapter as ProcessAdapter?)?.updateKeywords(s.toString())
                }
            }
        })

        // 排序方式
        process_sort_mode.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {
            }
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                (process_list.adapter as ProcessAdapter?)?.updateSortMode(when(position) {
                    0 -> ProcessAdapter.SORT_MODE_CPU
                    1 -> ProcessAdapter.SORT_MODE_MEM
                    2 -> ProcessAdapter.SORT_MODE_PID
                    else -> ProcessAdapter.SORT_MODE_DEFAULT
                })
            }
        }

        // 过滤筛选
        process_filter.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {
            }
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                (process_list.adapter as ProcessAdapter?)?.updateFilterMode(when(position) {
                    0 -> ProcessAdapter.FILTER_USER
                    1 -> ProcessAdapter.FILTER_KERNEL
                    else -> ProcessAdapter.FILTER_ALL
                })
            }
        }
    }

    override fun onResume() {
        super.onResume()
        activity!!.title = getString(R.string.menu_processes)
    }
}

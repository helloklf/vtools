package com.omarea.vtools.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
        } else {
            process_unsupported.visibility = View.VISIBLE
        }
    }

    override fun onResume() {
        super.onResume()
        activity!!.title = getString(R.string.menu_processes)

        if (supported) {
            process_list.adapter = ProcessAdapter(this.context!!, processUtils.allProcess, "")
        }
    }
}

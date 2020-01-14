package com.omarea.vtools.fragments

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.omarea.permissions.CheckRootStatus
import com.omarea.vtools.R
import kotlinx.android.synthetic.main.fragment_not_root.*


class FragmentCpuModes : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? =
            inflater.inflate(R.layout.fragment_cpu_modes, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    }

    companion object {
        fun createPage(): Fragment {
            val fragment = FragmentCpuModes()
            return fragment
        }
    }
}

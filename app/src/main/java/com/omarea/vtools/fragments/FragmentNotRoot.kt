package com.omarea.vtools.fragments

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.os.StatFs
import android.support.design.widget.Snackbar
import android.support.v4.app.Fragment
import android.support.v7.app.AlertDialog
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.SimpleAdapter
import android.widget.Toast
import com.omarea.shared.CommonCmds
import com.omarea.shell.CheckRootStatus
import com.omarea.shell.units.BackupRestoreUnit
import com.omarea.vtools.R
import com.omarea.vtools.activitys.ActivityFileSelector
import kotlinx.android.synthetic.main.fragment_img.*
import kotlinx.android.synthetic.main.fragment_not_root.*
import java.io.File
import java.util.*


class FragmentNotRoot : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? =
            inflater.inflate(R.layout.fragment_not_root, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        btn_retry.setOnClickListener {
            CheckRootStatus(this.context!!, Runnable {
                if (this.activity != null) {
                    this.activity!!.recreate()
                }
            }, false, null).forceGetRoot()
        }
    }

    companion object {
        fun createPage(): Fragment {
            val fragment = FragmentNotRoot()
            return fragment
        }
    }
}

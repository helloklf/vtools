package com.omarea.vtools.fragments

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.support.design.widget.Snackbar
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.*
import android.widget.AdapterView.OnItemClickListener
import com.omarea.shared.AppListHelper
import com.omarea.shared.CommonCmds
import com.omarea.shared.SpfConfig
import com.omarea.shared.model.Appinfo
import com.omarea.shell.KeepShellPublic
import com.omarea.ui.AppListAdapter
import com.omarea.ui.OverScrollListView
import com.omarea.ui.ProgressBarDialog
import com.omarea.ui.SearchTextWatcher
import com.omarea.vtools.R
import com.omarea.vtools.dialogs.DialogAppOptions
import com.omarea.vtools.dialogs.DialogSingleAppOptions
import kotlinx.android.synthetic.main.layout_applictions.*
import java.lang.ref.WeakReference


class FragmentFreezer : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? =
            inflater.inflate(R.layout.layout_freezer, container, false)

    @SuppressLint("InflateParams")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        AlertDialog.Builder(this.context)
                .setTitle("关于冷库")
                .setMessage("将你不常用或不想后台运行的应用加入冷库，在你退出应用一段时间后，Scene将自动冻结它们。\n\n当然，在下次需要打开这些应用时，你可能只能要从Scene启动。")
                .setPositiveButton(R.string.btn_confirm, {
                    _, _ ->
                })
                .create()
                .show()

    }

    companion object {
        fun createPage(): Fragment {
            val fragment = FragmentFreezer()
            return fragment
        }
    }
}
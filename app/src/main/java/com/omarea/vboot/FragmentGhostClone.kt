package com.omarea.vboot

import android.content.Intent
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v4.app.Fragment
import android.support.v7.app.AlertDialog
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ProgressBar
import android.widget.SimpleAdapter
import android.widget.Toast
import com.omarea.shared.Consts
import com.omarea.shared.cmd_shellTools
import com.omarea.shell.units.BackupRestoreUnit
import kotlinx.android.synthetic.main.layout_img.*
import java.io.File
import java.util.*


class FragmentGhostClone : Fragment() {
    internal var thisview: ActivityMain? = null

    fun createItem(title: String, desc: String): HashMap<String, Any> {
        val item = HashMap<String, Any>()
        item.put("Title", title)
        item.put("Desc", desc)
        return item
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? =
            inflater!!.inflate(R.layout.layout_ghost_clone, container, false)

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        val file = File("/data/multiboot/data-slot-miui/data/app")
        if (!file.exists()) {
            Toast.makeText(context, "文件夹不存在", Toast.LENGTH_SHORT).show()
        } else if (file.canRead()) {
            Toast.makeText(context, "可以读取", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "不能读取", Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        fun createPage(thisView: ActivityMain): Fragment {
            val fragment = FragmentGhostClone()
            fragment.thisview = thisView
            return fragment
        }
    }
}

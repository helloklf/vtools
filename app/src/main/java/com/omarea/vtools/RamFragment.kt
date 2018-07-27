package com.omarea.vtools

import android.app.ActivityManager
import android.support.v4.app.Fragment
import android.content.Context
import android.content.Context.ACTIVITY_SERVICE
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.omarea.shell.KeepShellSync
import kotlinx.android.synthetic.main.fragment_ram.*


class RamFragment : Fragment() {
    private var myHandler = Handler()
    private var currentContext: Context? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_ram, container, false)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        this.currentContext = context
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        home_clear_ram.setOnClickListener {
            home_raminfo_text.text = "稍等一下"
            Thread(Runnable {
                KeepShellSync.doCmdSync("sync\n" +
                        "echo 3 > /proc/sys/vm/drop_caches")
                myHandler.postDelayed({
                    updateInfo()
                    Toast.makeText(context, "缓存已清理...", Toast.LENGTH_SHORT).show()
                }, 1000)
            }).start()
        }
        updateInfo()
    }

    private fun updateInfo() {
        val activityManager = currentContext!!.getSystemService(ACTIVITY_SERVICE) as ActivityManager
        val info = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(info)

        val totalMem = (info.totalMem / 1024 / 1024f).toInt()
        val availMem = (info.availMem / 1024 / 1024f).toInt()

        home_raminfo_text.text = "${availMem} / ${totalMem}MB"
        home_raminfo.setData(totalMem.toFloat(), availMem.toFloat())
    }


    override fun onDetach() {
        super.onDetach()
    }
}

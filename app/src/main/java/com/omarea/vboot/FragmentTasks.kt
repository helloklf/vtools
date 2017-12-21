package com.omarea.vboot

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
import android.widget.ListView
import android.widget.ProgressBar
import com.omarea.shell.SysUtils
import com.omarea.shell.units.TopTasksUnit
import com.omarea.ui.task_adapter
import kotlinx.android.synthetic.main.layout_task.*
import java.util.*
import kotlin.collections.LinkedHashMap

class FragmentTasks : Fragment() {
    internal lateinit var thisview: ActivityMain
    internal lateinit var view: View
    internal lateinit var progressBar: ProgressBar
    internal lateinit var myHandler: Handler
    var refresh = true
    var kernel = false
    var process: Process? = null

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        view = inflater!!.inflate(R.layout.layout_task, container, false)

        progressBar = thisview.findViewById(R.id.shell_on_execute) as ProgressBar

        myHandler = object : android.os.Handler() {
            override fun handleMessage(msg: Message) {
                super.handleMessage(msg)
                if (msg.what == 0) {
                    process = msg.obj as Process
                } else if (msg.what == 1) {
                    var txt = msg.obj.toString()
                    txt = txt.replace("\t\t", "\t").replace("\t", " ")
                    while (txt.contains("  ")) {
                        txt = txt.replace("  ", " ")
                    }
                    var list = ArrayList<HashMap<String, String>>()
                    var rows = txt.split("\n").toMutableList()
                    if (rows.size < 1) {
                        return
                    }
                    var tr = rows[0].split(" ").toMutableList()
                    var pidIndex = tr.indexOf("PID")
                    var typeIndex = tr.indexOf("USER")
                    var cpuIndex = tr.indexOf("CPU%")
                    var nameIndex = tr.indexOf("Name")
                    if (typeIndex < 0) {
                        typeIndex = tr.indexOf("UID")
                    }
                    if (pidIndex < 0 || typeIndex < 0 || cpuIndex < 0 || nameIndex < 0) {
                        return
                    }

                    for (i in 0..rows.size - 1) {
                        var tr = LinkedHashMap<String, String>()
                        var params = rows[i].split(" ").toMutableList()
                        if (params.size > 4) {
                            tr.put("itemPid", params[pidIndex])
                            if (!kernel && i != 0 && params[typeIndex].indexOf("u") != 0) {
                                continue
                            }
                            if (params[nameIndex] == "top") {
                                continue
                            }
                            tr.put("itemType", params[typeIndex])
                            tr.put("itemCpu", params[cpuIndex])
                            tr.put("itemName", params[nameIndex])

                            list.add(tr)
                        }
                    }

                    myHandler.post {
                        if (progressBar.visibility === View.VISIBLE || refresh) {
                            var datas = task_adapter(context, list)
                            list_tasks.setAdapter(datas)
                            progressBar.visibility = View.GONE
                        }
                    }
                }
            }
        }
        return view
    }

    class TaskHandler(var context: Context, var progressBar:ProgressBar, var list_tasks: ListView) : Handler() {
        var process: Process? = null
        internal var kernel:Boolean = false
        internal var refresh:Boolean = false

        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            if (msg.what == 0) {
                process = msg.obj as Process
            } else if (msg.what == 1) {
                var txt = msg.obj.toString()
                txt = txt.replace("\t\t", "\t").replace("\t", " ")
                while (txt.contains("  ")) {
                    txt = txt.replace("  ", " ")
                }
                val list = ArrayList<HashMap<String, String>>()
                val rows = txt.split("\n").toMutableList()
                if (rows.size < 1) {
                    return
                }
                val tr = rows[0].split(" ").toMutableList()
                val pidIndex = tr.indexOf("PID")
                var typeIndex = tr.indexOf("USER")
                val cpuIndex = tr.indexOf("CPU%")
                val nameIndex = tr.indexOf("Name")
                if (typeIndex < 0) {
                    typeIndex = tr.indexOf("UID")
                }
                if (pidIndex < 0 || typeIndex < 0 || cpuIndex < 0 || nameIndex < 0) {
                    return
                }

                for (i in 0..rows.size - 1) {
                    val r = LinkedHashMap<String, String>()
                    val params = rows[i].split(" ").toMutableList()
                    if (params.size > 4) {
                        r.put("itemPid", params[pidIndex])
                        if (!kernel && i != 0 && params[typeIndex].indexOf("u") != 0) {
                            continue
                        }
                        if (params[nameIndex] == "top") {
                            continue
                        }
                        r.put("itemType", params[typeIndex])
                        r.put("itemCpu", params[cpuIndex])
                        r.put("itemName", params[nameIndex])

                        list.add(r)
                    }
                }

                this.post {
                    if (progressBar.visibility == View.VISIBLE || refresh) {
                        val datas = task_adapter(context, list)
                        list_tasks.setAdapter(datas)
                        progressBar.visibility = View.GONE
                    }
                }
            }
        }
    }

    internal var getSwaps = {
        Thread({
            TopTasksUnit.executeCommandWithOutput(myHandler)
        }).start()
    }

    override fun onResume() {
        super.onResume()
        progressBar.visibility = View.VISIBLE

        getSwaps()
    }

    override fun onPause() {
        if (process != null) {
            process!!.destroy()
            process = null
        }
        super.onPause()
    }

    override fun onDestroy() {
        thisview.progressBar.visibility = View.GONE
        if (process != null) {
            process!!.destroy()
            process = null
        }
        super.onDestroy()
    }

    fun killProcess(pid: String) {
        SysUtils.executeRootCommand(mutableListOf("kill " + pid))
        progressBar.visibility = View.VISIBLE
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        checkbox_refresh.isChecked = this.refresh
        checkbox_refresh.setOnCheckedChangeListener({ _, isChecked ->
            this.refresh = isChecked
            if (isChecked) {
                progressBar.visibility = View.VISIBLE
            } else {
                progressBar.visibility = View.GONE
            }
        })
        checkbox_kernel.isChecked = this.kernel
        checkbox_kernel.setOnCheckedChangeListener({ _, isChecked ->
            this.kernel = isChecked
            progressBar.visibility = View.VISIBLE
        })
        list_tasks.setOnItemClickListener { _, dialogView, position, _ ->
            val adapter = list_tasks.adapter as task_adapter
            val item = adapter.getItem(position)
            if (item.get("itemName") == "com.omarea.vboot") {
                Snackbar.make(dialogView, "你这是要我自杀啊！！！", Snackbar.LENGTH_SHORT).show()
                return@setOnItemClickListener
            }
            AlertDialog.Builder(context).setTitle("结束" + item.get("itemName") + "?").setMessage("确定要强行停止这个任务吗，这可能导致数据丢失，甚至系统崩溃需要重启！")
                    .setNegativeButton(
                            "确定",
                            { _, _ ->
                                killProcess(item.get("itemPid").toString())
                            }
                    )
                    .setNeutralButton("取消",
                            { _, _ ->
                            })
                    .create().show()
        }
    }

    companion object {
        fun Create(thisView: ActivityMain): Fragment {
            val fragment = FragmentTasks()
            fragment.thisview = thisView
            return fragment
        }
    }
}// Required empty public constructor
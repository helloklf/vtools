package com.omarea.vtools.fragments

import android.app.AlertDialog
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Handler
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.omarea.common.ui.DialogHelper
import com.omarea.model.ProcessInfo
import com.omarea.shell_utils.ProcessUtils
import com.omarea.ui.ProcessAdapter
import com.omarea.vtools.R
import kotlinx.android.synthetic.main.fragment_process.*
import java.util.*

class FragmentProcess : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? =
            inflater.inflate(R.layout.fragment_process, container, false)

    private val processUtils = ProcessUtils()
    private var supported: Boolean = false
    private val handle = Handler()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        supported = processUtils.supported(context)

        if (supported) {
            process_unsupported.visibility = View.GONE
            process_view.visibility = View.VISIBLE
        } else {
            process_unsupported.visibility = View.VISIBLE
            process_view.visibility = View.GONE
        }

        if (supported) {
            process_list.adapter = ProcessAdapter(this.context!!)
            process_list.setOnItemClickListener { _, _, position, _ ->
                openProcessDetail((process_list.adapter as ProcessAdapter).getItem(position) as ProcessInfo)
            }
        }

        // 搜索关键字
        process_search.addTextChangedListener(object : TextWatcher {
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
                (process_list.adapter as ProcessAdapter?)?.updateSortMode(when (position) {
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
                (process_list.adapter as ProcessAdapter?)?.updateFilterMode(when (position) {
                    0 -> ProcessAdapter.FILTER_ANDROID_USER
                    1 -> ProcessAdapter.FILTER_ANDROID_SYSTEM
                    2 -> ProcessAdapter.FILTER_ANDROID
                    3 -> ProcessAdapter.FILTER_OTHER
                    4 -> ProcessAdapter.FILTER_ALL
                    else -> ProcessAdapter.FILTER_ALL
                })
            }
        }
    }

    // 更新任务列表
    private fun updateData() {
        val data = processUtils.allProcess
        handle.post {
            (process_list?.adapter as ProcessAdapter?)?.setList(data)
        }
    }

    private fun resume() {
        if (supported && timer == null) {
            timer = Timer()
            timer!!.schedule(object : TimerTask() {
                override fun run() {
                    updateData()
                }
            }, 0, 3000)
        }
    }

    private fun pause() {
        if (timer != null) {
            timer?.cancel()
            timer = null
        }
    }

    private var timer: Timer? = null
    override fun onResume() {
        super.onResume()
        activity!!.title = getString(R.string.menu_processes)
        resume()
    }

    override fun onPause() {
        pause()
        super.onPause()
    }


    private val regexUser = Regex("u[0-9]+_.*")
    private val regexPackageName = Regex(".*\\..*")
    private fun isAndroidProcess(processInfo: ProcessInfo): Boolean {
        return (processInfo.command.contains("app_process") && processInfo.name.matches(regexPackageName))
    }

    private var pm: PackageManager? = null
    private fun loadIcon(imageView: ImageView, item: ProcessInfo) {
        if (isAndroidProcess(item)) {
            Thread(Runnable {
                var icon: Drawable? = null
                try {
                    val name = if (item.name.contains(":")) item.name.substring(0, item.name.indexOf(":")) else item.name
                    val installInfo = pm!!.getPackageInfo(name, 0)
                    icon = installInfo.applicationInfo.loadIcon(pm)
                } catch (ex: Exception) {
                } finally {
                    if (icon != null) {
                        imageView.post {
                            imageView.setImageDrawable(icon)
                        }
                    } else {
                        imageView.post {
                            imageView.setImageDrawable(context!!.getDrawable(R.drawable.process_android))
                        }
                    }
                }
            }).start()
        }
    }

    private fun openProcessDetail(processInfo: ProcessInfo) {
        val detail = processUtils.getProcessDetail(processInfo.pid)
        if (detail != null) {
            val view = LayoutInflater.from(context).inflate(R.layout.dialog_process_detail, null)

            if (pm == null) {
                pm = context!!.packageManager
            }

            val name = if (detail.name.contains(":")) detail.name.substring(0, detail.name.indexOf(":")) else detail.name
            try {
                val app = pm!!.getApplicationInfo(name, 0)
                detail.friendlyName = "" + app.loadLabel(pm)
            } catch (ex: java.lang.Exception) {
                detail.friendlyName = name
            }
            val dialog = AlertDialog.Builder(context).setView(view).create()

            view.run {
                findViewById<TextView>(R.id.ProcessFriendlyName).text = detail.friendlyName
                findViewById<TextView>(R.id.ProcessName).text = detail.name
                findViewById<TextView>(R.id.ProcessCommand).text = detail.command
                findViewById<TextView>(R.id.ProcessCmdline).text = detail.cmdline
                findViewById<TextView>(R.id.ProcessPID).text = detail.pid.toString()
                findViewById<TextView>(R.id.ProcessCPU).text = detail.cpu.toString() + "%"
                if (processInfo.rss > 8192) {
                    findViewById<TextView>(R.id.ProcessRSS).text = (detail.rss / 1024).toInt().toString() + "MB"
                } else {
                    findViewById<TextView>(R.id.ProcessRSS).text = detail.rss.toString() + "KB"
                }
                findViewById<TextView>(R.id.ProcessUSER).text = processInfo.user
                loadIcon(findViewById<ImageView>(R.id.ProcessIcon), processInfo)
                findViewById<View>(R.id.ProcessKill).setOnClickListener {
                    processUtils.killProcess(detail.pid)
                    dialog.dismiss()
                }
            }

            DialogHelper.animDialog(dialog)
        } else {
            Toast.makeText(context, "无法获取详情，该进程可能已经退出!", Toast.LENGTH_SHORT).show()
        }
    }
}

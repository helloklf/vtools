package com.omarea.vtools.activities

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.*
import com.omarea.common.ui.DialogHelper
import com.omarea.library.shell.ProcessUtils
import com.omarea.model.ProcessInfo
import com.omarea.ui.AdapterProcess
import com.omarea.vtools.R
import kotlinx.android.synthetic.main.activty_process.*
import java.util.*

class ActivityProcess : ActivityBase() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activty_process)

        setBackArrow()

        onViewCreated(this)
    }

    private val processUtils = ProcessUtils()
    private var supported: Boolean = false
    private val handle = Handler(Looper.getMainLooper())

    private fun onViewCreated(context: Context) {
        supported = processUtils.supported(context)

        if (supported) {
            process_unsupported.visibility = View.GONE
            process_view.visibility = View.VISIBLE
        } else {
            process_unsupported.visibility = View.VISIBLE
            process_view.visibility = View.GONE
        }

        if (supported) {
            process_list.adapter = AdapterProcess(context)
            process_list.setOnItemClickListener { _, _, position, _ ->
                openProcessDetail((process_list.adapter as AdapterProcess).getItem(position))
            }
        }

        // 搜索关键字
        process_search.setOnEditorActionListener { v, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                (process_list.adapter as AdapterProcess?)?.updateKeywords(v.text.toString())
                return@setOnEditorActionListener true
            }
            false
        }

        // 排序方式
        process_sort_mode.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {
            }

            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                (process_list.adapter as AdapterProcess?)?.updateSortMode(when (position) {
                    0 -> AdapterProcess.SORT_MODE_CPU
                    1 -> AdapterProcess.SORT_MODE_RES
                    2 -> AdapterProcess.SORT_MODE_PID
                    else -> AdapterProcess.SORT_MODE_DEFAULT
                })
            }
        }

        // 过滤筛选
        process_filter.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {
            }

            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                (process_list.adapter as AdapterProcess?)?.updateFilterMode(when (position) {
                    0 -> AdapterProcess.FILTER_ANDROID_USER
                    1 -> AdapterProcess.FILTER_ANDROID_SYSTEM
                    2 -> AdapterProcess.FILTER_ANDROID
                    3 -> AdapterProcess.FILTER_OTHER
                    4 -> AdapterProcess.FILTER_ALL
                    else -> AdapterProcess.FILTER_ALL
                })
            }
        }
    }

    // 更新任务列表
    private fun updateData() {
        val data = processUtils.allProcess
        handle.post {
            (process_list?.adapter as AdapterProcess?)?.setList(data)
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
        title = getString(R.string.menu_processes)
        resume()
    }

    override fun onPause() {
        pause()
        super.onPause()
    }

    //返回键事件
    override fun onBackPressed() {
        excludeFromRecent()
        this.finish()
    }

    private val regexUser = Regex("u[0-9]+_.*")
    private val regexPackageName = Regex(".*\\..*")
    private fun isAndroidProcess(processInfo: ProcessInfo): Boolean {
        return (processInfo.command.contains("app_process") && processInfo.name.matches(regexPackageName))
    }

    private var pm: PackageManager? = null
    private fun loadIcon(imageView: ImageView, item: ProcessInfo) {
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
                        imageView.setImageDrawable(getDrawable(R.drawable.process_android))
                    }
                }
            }
        }).start()
    }

    private fun openProcessDetail(processInfo: ProcessInfo) {
        val detail = processUtils.getProcessDetail(processInfo.pid)
        if (detail != null) {
            val view = LayoutInflater.from(this).inflate(R.layout.dialog_process_detail, null)

            if (pm == null) {
                pm = packageManager
            }

            val name = if (detail.name.contains(":")) detail.name.substring(0, detail.name.indexOf(":")) else detail.name
            try {
                val app = pm!!.getApplicationInfo(name, 0)
                detail.friendlyName = "" + app.loadLabel(pm!!)
            } catch (ex: java.lang.Exception) {
                detail.friendlyName = name
            }
            val dialog = DialogHelper.customDialog(this, view)

            /*
            # Android Q 可通过 /proc/[pid]/reclaim 手动回收内存
            # 示例，回收当个应用的内存
            pgrep -f com.tencent.mobileqq | while read line ; do
              echo all > /proc/$line/reclaim
            done

            # 示例，回收所有第三方应用的内存（遍历效率低）
            # pm list packages | awk -F ':' '{print $2}' |  while read app ; do
            pm list packages | cut -f2 -d ':' |  while read app ; do
              echo $app
              pgrep -f $app | while read pid; do
                echo all > /proc/$pid/reclaim
              done
            done

            # 示例，回收所有后台应用的内存（遍历效率略高）
            cat /dev/cpuset/background/tasks | while read line ; do
              echo all > /proc/$line/reclaim 2>/dev/null 2> /dev/null
            done

            # 示例，回收所有后台应用的内存（加强 只过滤空进程）
            cat /dev/cpuset/background/tasks | while read line ; do
              if [[ -f /proc/$line/oom_adj ]] && [[ `cat /proc/$line/oom_adj` == 15 ]]; then
                echo all > /proc/$line/reclaim 2>/dev/null
              fi
            done
            */

            view.run {
                findViewById<TextView>(R.id.ProcessFriendlyName).text = detail.friendlyName
                findViewById<TextView>(R.id.ProcessName).text = detail.name
                findViewById<TextView>(R.id.ProcessCommand).text = detail.command
                findViewById<TextView>(R.id.ProcessCmdline).text = detail.cmdline
                findViewById<TextView>(R.id.ProcessPID).text = detail.pid.toString()
                findViewById<TextView>(R.id.ProcessCPU).text = detail.getCpu().toString() + "%"
                findViewById<TextView>(R.id.ProcessCpuSet).text = "" + detail.cpuSet.toString()
                findViewById<TextView>(R.id.ProcessCGroup).text = "" + detail.cGroup
                findViewById<TextView>(R.id.ProcessOOMADJ).text = "" + detail.oomAdj
                findViewById<TextView>(R.id.ProcessOOMScoreAdj).text = "" + detail.oomScoreAdj
                findViewById<TextView>(R.id.ProcessState).text = detail.getState()
                if (detail.res > 8192) {
                    findViewById<TextView>(R.id.ProcessMEM).text = (detail.res / 1024).toInt().toString() + "MB"
                } else {
                    findViewById<TextView>(R.id.ProcessMEM).text = detail.res.toString() + "KB"
                }
                if (detail.swap > 8192) {
                    findViewById<TextView>(R.id.ProcessSWAP).text = (detail.swap / 1024).toInt().toString() + "MB"
                } else {
                    findViewById<TextView>(R.id.ProcessSWAP).text = detail.swap.toString() + "KB"
                }
                findViewById<TextView>(R.id.ProcessUSER).text = processInfo.user
                if (isAndroidProcess(processInfo)) {
                    loadIcon(findViewById<ImageView>(R.id.ProcessIcon), processInfo)
                    val btn = findViewById<Button>(R.id.ProcessStopApp)
                    btn.setOnClickListener {
                        processUtils.killProcess(processInfo)
                        dialog.dismiss()
                    }
                    btn.visibility = View.VISIBLE
                }
                findViewById<View>(R.id.ProcessKill).setOnClickListener {
                    processUtils.killProcess(detail.pid)
                    dialog.dismiss()
                }
            }
        } else {
            Toast.makeText(this, "无法获取详情，该进程可能已经退出!", Toast.LENGTH_SHORT).show()
        }
    }
}

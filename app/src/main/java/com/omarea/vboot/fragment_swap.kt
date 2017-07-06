package com.omarea.vboot

import android.app.AlertDialog
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.support.design.widget.Snackbar
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.Toast
import com.omarea.shared.ConfigInfo
import com.omarea.shared.cmd_shellTools
import com.omarea.ui.swaplist_adapter
import kotlinx.android.synthetic.main.layout_swap.*
import java.util.ArrayList
import java.util.HashMap


class fragment_swap : Fragment() {
    internal lateinit var thisview: main
    internal lateinit var view: View
    internal lateinit var cmdshellTools: cmd_shellTools
    internal lateinit var progressBar: ProgressBar
    internal lateinit var myHandler: Handler

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        view = inflater!!.inflate(R.layout.layout_swap, container, false)

        progressBar = thisview.findViewById(R.id.shell_on_execute) as ProgressBar
        myHandler = Handler()
        cmdshellTools = cmd_shellTools(null, null)

        return view
    }

    internal var getSwaps = {
        var txt = cmdshellTools.GetProp("/proc/swaps", null)
        txt = txt.replace("\t\t", "\t").replace("\t", " ")
        while (txt.contains("  ")) {
            txt = txt.replace("  ", " ")
        }
        var list = ArrayList<HashMap<String, String>>()
        var rows = txt.split("\n").toMutableList()
        var thr = LinkedHashMap<String, String>()
        thr.put("path", "路径")
        thr.put("type", "类型")
        thr.put("size", "大小")
        thr.put("used", "已用")
        thr.put("priority", "优先级")
        list.add(thr)

        for (i in 1..rows.size - 1) {
            var tr = LinkedHashMap<String, String>()
            var params = rows[i].split(" ").toMutableList()
            tr.put("path", params[0])
            tr.put("type", params[1].replace("file", "文件").replace("partition", "分区"))

            var size = params[2]
            tr.put("size", if (size.length > 3) (size.substring(0, size.length - 3) + "m") else "0")

            var used = params[3]
            tr.put("used", if (used.length > 3) (used.substring(0, used.length - 3) + "m") else "0")

            tr.put("priority", params[4])
            list.add(tr)
        }

        var swappiness = cmdshellTools.GetProp("/proc/sys/vm/swappiness")
        txt_swapstus_swappiness.setText("Swappiness：" + swappiness)
        list_swaps.setAdapter(swaplist_adapter(context, list))
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        chk_swap_disablezram.isChecked = ConfigInfo.getConfigInfo().AutoStartSwapDisZram
        chk_swap_autostart.isChecked = ConfigInfo.getConfigInfo().AutoStartSwap
        var piness = ConfigInfo.getConfigInfo().AutoStartSwappiness
        if(piness>=0&&piness!=65&&piness<=100)
            txt_swap_swappiness.setText(ConfigInfo.getConfigInfo().AutoStartSwappiness.toString())
        getSwaps()
    }


    internal var showWait = {
        Toast.makeText(thisview, "正在执行操作，请稍等...", Toast.LENGTH_SHORT).show()
        progressBar.visibility = View.VISIBLE
    }

    internal var showCreated = {
        Snackbar.make(view!!, "虚拟Swap分区已创建，现在可以点击启动按钮来开启它！", Snackbar.LENGTH_LONG).show()
        progressBar.visibility = View.GONE
    }

    internal var showSwapOpened = {
        Snackbar.make(view!!, "操作已完成！", Snackbar.LENGTH_LONG).show()
        progressBar.visibility = View.GONE
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        btn_swap_create.setOnClickListener {
            var size = txt_swap_size.text
            if (size.length != 0) {
                var run = Runnable {
                    myHandler.post(showWait)
                    var sb = StringBuilder()
                    sb.append("swapoff /data/swapfile\n")
                    sb.append("dd if=/dev/zero of=/data/swapfile bs=1m count=" + size + "\n")
                    sb.append("mkswap /data/swapfile\n")
                    cmdshellTools.DoCmdSync(sb.toString())
                    myHandler.post(getSwaps)
                    myHandler.post(showCreated)
                }
                var isUse = cmdshellTools.GetProp("/proc/swaps", "/data/swapfile")
                if (isUse != null && isUse.length != 0) {
                    var builder = AlertDialog.Builder(this.context)
                    builder.setTitle("要覆盖文件吗？")
                    builder.setMessage("文件已经存在，且正在使用。关闭它可能需要不少时间，因为回收ZRAM、SWAP已用空间非常的慢！")
                    builder.setNegativeButton("取消", { dialog, which -> })
                    builder.setPositiveButton("确定", { dialog, which ->
                        Thread(run).start()
                    })
                    builder.show()
                } else {
                    Thread(run).start()
                }
            } else {
                Snackbar.make(this.view, "请输入Swap大小！\n推荐为RAM大小的50%且不超过2GB", Snackbar.LENGTH_LONG).show()
            }
        }
        btn_swap_start.setOnClickListener {
            var swappiness = txt_swap_swappiness.text.toString()
            var autostart = chk_swap_autostart.isChecked
            var disablezram = chk_swap_disablezram.isChecked

            var sb = StringBuilder()
            var value = 65
            try {
                value = Integer.parseInt(swappiness)
            } catch (ex: Exception) {
            }
            if (disablezram) {
                sb.append("swapon /data/swapfile -p 32767\n")
                //sb.append("swapoff /dev/block/zram0\n")
            }
            else{
                sb.append("swapon /data/swapfile\n")
            }
            sb.append("echo 65 > /proc/sys/vm/swappiness\n")
            sb.append("echo " + value + " > /proc/sys/vm/swappiness\n")

            ConfigInfo.getConfigInfo().AutoStartSwap = autostart
            ConfigInfo.getConfigInfo().AutoStartSwapDisZram = disablezram
            ConfigInfo.getConfigInfo().AutoStartSwappiness = value

            var run = Runnable {
                if (disablezram)
                    myHandler.post(showWait)
                cmdshellTools.DoCmdSync(sb.toString())
                myHandler.post(getSwaps)
                myHandler.post(showSwapOpened)
            }
            Thread(run).start()
        }
    }

    companion object {
        fun Create(thisView: main, cmdshellTools: cmd_shellTools): Fragment {
            val fragment = fragment_swap()
            fragment.cmdshellTools = cmdshellTools
            fragment.thisview = thisView
            return fragment
        }
    }
}// Required empty public constructor

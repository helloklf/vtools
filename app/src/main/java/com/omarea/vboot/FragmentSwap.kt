package com.omarea.vboot

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.support.design.widget.Snackbar
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.omarea.shared.SpfConfig
import com.omarea.shared.cmd_shellTools
import com.omarea.shell.KernelProrp
import com.omarea.shell.SuDo
import com.omarea.shell.units.ChangeZRAM
import com.omarea.ui.ProgressBarDialog
import com.omarea.ui.swaplist_adapter
import kotlinx.android.synthetic.main.layout_swap.*
import java.io.File
import java.util.ArrayList
import java.util.HashMap
import kotlin.collections.LinkedHashMap


class FragmentSwap : Fragment() {
    private lateinit var processBarDialog: ProgressBarDialog
    internal lateinit var view: View
    private lateinit var cmdshellTools: cmd_shellTools
    private lateinit var myHandler: Handler
    private lateinit var swapConfig: SharedPreferences

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        view = inflater!!.inflate(R.layout.layout_swap, container, false)

        myHandler = Handler()
        cmdshellTools = cmd_shellTools(null, null)
        swapConfig = context.getSharedPreferences(SpfConfig.SWAP_SPF, Context.MODE_PRIVATE)
        return view
    }

    internal var getSwaps = {
        val ret = cmdshellTools.GetProp("/proc/swaps", null)
        var txt = if (ret == null) "" else ret.replace("\t\t", "\t").replace("\t", " ")
        while (txt.contains("  ")) {
            txt = txt.replace("  ", " ")
        }
        val list = ArrayList<HashMap<String, String>>()
        val rows = txt.split("\n").toMutableList()
        val thr = LinkedHashMap<String, String>()
        thr.put("path", "路径")
        thr.put("type", "类型")
        thr.put("size", "大小")
        thr.put("used", "已用")
        thr.put("priority", "优先级")
        list.add(thr)

        for (i in 1..rows.size - 1) {
            val tr = LinkedHashMap<String, String>()
            val params = rows[i].split(" ").toMutableList()
            tr.put("path", params[0])
            tr.put("type", params[1].replace("file", "文件").replace("partition", "分区"))

            val size = params[2]
            tr.put("size", if (size.length > 3) (size.substring(0, size.length - 3) + "m") else "0")

            val used = params[3]
            tr.put("used", if (used.length > 3) (used.substring(0, used.length - 3) + "m") else "0")

            tr.put("priority", params[4])
            list.add(tr)
        }

        val swappiness = KernelProrp.getProp("/proc/sys/vm/swappiness")
        txt_swapstus_swappiness.setText("Swappiness: $swappiness")
        txt_zramstus_swappiness.setText("Swappiness: $swappiness")
        val datas = swaplist_adapter(context, list)
        list_swaps.setAdapter(datas)
        list_swaps2.setAdapter(datas)
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        processBarDialog = ProgressBarDialog(this.context)

        chk_swap_disablezram.isChecked = swapConfig.getBoolean(SpfConfig.SWAP_SPF_SWAP_FIRST, false)
        chk_swap_autostart.isChecked = swapConfig.getBoolean(SpfConfig.SWAP_SPF_SWAP, false)
        chk_zram_autostart.isChecked = swapConfig.getBoolean(SpfConfig.SWAP_SPF_ZRAM, false)
        val piness = swapConfig.getInt(SpfConfig.SWAP_SPF_SWAPPINESS, 65)
        if (piness >= 0 && piness != 65 && piness <= 100) {
            txt_swap_swappiness.setText(piness.toString())
            txt_zram_swappiness.setText(piness.toString())
        }
        if (swapConfig.getInt(SpfConfig.SWAP_SPF_ZRAM_SIZE, 0) != 0)
            txt_zram_size.setText(swapConfig.getInt(SpfConfig.SWAP_SPF_ZRAM_SIZE, 0).toString())
        getSwaps()
    }


    private var showWait = {
        processBarDialog.showDialog()
        Toast.makeText(context, "正在执行操作，请稍等...", Toast.LENGTH_SHORT).show()
    }

    private var showCreated = {
        Snackbar.make(view, "虚拟Swap分区已创建，现在可以点击启动按钮来开启它！", Snackbar.LENGTH_LONG).show()
        processBarDialog.hideDialog()
    }

    private var showSwapOpened = {
        Snackbar.make(view, "操作已完成！", Snackbar.LENGTH_LONG).show()
        processBarDialog.hideDialog()
    }

    @SuppressLint("ApplySharedPref")
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        swapzram_tabhost.setup()
        swapzram_tabhost.addTab(swapzram_tabhost.newTabSpec("swap_tab").setContent(R.id.swapzram_tab0).setIndicator("SWAP设置"))
        if (File("/dev/block/zram0").exists())
            swapzram_tabhost.addTab(swapzram_tabhost.newTabSpec("zram_tab").setContent(R.id.swapzram_tab1).setIndicator("ZRAM设置"))
        else
            swapzram_tab1.visibility = View.GONE
        swapzram_tabhost.currentTab = 0

        btn_swap_create.setOnClickListener {
            val size = txt_swap_size.text.toString()
            if (size.isNotEmpty()) {
                val run = Runnable {
                    myHandler.post(showWait)
                    ChangeZRAM(context).createSwapFile(size)
                    myHandler.post(getSwaps)
                    myHandler.post(showCreated)
                }
                Thread(run).start()
            } else {
                Snackbar.make(this.view, "请输入Swap大小！\n推荐为RAM大小的50%且不超过2GB", Snackbar.LENGTH_LONG).show()
            }
        }
        btn_swap_start.setOnClickListener {
            val swappiness = txt_swap_swappiness.text.toString()
            val autostart = chk_swap_autostart.isChecked
            val disablezram = chk_swap_disablezram.isChecked

            val sb = StringBuilder()
            var value = 65
            try {
                value = Integer.parseInt(swappiness)
            } catch (ex: Exception) {
            }
            if (disablezram) {
                sb.append("swapon /data/swapfile -p 32767\n")
                //sb.append("swapoff /dev/block/zram0\n")
            } else {
                sb.append("swapon /data/swapfile\n")
            }
            sb.append("echo 65 > /proc/sys/vm/swappiness\n")
            sb.append("echo $value > /proc/sys/vm/swappiness\n")

            val edit = swapConfig.edit()
            edit.putBoolean(SpfConfig.SWAP_SPF_SWAP, autostart)
            edit.putBoolean(SpfConfig.SWAP_SPF_SWAP_FIRST, disablezram)
            edit.putInt(SpfConfig.SWAP_SPF_SWAPPINESS, value)
            edit.commit()

            Thread(Runnable {
                if (disablezram)
                    myHandler.post(showWait)
                SuDo(context).execCmdSync(sb.toString())
                myHandler.post(getSwaps)
                myHandler.post(showSwapOpened)
            }).start()
        }
        btn_zram_resize.setOnClickListener {
            val size = txt_zram_size.text.toString()
            val swappiness = txt_zram_swappiness.text.toString()
            var value = 65
            var sizeVal = -1

            try {
                sizeVal = Integer.parseInt(size)
                value = Integer.parseInt(swappiness)
            } catch (ex: Exception) {
            }

            if (sizeVal < 2049 && sizeVal > -1) {
                myHandler.post(showWait)

                val run = Thread({
                    val sb = StringBuilder()
                    sb.append("if [ `cat /sys/block/zram0/disksize` != '" + sizeVal + "000000' ] ; then ")
                    sb.append("swapoff /dev/block/zram0 >/dev/null 2>&1;")
                    sb.append("echo 1 > /sys/block/zram0/reset;")
                    sb.append("echo " + sizeVal + "000000 > /sys/block/zram0/disksize;")
                    sb.append("mkswap /dev/block/zram0 >/dev/null 2>&1;")
                    sb.append("swapon /dev/block/zram0 >/dev/null 2>&1;")
                    sb.append("fi;\n")
                    sb.append("echo 65 > /proc/sys/vm/swappiness\n")
                    sb.append("echo $value > /proc/sys/vm/swappiness\n")

                    SuDo(context).execCmdSync(sb.toString())
                    myHandler.post(getSwaps)
                    myHandler.post(showSwapOpened)
                })
                Thread(run).start()
                val edit = swapConfig.edit()
                edit.putBoolean(SpfConfig.SWAP_SPF_ZRAM, chk_zram_autostart.isChecked)
                edit.putInt(SpfConfig.SWAP_SPF_ZRAM_SIZE, sizeVal)
                edit.putInt(SpfConfig.SWAP_SPF_SWAPPINESS, value)
                edit.commit()
            } else {
                Snackbar.make(this.view, "请输入ZRAM大小，值应在0 - 2048之间！", Snackbar.LENGTH_LONG).show()
            }
        }

        chk_zram_autostart.setOnCheckedChangeListener { _, isChecked ->
            if (!isChecked)
                swapConfig.edit().putBoolean(SpfConfig.SWAP_SPF_ZRAM, isChecked).commit()
        }
        chk_swap_autostart.setOnCheckedChangeListener { _, isChecked ->
            if (!isChecked)
                swapConfig.edit().putBoolean(SpfConfig.SWAP_SPF_SWAP, isChecked).commit()
        }
    }

    companion object {
        fun createPage(cmdshellTools: cmd_shellTools): Fragment {
            val fragment = FragmentSwap()
            fragment.cmdshellTools = cmdshellTools
            return fragment
        }
    }
}// Required empty public constructor

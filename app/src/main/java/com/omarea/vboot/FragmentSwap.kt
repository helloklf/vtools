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
import android.widget.SeekBar
import android.widget.Toast
import com.omarea.shared.SpfConfig
import com.omarea.shell.KeepShellSync
import com.omarea.shell.KernelProrp
import com.omarea.shell.SysUtils
import com.omarea.shell.units.ChangeZRAM
import com.omarea.ui.AdapterSwaplist
import com.omarea.ui.ProgressBarDialog
import kotlinx.android.synthetic.main.layout_swap.*
import java.io.File
import java.util.ArrayList
import java.util.HashMap
import kotlin.collections.LinkedHashMap


class FragmentSwap : Fragment() {
    private lateinit var processBarDialog: ProgressBarDialog
    internal lateinit var view: View
    private lateinit var myHandler: Handler
    private lateinit var swapConfig: SharedPreferences

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        view = inflater.inflate(R.layout.layout_swap, container, false)

        myHandler = Handler()
        swapConfig = context!!.getSharedPreferences(SpfConfig.SWAP_SPF, Context.MODE_PRIVATE)
        return view
    }

    internal var getSwaps = {
        val ret = KernelProrp.getProp("/proc/swaps")
        var txt = ret.replace("\t\t", "\t").replace("\t", " ")
        while (txt.contains("  ")) {
            txt = txt.replace("  ", " ")
        }
        val list = ArrayList<HashMap<String, String>>()
        val rows = txt.split("\n").toMutableList()
        val thr = LinkedHashMap<String, String>().apply {
            put("path", getString(R.string.path))
            put("type", getString(R.string.type))
            put("size", getString(R.string.size))
            put("used", getString(R.string.used))
            put("priority", getString(R.string.priority))
        }
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
        swap_swappiness_display.text = "Swappiness: " + swappiness
        try {
            txt_swap_swappiness.progress = swappiness.toInt()
        } catch (ex: Exception) {

        }

        val datas = AdapterSwaplist(context, list)
        list_swaps2.adapter = datas

        txt_mem.text = KernelProrp.getProp("/proc/meminfo")
        btn_swap_create.isEnabled = !File("/data/swapfile").exists()
        btn_swap_start.isEnabled = !txt.contains("/data/swapfile") && !txt.contains("/swapfile") && File("/data/swapfile").exists()
        btn_swap_close.isEnabled = txt.contains("/data/swapfile")
        btn_swap_delete.isEnabled = File("/data/swapfile").exists()
    }

    override fun onResume() {
        super.onResume()
        getSwaps()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        processBarDialog = ProgressBarDialog(this.context!!)

        chk_swap_disablezram.isChecked = swapConfig.getBoolean(SpfConfig.SWAP_SPF_SWAP_FIRST, false)
        chk_swap_autostart.isChecked = swapConfig.getBoolean(SpfConfig.SWAP_SPF_SWAP, false)
        chk_zram_autostart.isChecked = swapConfig.getBoolean(SpfConfig.SWAP_SPF_ZRAM, false)

        txt_swap_size.progress = swapConfig.getInt(SpfConfig.SWAP_SPF_SWAP_SWAPSIZE, if (File("/data/swapfile").exists()) (File("/data/swapfile").length() / 1024 / 1024).toInt() else 0)
        txt_swap_size_display.text = swapConfig.getInt(SpfConfig.SWAP_SPF_SWAP_SWAPSIZE, 0).toString() + "MB"
        txt_zram_size.progress = swapConfig.getInt(SpfConfig.SWAP_SPF_ZRAM_SIZE, 0)
        txt_zram_size_display.text = swapConfig.getInt(SpfConfig.SWAP_SPF_ZRAM_SIZE, 0).toString() + "MB"
        txt_swap_swappiness.progress = swapConfig.getInt(SpfConfig.SWAP_SPF_SWAPPINESS, 65)
        txt_swap_size.setOnSeekBarChangeListener(OnSeekBarChangeListener(Runnable {
            txt_swap_size_display.text = swapConfig.getInt(SpfConfig.SWAP_SPF_SWAP_SWAPSIZE, 0).toString() + "MB"
        }, swapConfig, SpfConfig.SWAP_SPF_SWAP_SWAPSIZE))
        txt_zram_size.setOnSeekBarChangeListener(OnSeekBarChangeListener(Runnable {
            txt_zram_size_display.text = swapConfig.getInt(SpfConfig.SWAP_SPF_ZRAM_SIZE, 0).toString() + "MB"
        }, swapConfig, SpfConfig.SWAP_SPF_ZRAM_SIZE))
        txt_swap_swappiness.setOnSeekBarChangeListener(OnSeekBarChangeListener(Runnable {
            val swappiness = swapConfig.getInt(SpfConfig.SWAP_SPF_SWAPPINESS, 0)
            txt_zramstus_swappiness.text = swappiness.toString()
            KeepShellSync.doCmdSync("echo $swappiness > /proc/sys/vm/swappiness;")
            swap_swappiness_display.text = "Swappiness: " + KernelProrp.getProp("/proc/sys/vm/swappiness")
        }, swapConfig, SpfConfig.SWAP_SPF_SWAPPINESS))

        btn_swap_close.setOnClickListener {
            processBarDialog.showDialog(getString(R.string.swap_on_close))
            val run = Runnable {
                SysUtils.executeCommandWithOutput(true, "sync\necho 3 > /proc/sys/vm/drop_caches\nbusybox swapoff /data/swapfile > /dev/null 2>&1")
                myHandler.post({
                    processBarDialog.hideDialog()
                    getSwaps()
                })
            }
            Thread(run).start()
        }
        btn_swap_delete.setOnClickListener {
            processBarDialog.showDialog(getString(R.string.swap_on_close))
            val run = Runnable {
                val sb = StringBuilder()
                sb.append("sync\necho 3 > /proc/sys/vm/drop_caches\nswapoff /data/swapfile >/dev/null 2>&1;")
                sb.append("rm -f /data/swapfile;")
                SysUtils.executeCommandWithOutput(true, sb.toString())
                myHandler.post({
                    processBarDialog.hideDialog()
                    getSwaps()
                })
            }
            Thread(run).start()
        }
    }

    private var showWait = {
        processBarDialog.showDialog()
        Toast.makeText(context, getString(R.string.on_execute_please_wait), Toast.LENGTH_SHORT).show()
    }

    private var showSwapOpened = {
        Snackbar.make(view, getString(R.string.executed), Snackbar.LENGTH_LONG).show()
        processBarDialog.hideDialog()
    }

    @SuppressLint("ApplySharedPref")
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        if (!KeepShellSync.doCmdSync("if [[ -e /dev/block/zram0 ]]; then echo 1; else echo 0; fi;").equals("1")) {
            swap_config_zram.visibility = View.GONE
        }

        btn_swap_create.setOnClickListener {
            val size = txt_swap_size.progress

            val run = Runnable {
                val startTime = System.currentTimeMillis()
                myHandler.post({
                    processBarDialog.showDialog(getString(R.string.file_creating))
                })
                ChangeZRAM(context!!).createSwapFile(size)
                myHandler.post(getSwaps)
                val time = System.currentTimeMillis() - startTime
                myHandler.post({
                    processBarDialog.hideDialog()
                    Toast.makeText(context, "Swapfile创建完毕，耗时${time / 1000}s，平均写入速度：${(size * 1000.0 / time).toInt()}MB/s", Toast.LENGTH_LONG).show()
                })
            }
            Thread(run).start()
        }

        btn_swap_start.setOnClickListener {
            val autostart = chk_swap_autostart.isChecked
            val disablezram = chk_swap_disablezram.isChecked

            val sb = StringBuilder()
            if (disablezram) {
                sb.append("swapon /data/swapfile -p 32767\n")
                //sb.append("swapoff /dev/block/zram0\n")
            } else {
                sb.append("swapon /data/swapfile\n")
            }

            val edit = swapConfig.edit()
            edit.putBoolean(SpfConfig.SWAP_SPF_SWAP, autostart)
            edit.putBoolean(SpfConfig.SWAP_SPF_SWAP_FIRST, disablezram)
            edit.commit()

            Thread(Runnable {
                if (disablezram)
                    myHandler.post(showWait)
                SysUtils.executeCommandWithOutput(true, sb.toString())
                myHandler.post(getSwaps)
                myHandler.post(showSwapOpened)
            }).start()
        }
        btn_zram_resize.setOnClickListener {
            val sizeVal = txt_zram_size.progress

            if (sizeVal < 2049 && sizeVal > -1) {
                processBarDialog.showDialog(getString(R.string.zram_resizing))

                val run = Thread({
                    val sb = StringBuilder()
                    sb.append("if [ `cat /sys/block/zram0/disksize` != '" + sizeVal + "000000' ] ; then ")
                    sb.append(
                            "sync\n" +
                            "echo 3 > /proc/sys/vm/drop_caches\n" +
                            "swapoff /dev/block/zram0 >/dev/null 2>&1;")
                    sb.append("echo 1 > /sys/block/zram0/reset;")
                    sb.append("echo " + sizeVal + "000000 > /sys/block/zram0/disksize;")
                    sb.append("mkswap /dev/block/zram0 >/dev/null 2>&1;")
                    sb.append("fi;")
                    sb.append("\n")
                    sb.append("swapon /dev/block/zram0 >/dev/null 2>&1;")
                    sb.append("sleep 2;")
                    SysUtils.executeCommandWithOutput(true, sb.toString())
                    myHandler.post(getSwaps)
                    myHandler.post {
                        processBarDialog.hideDialog()
                    }
                })
                Thread(run).start()
                swapConfig.edit().putInt(SpfConfig.SWAP_SPF_ZRAM_SIZE, sizeVal).commit()
            } else {
                Snackbar.make(this.view, getString(R.string.zram_size_area), Snackbar.LENGTH_LONG).show()
            }
        }

        chk_swap_disablezram.setOnCheckedChangeListener { _, isChecked ->
            Toast.makeText(context, "该选项会在下次启动Swap时生效，而不是现在！", Toast.LENGTH_SHORT).show()
            swapConfig.edit().putBoolean(SpfConfig.SWAP_SPF_SWAP_FIRST, isChecked).commit()
        }
        chk_zram_autostart.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                Toast.makeText(context, "注意：你需要允许工具箱自启动，下次开机才会生效！", Toast.LENGTH_SHORT).show()
            }
            swapConfig.edit().putBoolean(SpfConfig.SWAP_SPF_ZRAM, isChecked).commit()
        }
        chk_swap_autostart.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                Toast.makeText(context, "注意：你需要允许工具箱自启动，下次开机才会生效！", Toast.LENGTH_SHORT).show()
            }
            swapConfig.edit().putBoolean(SpfConfig.SWAP_SPF_SWAP, isChecked).commit()
        }
    }

    class OnSeekBarChangeListener(private var next: Runnable, private var spf: SharedPreferences, private var spfProp: String) : SeekBar.OnSeekBarChangeListener {
        override fun onStopTrackingTouch(seekBar: SeekBar?) {
        }

        override fun onStartTrackingTouch(seekBar: SeekBar?) {
        }

        @SuppressLint("ApplySharedPref")
        override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
            if (spf.getInt(spfProp, Int.MIN_VALUE) == progress) {
                return
            }
            spf.edit().putInt(spfProp, progress).commit()
            next.run()
        }
    }


    override fun onDetach() {
        processBarDialog.hideDialog()
        super.onDetach()
    }

    companion object {
        fun createPage(): Fragment {
            val fragment = FragmentSwap()
            return fragment
        }
    }
}// Required empty public constructor

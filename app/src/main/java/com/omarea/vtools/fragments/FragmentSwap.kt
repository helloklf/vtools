package com.omarea.vtools.fragments

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.app.AlertDialog
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.google.android.material.snackbar.Snackbar
import com.omarea.common.shell.KeepShellPublic
import com.omarea.common.shell.KernelProrp
import com.omarea.common.ui.DialogHelper
import com.omarea.common.ui.ProgressBarDialog
import com.omarea.shell_utils.LMKUtils
import com.omarea.shell_utils.SwapUtils
import com.omarea.store.SpfConfig
import com.omarea.ui.AdapterSwaplist
import com.omarea.vtools.R
import kotlinx.android.synthetic.main.fragment_swap.*
import java.util.*
import kotlin.collections.LinkedHashMap


class FragmentSwap : androidx.fragment.app.Fragment() {
    private lateinit var processBarDialog: ProgressBarDialog
    internal lateinit var view: View
    private val myHandler = Handler()
    private lateinit var swapConfig: SharedPreferences
    private var totalMem = 2048
    private lateinit var swapUtils: SwapUtils

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        view = inflater.inflate(R.layout.fragment_swap, container, false)
        swapUtils = SwapUtils(context!!)

        swapConfig = context!!.getSharedPreferences(SpfConfig.SWAP_SPF, Context.MODE_PRIVATE)

        val activityManager = context!!.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val info = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(info)

        totalMem = (info.totalMem / 1024 / 1024f).toInt()
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
            put("priority", getString(R.string.order)) // put("priority", getString(R.string.priority))
        }
        list.add(thr)

        for (i in 1 until rows.size) {
            val tr = LinkedHashMap<String, String>()
            val params = rows[i].split(" ").toMutableList()
            tr.put("path", params[0])
            tr.put("type", params[1].replace("file", "文件").replace("partition", "分区"))

            val size = params[2]
            // tr.put("size", if (size.length > 3) (size.substring(0, size.length - 3) + "m") else "0")
            tr.put("size", swapUsedSizeParseMB(size))

            val used = params[3]
            // tr.put("used", if (used.length > 3) (used.substring(0, used.length - 3) + "m") else "0")
            tr.put("used", swapUsedSizeParseMB(used))

            tr.put("priority", params[4])
            list.add(tr)
        }

        val swappiness = KernelProrp.getProp("/proc/sys/vm/swappiness")
        swap_swappiness_display.text = "/proc/sys/vm/swappiness :  $swappiness"

        val datas = AdapterSwaplist(context, list)
        list_swaps2.adapter = datas

        txt_mem.text = KernelProrp.getProp("/proc/meminfo")
        val swapFileExists = swapUtils.swapExists
        btn_swap_create.visibility = if (swapFileExists) View.GONE else View.VISIBLE
        if (swapFileExists) {
            seekbar_swap_size.visibility = View.GONE
        } else {
            seekbar_swap_size.visibility = View.VISIBLE
        }

        val currentSwap = swapUtils.sceneSwaps
        if (currentSwap.isNotEmpty()) {
            btn_swap_start.visibility = View.GONE
            btn_swap_delete.visibility = View.GONE
            btn_swap_close.visibility = View.VISIBLE
            chk_swap_use_loop.visibility = View.GONE
            chk_swap_order.visibility = View.GONE
            swap_state.setText(getString(R.string.swap_state_using))
        } else {
            btn_swap_start.visibility = if (swapFileExists) View.VISIBLE else View.GONE
            btn_swap_delete.visibility = if (swapFileExists) View.VISIBLE else View.GONE

            btn_swap_close.visibility = View.GONE
            chk_swap_use_loop.visibility = if (swapFileExists) View.VISIBLE else View.GONE
            chk_swap_order.visibility = if (swapFileExists) View.VISIBLE else View.GONE
            if (swapFileExists) {
                swap_state.setText(getString(R.string.swap_state_created))
            } else {
                swap_state.setText(getString(R.string.swap_state_undefined))
            }
        }

        if (swapUtils.zramEnabled) {
            zram_state.setText(getString(R.string.swap_state_using))
        } else {
            zram_state.setText(getString(R.string.swap_state_created))
        }

        swap_auto_lmk.isChecked = swapConfig.getBoolean(SpfConfig.SWAP_SPF_AUTO_LMK, false)
        val lmkUtils = LMKUtils()
        if (lmkUtils.supported()) {
            swap_lmk_current.text = lmkUtils.getCurrent()
        } else {
            swap_auto_lmk_wrap.visibility = View.GONE
        }

        val extra_free_kbytes = KernelProrp.getProp("/proc/sys/vm/extra_free_kbytes")
        try {
            val bytes = extra_free_kbytes.toInt()
            if (seekbar_extra_free_kbytes.max < bytes) {
                seekbar_extra_free_kbytes.max = bytes
            }
            extra_free_kbytes_display.text = "/proc/sys/vm/extra_free_kbytes : " + extra_free_kbytes
        } catch (ex: Exception) {
        }

        // 压缩算法
        val comp_algorithm = swapUtils.compAlgorithm
        // 最大压缩流
        // val max_comp_streams = KernelProrp.getProp("/sys/block/zram0/max_comp_streams")
        // 存储在此磁盘中的未压缩数据大小
        val orig_data_size = KernelProrp.getProp("/sys/block/zram0/orig_data_size")
        // 存储在此磁盘中的压缩数据大小
        val compr_data_size = KernelProrp.getProp("/sys/block/zram0/compr_data_size")
        // 为此磁盘分配的内存量
        val mem_used_total = KernelProrp.getProp("/sys/block/zram0/mem_used_total")
        // 可用于存储的最大内存量
        val mem_limit = KernelProrp.getProp("/sys/block/zram0/mem_limit")
        // 消耗的最大内存量
        val mem_used_max = KernelProrp.getProp("/sys/block/zram0/mem_used_max")
        // 写入此磁盘的相同元素填充页面的数量 不占用内存
        // val same_pages = KernelProrp.getProp("/sys/block/zram0/same_pages")
        // 压缩期间释放的页数
        // val pages_compacted = KernelProrp.getProp("/sys/block/zram0/pages_compacted")
        // 不可压缩数据
        // val huge_pages = KernelProrp.getProp("/sys/block/zram0/huge_pages")

        zram0_stat.text = String.format(
                getString(R.string.swap_zram_stat_format),
                comp_algorithm,
                zramInfoValueParseMB(orig_data_size),
                zramInfoValueParseMB(compr_data_size),
                zramInfoValueParseMB(mem_used_total),
                zramInfoValueParseMB(mem_used_max),
                if (mem_limit == "0") "" else mem_limit,
                zramCompressionRatio(orig_data_size, compr_data_size),
                zramCompressionRatio(orig_data_size, mem_used_total))

        zram_compact_algorithm.text = swapConfig.getString(SpfConfig.SWAP_SPF_ALGORITHM, comp_algorithm)
    }

    private fun zramInfoValueParseMB(sizeStr: String): String {
        try {
            return (sizeStr.toLong() / 1024 / 1024).toString() + "MB"
        } catch (ex: java.lang.Exception) {
            return sizeStr
        }
    }

    private fun zramCompressionRatio(origDataSize: String, comprDataSize: String): String {
        try {
            return (comprDataSize.toLong() * 1000 / origDataSize.toLong() / 10.0).toString() + "%"
        } catch (ex: java.lang.Exception) {
            return "$comprDataSize/$origDataSize"
        }
    }

    fun swapUsedSizeParseMB(sizeStr: String): String {
        try {
            return (sizeStr.toLong() / 1024).toString()
        } catch (ex: java.lang.Exception) {
            return sizeStr
        }
    }

    override fun onResume() {
        super.onResume()
        if (isDetached) {
            return
        }
        activity!!.title = getString(R.string.menu_swap)
        getSwaps()
    }

    @SuppressLint("ApplySharedPref")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        processBarDialog = ProgressBarDialog(this.context!!)

        val swapPriority = swapConfig.getInt(SpfConfig.SWAP_SPF_SWAP_PRIORITY, -2)
        if (swapPriority == 5) {
            swap_swap_preferred.isChecked = true
        } else if (swapPriority == 0) {
            swap_zram_equitable.isChecked = true
        } else {
            swap_zram_preferred.isChecked = true
        }

        chk_swap_autostart.isChecked = swapConfig.getBoolean(SpfConfig.SWAP_SPF_SWAP, false)
        chk_zram_autostart.isChecked = swapConfig.getBoolean(SpfConfig.SWAP_SPF_ZRAM, false)
        chk_swap_use_loop.isChecked = swapConfig.getBoolean(SpfConfig.SWAP_SPF_SWAP_USE_LOOP, false)

        var swapSize = 0
        if (swapUtils.swapExists) {
            swapSize = swapUtils.swapFileSize
        } else {
            swapConfig.getInt(SpfConfig.SWAP_SPF_SWAP_SWAPSIZE, 0)
        }

        seekbar_swap_size.progress = swapSize / 128
        txt_swap_size_display.text = "${swapSize}MB"
        seekbar_zram_size.max = totalMem / 128
        var zramSize = swapConfig.getInt(SpfConfig.SWAP_SPF_ZRAM_SIZE, 0)
        if (zramSize > totalMem)
            zramSize = totalMem
        seekbar_zram_size.progress = zramSize / 128
        txt_zram_size_display.text = "${zramSize}MB"
        seekbar_swap_swappiness.progress = swapConfig.getInt(SpfConfig.SWAP_SPF_SWAPPINESS, 65)
        txt_zramstus_swappiness.text = seekbar_swap_swappiness.progress.toString()
        seekbar_extra_free_kbytes.progress = swapConfig.getInt(SpfConfig.SWAP_MIN_FREE_KBYTES, 29615)
        txt_extra_free_kbytes.text = seekbar_extra_free_kbytes.progress.toString() + "\n(" + (seekbar_extra_free_kbytes.progress / 1024) + "MB)"

        seekbar_swap_size.setOnSeekBarChangeListener(OnSeekBarChangeListener(Runnable {
            txt_swap_size_display.text = swapConfig.getInt(SpfConfig.SWAP_SPF_SWAP_SWAPSIZE, 0).toString() + "MB"
        }, null, swapConfig, SpfConfig.SWAP_SPF_SWAP_SWAPSIZE, 128))
        seekbar_zram_size.setOnSeekBarChangeListener(OnSeekBarChangeListener(Runnable {
            txt_zram_size_display.text = swapConfig.getInt(SpfConfig.SWAP_SPF_ZRAM_SIZE, 0).toString() + "MB"
        }, null, swapConfig, SpfConfig.SWAP_SPF_ZRAM_SIZE, 128))
        seekbar_swap_swappiness.setOnSeekBarChangeListener(OnSeekBarChangeListener(Runnable {
            val swappiness = swapConfig.getInt(SpfConfig.SWAP_SPF_SWAPPINESS, 0)
            txt_zramstus_swappiness.text = swappiness.toString()
        }, Runnable {
            val swappiness = swapConfig.getInt(SpfConfig.SWAP_SPF_SWAPPINESS, 0)
            txt_zramstus_swappiness.text = swappiness.toString()
            KeepShellPublic.doCmdSync("echo $swappiness > /proc/sys/vm/swappiness")
            swap_swappiness_display.text = "/proc/sys/vm/swappiness :  " + KernelProrp.getProp("/proc/sys/vm/swappiness")
        }, swapConfig, SpfConfig.SWAP_SPF_SWAPPINESS))

        seekbar_extra_free_kbytes.setOnSeekBarChangeListener(OnSeekBarChangeListener(Runnable {
            val value = swapConfig.getInt(SpfConfig.SWAP_MIN_FREE_KBYTES, 32768)
            txt_extra_free_kbytes.text = value.toString() + "\n(" + (value / 1024) + "MB)"
        }, Runnable {
            val value = swapConfig.getInt(SpfConfig.SWAP_MIN_FREE_KBYTES, 32768)
            txt_extra_free_kbytes.text = value.toString() + "\n(" + (value / 1024) + "MB)"
            processBarDialog.showDialog(getString(R.string.swap_on_close))
            val run = Runnable {
                KeepShellPublic.doCmdSync("echo $value > /proc/sys/vm/extra_free_kbytes")
                myHandler.post {
                    processBarDialog.hideDialog()
                    getSwaps()
                }
            }
            Thread(run).start()
        }, swapConfig, SpfConfig.SWAP_MIN_FREE_KBYTES))

        btn_swap_close.setOnClickListener {
            processBarDialog.showDialog(getString(R.string.swap_on_close))
            val run = Runnable {
                swapUtils.swapOff()
                myHandler.post {
                    processBarDialog.hideDialog()
                    getSwaps()
                }
            }
            Thread(run).start()
        }
        btn_swap_delete.setOnClickListener {
            processBarDialog.showDialog(getString(R.string.swap_on_close))
            val run = Runnable {
                swapUtils.swapDelete()

                myHandler.post {
                    processBarDialog.hideDialog()
                    getSwaps()
                }
            }
            Thread(run).start()
        }
        swap_auto_lmk.setOnClickListener {
            val checked = (it as Switch).isChecked
            swapConfig.edit().putBoolean(SpfConfig.SWAP_SPF_AUTO_LMK, checked).commit()
            if (checked) {
                val activityManager = context!!.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
                val info = ActivityManager.MemoryInfo()
                activityManager.getMemoryInfo(info)
                val utils = LMKUtils()
                utils.autoSetLMK(info.totalMem)
                swap_lmk_current.text = utils.getCurrent()
            } else {
                Toast.makeText(context!!, "需要重启手机才会恢复默认的LMK参数！", Toast.LENGTH_SHORT).show()
            }
        }

        zram_compact_algorithm.setOnClickListener {
            val current = swapUtils.compAlgorithm
            val options = swapUtils.compAlgorithmOptions
            var selectedIndex = options.indexOf(current)
            DialogHelper.animDialog(AlertDialog.Builder(context)
                    .setTitle(R.string.swap_zram_comp_options)
                    .setSingleChoiceItems(options, selectedIndex) { _, index ->
                        selectedIndex = index
                    }
                    .setNeutralButton(R.string.btn_help) { _, _ ->
                        DialogHelper.helpInfo(context!!, R.string.help, R.string.swap_zram_comp_algorithm_desc)
                    }
                    .setPositiveButton(R.string.btn_confirm) { _, _ ->
                        val algorithm = options.get(selectedIndex)
                        swapUtils.compAlgorithm = algorithm
                        swapConfig.edit().putString(SpfConfig.SWAP_SPF_ALGORITHM, algorithm).apply()

                        if (swapUtils.zramEnabled) {
                            DialogHelper.helpInfo(
                                    context!!,
                                    R.string.swap_zram_comp_algorithm_wran,
                                    R.string.swap_zram_comp_algorithm_wran_desc)
                            (it as TextView).text = algorithm
                        } else {
                            (it as TextView).text = swapUtils.compAlgorithm
                        }
                        //
                    })
        }
    }

    private var showSwapOpened = {
        Snackbar.make(view, getString(R.string.executed), Snackbar.LENGTH_LONG).show()
        processBarDialog.hideDialog()
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        if (!swapUtils.zramSupport) {
            swap_config_zram.visibility = View.GONE
            zram_stat.visibility = View.GONE
        }

        btn_swap_create.setOnClickListener {
            val size = seekbar_swap_size.progress * 128

            val run = Runnable {
                val startTime = System.currentTimeMillis()
                myHandler.post {
                    processBarDialog.showDialog(getString(R.string.file_creating))
                }
                swapUtils.mkswap(size)
                myHandler.post(getSwaps)
                val time = System.currentTimeMillis() - startTime
                myHandler.post {
                    processBarDialog.hideDialog()
                    Toast.makeText(context, "Swapfile创建完毕，耗时${time / 1000}s，平均写入速度：${(size * 1000.0 / time).toInt()}MB/s", Toast.LENGTH_LONG).show()
                }
            }
            Thread(run).start()
        }

        btn_swap_start.setOnClickListener {
            val autostart = chk_swap_autostart.isChecked
            val priority = if (swap_swap_preferred.isChecked) {
                5
            } else if (swap_zram_equitable.isChecked) {
                0
            } else {
                -2
            }
            swapConfig.edit().putInt(SpfConfig.SWAP_SPF_SWAP_PRIORITY, priority).apply()

            val edit = swapConfig.edit()
            edit.putBoolean(SpfConfig.SWAP_SPF_SWAP, autostart)
            edit.putInt(SpfConfig.SWAP_SPF_SWAP_PRIORITY, priority)
            edit.apply()

            processBarDialog.showDialog("稍等...")
            Thread(Runnable {
                swapUtils.swapOn(priority, swapConfig.getBoolean(SpfConfig.SWAP_SPF_SWAP_USE_LOOP, false))

                myHandler.post(getSwaps)
                myHandler.post(showSwapOpened)
                processBarDialog.hideDialog()
            }).start()
        }
        btn_zram_resize.setOnClickListener {
            val sizeVal = seekbar_zram_size.progress * 128

            processBarDialog.showDialog(getString(R.string.zram_resizing))

            val run = Thread {
                val algorithm = swapConfig.getString(SpfConfig.SWAP_SPF_ALGORITHM, "")
                swapUtils.resizeZram(sizeVal, algorithm!!)

                myHandler.post(getSwaps)
                myHandler.post {
                    processBarDialog.hideDialog()
                }
            }
            Thread(run).start()
            swapConfig.edit().putInt(SpfConfig.SWAP_SPF_ZRAM_SIZE, sizeVal).apply()
        }

        chk_zram_autostart.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                Toast.makeText(context, "注意：你需要允许Scene自启动，下次开机才会生效！", Toast.LENGTH_SHORT).show()
            }
            swapConfig.edit().putBoolean(SpfConfig.SWAP_SPF_ZRAM, isChecked).apply()
        }
        chk_swap_autostart.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                Toast.makeText(context, "注意：你需要允许Scene自启动，下次开机才会生效！", Toast.LENGTH_SHORT).show()
            }
            swapConfig.edit().putBoolean(SpfConfig.SWAP_SPF_SWAP, isChecked).apply()
        }
        chk_swap_use_loop.setOnClickListener {
            if ((it as CheckBox).isChecked) {
                DialogHelper.animDialog(AlertDialog.Builder(context!!)
                        .setTitle(R.string.swap_use_loop)
                        .setMessage(R.string.swap_use_loop_desc)
                        .setPositiveButton(R.string.btn_confirm) { _, _ ->
                            it.isChecked = true
                            swapConfig.edit().putBoolean(SpfConfig.SWAP_SPF_SWAP_USE_LOOP, true).apply()
                        }
                        .setNeutralButton(R.string.btn_cancel) { _, _ ->
                            it.isChecked = false
                            swapConfig.edit().putBoolean(SpfConfig.SWAP_SPF_SWAP_USE_LOOP, false).apply()
                        }
                        .setCancelable(false))
            } else {
                swapConfig.edit().putBoolean(SpfConfig.SWAP_SPF_SWAP_USE_LOOP, false).apply()
            }
        }
    }

    class OnSeekBarChangeListener(
            private var onValueChange: Runnable?,
            private var omCompleted: Runnable?,
            private var spf: SharedPreferences,
            private var spfProp: String,
            private var ratio: Int = 1) : SeekBar.OnSeekBarChangeListener {
        override fun onStopTrackingTouch(seekBar: SeekBar?) {
            omCompleted?.run()
        }

        override fun onStartTrackingTouch(seekBar: SeekBar?) {
        }

        @SuppressLint("ApplySharedPref")
        override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
            val value = progress * ratio
            if (spf.getInt(spfProp, Int.MIN_VALUE) == value) {
                return
            }
            spf.edit().putInt(spfProp, value).commit()
            onValueChange?.run()
        }
    }


    override fun onDetach() {
        processBarDialog.hideDialog()
        super.onDetach()
    }

    companion object {
        fun createPage(): androidx.fragment.app.Fragment {
            val fragment = FragmentSwap()
            return fragment
        }
    }
}// Required empty public constructor

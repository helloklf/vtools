package com.omarea.vtools.activities

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.CompoundButton
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import com.omarea.Scene
import com.omarea.common.model.SelectItem
import com.omarea.common.shared.MagiskExtend
import com.omarea.common.shell.KeepShellPublic
import com.omarea.common.shell.KernelProrp
import com.omarea.common.shell.RootFile
import com.omarea.common.ui.DialogHelper
import com.omarea.common.ui.DialogItemChooserMini
import com.omarea.common.ui.ProgressBarDialog
import com.omarea.library.basic.RadioGroupSimulator
import com.omarea.library.shell.LMKUtils
import com.omarea.library.shell.PropsUtils
import com.omarea.library.shell.SwapModuleUtils
import com.omarea.library.shell.SwapUtils
import com.omarea.store.SpfConfig
import com.omarea.ui.AdapterSwaplist
import com.omarea.vtools.R
import kotlinx.android.synthetic.main.activity_swap.*
import java.util.*
import kotlin.collections.LinkedHashMap


class ActivitySwap : ActivityBase() {
    private lateinit var processBarDialog: ProgressBarDialog
    private val myHandler = Handler(Looper.getMainLooper())
    private lateinit var swapConfig: SharedPreferences
    private var totalMem = 2048
    private val swapUtils = SwapUtils(Scene.context)
    private val swapModuleUtils = SwapModuleUtils()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_swap)
        setBackArrow()

        swapConfig = getSharedPreferences(SpfConfig.SWAP_SPF, Context.MODE_PRIVATE)

        val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val info = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(info)

        totalMem = (info.totalMem / 1024 / 1024f).toInt()

        // 进入界面时 加载Magisk模块的配置
        swapModuleUtils.loadModuleConfig(swapConfig)

        setView()
    }

    private fun swapOffAwait(): Timer {
        val timer = Timer()
        val totalUsed = swapUtils.swapUsedSize
        val startTime = System.currentTimeMillis()
        timer.schedule(object : TimerTask() {
            override fun run() {
                val currentUsed = swapUtils.swapUsedSize
                val avgSpeed = (totalUsed - currentUsed).toFloat() / (System.currentTimeMillis() - startTime) * 1000
                val tipStr = StringBuilder()
                tipStr.append(String.format("回收Swapfile ${currentUsed}/${totalUsed}MB (%.1fMB/s)\n", avgSpeed))
                if (avgSpeed > 0) {
                    tipStr.append("大约还需 " + (currentUsed / avgSpeed).toInt() + "秒")
                } else {
                    tipStr.append("请耐心等待~")
                }
                myHandler.post {
                    processBarDialog.showDialog(tipStr.toString())
                }
            }
        }, 0, 1000)
        return timer
    }

    private fun zramOffAwait(): Timer {
        val timer = Timer()
        val totalUsed = swapUtils.zramUsedSize
        val startTime = System.currentTimeMillis()
        timer.schedule(object : TimerTask() {
            override fun run() {
                val currentUsed = swapUtils.zramUsedSize
                val avgSpeed = (totalUsed - currentUsed).toFloat() / (System.currentTimeMillis() - startTime) * 1000
                val tipStr = StringBuilder()
                tipStr.append(String.format("回收ZRAM ${currentUsed}/${totalUsed}MB (%.1fMB/s)\n", avgSpeed))
                if (avgSpeed > 0) {
                    tipStr.append("大约还需 " + (currentUsed / avgSpeed).toInt() + "秒")
                } else {
                    tipStr.append("请耐心等待~")
                }
                myHandler.post {
                    processBarDialog.showDialog(tipStr.toString())
                }
            }
        }, 0, 1000)
        return timer
    }

    private fun setView() {
        val context = this
        processBarDialog = ProgressBarDialog(context)

        if (swapModuleUtils.magiskModuleInstalled) {
            swap_module_installed.visibility = View.VISIBLE
            swap_module_uninstalled.visibility = View.GONE
        } else {
            swap_module_installed.visibility = View.GONE
            swap_module_uninstalled.visibility = View.VISIBLE
        }

        if (MagiskExtend.magiskSupported()) {
            val currentVersion = swapModuleUtils.getModuleVersion()
            if (currentVersion < getString(R.string.swap_module_target_version).toInt()) {
                swap_module_downloadable.visibility = View.VISIBLE
                swap_module_downloadable.setOnClickListener {
                    swapModuleUpdateDialog()
                }
            } else {
                swap_module_downloadable.visibility = View.GONE
            }
        }

        // 关闭swap
        btn_swap_close.setOnClickListener {
            val usedSize = swapUtils.swapUsedSize
            if (usedSize > 500) {
                DialogHelper.confirm(this,
                        "确认重启手机？",
                        "Swap被大量使用(${usedSize}MB)，短时间内很难完成回收。\n因此需要重启手机来完成此操作，请确保你的重要数据都已保存！！！", {
                    swapConfig.edit().putBoolean(SpfConfig.SWAP_SPF_SWAP, false).apply()
                    swapModuleUtils.saveModuleConfig(swapConfig)
                    KeepShellPublic.doCmdSync("sync\nsleep 2\nsvc power reboot || reboot")
                })
            } else {
                swapOffDialog()
            }
        }

        // 自动lmk调节
        swap_auto_lmk.setOnClickListener {
            val checked = (it as CompoundButton).isChecked
            swapConfig.edit().putBoolean(SpfConfig.SWAP_SPF_AUTO_LMK, checked).apply()
            if (checked) {
                val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
                val info = ActivityManager.MemoryInfo()
                activityManager.getMemoryInfo(info)
                val utils = LMKUtils()
                utils.autoSetLMK(info.totalMem)
                swap_lmk_current.text = utils.getCurrent()
            } else {
                Toast.makeText(context, "需要重启手机才会恢复默认的LMK参数！", Toast.LENGTH_SHORT).show()
            }
        }

        // 是否支持zram
        if (!swapUtils.zramSupport) {
            swap_config_zram.visibility = View.GONE
            zram_stat.visibility = View.GONE
        }

        // swap启动
        btn_swap_create.setOnClickListener {
            swapCreateDialog()
        }

        // 调整zram大小操作
        btn_zram_resize.setOnClickListener {
            zramResizeDialog()
        }

        swappiness_adj.setOnClickListener {
            swappinessAdjDialog()
        }
    }

    // 获取新版本模块
    private fun swapModuleUpdateDialog () {
        val view = layoutInflater.inflate(R.layout.dialog_swap_module, null)
        val dialog = DialogHelper.customDialog(this, view)

        view.findViewById<View>(R.id.btn_cancel).setOnClickListener {
            dialog.dismiss()
        }
        view.findViewById<View>(R.id.btn_confirm).setOnClickListener {
            dialog.dismiss()

            try {
                val intent = Intent()
                intent.setAction(Intent.ACTION_VIEW)
                intent.data = Uri.parse(getString(R.string.swap_module_download_url))
                context.startActivity(intent)
            } catch (ex: java.lang.Exception) {
                Toast.makeText(context, "启动下载失败！", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun swapOffDialog() {
        val view = layoutInflater.inflate(R.layout.dialog_swap_delete, null)
        val deleteFile = view.findViewById<CompoundButton>(R.id.swap_delete_file)
        val dialog = DialogHelper.customDialog(this, view)

        view.findViewById<View>(R.id.btn_cancel).setOnClickListener {
            dialog.dismiss()
        }
        view.findViewById<View>(R.id.btn_confirm).setOnClickListener {
            dialog.dismiss()
            val delete = deleteFile.isChecked

            processBarDialog.showDialog(getString(R.string.swap_on_close))
            val run = Runnable {
                val timer = swapOffAwait()

                if (delete) {
                    swapUtils.swapDelete()
                } else {
                    swapUtils.swapOff()
                }

                timer.cancel()
                myHandler.post {
                    swapConfig.edit().putBoolean(SpfConfig.SWAP_SPF_SWAP, false).apply()
                    processBarDialog.hideDialog()
                    getSwaps()
                }
            }
            Thread(run).start()
        }
    }

    private var timer: Timer? = null
    private fun startTimer() {
        stopTimer()

        timer = Timer()
        timer!!.schedule(object : TimerTask() {
            override fun run() {
                getSwaps()
            }
        }, 0, 5000)
    }

    private fun stopTimer() {
        timer?.cancel()
        timer = null
    }

    private fun swappinessAdjDialog() {
        val view = layoutInflater.inflate(R.layout.dialog_swappines, null)

        val swappinessSeekBar = view.findViewById<SeekBar>(R.id.seekbar_swap_swappiness)
        val swappinessText = view.findViewById<TextView>(R.id.txt_zramstus_swappiness)
        val extraFreeSeekBar = view.findViewById<SeekBar>(R.id.seekbar_extra_free_kbytes)
        val extraFreeText = view.findViewById<TextView>(R.id.text_extra_free_kbytes)
        val watermarkScaleSeekBar = view.findViewById<SeekBar>(R.id.seekbar_watermark_scale_factor)
        val watermarkScaleText = view.findViewById<TextView>(R.id.text_watermark_scale_factor)


        swappinessSeekBar.progress = swapConfig.getInt(SpfConfig.SWAP_SPF_SWAPPINESS, 65)
        swappinessText.text = swappinessSeekBar.progress.toString()

        val extraFreeKbytes = KernelProrp.getProp("/proc/sys/vm/extra_free_kbytes")
        try {
            val bytes = extraFreeKbytes.toInt()
            extraFreeSeekBar.progress = swapConfig.getInt(SpfConfig.SWAP_SPF_EXTRA_FREE_KBYTES, bytes)
        } catch (ex: Exception) {
            extraFreeSeekBar.progress = swapConfig.getInt(SpfConfig.SWAP_SPF_EXTRA_FREE_KBYTES, 29615)
        }

        extraFreeText.text = extraFreeSeekBar.progress.toString() + "(" + (extraFreeSeekBar.progress / 1024) + "MB)"

        watermarkScaleSeekBar.progress = swapConfig.getInt(SpfConfig.SWAP_SPF_WATERMARK_SCALE, 100)
        watermarkScaleText.text = watermarkScaleSeekBar.progress.run {
            "$this(${this / 100F}%)"
        }

        swappinessSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
                swappinessText.text = p1.toString()
            }

            override fun onStartTrackingTouch(p0: SeekBar?) {
            }

            override fun onStopTrackingTouch(p0: SeekBar?) {
            }
        })

        // extra_free_kbytes设置
        extraFreeSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
                extraFreeText.text = p1.toString() + "(" + (p1 / 1024) + "MB)"
            }

            override fun onStartTrackingTouch(p0: SeekBar?) {
            }

            override fun onStopTrackingTouch(p0: SeekBar?) {
            }
        })

        watermarkScaleSeekBar.isEnabled = RootFile.fileExists("/proc/sys/vm/watermark_scale_factor")
        watermarkScaleSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
                watermarkScaleText.text = p1.run {
                    "$p1(${p1 / 100F}%)"
                }
            }

            override fun onStartTrackingTouch(p0: SeekBar?) {
            }

            override fun onStopTrackingTouch(p0: SeekBar?) {
            }
        })

        val dialog = DialogHelper.customDialog(this, view)
        view.findViewById<View>(R.id.btn_cancel).setOnClickListener {
            dialog.dismiss()
        }
        view.findViewById<View>(R.id.btn_confirm).setOnClickListener {
            dialog.dismiss()

            val swappiness = swappinessSeekBar.progress
            val extraFree = extraFreeSeekBar.progress
            val watermarkScale = watermarkScaleSeekBar.progress


            val config = swapConfig.edit()
                    .putInt(SpfConfig.SWAP_SPF_SWAPPINESS, swappiness)
                    .putInt(SpfConfig.SWAP_SPF_EXTRA_FREE_KBYTES, extraFree)

            KeepShellPublic.doCmdSync("echo $swappiness > /proc/sys/vm/swappiness")
            KeepShellPublic.doCmdSync("echo $extraFree > /proc/sys/vm/extra_free_kbytes")
            if (watermarkScaleSeekBar.isEnabled) {
                KeepShellPublic.doCmdSync("echo $watermarkScale > /proc/sys/vm/watermark_scale_factor")

                config.putInt(SpfConfig.SWAP_SPF_WATERMARK_SCALE, watermarkScale)
            }
            config.apply()

            myHandler.post {
                getSwaps()
            }
        }
    }

    private fun zramResizeDialog() {
        val view = layoutInflater.inflate(R.layout.dialog_zram_resize, null)
        val zramSizeBar = view.findViewById<SeekBar>(R.id.zram_size)
        val zramAutoStart = view.findViewById<CompoundButton>(R.id.zram_auto_start)
        val compactAlgorithm = view.findViewById<TextView>(R.id.zram_compact_algorithm)
        val zramSizeText = view.findViewById<TextView>(R.id.zram_size_text)

        zramAutoStart.isChecked = swapConfig.getBoolean(SpfConfig.SWAP_SPF_ZRAM, false)
        val compAlgorithmOptions = swapUtils.compAlgorithmOptions
        var currentAlgorithm = swapConfig.getString(SpfConfig.SWAP_SPF_ALGORITHM, swapUtils.compAlgorithm)
        if (!compAlgorithmOptions.contains(currentAlgorithm!!)) {
            currentAlgorithm = swapUtils.compAlgorithm
            swapConfig.edit().putString(SpfConfig.SWAP_SPF_ALGORITHM, currentAlgorithm).apply()
        }
        compactAlgorithm.text = currentAlgorithm
        compactAlgorithm.setOnClickListener {
            DialogItemChooserMini
                    .singleChooser(context, compAlgorithmOptions, compAlgorithmOptions.indexOf(currentAlgorithm))
                    .setTitle(R.string.swap_zram_comp_options)
                    .setCallback(object : DialogItemChooserMini.Callback {
                        override fun onConfirm(selected: List<SelectItem>, status: BooleanArray) {
                            val algorithm = selected.firstOrNull()?.value
                            algorithm?.run {
                                (it as TextView).text = algorithm
                            }
                        }
                    })
                    .show()
        }

        zramSizeBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                zramSizeText.text = (progress * 128).toString() + "MB"
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
            }
        })

        zramSizeBar.max = totalMem / 128
        var zramSize = swapConfig.getInt(SpfConfig.SWAP_SPF_ZRAM_SIZE, 0)
        if (zramSize > totalMem)
            zramSize = totalMem
        zramSizeBar.progress = zramSize / 128

        val dialog = DialogHelper.customDialog(this, view)
        view.findViewById<View>(R.id.btn_cancel).setOnClickListener {
            dialog.dismiss()
        }
        view.findViewById<View>(R.id.btn_confirm).setOnClickListener {
            dialog.dismiss()

            val sizeVal = zramSizeBar.progress * 128
            val autoStart = zramAutoStart.isChecked
            val algorithm = "" + compactAlgorithm.text

            processBarDialog.showDialog(getString(R.string.zram_resizing))
            swapConfig.edit()
                    .putInt(SpfConfig.SWAP_SPF_ZRAM_SIZE, sizeVal)
                    .putBoolean(SpfConfig.SWAP_SPF_ZRAM, autoStart)
                    .putString(SpfConfig.SWAP_SPF_ALGORITHM, algorithm)
                    .apply()

            val run = Thread {
                if (swapUtils.zramEnabled && algorithm != swapUtils.compAlgorithm || sizeVal != swapUtils.zramCurrentSizeMB) {
                    val timer = zramOffAwait()
                    swapUtils.zramOff()
                    timer.cancel()
                }
                myHandler.post {
                    processBarDialog.showDialog(getString(R.string.zram_resizing))
                }
                swapUtils.resizeZram(sizeVal, algorithm)

                getSwaps()
                myHandler.post {
                    processBarDialog.hideDialog()
                }
            }
            Thread(run).start()
        }
    }

    private fun swapCreateDialog() {
        val view = layoutInflater.inflate(R.layout.dialog_swap_create, null)
        val swapSize = view.findViewById<SeekBar>(R.id.swap_size)
        val swapSizeText = view.findViewById<TextView>(R.id.swap_size_text)

        val dialog = DialogHelper.customDialog(this, view)

        swapSize.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                swapSizeText.text = (progress * 128).toString() + "MB"
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
            }
        })

        var swapCurrentSize = 0
        if (swapUtils.swapExists) {
            swapCurrentSize = swapUtils.swapFileSize
        } else {
            swapCurrentSize = swapConfig.getInt(SpfConfig.SWAP_SPF_SWAP_SWAPSIZE, 0)
        }

        swapSize.progress = swapCurrentSize / 128
        swapSizeText.text = (swapSize.progress * 128).toString() + "MB"

        view.findViewById<View>(R.id.btn_cancel).setOnClickListener {
            dialog.dismiss()
        }
        view.findViewById<View>(R.id.btn_confirm).setOnClickListener {
            dialog.dismiss()

            val size = swapSize.progress * 128
            if (size < 1) {
                Scene.toast("请先设定SWAP大小！")
                return@setOnClickListener
            } else if (size == swapUtils.swapFileSize) {
                // 如果大小和已经创建的文件一致，跳过创建

                // 保存设置
                swapConfig.edit().putInt(SpfConfig.SWAP_SPF_SWAP_SWAPSIZE, size).apply()

                swapActiveDialog()
            } else {
                val run = Runnable {
                    val startTime = System.currentTimeMillis()
                    myHandler.post {
                        processBarDialog.showDialog(getString(R.string.file_creating))
                    }
                    swapUtils.mkswap(size)

                    // 保存设置
                    swapConfig.edit().putInt(SpfConfig.SWAP_SPF_SWAP_SWAPSIZE, size).apply()

                    getSwaps()
                    val time = System.currentTimeMillis() - startTime
                    myHandler.post {
                        processBarDialog.hideDialog()
                        val speed = (size * 1000.0 / time).toInt()
                        Toast.makeText(
                                context,
                                "Swapfile创建完毕，耗时${time / 1000}s，平均写入速度：${speed}MB/s",
                                Toast.LENGTH_LONG
                        ).show()
                        swapActiveDialog()
                    }
                }
                Thread(run).start()
            }
        }
    }

    private fun swapActiveDialog() {
        val view = layoutInflater.inflate(R.layout.dialog_swap_active, null)
        val dialog = DialogHelper.customDialog(this, view)

        val priorityHight = view.findViewById<CompoundButton>(R.id.swap_priority_high)
        val priorityMiddle = view.findViewById<CompoundButton>(R.id.swap_priority_middle)
        val priorityLow = view.findViewById<CompoundButton>(R.id.swap_priority_low)
        val autoStart = view.findViewById<CompoundButton>(R.id.swap_auto_start)
        val mountLoop = view.findViewById<CompoundButton>(R.id.swap_mount_loop)

        // 设置选中状态
        val radioGroupSimulator = RadioGroupSimulator(priorityHight, priorityMiddle, priorityLow)
        when (swapConfig.getInt(SpfConfig.SWAP_SPF_SWAP_PRIORITY, -2)) {
            5 -> priorityHight.isChecked = true
            0 -> priorityMiddle.isChecked = true
            else -> priorityLow.isChecked = true
        }
        mountLoop.isChecked = swapConfig.getBoolean(SpfConfig.SWAP_SPF_SWAP_USE_LOOP, false)
        autoStart.isChecked = swapConfig.getBoolean(SpfConfig.SWAP_SPF_SWAP, false)

        view.findViewById<View>(R.id.btn_cancel).setOnClickListener {
            dialog.dismiss()
        }
        view.findViewById<View>(R.id.btn_confirm).setOnClickListener {
            dialog.dismiss()

            val priority: Int
            when (radioGroupSimulator.checked) {
                priorityHight -> {
                    priority = 5
                }
                priorityMiddle -> {
                    priority = 0
                }
                priorityLow -> {
                    priority = -2
                }
                else -> {
                    return@setOnClickListener
                }
            }
            // 保存配置
            swapConfig.edit()
                    .putBoolean(SpfConfig.SWAP_SPF_SWAP_USE_LOOP, mountLoop.isChecked)
                    .putInt(SpfConfig.SWAP_SPF_SWAP_PRIORITY, priority)
                    .putBoolean(SpfConfig.SWAP_SPF_SWAP, autoStart.isChecked)
                    .apply()

            processBarDialog.showDialog("稍等...")
            Thread {
                val swapPriority = swapConfig.getInt(SpfConfig.SWAP_SPF_SWAP_PRIORITY, -2)
                if (swapPriority == 0) {
                    val zramPriority = swapUtils.zramPriority
                    if (zramPriority != null && zramPriority < 0) {
                        val timer = zramOffAwait()
                        swapUtils.zramOff()
                        timer.cancel()
                    }
                }
                val result = swapUtils.swapOn(
                        swapPriority,
                        swapConfig.getBoolean(SpfConfig.SWAP_SPF_SWAP_USE_LOOP, false)
                )
                if (result.isNotEmpty()) {
                    Scene.toast(result, Toast.LENGTH_LONG)
                }

                getSwaps()

                myHandler.post(showSwapOpened)
                processBarDialog.hideDialog()
            }.start()
        }
    }

    private var swapsTHRowCache: LinkedHashMap<String, String>? = null
    private val swapsTHRow: LinkedHashMap<String, String>
        get() {
            if (swapsTHRowCache == null) {
                swapsTHRowCache = LinkedHashMap<String, String>().apply {
                    put("path", getString(R.string.path))
                    put("type", getString(R.string.type))
                    put("size", getString(R.string.size))
                    put("used", getString(R.string.used))
                    put("priority", getString(R.string.order)) // put("priority", getString(R.string.priority))
                }
            }
            return swapsTHRowCache!!
        }

    internal val getSwaps = {
        val zramEnabled = swapUtils.zramEnabled
        val swappiness = KernelProrp.getProp("/proc/sys/vm/swappiness")
        val watermarkScale = KernelProrp.getProp("/proc/sys/vm/watermark_scale_factor")
        val memInfo = KernelProrp.getProp("/proc/meminfo")
        val swapFileExists = swapUtils.swapExists
        val currentSwap = swapUtils.sceneSwaps
        val rows = swapUtils.procSwaps
        val extraFreeKbytes = KernelProrp.getProp("/proc/sys/vm/extra_free_kbytes")
        // 压缩算法
        val compAlgorithm = swapUtils.compAlgorithm
        // zram统计
        val zramStatus = getZRamStatus(compAlgorithm)
        val swapFileSize = swapUtils.swapFileSize

        var loopName: String? = PropsUtils.getProp("vtools.swap.loop").split("/").lastOrNull()
        if (loopName != null && !loopName.contains("loop")) {
            loopName = null
        }

        val list = ArrayList<HashMap<String, String>>()
        list.add(swapsTHRow)

        var swapSize = 0f
        var swapFree = 0f
        // 按理说ZRAM的虚拟磁盘大小不取决于是否启用，但是为了避免引起误会，未启用还是刻意显示为0比较好
        val zramSize = if (zramEnabled) swapUtils.zramCurrentSizeMB else 0
        var zramFree = 0f
        for (i in 1 until rows.size) {
            val tr = LinkedHashMap<String, String>()
            val params = rows[i].split(" ").toMutableList()
            val path = params[0]
            tr["path"] = path
            tr["type"] = params[1].replace("file", "文件").replace("partition", "分区")

            val size = swapUsedSizeParseMB(params[2])
            // tr.put("size", if (size.length > 3) (size.substring(0, size.length - 3) + "m") else "0")
            tr["size"] = size

            val used = swapUsedSizeParseMB(params[3])
            // tr.put("used", if (used.length > 3) (used.substring(0, used.length - 3) + "m") else "0")
            tr["used"] = used

            tr["priority"] = params[4]
            list.add(tr)

            if (path.startsWith("/swapfile") || path.equals("/data/swapfile") || (loopName != null && path.contains(loopName))) {
                try {
                    swapSize = size.toFloat()
                    swapFree = size.toFloat() - used.toFloat()
                } catch (ex: java.lang.Exception) {
                }
            } else if (path.startsWith("/block/zram0") || path.startsWith("/dev/block/zram0")) {
                try {
                    // zramSize = size.toFloat()
                    zramFree = size.toFloat() - used.toFloat()
                } catch (ex: java.lang.Exception) {
                }
            }
        }
        val swaps = AdapterSwaplist(this, list)

        myHandler.post {
            try {
                txt_swap_size_display.text = swapFileSize.toString() + "MB"
                swap_usage.setData(swapSize, swapFree)
                zram_usage.setData(zramSize.toFloat(), zramFree)
                if (swapSize > 0) {
                    swap_usage_ratio.text = (100 - (swapFree * 100 / swapSize).toInt()).toString() + "%"
                } else {
                    swap_usage_ratio.text = "0%"
                }
                if (zramSize > 0 && zramFree > 0) {
                    zram_usage_ratio.text = (100 - (zramFree * 100 / zramSize).toInt()).toString() + "%"
                } else {
                    zram_usage_ratio.text = "0%"
                }

                swap_swappiness_display.text = swappiness
                watermark_scale_factor_display.text = watermarkScale

                list_swaps.adapter = swaps

                txt_mem.text = memInfo

                if (currentSwap.isNotEmpty()) {
                    btn_swap_close.visibility = View.VISIBLE
                    btn_swap_create.visibility = View.GONE
                    swap_state.text = getString(R.string.swap_state_using)
                } else {
                    btn_swap_close.visibility = View.GONE
                    btn_swap_create.visibility = View.VISIBLE
                    if (swapFileExists) {
                        swap_state.text = getString(R.string.swap_state_created)
                    } else {
                        swap_state.text = getString(R.string.swap_state_undefined)
                    }
                }

                if (zramEnabled) {
                    zram_state.text = getString(R.string.swap_state_using)
                } else {
                    zram_state.text = getString(R.string.swap_state_created)
                }

                swap_auto_lmk.isChecked = swapConfig.getBoolean(SpfConfig.SWAP_SPF_AUTO_LMK, false)
                val lmkUtils = LMKUtils()
                if (lmkUtils.supported() && !swapModuleUtils.magiskModuleInstalled) {
                    swap_lmk_current.text = lmkUtils.getCurrent()
                    swap_auto_lmk_wrap.visibility = View.VISIBLE
                } else {
                    swap_auto_lmk_wrap.visibility = View.GONE
                }

                extra_free_kbytes_display.text = extraFreeKbytes

                zram0_stat.text = zramStatus
                txt_swap_io.text = vmStat

                txt_zram_size_display.text = "${zramSize}MB"

                zram_compact_algorithm.text = compAlgorithm

                if (currentSwap.isNotEmpty()) {
                    txt_swap_auto_start.text = if (swapConfig.getBoolean(SpfConfig.SWAP_SPF_SWAP, false)) "重启后保持当前设置" else "重启后失效"
                } else {
                    txt_swap_auto_start.text = "--"
                }
                txt_zram_auto_start.text = if (swapConfig.getBoolean(SpfConfig.SWAP_SPF_ZRAM, false)) "重启后保持当前设置" else "重启后还原系统设定"
            } catch (ex: java.lang.Exception) {
            }
        }
    }

    private val vmStat: String
        get() {
            val vmstat = KernelProrp.getProp("/proc/vmstat")
            vmstat.run {
                val text = StringBuilder()
                try {
                    var prop = "";
                    var value = "";
                    for (row in split("\n")) {
                        if (row.startsWith("pswpin")) {
                            prop = "从SWAP读出："
                        } else if (row.startsWith("pswpout")) {
                            prop = "写入到SWAP："
                        } else {
                            continue
                        }
                        value = row.split(" ")[1]
                        text.append(prop)
                        val mb = (value.toLong() * 4 / 1024)
                        if (mb > 10240) {
                            text.append(String.format("%.2f", (mb / 1024f)))
                            text.append("GB\n")
                        } else {
                            text.append(mb)
                            text.append("MB\n")
                        }
                    }
                } catch (ex: Exception) {
                }

                return text.toString().trim()
            }
        }

    private fun getZRamStatus(compAlgorithm: String): String {
        return if (RootFile.fileExists("/proc/zraminfo")) {
            KernelProrp.getProp("/proc/zraminfo")
        } else {
            // 最大压缩流
            // val max_comp_streams = KernelProrp.getProp("/sys/block/zram0/max_comp_streams")
            // 存储在此磁盘中的未压缩数据大小
            var origDataSize = KernelProrp.getProp("/sys/block/zram0/orig_data_size")
            // 存储在此磁盘中的压缩数据大小
            var comprDataSize = KernelProrp.getProp("/sys/block/zram0/compr_data_size")
            if (origDataSize.isBlank() || comprDataSize.isBlank()) {
                val mmStat = KernelProrp.getProp("/sys/block/zram0/mm_stat").split("[ ]+".toRegex())
                if (mmStat.size > 1) {
                    origDataSize = mmStat[0]
                    comprDataSize = mmStat[1]
                }
            }

            // 为此磁盘分配的内存量
            val memUsedTotal = KernelProrp.getProp("/sys/block/zram0/mem_used_total")

            val zramWriteBackStat = if (swapUtils.zramWriteBackSupport) swapUtils.writeBackStat else null

            val generalStats = if (memUsedTotal.length > 0) {
                // 可用于存储的最大内存量
                val memLimit = KernelProrp.getProp("/sys/block/zram0/mem_limit")
                // 消耗的最大内存量
                val memUsedMax = KernelProrp.getProp("/sys/block/zram0/mem_used_max")
                // 写入此磁盘的相同元素填充页面的数量 不占用内存
                // val same_pages = KernelProrp.getProp("/sys/block/zram0/same_pages")
                // 压缩期间释放的页数
                // val pages_compacted = KernelProrp.getProp("/sys/block/zram0/pages_compacted")
                // 不可压缩数据
                // val huge_pages = KernelProrp.getProp("/sys/block/zram0/huge_pages")

                String.format(
                        getString(R.string.swap_zram_stat_format),
                        compAlgorithm,
                        zramInfoValueParseMB(origDataSize),
                        zramInfoValueParseMB(comprDataSize),
                        zramInfoValueParseMB(memUsedTotal),
                        zramInfoValueParseMB(memUsedMax),
                        if (memLimit == "0") "" else memLimit,
                        zramCompressionRatio(origDataSize, comprDataSize))
            } else {
                String.format(
                        getString(R.string.swap_zram_stat_format2),
                        compAlgorithm,
                        zramInfoValueParseMB(origDataSize),
                        zramInfoValueParseMB(comprDataSize),
                        zramCompressionRatio(origDataSize, comprDataSize))
            }
            if (zramWriteBackStat != null) {
                return generalStats + "\n\n" + String.format(
                        getString(R.string.swap_zram_writback_stat),
                        zramWriteBackStat.backingDev,
                        zramWriteBackStat.backed / 1024,
                        zramWriteBackStat.backReads / 1024,
                        zramWriteBackStat.backWrites / 1024
                )
            } else {
                return generalStats
            }
        }
    }

    private fun zramInfoValueParseMB(sizeStr: String): String {
        return try {
            (sizeStr.toLong() / 1024 / 1024).toString() + "MB"
        } catch (ex: java.lang.Exception) {
            sizeStr
        }
    }

    private fun zramCompressionRatio(origDataSize: String, comprDataSize: String): String {
        return try {
            (comprDataSize.toLong() * 1000 / origDataSize.toLong() / 10.0).toString() + "%"
        } catch (ex: java.lang.Exception) {
            "$comprDataSize/$origDataSize"
        }
    }

    private fun swapUsedSizeParseMB(sizeStr: String): String {
        return try {
            (sizeStr.toLong() / 1024).toString()
        } catch (ex: java.lang.Exception) {
            sizeStr
        }
    }

    override fun onResume() {
        super.onResume()
        title = getString(R.string.menu_swap)
        startTimer()
    }

    private var showSwapOpened = {
        Toast.makeText(context, getString(R.string.executed), Toast.LENGTH_LONG).show()
        processBarDialog.hideDialog()
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

    // 离开界面时保存配置
    override fun onPause() {
        stopTimer()
        swapModuleUtils.saveModuleConfig(swapConfig)
        super.onPause()
    }

    override fun onDestroy() {
        processBarDialog.hideDialog()
        super.onDestroy()
    }
}

package com.omarea.vtools.activities

import android.annotation.SuppressLint
import android.app.TimePickerDialog
import android.content.*
import android.os.BatteryManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.SpannableString
import android.text.Spanned
import android.text.style.AbsoluteSizeSpan
import android.view.View
import android.widget.*
import com.omarea.Scene
import com.omarea.common.shared.FileWrite
import com.omarea.common.shell.KeepShellPublic
import com.omarea.common.ui.DialogHelper
import com.omarea.data.EventBus
import com.omarea.data.EventType
import com.omarea.data.GlobalStatus
import com.omarea.library.device.BatteryCapacity
import com.omarea.library.shell.BatteryUtils
import com.omarea.store.SpfConfig
import com.omarea.vtools.R
import com.omarea.vtools.dialogs.DialogNumberInput
import kotlinx.android.synthetic.main.activity_battery.*
import java.util.*


class ActivityBattery : ActivityBase() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_battery)

        setBackArrow()

        onViewCreated()
    }

    private fun onViewCreated() {

        battery_exec_options.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {
            }

            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val defaultValue = spf.getInt(SpfConfig.CHARGE_SPF_EXEC_MODE, SpfConfig.CHARGE_SPF_EXEC_MODE_DEFAULT)
                val currentValue = when (position) {
                    0 -> SpfConfig.CHARGE_SPF_EXEC_MODE_SPEED_DOWN
                    1 -> SpfConfig.CHARGE_SPF_EXEC_MODE_SPEED_UP
                    2 -> SpfConfig.CHARGE_SPF_EXEC_MODE_SPEED_FORCE
                    else -> SpfConfig.CHARGE_SPF_EXEC_MODE_SPEED_UP
                }
                if (currentValue == defaultValue) {
                    return
                } else {
                    spf.edit().putInt(SpfConfig.CHARGE_SPF_EXEC_MODE, currentValue).apply()
                }
            }
        }


        ResumeCharge = "sh " + FileWrite.writePrivateShellFile("addin/resume_charge.sh", "addin/resume_charge.sh", this)
        spf = getSharedPreferences(SpfConfig.CHARGE_SPF, Context.MODE_PRIVATE)
        qcSettingSuupport = batteryUtils.qcSettingSupport()
        pdSettingSupport = batteryUtils.pdSupported()

        settings_qc.setOnClickListener {
            val checked = (it as CompoundButton).isChecked
            spf.edit().putBoolean(SpfConfig.CHARGE_SPF_QC_BOOSTER, checked).apply()
            if (checked) {
                notifyConfigChanged()
                Scene.toast(R.string.battery_auto_boot_desc, Toast.LENGTH_LONG)
            } else {
                Scene.toast(R.string.battery_qc_rehoot_desc, Toast.LENGTH_LONG)
            }
        }
        settings_qc.setOnCheckedChangeListener { _, isChecked ->
            battery_charge_speed_ext.visibility = if (isChecked) View.VISIBLE else View.GONE
            if (!isChecked) {
                battery_night_mode.isChecked = false
                spf.edit().putBoolean(SpfConfig.CHARGE_SPF_NIGHT_MODE, false).apply()
            }
        }
        settings_bp.setOnClickListener {
            spf.edit().putBoolean(SpfConfig.CHARGE_SPF_BP, settings_bp.isChecked).apply()
            //禁用电池保护：恢复充电功能
            if (!settings_bp.isChecked) {
                KeepShellPublic.doCmdSync(ResumeCharge)
            } else {
                notifyConfigChanged()
                Scene.toast(R.string.battery_auto_boot_desc, Toast.LENGTH_LONG)
            }
        }

        settings_bp_level.setOnSeekBarChangeListener(OnSeekBarChangeListener(Runnable {
            notifyConfigChanged()
        }, spf, battery_bp_level_desc))
        settings_qc_limit.setOnSeekBarChangeListener(OnSeekBarChangeListener2(Runnable {
            val level = spf.getInt(SpfConfig.CHARGE_SPF_QC_LIMIT, SpfConfig.CHARGE_SPF_QC_LIMIT_DEFAULT)
            if (spf.getBoolean(SpfConfig.CHARGE_SPF_QC_BOOSTER, false)) {
                batteryUtils.setChargeInputLimit(level, this)
            }
            notifyConfigChanged()
        }, spf, settings_qc_limit_desc))

        if (!qcSettingSuupport) {
            settings_qc.isEnabled = false
            spf.edit().putBoolean(SpfConfig.CHARGE_SPF_QC_BOOSTER, false).putBoolean(SpfConfig.CHARGE_SPF_NIGHT_MODE, false).apply()
            settings_qc_limit.isEnabled = false
            settings_qc_limit_current.visibility = View.GONE
        }

        if (!batteryUtils.bpSettingSupport()) {
            settings_bp.isEnabled = false
            spf.edit().putBoolean(SpfConfig.CHARGE_SPF_BP, false).apply()

            bp_cardview.visibility = View.GONE
        } else {
            bp_cardview.visibility = View.VISIBLE
        }

        if (pdSettingSupport) {
            settings_pd_support.visibility = View.VISIBLE
            settings_pd.setOnClickListener {
                val isChecked = (it as CompoundButton).isChecked
                batteryUtils.setAllowed(isChecked)
            }
            settings_pd.isChecked = batteryUtils.pdAllowed()
            settings_pd_state.text = if (batteryUtils.pdActive()) getString(R.string.battery_pd_active_1) else getString(R.string.battery_pd_active_0)
        } else {
            settings_pd_support.visibility = View.GONE
        }

        if (batteryUtils.stepChargeSupport()) {
            settings_step_charge.visibility = View.VISIBLE
            settings_step_charge_enabled.setOnClickListener {
                batteryUtils.setStepCharge((it as Checkable).isChecked)
            }
            settings_step_charge_enabled.isChecked = batteryUtils.getStepCharge()
        } else {
            settings_step_charge.visibility = View.GONE
        }

        btn_battery_history.setOnClickListener {
            try {
                val powerUsageIntent = Intent(Intent.ACTION_POWER_USAGE_SUMMARY)
                val resolveInfo = packageManager.resolveActivity(powerUsageIntent, 0)
                // check that the Battery app exists on this device
                if (resolveInfo != null) {
                    startActivity(powerUsageIntent)
                }
                /*
                Intent intent = new Intent("/");
                ComponentName cm = new ComponentName("com.android.settings","com.android.settings.BatteryInfo ");
                intent.setComponent(cm);
                intent.setAction("android.intent.action.VIEW");
                activity.startActivityForResult( intent , 0);
                */
            } catch (ex: Exception) {

            }
        }
        btn_battery_history_del.setOnClickListener {
            DialogHelper.confirm(this,
                    "需要重启",
                    "删除电池使用记录需要立即重启手机，是否继续？",
                    {
                        KeepShellPublic.doCmdSync(
                                "rm -f /data/system/batterystats-checkin.bin;" +
                                        "rm -f /data/system/batterystats-daily.xml;" +
                                        "rm -f /data/system/batterystats.bin;" +
                                        "rm -rf /data/system/battery-history;" +
                                        "rm -rf /data/charge_logger;" +
                                        "rm -rf /data/vendor/charge_logger;" +
                                        "sync;" +
                                        "sleep 2;" +
                                        "reboot;")
                    })
        }

        bp_disable_charge.setOnClickListener {
            KeepShellPublic.doCmdSync("sh " + FileWrite.writePrivateShellFile("addin/disable_charge.sh", "addin/disable_charge.sh", this.context))
            Scene.toast(R.string.battery_charge_disabled, Toast.LENGTH_LONG)
        }
        bp_enable_charge.setOnClickListener {
            KeepShellPublic.doCmdSync(ResumeCharge)
            Scene.toast(R.string.battery_charge_resumed, Toast.LENGTH_LONG)
        }

        battery_get_up.setText(minutes2Str(spf.getInt(SpfConfig.CHARGE_SPF_TIME_GET_UP, SpfConfig.CHARGE_SPF_TIME_GET_UP_DEFAULT)))
        battery_get_up.setOnClickListener {
            val nightModeGetUp = spf.getInt(SpfConfig.CHARGE_SPF_TIME_GET_UP, SpfConfig.CHARGE_SPF_TIME_GET_UP_DEFAULT)
            TimePickerDialog(this.context, TimePickerDialog.OnTimeSetListener { view, hourOfDay, minute ->
                spf.edit().putInt(SpfConfig.CHARGE_SPF_TIME_GET_UP, hourOfDay * 60 + minute).apply()
                battery_get_up.setText(String.format(getString(R.string.battery_night_mode_time), hourOfDay, minute))
                notifyConfigChanged()
            }, nightModeGetUp / 60, nightModeGetUp % 60, true).show()
        }

        battery_sleep.setText(minutes2Str(spf.getInt(SpfConfig.CHARGE_SPF_TIME_SLEEP, SpfConfig.CHARGE_SPF_TIME_SLEEP_DEFAULT)))
        battery_sleep.setOnClickListener {
            val nightModeSleep = spf.getInt(SpfConfig.CHARGE_SPF_TIME_SLEEP, SpfConfig.CHARGE_SPF_TIME_SLEEP_DEFAULT)
            TimePickerDialog(this.context, TimePickerDialog.OnTimeSetListener { _, hourOfDay, minute ->
                spf.edit().putInt(SpfConfig.CHARGE_SPF_TIME_SLEEP, hourOfDay * 60 + minute).apply()
                battery_sleep.setText(String.format(getString(R.string.battery_night_mode_time), hourOfDay, minute))
                notifyConfigChanged()
            }, nightModeSleep / 60, nightModeSleep % 60, true).show()
        }
        battery_night_mode.isChecked = spf.getBoolean(SpfConfig.CHARGE_SPF_NIGHT_MODE, false)
        battery_night_mode.setOnClickListener {
            val checked = (it as CompoundButton).isChecked
            if (checked && !spf.getBoolean(SpfConfig.CHARGE_SPF_QC_BOOSTER, false)) {
                Toast.makeText(this.context, "需要开启 " + getString(R.string.battery_qc_charger), Toast.LENGTH_LONG).show()
                it.isChecked = false
            } else {
                spf.edit().putBoolean(SpfConfig.CHARGE_SPF_NIGHT_MODE, checked).apply()
                notifyConfigChanged()
            }
        }
    }

    private fun minutes2Str(minutes: Int): String {
        return String.format(getString(R.string.battery_night_mode_time), minutes / 60, minutes % 60)
    }

    private var myHandler: Handler = Handler(Looper.getMainLooper())
    private var timer: Timer? = null
    private lateinit var batteryMAH: String
    private var temp = 0.0
    private var level = 0
    private var kernelCapacity = -1f
    private var powerChonnected = false
    private var voltage: Double = 0.toDouble()
    private var batteryUtils = BatteryUtils()
    private lateinit var spf: SharedPreferences

    @SuppressLint("ApplySharedPref", "SetTextI18n")
    override fun onResume() {
        super.onResume()

        title = getString(R.string.menu_battery)

        settings_qc.isChecked = spf.getBoolean(SpfConfig.CHARGE_SPF_QC_BOOSTER, false)
        settings_bp.isChecked = spf.getBoolean(SpfConfig.CHARGE_SPF_BP, false)
        val bpLevel = spf.getInt(SpfConfig.CHARGE_SPF_BP_LEVEL, SpfConfig.CHARGE_SPF_BP_LEVEL_DEFAULT)
        settings_bp_level.progress = bpLevel - 30
        battery_bp_level_desc.text = String.format(battery_bp_level_desc.context.getString(R.string.battery_bp_status), bpLevel, bpLevel - 20)
        settings_qc_limit.progress = spf.getInt(SpfConfig.CHARGE_SPF_QC_LIMIT, SpfConfig.CHARGE_SPF_QC_LIMIT_DEFAULT) / 100
        settings_qc_limit_desc.text = "" + spf.getInt(SpfConfig.CHARGE_SPF_QC_LIMIT, SpfConfig.CHARGE_SPF_QC_LIMIT_DEFAULT) + "mA"
        battery_exec_options.setSelection(when (spf.getInt(SpfConfig.CHARGE_SPF_EXEC_MODE, SpfConfig.CHARGE_SPF_EXEC_MODE_DEFAULT)) {
            SpfConfig.CHARGE_SPF_EXEC_MODE_SPEED_DOWN -> 0
            SpfConfig.CHARGE_SPF_EXEC_MODE_SPEED_UP -> 1
            SpfConfig.CHARGE_SPF_EXEC_MODE_SPEED_FORCE -> 2
            else -> 0
        })

        if (broadcast == null) {
            broadcast = object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) = try {
                    temp = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0).toDouble()
                    temp /= 10.0
                    level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0)
                    voltage = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0).toDouble()
                    if (voltage > 1000)
                        voltage /= 1000.0
                    if (voltage > 100)
                        voltage /= 100.0
                    else if (voltage > 10)
                        voltage /= 10.0
                    powerChonnected = intent.getIntExtra(BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_NOT_CHARGING) == BatteryManager.BATTERY_STATUS_CHARGING
                } catch (ex: Exception) {
                    print(ex.message)
                }
            }
        }
        registerReceiver(broadcast, IntentFilter(Intent.ACTION_BATTERY_CHANGED))

        val battrystatus = findViewById(R.id.battrystatus) as TextView
        batteryMAH = BatteryCapacity().getBatteryCapacity(this).toString() + "mAh" + "   "
        temp = GlobalStatus.updateBatteryTemperature().toDouble()

        timer = Timer()

        var limit = ""
        var batteryInfo: String
        var usbInfo: String
        timer!!.schedule(object : TimerTask() {
            override fun run() {
                var pdAllowed = false
                var pdActive = false
                if (pdSettingSupport) {
                    pdAllowed = batteryUtils.pdAllowed()
                    pdActive = batteryUtils.pdActive()
                }
                if (qcSettingSuupport) {
                    limit = batteryUtils.getQcLimit()
                }
                batteryInfo = batteryUtils.batteryInfo
                usbInfo = batteryUtils.usbInfo
                kernelCapacity = batteryUtils.getKernelCapacity(level)

                myHandler.post {
                    try {
                        if (qcSettingSuupport) {
                            settings_qc_limit_current.text = getString(R.string.battery_reality_limit) + limit
                        }
                        battrystatus.text = getString(R.string.battery_title) +
                                batteryMAH +
                                temp + "°C   " +
                                voltage + "v"
                        if (kernelCapacity > -1) {
                            val str = "" + kernelCapacity + "%"
                            val ss = SpannableString(str)
                            if (str.contains(".")) {
                                val small = AbsoluteSizeSpan((battrystatus_level.textSize * 0.3).toInt(), false)
                                ss.setSpan(small, str.indexOf("."), str.lastIndexOf("%"), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                                val medium = AbsoluteSizeSpan((battrystatus_level.textSize * 0.5).toInt(), false)
                                ss.setSpan(medium, str.indexOf("%"), str.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                            }
                            battrystatus_level.text = ss
                        } else {
                            battrystatus_level.text = "" + level + "%"
                        }

                        battery_capacity_chart.setData(100f, 100f - level, temp.toFloat())

                        settings_qc.isChecked = spf.getBoolean(SpfConfig.CHARGE_SPF_QC_BOOSTER, false)
                        battery_uevent.text = batteryInfo
                        battery_usb_uevent.text = usbInfo

                        if (pdSettingSupport) {
                            settings_pd.isChecked = pdAllowed
                            settings_pd_state.text = if (pdActive) getString(R.string.battery_pd_active_1) else getString(R.string.battery_pd_active_0)
                        }
                    } catch (ex: java.lang.Exception) {
                    }
                }
            }
        }, 0, 3000)
        updateBatteryForgery()
    }

    private fun batteryForgeryRatio() {
        DialogNumberInput(this).showDialog(object : DialogNumberInput.DialogNumberInputRequest {
            override var min = -1
            override var max = 100
            override var default = batteryUtils.getCpacity()

            override fun onApply(value: Int) {
                batteryUtils.setCapacity(value)
                updateBatteryForgery()
            }
        })
    }

    private fun batteryForgeryChargeFull() {
        DialogNumberInput(this).showDialog(object : DialogNumberInput.DialogNumberInputRequest {
            override var min = 1000
            override var max = 20000
            override var default = batteryUtils.getChargeFull()

            override fun onApply(value: Int) {
                batteryUtils.setChargeFull(value)
                updateBatteryForgery()
            }
        })
    }

    private fun updateBatteryForgery() {
        val cpacity = batteryUtils.getCpacity()
        val chargeFull = batteryUtils.getChargeFull()
        if (cpacity > 0) {
            battery_forgery_ratio.text = cpacity.toString() + "%"
            battery_forgery_ratio.setOnClickListener {
                batteryForgeryRatio()
            }
        } else {
            battery_forgery_ratio.setOnClickListener {
                Toast.makeText(this, getString(R.string.device_unsupport), Toast.LENGTH_SHORT).show()
            }
        }
        if (chargeFull > 0) {
            battery_forgery_full_now.text = chargeFull.toString() + "mAh"
            battery_forgery_full_now.setOnClickListener {
                batteryForgeryChargeFull()
            }
        } else {
            battery_forgery_full_now.setOnClickListener {
                Toast.makeText(this, getString(R.string.device_unsupport), Toast.LENGTH_SHORT).show()
            }
        }

        if (cpacity < 1 && chargeFull < 1) {
            battery_forgery.visibility = View.GONE
        } else {
            battery_forgery.visibility = View.VISIBLE
        }
    }

    override fun onPause() {
        super.onPause()
        if (timer != null) {
            timer!!.cancel()
            timer = null
        }
        try {
            if (broadcast != null)
                unregisterReceiver(broadcast)
        } catch (ex: Exception) {
        }
    }

    override fun onDestroy() {
        try {
            if (broadcast != null)
                unregisterReceiver(broadcast)
        } catch (ex: Exception) {
        }
        broadcast = null
        super.onDestroy()
    }

    private var broadcast: BroadcastReceiver? = null
    private var qcSettingSuupport = false
    private var pdSettingSupport = false
    private var ResumeCharge = ""

    private fun notifyConfigChanged() {
        Thread(Runnable {
            EventBus.publish(EventType.CHARGE_CONFIG_CHANGED)
        }).start()
    }

    class OnSeekBarChangeListener(private var next: Runnable, private var spf: SharedPreferences, private var battery_bp_level_desc: TextView) : SeekBar.OnSeekBarChangeListener {
        override fun onStopTrackingTouch(seekBar: SeekBar?) {
            val progress = seekBar!!.progress + 30
            if (spf.getInt(SpfConfig.CHARGE_SPF_BP_LEVEL, Int.MIN_VALUE) == progress) {
                return
            }
            spf.edit().putInt(SpfConfig.CHARGE_SPF_BP_LEVEL, progress).apply()
            next.run()
        }

        override fun onStartTrackingTouch(seekBar: SeekBar?) {
        }

        override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
            battery_bp_level_desc.text = String.format(battery_bp_level_desc.context.getString(R.string.battery_bp_status), progress + 30, progress + 10)
        }
    }

    class OnSeekBarChangeListener2(private var next: Runnable, private var spf: SharedPreferences, private var settings_qc_limit_desc: TextView) : SeekBar.OnSeekBarChangeListener {
        override fun onStopTrackingTouch(seekBar: SeekBar?) {
            val progress = seekBar!!.progress * 100
            if (spf.getInt(SpfConfig.CHARGE_SPF_QC_LIMIT, SpfConfig.CHARGE_SPF_QC_LIMIT_DEFAULT) == progress) {
                return
            }
            spf.edit().putInt(SpfConfig.CHARGE_SPF_QC_LIMIT, progress).apply()
            next.run()
        }

        override fun onStartTrackingTouch(seekBar: SeekBar?) {

        }

        override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
            settings_qc_limit_desc.text = "" + progress * 100 + "mA"
        }
    }
}

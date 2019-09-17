package com.omarea.vtools.fragments

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.TimePickerDialog
import android.content.*
import android.os.BatteryManager
import android.os.Bundle
import android.os.Handler
import android.support.design.widget.Snackbar
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import com.omarea.charger_booster.BatteryInfo
import com.omarea.charger_booster.ServiceBattery
import com.omarea.common.shell.KeepShellPublic
import com.omarea.common.ui.DialogHelper
import com.omarea.shell_utils.BatteryUtils
import com.omarea.store.SpfConfig
import com.omarea.vtools.R
import kotlinx.android.synthetic.main.fragment_battery.*
import java.util.*


class FragmentBattery : Fragment() {
    internal lateinit var view: View

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        view = inflater.inflate(R.layout.fragment_battery, container, false)
        return view
    }

    private var myHandler: Handler = Handler()
    private var timer: Timer? = null
    private lateinit var batteryMAH: String
    private var temp = 0.0
    private var level = 0
    private var powerChonnected = false
    private var voltage: Double = 0.toDouble()
    private var batteryUnits = BatteryUtils()
    private lateinit var spf: SharedPreferences

    @SuppressLint("ApplySharedPref", "SetTextI18n")
    override fun onResume() {
        super.onResume()
        if (isDetached) {
            return
        }

        settings_qc.isChecked = spf.getBoolean(SpfConfig.CHARGE_SPF_QC_BOOSTER, false)
        settings_bp.isChecked = spf.getBoolean(SpfConfig.CHARGE_SPF_BP, false)
        settings_bp_level.progress = spf.getInt(SpfConfig.CHARGE_SPF_BP_LEVEL, SpfConfig.CHARGE_SPF_BP_LEVEL_DEFAULT)
        val bpLevel = spf.getInt(SpfConfig.CHARGE_SPF_BP_LEVEL, SpfConfig.CHARGE_SPF_BP_LEVEL_DEFAULT)
        battery_bp_level_desc.text = "达到$bpLevel%停止充电，低于${bpLevel - 20}%恢复"
        settings_qc_limit.progress = spf.getInt(SpfConfig.CHARGE_SPF_QC_LIMIT, 3300) / 100
        settings_qc_limit_desc.text = "" + spf.getInt(SpfConfig.CHARGE_SPF_QC_LIMIT, 5000) + "mA"

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
            context!!.registerReceiver(broadcast, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        }

        val battrystatus = view.findViewById(R.id.battrystatus) as TextView
        batteryMAH = BatteryInfo().getBatteryCapacity(this.context!!).toString() + "mAh" + "   "
        val context = context!!.applicationContext
        serviceRunning = ServiceBattery.serviceIsRunning(context)

        timer = Timer()

        var limit = ""
        var batteryInfo = ""
        timer!!.schedule(object : TimerTask() {
            override fun run() {
                if (isDetached) {
                    return
                }
                var pdAllowed = false
                var pdActive = false
                if (pdSettingSupport) {
                    pdAllowed = batteryUnits.pdAllowed()
                    pdActive = batteryUnits.pdActive()
                }
                if (qcSettingSuupport) {
                    limit = batteryUnits.getqcLimit()
                }
                batteryInfo = batteryUnits.batteryInfo

                myHandler.post {
                    if (qcSettingSuupport) {
                        settings_qc_limit_current.text = getString(R.string.battery_reality_limit) + limit
                    }
                    battrystatus.text = getString(R.string.battery_title) +
                            batteryMAH +
                            temp + "°C   " +
                            level + "%    " +
                            voltage + "v"

                    settings_qc.isChecked = spf.getBoolean(SpfConfig.CHARGE_SPF_QC_BOOSTER, false) && serviceRunning
                    battery_uevent.text = batteryInfo

                    if (pdSettingSupport) {
                        settings_pd.isChecked = pdAllowed
                        settings_pd_state.text = if (pdActive) getString(R.string.battery_pd_active_1) else getString(R.string.battery_pd_active_0)
                    }
                }
            }
        }, 0, 3000)
    }

    internal var serviceRunning = false

    override fun onPause() {
        super.onPause()
        if (timer != null) {
            timer!!.cancel()
            timer = null
        }
        try {
            if (broadcast != null)
                context!!.unregisterReceiver(broadcast)
        } catch (ex: Exception) {
        }
    }

    override fun onDestroy() {
        try {
            if (broadcast != null)
                context!!.unregisterReceiver(broadcast)
        } catch (ex: Exception) {
        }
        super.onDestroy()
    }

    private var broadcast: BroadcastReceiver? = null
    private var qcSettingSuupport = false
    private var pdSettingSupport = false
    private var ResumeCharge = ""

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        ResumeCharge = "sh " + com.omarea.common.shared.FileWrite.writePrivateShellFile("addin/resume_charge.sh", "addin/resume_charge.sh", this.context!!)
        spf = context!!.getSharedPreferences(SpfConfig.CHARGE_SPF, Context.MODE_PRIVATE)
        qcSettingSuupport = batteryUnits.qcSettingSuupport()
        pdSettingSupport = batteryUnits.pdSupported()

        settings_qc.setOnClickListener {
            val checked = (it as Switch).isChecked
            spf.edit().putBoolean(SpfConfig.CHARGE_SPF_QC_BOOSTER, checked).apply()
            if (checked) {
                //启用电池服务
                startBatteryService()
                Snackbar.make(this.view, R.string.battery_auto_boot_desc, Snackbar.LENGTH_LONG).show()
            } else {
                Snackbar.make(this.view, R.string.battery_qc_rehoot_desc, Snackbar.LENGTH_LONG).show()
            }
        }
        settings_qc.setOnCheckedChangeListener { buttonView, isChecked ->
            battery_night_mode_configs.visibility = if(isChecked) View.VISIBLE else View.GONE
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
                //启用电池服务
                startBatteryService()
                Snackbar.make(this.view, R.string.battery_auto_boot_desc, Snackbar.LENGTH_LONG).show()
            }
        }

        settings_bp_level.setOnSeekBarChangeListener(OnSeekBarChangeListener(Runnable {
            startBatteryService()
        }, spf, battery_bp_level_desc))
        settings_qc_limit.setOnSeekBarChangeListener(OnSeekBarChangeListener2(Runnable {
            val level = spf.getInt(SpfConfig.CHARGE_SPF_QC_LIMIT, 5000)
            startBatteryService()
            if (spf.getBoolean(SpfConfig.CHARGE_SPF_QC_BOOSTER, false)) {
                batteryUnits.setChargeInputLimit(level, context!!)
            }
        }, spf, settings_qc_limit_desc))

        if (!qcSettingSuupport) {
            settings_qc.isEnabled = false
            spf.edit().putBoolean(SpfConfig.CHARGE_SPF_QC_BOOSTER, false).putBoolean(SpfConfig.CHARGE_SPF_NIGHT_MODE, false).apply()
            settings_qc_limit.isEnabled = false
            settings_qc_limit_current.visibility = View.GONE
        }

        if (!batteryUnits.bpSettingSuupport()) {
            settings_bp.isEnabled = false
            spf.edit().putBoolean(SpfConfig.CHARGE_SPF_BP, false).apply()

            bp_cardview.visibility = View.GONE
        } else {
            bp_cardview.visibility = View.VISIBLE
        }

        if (pdSettingSupport) {
            settings_pd_support.visibility = View.VISIBLE
            settings_pd.setOnClickListener {
                val isChecked = (it as Switch).isChecked
                batteryUnits.setAllowed(isChecked)
            }
            settings_pd.isChecked = batteryUnits.pdAllowed()
            settings_pd_state.text = if (batteryUnits.pdActive())getString(R.string.battery_pd_active_1) else getString(R.string.battery_pd_active_0)
        } else {
            settings_pd_support.visibility = View.GONE
        }

        btn_battery_history.setOnClickListener {
            try {
                val powerUsageIntent = Intent(Intent.ACTION_POWER_USAGE_SUMMARY)
                val resolveInfo = context!!.packageManager.resolveActivity(powerUsageIntent, 0)
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
            DialogHelper.animDialog(AlertDialog.Builder(context!!)
                    .setTitle("需要重启")
                    .setMessage("删除电池使用记录需要立即重启手机，是否继续？")
                    .setPositiveButton(R.string.btn_confirm, DialogInterface.OnClickListener { dialog, which ->
                        KeepShellPublic.doCmdSync("rm -f /data/system/batterystats-checkin.bin;rm -f /data/system/batterystats-daily.xml;rm -f /data/system/batterystats.bin;sync;sleep 2; reboot;")
                    })
                    .setNegativeButton(R.string.btn_cancel, DialogInterface.OnClickListener { dialog, which -> }))
        }

        bp_disable_charge.setOnClickListener {
            KeepShellPublic.doCmdSync("sh " + com.omarea.common.shared.FileWrite.writePrivateShellFile("addin/disable_charge.sh", "addin/disable_charge.sh", this.context!!))
            Snackbar.make(this.view, R.string.battery_charge_disabled, Toast.LENGTH_LONG).show()
        }
        bp_enable_charge.setOnClickListener {
            KeepShellPublic.doCmdSync(ResumeCharge)
            Snackbar.make(this.view, R.string.battery_charge_resumed, Toast.LENGTH_LONG).show()
        }

        val nightModeGetUp = spf.getInt(SpfConfig.CHARGE_SPF_TIME_GET_UP, SpfConfig.CHARGE_SPF_TIME_GET_UP_DEFAULT)
        battery_get_up.setText(String.format(getString(R.string.battery_night_mode_time), nightModeGetUp / 60, nightModeGetUp % 60))
        battery_get_up.setOnClickListener {
            TimePickerDialog(this.context, TimePickerDialog.OnTimeSetListener { view, hourOfDay, minute ->
                spf.edit().putInt(SpfConfig.CHARGE_SPF_TIME_GET_UP, hourOfDay * 60 + minute).apply()
                battery_get_up.setText(String.format(getString(R.string.battery_night_mode_time), hourOfDay, minute))
            }, nightModeGetUp / 60, nightModeGetUp % 60, true).show()
        }
        val nightModeSleep = spf.getInt(SpfConfig.CHARGE_SPF_TIME_SLEEP, SpfConfig.CHARGE_SPF_TIME_SLEEP_DEFAULT)
        battery_sleep.setText(String.format(getString(R.string.battery_night_mode_time), nightModeSleep / 60, nightModeSleep % 60))
        battery_sleep.setOnClickListener {
            TimePickerDialog(this.context, TimePickerDialog.OnTimeSetListener { view, hourOfDay, minute ->
                spf.edit().putInt(SpfConfig.CHARGE_SPF_TIME_SLEEP, hourOfDay * 60 + minute).apply()
                battery_sleep.setText(String.format(getString(R.string.battery_night_mode_time), hourOfDay, minute))
            }, nightModeSleep / 60, nightModeSleep % 60, true).show()
        }
        battery_night_mode.isChecked = spf.getBoolean(SpfConfig.CHARGE_SPF_NIGHT_MODE, false)
        battery_night_mode.setOnClickListener {
            val checked = (it as Switch).isChecked
            if (checked && !spf.getBoolean(SpfConfig.CHARGE_SPF_QC_BOOSTER, false)) {
                Toast.makeText(this.context, "需要开启 " + getString(R.string.battery_qc_charger), Toast.LENGTH_LONG).show()
                it.isChecked = false
            } else {
                spf.edit().putBoolean(SpfConfig.CHARGE_SPF_NIGHT_MODE, checked).apply()
            }
        }
    }

    class OnSeekBarChangeListener(private var next: Runnable, private var spf: SharedPreferences, private var battery_bp_level_desc: TextView) : SeekBar.OnSeekBarChangeListener {
        override fun onStopTrackingTouch(seekBar: SeekBar?) {
            val progress = seekBar!!.progress
            if (spf.getInt(SpfConfig.CHARGE_SPF_BP_LEVEL, Int.MIN_VALUE) == progress) {
                return
            }
            spf.edit().putInt(SpfConfig.CHARGE_SPF_BP_LEVEL, progress).apply()
            next.run()
        }

        override fun onStartTrackingTouch(seekBar: SeekBar?) {
        }

        override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
            battery_bp_level_desc.text = String.format(battery_bp_level_desc.context.getString(R.string.battery_bp_status), progress, progress - 20)
        }
    }

    class OnSeekBarChangeListener2(private var next: Runnable, private var spf: SharedPreferences, private var settings_qc_limit_desc: TextView) : SeekBar.OnSeekBarChangeListener {
        override fun onStopTrackingTouch(seekBar: SeekBar?) {
            val progress = seekBar!!.progress * 100
            if (spf.getInt(SpfConfig.CHARGE_SPF_QC_LIMIT, Int.MIN_VALUE) == progress) {
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

    //启动电池服务
    private fun startBatteryService() {
        try {
            val intent = Intent(context, ServiceBattery::class.java)
            context!!.startService(intent)
            serviceRunning = ServiceBattery.serviceIsRunning(context!!)
        } catch (ex: Exception) {
        }
    }

    companion object {
        fun createPage(): Fragment {
            val fragment = FragmentBattery()
            return fragment
        }
    }
}

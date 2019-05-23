package com.omarea.vtools.fragments

import android.annotation.SuppressLint
import android.app.AlertDialog
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
import com.omarea.shared.FileWrite
import com.omarea.shared.SpfConfig
import com.omarea.shell.KeepShellPublic
import com.omarea.shell.units.BatteryUnit
import com.omarea.vtools.R
import com.omarea.vtools.services.ServiceBattery
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
    private var batteryUnits = BatteryUnit()
    private lateinit var spf: SharedPreferences

    @SuppressLint("ApplySharedPref", "SetTextI18n")
    override fun onResume() {
        super.onResume()
        if (isDetached) {
            return
        }

        settings_qc.isChecked = spf.getBoolean(SpfConfig.CHARGE_SPF_QC_BOOSTER, false)
        settings_bp.isChecked = spf.getBoolean(SpfConfig.CHARGE_SPF_BP, false)
        settings_bp_level.progress = spf.getInt(SpfConfig.CHARGE_SPF_BP_LEVEL, 85)
        accessbility_bp_level_desc.text = "充电限制电量：" + spf.getInt(SpfConfig.CHARGE_SPF_BP_LEVEL, 85) + "%"
        settings_qc_limit.progress = spf.getInt(SpfConfig.CHARGE_SPF_QC_LIMIT, 5000)
        settings_qc_limit_desc.text = "设定上限电流：" + spf.getInt(SpfConfig.CHARGE_SPF_QC_LIMIT, 5000) + "mA"

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
        batteryMAH = batteryUnits.batteryMAH + "   "
        val context = context!!.applicationContext
        serviceRunning = ServiceBattery.serviceIsRunning(context)

        timer = Timer()

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
                myHandler.post {
                    if (qcSettingSuupport) {
                        settings_qc_limit_current.text = "实际上限电流：" + batteryUnits.getqcLimit()
                    }
                    battrystatus.text = "电池信息：" +
                            batteryMAH +
                            temp + "°C   " +
                            level + "%    " +
                            voltage + "v"

                    settings_qc.isChecked = spf.getBoolean(SpfConfig.CHARGE_SPF_QC_BOOSTER, false) && serviceRunning
                    battery_uevent.text = batteryUnits.batteryInfo

                    if (pdSettingSupport) {
                        settings_pd.isChecked = pdAllowed
                        settings_pd_state.text = if (pdActive) "已进入PD快充模式" else "未进入PD快充模式"
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
    private var ResumeChanger = ""

    @SuppressLint("ApplySharedPref")
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        ResumeChanger = "sh " + FileWrite.writePrivateShellFile("custom/battery/resume_charge.sh", "resume_charge.sh", this.context!!)
        spf = context!!.getSharedPreferences(SpfConfig.CHARGE_SPF, Context.MODE_PRIVATE)
        qcSettingSuupport = batteryUnits.qcSettingSuupport()
        pdSettingSupport = batteryUnits.pdSupported()

        settings_qc.setOnClickListener {
            spf.edit().putBoolean(SpfConfig.CHARGE_SPF_QC_BOOSTER, settings_qc.isChecked).commit()
            if (!settings_qc.isChecked) {
                Snackbar.make(this.view, "充电加速服务已禁用，可能需要重启手机才能恢复默认设置！", Snackbar.LENGTH_SHORT).show()
            } else {
                //启用电池服务
                startBatteryService()
                Snackbar.make(this.view, "OK！如果你要手机重启后自动开启本功能，请允许Scene开机自启！", Snackbar.LENGTH_SHORT).show()
            }
        }
        settings_bp.setOnClickListener {
            spf.edit().putBoolean(SpfConfig.CHARGE_SPF_BP, settings_bp.isChecked).commit()
            //禁用电池保护：恢复充电功能
            if (!settings_bp.isChecked) {
                KeepShellPublic.doCmdSync(ResumeChanger)
            } else {
                //启用电池服务
                startBatteryService()
                Snackbar.make(this.view, "OK！如果你要手机重启后自动开启本功能，请允许Scene开机自启！", Snackbar.LENGTH_SHORT).show()
            }
        }

        settings_bp_level.setOnSeekBarChangeListener(OnSeekBarChangeListener(Runnable {
            startBatteryService()
        }, spf, accessbility_bp_level_desc))
        settings_qc_limit.setOnSeekBarChangeListener(OnSeekBarChangeListener2(Runnable {
            val level = spf.getInt(SpfConfig.CHARGE_SPF_QC_LIMIT, 5000)
            startBatteryService()
            batteryUnits.setChargeInputLimit(level)
        }, spf, settings_qc_limit_desc))

        if (!qcSettingSuupport) {
            settings_qc.isEnabled = false
            spf.edit().putBoolean(SpfConfig.CHARGE_SPF_QC_BOOSTER, false).commit()
            settings_qc_limit.isEnabled = false
            settings_qc_limit_current.visibility = View.GONE
        }

        if (!batteryUnits.bpSettingSuupport()) {
            settings_bp.isEnabled = false
            spf.edit().putBoolean(SpfConfig.CHARGE_SPF_BP, false).commit()

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
            var pdAllowed = false
            var pdActive = false
            pdAllowed = batteryUnits.pdAllowed()
            pdActive = batteryUnits.pdActive()
            settings_pd.isChecked = pdAllowed
            settings_pd_state.text = if (pdActive) "已进入PD快充模式" else "未进入PD快充模式"
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
            val dialog = AlertDialog.Builder(context!!)
                    .setTitle("需要重启")
                    .setMessage("删除电池使用记录需要立即重启手机，是否继续？")
                    .setPositiveButton(R.string.btn_confirm, DialogInterface.OnClickListener { dialog, which ->
                        KeepShellPublic.doCmdSync("rm -f /data/system/batterystats-checkin.bin;rm -f /data/system/batterystats-daily.xml;rm -f /data/system/batterystats.bin;sync;sleep 2; reboot;")
                    })
                    .setNegativeButton(R.string.btn_cancel, DialogInterface.OnClickListener { dialog, which -> })
                    .create()
            dialog.window!!.setWindowAnimations(R.style.windowAnim)
            dialog.show()
        }

        bp_disable_charge.setOnClickListener {
            KeepShellPublic.doCmdSync("sh " + FileWrite.writePrivateShellFile("custom/battery/disable_charge.sh", "disable_charge.sh", this.context!!))
            Toast.makeText(context!!, "充电功能已禁止！", Toast.LENGTH_SHORT).show()
        }
        bp_enable_charge.setOnClickListener {
            KeepShellPublic.doCmdSync(ResumeChanger)
            Toast.makeText(context!!, "充电功能已恢复！", Toast.LENGTH_SHORT).show()
        }
    }

    class OnSeekBarChangeListener(private var next: Runnable, private var spf: SharedPreferences, private var accessbility_bp_level_desc: TextView) : SeekBar.OnSeekBarChangeListener {
        override fun onStopTrackingTouch(seekBar: SeekBar?) {
            val progress = seekBar!!.progress
            if (spf.getInt(SpfConfig.CHARGE_SPF_BP_LEVEL, Int.MIN_VALUE) == progress) {
                return
            }
            spf.edit().putInt(SpfConfig.CHARGE_SPF_BP_LEVEL, progress).commit()
            next.run()
        }

        override fun onStartTrackingTouch(seekBar: SeekBar?) {

        }

        override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
            accessbility_bp_level_desc.text = "充电限制电量：$progress%"
        }
    }

    class OnSeekBarChangeListener2(private var next: Runnable, private var spf: SharedPreferences, private var settings_qc_limit_desc: TextView) : SeekBar.OnSeekBarChangeListener {
        @SuppressLint("ApplySharedPref")
        override fun onStopTrackingTouch(seekBar: SeekBar?) {
            val progress = seekBar!!.progress
            if (spf.getInt(SpfConfig.CHARGE_SPF_QC_LIMIT, Int.MIN_VALUE) == progress) {
                return
            }
            spf.edit().putInt(SpfConfig.CHARGE_SPF_QC_LIMIT, progress).commit()
            next.run()
        }

        override fun onStartTrackingTouch(seekBar: SeekBar?) {

        }

        override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
            settings_qc_limit_desc.text = "充电上限电流：" + progress + "mA"
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

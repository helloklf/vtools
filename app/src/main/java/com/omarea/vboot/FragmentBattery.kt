package com.omarea.vboot

import android.annotation.SuppressLint
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
import android.widget.TextView
import com.omarea.shared.Consts
import com.omarea.shared.SpfConfig
import com.omarea.shell.SuDo
import com.omarea.shell.units.BatteryUnit
import kotlinx.android.synthetic.main.layout_battery.*
import java.util.*


class FragmentBattery : Fragment() {
    lateinit internal var view: View

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        view = inflater!!.inflate(R.layout.layout_battery, container, false)
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

    @SuppressLint("ApplySharedPref")
    override fun onResume() {
        super.onResume()

        settings_qc.isChecked = spf.getBoolean(SpfConfig.CHARGE_SPF_QC_BOOSTER, false)
        settings_bp.isChecked = spf.getBoolean(SpfConfig.CHARGE_SPF_BP, false)
        settings_bp_level.setProgress(spf.getInt(SpfConfig.CHARGE_SPF_BP_LEVEL, 85))
        accessbility_bp_level_desc.setText("充电限制电量：" + spf.getInt(SpfConfig.CHARGE_SPF_BP_LEVEL, 85) + "%")
        settings_qc_limit.setProgress(spf.getInt(SpfConfig.CHARGE_SPF_QC_LIMIT, 5000))
        settings_qc_limit_desc.setText("设定上限电流：" + spf.getInt(SpfConfig.CHARGE_SPF_QC_LIMIT, 5000) + "mA")


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

    @SuppressLint("ApplySharedPref")
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        spf = context!!.getSharedPreferences(SpfConfig.CHARGE_SPF, Context.MODE_PRIVATE)
        qcSettingSuupport = batteryUnits.qcSettingSuupport()

        settings_qc.setOnClickListener {
            spf.edit().putBoolean(SpfConfig.CHARGE_SPF_QC_BOOSTER, settings_qc.isChecked).commit()
            if (!settings_qc.isChecked) {
                Snackbar.make(this.view, "充电加速服务已禁用，可能需要重启手机才能恢复默认设置！", Snackbar.LENGTH_SHORT).show()
            } else {
                //启用电池服务
                startBatteryService()
                Snackbar.make(this.view, "OK！如果你要手机重启后自动开启本功能，请允许微工具箱开机自启！", Snackbar.LENGTH_SHORT).show()
            }
        }
        settings_bp.setOnClickListener {
            spf.edit().putBoolean(SpfConfig.CHARGE_SPF_BP, settings_bp.isChecked).commit()
            //禁用电池保护：恢复充电功能
            if (!settings_bp.isChecked) {
                SuDo(context).execCmdSync(Consts.ResumeChanger)
            } else {
                //启用电池服务
                startBatteryService()
                Snackbar.make(this.view, "OK！如果你要手机重启后自动开启本功能，请允许微工具箱开机自启！", Snackbar.LENGTH_SHORT).show()
            }
        }

        settings_bp_level.setOnSeekBarChangeListener(OnSeekBarChangeListener(Runnable {
            startBatteryService()
            accessbility_bp_level_desc.setText("充电限制电量：" + spf.getInt(SpfConfig.CHARGE_SPF_BP_LEVEL, 85) + "%")
        }, spf, SpfConfig.CHARGE_SPF_BP_LEVEL))
        settings_qc_limit.setOnSeekBarChangeListener(OnSeekBarChangeListener(Runnable {
            val level = spf.getInt(SpfConfig.CHARGE_SPF_QC_LIMIT, 5000)
            startBatteryService()
            batteryUnits.setChargeInputLimit(level);
            settings_qc_limit_desc.setText("充电上限电流：" + level + "mA")
        }, spf, SpfConfig.CHARGE_SPF_QC_LIMIT))


        if (!qcSettingSuupport) {
            settings_qc.isEnabled = false
            spf.edit().putBoolean(SpfConfig.CHARGE_SPF_QC_BOOSTER, false).commit()
            settings_qc_limit.isEnabled = false
            settings_qc_limit_current.visibility = View.GONE
        }

        if (!batteryUnits.bpSetting()) {
            settings_bp.isEnabled = false
            spf.edit().putBoolean(SpfConfig.CHARGE_SPF_BP, false).commit()
            settings_bp_level.isEnabled = false
        }
    }

    class  OnSeekBarChangeListener(private var next:Runnable, private var spf: SharedPreferences, private var spfProp:String) : SeekBar.OnSeekBarChangeListener {
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

    //启动电池服务
    private fun startBatteryService() {
        try {
            val intent = Intent(context, ServiceBattery::class.java)
            context!!.startService(intent)
            serviceRunning = ServiceBattery.serviceIsRunning(context)
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

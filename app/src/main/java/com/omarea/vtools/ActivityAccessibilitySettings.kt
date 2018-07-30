package com.omarea.vtools

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.provider.Settings
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.view.View
import android.widget.Toast
import com.omarea.shared.AccessibleServiceHelper
import com.omarea.shared.Consts
import com.omarea.shared.SpfConfig
import com.omarea.shell.KeepShellSync
import com.omarea.ui.ProgressBarDialog
import kotlinx.android.synthetic.main.activity_accessibility_settings.*

class ActivityAccessibilitySettings : AppCompatActivity() {
    private lateinit var spf: SharedPreferences
    private var myHandler = Handler()
    private lateinit var globalSPF: SharedPreferences

    override fun onPostResume() {
        super.onPostResume()
        delegate.onPostResume()

        home_app_nightmode.isChecked = globalSPF.getBoolean(SpfConfig.GLOBAL_SPF_NIGHT_MODE, false)
        home_hide_in_recents.isChecked = globalSPF.getBoolean(SpfConfig.GLOBAL_SPF_AUTO_REMOVE_RECENT, false)

        val serviceState = AccessibleServiceHelper().serviceIsRunning(this)
        vtoolsserviceSettings!!.visibility = if (serviceState) View.VISIBLE else View.GONE

        vtoolsservice_state.text = if (serviceState) getString(R.string.accessibility_running) else getString(R.string.accessibility_stoped)

        settings_autoinstall.isChecked = spf.getBoolean(SpfConfig.GLOBAL_SPF_AUTO_INSTALL, false)
        settings_debugmode.isChecked = spf.getBoolean(SpfConfig.GLOBAL_SPF_DEBUG, false)
        settings_delaystart.isChecked = spf.getBoolean(SpfConfig.GLOBAL_SPF_DELAY, false)
        accessbility_notify.isChecked = spf.getBoolean(SpfConfig.GLOBAL_SPF_NOTIFY, true)
        settings_disable_selinux.isChecked = spf.getBoolean(SpfConfig.GLOBAL_SPF_DISABLE_ENFORCE, true)
    }

    @SuppressLint("ApplySharedPref")
    override fun onCreate(savedInstanceState: Bundle?) {
        spf = getSharedPreferences(SpfConfig.GLOBAL_SPF, Context.MODE_PRIVATE)
        if (spf.getBoolean(SpfConfig.GLOBAL_SPF_NIGHT_MODE, false))
            this.setTheme(R.style.AppTheme_NoActionBarNight)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_accessibility_settings)
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        globalSPF = getSharedPreferences(SpfConfig.GLOBAL_SPF, Context.MODE_PRIVATE)

        home_hide_in_recents.setOnCheckedChangeListener({ _, checked ->
            globalSPF.edit().putBoolean(SpfConfig.GLOBAL_SPF_AUTO_REMOVE_RECENT, checked).commit()
        })
        home_app_nightmode.setOnCheckedChangeListener({ _, checked ->
            if (globalSPF.getBoolean(SpfConfig.GLOBAL_SPF_NIGHT_MODE, false) == checked) {
                return@setOnCheckedChangeListener
            }
            globalSPF.edit().putBoolean(SpfConfig.GLOBAL_SPF_NIGHT_MODE, checked).commit()
            Toast.makeText(applicationContext, "此设置将在下次启动Scene时生效！", Toast.LENGTH_SHORT).show()
        })

        spf = this.getSharedPreferences(SpfConfig.GLOBAL_SPF, Context.MODE_PRIVATE)

        vtoolsservice_state.setOnClickListener {
            if (AccessibleServiceHelper().serviceIsRunning(this)) {
                try {
                    val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                    startActivity(intent)
                } catch (ex: Exception) {

                }
                return@setOnClickListener
            }
            val dialog = ProgressBarDialog(this)
            dialog.showDialog("尝试使用ROOT权限开启服务...")
            Thread(Runnable {
                if (!AccessibleServiceHelper().startServiceUseRoot(this)) {
                    try {
                        myHandler.post {
                            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                            startActivity(intent)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    } finally {
                        myHandler.post {
                            dialog.hideDialog()
                        }
                    }
                } else {
                    myHandler.post {
                        dialog.hideDialog()
                        val serviceState = AccessibleServiceHelper().serviceIsRunning(this)
                        vtoolsserviceSettings!!.visibility = if (serviceState) View.VISIBLE else View.GONE
                        vtoolsservice_state.text = if (serviceState) this.getString(R.string.accessibility_running) else this.getString(R.string.accessibility_stoped)
                    }
                }
            }).start()
        }

        settings_delaystart.setOnCheckedChangeListener({ _, checked ->
            spf.edit().putBoolean(SpfConfig.GLOBAL_SPF_DELAY, checked).commit()
        })
        settings_debugmode.setOnCheckedChangeListener({ _, checked ->
            spf.edit().putBoolean(SpfConfig.GLOBAL_SPF_DEBUG, checked).commit()
        })
        settings_autoinstall.setOnCheckedChangeListener({ _, checked ->
            spf.edit().putBoolean(SpfConfig.GLOBAL_SPF_AUTO_INSTALL, checked).commit()
        })
        accessbility_notify.setOnCheckedChangeListener({ _, checked ->
            spf.edit().putBoolean(SpfConfig.GLOBAL_SPF_NOTIFY, checked).commit()
        })
        settings_disable_selinux.setOnClickListener {
            if (settings_disable_selinux.isChecked) {
                KeepShellSync.doCmdSync(Consts.DisableSELinux)
                myHandler.postDelayed({
                    spf.edit().putBoolean(SpfConfig.GLOBAL_SPF_DISABLE_ENFORCE, true).commit()
                }, 10000)
            } else {
                KeepShellSync.doCmdSync(Consts.ResumeSELinux)
                spf.edit().putBoolean(SpfConfig.GLOBAL_SPF_DISABLE_ENFORCE, false).commit()
            }
        }
    }

    public override fun onPause() {
        super.onPause()
    }
}

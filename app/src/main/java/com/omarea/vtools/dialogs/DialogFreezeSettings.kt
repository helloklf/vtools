package com.omarea.vtools.dialogs

import android.app.AlertDialog
import android.content.Context
import android.view.View
import android.widget.Switch
import com.omarea.common.ui.DialogHelper
import com.omarea.store.SpfConfig
import com.omarea.vtools.R

class DialogFreezeSettings {
    fun showOptions(context: Context, onClose: Runnable) {
        val config = context.getSharedPreferences(SpfConfig.GLOBAL_SPF, Context.MODE_PRIVATE)

        val view = View.inflate(context, R.layout.dialog_freeze_settings, null)
        val switch = view.findViewById<Switch>(R.id.freeze_privacy)
        val fingerprintVerify = view.findViewById<Switch>(R.id.freeze_fingerprint_verify)
        val pretend = view.findViewById<Switch>(R.id.freeze_pretend)

        switch.isChecked = config.getBoolean(SpfConfig.GLOBAL_SPF_FREEZE_PRIVACY, false)
        switch.setOnClickListener {
            config.edit().putBoolean(SpfConfig.GLOBAL_SPF_FREEZE_PRIVACY, (it as Switch).isChecked).apply()
        }
        fingerprintVerify.isChecked = config.getBoolean(SpfConfig.GLOBAL_SPF_FREEZE_FINGERPRINT, false)
        fingerprintVerify.setOnClickListener {
            config.edit().putBoolean(SpfConfig.GLOBAL_SPF_FREEZE_FINGERPRINT, (it as Switch).isChecked).apply()
        }
        pretend.isChecked = config.getBoolean(SpfConfig.GLOBAL_SPF_FREEZE_PRETEND, false)
        pretend.setOnClickListener {
            config.edit().putBoolean(SpfConfig.GLOBAL_SPF_FREEZE_PRETEND, (it as Switch).isChecked).apply()
        }

        DialogHelper.animDialog(
                AlertDialog.Builder(context)
                        .setView(view)
                        .setCancelable(true)
                        .setOnDismissListener {
                            onClose.run()
                        }
        )
    }
}

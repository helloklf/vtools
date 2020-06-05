package com.omarea.vtools.activities

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.omarea.scene_mode.ModeSwitcher
import com.omarea.vtools.popup.FloatPowercfgSelector

class ActivityPowerModeTile : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (ModeSwitcher().modeConfigCompleted()) {
            if (Build.VERSION.SDK_INT >= 23 && !Settings.canDrawOverlays(this)) {
                //若没有权限，提示获取
                //val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
                //startActivity(intent);
                val overlayPermission = Intent().addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                overlayPermission.action = "android.settings.APPLICATION_DETAILS_SETTINGS"
                overlayPermission.data = Uri.fromParts("package", this.packageName, null)
                Toast.makeText(this, "为Scene授权显示悬浮窗权限，从而在应用中快速切换模式！", Toast.LENGTH_SHORT).show();
            } else {
                FloatPowercfgSelector(this.applicationContext).open(this.packageName)
            }

        } else {
            Toast.makeText(this, "性能配置未完成，无法使用快捷切换！", Toast.LENGTH_SHORT).show();
        }
        finish()
    }
}

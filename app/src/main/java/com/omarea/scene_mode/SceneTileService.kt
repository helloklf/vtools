package com.omarea.scene_mode

import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.annotation.RequiresApi
import com.omarea.vtools.R
import com.omarea.vtools.activities.ActivityPowerModeTile

@RequiresApi(api = Build.VERSION_CODES.N)
class SceneTileService : TileService() {
    override fun onClick() {
        // sendBroadcast(Intent(this, ReceiverSceneMode::class.java).putExtra("packageName", packageName))
        startActivityAndCollapse(
                Intent(this, ActivityPowerModeTile::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        .addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
                        .addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
        )
        super.onClick()
    }

    private fun getIcon(id: Int): Icon? {
        return Icon.createWithResource(getApplicationContext(), id);
    }

    override fun onStartListening() {
        super.onStartListening()

        val currentMode = ModeSwitcher.getCurrentPowerMode()
        when (currentMode) {
            ModeSwitcher.POWERSAVE -> {
                qsTile.run {
                    state = Tile.STATE_ACTIVE
                    icon = getIcon(R.drawable.p1)
                    label = getString(R.string.powersave)
                }
            }
            ModeSwitcher.BALANCE -> {
                qsTile.run {
                    state = Tile.STATE_ACTIVE
                    icon = getIcon(R.drawable.p2)
                    label = getString(R.string.balance)
                }
            }
            ModeSwitcher.PERFORMANCE -> {
                qsTile.run {
                    state = Tile.STATE_ACTIVE
                    icon = getIcon(R.drawable.p3)
                    label = getString(R.string.performance)
                }
            }
            ModeSwitcher.FAST -> {
                qsTile.run {
                    state = Tile.STATE_ACTIVE
                    icon = getIcon(R.drawable.p4)
                    label = getString(R.string.fast)
                }
            }
            else -> {
                qsTile.run {
                    state = Tile.STATE_INACTIVE
                    icon = getIcon(R.drawable.p2)
                    label = getString(R.string.app_name)
                }
            }
        }
        qsTile.updateTile()
    }
}
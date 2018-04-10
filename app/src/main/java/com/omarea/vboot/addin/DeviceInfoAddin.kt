package com.omarea.vboot.addin

import android.content.Context
import com.omarea.shared.Consts

/**
 * Created by Hello on 2018/03/22.
 */

class DeviceInfoAddin(private var context: Context) : AddinBase(context) {
    fun modifyR11() {
        command = StringBuilder()
                .append(Consts.MountSystemRW)
                .append(
                        "busybox sed 's/^ro.product.model=.*/ro.product.model=OPPO R11 Plus/' /system/build.prop > /data/build.prop;" +
                                "busybox sed -i 's/^ro.product.brand=.*/ro.product.brand=OPPO/' /data/build.prop;" +
                                "busybox sed -i 's/^ro.product.name=.*/ro.product.name=R11 Plus/' /data/build.prop;" +
                                "busybox sed -i 's/^ro.product.device=.*/ro.product.device=R11 Plus/' /data/build.prop;" +
                                "busybox sed -i 's/^ro.build.product=.*/ro.build.product=R11 Plus/' /data/build.prop;" +
                                "busybox sed -i 's/^ro.product.manufacturer=.*/ro.product.manufacturer=OPPO/' /data/build.prop;")
                .append("cp /system/build.prop /system/build.bak.prop\n")
                .append("cp /data/build.prop /system/build.prop\n")
                .append("rm /data/build.prop\n")
                .append("chmod 0644 /system/build.prop\n")
                .append(Consts.Reboot)
                .toString()

        super.run()
    }

    fun modifyX20() {
        command = StringBuilder()
                .append(Consts.MountSystemRW)
                .append(
                        "busybox sed 's/^ro.product.model=.*/ro.product.model=vivo X20/' /system/build.prop > /data/build.prop;" +
                                "busybox sed -i 's/^ro.product.brand=.*/ro.product.brand=vivo/' /data/build.prop;" +
                                "busybox sed -i 's/^ro.product.name=.*/ro.product.name=X20/' /data/build.prop;" +
                                "busybox sed -i 's/^ro.product.device=.*/ro.product.device=X20/' /data/build.prop;" +
                                "busybox sed -i 's/^ro.build.product=.*/ro.build.product=X20/' /data/build.prop;" +
                                "busybox sed -i 's/^ro.product.manufacturer=.*/ro.product.manufacturer=vivo/' /data/build.prop;")
                .append("cp /system/build.prop /system/build.bak.prop\n")
                .append("cp /data/build.prop /system/build.prop\n")
                .append("rm /data/build.prop\n")
                .append("chmod 0644 /system/build.prop\n")
                .append(Consts.Reboot)
                .toString()

        super.run()
    }
}

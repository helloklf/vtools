package com.omarea.shell.units;

import com.omarea.shell.SysUtils;

import java.util.ArrayList;

/**
 * Created by Hello on 2017/11/01.
 */

public class NubiaUnit {
    public boolean DisableSomeApp() {
        ArrayList<String> commands = new ArrayList<String>(){{
            add(
                "pm disable com.android.cellbroadcastreceiver\n" +
                "pm disable com.android.printspooler\n" +
                "pm disable com.android.galaxy4\n" +
                "pm disable com.android.noisefield\n" +
                "pm disable com.android.phasebeam\n" +
                "pm disable com.android.wallpaper.holospiral\n" +
                "pm disable com.android.dreams.basic\n" +
                "pm disable com.google.android.configupdater\n" +
                "pm disable com.google.android.syncadapters.contacts\n" +
                "pm disable com.google.android.syncadapters.calendar\n" +
                "pm disable com.google.android.feedback\n" +
                "pm disable com.google.android.backuptransport\n" +
                "pm disable com.android.dreams.phototable\n" +
                "pm disable com.google.android.partnersetup\n" +
                "pm disable com.google.android.gsf\n" +
                "pm disable com.google.android.gsf.login\n" +
                "pm disable com.google.android.gms\n" +
                "pm disable com.chaozh.iReaderNubia\n" +
                "pm disable cn.nubia.presetpackageinstaller\n" +
                "pm disable cn.nubia.factory\n" +
                "pm disable cn.nubia.neostore\n" +
                "pm disable cn.nubia.bootanimationinfo\n" +
                "pm disable cn.nubia.ultrapower.launcher\n" +
                "pm disable cn.nubia.email\n" +
                "pm disable cn.nubia.exchange\n" +
                "pm disable cn.nubia.video\n" +
                "pm disable cn.nubia.nbgame\n" +
                "pm disable cn.nubia.zbiglauncher.preset\n" +
                "pm disable cn.nubia.phonemanualintegrate.preset\n" +
                "pm disable cn.nubia.music.preset\n" +
                "pm disable cn.nubia.nubiashop\n" +
                "pm disable com.yulore.framework\n" +
                "pm disable cn.nubia.yulorepage\n" +
                "pm disable cn.nubia.festivalwallpaper\n" +
                "pm disable cn.nubia.gallerylockscreen\n" +
                "pm disable cn.nubia.wps_moffice\n" +
                "pm disable cn.nubia.aftersale\n" +
                "pm disable cn.nubia.aftersale\n" +
                "pm disable com.sohu.inputmethod.sogou.nubia\n" +
                "pm disable com.dolby\n" +
                "pm disable com.dolby.daxappUI\n"
            );
        }};
        return SysUtils.executeRootCommand(commands);
    }
}

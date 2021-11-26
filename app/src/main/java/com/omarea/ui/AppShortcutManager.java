package com.omarea.ui;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.graphics.drawable.Icon;
import android.net.Uri;

import com.omarea.vtools.R;

import java.util.ArrayList;

/**
 * 管理应用的Shortcut
 */
public class AppShortcutManager {
    private Context context;

    public AppShortcutManager(Context context) {
        this.context = context;
    }

    public void initMenu() {
        SharedPreferences sharedPreferences = context.getSharedPreferences("shortcut", Context.MODE_PRIVATE);
        if (!sharedPreferences.getBoolean("init", false)) {
            setMenu();
            sharedPreferences.edit().putBoolean("init", true).apply();
        }
    }

    public void removeMenu() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N_MR1) {
            ShortcutManager shortcutManager = context.getSystemService(ShortcutManager.class);
            assert shortcutManager != null;
            shortcutManager.removeAllDynamicShortcuts();
        }
    }

    private void setMenu() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N_MR1) {
            ShortcutManager shortcutManager = context.getSystemService(ShortcutManager.class);
            ArrayList<ShortcutInfo> shortcutInfos = new ArrayList<>();
            shortcutInfos.add(new ShortcutInfo.Builder(context, "powersave")
                    .setShortLabel("省电模式")
                    .setLongLabel("性能配置-省电")
                    .setDisabledMessage("Disabled")
                    .setIcon(Icon.createWithResource(context, R.drawable.shortcut_p1))
                    .setIntent(new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.trinea.cn/")))
                    .build());
            shortcutInfos.add(new ShortcutInfo.Builder(context, "balance")
                    .setShortLabel("省电模式")
                    .setLongLabel("性能配置-均衡")
                    .setDisabledMessage("Disabled")
                    .setIcon(Icon.createWithResource(context, R.drawable.shortcut_p2))
                    .setIntent(new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.trinea.cn/")))
                    .build());
            shortcutInfos.add(new ShortcutInfo.Builder(context, "performance")
                    .setShortLabel("性能模式")
                    .setLongLabel("性能配置-性能")
                    .setDisabledMessage("Disabled")
                    .setIcon(Icon.createWithResource(context, R.drawable.shortcut_p3))
                    .setIntent(new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.trinea.cn/")))
                    .build());
            shortcutInfos.add(new ShortcutInfo.Builder(context, "fast")
                    .setShortLabel("极速模式")
                    .setLongLabel("性能配置-极速")
                    .setDisabledMessage("Disabled")
                    .setIcon(Icon.createWithResource(context, R.drawable.shortcut_p4))
                    .setIntent(new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.trinea.cn/")))
                    .build());
            assert shortcutManager != null;
            shortcutManager.setDynamicShortcuts(shortcutInfos);
        }
    }
}

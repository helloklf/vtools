package com.omarea.shared;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.AssetManager;
import android.os.Message;
import android.view.accessibility.AccessibilityManager;
import android.widget.Toast;

import com.omarea.vboot.reciver_batterychanged;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

/**
 * Created by helloklf on 2016/10/1.
 */
public class ServiceHelper {
    public ServiceHelper(Context context) {
        EventBus.subscribe(Events.DyamicCoreConfigChanged, new IEventSubscribe() {
            @Override
            public void messageRecived(Object message) {
                if (ConfigInfo.getConfigInfo().DyamicCore) {
                    InstallConfig();
                    onAccessibilityEvent();
                } else
                    UnInstallConfig();
            }
        });
        EventBus.subscribe(Events.PowerDisConnection, new IEventSubscribe() {
            @Override
            public void messageRecived(Object message) {
                onChanger = false;
                onAccessibilityEvent();
            }
        });
        EventBus.subscribe(Events.PowerConnection, new IEventSubscribe() {
            @Override
            public void messageRecived(Object message) {
                onChanger = true;
                onAccessibilityEvent();
            }
        });
        EventBus.subscribe(Events.BatteryChanged, new IEventSubscribe() {
            @Override
            public void messageRecived(Object message) {
                batteryLevel = (int) message;
                onAccessibilityEvent();
            }
        });
        EventBus.subscribe(Events.CoreConfigChanged, new IEventSubscribe() {
            @Override
            public void messageRecived(Object message) {
                configInstalled = false;
                onAccessibilityEvent();
            }
        });

        this.context = context;
    }

    private reciver_batterychanged batteryChangedReciver;
    private Context context = null;
    private String lastPackage;
    private Configs lastMode = Configs.None;
    private boolean onChanger;//是否正在充电
    private boolean configInstalled;
    private int batteryLevel; //电量级别
    private Process p = null;
    private String cpuName;
    private long serviceCreatedTime = new Date().getTime();
    private cmd_shellTools cmd_shellTools = new cmd_shellTools(null, null);
    //标识是否已经加载完设置
    private boolean SettingsLoaded = false;

    public ArrayList<String> supportCpus = new ArrayList() {
        {
            add("msm8992");
            add("msm8996");
        }
    };

    //加载设置
    private boolean SettingsLoad() {
        if (ConfigInfo.getConfigInfo().DelayStart && !AppShared.system_inited && (new Date().getTime() - serviceCreatedTime) < 20000)
            return false;

        DoCmd(Consts.DisableSELinux);

        cpuName = cmd_shellTools.GetCPUName();
        if (!(cpuName.contains("8996") || cpuName.contains("8992"))) {
            ConfigInfo.getConfigInfo().DyamicCore = false;
        }

        ShowMsg("微工具箱增强服务已启动");

        SettingsLoaded = true;
        AppShared.system_inited = true;

        try {
            if (batteryChangedReciver == null) {
                //监听电池改变
                batteryChangedReciver = new reciver_batterychanged();
                batteryChangedReciver.serviceHelper = this;
                //启动完成
                IntentFilter ACTION_BOOT_COMPLETED = new IntentFilter(Intent.ACTION_BOOT_COMPLETED);
                context.registerReceiver(batteryChangedReciver, ACTION_BOOT_COMPLETED);
                //电源连接
                IntentFilter ACTION_POWER_CONNECTED = new IntentFilter(Intent.ACTION_POWER_CONNECTED);
                context.registerReceiver(batteryChangedReciver, ACTION_POWER_CONNECTED);
                //电源断开
                IntentFilter ACTION_POWER_DISCONNECTED = new IntentFilter(Intent.ACTION_POWER_DISCONNECTED);
                context.registerReceiver(batteryChangedReciver, ACTION_POWER_DISCONNECTED);
                //电量变化
                IntentFilter ACTION_BATTERY_CHANGED = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
                context.registerReceiver(batteryChangedReciver, ACTION_BATTERY_CHANGED);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return true;
    }

    //判断服务是否激活
    public static boolean serviceIsRunning(Context context) {
        AccessibilityManager m = (AccessibilityManager) context.getSystemService(Context.ACCESSIBILITY_SERVICE);
        List<AccessibilityServiceInfo> serviceInfos = m.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK);
        for (AccessibilityServiceInfo serviceInfo : serviceInfos) {
            if (serviceInfo.getId().equals("com.omarea.vboot/.service_accessibility")) {
                return true;
            }
        }
        return false;
    }

    //判断Xposed插件是否已经激活（将在Xposed部分中hook返回值为true）
    public static boolean xposedIsRunning() {
        return false;
    }

    android.os.Handler myHandler = new android.os.Handler() {
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
        }
    };

    void tryExit() {
        try {
            if (out != null)
                out.close();
        } catch (Exception ex) {
        }

        try {
            p.destroy();
        } catch (Exception ex) {
        }
    }

    void DoCmd(final String cmd) {
        DoCmd(cmd, false);
    }

    DataOutputStream out;

    void DoCmd(final String cmd, final boolean isRedo) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    tryExit();
                    if (p == null || isRedo || out == null) {
                        tryExit();
                        p = Runtime.getRuntime().exec("su");
                        out = new DataOutputStream(p.getOutputStream());
                    }
                    out.writeBytes(cmd);
                    out.writeBytes("\n");
                    out.flush();
                    out.close();
                } catch (IOException e) {
                    //重试一次
                    if (!isRedo)
                        DoCmd(cmd, true);
                    else
                        ShowMsg("执行动作失败！\r\n错误信息：" + e.getMessage() + "\n\n\n命令内容：\r\n" + cmd);
                }
            }
        }).start();
    }

    private void ShowMsg(final String msg) {
        if (context != null)
            myHandler.post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
                }
            });
        else {
            //XposedBridge.log("微工具箱 Message：" + msg);
        }
    }

    String msgTemplate = "当前应用：%s\n配置模式：%s";

    private void ShowModeToggleMsg(String packageName, String modeName) {
        if (ConfigInfo.getConfigInfo().DebugMode)
            ShowMsg(String.format(msgTemplate, packageName, modeName));
    }

    //自动切换模式
    private void autoToggleMode(String packageName) {
        //打包安装程序速度优化-使用游戏模式
        if (packageName.equals("com.android.packageinstaller")) {
            if (lastMode != Configs.Game) {
                try {
                    ToggleConfig(Configs.Game);
                    ShowModeToggleMsg("打包安装程序，自动调整性能。", "游戏模式");
                } catch (Exception ex) {
                    ShowModeToggleMsg(packageName, "切换模式失败，请允许本应用使用ROOT权限！");
                }
            }
            return;
        }

        for (HashMap<String, Object> item : ConfigInfo.getConfigInfo().gameList) {
            if (item.get("packageName").toString().equals(packageName)) {
                if (lastMode != Configs.Game) {
                    try {
                        ToggleConfig(Configs.Game);
                        ShowModeToggleMsg(packageName, "游戏模式");
                    } catch (Exception ex) {
                        ShowModeToggleMsg(packageName, "切换模式失败，请允许本应用使用ROOT权限！");
                    }
                }
                return;
            }
        }


        for (HashMap<String, Object> item : ConfigInfo.getConfigInfo().powersaveList) {
            if (item.get("packageName").toString().equals(packageName)) {
                if (lastMode != Configs.PowerSave) {
                    try {
                        ToggleConfig(Configs.PowerSave);
                        ShowModeToggleMsg(packageName, "省电模式");
                    } catch (Exception ex) {
                        ShowModeToggleMsg(packageName, "切换模式失败，请允许本应用使用ROOT权限！");
                    }
                }
                return;
            }
        }

        if (lastMode != Configs.Default) {
            try {
                ToggleConfig(Configs.Default);
                ShowModeToggleMsg(packageName, "默认模式");
            } catch (Exception ex) {
                ShowModeToggleMsg(packageName, "切换模式失败，请允许本应用使用ROOT权限！");
            }
        }
    }

    //终止进程
    void autoBoosterApp(String packageName) {
        if (lastPackage.equals("android") || lastPackage.equals("com.android.systemui"))
            return;

        if (ConfigInfo.getConfigInfo().blacklist.contains(packageName)) {
            if (ConfigInfo.getConfigInfo().UsingDozeMod) {
                try {
                    DoCmd("am set-inactive " + packageName + " true");
                    //am set-idle com.tencent.mobileqq true
                    if (ConfigInfo.getConfigInfo().DebugMode)
                        ShowMsg("尝试休眠了应用：" + packageName);
                } catch (Exception ex) {

                }
                return;
            }

            //android.os.Process.killProcess(android.os.Process.myPid());//自杀
            try {
                DoCmd("pgrep " + packageName + " |xargs kill -9");
                if (ConfigInfo.getConfigInfo().DebugMode)
                    ShowMsg("强行终止了进程：" + packageName);
            } catch (Exception ex) {

            }
        }
    }

    //切换配置
    public void ToggleConfig(Configs mode) throws IOException, InterruptedException {
        StringBuilder cmd = new StringBuilder();
        switch (mode) {
            case Game: {
                cmd.append(Consts.ToggleGameMode);
                break;
            }
            case PowerSave: {
                cmd.append(Consts.TogglePowersaveMode);
                break;
            }
            case Fast: {
                cmd.append(Consts.ToggleFastMode);
                break;
            }
            default: {
                cmd.append(Consts.ToggleDefaultMode);
                break;
            }
        }
        DoCmd(cmd.toString());

        lastMode = mode;
    }

    //安装调频文件
    void InstallConfig() {
        if (context == null) return;

        if (!supportCpus.contains(cpuName)) {
            ShowMsg("您的设备不在兼容列表中（目前仅兼容骁龙808、骁龙820处理器），无法使用动态响应功能！");
            return;
        }

        try {
            AssetManager ass = context.getAssets();

            String cpuNumber = cpuName.replace("msm", "");

            if (ConfigInfo.getConfigInfo().UseBigCore) {
                AppShared.WriteFile(ass, cpuName + "/thermal-engine-bigcore.conf", "thermal-engine.conf");
                AppShared.WriteFile(ass, cpuName + "/init.qcom.post_boot-bigcore.sh", "init.qcom.post_boot.sh");
                AppShared.WriteFile(ass, cpuName + "/powercfg-bigcore.sh", "powercfg.sh");
            } else {
                AppShared.WriteFile(ass, cpuName + "/thermal-engine-default.conf", "thermal-engine.conf");
                AppShared.WriteFile(ass, cpuName + "/init.qcom.post_boot-default.sh", "init.qcom.post_boot.sh");
                AppShared.WriteFile(ass, cpuName + "/powercfg-default.sh", "powercfg.sh");
            }


            String cmd = new StringBuilder().append(Consts.InstallConfig).append(Consts.ExecuteConfig)
                    .toString().replace("cpuNumber", cpuNumber);
            DoCmd(cmd);

            ToggleConfig(Configs.Default);
            configInstalled = true;

            if (ConfigInfo.getConfigInfo().DebugMode)
                ShowMsg("已安装自定义调频！");
        } catch (Exception ex) {
            ShowMsg("配置文件安装失败！");
        }
    }

    public void UnInstallConfig() {
        if (cpuName == null || cpuName.trim().equals("")) return;

        try {
            ToggleConfig(Configs.Game);
            DoCmd(new StringBuilder().append(Consts.ExecuteConfig)
                    .toString().replace("cpuNumber", cpuName.replace("msm", ""))
            );
            ShowMsg("已替换回默认调频，部分修改可能需要重启才能还原！");
        } catch (Exception ex) {
            ShowMsg("还原默认配置失败，你可能需要手动还原它！");
        }
    }

    public enum Configs {
        None,
        Default,
        Game,
        PowerSave,
        Fast
    }

    public void onAccessibilityEvent() {
        try {
            onAccessibilityEvent(lastPackage);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void onAccessibilityEvent(String packageName) {
        if (!SettingsLoaded && !SettingsLoad() || !ConfigInfo.getConfigInfo().DyamicCore)
            return;

        if (!configInstalled || lastPackage == null) {
            lastPackage = "com.android.systemui";
            InstallConfig();
        }

        if (packageName == null) {
            packageName = lastPackage;
        }
        if (ConfigInfo.getConfigInfo().AutoClearCache && lastPackage != packageName) {
            DoCmd(Consts.ClearCache);
        }

        //开启电源适配且电量充足
        if (onChanger && ConfigInfo.getConfigInfo().PowerAdapter && cpuName.equals("msm8996") && batteryLevel > 29) {
            try {
                if (lastPackage != packageName && lastMode == Configs.Fast)
                    return;

                ToggleConfig(Configs.Fast);
                lastPackage = packageName;
                if (ConfigInfo.getConfigInfo().DebugMode)
                    ShowMsg("当前电量充足，开启极速模式...");
            } catch (Exception ex) {

            }
            return;
        }

        if (!onChanger && ConfigInfo.getConfigInfo().PowerAdapter && batteryLevel < 20 && batteryLevel > 0) {
            try {
                if (lastMode != Configs.PowerSave) {
                    ToggleConfig(Configs.PowerSave);
                    lastPackage = packageName;
                    ShowMsg("当前电量不足，强制使用省电模式...");
                }
            } catch (Exception ex) {

            }
            return;
        }

        //如果没有切换应用
        if (packageName.equals(lastPackage) && lastMode != Configs.Fast)
            return;

        if (packageName.equals("android") || packageName.equals("com.android.systemui") || packageName.equals("com.omarea.vboot") || packageName.contains("inputmethod"))
            return;

        autoToggleMode(packageName);

        if (ConfigInfo.getConfigInfo().AutoBooster)
            autoBoosterApp(lastPackage);

        lastPackage = packageName;
    }

    public void onInterrupt() {
        if (batteryChangedReciver != null) {
            DoCmd(Consts.BPReset);
            context.unregisterReceiver(batteryChangedReciver);
            batteryChangedReciver.serviceHelper = null;
            batteryChangedReciver = null;
        }
        UnInstallConfig();
    }

}

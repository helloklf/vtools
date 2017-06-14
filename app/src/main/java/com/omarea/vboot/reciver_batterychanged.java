package com.omarea.vboot;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.BatteryManager;
import android.os.Handler;
import android.os.Message;
import android.widget.Toast;

import com.omarea.shared.AppShared;
import com.omarea.shared.ConfigInfo;
import com.omarea.shared.Consts;
import com.omarea.shared.EventBus;
import com.omarea.shared.Events;
import com.omarea.shared.ServiceHelper;

import java.io.DataOutputStream;
import java.io.IOException;

/**
 * Created by helloklf on 2016/12/21.
 */


public class reciver_batterychanged extends BroadcastReceiver {
    public static ServiceHelper serviceHelper;

    private void DoCmd(String cmd) {
        Process p = null;
        try {
            try {
                p = Runtime.getRuntime().exec("su");
                DataOutputStream out = new DataOutputStream(p.getOutputStream());
                out.writeBytes(cmd);
                out.writeBytes("\n");
                out.writeBytes("exit\n");
                out.writeBytes("exit\n");
                out.flush();
                p.waitFor();
            } catch (IOException e) {
                if (p != null) {
                    p.destroy();
                    p = null;
                }
                NoRoot();
            } catch (Exception ex) {
                if (p != null) {
                    p.destroy();
                    p = null;
                }
                ShowMsg("电量监控服务异常：" + ex.getMessage(), false);
            } finally {
                if (p != null)
                    p.destroy();
            }
        } catch (Exception ex) {
            ShowMsg("电量监控服务异常：" + ex.getMessage(), false);
        }
    }

    Context context;
    private Handler myHandler = new Handler() {
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
        }
    };

    //显示文本消息
    private void ShowMsg(final String msg, final boolean longMsg) {
        if (context != null)
            myHandler.post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(context, msg, longMsg ? Toast.LENGTH_LONG : Toast.LENGTH_SHORT).show();
                }
            });
    }

    private void NoRoot() {
        ShowMsg("充电加速服务异常，请检查是否已ROOT手机，并允许本应用访问ROOT权限！", false);
    }

    //快速充电
    private void FastCharger() {
        if (!ConfigInfo.getConfigInfo().QcMode)
            return;
        DoCmd(Consts.FastChanger);
    }

    /**
     * 电池保护
     */
    private void BP() {
        DoCmd(Consts.BP);
    }

    private void BPReset() {
        DoCmd(Consts.BPReset);
    }

    int lastBatteryLeavel = -1;
    boolean lastChangerState = false;

    @Override
    public void onReceive(Context context, Intent intent) {
        this.context = context;
        try {
            String action = intent.getAction();
            boolean onChanger = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1) == BatteryManager.BATTERY_STATUS_CHARGING;
            int batteryLevel = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);//intent.getIntExtra(BatteryManager.EXTRA_SCALE,-1);

            if (lastBatteryLeavel != batteryLevel) {
                EventBus.publish(Events.BatteryChanged, batteryLevel);
                lastBatteryLeavel = batteryLevel;
            }

            if (onChanger && (batteryLevel < 85 || !ConfigInfo.getConfigInfo().BatteryProtection))
                BPReset();

            if (action.equals(Intent.ACTION_BATTERY_CHANGED)) {
                if (AppShared.system_inited && onChanger) {
                    if (ConfigInfo.getConfigInfo().QcMode) FastCharger();
                    if (ConfigInfo.getConfigInfo().BatteryProtection) BP();
                }
            } else if (action.equals(Intent.ACTION_POWER_CONNECTED)) {
            } else if ((action.equals(Intent.ACTION_BOOT_COMPLETED) && onChanger)) {
            } else if (action.equals(Intent.ACTION_POWER_DISCONNECTED)) {
                BPReset();
                EventBus.publish(Events.PowerDisConnection);
                return;
            }

            //如果充电状态切换
            if (lastChangerState != onChanger) {
                lastChangerState = onChanger;
                if (onChanger) {
                    BPReset();
                    entryFastChanger(onChanger);
                    EventBus.publish(Events.PowerConnection);
                } else
                    EventBus.publish(Events.PowerDisConnection);
            }
        } catch (Exception ex) {
        }
    }

    private void entryFastChanger(boolean onChanger) {
        if (AppShared.system_inited && onChanger) {
            if (ConfigInfo.getConfigInfo().QcMode)
                FastCharger();
            if (ConfigInfo.getConfigInfo().BatteryProtection)
                BP();
            if (ConfigInfo.getConfigInfo().DebugMode)
                ShowMsg("充电器已连接！", false);
        }
    }
}

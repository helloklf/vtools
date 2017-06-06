package com.omarea.vboot;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.omarea.shared.AppShared;
import com.omarea.shared.ConfigInfo;
import com.omarea.shared.Consts;
import com.omarea.shared.EventBus;
import com.omarea.shared.Events;
import com.omarea.shared.IEventSubscribe;
import com.omarea.shared.ServiceHelper;
import com.omarea.shared.cmd_shellTools;
import com.omarea.shell.DynamicConfig;

import java.util.Timer;
import java.util.TimerTask;


public class fragment_home extends Fragment {

    public fragment_home() {
        // Required empty public constructor
    }

    View view;
    IEventSubscribe subscribePowerDisConn = new IEventSubscribe() {
        @Override
        public void messageRecived(Object message) {
            Snackbar.make(view, "充电器已断开连接！", Snackbar.LENGTH_SHORT).show();
        }
    };
    IEventSubscribe subscribePowerConn = new IEventSubscribe() {
        @Override
        public void messageRecived(Object message) {
            Snackbar.make(view, "充电器已连接！", Snackbar.LENGTH_SHORT).show();
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.layout_home, container, false);

        cmdshellTools = new cmd_shellTools(null, null);

        if(new DynamicConfig().DynamicSupport(ConfigInfo.getConfigInfo().CPUName)){
            view.findViewById(R.id.powermode_toggles).setVisibility(View.VISIBLE);
        }

        this.btn_powersave = (Button) view.findViewById(R.id.btn_powersave);
        this.btn_defaultmode = (Button) view.findViewById(R.id.btn_defaultmode);
        this.btn_gamemode = (Button) view.findViewById(R.id.btn_gamemode);
        this.btn_fastmode = (Button) view.findViewById(R.id.btn_fastmode);

        btn_powersave.setOnClickListener(togglePowerSave);
        btn_defaultmode.setOnClickListener(toggleDefaultMode);
        btn_gamemode.setOnClickListener(toggleGameMode);
        btn_fastmode.setOnClickListener(toggleFastMode);

        return view;
    }

    Handler myHandler = new Handler() {
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
        }
    };

    Timer timer;
    String batteryMAH;
    IEventSubscribe eventSubscribe = new IEventSubscribe() {
        @Override
        public void messageRecived(Object message) {
            setModeState();
        }
    };

    double temp = 0;
    int level = 0;
    boolean powerChonnected = false;
    double voltage;

    @Override
    public void onResume() {
        if (broadcast == null) {
            broadcast = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    try {
                        temp = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0);
                        temp = temp / 10.0;
                        level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
                        voltage = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0);
                        if(voltage>1000)
                            voltage = voltage/1000.0;
                        if(voltage>100)
                            voltage = voltage/100.0;
                        else if(voltage>10)
                            voltage = voltage/10.0;
                        powerChonnected = intent.getIntExtra(BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_NOT_CHARGING)
                                == BatteryManager.BATTERY_STATUS_CHARGING;
                        //intent.getDoubleExtra(BatteryManager.EXTRA_HEALTH);
                    } catch (Exception ex) {
                        System.out.print(ex.getMessage());
                    }
                }
            };
            IntentFilter ACTION_BATTERY_CHANGED = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
            getContext().registerReceiver(broadcast, ACTION_BATTERY_CHANGED);
        }

        EventBus.INSTANCE.subscribe(Events.INSTANCE.getPowerDisConnection(), subscribePowerDisConn);
        EventBus.INSTANCE.subscribe(Events.INSTANCE.getPowerConnection(), subscribePowerConn);
        EventBus.INSTANCE.subscribe(Events.INSTANCE.getModeToggle(), eventSubscribe);

        setModeState();

        final TextView battrystatus = (TextView) view.findViewById(R.id.battrystatus);
        final TextView powerstatus = (TextView) view.findViewById(R.id.powerstatus);
        final TextView sdfree = (TextView) view.findViewById(R.id.sdfree);
        final TextView datafree = (TextView) view.findViewById(R.id.datafree);
        sdfree.setText("共享存储：" + cmdshellTools.GetDirFreeSizeMB("/sdcard") + " MB");
        datafree.setText("应用存储：" + cmdshellTools.GetDirFreeSizeMB("/data") + " MB");
        batteryMAH = cmdshellTools.getBatteryMAH() + "   ";
        serviceRunning = (ServiceHelper.Companion.serviceIsRunning(getContext().getApplicationContext()));

        timer = new Timer();

        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                myHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        battrystatus.setText("电池信息：" +
                                batteryMAH +
                                temp + "°C   " +
                                level + "%    " +
                                voltage + "v"
                        );

                        powerstatus.setText("电池充放：" +
                                (powerChonnected ? "+" : "-") + cmdshellTools.getChangeMAH() + "ma      " +
                                (ConfigInfo.getConfigInfo().QcMode && serviceRunning ? "充电已加速" : "未加速")
                        );
                    }
                });
            }
        }, 0, 3000);


        super.onResume();
    }

    Button btn_powersave = null;
    Button btn_defaultmode = null;
    Button btn_gamemode = null;
    Button btn_fastmode = null;

    private void setModeState() {
        btn_powersave.setText("省电");
        btn_defaultmode.setText("均衡");
        btn_gamemode.setText("游戏");
        btn_fastmode.setText("极速");
    }

    private void installConfig() {

        if (ConfigInfo.getConfigInfo().UseBigCore)
            AppShared.INSTANCE.WriteFile(getContext().getAssets(), ConfigInfo.getConfigInfo().CPUName + "/powercfg-bigcore.sh", "powercfg.sh");
        else
            AppShared.INSTANCE.WriteFile(getContext().getAssets(), ConfigInfo.getConfigInfo().CPUName + "/powercfg-default.sh", "powercfg.sh");
        cmdshellTools.DoCmd(Consts.INSTANCE.getInstallPowerToggleConfigToCache());
    }

    Button.OnClickListener togglePowerSave = new Button.OnClickListener() {
        @Override
        public void onClick(View v) {
            installConfig();
            btn_powersave.setText("省电 √");
            btn_defaultmode.setText("均衡");
            btn_gamemode.setText("游戏");
            btn_fastmode.setText("极速");

            cmdshellTools.DoCmd(Consts.INSTANCE.getTogglePowersaveMode());
            Snackbar.make(v, "已切换为省电模式，适合长时间媒体播放或阅读，综合使用时并不效率也不会省电太多！", Snackbar.LENGTH_LONG).show();
        }
    };
    Button.OnClickListener toggleGameMode = new Button.OnClickListener() {
        @Override
        public void onClick(View v) {
            installConfig();
            btn_powersave.setText("省电");
            btn_defaultmode.setText("均衡");
            btn_gamemode.setText("游戏 √");
            btn_fastmode.setText("极速");

            cmdshellTools.DoCmd(Consts.INSTANCE.getToggleGameMode());
            Snackbar.make(v, "已切换为游戏（性能）模式，但受温度影响并不一定会更快，你可以考虑删除温控！", Snackbar.LENGTH_LONG).show();
        }
    };
    Button.OnClickListener toggleFastMode = new Button.OnClickListener() {
        @Override
        public void onClick(View v) {
            installConfig();
            btn_powersave.setText("省电");
            btn_defaultmode.setText("均衡");
            btn_gamemode.setText("游戏");
            btn_fastmode.setText("极速 √");

            cmdshellTools.DoCmd(Consts.INSTANCE.getToggleFastMode());
            Snackbar.make(v, "已切换为极速模式，这会大幅增加发热，如果不删除温控性能并不稳定！", Snackbar.LENGTH_LONG).show();
        }
    };
    Button.OnClickListener toggleDefaultMode = new Button.OnClickListener() {
        @Override
        public void onClick(View v) {
            installConfig();
            btn_powersave.setText("省电");
            btn_defaultmode.setText("均衡 √");
            btn_gamemode.setText("游戏");
            btn_fastmode.setText("极速");

            cmdshellTools.DoCmd(Consts.INSTANCE.getToggleDefaultMode());
            Snackbar.make(v, "已切换为均衡模式，适合日常使用，速度与耗电平衡！", Snackbar.LENGTH_LONG).show();
        }
    };

    boolean serviceRunning = false;

    @Override
    public void onPause() {
        super.onPause();
        if (timer != null) {
            timer.cancel();
            timer = null;
        }

        try{
            EventBus.INSTANCE.unSubscribe(Events.INSTANCE.getModeToggle(), eventSubscribe);
            EventBus.INSTANCE.unSubscribe(Events.INSTANCE.getPowerDisConnection(), subscribePowerDisConn);
            EventBus.INSTANCE.unSubscribe(Events.INSTANCE.getPowerDisConnection(), subscribePowerConn);
            if (broadcast != null)
                getContext().unregisterReceiver(broadcast);
        }
        catch (Exception ex){

        }
    }

    @Override
    public void onDestroy() {
        try{
            EventBus.INSTANCE.unSubscribe(Events.INSTANCE.getModeToggle(), eventSubscribe);
            EventBus.INSTANCE.unSubscribe(Events.INSTANCE.getPowerDisConnection(), subscribePowerDisConn);
            EventBus.INSTANCE.unSubscribe(Events.INSTANCE.getPowerDisConnection(), subscribePowerConn);

            if (broadcast != null)
                getContext().unregisterReceiver(broadcast);
        }
        catch (Exception ex){

        }
        super.onDestroy();
    }

    BroadcastReceiver broadcast;
    cmd_shellTools cmdshellTools;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }
}

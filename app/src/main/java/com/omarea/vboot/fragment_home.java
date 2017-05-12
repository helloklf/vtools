package com.omarea.vboot;

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

import java.math.BigDecimal;
import java.util.Timer;
import java.util.TimerTask;


public class fragment_home extends Fragment {

    public fragment_home() {
        // Required empty public constructor
    }

    View view;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.layout_home, container, false);

        this.btn_powersave = (Button)view.findViewById(R.id.btn_powersave);
        this.btn_defaultmode = (Button)view.findViewById(R.id.btn_defaultmode);
        this.btn_gamemode = (Button)view.findViewById(R.id.btn_gamemode);
        this.btn_fastmode = (Button)view.findViewById(R.id.btn_fastmode);

        btn_powersave.setOnClickListener(togglePowerSave);
        btn_defaultmode.setOnClickListener(toggleDefaultMode);
        btn_gamemode.setOnClickListener(toggleGameMode);
        btn_fastmode.setOnClickListener(toggleFastMode);

        return view;
    }

    String tempConvert(String temp) {
        BigDecimal t = new BigDecimal(temp);
        t = t.divide(new BigDecimal("1000"));
        return t.setScale(1, BigDecimal.ROUND_HALF_UP).toString();
    }

    String batteryioConvert(String ua) {
        int ma = 0;
        if (ua.length() > 3)
            ma = Integer.parseInt(ua.substring(0, ua.length() - 3));
        else
            ma = Integer.parseInt(ua);


        return (ma < 0 ? "+" : "-") + Math.abs(ma);
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

    @Override
    public void onResume() {
        setModeState();
        EventBus.subscribe(Events.ModeToggle, eventSubscribe);

        final TextView battrystatus = (TextView) view.findViewById(R.id.battrystatus);
        final TextView powerstatus = (TextView) view.findViewById(R.id.powerstatus);
        final TextView coretemp = (TextView) view.findViewById(R.id.coretemp);
        final TextView ctltemp = (TextView) view.findViewById(R.id.ctltemp);
        final TextView sdfree = (TextView) view.findViewById(R.id.sdfree);
        final TextView datafree = (TextView) view.findViewById(R.id.datafree);
        final TextView system_state = (TextView) view.findViewById(R.id.system_state);
        system_state.setText("系统状态：" + (cmdshellTools.IsDualSystem() ? "双系统，" : "单系统，") + (cmdshellTools.CurrentSystemOne() ? "系统一" : "系统二"));
        sdfree.setText("共享存储：" + cmdshellTools.GetDirFreeSizeMB("/sdcard") + " MB");
        datafree.setText("应用存储：" + cmdshellTools.GetDirFreeSizeMB("/data") + " MB");
        batteryMAH = cmdshellTools.getBatteryMAH()+"   ";
        serviceRunning = (ServiceHelper.serviceIsRunning(getContext().getApplicationContext()));

        timer = new Timer();

        timer.schedule(new TimerTask() {
            @Override
            public void run() {

                final String cpuName = cmdshellTools.GetCPUName();

                myHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        switch (cpuName.toLowerCase()) {
                            case "msm8992": {
                                coretemp.setText("核心温度：" + tempConvert(cmdshellTools.GetProp("/sys/class/thermal/thermal_zone15/temp")) + "°C");
                                ctltemp.setText("温控温度：" + cmdshellTools.GetProp("/sys/class/thermal/thermal_zone21/temp") + "°C");
                                break;
                            }
                            case "msm8996": {
                                coretemp.setText("核心温度：" + tempConvert(cmdshellTools.GetProp("/sys/class/thermal/thermal_zone22/temp")) + "°C");
                                ctltemp.setText("温控温度：" + cmdshellTools.GetProp("/sys/class/thermal/thermal_zone28/temp") + "°C");
                                break;
                            }
                        }

                        //battrytemp.setText("电池温度："+ tempConvert( cmdshellTools.GetProp("/sys/class/power_supply/battery/temp"))+"°C");
                        battrystatus.setText("电池信息：" +
                                batteryMAH+
                                tempConvert(cmdshellTools.GetProp("/sys/class/thermal/thermal_zone0/temp")) + "°C   "+
                                cmdshellTools.GetProp("/sys/class/power_supply/battery/health")+ "   " +
                                cmdshellTools.GetProp("/sys/class/power_supply/battery/capacity")+ "%"
                        );

                        powerstatus.setText("电源状态：" +
                                batteryioConvert(cmdshellTools.GetProp("/sys/class/power_supply/battery/current_now")) + "ma      " +
                                (ConfigInfo.getConfigInfo().QcMode && serviceRunning ? "充电已加速" : "未加速")
                        );
                    }
                });
            }
        }, 0, 3000);


        super.onResume();
    }
    String cpuName;
    Button btn_powersave = null;
    Button btn_defaultmode = null;
    Button btn_gamemode = null;
    Button btn_fastmode = null;

    private void setModeState(){
        btn_powersave.setText("省电");
        btn_defaultmode.setText("均衡");
        btn_gamemode.setText("游戏");
        btn_fastmode.setText("极速");
    }

    private void installConfig(){
        if(cpuName==null)
            cpuName = cmdshellTools.GetCPUName();

        if(ConfigInfo.getConfigInfo().UseBigCore)
            AppShared.WriteFile(getContext().getAssets(), cpuName + "/powercfg-bigcore.sh", "powercfg.sh");
        else
            AppShared.WriteFile(getContext().getAssets(), cpuName + "/powercfg-default.sh", "powercfg.sh");
        cmdshellTools.DoCmd(Consts.InstallPowerToggleConfigToCache);
    }

    Button.OnClickListener togglePowerSave = new Button.OnClickListener() {
        @Override
        public void onClick(View v) {
            installConfig();
            btn_powersave.setText("省电 √");
            btn_defaultmode.setText("均衡");
            btn_gamemode.setText("游戏");
            btn_fastmode.setText("极速");

            cmdshellTools.DoCmd(Consts.TogglePowersaveMode);
            Snackbar.make(v,"已切换为省电模式，适合长时间媒体播放或阅读，综合使用时并不效率也不会省电太多！",Snackbar.LENGTH_LONG).show();
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

            cmdshellTools.DoCmd(Consts.ToggleGameMode);
            Snackbar.make(v,"已切换为游戏（性能）模式，但受温度影响并不一定会更快，你可以考虑删除温控！",Snackbar.LENGTH_LONG).show();
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

            cmdshellTools.DoCmd(Consts.ToggleFastMode);
            Snackbar.make(v,"已切换为极速模式，这会大幅增加发热，如果不删除温控性能并不稳定！",Snackbar.LENGTH_LONG).show();
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

            cmdshellTools.DoCmd(Consts.ToggleDefaultMode);
            Snackbar.make(v,"已切换为均衡模式，适合日常使用，速度与耗电平衡！",Snackbar.LENGTH_LONG).show();
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
        EventBus.unSubscribe(Events.ModeToggle,eventSubscribe);
    }

    @Override
    public void onDestroy() {
        EventBus.unSubscribe(Events.ModeToggle,eventSubscribe);

        super.onDestroy();
    }

    cmd_shellTools cmdshellTools;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        cmdshellTools = new cmd_shellTools(null, null);
    }
}

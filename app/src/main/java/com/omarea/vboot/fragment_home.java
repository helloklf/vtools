package com.omarea.vboot;

import android.os.Bundle;
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
import com.omarea.shared.cmd_shellTools;
import com.omarea.shell.DynamicConfig;


public class fragment_home extends Fragment {

    public fragment_home() {
    }

    /*
    void toggleIcon() {
        PackageManager packageManager = getPackageManager();
        ComponentName componentName = new ComponentName(this, "com.smartmadsoft.xposed.aio.Launch");
        int newComponentState = 0x2;
        if(packageManager.getComponentEnabledSetting(componentName) == newComponentState) {
            newComponentState = 0x1;
            String text = "Launcher icon has been restored";
        }
        packageManager.setComponentEnabledSetting(componentName, newComponentState, 0x1);
        Toast.makeText(this, text, 0x0).show();
    }
    */

    View view;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.layout_home, container, false);

        cmdshellTools = new cmd_shellTools(null, null);

        if (new DynamicConfig().DynamicSupport(getContext())) {
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

    IEventSubscribe eventSubscribe = new IEventSubscribe() {
        @Override
        public void messageRecived(Object message) {
            setModeState();
        }
    };

    @Override
    public void onResume() {
        EventBus.INSTANCE.subscribe(Events.INSTANCE.getModeToggle(), eventSubscribe);

        setModeState();

        final TextView sdfree = (TextView) view.findViewById(R.id.sdfree);
        final TextView datafree = (TextView) view.findViewById(R.id.datafree);
        sdfree.setText("共享存储：" + cmdshellTools.GetDirFreeSizeMB("/sdcard") + " MB");
        datafree.setText("应用存储：" + cmdshellTools.GetDirFreeSizeMB("/data") + " MB");
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
        String cfg = cmdshellTools.GetProp2("vtools.powercfg");
        switch (cfg) {
            case "default": {
                btn_defaultmode.setText("均衡 √");
                break;
            }
            case "game": {
                btn_gamemode.setText("游戏 √");
                break;
            }
            case "powersave": {
                btn_powersave.setText("省电 √");
                break;
            }
            case "fast": {
                btn_fastmode.setText("极速 √");
                break;
            }
        }
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

    @Override
    public void onPause() {
        super.onPause();
        try {
            EventBus.INSTANCE.unSubscribe(Events.INSTANCE.getModeToggle(), eventSubscribe);
        } catch (Exception ex) {

        }
    }

    @Override
    public void onDestroy() {
        try {
            EventBus.INSTANCE.unSubscribe(Events.INSTANCE.getModeToggle(), eventSubscribe);
        } catch (Exception ex) {

        }
        super.onDestroy();
    }
    cmd_shellTools cmdshellTools;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }
}

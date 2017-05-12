package com.omarea.vboot;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import com.omarea.shared.ConfigInfo;
import com.omarea.shared.Consts;
import com.omarea.shared.AppShared;
import com.omarea.shared.cmd_shellTools;

public class Main2Activity extends AppCompatActivity {

    cmd_shellTools cmd_shellTools;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);
        String action = getIntent().getAction();
        switch (action) {
            case "clear": {
                cmd_shellTools = new cmd_shellTools(this, null);
                cmd_shellTools.DoCmd(Consts.ClearCache);
                break;
            }
            case "powersave": {
                cmd_shellTools = new cmd_shellTools(this, null);
                InstallPowerToggleConfig();
                cmd_shellTools.DoCmd(Consts.TogglePowersaveMode);
                break;
            }
            case "defaultmode": {
                cmd_shellTools = new cmd_shellTools(this, null);
                InstallPowerToggleConfig();
                cmd_shellTools.DoCmd(Consts.ToggleDefaultMode);
                break;
            }
            case "gamemode": {
                cmd_shellTools = new cmd_shellTools(this, null);
                InstallPowerToggleConfig();
                cmd_shellTools.DoCmd(Consts.ToggleGameMode);
                break;
            }
            case "fastmode": {
                cmd_shellTools = new cmd_shellTools(this, null);
                InstallPowerToggleConfig();
                cmd_shellTools.DoCmd(Consts.ToggleFastMode);
                break;
            }
            case "systemtoggle": {
                cmd_shellTools = new cmd_shellTools(this, null);
                if (cmd_shellTools.IsDualSystem()) {
                    Toast.makeText(this, "正在切换系统，请不要终止操作！", Toast.LENGTH_LONG).show();
                    cmd_shellTools.ToggleSystem();
                    return;
                } else {
                    Toast.makeText(this, "双系统未安装，无法切换！", Toast.LENGTH_LONG).show();
                    finish();
                    return;
                }
            }
            case "android.intent.action.VIEW": {
                break;
            }
            default: {
                break;
            }
        }
        Toast.makeText(this, action + " 操作完成！", Toast.LENGTH_SHORT).show();
        finish();
    }

    String cpuName;
    void InstallPowerToggleConfig() {
        if(cpuName==null)
            cpuName = cmd_shellTools.GetCPUName();

        if(ConfigInfo.getConfigInfo().UseBigCore)
            AppShared.WriteFile(getAssets(), cpuName + "/powercfg-bigcore.sh", "powercfg.sh");
        else
            AppShared.WriteFile(getAssets(), cpuName + "/powercfg-default.sh", "powercfg.sh");
        cmd_shellTools.DoCmd(Consts.InstallPowerToggleConfigToCache);
    }
}

package com.omarea.vboot;

import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.GridLayout;
import android.widget.Switch;
import android.widget.TextView;

import com.omarea.shared.ConfigInfo;
import com.omarea.shared.EventBus;
import com.omarea.shared.Events;
import com.omarea.shared.ServiceHelper;

public class activity_accessibility_service_settings extends AppCompatActivity {

    GridLayout vbootserviceSettings;

    @Override
    protected void onPostResume() {
        super.onPostResume();
        getDelegate().onPostResume();

        if (vbootserviceSettings == null)
            vbootserviceSettings = (GridLayout) findViewById(R.id.vbootserviceSettings);

        boolean serviceState = ServiceHelper.serviceIsRunning(getApplicationContext());

        vbootserviceSettings.setVisibility(serviceState ? View.VISIBLE : View.GONE);

        servicceState.setText(serviceState ? "服务已启动" : "点击此处去勾选辅助服务");
    }

    TextView servicceState;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_accessibility_service_settings);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        servicceState = ((TextView) findViewById(R.id.vbootservice_state));

        servicceState.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
                    startActivity(intent);
                } catch (Exception e) {
                }
            }
        });

        final Switch settings_autoinstall = (Switch) findViewById(R.id.settings_autoinstall);
        final Switch settings_autobooster = (Switch) findViewById(R.id.settings_autobooster);
        final Switch settings_dynamic = (Switch) findViewById(R.id.settings_dynamic);
        final Switch settings_debugmode = (Switch) findViewById(R.id.settings_debugmode);
        final Switch settings_qc = (Switch) findViewById(R.id.settings_qc);
        final Switch settings_delaystart = (Switch) findViewById(R.id.settings_delaystart);
        final Switch settings_bp = (Switch) findViewById(R.id.settings_bp);

        settings_autoinstall.setChecked(ConfigInfo.getConfigInfo().AutoInstall);
        settings_autobooster.setChecked(ConfigInfo.getConfigInfo().AutoBooster);
        settings_dynamic.setChecked(ConfigInfo.getConfigInfo().DyamicCore);
        settings_debugmode.setChecked(ConfigInfo.getConfigInfo().DebugMode);
        settings_qc.setChecked(ConfigInfo.getConfigInfo().QcMode);
        settings_delaystart.setChecked(ConfigInfo.getConfigInfo().DelayStart);
        settings_bp.setChecked(ConfigInfo.getConfigInfo().BatteryProtection);


        settings_bp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ConfigInfo.getConfigInfo().BatteryProtection = settings_bp.isChecked();
            }
        });

        settings_delaystart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ConfigInfo.getConfigInfo().DelayStart = settings_delaystart.isChecked();
            }
        });
        settings_debugmode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ConfigInfo.getConfigInfo().DebugMode = settings_debugmode.isChecked();
            }
        });
        settings_autoinstall.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ConfigInfo.getConfigInfo().AutoInstall = settings_autoinstall.isChecked();
            }
        });
        settings_autobooster.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ConfigInfo.getConfigInfo().AutoBooster = settings_autobooster.isChecked();
            }
        });
        settings_dynamic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ConfigInfo.getConfigInfo().DyamicCore = settings_dynamic.isChecked();
                EventBus.publish(Events.DyamicCoreConfigChanged);
            }
        });
        settings_qc.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ConfigInfo.getConfigInfo().QcMode = settings_qc.isChecked();
            }
        });
    }

    @Override
    public void onPause() {
        ConfigInfo.getConfigInfo().saveChange();

        super.onPause();
    }

    @Override
    protected void onDestroy() {
        ConfigInfo.getConfigInfo().saveChange();

        super.onDestroy();
    }
}

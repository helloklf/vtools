package com.omarea.gesture.fragments;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityManager;
import android.widget.Checkable;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.omarea.gesture.R;
import com.omarea.gesture.SpfConfig;
import com.omarea.gesture.util.ForceHideNavBarThread;
import com.omarea.gesture.util.Overscan;
import com.omarea.gesture.util.ResumeNavBar;

import java.util.List;

public class FragmentBasic extends FragmentSettingsBase {
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.gesture_basic_options, container, false);
    }

    @Override
    public void onResume() {
        super.onResume();
        final Activity activity = getActivity();

        final CompoundButton enable_service = activity.findViewById(R.id.enable_service);
        enable_service.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (serviceRunning()) {
                    // System.exit(0);
                    try {
                        Intent intent = new Intent(getString(R.string.action_service_disable));
                        getActivity().sendBroadcast(intent);
                        v.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                restartService();
                            }
                        }, 1000);
                    } catch (Exception ignored) {
                    }
                } else {
                    try {
                        Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
                        startActivity(intent);
                    } catch (Exception ex) {
                    }
                    String msg = getString(R.string.service_active_desc) + getString(R.string.gesture_app_name);
                    Toast.makeText(activity, msg, Toast.LENGTH_LONG).show();
                }
            }
        });

        bindSeekBar(R.id.bar_hover_time, SpfConfig.CONFIG_HOVER_TIME, SpfConfig.CONFIG_HOVER_TIME_DEFAULT, true);
        bindSeekBar(R.id.vibrator_time, SpfConfig.VIBRATOR_TIME, SpfConfig.VIBRATOR_TIME_DEFAULT, true);
        bindSeekBar(R.id.vibrator_amplitude, SpfConfig.VIBRATOR_AMPLITUDE, SpfConfig.VIBRATOR_AMPLITUDE_DEFAULT, true);

        if (Build.MANUFACTURER.equals("samsung") && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            if (canWriteGlobalSettings()) {
                activity.findViewById(R.id.samsung_guide).setVisibility(View.GONE);
                activity.findViewById(R.id.samsung_options).setVisibility(View.VISIBLE);
                Switch samsung_optimize = activity.findViewById(R.id.samsung_optimize);
                samsung_optimize.setChecked(config.getBoolean(SpfConfig.SAMSUNG_OPTIMIZE, SpfConfig.SAMSUNG_OPTIMIZE_DEFAULT));
                samsung_optimize.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Switch switchItem = activity.findViewById(R.id.samsung_optimize);
                        if (switchItem.isChecked()) {
                            new ForceHideNavBarThread(activity.getContentResolver()).start();
                        } else {
                            new ResumeNavBar(activity.getContentResolver()).run();
                        }
                        config.edit().putBoolean(SpfConfig.SAMSUNG_OPTIMIZE, switchItem.isChecked()).apply();
                    }
                });
            } else {
                activity.findViewById(R.id.samsung_guide).setVisibility(View.VISIBLE);
                activity.findViewById(R.id.samsung_options).setVisibility(View.GONE);
                activity.findViewById(R.id.copy_shell).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        try {
                            ClipboardManager myClipboard = (ClipboardManager) activity.getSystemService(Context.CLIPBOARD_SERVICE);
                            ClipData myClip = ClipData.newPlainText("text",
                                    ((TextView) activity.findViewById(R.id.shell_content)).getText().toString());
                            myClipboard.setPrimaryClip(myClip);
                            Toast.makeText(activity.getBaseContext(), getString(R.string.copy_success), Toast.LENGTH_SHORT).show();
                        } catch (Exception ex) {
                            Toast.makeText(activity.getBaseContext(), getString(R.string.copy_fail), Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        } else {
            /*
            if (canWriteGlobalSettings()) {
                activity.findViewById(R.id.overscan_guide).setVisibility(View.GONE);
                activity.findViewById(R.id.overscan_options).setVisibility(View.VISIBLE);
                Switch overscan_switch = activity.findViewById(R.id.overscan_switch);
                overscan_switch.setChecked(config.getBoolean(SpfConfig.OVERSCAN_SWITCH, SpfConfig.OVERSCAN_SWITCH_DEFAULT));
                overscan_switch.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Switch switchItem = activity.findViewById(R.id.overscan_switch);
                        if (switchItem.isChecked()) {
                            if(!new Overscan().setOverscan(activity)) {
                                Toast.makeText(getActivity(), "隐藏失败", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            new Overscan().resetOverscan(activity);
                        }
                        config.edit().putBoolean(SpfConfig.OVERSCAN_SWITCH, switchItem.isChecked()).apply();
                    }
                });
            } else {
                activity.findViewById(R.id.overscan_guide).setVisibility(View.VISIBLE);
                activity.findViewById(R.id.overscan_options).setVisibility(View.GONE);
                activity.findViewById(R.id.overscan_copy_shell).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        try {
                            ClipboardManager myClipboard = (ClipboardManager) activity.getSystemService(Context.CLIPBOARD_SERVICE);
                            ClipData myClip = ClipData.newPlainText("text",
                                    ((TextView) activity.findViewById(R.id.overscan_shell_content)).getText().toString());
                            myClipboard.setPrimaryClip(myClip);
                            Toast.makeText(activity.getBaseContext(), getString(R.string.copy_success), Toast.LENGTH_SHORT).show();
                        } catch (Exception ex) {
                            Toast.makeText(activity.getBaseContext(), getString(R.string.copy_fail), Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
            */
        }

        updateView();
    }

    private void updateView() {
        Activity activity = getActivity();

        ((Checkable) activity.findViewById(R.id.enable_service)).setChecked(serviceRunning());
    }

    @Override
    protected void restartService() {
        updateView();

        super.restartService();
    }

    private boolean serviceRunning(Context context, String serviceName) {
        AccessibilityManager m = (AccessibilityManager) context.getSystemService(Context.ACCESSIBILITY_SERVICE);
        List<AccessibilityServiceInfo> serviceInfos = m.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK);
        for (AccessibilityServiceInfo serviceInfo : serviceInfos) {
            if (serviceInfo.getId().endsWith(serviceName)) {
                return true;
            }
        }
        return false;
    }

    private boolean serviceRunning() {
        return serviceRunning(getActivity(), "AccessibilitySceneGesture");
    }

    private boolean canWriteGlobalSettings() {
        return new Overscan().canWriteSecureSettings(getActivity());
    }
}

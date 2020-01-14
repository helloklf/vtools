package com.omarea.gesture.ui;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.provider.Settings;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.omarea.gesture.AccessibilitySceneGesture;
import com.omarea.gesture.AppSwitchActivity;
import com.omarea.gesture.DialogFrequentlyAppEdit;
import com.omarea.gesture.R;
import com.omarea.gesture.SpfConfigEx;
import com.omarea.gesture.util.UITools;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class QuickPanel {
    @SuppressLint("StaticFieldLeak")
    private static View view;
    private AccessibilitySceneGesture accessibilityService;
    private ArrayList<AppInfo> apps;

    public QuickPanel(AccessibilitySceneGesture context) {
        accessibilityService = context;
    }

    public void close() {
        WindowManager mWindowManager = (WindowManager) (accessibilityService.getSystemService(Context.WINDOW_SERVICE));
        if (view != null) {
            mWindowManager.removeView(view);
            view = null;
        }
    }

    private WindowManager.LayoutParams getLayoutParams(int x, int y) {
        final WindowManager.LayoutParams params = new WindowManager.LayoutParams();

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP_MR1) {
            params.type = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && Settings.canDrawOverlays(accessibilityService)) {
            params.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            params.type = WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY;
        }

        params.format = PixelFormat.TRANSLUCENT;

        params.width = WindowManager.LayoutParams.WRAP_CONTENT;
        params.height = WindowManager.LayoutParams.WRAP_CONTENT;

        if (x > 0 && y > 0) {
            params.gravity = Gravity.START | Gravity.TOP;

            params.x = x - UITools.dp2px(accessibilityService, 115);
            params.y = y - UITools.dp2px(accessibilityService, 110);
        } else {
            params.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
        }
        params.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_FULLSCREEN | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH;

        params.windowAnimations = android.R.style.Animation_Translucent;
        // params.windowAnimations = android.R.style.Animation_Dialog;

        return params;
    }

    private void saveConfig() {
        SharedPreferences config = accessibilityService.getSharedPreferences(SpfConfigEx.configFile, Context.MODE_PRIVATE);
        if (apps != null) {
            HashSet<String> configApps = new HashSet<>();
            for (AppInfo appInfo : apps) {
                configApps.add(appInfo.packageName);
            }
            config.edit().putStringSet(SpfConfigEx.frequently_apps, configApps).apply();
        }
    }

    private Set<String> getCurrentConfig() {
        SharedPreferences config = accessibilityService.getSharedPreferences(SpfConfigEx.configFile, Context.MODE_PRIVATE);

        return config.getStringSet(SpfConfigEx.frequently_apps, new HashSet<String>() {{
            add("com.tencent.mm");
            add("com.tencent.mobileqq");
            add("com.android.browser");
            add("com.netease.cloudmusic");
            add("com.sankuai.meituan");
            add("com.eg.android.AlipayGphone");
            add("com.android.contacts");
            add("com.android.mms");
        }});
    }

    private ArrayList<AppInfo> listFrequentlyApps() {
        SharedPreferences config = accessibilityService.getSharedPreferences(SpfConfigEx.configFile, Context.MODE_PRIVATE);

        final String[] apps = getCurrentConfig().toArray(new String[0]);

        ArrayList<AppInfo> appInfos = new ArrayList<>();
        final PackageManager pm = accessibilityService.getPackageManager();
        for (String app : apps) {
            try {
                AppInfo appInfo = new AppInfo(app);
                ApplicationInfo applicationInfo = pm.getApplicationInfo(app, 0);
                appInfo.appName = (String) applicationInfo.loadLabel(pm);
                appInfo.icon = applicationInfo.loadIcon(pm);
                appInfos.add(appInfo);
            } catch (Exception ignored) {
            }
        }
        return appInfos;
    }

    private void setFrequentlyAppList(final GridView gridView, final boolean editMode) {
        if (apps == null) {
            apps = listFrequentlyApps();
        }

        gridView.setAdapter(new BaseAdapter() {
            @Override
            public int getCount() {
                return editMode ? (apps.size() + 1) : apps.size();
            }

            @Override
            public Object getItem(int position) {
                return apps.get(position);
            }

            @Override
            public long getItemId(int position) {
                return position;
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                if (position >= apps.size()) {
                    return LayoutInflater.from(accessibilityService).inflate(R.layout.layout_quick_panel_add, null);
                } else {
                    View view = LayoutInflater.from(accessibilityService).inflate(R.layout.gesture_layout_quick_panel_item, null);
                    AppInfo appInfo = (AppInfo) getItem(position);
                    ImageView imageView = view.findViewById(R.id.qp_icon);
                    TextView nameView = view.findViewById(R.id.qp_name);
                    if (appInfo.icon != null) {
                        imageView.setImageDrawable(appInfo.icon);
                    }
                    nameView.setText(appInfo.appName);
                    return view;
                }
            }
        });

        if (editMode) {
            gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    if (position >= apps.size()) {
                        close();
                        new DialogFrequentlyAppEdit(accessibilityService).openEdit(getCurrentConfig());
                        // Toast.makeText(accessibilityService, accessibilityService.getString(R.string.coming_soon), Toast.LENGTH_SHORT).show();
                    } else {
                        apps.remove(position);
                        ((BaseAdapter) gridView.getAdapter()).notifyDataSetChanged();
                    }
                }
            });
        } else {
            gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    Intent intent = AppSwitchActivity.getOpenAppIntent(accessibilityService);
                    intent.putExtra("app", apps.get(position).packageName);
                    accessibilityService.startActivity(intent);

                    close();
                }
            });

            gridView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
                @Override
                public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                    Intent intent = AppSwitchActivity.getOpenAppIntent(accessibilityService);
                    intent.putExtra("app-window", apps.get(position).packageName);
                    accessibilityService.startActivity(intent);

                    close();
                    return false;
                }
            });
        }
    }

    public void open(int x, int y) {
        WindowManager mWindowManager = (WindowManager) (accessibilityService.getSystemService(Context.WINDOW_SERVICE));
        close();

        view = LayoutInflater.from(accessibilityService).inflate(R.layout.layout_quick_panel, null);
        view.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_OUTSIDE) {
                    close();
                }
                return false;
            }
        });

        final View editBtn = view.findViewById(R.id.quick_edit);
        final View saveBtn = view.findViewById(R.id.quick_save);
        final View questionBtn = view.findViewById(R.id.quick_question);
        saveBtn.setVisibility(View.GONE);
        editBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                editBtn.setVisibility(View.GONE);
                saveBtn.setVisibility(View.VISIBLE);
                setFrequentlyAppList((GridView) view.findViewById(R.id.quick_apps), true);
            }
        });
        saveBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                editBtn.setVisibility(View.VISIBLE);
                saveBtn.setVisibility(View.GONE);
                saveConfig();
                setFrequentlyAppList((GridView) view.findViewById(R.id.quick_apps), false);
            }
        });
        questionBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                close();
                Toast.makeText(accessibilityService, accessibilityService.getString(R.string.quick_question), Toast.LENGTH_LONG).show();
            }
        });

        setFrequentlyAppList((GridView) view.findViewById(R.id.quick_apps), false);

        mWindowManager.addView(view, getLayoutParams(x, y));
    }

    class AppInfo {
        String appName;
        String packageName;
        Drawable icon;

        AppInfo(String packageName) {
            this.packageName = packageName;
        }
    }
}

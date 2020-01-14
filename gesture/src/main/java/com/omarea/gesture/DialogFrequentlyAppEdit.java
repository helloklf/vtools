package com.omarea.gesture;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.PixelFormat;
import android.os.Build;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.omarea.gesture.util.AppInfo;
import com.omarea.gesture.util.AppListHelper;
import com.omarea.gesture.util.UITools;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class DialogFrequentlyAppEdit {
    private AccessibilitySceneGesture accessibilityService;

    public DialogFrequentlyAppEdit(AccessibilitySceneGesture accessibilityServiceKeyEvent) {
        accessibilityService = accessibilityServiceKeyEvent;
    }

    private WindowManager.LayoutParams getLayoutParams() {
        final WindowManager.LayoutParams params = new WindowManager.LayoutParams();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            params.type = WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY;
        } else {
            params.type = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;
        }

        params.format = PixelFormat.TRANSLUCENT;

        params.width = UITools.dp2px(accessibilityService, 240); // WindowManager.LayoutParams.MATCH_PARENT;
        params.height = UITools.dp2px(accessibilityService, 300); // WindowManager.LayoutParams.MATCH_PARENT;

        params.gravity = Gravity.CENTER;

        params.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_FULLSCREEN | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH;
        return params;
    }

    public void openEdit(Set<String> current) {
        final WindowManager mWindowManager = (WindowManager) (accessibilityService.getSystemService(Context.WINDOW_SERVICE));

        final ArrayList<AppInfo> appInfos = new AppListHelper().loadAppList(accessibilityService);
        final Boolean[] status = new Boolean[appInfos.size()];
        for (int i = 0; i < appInfos.size(); i++) {
            status[i] = current.contains(appInfos.get(i).packageName);
        }

        BaseAdapter adapter = new BaseAdapter() {
            @Override
            public int getCount() {
                return appInfos.size();
            }

            @Override
            public Object getItem(int position) {
                return appInfos.get(position);
            }

            @Override
            public long getItemId(int position) {
                return position;
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
                View view = layoutInflater.inflate(R.layout.gesture_layout_app_option2, null);
                TextView title = view.findViewById(R.id.item_title);
                TextView desc = view.findViewById(R.id.item_desc);
                CheckBox state = view.findViewById(R.id.item_state);

                AppInfo appInfo = (AppInfo) getItem(position);
                title.setText(appInfo.appName);
                desc.setText(appInfo.packageName);
                state.setChecked(status[position]);

                return view;
            }
        };

        final View view = LayoutInflater.from(accessibilityService).inflate(R.layout.layout_frequently_app_edit, null);
        final ListView listView = view.findViewById(R.id.app_list);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                CheckBox checkBox = view.findViewById(R.id.item_state);
                checkBox.setChecked(!checkBox.isChecked());

                status[position] = checkBox.isChecked();
            }
        });

        view.findViewById(R.id.quick_save).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    HashSet<String> configApps = new HashSet<>();
                    for (int i = 0; i < appInfos.size(); i++) {
                        if (status[i]) {
                            configApps.add(appInfos.get(i).packageName);
                        }
                    }
                    SharedPreferences config = accessibilityService.getSharedPreferences(SpfConfigEx.configFile, Context.MODE_PRIVATE);
                    config.edit().putStringSet(SpfConfigEx.frequently_apps, configApps).apply();
                    Toast.makeText(accessibilityService, accessibilityService.getString(R.string.save_succeed), Toast.LENGTH_SHORT).show();
                } catch (Exception ex) {
                    Toast.makeText(accessibilityService, accessibilityService.getString(R.string.save_fail), Toast.LENGTH_SHORT).show();
                }

                mWindowManager.removeView(view);
            }
        });

        view.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_OUTSIDE) {
                    mWindowManager.removeView(view);
                }
                return true;
            }
        });

        mWindowManager.addView(view, getLayoutParams());
    }
}

package com.omarea.vboot;

import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TabHost;
import android.widget.TextView;

import com.omarea.shared.ConfigInfo;
import com.omarea.shared.ServiceHelper;
import com.omarea.shared.cmd_shellTools;
import com.omarea.ui.list_adapter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static android.widget.AdapterView.OnItemClickListener;


public class fragment_booster extends Fragment {

    public fragment_booster() {
        // Required empty public constructor
    }

    View frameView;

    cmd_shellTools cmdshellTools = null;
    activity_main thisview = null;

    public static Fragment Create(activity_main thisView, cmd_shellTools cmdshellTools) {
        fragment_booster fragment = new fragment_booster();
        fragment.cmdshellTools = cmdshellTools;
        fragment.thisview = thisView;
        return fragment;
    }


    Button btn_booster_service_not_active;
    Button btn_booster_dynamicservice_not_active;
    Switch cacheclear;
    Switch dozemod;

    @Override
    public void onResume() {
        boolean serviceState = ServiceHelper.serviceIsRunning(getContext());
        btn_booster_service_not_active.setVisibility(serviceState ? GONE : VISIBLE);
        btn_booster_dynamicservice_not_active.setVisibility((serviceState && !ConfigInfo.getConfigInfo().AutoBooster) ? VISIBLE : GONE);

        super.onResume();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.layout_booster, container, false);
    }


    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        this.frameView = view;

        btn_booster_service_not_active = (Button) view.findViewById(R.id.btn_booster_service_not_active);
        btn_booster_dynamicservice_not_active = (Button) view.findViewById(R.id.btn_booster_dynamicservice_not_active);
        cacheclear = (Switch) view.findViewById(R.id.cacheclear);
        dozemod = (Switch) view.findViewById(R.id.dozemod);

        cacheclear.setChecked( ConfigInfo.getConfigInfo().AutoClearCache);
        dozemod.setChecked( ConfigInfo.getConfigInfo().UsingDozeMod);

        cacheclear.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                ConfigInfo.getConfigInfo().AutoClearCache = isChecked;
            }
        });
        dozemod.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                ConfigInfo.getConfigInfo().UsingDozeMod = isChecked;
            }
        });

        btn_booster_service_not_active.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
                    startActivity(intent);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        btn_booster_dynamicservice_not_active.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(thisview, activity_accessibility_service_settings.class);
                startActivity(intent);
            }
        });

        final TabHost tabHost = (TabHost) view.findViewById(R.id.blacklist_tabhost);
        tabHost.setup();

        tabHost.addTab(tabHost.newTabSpec("tab_1")
                .setContent(R.id.blacklist_tab1).setIndicator("阻止后台",
                        this.getResources().getDrawable(R.drawable.check)));
        tabHost.addTab(tabHost.newTabSpec("tab_2")
                .setContent(R.id.blacklist_tab2).setIndicator("配置文件",
                        this.getResources().getDrawable(R.drawable.check)));
        tabHost.setCurrentTab(0);

        SetList(view);

        booster_blacklist = (ListView) view.findViewById(R.id.booster_blacklist);

        OnItemClickListener config_powersavelistClick = new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, final int position, long id) {
                ToogleBlackItem(((TextView) (view.findViewById(R.id.ItemText))).getText().toString());
                CheckBox checkBox = ((CheckBox) (view.findViewById(R.id.select_state)));
                checkBox.setChecked(!checkBox.isChecked());
                System.out.print(view);
            }
        };
        booster_blacklist.setOnItemClickListener(config_powersavelistClick);
    }

    ListView booster_blacklist;

    final Handler myHandler = new Handler() {
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
        }
    };

    private void SetList(View view) {
        thisview.progressBar.setVisibility(View.VISIBLE);

        new Thread(new Runnable() {
            @Override
            public void run() {
                if (installedList == null) {
                    LoadList();
                }
                SetListData(installedList, booster_blacklist);
            }
        }).start();
    }

    void SetListData(final ArrayList<HashMap<String, Object>> dl, final ListView lv) {
        myHandler.post(new Runnable() {
            @Override
            public void run() {
                thisview.progressBar.setVisibility(View.GONE);
                lv.setAdapter(new list_adapter(getContext(), dl));
            }
        });
    }

    PackageManager packageManager;
    List<ApplicationInfo> packageInfos;
    ArrayList<HashMap<String, Object>> installedList;

    void LoadList() {
        packageManager = thisview.getPackageManager();
        packageInfos = packageManager.getInstalledApplications(0);

        installedList = new ArrayList<>();/*在数组中存放数据*/
        for (int i = 0; i < packageInfos.size(); i++) {
            if (packageInfos.get(i).sourceDir.indexOf("/system") == 0)
                continue;
            HashMap<String, Object> item = new HashMap<String, Object>();
            Drawable d = packageInfos.get(i).loadIcon(packageManager);
            item.put("icon", d);
            String pkgName = packageInfos.get(i).packageName.toLowerCase();
            item.put("select_state", false);
            for (String blitem : ConfigInfo.getConfigInfo().blacklist) {
                if (blitem.equals(pkgName)) {
                    item.put("select_state", true);
                    break;
                }
            }
            item.put("name", packageInfos.get(i).loadLabel(packageManager));
            item.put("packageName", pkgName);
            installedList.add(item);
        }
    }

    void ToogleBlackItem(String pkgName) {
        try {
            if (!ConfigInfo.getConfigInfo().blacklist.contains(pkgName)) {
                ConfigInfo.getConfigInfo().blacklist.add(pkgName);
            } else {
                ConfigInfo.getConfigInfo().blacklist.remove(pkgName);
            }
        } catch (Exception ex) {

        }
    }
}

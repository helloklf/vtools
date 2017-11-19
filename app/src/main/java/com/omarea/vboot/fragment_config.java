package com.omarea.vboot;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TabHost;
import android.widget.TextView;

import com.omarea.shared.ConfigInfo;
import com.omarea.shared.EventBus;
import com.omarea.shared.Events;
import com.omarea.shared.ServiceHelper;
import com.omarea.shared.SpfConfig;
import com.omarea.shared.cmd_shellTools;
import com.omarea.shell.Platform;
import com.omarea.ui.list_adapter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static android.view.View.OnClickListener;
import static android.widget.AdapterView.GONE;
import static android.widget.AdapterView.OnItemClickListener;
import static android.widget.AdapterView.VISIBLE;


public class fragment_config extends Fragment {
    cmd_shellTools cmdshellTools = null;
    main thisview = null;

    SharedPreferences spfPowercfg;
    SharedPreferences.Editor editor;

    public static Fragment Create(main thisView, cmd_shellTools cmdshellTools) {
        fragment_config fragment = new fragment_config();
        fragment.cmdshellTools = cmdshellTools;
        fragment.thisview = thisView;
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.layout_config, container, false);
    }

    Button btn_config_service_not_active;
    Button btn_config_dynamicservice_not_active;

    @Override
    public void onResume() {
        boolean serviceState = ServiceHelper.Companion.serviceIsRunning(getContext());
        btn_config_service_not_active.setVisibility(serviceState ? GONE : VISIBLE);
        btn_config_dynamicservice_not_active.setVisibility((serviceState && !ConfigInfo.getConfigInfo().DyamicCore) ? VISIBLE : GONE);

        super.onResume();
    }

    @Override
    public void onViewCreated(final View view, @Nullable Bundle savedInstanceState) {
        spfPowercfg = getContext().getSharedPreferences(SpfConfig.POWER_CONFIG_SPF, Context.MODE_PRIVATE);
        editor = spfPowercfg.edit();

        btn_config_service_not_active = (Button) view.findViewById(R.id.btn_config_service_not_active);
        btn_config_dynamicservice_not_active = (Button) view.findViewById(R.id.btn_config_dynamicservice_not_active);

        btn_config_service_not_active.setOnClickListener(new OnClickListener() {
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
        btn_config_dynamicservice_not_active.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(thisview, accessibility_settings.class);
                startActivity(intent);
            }
        });

        final FloatingActionButton btn = (FloatingActionButton) view.findViewById(R.id.config_addtodefaultlist);
        final TabHost tabHost = (TabHost) view.findViewById(R.id.configlist_tabhost);

        tabHost.setup();

        tabHost.addTab(tabHost.newTabSpec("def_tab").setContent(R.id.configlist_tab0).setIndicator("均衡"));
        tabHost.addTab(tabHost.newTabSpec("game_tab").setContent(R.id.configlist_tab1).setIndicator("性能"));
        tabHost.addTab(tabHost.newTabSpec("power_tab").setContent(R.id.configlist_tab2).setIndicator("省电"));
        tabHost.addTab(tabHost.newTabSpec("fast_tab").setContent(R.id.configlist_tab3).setIndicator("极速"));
        tabHost.addTab(tabHost.newTabSpec("fast_tab").setContent(R.id.configlist_tab4).setIndicator("忽略"));
        tabHost.addTab(tabHost.newTabSpec("confg_tab").setContent(R.id.configlist_tab5).setIndicator("设置"));
        tabHost.setCurrentTab(0);
        tabHost.setOnTabChangedListener(new TabHost.OnTabChangeListener() {
            @Override
            public void onTabChanged(String tabId) {
                btn.setVisibility((tabHost.getCurrentTab() > 4) ? GONE : VISIBLE);
            }
        });

        btn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (tabHost.getCurrentTab()) {
                    case 0: {
                        final AlertDialog.Builder builder = new AlertDialog.Builder(thisview);
                        String[] items = new String[]{"添加选中到 -> 性能模式", "添加选中到 -> 省电模式", "添加选中到 -> 极速模式", "添加选中到 -> 忽略列表"};
                        final Configs[] configses = new Configs[]{Configs.Game, Configs.PowerSave, Configs.Fast, Configs.Ignored};
                        builder.setItems(items, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                list_adapter listadapter = (list_adapter) config_defaultlist.getAdapter();
                                AddToList(defaultList, listadapter.states, configses[which]);
                            }
                        });
                        builder.setIcon(R.drawable.ic_menu_profile).setTitle("设置配置模式").create().show();
                        break;
                    }
                    case 1: {
                        final AlertDialog.Builder builder = new AlertDialog.Builder(thisview);
                        String[] items = new String[]{"添加选中到 -> 均衡模式", "添加选中到 -> 省电模式", "添加选中到 -> 极速模式", "添加选中到 -> 忽略列表"};
                        final Configs[] configses = new Configs[]{Configs.Default, Configs.PowerSave, Configs.Fast, Configs.Ignored};
                        builder.setItems(items, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                list_adapter listadapter = (list_adapter) config_gamelist.getAdapter();
                                AddToList(gameList, listadapter.states, configses[which]);
                            }
                        });
                        builder.setIcon(R.drawable.ic_menu_profile).setTitle("设置配置模式").create().show();
                        break;
                    }
                    case 2: {
                        final AlertDialog.Builder builder = new AlertDialog.Builder(thisview);
                        String[] items = new String[]{"添加选中到 -> 均衡模式", "添加选中到 -> 性能模式", "添加选中到 -> 极速模式", "添加选中到 -> 忽略列表"};
                        final Configs[] configses = new Configs[]{Configs.Default, Configs.Game, Configs.Fast, Configs.Ignored};
                        builder.setItems(items, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                list_adapter listadapter = (list_adapter) config_powersavelist.getAdapter();
                                AddToList(powersaveList, listadapter.states, configses[which]);
                            }
                        });
                        builder.setIcon(R.drawable.ic_menu_profile).setTitle("设置配置模式").create().show();
                        break;
                    }
                    case 3: {
                        final AlertDialog.Builder builder = new AlertDialog.Builder(thisview);
                        String[] items = new String[]{"添加选中到 -> 均衡模式", "添加选中到 -> 性能模式", "添加选中到 -> 省电模式", "添加选中到 -> 忽略列表"};
                        final Configs[] configses = new Configs[]{Configs.Default, Configs.Game, Configs.PowerSave, Configs.Ignored};
                        builder.setItems(items, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                list_adapter listadapter = (list_adapter) config_fastlist.getAdapter();
                                AddToList(fastList, listadapter.states, configses[which]);
                            }
                        });
                        builder.setIcon(R.drawable.ic_menu_profile).setTitle("设置配置模式").create().show();
                        break;
                    }
                    case 4: {
                        final AlertDialog.Builder builder = new AlertDialog.Builder(thisview);
                        String[] items = new String[]{"添加选中到 -> 均衡模式", "添加选中到 -> 性能模式", "添加选中到 -> 省电模式", "添加选中到 -> 极速模式"};
                        final Configs[] configses = new Configs[]{Configs.Default, Configs.Game, Configs.PowerSave, Configs.Fast};
                        builder.setItems(items, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                list_adapter listadapter = (list_adapter) config_ignoredlist.getAdapter();
                                AddToList(ignoredList, listadapter.states, configses[which]);
                            }
                        });
                        builder.setIcon(R.drawable.ic_menu_profile).setTitle("设置配置模式").create().show();
                        break;
                    }
                }
            }
        });

        final Switch default_config_bigcore = (Switch) view.findViewById(R.id.default_config_bigcore);
        boolean useBigCore = ConfigInfo.getConfigInfo().UseBigCore;
        default_config_bigcore.setChecked(useBigCore);

        setConfigInfoText();

        default_config_bigcore.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean useBigCore = default_config_bigcore.isChecked();
                ConfigInfo.getConfigInfo().UseBigCore = useBigCore;
                setConfigInfoText();
                EventBus.INSTANCE.publish(Events.INSTANCE.getCoreConfigChanged());
            }
        });

        final Switch config_showSystemApp = (Switch) view.findViewById(R.id.config_showSystemApp);
        config_showSystemApp.setChecked(ConfigInfo.getConfigInfo().HasSystemApp);
        config_showSystemApp.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                ConfigInfo.getConfigInfo().HasSystemApp = config_showSystemApp.isChecked();
                LoadList();
            }
        });

        config_defaultlist = (ListView) view.findViewById(R.id.config_defaultlist);
        config_gamelist = (ListView) view.findViewById(R.id.config_gamelist);
        config_powersavelist = (ListView) view.findViewById(R.id.config_powersavelist);
        config_fastlist = (ListView) view.findViewById(R.id.config_fastlist);
        config_ignoredlist = (ListView) view.findViewById(R.id.config_ignoredlist);

        config_defaultlist.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View current, final int position, long id) {
                CheckBox select_state = (CheckBox) current.findViewById(R.id.select_state);
                if (select_state != null)
                    select_state.setChecked(!select_state.isChecked());
                defaultList.get(position).put("select_state", !select_state.isChecked());
            }
        });

        config_gamelist.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View current, final int position, long id) {
                CheckBox select_state = (CheckBox) current.findViewById(R.id.select_state);
                if (select_state != null)
                    select_state.setChecked(!select_state.isChecked());
                gameList.get(position).put("select_state", !select_state.isChecked());
            }
        });

        config_powersavelist.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View current, final int position, long id) {
                CheckBox select_state = (CheckBox) current.findViewById(R.id.select_state);
                if (select_state != null)
                    select_state.setChecked(!select_state.isChecked());
                powersaveList.get(position).put("select_state", !select_state.isChecked());
            }
        });

        config_fastlist.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View current, final int position, long id) {
                CheckBox select_state = (CheckBox) current.findViewById(R.id.select_state);
                if (select_state != null)
                    select_state.setChecked(!select_state.isChecked());
                fastList.get(position).put("select_state", !select_state.isChecked());
            }
        });

        config_ignoredlist.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View current, final int position, long id) {
                CheckBox select_state = (CheckBox) current.findViewById(R.id.select_state);
                if (select_state != null)
                    select_state.setChecked(!select_state.isChecked());
                ignoredList.get(position).put("select_state", !select_state.isChecked());
            }
        });

        LoadList();
    }

    //设置配置文件提示信息
    private void setConfigInfoText() {
        final String cpuName = new Platform().GetCPUName();
        final TextView defaultconfighelp = (TextView) thisview.findViewById(R.id.defaultconfighelp);
        switch (cpuName.toLowerCase()) {
            case "msm8996": {
                defaultconfighelp.setText(ConfigInfo.getConfigInfo().UseBigCore ? R.string.defaultconfighelp_bigcore_820 : R.string.defaultconfighelp_820);
                break;
            }
            case "msm8998": {
                defaultconfighelp.setText(ConfigInfo.getConfigInfo().UseBigCore ? R.string.defaultconfighelp_bigcore_835 : R.string.defaultconfighelp_835);
                break;
            }
            default: {
                defaultconfighelp.setText("");
                break;
            }
        }
    }

    ListView config_defaultlist;
    ListView config_gamelist;
    ListView config_powersavelist;
    ListView config_ignoredlist;
    ListView config_fastlist;

    final Handler myHandler = new Handler() {
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
        }
    };

    void SetListData(final ArrayList<HashMap<String, Object>> dl, final ListView lv) {
        myHandler.post(new Runnable() {
            @Override
            public void run() {
                lv.setAdapter(new list_adapter(getContext(), dl));
                thisview.progressBar.setVisibility(View.GONE);
            }
        });
    }

    private ArrayList<HashMap<String, Object>> defaultList;
    private ArrayList<HashMap<String, Object>> gameList;
    private ArrayList<HashMap<String, Object>> powersaveList;
    private ArrayList<HashMap<String, Object>> fastList;
    private ArrayList<HashMap<String, Object>> ignoredList;
    private ArrayList<HashMap<String, Object>> installedList;

    ArrayList<HashMap<String, Object>> GetAppIcon(List<HashMap<String, Object>> list) {
        ArrayList<HashMap<String, Object>> newDic = new ArrayList<>();

        String pkg;
        for (HashMap<String, Object> row : list) {
            HashMap<String, Object> newRow = null;
            pkg = row.get("packageName").toString();

            for (HashMap<String, Object> item : installedList) {
                String pkg2 = item.get("packageName").toString();
                if (pkg.equals(pkg2)) {
                    newRow = new HashMap<String, Object>();
                    for (String key : item.keySet()) {
                        newRow.put(key, item.get(key));
                    }
                    break;
                }
            }
            if (newRow == null) {
                newRow = new HashMap<String, Object>();
                for (String key : row.keySet()) {
                    newRow.put(key, row.get(key));
                }
            }
            newDic.add(newRow);
        }
        return newDic;
    }

    PackageManager packageManager;

    void LoadList() {
        thisview.progressBar.setVisibility(View.VISIBLE);
        if (packageManager == null) {
            packageManager = thisview.getPackageManager();
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                if (installedList == null || installedList.size() == 0) {
                    List<ApplicationInfo> packageInfos = packageManager.getInstalledApplications(0);

                    installedList = new ArrayList<HashMap<String, Object>>();/*在数组中存放数据*/
                    Boolean hasSystemApp = ConfigInfo.getConfigInfo().HasSystemApp;
                    for (int i = 0; i < packageInfos.size(); i++) {
                        if (!hasSystemApp && (packageInfos.get(i).sourceDir.indexOf("/system") == 0)) {
                            continue;
                        }

                        HashMap<String, Object> item = new HashMap<String, Object>();
                        item.put("icon", packageInfos.get(i).loadIcon(packageManager));
                        item.put("name", packageInfos.get(i).loadLabel(packageManager));
                        item.put("packageName", packageInfos.get(i).packageName.toLowerCase());
                        item.put("select_state", false);
                        installedList.add(item);
                    }
                    installedList = GetAppIcon(installedList);
                }
                defaultList = new ArrayList<HashMap<String, Object>>();
                gameList = new ArrayList<HashMap<String, Object>>();
                powersaveList = new ArrayList<HashMap<String, Object>>();
                fastList = new ArrayList<HashMap<String, Object>>();
                ignoredList = new ArrayList<HashMap<String, Object>>();

                for (int i = 0; i < installedList.size(); i++) {
                    HashMap<String, Object> item = installedList.get(i);
                    if (item.containsKey("select_state")) {
                        item.remove("select_state");
                    }
                    item.put("select_state", false);
                    String config = spfPowercfg.getString(installedList.get(i).get("packageName").toString().toLowerCase(), "default");
                    switch (config) {
                        case "powersave": {
                            powersaveList.add(installedList.get(i));
                            break;
                        }
                        case "game": {
                            gameList.add(installedList.get(i));
                            break;
                        }
                        case "fast": {
                            fastList.add(installedList.get(i));
                            break;
                        }
                        case "igoned": {
                            ignoredList.add(installedList.get(i));
                            break;
                        }
                        default: {
                            defaultList.add(installedList.get(i));
                            break;
                        }
                    }
                }
                myHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        thisview.progressBar.setVisibility(View.GONE);
                        SetListData(defaultList, config_defaultlist);
                        SetListData(gameList, config_gamelist);
                        SetListData(powersaveList, config_powersavelist);
                        SetListData(fastList, config_fastlist);
                        SetListData(ignoredList, config_ignoredlist);
                    }
                });
            }
        }).start();
    }

    /**
     * 从当前列表中获取已选中的应用，添加到指定模式
     *
     * @param list     当前列表
     * @param postions 各个序号的选中状态
     * @param config   指定的新模式
     */
    void AddToList(ArrayList<HashMap<String, Object>> list, HashMap<Integer, Boolean> postions, Configs config) {
        ArrayList<HashMap<String, Object>> selectedItems = new ArrayList<>();
        for (int position : postions.keySet()) {
            if (postions.get(position) == true) {
                HashMap<String, Object> item = list.get(position);
                selectedItems.add(item);
                switch (config) {
                    case Default: {
                        editor.putString(item.get("packageName").toString().toLowerCase(), "default").commit();
                        break;
                    }
                    case Game: {
                        editor.putString(item.get("packageName").toString().toLowerCase(), "game").commit();
                        break;
                    }
                    case PowerSave: {
                        editor.putString(item.get("packageName").toString().toLowerCase(), "powersave").commit();
                        break;
                    }
                    case Fast: {
                        editor.putString(item.get("packageName").toString().toLowerCase(), "fast").commit();
                        break;
                    }
                    case Ignored: {
                        editor.putString(item.get("packageName").toString().toLowerCase(), "igoned").commit();
                        break;
                    }
                }
            }
        }
        try {
            LoadList();
        } catch (Exception ex) {

        }
    }

    enum Configs {
        Default,
        Game,
        PowerSave,
        Fast,
        Ignored;
    }
}

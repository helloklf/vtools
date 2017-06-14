package com.omarea.vboot;

import android.content.DialogInterface;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.TabHost;

import com.omarea.shared.Consts;
import com.omarea.shared.cmd_shellTools;
import com.omarea.ui.list_adapter2;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static android.widget.AdapterView.OnItemClickListener;


public class fragment_applistions extends Fragment {

    public fragment_applistions() {
        // Required empty public constructor
    }

    View frameView;

    cmd_shellTools cmdshellTools = null;
    main thisview = null;

    public static Fragment Create(main thisView, cmd_shellTools cmdshellTools) {
        fragment_applistions fragment = new fragment_applistions();
        fragment.cmdshellTools = cmdshellTools;
        fragment.thisview = thisView;
        return fragment;
    }

    @Override
    public void onResume() {

        super.onResume();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.layout_applictions, container, false);
    }


    @Override
    public void onViewCreated(final View view, @Nullable Bundle savedInstanceState) {
        this.frameView = view;

        final TabHost tabHost = (TabHost) view.findViewById(R.id.blacklist_tabhost);
        tabHost.setup();

        tabHost.addTab(tabHost.newTabSpec("tab_1")
                .setContent(R.id.tab_apps_user).setIndicator("用户程序",
                        this.getResources().getDrawable(R.drawable.check)));
        tabHost.addTab(tabHost.newTabSpec("tab_2")
                .setContent(R.id.tab_apps_system).setIndicator("系统自带",
                        this.getResources().getDrawable(R.drawable.check)));
        tabHost.addTab(tabHost.newTabSpec("tab_3")
                .setContent(R.id.tab_apps_backuped).setIndicator("已备份",
                        this.getResources().getDrawable(R.drawable.check)));
        tabHost.setCurrentTab(0);

        tab_apps_user = (ListView) (view.findViewById(R.id.apps_userlist));
        tab_apps_system = (ListView) (view.findViewById(R.id.apps_systemlist));
        tab_apps_backuped = (ListView) (view.findViewById(R.id.apps_backupedlist));

        OnItemClickListener config_powersavelistClick = new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, final int position, long id) {
                CheckBox checkBox = ((CheckBox) (view.findViewById(R.id.select_state)));
                checkBox.setChecked(!checkBox.isChecked());
            }
        };
        tab_apps_user.setOnItemClickListener(config_powersavelistClick);
        tab_apps_system.setOnItemClickListener(config_powersavelistClick);
        tab_apps_backuped.setOnItemClickListener(config_powersavelistClick);

        ((FloatingActionButton) view.findViewById(R.id.fab_apps_user)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                list_adapter2 listadapter = (list_adapter2) tab_apps_user.getAdapter();
                final HashMap<Integer, Boolean> states = listadapter.states;
                AlertDialog.Builder builder = new AlertDialog.Builder(thisview);
                builder.setTitle("请选择操作");
                builder.setItems(
                        new String[]{
                                "冻结",
                                "解冻",
                                "卸载-删除数据",
                                "卸载-保留数据",
                                "备份应用",
                                "取消"
                        },
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if (which == 5)
                                    return;

                                ArrayList<HashMap<String, Object>> selectedItems = new ArrayList<>();
                                for (int position : states.keySet()) {
                                    if (states.get(position) == true) {
                                        selectedItems.add(installedList.get(position));
                                    }
                                }

                                switch (which) {
                                    case 0: {
                                        disabledApp(selectedItems, false);
                                        break;
                                    }
                                    case 1: {
                                        enableApp(selectedItems, false);
                                        break;
                                    }
                                    case 2: {
                                        uninstallApp(selectedItems, false);
                                        break;
                                    }
                                    case 3: {
                                        uninstallApp(selectedItems, true);
                                        break;
                                    }
                                    case 4: {
                                        backupApp(selectedItems);
                                        break;
                                    }
                                }
                            }
                        }
                );
                builder.show();
            }
        });


        ((FloatingActionButton) view.findViewById(R.id.fab_apps_system)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final list_adapter2 listadapter = (list_adapter2) tab_apps_system.getAdapter();
                final HashMap<Integer, Boolean> states = listadapter.states;
                AlertDialog.Builder builder = new AlertDialog.Builder(thisview);
                builder.setTitle("系统应用，请谨慎操作！！！");
                builder.setItems(
                        new String[]{
                                "冻结",
                                "解冻",
                                "删除-需要重启",
                                "取消"
                        },
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if (which == 3)
                                    return;

                                ArrayList<HashMap<String, Object>> selectedItems = new ArrayList<>();
                                for (int position : states.keySet()) {
                                    if (states.get(position) == true) {
                                        selectedItems.add(systemList.get(position));
                                    }
                                }

                                switch (which) {
                                    case 0: {
                                        disabledApp(selectedItems, true);
                                        break;
                                    }
                                    case 1: {
                                        enableApp(selectedItems, true);
                                        break;
                                    }
                                    case 2: {
                                        deleteApp(selectedItems);
                                        break;
                                    }
                                }
                            }
                        }
                );
                builder.show();
            }
        });


        ((FloatingActionButton) view.findViewById(R.id.fab_apps_backuped)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final list_adapter2 listadapter = (list_adapter2) tab_apps_backuped.getAdapter();
                final HashMap<Integer, Boolean> states = listadapter.states;
                AlertDialog.Builder builder = new AlertDialog.Builder(thisview);
                builder.setTitle("系统应用，请谨慎操作！！！");
                builder.setItems(
                        new String[]{
                                "安装（ROOT模式）",
                                "删除备份",
                                "取消"
                        },
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if (which == 3)
                                    return;

                                ArrayList<HashMap<String, Object>> selectedItems = new ArrayList<>();
                                for (int position : states.keySet()) {
                                    if (states.get(position) == true) {
                                        selectedItems.add(backupedList.get(position));
                                    }
                                }

                                switch (which) {
                                    case 0: {
                                        installApp(selectedItems);
                                        break;
                                    }
                                    case 1: {
                                        deleteBackup(selectedItems);
                                        break;
                                    }
                                }
                            }
                        }
                );
                builder.show();
            }
        });
        setList();
    }

    private void installApp(final ArrayList<HashMap<String, Object>> apps) {
        if (apps == null || apps.size() == 0)
            return;
        final AlertDialog.Builder builder = new AlertDialog.Builder(thisview);
        builder.setTitle("安装提示");
        builder.setMessage("\n这需要好些时间，而且期间你看不到安装进度，只能通过桌面程序观察应用是否已经安装！\n\n");
        builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                try {
                    final StringBuffer stringBuffer = new StringBuffer();
                    for (int i = 0; i < apps.size(); i++) {
                        String path = apps.get(i).get("path").toString();
                        File file = new File(path);
                        if (file.exists()) {
                            stringBuffer.append("pm install -r \"");
                            stringBuffer.append(path);
                            stringBuffer.append("\";\n");
                        }
                    }
                    thisview.progressBar.setVisibility(View.VISIBLE);
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            cmdshellTools.DoCmdSync(stringBuffer.toString());
                            backupedList = null;
                            installedList = null;
                            systemList = null;
                            allApps.clear();
                            setList();
                        }
                    }).start();
                } catch (Exception ex) {

                }
            }
        });
        builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                for (int i = 0; i < apps.size(); i++) {
                    File file = new File(apps.get(i).get("path").toString());
                    if (file.exists() && file.canWrite())
                        file.delete();
                }
                //backupedList = null;
                //setList();
            }
        });
        builder.show();
    }

    private void deleteBackup(final ArrayList<HashMap<String, Object>> apps) {
        if (apps == null || apps.size() == 0)
            return;
        final AlertDialog.Builder builder = new AlertDialog.Builder(thisview);
        builder.setTitle("删除提示");
        builder.setMessage("\n确定删除？\n\n");
        builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                for (int i = 0; i < apps.size(); i++) {
                    File file = new File(apps.get(i).get("path").toString());
                    if (file.exists() && file.canWrite())
                        file.delete();
                }
                backupedList = null;
                setList();
            }
        });
        builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
            }
        });
        builder.show();
    }

    private void backupApp(final ArrayList<HashMap<String, Object>> apps) {
        if (apps == null || apps.size() == 0)
            return;

        final AlertDialog.Builder builder = new AlertDialog.Builder(thisview);
        builder.setTitle("备份提示");
        builder.setMessage("\n我只能帮你提取apk文件，暂时不支持备份应用数据。\n备份完后在 /sdcard/Android/apps 下！\n\n");
        builder.setPositiveButton("确定备份", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                final StringBuffer stringBuffer = new StringBuffer();
                stringBuffer.append("mkdir /sdcard/Android/apps;\n");
                for (int i = 0; i < apps.size(); i++) {
                    stringBuffer.append("cp -f ");
                    stringBuffer.append(apps.get(i).get("path"));
                    stringBuffer.append(" /sdcard/Android/apps/");
                    stringBuffer.append(apps.get(i).get("packageName"));
                    stringBuffer.append(".apk;\n");
                }
                thisview.progressBar.setVisibility(View.VISIBLE);

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        cmdshellTools.DoCmdSync(stringBuffer.toString());
                        backupedList = null;
                        setList();
                    }
                }).start();
                //Toast.makeText(getContext(),stringBuffer.toString(),Toast.LENGTH_LONG).show();
            }
        });
        builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
            }
        });
        builder.show();
    }

    private void uninstallApp(final ArrayList<HashMap<String, Object>> apps, final boolean keepData) {
        if (apps == null || apps.size() == 0)
            return;

        AlertDialog.Builder builder = new AlertDialog.Builder(thisview);
        builder.setTitle("确定要卸载 " + apps.size() + " 个应用吗？");
        builder.setMessage("\n最好不要一口气吃成大胖子，卸载错了我可不管！\n\n");
        builder.setPositiveButton("确定卸载", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                final StringBuffer stringBuffer = new StringBuffer();
                for (int i = 0; i < apps.size(); i++) {
                    stringBuffer.append(keepData ? "pm uninstall -k " : "pm uninstall ");
                    stringBuffer.append(apps.get(i).get("packageName"));
                    stringBuffer.append(";\n");
                }
                thisview.progressBar.setVisibility(View.VISIBLE);

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        cmdshellTools.DoCmdSync(stringBuffer.toString());
                        installedList = null;
                        setList();
                    }
                }).start();
                //Toast.makeText(getContext(),stringBuffer.toString(),Toast.LENGTH_LONG).show();
            }
        });
        builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

            }
        });
        builder.show();
    }

    private void enableApp(final ArrayList<HashMap<String, Object>> apps, final boolean isSystem) {
        if (apps == null || apps.size() == 0)
            return;

        final StringBuffer stringBuffer = new StringBuffer();
        for (int i = 0; i < apps.size(); i++) {
            stringBuffer.append("pm enable ");
            stringBuffer.append(apps.get(i).get("packageName"));
            stringBuffer.append(";\n");
        }
        thisview.progressBar.setVisibility(View.VISIBLE);

        new Thread(new Runnable() {
            @Override
            public void run() {
                cmdshellTools.DoCmdSync(stringBuffer.toString());
                if (isSystem)
                    systemList = null;
                else
                    installedList = null;
                setList();
            }
        }).start();
    }

    private void disabledApp(final ArrayList<HashMap<String, Object>> apps, final boolean isSystem) {
        if (apps == null || apps.size() == 0)
            return;

        AlertDialog.Builder builder = new AlertDialog.Builder(thisview);
        builder.setTitle("确定要冻结 " + apps.size() + " 个应用吗？");
        builder.setMessage(isSystem ? "\n如果你不知道这些应用是干嘛的，千万别乱冻结，随时会挂掉的！！！\n\n" : "\n一口气干掉太多容易闪到腰哦，搞错了我可不管！！！\n\n");
        builder.setPositiveButton("确定冻结", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                final StringBuffer stringBuffer = new StringBuffer();
                for (int i = 0; i < apps.size(); i++) {
                    stringBuffer.append("pm disable ");
                    stringBuffer.append(apps.get(i).get("packageName"));
                    stringBuffer.append(";\n");
                }
                thisview.progressBar.setVisibility(View.VISIBLE);

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        cmdshellTools.DoCmdSync(stringBuffer.toString());
                        if (isSystem)
                            systemList = null;
                        else installedList = null;
                        setList();
                    }
                }).start();
                //Toast.makeText(getContext(),stringBuffer.toString(),Toast.LENGTH_LONG).show();
            }
        });
        builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

            }
        });
        builder.show();
    }

    private void deleteApp(final ArrayList<HashMap<String, Object>> apps) {
        if (apps == null || apps.size() == 0)
            return;

        final AlertDialog.Builder builder = new AlertDialog.Builder(thisview);
        builder.setTitle("删除提示");
        builder.setMessage("\n这是个非常危险的操作，如果你删错了重要的应用，手机可能会没法开机。\n\n你最好有个可用的救机方式！或者有十足的把握，确定勾选的都是无用的应用。\n\n");
        builder.setPositiveButton("确定删除", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                final StringBuffer stringBuffer = new StringBuffer();
                stringBuffer.append(Consts.INSTANCE.getMountSystemRW());
                for (int i = 0; i < apps.size(); i++) {
                    stringBuffer.append("rm -rf ");
                    stringBuffer.append(apps.get(i).get("dir"));
                    stringBuffer.append("\n");
                }
                thisview.progressBar.setVisibility(View.VISIBLE);

                //Toast.makeText(getContext(),stringBuffer.toString(),Toast.LENGTH_LONG).show();
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        cmdshellTools.DoCmdSync(stringBuffer.toString());
                        systemList = null;
                        setList();
                    }
                }).start();
            }
        });
        builder.setNegativeButton("取消（推荐）", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
            }
        });
        builder.show();
    }

    ListView tab_apps_user;
    ListView tab_apps_system;
    ListView tab_apps_backuped;

    final Handler myHandler = new Handler() {
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
        }
    };

    private void setList() {
        thisview.progressBar.setVisibility(View.VISIBLE);

        new Thread(new Runnable() {
            @Override
            public void run() {
                if (installedList == null)
                    installedList = loadList(false);
                if (systemList == null)
                    systemList = loadList(true);
                if (backupedList == null)
                    backupedList = loadbackupedList();

                setListData(installedList, tab_apps_user);
                setListData(systemList, tab_apps_system);
                setListData(backupedList, tab_apps_backuped);
            }
        }).start();
    }

    void setListData(final ArrayList<HashMap<String, Object>> dl, final ListView lv) {
        myHandler.post(new Runnable() {
            @Override
            public void run() {
                thisview.progressBar.setVisibility(View.GONE);
                lv.setAdapter(new list_adapter2(getContext(), dl));
            }
        });
    }

    PackageManager packageManager;
    List<ApplicationInfo> packageInfos;
    ArrayList<HashMap<String, Object>> installedList;
    ArrayList<HashMap<String, Object>> systemList;
    ArrayList<HashMap<String, Object>> backupedList;
    ArrayList<String> allApps = new ArrayList<>();

    ArrayList<String> ignore = new ArrayList<String>() {{
        add("com.android.mms");
        add("com.android.providers.media");
        add("com.android.packageinstaller");
        add("com.miui.packageinstaller");
        add("com.google.android.packageinstaller");
        add("com.android.defcountainer");
        add("com.android.settings");
        add("com.android.providers.settings");
        add("com.android.vpndialogs");
        add("com.android.shell");
        add("com.android.phone");
        add("com.android.onetimeinitializer");
        add("com.android.providers.contacts");
        add("com.android.providers.blockednumber");
        add("com.android.contacts");
        add("com.android.providers.telephony");
        add("com.android.incallui");
        add("com.android.systemui");
        add("com.android.providers.downloads.ui");
        add("com.android.providers.downloads");
        add("android");
        add("com.android.carrierconfig");
        add("com.android.frameworks.telresources");
        add("com.android.keyguard");
        add("com.android.wallpapercropper");
        add("com.miui.rom");
        add("com.miui.system");
        add("com.qualcomm.location");
        add("com.google.android.webview");
        add("com.android.webview");
    }};

    private ArrayList loadList(boolean systemApp) {
        packageManager = thisview.getPackageManager();
        packageInfos = packageManager.getInstalledApplications(0);

        ArrayList list = new ArrayList<>();/*在数组中存放数据*/
        for (int i = 0; i < packageInfos.size(); i++) {
            ApplicationInfo packageInfo = packageInfos.get(i);

            if (ignore.contains(packageInfo.packageName)) {
                continue;
            }

            if ((!systemApp) && packageInfo.sourceDir.startsWith("/system"))
                continue;
            if (systemApp && packageInfo.sourceDir.startsWith("/data"))
                continue;

            File file = new File(packageInfo.publicSourceDir);
            if (!file.exists())
                continue;

            HashMap<String, Object> item = new HashMap<String, Object>();
            Drawable d = packageInfo.loadIcon(packageManager);
            item.put("icon", d);
            item.put("select_state", false);
            item.put("dir", packageInfo.sourceDir);
            item.put("enabled", packageInfo.enabled);
            item.put("enabled_state", packageInfo.enabled ? null : "已冻结");

            item.put("name", packageInfo.loadLabel(packageManager));
            item.put("packageName", packageInfo.packageName);
            item.put("path", packageInfo.sourceDir);
            item.put("dir", file.getParent());
            if (!allApps.contains(packageInfo.packageName))
                allApps.add(packageInfo.packageName);
            list.add(item);
        }
        return list;
    }

    private ArrayList loadbackupedList() {
        ArrayList list = new ArrayList<>();/*在数组中存放数据*/
        File dir = new File("/sdcard/Android/apps");
        if (!dir.exists())
            return list;

        if (!dir.isDirectory()) {
            dir.delete();
            dir.mkdirs();
            return list;
        }

        String[] files = dir.list(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.toLowerCase().endsWith(".apk");
            }
        });

        for (int i = 0; i < files.length; i++) {
            String absPath = "/sdcard/Android/apps/" + files[i];
            try {
                PackageManager packageManager = getContext().getPackageManager();
                PackageInfo packageInfo = packageManager.getPackageArchiveInfo(absPath, PackageManager.GET_ACTIVITIES);
                if (packageInfo != null) {
                    ApplicationInfo applicationInfo = packageInfo.applicationInfo;
                    applicationInfo.sourceDir = absPath;
                    applicationInfo.publicSourceDir = absPath;

                    HashMap<String, Object> item = new HashMap<String, Object>();
                    Drawable d = applicationInfo.loadIcon(packageManager);
                    item.put("icon", d);
                    item.put("select_state", false);
                    item.put("name", applicationInfo.loadLabel(packageManager) + "  (" + packageInfo.versionCode + ")");
                    item.put("packageName", applicationInfo.packageName);
                    item.put("path", applicationInfo.sourceDir);
                    item.put("enabled_state", allApps.contains(applicationInfo.packageName) ? "已安装" : null);
                    list.add(item);
                }
            } catch (Exception ex) {

            }
        }

        return list;
    }
}

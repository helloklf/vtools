package com.omarea.vboot;

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleAdapter;

import com.omarea.shared.Consts;
import com.omarea.shared.AppShared;
import com.omarea.shared.cmd_shellTools;

import java.util.ArrayList;
import java.util.HashMap;


public class fragment_addin extends Fragment {

    public fragment_addin() {
        // Required empty public constructor
    }


    cmd_shellTools cmdshellTools = null;
    activity_main thisview = null;

    public static Fragment Create(activity_main thisView, cmd_shellTools cmdshellTools) {
        fragment_addin fragment = new fragment_addin();
        fragment.cmdshellTools = cmdshellTools;
        fragment.thisview = thisView;
        return fragment;
    }

    public HashMap<String, Object> createItem(String title, String desc) {
        HashMap<String, Object> item = new HashMap<String, Object>();
        item.put("Title", title);
        item.put("Desc", desc);
        return item;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.layout_addin, container, false);
    }


    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        ListView listView = (ListView) view.findViewById(R.id.addin_action_listview);
        final ArrayList<HashMap<String, Object>> listItem = new ArrayList<HashMap<String, Object>>();/*在数组中存放数据*/

        listItem.add(createItem("内存清理", "Linux标准缓存清理命令：echo 3 > /proc/sys/vm/drop_caches"));
        listItem.add(createItem("开启ZRAM 500M", "该功能需要内核支持。size=0.5GB swappiness=100"));
        listItem.add(createItem("开启ZRAM 1GB", "该功能需要内核支持。size=1.0GB swappiness=100"));
        listItem.add(createItem("开启ZRAM 2GB", "该功能需要内核支持。size=1.8GB swappiness=100"));
        listItem.add(createItem("调整DPI为410", "部分ROM不支持调整，可能出现UI错误。需要重启。"));
        listItem.add(createItem("调整DPI为440", "部分ROM不支持调整，可能出现UI错误。需要重启。"));
        listItem.add(createItem("调整DPI为480", "部分ROM不支持调整，可能出现UI错误。需要重启。"));
        listItem.add(createItem("fstrim", "对磁盘进行fstrim操作，也许会解决读写速度变慢的问题。"));
        listItem.add(createItem("挂载System可读写", "将System重新挂载为可读写状态（如果System未结果，此操作将无效）。"));
        listItem.add(createItem("QQ净化", "干掉QQ个性气泡、字体、头像挂件，会重启QQ"));
        listItem.add(createItem("QQ净化恢复", "恢复QQ个性气泡、字体、头像挂件，会重启QQ"));
        listItem.add(createItem("干掉温控模块", "可能会对系统造成一些影响，请谨慎使用，需要重启手机。此功能对小米5较新系统无效"));
        listItem.add(createItem("恢复温控模块", "需要重启手机"));
        listItem.add(createItem("删除锁屏密码", "如果你忘了锁屏密码，或者恢复系统后密码不正确，这能帮你解决。会重启手机"));
        listItem.add(createItem("MIUI一键精简", "删除或冻结MIUI中一些内置的无用软件。"));

        SimpleAdapter mSimpleAdapter = new SimpleAdapter(
                view.getContext(), listItem,
                R.layout.action_row_item,
                new String[]{"Title", "Desc"},
                new int[]{R.id.Title, R.id.Desc}
        );
        listView.setAdapter(mSimpleAdapter);


        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, final View view, final int position, long id) {

                AlertDialog.Builder builder = new AlertDialog.Builder(thisview);
                builder.setTitle("执行这个脚本？");
                builder.setNegativeButton(android.R.string.cancel, null);
                builder.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        executeScript(view, position);
                    }
                });
                builder.setMessage(
                        listItem.get(position).get("Title") + "：" + listItem.get(position).get("Desc") +
                                "\n\n请确保你已了解此脚本的用途，并清除对设备的影响");
                builder.create().show();
            }
        });
    }

    private void executeScript(View view, int position) {
        StringBuilder stringBuilder = new StringBuilder();
        switch (position) {
            case 0: {
                stringBuilder.append("echo 3 > /proc/sys/vm/drop_caches");
                break;
            }
            case 1: {
                stringBuilder.append(
                        "swapoff /dev/block/zram0\n" +
                                "echo 1 > /sys/block/zram0/reset\n" +
                                "echo 597000000 > /sys/block/zram0/disksize\n" +
                                "mkswap /dev/block/zram0 &> /dev/null\n" +
                                "swapon /dev/block/zram0 &> /dev/null\n" +
                                "echo 100 > /proc/sys/vm/swappiness\n");
                break;
            }
            case 2: {
                stringBuilder.append(
                        "swapoff /dev/block/zram0\n" +
                                "echo 1 > /sys/block/zram0/reset\n" +
                                "echo 1097000000 > /sys/block/zram0/disksize\n" +
                                "mkswap /dev/block/zram0 &> /dev/null\n" +
                                "swapon /dev/block/zram0 &> /dev/null\n" +
                                "echo 100 > /proc/sys/vm/swappiness\n");
                break;
            }
            case 3: {
                stringBuilder.append(
                        "swapoff /dev/block/zram0\n" +
                                "echo 1 > /sys/block/zram0/reset\n" +
                                "echo 2097000000 > /sys/block/zram0/disksize\n" +
                                "mkswap /dev/block/zram0 &> /dev/null\n" +
                                "swapon /dev/block/zram0 &> /dev/null\n" +
                                "echo 100 > /proc/sys/vm/swappiness\n");
                break;
            }
            case 4: {
                stringBuilder.append(Consts.MountSystemRW);
                stringBuilder.append("sed '/ro.sf.lcd_density=/'d /system/build.prop > /data/build.prop\n");
                stringBuilder.append("sed '$aro.sf.lcd_density=410' /data/build.prop > /data/build2.prop\n");
                stringBuilder.append("cp /system/build.prop /system/build.bak.prop\n");
                stringBuilder.append("cp /data/build2.prop /system/build.prop\n");
                stringBuilder.append("rm /data/build.prop\n");
                stringBuilder.append("rm /data/build2.prop\n");
                stringBuilder.append("chmod 0644 /system/build.prop\n");
                stringBuilder.append("wm size 1080x1920\n");
                stringBuilder.append("sync\n");
                stringBuilder.append("reboot\n");
                break;
            }
            case 5: {
                stringBuilder.append(Consts.MountSystemRW);
                stringBuilder.append("sed '/ro.sf.lcd_density=/'d /system/build.prop > /data/build.prop\n");
                stringBuilder.append("sed '$aro.sf.lcd_density=440' /data/build.prop > /data/build2.prop\n");
                stringBuilder.append("cp /system/build.prop /system/build.bak.prop\n");
                stringBuilder.append("cp /data/build2.prop /system/build.prop\n");
                stringBuilder.append("rm /data/build.prop\n");
                stringBuilder.append("rm /data/build2.prop\n");
                stringBuilder.append("chmod 0644 /system/build.prop\n");
                stringBuilder.append("wm size 1080x1920\n");
                stringBuilder.append("sync\n");
                stringBuilder.append("reboot\n");
                break;
            }
            case 6: {
                stringBuilder.append(Consts.MountSystemRW);
                stringBuilder.append("sed '/ro.sf.lcd_density=/'d /system/build.prop > /data/build.prop\n");
                stringBuilder.append("sed '$aro.sf.lcd_density=480' /data/build.prop > /data/build2.prop\n");
                stringBuilder.append("cp /system/build.prop /system/build.bak.prop\n");
                stringBuilder.append("cp /data/build2.prop /system/build.prop\n");
                stringBuilder.append("rm /data/build.prop\n");
                stringBuilder.append("rm /data/build2.prop\n");
                stringBuilder.append("chmod 0644 /system/build.prop\n");
                stringBuilder.append("wm size 1080x1920\n");
                stringBuilder.append("sync\n");
                stringBuilder.append("reboot\n");
                break;
            }
            case 7: {
                AppShared.WriteFile(getContext().getAssets(), "fstrim", "fstrim");
                stringBuilder.append("cp " + AppShared.baseUrl + "fstrim" + " /cache/fstrim\n");
                stringBuilder.append("chmod 0777 /cache/fstrim\n");
                stringBuilder.append("/cache/fstrim -v /data\n");
                stringBuilder.append("/cache/fstrim -v /cache\n");
                stringBuilder.append("/cache/fstrim -v /system\n");
                stringBuilder.append("/cache/fstrim -v /MainData\n");
                break;
            }
            case 8: {
                stringBuilder.append(Consts.MountSystemRW);
                break;
            }
            case 9: {
                stringBuilder.append(Consts.RMQQStyles);
                break;
            }
            case 10: {
                stringBuilder.append(Consts.ResetQQStyles);
                break;
            }
            case 11: {
                stringBuilder.append(Consts.RMThermal);
                break;
            }
            case 12: {
                stringBuilder.append(Consts.MountSystemRW);
                stringBuilder.append(Consts.ResetThermal);
                break;
            }
            case 13: {
                stringBuilder.append(Consts.DeleteLockPwd);
                break;
            }
            case 14: {
                stringBuilder.append(Consts.MountSystemRW);
                stringBuilder.append(Consts.MiuiUninstall);
                break;
            }
        }
        cmdshellTools.DoCmd(stringBuilder.toString());
        Snackbar.make(view, "命令已执行！", Snackbar.LENGTH_SHORT).show();
    }
}

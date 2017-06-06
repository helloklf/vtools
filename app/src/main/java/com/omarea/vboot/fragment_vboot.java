package com.omarea.vboot;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleAdapter;

import com.omarea.shared.cmd_shellTools;
import com.omarea.shell.Busybox;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;


public class fragment_vboot extends Fragment {
    public fragment_vboot() {
        // Required empty public constructor
    }

    cmd_shellTools cmdshellTools = null;
    AppCompatActivity thisview = null;

    public static Fragment Create(AppCompatActivity thisView, cmd_shellTools cmdshellTools) {
        fragment_vboot fragment = new fragment_vboot();
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
        return inflater.inflate(R.layout.layout_vboot, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        ListView listView = (ListView) view.findViewById(R.id.vboot_action_listview);
        ArrayList<HashMap<String, Object>> listItem = new ArrayList<HashMap<String, Object>>();/*在数组中存放数据*/

        listItem.add(createItem("清空系统二", "清空系统二的用户数据，以便于更换系统二或修复错误"));
        listItem.add(createItem("压缩系统", "缩小系统二System分区大小，以节省空间"));
        listItem.add(createItem("调整容量", "调整系统二可使用的空间大小"));
        listItem.add(createItem("卸载双系统", "彻底删除系统二以释放空间，或重新安装系统二"));

        SimpleAdapter mSimpleAdapter = new SimpleAdapter(
                view.getContext(), listItem,
                R.layout.action_row_item,
                new String[]{"Title", "Desc"},
                new int[]{R.id.Title, R.id.Desc}
        );
        listView.setAdapter(mSimpleAdapter);


        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, final View view, int position, long id) {
                //setTitle("你点击了第"+position+"行");
                switch (position) {
                    case 0: {
                        //清空vboot
                        if (!cmdshellTools.CurrentSystemOne()) {
                            Snackbar.make(getView(), "无法操作当前正在使用的系统，请切换到系统一后进行操作！", Snackbar.LENGTH_SHORT).show();
                            return;
                        }
                        AlertDialog.Builder builder = new AlertDialog.Builder(thisview);
                        builder.setTitle("确定要清空？");
                        builder.setMessage("此操作将清除系统二的设置和已安装程序，请确保您已备份好重要数据！！！\n");
                        builder.setNeutralButton(android.R.string.cancel, null);
                        builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                cmdshellTools.WipeVBOOTData();
                            }
                        });
                        builder.create().show();
                        break;
                    }
                    case 1: {
                        AlertDialog.Builder builder = new AlertDialog.Builder(thisview);
                        builder.setTitle("请选择压缩模式");
                        builder.setNeutralButton(android.R.string.cancel, null);
                        builder.setNegativeButton("完全压缩", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                cmdshellTools.ZipSystem();
                            }
                        });
                        builder.setPositiveButton("留100M", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if (!new Busybox().IsBusyboxInstalled()) {
                                    Snackbar.make(view, "Busybox未安装，无法进行此操作！", Snackbar.LENGTH_SHORT).show();
                                    return;
                                }
                                try {
                                    cmdshellTools.ZipSystem2();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }
                        });
                        builder.setMessage("此操作会压缩VBOOT系统二的System分区可用空间。\n如果选择完全压缩，系统二将无法刷入补丁包。\n建议预留100M，方便后续安装补资源！\n");
                        builder.create().show();
                        break;
                    }
                    case 2: {
                        //调整vboot容量
                        if (cmdshellTools.CurrentSystemOne()) {
                            if (!new Busybox().IsBusyboxInstalled()) {
                                Snackbar.make(view, "Busybox未安装，无法进行此操作！", Snackbar.LENGTH_SHORT).show();
                                return;
                            }
                            Intent intent = new Intent(thisview, vbootresize.class);
                            startActivity(intent);
                        } else {
                            Snackbar.make(view, "请在系统一下进行此操作！", Snackbar.LENGTH_SHORT).show();
                        }
                        break;
                    }
                    case 3: {
                        if (cmdshellTools.CurrentSystemOne()) {
                            AlertDialog.Builder builder = new AlertDialog.Builder(thisview);
                            builder.setTitle("真的卸载？");
                            builder.setNegativeButton(android.R.string.cancel, null);
                            //builder.setNeutralButton(android.R.string.yes, null);
                            builder.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    cmdshellTools.UnInstallVBOOT();
                                }
                            });
                            builder.setMessage("确保您已经备份好重要的数据，并不需要再使用系统二！\n");
                            builder.create().show();
                        } else {
                            Snackbar.make(view, "请在系统一下进行此操作！", Snackbar.LENGTH_SHORT).show();
                        }
                        break;
                    }
                    default: {
                        Snackbar.make(view, "暂不支持此操作！", Snackbar.LENGTH_LONG).show();
                    }
                }
            }
        });
    }
}

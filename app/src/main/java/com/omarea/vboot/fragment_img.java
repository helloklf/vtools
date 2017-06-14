package com.omarea.vboot;

import android.content.DialogInterface;
import android.content.Intent;
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

import com.omarea.shared.cmd_shellTools;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;


public class fragment_img extends Fragment {

    public fragment_img() {
        // Required empty public constructor
    }


    cmd_shellTools cmdshellTools = null;
    activity_main thisview = null;

    public static Fragment Create(activity_main thisView, cmd_shellTools cmdshellTools) {
        fragment_img fragment = new fragment_img();
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
        return inflater.inflate(R.layout.layout_img, container, false);
    }


    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        ListView listView = (ListView) view.findViewById(R.id.img_action_listview);
        ArrayList<HashMap<String, Object>> listItem = new ArrayList<HashMap<String, Object>>();/*在数组中存放数据*/

        listItem.add(createItem("导出Boot", "备份当前使用中的Boot为img，复制到/sdcard/boot.img"));
        listItem.add(createItem("刷入Boot", "选择刷入boot.img"));
        listItem.add(createItem("导出Recovery", "备份当前使用中的导出Recovery为img，复制到/sdcard/recovery.img"));
        listItem.add(createItem("刷入Recovery", "选择刷入recovery.img"));
        listItem.add(createItem("ZIP打包", "强大的卡刷包制作工具，一键打包调频、Boot、ROM"));

        SimpleAdapter mSimpleAdapter = new SimpleAdapter(
                view.getContext(), listItem,
                R.layout.action_row_item,
                new String[]{"Title", "Desc"},
                new int[]{R.id.Title, R.id.Desc}
        );
        listView.setAdapter(mSimpleAdapter);


        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                //setTitle("你点击了第"+position+"行");
                switch (position) {
                    case 0: {
                        if (new File("/sdcard/boot.img").exists()) {
                            AlertDialog.Builder builder = new AlertDialog.Builder(thisview);
                            builder.setTitle("覆盖文件？");
                            builder.setNegativeButton(android.R.string.cancel, null);
                            //builder.setNeutralButton(android.R.string.yes, null);
                            builder.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    //导出boot
                                    cmdshellTools.SaveBoot("");
                                }
                            });
                            builder.setMessage("/sdcard/boot.img已经存在，是否覆盖已有文件？\n");
                            builder.create().show();
                        } else {
                            //导出boot
                            cmdshellTools.SaveBoot("");
                        }
                        break;
                    }
                    case 1: {
                        //刷入boot
                        AlertDialog.Builder builder = new AlertDialog.Builder(thisview);
                        builder.setTitle("确定刷入BOOT？");
                        builder.setNegativeButton(android.R.string.cancel, null);
                        //builder.setNeutralButton(android.R.string.yes, null);
                        builder.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                                intent.setType("*/img");
                                intent.addCategory(Intent.CATEGORY_OPENABLE);
                                startActivityForResult(intent, 1);
                                thisview.fileSelectType = activity_main.FileSelectType.BootFlash;
                            }
                        });
                        builder.setMessage("此操作非常危险，刷入错误的BOOT会导致系统无法正常启动，或破坏双系统切换。如果你只是想正常切换系统，请勿使用该功能！\n");
                        builder.create().show();
                        break;
                    }
                    case 2: {
                        if (new File("/sdcard/recovery.img").exists()) {
                            AlertDialog.Builder builder = new AlertDialog.Builder(thisview);
                            builder.setTitle("覆盖文件？");
                            builder.setNegativeButton(android.R.string.cancel, null);
                            //builder.setNeutralButton(android.R.string.yes, null);
                            builder.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    //导出rec
                                    cmdshellTools.SaveRecovery("");
                                }
                            });
                            builder.setMessage("/sdcard/recovery.img已经存在，是否覆盖已有文件？\n");
                            builder.create().show();
                        } else {
                            //导出rec
                            cmdshellTools.SaveRecovery("");
                        }
                        break;
                    }
                    case 3: {
                        //刷入rec
                        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                        intent.setType("*/img");//设置MIME类型
                        intent.addCategory(Intent.CATEGORY_OPENABLE);
                        startActivityForResult(intent, 1);
                        thisview.fileSelectType = activity_main.FileSelectType.RecFlash;
                        break;
                    }
                    case 4: {
                        //打包rom
                        Intent intent = new Intent(thisview, rom2zip.class);
                        startActivity(intent);
                        break;
                    }
                    default: {
                        Snackbar.make(getView(), "暂不支持此操作！", Snackbar.LENGTH_LONG).show();
                    }
                }
            }
        });
    }

}

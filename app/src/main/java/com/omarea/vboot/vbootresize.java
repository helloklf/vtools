package com.omarea.vboot;


import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.omarea.shared.cmd_shellTools;

import java.io.IOException;

public class vbootresize extends AppCompatActivity {
    vbootresize view;
    cmd_shellTools cmdshellTools;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vbootresize);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar1);
        setSupportActionBar(toolbar);

        view = this;
        final ProgressBar progressBar = ((ProgressBar) (findViewById(R.id.progressBar2)));
        cmdshellTools = new cmd_shellTools(this, progressBar);
        final CheckBox resizesavedata = (CheckBox) findViewById(R.id.resizesavedata);

        resizesavedata.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!resizesavedata.isChecked()) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(view);
                    builder.setTitle("数据安全警告！");
                    builder.setMessage("真的要以不保存数据方式调整系统二容量？\n这可以加快速度，但会导致系统二用户数据丢失。");
                    builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            resizesavedata.setChecked(true);
                        }
                    });
                    builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
                        @Override
                        public void onCancel(DialogInterface dialog) {
                            resizesavedata.setChecked(true);
                        }
                    });
                    builder.setPositiveButton(android.R.string.yes, null);
                    builder.create().show();
                }
            }
        });

        findViewById(R.id.CommitBtn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String text = ((EditText) (findViewById(R.id.DataSizeValue))).getText().toString();
                if (text != "" && text.trim() != "") {
                    final int value = Integer.parseInt(text);
                    if (value < 1280) {
                        Snackbar.make(v, "抱歉，至少应为系统二分配1.3G空间，否则可能无法正常启动！", Snackbar.LENGTH_SHORT).show();
                    } else {
                        AlertDialog.Builder builder = new AlertDialog.Builder(view);
                        if (resizesavedata.isChecked()) {
                            builder.setTitle("确定调整容量？");
                            builder.setMessage("为确保资料安全，建议您先备份系统二上的重要数据！\n");
                            builder.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    cmdshellTools.VBOOTDataReSize(value);
                                }
                            });
                        } else {
                            builder.setTitle("确定不保留数据？");
                            builder.setMessage("调整系统二容量且不保留数据？\n请确保您已备份系统二上的重要数据！！！\n");
                            builder.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    cmdshellTools.CreateVBOOTData(value);
                                }
                            });
                        }
                        builder.setNegativeButton(android.R.string.cancel, null);
                        builder.create().show();
                    }
                }
            }
        });
        try {
            final TextView dataimgusesize = ((TextView) findViewById(R.id.dataimgusesize));
            final TextView dataimgfreesize = ((TextView) findViewById(R.id.dataimgfreesize));
            final TextView dataimgtotalsize = ((TextView) findViewById(R.id.dataimgtotalsize));
            final TextView sysdatafreesize = ((TextView) findViewById(R.id.sysdatafreesize));


            final Handler myHandler = new Handler() {
                public void handleMessage(Message msg) {
                    super.handleMessage(msg);
                }
            };

            progressBar.setVisibility(View.VISIBLE);
            Toast.makeText(this, "正在检查和修复系统二Data，请稍等！", Toast.LENGTH_SHORT).show();
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        final int useSize = cmdshellTools.GetImgUseDataSizeMB();
                        final int freeSize = cmdshellTools.GetImgFreeSizeMB();
                        final int totalSize = cmdshellTools.GetVBOOTDataSize();
                        final long sdcardSize = cmdshellTools.GetSDFreeSizeMB();
                        myHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                dataimgusesize.setText("Data.img已用：" + useSize + "MB");
                                dataimgfreesize.setText("Data.img可用：" + freeSize + "MB");
                                dataimgtotalsize.setText("Data.img总计：" + totalSize + "MB");
                                sysdatafreesize.setText("SD Card  可用：" + sdcardSize + "MB");
                                progressBar.setVisibility(View.GONE);
                            }
                        });
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }).start();

            //dataimgusesize.setText("Data.img已用："+cmdshellTools.GetImgUseDataSizeMB()+"MB");
            //dataimgfreesize.setText("Data.img可用："+cmdshellTools.GetImgFreeSizeMB()+"MB");
            //dataimgtotalsize.setText("Data.img总计："+cmdshellTools.GetVBOOTDataSize()+"MB");
            //sysdatafreesize.setText("SD Card  可用："+cmdshellTools.GetSDFreeSizeMB()+"MB");
        } catch (Exception ex) {

        }
    }


}

package com.omarea.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Handler;
import android.support.design.widget.Snackbar;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.omarea.vtools.R;

import java.io.File;
import java.util.ArrayList;

public class AdapterFileSelector extends BaseAdapter {
    private File[] fileArray;
    private Runnable fileSelected;
    private File currentDir;
    private File selectedFile;
    private Handler handler = new Handler();
    private ProgressBarDialog progressBarDialog;

    public AdapterFileSelector(File rootDir, Runnable fileSelected, ProgressBarDialog progressBarDialog) {
        this.fileSelected = fileSelected;
        this.progressBarDialog = progressBarDialog;
        loadDir(rootDir);
    }

    private void loadDir (final File dir) {
        progressBarDialog.showDialog("加载中...");
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (dir.exists() && dir.canRead()) {
                    File[] files = dir.listFiles();
                    for (int i = 0; i < files.length; i++) {
                        for (int j = i + 1; j < files.length; j ++) {
                            if ((files[j].isDirectory() && files[i].isFile())) {
                                File t = files[i];
                                files[i] = files[j];
                                files[j] = t;
                            } else if (files[j].isDirectory() == files[i].isDirectory()&& (files[j].getName().compareTo(files[i].getName()) < 0)) {
                                File t = files[i];
                                files[i] = files[j];
                                files[j] = t;
                            }
                        }
                    }
                    fileArray = files;
                }
                currentDir = dir;
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        notifyDataSetChanged();
                        progressBarDialog.hideDialog();
                    }
                });
            }
        }).start();
    }

    public boolean goParent () {
        File parent = new File(currentDir.getParent());
        if (parent.exists() && parent.canRead()) {
            loadDir(parent);
            return true;
        }
        return false;
    }

    @Override
    public int getCount() {
        if (fileArray == null) {
            return 0;
        }
        return fileArray.length;
    }

    @Override
    public Object getItem(int position) {
        return fileArray[position];
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final View view;
        final File file = (File) getItem(position);
        if (file.isDirectory()) {
            view = View.inflate(parent.getContext(), R.layout.dir_item, null);
            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    File[] files = file.listFiles();
                    if (files != null && files.length > 0) {
                        loadDir(file);
                    } else {
                        Snackbar.make(view, "该目录下没有文件！", Snackbar.LENGTH_SHORT).show();
                    }
                }
            });
        } else {
            view = View.inflate(parent.getContext(), R.layout.file_item, null);
            ((TextView) (view.findViewById(R.id.ItemText))).setText(String.format("%.2f", (file.length() / 1024 / 1024.0)) + "MB");
            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    new AlertDialog.Builder(view.getContext()).setTitle("选定文件？")
                            .setMessage(file.getAbsolutePath())
                            .setPositiveButton(R.string.btn_confirm, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    selectedFile = file;
                                    fileSelected.run();
                                }
                            })
                            .setNegativeButton(R.string.btn_cancel, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {

                                }
                            }).create().show();
                }
            });
        }
        ((TextView) (view.findViewById(R.id.ItemTitle))).setText(file.getName());
        return view;
    }

    public File getSelectedFile () {
        return this.selectedFile;
    }
}

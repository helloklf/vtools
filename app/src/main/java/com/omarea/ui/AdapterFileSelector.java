package com.omarea.ui;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Handler;
import android.support.design.widget.Snackbar;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.omarea.common.ui.ProgressBarDialog;
import com.omarea.vtools.R;

import java.io.File;
import java.io.FileFilter;

public class AdapterFileSelector extends BaseAdapter {
    private File[] fileArray;
    private Runnable fileSelected;
    private Runnable fileDelete;
    private File currentDir;
    private File selectedFile;
    private Handler handler = new Handler();
    private ProgressBarDialog progressBarDialog;
    private String extension;
    private boolean clickSelected = true; // 点击选中
    private boolean longClickDelete = false; // 长按选项
    private boolean hasParent = false; // 是否还有父级
    private String rootDir = "/"; // 根目录
    private boolean leaveRootDir = true; // 是否允许离开设定的rootDir到更父级的目录去

    public AdapterFileSelector(File rootDir, Runnable fileSelected, ProgressBarDialog progressBarDialog, String extension) {
        init(rootDir, fileSelected, progressBarDialog, extension);
    }

    public AdapterFileSelector(File rootDir, Runnable fileSelected, ProgressBarDialog progressBarDialog, String extension, boolean clickSelected, boolean longClickDelete, Runnable fileDelete, boolean leaveRootDir) {
        this.leaveRootDir = leaveRootDir;
        this.clickSelected = clickSelected;
        this.longClickDelete = longClickDelete;
        this.fileDelete = fileDelete;
        init(rootDir, fileSelected, progressBarDialog, extension);
    }

    private void init(File rootDir, Runnable fileSelected, ProgressBarDialog progressBarDialog, String extension) {
        this.rootDir = rootDir.getAbsolutePath();
        this.fileSelected = fileSelected;
        this.progressBarDialog = progressBarDialog;
        if (extension != null) {
            if (extension.startsWith(".")) {
                this.extension = extension;
            } else {
                this.extension = "." + extension;
            }
        }
        loadDir(rootDir);
    }

    private void loadDir(final File dir) {
        progressBarDialog.showDialog("加载中...");
        new Thread(new Runnable() {
            @Override
            public void run() {
                File parent = new File(dir.getParent());
                String parentPath = parent.getAbsolutePath();
                hasParent = parent.exists() && parent.canRead() && (leaveRootDir || !(rootDir.startsWith(parentPath) && rootDir.length() > parentPath.length()));

                if (dir.exists() && dir.canRead()) {

                    File[] files = dir.listFiles(new FileFilter() {
                        @Override
                        public boolean accept(File pathname) {
                            return pathname.exists() && (!pathname.isFile() || extension == null || extension.isEmpty() || pathname.getName().endsWith(extension));
                        }
                    });

                    // 文件排序
                    for (int i = 0; i < files.length; i++) {
                        for (int j = i + 1; j < files.length; j++) {
                            if ((files[j].isDirectory() && files[i].isFile())) {
                                File t = files[i];
                                files[i] = files[j];
                                files[j] = t;
                            } else if (files[j].isDirectory() == files[i].isDirectory() && (files[j].getName().toLowerCase().compareTo(files[i].getName().toLowerCase()) < 0)) {
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

    public boolean goParent() {
        if (hasParent) {
            loadDir(new File(currentDir.getParent()));
            return true;
        }
        return false;
    }

    @Override
    public int getCount() {
        if (hasParent) {
            if (fileArray == null) {
                return 1;
            }
            return fileArray.length + 1;
        } else {
            if (fileArray == null) {
                return 0;
            }
            return fileArray.length;
        }
    }

    public void refresh() {
        if (this.currentDir != null) {
            this.loadDir(currentDir);
        }
    }

    @Override
    public Object getItem(int position) {
        if (hasParent) {
            if (position == 0) {
                return new File(currentDir.getParent());
            } else {
                return fileArray[position - 1];
            }
        } else {
            return fileArray[position];
        }
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final View view;
        if (hasParent && position == 0) {
            view = View.inflate(parent.getContext(), R.layout.list_item_dir, null);
            ((TextView) (view.findViewById(R.id.ItemTitle))).setText("...");
            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    goParent();
                }
            });
            return view;
        } else {
            final File file = (File) getItem(position);
            if (file.isDirectory()) {
                view = View.inflate(parent.getContext(), R.layout.list_item_dir, null);
                view.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (!file.exists()) {
                            Toast.makeText(view.getContext(), "所选的文件已被删除，请重新选择！", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        File[] files = file.listFiles();
                        if (files != null && files.length > 0) {
                            loadDir(file);
                        } else {
                            Snackbar.make(view, "该目录下没有文件！", Snackbar.LENGTH_SHORT).show();
                        }
                    }
                });
            } else {
                view = View.inflate(parent.getContext(), R.layout.list_item_file, null);
                long fileLength = file.length();
                String fileSize;
                if (fileLength < 1024) {
                    fileSize = fileLength + "B";
                } else if (fileLength < 1048576) {
                    fileSize = String.format("%sKB", String.format("%.2f", (file.length() / 1024.0)));
                } else if (fileLength < 1073741824) {
                    fileSize = String.format("%sMB", String.format("%.2f", (file.length() / 1048576.0)));
                } else {
                    fileSize = String.format("%sGB", String.format("%.2f", (file.length() / 1073741824.0)));
                }

                ((TextView) (view.findViewById(R.id.ItemText))).setText(fileSize);
                if (clickSelected) {
                    view.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            new AlertDialog.Builder(view.getContext()).setTitle("选定文件？")
                                    .setMessage(file.getAbsolutePath())
                                    .setPositiveButton(R.string.btn_confirm, new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            if (!file.exists()) {
                                                Toast.makeText(view.getContext(), "所选的文件已被删除，请重新选择！", Toast.LENGTH_SHORT).show();
                                                return;
                                            }
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
            }
            if (longClickDelete) {
                view.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View v) {
                        new AlertDialog.Builder(view.getContext()).setTitle("删除所选文件？")
                                .setMessage(file.getAbsolutePath())
                                .setPositiveButton(R.string.btn_confirm, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        if (!file.exists()) {
                                            Toast.makeText(view.getContext(), "所选的文件已被删除，请重新选择！", Toast.LENGTH_SHORT).show();
                                            return;
                                        }
                                        selectedFile = file;
                                        fileDelete.run();
                                    }
                                })
                                .setNegativeButton(R.string.btn_cancel, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {

                                    }
                                }).create().show();
                        return true;
                    }
                });
            }
            ((TextView) (view.findViewById(R.id.ItemTitle))).setText(file.getName());
            return view;
        }
    }

    public File getSelectedFile() {
        return this.selectedFile;
    }
}

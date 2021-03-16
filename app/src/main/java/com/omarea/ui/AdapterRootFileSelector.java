package com.omarea.ui;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.snackbar.Snackbar;
import com.omarea.common.shared.RootFileInfo;
import com.omarea.common.ui.DialogHelper;
import com.omarea.common.ui.ProgressBarDialog;
import com.omarea.vtools.R;

import java.util.ArrayList;

public class AdapterRootFileSelector extends BaseAdapter {
    private ArrayList<RootFileInfo> fileArray;
    private Runnable fileSelected;
    private Runnable fileDelete;
    private RootFileInfo currentDir;
    private RootFileInfo selectedFile;
    private Handler handler = new Handler(Looper.getMainLooper());
    private ProgressBarDialog progressBarDialog;
    private String extension;
    private boolean clickSelected = true; // 点击选中
    private boolean longClickDelete = false; // 长按选项
    private boolean hasParent = false; // 是否还有父级
    private String rootDir = "/"; // 根目录
    private boolean leaveRootDir = true; // 是否允许离开设定的rootDir到更父级的目录去

    public AdapterRootFileSelector(RootFileInfo rootDir, Runnable fileSelected, ProgressBarDialog progressBarDialog, String extension) {
        init(rootDir, fileSelected, progressBarDialog, extension);
    }

    public AdapterRootFileSelector(RootFileInfo rootDir,
                                   Runnable fileSelected,
                                   ProgressBarDialog progressBarDialog,
                                   String extension, boolean clickSelected,
                                   boolean longClickDelete,
                                   Runnable fileDelete,
                                   boolean leaveRootDir) {
        this.leaveRootDir = leaveRootDir;
        this.clickSelected = clickSelected;
        this.longClickDelete = longClickDelete;
        this.fileDelete = fileDelete;
        init(rootDir, fileSelected, progressBarDialog, extension);
    }

    private void init(RootFileInfo rootDir, Runnable fileSelected, ProgressBarDialog progressBarDialog, String extension) {
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

    private void loadDir(final RootFileInfo dir) {
        progressBarDialog.showDialog("加载中...");
        new Thread(new Runnable() {
            @Override
            public void run() {
                RootFileInfo parent = new RootFileInfo(dir.getParent());
                String parentPath = parent.getAbsolutePath();
                hasParent = parent.exists() && (leaveRootDir || !(rootDir.startsWith(parentPath) && rootDir.length() > parentPath.length()));

                if (dir.exists()) {
                    ArrayList<RootFileInfo> files = dir.listFiles();
                    // pathname.exists() && (!pathname.isFile() || extension == null || extension.isEmpty() || pathname.getName().endsWith(extension));

                    // 文件排序
                    for (int i = 0; i < files.size(); i++) {
                        for (int j = i + 1; j < files.size(); j++) {
                            RootFileInfo curr = files.get(j);
                            if ((curr.isDirectory() && files.get(i).isFile())) {
                                RootFileInfo t = files.get(i);
                                files.set(i, files.get(j));
                                files.set(j, t);
                            } else if (curr.isDirectory() == files.get(i).isDirectory() && (curr.getName().toLowerCase().compareTo(files.get(i).getName().toLowerCase()) < 0)) {
                                RootFileInfo t = files.get(i);
                                files.set(i, curr);
                                files.set(j, t);
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
            loadDir(new RootFileInfo(currentDir.getParent()));
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
            return fileArray.size() + 1;
        } else {
            if (fileArray == null) {
                return 0;
            }
            return fileArray.size();
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
                return new RootFileInfo(currentDir.getParent());
            } else {
                return fileArray.get(position - 1);
            }
        } else {
            return fileArray.get(position);
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
            final RootFileInfo file = (RootFileInfo) getItem(position);
            if (file.isDirectory()) {
                view = View.inflate(parent.getContext(), R.layout.list_item_dir, null);
                view.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (!file.exists()) {
                            Toast.makeText(view.getContext(), "所选的文件已被删除，请重新选择！", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        ArrayList<RootFileInfo> files = file.listFiles();
                        if (files.size() > 0) {
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
                            DialogHelper.Companion.animDialog(new AlertDialog.Builder(view.getContext()).setTitle("选定文件？")
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
                                    }));
                        }
                    });
                }
            }
            if (longClickDelete) {
                view.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View v) {
                        DialogHelper.Companion.confirm(view.getContext(),
                                "删除所选文件？",
                                file.getAbsolutePath(),
                                new Runnable() {
                                    @Override
                                    public void run() {
                                        if (!file.exists()) {
                                            Toast.makeText(view.getContext(), "所选的文件已被删除，请重新选择！", Toast.LENGTH_SHORT).show();
                                            return;
                                        }
                                        selectedFile = file;
                                        fileDelete.run();
                                    }
                                }, new Runnable() {
                                    @Override
                                    public void run() {
                                    }
                                });
                        return true;
                    }
                });
            }
            ((TextView) (view.findViewById(R.id.ItemTitle))).setText(file.getName());
            return view;
        }
    }

    public RootFileInfo getSelectedFile() {
        return this.selectedFile;
    }
}

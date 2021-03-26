package com.omarea.ui;

import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.snackbar.Snackbar;
import com.omarea.common.ui.DialogHelper;
import com.omarea.common.ui.ProgressBarDialog;
import com.omarea.vtools.R;

import java.io.File;
import java.io.FileFilter;

public class AdapterFileSelector extends BaseAdapter {
    private File[] fileArray;
    private Runnable fileSelected;
    private File currentDir;
    private File selectedFile;
    private Handler handler = new Handler(Looper.getMainLooper());
    private ProgressBarDialog progressBarDialog;
    private String extension;
    private boolean hasParent = false; // 是否还有父级
    private String rootDir = "/"; // 根目录
    private boolean leaveRootDir = true; // 是否允许离开设定的rootDir到更父级的目录去
    private boolean folderChooserMode = false; // 是否是目录选择模式（目录选择模式下不显示文件，长按目录选中）

    private AdapterFileSelector(File rootDir, Runnable fileSelected, ProgressBarDialog progressBarDialog, String extension) {
        init(rootDir, fileSelected, progressBarDialog, extension);
    }

    public static AdapterFileSelector FolderChooser(File rootDir, Runnable fileSelected, ProgressBarDialog progressBarDialog) {
        AdapterFileSelector adapterFileSelector = new AdapterFileSelector(rootDir, fileSelected, progressBarDialog, null);
        adapterFileSelector.folderChooserMode = true;
        return adapterFileSelector;
    }

    public static AdapterFileSelector FileChooser(File rootDir, Runnable fileSelected, ProgressBarDialog progressBarDialog, String extension) {
        AdapterFileSelector adapterFileSelector = new AdapterFileSelector(rootDir, fileSelected, progressBarDialog, extension);
        adapterFileSelector.folderChooserMode = false;
        return adapterFileSelector;
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
                File parent = dir.getParentFile();
                if (parent != null) {
                    String parentPath = parent.getAbsolutePath();
                    hasParent = parent.exists() && parent.canRead() && (leaveRootDir || !(rootDir.startsWith(parentPath) && rootDir.length() > parentPath.length()));
                } else {
                    hasParent = false;
                }

                if (dir.exists() && dir.canRead()) {
                    File[] files = dir.listFiles(new FileFilter() {
                        @Override
                        public boolean accept(File fileItem) {
                            if (folderChooserMode) {
                                return fileItem.isDirectory();
                            } else {
                                return fileItem.exists() && (!fileItem.isFile() || extension == null || extension.isEmpty() || fileItem.getName().endsWith(extension));
                            }
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
                if (folderChooserMode) {
                    view.setOnLongClickListener(new View.OnLongClickListener() {
                        @Override
                        public boolean onLongClick(View v) {
                            DialogHelper.Companion.confirm(view.getContext(),
                                    "选定目录？",
                                    file.getAbsolutePath(),
                                    new Runnable() {
                                        @Override
                                        public void run() {
                                            if (!file.exists()) {
                                                Toast.makeText(view.getContext(), "所选的目录已被删除，请重新选择！", Toast.LENGTH_SHORT).show();
                                                return;
                                            }
                                            selectedFile = file;
                                            fileSelected.run();
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

                view.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        DialogHelper.Companion.confirm(view.getContext(),
                                "选定文件？",
                                file.getAbsolutePath(),
                                new Runnable() {
                                    @Override
                                    public void run() {
                                        if (!file.exists()) {
                                            Toast.makeText(view.getContext(), "所选的文件已被删除，请重新选择！", Toast.LENGTH_SHORT).show();
                                            return;
                                        }
                                        selectedFile = file;
                                        fileSelected.run();
                                    }
                                }, new Runnable() {
                                    @Override
                                    public void run() {
                                    }
                                });
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

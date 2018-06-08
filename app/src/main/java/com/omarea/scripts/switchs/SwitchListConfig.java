package com.omarea.scripts.switchs;

import android.content.DialogInterface;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import com.omarea.scripts.SwitchAdapter;
import com.omarea.scripts.simple.shell.SimpleShellExecutor;
import com.omarea.ui.OverScrollListView;
import com.omarea.vboot.R;

import java.util.ArrayList;
import java.util.HashMap;

public class SwitchListConfig {
    private FragmentActivity context;
    private OverScrollListView listView;

    public SwitchListConfig(FragmentActivity mainActivity) {
        this.context = mainActivity;
    }

    public void setListData(ArrayList<SwitchInfo> switchInfos) {
        if (switchInfos != null) {
            listView = context.findViewById(R.id.list_switchs);
            assert listView != null;
            listView.setOverScrollMode(ListView.OVER_SCROLL_ALWAYS);
            listView.setAdapter(new SwitchAdapter(switchInfos));
            listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, final int position, long id) {
                    onActionClick((SwitchInfo) parent.getAdapter().getItem(position), new Runnable() {
                        @Override
                        public void run() {
                            if (listView != null) {
                                listView.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        ((SwitchAdapter) listView.getAdapter()).update(position, listView);
                                    }
                                });
                            }
                        }
                    });
                }
            });
        }
    }

    private void onActionClick(final SwitchInfo action, final Runnable onExit) {
        final boolean toValue = !action.selected;
        if (action.confirm) {
            new AlertDialog.Builder(context)
                    .setTitle(action.title)
                    .setMessage(action.desc)
                    .setPositiveButton("执行", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            executeScript(action, toValue, onExit);
                        }
                    })
                    .setNegativeButton("取消", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    })
                    .create()
                    .show();
        } else {
            executeScript(action, toValue, onExit);
        }
    }

    private void executeScript(final SwitchInfo action, final boolean toValue, Runnable onExit) {
        String script = action.setState;
        if (script == null) {
            return;
        }
        final StringBuilder cmds = new StringBuilder();
        cmds.append(script);

        String startPath = context.getFilesDir().getAbsolutePath();
        if (action.start != null) {
            startPath = action.start;
        }
        cmds.insert(0, "state=\"" + (toValue ? "1" : "0") + "\"\n");
        if (action.setStateType == SwitchInfo.ActionScript.ASSETS_FILE) {
            cmds.append(" $state");
        }
        cmds.append("\n\n");
        cmds.append("\n\n");
        executeScript(action.title, action.root, cmds, startPath, onExit, new HashMap<String, String>() {{
            put("state", (toValue ? "1" : "0"));
        }});
    }

    private void executeScript(String title, Boolean root, StringBuilder cmds, String startPath, Runnable onExit, HashMap<String, String> params) {
        new SimpleShellExecutor(context, context.getWindow()).execute(root, title, cmds, startPath, onExit, params);
    }
}

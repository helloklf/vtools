package com.omarea.krscripts.switchs;

import android.content.DialogInterface;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import com.omarea.krscripts.simple.shell.SimpleShellExecutor;
import com.omarea.ui.OverScrollListView;
import com.omarea.vtools.R;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;

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
            AlertDialog dialog = new AlertDialog.Builder(context)
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
                    .create();
            Objects.requireNonNull(dialog.getWindow()).setWindowAnimations(R.style.windowAnim);
            dialog.show();
        } else {
            executeScript(action, toValue, onExit);
        }
    }

    private void executeScript(final SwitchInfo action, final boolean toValue, Runnable onExit) {
        String script = action.setState;
        if (script == null) {
            return;
        }

        String startPath = null;
        if (action.start != null) {
            startPath = action.start;
        }

        executeScript(action.title, script, startPath, onExit, new HashMap<String, String>() {{
            put("state", (toValue ? "1" : "0"));
        }});
    }

    private void executeScript(String title, String cmds, String startPath, Runnable onExit, HashMap<String, String> params) {
        new SimpleShellExecutor(context).execute(title, cmds, startPath, onExit, params);
    }
}

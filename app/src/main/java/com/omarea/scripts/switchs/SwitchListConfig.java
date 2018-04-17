package com.omarea.scripts.switchs;

import android.app.Activity;
import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Switch;
import com.omarea.scripts.simple.shell.SimpleShellExecutor;
import com.omarea.ui.OverScrollListView;
import com.omarea.vboot.R;
import java.util.ArrayList;
import java.util.HashMap;

public class SwitchListConfig {
    private Activity context;
    public SwitchListConfig(Activity mainActivity) {
        this.context = mainActivity;
    }

    public void setListData(ArrayList<ActionInfo> actionInfos) {
        if (actionInfos != null) {
            ArrayList<HashMap<String, Object>> data = new ArrayList<>();
            for (ActionInfo actionInfo : actionInfos) {
                HashMap<String,Object> row = new HashMap<>();
                row.put("title", actionInfo.title);
                row.put("desc", actionInfo.desc);
                row.put("selected", actionInfo.selected);
                row.put("item", actionInfo);
                data.add(row);
            }
            OverScrollListView listView = (OverScrollListView) context.findViewById(R.id.list_switchs);
            assert listView != null;
            listView.setOverScrollMode(ListView.OVER_SCROLL_ALWAYS);
            SimpleAdapter mSimpleAdapter = new SimpleAdapter(
                    context, data,
                    R.layout.switch_row_item,
                    new String[]{"title", "desc", "selected"},
                    new int[]{R.id.Title, R.id.Desc, R.id.Title}
            );
            listView.setAdapter(mSimpleAdapter);
            listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    Switch switchItem = view.findViewById(R.id.Title);
                    switchItem.setChecked(!switchItem.isChecked());
                    onActionClick((ActionInfo) ((HashMap<String, Object>) parent.getAdapter().getItem(position)).get("item"), switchItem.isChecked());
                }
            });
        }
    }

    private void onActionClick(final ActionInfo action, final boolean toValue) {
        if (action.confirm) {
            new AlertDialog.Builder(context)
                    .setTitle(action.title)
                    .setMessage(action.desc)
                    .setPositiveButton("执行", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                        executeScript(action, toValue);
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
            executeScript(action, toValue);
        }
    }

    private void executeScript(final ActionInfo action, final boolean toValue) {
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
        cmds.insert(0, "state=\"" + (toValue ? "1":"0") + "\"\n");
        if (action.setStateType == ActionInfo.ActionScript.ASSETS_FILE) {
            cmds.append(" $state");
        }
        cmds.append("\n\n");
        cmds.append("\n\n");
        executeScript(action.title, action.root, cmds, startPath);
    }

    private void executeScript(String title, Boolean root, StringBuilder cmds, String startPath) {
        new SimpleShellExecutor(context).execute(root, cmds, startPath);
    }

}

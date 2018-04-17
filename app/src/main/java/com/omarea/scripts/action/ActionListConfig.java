package com.omarea.scripts.action;

import android.app.Activity;
import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Spinner;
import com.omarea.scripts.simple.shell.SimpleShellExecutor;
import com.omarea.ui.OverScrollListView;
import java.util.ArrayList;
import java.util.HashMap;
import com.omarea.vboot.R;

public class ActionListConfig {
    private Activity context;
    public ActionListConfig(Activity mainActivity) {
        this.context = mainActivity;
    }

    public void setListData(ArrayList<ActionInfo> actionInfos) {
        if (actionInfos != null) {
            ArrayList<HashMap<String, Object>> data = new ArrayList<>();
            for (ActionInfo actionInfo : actionInfos) {
                HashMap<String,Object> row = new HashMap<>();
                row.put("title", actionInfo.title);
                row.put("desc", actionInfo.desc);
                row.put("item", actionInfo);
                data.add(row);
            }
            OverScrollListView listView = (OverScrollListView) context.findViewById(R.id.list_actions);
            assert listView != null;
            listView.setOverScrollMode(ListView.OVER_SCROLL_ALWAYS);
            SimpleAdapter mSimpleAdapter = new SimpleAdapter(
                    context, data,
                    R.layout.action_row_item,
                    new String[]{"title", "desc"},
                    new int[]{R.id.Title, R.id.Desc}
            );
            listView.setAdapter(mSimpleAdapter);
            listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    onActionClick((ActionInfo) ((HashMap<String, Object>) parent.getAdapter().getItem(position)).get("item"));
                }
            });
        }
    }

    private void onActionClick(final ActionInfo action) {
        if (action.confirm) {
            new AlertDialog.Builder(context)
                    .setTitle(action.title)
                    .setMessage(action.desc)
                    .setPositiveButton("执行", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            executeScript(action);
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
            executeScript(action);
        }
    }

    private void executeScript(final ActionInfo action) {
        String script = action.script;
        if (script == null) {
            return;
        }
        final StringBuilder cmds = new StringBuilder();
        cmds.append(script);

        String startPath = context.getFilesDir().getAbsolutePath();
        if (action.start != null) {
            startPath = action.start;
        }
        if (action.params != null) {
            final ArrayList<ActionParamInfo> actionParamInfos = (ArrayList<ActionParamInfo>) action.params;
            if (actionParamInfos.size() > 0) {
                LayoutInflater layoutInflater = context.getLayoutInflater();
                View view = layoutInflater.inflate(R.layout.dialog_params, null);
                final LinearLayout linearLayout = (LinearLayout) view.findViewById(R.id.params_list);

                for (ActionParamInfo actionParamInfo : actionParamInfos) {
                    if (actionParamInfo.options != null && actionParamInfo.options.size() > 0) {
                        Spinner spinner = new Spinner(context);
                        ArrayList<HashMap<String, Object>> options = new ArrayList<>();
                        int selectedIndex = -1;
                        int index = 0;
                        for (ActionParamInfo.ActionParamOption option : actionParamInfo.options) {
                            HashMap<String, Object> opt = new HashMap<>();
                            opt.put("title", option.desc);
                            opt.put("item", option);
                            options.add(opt);
                            if (actionParamInfo.value != null && option.value.equals(actionParamInfo.value)) {
                                selectedIndex = index;
                            }
                            index++;
                        }
                        spinner.setAdapter(new SimpleAdapter(context, options, R.layout.string_item,new String[]{ "title" }, new int[]{ R.id.text }));
                        spinner.setTag(actionParamInfo);
                        linearLayout.addView(spinner);
                        LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) spinner.getLayoutParams();
                        lp.setMargins(0, 10, 0, 20);
                        spinner.setLayoutParams(lp);
                        if(selectedIndex > -1) {
                            spinner.setSelection(selectedIndex);
                        }
                    } else if (actionParamInfo.type != null && actionParamInfo.type.equals("bool")) {
                        CheckBox checkBox = new CheckBox(context);
                        if (actionParamInfo.desc != null) {
                            checkBox.setHint(actionParamInfo.desc);
                        } else {
                            checkBox.setHint(actionParamInfo.name);
                        }
                        if (actionParamInfo.value != null) {
                            checkBox.setChecked(actionParamInfo.value.equals("1") || actionParamInfo.value.toLowerCase().equals("true"));
                        }
                        checkBox.setTag(actionParamInfo);
                        linearLayout.addView(checkBox);
                        LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) checkBox.getLayoutParams();
                        lp.setMargins(0, 10, 0, 20);
                        checkBox.setLayoutParams(lp);
                    } else {
                        EditText editText = new EditText(context);
                        if (actionParamInfo.desc != null) {
                            editText.setHint(actionParamInfo.desc);
                        } else {
                            editText.setHint(actionParamInfo.name);
                        }
                        if (actionParamInfo.value != null) {
                            editText.setText(actionParamInfo.value);
                        }
                        editText.setFilters(new ActionParamInfo.ParamInfoFilter[]{ new ActionParamInfo.ParamInfoFilter(actionParamInfo) });
                        editText.setTag(actionParamInfo);
                        linearLayout.addView(editText);
                        LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) editText.getLayoutParams();
                        lp.setMargins(0, 10, 0, 20);
                        editText.setLayoutParams(lp);
                    }
                }

                final String finalStartPath = startPath;
                new AlertDialog.Builder(context)
                        .setTitle(action.title)
                        .setView(view)
                        .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                for (ActionParamInfo actionParamInfo : actionParamInfos) {
                                    View view = linearLayout.findViewWithTag(actionParamInfo);
                                    if (view instanceof EditText) {
                                        actionParamInfo.value = ((EditText) view).getText().toString();
                                    } else if (view instanceof CheckBox) {
                                        actionParamInfo.value = ((CheckBox) view).isChecked() ? "1" : "0";
                                    } else if (view instanceof Spinner) {
                                        Object item = ((Spinner) view).getSelectedItem();
                                        if (item instanceof HashMap) {
                                            ActionParamInfo.ActionParamOption opt = (ActionParamInfo.ActionParamOption) ((HashMap<String, Object>) item).get("item");
                                            actionParamInfo.value = opt.value;
                                        } else
                                            actionParamInfo.value = item.toString();
                                    }
                                    cmds.insert(0, actionParamInfo.name + "=\"" + actionParamInfo.value.replaceAll("\"", " ") + "\"\n");
                                    if (action.scriptType == ActionInfo.ActionScript.ASSETS_FILE) {
                                        cmds.append(" $");
                                        cmds.append(actionParamInfo.name);
                                    }
                                }
                                cmds.append("\n\n");
                                executeScript(action.title, action.root, cmds, finalStartPath);
                            }
                        })
                        .create()
                        .show();
                return;
            }
        }
        cmds.append("\n\n");
        executeScript(action.title, action.root, cmds, startPath);
    }

    private void executeScript(String title, Boolean root, StringBuilder cmds, String startPath) {
        new SimpleShellExecutor(context).execute(root, cmds, startPath);
    }

}

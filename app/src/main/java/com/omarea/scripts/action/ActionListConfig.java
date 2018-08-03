package com.omarea.scripts.action;

import android.content.DialogInterface;
import android.os.Handler;
import android.support.v4.app.FragmentActivity;
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

import com.omarea.scripts.ActionAdapter;
import com.omarea.scripts.simple.shell.ExecuteCommandWithOutput;
import com.omarea.scripts.simple.shell.SimpleShellExecutor;
import com.omarea.ui.OverScrollListView;
import com.omarea.ui.ProgressBarDialog;
import com.omarea.vboot.R;

import java.util.ArrayList;
import java.util.HashMap;

public class ActionListConfig {
    private FragmentActivity context;
    private OverScrollListView listView;
    private ProgressBarDialog progressBarDialog;

    public ActionListConfig(FragmentActivity mainActivity) {
        this.context = mainActivity;
        this.progressBarDialog = new ProgressBarDialog(mainActivity);
    }

    public void setListData(ArrayList<ActionInfo> actionInfos) {
        if (actionInfos != null) {
            listView = context.findViewById(R.id.list_actions);
            assert listView != null;
            listView.setOverScrollMode(ListView.OVER_SCROLL_ALWAYS);
            listView.setAdapter(new ActionAdapter(actionInfos));
            listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, final int position, long id) {
                    onActionClick((ActionInfo) parent.getAdapter().getItem(position), new Runnable() {
                        @Override
                        public void run() {
                            if (listView != null) {
                                listView.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        ((ActionAdapter) listView.getAdapter()).update(position, listView);
                                    }
                                });
                            }
                        }
                    });
                }
            });
        }
    }

    private void onActionClick(final ActionInfo action, final Runnable onExit) {
        if (action.confirm) {
            new AlertDialog
                    .Builder(context)
                    .setTitle(action.title)
                    .setMessage(action.desc)
                    .setPositiveButton("执行", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            executeScript(action, onExit);
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
            executeScript(action, onExit);
        }
    }

    private void executeScript(final ActionInfo action, final Runnable onExit) {
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
            final ArrayList<ActionParamInfo> actionParamInfos = action.params;
            if (actionParamInfos.size() > 0) {
                LayoutInflater layoutInflater = context.getLayoutInflater();
                final View view = layoutInflater.inflate(R.layout.dialog_params, null);
                final LinearLayout linearLayout = view.findViewById(R.id.params_list);

                final Handler handler = new Handler();
                final String finalStartPath = startPath;
                progressBarDialog.showDialog("正在读取数据...");
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        for (ActionParamInfo actionParamInfo : actionParamInfos) {
                            if (actionParamInfo.valueSUShell != null) {
                                actionParamInfo.valueFromShell = ExecuteCommandWithOutput.executeCommandWithOutput(true, actionParamInfo.valueSUShell);
                            } else if (actionParamInfo.valueShell != null) {
                                actionParamInfo.valueFromShell = ExecuteCommandWithOutput.executeCommandWithOutput(false, actionParamInfo.valueShell);
                            }
                        }
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                progressBarDialog.hideDialog();
                                for (ActionParamInfo actionParamInfo : actionParamInfos) {
                                    if ((actionParamInfo.options != null && actionParamInfo.options.size() > 0) || (actionParamInfo.optionsSh != null && !actionParamInfo.optionsSh.isEmpty()) || (actionParamInfo.optionsSU != null && !actionParamInfo.optionsSU.isEmpty())) {
                                        if (actionParamInfo.options == null)
                                            actionParamInfo.options = new ArrayList<>();
                                        Spinner spinner = new Spinner(context);
                                        ArrayList<HashMap<String, Object>> options = new ArrayList<>();
                                        int selectedIndex = -1;
                                        int index = 0;
                                        ArrayList<String> valList = new ArrayList<>();

                                        if ((actionParamInfo.optionsSh != null && !actionParamInfo.optionsSh.isEmpty()) || (actionParamInfo.optionsSU != null && !actionParamInfo.optionsSU.isEmpty())) {
                                            String shellResult = "";
                                            if (actionParamInfo.optionsSh != null && !actionParamInfo.optionsSh.isEmpty()) {
                                                shellResult = ExecuteCommandWithOutput.executeCommandWithOutput(false, actionParamInfo.optionsSh);
                                            } else {
                                                shellResult = ExecuteCommandWithOutput.executeCommandWithOutput(true, actionParamInfo.optionsSU);
                                            }
                                            if (shellResult != null && !shellResult.isEmpty()) {
                                                for (final String item : shellResult.split("\n")) {
                                                    if (item.contains("|")) {
                                                        final String[] itemSplit = item.split("\\|");
                                                        options.add(new HashMap<String, Object>() {{
                                                            put("title", itemSplit[1]);
                                                            put("item", new ActionParamInfo.ActionParamOption() {{
                                                                value = itemSplit[0];
                                                                desc = itemSplit[1];
                                                            }});
                                                        }});
                                                    } else {
                                                        options.add(new HashMap<String, Object>() {{
                                                            put("title", item);
                                                            put("item", new ActionParamInfo.ActionParamOption() {{
                                                                value = item;
                                                                desc = item;
                                                            }});
                                                        }});
                                                    }
                                                }
                                            } else {
                                                for (ActionParamInfo.ActionParamOption option : actionParamInfo.options) {
                                                    HashMap<String, Object> opt = new HashMap<>();
                                                    opt.put("title", option.desc);
                                                    opt.put("item", option);
                                                    options.add(opt);
                                                }
                                            }
                                        } else {
                                            for (ActionParamInfo.ActionParamOption option : actionParamInfo.options) {
                                                HashMap<String, Object> opt = new HashMap<>();
                                                opt.put("title", option.desc);
                                                opt.put("item", option);
                                                options.add(opt);
                                            }
                                        }

                                        if (actionParamInfo.valueFromShell != null)
                                            valList.add(actionParamInfo.valueFromShell);
                                        if (actionParamInfo.value != null) {
                                            valList.add(actionParamInfo.value);
                                        }
                                        if (valList.size() > 0) {
                                            for (int j = 0; j < valList.size(); j++) {
                                                for (HashMap<String, Object> option : options) {
                                                    if (((ActionParamInfo.ActionParamOption) option.get("item")).value.equals(valList.get(j))) {
                                                        selectedIndex = index;
                                                        break;
                                                    }
                                                    index++;
                                                }
                                                if (selectedIndex > -1)
                                                    break;
                                            }
                                        }
                                        spinner.setAdapter(new SimpleAdapter(context, options, R.layout.string_item, new String[]{"title"}, new int[]{R.id.text}));
                                        spinner.setTag(actionParamInfo);
                                        if (options.size() > 6) {
                                            //TODO:列表过长时切换为弹窗
                                        }
                                        if (actionParamInfo.desc != null && !actionParamInfo.desc.isEmpty()) {
                                            spinner.setPrompt(actionParamInfo.desc);
                                        }
                                        linearLayout.addView(spinner);
                                        LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) spinner.getLayoutParams();
                                        lp.setMargins(0, 10, 0, 20);
                                        spinner.setLayoutParams(lp);
                                        if (selectedIndex > -1 && selectedIndex < options.size()) {
                                            spinner.setSelection(selectedIndex);
                                        }
                                    } else if (actionParamInfo.type != null && actionParamInfo.type.equals("bool")) {
                                        CheckBox checkBox = new CheckBox(context);
                                        checkBox.setHint(actionParamInfo.desc != null ? actionParamInfo.desc : actionParamInfo.name);

                                        if (actionParamInfo.valueFromShell != null)
                                            checkBox.setChecked(actionParamInfo.valueFromShell.equals("1") || actionParamInfo.valueFromShell.toLowerCase().equals("true"));
                                        else if (actionParamInfo.value != null)
                                            checkBox.setChecked(actionParamInfo.value.equals("1") || actionParamInfo.value.toLowerCase().equals("true"));

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
                                        if (actionParamInfo.valueFromShell != null)
                                            editText.setText(actionParamInfo.valueFromShell);
                                        else if (actionParamInfo.value != null)
                                            editText.setText(actionParamInfo.value);
                                        editText.setFilters(new ActionParamInfo.ParamInfoFilter[]{new ActionParamInfo.ParamInfoFilter(actionParamInfo)});
                                        editText.setTag(actionParamInfo);
                                        linearLayout.addView(editText);
                                        LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) editText.getLayoutParams();
                                        lp.setMargins(0, 10, 0, 20);
                                        editText.setLayoutParams(lp);
                                    }
                                }
                                new AlertDialog.Builder(context)
                                        .setTitle(action.title)
                                        .setView(view)
                                        .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                executeScript(action.title, action.root, cmds, finalStartPath, onExit, readInput(actionParamInfos, linearLayout, cmds, action));
                                            }
                                        })
                                        .create()
                                        .show();
                            }
                        });
                    }
                }).start();


                return;
            }
        }
        cmds.append("\n\n");
        executeScript(action.title, action.root, cmds, startPath, onExit, null);
    }

    private HashMap<String, String> readInput(ArrayList<ActionParamInfo> actionParamInfos, LinearLayout linearLayout, StringBuilder cmds, ActionInfo actionInfo) {
        HashMap<String, String> params = new HashMap<>();
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
            if (actionInfo.scriptType == ActionInfo.ActionScript.ASSETS_FILE) {
                cmds.append(" $");
                cmds.append(actionParamInfo.name);
            }
            params.put(actionParamInfo.name, actionParamInfo.value);
        }
        cmds.append("\n\n");
        return params;
    }

    private void executeScript(String title, Boolean root, StringBuilder cmds, String startPath, Runnable onExit, HashMap<String, String> params) {
        new SimpleShellExecutor(context, context.getWindow()).execute(root, title, cmds, startPath, onExit, params);
    }
}

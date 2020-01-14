package com.omarea.gesture;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.TextView;

import com.omarea.gesture.util.AppInfo;
import com.omarea.gesture.util.AppListHelper;
import com.omarea.gesture.util.Handlers;

import java.util.ArrayList;

public class DialogHandlerEX {
    public void openDialog(Context context, final String key, int customActionCode) {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(context).setCancelable(false);

        final SharedPreferences configFile = context.getSharedPreferences(SpfConfigEx.configFile, Context.MODE_PRIVATE);
        final SharedPreferences.Editor config = configFile.edit();
        config.remove(SpfConfigEx.prefix_app + key);
        config.remove(SpfConfigEx.prefix_app_window + key);
        config.remove(SpfConfigEx.prefix_shell + key);

        switch (customActionCode) {
            case Handlers.CUSTOM_ACTION_APP:
            case Handlers.CUSTOM_ACTION_APP_WINDOW: {
                final String fullKey = (customActionCode == Handlers.CUSTOM_ACTION_APP ? SpfConfigEx.prefix_app : SpfConfigEx.prefix_app_window) + key;

                final ArrayList<AppInfo> appInfos = new AppListHelper().loadAppList(context);
                final String currentApp = configFile.getString(fullKey, "");
                alertDialog.setTitle(context.getString(R.string.custom_app));
                int currentIndex = -1;
                if (currentApp != null && !currentApp.isEmpty()) {
                    for (int i = 0; i < appInfos.size(); i++) {
                        if (appInfos.get(i).packageName.equals(currentApp)) {
                            currentIndex = i;
                            break;
                        }
                    }
                }

                final int finalCurrentIndex = currentIndex;
                alertDialog.setSingleChoiceItems(
                        new BaseAdapter() {
                            @Override
                            public int getCount() {
                                return appInfos.size();
                            }

                            @Override
                            public Object getItem(int position) {
                                return appInfos.get(position);
                            }

                            @Override
                            public long getItemId(int position) {
                                return position;
                            }

                            @Override
                            public View getView(int position, View convertView, ViewGroup parent) {
                                LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
                                View view = layoutInflater.inflate(R.layout.gesture_layout_app_option, null);
                                TextView title = view.findViewById(R.id.item_title);
                                TextView desc = view.findViewById(R.id.item_desc);
                                AppInfo appInfo = (AppInfo) getItem(position);
                                title.setText(appInfo.appName);
                                desc.setText(appInfo.packageName);
                                if (position == finalCurrentIndex) {
                                    title.setTextColor(title.getHighlightColor());
                                }

                                return view;
                            }
                        },
                        currentIndex, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                config.putString(fullKey, appInfos.get(which).packageName).apply();

                                dialog.dismiss();
                            }
                        });

                break;
            }
            case Handlers.CUSTOM_ACTION_SHELL: {
                final String fullKey = SpfConfigEx.prefix_shell + key;

                alertDialog.setTitle(context.getString(R.string.custom_shell));
                View view = LayoutInflater.from(context).inflate(R.layout.gesture_layout_ex_shell, null);
                final EditText editText = view.findViewById(R.id.ex_shell);
                editText.setText(configFile.getString(fullKey, ""));

                alertDialog.setView(view);
                alertDialog.setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        config.putString(fullKey, editText.getText().toString()).apply();
                    }
                });

                break;
            }
            default: {
                return;
            }
        }

        alertDialog.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
            }
        });
        alertDialog.show();
    }
}

package com.omarea.gesture.fragments;

import android.app.AlertDialog;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.Checkable;
import android.widget.CompoundButton;
import android.widget.SeekBar;
import android.widget.TextView;

import com.omarea.gesture.ActionModel;
import com.omarea.gesture.DialogHandlerEX;
import com.omarea.gesture.R;
import com.omarea.gesture.SpfConfig;
import com.omarea.gesture.util.Handlers;
import com.omarea.gesture.util.UITools;

public class FragmentSettingsBase extends Fragment {
    protected SharedPreferences config;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        config = getActivity().getSharedPreferences(SpfConfig.ConfigFile, Context.MODE_PRIVATE);

        super.onCreate(savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    protected void restartService() {
        try {
            Intent intent = new Intent(getString(R.string.action_config_changed));
            getActivity().sendBroadcast(intent);
        } catch (Exception ignored) {
        }
    }


    protected void bindHandlerPicker(int id, final String key, final int defValue) {
        final Button button = getActivity().findViewById(id);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openHandlerPicker(key, defValue);
            }
        });
    }

    private void openHandlerPicker(final String key, final int defValue) {
        final ActionModel[] items = Handlers.getOptions();

        final int currentValue = config.getInt(key, defValue);
        int index = -1;
        for (int i = 0; i < items.length; i++) {
            if (items[i].actionCode == currentValue) {
                index = i;
                break;
            }
        }

        final int finalIndex = index;
        new AlertDialog.Builder(getActivity())
                .setTitle(getString(R.string.handler_picker))
                .setSingleChoiceItems(new BaseAdapter() {
                    @Override
                    public int getCount() {
                        return items.length;
                    }

                    @Override
                    public Object getItem(int position) {
                        return items[position];
                    }

                    @Override
                    public long getItemId(int position) {
                        return items[position].actionCode;
                    }

                    @Override
                    public View getView(int position, View convertView, ViewGroup parent) {
                        ActionModel actionModel = ((ActionModel) getItem(position));
                        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.gesture_layout_action_option, null);
                        TextView textView = view.findViewById(R.id.item_title);
                        textView.setText(actionModel.title);
                        if (position == finalIndex) {
                            textView.setTextColor(textView.getHighlightColor());
                        }
                        return view;
                    }
                }, finalIndex, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        int code = items[which].actionCode;
                        config.edit().putInt(key, code).apply();
                        restartService();
                        dialog.dismiss();

                        if (code >= Handlers.CUSTOM_ACTION_APP) {
                            new DialogHandlerEX().openDialog(getActivity(), key, code);
                        }
                    }
                }).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
            }
        }).create().show();
    }

    protected void updateActionText(int id, String key, int defaultAction) {
        ((Button) getActivity().findViewById(id)).setText(Handlers.getOption(config.getInt(key, defaultAction)));
    }

    protected void bindCheckable(int id, final String key, boolean defValue) {
        final CompoundButton switchItem = getActivity().findViewById(id);
        switchItem.setChecked(config.getBoolean(key, defValue));
        switchItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                config.edit().putBoolean(key, ((Checkable) v).isChecked()).apply();
                restartService();
            }
        });
    }

    protected void setViewBackground(View view, int color) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.RECTANGLE);
        drawable.setGradientType(GradientDrawable.RECTANGLE);
        drawable.setCornerRadius(UITools.dp2px(getActivity(), 15));
        drawable.setColor(color);
        drawable.setStroke(2, 0xff888888);
        view.setBackground(drawable);
    }

    /**
     * 选择颜色
     *
     * @param key
     * @param defValue
     */
    private void openColorPicker(final String key, final int defValue) {
        View view = getActivity().getLayoutInflater().inflate(R.layout.gesture_color_picker, null);
        int currentColor = config.getInt(key, defValue);
        final SeekBar alphaBar = view.findViewById(R.id.color_alpha);
        final SeekBar redBar = view.findViewById(R.id.color_red);
        final SeekBar greenBar = view.findViewById(R.id.color_green);
        final SeekBar blueBar = view.findViewById(R.id.color_blue);
        final Button colorPreview = view.findViewById(R.id.color_preview);

        alphaBar.setProgress(Color.alpha(currentColor));
        redBar.setProgress(Color.red(currentColor));
        greenBar.setProgress(Color.green(currentColor));
        blueBar.setProgress(Color.blue(currentColor));
        colorPreview.setBackgroundColor(currentColor);

        SeekBar.OnSeekBarChangeListener listener = new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int color = Color.argb(alphaBar.getProgress(), redBar.getProgress(), greenBar.getProgress(), blueBar.getProgress());
                colorPreview.setBackgroundColor(color);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        };
        alphaBar.setOnSeekBarChangeListener(listener);
        redBar.setOnSeekBarChangeListener(listener);
        greenBar.setOnSeekBarChangeListener(listener);
        blueBar.setOnSeekBarChangeListener(listener);

        new AlertDialog.Builder(getActivity())
                .setTitle(getString(R.string.effect_color_picker))
                .setView(view)
                .setPositiveButton(getString(R.string.confirm), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        int color = Color.argb(alphaBar.getProgress(), redBar.getProgress(), greenBar.getProgress(), blueBar.getProgress());
                        config.edit().putInt(key, color).apply();
                        restartService();
                    }
                })
                .setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                })
                .create()
                .show();
    }

    protected void bindColorPicker(int id, final String key, final int defValue) {
        final Button button = getActivity().findViewById(id);

        setViewBackground(button, config.getInt(key, defValue));

        // button.setBackgroundColor(config.getInt(key, defValue));
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openColorPicker(key, defValue);
            }
        });
    }

    protected void bindSeekBar(int id, final String key, int defValue, final boolean updateView) {
        final SeekBar seekBar = getActivity().findViewById(id);
        seekBar.setProgress(config.getInt(key, defValue));
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                config.edit().putInt(key, (seekBar.getProgress())).apply();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (updateView) {
                    restartService();
                }
            }
        });
    }
}

package com.omarea.gesture.fragments;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Checkable;
import android.widget.Switch;
import android.widget.Toast;

import com.omarea.gesture.R;
import com.omarea.gesture.SpfConfig;

public class FragmentWhiteBar extends FragmentSettingsBase {

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {

        return inflater.inflate(R.layout.gesture_ios_bar_options, container, false);
    }

    @Override
    public void onResume() {
        super.onResume();

        bindCheckable(R.id.landscape_ios_bar, SpfConfig.LANDSCAPE_IOS_BAR, SpfConfig.LANDSCAPE_IOS_BAR_DEFAULT);
        bindCheckable(R.id.portrait_ios_bar, SpfConfig.PORTRAIT_IOS_BAR, SpfConfig.PORTRAIT_IOS_BAR_DEFAULT);

        bindHandlerPicker(R.id.ios_bar_slide_left, SpfConfig.IOS_BAR_SLIDE_LEFT, SpfConfig.IOS_BAR_SLIDE_LEFT_DEFAULT);
        bindHandlerPicker(R.id.ios_bar_slide_right, SpfConfig.IOS_BAR_SLIDE_RIGHT, SpfConfig.IOS_BAR_SLIDE_RIGHT_DEFAULT);
        bindHandlerPicker(R.id.ios_bar_slide_up, SpfConfig.IOS_BAR_SLIDE_UP, SpfConfig.IOS_BAR_SLIDE_UP_DEFAULT);
        bindHandlerPicker(R.id.ios_bar_slide_up_hover, SpfConfig.IOS_BAR_SLIDE_UP_HOVER, SpfConfig.IOS_BAR_SLIDE_UP_HOVER_DEFAULT);
        bindHandlerPicker(R.id.ios_bar_touch, SpfConfig.IOS_BAR_TOUCH, SpfConfig.IOS_BAR_TOUCH_DEFAULT);
        bindHandlerPicker(R.id.ios_bar_press, SpfConfig.IOS_BAR_PRESS, SpfConfig.IOS_BAR_PRESS_DEFAULT);

        bindColorPicker(R.id.ios_bar_color_shadow, SpfConfig.IOS_BAR_COLOR_SHADOW, SpfConfig.IOS_BAR_COLOR_SHADOW_DEFAULT);
        bindColorPicker(R.id.ios_bar_color_stroke, SpfConfig.IOS_BAR_COLOR_STROKE, SpfConfig.IOS_BAR_COLOR_STROKE_DEFAULT);

        bindSeekBar(R.id.ios_bar_width_landscape, SpfConfig.IOS_BAR_WIDTH_LANDSCAPE, SpfConfig.IOS_BAR_WIDTH_DEFAULT_LANDSCAPE, true);
        bindSeekBar(R.id.ios_bar_width_portrait, SpfConfig.IOS_BAR_WIDTH_PORTRAIT, SpfConfig.IOS_BAR_WIDTH_DEFAULT_PORTRAIT, true);
        bindSeekBar(R.id.ios_bar_alpha_fadeout_portrait, SpfConfig.IOS_BAR_ALPHA_FADEOUT_PORTRAIT, SpfConfig.IOS_BAR_ALPHA_FADEOUT_PORTRAIT_DEFAULT, true);
        bindSeekBar(R.id.ios_bar_alpha_fadeout_landscape, SpfConfig.IOS_BAR_ALPHA_FADEOUT_LANDSCAPE, SpfConfig.IOS_BAR_ALPHA_FADEOUT_LANDSCAPE_DEFAULT, true);
        bindColorPicker(R.id.ios_bar_color_landscape, SpfConfig.IOS_BAR_COLOR_LANDSCAPE, SpfConfig.IOS_BAR_COLOR_LANDSCAPE_DEFAULT);
        bindColorPicker(R.id.ios_bar_color_portrait, SpfConfig.IOS_BAR_COLOR_PORTRAIT, SpfConfig.IOS_BAR_COLOR_PORTRAIT_DEFAULT);
        bindSeekBar(R.id.ios_bar_size_shadow, SpfConfig.IOS_BAR_SHADOW_SIZE, SpfConfig.IOS_BAR_SHADOW_SIZE_DEFAULT, true);
        bindSeekBar(R.id.ios_bar_size_stroke, SpfConfig.IOS_BAR_STROKE_SIZE, SpfConfig.IOS_BAR_STROKE_SIZE_DEFAULT, true);
        bindSeekBar(R.id.ios_bar_total_height, SpfConfig.IOS_BAR_MARGIN_BOTTOM, SpfConfig.IOS_BAR_MARGIN_BOTTOM_DEFAULT, true);
        bindSeekBar(R.id.ios_bar_height, SpfConfig.IOS_BAR_HEIGHT, SpfConfig.IOS_BAR_HEIGHT_DEFAULT, true);
        bindCheckable(R.id.ios_bar_lock_hide, SpfConfig.IOS_BAR_LOCK_HIDE, SpfConfig.IOS_BAR_LOCK_HIDE_DEFAULT);
        bindCheckable(R.id.ios_bar_auto_color_fast, SpfConfig.IOS_BAR_COLOR_FAST, SpfConfig.IOS_BAR_COLOR_FAST_DEFAULT);
        setViewBackground(getActivity().findViewById(R.id.ios_bar_color_fadeout_portrait), 0xff888888);
        setViewBackground(getActivity().findViewById(R.id.ios_bar_color_fadeout_landscape), 0xff888888);

        Switch ios_bar_auto_color_root = getActivity().findViewById(R.id.ios_bar_auto_color_root);
        ios_bar_auto_color_root.setChecked(config.getBoolean(SpfConfig.IOS_BAR_AUTO_COLOR_ROOT, SpfConfig.IOS_BAR_AUTO_COLOR_ROOT_DEFAULT));
        if (!ios_bar_auto_color_root.isChecked()) {
            Switch checkable = getActivity().findViewById(R.id.ios_bar_auto_color_fast);
            checkable.setEnabled(false);
        }
        ios_bar_auto_color_root.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Checkable ele = (Checkable) v;
                if (ele.isChecked()) {
                    if (config.getBoolean(SpfConfig.ROOT, SpfConfig.ROOT_DEFAULT)) {
                        config.edit().putBoolean(SpfConfig.IOS_BAR_AUTO_COLOR_ROOT, true).apply();
                        getActivity().findViewById(R.id.ios_bar_auto_color_fast).setEnabled(true);
                        restartService();
                    } else {
                        ele.setChecked(false);
                        Toast.makeText(getActivity().getApplicationContext(), getString(R.string.need_root_mode), Toast.LENGTH_SHORT).show();
                    }
                } else {
                    config.edit().putBoolean(SpfConfig.IOS_BAR_AUTO_COLOR_ROOT, false).putBoolean(SpfConfig.IOS_BAR_COLOR_FAST, false).apply();
                    Switch checkable = getActivity().findViewById(R.id.ios_bar_auto_color_fast);
                    checkable.setEnabled(false);
                    checkable.setChecked(false);
                    restartService();
                }
            }
        });

        updateView();
    }

    private void updateView() {
        Activity context = getActivity();
        setViewBackground(context.findViewById(R.id.ios_bar_color_landscape), config.getInt(SpfConfig.IOS_BAR_COLOR_LANDSCAPE, SpfConfig.IOS_BAR_COLOR_LANDSCAPE_DEFAULT));
        setViewBackground(context.findViewById(R.id.ios_bar_color_portrait), config.getInt(SpfConfig.IOS_BAR_COLOR_PORTRAIT, SpfConfig.IOS_BAR_COLOR_PORTRAIT_DEFAULT));
        setViewBackground(context.findViewById(R.id.ios_bar_color_shadow), config.getInt(SpfConfig.IOS_BAR_COLOR_SHADOW, SpfConfig.IOS_BAR_COLOR_SHADOW_DEFAULT));
        setViewBackground(context.findViewById(R.id.ios_bar_color_stroke), config.getInt(SpfConfig.IOS_BAR_COLOR_STROKE, SpfConfig.IOS_BAR_COLOR_STROKE_DEFAULT));

        context.findViewById(R.id.ios_bar_color_fadeout_portrait).setAlpha(config.getInt(SpfConfig.IOS_BAR_ALPHA_FADEOUT_PORTRAIT, SpfConfig.IOS_BAR_ALPHA_FADEOUT_PORTRAIT_DEFAULT) / 100f);
        context.findViewById(R.id.ios_bar_color_fadeout_landscape).setAlpha(config.getInt(SpfConfig.IOS_BAR_ALPHA_FADEOUT_LANDSCAPE, SpfConfig.IOS_BAR_ALPHA_FADEOUT_LANDSCAPE_DEFAULT) / 100f);

        updateActionText(R.id.ios_bar_slide_left, SpfConfig.IOS_BAR_SLIDE_LEFT, SpfConfig.IOS_BAR_SLIDE_LEFT_DEFAULT);
        updateActionText(R.id.ios_bar_slide_right, SpfConfig.IOS_BAR_SLIDE_RIGHT, SpfConfig.IOS_BAR_SLIDE_RIGHT_DEFAULT);
        updateActionText(R.id.ios_bar_slide_up, SpfConfig.IOS_BAR_SLIDE_UP, SpfConfig.IOS_BAR_SLIDE_UP_DEFAULT);
        updateActionText(R.id.ios_bar_slide_up_hover, SpfConfig.IOS_BAR_SLIDE_UP_HOVER, SpfConfig.IOS_BAR_SLIDE_UP_HOVER_DEFAULT);
        updateActionText(R.id.ios_bar_touch, SpfConfig.IOS_BAR_TOUCH, SpfConfig.IOS_BAR_TOUCH_DEFAULT);
        updateActionText(R.id.ios_bar_press, SpfConfig.IOS_BAR_PRESS, SpfConfig.IOS_BAR_PRESS_DEFAULT);
    }

    @Override
    protected void restartService() {
        updateView();

        super.restartService();
    }
}

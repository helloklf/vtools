package com.omarea.gesture.fragments;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.omarea.gesture.R;
import com.omarea.gesture.SpfConfig;

public class FragmentEdge extends FragmentSettingsBase {
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.gesture_edge_options, container, false);
    }

    @Override
    public void onResume() {
        super.onResume();

        bindCheckable(R.id.allow_bottom_landscape, SpfConfig.CONFIG_BOTTOM_ALLOW_LANDSCAPE, SpfConfig.CONFIG_BOTTOM_ALLOW_LANDSCAPE_DEFAULT);
        bindCheckable(R.id.allow_bottom_portrait, SpfConfig.CONFIG_BOTTOM_ALLOW_PORTRAIT, SpfConfig.CONFIG_BOTTOM_ALLOW_PORTRAIT_DEFAULT);
        bindSeekBar(R.id.bar_width_bottom, SpfConfig.CONFIG_BOTTOM_WIDTH, SpfConfig.CONFIG_BOTTOM_WIDTH_DEFAULT, true);
        bindColorPicker(R.id.bar_color_bottom, SpfConfig.CONFIG_BOTTOM_COLOR, SpfConfig.CONFIG_BOTTOM_COLOR_DEFAULT);
        bindHandlerPicker(R.id.tap_bottom, SpfConfig.CONFIG_BOTTOM_EVENT, SpfConfig.CONFIG_BOTTOM_EVENT_DEFAULT);
        bindHandlerPicker(R.id.hover_bottom, SpfConfig.CONFIG_BOTTOM_EVENT_HOVER, SpfConfig.CONFIG_BOTTOM_EVENT_HOVER_DEFAULT);

        bindCheckable(R.id.allow_right_landscape, SpfConfig.CONFIG_RIGHT_ALLOW_LANDSCAPE, SpfConfig.CONFIG_RIGHT_ALLOW_LANDSCAPE_DEFAULT);
        bindCheckable(R.id.allow_right_portrait, SpfConfig.CONFIG_RIGHT_ALLOW_PORTRAIT, SpfConfig.CONFIG_RIGHT_ALLOW_PORTRAIT_DEFAULT);
        bindSeekBar(R.id.bar_height_right, SpfConfig.CONFIG_RIGHT_HEIGHT, SpfConfig.CONFIG_RIGHT_HEIGHT_DEFAULT, true);
        bindColorPicker(R.id.bar_color_right, SpfConfig.CONFIG_RIGHT_COLOR, SpfConfig.CONFIG_RIGHT_COLOR_DEFAULT);
        bindHandlerPicker(R.id.tap_right, SpfConfig.CONFIG_RIGHT_EVENT, SpfConfig.CONFIG_RIGHT_EVENT_DEFAULT);
        bindHandlerPicker(R.id.hover_right, SpfConfig.CONFIG_RIGHT_EVENT_HOVER, SpfConfig.CONFIG_RIGHT_EVENT_HOVER_DEFAULT);

        bindCheckable(R.id.allow_left_landscape, SpfConfig.CONFIG_LEFT_ALLOW_LANDSCAPE, SpfConfig.CONFIG_LEFT_ALLOW_LANDSCAPE_DEFAULT);
        bindCheckable(R.id.allow_left_portrait, SpfConfig.CONFIG_LEFT_ALLOW_PORTRAIT, SpfConfig.CONFIG_LEFT_ALLOW_PORTRAIT_DEFAULT);
        bindSeekBar(R.id.bar_height_left, SpfConfig.CONFIG_LEFT_HEIGHT, SpfConfig.CONFIG_LEFT_HEIGHT_DEFAULT, true);
        bindColorPicker(R.id.bar_color_left, SpfConfig.CONFIG_LEFT_COLOR, SpfConfig.CONFIG_LEFT_COLOR_DEFAULT);
        bindHandlerPicker(R.id.tap_left, SpfConfig.CONFIG_LEFT_EVENT, SpfConfig.CONFIG_LEFT_EVENT_DEFAULT);
        bindHandlerPicker(R.id.hover_left, SpfConfig.CONFIG_LEFT_EVENT_HOVER, SpfConfig.CONFIG_LEFT_EVENT_HOVER_DEFAULT);

        bindSeekBar(R.id.edge_side_width, SpfConfig.CONFIG_HOT_SIDE_WIDTH, SpfConfig.CONFIG_HOT_SIDE_WIDTH_DEFAULT, true);
        bindSeekBar(R.id.edge_bottom_height, SpfConfig.CONFIG_HOT_BOTTOM_HEIGHT, SpfConfig.CONFIG_HOT_BOTTOM_HEIGHT_DEFAULT, true);

        updateView();
    }


    private void updateView() {
        Activity activity = getActivity();
        setViewBackground(activity.findViewById(R.id.bar_color_bottom), config.getInt(SpfConfig.CONFIG_BOTTOM_COLOR, SpfConfig.CONFIG_BOTTOM_COLOR_DEFAULT));
        setViewBackground(activity.findViewById(R.id.bar_color_left), config.getInt(SpfConfig.CONFIG_LEFT_COLOR, SpfConfig.CONFIG_LEFT_COLOR_DEFAULT));
        setViewBackground(activity.findViewById(R.id.bar_color_right), config.getInt(SpfConfig.CONFIG_RIGHT_COLOR, SpfConfig.CONFIG_RIGHT_COLOR_DEFAULT));

        updateActionText(R.id.tap_bottom, SpfConfig.CONFIG_BOTTOM_EVENT, SpfConfig.CONFIG_BOTTOM_EVENT_DEFAULT);
        updateActionText(R.id.hover_bottom, SpfConfig.CONFIG_BOTTOM_EVENT_HOVER, SpfConfig.CONFIG_BOTTOM_EVENT_HOVER_DEFAULT);
        updateActionText(R.id.tap_left, SpfConfig.CONFIG_LEFT_EVENT, SpfConfig.CONFIG_LEFT_EVENT_DEFAULT);
        updateActionText(R.id.hover_left, SpfConfig.CONFIG_LEFT_EVENT_HOVER, SpfConfig.CONFIG_LEFT_EVENT_HOVER_DEFAULT);
        updateActionText(R.id.tap_right, SpfConfig.CONFIG_RIGHT_EVENT, SpfConfig.CONFIG_RIGHT_EVENT_DEFAULT);
        updateActionText(R.id.hover_right, SpfConfig.CONFIG_RIGHT_EVENT_HOVER, SpfConfig.CONFIG_RIGHT_EVENT_HOVER_DEFAULT);
    }

    @Override
    protected void restartService() {
        updateView();

        super.restartService();
    }
}

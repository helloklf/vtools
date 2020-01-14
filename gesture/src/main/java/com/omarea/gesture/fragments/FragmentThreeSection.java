package com.omarea.gesture.fragments;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.omarea.gesture.R;
import com.omarea.gesture.SpfConfig;

public class FragmentThreeSection extends FragmentSettingsBase {

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {

        return inflater.inflate(R.layout.gesture_three_section_options, container, false);
    }

    @Override
    public void onResume() {
        super.onResume();

        bindCheckable(R.id.allow_three_section_landscape, SpfConfig.THREE_SECTION_LANDSCAPE, SpfConfig.THREE_SECTION_LANDSCAPE_DEFAULT);
        bindCheckable(R.id.allow_three_section_portrait, SpfConfig.THREE_SECTION_PORTRAIT, SpfConfig.THREE_SECTION_PORTRAIT_DEFAULT);
        bindSeekBar(R.id.bar_width_three_section, SpfConfig.THREE_SECTION_WIDTH, SpfConfig.THREE_SECTION_WIDTH_DEFAULT, true);
        bindSeekBar(R.id.three_section_height, SpfConfig.THREE_SECTION_HEIGHT, SpfConfig.THREE_SECTION_HEIGHT_DEFAULT, true);

        bindHandlerPicker(R.id.three_section_left_slide, SpfConfig.THREE_SECTION_LEFT_SLIDE, SpfConfig.THREE_SECTION_LEFT_SLIDE_DEFAULT);
        bindHandlerPicker(R.id.three_section_center_slide, SpfConfig.THREE_SECTION_CENTER_SLIDE, SpfConfig.THREE_SECTION_CENTER_SLIDE_DEFAULT);
        bindHandlerPicker(R.id.three_section_right_slide, SpfConfig.THREE_SECTION_RIGHT_SLIDE, SpfConfig.THREE_SECTION_RIGHT_SLIDE_DEFAULT);
        bindHandlerPicker(R.id.three_section_left_hover, SpfConfig.THREE_SECTION_LEFT_HOVER, SpfConfig.THREE_SECTION_LEFT_HOVER_DEFAULT);
        bindHandlerPicker(R.id.three_section_center_hover, SpfConfig.THREE_SECTION_CENTER_HOVER, SpfConfig.THREE_SECTION_CENTER_HOVER_DEFAULT);
        bindHandlerPicker(R.id.three_section_right_hover, SpfConfig.THREE_SECTION_RIGHT_HOVER, SpfConfig.THREE_SECTION_RIGHT_HOVER_DEFAULT);

        bindColorPicker(R.id.bar_color_three_section, SpfConfig.THREE_SECTION_COLOR, SpfConfig.THREE_SECTION_COLOR_DEFAULT);

        updateView();
    }

    private void updateView() {
        setViewBackground(getActivity().findViewById(R.id.bar_color_three_section), config.getInt(SpfConfig.THREE_SECTION_COLOR, SpfConfig.THREE_SECTION_COLOR_DEFAULT));

        updateActionText(R.id.three_section_left_slide, SpfConfig.THREE_SECTION_LEFT_SLIDE, SpfConfig.THREE_SECTION_LEFT_SLIDE_DEFAULT);
        updateActionText(R.id.three_section_center_slide, SpfConfig.THREE_SECTION_CENTER_SLIDE, SpfConfig.THREE_SECTION_CENTER_SLIDE_DEFAULT);
        updateActionText(R.id.three_section_right_slide, SpfConfig.THREE_SECTION_RIGHT_SLIDE, SpfConfig.THREE_SECTION_RIGHT_SLIDE_DEFAULT);
        updateActionText(R.id.three_section_left_hover, SpfConfig.THREE_SECTION_LEFT_HOVER, SpfConfig.THREE_SECTION_LEFT_HOVER_DEFAULT);
        updateActionText(R.id.three_section_center_hover, SpfConfig.THREE_SECTION_CENTER_HOVER, SpfConfig.THREE_SECTION_CENTER_HOVER_DEFAULT);
        updateActionText(R.id.three_section_right_hover, SpfConfig.THREE_SECTION_RIGHT_HOVER, SpfConfig.THREE_SECTION_RIGHT_HOVER_DEFAULT);
    }

    @Override
    protected void restartService() {
        updateView();

        super.restartService();
    }
}

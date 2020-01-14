package com.omarea.gesture;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TabHost;

import com.omarea.gesture.fragments.FragmentBasic;
import com.omarea.gesture.fragments.FragmentEdge;
import com.omarea.gesture.fragments.FragmentOther;
import com.omarea.gesture.fragments.FragmentThreeSection;
import com.omarea.gesture.fragments.FragmentWhiteBar;
import com.omarea.gesture.util.GlobalState;


public class SettingsActivity extends Activity {
    private boolean inited = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gesture_settings);

        try {
            final TabHost tabHost = findViewById(R.id.main_tabhost);
            tabHost.setup();
            tabHost.addTab(tabHost.newTabSpec("Basic")
                    .setContent(R.id.main_tab_0).setIndicator("", getDrawable(R.drawable.gesture_tab_switch)));
            tabHost.addTab(tabHost.newTabSpec("WhiteBar")
                    .setContent(R.id.main_tab_1).setIndicator("", getDrawable(R.drawable.gesture_tab_apple)));
            tabHost.addTab(tabHost.newTabSpec("Edge")
                    .setContent(R.id.main_tab_2).setIndicator("", getDrawable(R.drawable.gesture_tab_lab)));
            tabHost.addTab(tabHost.newTabSpec("ThreeSection")
                    .setContent(R.id.main_tab_3).setIndicator("", getDrawable(R.drawable.gesture_tab_edge)));
            tabHost.addTab(tabHost.newTabSpec("Other")
                    .setContent(R.id.main_tab_4).setIndicator("", getDrawable(R.drawable.gesture_tab_settings)));

            getFragmentManager().beginTransaction()
                    .replace(R.id.main_tab_0, new FragmentBasic()).commit();
            getFragmentManager().beginTransaction()
                    .replace(R.id.main_tab_1, new FragmentWhiteBar()).commit();
            getFragmentManager().beginTransaction()
                    .replace(R.id.main_tab_2, new FragmentEdge()).commit();
            getFragmentManager().beginTransaction()
                    .replace(R.id.main_tab_3, new FragmentThreeSection()).commit();
            getFragmentManager().beginTransaction()
                    .replace(R.id.main_tab_4, new FragmentOther()).commit();
        } catch (Exception ex) {
        }
    }

    private void updateView() {
        try {
            Intent intent = new Intent(getString(R.string.action_config_changed));
            sendBroadcast(intent);
        } catch (Exception ignored) {
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        GlobalState.testMode = true;
        updateView();
    }

    @Override
    protected void onPause() {
        super.onPause();
        GlobalState.testMode = false;
        updateView();
    }


    private void setExcludeFromRecents(boolean excludeFromRecents) {
        try {
            ActivityManager service = (ActivityManager) this.getSystemService(Context.ACTIVITY_SERVICE);
            int taskId = this.getTaskId();
            for (ActivityManager.AppTask task : service.getAppTasks()) {
                if (task.getTaskInfo().id == taskId) {
                    task.setExcludeFromRecents(excludeFromRecents);
                }
            }
        } catch (Exception ex) {
        }
    }

    @Override
    public void onBackPressed() {
        setExcludeFromRecents(true);
        super.onBackPressed();
    }
}

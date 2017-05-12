package com.omarea.shared;

import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Hello on 2017/4/8.
 */

public class AutoClickService {
    ArrayList<String> autoClickKeyWords = new ArrayList<String>() {{
        add("下一步");
        add("下一步");
    }};
    ArrayList<String> autoClickKeyWords2 = new ArrayList<String>() {{
        add("安装");
        add("完成");
    }};

    public AutoClickService() {

    }

    public void packageinstallerAutoClick(AccessibilityEvent event) {
        if ((!ConfigInfo.getConfigInfo().AutoInstall) || (event.getSource() == null))
            return;

        for (int ki = 0; ki < autoClickKeyWords.size(); ki++) {
            List<AccessibilityNodeInfo> next_nodes = event.getSource().findAccessibilityNodeInfosByText(autoClickKeyWords.get(ki));
            if (next_nodes != null && !next_nodes.isEmpty()) {
                AccessibilityNodeInfo node;
                for (int i = 0; i < next_nodes.size(); i++) {
                    node = next_nodes.get(i);
                    if (node.getClassName().toString().toLowerCase().contains("button") && node.isEnabled()) {
                        if(!node.getText().equals(autoClickKeyWords.get(ki)))
                            continue;
                        node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                        try {
                            Thread.sleep(300);
                        } catch (Exception ex) {
                        }
                    }
                }
            }
        }


        for (int ki = 0; ki < autoClickKeyWords.size(); ki++) {

            List<AccessibilityNodeInfo> nodes = event.getSource().findAccessibilityNodeInfosByText(autoClickKeyWords2.get(ki));
            if (nodes != null && !nodes.isEmpty()) {
                AccessibilityNodeInfo node;
                for (int i = 0; i < nodes.size(); i++) {
                    node = nodes.get(i);
                    if (node.getClassName().toString().toLowerCase().contains("button") && node.isEnabled()) {
                        if(!node.getText().equals(autoClickKeyWords2.get(ki)))
                            continue;
                        node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                    }
                }
            }
        }
    }

    public void miuiUsbInstallAutoClick(AccessibilityEvent event) {
        List<AccessibilityNodeInfo> next2_nodes = event.getSource().findAccessibilityNodeInfosByText("继续安装");
        if (next2_nodes != null && !next2_nodes.isEmpty()) {
            AccessibilityNodeInfo node;
            for (int i = 0; i < next2_nodes.size(); i++) {
                node = next2_nodes.get(i);
                if (node.getClassName().toString().toLowerCase().contains("button") && node.isEnabled()) {
                    node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                }
            }
        }
    }
}

package com.omarea.xposed.wx;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import de.robv.android.xposed.XposedBridge;

public class WeChatLayoutAnalyser {
    // 寻找ScanMaskView的下一个节点（微信7.0）
    private RelativeLayout getScanMaskViewNext(View view, int level) {
        if (view instanceof ViewGroup) {
            ViewGroup vp = (ViewGroup) view;
            for (int i = 0; i < vp.getChildCount(); i++) {
                View child = vp.getChildAt(i);
                String className = child.getClass().getName();
                // 输出日志，用于分析Layout层级
                // XposedBridge.log("Scene WeChat " + prefixSpace(level) + className);

                // 根据日志得出的结论 ScanMaskView后面有个适于插入控件的容器
                // 所以找到ScanMaskView后，就返回它的下一个节点
                if (className.equals("com.tencent.mm.plugin.scanner.ui.ScanMaskView")) {
                    if (i + 1 < vp.getChildCount()) {
                        return (RelativeLayout) vp.getChildAt(i + 1);
                    }
                } else {
                    // 遍历子节点
                    RelativeLayout relativeLayout = getScanMaskViewNext(child, level + 1);
                    if (relativeLayout != null) {
                        return relativeLayout;
                    }
                }
            }
        }
        return null;
    }

    // 在ScanSharedMaskView的子节点里找RelativeLayout
    private RelativeLayout getRelativeLayout(ViewGroup scanSharedMaskView) {
        for (int i = 0; i < scanSharedMaskView.getChildCount(); i++) {
            View sc = scanSharedMaskView.getChildAt(i);
            String className2 = sc.getClass().getName();
            if (className2.equals("android.widget.RelativeLayout")) {
                return (RelativeLayout) sc;
            }
        }
        return null;
    }

    // 寻找ScanMaskView的下一个节点（微信8.0）
    private RelativeLayout getScanSharedMaskViewChild(View view, int level) {
        if (view instanceof ViewGroup) {
            ViewGroup vp = (ViewGroup) view;
            for (int i = 0; i < vp.getChildCount(); i++) {
                View child = vp.getChildAt(i);
                String className = child.getClass().getName();
                // 输出日志，用于分析Layout层级
                // XposedBridge.log("Scene WeChat " + prefixSpace(level) + className);

                // 根据日志得出的结论 ScanSharedMaskView里有个适于插入控件的容器
                // 所以找到ScanSharedMaskView后，就返回它里面的一个容器控件
                if (className.equals("com.tencent.mm.plugin.scanner.ui.widget.ScanSharedMaskView")) {
                    return getRelativeLayout((ViewGroup) child);
                } else {
                    // 遍历子节点
                    RelativeLayout relativeLayout = getScanSharedMaskViewChild(child, level + 1);
                    if (relativeLayout != null) {
                        return relativeLayout;
                    }
                }
            }
        }
        return null;
    }

    public RelativeLayout getInjectContainer(Activity wxActivity) {
        XposedBridge.log("Scene WeChat BaseScanUI onResume -> getInjectContainer");

        int versionCode = 1841; // 微信 8.0.1
        View rootView = wxActivity.getWindow().getDecorView();
        try {
            versionCode = wxActivity.getPackageManager().getPackageInfo(wxActivity.getPackageName(), 0).versionCode;
        } catch (Exception ignored) {
        }
        return (versionCode >= 1841) ? getScanSharedMaskViewChild(rootView, 0) : getScanMaskViewNext(rootView, 0);
    }
}

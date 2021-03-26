package com.omarea.xposed;

import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

import de.robv.android.xposed.XposedBridge;

public class DumpView {
    // 填充空格 用于格式输出Layout节点信息
    private String prefixSpace(int count) {
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < count; i++) {
            stringBuilder.append("  ");
        }
        return stringBuilder.toString();
    }

    private List<View> getAllChildViews(View view, int level) {
        List<View> allChildren = new ArrayList<View>();
        if (view instanceof ViewGroup) {
            ViewGroup vp = (ViewGroup) view;
            for (int i = 0; i < vp.getChildCount(); i++) {
                View viewchild = vp.getChildAt(i);
                allChildren.add(viewchild);
                XposedBridge.log("Scene Wechat : " + prefixSpace(level) + viewchild.getClass().getName());
                //再次 调用本身（递归）
                allChildren.addAll(getAllChildViews(viewchild, level + 1));
            }
        }
        return allChildren;
    }
}

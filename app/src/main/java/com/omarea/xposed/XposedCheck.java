package com.omarea.xposed;

import android.util.Log;

/**
 * Created by helloklf on 2017/6/3.
 */

public class XposedCheck {
    //判断Xposed插件是否已经激活（将在Xposed部分中hook返回值为true）
    public static boolean xposedIsRunning() {
        Log.i("SceneXposed", "Inspect Xposed");
        return false;
    }
}

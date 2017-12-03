package com.omarea.xposed;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

/**
 * Created by helloklf on 2017/11/30.
 */

public class WebView {
    public void allowDebug() {
        //强制开启webview调试
        XposedBridge.hookAllConstructors(android.webkit.WebView.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
            XposedHelpers.callStaticMethod(android.webkit.WebView.class, "setWebContentsDebuggingEnabled", true);
            }
        });

        XposedBridge.hookAllMethods(android.webkit.WebView.class, "setWebContentsDebuggingEnabled", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                param.args[0] = true;
            }
        });
    }
}

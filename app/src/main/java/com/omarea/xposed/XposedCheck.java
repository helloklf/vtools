package com.omarea.xposed;

/**
 * Created by helloklf on 2017/6/3.
 */

public class XposedCheck {
    private static int check = 0;

    //判断Xposed插件是否已经激活（将在Xposed部分中hook返回值为true）
    public static boolean xposedIsRunning() {
        check %= 1;
        return false;
    }
}

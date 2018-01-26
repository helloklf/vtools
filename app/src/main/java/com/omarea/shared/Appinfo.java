package com.omarea.shared;

import android.graphics.drawable.Drawable;

/**
 * Created by Hello on 2018/01/26.
 */

public class Appinfo {
    public static Appinfo getItem() {
        return new Appinfo();
    }

    public CharSequence appName = "";
    public CharSequence packageName = "";
    public Drawable icon = null;
    public CharSequence enabledState = "";
    public CharSequence wranState = "";
    public Boolean selectState = false;
    public CharSequence path = "";
    public CharSequence dir = "";
    public Boolean enabled = false;
}

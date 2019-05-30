package com.omarea.vtools.activitys;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.view.View;

import com.omarea.shared.SpfConfig;
import com.omarea.vtools.R;

public class ThemeSwitch {
    private static SharedPreferences globalSPF = null;

    static void switchTheme(Activity activity) {
        if (globalSPF == null) {
            globalSPF = activity.getSharedPreferences(SpfConfig.GLOBAL_SPF, Context.MODE_PRIVATE);
        }

        int theme = globalSPF.getInt(SpfConfig.GLOBAL_SPF_THEME, 1);
        switch (theme) {
            case 0: {
                activity.setTheme(R.style.AppThemeBlue);
                break;
            }
            case 1: {
                activity.setTheme(R.style.AppThemeCyan);
                break;
            }
            case 2: {
                activity.setTheme(R.style.AppThemeGreen);
                break;
            }
            case 3: {
                activity.setTheme(R.style.AppThemeOrange);
                break;
            }
            case 4: {
                activity.setTheme(R.style.AppThemeRed);
                break;
            }
            case 5: {
                activity.setTheme(R.style.AppThemePink);
                break;
            }
            case 6: {
                activity.setTheme(R.style.AppThemePretty);
                break;
            }
            case 7: {
                activity.setTheme(R.style.AppThemeViolet);
                break;
            }
            case 8: {
                activity.setTheme(R.style.AppThemeNoActionBarNight);
                break;
            }
            case 9: {
                activity.setTheme(R.style.AppThemeWhite);
                if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    activity.getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
                }
                break;
            }
        }
    }

    enum Themes {
        BLUE, // 蓝色
        CYAN, // 水鸭青
        GREEN, // 绿色
        ORANAGE, // 成色
        RED, // 红色
        PINK, // 粉色
        PRETTY, // 骚？
        VIOLET, // 紫色
        BLACK // 黑色
    }
}

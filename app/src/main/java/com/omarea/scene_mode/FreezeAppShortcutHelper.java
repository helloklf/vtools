package com.omarea.scene_mode;

import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.AdaptiveIconDrawable;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.graphics.drawable.LayerDrawable;
import android.os.Build;

import androidx.annotation.RequiresApi;

import com.omarea.vtools.R;
import com.omarea.vtools.activities.ActivityQuickStart;

import java.util.ArrayList;
import java.util.List;

public class FreezeAppShortcutHelper {
    private static int requestCount = 0;
    private String prefix = "*";

    /**
     * 移除快捷方式（实践表明，不管什么版本的系统，基本上都不好使）
     *
     * @param context
     * @param packageName
     * @return
     */
    public boolean removeShortcut(Context context, String packageName) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return disableShortcutOreo(context, packageName);
        } else {
            try {
                Intent shortcut = new Intent("com.android.launcher.action.UNINSTALL_SHORTCUT");
                ApplicationInfo applicationInfo = context.getPackageManager().getPackageInfo(packageName, 0).applicationInfo;
                PackageManager packageManager = context.getPackageManager();

                //快捷方式的名称
                shortcut.putExtra(Intent.EXTRA_SHORTCUT_NAME, prefix + applicationInfo.loadLabel(packageManager));//快捷方式的名字

                Intent shortcutIntent = new Intent(Intent.ACTION_MAIN);
                shortcutIntent.addCategory(Intent.CATEGORY_LAUNCHER);
                shortcutIntent.setClassName(context.getApplicationContext(), ActivityQuickStart.class.getName());
                shortcutIntent.putExtra("packageName", packageName);
                shortcut.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
                context.sendBroadcast(shortcut);

                return true;
            } catch (Exception ex) {
                return false;
            }
        }
    }

    public ArrayList<String> getPinnedShortcuts(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return getPinnedShortcutsOreo(context);
        } else {
            /*
            boolean hasInstall = false;
            // 实践表明 基本获取不到已添加的快捷方式数据
            final String AUTHORITY = "com.android.launcher2.settings";
            Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/favorites?notify=true");
             Cursor cursor = context.getContentResolver().query(CONTENT_URI, new String[] { "title", "iconResource" }, "title=?", new String[] { "*" }, null);

            if (cursor != null && cursor.getCount() > 0) {
                hasInstall = true;
            }
            if (cursor != null) {
                cursor.close();
            }
            */
            return new ArrayList<>();
        }
    }

    public boolean createShortcut(Context context, String packageName) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return createShortcutOreo(context, packageName);
        }
        try {
            Intent shortcut = new Intent("com.android.launcher.action.INSTALL_SHORTCUT");
            ApplicationInfo applicationInfo = context.getPackageManager().getPackageInfo(packageName, 0).applicationInfo;
            PackageManager packageManager = context.getPackageManager();

            //快捷方式的名称
            shortcut.putExtra(Intent.EXTRA_SHORTCUT_NAME, prefix + applicationInfo.loadLabel(packageManager));//快捷方式的名字
            shortcut.putExtra("duplicate", false); // 是否允许重复创建

            Bitmap icon = drawableToBitmap(applicationInfo.loadIcon(packageManager));

            //快捷方式的图标
            // shortcut.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, Intent.ShortcutIconResource.fromContext(context, R.drawable.linux));
            shortcut.putExtra(Intent.EXTRA_SHORTCUT_ICON, icon);

            // Intent appIntent = context.getPackageManager().getLaunchIntentForPackage(packageName);

            Intent shortcutIntent = new Intent(Intent.ACTION_MAIN);
            shortcutIntent.addCategory(Intent.CATEGORY_LAUNCHER);
            shortcutIntent.setClassName(context.getApplicationContext(), ActivityQuickStart.class.getName());
            shortcutIntent.putExtra("packageName", packageName);
            // Intent shortcutIntent = new Intent(Intent.ACTION_MAIN);
            // shortcutIntent.setClassName(context.getApplicationContext(), ActivityMain.class.getName());
            shortcut.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
            context.sendBroadcast(shortcut);

            return true;
        } catch (Exception ex) {
            // Toast.makeText(context, "创建快捷方式失败" + ex.getMessage(), Toast.LENGTH_LONG).show();
            return false;
        }
    }

    private Bitmap drawableToBitmap(Drawable drawable) {
        if (drawable instanceof BitmapDrawable) {
            return ((BitmapDrawable) drawable).getBitmap();
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && drawable instanceof AdaptiveIconDrawable) {
            Drawable backgroundDr = ((AdaptiveIconDrawable) drawable).getBackground();
            Drawable foregroundDr = ((AdaptiveIconDrawable) drawable).getForeground();

            Drawable[] drr = new Drawable[2];
            drr[0] = backgroundDr;
            drr[1] = foregroundDr;

            LayerDrawable layerDrawable = new LayerDrawable(drr);

            int width = layerDrawable.getIntrinsicWidth();
            int height = layerDrawable.getIntrinsicHeight();

            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

            Canvas canvas = new Canvas(bitmap);

            layerDrawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
            layerDrawable.draw(canvas);

            return bitmap;
        }
        return null;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private boolean createShortcutOreo(Context context, String packageName) {
        try {
            ShortcutManager shortcutManager = (ShortcutManager) context.getSystemService(Context.SHORTCUT_SERVICE);
            ApplicationInfo applicationInfo = context.getPackageManager().getPackageInfo(packageName, 0).applicationInfo;
            PackageManager packageManager = context.getPackageManager();

            if (shortcutManager.isRequestPinShortcutSupported()) {
                // Intent appIntent = context.getPackageManager().getLaunchIntentForPackage(packageName);

                Intent shortcutIntent = new Intent(Intent.ACTION_MAIN);
                shortcutIntent.setClassName(context.getApplicationContext(), ActivityQuickStart.class.getName());
                shortcutIntent.putExtra("packageName", packageName);

                Bitmap icon = drawableToBitmap(applicationInfo.loadIcon(packageManager));

                final ShortcutInfo info = new ShortcutInfo.Builder(context, packageName)
                        .setIcon(Icon.createWithBitmap(icon))
                        .setShortLabel(prefix + applicationInfo.loadLabel(packageManager))
                        .setIntent(shortcutIntent)
                        .setActivity(new ComponentName(context, ActivityQuickStart.class)) // 只有“主要”活动 - 定义过滤器Intent#ACTION_MAIN 和Intent#CATEGORY_LAUNCHER意图过滤器的活动 - 才能成为目标活动
                        .build();

                //当添加快捷方式的确认弹框弹出来时，将被回调
                Intent callback = new Intent(context, ReceiverShortcut.class);
                callback.setAction(context.getString(R.string.scene_create_shortcut_action));
                callback.putExtra("packageName", packageName);

                requestCount++;
                PendingIntent shortcutCallbackIntent = PendingIntent.getBroadcast(context, requestCount, callback, PendingIntent.FLAG_CANCEL_CURRENT);
                // shortcutManager.removeAllDynamicShortcuts();
                if (shortcutManager.isRequestPinShortcutSupported()) {
                    List<ShortcutInfo> items = shortcutManager.getPinnedShortcuts();
                    for (ShortcutInfo item : items) {
                        if (item.getId().equals(info.getId())) {
                            shortcutManager.updateShortcuts(new ArrayList<ShortcutInfo>() {{
                                add(info);
                            }});
                            return true;
                        }
                    }
                    shortcutManager.requestPinShortcut(info, shortcutCallbackIntent.getIntentSender());
                    // shortcutManager.getPinnedShortcuts();
                    return true;
                } else {
                    return false;
                }
            }
            return true;
        } catch (Exception ex) {
            // Toast.makeText(context, "处理快捷方式失败" + ex.getMessage(), Toast.LENGTH_LONG).show();
            return false;
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private boolean disableShortcutOreo(Context context, final String packageName) {
        try {
            ShortcutManager shortcutManager = (ShortcutManager) context.getSystemService(Context.SHORTCUT_SERVICE);

            shortcutManager.removeDynamicShortcuts(new ArrayList<String>() {{
                add(packageName);
            }});
            shortcutManager.disableShortcuts(new ArrayList<String>() {{
                add(packageName);
            }});
            return true;
        } catch (Exception ex) {
            // Toast.makeText(context, "处理快捷方式失败" + ex.getMessage(), Toast.LENGTH_LONG).show();
            return false;
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private ArrayList<String> getPinnedShortcutsOreo(Context context) {
        ArrayList<String> packages = new ArrayList<String>();
        try {
            ShortcutManager shortcutManager = (ShortcutManager) context.getSystemService(Context.SHORTCUT_SERVICE);
            List<ShortcutInfo> items = shortcutManager.getPinnedShortcuts();
            for (ShortcutInfo shortcutInfo : items) {
                CharSequence shortLabel = shortcutInfo.getShortLabel();
                if (shortLabel != null && shortLabel.toString().startsWith(prefix)) {
                    packages.add(shortcutInfo.getId());
                }
            }
        } catch (Exception ignored) {
        }
        return packages;
    }
}

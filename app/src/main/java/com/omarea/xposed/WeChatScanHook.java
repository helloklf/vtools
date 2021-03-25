package com.omarea.xposed;

import android.app.Activity;
import android.graphics.Color;
import android.hardware.Camera;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class WeChatScanHook {
    // 填充空格 用于格式输出Layout节点信息
    private String prefixSpace(int count) {
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < count; i++) {
            stringBuilder.append("  ");
        }
        return stringBuilder.toString();
    }

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

    // 打印所有后置摄像头的Id
    private void dumpCameraList() {
        // 枚举所有摄像头
        XposedBridge.log("Scene: 摄像头数量 " + Camera.getNumberOfCameras());
        for (int cameraId = 0; cameraId < Camera.getNumberOfCameras(); cameraId++) {
            Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
            Camera.getCameraInfo(cameraId, cameraInfo);
            // 如果是后置摄像头
            if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                XposedBridge.log("Scene [Dump CameraInfo] cameraId: " + cameraId);
            }
        }
    }

    private int hackParam = -1; // -1 表示不hook cameraId
    private boolean valueKeepOnece = false;
    private void setCameraIdHook (int cameraId) {
        hackParam = cameraId;
        valueKeepOnece = true;
        XposedBridge.log("Scene: 切换摄像头 " + cameraId);
    }
    private int getCameraIdHook() {
        return hackParam;
    }
    private void resetHooK() {
        if (valueKeepOnece) {
            valueKeepOnece = false;
        } else {
            setCameraIdHook(-1);
        }
    }

    public void hook(final XC_LoadPackage.LoadPackageParam loadPackageParam) {
        XposedHelpers.findAndHookMethod(Camera.class, "open", int.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                super.beforeHookedMethod(param);
                int targetCameraId = getCameraIdHook();

                if (targetCameraId > -1) {
                    param.args[0] = targetCameraId;
                }

                XposedBridge.log("Scene: 微信启动相机 CameraId [" + param.args[0] + "] Total: " + Camera.getNumberOfCameras());
            }
        });

        /*
        // 进入扫码页（微信7.0可用，8.0 hook不到）
        XposedHelpers.findAndHookMethod(
                "com.tencent.mm.plugin.scanner.ui.BaseScanUI",
                loadPackageParam.classLoader,
                "onResume", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        super.afterHookedMethod(param);

                        scanActivityInject(param);
                    }
                });
         */

        // hook所有Activity再过滤扫码页（微信8.0也可用）
        XposedHelpers.findAndHookMethod(Activity.class, "onResume", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                super.beforeHookedMethod(param);
                String className = param.thisObject.getClass().getName();
                if (className.equals("com.tencent.mm.plugin.scanner.ui.BaseScanUI")) {
                    scanActivityInject(param);
                }
            }
        });


        /*

        // 离开扫码页（微信7.0可用）
        XposedHelpers.findAndHookMethod(
                "com.tencent.mm.plugin.scanner.ui.BaseScanUI",
                loadPackageParam.classLoader,
                "onPause", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        super.afterHookedMethod(param);

                        // 离开扫码页面后，还原Hook参数，以免影响其它页面调用摄像头
                        resetHooK();
                    }
                });
        */

        // hook所有Activity再过滤扫码页（微信8.0也可用）
        XposedHelpers.findAndHookMethod(Activity.class, "onPause", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                super.beforeHookedMethod(param);
                String className = param.thisObject.getClass().getName();
                if (className.equals("com.tencent.mm.plugin.scanner.ui.BaseScanUI")) {
                    // 离开扫码页面后，还原Hook参数，以免影响其它页面调用摄像头
                    resetHooK();
                }
            }
        });

        /*
        XposedHelpers.findAndHookMethod(Activity.class, "onResume", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                super.beforeHookedMethod(param);
                String className = param.thisObject.getClass().getName();
                if (className.equals("com.tencent.mm.plugin.scanner.ui.BaseScanUI")) {
                    XposedBridge.log("Scene WeChat BaseScanUI onResume2");
                }
                XposedBridge.log("Scene: Activity onResume [" + className + "]");
            }
        });
        */
    }

    private void scanActivityInject(XC_MethodHook.MethodHookParam param) {
        final Activity activity = (Activity) param.thisObject;

        String version = "8.0.0";
        try {
            version = activity.getPackageManager().getPackageInfo(activity.getPackageName(), 0).versionName;
        } catch (Exception ignored) {}
        XposedBridge.log("Scene WeChat (" + version + ") BaseScanUI onResume");

        View rootView = activity.getWindow().getDecorView();
        boolean gt7 = version.startsWith("8");

        // 找到一个合适插入按钮的容器
        RelativeLayout container = gt7 ? getScanSharedMaskViewChild(rootView, 0) : getScanMaskViewNext(rootView, 0);
        if (container != null) {
            dumpCameraList();

            // 创建一个按钮设置外观样式
            TextView textView = new TextView(activity);
            textView.setTextColor(Color.YELLOW);
            textView.setPadding(30, 30, 30, 30);
            RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(
                    RelativeLayout.LayoutParams.WRAP_CONTENT,
                    RelativeLayout.LayoutParams.WRAP_CONTENT
            );
            layoutParams.addRule(RelativeLayout.CENTER_IN_PARENT);

            final int cameraId = getCameraIdHook();

            if (cameraId == 0 || cameraId == -1) {
                textView.setText("0.6x (1x) 2x 4x");
            } else if (cameraId == 2) {
                textView.setText("0.6x 1x (2x) 4x");
            } else if (cameraId == 3) {
                textView.setText("(0.6x) 1x 2x 4x");
            } else if (cameraId == 5) {
                textView.setText("0.6x 1x 2x (4x)");
            }

            // 设置点击后切换摄像头
            textView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // Mi9      0 广角，2 长焦， 3 超广角
                    // CC9Pro   0 广角  2 长焦  3 超广角  4 微距  5 超长焦
                    if (cameraId == 0 || cameraId == -1) {
                        setCameraIdHook(2);
                    } else if (cameraId == 2) {
                        setCameraIdHook(5);
                    } else if (cameraId == 5) {
                        setCameraIdHook(3);
                    } else if (cameraId == 3) {
                        setCameraIdHook(0);
                    }
                    // 其实就是改变hook参数并重启activity啦
                    activity.recreate();
                }
            });
            container.addView(textView, layoutParams);
        }
    }
}

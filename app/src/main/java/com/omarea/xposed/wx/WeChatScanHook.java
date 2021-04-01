package com.omarea.xposed.wx;

import android.app.Activity;
import android.graphics.Color;
import android.hardware.Camera;
import android.os.Build;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class WeChatScanHook {


    public boolean supported() {
        if (CameraHookProvider.devices.contains(Build.MODEL)) {
            return Camera.getNumberOfCameras() > 2;
        }
        return false;
    }

    public void hook(final XC_LoadPackage.LoadPackageParam loadPackageParam) {
        if (supported()) {
            // hook 相机启动，以便于更改目标相机id
            XposedHelpers.findAndHookMethod(Camera.class, "open", int.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    super.beforeHookedMethod(param);
                    VirtualCameraInfo targetCamera = cameraHookProvider.getCameraIdHook();
                    param.args[0] = targetCamera.cameraId;

                    XposedBridge.log("Scene: 微信启动相机 CameraId [" + param.args[0] + "] Total: " + Camera.getNumberOfCameras());
                }
            });

            // hook所有Activity再过滤扫码页（微信7.0，8.0 测试可用）
            XposedHelpers.findAndHookMethod(Activity.class, "onResume", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    super.beforeHookedMethod(param);
                    String className = param.thisObject.getClass().getName();
                    if (className.equals("com.tencent.mm.plugin.scanner.ui.BaseScanUI")) {
                        scanActivityInject(param);
                    }
                    // XposedBridge.log("Scene: Activity onResume [" + className + "]");
                }
            });


            // hook所有Activity再过滤扫码页（微信7.0，8.0 测试可用）
            XposedHelpers.findAndHookMethod(Activity.class, "onPause", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    super.beforeHookedMethod(param);
                    String className = param.thisObject.getClass().getName();
                    if (className.equals("com.tencent.mm.plugin.scanner.ui.BaseScanUI")) {
                        // 离开扫码页面后，还原Hook参数，以免影响其它页面调用摄像头
                        cameraHookProvider.resetHooK();
                    }
                }
            });
        }
    }

    private final CameraHookProvider cameraHookProvider = new CameraHookProvider();
    private final WeChatLayoutAnalyser weChatLayoutAnalyser = new WeChatLayoutAnalyser();

    // 向微信界面注入摄像头切换按钮
    private void scanActivityInject(XC_MethodHook.MethodHookParam param) {
        if (cameraHookProvider.cameraList.length > 1) {

            final Activity activity = (Activity) param.thisObject;
            // 找到一个合适插入按钮的容器
            RelativeLayout container = weChatLayoutAnalyser.getInjectContainer(activity);
            if (container != null) {
                TextView textView = createControls(container);

                // 设置点击后切换摄像头
                textView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        cameraHookProvider.setCameraIdHook(
                                cameraHookProvider.getCameraIdHookNext()
                        );

                        // 其实就是改变hook参数并重启activity啦
                        activity.recreate();
                    }
                });
            }
        }
    }

    // 创建按钮并添加到容器
    private TextView createControls(ViewGroup container) {// 创建一个按钮设置外观样式
        TextView textView = new TextView(container.getContext());
        textView.setTextColor(Color.WHITE);
        textView.setPadding(100, 0, 100, 0);
        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT
        );
        textView.setTextSize(40);
        layoutParams.addRule(RelativeLayout.CENTER_IN_PARENT);

        // 显示当前变焦倍率
        textView.setText(
                cameraHookProvider.getCameraIdHook().cameraName
        );

        container.addView(textView, layoutParams);

        return textView;
    }
}

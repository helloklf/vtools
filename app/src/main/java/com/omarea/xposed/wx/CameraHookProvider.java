package com.omarea.xposed.wx;

import android.hardware.Camera;
import android.os.Build;

import java.util.ArrayList;

import de.robv.android.xposed.XposedBridge;

public class CameraHookProvider {
    public static ArrayList<String> devices = new ArrayList<String>() {{
            add("MI 9");
            add("MI CC9 Pro");
            // 目前 Mi 10 Pro还没测试通过
            // add("Mi 10 Pro");
    }};

    public VirtualCameraInfo[] cameraList;
    private int defaultCameraIndex;

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

    public CameraHookProvider() {

        String model = Build.MODEL;
        switch (model) {
            case "MI 9": {
                // 相机列表因机型而异
                // Mi9      0 广角，2 长焦， 3 超广角
                this.cameraList = new VirtualCameraInfo[]{
                        // Gnew VirtualCameraInfo(3, 0.6), // 超广角扫码没啥意义，去掉
                        new VirtualCameraInfo(0, "❶"),
                        new VirtualCameraInfo(2, "❷")
                };
                this.defaultCameraIndex = 0; // 默认摄像头在cameraList中的索引
                break;
            }
            case "MI CC9 Pro": {
                // 相机列表因机型而异
                // CC9Pro   0 广角  2 长焦  3 超广角  4 微距  5 超长焦
                this.cameraList = new VirtualCameraInfo[]{
                        // new VirtualCameraInfo(3, 0.6), // 超广角扫码没啥意义，去掉
                        new VirtualCameraInfo(0, "❶"),
                        new VirtualCameraInfo(2, "❷"),
                        new VirtualCameraInfo(5, "❹"),
                };
                this.defaultCameraIndex = 0; // 默认摄像头在cameraList中的索引
                break;
            }
            case "Mi 10 Pro": {
                this.cameraList = new VirtualCameraInfo[]{
                        new VirtualCameraInfo(0, "❶")
                };
                this.defaultCameraIndex = 0; // 默认摄像头在cameraList中的索引
                break;
            }
            default: {
                this.cameraList = new VirtualCameraInfo[]{
                        new VirtualCameraInfo(0, 1.0),
                };
                this.defaultCameraIndex = 0; // 默认摄像头在cameraList中的索引
                dumpCameraList();
                break;
            }
        }
    }

    private int hackCameraIndex = -1; // -1 表示默认
    private boolean valueKeepOnece = false;

    public void setCameraIdHook(int cameraIndex) {
        hackCameraIndex = cameraIndex;
        valueKeepOnece = true;
        XposedBridge.log("Scene: 切换摄像头 " + cameraIndex);
    }

    public VirtualCameraInfo getCameraIdHook() {
        if (hackCameraIndex > -1) {
            return cameraList[hackCameraIndex];
        }
        return cameraList[defaultCameraIndex];
    }

    public int getCameraIdHookNext() {
        if (hackCameraIndex > -1) {
            return (hackCameraIndex + 1) % cameraList.length;
        } else {
            return (defaultCameraIndex + 1) % cameraList.length;
        }
    }

    public void resetHooK() {
        if (valueKeepOnece) {
            valueKeepOnece = false;
        } else {
            setCameraIdHook(-1);
        }
    }
}

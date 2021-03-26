package com.omarea.xposed.wx;

public class VirtualCameraInfo {
    public VirtualCameraInfo(int cameraId, double zoomRatio) {
        this.cameraId = cameraId;
        this.zoomRatio = zoomRatio;
        this.cameraName = "" + zoomRatio + "×";
    }
    public VirtualCameraInfo(int cameraId, String cameraName) {
        this.cameraId = cameraId;
        this.cameraName = cameraName;
    }

    public int cameraId;
    private double zoomRatio; // 缩放比例（100%）
    public String cameraName;
}

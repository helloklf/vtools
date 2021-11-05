package com.omarea.library.device;

import android.annotation.SuppressLint;
import android.opengl.GLSurfaceView;
import android.util.Log;
import android.view.ViewGroup;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class GpuInfo {
    public String glVersion;
    public String glVendor;
    public String glRender;
    public String glExtensions;

    public interface GpuInfoHandler {
        void onSurfaceCreated(GpuInfo gpuInfo);
    }

    // 注意：container必须是可见的，且高度宽度不能为0，否则回调不会被执行
    public static void getGpuInfo(ViewGroup container, GpuInfoHandler gpuInfoHandler) {
        GpuInfoView gpuInfoView = new GpuInfoView(container, gpuInfoHandler);
        container.removeAllViews();
        container.addView(gpuInfoView);
    }

    @SuppressLint("ViewConstructor")
    static class GpuInfoView extends GLSurfaceView {
        public GpuInfoView(final ViewGroup container, final GpuInfoHandler gpuInfoHandler) {
            super(container.getContext());
            final GpuInfoView view = this;
            setEGLConfigChooser(8, 8, 8, 8, 0, 0);
            DemoRenderer mRenderer = new DemoRenderer(new GpuInfoHandler() {
                @Override
                public void onSurfaceCreated(final GpuInfo gpuInfo) {
                    container.getRootView().post(new Runnable() {
                        @Override
                        public void run() {
                        container.removeView(view);
                        gpuInfoHandler.onSurfaceCreated(gpuInfo);
                        }
                    });
                }
            });
            setEGLContextClientVersion(2);
            setRenderer(mRenderer);

            container.addView(this);
        }
    }

    static class DemoRenderer implements GLSurfaceView.Renderer {
        private final GpuInfoHandler gpuInfoHandler;

        public DemoRenderer(GpuInfoHandler gpuInfoHandler) {
            this.gpuInfoHandler = gpuInfoHandler;
        }

        public void onSurfaceCreated(final GL10 gl, EGLConfig config) {
            // Log.d("SystemInfo", "GL_RENDERER = " + gl.glGetString(GL10.GL_RENDERER));
            // Log.d("SystemInfo", "GL_VENDOR = " + gl.glGetString(GL10.GL_VENDOR));
            // Log.d("SystemInfo", "GL_VERSION = " + gl.glGetString(GL10.GL_VERSION));
            // Log.i("SystemInfo", "GL_EXTENSIONS = " + gl.glGetString(GL10.GL_EXTENSIONS));
            gpuInfoHandler.onSurfaceCreated(new GpuInfo() {{
                glRender = gl.glGetString(GL10.GL_RENDERER);
                glVendor = gl.glGetString(GL10.GL_VENDOR);
                glVersion = gl.glGetString(GL10.GL_VERSION);
                glExtensions = gl.glGetString(GL10.GL_EXTENSIONS);
            }});
        }


        @Override
        public void onDrawFrame(GL10 arg0) {
        }

        @Override
        public void onSurfaceChanged(GL10 arg0, int arg1, int arg2) {
        }
    }
}

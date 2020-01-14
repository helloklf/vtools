package com.omarea.gesture.shell;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.util.Log;

import com.omarea.gesture.util.GlobalState;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;

public class ScreenColor1 {
    private static final Object threadRun = "";
    private static Process exec;
    private static Thread thread;
    private static long updateTime = 0;
    private static boolean hasNext = false;

    public static void updateBarColor(boolean hasNext) {
        // 如果距离上次执行已经超过6秒，认位颜色获取进程已经崩溃，将其结束重启
        if (updateTime > -1 && System.currentTimeMillis() - updateTime > 6000) {
            try {
                if (exec != null) {
                    exec.destroy();
                    exec = null;
                }
            } catch (Exception ignored) {
            }
            try {
                if (thread != null) {
                    thread.interrupt();
                    thread = null;
                }
            } catch (Exception ignored) {
            }
        }
        ScreenColor1.hasNext = hasNext;

        if (thread == null) {
            thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    do {
                        try {
                            updateTime = System.currentTimeMillis();
                            long start = System.currentTimeMillis();
                            if (new ScreenColor1().screenIsLightColor()) {
                                Log.d(">>>>", "变黑色");
                                GlobalState.iosBarColor = Color.BLACK;
                            } else {
                                Log.d(">>>>", "变白色");
                                GlobalState.iosBarColor = Color.WHITE;
                            }
                            if (GlobalState.updateBar != null) {
                                GlobalState.updateBar.run();
                            }
                            Log.d(">>>>", "time " + (System.currentTimeMillis() - start));
                            updateTime = -1;
                        } catch (Exception ignored) {
                        }
                        try {
                            synchronized (threadRun) {
                                if (ScreenColor1.hasNext) {
                                    thread.wait(1000);
                                    ScreenColor1.hasNext = false;
                                } else {
                                    threadRun.wait();
                                }
                            }
                        } catch (Exception ex) {
                            break;
                        }
                    } while (true);
                    thread = null;
                }
            });
            thread.start();
        } else {
            synchronized (threadRun) {
                threadRun.notify();
            }
        }
    }

    public boolean screenIsLightColor() {
        int pixel = getScreenBottomColor();
        int r = Color.red(pixel);
        int g = Color.green(pixel);
        int b = Color.blue(pixel);

        Log.d(">>>>", "rgb(" + r + "," + g + "," + b + ")");

        return (r > 180 && g > 180 && b > 180);
    }

    private boolean isWhiteTopColor(byte[] rawImage) {
        int r = 0, g = 0, b = 0, a = 0;
        int index = 12 + (540 * 4); // 前面12位不属于像素信息，跳过12位，并以屏幕分辨率位1080p取顶部中间那个像素的颜色（32位色，每个像素4byte）
        r = rawImage[index];
        g = rawImage[index + 1];
        b = rawImage[index + 2];
        a = rawImage[index + 3];
        if (r == -1) {
            r = 255;
        }
        if (g == -1) {
            g = 255;
        }
        if (b == -1) {
            b = 255;
        }
        return (r > 180 && b > 180 && g > 180);
    }

    private int getScreenBottomColor() {
        boolean usePng = false;
        byte[] bytes = getScreenCapBytes(usePng);
        if (usePng) {
            Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
            int y = bitmap.getHeight() - 15;
            int x = bitmap.getWidth() / 2 - 1;
            return bitmap.getPixel(x, y);
        } else {
            // 如果状态栏都是白色的，那这个界面肯定是白色啦
            if (isWhiteTopColor(bytes)) {
                return Color.WHITE;
            }

            int r = 0, g = 0, b = 0, a = 0;
            // int index = 12; // 1080 * (2340 - 15) + 540;
            int index = bytes.length - 8;// 后面4byte 不知道是什么，总之也不是像素信息
            r = bytes[index];
            g = bytes[index + 1];
            b = bytes[index + 2];
            a = bytes[index + 3];
            if (r == -1) {
                r = 255;
            }
            if (g == -1) {
                g = 255;
            }
            if (b == -1) {
                b = 255;
            }
            Log.d(">>>>", "raw:" + bytes.length);
            Log.d(">>>>", "raw rgba(" + r + "," + g + "," + b + "," + a + ")");
            return Color.argb(a, r, g, b);
        }
    }

    /***
     *
     * @param usePng 是否编码为png格式
     * @return
     */
    private byte[] getScreenCapBytes(boolean usePng) {
        int cacheSize = 1024 * 1024 * 4; // 4M
        byte[] tempBuffer = new byte[cacheSize];
        int size = GlobalState.displayHeight * GlobalState.displayWidth * 4 + (12 + 4); // 截图大小为 分辨率 * 32bit(4Byte) + 头尾(16Byte)
        ByteBuffer byteBuffer = ByteBuffer.allocate(size);
        //LogUtils.i("获取屏幕图片bytes");

        try {
            if (exec == null) {
                exec = Runtime.getRuntime().exec("su");
            }

            OutputStream outputStream = exec.getOutputStream();
            if (usePng) {
                outputStream.write("screencap -p 2> /dev/null\n".getBytes());
            } else {
                outputStream.write("screencap 2> /dev/null\n".getBytes());
            }
            outputStream.flush();
            try {
                final InputStream inputStream = exec.getInputStream();
                BufferedInputStream bufferedInputStream = new BufferedInputStream(inputStream);
                int count;
                int totalCount = 0;
                while ((count = bufferedInputStream.read(tempBuffer)) > 0) {
                    totalCount += count;
                    if (count > byteBuffer.remaining()) {
                        Log.e(">>>>", "" + totalCount);
                        exec.destroy();
                        byteBuffer.put(tempBuffer, 0, byteBuffer.remaining());
                    } else {
                        byteBuffer.put(tempBuffer, 0, count);
                    }
                    if (totalCount == size) {
                        break;
                    }
                }
                Log.d(">>>>", "frame end");
            } catch (final IOException e) {
                Log.d(">>>>", "frame IOException");
                exec.destroy();
                exec = null;
            }
        } catch (IOException e) {
            Log.d(">>>>", "frame IOException");
            exec.destroy();
            exec = null;
        }
        byteBuffer.flip();
        byte[] out = new byte[byteBuffer.limit()];
        byteBuffer.get(out, 0, out.length);
        // byteBuffer.reset();
        return out;
    }
}
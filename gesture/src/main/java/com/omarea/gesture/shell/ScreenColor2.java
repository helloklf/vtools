package com.omarea.gesture.shell;

import android.graphics.Color;
import android.util.Log;

import com.omarea.gesture.util.GlobalState;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;

public class ScreenColor2 {
    private static final Object threadRun = "";
    private static final Object screencapRead = "";
    private static ScreenCapThread thread;
    private static long updateTime = 0;
    private static boolean hasNext = false;
    private static boolean notifyed = false; // 是否已经notify过，并且还未进入wait清除notifyed状态，避免多次notify进入队列
    private static boolean discardBuffer = true; // 读取输出流之前，是否要舍弃缓冲区已经读取的内容

    static class ScreenCapThread extends Thread {
        private static Process exec;

        @Override
        public void interrupt() {
            try {
                if (exec != null) {
                    exec.destroy();
                    exec = null;
                }
            } catch (Exception ignored) {
            }
            super.interrupt();
        }

        @Override
        public void run() {
            do {
                try {
                    Thread.sleep(100);
                    updateTime = System.currentTimeMillis();
                    long start = System.currentTimeMillis();
                    updateTime = -1;
                    writeCommand();
                    try {
                        synchronized (screencapRead) {
                            screencapRead.wait(2500);
                            discardBuffer = true;
                        }
                    } catch (Exception ignored) {
                    }
                    Log.d(">>>>", "time " + (System.currentTimeMillis() - start));
                } catch (Exception ignored) {
                }
                notifyed = false;
                try {
                    synchronized (threadRun) {
                        if (hasNext) {
                            threadRun.wait(1000);
                            hasNext = false;
                        } else {
                            threadRun.wait();
                        }
                    }
                } catch (Exception ex) {
                    break;
                }
            } while (true);
            interrupt();
        }

        private void writeCommand() {
            try {
                if (exec == null) {
                    exec = Runtime.getRuntime().exec("su");
                    new ReadThread(exec.getInputStream()).start();
                }

                Log.e(">>>>", "screencap");
                OutputStream outputStream = exec.getOutputStream();
                outputStream.write("screencap 2> /dev/null\n".getBytes());

                outputStream.flush();

            } catch (IOException e) {
                Log.d(">>>>", "frame IOException");
                exec.destroy();
                exec = null;
            }
        }
    }

    public static void updateBarColor(boolean hasNext) {
        // 如果距离上次执行已经超过6秒，认位颜色获取进程已经崩溃，将其结束重启
        if (updateTime > -1 && System.currentTimeMillis() - updateTime > 6000) {
            stopProcess();
        }
        ScreenColor2.hasNext = true;

        if (thread != null && !thread.isAlive()) {
            Log.e(">>>> ", " 获取线程状态有点奇怪");
        }
        if (thread != null && thread.isAlive() && !thread.isInterrupted()) {
            synchronized (threadRun) {
                if (!notifyed && (thread.getState() == Thread.State.WAITING || thread.getState() == Thread.State.TIMED_WAITING)) {
                    threadRun.notify();
                    notifyed = true;
                }
            }
        } else {
            stopProcess();

            thread = new ScreenCapThread();
            thread.start();
        }
    }

    public static void stopProcess() {
        if (thread != null) {
            try {
                Log.e(">>>>", "分辨率改变重启取色进程");
                thread.interrupt();
                thread = null;
                notifyed = false;
            } catch (Exception ignored) {
            }
        }
    }

    static class ReadThread extends Thread {
        private BufferedInputStream bufferedInputStream;

        ReadThread(InputStream inputStream) {
            bufferedInputStream = new BufferedInputStream(inputStream);
        }

        private void setBarColor(byte[] bytes) {
            if (bytes.length == 0) {
                Log.e(">>>>", "Size is Zero");
                return;
            }

            boolean isLightColor = false;
            // 如果状态栏都是白色的，那这个界面肯定是白色啦
            // 前面12位不属于像素信息，跳过12位，并以屏幕分辨率位1080p取顶部中间那个像素的颜色（32位色，每个像素4byte）
            // TODO:状态栏取色，考虑是否要继续使用
            // if (pixelIsLightColor(bytes, 12 + ((GlobalState.displayWidth / 2) * 4))) {
            //     isLightColor = true;
            // }
            if (!isLightColor) {
                isLightColor = pixelIsLightColor(bytes, bytes.length - 8); // 后面4byte 不知道是什么，总之也不是像素信息
            }

            if (isLightColor) {
                Log.d(">>>>", "变黑色");
                GlobalState.iosBarColor = Color.BLACK;
            } else {
                Log.d(">>>>", "变白色");
                GlobalState.iosBarColor = Color.WHITE;
            }

            if (GlobalState.updateBar != null) {
                GlobalState.updateBar.run();
            }

            try {
                synchronized (screencapRead) {
                    screencapRead.notify();
                }
            } catch (Exception ignored) {
            }

            updateTime = -1;
        }

        private boolean pixelIsLightColor(byte[] rawImage, int index) {
            if (index > -1 && (index + 3) < rawImage.length) {
                int r = 0, g = 0, b = 0, a = 0;
                r = rawImage[index];
                g = rawImage[index + 1];
                b = rawImage[index + 2];
                a = rawImage[index + 3];
                if (r < 0) {
                    r = 255;
                }
                if (g < 0) {
                    g = 255;
                }
                if (b < 0) {
                    b = 255;
                }
                return (r > 180 && b > 180 && g > 180);
            }
            return false;
        }

        @Override
        public void run() {
            int cacheSize = 1024 * 1024 * 4; // 4M
            byte[] tempBuffer = new byte[cacheSize];
            int size = GlobalState.displayHeight * GlobalState.displayWidth * 4 + (12 + 4); // 截图大小为 分辨率 * 32bit(4Byte) + 头尾(16Byte)
            ByteBuffer byteBuffer = ByteBuffer.allocate(size);

            try {
                int count;
                while ((count = bufferedInputStream.read(tempBuffer)) > 0) {
                    if (discardBuffer) {
                        byteBuffer.clear();
                        discardBuffer = false;
                    }

                    int remaining = byteBuffer.remaining();
                    if (count > remaining) { // 读取了超过一帧
                        byteBuffer.put(tempBuffer, 0, remaining);

                        // 更新颜色
                        setBarColor(byteBuffer.array());
                        byteBuffer.clear();

                        // 把剩余的部分写入缓冲区
                        byteBuffer.put(tempBuffer, remaining, tempBuffer.length - remaining);
                    } else if (count == remaining) { // 刚好读满一帧
                        byteBuffer.put(tempBuffer, 0, count);

                        // 更新颜色
                        setBarColor(byteBuffer.array());

                        discardBuffer = true;
                    } else { // 不到一帧，继续读
                        byteBuffer.put(tempBuffer, 0, count);
                    }
                }
            } catch (final IOException e) {
                Log.d(">>>>", "frame IOException");
            }
        }
    }
}
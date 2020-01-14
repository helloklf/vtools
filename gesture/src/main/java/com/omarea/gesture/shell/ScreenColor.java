package com.omarea.gesture.shell;

import android.graphics.Color;
import android.os.Build;
import android.util.Log;

import com.omarea.gesture.util.GlobalState;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;

public class ScreenColor {
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
                    writeCommand();
                    try {
                        synchronized (screencapRead) {
                            screencapRead.wait(2500);
                            discardBuffer = true;
                        }
                    } catch (Exception ignored) {
                    }
                    updateTime = -1;
                    Log.d(">>>>", "time " + (System.currentTimeMillis() - start));
                } catch (Exception ignored) {
                }
                notifyed = false;
                try {
                    synchronized (threadRun) {
                        if (hasNext) {
                            threadRun.wait(600);
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
        ScreenColor.hasNext = true;

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

        // 像素采集
        private static class PixelGather {
            private int frameSize;
            private int position;
            private static final int fileHeader = 12; // 文件头部长度
            private static final int fileFooter = (Build.VERSION.SDK_INT > Build.VERSION_CODES.O) ? 4 : 0; // 文件脚部长度（Android 8.1 即SDK27 以前没有这4Byte！）
            private ByteBuffer buffer; // 真实的缓冲区
            // 除了头部和脚部，则每4Byte(RGBA)代表一个像素，例如左上角第一个像素就是 bytes[16] ~ bytes[19]

            // 用于取样的像素在帧数据中的位置
            // 采样的像素点数量建议设为 单数，因为最终会对比 暗色/亮色 点的数量
            private int[] samplingPixel = {
                    fileHeader + (GlobalState.displayWidth / 4 * 4), // y: 0, x: 0.25
                    fileHeader + (GlobalState.displayWidth / 4 * 3 * 4), // y: 0, x: 0.75
                    fileHeader + (((GlobalState.displayWidth * GlobalState.displayHeight) - (GlobalState.displayWidth / 4)) * 4), // y: 1, x : 0.25
                    fileHeader + (((GlobalState.displayWidth * GlobalState.displayHeight) - (GlobalState.displayWidth / 2)) * 4), // y: 1, x : 0.5
                    fileHeader + (((GlobalState.displayWidth * GlobalState.displayHeight) - (GlobalState.displayWidth / 4 * 3)) * 4) // y: 1, x : 0.75
            };

            private PixelGather(int frameSize) {
                this.frameSize = frameSize;
                // 32位的像素，每个占用 4 字节
                this.buffer = ByteBuffer.allocate(samplingPixel.length * 4);
            }

            static PixelGather frameBuffer(int height, int width) {
                return new PixelGather(fileHeader + (height * width * 4) + fileFooter);
            }

            void put(byte[] bytes, int offset, int count) {
                if (position + count > frameSize) {
                    throw new IndexOutOfBoundsException("数据写入量超出缓冲区可用空间");
                }
                if (count != 0) {
                    int rangeLeft = position;
                    int rangeRight = position + count;

                    for (int pixel : samplingPixel) {
                        sampling(bytes, offset, count, pixel);
                    }
                }
                position += count;
            }

            void sampling(byte[] bytes, int offset, int count, int pixel) {
                int rangeLeft = position;
                int rangeRight = position + count;

                // 判断像素是否在区域内
                if (pixel + 3 >= rangeLeft && pixel + 3 <= rangeRight) {
                    // Log.d("AAAAA", "position " + position + "  rangeLeft " + rangeLeft + " rangeRight" + rangeRight);
                    int targetIndex = position > pixel ? position : pixel;
                    for (; targetIndex <= pixel + 3; targetIndex++) {
                        buffer.put(bytes[targetIndex - position + offset]);
                    }
                }
            }

            int remaining() {
                return this.frameSize - position;
            }

            void clear() {
                position = 0;
                buffer.clear();
            }

            byte[] array() {
                return buffer.array();
            }
        }

        ReadThread(InputStream inputStream) {
            bufferedInputStream = new BufferedInputStream(inputStream);
        }

        private void setBarColor(byte[] bytes) {
            if (bytes.length == 0) {
                Log.e(">>>>", "Size is Zero");
                return;
            }

            int lightPixelCount = 0;
            int darkPixelCount = 0;
            for (int i = 0; i < bytes.length; i += 4) {
                if (pixelIsLightColor(bytes, i)) {
                    lightPixelCount++;
                } else {
                    darkPixelCount++;
                }
            }

            if (lightPixelCount > darkPixelCount) {
                Log.d(">>>>", "变黑色 " + "light:dark = " + lightPixelCount + ":" + darkPixelCount);
                GlobalState.iosBarColor = Color.BLACK;
            } else {
                Log.d(">>>>", "变白色 " + "light:dark = " + lightPixelCount + ":" + darkPixelCount);
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
            if (index > -1 && (index + 3) <= rawImage.length) {
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
            // Log.d(">>>>", "pixel overflow, index:" + index + "  array:" + rawImage.length);
            return false;
        }

        @Override
        public void run() {
            int cacheSize = 1024 * 1024; // 1M
            byte[] tempBuffer = new byte[cacheSize];
            PixelGather byteBuffer = PixelGather.frameBuffer(GlobalState.displayHeight, GlobalState.displayWidth);

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
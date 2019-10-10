package com.omarea.common.shared;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.PixelFormat;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

public class BitmapUtil {
    public Bitmap temp;

    /**
     * 根据指定的高度进行缩放（source是bitmap）
     */
    public Bitmap bitmapZoomByHeight(Bitmap srcBitmap, float newHeight) {
        float scale = newHeight / (((float) srcBitmap.getHeight()));
        return bitmapZoomByScale(srcBitmap, scale, scale);
    }

    /**
     * 根据指定的高度进行缩放（source是drawable）
     */
    public Bitmap bitmapZoomByHeight(Drawable drawable, float newHeight) {
        Bitmap bitmap = drawableToBitmap(drawable);
        float scale = newHeight / (((float) bitmap.getHeight()));
        return bitmapZoomByScale(bitmap, scale, scale);
    }

    /**
     * 根据指定的宽度比例值和高度比例值进行缩放
     */
    public Bitmap bitmapZoomByScale(Bitmap srcBitmap, float scaleWidth, float scaleHeight) {
        int width = srcBitmap.getWidth();
        int height = srcBitmap.getHeight();
        Matrix matrix = new Matrix();
        matrix.postScale(scaleWidth, scaleHeight);
        Bitmap bitmap = Bitmap.createBitmap(srcBitmap, 0, 0, width, height, matrix, true);
        if (bitmap != null) {
            return bitmap;
        } else {
            return srcBitmap;
        }
    }

    /**
     * 将drawable对象转成bitmap对象
     */
    public Bitmap drawableToBitmap(Drawable drawable) {
        int width = drawable.getIntrinsicWidth();
        int height = drawable.getIntrinsicHeight();
        Bitmap.Config config = drawable.getOpacity() != PixelFormat.OPAQUE ? Bitmap.Config.ARGB_8888 : Bitmap.Config.RGB_565;
        Bitmap bitmap = Bitmap.createBitmap(width, height, config);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, width, height);
        drawable.draw(canvas);
        return bitmap;
    }

    /**
     * 将drawable对象转成bitmap对象
     */
    public Bitmap drawableToBitmap2(Drawable drawable) {
        BitmapDrawable bd = (BitmapDrawable) drawable;
        Bitmap bm = bd.getBitmap();
        return bm;
    }

    /**
     * 将bitmap对象保存成图片到sd卡中
     */
    public void saveBitmapToSDCard(Bitmap bitmap, String path) {
        File file = new File(path);
        if (file.exists()) {
            file.delete();
        }
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, ((OutputStream) fileOutputStream));//设置PNG的话，透明区域不会变成黑色

            fileOutputStream.close();
            System.out.println("----------save success-------------------");
        } catch (Exception v0) {
            v0.printStackTrace();
        }
    }

    /**
     * 从sd卡中获取图片的bitmap对象
     */
    public Bitmap getBitmapFromSDCard(String path) {
        Bitmap bitmap = null;
        try {
            FileInputStream fileInputStream = new FileInputStream(path);
            if (fileInputStream != null) {
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inSampleSize = 2; //当图片资源太大的适合，会出现内存溢出。图片宽高都为原来的二分之一，即图片为原来的四分一
                bitmap = BitmapFactory.decodeStream(((InputStream) fileInputStream), null, options);
            }
        } catch (Exception e) {
            return null;
        }

        return bitmap;
    }
}
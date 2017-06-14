package com.omarea.shared;

import android.content.res.AssetManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

/**
 * Created by helloklf on 2016/8/27.
 */
public class AppShared {
    public static String baseUrl = "/sdcard/Android/data/com.omarea.vboot/";
    public static String datFile = baseUrl + "data.dat";


    public static JSONObject getConfigData(){
        File file = new File(datFile);
        try {
            if (!file.exists()) {
                new File(baseUrl).mkdirs();
                file.createNewFile();
            }

            FileInputStream inputStream = new FileInputStream(file);
            DataInputStream dataInputStream = new DataInputStream(inputStream);

            byte[] datas = new byte[(int) (file.length())];
            dataInputStream.read(datas);
            String json = new String(datas, Charset.forName("UTF-8"));

            JSONObject jsonObject;

            try {
                jsonObject = new JSONObject(json);
            } catch (JSONException e) {
                jsonObject = new JSONObject();
            }

            return jsonObject;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new JSONObject();
    }

    public static boolean setConfigData(JSONObject jsonObject){
        File file = new File(datFile);
        try {
            FileOutputStream outputStream = new FileOutputStream(file);
            DataOutputStream dataOutputStream = new DataOutputStream(outputStream);

            String data = jsonObject.toString();
            dataOutputStream.writeBytes(data);

            dataOutputStream.flush();
            dataOutputStream.close();
            outputStream.close();

            return true;
        } catch (IOException e) {
        }
        return false;
    }

    public static void WriteFile(AssetManager ass, String file, boolean hasExtName) {
        try {
            InputStream is = ass.open(file);
            byte[] datas = new byte[2 * 1024 * 1024];
            int len = is.read(datas);

            /*
            //获取SD卡的目录
            File sdCardDir = Environment.getExternalStorageDirectory();
            File targetFile = new File(sdCardDir.getCanonicalPath() + "shelltoolsfile.zip");
            //以指定文件创建RandomAccessFile对象
            RandomAccessFile raf = new RandomAccessFile(targetFile, "rw");
            //将文件记录指针移动到最后
            raf.seek(targetFile.length());
            //输出文件内容
            raf.write(datas,0,len);
            raf.close();
            */
            File dir = new File(AppShared.baseUrl);
            if (!dir.exists())
                dir.mkdirs();
            String filePath = AppShared.baseUrl + ((hasExtName ? file :
                    (file.substring(0, (file.lastIndexOf(".") > 0 ? file.lastIndexOf(".") : file.length())))));

            FileOutputStream fileOutputStream = new FileOutputStream(filePath);
            fileOutputStream.write(datas, 0, len);
            fileOutputStream.close();
            is.close();
            //getApplicationContext().getClassLoader().getResourceAsStream("");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void WriteFile(AssetManager assetManager, String file, String outName) {
        try {
            InputStream is = assetManager.open(file);
            File f = new File(file);
            byte[] datas = new byte[2 * 1024 * 1024];
            int a = is.available();
            int len = is.read(datas);
            File dir = new File(AppShared.baseUrl);
            if (!dir.exists())
                dir.mkdirs();
            String filePath = AppShared.baseUrl + outName;

            FileOutputStream fileOutputStream = new FileOutputStream(filePath);
            fileOutputStream.write(datas, 0, len);
            fileOutputStream.close();
            is.close();
            //getApplicationContext().getClassLoader().getResourceAsStream("");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static boolean system_inited = false;
}

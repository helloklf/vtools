package com.omarea.vboot;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;


public class fragment_helpinfo extends Fragment {

    public fragment_helpinfo() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.layout_helpinfo, container, false);
    }

    public static String readStreamToString(InputStream inputStream) throws IOException {
        //创建字节数组输出流 ，用来输出读取到的内容
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        //创建读取缓存,大小为1024
        byte[] buffer = new byte[1024];
        //每次读取长度
        int len = 0;
        //开始读取输入流中的文件
        while ((len = inputStream.read(buffer)) != -1) { //当等于-1说明没有数据可以读取了
            byteArrayOutputStream.write(buffer, 0, len); // 把读取的内容写入到输出流中
        }
        //把读取到的字节数组转换为字符串
        String result = byteArrayOutputStream.toString();

        //关闭输入流和输出流
        inputStream.close();
        byteArrayOutputStream.close();
        //返回字符串结果
        return result;
    }


    @Override
    public void onViewCreated(final View view, @Nullable Bundle savedInstanceState) {
        this.view = view;/**/

        /*
        Button button = (Button) view.findViewById(R.id.checkupdate);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                try {
                    //获取包管理器
                    PackageManager pm = view.getContext().getPackageManager();
                    //获取包信息
                    PackageInfo packageInfo = pm.getPackageInfo(view.getContext().getPackageName(), 0);
                    CheckupThread checkupThread = new CheckupThread();
                    checkupThread.localVersionCode = packageInfo.versionCode;
                    checkupThread.start();
                } catch (Exception ex) {
                    Snackbar.make(view, "获取更新异常！\n" + ex.getMessage(), Snackbar.LENGTH_LONG).show();
                }
            }
        });
        */
    }

    android.os.Handler myHandler = new android.os.Handler() {
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
        }
    };

    View view;

    private void ShowMsg(final String msg) {
        myHandler.post(new Runnable() {
            @Override
            public void run() {
                Snackbar.make(view, msg, Snackbar.LENGTH_SHORT).show();
            }
        });
    }

    class CheckupThread extends Thread {
        int localVersionCode;

        @Override
        public void run() {
            super.run();

            try {
                //raw.githubusercontent.com
                HttpsURLConnection urlConnection =
                        (HttpsURLConnection) new URL("https://151.101.72.133/helloklf/vtools/master/lastversion.json").openConnection();
                urlConnection.setUseCaches(false);
                //urlConnection.connect();
                InputStream inputStream = urlConnection.getInputStream();
                JSONObject jsonObject = new JSONObject(readStreamToString(inputStream));
                int versionCode = jsonObject.getInt("versionCode");
                String versionName = jsonObject.getString("versionName");
                //String versionContent = jsonObject.getString("versionContent");


                if (localVersionCode < versionCode) {
                    ShowMsg("找到可用更新版本：" + versionName);
                    final String downloadUrl = "https://github.com/helloklf/vtools/raw/master/%E7%BC%96%E8%AF%91%E7%89%88%E6%9C%AC-APK/" + versionName + ".apk";
                    myHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            Intent intent = new Intent();
                            intent.setAction("android.intent.action.VIEW");
                            Uri content_url = Uri.parse(downloadUrl);
                            intent.setData(content_url);
                            startActivity(intent);
                        }
                    });
                }
            } catch (Exception ex) {
                ShowMsg("获取更新异常！\n" + ex.getMessage());
            }
        }
    }
}

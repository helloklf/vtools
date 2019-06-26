package com.omarea.krscript.model;

import android.app.Dialog;
import android.graphics.Color;
import android.os.Handler;
import android.os.Message;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.regex.Pattern;

/**
 * Created by Hello on 2018/04/01.
 */

public abstract class ShellHandlerBase extends Handler {
    /**
     * 处理启动信息
     */
    public static final int EVENT_START = 0;

    /**
     * 命令行输出内容
     */
    public static final int EVENT_REDE = 2;

    /**
     * 命令行错误输出
     */
    public static final int EVENT_READ_ERROR = 4;

    /**
     * 脚本写入日志
     */
    public static final int EVENT_WRITE = 6;

    /**
     * 处理Exitvalue
     */
    public static final int EVENT_EXIT = -2;

    public boolean finished;

    protected abstract void onProgress(int current, int total);

    protected abstract void onStart(Object msg);
    protected abstract void onExit(Object msg);

    protected abstract void cleanUp();
    /**
     * 输出格式化内容
     *
     * @param msg
     */
    protected abstract void updateLog(final SpannableString msg);

    @Override
    public void handleMessage(Message msg) {
        super.handleMessage(msg);
        switch (msg.what) {
            case ShellHandlerBase.EVENT_EXIT:
                onExit(msg.obj);
                break;
            case ShellHandlerBase.EVENT_START: {
                onStart(msg.obj);
                break;
            }
            case ShellHandlerBase.EVENT_REDE:
                onReader(msg.obj);
                break;
            case ShellHandlerBase.EVENT_READ_ERROR:
                onError(msg.obj);
                break;
            case ShellHandlerBase.EVENT_WRITE: {
                onWrite(msg.obj);
                break;
            }
        }
    }

    private void onReader(Object msg) {
        if (msg != null) {
            String log = msg.toString().trim();
            if (Pattern.matches("^progress:\\[[\\-0-9\\\\]{1,}/[0-9\\\\]{1,}]$", log)) {
                String[] values = log.substring("progress:[".length(), log.indexOf("]")).split("/");
                int start = Integer.parseInt(values[0]);
                int total = Integer.parseInt(values[1]);
                onProgress(start, total);
            } else {
                updateLog(msg, "#00cc55");
            }
        }
    }

    private void onWrite(Object msg) {
        updateLog(msg, "#808080");
    }

    private void onError(Object msg) {
        updateLog(msg, "#ff0000");
    }

    /**
     * 输出指定颜色的内容
     *
     * @param msg
     * @param color
     */
    protected void updateLog(final Object msg, final String color) {
        if (msg != null) {
            String msgStr = msg.toString();
            SpannableString spannableString = new SpannableString(msgStr);
            spannableString.setSpan(new ForegroundColorSpan(Color.parseColor(color)), 0, msgStr.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            updateLog(spannableString);
        }
    }

    protected void updateLog(final Object msg, final int color) {
        if (msg != null) {
            String msgStr = msg.toString();
            SpannableString spannableString = new SpannableString(msgStr);
            spannableString.setSpan(new ForegroundColorSpan(color), 0, msgStr.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            updateLog(spannableString);
        }
    }
}

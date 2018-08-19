package com.omarea.scripts.simple.shell;

import android.content.Context;
import android.graphics.Color;
import android.os.Message;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.widget.TextView;

import com.omarea.vtools.R;

public class SimpleShellHandler extends ShellHandler {
    Context context;

    public SimpleShellHandler(TextView textView) {
        this.textView = textView;
        context = textView.getContext();
    }

    @Override
    public void handleMessage(Message msg) {
        super.handleMessage(msg);
        switch (msg.what) {
            case -2:
                onExit(msg.obj);
                break;
            case 0: {
                onStart(msg.obj);
                break;
            }
            case 2:
                onReader(msg.obj);
                break;
            case 4:
                onError(msg.obj);
                break;
            case 6: {
                onWrite(msg.obj);
                break;
            }
        }
    }

    private void onWrite(Object msg) {
        updateLog(msg, "#808080");
    }

    private void onStart(Object msg) {
        updateLog(msg, "#000000");
    }

    private void onError(Object msg) {
        updateLog(msg, "#ff0000");
    }

    private void onExit(Object msg) {
        if (msg != null && msg instanceof Integer) {
            if ((Integer) msg == 0)
                updateLog("\n\n" + context.getString(R.string.execute_success), "#138ed6");
            else
                updateLog("\n\nexit value: " + msg, "#138ed6");
        } else
            updateLog(msg, "#5500cc");
    }

    private void onReader(Object msg) {
        updateLog(msg, "#00cc55");
    }

    /**
     * 输出指定颜色的内容
     *
     * @param msg
     * @param color
     */
    private void updateLog(final Object msg, final String color) {
        if (msg != null) {
            String msgStr = msg.toString();
            SpannableString spannableString = new SpannableString(msgStr);
            spannableString.setSpan(new ForegroundColorSpan(Color.parseColor(color)), 0, msgStr.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            updateLog(spannableString);
        }
    }

    /**
     * 输出格式化内容
     *
     * @param msg
     */
    private void updateLog(final SpannableString msg) {
        if (this.textView != null && msg != null) {
            this.textView.post(new Runnable() {
                @Override
                public void run() {
                    textView.append(msg);
                }
            });
        }
    }

    /**
     * 无格式输出
     *
     * @param msg
     */
    private void updateLog(final String msg) {
        if (this.textView != null && msg != null) {
            this.textView.post(new Runnable() {
                @Override
                public void run() {
                    textView.append(msg);
                }
            });
        }
    }
}

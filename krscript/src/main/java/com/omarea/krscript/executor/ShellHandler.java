package com.omarea.krscript.executor;

import android.app.Dialog;
import android.os.Handler;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

/**
 * Created by Hello on 2018/04/01.
 */

public abstract class ShellHandler extends Handler {
    /**
     * 处理启动信息
     */
    static final int EVENT_START = 0;

    /**
     * 命令行输出内容
     */
    static final int EVENT_REDE = 2;

    /**
     * 命令行错误输出
     */
    static final int EVENT_READ_ERROR = 4;

    /**
     * 脚本写入日志
     */
    static final int EVENT_WRITE = 6;

    /**
     * 处理Exitvalue
     */
    static final int EVENT_EXIT = -2;

    TextView textView;
    ProgressBar shellProgress;
    TextView shellTitle;
    Button btnExit;
    Button btnHide;
    Dialog dialog;
    boolean finished;
}

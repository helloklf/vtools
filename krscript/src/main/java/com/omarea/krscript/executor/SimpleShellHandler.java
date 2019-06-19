package com.omarea.krscript.executor;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.os.Message;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;

import com.omarea.krscript.R;

import java.util.Objects;
import java.util.regex.Pattern;

public class SimpleShellHandler extends ShellHandler {
    Context context;
    private boolean autoOff = false;
    private Runnable forceStop = null;

    public SimpleShellHandler(Context context, String title, final Runnable forceStop, boolean autoOff) {
        this.context = context;
        this.autoOff = autoOff;
        this.forceStop = forceStop;

        if (title.isEmpty()) {
            title = context.getString(R.string.shell_executor);
        }

        LayoutInflater layoutInflater = LayoutInflater.from(context);
        View view = layoutInflater.inflate(R.layout.dialog_shell_executor, null);
        this.shellTitle = view.findViewById(R.id.shell_title);
        this.shellProgress = view.findViewById(R.id.shell_progress);
        this.textView = view.findViewById(R.id.shell_output);
        this.btnExit = view.findViewById(R.id.btn_exit);
        this.btnHide = view.findViewById(R.id.btn_hide);

        this.shellTitle.setText(title);

        dialog = new AlertDialog.Builder(context)
                .setView(view)
                .setCancelable(false)
                .create();
        Objects.requireNonNull(dialog.getWindow()).setWindowAnimations(R.style.windowAnim);
        dialog.show();

        btnHide.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                cleanUp();
            }
        });
        btnExit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                exit();
            }
        });

        if (this.forceStop != null) {
            btnExit.setVisibility(View.VISIBLE);
        } else {
            btnExit.setVisibility(View.GONE);
        }
    }

    private void cleanUp() {
        dialog = null;
        shellTitle = null;
        shellProgress = null;
        textView = null;
        btnExit = null;
        btnHide = null;
    }

    @Override
    public void handleMessage(Message msg) {
        super.handleMessage(msg);
        switch (msg.what) {
            case ShellHandler.EVENT_EXIT:
                onExit(msg.obj);
                break;
            case ShellHandler.EVENT_START: {
                onStart(msg.obj);
                break;
            }
            case ShellHandler.EVENT_REDE:
                onReader(msg.obj);
                break;
            case ShellHandler.EVENT_READ_ERROR:
                onError(msg.obj);
                break;
            case ShellHandler.EVENT_WRITE: {
                onWrite(msg.obj);
                break;
            }
        }
    }

    private void onWrite(Object msg) {
        updateLog(msg, "#808080");
    }

    private void onStart(Object msg) {
        // updateLog(msg, "#000000");
    }

    private void onError(Object msg) {
        updateLog(msg, "#ff0000");
    }

    private void onExit(Object msg) {
        if (btnHide != null) {
            btnHide.setVisibility(View.GONE);
        }
        finished = true;
        onProgress(1, 1);

        if (msg instanceof Integer) {
            if ((Integer) msg == 0)
                updateLog("\n\n" + context.getString(R.string.execute_success), "#138ed6");
            else
                updateLog("\n\nexit value: " + msg, "#138ed6");
        } else
            updateLog(msg, "#5500cc");
        if (autoOff && dialog != null) {
            dialog.dismiss();
            dialog = null;
        } else if (btnExit != null) {
            btnExit.setVisibility(View.VISIBLE);
        }
    }

    private void onReader(Object msg) {
        if (msg != null) {
            String log = msg.toString().trim();
            if(Pattern.matches("^progress:\\[[\\-0-9\\\\]{1,}/[0-9\\\\]{1,}]$", log)) {
                String [] values = log.substring("progress:[".length(), log.indexOf("]")).split("/");
                int start = Integer.parseInt(values[0]);
                int total = Integer.parseInt(values[1]);
                onProgress(start, total);
            } else {
                updateLog(msg, "#00cc55");
            }
        }
    }

    private void onProgress(int current, int total){
        if (this.shellProgress != null) {
            if (current == -1) {
                this.shellProgress.setVisibility(View.VISIBLE);
                this.shellProgress.setIndeterminate(true);
            } else if (current == total) {
                this.shellProgress.setVisibility(View.GONE);
            } else {
                this.shellProgress.setVisibility(View.VISIBLE);
                this.shellProgress.setIndeterminate(false);
                this.shellProgress.setMax(total);
                this.shellProgress.setProgress(current);
            }
        }
    }

    /**
     * 输出指定颜色的内容
     *
     * @param msg
     * @param color
     */
    private void updateLog(final Object msg, final String color) {
        if (msg != null && textView != null) {
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

    private void exit() {
        if (dialog != null) {
            dialog.dismiss();
        }
        cleanUp();
        if (forceStop != null && !finished) {
            forceStop.run();
        }
    }
}

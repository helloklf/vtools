package com.omarea.krscript.executor;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.text.SpannableString;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.omarea.krscript.R;
import com.omarea.krscript.model.ShellHandlerBase;

import java.util.Objects;

public class SimpleShellHandler extends ShellHandlerBase {
    private Context context;
    private boolean autoOff;
    private TextView textView;
    private ProgressBar shellProgress;
    private TextView shellTitle;
    private Button btnExit;
    private Button btnHide;
    public Dialog dialog;
    private Runnable forceStop;

    SimpleShellHandler(final Context context, String title, boolean autoOff) {
        this.context = context;
        this.autoOff = autoOff;

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
                if (dialog != null) {
                    dialog.dismiss();
                }
                cleanUp();
                if (forceStop != null && !finished) {
                    forceStop.run();
                }
            }
        });
        view.findViewById(R.id.btn_copy).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    ClipboardManager myClipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                    ClipData myClip = ClipData.newPlainText("text", textView.getText().toString());
                    myClipboard.setPrimaryClip(myClip);
                    Toast.makeText(context, context.getString(R.string.copy_success), Toast.LENGTH_SHORT).show();
                } catch (Exception ex) {
                    Toast.makeText(context, context.getString(R.string.copy_fail), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @Override
    protected void onExit(Object msg) {
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

    @Override
    protected void onProgress(int current, int total) {
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

    @Override
    protected void onStart(Object msg) {

    }

    @Override
    public void onStart(Runnable forceStop) {
        this.forceStop = forceStop;
        if (forceStop != null) {
            btnExit.setVisibility(View.VISIBLE);
        } else {
            btnExit.setVisibility(View.GONE);
        }
    }

    /**
     * 输出格式化内容
     *
     * @param msg
     */
    @Override
    protected void updateLog(final SpannableString msg) {
        if (this.textView != null && msg != null) {
            this.textView.post(new Runnable() {
                @Override
                public void run() {
                    textView.append(msg);
                    ((ScrollView) textView.getParent()).fullScroll(ScrollView.FOCUS_DOWN);
                }
            });
        }
    }

    @Override
    protected void cleanUp() {
        dialog = null;
        shellTitle = null;
        shellProgress = null;
        textView = null;
        btnExit = null;
        btnHide = null;
    }
}

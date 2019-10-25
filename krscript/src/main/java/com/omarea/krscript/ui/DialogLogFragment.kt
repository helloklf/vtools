package com.omarea.krscript.ui

import android.app.Dialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.os.Message
import android.support.v4.app.DialogFragment
import android.text.SpannableString
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import com.omarea.krscript.R
import com.omarea.krscript.executor.ShellExecutor
import com.omarea.krscript.model.ConfigItemBase
import com.omarea.krscript.model.ShellHandlerBase
import kotlinx.android.synthetic.main.kr_dialog_log.*


class DialogLogFragment : DialogFragment() {
    private lateinit var dialogInstance: Dialog
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // val view = inflater.inflate(R.layout.kr_dialog_log, container, false)

        currentView = inflater.inflate(R.layout.kr_dialog_log, container)
        return currentView
    }

    private var running = false
    private lateinit var configItem: ConfigItemBase
    private lateinit var onExit: Runnable
    private lateinit var script: String
    private var params: HashMap<String, String>? = null
    private var themeResId: Int = 0
    private lateinit var currentView: View

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return Dialog(activity!!, if (themeResId != 0) themeResId else R.style.kr_full_screen_dialog_light)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        val shellHandler = openExecutor()

        if (shellHandler != null) {
            ShellExecutor().execute(this.activity, configItem, script, onExit, params, shellHandler)
        }
    }

    private fun openExecutor(): ShellHandlerBase? {
        var forceStopRunnable: Runnable? = null

        btn_hide.setOnClickListener {
            closeView()
        }
        btn_exit.setOnClickListener {
            if (running && forceStopRunnable != null) {
                forceStopRunnable!!.run()
            }
            closeView()
        }

        btn_copy.setOnClickListener {
            try {
                val myClipboard: ClipboardManager = this.context!!.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val myClip: ClipData = ClipData.newPlainText("text", shell_output.text.toString())
                myClipboard.primaryClip = myClip
                Toast.makeText(context, getString(R.string.copy_success), Toast.LENGTH_SHORT).show()
            } catch (ex: Exception) {
                Toast.makeText(context, getString(R.string.copy_fail), Toast.LENGTH_SHORT).show()
            }
        }
        if (configItem.interruptible) {
            btn_hide?.visibility = View.VISIBLE
            btn_exit?.visibility = View.VISIBLE
        } else {
            btn_hide?.visibility = View.GONE
            btn_exit?.visibility = View.GONE
        }

        title.text = configItem.title
        action_progress.isIndeterminate = true
        return MyShellHandler(object : IActionEventHandler {
            override fun onCompleted() {
                running = false

                onExit.run()
                if (btn_hide != null) {
                    btn_hide.visibility = View.GONE
                    btn_exit.visibility = View.VISIBLE
                    action_progress.visibility = View.GONE
                }

                isCancelable = true
            }

            override fun onSuccess() {
                if (configItem.autoOff) {
                    closeView()
                }
            }

            override fun onStart(forceStop: Runnable?) {
                running = true

                if (configItem.interruptible && forceStop != null) {
                    btn_exit.visibility = View.VISIBLE
                } else {
                    btn_exit.visibility = View.GONE
                }
                forceStopRunnable = forceStop
            }

        }, shell_output, action_progress)
    }

    @FunctionalInterface
    interface IActionEventHandler {
        fun onStart(forceStop: Runnable?)
        fun onSuccess()
        fun onCompleted()
    }

    class MyShellHandler(
            private var actionEventHandler: IActionEventHandler,
            private var logView: TextView,
            private var shellProgress: ProgressBar) : ShellHandlerBase() {
        private val context = logView.context
        private val errorColor = Color.parseColor(context.getString(R.string.kr_shell_log_error))
        private val basicColor = Color.parseColor(context.getString(R.string.kr_shell_log_basic))
        private val scriptColor = Color.parseColor(context.getString(R.string.kr_shell_log_script))
        private val endColor = Color.parseColor(context.getString(R.string.kr_shell_log_end))

        private var hasError = false // 执行过程是否出现错误

        override fun handleMessage(msg: Message) {
            when (msg.what) {
                EVENT_EXIT -> onExit(msg.obj)
                EVENT_START -> {
                    onStart(msg.obj)
                }
                EVENT_REDE -> onReaderMsg(msg.obj)
                EVENT_READ_ERROR -> onError(msg.obj)
                EVENT_WRITE -> {
                    onWrite(msg.obj)
                }
            }
        }

        override fun onReader(msg: Any) {
            updateLog(msg, basicColor)
        }

        override fun onWrite(msg: Any) {
            updateLog(msg, scriptColor)
        }

        override fun onError(msg: Any) {
            hasError = true
            updateLog(msg, errorColor)
        }

        override fun onStart(forceStop: Runnable?) {
            actionEventHandler.onStart(forceStop)
        }

        override fun onProgress(current: Int, total: Int) {
            when (current) {
                -1 -> {
                    this.shellProgress.visibility = View.VISIBLE
                    this.shellProgress.isIndeterminate = true
                }
                total -> this.shellProgress.visibility = View.GONE
                else -> {
                    this.shellProgress.visibility = View.VISIBLE
                    this.shellProgress.isIndeterminate = false
                    this.shellProgress.max = total
                    this.shellProgress.progress = current
                }
            }
        }

        override fun onStart(msg: Any?) {
            this.logView.text = ""
            // updateLog(msg, scriptColor)
        }

        override fun onExit(msg: Any?) {
            updateLog(context.getString(R.string.kr_shell_completed), endColor)
            actionEventHandler.onCompleted()
            if (!hasError) {
                actionEventHandler.onSuccess()
            }
        }

        override fun updateLog(msg: SpannableString?) {
            if (msg != null) {
                this.logView.post {
                    logView.append(msg)
                    (logView.parent as ScrollView).fullScroll(ScrollView.FOCUS_DOWN)
                }
            }
        }
    }

    private fun closeView() {
        try {
            dismiss()
        } catch (ex: java.lang.Exception) {
        }
    }

    companion object {
        fun create(configItem: ConfigItemBase,
                   onExit: Runnable,
                   script: String,
                   params: HashMap<String, String>?,
                   darkMode: Boolean = false): DialogLogFragment {
            val fragment = DialogLogFragment()
            fragment.configItem = configItem
            fragment.onExit = onExit
            fragment.script = script
            fragment.params = params
            fragment.themeResId = if (darkMode) R.style.kr_full_screen_dialog_dark else R.style.kr_full_screen_dialog_light

            return fragment
        }
    }
}

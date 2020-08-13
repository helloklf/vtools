package com.omarea.krscript

import android.content.Context
import android.os.Bundle
import android.text.SpannableString
import android.widget.Toast
import com.omarea.krscript.executor.ShellExecutor
import com.omarea.krscript.model.RunnableNode
import com.omarea.krscript.model.ShellHandlerBase

class HiddenTaskThread(private var process: Process) : Thread() {
    override fun run() {
        try {
            process.waitFor()
        } catch (ex: java.lang.Exception) {
        }
    }

    class ServiceShellHandler(private val context: Context) : ShellHandlerBase() {
        private var errorRows = ArrayList<String>()
        private var notificationMShortMsg = ""
        private var progressCurrent = 0
        private var progressTotal = 0
        private var forceStop: Runnable? = null
        private var isFinished = false

        override fun updateLog(msg: SpannableString?) {
        }

        override fun onReader(msg: Any?) {
        }

        override fun onError(msg: Any?) {
            notificationMShortMsg = context.getString(R.string.kr_script_task_has_error)
            synchronized(errorRows) {
                errorRows.add("" + msg?.toString())
            }
        }

        override fun onWrite(msg: Any?) {
        }

        override fun onExit(msg: Any?) {
            isFinished = true
            if (errorRows.size > 0) {
                Toast.makeText(
                        context,
                        context.getString(R.string.kr_script_task_has_error) + "\n\n" +
                                errorRows.joinToString("\n"),
                        Toast.LENGTH_LONG).show()
            }
        }

        override fun onStart(forceStop: Runnable?) {
            this.forceStop = forceStop
        }

        override fun onStart(msg: Any?) {
        }

        override fun onProgress(current: Int, total: Int) {
            progressCurrent = current
            progressTotal = total
        }
    }

    companion object {
        private var notificationCounter = 34050

        fun startTask(context: Context, script: String, params: HashMap<String, String>?, nodeInfo: RunnableNode, onExit: Runnable, onDismiss: Runnable) {
            val applicationContext = context.applicationContext
            notificationCounter += 1

            val handler = ServiceShellHandler(applicationContext)
            ShellExecutor().execute(
                    context,
                    nodeInfo,
                    script,
                    {
                        try {
                            onExit.run()
                            onDismiss.run()
                        } catch (ex: Exception) {
                        }
                    },
                    params,
                    handler)

            val bundle = Bundle()
            params?.run {
                bundle.putSerializable("params", params)
            }
        }
    }
}

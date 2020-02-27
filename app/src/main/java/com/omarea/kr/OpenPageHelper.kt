package com.projectkr.shell

import android.app.Activity
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import com.omarea.common.ui.ProgressBarDialog
import com.omarea.krscript.model.PageNode
import com.omarea.vtools.activities.ActionPage

class OpenPageHelper(private var activity: Activity) {
    private var progressBarDialog: ProgressBarDialog? = null
    private var handler = Handler(Looper.getMainLooper())

    private val dialog: ProgressBarDialog
        get() {
            if (progressBarDialog == null) {
                progressBarDialog = ProgressBarDialog(activity)
            }
            return progressBarDialog!!
        }

    private fun showDialog(msg: String) {
        handler.post {
            dialog.showDialog(msg)
        }
    }

    private fun hideDialog() {
        handler.post {
            dialog.hideDialog()
        }
    }

    fun openPage(pageInfo: PageNode) {
        try {
            var intent: Intent? = null
            // TODO: 用于访问网页的页面
            // if (!pageInfo.onlineHtmlPage.isEm=pty()) {
            //     intent = Intent(activity, ActivityAddinOnline::class.java)
            //     intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            //     intent.putExtra("config", pageInfo.onlineHtmlPage)
            // }

            if (!pageInfo.pageConfigSh.isEmpty()) {
                if (intent == null) {
                    intent = Intent(activity, ActionPage::class.java)
                }
                intent.putExtra("pageConfigSh", pageInfo.pageConfigSh)
            }

            if (!pageInfo.pageConfigPath.isEmpty()) {
                if (intent == null) {
                    intent = Intent(activity, ActionPage::class.java)
                }
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                intent.putExtra("config", pageInfo.pageConfigPath)
            }

            intent?.run {
                putExtra("title", pageInfo.title)

                if (pageInfo.beforeRead.isNotBlank()) {
                    intent.putExtra("beforeRead", pageInfo.beforeRead)
                }
                if (pageInfo.afterRead.isNotBlank()) {
                    intent.putExtra("afterRead", pageInfo.afterRead)
                }
                if (pageInfo.loadSuccess.isNotBlank()) {
                    intent.putExtra("loadSuccess", pageInfo.loadSuccess)
                }
                if (pageInfo.loadFail.isNotBlank()) {
                    intent.putExtra("loadFail", pageInfo.loadFail)
                }
                if (pageInfo.parentPageConfigDir.isNotEmpty()) {
                    intent.putExtra("parentDir", pageInfo.parentPageConfigDir)
                }

                activity.startActivity(intent)
            }
        } catch (ex: Exception) {
            Toast.makeText(activity, "" + ex.message, Toast.LENGTH_SHORT).show()
        }
    }
}

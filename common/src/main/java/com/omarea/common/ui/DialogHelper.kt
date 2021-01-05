package com.omarea.common.ui

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.graphics.drawable.BitmapDrawable
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import com.omarea.common.R

class DialogHelper {
    class DialogWrap(private val dialog: AlertDialog) {
        public val context = dialog.context

        public fun dismiss() {
            try {
                dialog.dismiss()
            } catch (ex: Exception) {
            }
        }

        public fun hide() {
            try {
                dialog.hide()
            } catch (ex: Exception) {
            }
        }

        public val isShowing: Boolean
            get() {
                return dialog.isShowing()
            }
    }

    companion object {
        fun animDialog(dialog: AlertDialog?): DialogWrap? {
            if (dialog != null && !dialog.isShowing) {
                dialog.window?.run {
                    setWindowAnimations(R.style.windowAnim)
                }
                dialog.show()
            }
            return if (dialog != null) DialogWrap(dialog) else null
        }

        fun animDialog(builder: AlertDialog.Builder): DialogWrap {
            val dialog = builder.create()
            animDialog(dialog)
            return DialogWrap(dialog)
        }

        fun helpInfo(context: Context, message: String, onDismiss: Runnable? = null): DialogWrap {
            return helpInfo(context, "", message, onDismiss)
        }

        fun helpInfo(context: Context, title: String, message: String, onDismiss: Runnable? = null): DialogWrap {
            val layoutInflater = LayoutInflater.from(context)
            val dialog = layoutInflater.inflate(R.layout.dialog_help_info, null)
            val alert = AlertDialog.Builder(context).setView(dialog)
            alert.setCancelable(true)

            (dialog.findViewById(R.id.dialog_help_title) as TextView).run {
                if (title.isNotEmpty()) {
                    text = title
                    visibility = View.VISIBLE
                } else {
                    visibility = View.GONE
                }
            }

            (dialog.findViewById(R.id.dialog_help_info) as TextView).run {
                if (message.isNotEmpty()) {
                    text = message
                    visibility = View.VISIBLE
                } else {
                    visibility = View.GONE
                }
            }
            if (onDismiss != null) {
                alert.setPositiveButton(R.string.btn_confirm) { d, _ ->
                    d.dismiss()
                }
                alert.setCancelable(false)
            }
            alert.setOnDismissListener {
                onDismiss?.run()
            }

            return animDialog(alert)
        }

        fun confirm(context: Context,
                    title: String = "",
                    message: String = "",
                    onConfirm: Runnable? = null,
                    onCancel: Runnable? = null): DialogWrap {
            val view = LayoutInflater.from(context).inflate(R.layout.dialog_confirm, null)

            if (title.isEmpty()) {
                view.findViewById<TextView>(R.id.confirm_title).visibility = View.GONE
            } else {
                view.findViewById<TextView>(R.id.confirm_title).setText(title)
            }
            if (message.isEmpty()) {
                view.findViewById<TextView>(R.id.confirm_message).visibility = View.GONE
            } else {
                view.findViewById<TextView>(R.id.confirm_message).setText(message)
            }

            val dialog = customDialog(context, view)
            view.findViewById<View>(R.id.btn_cancel).setOnClickListener {
                dialog.dismiss()
                onCancel?.run()
            }
            view.findViewById<View>(R.id.btn_confirm).setOnClickListener {
                dialog.dismiss()
                onConfirm?.run()
            }

            return dialog
        }

        fun confirm(context: Context,
                    title: String = "",
                    message: String = "",
                    contentView: View? = null,
                    onConfirm: Runnable? = null,
                    onCancel: Runnable? = null): DialogWrap {
            val view = LayoutInflater.from(context).inflate(R.layout.dialog_confirm, null)
            if (title.isEmpty()) {
                view.findViewById<TextView>(R.id.confirm_title).visibility = View.GONE
            } else {
                view.findViewById<TextView>(R.id.confirm_title).setText(title)
            }
            if (message.isEmpty()) {
                view.findViewById<TextView>(R.id.confirm_message).visibility = View.GONE
            } else {
                view.findViewById<TextView>(R.id.confirm_message).setText(message)
            }
            if (contentView != null) {
                view.findViewById<FrameLayout>(R.id.confirm_custom_view).addView(contentView)
            }
            val dialog = customDialog(context, view)
            view.findViewById<View>(R.id.btn_cancel).setOnClickListener {
                dialog.dismiss()
                onCancel?.run()
            }
            view.findViewById<View>(R.id.btn_confirm).setOnClickListener {
                dialog.dismiss()
                onConfirm?.run()
            }

            return dialog
        }

        fun confirmBlur(context: Activity,
                    title: String = "",
                    message: String = "",
                    onConfirm: Runnable? = null,
                    onCancel: Runnable? = null): DialogWrap {
            val view = LayoutInflater.from(context).inflate(R.layout.dialog_confirm, null)
            view.findViewById<TextView>(R.id.confirm_title).setText(title)
            view.findViewById<TextView>(R.id.confirm_message).setText(message)
            val dialog = customDialogBlurBg(context, view)
            view.findViewById<View>(R.id.btn_cancel).setOnClickListener {
                dialog.dismiss()
                onCancel?.run()
            }
            view.findViewById<View>(R.id.btn_confirm).setOnClickListener {
                dialog.dismiss()
                onConfirm?.run()
            }

            return dialog
        }

        fun customDialog(context: Context, view: View): DialogWrap {
            val dialog = AlertDialog
                    .Builder(context)
                    .setView(view)
                    .setCancelable(true)
                    .create()

            dialog.window?.run {
                setWindowAnimations(R.style.windowAnim2)
            }
            dialog.show()
            dialog.window?.run {
                setBackgroundDrawableResource(android.R.color.transparent)
            }

            return DialogWrap(dialog)
        }

        fun customDialogBlurBg(activity: Activity, view: View): DialogWrap {
            return customDialogBlurBg(activity, view, true)
        }

        fun customDialogBlurBg(activity: Activity, view: View, cancelable: Boolean): DialogWrap {
            val dialog = AlertDialog
                    .Builder(activity, R.style.custom_alert_dialog)
                    .setView(view)
                    .setCancelable(cancelable)
                    .create()

            dialog.show()
            dialog.window?.run {
                // setBackgroundDrawableResource(android.R.color.transparent)

                // TODO:处理模糊背景
                // BlurBackground(activity).setScreenBgLight(dialog)

                // val attrs = attributes
                // attrs.alpha = 0.1f
                // attributes =attrs
                // decorView.setPadding(0, 0, 0, 0)

                setBackgroundDrawable(BitmapDrawable(activity.getResources(), FastBlurUtility.getBlurBackgroundDrawer(activity)))
                decorView.run {
                    systemUiVisibility = activity.window.decorView.systemUiVisibility // View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                }
                /*
                // 隐藏状态栏和导航栏
                decorView.run {
                    systemUiVisibility = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    setOnSystemUiVisibilityChangeListener {
                        var uiOptions = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or  //布局位于状态栏下方
                                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or  //全屏
                                View.SYSTEM_UI_FLAG_FULLSCREEN or  //隐藏导航栏
                                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        uiOptions = uiOptions or 0x00001000
                        systemUiVisibility = uiOptions
                    }
                }
                */

                // setWindowAnimations(R.style.windowAnim2)
            }

            return DialogWrap(dialog)
        }

        fun helpInfo(context: Context, title: Int, message: Int): DialogWrap {
            val dialog =
                    AlertDialog.Builder(context)
                            .setTitle(title)
                            .setMessage(message)
                            .setPositiveButton(R.string.btn_confirm) { _, _ ->
                            }
            return animDialog(dialog)
        }
    }
}

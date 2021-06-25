package com.omarea.krscript.ui

import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.net.Uri
import android.text.Layout
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.*
import android.view.View
import android.widget.TextView
import android.widget.Toast
import com.omarea.common.ui.DialogHelper
import com.omarea.krscript.R
import com.omarea.krscript.TryOpenActivity
import com.omarea.krscript.executor.ScriptEnvironmen
import com.omarea.krscript.model.TextNode


class ListItemText(private val context: Context,
                   layoutId: Int,
                   config: TextNode) : ListItemView(context, layoutId, config) {

    private val rowsView = layout.findViewById<TextView?>(R.id.kr_rows)

    init {
        if (config.rows.size > 0 && rowsView != null) {
            rowsView.movementMethod = LinkMovementMethod.getInstance() // 不设置 ClickableSpan 点击没反应
            // rowsView.setOnClickListener {}

            rowsView.visibility = View.VISIBLE
            for (row in config.rows) {
                if (row.breakRow || row.align != Layout.Alignment.ALIGN_NORMAL) {
                    rowsView.append("\n")
                }
                val text = row.text
                val length = text.length
                val spannableString = SpannableString(text)


                if (row.underline) {
                    spannableString.setSpan(UnderlineSpan(), 0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                }

                if (row.link.isNotEmpty()) {
                    spannableString.setSpan(object : ClickableSpan() {
                        override fun onClick(widget: View) {
                            if (row.link.isNotEmpty()) {
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(row.link))
                                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    context.startActivity(intent)
                                } catch (ex: Exception) {
                                    Toast.makeText(context, context.getString(R.string.kr_slice_activity_fail), Toast.LENGTH_SHORT).show()
                                }
                            }
                        }

                        override fun updateDrawState(ds: TextPaint) {
                            ds.color = if (row.color != 1) ds.linkColor else row.color
                            ds.isUnderlineText = row.underline
                        }
                    }, 0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                }

                if (row.activity.isNotEmpty()) {
                    spannableString.setSpan(object : ClickableSpan() {
                        override fun onClick(widget: View) {
                            TryOpenActivity(context, row.activity).tryOpen()
                        }

                        override fun updateDrawState(ds: TextPaint) {
                            ds.color = if (row.color != 1) ds.linkColor else row.color
                            ds.isUnderlineText = row.underline
                        }
                    }, 0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                }

                if (row.onClickScript.isNotEmpty()) {
                    spannableString.setSpan(object : ClickableSpan() {
                        override fun onClick(widget: View) {
                            val result = ScriptEnvironmen.executeResultRoot(context, row.onClickScript, config)
                            if (result.trim().isNotEmpty()) {
                                DialogHelper.helpInfo(context, context.getString(R.string.kr_slice_script_result), result)
                            }
                        }

                        override fun updateDrawState(ds: TextPaint) {
                            ds.color = if (row.color != 1) ds.linkColor else row.color
                            ds.isUnderlineText = row.underline
                        }
                    }, 0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                }

                if (row.color != -1) {
                    spannableString.setSpan(ForegroundColorSpan(row.color), 0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                }

                if (row.bgColor != -1) {
                    spannableString.setSpan(BackgroundColorSpan(row.bgColor), 0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                }

                if (row.bold && row.italic) {
                    spannableString.setSpan(StyleSpan(Typeface.BOLD_ITALIC), 0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                } else if (row.bold) {
                    spannableString.setSpan(StyleSpan(Typeface.BOLD), 0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                } else if (row.italic) {
                    spannableString.setSpan(StyleSpan(Typeface.ITALIC), 0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                }

                if (row.size != -1) {
                    spannableString.setSpan(AbsoluteSizeSpan(row.size, true), 0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                }

                spannableString.setSpan(AlignmentSpan.Standard(row.align), 0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

                rowsView.append(spannableString)
            }
            // NOTE: 修补 android.widget.Editor.touchPositionIsInSelection(Editor.java:1363) 导致的奔溃
            rowsView.setOnLongClickListener {
                true
            }
        } else {
            rowsView?.visibility = View.GONE
        }
    }
}

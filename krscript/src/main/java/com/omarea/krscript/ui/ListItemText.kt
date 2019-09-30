package com.omarea.krscript.ui

import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.net.Uri
import android.text.Layout
import android.text.SpannableString
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.*
import android.view.View
import android.widget.TextView
import android.widget.Toast
import com.omarea.krscript.R
import com.omarea.krscript.model.TextInfo


class ListItemText(private val context: Context,
                   private val layoutId: Int,
                   private val config: TextInfo = TextInfo()) : ListItemView(context, layoutId, config) {

    private val rowsView = layout.findViewById<TextView?>(R.id.kr_rows)

    init {
        if (config.rows.size > 0 && rowsView != null) {
            rowsView.setMovementMethod(LinkMovementMethod.getInstance()) // 不设置 ClickableSpan 点击没反应
            // rowsView.setOnClickListener {}

            rowsView.visibility = View.VISIBLE
            for (row in config.rows) {
                if (row.breakRow || row.align != Layout.Alignment.ALIGN_NORMAL) {
                    rowsView.append("\n")
                }
                val text = row.text
                val length = text.length
                val spannableString = SpannableString(text)

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


                if (row.underline) {
                    spannableString.setSpan(UnderlineSpan(), 0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                }

                if (row.link.isNotEmpty()) {
                    spannableString.setSpan(object : ClickableSpan() {
                        override fun onClick(widget: View) {
                            if (row.link.isNotEmpty()) {
                                try {
                                    val uri = Uri.parse(row.link)
                                    val intent = Intent(Intent.ACTION_VIEW, uri)
                                    context.startActivity(intent)
                                } catch (ex: Exception) {
                                    Toast.makeText(context, "无法打开活动~", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }, 0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                }

                if (row.activity.isNotEmpty()) {
                    spannableString.setSpan(object : ClickableSpan() {
                        override fun onClick(widget: View) {
                            if (row.activity.isNotEmpty()) {
                                try {
                                    val intent = Intent(row.activity)
                                    context.startActivity(intent)
                                } catch (ex: Exception) {
                                    Toast.makeText(context, "无法打开活动~", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }, 0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                }

                rowsView.append(spannableString)
            }
        } else {
            rowsView?.visibility = View.GONE
        }
    }
}

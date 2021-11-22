package com.omarea.ui

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageButton
import android.widget.RelativeLayout
import com.omarea.common.ui.DialogHelper
import com.omarea.store.SpfConfig
import com.omarea.vtools.R

class HelpIcon : RelativeLayout {
    private fun init(context: Context?, attrs: AttributeSet?) {
        val view: View = View.inflate(context, R.layout.layout_help_icon, this) as View
        val config = context?.getSharedPreferences(SpfConfig.GLOBAL_SPF, Context.MODE_PRIVATE)
        config?.run {
            if (!getBoolean(SpfConfig.GLOBAL_SPF_HELP_ICON, true)) {
                view.visibility = View.GONE
            }
        }
        attrs?.run {
            for (i in 0 until attributeCount) {
                val attrName = getAttributeName(i)
                if (attrName == "text") {
                    val attrValue = getAttributeValue(i)
                    val text = if (attrValue.startsWith("@")) context!!.getString(attrValue.replace("@", "").toInt()) else attrValue
                    view.findViewById<ImageButton>(android.R.id.button1).setOnClickListener {
                        /*
                        if (text.length < 50) {
                            Snackbar.make(it, text, Snackbar.LENGTH_LONG).show()
                        } else {
                            DialogHelper.helpInfo(context!!, text)
                        }
                        */
                        DialogHelper.helpInfo(context!!, text)
                    }
                } else if (attrName == "layout_res") { // attrValue 因为Layout资源id，例如 app:layout_res="@layout/dialog_power_legend"
                    val attrValue = getAttributeValue(i)
                    view.findViewById<ImageButton>(android.R.id.button1).setOnClickListener {
                        DialogHelper.helpInfo(
                                context!!,
                                context.getString(R.string.help_title),
                                "",
                                LayoutInflater.from(context).inflate(
                                    attrValue.replace("@", "").toInt(),
                                    null
                                )
                        )
                    }
                }
            }
        }
    }

    constructor(context: Context?) : super(context) {
        init(context, null)
    }

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
        init(context, attrs)
    }

    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        init(context, attrs)
    }

    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes) {
        init(context, attrs)
    }
}
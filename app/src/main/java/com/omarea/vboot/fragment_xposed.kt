package com.omarea.vboot

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.ViewGroup
import com.omarea.shared.cmd_shellTools
import com.omarea.shared.xposed_check
import kotlinx.android.synthetic.main.layout_xposed.*


class fragment_xposed : Fragment() {
    internal var cmdshellTools: cmd_shellTools? = null
    internal var thisview: main? = null

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater!!.inflate(R.layout.layout_xposed, container, false)
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        xposed_tabs.setup();

        xposed_tabs.addTab(xposed_tabs.newTabSpec("tab_a").setContent(R.id.xposed_tab_a).setIndicator(getString(R.string.xposed_tab_a)));
        xposed_tabs.addTab(xposed_tabs.newTabSpec("tab_b").setContent(R.id.xposed_tab_b).setIndicator(getString(R.string.xposed_tab_b)));
        xposed_tabs.setCurrentTab(0);

        vbootxposedservice_state.setOnClickListener {
            try {
                var intent = context.packageManager.getLaunchIntentForPackage("de.robv.android.xposed.installer");
                //intent.putExtra("section", "modules")
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
            } catch (e: Exception) {
                Snackbar.make(getView()!!, context.getString(R.string.xposed_cannot_openxposed), Snackbar.LENGTH_SHORT).show()
            }
        }

        if (xposed_check.xposedIsRunning())
            vbootxposedservice_state.visibility = GONE;
    }

    companion object {
        fun Create(thisView: main, cmdshellTools: cmd_shellTools): Fragment {
            val fragment = fragment_xposed()
            fragment.cmdshellTools = cmdshellTools
            fragment.thisview = thisView
            return fragment
        }
    }

}

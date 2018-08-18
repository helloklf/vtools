package com.omarea.vtools

import android.content.Intent
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.omarea.shell.KeepShellPublic


class FragmentRecent : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? =
            inflater.inflate(R.layout.layout_recent, container, false)


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
    }

    override fun onResume() {
        super.onResume()
        val recentData = KeepShellPublic.doCmdSync("dumpsys activity recents").split("\n")

        for (row in recentData) {

        }
    }

    companion object {
        fun createPage(): Fragment {
            val fragment = FragmentRecent()
            return fragment
        }
    }
}

package com.omarea.vboot

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

class fragment_helpinfo : Fragment() {

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater!!.inflate(R.layout.layout_helpinfo, container, false)
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {

    }
}

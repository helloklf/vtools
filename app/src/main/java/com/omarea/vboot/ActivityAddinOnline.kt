package com.omarea.vboot

import android.annotation.SuppressLint
import android.content.Context
import android.content.DialogInterface
import android.content.SharedPreferences
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.webkit.JsPromptResult
import android.webkit.JsResult
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.widget.EditText
import android.widget.TextView
import com.omarea.scripts.VToolsOnlineNative
import com.omarea.shared.SpfConfig
import kotlinx.android.synthetic.main.activity_addin_online.*


class ActivityAddinOnline : AppCompatActivity() {
    private lateinit var spf: SharedPreferences

    override fun onPostResume() {
        super.onPostResume()
        delegate.onPostResume()

    }

    @SuppressLint("ApplySharedPref")
    override fun onCreate(savedInstanceState: Bundle?) {
        spf = getSharedPreferences(SpfConfig.GLOBAL_SPF, Context.MODE_PRIVATE)
        if (spf.getBoolean(SpfConfig.GLOBAL_SPF_NIGHT_MODE, false))
            this.setTheme(R.style.AppTheme_NoActionBarNight)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_addin_online)
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onStart() {
        super.onStart()

        vtools_online.loadUrl("http://omarea.com/")
        //vtools_online.setWebChromeClient(WebChromeClient())
        //vtools_online.setWebViewClient(WebViewClient())
        vtools_online.settings.javaScriptEnabled = true
        vtools_online.settings.setLoadWithOverviewMode(true);
        vtools_online.settings.setUseWideViewPort(true);
        //vtools_online.loadUrl("http://192.168.2.144/")

        vtools_online.loadUrl("file:///android_asset/index.html")
        vtools_online.addJavascriptInterface(VToolsOnlineNative(this, vtools_online), "VToolsNative")
    }

    public override fun onPause() {
        super.onPause()
    }
}

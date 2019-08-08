package com.omarea.krscript;

import android.annotation.SuppressLint;
import android.content.Context;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;

import com.omarea.krscript.executor.ScriptEnvironmen;

public class WebViewInjector {
    private WebView webView;
    private Context context;

    public WebViewInjector(WebView webView, Context context) {
        this.webView = webView;
        this.context = context;
    }

    @SuppressLint("JavascriptInterface")
    public void inject() {
        if (webView != null) {
            webView.addJavascriptInterface(
                    new KrScriptEngine(context),
                    KrScriptEngine.class.getSimpleName()
            );
        }
    }

    private class KrScriptEngine {
        private Context context;

        private KrScriptEngine(Context context) {
            this.context = context;
        }

        /**
         * 同步执行shell脚本 并返回结果（不包含错误信息）
         * @param script 脚本内容
         * @return 执行过程中的输出内容
         */
        @JavascriptInterface
        public String executeShell(String script) {
            if (script != null && !script.isEmpty()) {
                return ScriptEnvironmen.executeResultRoot(context, script);
            }
            return "";
        }

        public void executeShellAsync(String script) {

        }
    }
}

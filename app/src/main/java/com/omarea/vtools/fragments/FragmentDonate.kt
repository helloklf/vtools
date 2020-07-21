package com.omarea.vtools.fragments

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.omarea.utils.AlipayDonate
import com.omarea.vtools.R
import kotlinx.android.synthetic.main.fragment_donate.*


class FragmentDonate : androidx.fragment.app.Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? =
            inflater.inflate(R.layout.fragment_donate, container, false)

    override fun onResume() {
        super.onResume()
        // activity!!.title = getString(R.string.menu_paypal)
        activity!!.title = getString(R.string.app_name)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        pay_paypal.setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.paypal.me/duduski")))
        }
        pay_alipay.setOnClickListener {
            AlipayDonate(context!!).jumpAlipay()
        }
        pay_wxpay.setOnClickListener {
            /*
            try {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("weixin://dl/business/?ticket=wxp://f2f0YqS-OUviH9sQNUDgXJhOP3fld3htEqqO")))
            } catch (ex: Exception) {
                Toast.makeText(context!!, "暂不支持此方式！", Toast.LENGTH_SHORT).show()
            }
            */
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("http://vtools.omarea.com/")))
            Toast.makeText(context!!, "暂不支持直接调起，请保存收款码然后使用微信扫码（在扫一扫界面从相册选择图片）！", Toast.LENGTH_SHORT).show()
        }
        pay_alipay_code.setOnClickListener {
            //获取剪贴板管理器：
            val cm = context!!.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            // 创建普通字符型ClipData
            val mClipData = ClipData.newPlainText("支付宝红包码", "511531087")
            // 将ClipData内容放到系统剪贴板里
            cm.setPrimaryClip(mClipData)
            Toast.makeText(context!!, "红包码已复制！", Toast.LENGTH_SHORT).show()
            try {
                val packageManager = context!!.getApplicationContext().getPackageManager()
                val intent = packageManager.getLaunchIntentForPackage("com.eg.android.AlipayGphone")
                startActivity(intent)
            } catch (ex: Exception) {
            }
        }
        pay_alipay_command.setOnClickListener {
            //获取剪贴板管理器：
            val cm = context!!.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            // 创建普通字符型ClipData
            val mClipData = ClipData.newPlainText("支付宝口令", "支付宝发红包啦！即日起还有机会额外获得余额宝消费红包！长按复制此消息，打开最新版支付宝就能领取！Z3DGmD87Rf")
            // 将ClipData内容放到系统剪贴板里。
            cm.setPrimaryClip(mClipData)
            Toast.makeText(context!!, "红包口令已复制！", Toast.LENGTH_SHORT).show()
            try {
                val packageManager = context!!.getApplicationContext().getPackageManager()
                val intent = packageManager.getLaunchIntentForPackage("com.eg.android.AlipayGphone")
                startActivity(intent)
            } catch (ex: Exception) {
            }
        }
    }

    companion object {
        fun createPage(): androidx.fragment.app.Fragment {
            val fragment = FragmentDonate()
            return fragment
        }
    }
}

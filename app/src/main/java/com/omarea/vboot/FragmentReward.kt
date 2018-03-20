package com.omarea.vboot

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import kotlinx.android.synthetic.main.layout_reward.*


class FragmentReward : Fragment() {
    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? =
            inflater!!.inflate(R.layout.layout_reward, container, false)

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        reward_alipay.setOnClickListener({
            try {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("HTTPS://QR.ALIPAY.COM/FKX04429SWVGJZNETBJHD6".toLowerCase())))
            } catch (ex: Exception) {
                Toast.makeText(context, "调用浏览器失败~", Toast.LENGTH_SHORT).show()
            }
        })
        reward_wechat.setOnClickListener({
            try {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("wxp://f2f0B9VzSuz_RlKssYfgIt1vaIaMb9xmNaiF")))
            } catch (ex: Exception) {
                Toast.makeText(context, "启动微信失败~", Toast.LENGTH_SHORT).show()
            }
        })
    }

    companion object {
        fun createPage(): Fragment {
            val fragment = FragmentReward()
            return fragment
        }
    }
}

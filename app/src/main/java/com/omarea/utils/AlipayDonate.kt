package com.omarea.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import java.net.URLEncoder


/**
 * 测试不集成sdk,直接对商户 个人二维码 个人收款码进行转账
 * 个人二维码可以截图下来.通过扫码工具获得里面的字符串
 */
class AlipayDonate(private var context: Context) {

    private val ALIPAY_SHOP = "HTTPS://QR.ALIPAY.COM/FKX05665KXCDCLC2YAEL0E"//商户
    private val ALIPAY_PERSON = "HTTPS://QR.ALIPAY.COM/FKX05665KXCDCLC2YAEL0E"//个人(支付宝里面我的二维码)
    private val ALIPAY_PERSON_2_PAY = "HTTPS://QR.ALIPAY.COM/FKX05665KXCDCLC2YAEL0E"//个人(支付宝里面我的二维码,然后提示让用的收款码)

    public fun jumpAlipay() {
        openAliPay2Pay(ALIPAY_SHOP)
    }

    /**
     * 支付
     * @param qrCode
     */
    private fun openAliPay2Pay(qrCode: String) {
        openAlipayPayPage(context, qrCode)
    }

    fun openAlipayPayPage(context: Context, qrcode: String): Boolean {
        var encodeedQrcode = qrcode
        try {
            encodeedQrcode = URLEncoder.encode(qrcode, "utf-8")
        } catch (e: Exception) {
        }
        try {
            val alipayqr = "alipayqr://platformapi/startapp?saId=10000007&clientVersion=3.7.0.0718&qrcode=$encodeedQrcode"
            val url = alipayqr + "%3F_s%3Dweb-other&_t=" + System.currentTimeMillis()
            openUri(context, url)
            return true
        } catch (e: Exception) {
        }
        return false
    }

    /**
     * 发送一个intent
     * @param context
     * @param s
     */
    private fun openUri(context: Context, s: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(s))
        context.startActivity(intent)
    }
}

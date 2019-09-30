package com.omarea.krscript.config

import android.content.Context
import android.util.Xml
import android.widget.Toast
import com.omarea.krscript.executor.ExtractAssets
import com.omarea.krscript.executor.ScriptEnvironmen
import com.omarea.krscript.model.PageInfo
import org.xmlpull.v1.XmlPullParser
import java.io.InputStream

class PageListReader(private val context: Context) {
    private val ASSETS_FILE = "file:///android_asset/"
    private fun getConfig(context: Context, filePath: String): InputStream? {
        try {
            if (filePath.startsWith(ASSETS_FILE)) {
                return context.assets.open(filePath.substring(ASSETS_FILE.length))
            } else {
                return context.assets.open(filePath)
            }
        } catch (ex: Exception) {
            return null
        }
    }

    fun readPageList(filePath: String): ArrayList<PageInfo> {
        val extractAssets = ExtractAssets(context)
        val pages = ArrayList<PageInfo>()

        try {
            val fileInputStream = getConfig(context, filePath) ?: return ArrayList()
            val parser = Xml.newPullParser()// 获取xml解析器
            parser.setInput(fileInputStream, "utf-8")// 参数分别为输入流和字符编码
            var type = parser.eventType
            var page: PageInfo? = null
            while (type != XmlPullParser.END_DOCUMENT) {// 如果事件不等于文档结束事件就继续循环
                when (type) {
                    XmlPullParser.START_TAG -> {
                        val name = parser.name
                        if (name == "page" && page == null) {
                            page = PageInfo()
                            for (attrIndex in 0 until parser.attributeCount) {
                                val attrName = parser.getAttributeName(attrIndex)
                                if (attrName == "support") {
                                    if (executeResultRoot(context, parser.getAttributeValue(attrIndex)) != "1") {
                                        page = null
                                        break
                                    }
                                } else if (attrName == "config") {
                                    val value = parser.getAttributeValue(attrIndex)
                                    page!!.pageConfigPath = value
                                } else if (attrName == "html") {
                                    val value = parser.getAttributeValue(attrIndex)
                                    page!!.onlineHtmlPage = value
                                } else if (attrName == "title") {
                                    val value = parser.getAttributeValue(attrIndex)
                                    page!!.title = value
                                } else if (attrName == "desc") {
                                    val value = parser.getAttributeValue(attrIndex)
                                    page!!.desc = value
                                }
                            }
                        } else if (page != null) {
                            if (name == "title") {
                                page.title = parser.nextText();
                            } else if (name == "desc") {
                                page.desc = parser.nextText();
                            } else if (name == "config") {
                                page.pageConfigPath = parser.nextText();
                            } else if ("resource" == parser.name) {
                                for (i in 0 until parser.attributeCount) {
                                    if (parser.getAttributeName(i) == "file") {
                                        val file = parser.getAttributeValue(i).trim()
                                        if (file.startsWith(ASSETS_FILE)) {
                                            extractAssets.extractResource(file)
                                        }
                                    } else if (parser.getAttributeName(i) == "dir") {
                                        val file = parser.getAttributeValue(i).trim()
                                        if (file.startsWith(ASSETS_FILE)) {
                                            extractAssets.extractResources(file)
                                        }
                                    }
                                }
                            }
                        } else if ("resource" == parser.name) {
                            for (i in 0 until parser.attributeCount) {
                                if (parser.getAttributeName(i) == "file") {
                                    val file = parser.getAttributeValue(i).trim()
                                    if (file.startsWith(ASSETS_FILE)) {
                                        extractAssets.extractResource(file)
                                    }
                                } else if (parser.getAttributeName(i) == "dir") {
                                    val file = parser.getAttributeValue(i).trim()
                                    if (file.startsWith(ASSETS_FILE)) {
                                        extractAssets.extractResources(file)
                                    }
                                }
                            }
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        if (parser.name == "page") {
                            if (page != null) {
                                pages.add(page)
                                page = null
                            }
                        }
                    }
                }
                type = parser.next()// 继续下一个事件
            }
        } catch (ex: Exception) {
            Toast.makeText(context, ex.message, Toast.LENGTH_LONG).show()
        }
        return pages;
    }

    private fun executeResultRoot(context: Context, scriptIn: String): String {
        return ScriptEnvironmen.executeResultRoot(context, scriptIn);
    }
}

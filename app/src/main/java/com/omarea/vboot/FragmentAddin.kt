package com.omarea.vboot

import android.os.Bundle
import android.os.Handler
import android.support.design.widget.Snackbar
import android.support.v4.app.Fragment
import android.support.v7.app.AlertDialog
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.omarea.shared.AppShared
import com.omarea.shared.Consts
import com.omarea.shell.SuDo
import com.omarea.shell.units.FlymeUnit
import com.omarea.shell.units.FullScreenSUnit
import com.omarea.shell.units.NubiaUnit
import com.omarea.shell.units.QQStyleUnit
import com.omarea.vboot.dialogs.DialogAddinModifyDPI
import com.omarea.vboot.dialogs.DialogAddinModifydevice
import kotlinx.android.synthetic.main.layout_addin.*
import java.util.*


class FragmentAddin : Fragment() {
    internal var thisview: ActivityMain? = null
    lateinit internal var progressBar: ProgressBar
    internal val myHandler: Handler = Handler()

    private fun createItem(title: String, desc: String): HashMap<String, Any> {
        val item = HashMap<String, Any>()
        item.put("Title", title)
        item.put("Desc", desc)
        return item
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? =
            inflater!!.inflate(R.layout.layout_addin, container, false)


    private fun initSoftAddin(view: View) {
        val listView = view.findViewById(R.id.addin_soft_listview) as ListView
        val listItem = ArrayList<HashMap<String, Any>>()/*在数组中存放数据*/

        listItem.add(createItem("QQ净化", "干掉QQ个性气泡、字体、头像挂件，会重启QQ"))
        listItem.add(createItem("QQ净化恢复", "恢复QQ个性气泡、字体、头像挂件，会重启QQ"))
        listItem.add(createItem("MIUI一键精简", "删除（由于部分应用无法冻结，只能删除）或冻结系统中一些内置的无用软件，会重启手机。"))
        listItem.add(createItem("Nubia一键精简", "冻结系统中一些内置的无用软件。"))
        listItem.add(createItem("Flyme一键精简", "删除系统中一些内置的无用软件。"))
        listItem.add(createItem("开启沉浸模式", "自动隐藏状态栏、导航栏"))
        listItem.add(createItem("禁用沉浸模式", "恢复状态栏、导航栏自动显示"))
        listItem.add(createItem("减少Flyme6模糊", "禁用Flyme6下拉通知中心的实时模糊效果，以减少在游戏或视频播放时下拉通知中心的卡顿，或许还能省电"))
        listItem.add(createItem("MIUI9去通知中心搜索", "默认隐藏MIUI9系统下拉通知中心的搜索框"))

        val mSimpleAdapter = SimpleAdapter(
                view.context, listItem,
                R.layout.action_row_item,
                arrayOf("Title", "Desc"),
                intArrayOf(R.id.Title, R.id.Desc)
        )
        listView.adapter = mSimpleAdapter


        listView.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
            val builder = AlertDialog.Builder(thisview!!)
            builder.setTitle("执行这个脚本？")
            builder.setNegativeButton(android.R.string.cancel, null)
            builder.setPositiveButton(android.R.string.yes) { _, _ -> executeSoftScript(view, position) }
            builder.setMessage(
                    listItem[position]["Title"].toString() + "：" + listItem[position]["Desc"] +
                            "\n\n请确保你已了解此脚本的用途，并清楚对设备的影响")
            builder.create().show()
        }
    }

    private fun executeSoftScript(view: View, position: Int) {
        val stringBuilder = StringBuilder()
        when (position) {
            0 -> {
                QQStyleUnit().DisableQQStyle()
                return
            }
            1 -> {
                QQStyleUnit().RestoreQQStyle()
                return
            }
            2 -> {
                stringBuilder.append(Consts.MountSystemRW)
                stringBuilder.append(Consts.MiuiUninstall)
            }
            3 -> {
                NubiaUnit().DisableSomeApp()
                return
            }
            4 -> {
                stringBuilder.append(Consts.MountSystemRW)
                stringBuilder.append(Consts.FlymeUninstall)
            }
            5 -> {
                FullScreenSUnit().FullScreen()
                return
            }
            6 -> {
                FullScreenSUnit().ExitFullScreen()
                return
            }
            7 -> {
                FlymeUnit().StaticBlur()
                return
            }
            8 -> {
                AppShared.WriteFile(context.assets, "com.android.systemui", "com.android.systemui")
                stringBuilder.append(Consts.MountSystemRW)
                stringBuilder.append("cp ${Consts.SDCardDir}/Android/data/${Consts.PACKAGE_NAME}/com.android.systemui /system/media/theme/default/com.android.systemui\n")
                stringBuilder.append("chmod 0644 /system/media/theme/default/com.android.systemui\n")
                stringBuilder.append(Consts.Reboot)
            }
        }
        progressBar.visibility = View.VISIBLE
        Thread(Runnable {
            SuDo(context).execCmdSync(stringBuilder.toString())
            myHandler.post {
                Snackbar.make(view, "命令已执行！", Snackbar.LENGTH_SHORT).show()
                progressBar.visibility = View.GONE
            }
        }).start()
    }


    private fun initSystemAddin(view: View) {
        val listView = view.findViewById(R.id.addin_system_listview) as ListView
        val listItem = ArrayList<HashMap<String, Any>>()/*在数组中存放数据*/

        listItem.add(createItem("内存清理", "Linux标准缓存清理命令：echo 3 > /proc/sys/vm/drop_caches"))
        listItem.add(createItem("干掉温控模块", "这可以确保手机性能更加稳定，但会显著增加发热，同时也会导致MIUI系统的CPU智能调度失效，需要重启手机。"))
        listItem.add(createItem("恢复温控模块", "如果你后悔了想把温控还原回来，可以点这个。需要重启手机"))
        listItem.add(createItem("删除锁屏密码", "如果你忘了锁屏密码，或者恢复系统后密码不正确，这能帮你解决。会重启手机"))
        listItem.add(createItem("禁止充电", "停止对电池充电，同时使用USB电源为手机供电。（与充电加速和电池保护功能冲突！）"))
        listItem.add(createItem("恢复充电", "恢复对电池充电，由设备自行管理充放。"))
        listItem.add(createItem("改机型为OPPO R11", "将机型信息改为OPPO R11 Plus，以便在王者荣耀或获得专属优化体验。（会导致部分设备软件fc）"))
        listItem.add(createItem("改机型为vivo X20", "将机型信息改为vivo X20，据说比改OPPO R11还好用？（会导致部分设备软件fc）"))
        listItem.add(createItem("build.prop参数还原", "使用了DPI修改和机型伪装的小伙伴，可以点这个还原到上次修改前的状态"))


        val mSimpleAdapter = SimpleAdapter(
                view.context, listItem,
                R.layout.action_row_item,
                arrayOf("Title", "Desc"),
                intArrayOf(R.id.Title, R.id.Desc)
        )
        listView.adapter = mSimpleAdapter


        listView.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
            val builder = AlertDialog.Builder(thisview!!)
            builder.setTitle("执行这个脚本？")
            builder.setNegativeButton(android.R.string.cancel, null)
            builder.setPositiveButton(android.R.string.yes) { _, _ -> executeSystemScript(view, position) }
            builder.setMessage(
                    listItem[position]["Title"].toString() + "：" + listItem[position]["Desc"] +
                            "\n\n请确保你已了解此脚本的用途，并清楚对设备的影响")
            builder.create().show()
        }
    }

    private fun executeSystemScript(view: View, position: Int) {
        val stringBuilder = StringBuilder()
        when (position) {
            0 -> {
                stringBuilder.append("echo 3 > /proc/sys/vm/drop_caches")
            }
            1 -> {
                stringBuilder.append(Consts.MountSystemRW)
                stringBuilder.append(Consts.RMThermal)
            }
            2 -> {
                stringBuilder.append(Consts.MountSystemRW)
                stringBuilder.append(Consts.ResetThermal)
            }
            3 -> {
                stringBuilder.append(Consts.DeleteLockPwd)
            }
            4 -> {
                stringBuilder.append(Consts.DisableChanger)
            }
            5 -> {
                stringBuilder.append(Consts.ResumeChanger)
            }
            6 -> {
                stringBuilder.append(Consts.MountSystemRW)
                stringBuilder.append(
                        "busybox sed 's/^ro.product.model=.*/ro.product.model=OPPO R11 Plus/' /system/build.prop > /data/build.prop;" +
                                "busybox sed -i 's/^ro.product.brand=.*/ro.product.brand=OPPO/' /data/build.prop;" +
                                "busybox sed -i 's/^ro.product.name=.*/ro.product.name=R11 Plus/' /data/build.prop;" +
                                "busybox sed -i 's/^ro.product.device=.*/ro.product.device=R11 Plus/' /data/build.prop;" +
                                "busybox sed -i 's/^ro.build.product=.*/ro.build.product=R11 Plus/' /data/build.prop;" +
                                "busybox sed -i 's/^ro.product.manufacturer=.*/ro.product.manufacturer=OPPO/' /data/build.prop;")
                stringBuilder.append("cp /system/build.prop /system/build.bak.prop\n")
                stringBuilder.append("cp /data/build.prop /system/build.prop\n")
                stringBuilder.append("rm /data/build.prop\n")
                stringBuilder.append("chmod 0644 /system/build.prop\n")
                stringBuilder.append("sync\n")
                stringBuilder.append("reboot\n")
            }
            7 -> {
                stringBuilder.append(Consts.MountSystemRW)
                stringBuilder.append(
                        "busybox sed 's/^ro.product.model=.*/ro.product.model=vivo X20/' /system/build.prop > /data/build.prop;" +
                                "busybox sed -i 's/^ro.product.brand=.*/ro.product.brand=vivo/' /data/build.prop;" +
                                "busybox sed -i 's/^ro.product.name=.*/ro.product.name=X20/' /data/build.prop;" +
                                "busybox sed -i 's/^ro.product.device=.*/ro.product.device=X20/' /data/build.prop;" +
                                "busybox sed -i 's/^ro.build.product=.*/ro.build.product=X20/' /data/build.prop;" +
                                "busybox sed -i 's/^ro.product.manufacturer=.*/ro.product.manufacturer=vivo/' /data/build.prop;")
                stringBuilder.append("cp /system/build.prop /system/build.bak.prop\n")
                stringBuilder.append("cp /data/build.prop /system/build.prop\n")
                stringBuilder.append("rm /data/build.prop\n")
                stringBuilder.append("chmod 0644 /system/build.prop\n")
                stringBuilder.append("sync\n")
                stringBuilder.append("reboot\n")
            }
            8 -> {
                stringBuilder.append(Consts.MountSystemRW)
                stringBuilder.append("if [ -f '/system/build.bak.prop' ];then rm /system/build.prop;cp /system/build.bak.prop /system/build.prop;chmod 0644 /system/build.prop; sync; reboot; fi;")
            }
        }
        progressBar.visibility = View.VISIBLE
        Thread(Runnable {
            SuDo(context).execCmdSync(stringBuilder.toString())
            myHandler.post {
                Snackbar.make(view, "命令已执行！", Snackbar.LENGTH_SHORT).show()
                progressBar.visibility = View.GONE
            }
        }).start()
    }

    private fun initCustomAddin(view: View) {
        val listItem = ArrayList<HashMap<String, Any>>()/*在数组中存放数据*/
        listItem.add(createItem("DPI、分辨率修改", "自定义手机DPI或分辨率，这可能导致设备无法正常启动或UI错误"))
        listItem.add(createItem("机型修改", "通过更改build.prop，把机型修改成别的手机"))

        val mSimpleAdapter = SimpleAdapter(
                view.context, listItem,
                R.layout.action_row_item,
                arrayOf("Title", "Desc"),
                intArrayOf(R.id.Title, R.id.Desc)
        )
        addin_custom_listview.adapter = mSimpleAdapter


        addin_custom_listview.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
            if (position == 0) {
                DialogAddinModifyDPI(context).modifyDPI(thisview!!.windowManager.defaultDisplay)
            } else if (position == 1) {
                DialogAddinModifydevice(context).modifyDeviceInfo()
            }
        }
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        val tabHost = view!!.findViewById(R.id.addinlist_tabhost) as TabHost

        tabHost.setup()

        tabHost.addTab(tabHost.newTabSpec("def_tab").setContent(R.id.system).setIndicator("系统"))
        tabHost.addTab(tabHost.newTabSpec("game_tab").setContent(R.id.soft).setIndicator("软件"))
        tabHost.addTab(tabHost.newTabSpec("custom_tab").setContent(R.id.custom).setIndicator("定制"))
        tabHost.currentTab = 0

        initSystemAddin(view)
        initSoftAddin(view)
        initCustomAddin(view)
    }

    companion object {

        fun createPage(thisView: ActivityMain): Fragment {
            val fragment = FragmentAddin()
            fragment.thisview = thisView
            fragment.progressBar = thisView.progressBar
            return fragment
        }
    }
}// Required empty public constructor

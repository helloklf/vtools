package com.omarea.vboot

import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v4.app.Fragment
import android.support.v7.app.AlertDialog
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ListView
import android.widget.SimpleAdapter
import android.widget.TabHost
import com.omarea.shared.Consts
import com.omarea.shared.cmd_shellTools
import java.util.*


class fragment_addin : Fragment() {


    internal var cmdshellTools: cmd_shellTools? = null
    internal var thisview: main? = null

    fun createItem(title: String, desc: String): HashMap<String, Any> {
        val item = HashMap<String, Any>()
        item.put("Title", title)
        item.put("Desc", desc)
        return item
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater!!.inflate(R.layout.layout_addin, container, false)
    }


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

        val mSimpleAdapter = SimpleAdapter(
                view.context, listItem,
                R.layout.action_row_item,
                arrayOf("Title", "Desc"),
                intArrayOf(R.id.Title, R.id.Desc)
        )
        listView.adapter = mSimpleAdapter


        listView.onItemClickListener = AdapterView.OnItemClickListener { parent, view, position, id ->
            val builder = AlertDialog.Builder(thisview!!)
            builder.setTitle("执行这个脚本？")
            builder.setNegativeButton(android.R.string.cancel, null)
            builder.setPositiveButton(android.R.string.yes) { dialog, which -> executeSoftScript(view, position) }
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
                stringBuilder.append(Consts.RMQQStyles)
            }
            1 -> {
                stringBuilder.append(Consts.ResetQQStyles)
            }
            2 -> {
                stringBuilder.append(Consts.MountSystemRW)
                stringBuilder.append(Consts.MiuiUninstall)
            }
            3 -> {
                stringBuilder.append(Consts.MountSystemRW)
                stringBuilder.append(Consts.NubiaUninstall)
            }
            4 -> {
                stringBuilder.append(Consts.MountSystemRW)
                stringBuilder.append(Consts.FlymeUninstall)
            }
            5 -> {
                //stringBuilder.append("settings put global policy_control immersive.full=*")
                stringBuilder.append("settings put global policy_control immersive.full=apps,-android,-com.android.systemui,-com.tencent.mobileqq,-com.tencent.tim,-com.tencent.mm")
            }
            6 -> {
                stringBuilder.append("settings put global policy_control null")
            }
        }
        cmdshellTools!!.DoCmd(stringBuilder.toString())
        Snackbar.make(view, "命令已执行！", Snackbar.LENGTH_SHORT).show()
    }


    private fun initSystemAddin(view: View) {
        val listView = view.findViewById(R.id.addin_system_listview) as ListView
        val listItem = ArrayList<HashMap<String, Any>>()/*在数组中存放数据*/

        listItem.add(createItem("内存清理", "Linux标准缓存清理命令：echo 3 > /proc/sys/vm/drop_caches"))
        listItem.add(createItem("开启ZRAM 500M", "该功能需要内核支持。size=0.5GB swappiness=100"))
        listItem.add(createItem("开启ZRAM 1GB", "该功能需要内核支持。size=1.0GB swappiness=100"))
        listItem.add(createItem("开启ZRAM 2GB", "该功能需要内核支持。size=1.8GB swappiness=100"))
        listItem.add(createItem("调整DPI为410", "部分ROM不支持调整，可能出现UI错误。需要重启。"))
        listItem.add(createItem("调整DPI为440", "部分ROM不支持调整，可能出现UI错误。需要重启。"))
        listItem.add(createItem("调整DPI为480", "部分ROM不支持调整，可能出现UI错误。需要重启。"))
        listItem.add(createItem("干掉温控模块", "可能会对系统造成一些影响，请谨慎使用，需要重启手机。此功能对小米5较新系统无效"))
        listItem.add(createItem("恢复温控模块", "需要重启手机"))
        listItem.add(createItem("删除锁屏密码", "如果你忘了锁屏密码，或者恢复系统后密码不正确，这能帮你解决。会重启手机"))
        listItem.add(createItem("强制打盹", "实验性，强制进入Doze模式（如果系统支持）"))
        listItem.add(createItem("禁止充电", "停止对电池充电，同时使用USB电源为手机供电。（与充电加速和电池保护功能冲突！）"))
        listItem.add(createItem("恢复充电", "恢复对电池充电，由设备自行管理充放。"))
        listItem.add(createItem("机型伪装", "将机型信息改为OPPO R11 Plus，以便在王者荣耀或获得专属优化体验。（会导致小米5 Home键轻触不可用！！！）"))


        val mSimpleAdapter = SimpleAdapter(
                view.context, listItem,
                R.layout.action_row_item,
                arrayOf("Title", "Desc"),
                intArrayOf(R.id.Title, R.id.Desc)
        )
        listView.adapter = mSimpleAdapter


        listView.onItemClickListener = AdapterView.OnItemClickListener { parent, view, position, id ->
            val builder = AlertDialog.Builder(thisview!!)
            builder.setTitle("执行这个脚本？")
            builder.setNegativeButton(android.R.string.cancel, null)
            builder.setPositiveButton(android.R.string.yes) { dialog, which -> executeSystemScript(view, position) }
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
                stringBuilder.append(
                        "swapoff /dev/block/zram0\n" +
                                "echo 1 > /sys/block/zram0/reset\n" +
                                "echo 597000000 > /sys/block/zram0/disksize\n" +
                                "mkswap /dev/block/zram0 &> /dev/null\n" +
                                "swapon /dev/block/zram0 &> /dev/null\n" +
                                "echo 100 > /proc/sys/vm/swappiness\n")
            }
            2 -> {
                stringBuilder.append(
                        "swapoff /dev/block/zram0\n" +
                                "echo 1 > /sys/block/zram0/reset\n" +
                                "echo 1097000000 > /sys/block/zram0/disksize\n" +
                                "mkswap /dev/block/zram0 &> /dev/null\n" +
                                "swapon /dev/block/zram0 &> /dev/null\n" +
                                "echo 100 > /proc/sys/vm/swappiness\n")
            }
            3 -> {
                stringBuilder.append(
                        "swapoff /dev/block/zram0\n" +
                                "echo 1 > /sys/block/zram0/reset\n" +
                                "echo 2097000000 > /sys/block/zram0/disksize\n" +
                                "mkswap /dev/block/zram0 &> /dev/null\n" +
                                "swapon /dev/block/zram0 &> /dev/null\n" +
                                "echo 100 > /proc/sys/vm/swappiness\n")
            }
            4 -> {
                stringBuilder.append(Consts.MountSystemRW)
                stringBuilder.append("sed '/ro.sf.lcd_density=/'d /system/build.prop > /data/build.prop\n")
                stringBuilder.append("sed '\$aro.sf.lcd_density=410' /data/build.prop > /data/build2.prop\n")
                stringBuilder.append("cp /system/build.prop /system/build.bak.prop\n")
                stringBuilder.append("cp /data/build2.prop /system/build.prop\n")
                stringBuilder.append("rm /data/build.prop\n")
                stringBuilder.append("rm /data/build2.prop\n")
                stringBuilder.append("chmod 0644 /system/build.prop\n")
                stringBuilder.append("wm size 1080x1920\n")
                stringBuilder.append("sync\n")
                stringBuilder.append("reboot\n")
            }
            5 -> {
                stringBuilder.append(Consts.MountSystemRW)
                stringBuilder.append("sed '/ro.sf.lcd_density=/'d /system/build.prop > /data/build.prop\n")
                stringBuilder.append("sed '\$aro.sf.lcd_density=440' /data/build.prop > /data/build2.prop\n")
                stringBuilder.append("cp /system/build.prop /system/build.bak.prop\n")
                stringBuilder.append("cp /data/build2.prop /system/build.prop\n")
                stringBuilder.append("rm /data/build.prop\n")
                stringBuilder.append("rm /data/build2.prop\n")
                stringBuilder.append("chmod 0644 /system/build.prop\n")
                stringBuilder.append("wm size 1080x1920\n")
                stringBuilder.append("sync\n")
                stringBuilder.append("reboot\n")
            }
            6 -> {
                stringBuilder.append(Consts.MountSystemRW)
                stringBuilder.append("sed '/ro.sf.lcd_density=/'d /system/build.prop > /data/build.prop\n")
                stringBuilder.append("sed '\$aro.sf.lcd_density=480' /data/build.prop > /data/build2.prop\n")
                stringBuilder.append("cp /system/build.prop /system/build.bak.prop\n")
                stringBuilder.append("cp /data/build2.prop /system/build.prop\n")
                stringBuilder.append("rm /data/build.prop\n")
                stringBuilder.append("rm /data/build2.prop\n")
                stringBuilder.append("chmod 0644 /system/build.prop\n")
                stringBuilder.append("wm size 1080x1920\n")
                stringBuilder.append("sync\n")
                stringBuilder.append("reboot\n")
            }
            7 -> {
                stringBuilder.append(Consts.MountSystemRW)
                stringBuilder.append(Consts.RMThermal)
            }
            8 -> {
                stringBuilder.append(Consts.MountSystemRW)
                stringBuilder.append(Consts.ResetThermal)
            }
            9 -> {
                stringBuilder.append(Consts.DeleteLockPwd)
            }
            10 -> {
                stringBuilder.append(Consts.ForceDoze)
            }
            11 -> {
                stringBuilder.append(Consts.DisableChanger)
            }
            12 -> {
                stringBuilder.append(Consts.ResumeChanger)
            }
            13 -> {
                stringBuilder.append(Consts.MountSystemRW)
                stringBuilder.append(
                        "busybox sed 's/^ro.product.model=.*/ro.product.model=OPPO R11 Plus/' /system/build.prop > /data/build.prop;" +
                        "busybox sed -i 's/^ro.product.brand=.*/ro.product.brand=OPPO/' /data/build.prop;" +
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
        }
        cmdshellTools!!.DoCmd(stringBuilder.toString())
        Snackbar.make(view, "命令已执行！", Snackbar.LENGTH_SHORT).show()
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        val tabHost = view!!.findViewById(R.id.addinlist_tabhost) as TabHost

        tabHost.setup()

        tabHost.addTab(tabHost.newTabSpec("def_tab").setContent(R.id.system).setIndicator("系统"))
        tabHost.addTab(tabHost.newTabSpec("game_tab").setContent(R.id.soft).setIndicator("软件"))
        tabHost.currentTab = 0

        initSystemAddin(view)
        initSoftAddin(view)
    }

    companion object {

        fun Create(thisView: main, cmdshellTools: cmd_shellTools): Fragment {
            val fragment = fragment_addin()
            fragment.cmdshellTools = cmdshellTools
            fragment.thisview = thisView
            return fragment
        }
    }
}// Required empty public constructor

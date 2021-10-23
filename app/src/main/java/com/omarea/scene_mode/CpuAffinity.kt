package com.omarea.scene_mode

import android.content.Context
import com.omarea.common.shell.KeepShellPublic
import com.omarea.common.shell.KernelProrp
import com.omarea.library.shell.PlatformUtils
import org.json.JSONArray
import org.json.JSONObject
import java.util.*
import kotlin.collections.LinkedHashMap

class CpuAffinity(private val context: Context) {
    private var apps = LinkedList<String>()
    private var affinityMasks = LinkedHashMap<String, String>()
    private lateinit var options: JSONArray
    private val threads = Collections.synchronizedMap(LinkedHashMap<String, String>())

    init {
        val platform = PlatformUtils().getCPUName()
        try {
            val json = context.assets.open("powercfg/$platform/powercfg_extra.json")
            val jsonStr = String(json.readBytes())
            val jsonObject = JSONObject(jsonStr)

            val coresObj = jsonObject.getJSONObject("affinityMask")
            for (core in coresObj.keys()) {
                affinityMasks.put(core, coresObj.getString(core))
            }

            val appOpts = jsonObject.getJSONArray("apps")
            val optCount = appOpts.length()
            for (i in 0 until optCount) {
                val packages = appOpts.getJSONObject(i).getJSONArray("packages");
                val appCount = packages.length()
                for (j in 0 until appCount) {
                    apps.add(packages.getString(j))
                }
            }

            options = appOpts
        } catch (ex: Exception) {
            // Log.e("@Scene", "" + ex.message)
        }
    }

    private fun getAppConfig(app: String): JSONObject? {
        val optCount = options.length()
        for (i in 0 until optCount) {
            val config = options.getJSONObject(i)
            val packages = config.getJSONArray("packages");
            val appCount = packages.length()
            for (j in 0 until appCount) {
                if (packages.getString(j) == app) {
                    return config
                }
            }
        }

        return null
    }

    private fun getPID (app: String): String {
        return KeepShellPublic.doCmdSync(
        "ps -ef -o PID,NAME | grep -e $app\$ | egrep -o '[0-9]{1,}' | head -n 1"
        )
    }

    private fun getTID (pid: String, thread: String): String {
        return KeepShellPublic.doCmdSync(
        "top -H -n 1 -b -q -m 5 -p $pid | grep '$thread' | head -n 1 | egrep  -o '[0-9]{1,}' | head -n 1"
        )
    }

    private var lastMode: String? = null
    private var lastApp: String? = null
    private var runner: Thread? = null

    fun onPowerCfgChange (mode: String, app: String) {
        if (runner?.isAlive == true) {
            runner!!.interrupt()
            runner = null
        }
        threads.clear()
        val notAppChange = (lastApp == app)

        if (apps.contains(app)) {
            val config = getAppConfig(app)
            if (config != null) {
                Thread {
                    val pid = getPID(app)
                    if (pid.isNotEmpty()) {
                        var runCount = (if (notAppChange) {
                            doConfig(pid, config, mode)
                            1
                        } else {
                            0
                        })

                        while (true) {
                            val waitSeconds: Long = (if (runCount == 0) {
                                10
                            } else if (runCount < 3) {
                                30
                            } else {
                                120
                            })

                            try {
                                Thread.sleep(waitSeconds * 1000)
                            } catch (ex: Exception) {}

                            val current = ModeSwitcher.getCurrentPowermodeApp()
                            if (current != app) {
                                break
                            }

                            doConfig(pid, config, mode)

                            runCount += 1
                        }
                    }
                }.start()
            }
        }
    }

    private fun getAffinity(coreStr: String): String {
        if (affinityMasks.contains(coreStr)) {
            return affinityMasks.get(coreStr)!!
        }
        return "ff"
    }

    private fun matchRule (ruleStr: String, cmd: String): Boolean {
        if (ruleStr == "*" || ruleStr == cmd) {
            return true
        } else if (ruleStr.startsWith("*") && ruleStr.endsWith("*")) {
            if (cmd.contains(ruleStr.subSequence(1, ruleStr.length - 1))) {
                return true
            }
        } else if (ruleStr.endsWith("*")) {
            if (
                    cmd.startsWith(
                        ruleStr.substring(0, ruleStr.length - 1)
                    )
            ) {
                return true
            }
        } else if (ruleStr.startsWith("*")) {
            if (
                    cmd.endsWith(
                        ruleStr.subSequence(1, ruleStr.length)
                    )
            ) {
                return true
            }
        }
        return false
    }

    private fun matchRule (rules: JSONArray, cmd: String, powerMode: String): JSONObject? {
        val ruleCount = rules.length()
        for (i in 0 until ruleCount) {
            val rule = rules.getJSONObject(i)
            val cmdRule = rule.get("cmd")
            val targetMode = if (rule.has("targetMode")) rule.getString("targetMode") else null
            if (!targetMode.isNullOrEmpty() && !targetMode.contains(powerMode)) {
                continue
            }
            if (cmdRule is JSONArray) {
                val colCount = cmdRule.length()
                for (r in 0 until colCount) {
                    val ruleStr = cmdRule.getString(r)
                    if (matchRule(ruleStr, cmd)) {
                        return rule
                    }
                }
            } else {
                val ruleStr = cmdRule.toString()
                if (matchRule(ruleStr, cmd)) {
                    return rule
                }
            }
        }
        return null
    }

    private fun doConfig (pid: String, config: JSONObject, powerMode: String) {
        val rules = if (config.has("rules")) config.getJSONArray("rules") else null
        val heavy = if (config.has("heavy")) config.getJSONArray("heavy") else null

        rules?.run {
            val ruleCount = rules.length()
            if (ruleCount < 1) {
                return@run
            }

            val taskStr = KeepShellPublic.doCmdSync("ls /proc/$pid/task")
            val tasks = taskStr.split("\n")
            tasks.forEach { tid ->
                val cmd = threads.getOrPut(tid, {
                    KernelProrp.getProp("/proc/$pid/task/$tid/comm")
                })

                matchRule(rules, cmd, powerMode)?.run {
                    val affinity = getAffinity(getString("cores"))
                    if (affinity.isNotEmpty()) {
                        KeepShellPublic.doCmdSync("taskset -p '$affinity' $tid > /dev/null 2>&1")
                    }
                }
            }
        }

        heavy?.run {
            val ruleCount = heavy.length()
            if (ruleCount < 1) {
                return@run
            }
            for (i in 0 until ruleCount) {
                val rule = getJSONObject(i)
                val affinity = getAffinity(rule.getString("cores"))
                if (affinity.isNotEmpty()) {
                    val thread = rule.getString("cmd")
                    val tid = getTID(pid, thread)
                    if (tid.isNotEmpty()) {
                        KeepShellPublic.doCmdSync("taskset -p '$affinity' $tid > /dev/null 2>&1")
                    }
                }
            }
        }
    }
}
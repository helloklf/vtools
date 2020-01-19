package com.omarea.common.shell

/**
 * 操作内核参数节点
 * Created by Hello on 2017/11/01.
 */
object KernelProrp {
    /**
     * 获取属性
     * @param propName 属性名称
     * @return
     */
    fun getProp(propName: String): String {
        return KeepShellPublic.doCmdSync("if [[ -e \"$propName\" ]]; then cat \"$propName\"; fi;")
    }

    fun getProp(propName: String, grep: String): String {
        return KeepShellPublic.doCmdSync("if [[ -e \"$propName\" ]]; then cat \"$propName\" | grep \"$grep\"; fi;")
    }

    /**
     * 保存属性
     * @param propName 属性名称（要永久保存，请以persist.开头）
     * @param value    属性值,值尽量是简单的数字或字母，避免出现错误
     */
    fun setProp(propName: String, value: String): Boolean {
        return KeepShellPublic.doCmdSync(
                "chmod 664 \"$propName\" 2 > /dev/null\n" +
                "echo \"$value\" > \"$propName\""
        ) != "error"
    }
}
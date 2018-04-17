package com.omarea.shell;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;

/**
 * 操作内核参数节点
 * Created by Hello on 2017/11/01.
 */

public class KernelProrp {
    /**
     * 获取属性
     *
     * @param propName 属性名称
     * @return
     */
    public static String getProp(String propName) {
        return getProp(propName, false);
    }
    /**
     * 获取属性
     *
     * @param propName 属性名称
     * @return
     */
    public static String getProp(String propName, Boolean root) {
        if (!new File(propName).exists()) {
            return "";
        }
        try {
            Process p = Runtime.getRuntime().exec(root ? "su" : "sh");
            DataOutputStream out = new DataOutputStream(p.getOutputStream());
            out.write(("cat " + propName).getBytes("UTF-8"));
            out.writeBytes("\n");
            out.writeBytes("exit\n");
            out.writeBytes("exit\n");
            out.flush();

            InputStream inputstream = p.getInputStream();
            InputStreamReader inputstreamreader = new InputStreamReader(inputstream);
            BufferedReader bufferedreader = new BufferedReader(inputstreamreader);

            String line;
            StringBuilder stringBuilder = new StringBuilder();
            while ((line = bufferedreader.readLine()) != null) {
                stringBuilder.append(line);
                stringBuilder.append("\n");
            }
            out.flush();
            out.close();
            bufferedreader.close();
            inputstream.close();
            inputstreamreader.close();
            if (p.waitFor() != 0) {
                p.destroy();
                return getProp(propName, true);
            } else  {
                p.destroy();
                return stringBuilder.toString().trim();
            }
        } catch (Exception e) {
        }
        return null;
    }

    /*
    fun GetProp(prop: String, grep: String?): String? {
        try {
            val p = Runtime.getRuntime().exec("sh")
            val out = DataOutputStream(p.outputStream)
            out.writeBytes("if [ ! -f \"$prop\" ]; then echo \"\"; exit 1; fi;\n")
            val cmd = "cat " + prop + if (grep != null && grep.length > 0) " | grep " + grep else ""
            out.writeBytes(cmd)
            out.writeBytes("\n")
            out.writeBytes("exit\n")
            out.flush()
            out.close()

            val bufferedreader = p.inputStream.bufferedReader()

            val stringBuffer = StringBuilder()
            bufferedreader.lineSequence().joinTo(stringBuffer,"\n")
            bufferedreader.close()
            p.destroy()
            return stringBuffer.toString().trim { it <= ' ' }
        } catch (e: Exception) {
            e.stackTrace
        }

        return null
    }
    */

    /**
     * 保存属性
     *
     * @param propName 属性名称（要永久保存，请以persist.开头）
     * @param value    属性值,值尽量是简单的数字或字母，避免出现错误
     */
    public static boolean setProp(String propName, String value) {
        try {
            Process p = Runtime.getRuntime().exec("sh");
            DataOutputStream out = new DataOutputStream(p.getOutputStream());
            out.writeBytes("echo " + value + " > " + propName);
            out.writeBytes("\n");
            out.flush();
            return true;
        } catch (Exception ex) {
            return false;
        }
    }
}

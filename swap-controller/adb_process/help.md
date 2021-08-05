Demo:https://github.com/zgj224/Android-Demo/tree/master/app_process_exec_java_demo
栗子:
# am start -n com.android.browser/com.android.browser.BrowserActivity
这里的am命令是一个可执行文件,查看系统am可以发现其实里面调用的是app_process
# adb root
# adb shell cat /system/bin/am
 
#!/system/bin/sh
base=/system
export CLASSPATH=$base/framework/am.jar
exec app_process $base/bin com.android.commands.am.Am "$@"
 
 
1.执行jar包里的java可执行文件
使用app_process启动java文件,其中java里必须有main()方法，这是函数入口。
# adb shell CLASSPATH=/system/framework/Demo.jar exec app_process /system/bin com.example.Demo
 
或在apk里启动一个可执行com.example.helloworld.Console里的main()
# adb shell CLASSPATH=/data/app/com.example.helloworld-1.apk exec app_process /system/bin com.example.helloworld.Console
注意：/system/bin这个目录可以替换为任意目录
 
2.执行java文件
<1>.Hello.java
public static class Hello {
　　public void main(String args[]){
　　　　System.out.println("Hello Android");
　　}
}
 
<2>.编译
# javac Hello.java
编译出Hello.class文件可以在普通的jvm上运行，要放到android下还需要转换成dex，需要用android sdk中的dx工具进行转换
> 如果出现“错误: 编码GBK的不可映射字符”提示，可使用 `javac Hello.java -encoding UTF-8` 命令编译

# cd SDK/build-tools //SDK为自己下载的android sdk（例如C:\Users\用户名\AppData\Local\Android\Sdk\build-tools\29.0.2）
# dx --dex --output=Hello.dex Hello.class
得到Hello.dex
 
<3>.Hello.dex push到/sdcard
# adb push Hello.dex /sdcard
 
<4>.使用app_process 运行hello.dex
# app_process -Djava.class.path=/sdcard/Hello.dex /sdcard Hello
————————————————
版权声明：本文为CSDN博主「慢慢的燃烧」的原创文章，遵循 CC 4.0 BY-SA 版权协议，转载请附上原文出处链接及本声明。
原文链接：https://blog.csdn.net/u010164190/article/details/81335727
# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in C:\Users\helloklf\AppData\Local\Android\Sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

-keep class com.omarea.shared.XposedInterface{*;}
-keep class com.omarea.shared.XposedCheck{*;}
-keep class com.omarea.vboot.ServiceBattery{*;}

-keepclassmembers class com.omarea.shared.XposedInterface{*;}
-keepclassmembers class com.omarea.shared.XposedCheck{*;}
-keepclassmembers class com.omarea.vboot.ServiceBattery{*;}
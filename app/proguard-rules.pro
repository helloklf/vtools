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

-keepclassmembers public class * extends android.app.Activity
-keepclassmembers public class * extends android.app.Application
-keepclassmembers public class * extends android.app.Service
-keepclassmembers public class * extends android.content.BroadcastReceiver
-keepclassmembers class * extends java.io.Serializable{*;}
-keepclassmembers class * implements java.io.Serializable{*;}
#-keepclassmembers public class * extends android.content.ContentProvider
#-keepclassmembers public class * extends android.app.backup.BackupAgentHelper
#-keepclassmembers public class * extends android.preference.Preference
#-keepclassmembers public class * extends android.view.View
#-keepclassmembers public class com.android.vending.licensing.ILicensingService
#-keepclassmembers class android.support.** {*;}

-keep class com.omarea.xposed.XposedInterface{*;}
-keep class com.omarea.xposed.XposedCheck{*;}
-keep class com.omarea.data.customer.ServiceBattery{*;}
-keep class com.omarea.vtools.activities.ActivityFreezeApps{*;}
-keep class com.omarea.model.**{*;}
-keep class com.omarea.krscript.model.**{*;}

-keepclassmembers class com.omarea.xposed.XposedInterface{*;}
-keepclassmembers class com.omarea.xposed.XposedCheck{*;}
-keepclassmembers class com.omarea.data.customer.ServiceBattery{*;}
-keepclassmembers class com.omarea.vtools.activities.ActivityFreezeApps{*;}
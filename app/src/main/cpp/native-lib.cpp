#include <jni.h>
#include <string>

// @ https://blog.csdn.net/xlxxcc/article/details/51106721

/*
jstring charTojstring(JNIEnv* env, const char* pat) {
    //定义java String类 strClass
    jclass strClass = (env)->FindClass("Ljava/lang/String;");
    //获取String(byte[],String)的构造器,用于将本地byte[]数组转换为一个新String
    jmethodID ctorID = (env)->GetMethodID(strClass, "<init>", "([BLjava/lang/String;)V");
    //建立byte数组
    jbyteArray bytes = (env)->NewByteArray(strlen(pat));
    //将char* 转换为byte数组
    (env)->SetByteArrayRegion(bytes, 0, strlen(pat), (jbyte*) pat);
    // 设置String, 保存语言类型,用于byte数组转换至String时的参数
    jstring encoding = (env)->NewStringUTF("GB2312");
    //将byte数组转换为java String,并输出
    return (jstring) (env)->NewObject(strClass, ctorID, bytes, encoding);
}
*/

char* jstringToChar(JNIEnv* env, jstring jstr) {
    char* rtn = NULL;
    jclass clsstring = env->FindClass("java/lang/String");
    jstring strencode = env->NewStringUTF("GB2312");
    jmethodID mid = env->GetMethodID(clsstring, "getBytes", "(Ljava/lang/String;)[B");
    jbyteArray barr = (jbyteArray) env->CallObjectMethod(jstr, mid, strencode);
    jsize alen = env->GetArrayLength(barr);
    jbyte* ba = env->GetByteArrayElements(barr, JNI_FALSE);
    if (alen > 0) {
        rtn = (char*) malloc(alen + 1);
        memcpy(rtn, ba, alen);
        rtn[alen] = 0;
    }
    env->ReleaseByteArrayElements(barr, ba, 0);
    return rtn;
}


extern "C" JNIEXPORT jlong JNICALL
Java_com_omarea_vtools_SceneJNI_getKernelPropLong(
        JNIEnv *env,
        jobject,
        jstring path) {
    // std::string hello = "Hello from C++";
    // return env->NewStringUTF(hello.c_str());

    // 读取路径 /sys/devices/system/cpu/cpu0/cpufreq/scaling_cur_freq
    char* charData = jstringToChar(env, path);

    FILE *kernelProp = fopen(charData, "r");
    if (kernelProp == nullptr)
        return -1;
    long freq;
    fscanf(kernelProp,"%ld",&freq);
    fclose(kernelProp);
    return freq;
}

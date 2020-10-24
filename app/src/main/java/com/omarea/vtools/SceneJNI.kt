package com.omarea.vtools

class SceneJNI {
    external fun getKernelPropLong(path: String): Long

    companion object {
        // Used to load the 'native-lib' library on application startup.
        init {
            System.loadLibrary("native-lib")
        }
    }
}
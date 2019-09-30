package com.omarea.krscript.model

interface AutoRunTask {
    fun onCompleted(result: Boolean?)
    val key: String?
}

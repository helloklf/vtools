package com.omarea.library.shell

import android.content.Context

abstract class TripleCacheValue(private val context: Context, private val storageKey: String) {
    private var cache: String? = null // 内存缓存
    private val value: String?
        get() {
            if (cache == null) {
                // 磁盘缓存
                val storage = context.getSharedPreferences("TripleCacheValues", Context.MODE_PRIVATE)
                if (!storage.contains(storageKey)) {
                    // 重新生成值（通常是耗时操作）
                    cache = initValue()
                    if (cache?.isNotEmpty() == true) {
                        storage.edit().putString(storageKey, cache).apply()
                    }
                } else {
                    cache = storage.getString(storageKey, "")
                }
            }
            return cache
        }

    abstract fun initValue(): String?

    override fun toString(): String {
        val value = this.value
        return value ?: ""
    }

    public fun toInt(): Int {
        val value = this.value
        if (value.isNullOrEmpty()) {
            return 0
        }
        return value.toInt()
    }
}
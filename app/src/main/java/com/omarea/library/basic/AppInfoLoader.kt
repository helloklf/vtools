package com.omarea.library.basic

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.util.LruCache
import com.omarea.model.AppInfo
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import java.io.File

open class AppInfoLoader(private val context: Context, private val cacheSize: Int = 0) {
    private val iconCaches = if (cacheSize < 1) null else LruCache<String, Drawable>(cacheSize)
    private fun saveCache(packageName: String, drawable: Drawable?) {
        if (iconCaches != null && drawable != null) {
            iconCaches.put(packageName, drawable)
        }
    }

    private var pmInstance: PackageManager? = null
    protected val pm: PackageManager
        get() {
            if (pmInstance == null) {
                pmInstance = context.packageManager
            }
            return pmInstance!!
        }

    fun loadIcon(item: AppInfo): Deferred<Drawable?> {
        return GlobalScope.async(Dispatchers.IO) {
            val cache = iconCaches?.get(item.packageName)
            if (cache != null) {
                return@async cache
            }

            var icon: Drawable? = loadIcon(item.packageName).await()
            if (icon == null && item.path.isNotEmpty()) {
                try {
                    val file = File(item.path.toString())
                    if (file.exists() && file.canRead()) {
                        icon = pm.getPackageArchiveInfo(file.absolutePath, PackageManager.GET_ACTIVITIES)?.applicationInfo?.loadIcon(pm)
                        saveCache(item.packageName, icon)
                    }
                } catch (ex: Exception) {
                }
            }
            return@async icon
        }
    }

    fun loadIcon(packageName: String): Deferred<Drawable?> {
        return GlobalScope.async(Dispatchers.IO) {
            val cache = iconCaches?.get(packageName)
            if (cache != null) {
                return@async cache
            }

            var icon: Drawable? = null
            try {
                val installInfo = pm.getPackageInfo(packageName, 0)
                icon = installInfo.applicationInfo.loadIcon(pm)
                saveCache(packageName, icon)
            } catch (ex: Exception) {
            } finally {
            }
            return@async icon
        }
    }

    class AppBasicInfo(var appName: String, var icon: Drawable?) {
    }

    fun loadAppBasicInfo(packageName: String): Deferred<AppBasicInfo> {
        return GlobalScope.async(Dispatchers.IO) {
            var icon: Drawable? = null
            var name = packageName
            try {
                val installInfo = pm.getPackageInfo(packageName, 0)
                name = "" + installInfo.applicationInfo.loadLabel(pm)
                icon = installInfo.applicationInfo.loadIcon(pm)
                // saveCache(packageName, icon)
            } catch (ex: Exception) {
            } finally {
            }

            return@async AppBasicInfo(name, icon)
        }
    }
}
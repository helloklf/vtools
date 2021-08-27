package com.omarea.model

public class ZramWriteBackStat {
    public var backingDev: String? = null
    // 已写入备份设备 KB
    public var backed: Int = 0
    // 历史读取(从备份设备) KB
    public var backReads: Int = 0
    // 历史回写(到备份设备) KB
    public var backWrites: Int = 0
}
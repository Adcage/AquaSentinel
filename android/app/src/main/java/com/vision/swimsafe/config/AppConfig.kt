package com.vision.swimsafe.config

import android.content.Context
import android.content.pm.PackageManager

/**
 * 应用配置提供者
 */
object AppConfig {
    private var amapKey: String? = null

    /**
     * 初始化配置（在 Application 中调用）
     */
    fun init(context: Context) {
        try {
            val appInfo = context.packageManager.getApplicationInfo(
                context.packageName,
                PackageManager.GET_META_DATA
            )
            amapKey = appInfo.metaData?.getString("com.amap.api.v2.apikey")
        } catch (e: Exception) {
            // 忽略
        }
    }

    /**
     * 获取高德地图 API Key
     */
    fun getAMapKey(): String = amapKey ?: ""
}

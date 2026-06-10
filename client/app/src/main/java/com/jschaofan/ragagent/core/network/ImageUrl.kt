package com.jschaofan.ragagent.core.network

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

/**
 * 将包含中文路径的 OSS 地址转换为标准百分号编码 URL，避免图片加载器解析失败。
 */
fun String?.toEncodedImageUrl(): String? {
    if (isNullOrBlank()) return null
    return toHttpUrlOrNull()?.toString() ?: this
}

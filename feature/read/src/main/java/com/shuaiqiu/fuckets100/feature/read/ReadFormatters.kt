package com.shuaiqiu.fuckets100.feature.read

/**
 * 将字节大小格式化为可读文本。
 */
internal fun formatFileSize(size: Long): String {
    return when {
        size < 1024 -> "$size B"
        size < 1024 * 1024 -> "${size / 1024} KB"
        else -> "${size / (1024 * 1024)} MB"
    }
}

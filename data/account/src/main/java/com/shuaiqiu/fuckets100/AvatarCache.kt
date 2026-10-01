package com.shuaiqiu.fuckets100

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import java.io.File

/**
 * 用户头像磁盘缓存。
 *
 * 后端 avatar_url 形如 `/user/{id}/avatar?v={content_hash[:16]}`, `?v=` 是
 * 头像内容哈希前缀。缓存文件名 = `${userId}_${version}.png`, 版本不变则直接
 * 读缓存 (重启后不再实时下载), 版本变化 (头像被更换) 时才重新下载并替换。
 */
object AvatarCache {

    private const val CACHE_DIR = "avatar_cache"

    private fun dir(context: Context): File =
        File(context.filesDir, CACHE_DIR).apply { if (!exists()) mkdirs() }

    /** 从 avatar_url 中提取 `?v=` 版本号, 无则返回 null。 */
    fun versionFrom(url: String?): String? {
        if (url == null) return null
        val index = url.indexOf("?v=")
        return if (index >= 0) url.substring(index + 3).takeIf { it.isNotBlank() } else null
    }

    /** 读取指定版本的缓存头像。 */
    fun load(context: Context, userId: Long, version: String?): ImageBitmap? {
        if (version == null) return null
        val file = cacheFile(context, userId, version)
        if (!file.isFile) return null
        return runCatching {
            android.graphics.BitmapFactory.decodeFile(file.absolutePath)?.asImageBitmap()
        }.getOrNull()
    }

    /** 保存新版本头像并清理该用户的旧版本缓存。 */
    fun save(context: Context, userId: Long, version: String, bitmap: Bitmap) {
        if (version.isBlank()) return
        runCatching {
            val file = cacheFile(context, userId, version)
            file.parentFile?.let { if (!it.exists()) it.mkdirs() }
            file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 90, it) }
            dir(context).listFiles()
                ?.filter { it.isFile && it.name != file.name && it.name.startsWith("${userId}_") }
                ?.forEach { it.delete() }
        }
    }

    private fun cacheFile(context: Context, userId: Long, version: String): File =
        File(dir(context), "${userId}_${version}.png")
}

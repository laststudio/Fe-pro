package com.shuaiqiu.fuckets100

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URL
import java.net.URLConnection
import java.security.MessageDigest

/**
 * 公告/更新日志等远程文本内容的读取与本地缓存。
 *
 * 网络访问集中在这里，界面层只消费结果，不再直接持有 URLConnection。
 */
class RemoteContentRepository(
    private val context: Context,
) {
    suspend fun fetchTextOrNull(url: String): String? {
        if (url.isBlank()) return null
        return withContext(Dispatchers.IO) {
            val cacheFile = runCatching {
                File(context.cacheDir, "remote_content/${sha256(url)}.txt")
            }.getOrNull()
            val cached = cacheFile?.takeIf { it.isFile }?.let { file ->
                val age = System.currentTimeMillis() - file.lastModified()
                if (age in 0..CACHE_TTL_MS) file.readText() else null
            }
            if (!cached.isNullOrBlank()) return@withContext cached

            val fetched = runCatching {
                val connection = URL(url).openConnection() as URLConnection
                connection.connectTimeout = TIMEOUT_MS
                connection.readTimeout = TIMEOUT_MS
                connection.connect()
                connection.inputStream.bufferedReader().use { it.readText() }
            }.getOrNull()
            if (!fetched.isNullOrBlank()) {
                runCatching {
                    cacheFile?.parentFile?.mkdirs()
                    cacheFile?.writeText(fetched)
                }
            }
            fetched ?: cacheFile?.takeIf { it.isFile }?.let { it.readText() }
        }
    }

    private fun sha256(value: String): String = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it) }

    private companion object {
        const val TIMEOUT_MS = 5_000
        const val CACHE_TTL_MS = 5 * 60 * 1000L
    }
}

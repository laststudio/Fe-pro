package com.shuaiqiu.fuckets100

import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import android.net.Uri
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

/**
 * 账号头像的加载与本地缓存。
 *
 * 网络与磁盘访问集中在此，界面层只接收 [Bitmap]。
 */
class AvatarRepository(
    private val context: Context,
) {
    suspend fun load(avatarUrl: String?, userId: Long?): ImageBitmap? {
        val version = AvatarCache.versionFrom(avatarUrl)
        val cached = AvatarCache.load(context, userId ?: -1L, version)
        if (cached != null) return cached
        if (avatarUrl.isNullOrBlank()) return null

        val resolved = if (avatarUrl.startsWith("/")) FeAccountApi.BASE_URL + avatarUrl else avatarUrl
        return try {
            val uri = Uri.parse(resolved)
            require(uri.scheme == "https" && uri.host == "api.lastudio.cc")
            withContext(Dispatchers.IO) {
                val connection = (URL(resolved).openConnection() as HttpURLConnection).apply {
                    connectTimeout = TIMEOUT_MS
                    readTimeout = TIMEOUT_MS
                    instanceFollowRedirects = false
                }
                try {
                    require(connection.responseCode == HttpURLConnection.HTTP_OK)
                    val bitmap = connection.inputStream.use { BitmapFactory.decodeStream(it) }
                        ?: return@withContext null
                    if (userId != null && version != null) {
                        AvatarCache.save(context, userId, version, bitmap)
                    }
                    bitmap.asImageBitmap()
                } finally {
                    connection.disconnect()
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        }
    }

    private companion object {
        const val TIMEOUT_MS = 5_000
    }
}

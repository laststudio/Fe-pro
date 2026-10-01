package com.shuaiqiu.fuckets100.feature.account

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

import com.shuaiqiu.fuckets100.AvatarCache
import com.shuaiqiu.fuckets100.FeAccountApi
@Composable
fun AccountInitialAvatar(
    username: String,
    size: Int = 56,
    avatarUrl: String? = null,
    userId: Long? = null,
) {
    val context = LocalContext.current
    var avatar by remember(avatarUrl, userId) { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }
    LaunchedEffect(avatarUrl, userId) {
        val version = AvatarCache.versionFrom(avatarUrl)
        avatar = AvatarCache.load(context, userId ?: -1L, version)
            ?: avatarUrl?.let { path ->
                val resolved = if (path.startsWith("/")) FeAccountApi.BASE_URL + path else path
                try {
                    val uri = Uri.parse(resolved)
                    require(uri.scheme == "https" && uri.host == "api.lastudio.cc")
                    withContext(Dispatchers.IO) {
                        val connection = (URL(resolved).openConnection() as HttpURLConnection).apply {
                            connectTimeout = AVATAR_TIMEOUT_MS
                            readTimeout = AVATAR_TIMEOUT_MS
                            instanceFollowRedirects = false
                        }
                        try {
                            require(connection.responseCode == HttpURLConnection.HTTP_OK)
                            val bitmap = connection.inputStream.use {
                                android.graphics.BitmapFactory.decodeStream(it)
                            } ?: return@withContext null
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
    }
    Box(
        Modifier.size(size.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        if (avatar != null) {
            Image(avatar!!, contentDescription = "用户头像", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        } else {
            Text(username.trim().firstOrNull()?.uppercase() ?: "F", style = MaterialTheme.typography.headlineMedium)
        }
    }
}

internal const val AVATAR_TIMEOUT_MS = 5_000

package com.shuaiqiu.fuckets100.feature.shared

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel

@Composable
fun AccountInitialAvatar(
    username: String,
    size: Int = 56,
    avatarUrl: String? = null,
    userId: Long? = null,
    viewModel: AvatarViewModel = hiltViewModel(),
) {
    var avatar by remember(avatarUrl, userId) {
        mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null)
    }
    LaunchedEffect(avatarUrl, userId, viewModel) {
        avatar = viewModel.load(avatarUrl, userId)
    }
    Box(
        Modifier.size(size.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        val current = avatar
        if (current != null) {
            Image(current, contentDescription = "用户头像", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        } else {
            Text(username.trim().firstOrNull()?.uppercase() ?: "F", style = MaterialTheme.typography.headlineMedium)
        }
    }
}

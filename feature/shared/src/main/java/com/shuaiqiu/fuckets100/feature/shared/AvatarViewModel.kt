package com.shuaiqiu.fuckets100.feature.shared

import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.ViewModel
import com.shuaiqiu.fuckets100.AvatarRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class AvatarViewModel @Inject constructor(
    private val avatarRepository: AvatarRepository,
) : ViewModel() {
    suspend fun load(avatarUrl: String?, userId: Long?): ImageBitmap? =
        avatarRepository.load(avatarUrl, userId)
}

package com.shuaiqiu.fuckets100.feature.settings

import androidx.lifecycle.ViewModel
import com.shuaiqiu.fuckets100.core.common.AppBuildInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class AboutViewModel @Inject constructor(
    val buildInfo: AppBuildInfo,
) : ViewModel()

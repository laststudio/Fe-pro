package com.shuaiqiu.fuckets100.feature.home

data class HomeRuntimeStatus(
    val hasFilesPerm: Boolean,
    val hasRootAvailable: Boolean,
    val hasDirectReadAvailable: Boolean,
    val cloudLoggedIn: Boolean,
    val etsAppInfo: Pair<Boolean, String>? = null,
)

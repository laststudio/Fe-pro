package com.shuaiqiu.fuckets100.navigation

import android.os.Parcelable
import androidx.navigation3.runtime.NavKey
import kotlinx.parcelize.Parcelize

sealed interface FeRoute : NavKey, Parcelable {
    @Parcelize
    data object Main : FeRoute

    @Parcelize
    data object Activation : FeRoute

    @Parcelize
    data object CloudActivation : FeRoute

    @Parcelize
    data object GeneralSettings : FeRoute

    @Parcelize
    data object ThemeSettings : FeRoute

    @Parcelize
    data object Legal : FeRoute

    @Parcelize
    data object Donate : FeRoute

    @Parcelize
    data class RemoteContent(
        val announcementTitle: String,
        val announcementMessage: String,
        val announcementUpdatedAt: String,
        val announcementUrl: String,
        val changelogTitle: String,
        val changelogSummary: String,
        val changelogUrl: String,
    ) : FeRoute
}

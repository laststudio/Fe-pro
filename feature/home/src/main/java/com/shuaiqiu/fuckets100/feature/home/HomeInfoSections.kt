package com.shuaiqiu.fuckets100.feature.home

import android.content.ClipData
import android.content.ClipboardManager
import android.os.Build
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.shuaiqiu.fuckets100.ActivationMode
import com.shuaiqiu.fuckets100.FeAccountGateState
import com.shuaiqiu.fuckets100.core.designsystem.RighteousFont
import com.shuaiqiu.fuckets100.ShizukuState
import com.shuaiqiu.fuckets100.UpdateStatus
import com.shuaiqiu.fuckets100.core.designsystem.ExpressiveScaffold
import com.shuaiqiu.fuckets100.core.designsystem.ExpressiveSegmentedListItem
import com.shuaiqiu.fuckets100.core.designsystem.SegmentedColumn
import com.shuaiqiu.fuckets100.core.designsystem.TonalCard
import com.shuaiqiu.fuckets100.core.designsystem.expressiveTopAppBarColors
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver

@Composable
internal fun HomeVersionInfoSection(
    context: android.content.Context,
    appVersion: String,
    etsAppInfo: Pair<Boolean, String>?,
) {
    val deviceModel = remember { DeviceInfoHelper.getDeviceModel() }
    val androidVersion = remember { DeviceInfoHelper.getAndroidVersion() }
    val etsStatus = when (etsAppInfo?.first) {
        true -> "已安装 · ${etsAppInfo.second.ifBlank { "未知版本" }}"
        false -> "未安装"
        null -> "未知"
    }

    SegmentedColumn(
        title = "版本信息",
        items = listOf(
            {
                ExpressiveSegmentedListItem(
                    index = 0,
                    count = 4,
                    onClick = { copyToClipboard(context, "设备型号", deviceModel) },
                    headlineContent = { Text("设备") },
                    supportingContent = {
                        Text(
                            deviceModel,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                )
            },
            {
                ExpressiveSegmentedListItem(
                    index = 1,
                    count = 4,
                    onClick = { copyToClipboard(context, "Android 版本", androidVersion) },
                    headlineContent = { Text("Android 版本") },
                    supportingContent = {
                        Text(
                            androidVersion,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                )
            },
            {
                ExpressiveSegmentedListItem(
                    index = 2,
                    count = 4,
                    onClick = { copyToClipboard(context, "管理器版本", appVersion) },
                    headlineContent = { Text("管理器版本") },
                    supportingContent = {
                        Text(
                            appVersion,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                )
            },
            {
                ExpressiveSegmentedListItem(
                    index = 3,
                    count = 4,
                    onClick = { copyToClipboard(context, "e听说状态", etsStatus) },
                    headlineContent = { Text("e听说") },
                    supportingContent = {
                        Text(
                            etsStatus,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                )
            }
        )
    )
}

@Composable
internal fun HomeAnnouncementSection(
    status: UpdateStatus?,
    onNavigateToRemoteContent: (UpdateStatus?) -> Unit,
) {
    val announcementMessage = status?.announcementMessage.orEmpty()
    val announcementTitle = status?.announcementTitle?.takeIf { it.isNotBlank() } ?: "公告与动态"

    SegmentedColumn(
        title = "公告",
        items = listOf(
            {
                ExpressiveSegmentedListItem(
                    index = 0,
                    count = 1,
                    onClick = { onNavigateToRemoteContent(status) },
                    headlineContent = { Text(announcementTitle) },
                    supportingContent = {
                        Text(
                            announcementMessage.ifBlank { if (status == null) "正在同步远程内容..." else "暂无最新公告" },
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    trailingContent = {
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                )
            }
        )
    )
}

@Composable
internal fun StatusTag(
    label: String,
    containerColor: androidx.compose.ui.graphics.Color,
    contentColor: androidx.compose.ui.graphics.Color,
) {
    Surface(
        color = containerColor,
        contentColor = contentColor,
        shape = MaterialTheme.shapes.extraSmall,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmallEmphasized,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
        )
    }
}

internal val ActivationMode.shortLabel: String
    get() = when (this) {
        ActivationMode.DEFAULT -> "未激活"
        ActivationMode.SHIZUKU -> "Shizuku"
        ActivationMode.ROOT -> "Root"
        ActivationMode.DIRECT_READ -> "Direct Read"
        ActivationMode.CLOUD -> "云端"
    }

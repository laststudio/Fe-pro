package com.shuaiqiu.fuckets100.feature.settings

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Update
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.Alignment
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.shuaiqiu.fuckets100.core.designsystem.ExpressiveScaffold
import com.shuaiqiu.fuckets100.core.designsystem.TonalCard
import com.shuaiqiu.fuckets100.core.designsystem.TopBarBackButton
import com.shuaiqiu.fuckets100.core.designsystem.expressiveTopAppBarColors
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

import com.shuaiqiu.fuckets100.UpdateStatus
data class RemoteContent(
    val announcementTitle: String,
    val announcementMessage: String,
    val announcementUpdatedAt: String,
    val announcementUrl: String,
    val changelogTitle: String,
    val changelogSummary: String,
    val changelogContent: String = "",
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun RemoteContentScreen(
    content: RemoteContent,
    onBack: () -> Unit,
    viewModel: RemoteContentViewModel = hiltViewModel(),
) {
    val appVersionName = viewModel.appVersionName
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var announcementBody by remember(content) {
        mutableStateOf(content.announcementMessage.ifBlank { "暂无公告" })
    }
    var changelogBody by remember(content) {
        mutableStateOf(content.changelogSummary.ifBlank { "暂无更新内容" })
    }
    var fetchStatus by remember(content) {
        mutableStateOf(
            if (content.announcementUrl.isNotBlank() || content.changelogContent.isNotBlank()) {
                "正在同步远程内容..."
            } else {
                "显示本地缓存公告"
            }
        )
    }
    var updateDialogStatus by remember {
        mutableStateOf<UpdateStatus?>(null)
    }
    var displayedContent by remember(content) { mutableStateOf(content) }
    var isRefreshing by remember(content) { mutableStateOf(false) }
    val pullRefreshState = rememberPullToRefreshState()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    suspend fun syncContent(nextContent: RemoteContent, forceRefresh: Boolean = false) {
        val announcementResult = viewModel.fetchTextOrNull(nextContent.announcementUrl, forceRefresh)
        var successCount = 0
        announcementBody = if (!announcementResult.isNullOrBlank()) {
            successCount++
            parseRemoteContent(announcementResult)
        } else {
            nextContent.announcementMessage.ifBlank { "暂无公告" }
        }
        changelogBody = if (nextContent.changelogContent.isNotBlank()) {
            successCount++
            parseCurrentVersionChangelog(nextContent.changelogContent, appVersionName)
                .ifBlank { parseRemoteContent(nextContent.changelogContent) }
        } else {
            nextContent.changelogSummary.ifBlank { "暂无更新内容" }
        }
        displayedContent = nextContent
        fetchStatus = when {
            nextContent.announcementUrl.isBlank() && nextContent.changelogContent.isBlank() ->
                "显示本地缓存内容"
            successCount > 0 -> "远程内容同步完成"
            else -> "获取失败，已显示缓存内容"
        }
    }

    LaunchedEffect(content) {
        isRefreshing = true
        try {
            syncContent(content)
        } finally {
            isRefreshing = false
        }
    }

    val onRefresh: () -> Unit = {
        if (!isRefreshing) {
            scope.launch {
                isRefreshing = true
                fetchStatus = "正在同步远程内容..."
                try {
                    val status = viewModel.checkStatus()
                    syncContent(
                        nextContent = RemoteContent(
                            announcementTitle = status.announcementTitle,
                            announcementMessage = status.announcementMessage,
                            announcementUpdatedAt = status.announcementUpdatedAt,
                            announcementUrl = status.announcementUrl,
                            changelogTitle = status.changelogTitle,
                            changelogSummary = status.changelogSummary.ifBlank { status.message },
                            changelogContent = status.changelogContent,
                        ),
                        forceRefresh = true,
                    )
                    if (status.showDialog) {
                        updateDialogStatus = status
                    } else {
                        Toast.makeText(context, "当前已是最新版本", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    fetchStatus = "刷新失败，已保留当前内容"
                    Toast.makeText(context, "刷新失败", Toast.LENGTH_SHORT).show()
                } finally {
                    isRefreshing = false
                }
            }
        }
    }

    ExpressiveScaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text("公告与更新") },
                navigationIcon = { TopBarBackButton(onClick = onBack) },
                windowInsets = WindowInsets.safeDrawing.only(
                    WindowInsetsSides.Top + WindowInsetsSides.Horizontal,
                ),
                colors = expressiveTopAppBarColors(),
                scrollBehavior = scrollBehavior,
            )
        },
        contentWindowInsets = WindowInsets.safeDrawing.only(
            WindowInsetsSides.Top + WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom,
        ),
    ) { padding ->
        PullToRefreshBox(
            state = pullRefreshState,
            isRefreshing = isRefreshing,
            onRefresh = onRefresh,
            modifier = Modifier.fillMaxSize().padding(padding),
            indicator = {
                PullToRefreshDefaults.LoadingIndicator(
                    modifier = Modifier.align(Alignment.TopCenter),
                    state = pullRefreshState,
                    isRefreshing = isRefreshing,
                )
            },
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    fetchStatus,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                RemoteContentSection(
                    icon = Icons.Default.Campaign,
                    title = displayedContent.announcementTitle.ifBlank { "公告" },
                    body = announcementBody,
                    meta = displayedContent.announcementUpdatedAt
                )

                RemoteContentSection(
                    icon = Icons.Default.Update,
                    title = displayedContent.changelogTitle.ifBlank { "更新日志" },
                    body = changelogBody,
                    meta = "",
                )

            }
        }
    }

    updateDialogStatus?.let { status ->
        UpdateDialog(
            status = status,
            onDismiss = { updateDialogStatus = null }
        )
    }
}

@Composable
private fun RemoteContentSection(
    icon: ImageVector,
    title: String,
    body: String,
    meta: String
) {
    TonalCard(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(12.dp))
                Text(
                    title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            if (meta.isNotBlank()) {
                Text(
                    meta,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun parseRemoteContent(raw: String): String {
    return raw
        .replace("\r\n", "\n")
        .replace("\r", "\n")
        .lines()
        .map { line ->
            line.trim()
                .replace(Regex("^#{1,6}\\s*"), "")
                .replace(Regex("^>\\s?"), "")
                .replace(Regex("^[-*+]\\s+"), "• ")
                .replace(Regex("\\[(.+?)]\\((.+?)\\)"), "$1")
                .replace(Regex("[*_`]+"), "")
        }
        .joinToString("\n")
        .replace(Regex("\\n{3,}"), "\n\n")
        .trim()
}

private fun parseCurrentVersionChangelog(raw: String, versionName: String): String {
    val normalized = raw.replace("\r\n", "\n").replace("\r", "\n")
    val versionHeader = Regex("^##\\s+\\[?${Regex.escape(versionName)}]?\\b.*$", RegexOption.MULTILINE)
    val match = versionHeader.find(normalized) ?: return ""
    val nextHeader = Regex("^##\\s+", RegexOption.MULTILINE).find(normalized, match.range.last + 1)
    val sectionEnd = nextHeader?.range?.first ?: normalized.length
    return parseRemoteContent(normalized.substring(match.range.first, sectionEnd))
}

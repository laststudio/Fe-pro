package com.shuaiqiu.fuckets100

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.Alignment
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.shuaiqiu.fuckets100.component.material.ExpressiveScaffold
import com.shuaiqiu.fuckets100.component.material.TonalCard
import com.shuaiqiu.fuckets100.component.material.TopBarBackButton
import com.shuaiqiu.fuckets100.component.material.expressiveTopAppBarColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL
import java.net.URLConnection

open class RemoteContentActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        applyPredictiveBackWindowTheme()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        ThemeManager.init(this)

        val content = RemoteContent(
            announcementTitle = intent.getStringExtra(EXTRA_ANNOUNCEMENT_TITLE).orEmpty(),
            announcementMessage = intent.getStringExtra(EXTRA_ANNOUNCEMENT_MESSAGE).orEmpty(),
            announcementUpdatedAt = intent.getStringExtra(EXTRA_ANNOUNCEMENT_UPDATED_AT).orEmpty(),
            announcementUrl = intent.getStringExtra(EXTRA_ANNOUNCEMENT_URL).orEmpty(),
            changelogTitle = intent.getStringExtra(EXTRA_CHANGELOG_TITLE).orEmpty(),
            changelogSummary = intent.getStringExtra(EXTRA_CHANGELOG_SUMMARY).orEmpty(),
            changelogUrl = intent.getStringExtra(EXTRA_CHANGELOG_URL).orEmpty()
        )

        setContent {
            val effectiveDarkMode = if (ThemeManager.getSavedAutoDarkMode()) {
                isSystemInDarkTheme()
            } else {
                ThemeManager.getSavedDarkMode()
            }

            FeThemeWrapper(
                theme = ThemeManager.getSavedTheme(),
                isDarkMode = effectiveDarkMode
            ) {
                PredictiveBackContent(onBack = { finish() }) {
                    RemoteContentScreen(
                        content = content,
                        onBack = { finish() }
                    )
                }
            }
        }
    }

    companion object {
        private const val EXTRA_ANNOUNCEMENT_TITLE = "announcement_title"
        private const val EXTRA_ANNOUNCEMENT_MESSAGE = "announcement_message"
        private const val EXTRA_ANNOUNCEMENT_UPDATED_AT = "announcement_updated_at"
        private const val EXTRA_ANNOUNCEMENT_URL = "announcement_url"
        private const val EXTRA_CHANGELOG_TITLE = "changelog_title"
        private const val EXTRA_CHANGELOG_SUMMARY = "changelog_summary"
        private const val EXTRA_CHANGELOG_URL = "changelog_url"

        fun createIntent(
            context: Context,
            announcementTitle: String,
            announcementMessage: String,
            announcementUpdatedAt: String,
            announcementUrl: String,
            changelogTitle: String,
            changelogSummary: String,
            changelogUrl: String
        ): Intent {
            return Intent(
                context,
                predictiveBackActivityClass(
                    RemoteContentActivity::class.java,
                    RemoteContentOpaqueActivity::class.java,
                    RemoteContentKernelSuClassicActivity::class.java
                )
            ).apply {
                putExtra(EXTRA_ANNOUNCEMENT_TITLE, announcementTitle)
                putExtra(EXTRA_ANNOUNCEMENT_MESSAGE, announcementMessage)
                putExtra(EXTRA_ANNOUNCEMENT_UPDATED_AT, announcementUpdatedAt)
                putExtra(EXTRA_ANNOUNCEMENT_URL, announcementUrl)
                putExtra(EXTRA_CHANGELOG_TITLE, changelogTitle)
                putExtra(EXTRA_CHANGELOG_SUMMARY, changelogSummary)
                putExtra(EXTRA_CHANGELOG_URL, changelogUrl)
            }
        }
    }
}

private data class RemoteContent(
    val announcementTitle: String,
    val announcementMessage: String,
    val announcementUpdatedAt: String,
    val announcementUrl: String,
    val changelogTitle: String,
    val changelogSummary: String,
    val changelogUrl: String
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun RemoteContentScreen(
    content: RemoteContent,
    onBack: () -> Unit
) {
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
            if (content.announcementUrl.isNotBlank() || content.changelogUrl.isNotBlank()) {
                "正在同步远程内容..."
            } else {
                "显示本地缓存公告"
            }
        )
    }
    var updateDialogStatus by remember {
        mutableStateOf<UpdateStatus?>(null)
    }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    LaunchedEffect(content) {
        val announcementResult = fetchRemoteTextOrNull(content.announcementUrl)
        val changelogResult = fetchRemoteTextOrNull(content.changelogUrl)
        var successCount = 0
        if (!announcementResult.isNullOrBlank()) {
            announcementBody = parseRemoteContent(announcementResult)
            successCount++
        }
        if (!changelogResult.isNullOrBlank()) {
            changelogBody = parseCurrentVersionChangelog(changelogResult, BuildConfig.VERSION_NAME)
                .ifBlank { parseRemoteContent(changelogResult) }
            successCount++
        }
        fetchStatus = when {
            content.announcementUrl.isBlank() && content.changelogUrl.isBlank() -> "显示本地缓存内容"
            successCount > 0 -> "远程内容同步完成"
            else -> "获取失败，已显示缓存内容"
        }
    }

    ExpressiveScaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text("公告与更新") },
                navigationIcon = { TopBarBackButton(onClick = onBack) },
                actions = {
                    IconButton(
                        onClick = {
                            scope.launch {
                                runCatching { RemoteConfigManager.checkStatus() }
                                    .onSuccess { status ->
                                        if (status.showDialog) {
                                            updateDialogStatus = status
                                        } else {
                                            Toast.makeText(context, "当前已是最新版本", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                    .onFailure {
                                        Toast.makeText(context, "检测更新失败", Toast.LENGTH_SHORT).show()
                                    }
                            }
                        }
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "检测更新")
                    }
                },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
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
                title = content.announcementTitle.ifBlank { "公告" },
                body = announcementBody,
                meta = content.announcementUpdatedAt
            )

            RemoteContentSection(
                icon = Icons.Default.Update,
                title = content.changelogTitle.ifBlank { "更新日志" },
                body = changelogBody,
                meta = "",
            )

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

private suspend fun fetchRemoteTextOrNull(url: String): String? {
    if (url.isBlank()) return null
    return withContext(Dispatchers.IO) {
        runCatching {
            val connection = URL(url).openConnection() as URLConnection
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            connection.connect()
            connection.inputStream.bufferedReader().use { it.readText() }
        }.getOrNull()
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

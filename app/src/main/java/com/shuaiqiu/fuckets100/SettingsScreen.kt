package com.shuaiqiu.fuckets100

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import android.content.Intent
import android.net.Uri
import com.shuaiqiu.fuckets100.component.material.ExpressiveScaffold
import com.shuaiqiu.fuckets100.component.material.ExpressiveSegmentedListItem
import com.shuaiqiu.fuckets100.component.material.SegmentedColumn
import com.shuaiqiu.fuckets100.component.material.expressiveTopAppBarColors

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SettingsScreen(
    onNavigateActivation: () -> Unit,
    onNavigateGeneralSettings: () -> Unit,
    onNavigateThemeSettings: () -> Unit,
    onNavigateLegal: () -> Unit,
    onNavigateAccount: () -> Unit,
    accountSession: FeAccountSession?,
) {
    val context = LocalContext.current
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    ExpressiveScaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text("设置") },
                subtitle = { Text("配置应用行为与个性化选项") },
                windowInsets = WindowInsets.safeDrawing.only(
                    WindowInsetsSides.Top + WindowInsetsSides.Horizontal,
                ),
                colors = expressiveTopAppBarColors(),
                scrollBehavior = scrollBehavior,
            )
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        contentWindowInsets = WindowInsets.safeDrawing.only(
            WindowInsetsSides.Top + WindowInsetsSides.Horizontal,
        ),
    ) { p ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(p)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(Modifier.height(16.dp))

            Card(
                onClick = onNavigateAccount,
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.extraLarge,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                ),
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AccountInitialAvatar(
                        username = accountSession?.user?.username.orEmpty(),
                        avatarUrl = accountSession?.user?.avatarUrl,
                        userId = accountSession?.user?.id,
                    )
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            accountSession?.user?.username ?: "Fe 账号",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            accountSession?.user?.let { "LA Studio · ${it.tierDisplayName}" }
                                ?: "查看账号信息",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Icon(Icons.Default.ChevronRight, contentDescription = "进入账号")
                }
            }

            Spacer(Modifier.height(24.dp))

            SegmentedColumn(
                title = "应用设置",
                items = listOf(
                    {
                        SettingsSegmentedItem(
                            index = 0,
                            count = 3,
                            icon = Icons.Default.Build,
                            title = "运行授权",
                            supportingText = "配置 Shizuku、Root 或其他模式",
                            onClick = {
                                onNavigateActivation()
                            },
                        )
                    },
                    {
                        SettingsSegmentedItem(
                            index = 1,
                            count = 3,
                            icon = Icons.Default.Tune,
                            title = "通用设置",
                            supportingText = "配置答案显示方式",
                            onClick = {
                                onNavigateGeneralSettings()
                            },
                        )
                    },
                    {
                        SettingsSegmentedItem(
                            index = 2,
                            count = 3,
                            icon = Icons.Default.Palette,
                            title = "主题",
                            supportingText = "彩色主题与明暗模式",
                            onClick = {
                                onNavigateThemeSettings()
                            },
                        )
                    },
                ),
            )

            Spacer(Modifier.height(24.dp))

            SegmentedColumn(
                title = "其他",
                items = listOf(
                    {
                        SettingsSegmentedItem(
                            index = 0,
                            count = 3,
                            icon = Icons.Default.Info,
                            title = "关于 Fe",
                            supportingText = "应用信息与致谢",
                            onClick = { context.startActivity(Intent(context, AboutActivity::class.java)) },
                        )
                    },
                    {
                        SettingsSegmentedItem(
                            index = 1,
                            count = 3,
                            icon = Icons.Default.Gavel,
                            title = "法律信息与使用守则",
                            supportingText = "使用前请阅读并遵守",
                            onClick = {
                                onNavigateLegal()
                            },
                        )
                    },
                    {
                        SettingsSegmentedItem(
                            index = 2,
                            count = 3,
                            icon = Icons.AutoMirrored.Filled.Help,
                            title = "访问官网",
                            supportingText = "lastudio.cc",
                            trailingIcon = Icons.AutoMirrored.Filled.OpenInNew,
                            trailingContentDescription = "访问官网",
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://lastudio.cc"))
                                context.startActivity(intent)
                            },
                        )
                    },
                ),
            )

            Spacer(Modifier.height(88.dp))
        }
    }

}

@Composable
private fun SettingsSegmentedItem(
    index: Int,
    count: Int,
    icon: ImageVector,
    title: String,
    supportingText: String,
    onClick: () -> Unit,
    trailingIcon: ImageVector = Icons.Default.ChevronRight,
    trailingContentDescription: String? = null,
) {
    ExpressiveSegmentedListItem(
        index = index,
        count = count,
        onClick = onClick,
        headlineContent = { Text(title) },
        supportingContent = { Text(supportingText) },
        leadingContent = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
        },
        trailingContent = {
            Icon(
                imageVector = trailingIcon,
                contentDescription = trailingContentDescription,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
    )
}

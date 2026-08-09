package com.shuaiqiu.fuckets100

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShortText
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import com.shuaiqiu.fuckets100.component.material.ExpressiveScaffold
import com.shuaiqiu.fuckets100.component.material.ExpressiveSegmentedListItem
import com.shuaiqiu.fuckets100.component.material.ExpressiveSegmentedSwitchItem
import com.shuaiqiu.fuckets100.component.material.SegmentedColumn
import com.shuaiqiu.fuckets100.component.material.TopBarBackButton
import com.shuaiqiu.fuckets100.component.material.expressiveTopAppBarColors

/**
 * 通用设置页面
 * 包含语言设置、时区设置等通用选项
 * 喵~ 现在还包含调试模式和强执读取模式的开关喵！
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun GeneralSettingsScreen(onBack: () -> Unit) {
    var debugModeEnabled by remember { mutableStateOf(SettingsManager.getDebugMode()) }
    var forceReadModeEnabled by remember { mutableStateOf(SettingsManager.getForceReadMode()) }
    var hideDebugButton by remember { mutableStateOf(SettingsManager.getHideDebugButton()) }
    var compactAnswerDisplay by remember { mutableStateOf(SettingsManager.getCompactAnswerDisplay()) }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    ExpressiveScaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text("通用设置") },
                subtitle = { Text("语言、时区与阅读行为") },
                navigationIcon = { TopBarBackButton(onClick = onBack) },
                windowInsets = WindowInsets.safeDrawing.only(
                    WindowInsetsSides.Top + WindowInsetsSides.Horizontal,
                ),
                colors = expressiveTopAppBarColors(),
                scrollBehavior = scrollBehavior,
            )
        },
    ) { paddingValues ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            Spacer(Modifier.height(16.dp))
            // 喵~ 调试设置分组（默认开启的隐藏功能）
            if (debugModeEnabled) {
                SegmentedColumn(
                    title = "调试设置",
                    items = listOf(
                        {
                            ExpressiveSegmentedSwitchItem(
                                index = 0,
                                count = 2,
                                checked = hideDebugButton,
                                onCheckedChange = { enabled ->
                                    hideDebugButton = enabled
                                    SettingsManager.saveHideDebugButton(enabled)
                                },
                                headlineContent = { Text("隐藏调试按钮") },
                                supportingContent = { Text("隐藏 ReadScreen 右下角的调试按钮") },
                                leadingContent = { Icon(Icons.Default.VisibilityOff, null, tint = MaterialTheme.colorScheme.primary) },
                            )
                        },
                        {
                            ExpressiveSegmentedSwitchItem(
                                index = 1,
                                count = 2,
                                checked = forceReadModeEnabled,
                                onCheckedChange = { enabled ->
                                    forceReadModeEnabled = enabled
                                    SettingsManager.saveForceReadMode(enabled)
                                },
                                headlineContent = { Text("强执读取模式") },
                                supportingContent = { Text("跳过权限检查，强制读取数据（仅特殊情况下使用）") },
                                leadingContent = { Icon(Icons.Default.Bolt, null, tint = MaterialTheme.colorScheme.primary) },
                            )
                        },
                    ),
                )
                Spacer(Modifier.height(24.dp))
            }

            SegmentedColumn(
                title = "通用设置",
                items = listOf(
                    {
                        ExpressiveSegmentedSwitchItem(
                            index = 0,
                            count = 3,
                            checked = compactAnswerDisplay,
                            onCheckedChange = { enabled ->
                                compactAnswerDisplay = enabled
                                SettingsManager.saveCompactAnswerDisplay(enabled)
                            },
                            headlineContent = { Text("精简答案") },
                            supportingContent = { Text("开启后按题型只显示最常用的 1-2 个参考答案") },
                            leadingContent = { Icon(Icons.AutoMirrored.Filled.ShortText, null, tint = MaterialTheme.colorScheme.primary) },
                        )
                    },
                    {
                        ExpressiveSegmentedListItem(
                            index = 1,
                            count = 3,
                            onClick = { /* TODO: 语言设置功能待实现 */ },
                            headlineContent = { Text("语言设置 (Language)") },
                            supportingContent = { Text("简体中文 (zh-CN)") },
                            leadingContent = { Icon(Icons.Default.Language, null, tint = MaterialTheme.colorScheme.primary) },
                            trailingContent = { Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.outlineVariant) },
                        )
                    },
                    {
                        ExpressiveSegmentedListItem(
                            index = 2,
                            count = 3,
                            onClick = { /* TODO: 时区设置功能待实现 */ },
                            headlineContent = { Text("时区设置") },
                            supportingContent = { Text("自动获取设备时区 (Asia/Shanghai)") },
                            leadingContent = { Icon(Icons.Default.Schedule, null, tint = MaterialTheme.colorScheme.primary) },
                            trailingContent = { Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.outlineVariant) },
                        )
                    },
                ),
            )
            Spacer(Modifier.height(88.dp))
        }
    }
}

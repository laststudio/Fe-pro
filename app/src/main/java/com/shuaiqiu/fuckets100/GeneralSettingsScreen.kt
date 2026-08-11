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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import com.shuaiqiu.fuckets100.component.material.ExpressiveScaffold
import com.shuaiqiu.fuckets100.component.material.ExpressiveSegmentedSwitchItem
import com.shuaiqiu.fuckets100.component.material.SegmentedColumn
import com.shuaiqiu.fuckets100.component.material.TopBarBackButton
import com.shuaiqiu.fuckets100.component.material.expressiveTopAppBarColors

/**
 * 通用设置页面
 * 包含答案显示相关的通用选项。
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun GeneralSettingsScreen(onBack: () -> Unit) {
    var compactAnswerDisplay by remember { mutableStateOf(SettingsManager.getCompactAnswerDisplay()) }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    ExpressiveScaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text("通用设置") },
                subtitle = { Text("答案显示行为") },
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
            SegmentedColumn(
                title = "通用设置",
                items = listOf(
                    {
                        ExpressiveSegmentedSwitchItem(
                            index = 0,
                            count = 1,
                            checked = compactAnswerDisplay,
                            onCheckedChange = { enabled ->
                                compactAnswerDisplay = enabled
                                SettingsManager.saveCompactAnswerDisplay(enabled)
                            },
                            headlineContent = { Text("精简答案") },
                            supportingContent = { Text("开启后按题型只显示最常用的 1-2 个参考答案") },
                            leadingContent = { Icon(Icons.AutoMirrored.Filled.ShortText, null, tint = MaterialTheme.colorScheme.primary) },
                            modifier = Modifier.clip(MaterialTheme.shapes.extraLarge),
                        )
                    },
                ),
            )
            Spacer(Modifier.height(88.dp))
        }
    }
}

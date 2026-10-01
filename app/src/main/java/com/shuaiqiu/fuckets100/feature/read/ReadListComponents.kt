package com.shuaiqiu.fuckets100.feature.read

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.content.pm.PackageManager
import android.Manifest
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.util.Log
import android.widget.Toast
import org.json.JSONObject
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.shuaiqiu.fuckets100.core.designsystem.ExpressiveScaffold
import com.shuaiqiu.fuckets100.core.designsystem.TonalCard
import com.shuaiqiu.fuckets100.core.designsystem.TopBarBackButton
import com.shuaiqiu.fuckets100.core.designsystem.expressiveTopAppBarColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.charset.StandardCharsets
import com.shuaiqiu.fuckets100.ActivationMode
import com.shuaiqiu.fuckets100.AppCoroutineScope
import com.shuaiqiu.fuckets100.ChangyanWebLoginActivity
import com.shuaiqiu.fuckets100.CloudContent
import com.shuaiqiu.fuckets100.CloudHomeworkState
import com.shuaiqiu.fuckets100.ETS100AnswerReader
import com.shuaiqiu.fuckets100.ETS100ApiClient
import com.shuaiqiu.fuckets100.ETS100AuthManager
import com.shuaiqiu.fuckets100.ETS100FileReader
import com.shuaiqiu.fuckets100.FeAccountFeatures
import com.shuaiqiu.fuckets100.FeAccountManager
import com.shuaiqiu.fuckets100.core.designsystem.FeFilledCard
import com.shuaiqiu.fuckets100.core.designsystem.FeThinDivider
import com.shuaiqiu.fuckets100.core.designsystem.FeTonalIconContainer
import com.shuaiqiu.fuckets100.PaperSource
import com.shuaiqiu.fuckets100.PaperSourceExporter
import com.shuaiqiu.fuckets100.ResolverAuthorizationDeniedException
import com.shuaiqiu.fuckets100.ResolverAuthorizer
import com.shuaiqiu.fuckets100.SettingsManager
import com.shuaiqiu.fuckets100.ZipPasswordGenerator
import com.shuaiqiu.fuckets100.core.designsystem.FeFilledCard
import com.shuaiqiu.fuckets100.core.designsystem.FeThinDivider
import com.shuaiqiu.fuckets100.core.designsystem.FeTonalIconContainer

@Composable
internal fun LocalAnswerParsingCard(
    phase: LocalParsePhase,
    doneCount: Int,
    totalCount: Int,
    sectionCount: Int,
    questionCount: Int,
    completedGroupCount: Int,
    totalGroupCount: Int,
    activeGroupIndex: Int,
    message: String
) {
    val isFinished = phase == LocalParsePhase.COMPLETED
    val isFailed = phase == LocalParsePhase.FAILED
    val progress = if (totalCount > 0) {
        (doneCount.toFloat() / totalCount).coerceIn(0f, 1f)
    } else {
        null
    }
    val groupProgress = if (totalGroupCount > 0) {
        (completedGroupCount.toFloat() / totalGroupCount).coerceIn(0f, 1f)
    } else 0f
    val animatedProgress by animateFloatAsState(
        targetValue = groupProgress,
        animationSpec = tween(320),
        label = "localAnswerGroupProgress",
    )
    val title = when (phase) {
        LocalParsePhase.SCANNING -> "正在扫描试卷列表"
        LocalParsePhase.PARSING -> "正在后台解析答案"
        LocalParsePhase.COMPLETED -> "答案解析完成"
        LocalParsePhase.FAILED -> "答案解析异常"
        LocalParsePhase.IDLE -> "读取准备中"
    }
    val description = when (phase) {
        LocalParsePhase.SCANNING -> message.ifBlank { "正在批量读取资源目录并生成试卷列表" }
        LocalParsePhase.PARSING -> {
            val groupText = if (totalGroupCount > 0) "（已完成 $completedGroupCount/$totalGroupCount 组）" else ""
            val activeText = if (activeGroupIndex >= 0 && totalGroupCount > 0) "正在处理第 ${activeGroupIndex + 1} 组" else "正在后台解析答案"
            message.ifBlank { activeText } + groupText
        }
        LocalParsePhase.COMPLETED -> message.ifBlank { "全部试卷已经解析完成" }
        LocalParsePhase.FAILED -> message.ifBlank { "后台解析失败，请重新读取" }
        LocalParsePhase.IDLE -> message
    }
    val statsText = "已解析 $sectionCount 个分区 · $questionCount 道题"
    val iconColor = when {
        isFinished -> MaterialTheme.colorScheme.primary
        isFailed -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.primary
    }
    val containerColor = if (isFailed) {
        MaterialTheme.colorScheme.errorContainer
    } else {
        MaterialTheme.colorScheme.surfaceBright
    }
    val textColor = if (isFailed) {
        MaterialTheme.colorScheme.onErrorContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    TonalCard(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(animationSpec = tween(260))
            .then(
                if (isFailed) Modifier.border(1.dp, MaterialTheme.colorScheme.error, MaterialTheme.shapes.large)
                else Modifier
            ),
        containerColor = containerColor,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                when {
                    isFinished -> {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = iconColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    isFailed -> {
                        Icon(
                            Icons.Default.ErrorOutline,
                            contentDescription = null,
                            tint = iconColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    else -> {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = iconColor
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    AnimatedContent(
                        targetState = title,
                        transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(120)) },
                        label = "localAnswerPhaseTitle",
                    ) { phaseTitle ->
                        Text(
                            text = phaseTitle,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = iconColor,
                        )
                    }
                    AnimatedContent(
                        targetState = description,
                        transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(120)) },
                        label = "localAnswerPhaseDescription",
                    ) { phaseDescription ->
                        Text(
                            text = phaseDescription,
                            style = MaterialTheme.typography.bodySmall,
                            color = textColor,
                        )
                    }
                    if (sectionCount > 0 || questionCount > 0) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = statsText,
                            style = MaterialTheme.typography.labelMedium,
                            color = textColor
                        )
                    }
                }
            }
            if (!isFinished && !isFailed) {
                Spacer(modifier = Modifier.height(12.dp))
                // 组内解析目前没有更细粒度的回调：上方使用真实的组完成进度，
                // 同时保留一条流动指示，避免把静态百分比误认为实时精确进度。
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                if (totalGroupCount > 0) {
                    Spacer(Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { animatedProgress },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

@Composable
internal fun CloudHomeworkCategorySelector(
    selectedCategory: String,
    homeworkCount: Int,
    onCategorySelect: (String) -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    var menuOffsetX by remember { mutableStateOf(0.dp) }
    var anchorWidthPx by remember { mutableIntStateOf(0) }
    val density = LocalDensity.current
    val chevronRotation by animateFloatAsState(
        targetValue = if (menuExpanded) 180f else 0f,
        label = "cloudCategoryChevron",
    )

    val categoryTitle = when (selectedCategory) {
        CloudHomeworkState.CATEGORY_HISTORY -> "历史作业"
        CloudHomeworkState.CATEGORY_VOCAB_PK -> "词汇PK"
        else -> "当前作业"
    }

    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedCard(
            modifier = Modifier
                .fillMaxWidth()
                .onGloballyPositioned { anchorWidthPx = it.size.width }
                .pointerInput(density, anchorWidthPx) {
                    detectTapGestures { position ->
                        val menuWidthPx = with(density) { 200.dp.toPx() }
                        val maxOffsetPx = (anchorWidthPx - menuWidthPx).coerceAtLeast(0f)
                        val centeredOffsetPx = position.x - menuWidthPx / 2f
                        menuOffsetX = with(density) {
                            centeredOffsetPx.coerceIn(0f, maxOffsetPx).toDp()
                        }
                        menuExpanded = true
                    }
                },
            shape = MaterialTheme.shapes.medium,
            colors = CardDefaults.outlinedCardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = categoryTitle,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = homeworkCount.toString(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.weight(1f))
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = if (menuExpanded) "收起作业分类" else "选择作业分类",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .size(24.dp)
                        .rotate(chevronRotation),
                )
            }
        }

        // 以触点为菜单中心，并将菜单限制在入口卡片范围内，避免贴边或越界。
        DropdownMenu(
            expanded = menuExpanded,
            onDismissRequest = { menuExpanded = false },
            offset = DpOffset(x = menuOffsetX, y = 4.dp),
            modifier = Modifier.width(200.dp),
            shape = MaterialTheme.shapes.large,
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            tonalElevation = 3.dp,
        ) {
            listOf(
                "当前作业" to CloudHomeworkState.CATEGORY_CURRENT,
                "历史作业" to CloudHomeworkState.CATEGORY_HISTORY,
                "词汇PK" to CloudHomeworkState.CATEGORY_VOCAB_PK,
            ).forEach { (label, category) ->
                DropdownMenuItem(
                    text = { Text(label, style = MaterialTheme.typography.labelLarge) },
                    trailingIcon = if (selectedCategory == category) {
                        { Icon(Icons.Default.Check, contentDescription = null) }
                    } else null,
                    onClick = {
                        menuExpanded = false
                        onCategorySelect(category)
                    },
                )
            }
        }
    }
}

/**
 * 宝贝删除确认对话框喵~
 * 用户点击删除按钮后会弹出这个对话框确认
 */
@Composable
internal fun DeleteConfirmDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    isCloudMode: Boolean = false
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(48.dp)
            )
        },
        title = {
            Text(if (isCloudMode) "确认清除云端缓存？" else "确认删除本地缓存？")
        },
        text = {
            if (isCloudMode) {
                Text("这将清除所有云端缓存的作业数据。\n\n此操作不可撤销！")
            } else {
                Text(
                    "这将删除本地题目缓存：\n\n" +
                        "• data/ 目录\n" +
                        "• resource/ 目录\n\n" +
                        "删除后可以重新前往 e听说客户端下载题目缓存并再次读取。\n" +
                        "不会影响 e听说账号、登录状态或其他应用数据。\n\n" +
                        "此操作不可撤销。"
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text(if (isCloudMode) "确认清除" else "确认删除")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

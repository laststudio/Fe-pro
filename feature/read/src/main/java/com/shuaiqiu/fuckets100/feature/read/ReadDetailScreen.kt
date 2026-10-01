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
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
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

/**
 * 试卷详情二级页面
 * 宝贝这个页面显示一个试卷的所有题目喵~
 * 已移除搜索功能，显示所有题目喵~
 */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaperDetailScreen(
    paper: ETS100AnswerReader.Paper,
    onBack: () -> Unit,
    categoryColors: Map<String, Color>,
    compactAnswerDisplay: Boolean = SettingsManager.getCompactAnswerDisplay(),
    onCopyText: () -> Unit = {},
    accountManager: FeAccountManager = hiltViewModel<ReadViewModel>().accountManager,
) {
    var showMenu by remember { mutableStateOf(false) }
    var sourceExportPaper by remember { mutableStateOf<ETS100AnswerReader.Paper?>(null) }
    val context = LocalContext.current
    val defaultPrimaryColor = MaterialTheme.colorScheme.primary
    val categoryPalette = answerCategoryPalette()
    val fallbackCategoryStyle = fallbackAnswerCategoryStyle(defaultPrimaryColor)
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    ExpressiveScaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        contentWindowInsets = WindowInsets.safeDrawing.only(
            WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom
        ),
        topBar = {
            LargeFlexibleTopAppBar(
                title = {
                    Text(
                        text = paper.title,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                subtitle = {
                    Text("${paper.sections.sumOf { it.questions.size }} 道题目")
                },
                navigationIcon = { TopBarBackButton(onClick = onBack) },
            actions = {
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(
                            Icons.Default.MoreVert,
                            contentDescription = "\u66f4\u591a\u64cd\u4f5c"
                        )
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("\u590d\u5236\u7b54\u6848") },
                            onClick = {
                                showMenu = false
                                onCopyText()
                            },
                            leadingIcon = {
                                Icon(Icons.Default.ContentCopy, contentDescription = null)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("\u5bfc\u51fa\u56fe\u7247") },
                            onClick = {
                                showMenu = false
                                exportPaperAsImage(context, paper, accountManager)
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Image, contentDescription = null)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(if (paper.source != null) "源文件导出" else "源文件不可用") },
                            onClick = {
                                showMenu = false
                                if (paper.source != null) {
                                    sourceExportPaper = paper
                                } else {
                                    Toast.makeText(context, "来源信息已失效，请重新读取该试卷", Toast.LENGTH_LONG).show()
                                }
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Archive, contentDescription = null)
                            }
                        )
                    }
                }
            },
                windowInsets = WindowInsets.safeDrawing.only(
                    WindowInsetsSides.Top + WindowInsetsSides.Horizontal
                ),
                colors = expressiveTopAppBarColors(),
                scrollBehavior = scrollBehavior,
            )
        },
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(
                start = 16.dp,
                top = 16.dp,
                end = 16.dp,
                bottom = 16.dp
            ),
            verticalArrangement = Arrangement.spacedBy(13.dp)
        ) {
            item(key = "paper_metadata") {
                PaperMetadataCard(paper)
            }

            if (paper.sections.none { it.questions.isNotEmpty() }) {
                item(key = "paper_empty") {
                    TonalCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(20.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.AutoMirrored.Filled.HelpOutline, contentDescription = null)
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text("暂无可显示的答案", style = MaterialTheme.typography.titleSmall)
                                Text(
                                    "该试卷没有解析出有效题目，请尝试重新读取。",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }

            paper.sections.forEachIndexed { sectionIndex, section ->
                val categoryStyle = categoryPalette[section.category]
                    ?: fallbackCategoryStyle.copy(accent = categoryColors[section.category] ?: defaultPrimaryColor)

                if (section.questions.isNotEmpty()) {
                    item {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(4.dp, 20.dp)
                                    .background(categoryStyle.accent, RoundedCornerShape(2.dp))
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = section.title,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = categoryStyle.accent,
                                modifier = Modifier.weight(1f),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                color = categoryStyle.container.copy(alpha = 0.76f),
                                shape = MaterialTheme.shapes.small
                            ) {
                                Text(
                                    text = "${section.displayCategoryLabel()} · ${section.questions.size} 题",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = categoryStyle.onContainer,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.widthIn(max = 136.dp).padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    val groupedQuestions = section.questions.groupBy { it.originalText }

                    groupedQuestions.entries.forEachIndexed { groupIndex, (_, questionsInGroup) ->
                        item(key = "section:$sectionIndex:group:$groupIndex") {
                            MergedQuestionBlock(
                                groupKey = "${paper.paperId}:$sectionIndex:$groupIndex",
                                sectionTitle = section.title,
                                sectionCategory = section.category,
                                questions = questionsInGroup,
                                categoryStyle = categoryStyle,
                                compactAnswerDisplay = compactAnswerDisplay
                            )
                        }
                    }
                }
            }
        }
    }

    SourceExportDialogHost(
        paper = sourceExportPaper,
        onDismissRequest = { sourceExportPaper = null }
    )
}

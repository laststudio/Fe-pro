package com.shuaiqiu.fuckets100.feature.read

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Badge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.shuaiqiu.fuckets100.ETS100AnswerReader
import com.shuaiqiu.fuckets100.core.designsystem.TonalCard

import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.style.TextAlign
import com.shuaiqiu.fuckets100.CloudHomeworkState
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
private fun ETS100AnswerReader.Paper.firstQuestionPreviewText(): String {
    val firstQuestion = sections
        .asSequence()
        .flatMap { it.questions.asSequence() }
        .firstOrNull { question ->
            question.category != LOCAL_PAPER_LOADING_CATEGORY &&
                question.questionText != "未下载"
        } ?: return ""

    return when {
        firstQuestion.category == "read_chapter" -> {
            firstQuestion.formattedOriginalText.firstSentenceOrLine()
                .ifBlank { firstQuestion.question.firstSentenceOrLine() }
        }
        else -> {
            val choiceOption = (firstQuestion.content as? ETS100AnswerReader.AnswerContent.Choice)
                ?.items
                ?.asSequence()
                ?.flatMap { it.options.asSequence() }
                ?.firstOrNull { it.isNotBlank() }
                ?.trim()
                ?: "${firstQuestion.question}\n${firstQuestion.formattedOriginalText}".firstChoiceOption()
            choiceOption ?: firstQuestion.question.ifBlank { firstQuestion.formattedOriginalText }.firstSentenceOrLine()
        }
    }.normalizePreviewText()
}

private fun String.firstChoiceOption(): String? {
    val match = Regex(
        pattern = """(?is)(?:^|[\s\n])(?:A|①|1)[.．、)]?\s*(.+?)(?=[\s\n](?:B|②|2)[.．、)]?\s*)"""
    ).find(this)

    return match
        ?.groupValues
        ?.getOrNull(1)
        ?.normalizePreviewText()
        ?.takeIf { it.isNotBlank() }
}

private fun String.firstSentenceOrLine(): String {
    val normalized = normalizePreviewText()
    if (normalized.isBlank()) return ""

    val sentenceEnd = listOf("。", "！", "？", ".", "!", "?")
        .map { normalized.indexOf(it) }
        .filter { it >= 0 }
        .minOrNull()

    return if (sentenceEnd != null) {
        normalized.take(sentenceEnd + 1)
    } else {
        normalized.lineSequence().firstOrNull { it.isNotBlank() }?.trim().orEmpty()
    }
}

private fun String.normalizePreviewText(): String =
    replace(Regex("\\s+"), " ").trim()

private fun ETS100AnswerReader.Paper.localDisplayNumberFromTitle(): Int? =
    Regex("""#(\d+)\s*$""").find(title)?.groupValues?.getOrNull(1)?.toIntOrNull()

/**
 * 试卷列表项组件
 * 宝贝这是一个简单的试卷项目，点击后进入二级页面喵~
 */
@Composable
internal fun PaperListItem(
    paper: ETS100AnswerReader.Paper,
    paperIndex: Int,
    onClick: () -> Unit,
    categoryColors: Map<String, Color>,
    isLoading: Boolean = false,
    isFailed: Boolean = false,
    isDownloaded: Boolean = false,  // 宝贝标记是否已下载喵~
    isClickEnabled: Boolean = true,
    downloadProgress: CloudHomeworkState.DownloadProgress? = null,
    isCloudMode: Boolean = false,
    onExportSource: (() -> Unit)? = null
) {
    val localPreviewText = remember(paper) { paper.firstQuestionPreviewText() }
    var showMenu by remember { mutableStateOf(false) }
    val listNumber = if (isCloudMode) {
        paperIndex + 1
    } else {
        paper.localDisplayNumberFromTitle() ?: (paperIndex + 1)
    }
    
    TonalCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                enabled = isClickEnabled,
                onClick = onClick
            ),
        containerColor = if (isFailed) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceBright
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // 宝贝根据状态显示不同的图标喵~
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            when {
                                isLoading -> MaterialTheme.colorScheme.primaryContainer
                                isFailed -> MaterialTheme.colorScheme.errorContainer
                                else -> MaterialTheme.colorScheme.primaryContainer
                            },
                            MaterialTheme.shapes.medium
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        isLoading -> {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        isFailed -> {
                            Icon(
                                Icons.Default.ErrorOutline,
                                contentDescription = "加载失败",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        else -> {
                            Text(
                                text = "$listNumber",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 宝贝添加地区标签 Badge 喵~
                        if (paper.regionLabel != "未知") {
                            val regionContainerColor = when (paper.regionLabel) {
                                "初中" -> MaterialTheme.colorScheme.secondaryContainer
                                "高中" -> MaterialTheme.colorScheme.secondaryContainer
                                "词汇PK", "PK/词汇" -> MaterialTheme.colorScheme.tertiaryContainer
                                else -> MaterialTheme.colorScheme.surfaceContainer
                            }
                            val regionContentColor = when (paper.regionLabel) {
                                "初中" -> MaterialTheme.colorScheme.onSecondaryContainer
                                "高中" -> MaterialTheme.colorScheme.onSecondaryContainer
                                "词汇PK", "PK/词汇" -> MaterialTheme.colorScheme.onTertiaryContainer
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            }
                            Surface(
                                color = regionContainerColor,
                                shape = MaterialTheme.shapes.small
                            ) {
                                Text(
                                    text = paper.regionLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = regionContentColor,
                                    maxLines = 1,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text(
                            text = paper.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        if (isDownloaded) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                modifier = Modifier.widthIn(min = 48.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f),
                                shape = MaterialTheme.shapes.small
                            ) {
                                Text(
                                    text = "已下载",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    maxLines = 1,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    if (!isCloudMode && localPreviewText.isNotBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = localPreviewText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    // 宝贝根据状态显示不同的信息喵~
                    when {
                        isLoading -> {
                            val progressText = downloadProgress?.let { progress ->
                                val totalText = if (progress.totalBytes > 0L) {
                                    " / ${formatFileSize(progress.totalBytes)}"
                                } else {
                                    ""
                                }
                                "下载中 ${formatFileSize(progress.downloadedBytes)}$totalText"
                            } ?: "正在加载..."
                            Text(
                                text = progressText,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            downloadProgress?.progressFraction?.let { progressFraction ->
                                LinearProgressIndicator(
                                    progress = { progressFraction },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            } ?: LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                            downloadProgress?.currentFileName?.takeIf { it.isNotBlank() }?.let { currentFileName ->
                                Text(
                                    text = currentFileName,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        isFailed -> {
                            Text(
                                text = if (onExportSource != null) "解析失败，点击重试或导出源文件" else "加载失败，点击重试",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                        else -> {
                            val countText = "${paper.sections.size} 个分区 · ${paper.sections.sumOf { it.questions.size }} 个内容"
                            Text(
                                text = countText,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (isCloudMode) {
                                Text(
                                    text = if (isDownloaded) "点击查看答案" else "点击下载并解析",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isDownloaded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            } else {
                                Text(
                                    text = "点击查看答案",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                    }
                }
            }

            if (onExportSource != null) {
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(
                            Icons.Default.MoreVert,
                            contentDescription = "更多操作",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("源文件导出") },
                            onClick = {
                                showMenu = false
                                onExportSource()
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Archive, contentDescription = null)
                            }
                        )
                    }
                }
            } else if (!isLoading && !isFailed) {
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = "进入",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}


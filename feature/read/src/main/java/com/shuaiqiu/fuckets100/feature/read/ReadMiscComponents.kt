package com.shuaiqiu.fuckets100.feature.read

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.shuaiqiu.fuckets100.ETS100AnswerReader
import com.shuaiqiu.fuckets100.core.designsystem.TonalCard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FolderOff
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.Badge
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.IconButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.window.Dialog
import com.shuaiqiu.fuckets100.core.designsystem.FeFilledCard
import com.shuaiqiu.fuckets100.core.designsystem.FeThinDivider
@Composable
internal fun DeletingOverlay() {
    Dialog(onDismissRequest = {}) {
        FeFilledCard(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "正在删除...",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "请稍等，文件清理在后台执行",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
internal fun EmptyCloudHomeworkCard(label: String, hasLoaded: Boolean, isVocabPk: Boolean = false) {
    TonalCard(
        modifier = Modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.surfaceBright
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                if (hasLoaded) {
                    if (isVocabPk) Icons.Default.SportsEsports else Icons.Default.FolderOff
                } else Icons.Default.CloudDownload,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = if (hasLoaded) "未找到$label" else "尚未读取$label",
                    style = MaterialTheme.typography.titleSmall,
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = if (hasLoaded) {
                        if (isVocabPk) "未在当前已同步作业中检测到词汇PK题目" else "该分类暂时没有可用作业"
                    } else "点击右下角“读取作业”获取列表",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
internal fun PaperCard(
    paper: ETS100AnswerReader.Paper,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    expandedSections: Set<Pair<Int, Int>>,
    onSectionToggle: (Int) -> Unit,
    selectedSectionIndex: Int,
    selectedQuestionIndex: Int,
    onQuestionSelect: (Int, Int) -> Unit,
    categoryColors: Map<String, Color>,
    searchQuery: String,
    showOnlyAnswered: Boolean
) {
    TonalCard(
        modifier = Modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.surfaceBright
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggleExpand),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(
                        if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (isExpanded) "收起" else "展开",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            paper.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            "${paper.sections.size} 个分区 · ${paper.sections.sumOf { it.questions.size }} 道题目",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                Badge(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ) {
                    Text("${paper.sections.size}")
                }
            }
            
            if (isExpanded) {
                Spacer(modifier = Modifier.height(12.dp))
                FeThinDivider()
                Spacer(modifier = Modifier.height(12.dp))
                
                paper.sections.forEachIndexed { sectionIndex, section ->
                    val sectionKey = Pair(paper.sections.indexOf(section), sectionIndex)
                    val isSectionExpanded = sectionKey in expandedSections
                    
                    SectionItem(
                        section = section,
                        sectionIndex = sectionIndex,
                        isExpanded = isSectionExpanded,
                        onToggleExpand = { onSectionToggle(sectionIndex) },
                        selectedQuestionIndex = if (selectedSectionIndex == sectionIndex) selectedQuestionIndex else -1,
                        onQuestionSelect = { onQuestionSelect(sectionIndex, it) },
                        categoryColor = categoryColors[section.category] ?: MaterialTheme.colorScheme.primary,
                        searchQuery = searchQuery,
                        showOnlyAnswered = showOnlyAnswered
                    )
                    
                    if (sectionIndex < paper.sections.size - 1) {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

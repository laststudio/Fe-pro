package com.shuaiqiu.fuckets100.feature.read

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.shuaiqiu.fuckets100.ETS100AnswerReader
import com.shuaiqiu.fuckets100.PaperSource
import com.shuaiqiu.fuckets100.core.designsystem.TonalCard

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import com.shuaiqiu.fuckets100.CloudHomeworkState
import com.shuaiqiu.fuckets100.core.designsystem.FeTonalIconContainer
@Composable
internal fun PaperMetadataCard(paper: ETS100AnswerReader.Paper) {
    val sourceLabel = when (paper.source) {
        is PaperSource.Cloud -> "云端作业"
        is PaperSource.Local -> "本地读取"
        null -> if (paper.regionLabel == "云端") "云端缓存" else "缓存数据"
    }
    TonalCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            DetailInfoItem("来源", sourceLabel)
            DetailInfoItem("地区", paper.regionLabel.takeUnless { it == "未知" }.orEmpty().ifBlank { "未标注" })
            DetailInfoItem("内容", "${paper.sections.size} 个分区 · ${paper.sections.sumOf { it.questions.size }} 道题目")
            if (paper.fileSize > 0L) {
                DetailInfoItem("数据大小", formatFileSize(paper.fileSize))
            }
        }
    }
}

@Composable
internal fun DetailInfoItem(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

internal fun ETS100AnswerReader.Section.displayCategoryLabel(): String = when (category) {
    "read_chapter" -> "模仿朗读"
    "simple_expression_ufi" -> typeName.ifBlank { "听说信息" }
    "simple_expression_ufk" -> typeName.ifBlank { "问答信息" }
    "simple_expression_ufj" -> typeName.ifBlank { "询问信息" }
    "topic" -> typeName.ifBlank { "信息转述" }
    ETS100AnswerReader.StructureType.COLLECTOR_READ -> typeName.ifBlank { "模仿朗读" }
    ETS100AnswerReader.StructureType.COLLECTOR_ROLE -> typeName.ifBlank { "问答信息" }
    ETS100AnswerReader.StructureType.COLLECTOR_PICTURE -> typeName.ifBlank { "信息转述" }
    ETS100AnswerReader.StructureType.COLLECTOR_3Q5A -> typeName.ifBlank { "3问5答" }
    ETS100AnswerReader.StructureType.COLLECTOR_CHOOSE -> typeName.ifBlank { "听后选择" }
    ETS100AnswerReader.StructureType.COLLECTOR_FILL -> typeName.ifBlank { "听后记录" }
    ETS100AnswerReader.StructureType.COLLECTOR_DIALOGUE -> typeName.ifBlank { "回答问题" }
    ETS100AnswerReader.StructureType.COLLECTOR_REPEAT_ESSAY -> typeName.ifBlank { "短文复述" }
    ETS100AnswerReader.StructureType.WORD_CHOICE -> typeName.ifBlank { "词汇选择" }
    ETS100AnswerReader.StructureType.WORD_WRITE -> typeName.ifBlank { "单词拼写" }
    ETS100AnswerReader.StructureType.WORD_READ -> typeName.ifBlank { "单词跟读" }
    ETS100AnswerReader.StructureType.WORD_DICTATION -> typeName.ifBlank { "词汇听写" }
    ETS100AnswerReader.StructureType.WORD_TRANSLATE_EN_CN -> typeName.ifBlank { "英译中" }
    ETS100AnswerReader.StructureType.WORD_TRANSLATE_CN_EN -> typeName.ifBlank { "中译英" }
    else -> typeName.ifBlank { category.ifBlank { "其他" } }
}


/**
 * 宝贝云端模式信息卡片喵~
 * 显示当前为云端作业模式
 */
@Composable
internal fun CloudModeInfoCard(
    selectedStatus: String,
) {
    val isHistory = selectedStatus == CloudHomeworkState.STATUS_HISTORY
    TonalCard(
        modifier = Modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.surfaceBright
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FeTonalIconContainer(
                icon = Icons.Default.Cloud,
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.54f),
                size = 44.dp,
                iconSize = 24.dp
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = if (isHistory) "历史作业" else "当前作业",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "选择作业下载并解析，已下载项目可直接查看答案",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

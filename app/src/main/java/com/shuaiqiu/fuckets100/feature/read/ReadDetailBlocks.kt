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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.shuaiqiu.fuckets100.ETS100AnswerReader

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.style.TextOverflow
import com.shuaiqiu.fuckets100.core.designsystem.FeThinDivider
import com.shuaiqiu.fuckets100.core.designsystem.TonalCard
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.CheckCircle
@Composable
internal fun MergedQuestionBlock(
    groupKey: String,
    sectionTitle: String,
    sectionCategory: String,
    questions: List<ETS100AnswerReader.Question>,
    categoryStyle: AnswerCategoryColor,
    compactAnswerDisplay: Boolean
) {
    val originalText = questions.firstOrNull()?.formattedOriginalText.orEmpty()
    val firstQuestion = questions.firstOrNull()
    val isReadPassage = sectionCategory == "read_chapter" ||
        sectionCategory == ETS100AnswerReader.StructureType.COLLECTOR_READ ||
        firstQuestion?.category == "read_chapter" ||
        firstQuestion?.typeName == ETS100AnswerReader.StructureType.COLLECTOR_READ
    var originalExpanded by rememberSaveable(groupKey) { mutableStateOf(isReadPassage) }
    TonalCard(
        modifier = Modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.surfaceBright
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // 块头部 - 显示原文和题目数量
            if (originalText.isNotBlank()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(4.dp, 32.dp)
                            .background(categoryStyle.accent, RoundedCornerShape(2.dp))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "原文 · ${questions.size} 题",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = categoryStyle.accent
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                color = categoryStyle.container.copy(alpha = 0.76f),
                                shape = MaterialTheme.shapes.small
                            ) {
                                Text(
                                    sectionTitle,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = categoryStyle.onContainer,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.widthIn(max = 116.dp).padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    
                    val hasAllAnswers = questions.all { it.answer.isNotEmpty() }
                    if (hasAllAnswers) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = "全部有答案",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    IconButton(onClick = { originalExpanded = !originalExpanded }) {
                        Icon(
                            if (originalExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (originalExpanded) "收起原文" else "展开原文",
                        )
                    }
                }

                AnimatedVisibility(visible = originalExpanded) {
                    Column {
                        Spacer(modifier = Modifier.height(12.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            shape = MaterialTheme.shapes.large
                        ) {
                            Text(
                                text = originalText,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                FeThinDivider()
                Spacer(modifier = Modifier.height(12.dp))
            }
            if (originalText.isBlank()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(4.dp, 24.dp)
                            .background(categoryStyle.accent, RoundedCornerShape(2.dp)),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "$sectionTitle · ${questions.size} 题",
                        style = MaterialTheme.typography.titleSmall,
                        color = categoryStyle.accent,
                    )
                }
                Spacer(Modifier.height(12.dp))
            }

            // 题目列表
            questions.forEachIndexed { index, question ->
                if (index > 0) {
                    Spacer(modifier = Modifier.height(12.dp))
                    FeThinDivider()
                    Spacer(modifier = Modifier.height(12.dp))
                }
                
                // 单个题目
                QuestionItemSimple(
                    questionIndex = index,
                    question = question,
                    categoryStyle = categoryStyle,
                    compactAnswerDisplay = compactAnswerDisplay,
                    isReadPassage = isReadPassage,
                )
            }
        }
    }
}

/**
 * 简化题目项组件
 * 宝贝用于在合并块中显示单个题目喵~
 */
@Composable
internal fun QuestionItemSimple(
    questionIndex: Int,
    question: ETS100AnswerReader.Question,
    categoryStyle: AnswerCategoryColor,
    compactAnswerDisplay: Boolean,
    isReadPassage: Boolean,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // 题目编号和题目内容
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .background(categoryStyle.container.copy(alpha = 0.76f), MaterialTheme.shapes.small)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    "Q${question.displayOrder ?: questionIndex + 1}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = categoryStyle.onContainer
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // 题目内容
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = MaterialTheme.shapes.large,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.32f))
        ) {
            Text(
                text = question.question,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(12.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // 答案是详情页的主要信息，使用高对比 tonal surface 直接展示。
        if (question.answerList.isNotEmpty()) {
            CollapsibleItem(
                title = "答案",
                content = formatAnswerForDisplay(question, compactAnswerDisplay),
                defaultExpanded = compactAnswerDisplay,
            )
        } else {
            val noAnswerText = if (isReadPassage) {
                "该类型无需参考答案"
            } else {
                "暂无答案"
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh, MaterialTheme.shapes.large)
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.HelpOutline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    noAnswerText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}


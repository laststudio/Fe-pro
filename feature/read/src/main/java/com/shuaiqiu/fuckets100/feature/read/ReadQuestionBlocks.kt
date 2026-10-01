package com.shuaiqiu.fuckets100.feature.read

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.shuaiqiu.fuckets100.core.designsystem.FeFilledCard
import com.shuaiqiu.fuckets100.core.designsystem.FeThinDivider

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.style.TextOverflow
import com.shuaiqiu.fuckets100.SettingsManager
import com.shuaiqiu.fuckets100.core.designsystem.TonalCard
@Composable
internal fun SectionItem(
    section: ETS100AnswerReader.Section,
    sectionIndex: Int,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    selectedQuestionIndex: Int,
    onQuestionSelect: (Int) -> Unit,
    categoryColor: Color,
    searchQuery: String,
    showOnlyAnswered: Boolean
) {
    var expanded by remember { mutableStateOf(isExpanded) }
    val categoryStyle = answerCategoryStyle(section.category, categoryColor)
    
    LaunchedEffect(isExpanded) {
        expanded = isExpanded
    }
    
    TonalCard(
        modifier = Modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.surfaceBright
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f),
                ) {
                    Box(
                        modifier = Modifier
                            .size(4.dp, 24.dp)
                            .background(categoryStyle.accent, RoundedCornerShape(2.dp))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            section.title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            "${section.questions.size} 道题目",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        section.category,
                        style = MaterialTheme.typography.labelSmall,
                        color = categoryStyle.accent
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (expanded) "收起" else "展开",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            if (expanded) {
                Spacer(modifier = Modifier.height(8.dp))
                
                section.questions.forEachIndexed { questionIndex, question ->
                    val matchesSearch = searchQuery.isEmpty() || 
                        question.question.contains(searchQuery, ignoreCase = true) ||
                        question.answer.contains(searchQuery, ignoreCase = true)
                    
                    val hasAnswer = question.answer.isNotEmpty()
                    
                    if (matchesSearch && (!showOnlyAnswered || hasAnswer)) {
                        QuestionItem(
                            question = question,
                            questionIndex = questionIndex,
                            isSelected = selectedQuestionIndex == questionIndex,
                            onClick = { onQuestionSelect(questionIndex) },
                            categoryStyle = categoryStyle
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun QuestionItem(
    question: ETS100AnswerReader.Question,
    questionIndex: Int,
    isSelected: Boolean,
    onClick: () -> Unit,
    categoryStyle: AnswerCategoryColor
) {
    FeFilledCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        containerColor = if (isSelected) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.38f)
        } else {
            MaterialTheme.colorScheme.surface
        },
        borderColor = if (isSelected) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
        } else {
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.32f)
        },
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Q${question.displayOrder ?: questionIndex + 1}",
                    style = MaterialTheme.typography.labelMedium,
                    color = categoryStyle.accent,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    question.question,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            if (question.answer.isNotEmpty()) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = "有答案",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            } else {
                Icon(
                    Icons.AutoMirrored.Filled.HelpOutline,
                    contentDescription = "无答案",
                    tint = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

/**
 * 可折叠项组件
 * 宝贝这个组件用于显示原文和答案的折叠项喵~
 */
@Composable
internal fun CollapsibleItem(
    title: String,
    content: String,
    defaultExpanded: Boolean = false
) {
    var expanded by remember { mutableStateOf(defaultExpanded) }
    
    FeFilledCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Icon(
                    if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "收起" else "展开",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Text(
                    text = content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}

/**
 * 题目块组件
 * 宝贝每个题目显示为一个块，包含题目信息和两个折叠项（原文、答案）喵~
 */
@Composable
internal fun QuestionBlock(
    questionIndex: Int,
    sectionTitle: String,
    question: ETS100AnswerReader.Question,
    categoryColor: Color,
    defaultOriginalExpanded: Boolean = false,
    defaultAnswerExpanded: Boolean = false,  // 宝贝答案默认折叠喵~
    compactAnswerDisplay: Boolean = SettingsManager.getCompactAnswerDisplay()
) {
    val categoryStyle = fallbackAnswerCategoryStyle(categoryColor)

    FeFilledCard(
        modifier = Modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // 题目头部
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
                            "Q${question.displayOrder ?: questionIndex + 1}",
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
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                
                if (question.answer.isNotEmpty()) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = "有答案",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // 题目内容
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                shape = MaterialTheme.shapes.small
            ) {
                Text(
                    text = question.question,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(12.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // 原文折叠项
            if (question.formattedOriginalText.isNotEmpty()) {
                CollapsibleItem(
                    title = "原文",
                    content = question.formattedOriginalText,
                    defaultExpanded = defaultOriginalExpanded
                )
            }
            
            // 答案折叠项
            if (question.answerList.isNotEmpty()) {
                CollapsibleItem(
                    title = "答案",
                    content = formatAnswerForDisplay(question, compactAnswerDisplay),
                    defaultExpanded = defaultAnswerExpanded || compactAnswerDisplay
                )
            }
        }
    }
}

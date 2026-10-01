package com.shuaiqiu.fuckets100.feature.read

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.shuaiqiu.fuckets100.ETS100AnswerReader

@Composable
internal fun answerCategoryStyle(
    category: String,
    fallbackAccent: Color = MaterialTheme.colorScheme.primary
): AnswerCategoryColor {
    return answerCategoryPalette()[category] ?: fallbackAnswerCategoryStyle(fallbackAccent)
}

@Composable
internal fun fallbackAnswerCategoryStyle(accent: Color): AnswerCategoryColor {
    return AnswerCategoryColor(
        accent = accent,
        container = MaterialTheme.colorScheme.secondaryContainer,
        onContainer = MaterialTheme.colorScheme.onSecondaryContainer
    )
}

internal fun displayAnswerList(
    question: ETS100AnswerReader.Question,
    compactAnswerDisplay: Boolean
): List<String> {
    if (!compactAnswerDisplay) return question.answerList

    val isZhuanShu = question.typeName.contains("转述") || question.category == "topic"
    return question.answerList.take(if (isZhuanShu) 1 else 2)
}

internal fun formatAnswerForDisplay(
    question: ETS100AnswerReader.Question,
    compactAnswerDisplay: Boolean
): String {
    if (!compactAnswerDisplay) return question.formattedAnswer

    val isZhuanShu = question.typeName.contains("转述") || question.category == "topic"
    val cleaned = displayAnswerList(question, compactAnswerDisplay).map { answer ->
        ETS100AnswerReader.cleanAnswerText(answer).lines()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
    }

    return if (isZhuanShu) {
        cleaned.flatten().joinToString(" ")
    } else {
        cleaned.joinToString("\n") { it.joinToString(" ") }
    }
}


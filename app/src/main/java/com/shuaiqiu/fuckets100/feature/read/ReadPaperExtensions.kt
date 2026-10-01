package com.shuaiqiu.fuckets100.feature.read

import com.shuaiqiu.fuckets100.ETS100AnswerReader

internal const val LOCAL_PAPER_LOADING_CATEGORY = "local_loading"

internal fun List<ETS100AnswerReader.Paper>.totalSectionCount(): Int = sumOf { it.sections.size }

internal fun List<ETS100AnswerReader.Paper>.totalQuestionCount(): Int =
    sumOf { paper -> paper.sections.sumOf { section -> section.questions.size } }

internal fun ETS100AnswerReader.Paper.isLocalAnswerLoading(): Boolean {
    return sections.any { it.category == LOCAL_PAPER_LOADING_CATEGORY }
}


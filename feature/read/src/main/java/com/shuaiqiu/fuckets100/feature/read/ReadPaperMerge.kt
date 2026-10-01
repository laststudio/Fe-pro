package com.shuaiqiu.fuckets100.feature.read

import com.shuaiqiu.fuckets100.ETS100AnswerReader

internal fun mergeParsedGroup(
    currentPapers: List<ETS100AnswerReader.Paper>,
    groupIndex: Int,
    parsedGroupPapers: List<ETS100AnswerReader.Paper>,
): List<ETS100AnswerReader.Paper> = currentPapers.toMutableList().also { papers ->
    parsedGroupPapers.forEachIndexed { offset, parsedPaper ->
        val existingIndex = papers.indexOfFirst { it.paperId == parsedPaper.paperId }
        if (existingIndex >= 0) {
            papers[existingIndex] = parsedPaper
        } else {
            val insertIndex = (groupIndex + offset).coerceIn(0, papers.size)
            papers.add(insertIndex, parsedPaper)
        }
    }
}

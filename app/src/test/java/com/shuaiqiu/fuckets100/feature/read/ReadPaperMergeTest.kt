package com.shuaiqiu.fuckets100.feature.read

import com.shuaiqiu.fuckets100.ETS100AnswerReader
import org.junit.Assert.assertEquals
import org.junit.Test

class ReadPaperMergeTest {
    private fun paper(id: Long, title: String = "paper-$id") = ETS100AnswerReader.Paper(
        paperId = id,
        title = title,
        dataFileName = "f$id",
        fileSize = 0L,
        sections = emptyList(),
    )

    @Test
    fun replacesExistingPaperAtOriginalPosition() {
        val merged = mergeParsedGroup(
            currentPapers = listOf(paper(1), paper(2)),
            groupIndex = 1,
            parsedGroupPapers = listOf(paper(2, "updated")),
        )

        assertEquals(listOf(1L, 2L), merged.map { it.paperId })
        assertEquals("updated", merged[1].title)
    }

    @Test
    fun insertsNewPapersAtGroupPosition() {
        val merged = mergeParsedGroup(
            currentPapers = listOf(paper(1), paper(4)),
            groupIndex = 1,
            parsedGroupPapers = listOf(paper(2), paper(3)),
        )

        assertEquals(listOf(1L, 2L, 3L, 4L), merged.map { it.paperId })
    }

    @Test
    fun clampsInsertPositionToCurrentListSize() {
        val merged = mergeParsedGroup(
            currentPapers = listOf(paper(1)),
            groupIndex = 99,
            parsedGroupPapers = listOf(paper(2)),
        )

        assertEquals(listOf(1L, 2L), merged.map { it.paperId })
    }
}

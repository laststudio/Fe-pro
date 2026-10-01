package com.shuaiqiu.fuckets100.feature.read

import com.shuaiqiu.fuckets100.CloudHomeworkState
import com.shuaiqiu.fuckets100.ETS100AnswerReader
import com.shuaiqiu.fuckets100.ETS100ApiClient
import com.shuaiqiu.fuckets100.PaperSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReadDerivedStateTest {
    private fun homework(id: String, pk: Int = 0, setType: Int = 0) = ETS100ApiClient.HomeworkInfo(
        id = id,
        name = "hw-$id",
        contents = emptyList(),
        setType = setType,
        pk = pk,
    )

    private fun cloudPaper(id: Long) = ETS100AnswerReader.Paper(
        paperId = id,
        title = "paper-$id",
        dataFileName = "f$id",
        fileSize = 0L,
        sections = emptyList(),
        source = PaperSource.Cloud(
            status = CloudHomeworkState.STATUS_CURRENT,
            homeworkIdentity = "hw",
            baseUrl = "https://cdn.example.com",
            contents = listOf(com.shuaiqiu.fuckets100.CloudContent("g", "https://cdn.example.com/a.json")),
        ),
    )

    private fun localPaper(id: Long) = ETS100AnswerReader.Paper(
        paperId = id,
        title = "local-$id",
        dataFileName = "l$id",
        fileSize = 0L,
        sections = emptyList(),
    )

    @Test
    fun currentCategoryReturnsCurrentStatusList() {
        val map = mapOf(
            CloudHomeworkState.STATUS_CURRENT to listOf(homework("a"), homework("b")),
            CloudHomeworkState.STATUS_HISTORY to listOf(homework("h")),
        )
        val result = ReadDerivedState.homeworkList(CloudHomeworkState.CATEGORY_CURRENT, map)
        assertEquals(listOf("a", "b"), result.map { it.id })
    }

    @Test
    fun vocabPkCategoryFiltersByPkOrSetTypeAndDeduplicates() {
        val map = mapOf(
            CloudHomeworkState.STATUS_CURRENT to listOf(
                homework("dup", pk = 1),
                homework("set6", setType = 6),
                homework("plain"),
            ),
            CloudHomeworkState.STATUS_HISTORY to listOf(
                homework("dup", pk = 1),
                homework("hist6", setType = 6),
            ),
        )
        val result = ReadDerivedState.homeworkList(CloudHomeworkState.CATEGORY_VOCAB_PK, map)
        assertEquals(listOf("dup", "set6", "hist6"), result.map { it.id })
    }

    @Test
    fun categoryLabelFallsBackToCurrent() {
        assertEquals("当前作业", ReadDerivedState.categoryLabel("unknown"))
        assertEquals("历史作业", ReadDerivedState.categoryLabel(CloudHomeworkState.CATEGORY_HISTORY))
        assertEquals("词汇PK", ReadDerivedState.categoryLabel(CloudHomeworkState.CATEGORY_VOCAB_PK))
    }

    @Test
    fun isCategoryLoadedReflectsPresenceInMap() {
        val map = mapOf(CloudHomeworkState.STATUS_CURRENT to emptyList<ETS100ApiClient.HomeworkInfo>())
        assertTrue(ReadDerivedState.isCategoryLoaded(CloudHomeworkState.CATEGORY_CURRENT, map))
        assertFalse(ReadDerivedState.isCategoryLoaded(CloudHomeworkState.CATEGORY_HISTORY, map))
        assertTrue(ReadDerivedState.isCategoryLoaded(CloudHomeworkState.CATEGORY_VOCAB_PK, map))
    }

    @Test
    fun downloadedCloudPapersKeepsOnlyCloudSources() {
        val map = mapOf(
            "a" to listOf(cloudPaper(1), localPaper(2)),
            "b" to listOf(cloudPaper(3)),
        )
        val result = ReadDerivedState.downloadedCloudPapers(map)
        assertEquals(listOf(1L, 3L), result.map { it.paperId })
    }

    @Test
    fun sanitizeDownloadedPapersDropsEmptyAndNonCloudEntries() {
        val map = mapOf(
            "ok" to listOf(cloudPaper(1)),
            "mixed" to listOf(cloudPaper(2), localPaper(3)),
            "empty" to emptyList(),
        )
        val (papers, names) = ReadDerivedState.sanitizeDownloadedPapers(map, setOf("ok", "mixed", "empty"))
        assertEquals(setOf("ok"), papers.keys)
        assertEquals(setOf("ok"), names)
    }
}

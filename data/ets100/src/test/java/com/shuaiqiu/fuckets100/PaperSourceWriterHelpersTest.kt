package com.shuaiqiu.fuckets100

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 锁定从 PaperSourceExporter 拆出的纯工具函数行为。
 */
class PaperSourceWriterHelpersTest {
    private fun cloudSource(vararg urls: String) = PaperSource.Cloud(
        status = "1",
        homeworkIdentity = "hw",
        baseUrl = "https://cdn.example.com",
        contents = urls.mapIndexed { index, url -> CloudContent("g$index", url) },
    )

    @Test
    fun cloudFileNameStripsQueryAndPath() {
        assertEquals("paper.zip", ets100CloudFileName("https://cdn.example.com/a/b/paper.zip?token=1"))
        assertEquals("paper.zip", ets100CloudFileName("/a/paper.zip"))
    }

    @Test
    fun cloudFileNameRejectsUnsafeSegments() {
        assertEquals("", ets100CloudFileName("https://cdn.example.com/a/.."))
        assertEquals("", ets100CloudFileName("https://cdn.example.com/a/"))
    }

    @Test
    fun cloudFileNameCollisionsDetectDuplicateNames() {
        val collisions = ets100CloudFileNameCollisions(
            cloudSource("https://a/x/paper.zip", "https://b/y/paper.zip"),
        )
        assertEquals(setOf("paper.zip"), collisions)

        val unique = ets100CloudFileNameCollisions(
            cloudSource("https://a/x/one.zip", "https://b/y/two.zip"),
        )
        assertTrue(unique.isEmpty())
    }

    @Test
    fun cloudFileNameCollisionsIgnoreIdenticalUrls() {
        val collisions = ets100CloudFileNameCollisions(
            cloudSource("https://a/x/paper.zip", "https://a/x/paper.zip"),
        )
        assertTrue(collisions.isEmpty())
    }

    @Test
    fun uniqueDataEntryNamePrefixesOnlyConflictingNames() {
        assertEquals("data_content.json", ets100UniqueDataEntryName("content.json", setOf("content.json")))
        assertEquals("other.json", ets100UniqueDataEntryName("other.json", setOf("content.json")))
    }

    @Test
    fun isSafeNameRejectsTraversalAndSeparators() {
        assertTrue(ets100IsSafeName("paper"))
        assertTrue(ets100IsSafeName("试卷 1"))
        assertFalse(ets100IsSafeName(""))
        assertFalse(ets100IsSafeName("   "))
        assertFalse(ets100IsSafeName("."))
        assertFalse(ets100IsSafeName(".."))
        assertFalse(ets100IsSafeName("a/b"))
        assertFalse(ets100IsSafeName("a\\b"))
    }

    @Test
    fun isSafeZipEntryRejectsAbsoluteAndTraversalPaths() {
        assertTrue(ets100IsSafeZipEntry("resource/paper/content.json"))
        assertFalse(ets100IsSafeZipEntry(""))
        assertFalse(ets100IsSafeZipEntry("/resource/content.json"))
        assertFalse(ets100IsSafeZipEntry("resource/../../etc/passwd"))
    }

    @Test
    fun sha256MatchesKnownDigest() {
        assertEquals(
            "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
            ets100Sha256("abc"),
        )
    }
}

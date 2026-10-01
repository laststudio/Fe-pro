package com.shuaiqiu.fuckets100

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 锁定从 ETS100FileReader 拆出的 shell 构造与输出解析工具行为。
 */
class ETS100FileReaderShellTest {
    @Test
    fun shellQuoteWrapsValueAndEscapesSingleQuotes() {
        assertEquals("'plain'", shellQuote("plain"))
        assertEquals("'a'\"'\"'b'", shellQuote("a'b"))
    }

    @Test
    fun builtCommandsEmbedQuotedPathSoSpacesAndQuotesStayLiteral() {
        val path = "/sdcard/app data/it's"
        val quoted = shellQuote(path)

        assertTrue(buildFastListCommand(path).contains(quoted))
        assertTrue(buildFastContentFolderCommand(path).contains(quoted))
        assertTrue(buildDirectorySizeCommand(path).contains(quoted))
        assertFalse(buildFastListCommand(path).contains("dir=$path"))
    }

    @Test
    fun parseFastListOutputParsesDirectoryAndFileEntries() {
        val output = """
            d	0	1700000000	2026-01-01 00:00:00.000000000 +0800	folder
            -	2048	1700000001	2026-01-01 00:00:01.000000000 +0800	content.json
        """.trimIndent()

        val items = parseFastListOutput(output)

        assertEquals(2, items.size)
        assertTrue(items[0].isDirectory)
        assertEquals("folder", items[0].name)
        assertFalse(items[1].isDirectory)
        assertEquals("content.json", items[1].name)
        assertEquals(2048L, items[1].size)
    }

    @Test
    fun parseFastListOutputSkipsIncompleteAndDotEntries() {
        val output = """
            d	0	1700000000	2026-01-01 00:00:00.000000000 +0800	.
            d	0	1700000000	2026-01-01 00:00:00.000000000 +0800	..
            -	1
            -	4096	1700000002	2026-01-01 00:00:02.000000000 +0800	real.json
        """.trimIndent()

        val items = parseFastListOutput(output)

        assertEquals(1, items.size)
        assertEquals("real.json", items[0].name)
    }

    @Test
    fun parseShellStatTimeMillisUsesSecondsWhenNoFullTimestamp() {
        assertEquals(1_700_000_000_000L, parseShellStatTimeMillis("1700000000", null))
        assertEquals(0L, parseShellStatTimeMillis("not-a-number", null))
    }

    @Test
    fun parseShellStatTimeMillisAddsFractionAndHonoursTimezone() {
        val utc = parseShellStatTimeMillis("0", "2026-01-01 00:00:00.250 +0000")
        val plusEight = parseShellStatTimeMillis("0", "2026-01-01 00:00:00.250 +0800")

        assertEquals(250L, utc % 1000L)
        assertEquals(250L, plusEight % 1000L)
        assertEquals(8 * 60 * 60 * 1000L, utc - plusEight)
    }

    @Test
    fun parseShellStatTimeMillisFallsBackOnUnparseableTimestamp() {
        assertEquals(1_700_000_000_000L, parseShellStatTimeMillis("1700000000", "not a timestamp"))
    }
}

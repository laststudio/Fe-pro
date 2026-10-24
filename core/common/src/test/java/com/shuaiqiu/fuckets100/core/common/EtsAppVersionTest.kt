package com.shuaiqiu.fuckets100.core.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EtsAppVersionTest {
    @Test
    fun `8 series versions are detected`() {
        assertTrue(EtsAppVersion("8.0.0.2601").isEightSeries)
        assertTrue(EtsAppVersion("8.x").isEightSeries)
    }

    @Test
    fun `7 series versions are not detected as 8 series`() {
        assertFalse(EtsAppVersion("7.0.71.2569").isEightSeries)
        assertFalse(EtsAppVersion("10.0.0").isEightSeries)
    }

    @Test
    fun `major version parser handles whitespace and invalid input`() {
        assertEquals(8, EtsAppVersion.parseMajorVersion("  8.0.0.2601"))
        assertEquals(7, EtsAppVersion.parseMajorVersion("7.0.71.2569"))
        assertEquals(null, EtsAppVersion.parseMajorVersion("unknown"))
        assertEquals(null, EtsAppVersion.parseMajorVersion(""))
    }
}

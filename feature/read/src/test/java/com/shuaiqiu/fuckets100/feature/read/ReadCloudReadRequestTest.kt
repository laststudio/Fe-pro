package com.shuaiqiu.fuckets100.feature.read

import org.junit.Assert.assertEquals
import org.junit.Test

class ReadCloudReadRequestTest {
    @Test
    fun disabledSkipSettingShowsWarningWithoutReading() {
        var warnings = 0
        var reads = 0

        requestCloudReadWithWarning(
            skipWarning = false,
            onWarningRequired = { warnings++ },
            onRead = { reads++ },
        )

        assertEquals(1, warnings)
        assertEquals(0, reads)
    }

    @Test
    fun enabledSkipSettingReadsWithoutWarning() {
        var warnings = 0
        var reads = 0

        requestCloudReadWithWarning(
            skipWarning = true,
            onWarningRequired = { warnings++ },
            onRead = { reads++ },
        )

        assertEquals(0, warnings)
        assertEquals(1, reads)
    }
}

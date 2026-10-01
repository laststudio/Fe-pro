package com.shuaiqiu.fuckets100.feature.read

import com.shuaiqiu.fuckets100.CloudHomeworkState
import org.junit.Assert.assertEquals
import org.junit.Test

class ReadCloudMappersTest {
    @Test
    fun historyCategoryMapsToHistoryStatus() {
        assertEquals(
            CloudHomeworkState.STATUS_HISTORY,
            cloudStatusForCategory(CloudHomeworkState.CATEGORY_HISTORY),
        )
    }

    @Test
    fun currentAndVocabCategoriesMapToCurrentStatus() {
        assertEquals(
            CloudHomeworkState.STATUS_CURRENT,
            cloudStatusForCategory(CloudHomeworkState.CATEGORY_CURRENT),
        )
        assertEquals(
            CloudHomeworkState.STATUS_CURRENT,
            cloudStatusForCategory(CloudHomeworkState.CATEGORY_VOCAB_PK),
        )
    }

    @Test
    fun statusToCategoryRoundTripsForKnownValues() {
        assertEquals(
            CloudHomeworkState.CATEGORY_HISTORY,
            cloudCategoryForStatus(CloudHomeworkState.STATUS_HISTORY),
        )
        assertEquals(
            CloudHomeworkState.CATEGORY_CURRENT,
            cloudCategoryForStatus(CloudHomeworkState.STATUS_CURRENT),
        )
        assertEquals(
            CloudHomeworkState.CATEGORY_CURRENT,
            cloudCategoryForStatus(CloudHomeworkState.STATUS_EXPIRED),
        )
    }
}

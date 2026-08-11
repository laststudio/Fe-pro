package com.shuaiqiu.fuckets100.navigation

import com.shuaiqiu.fuckets100.PaperStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FeNavigatorTest {
    @Test
    fun popUntilKeepsRootMain() {
        val navigator = FeNavigator(FeRoute.Main)
        navigator.push(FeRoute.Activation)
        navigator.push(FeRoute.Donate)

        navigator.popUntil { it is FeRoute.Main }

        assertEquals(FeRoute.Main, navigator.current())
    }

    @Test
    fun popRemovesAnswerPaperFromStore() {
        val key = PaperStore.put(
            com.shuaiqiu.fuckets100.ETS100AnswerReader.Paper(
                paperId = 1L,
                title = "测试",
                dataFileName = "test",
                fileSize = 0L,
                sections = emptyList(),
            ),
        )
        val navigator = FeNavigator(FeRoute.Main)
        navigator.push(FeRoute.Answer(key))

        navigator.pop()

        assertEquals(FeRoute.Main, navigator.current())
        assertNull(PaperStore.get(key))
        PaperStore.remove(key)
    }
}

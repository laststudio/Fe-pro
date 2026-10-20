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
    fun accountSubpageBackReturnsToAccount() {
        val navigator = FeNavigator(FeRoute.Main)
        navigator.push(FeRoute.Account)
        navigator.push(FeRoute.AccountDevices)

        navigator.pop()

        assertEquals(FeRoute.Account, navigator.current())
    }

    @Test
    fun accountFormsBackReturnToAccount() {
        listOf(
            FeRoute.AccountBindings(false),
            FeRoute.AccountBindings(true),
            FeRoute.AccountChangePassword,
            FeRoute.AccountSessions,
        ).forEach { route ->
            val navigator = FeNavigator(FeRoute.Main)
            navigator.push(FeRoute.Account)
            navigator.push(route)
            navigator.pop()
            assertEquals(FeRoute.Account, navigator.current())
        }
    }

    @Test
    fun passwordChangeNavigationCanReturnToRoot() {
        val navigator = FeNavigator(FeRoute.Main)
        navigator.push(FeRoute.Account)
        navigator.push(FeRoute.AccountChangePassword)
        navigator.popUntil { it is FeRoute.Main }
        assertEquals(FeRoute.Main, navigator.current())
    }

    @Test
    fun legalBackReturnsToAbout() {
        val navigator = FeNavigator(FeRoute.Main)
        navigator.push(FeRoute.About)
        navigator.push(FeRoute.Legal)

        navigator.pop()

        assertEquals(FeRoute.About, navigator.current())
    }

    @Test
    fun donateBackReturnsToAbout() {
        val navigator = FeNavigator(FeRoute.Main)
        navigator.push(FeRoute.About)
        navigator.push(FeRoute.Donate)

        navigator.pop()

        assertEquals(FeRoute.About, navigator.current())
    }

    @Test
    fun popInvokesRouteRemovalCallback() {
        val navigator = FeNavigator(FeRoute.Main)
        var removed: FeRoute? = null
        // 宿主（MainActivity）负责在该回调里释放 PaperStore 资源。
        navigator.onRouteRemoved = { removed = it }
        navigator.push(FeRoute.Answer("paper-key"))

        navigator.pop()

        assertEquals(FeRoute.Main, navigator.current())
        assertEquals(FeRoute.Answer("paper-key"), removed)
    }

    @Test
    fun popUntilInvokesRouteRemovalCallbackForEachRemovedRoute() {
        val navigator = FeNavigator(FeRoute.Main)
        val removed = mutableListOf<FeRoute>()
        navigator.onRouteRemoved = { removed += it }
        navigator.push(FeRoute.Answer("k1"))
        navigator.push(FeRoute.Answer("k2"))

        navigator.popUntil { it is FeRoute.Main }

        assertEquals(FeRoute.Main, navigator.current())
        assertEquals(
            listOf(FeRoute.Answer("k2"), FeRoute.Answer("k1")),
            removed,
        )
    }

    @Test
    fun wiringCallbackToPaperStoreReleasesPaperOnPop() {
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
        navigator.onRouteRemoved = { route ->
            if (route is FeRoute.Answer) PaperStore.remove(route.paperKey)
        }
        navigator.push(FeRoute.Answer(key))

        navigator.pop()

        assertEquals(FeRoute.Main, navigator.current())
        assertNull(PaperStore.get(key))
        PaperStore.remove(key)
    }
}

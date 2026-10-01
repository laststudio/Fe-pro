package com.shuaiqiu.fuckets100.navigation

import androidx.compose.runtime.saveable.SaverScope
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FeNavigatorCoreTest {
    @Test
    fun pushThenPopReturnsToPreviousRoute() {
        val navigator = FeNavigator(FeRoute.Main)
        navigator.push(FeRoute.About)
        navigator.push(FeRoute.Legal)

        navigator.pop()

        assertEquals(FeRoute.About, navigator.current())
    }

    @Test
    fun pushSameRouteTwiceIsIgnored() {
        val navigator = FeNavigator(FeRoute.Main)
        navigator.push(FeRoute.About)
        navigator.push(FeRoute.About)

        assertEquals(2, navigator.backStack.size)
    }

    @Test
    fun popAtRootKeepsRoot() {
        val navigator = FeNavigator(FeRoute.Main)

        navigator.pop()

        assertEquals(FeRoute.Main, navigator.current())
    }

    @Test
    fun removedRouteCallbackReceivesPoppedRoute() {
        val navigator = FeNavigator(FeRoute.Main)
        var removed: FeRoute? = null
        navigator.onRouteRemoved = { removed = it }
        navigator.push(FeRoute.Answer("k"))

        navigator.pop()

        assertEquals(FeRoute.Answer("k"), removed)
    }

    @Test
    fun popUntilStopsAtMatchingRoute() {
        val navigator = FeNavigator(FeRoute.Main)
        navigator.push(FeRoute.Account)
        navigator.push(FeRoute.AccountDevices)
        navigator.push(FeRoute.AccountSessions)

        navigator.popUntil { it is FeRoute.Account }

        assertEquals(FeRoute.Account, navigator.current())
    }

    @Test
    fun saverPersistsHierarchyAndDropsAnswerRoutes() {
        val navigator = FeNavigator(FeRoute.Main)
        navigator.push(FeRoute.About)
        navigator.push(FeRoute.Answer("transient-paper"))
        navigator.push(FeRoute.Legal)

        val scope = SaverScope { true }
        val saved = with(FeNavigator.Saver) { scope.save(navigator) }
        val restored = saved?.let { FeNavigator.Saver.restore(it) }

        assertNotNull(saved)
        assertNotNull(restored)
        assertEquals(
            listOf(FeRoute.Main, FeRoute.About, FeRoute.Legal),
            restored!!.backStack.toList(),
        )
    }

    @Test
    fun saverRestoreFallsBackToMainWhenNothingSaved() {
        val restored = FeNavigator.Saver.restore(emptyList<FeRoute>())

        assertNotNull(restored)
        assertEquals(FeRoute.Main, restored!!.current())
    }

    @Test
    fun restoredNavigatorKeepsWorkingBackStackSemantics() {
        val navigator = FeNavigator(FeRoute.Main)
        navigator.push(FeRoute.About)
        navigator.push(FeRoute.Donate)

        val scope = SaverScope { true }
        val restored = with(FeNavigator.Saver) { scope.save(navigator) }
            ?.let { FeNavigator.Saver.restore(it) }!!

        // popUntil 不受 pop() 的 100ms 去抖影响，用于验证恢复后的整条返回栈。
        restored.popUntil { it is FeRoute.Main }

        assertEquals(FeRoute.Main, restored.current())
        assertEquals(1, restored.backStack.size)
    }

    @Test
    fun popIsDebouncedWithinThrottleWindow() {
        val navigator = FeNavigator(FeRoute.Main)
        navigator.push(FeRoute.About)
        navigator.push(FeRoute.Donate)

        // 两次连续 pop 间隔小于 100ms：第二次被去抖丢弃，防止双击返回弹出两级。
        navigator.pop()
        navigator.pop()

        assertEquals(FeRoute.About, navigator.current())
    }
}

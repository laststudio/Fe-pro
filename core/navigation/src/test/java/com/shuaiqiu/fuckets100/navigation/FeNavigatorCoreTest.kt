package com.shuaiqiu.fuckets100.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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

}

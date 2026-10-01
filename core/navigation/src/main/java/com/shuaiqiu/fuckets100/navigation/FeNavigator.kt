package com.shuaiqiu.fuckets100.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Simple navigation helper that owns a back stack.
 */
class FeNavigator(initialRoute: FeRoute) {
    val backStack: SnapshotStateList<FeRoute> = mutableStateListOf(initialRoute)

    /**
     * 由宿主在弹出路由时清理与该路由关联的资源（例如答题页的 Paper）。
     * 放在这里而不是直接依赖具体存储，避免 navigation 层反向依赖业务层。
     */
    var onRouteRemoved: ((FeRoute) -> Unit)? = null

    /**
     * Push a key onto the back stack.
     */
    fun push(route: FeRoute) {
        if (backStack.lastOrNull() == route) {
            return
        }

        backStack.add(route)
    }

    private var lastPopTime = 0L

    /**
     * Pop the top key if present.
     */
    fun pop() {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastPopTime < 100) {
            return
        }
        lastPopTime = currentTime

        if (backStack.size <= 1) return
        val removed = backStack.removeLastOrNull()
        removed?.let { onRouteRemoved?.invoke(it) }
    }

    /**
     * Pop until predicate matches the top key.
     */
    fun popUntil(predicate: (FeRoute) -> Boolean) {
        while (backStack.isNotEmpty() && !predicate(backStack.last())) {
            val removed = backStack.removeAt(backStack.lastIndex)
            onRouteRemoved?.invoke(removed)
        }
    }

    /**
     * Get current NavKey on the back stack.
     */
    fun current(): FeRoute? {
        return backStack.lastOrNull()
    }

    companion object {
        val Saver: Saver<FeNavigator, Any> = listSaver(
            save = { it.backStack.filterNot { route -> route is FeRoute.Answer } },
            restore = { saved ->
                // 恢复后的返回栈必须至少含有一个路由，否则 current() 为 null 会导致页面空白。
                val routes = saved.ifEmpty { listOf(FeRoute.Main) }
                FeNavigator(routes.first()).also { navigator ->
                    navigator.backStack.clear()
                    navigator.backStack.addAll(routes)
                }
            },
        )
    }
}

@Composable
fun rememberFeNavigator(startRoute: FeRoute): FeNavigator {
    return rememberSaveable(startRoute, saver = FeNavigator.Saver) {
        FeNavigator(startRoute)
    }
}

val LocalFeNavigator = staticCompositionLocalOf<FeNavigator> {
    error("LocalFeNavigator not provided")
}

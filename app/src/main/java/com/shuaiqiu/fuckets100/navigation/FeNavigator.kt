package com.shuaiqiu.fuckets100.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.staticCompositionLocalOf
import com.shuaiqiu.fuckets100.PaperStore

/**
 * Simple navigation helper that owns a back stack.
 */
class FeNavigator(initialRoute: FeRoute) {
    val backStack: SnapshotStateList<FeRoute> = mutableStateListOf(initialRoute)

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
        (removed as? FeRoute.Answer)?.let { PaperStore.remove(it.paperKey) }
    }

    /**
     * Pop until predicate matches the top key.
     */
    fun popUntil(predicate: (FeRoute) -> Boolean) {
        while (backStack.isNotEmpty() && !predicate(backStack.last())) {
            val removed = backStack.removeAt(backStack.lastIndex)
            (removed as? FeRoute.Answer)?.let { PaperStore.remove(it.paperKey) }
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
                FeNavigator(saved.firstOrNull() ?: FeRoute.Main).also { navigator ->
                    navigator.backStack.clear()
                    navigator.backStack.addAll(saved)
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

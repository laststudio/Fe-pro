package com.shuaiqiu.fuckets100.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshots.SnapshotStateList

class FeNavigator(initialRoute: FeRoute) {
    val backStack: SnapshotStateList<FeRoute> = mutableStateListOf(initialRoute)

    fun push(route: FeRoute) {
        if (backStack.lastOrNull() != route) backStack.add(route)
    }

    fun pop() {
        if (backStack.size > 1) backStack.removeLastOrNull()
    }

    fun popUntil(predicate: (FeRoute) -> Boolean) {
        while (backStack.size > 1 && !predicate(backStack.last())) {
            backStack.removeAt(backStack.lastIndex)
        }
    }

    fun current(): FeRoute? = backStack.lastOrNull()

    companion object {
        val Saver: Saver<FeNavigator, Any> = listSaver(
            save = { it.backStack.toList() },
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
fun rememberFeNavigator(startRoute: FeRoute): FeNavigator = rememberSaveable(
    startRoute,
    saver = FeNavigator.Saver,
) {
    FeNavigator(startRoute)
}

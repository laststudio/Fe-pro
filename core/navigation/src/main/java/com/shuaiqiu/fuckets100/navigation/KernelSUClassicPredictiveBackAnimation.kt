package com.shuaiqiu.fuckets100.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation3.scene.Scene
import androidx.navigationevent.NavigationEventTransitionState

class KernelSUClassicPredictiveBackAnimation : PredictiveBackAnimationHandler {
    override suspend fun onBackPressed(
        transitionState: NavigationEventTransitionState?,
        currentPageKey: FeRoute?,
    ) {
        // ignore
    }

    @Composable
    override fun Modifier.predictiveBackAnimationDecorator(
        transitionState: NavigationEventTransitionState?,
        contentPageKey: Any,
        currentPageKey: FeRoute?,
    ): Modifier {
        return this
    }

    override fun AnimatedContentTransitionScope<Scene<FeRoute>>.onPredictivePopTransitionSpec(
        swipeEdge: Int
    ): ContentTransform =
        ContentTransform(
            targetContentEnter = slideInHorizontally(initialOffsetX = { fullWidth -> -fullWidth }),
            initialContentExit = scaleOut(targetScale = 0.9f) + fadeOut(),
            sizeTransform = null
        )

    override fun AnimatedContentTransitionScope<Scene<FeRoute>>.onPopTransitionSpec(): ContentTransform =
        ContentTransform(
            targetContentEnter = slideInHorizontally(initialOffsetX = { fullWidth -> -fullWidth }),
            initialContentExit = scaleOut(targetScale = 0.9f) + fadeOut(),
            sizeTransform = null
        )

    override fun AnimatedContentTransitionScope<Scene<FeRoute>>.onTransitionSpec(): ContentTransform =
        ContentTransform(
            targetContentEnter = slideInHorizontally(initialOffsetX = { fullWidth -> fullWidth }),
            initialContentExit = slideOutHorizontally(targetOffsetX = { fullWidth -> -fullWidth }),
            sizeTransform = null
        )
}

package com.shuaiqiu.fuckets100.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation3.scene.Scene
import androidx.navigation3.ui.defaultPredictivePopTransitionSpec
import androidx.navigationevent.NavigationEventTransitionState

class MiuixPredictiveBackAnimation : PredictiveBackAnimationHandler {

    override suspend fun onBackPressed(
        transitionState: NavigationEventTransitionState?,
        currentPageKey: FeRoute?,
    ) {
        // Deliberately empty. Predictive back gesture progress is natively handled
        // and synchronized by the Compose transition engine.
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
    ): ContentTransform = defaultPredictivePopTransitionSpec<FeRoute>().invoke(this, swipeEdge)

    override fun AnimatedContentTransitionScope<Scene<FeRoute>>.onPopTransitionSpec(): ContentTransform =
        ContentTransform(
            // Keep the previous page stable underneath the returning page. This is the
            // same pop treatment used by ReSukiSU's scale animation and avoids a one-frame
            // jump when the target scene is restored.
            targetContentEnter = slideInHorizontally(initialOffsetX = { -it / 4 }) + fadeIn(),
            initialContentExit = scaleOut(targetScale = 0.9f) + fadeOut(),
            sizeTransform = null,
        )

    override fun AnimatedContentTransitionScope<Scene<FeRoute>>.onTransitionSpec(): ContentTransform =
        ContentTransform(
            // ReSukiSU's scale animation deliberately fades the old scene instead of
            // sliding it away. Applying the same entry treatment here prevents the main
            // page from visibly teleporting during Activation/Theme navigation.
            targetContentEnter = slideInHorizontally(initialOffsetX = { it }),
            initialContentExit = fadeOut(),
            sizeTransform = null,
        )
}

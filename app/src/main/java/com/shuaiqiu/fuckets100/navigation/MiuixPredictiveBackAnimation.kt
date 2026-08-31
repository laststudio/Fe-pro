package com.shuaiqiu.fuckets100.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation3.scene.Scene
import androidx.navigation3.ui.defaultPopTransitionSpec
import androidx.navigation3.ui.defaultPredictivePopTransitionSpec
import androidx.navigation3.ui.defaultTransitionSpec
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
        defaultPopTransitionSpec<FeRoute>().invoke(this)

    override fun AnimatedContentTransitionScope<Scene<FeRoute>>.onTransitionSpec(): ContentTransform =
        defaultTransitionSpec<FeRoute>().invoke(this)
}

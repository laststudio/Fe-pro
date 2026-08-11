package com.shuaiqiu.fuckets100.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.dp
import androidx.navigation3.scene.Scene
import androidx.navigation3.ui.defaultPopTransitionSpec
import androidx.navigation3.ui.defaultPredictivePopTransitionSpec
import androidx.navigation3.ui.defaultTransitionSpec
import androidx.navigationevent.NavigationEvent.Companion.EDGE_LEFT
import androidx.navigationevent.NavigationEventTransitionState
import androidx.navigationevent.NavigationEventTransitionState.InProgress
import com.shuaiqiu.fuckets100.rememberDeviceCornerRadius
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

interface FePredictiveBackAnimation {
    suspend fun onBackPressed(
        transitionState: NavigationEventTransitionState?,
        currentPageKey: FeRoute?,
    )

    fun onPagePop(contentPageKey: Any, animationScope: CoroutineScope) = Unit

    @Composable
    fun Modifier.decoratePage(
        transitionState: NavigationEventTransitionState?,
        contentPageKey: Any,
        currentPageKey: FeRoute?,
    ): Modifier

    fun AnimatedContentTransitionScope<Scene<FeRoute>>.predictivePop(swipeEdge: Int): ContentTransform
    fun AnimatedContentTransitionScope<Scene<FeRoute>>.pop(): ContentTransform
    fun AnimatedContentTransitionScope<Scene<FeRoute>>.push(): ContentTransform
}

class FeAospPredictiveBackAnimation : FePredictiveBackAnimation {
    private var exitingPageKey: String? = null
    private val exitAnimatable = Animatable(0f)

    override suspend fun onBackPressed(
        transitionState: NavigationEventTransitionState?,
        currentPageKey: FeRoute?,
    ) {
        exitingPageKey = currentPageKey.toString()
        exitAnimatable.animateTo(1f, tween(durationMillis = 150, easing = LinearEasing))
    }

    override fun onPagePop(contentPageKey: Any, animationScope: CoroutineScope) {
        if (exitingPageKey == contentPageKey.toString()) {
            exitingPageKey = null
            animationScope.launch { exitAnimatable.snapTo(0f) }
        }
    }

    @Composable
    override fun Modifier.decoratePage(
        transitionState: NavigationEventTransitionState?,
        contentPageKey: Any,
        currentPageKey: FeRoute?,
    ): Modifier {
        val windowInfo = LocalWindowInfo.current
        val density = LocalDensity.current
        val deviceCornerRadius = rememberDeviceCornerRadius()
        val pageKey = contentPageKey.toString()
        val progressState = transitionState as? InProgress
        val edge = progressState?.latestEvent?.swipeEdge ?: EDGE_LEFT
        val touchY = progressState?.latestEvent?.touchY
        val gestureProgress = progressState?.latestEvent?.progress ?: 0f
        val linearProgress = exitAnimatable.value
        val emphasizedProgress = CubicBezierEasing(0.2f, 0f, 0f, 1f).transform(linearProgress)
        val direction = if (edge == EDGE_LEFT) 1f else -1f
        val dragScale = 1f - 0.15f * gestureProgress
        val pivotY = if (touchY != null && windowInfo.containerSize.height > 0) {
            (touchY / windowInfo.containerSize.height).coerceIn(0.1f, 0.9f)
        } else {
            0.5f
        }
        val pivotX = if (edge == EDGE_LEFT) 0.8f else 0.2f
        val exiting = exitingPageKey == pageKey
        val current = exitingPageKey == null && pageKey == currentPageKey.toString()
        val exitOffset = with(density) { 96.dp.toPx() }

        return graphicsLayer {
            if (progressState != null) transformOrigin = TransformOrigin(pivotX, pivotY)
            when {
                exiting -> {
                    val scale = dragScale + (0.85f - dragScale) * emphasizedProgress
                    scaleX = scale
                    scaleY = scale
                    translationX = exitOffset * direction * emphasizedProgress
                    alpha = if (linearProgress >= 0.2f) 0f else (1f - linearProgress * 5f).coerceAtLeast(0f)
                }
                current -> {
                    scaleX = dragScale
                    scaleY = dragScale
                }
                else -> {
                    val initialOffset = -exitOffset * direction
                    if (exitingPageKey != null) {
                        scaleX = dragScale + (1f - dragScale) * emphasizedProgress
                        scaleY = dragScale + (1f - dragScale) * emphasizedProgress
                        translationX = initialOffset * (1f - emphasizedProgress)
                    } else if (progressState != null) {
                        scaleX = dragScale
                        scaleY = dragScale
                        translationX = initialOffset
                    }
                }
            }
        }.clip(
            if (exiting || current) RoundedCornerShape(deviceCornerRadius)
            else RoundedCornerShape(0.dp),
        )
    }

    override fun AnimatedContentTransitionScope<Scene<FeRoute>>.predictivePop(swipeEdge: Int) =
        ContentTransform(EnterTransition.None, ExitTransition.None, sizeTransform = null)

    override fun AnimatedContentTransitionScope<Scene<FeRoute>>.pop() = ContentTransform(
        targetContentEnter = slideInHorizontally { -it / 4 },
        initialContentExit = scaleOut(targetScale = 0.9f) + fadeOut(),
        sizeTransform = null,
    )

    override fun AnimatedContentTransitionScope<Scene<FeRoute>>.push() = ContentTransform(
        targetContentEnter = slideInHorizontally { it },
        initialContentExit = ExitTransition.None,
        sizeTransform = null,
    )
}

class FeKernelSuPredictiveBackAnimation : FePredictiveBackAnimation {
    override suspend fun onBackPressed(transitionState: NavigationEventTransitionState?, currentPageKey: FeRoute?) = Unit

    @Composable
    override fun Modifier.decoratePage(
        transitionState: NavigationEventTransitionState?,
        contentPageKey: Any,
        currentPageKey: FeRoute?,
    ) = this

    override fun AnimatedContentTransitionScope<Scene<FeRoute>>.predictivePop(swipeEdge: Int) = pop()

    override fun AnimatedContentTransitionScope<Scene<FeRoute>>.pop() = ContentTransform(
        targetContentEnter = slideInHorizontally { -it },
        initialContentExit = scaleOut(targetScale = 0.9f) + fadeOut(),
        sizeTransform = null,
    )

    override fun AnimatedContentTransitionScope<Scene<FeRoute>>.push() = ContentTransform(
        targetContentEnter = slideInHorizontally { it },
        initialContentExit = slideOutHorizontally { -it },
        sizeTransform = null,
    )
}

class FeDefaultPredictiveBackAnimation : FePredictiveBackAnimation {
    override suspend fun onBackPressed(transitionState: NavigationEventTransitionState?, currentPageKey: FeRoute?) = Unit

    @Composable
    override fun Modifier.decoratePage(
        transitionState: NavigationEventTransitionState?,
        contentPageKey: Any,
        currentPageKey: FeRoute?,
    ) = this

    override fun AnimatedContentTransitionScope<Scene<FeRoute>>.predictivePop(swipeEdge: Int) =
        defaultPredictivePopTransitionSpec<FeRoute>().invoke(this, swipeEdge)

    override fun AnimatedContentTransitionScope<Scene<FeRoute>>.pop() =
        defaultPopTransitionSpec<FeRoute>().invoke(this)

    override fun AnimatedContentTransitionScope<Scene<FeRoute>>.push() =
        defaultTransitionSpec<FeRoute>().invoke(this)
}

class FeNoPredictiveBackAnimation : FePredictiveBackAnimation {
    override suspend fun onBackPressed(transitionState: NavigationEventTransitionState?, currentPageKey: FeRoute?) = Unit

    @Composable
    override fun Modifier.decoratePage(
        transitionState: NavigationEventTransitionState?,
        contentPageKey: Any,
        currentPageKey: FeRoute?,
    ) = this

    override fun AnimatedContentTransitionScope<Scene<FeRoute>>.predictivePop(swipeEdge: Int) =
        ContentTransform(EnterTransition.None, ExitTransition.None, sizeTransform = null)

    override fun AnimatedContentTransitionScope<Scene<FeRoute>>.pop() =
        ContentTransform(EnterTransition.None, ExitTransition.None, sizeTransform = null)

    override fun AnimatedContentTransitionScope<Scene<FeRoute>>.push() =
        ContentTransform(EnterTransition.None, ExitTransition.None, sizeTransform = null)
}

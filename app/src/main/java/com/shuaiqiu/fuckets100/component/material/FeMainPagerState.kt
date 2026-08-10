package com.shuaiqiu.fuckets100.component.material

import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlin.math.abs

class FeMainPagerState(
    val pagerState: PagerState,
    private val coroutineScope: CoroutineScope,
) {
    var selectedPage by mutableIntStateOf(pagerState.currentPage)
        private set

    private var isNavigating by mutableStateOf(false)
    private var navigationJob: Job? = null

    fun animateToPage(targetPage: Int) {
        if (targetPage == selectedPage || targetPage !in 0 until pagerState.pageCount) return

        navigationJob?.cancel()
        selectedPage = targetPage
        isNavigating = true

        val distance = abs(targetPage - pagerState.currentPage).coerceAtLeast(2)
        val duration = 100 * distance + 100
        val pageSize = pagerState.layoutInfo.pageSize + pagerState.layoutInfo.pageSpacing
        val distanceInPages = targetPage - pagerState.currentPage - pagerState.currentPageOffsetFraction

        navigationJob = coroutineScope.launch {
            val currentJob = coroutineContext.job
            try {
                pagerState.animateScrollBy(
                    value = distanceInPages * pageSize,
                    animationSpec = tween(durationMillis = duration, easing = EaseInOut),
                )
            } finally {
                if (navigationJob == currentJob) {
                    isNavigating = false
                    selectedPage = pagerState.currentPage
                }
            }
        }
    }

    fun syncPage() {
        if (!isNavigating && selectedPage != pagerState.currentPage) {
            selectedPage = pagerState.currentPage
        }
    }
}

@Composable
fun rememberFeMainPagerState(
    pagerState: PagerState,
    coroutineScope: CoroutineScope = rememberCoroutineScope(),
): FeMainPagerState = remember(pagerState, coroutineScope) {
    FeMainPagerState(pagerState, coroutineScope)
}

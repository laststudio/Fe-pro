package com.shuaiqiu.fuckets100.core.designsystem

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

class FeMainPagerState(
    val pagerState: PagerState,
    private val coroutineScope: CoroutineScope,
) {
    var selectedPage by mutableIntStateOf(pagerState.currentPage)
        private set

    var userScrollEnabled by mutableStateOf(true)
        private set

    private var animating by mutableStateOf(false)
    private var animateJob: Job? = null
    private var lastRequestedPage: Int = pagerState.currentPage

    fun handlePageChange(targetPage: Int) {
        if (targetPage !in 0 until pagerState.pageCount) return
        selectedPage = targetPage

        if (targetPage == pagerState.currentPage) {
            if (animateJob != null && lastRequestedPage != targetPage) {
                animateJob?.cancel()
                animateJob = null
                animating = false
                userScrollEnabled = true
            }
            lastRequestedPage = targetPage
        } else {
            if (animateJob != null && lastRequestedPage == targetPage) {
                // Already animating to the requested page
            } else {
                animateJob?.cancel()
                animating = true
                userScrollEnabled = false
                val job = coroutineScope.launch {
                    try {
                        pagerState.animateScrollToPage(targetPage)
                    } finally {
                        if (animateJob === this) {
                            userScrollEnabled = true
                            animating = false
                            animateJob = null
                        }
                    }
                }
                animateJob = job
                lastRequestedPage = targetPage
            }
        }
    }

    fun syncPage() {
        if (!animating) {
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

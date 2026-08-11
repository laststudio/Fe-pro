package com.shuaiqiu.fuckets100.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberDecoratedNavEntries
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.scene.SceneInfo
import androidx.navigation3.scene.SinglePaneSceneStrategy
import androidx.navigation3.scene.rememberSceneState
import androidx.navigation3.ui.NavDisplay
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.NavigationEventState
import androidx.navigationevent.compose.rememberNavigationEventState
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec
import com.shuaiqiu.fuckets100.ActivationMode
import com.shuaiqiu.fuckets100.ActivationSettingsScreen
import com.shuaiqiu.fuckets100.CloudActivationScreen
import com.shuaiqiu.fuckets100.DonateScreen
import com.shuaiqiu.fuckets100.GeneralSettingsScreen
import com.shuaiqiu.fuckets100.LegalScreen
import com.shuaiqiu.fuckets100.PredictiveBackMode
import com.shuaiqiu.fuckets100.RemoteContent
import com.shuaiqiu.fuckets100.RemoteContentScreen
import com.shuaiqiu.fuckets100.SettingsManager
import com.shuaiqiu.fuckets100.ShizukuState
import com.shuaiqiu.fuckets100.ThemeSettingsScreen
import kotlinx.coroutines.launch

@Composable
fun FeNavContainer(
    navigator: FeNavigator,
    predictiveBackMode: PredictiveBackMode,
    currentMode: ActivationMode,
    shizukuState: ShizukuState,
    onActivationModeChanged: (ActivationMode) -> Unit,
    onSelectRootPage: (Int) -> Unit,
    onDarkModeChanged: (Boolean) -> Unit,
    onAutoDarkModeChanged: (Boolean) -> Unit,
    onDynamicColorChanged: (Boolean) -> Unit,
    onKeyColorChanged: (Int) -> Unit,
    onPaletteStyleChanged: (PaletteStyle) -> Unit,
    onColorSpecChanged: (ColorSpec.SpecVersion) -> Unit,
    onAmoledChanged: (Boolean) -> Unit,
    onPredictiveBackChanged: (PredictiveBackMode) -> Unit,
    mainContent: @Composable () -> Unit,
) {
    val animation = remember(predictiveBackMode) {
        when (predictiveBackMode) {
            PredictiveBackMode.AOSP -> FeAospPredictiveBackAnimation()
            PredictiveBackMode.KERNELSU_CLASSIC -> FeKernelSuPredictiveBackAnimation()
            PredictiveBackMode.SLIDE -> FeDefaultPredictiveBackAnimation()
            PredictiveBackMode.NONE -> FeNoPredictiveBackAnimation()
        }
    }
    val scope = rememberCoroutineScope()
    var gestureState: NavigationEventState<SceneInfo<FeRoute>>? = null
    var cloudAuthRevision by remember { mutableIntStateOf(0) }
    var isBackCompleting by remember { mutableStateOf(false) }

    val entries = rememberDecoratedNavEntries(
        backStack = navigator.backStack,
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator(),
            NavEntryDecorator(
                onPop = { key -> animation.onPagePop(key, scope) },
            ) { content ->
                with(animation) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .decoratePage(
                                gestureState?.transitionState,
                                content.contentKey,
                                navigator.current(),
                            ),
                    ) {
                        content.Content()
                    }
                }
            },
        ),
        entryProvider = entryProvider {
            entry<FeRoute.Main> { mainContent() }
            entry<FeRoute.Activation> {
                ActivationSettingsScreen(
                    currentMode = currentMode,
                    shizukuState = shizukuState,
                    cloudAuthRevision = cloudAuthRevision,
                    onModeSelected = { mode ->
                        SettingsManager.saveActivationMode(mode)
                        onActivationModeChanged(mode)
                    },
                    onBack = navigator::pop,
                    onNavigateToCloudActivation = { navigator.push(FeRoute.CloudActivation) },
                    onNavigateToRead = {
                        navigator.popUntil { it is FeRoute.Main }
                        onSelectRootPage(1)
                    },
                )
            }
            entry<FeRoute.CloudActivation> {
                CloudActivationScreen(
                    onBack = navigator::pop,
                    onLoginSuccess = {
                        SettingsManager.saveActivationMode(ActivationMode.CLOUD)
                        onActivationModeChanged(ActivationMode.CLOUD)
                        cloudAuthRevision++
                        navigator.pop()
                    },
                )
            }
            entry<FeRoute.GeneralSettings> { GeneralSettingsScreen(onBack = navigator::pop) }
            entry<FeRoute.ThemeSettings> {
                ThemeSettingsScreen(
                    onBack = navigator::pop,
                    onDarkModeChanged = onDarkModeChanged,
                    onAutoDarkModeChanged = onAutoDarkModeChanged,
                    onDynamicColorChanged = onDynamicColorChanged,
                    onKeyColorChanged = onKeyColorChanged,
                    onPaletteStyleChanged = onPaletteStyleChanged,
                    onColorSpecChanged = onColorSpecChanged,
                    onAmoledChanged = onAmoledChanged,
                    onPredictiveBackChanged = onPredictiveBackChanged,
                )
            }
            entry<FeRoute.Legal> { LegalScreen(onBack = navigator::pop) }
            entry<FeRoute.Donate> { DonateScreen(onBack = navigator::pop) }
            entry<FeRoute.RemoteContent> { route ->
                RemoteContentScreen(
                    content = RemoteContent(
                        announcementTitle = route.announcementTitle,
                        announcementMessage = route.announcementMessage,
                        announcementUpdatedAt = route.announcementUpdatedAt,
                        announcementUrl = route.announcementUrl,
                        changelogTitle = route.changelogTitle,
                        changelogSummary = route.changelogSummary,
                        changelogUrl = route.changelogUrl,
                    ),
                    onBack = navigator::pop,
                )
            }
        },
    )

    val sceneState = rememberSceneState(
        entries = entries,
        sceneStrategies = listOf(SinglePaneSceneStrategy()),
        sceneDecoratorStrategies = emptyList(),
        sharedTransitionScope = null,
        onBack = navigator::pop,
    )
    val scene = sceneState.currentScene
    gestureState = rememberNavigationEventState(
        currentInfo = SceneInfo(scene),
        backInfo = sceneState.previousScenes.map(::SceneInfo),
    )

    NavigationBackHandler(
        state = gestureState,
        isBackEnabled = scene.previousEntries.isNotEmpty() && !isBackCompleting,
        onBackCompleted = {
            if (isBackCompleting) return@NavigationBackHandler
            isBackCompleting = true
            val route = navigator.current()
            val transitionState = gestureState.transitionState
            scope.launch {
                try {
                    animation.onBackPressed(transitionState, route)
                    if (navigator.current() == route) navigator.pop()
                } finally {
                    isBackCompleting = false
                }
            }
        },
        onBackCancelled = {},
    )

    NavDisplay(
        sceneState = sceneState,
        navigationEventState = gestureState,
        contentAlignment = Alignment.TopStart,
        sizeTransform = null,
        predictivePopTransitionSpec = { swipeEdge -> with(animation) { predictivePop(swipeEdge) } },
        popTransitionSpec = { with(animation) { pop() } },
        transitionSpec = { with(animation) { push() } },
    )
}

package com.shuaiqiu.fuckets100

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.shuaiqiu.fuckets100.core.designsystem.rememberFeMainPagerState
import com.shuaiqiu.fuckets100.feature.onboarding.OnboardingScreen
import com.shuaiqiu.fuckets100.feature.account.FeAccountGate
import com.shuaiqiu.fuckets100.feature.settings.AnnouncementDialog
import com.shuaiqiu.fuckets100.feature.settings.VerificationDialog
import com.shuaiqiu.fuckets100.feature.settings.UpdateDialog
import com.shuaiqiu.fuckets100.feature.settings.SettingsScreen
import com.shuaiqiu.fuckets100.feature.settings.RemoteContent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.shuaiqiu.fuckets100.feature.home.HomeScreen
import com.shuaiqiu.fuckets100.feature.read.ReadScreen
import com.shuaiqiu.fuckets100.navigation.FeNavContainer
import com.shuaiqiu.fuckets100.navigation.FeNavigator
import com.shuaiqiu.fuckets100.navigation.FeRoute
import com.shuaiqiu.fuckets100.navigation.rememberFeNavigator

private val rootTabRoutes = listOf(Screen.Home.route, Screen.Read.route, Screen.Settings.route)

private fun rootPageForRoute(route: String?): Int? = rootTabRoutes.indexOf(route).takeIf { it >= 0 }

private fun UpdateStatus?.toRemoteContentRoute(): FeRoute.RemoteContent = FeRoute.RemoteContent(
    announcementTitle = this?.announcementTitle?.takeIf { it.isNotBlank() } ?: "公告",
    announcementMessage = this?.announcementMessage.orEmpty().ifBlank { "暂无公告" },
    announcementUpdatedAt = this?.announcementUpdatedAt.orEmpty(),
    announcementUrl = this?.announcementUrl.orEmpty(),
    changelogTitle = this?.changelogTitle?.takeIf { it.isNotBlank() } ?: "更新日志",
    changelogSummary = this?.changelogSummary.orEmpty()
        .ifBlank { this?.message.orEmpty().ifBlank { "暂无更新内容" } },
    changelogContent = this?.changelogContent.orEmpty(),
)

private fun handleTargetRoute(
    route: String,
    navigator: FeNavigator,
    pagerState: com.shuaiqiu.fuckets100.core.designsystem.FeMainPagerState,
) {
    rootPageForRoute(route)?.let {
        navigator.popUntil { current -> current is FeRoute.Main }
        pagerState.handlePageChange(it)
    }
}

/**
 * 搴旂敤涓荤晫闈㈢粍鍚堝嚱鏁? */
@Composable
fun FeAppMain(accountManager: FeAccountManager) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val feAccountState by accountManager.gateState.collectAsState()
    val accountOnline = feAccountState is FeAccountGateState.Ready
    val navigator = rememberFeNavigator(FeRoute.Main)
    // 答题页被弹出时释放对应 Paper，避免 navigation 层直接依赖 PaperStore。
    LaunchedEffect(navigator) {
        navigator.onRouteRemoved = { route ->
            if (route is FeRoute.Answer) PaperStore.remove(route.paperKey)
        }
    }
    val initialRootPage = remember { rootPageForRoute(MainActivity.pendingTargetRoute) ?: 0 }
    val pagerState = rememberPagerState(initialPage = initialRootPage, pageCount = { rootTabRoutes.size })
    val mainPagerState = rememberFeMainPagerState(pagerState)
    var predictiveBackAnimation by remember {
        mutableStateOf(SettingsManager.getPredictiveBackAnimation())
    }
    var predictiveBackExitDirection by remember {
        mutableStateOf(SettingsManager.getPredictiveBackExitDirection())
    }
    var compactAnswerDisplay by remember { mutableStateOf(SettingsManager.getCompactAnswerDisplay()) }

    val shizukuState = rememberShizukuState()

    var currentTheme by remember { mutableStateOf(ThemeManager.getSavedTheme()) }
    var isDarkMode by remember { mutableStateOf(ThemeManager.getSavedDarkMode()) }
    var isAutoDarkMode by remember { mutableStateOf(ThemeManager.getSavedAutoDarkMode()) }
    var useDynamicColor by remember { mutableStateOf(ThemeManager.getSavedDynamicColor()) }
    var keyColor by remember { mutableStateOf(ThemeManager.getSavedKeyColor()) }
    var paletteStyle by remember { mutableStateOf(ThemeManager.getSavedPaletteStyle()) }
    var colorSpec by remember { mutableStateOf(ThemeManager.getSavedColorSpec()) }
    var isAmoled by remember { mutableStateOf(ThemeManager.getSavedAmoled()) }
    val systemDarkMode = isSystemInDarkTheme()
    val effectiveDarkMode = if (isAutoDarkMode) systemDarkMode else isDarkMode

    // 鏇存柊寮圭獥鐘舵€?- 浣跨敤 snapshotFlow 鐩戝惉 FeApplication.updateStatus 鐨勫彉鍖栧柕~
    var showUpdateDialog by remember { mutableStateOf(false) }
    var updateDialogStatus by remember { mutableStateOf<com.shuaiqiu.fuckets100.UpdateStatus?>(null) }
    var verificationDialogStatus by remember { mutableStateOf<com.shuaiqiu.fuckets100.UpdateStatus?>(null) }
    var remoteStatus by remember { mutableStateOf(FeApplication.remoteStatus) }
    var showOnboarding by remember { mutableStateOf(!SettingsManager.hasCompletedOnboarding()) }
    var showAnnouncementDialog by remember { mutableStateOf(false) }

    // 鐩戝惉鏇存柊鐘舵€?Flow锛岀‘淇濇瘡娆￠兘鑳芥敹鍒伴€氱煡鍠祣
    LaunchedEffect(Unit) {
        FeApplication.updateStatusFlow.collect { status ->
            Log.d("FeAppMain", "updateStatusFlow 鏀跺埌: $status")
            if (status != null && status.showDialog) {
                updateDialogStatus = status
                showUpdateDialog = true
            }
        }
    }

    LaunchedEffect(Unit) {
        FeApplication.remoteStatusFlow.collect { status ->
            remoteStatus = status
            if (status != null && status.requiresVerification) {
                verificationDialogStatus = status
            }
            if (status != null &&
                status.announcementMessage.isNotBlank() &&
                status.announcementUpdatedAt != SettingsManager.getLastSeenAnnouncementUpdatedAt()
            ) {
                showAnnouncementDialog = true
            }
        }
    }

    var currentMode by remember {
        mutableStateOf(
            SettingsManager.getSavedActivationMode() ?: ShizukuManager.getCurrentActivationMode()
        )
    }
    var homeStatusRevision by remember { mutableIntStateOf(0) }

    var readAccessChecking by remember { mutableStateOf(true) }
    var readAccessIssue by remember { mutableStateOf<String?>(null) }

    // 进入读取页前先做一次统一预检，避免把未激活/未授权状态转化成底层异常。
    LaunchedEffect(
        currentMode,
        accountOnline,
        shizukuState.isRunning,
        shizukuState.permissionGranted,
    ) {
        readAccessChecking = true
        readAccessIssue = withContext(Dispatchers.IO) {
            when {
                !accountOnline -> "LA Studio 账号服务尚未就绪，请先登录或稍后重试"
                currentMode == ActivationMode.DEFAULT -> "尚未选择运行模式，请先完成激活配置"
                currentMode == ActivationMode.CLOUD && !ETS100AuthManager.isLoggedIn(context) ->
                    "云端模式尚未登录，请先完成云端账号绑定"
                currentMode != ActivationMode.CLOUD && !PermissionsHelper.hasAllFilesAccess() ->
                    "缺少文件访问权限，无法读取本地答题数据"
                currentMode == ActivationMode.SHIZUKU && !shizukuState.isRunning ->
                    "Shizuku 服务未运行，暂时无法读取本地答题数据"
                currentMode == ActivationMode.SHIZUKU && !shizukuState.permissionGranted ->
                    "Shizuku 尚未授权，请先完成 Shizuku 授权"
                currentMode == ActivationMode.ROOT && !RootManager.isRootAvailable() ->
                    "Root 权限尚未获取，暂时无法读取本地答题数据"
                currentMode == ActivationMode.DIRECT_READ && !ZWCHelper.isDirectReadAvailable() ->
                    "漏洞直读尚未就绪，请先在激活页面测试可用性"
                else -> null
            }
        }
        readAccessChecking = false
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                currentMode = SettingsManager.getSavedActivationMode() ?: ShizukuManager.getCurrentActivationMode()
                currentTheme = ThemeManager.getSavedTheme()
                isDarkMode = ThemeManager.getSavedDarkMode()
                isAutoDarkMode = ThemeManager.getSavedAutoDarkMode()
                useDynamicColor = ThemeManager.getSavedDynamicColor()
                keyColor = ThemeManager.getSavedKeyColor()
                paletteStyle = ThemeManager.getSavedPaletteStyle()
                colorSpec = ThemeManager.getSavedColorSpec()
                isAmoled = ThemeManager.getSavedAmoled()
                predictiveBackAnimation = SettingsManager.getPredictiveBackAnimation()
                predictiveBackExitDirection = SettingsManager.getPredictiveBackExitDirection()
                compactAnswerDisplay = SettingsManager.getCompactAnswerDisplay()
                MainActivity.pendingTargetRoute?.let { route ->
                    MainActivity.pendingTargetRoute = null
                    handleTargetRoute(route, navigator, mainPagerState)
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(Unit) {
        MainActivity.pendingTargetRoute?.let { route ->
            MainActivity.pendingTargetRoute = null
            handleTargetRoute(route, navigator, mainPagerState)
        }
    }

    LaunchedEffect(Unit) {
        MainActivity.targetRouteEvents.collect { route ->
            handleTargetRoute(route, navigator, mainPagerState)
        }
    }

    LaunchedEffect(shizukuState.isRunning, shizukuState.isSui) {
        if (!SettingsManager.hasUserSelectedMode()) {
            currentMode = ShizukuManager.getCurrentActivationMode()
        }
    }

    LaunchedEffect(pagerState.currentPage) {
        mainPagerState.syncPage()
    }

    FeThemeWrapper(
        theme = currentTheme,
        isDarkMode = effectiveDarkMode,
        useDynamicColor = useDynamicColor,
        keyColor = keyColor,
        paletteStyle = paletteStyle,
        colorSpec = colorSpec,
        isAmoled = isAmoled,
    ) {
        if (showOnboarding) {
            OnboardingScreen(
                onFinished = {
                    showOnboarding = false
                    currentMode = SettingsManager.getSavedActivationMode() ?: ShizukuManager.getCurrentActivationMode()
                },
            )
        } else {
            FeAccountGate {
            FeNavContainer(
            navigator = navigator,
            predictiveBackAnimation = predictiveBackAnimation,
            predictiveBackExitDirection = predictiveBackExitDirection,
            currentMode = currentMode,
            shizukuState = shizukuState,
            readAccessChecking = readAccessChecking,
            readAccessIssue = readAccessIssue,
            onHomeRefreshRequested = { homeStatusRevision++ },
            onActivationModeChanged = { currentMode = it },
            onSelectRootPage = mainPagerState::handlePageChange,
            onDarkModeChanged = { isDarkMode = it },
            onAutoDarkModeChanged = { isAutoDarkMode = it },
            onDynamicColorChanged = { useDynamicColor = it },
            onKeyColorChanged = { keyColor = it },
            onPaletteStyleChanged = { paletteStyle = it },
            onColorSpecChanged = { colorSpec = it },
            onAmoledChanged = { isAmoled = it },
            onPredictiveBackChanged = { predictiveBackAnimation = it },
            onPredictiveBackExitDirectionChanged = { predictiveBackExitDirection = it },
            selectedRootPage = mainPagerState.selectedPage,
            mainContent = {
                BackHandler(pagerState.currentPage != 0) {
                    mainPagerState.handlePageChange(0)
                }
                HorizontalPager(
                    state = pagerState,
                    userScrollEnabled = mainPagerState.userScrollEnabled,
                    beyondViewportPageCount = 1,
                    modifier = Modifier.fillMaxSize(),
                ) { page ->
                    when (page) {
                        0 -> HomeScreen(
                            mode = currentMode,
                            shizukuState = shizukuState,
                            accountState = feAccountState,
                            refreshRevision = homeStatusRevision,
                            onNavigateToActivation = {
                                navigator.push(FeRoute.Activation)
                            },
                            onNavigateToRemoteContent = { status ->
                                navigator.push(status.toRemoteContentRoute())
                            },
                        )

                        1 -> ReadScreen(
                            currentMode = currentMode,
                            accountOnline = accountOnline,
                            readAccessChecking = readAccessChecking,
                            readAccessIssue = readAccessIssue,
                            onNavigateToActivation = {
                                navigator.push(FeRoute.Activation)
                            },
                            onOpenPaperDetail = { paper ->
                                navigator.push(FeRoute.Answer(PaperStore.put(paper)))
                            },
                            compactAnswerDisplay = compactAnswerDisplay,
                        )

                        2 -> SettingsScreen(
                            onNavigateActivation = { navigator.push(FeRoute.Activation) },
                            onNavigateGeneralSettings = { navigator.push(FeRoute.GeneralSettings) },
                            onNavigateThemeSettings = { navigator.push(FeRoute.ThemeSettings) },
                            onNavigateAbout = { navigator.push(FeRoute.About) },
                            onNavigateAccount = { navigator.push(FeRoute.Account) },
                             accountSession = feAccountState.cachedSessionOrNull,
                        )
                    }
                }
            },
        )
        // 鏇存柊寮圭獥 - 鏀惧湪 Scaffold 澶栭潰纭繚鑳借鐩栧叾浠栧唴瀹瑰柕~
        if (showUpdateDialog && updateDialogStatus != null) {
            Log.d("FeAppMain", "鏄剧ず鏇存柊寮圭獥: ${updateDialogStatus!!.message}")
            UpdateDialog(
                status = updateDialogStatus!!,
                onDismiss = {
                    showUpdateDialog = false
                    updateDialogStatus = null
                    FeApplication.updateStatus = null
                }
            )
        }

        if (verificationDialogStatus != null) {
            val status = verificationDialogStatus!!
            VerificationDialog(
                status = status,
                onVerified = {
                    verificationDialogStatus = null
                    FeApplication.remoteStatus = status.copy(requiresVerification = false)
                    FeApplication.refreshRemoteStatusAfterVerification()
                }
            )
        }

        if (showAnnouncementDialog && remoteStatus != null) {
            val status = remoteStatus!!
            AnnouncementDialog(
                title = status.announcementTitle.ifBlank { "公告" },
                message = status.announcementMessage,
                onViewDetail = {
                    showAnnouncementDialog = false
                    SettingsManager.saveLastSeenAnnouncementUpdatedAt(status.announcementUpdatedAt)
                    navigator.push(status.toRemoteContentRoute())
                },
                onDismiss = {
                    showAnnouncementDialog = false
                    SettingsManager.saveLastSeenAnnouncementUpdatedAt(status.announcementUpdatedAt)
                },
            )
        }
        }
        }
    }
}

// ============================================================================
// UI 缁勪欢瀹氫箟 - 椤堕儴搴旂敤鏍忓拰璁剧疆椤?// ============================================================================

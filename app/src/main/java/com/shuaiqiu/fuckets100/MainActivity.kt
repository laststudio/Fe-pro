package com.shuaiqiu.fuckets100

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.compose.*
import androidx.compose.animation.*
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import com.shuaiqiu.fuckets100.component.material.FeRootNavigationScaffold

// ============================================================================
// 瀛椾綋瀹氫箟 - 鐢ㄤ簬搴旂敤鏍囬鐨勯啋鐩睍绀烘晥鏋?// ============================================================================
val RighteousFont = FontFamily(
    Font(resId = R.font.righteous, weight = FontWeight.Normal)
)

// ============================================================================
// 瀛椾綋瀹氫箟 - 鐢ㄤ簬搴旂敤鏍囬鐨勯啋鐩睍绀烘晥鏋?// ============================================================================

/**
 * 婵€娲绘ā寮忔灇涓? * 瀹氫箟搴旂敤鐨勪笉鍚岃繍琛屾巿鏉冩ā寮? */
enum class ActivationMode(
    val title: String,
    val desc: String,
    val sysLabel: String,
    val badge: String,
    val icon: ImageVector,
    val hexColor: Color,
    val isSysOffline: Boolean
) {
    DEFAULT(
        "未激活",
        "请选择授权模式以启用核心功能",
        "SYS_OFFLINE",
        "Inactive",
        Icons.Default.Warning,
        Color(0xFFFFB4AB),
        true
    ),
    SHIZUKU(
        "Shizuku 已连接",
        "借助 Shizuku 在 Root 环境下以系统级 API 实现答题增强功能",
        "SYS_READY",
        "Recommended",
        Icons.Default.CheckCircle,
        Color(0xFF4ADE80),
        false
    ),
    ROOT(
        "Root 权限已获取",
        "通过 Root 权限直接访问应用数据文件实现答题增强功能",
        "SYS_READY",
        "Highest Perm",
        Icons.Default.Security,
        Color(0xFFFFB4AB),
        false
    ),
    DIRECT_READ(
        "Direct Read 漏洞直读",
        "利用零宽字符漏洞绕过 Android 限制直接读取应用内部存储实现答题增强",
        "SYS_DIRECT_READ",
        "Legacy",
        Icons.Default.Bolt,
        Color(0xFFFBBF24),
        false
    ),
    CLOUD(
        "云端模式",
        "通过 ETS100 云端 API 在线获取作业列表和答案",
        "SYS_READY",
        "Cloud",
        Icons.Default.Cloud,
        Color(0xFF60A5FA),
        false
    )
}

sealed class Screen(val route: String, val icon: ImageVector, val label: String) {
    object Home : Screen("home", Icons.Default.Home, "首页")
    object Read : Screen("read", Icons.AutoMirrored.Filled.MenuBook, "答题")
    object Settings : Screen("settings", Icons.Default.Settings, "设置")
    object Activation : Screen("activation", Icons.Default.Build, "激活")
    object GeneralSettings : Screen("general_settings", Icons.Default.Tune, "通用")
    object ThemeSettings : Screen("theme_settings", Icons.Default.Palette, "主题")
    object Debug : Screen("debug", Icons.Default.BugReport, "调试")
    object CloudActivation : Screen("cloud_activation", Icons.Default.Cloud, "云端激活")
    object Legal : Screen("legal", Icons.Default.Gavel, "法律")
}
// ============================================================================
// 涓昏娲诲姩绫?- 搴旂敤鍏ュ彛
// ============================================================================

class MainActivity : ComponentActivity() {
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent.getStringExtra(EXTRA_TARGET_ROUTE)?.let { route ->
            pendingTargetRoute = route
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge() // 鍚敤娌夋蹈寮忚竟缂樺埌杈圭紭甯冨眬
        intent.getStringExtra(EXTRA_TARGET_ROUTE)?.let { route ->
            pendingTargetRoute = route
        }
        
        // 鍒濆鍖?ThemeManager
        ThemeManager.init(this)
        
        setContent {
            FeAppMain()
        }
    }

    companion object {
        private const val EXTRA_TARGET_ROUTE = "target_route"

        var pendingTargetRoute: String? = null

        fun createIntent(context: android.content.Context, targetRoute: String? = null): Intent {
            return Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                if (targetRoute != null) {
                    putExtra(EXTRA_TARGET_ROUTE, targetRoute)
                    pendingTargetRoute = targetRoute
                }
            }
        }
    }
}

/**
 * 搴旂敤涓婚鍖呰鍣? */
@Composable
fun FeTheme(content: @Composable () -> Unit) {
    val theme = ThemeManager.getSavedTheme()
    val systemDarkMode = isSystemInDarkTheme()
    val effectiveDarkMode = if (ThemeManager.getSavedAutoDarkMode()) {
        systemDarkMode
    } else {
        ThemeManager.getSavedDarkMode()
    }
    val useDynamicColor = ThemeManager.getSavedDynamicColor()

    FeThemeWrapper(
        theme = theme,
        isDarkMode = effectiveDarkMode,
        useDynamicColor = useDynamicColor,
        content = content
    )
}

private val rootTabRoutes = setOf(Screen.Home.route, Screen.Read.route, Screen.Settings.route)

fun NavHostController.navigateRootTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}

private fun AnimatedContentTransitionScope<NavBackStackEntry>.isRootTabTransition(): Boolean {
    return initialState.destination.route in rootTabRoutes && targetState.destination.route in rootTabRoutes
}

private fun AnimatedContentTransitionScope<NavBackStackEntry>.slideEnterTransition(): EnterTransition {
    if (isRootTabTransition()) {
        return EnterTransition.None
    }

    return slideInHorizontally(
        initialOffsetX = { it },
        animationSpec = tween(300, easing = FastOutSlowInEasing)
    ) + fadeIn(
        initialAlpha = 0.85f,
        animationSpec = tween(120, easing = FastOutSlowInEasing)
    )
}

private fun AnimatedContentTransitionScope<NavBackStackEntry>.slideExitTransition(): ExitTransition {
    if (isRootTabTransition()) {
        return ExitTransition.None
    }

    return ExitTransition.None
}

private fun AnimatedContentTransitionScope<NavBackStackEntry>.slidePopEnterTransition(): EnterTransition {
    if (isRootTabTransition()) {
        return EnterTransition.None
    }

    return EnterTransition.None
}

private fun AnimatedContentTransitionScope<NavBackStackEntry>.slidePopExitTransition(): ExitTransition {
    if (isRootTabTransition()) {
        return ExitTransition.None
    }

    return slideOutHorizontally(
        targetOffsetX = { it },
        animationSpec = tween(300, easing = FastOutSlowInEasing)
    ) + fadeOut(
        targetAlpha = 0.85f,
        animationSpec = tween(120, easing = FastOutSlowInEasing)
    )
}

private fun AnimatedContentTransitionScope<NavBackStackEntry>.fadeOnlyEnterTransition(): EnterTransition {
    if (isRootTabTransition()) {
        return EnterTransition.None
    }

    return fadeIn()
}

private fun AnimatedContentTransitionScope<NavBackStackEntry>.fadeOnlyExitTransition(): ExitTransition {
    if (isRootTabTransition()) {
        return ExitTransition.None
    }

    return fadeOut()
}

private fun AnimatedContentTransitionScope<NavBackStackEntry>.fadeAndScalePopExitTransition(): ExitTransition {
    if (isRootTabTransition()) {
        return ExitTransition.None
    }

    return scaleOut(targetScale = 0.9f) + fadeOut()
}

/** 
 * 搴旂敤涓荤晫闈㈢粍鍚堝嚱鏁? */
@Composable
fun FeAppMain() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val rootContentAlpha = remember { Animatable(1f) }
    var previousRouteForRootAnimation by remember { mutableStateOf<String?>(null) }
    val predictiveBackState = rememberAospPredictiveBackState()
    var predictiveBackMode by remember { mutableStateOf(SettingsManager.getPredictiveBackMode()) }
    var compactAnswerDisplay by remember { mutableStateOf(SettingsManager.getCompactAnswerDisplay()) }

    val shizukuState = rememberShizukuState()
    
    var currentTheme by remember { mutableStateOf(ThemeManager.getSavedTheme()) }
    var isDarkMode by remember { mutableStateOf(ThemeManager.getSavedDarkMode()) }
    var isAutoDarkMode by remember { mutableStateOf(ThemeManager.getSavedAutoDarkMode()) }
    var useDynamicColor by remember { mutableStateOf(ThemeManager.getSavedDynamicColor()) }
    val systemDarkMode = isSystemInDarkTheme()
    val effectiveDarkMode = if (isAutoDarkMode) systemDarkMode else isDarkMode
    
    // 鏇存柊寮圭獥鐘舵€?- 浣跨敤 snapshotFlow 鐩戝惉 FeApplication.updateStatus 鐨勫彉鍖栧柕~
    var showUpdateDialog by remember { mutableStateOf(false) }
    var updateDialogStatus by remember { mutableStateOf<com.shuaiqiu.fuckets100.UpdateStatus?>(null) }
    var verificationDialogStatus by remember { mutableStateOf<com.shuaiqiu.fuckets100.UpdateStatus?>(null) }
    var showLegalDialog by remember { mutableStateOf(!SettingsManager.hasAcceptedLegal()) }
    
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
            if (status != null && status.requiresVerification) {
                verificationDialogStatus = status
            }
        }
    }
    
    var currentMode by remember {
        mutableStateOf(
            SettingsManager.getSavedActivationMode() ?: ShizukuManager.getCurrentActivationMode()
        )
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                currentMode = SettingsManager.getSavedActivationMode() ?: ShizukuManager.getCurrentActivationMode()
                currentTheme = ThemeManager.getSavedTheme()
                isDarkMode = ThemeManager.getSavedDarkMode()
                isAutoDarkMode = ThemeManager.getSavedAutoDarkMode()
                useDynamicColor = ThemeManager.getSavedDynamicColor()
                predictiveBackMode = SettingsManager.getPredictiveBackMode()
                compactAnswerDisplay = SettingsManager.getCompactAnswerDisplay()
                MainActivity.pendingTargetRoute?.let { route ->
                    MainActivity.pendingTargetRoute = null
                    navController.navigateRootTab(route)
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(Unit) {
        MainActivity.pendingTargetRoute?.let { route ->
            MainActivity.pendingTargetRoute = null
            navController.navigateRootTab(route)
        }
    }
    
    LaunchedEffect(shizukuState.isRunning, shizukuState.isSui) {
        if (!SettingsManager.hasUserSelectedMode()) {
            currentMode = ShizukuManager.getCurrentActivationMode()
        }
    }

    LaunchedEffect(currentRoute) {
        val previousRoute = previousRouteForRootAnimation
        val isRootSwitch = previousRoute != null &&
            previousRoute != currentRoute &&
            previousRoute in rootTabRoutes &&
            currentRoute in rootTabRoutes

        if (isRootSwitch) {
            rootContentAlpha.snapTo(0.05f)
            rootContentAlpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(650, easing = FastOutSlowInEasing)
            )
        } else if (rootContentAlpha.value != 1f) {
            rootContentAlpha.snapTo(1f)
        }

        previousRouteForRootAnimation = currentRoute
    }

    FeThemeWrapper(
        theme = currentTheme,
        isDarkMode = effectiveDarkMode,
        useDynamicColor = useDynamicColor
    ) {
        FeRootNavigationScaffold(
            currentRoute = currentRoute,
            onNavigate = navController::navigateRootTab,
        ) { innerPadding ->
            BoxWithConstraints(
                modifier = Modifier
                    .padding(innerPadding)
                    .graphicsLayer {
                        alpha = rootContentAlpha.value
                    }
            ) {
                val density = LocalDensity.current
                val containerHeightPx = with(density) { maxHeight.toPx() }
                val predictiveBackOffsetPx = with(density) { 96.dp.toPx() }
                val deviceCornerRadius = rememberDeviceCornerRadius()

                NavHost(
                    navController = navController,
                    startDestination = Screen.Home.route,
                    modifier = Modifier
                        .fillMaxSize()
                        .then(
                            if (predictiveBackMode == PredictiveBackMode.AOSP) {
                                Modifier.aospPredictiveBackAnimation(
                                    state = predictiveBackState,
                                    containerHeightPx = containerHeightPx,
                                    exitingOffsetPx = predictiveBackOffsetPx,
                                    deviceCornerRadius = deviceCornerRadius
                                )
                            } else {
                                Modifier
                            }
                        ),
                    enterTransition = {
                        when (predictiveBackMode) {
                            PredictiveBackMode.NONE -> EnterTransition.None
                            PredictiveBackMode.KERNELSU_CLASSIC -> fadeOnlyEnterTransition()
                            else -> slideEnterTransition()
                        }
                    },
                    exitTransition = {
                        when (predictiveBackMode) {
                            PredictiveBackMode.NONE -> ExitTransition.None
                            PredictiveBackMode.KERNELSU_CLASSIC -> fadeOnlyExitTransition()
                            else -> slideExitTransition()
                        }
                    },
                    popEnterTransition = {
                        when (predictiveBackMode) {
                            PredictiveBackMode.NONE -> EnterTransition.None
                            PredictiveBackMode.KERNELSU_CLASSIC -> fadeOnlyEnterTransition()
                            else -> slidePopEnterTransition()
                        }
                    },
                    popExitTransition = {
                        when (predictiveBackMode) {
                            PredictiveBackMode.NONE -> ExitTransition.None
                            PredictiveBackMode.KERNELSU_CLASSIC -> fadeAndScalePopExitTransition()
                            else -> slidePopExitTransition()
                        }
                    }
                ) {

                    composable(Screen.Home.route) { 
                        HomeScreen(
                            mode = currentMode,
                            shizukuState = shizukuState,
                            onNavigateToActivation = {
                                context.startActivity(ActivationActivity.createIntent(context))
                            }
                        )
                    }
                    
                    composable(Screen.Read.route) {
                        ReadScreen(
                            currentMode = currentMode,
                            onNavigateToActivation = {
                                context.startActivity(ActivationActivity.createIntent(context))
                            },
                            compactAnswerDisplay = compactAnswerDisplay
                        )
                    }
                    
                    composable(Screen.Settings.route) { 
                        SettingsScreen(navController) 
                    }
                    
                    composable(Screen.Debug.route) {
                        DebugScreen(navController = navController)
                    }
                }
            }
        }

        AospPredictiveBackHandler(
            state = predictiveBackState,
            enabled = predictiveBackMode == PredictiveBackMode.AOSP &&
                navController.previousBackStackEntry != null,
            onBack = { navController.popBackStack() }
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
        } else if (showLegalDialog) {
            LegalAcceptanceDialog(
                onAccepted = {
                    SettingsManager.saveLegalAccepted(true)
                    showLegalDialog = false
                }
            )
        }
    }
}

// ============================================================================
// UI 缁勪欢瀹氫箟 - 椤堕儴搴旂敤鏍忓拰璁剧疆椤?// ============================================================================

/**
 * 搴旂敤椤堕儴瀵艰埅鏍? */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeTopAppBar(title: String) {
    CenterAlignedTopAppBar(
        title = {
            Text(
                text = title,
                fontSize = 36.sp,
                letterSpacing = 0.em,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.primary,
                fontFamily = RighteousFont,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
    )
}

/**
 * 璁剧疆鍒楄〃椤圭粍浠? */
@Composable
fun SettingsListItem(
    icon: ImageVector,
    title: String,
    sub: String,
    hideChevron: Boolean = false,
    iconTint: Color = MaterialTheme.colorScheme.primary,
    onClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(24.dp)
        )
        Column(modifier = Modifier.weight(1f).padding(start = 16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            if (sub.isNotEmpty()) {
                Text(sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (!hideChevron) {
            Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.outlineVariant)
        }
    }
}

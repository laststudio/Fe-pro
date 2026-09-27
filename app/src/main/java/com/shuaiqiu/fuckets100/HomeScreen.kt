package com.shuaiqiu.fuckets100

import android.os.SystemClock
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.shuaiqiu.fuckets100.component.material.ExpressiveScaffold
import com.shuaiqiu.fuckets100.component.material.TonalCard
import com.shuaiqiu.fuckets100.component.material.expressiveTopAppBarColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private data class HomeRuntimeStatus(
    val hasFilesPerm: Boolean,
    val hasOverlayPerm: Boolean,
    val hasAppListPerm: Boolean,
    val hasRootAvailable: Boolean,
    val hasDirectReadAvailable: Boolean,
    val cloudLoggedIn: Boolean
)

private object HomeRuntimeStatusStore {
    private const val CACHE_TTL_MS = 30_000L

    var cached: HomeRuntimeStatus? = null
    var hasLoaded = false
    var lastRefreshTime = 0L
    var lastMode: ActivationMode? = null

    fun isFresh(): Boolean {
        return hasLoaded && cached != null && SystemClock.elapsedRealtime() - lastRefreshTime < CACHE_TTL_MS
    }

    fun getOrInit(context: android.content.Context): HomeRuntimeStatus {
        cached?.let { return it }
        val status = HomeRuntimeStatus(
            hasFilesPerm = PermissionsHelper.hasAllFilesAccess(),
            hasOverlayPerm = PermissionsHelper.hasOverlayPermission(context),
            hasAppListPerm = PermissionsHelper.hasAppListPermission(),
            hasRootAvailable = RootManager.isRootAvailable(),
            hasDirectReadAvailable = ZWCHelper.isDirectReadAvailable(),
            cloudLoggedIn = ETS100AuthManager.isLoggedIn(context)
        )
        cached = status
        hasLoaded = true
        lastRefreshTime = SystemClock.elapsedRealtime()
        return status
    }

    fun update(status: HomeRuntimeStatus, mode: ActivationMode? = lastMode) {
        cached = status
        hasLoaded = true
        lastRefreshTime = SystemClock.elapsedRealtime()
        lastMode = mode
    }
}

/**
 * 首页主屏幕
 * 显示系统状态、设备信息和激活状态
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    mode: ActivationMode,
    shizukuState: ShizukuState,
    accountState: FeAccountGateState,
    onNavigateToActivation: () -> Unit,
    onNavigateToRemoteContent: (UpdateStatus?) -> Unit,
    onNavigateToDonate: () -> Unit,
) {
    val context = LocalContext.current
    val appContext = remember(context) { context.applicationContext }
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val currentModeForRefresh by rememberUpdatedState(mode)
    
    var runtimeStatus by remember {
        mutableStateOf(HomeRuntimeStatusStore.getOrInit(appContext))
    }
    var remoteStatus by remember {
        mutableStateOf(FeApplication.remoteStatus)
    }
    
    // 生命周期监听 - 从系统设置返回时自动刷新权限状态
    var hasPaused by remember { mutableStateOf(false) }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) {
                hasPaused = true
            } else if (event == Lifecycle.Event.ON_RESUME && hasPaused) {
                scope.launch {
                    val status = loadHomeRuntimeStatus(appContext, force = true)
                    HomeRuntimeStatusStore.update(status, currentModeForRefresh)
                    runtimeStatus = status
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    
    LaunchedEffect(mode) {
        val status = loadHomeRuntimeStatus(
            context = appContext,
            force = HomeRuntimeStatusStore.lastMode != mode
        )
        HomeRuntimeStatusStore.update(status, mode)
        runtimeStatus = status
    }

    LaunchedEffect(Unit) {
        FeApplication.remoteStatusFlow.collect { status ->
            remoteStatus = status
        }
    }
    
    // 检查基础权限是否全部获取
    val hasAllBasicPermissions = runtimeStatus.hasFilesPerm &&
        runtimeStatus.hasOverlayPerm &&
        runtimeStatus.hasAppListPerm
    
    // 判断是否真正激活 - 根据不同模式判断激活条件
    // Direct Read 模式会检测零宽字符漏洞绕过限制，而其他模式需要相应的权限和配置
    val isTrulyActivated = when {
        mode == ActivationMode.SHIZUKU -> shizukuState.isRunning && shizukuState.permissionGranted && hasAllBasicPermissions
        mode == ActivationMode.ROOT -> hasAllBasicPermissions && runtimeStatus.hasRootAvailable
        mode == ActivationMode.DIRECT_READ -> hasAllBasicPermissions && runtimeStatus.hasDirectReadAvailable
        mode == ActivationMode.CLOUD -> runtimeStatus.cloudLoggedIn
        mode != ActivationMode.DEFAULT -> hasAllBasicPermissions
        else -> false
    }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    ExpressiveScaffold(
        topBar = {
            LargeFlexibleTopAppBar(
                title = {
                    Text(
                        text = "Fe",
                        fontFamily = RighteousFont,
                        fontWeight = FontWeight.Normal,
                        color = MaterialTheme.colorScheme.primary,
                    )
                },
                colors = expressiveTopAppBarColors(),
                windowInsets = WindowInsets.safeDrawing.only(
                    WindowInsetsSides.Top + WindowInsetsSides.Horizontal,
                ),
                scrollBehavior = scrollBehavior,
            )
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        contentWindowInsets = WindowInsets.safeDrawing.only(
            WindowInsetsSides.Top + WindowInsetsSides.Horizontal,
        ),
    ) { paddingValues ->
        Column(
            Modifier
                .padding(paddingValues)
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(13.dp)
        ) {
            StatusCard(
                mode = mode, 
                shizukuState = shizukuState, 
                accountState = accountState,
                isTrulyActivated = isTrulyActivated, 
                cloudLoggedIn = runtimeStatus.cloudLoggedIn,
                hasFilesPerm = runtimeStatus.hasFilesPerm,
                hasOverlayPerm = runtimeStatus.hasOverlayPerm,
                hasAppListPerm = runtimeStatus.hasAppListPerm,
                hasAllBasicPermissions = hasAllBasicPermissions,
                hasRootAvailable = runtimeStatus.hasRootAvailable,
                onNavigateToActivation = onNavigateToActivation
            )
            RuntimeInfoCard(
                mode = mode,
                shizukuState = shizukuState,
                runtimeStatus = runtimeStatus,
            )
            HomeRemoteContent(
                status = remoteStatus,
                onNavigateToRemoteContent = onNavigateToRemoteContent,
                onNavigateToDonate = onNavigateToDonate,
            )
            Spacer(Modifier.height(16.dp))
        }
    }
}

private suspend fun loadHomeRuntimeStatus(
    context: android.content.Context,
    force: Boolean
): HomeRuntimeStatus {
    if (!force && HomeRuntimeStatusStore.isFresh()) {
        return HomeRuntimeStatusStore.cached ?: HomeRuntimeStatusStore.getOrInit(context)
    }

    return withContext(Dispatchers.IO) {
        HomeRuntimeStatus(
            hasFilesPerm = PermissionsHelper.hasAllFilesAccess(),
            hasOverlayPerm = PermissionsHelper.hasOverlayPermission(context),
            hasAppListPerm = PermissionsHelper.hasAppListPermission(),
            hasRootAvailable = RootManager.isRootAvailable(),
            hasDirectReadAvailable = ZWCHelper.isDirectReadAvailable(),
            cloudLoggedIn = ETS100AuthManager.isLoggedIn(context)
        )
    }
}

/**
 * 状态卡片组件
 * 显示当前激活模式和权限状态
 */
@Composable
fun StatusCard(
    mode: ActivationMode,
    shizukuState: ShizukuState,
    accountState: FeAccountGateState,
    isTrulyActivated: Boolean,
    cloudLoggedIn: Boolean,
    hasFilesPerm: Boolean,
    hasOverlayPerm: Boolean,
    hasAppListPerm: Boolean,
    hasAllBasicPermissions: Boolean,
    hasRootAvailable: Boolean,
    onNavigateToActivation: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val isConnecting = accountState is FeAccountGateState.Checking
    val needsLogin = accountState is FeAccountGateState.LoginRequired
    val isOffline = accountState is FeAccountGateState.Offline
    val isBlocked = accountState is FeAccountGateState.Blocked
    val effectiveActivated = accountState is FeAccountGateState.Ready && isTrulyActivated
    val statusContainerColor = when {
        isConnecting -> MaterialTheme.colorScheme.surfaceContainerHighest
        effectiveActivated -> MaterialTheme.colorScheme.secondaryContainer
        else -> MaterialTheme.colorScheme.errorContainer
    }
    val statusContentColor = contentColorFor(statusContainerColor)

    // 根据当前状态显示不同的标题
    val displayTitle = when {
        isConnecting -> "正在连接 LA Studio"
        needsLogin -> "需要登录"
        isOffline -> "LA Studio 已离线"
        isBlocked -> "LA Studio 当前不可用"
        mode == ActivationMode.CLOUD && cloudLoggedIn -> "云端模式已激活"
        mode == ActivationMode.CLOUD -> "云端模式未激活"
        // 基础权限未授予时显示等待授权提示
        !hasAllBasicPermissions -> "等待授权"
        mode == ActivationMode.SHIZUKU && shizukuState.isRunning && !shizukuState.permissionGranted -> "Shizuku 等待授权"
        mode == ActivationMode.SHIZUKU && !shizukuState.isRunning -> "Shizuku 未运行"
        mode == ActivationMode.ROOT && !hasRootAvailable -> "Root 未获取"
        else -> mode.title
    }

    val displayIcon = when {
        effectiveActivated -> Icons.Outlined.CheckCircle
        mode == ActivationMode.DEFAULT -> Icons.Outlined.Block
        else -> Icons.Outlined.Warning
    }
    val summary = when {
        isConnecting -> "正在验证账号和软件授权，请稍候"
        needsLogin -> "请先登录 LA Studio 账号后继续使用"
        isOffline -> "LA Studio 已离线，部分功能受限"
        isBlocked -> accountState.message
        mode == ActivationMode.CLOUD -> if (cloudLoggedIn) "云端作业与试卷解析服务已连接" else "未绑定或未配置云端凭据，点击前往配置"
        !hasAllBasicPermissions -> when {
            !hasFilesPerm -> "需要文件访问权限才能读取题库，请授权"
            !hasOverlayPerm -> "需要悬浮窗权限才能显示答题界面，请授权"
            !hasAppListPerm -> "需要应用列表权限才能检测ETS应用，请授权"
            else -> "请授权上述权限后即可使用"
        }
        mode == ActivationMode.SHIZUKU -> when {
            !shizukuState.isRunning -> "请启动Shizuku后再返回此处"
            !shizukuState.permissionGranted -> "请授予Shizuku权限后即可使用"
            else -> {
                val uidStr = when (shizukuState.uid) {
                    0 -> "Root (0)"
                    2000 -> "ADB (2000)"
                    else -> shizukuState.uid.toString()
                }
                "${shizukuState.getRuntimeTypeName()} v${shizukuState.version} | UID: $uidStr"
            }
        }
        mode == ActivationMode.ROOT -> if (!hasRootAvailable) {
            "未检测到Root权限，请先获取Root"
        } else {
            "Root 权限已获取"
        }
        mode == ActivationMode.DIRECT_READ -> "Direct Read 模式已就绪"
        mode == ActivationMode.DEFAULT -> "请授权基础权限后选择激活模式"
        else -> "点击进入激活设置"
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
            onNavigateToActivation()
        },
        shape = MaterialTheme.shapes.large,
        color = statusContainerColor,
        contentColor = statusContentColor,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = displayIcon,
                contentDescription = null,
                tint = statusContentColor,
                modifier = Modifier.size(32.dp),
            )
            Spacer(Modifier.width(16.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = displayTitle,
                    style = MaterialTheme.typography.titleMediumEmphasized,
                    color = statusContentColor,
                    maxLines = 1,
                    minLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = summary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = statusContentColor.copy(alpha = 0.82f),
                    maxLines = 1,
                    minLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(12.dp))
            if (isConnecting) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = statusContentColor,
                    )
                    Text("连接中", color = statusContentColor, style = MaterialTheme.typography.labelMedium)
                }
            } else {
                StatusTag(
                    label = when {
                        isOffline -> "离线"
                        needsLogin -> "未登录"
                        effectiveActivated -> mode.shortLabel
                        else -> "未就绪"
                    },
                    containerColor = if (effectiveActivated) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                    contentColor = if (effectiveActivated) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onError,
                )
            }
        }
    }
}

@Composable
private fun HomeRemoteContent(
    status: UpdateStatus?,
    onNavigateToRemoteContent: (UpdateStatus?) -> Unit,
    onNavigateToDonate: () -> Unit,
) {
    val announcementMessage = status?.announcementMessage.orEmpty()
    val announcementTitle = status?.announcementTitle?.takeIf { it.isNotBlank() } ?: "公告"
    val donateEnabled = status?.donateEnabled ?: true

    Column(verticalArrangement = Arrangement.spacedBy(13.dp)) {
        HomeLinkCard(
            title = announcementTitle,
            summary = announcementMessage.ifBlank { if (status == null) "正在同步远程内容" else "暂无公告" },
            onClick = { onNavigateToRemoteContent(status) },
        )
        if (donateEnabled) {
            HomeLinkCard(
                title = "捐赠支持",
                summary = "支持 Fe 的后续维护与功能更新",
                onClick = onNavigateToDonate
            )
        }
    }
}

@Composable
private fun HomeLinkCard(
    title: String,
    summary: String,
    onClick: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    TonalCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
            onClick()
        },
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(4.dp))
                Text(
                    summary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(12.dp))
            Icon(Icons.Default.ChevronRight, contentDescription = null)
        }
    }
}

@Composable
private fun RuntimeInfoCard(
    mode: ActivationMode,
    shizukuState: ShizukuState,
    runtimeStatus: HomeRuntimeStatus,
) {
    TonalCard {
        Column(
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            InfoItem("应用版本", "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
            InfoItem("当前授权模式", mode.shortLabel)
            if (mode == ActivationMode.SHIZUKU) {
                val runtime = when {
                    !shizukuState.isRunning -> "未运行"
                    !shizukuState.permissionGranted -> "等待授权"
                    else -> "${shizukuState.getRuntimeTypeName()} · UID ${shizukuState.uid}"
                }
                InfoItem("Shizuku", runtime)
            }
            InfoItem(
                "基础权限",
                listOf(runtimeStatus.hasFilesPerm, runtimeStatus.hasOverlayPerm, runtimeStatus.hasAppListPerm)
                    .count { it }
                    .let { "$it / 3 已授权" },
            )
        }
    }
}

@Composable
private fun InfoItem(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun StatusTag(
    label: String,
    containerColor: androidx.compose.ui.graphics.Color,
    contentColor: androidx.compose.ui.graphics.Color,
) {
    Surface(
        color = containerColor,
        contentColor = contentColor,
        shape = MaterialTheme.shapes.extraSmall,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmallEmphasized,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
        )
    }
}

private val ActivationMode.shortLabel: String
    get() = when (this) {
        ActivationMode.DEFAULT -> "未激活"
        ActivationMode.SHIZUKU -> "Shizuku"
        ActivationMode.ROOT -> "Root"
        ActivationMode.DIRECT_READ -> "Direct Read"
        ActivationMode.CLOUD -> "云端"
    }

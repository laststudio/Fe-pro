package com.shuaiqiu.fuckets100.feature.activation

import android.content.pm.PackageManager
import android.os.Build
import android.os.SystemClock
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.shuaiqiu.fuckets100.core.designsystem.ExpressiveScaffold
import com.shuaiqiu.fuckets100.core.designsystem.ExpressiveSegmentedListItem
import com.shuaiqiu.fuckets100.core.designsystem.SegmentedColumn
import com.shuaiqiu.fuckets100.core.designsystem.TonalCard
import com.shuaiqiu.fuckets100.core.designsystem.TopBarBackButton
import com.shuaiqiu.fuckets100.core.designsystem.expressiveTopAppBarColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import com.shuaiqiu.fuckets100.ActivationMode
import com.shuaiqiu.fuckets100.ETS100AuthManager
import com.shuaiqiu.fuckets100.core.designsystem.FeThinDivider
import com.shuaiqiu.fuckets100.PermissionsHelper
import com.shuaiqiu.fuckets100.RootManager
import com.shuaiqiu.fuckets100.ShizukuManager
import com.shuaiqiu.fuckets100.ShizukuState
import com.shuaiqiu.fuckets100.ZWCHelper
import com.shuaiqiu.fuckets100.maskPhoneNumber
import com.shuaiqiu.fuckets100.core.designsystem.FeThinDivider
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ActivationSettingsScreen(
    currentMode: ActivationMode,
    shizukuState: ShizukuState,
    cloudAuthRevision: Int = 0,
    onModeSelected: (ActivationMode) -> Unit,
    onBack: () -> Unit,
    onNavigateToCloudActivation: () -> Unit,
    onNavigateToRead: () -> Unit,
    viewModel: ActivationViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val uiState by viewModel.uiState.collectAsState()
    val pageSnapshot = uiState.snapshot
    val hasFilesPerm = pageSnapshot?.hasFilesPermission == true
    val cloudLoggedIn = pageSnapshot?.cloudLoggedIn == true
    val cloudPhone = pageSnapshot?.cloudPhone
    val etsAppInfo = pageSnapshot?.etsAppInfo
    var pendingPermissionRequest by remember { mutableStateOf(false) }

    // 首次打开异步读取；已有短时缓存时直接沿用，不重复触发页面闪变。
    LaunchedEffect(cloudAuthRevision) {
        viewModel.refresh(force = false)
    }

    var hasPaused by remember { mutableStateOf(false) }
    // 监听生命周期，从系统设置返回时自动刷新基础权限的绿勾勾
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) {
                hasPaused = true
            } else if (event == Lifecycle.Event.ON_RESUME && hasPaused) {
                viewModel.refresh(force = true)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // 处理 Shizuku 权限请求回调
    LaunchedEffect(Unit) {
        val permissionListener: (Int, Int) -> Unit = { requestCode, grantResult ->
            if (requestCode == ShizukuManager.REQUEST_CODE_SHIZUKU_PERMISSION) {
                pendingPermissionRequest = false
                if (grantResult == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(context, "Shizuku 权限授权成功！", Toast.LENGTH_SHORT).show()
                    onModeSelected(ActivationMode.SHIZUKU)
                } else {
                    Toast.makeText(context, "Shizuku 权限被拒绝了", Toast.LENGTH_SHORT).show()
                }
            }
        }
        ShizukuManager.addPermissionResultListener(permissionListener)
    }

    val successColor = MaterialTheme.colorScheme.primary
    val hasLocalBasicPermissions = hasFilesPerm
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val activationModes = ActivationMode.values().filter { it != ActivationMode.DEFAULT }

    ExpressiveScaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        contentWindowInsets = WindowInsets.safeDrawing.only(
            WindowInsetsSides.Top + WindowInsetsSides.Horizontal,
        ),
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text("运行授权与权限") },
                subtitle = { Text("配置设备权限与工作模式") },
                navigationIcon = { TopBarBackButton(onClick = onBack) },
                windowInsets = WindowInsets.safeDrawing.only(
                    WindowInsetsSides.Top + WindowInsetsSides.Horizontal,
                ),
                colors = expressiveTopAppBarColors(),
                scrollBehavior = scrollBehavior,
            )
        }
    ) { p ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(p)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item { Spacer(Modifier.height(16.dp)) }

            // 当前模式状态卡片
            item {
                FeModeStatusCard(
                    currentMode = currentMode,
                    shizukuState = shizukuState,
                    hasAllBasicPermissions = hasLocalBasicPermissions,
                    cloudLoggedIn = cloudLoggedIn,
                    cloudPhone = cloudPhone,
                    context = context
                )
                Spacer(Modifier.height(8.dp))
            }

            item {
                ActivationDeviceCard(etsAppInfo = etsAppInfo)
            }

            // 基础权限申请区域：仅保留本地文件访问权限。
            item {
                SegmentedColumn(
                    title = "系统基础权限",
                    items = listOf(
                        {
                            ExpressiveSegmentedListItem(
                                index = 0,
                                count = 1,
                                onClick = {
                                    if (!hasFilesPerm) PermissionsHelper.requestAllFilesAccess(context)
                                    else Toast.makeText(context, "已经拥有全文件访问权限啦！", Toast.LENGTH_SHORT).show()
                                },
                                headlineContent = { Text("全文件访问") },
                                supportingContent = {
                                    Text(
                                        if (hasFilesPerm) "已授权" else "读取设备底层目录结构",
                                        color = if (hasFilesPerm) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                },
                                leadingContent = {
                                    Icon(
                                        if (hasFilesPerm) Icons.Default.CheckCircle else Icons.Default.FolderSpecial,
                                        contentDescription = null,
                                        tint = if (hasFilesPerm) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                    )
                                },
                            )
                        },
                    ),
                )
            }

            // 动态展开的授权模式卡片区
            item {
                SegmentedColumn(
                    title = "选择工作授权模式",
                    items = activationModes.mapIndexed { index, mode ->
                        {
                            val isSelected = currentMode == mode
                            val transitionState = remember { MutableTransitionState(isSelected) }.apply {
                                targetState = isSelected
                            }
                            ExpressiveSegmentedListItem(
                                index = index,
                                count = activationModes.size,
                                onClick = { onModeSelected(mode) },
                                headlineContent = {
                                    Text(
                                        mode.title,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                    )
                                },
                                leadingContent = {
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = null,
                                        colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary),
                                    )
                                },
                                supportingContent = {
                                    Column {
                                        Text(mode.desc, style = MaterialTheme.typography.bodySmall)
                                        AnimatedVisibility(
                                            visibleState = transitionState,
                                            enter = expandVertically() + fadeIn(),
                                            exit = shrinkVertically() + fadeOut(),
                                        ) {
                                            Column {
                                                FeThinDivider(modifier = Modifier.padding(vertical = 12.dp))
                                                when (mode) {
                                                    ActivationMode.SHIZUKU -> FeShizukuActivationPanel(
                                                        shizukuState = shizukuState,
                                                        onRequestPermission = {
                                                            if (shizukuState.isRunning) {
                                                                if (ShizukuManager.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
                                                                    Toast.makeText(context, "Shizuku 已授权！", Toast.LENGTH_SHORT).show()
                                                                } else {
                                                                    pendingPermissionRequest = true
                                                                    ShizukuManager.requestPermission(ShizukuManager.REQUEST_CODE_SHIZUKU_PERMISSION)
                                                                }
                                                            } else {
                                                                ShizukuManager.openShizukuApp(context)
                                                            }
                                                        },
                                                        onOpenShizuku = { ShizukuManager.openShizukuApp(context) },
                                                    )
                                                    ActivationMode.ROOT -> FeRootActivationPanel(context)
                                                    ActivationMode.DIRECT_READ -> FeDirectReadActivationPanel(hasLocalBasicPermissions)
                                                    ActivationMode.CLOUD -> FeCloudActivationPanel(
                                                        context = context,
                                                        isLoggedIn = cloudLoggedIn,
                                                        phone = cloudPhone,
                                                        onCloudAuthChanged = { viewModel.refresh(force = true) },
                                                        onNavigateToCloudActivation = onNavigateToCloudActivation,
                                                        onNavigateToRead = onNavigateToRead,
                                                    )
                                                    else -> Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically,
                                                    ) {
                                                        Text("需要手动确认以完成激活", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                        Button(
                                                            onClick = { Toast.makeText(context, "正在请求 ${mode.title} 激活...", Toast.LENGTH_SHORT).show() },
                                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary),
                                                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                                                            modifier = Modifier.height(32.dp),
                                                        ) { Text("执行激活", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold) }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                },
                            )
                        }
                    },
                )
            }
            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

@Composable
private fun ActivationDeviceCard(etsAppInfo: Pair<Boolean, String>?) {
    val themePrimaryColor = MaterialTheme.colorScheme.primary
    val successColor = MaterialTheme.colorScheme.primary
    val errorColor = MaterialTheme.colorScheme.error

    TonalCard(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        containerColor = MaterialTheme.colorScheme.surfaceBright,
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Smartphone, null, tint = themePrimaryColor)
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text(
                            "DEVICE",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            Build.MODEL ?: "Unknown",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "OS",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "Android ${Build.VERSION.RELEASE}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            FeThinDivider()

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Memory,
                        null,
                        tint = if (etsAppInfo?.first == true) successColor else errorColor
                    )
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text(
                            "ETS应用",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = if (etsAppInfo?.first == true) "已安装" else "未安装",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (etsAppInfo?.first == true) successColor else errorColor
                        )
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "VERSION",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = etsAppInfo?.second?.takeIf { it.isNotEmpty() } ?: "未知",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun FeModeStatusCard(
    currentMode: ActivationMode,
    shizukuState: ShizukuState,
    hasAllBasicPermissions: Boolean,
    cloudLoggedIn: Boolean,
    cloudPhone: String?,
    context: android.content.Context
) {
    var isRootAvailable by remember { mutableStateOf(false) }
    var isDirectReadAvailable by remember { mutableStateOf(false) }
    val refreshScope = rememberCoroutineScope()

    suspend fun refreshAvailability() {
        val availability = withContext(Dispatchers.IO) {
            RootManager.isRootAvailable() to ZWCHelper.isDirectReadAvailable()
        }
        isRootAvailable = availability.first
        isDirectReadAvailable = availability.second
    }

    // Root and direct-read checks can start processes and touch external storage.
    // Do not run them while the destination scene is being composed.
    LaunchedEffect(Unit) {
        refreshAvailability()
    }

    var hasPaused by remember { mutableStateOf(false) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) {
                hasPaused = true
            } else if (event == Lifecycle.Event.ON_RESUME && hasPaused) {
                refreshScope.launch {
                    refreshAvailability()
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    
    val errorRedColor = MaterialTheme.colorScheme.error
    
    val statusColor = when (currentMode) {
        ActivationMode.DEFAULT -> MaterialTheme.colorScheme.outline
        ActivationMode.SHIZUKU -> if (shizukuState.isRunning && shizukuState.permissionGranted) MaterialTheme.colorScheme.primary else errorRedColor
        ActivationMode.ROOT -> if (hasAllBasicPermissions && isRootAvailable) MaterialTheme.colorScheme.primary else errorRedColor
        ActivationMode.DIRECT_READ -> if (hasAllBasicPermissions && isDirectReadAvailable) MaterialTheme.colorScheme.primary else errorRedColor
        ActivationMode.CLOUD -> if (cloudLoggedIn) MaterialTheme.colorScheme.primary else errorRedColor
    }
    
    val isActive = statusColor == MaterialTheme.colorScheme.primary
    val statusContainerColor = if (isActive) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.errorContainer
    }
    
    val (statusTitle, statusDesc, statusDetail) = when (currentMode) {
        ActivationMode.DEFAULT -> Triple("未激活", "尚未配置核心终端行为", null)
        ActivationMode.SHIZUKU -> {
            val title = if (shizukuState.isRunning) "Shizuku 运行中" else "Shizuku 未运行"
            val desc = when {
                !shizukuState.isRunning -> "底层服务未连接"
                !shizukuState.permissionGranted -> "等待权限授权"
                else -> "一切准备就绪"
            }
            val detail = if (shizukuState.isRunning) {
                val uidStr = when (shizukuState.uid) {
                    0 -> "Root (0)"
                    2000 -> "ADB (2000)"
                    else -> shizukuState.uid.toString()
                }
                "v${shizukuState.version} • UID: $uidStr"
            } else null
            Triple(title, desc, detail)
        }
        ActivationMode.ROOT -> {
            val title = "Root 模式"
            val desc = when {
                !hasAllBasicPermissions -> "需要基础系统权限"
                !isRootAvailable -> "未获取 Root 权限"
                else -> "一切准备就绪"
            }
            Triple(title, desc, null)
        }
        ActivationMode.DIRECT_READ -> {
            val title = "漏洞直读模式"
            val desc = if (hasAllBasicPermissions) "通过零宽字符漏洞绕过限制" else "需要全文件访问权限"
            Triple(title, desc, null)
        }
        ActivationMode.CLOUD -> {
            val title = if (cloudLoggedIn) "云端模式已激活" else "云端模式未激活"
            val desc = if (cloudLoggedIn) {
                "（${maskPhoneNumber(cloudPhone)}）手机号已登录"
            } else {
                "需要登录 ETS100 账号"
            }
            Triple(title, desc, null)
        }
    }
    
    TonalCard(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        containerColor = MaterialTheme.colorScheme.surfaceBright,
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.padding(20.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(48.dp).background(statusContainerColor, CircleShape), Alignment.Center) {
                    Icon(
                        imageVector = if (isActive) Icons.Default.CheckCircle else Icons.Default.Warning,
                        contentDescription = null,
                        tint = statusColor,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Spacer(Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = currentMode.icon,
                            contentDescription = null,
                            tint = statusColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(statusTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(statusDesc, style = MaterialTheme.typography.bodySmall, color = statusColor)

                    if (statusDetail != null) {
                        Spacer(Modifier.height(4.dp))
                        Text(statusDetail, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                if (!isActive && currentMode == ActivationMode.SHIZUKU && !shizukuState.isRunning) {
                    FilledTonalButton(
                        onClick = { ShizukuManager.openShizukuDownloadPage(context) },
                        colors = ButtonDefaults.filledTonalButtonColors(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer)
                    ) {
                        Text("去下载", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun FeDirectReadActivationPanel(hasAllBasicPermissions: Boolean) {
    val context = LocalContext.current
    var isDirectReadAvailable by remember { mutableStateOf(false) }
    var isTesting by remember { mutableStateOf(false) }
    val refreshScope = rememberCoroutineScope()

    suspend fun refreshAvailability() {
        isDirectReadAvailable = withContext(Dispatchers.IO) {
            ZWCHelper.isDirectReadAvailable()
        }
    }

    LaunchedEffect(Unit) { refreshAvailability() }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refreshScope.launch { refreshAvailability() }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    if (hasAllBasicPermissions) {
        if (isDirectReadAvailable) {
            OutlinedButton(
                onClick = {},
                enabled = false,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                colors = ButtonDefaults.outlinedButtonColors(
                    disabledContentColor = MaterialTheme.colorScheme.primary,
                ),
                modifier = Modifier.fillMaxWidth().height(40.dp),
            ) {
                Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("漏洞直读已就绪", fontWeight = FontWeight.Bold)
            }
        } else {
            Button(
                onClick = {
                    if (isTesting) return@Button
                    isTesting = true
                    refreshScope.launch {
                        val available = withContext(Dispatchers.IO) {
                            ZWCHelper.isDirectReadAvailable()
                        }
                        isDirectReadAvailable = available
                        isTesting = false
                        Toast.makeText(
                            context,
                            if (available) "漏洞直读测试通过！" else "暂不支持漏洞直读，请授权全文件访问或使用其他模式",
                            if (available) Toast.LENGTH_SHORT else Toast.LENGTH_LONG,
                        ).show()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(40.dp),
                enabled = !isTesting,
            ) {
                if (isTesting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp,
                    )
                } else {
                    Icon(Icons.Default.BugReport, null, modifier = Modifier.size(18.dp))
                }
                Spacer(Modifier.width(8.dp))
                Text(if (isTesting) "测试中..." else "测试漏洞直读", fontWeight = FontWeight.Bold)
            }
        }
    } else {
        Text(
            text = "请先完成文件访问授权，然后返回此处测试漏洞",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun FeShizukuStatusCard(shizukuState: ShizukuState, context: android.content.Context) {
    val isRunning = shizukuState.isRunning
    val statusColor = if (isRunning) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error

    TonalCard(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        containerColor = MaterialTheme.colorScheme.surfaceBright,
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(48.dp)
                        .background(if (isRunning) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer, CircleShape),
                    Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isRunning) Icons.Default.CheckCircle else Icons.Default.Warning,
                        contentDescription = null,
                        tint = statusColor,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Spacer(Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Shizuku 核心状态", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(Modifier.height(4.dp))
                    Text(shizukuState.getRuntimeTypeName(), style = MaterialTheme.typography.bodySmall, color = statusColor)

                    if (isRunning) {
                        Spacer(Modifier.height(4.dp))
                        val uidStr = when (shizukuState.uid) {
                            0 -> "Root (0)"
                            2000 -> "ADB (2000)"
                            else -> shizukuState.uid.toString()
                        }
                        Text("版本: v${shizukuState.version} • UID: $uidStr", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                if (!isRunning) {
                    FilledTonalButton(
                        onClick = { ShizukuManager.openShizukuDownloadPage(context) },
                        colors = ButtonDefaults.filledTonalButtonColors(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer)
                    ) {
                        Text("去下载", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun FeShizukuActivationPanel(
    shizukuState: ShizukuState,
    onRequestPermission: () -> Unit,
    onOpenShizuku: () -> Unit
) {
    val successColor = MaterialTheme.colorScheme.primary

    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = when {
                    !shizukuState.isRunning -> "底层服务未启动，无法工作"
                    shizukuState.permissionGranted -> "底层通信已就绪，享受极客体验"
                    else -> "需要授权以桥接系统底层 API"
                },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.height(16.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (!shizukuState.isRunning) {
                Button(
                    onClick = onOpenShizuku,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary),
                    modifier = Modifier.weight(1f).height(40.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.OpenInNew, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("打开 Shizuku 去启动", fontWeight = FontWeight.Bold)
                }
            } else {
                if (shizukuState.permissionGranted) {
                    OutlinedButton(
                        onClick = { },
                        enabled = false,
                        border = BorderStroke(1.dp, successColor),
                        colors = ButtonDefaults.outlinedButtonColors(disabledContentColor = successColor),
                        modifier = Modifier.weight(1f).height(40.dp)
                    ) {
                        Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("已授权，一切准备就绪", fontWeight = FontWeight.Bold)
                    }
                } else {
                    Button(
                        onClick = onRequestPermission,
                        colors = ButtonDefaults.buttonColors(containerColor = successColor, contentColor = MaterialTheme.colorScheme.onPrimary),
                        modifier = Modifier.weight(1f).height(40.dp)
                    ) {
                        Icon(Icons.Default.Security, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("点击请求应用授权", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun FeRootActivationPanel(context: android.content.Context) {
    val successColor = MaterialTheme.colorScheme.primary
    val errorColor = MaterialTheme.colorScheme.error
    
    var isRootAvailable by remember { mutableStateOf(false) }
    var isChecking by remember { mutableStateOf(false) }
    val refreshScope = rememberCoroutineScope()

    suspend fun refreshRootAvailability() {
        isRootAvailable = withContext(Dispatchers.IO) {
            RootManager.isRootAvailable()
        }
    }

    LaunchedEffect(Unit) {
        refreshRootAvailability()
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refreshScope.launch {
                    refreshRootAvailability()
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = when {
                    isChecking -> "正在检测 Root 权限..."
                    isRootAvailable -> "Root 权限已获取，享受最高权限"
                    else -> "设备未获取 Root 权限"
                },
                style = MaterialTheme.typography.labelSmall,
                color = when {
                    isChecking -> MaterialTheme.colorScheme.onSurfaceVariant
                    isRootAvailable -> successColor
                    else -> errorColor
                }
            )
        }
        Spacer(Modifier.height(16.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (isRootAvailable) {
                OutlinedButton(
                    onClick = { },
                    enabled = false,
                    border = BorderStroke(1.dp, successColor),
                    colors = ButtonDefaults.outlinedButtonColors(disabledContentColor = successColor),
                    modifier = Modifier.weight(1f).height(40.dp)
                ) {
                    Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Root 已就绪", fontWeight = FontWeight.Bold)
                }
            } else {
                Button(
                    onClick = {
                        if (isChecking) return@Button
                        isChecking = true
                        refreshScope.launch {
                            val available = withContext(Dispatchers.IO) {
                                RootManager.clearCache()
                                RootManager.isRootAvailable()
                            }
                            isRootAvailable = available
                            isChecking = false

                            if (!available) {
                                Toast.makeText(context, "未检测到 Root 权限...", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "检测到 Root 权限！", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error, contentColor = MaterialTheme.colorScheme.onError),
                    modifier = Modifier.weight(1f).height(40.dp),
                    enabled = !isChecking
                ) {
                    if (isChecking) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = MaterialTheme.colorScheme.onError,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(Icons.Default.Refresh, null, modifier = Modifier.size(18.dp))
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(if (isChecking) "检测中..." else "检测 Root 权限", fontWeight = FontWeight.Bold)
                }
            }
        }
        
        if (!isRootAvailable) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = "提示：Root 权限需要通过 Magisk 或 SuperSU 等工具获取，并在授权管理中允许本应用使用 Root",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}

/**
 * 云端模式激活面板
 * 喵~ 用于显示登录状态和跳转到登录页面喵！
 */
@Composable
fun FeCloudActivationPanel(
    context: android.content.Context,
    isLoggedIn: Boolean,
    phone: String?,
    onCloudAuthChanged: () -> Unit,
    onNavigateToCloudActivation: () -> Unit,
    onNavigateToRead: () -> Unit,
    viewModel: ActivationViewModel = hiltViewModel(),
) {
    val cloudColor = MaterialTheme.colorScheme.tertiary
    val successColor = MaterialTheme.colorScheme.primary
    
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = when {
                    isLoggedIn -> "云端模式已激活"
                    else -> "云端模式未激活"
                },
                style = MaterialTheme.typography.labelSmall,
                color = if (isLoggedIn) successColor else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (isLoggedIn && !phone.isNullOrBlank()) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = "（${maskPhoneNumber(phone)}）手机号已登录",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Spacer(Modifier.height(16.dp))
        
        if (isLoggedIn) {
            // 已登录，显示进入云端首页按钮
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(
                    onClick = {
                        onNavigateToRead()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = cloudColor, contentColor = MaterialTheme.colorScheme.onTertiary),
                    modifier = Modifier.weight(1f).height(40.dp),
                ) {
                    Icon(Icons.Default.Cloud, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("云端", fontWeight = FontWeight.Bold, maxLines = 1)
                }

                OutlinedButton(
                    onClick = {
                        ETS100AuthManager.logout(context)
                        onCloudAuthChanged()
                        Toast.makeText(context, "已退出登录", Toast.LENGTH_SHORT).show()
                    },
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.weight(1f).height(40.dp),
                ) {
                    Icon(Icons.AutoMirrored.Filled.Logout, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("退出", fontWeight = FontWeight.Bold, maxLines = 1)
                }
            }
        } else {
            // 未登录，显示登录按钮
            Button(
                onClick = {
                    // 跳转到登录页面
                    onNavigateToCloudActivation()
                },
                colors = ButtonDefaults.buttonColors(containerColor = cloudColor, contentColor = MaterialTheme.colorScheme.onTertiary),
                modifier = Modifier.fillMaxWidth().height(40.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.Login, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("登录 ETS100 账号", fontWeight = FontWeight.Bold)
            }
        }
        
        if (!isLoggedIn) {
            Spacer(Modifier.height(8.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Cloud,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "云端模式说明",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "通过 ETS100 云端 API 在线获取作业列表和答案，无需本地文件",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

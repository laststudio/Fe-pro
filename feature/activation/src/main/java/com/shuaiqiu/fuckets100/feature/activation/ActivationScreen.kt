package com.shuaiqiu.fuckets100.feature.activation

import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
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
import com.shuaiqiu.fuckets100.core.designsystem.TopBarBackButton
import com.shuaiqiu.fuckets100.core.designsystem.expressiveTopAppBarColors

import com.shuaiqiu.fuckets100.ActivationMode
import com.shuaiqiu.fuckets100.PermissionsHelper
import com.shuaiqiu.fuckets100.ShizukuManager
import com.shuaiqiu.fuckets100.ShizukuState
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
    var statusRefreshRevision by remember { mutableIntStateOf(0) }

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
    DisposableEffect(Unit) {
        val permissionListener = ShizukuManager.addPermissionResultListener { requestCode, grantResult ->
            if (requestCode == ShizukuManager.REQUEST_CODE_SHIZUKU_PERMISSION) {
                pendingPermissionRequest = false
                if (grantResult == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(context, "Shizuku 权限授权成功！", Toast.LENGTH_SHORT).show()
                    statusRefreshRevision++
                    onModeSelected(ActivationMode.SHIZUKU)
                } else {
                    Toast.makeText(context, "Shizuku 权限被拒绝了", Toast.LENGTH_SHORT).show()
                }
            }
        }
        onDispose {
            ShizukuManager.removePermissionResultListener(permissionListener)
        }
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
            // 当前模式状态卡片：单独作为页面首个信息区块，和设备信息明确分组。
            item {
                ActivationSectionLabel(text = "当前授权状态")
                FeModeStatusCard(
                    currentMode = currentMode,
                    shizukuState = shizukuState,
                    hasAllBasicPermissions = hasLocalBasicPermissions,
                    cloudLoggedIn = cloudLoggedIn,
                    cloudPhone = cloudPhone,
                    context = context,
                    refreshRevision = statusRefreshRevision,
                )
            }

            item {
                ActivationSectionLabel(text = "设备与 e听说")
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
                                                        onStatusRefresh = { statusRefreshRevision++ },
                                                        onRequestPermission = {
                                                            if (shizukuState.isRunning) {
                                                                if (ShizukuManager.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
                                                                    Toast.makeText(context, "Shizuku 已授权！", Toast.LENGTH_SHORT).show()
                                                                    statusRefreshRevision++
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
                                                    ActivationMode.ROOT -> FeRootActivationPanel(
                                                        context = context,
                                                        onAvailabilityChanged = { statusRefreshRevision++ },
                                                    )
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
internal fun ActivationSectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, bottom = 8.dp),
    )
}

@Composable
internal fun ActivationDeviceCard(etsAppInfo: Pair<Boolean, String>?) {
    val installed = etsAppInfo?.first == true
    val statusColor = if (installed) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.error
    }

    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        ) {
            DeviceInfoRow(
                icon = Icons.Default.Smartphone,
                iconTint = MaterialTheme.colorScheme.primary,
                title = Build.MODEL ?: "Unknown",
                label = "设备",
                trailingLabel = "Android ${Build.VERSION.RELEASE}",
                trailingTitle = "系统版本",
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
            DeviceInfoRow(
                icon = Icons.Default.Memory,
                iconTint = statusColor,
                title = if (installed) "已安装" else "未安装",
                label = "e听说",
                titleColor = statusColor,
                trailingLabel = etsAppInfo?.second?.takeIf { it.isNotEmpty() } ?: "未知",
                trailingTitle = "版本",
            )
        }
    }
}

@Composable
internal fun DeviceInfoRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    title: String,
    label: String,
    trailingLabel: String,
    trailingTitle: String,
    titleColor: Color = MaterialTheme.colorScheme.onSurface,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = titleColor, maxLines = 1)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(trailingTitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(trailingLabel, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

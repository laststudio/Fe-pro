package com.shuaiqiu.fuckets100.feature.activation

import android.content.pm.PackageManager
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import com.shuaiqiu.fuckets100.ActivationMode
import com.shuaiqiu.fuckets100.RootManager
import com.shuaiqiu.fuckets100.ShizukuManager
import com.shuaiqiu.fuckets100.ShizukuState
import com.shuaiqiu.fuckets100.ZWCHelper
import com.shuaiqiu.fuckets100.maskPhoneNumber
@Composable
internal fun FeModeStatusCard(
    currentMode: ActivationMode,
    shizukuState: ShizukuState,
    hasAllBasicPermissions: Boolean,
    cloudLoggedIn: Boolean,
    cloudPhone: String?,
    context: android.content.Context,
    refreshRevision: Int = 0,
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
    LaunchedEffect(refreshRevision) {
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

    val shizukuPermissionGranted = shizukuState.permissionGranted ||
        ShizukuManager.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
    val statusColor = when (currentMode) {
        ActivationMode.DEFAULT -> MaterialTheme.colorScheme.outline
        ActivationMode.SHIZUKU -> if (shizukuState.isRunning && shizukuPermissionGranted) MaterialTheme.colorScheme.primary else errorRedColor
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
                !shizukuPermissionGranted -> "等待权限授权"
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

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = statusContainerColor,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = if (isActive) Icons.Default.CheckCircle else Icons.Default.Warning,
                contentDescription = null,
                tint = statusColor,
                modifier = Modifier.size(28.dp),
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(currentMode.icon, contentDescription = null, tint = statusColor, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(5.dp))
                    Text(statusTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.height(3.dp))
                Text(statusDesc, style = MaterialTheme.typography.bodySmall, color = statusColor)
                if (statusDetail != null) {
                    Spacer(Modifier.height(2.dp))
                    Text(statusDetail, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (!isActive && currentMode == ActivationMode.SHIZUKU && !shizukuState.isRunning) {
                Spacer(Modifier.width(8.dp))
                FilledTonalButton(
                    onClick = { ShizukuManager.openShizukuDownloadPage(context) },
                    contentPadding = PaddingValues(horizontal = 12.dp),
                ) { Text("去下载", style = MaterialTheme.typography.labelLarge) }
            }
        }
    }
}

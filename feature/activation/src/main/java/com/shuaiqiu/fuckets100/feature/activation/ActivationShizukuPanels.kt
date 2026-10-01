package com.shuaiqiu.fuckets100.feature.activation

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
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
import com.shuaiqiu.fuckets100.core.designsystem.TonalCard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import com.shuaiqiu.fuckets100.RootManager
import com.shuaiqiu.fuckets100.ShizukuManager
import com.shuaiqiu.fuckets100.ShizukuState
@Composable
internal fun FeShizukuStatusCard(shizukuState: ShizukuState, context: android.content.Context) {
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
internal fun FeShizukuActivationPanel(
    shizukuState: ShizukuState,
    onStatusRefresh: () -> Unit = {},
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
internal fun FeRootActivationPanel(
    context: android.content.Context,
    onAvailabilityChanged: () -> Unit = {},
) {
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
                            onAvailabilityChanged()

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

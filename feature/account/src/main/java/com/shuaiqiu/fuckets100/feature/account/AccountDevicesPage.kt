package com.shuaiqiu.fuckets100.feature.account

import com.shuaiqiu.fuckets100.feature.shared.AccountViewModel

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.json.JSONObject

import com.shuaiqiu.fuckets100.FeAccountApiException
import com.shuaiqiu.fuckets100.FeAccountManager
import com.shuaiqiu.fuckets100.FeAccountProfileApi
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountDevicesPage(
    manager: FeAccountManager = hiltViewModel<AccountViewModel>().manager,
    onBack: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var devices by remember { mutableStateOf<List<JSONObject>?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var pendingRevoke by remember { mutableStateOf<JSONObject?>(null) }

    fun refresh(force: Boolean = false) {
        scope.launch {
            error = null
            runCatching { manager.accountDevices(force = force) }
                .onSuccess { devices = it }
                .onFailure {
                    if (it is FeAccountApiException && it.error.statusCode == 401) {
                        manager.revalidateOnline()
                    } else {
                        error = it.message ?: "加载设备失败"
                    }
                }
        }
    }

    LaunchedEffect(Unit) { refresh() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("我的设备") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { refresh(force = true) }) {
                        Icon(Icons.Default.Refresh, "刷新设备")
                    }
                },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(20.dp)) {
            if (devices == null && error == null) {
                Box(Modifier.fillMaxWidth().padding(top = 80.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                error?.let {
                    Text(it, color = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.height(12.dp))
                }
                val list = devices.orEmpty()
                if (list.isEmpty()) {
                    Text("暂无设备", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    list.forEach { device ->
                        AccountDeviceCard(
                            device = device,
                            onRevoke = { pendingRevoke = device },
                        )
                        Spacer(Modifier.height(12.dp))
                    }
                }
            }
        }
    }

    pendingRevoke?.let { device ->
        AlertDialog(
            onDismissRequest = { pendingRevoke = null },
            title = { Text("下线设备？") },
            text = { Text("将下线「${device.optString("display_name", "未知设备")}」的全部登录会话。") },
            confirmButton = {
                TextButton(onClick = {
                    val id = device.optInt("id", -1)
                    pendingRevoke = null
                    scope.launch {
                    runCatching { FeAccountProfileApi().revokeDevice(manager.requireAccessToken(), id) }
                            .onSuccess {
                                manager.invalidateAccountData()
                                refresh(force = true)
                            }
                            .onFailure {
                                if (it is FeAccountApiException && it.error.statusCode == 401) {
                                    manager.revalidateOnline()
                                } else {
                                    error = it.message ?: "下线失败"
                                }
                            }
                    }
                }) { Text("下线") }
            },
            dismissButton = {
                TextButton(onClick = { pendingRevoke = null }) { Text("取消") }
            },
        )
    }
}

@Composable
fun AccountDeviceCard(device: JSONObject, onRevoke: () -> Unit) {
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    device.optString("display_name", "未知设备"),
                    style = MaterialTheme.typography.titleSmall,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "${device.optString("platform", "android")} · ${device.optString("device_id", "")}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    "状态：${if (device.optString("status") == "active") "在线" else "已下线"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (device.optString("status") == "active") {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
            if (device.optString("status") == "active") {
                TextButton(onClick = onRevoke) { Text("下线") }
            }
        }
    }
}

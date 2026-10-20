package com.shuaiqiu.fuckets100.feature.account

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.shuaiqiu.fuckets100.cachedSessionOrNull
import com.shuaiqiu.fuckets100.core.designsystem.SegmentedColumn
import com.shuaiqiu.fuckets100.feature.shared.AccountViewModel
import org.json.JSONObject

@Composable
internal fun AccountRecordsPage(
    kind: AccountRecordKind,
    onBack: () -> Unit,
    viewModel: AccountRecordsViewModel = hiltViewModel(),
    accountViewModel: AccountViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val gateState by accountViewModel.gateState.collectAsState()
    val currentSession = gateState.cachedSessionOrNull
    var selectedRecord by remember { mutableStateOf<JSONObject?>(null) }
    var pendingRevoke by remember { mutableStateOf<JSONObject?>(null) }
    val isDevice = kind == AccountRecordKind.Device
    val recordLabel = if (isDevice) "设备" else "会话"
    val busy = state.isRefreshing || state.isRevoking

    LaunchedEffect(kind, viewModel) { viewModel.refresh(kind) }
    AccountPageScaffold(
        title = if (isDevice) "我的设备" else "登录会话",
        onBack = onBack,
        isRefreshing = state.isRefreshing,
        onRefresh = { viewModel.refresh(kind, force = true) },
    ) {
        state.error?.let { AccountMessage(it, isError = true) }
        val records = state.records
        when {
            records == null && state.error == null -> AccountMessage("正在加载$recordLabel…")
            records == null -> AccountMessage("下拉刷新可重新加载。")
            records.isEmpty() -> AccountMessage("暂无$recordLabel")
            else -> SegmentedColumn(
                items = records.mapIndexed { index, record ->
                    {
                        val isCurrent = if (isDevice) {
                            isCurrentAccountRecord(record.optString("device_id"), currentSession?.deviceId)
                        } else {
                            isCurrentAccountRecord(record.optString("session_id"), currentSession?.sessionId)
                        }
                        val summary = if (isDevice) {
                            accountPlatformName(record.optString("platform"))
                        } else {
                            record.optString("client_ip").ifBlank { "未知地址" }
                        }
                        AccountActionRow(
                            index = index,
                            count = records.size,
                            title = recordTitle(record, isDevice),
                            icon = if (isDevice) Icons.Default.Devices else Icons.Default.History,
                            supportingText = "$summary · ${accountRecordStatus(record.optString("status"))}",
                            trailingText = if (isCurrent) "当前" else null,
                            onClick = { selectedRecord = record },
                        )
                    }
                },
            )
        }
    }
    selectedRecord?.let { record ->
        val fields = if (isDevice) {
            listOf("平台" to accountPlatformName(record.optString("platform")), "设备 ID" to record.optString("device_id"))
        } else {
            listOf(
                "客户端" to record.optString("user_agent"),
                "版本代码" to record.optInt("app_version_code", 0).takeIf { it > 0 }?.toString().orEmpty(),
                "IP 地址" to record.optString("client_ip"),
            )
        }
        AlertDialog(
            onDismissRequest = { selectedRecord = null },
            title = { Text(recordTitle(record, isDevice)) },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    AccountDetailField("状态", accountRecordStatus(record.optString("status")))
                    fields.forEach { (label, value) -> AccountDetailField(label, value.ifBlank { "未知" }) }
                }
            },
            confirmButton = { TextButton(onClick = { selectedRecord = null }) { Text("关闭") } },
            dismissButton = if (record.optString("status") == "active") {
                {
                    TextButton(enabled = !busy, onClick = { selectedRecord = null; pendingRevoke = record }) {
                        Text("下线$recordLabel", color = MaterialTheme.colorScheme.error)
                    }
                }
            } else null,
        )
    }
    pendingRevoke?.let { record ->
        AlertDialog(
            onDismissRequest = { if (!state.isRevoking) pendingRevoke = null },
            title = { Text("下线$recordLabel？") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        if (isDevice) "将下线「${recordTitle(record, true)}」的全部登录会话。"
                        else "这个会话需要重新登录后才能继续使用。",
                    )
                    state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !busy,
                    onClick = { viewModel.revoke(kind, record.optInt("id", -1)) { pendingRevoke = null } },
                ) {
                    if (state.isRevoking) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                    else Text("下线", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(enabled = !state.isRevoking, onClick = { pendingRevoke = null }) { Text("取消") } },
        )
    }
}

private fun recordTitle(record: JSONObject, isDevice: Boolean): String = if (isDevice) {
    record.optString("display_name").ifBlank { "未知设备" }
} else {
    accountClientName(record.optString("user_agent"))
}


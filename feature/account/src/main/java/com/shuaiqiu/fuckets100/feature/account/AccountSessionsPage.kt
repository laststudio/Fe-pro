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
fun AccountSessionsPage(
    manager: FeAccountManager = hiltViewModel<AccountViewModel>().manager,
    onBack: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var sessions by remember { mutableStateOf<List<JSONObject>?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    fun refresh(force: Boolean = false) {
        scope.launch {
            error = null
            runCatching { manager.accountSessions(force = force) }
                .onSuccess { sessions = it }
                .onFailure {
                    if (it is FeAccountApiException && it.error.statusCode == 401) {
                        manager.revalidateOnline()
                    } else {
                        error = it.message ?: "加载会话失败"
                    }
                }
        }
    }

    LaunchedEffect(Unit) { refresh() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("登录会话") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { refresh(force = true) }) {
                        Icon(Icons.Default.Refresh, "刷新会话")
                    }
                },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(20.dp)) {
            if (sessions == null && error == null) {
                Box(Modifier.fillMaxWidth().padding(top = 80.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                error?.let {
                    Text(it, color = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.height(12.dp))
                }
                val list = sessions.orEmpty()
                if (list.isEmpty()) {
                    Text("暂无会话", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    list.forEach { session ->
                        AccountSessionCard(
                            session = session,
                            onRevoke = {
                                val id = session.optInt("id", -1)
                                scope.launch {
                                    runCatching { FeAccountProfileApi().revokeSession(manager.requireAccessToken(), id) }
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
                            },
                        )
                        Spacer(Modifier.height(12.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun AccountSessionCard(session: JSONObject, onRevoke: () -> Unit) {
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    session.optString("user_agent", "未知客户端").take(40),
                    style = MaterialTheme.typography.titleSmall,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "版本 v${session.optInt("app_version_code", 0)} · ${session.optString("client_ip", "?")}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    if (session.optString("status") == "active") "当前在线" else "已下线",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (session.optString("status") == "active") {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
            if (session.optString("status") == "active") {
                TextButton(onClick = onRevoke) { Text("下线") }
            }
        }
    }
}

package com.shuaiqiu.fuckets100.feature.account

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.shuaiqiu.fuckets100.cachedSessionOrNull
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

import com.shuaiqiu.fuckets100.FeAccountApiException
import com.shuaiqiu.fuckets100.FeAccountGateState
import com.shuaiqiu.fuckets100.FeAccountProfileApi
import com.shuaiqiu.fuckets100.core.designsystem.SettingsListItem
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeAccountScreen(
    onBack: () -> Unit,
    onNavigateToDevices: () -> Unit,
    onNavigateToSessions: () -> Unit,
    onLogoutNavigate: () -> Unit,
    viewModel: AccountViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val manager = viewModel.manager
    val state by manager.gateState.collectAsState()
    val session = state.cachedSessionOrNull
    val scope = rememberCoroutineScope()

    if (state is FeAccountGateState.LoginRequired) {
        FeLoginScreen(manager)
        return
    }

    if (session == null) {
        AccountOfflineMessage(
            message = when (val current = state) {
                is FeAccountGateState.Checking -> "正在连接 LA Studio…"
                is FeAccountGateState.Offline -> current.message
                is FeAccountGateState.Blocked -> current.message
                is FeAccountGateState.Ready -> ""
                FeAccountGateState.LoginRequired -> ""
            },
            showProgress = state is FeAccountGateState.Checking,
            onRetry = { scope.launch { manager.checkGate() } },
            onBack = onBack,
        )
        return
    }

    val cachedSession = session

    var showChangePassword by remember { mutableStateOf(false) }
    var showBindings by remember { mutableStateOf(false) }
    var showBindEts100 by remember { mutableStateOf(false) }
    var showUnbindConfirm by remember { mutableStateOf(false) }
    var bindEts100Error by remember { mutableStateOf<String?>(null) }
    var isBindingEts100 by remember { mutableStateOf(false) }
    var isUnbindingEts100 by remember { mutableStateOf(false) }
    var bindings by remember { mutableStateOf<FeAccountProfileApi.AccountBindings?>(null) }
    var bindingsError by remember { mutableStateOf<String?>(null) }
    var changePasswordError by remember { mutableStateOf<String?>(null) }
    var logoutError by remember { mutableStateOf<String?>(null) }

    fun refreshBindings() {
        scope.launch {
            bindingsError = null
            runCatching { manager.accountBindings(force = true) }
                .onSuccess { bindings = it }
                .onFailure {
                    if (it is FeAccountApiException && it.error.statusCode == 401) {
                        manager.revalidateOnline()
                    } else {
                        bindingsError = it.message ?: "加载绑定信息失败"
                    }
                }
        }
    }

    LaunchedEffect(cachedSession, state is FeAccountGateState.Ready) {
        if (state is FeAccountGateState.Ready) {
            refreshBindings()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("个人中心") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
            .padding(20.dp),
        ) {
            if (state is FeAccountGateState.Offline || state is FeAccountGateState.Checking) {
                Text(
                    text = if (state is FeAccountGateState.Checking) "正在连接 LA Studio…" else "已离线",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.labelMedium,
                )
                Spacer(Modifier.height(12.dp))
            }
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AccountInitialAvatar(
                    username = cachedSession.user.username,
                    size = 84,
                    avatarUrl = cachedSession.user.avatarUrl,
                    userId = cachedSession.user.id,
                )
                Spacer(Modifier.width(18.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        cachedSession.user.username,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        "LA Studio 用户 #${cachedSession.user.id}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        cachedSession.user.tierDisplayName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    SettingsListItem(
                        icon = Icons.Default.Lock,
                        title = "修改密码",
                        sub = "定期更换密码更安全",
                        onClick = {
                            changePasswordError = null
                            showChangePassword = true
                        },
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    SettingsListItem(
                        icon = Icons.Default.Email,
                        title = "账号绑定",
                        sub = bindingsError
                            ?: bindingsSummary(bindings),
                        onClick = { showBindings = true },
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    SettingsListItem(
                        icon = Icons.Default.Phone,
                        title = "我的设备",
                        sub = "管理已登录的设备",
                        onClick = onNavigateToDevices,
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    SettingsListItem(
                        icon = Icons.Default.Settings,
                        title = "登录会话",
                        sub = "查看和下线登录会话",
                        onClick = onNavigateToSessions,
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    SettingsListItem(
                        icon = Icons.AutoMirrored.Filled.OpenInNew,
                        title = "前往官网个人中心",
                        sub = "前往 lastudio.cc 管理绑定、会员与账号安全",
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(ACCOUNT_CENTER_URL))
                            context.startActivity(intent)
                        },
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            if (logoutError != null) {
                Text(logoutError!!, color = MaterialTheme.colorScheme.error)
                Spacer(Modifier.height(8.dp))
            }
            OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    scope.launch {
                        val result = manager.logout(onBeforeGateTransition = onLogoutNavigate)
                        if (result.isFailure) {
                            logoutError = result.exceptionOrNull()?.message ?: "退出登录失败"
                        }
                    }
                },
            ) {
                Icon(Icons.AutoMirrored.Filled.Logout, null, Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("退出登录")
            }
        }
    }

    if (showChangePassword) {
        ChangePasswordDialog(
            onDismiss = { showChangePassword = false },
            onSubmit = { old, new, new2 ->
                scope.launch {
                    try {
                        FeAccountProfileApi().changePassword(
                            manager.requireAccessToken(),
                            old,
                            new,
                            new2,
                        )
                        showChangePassword = false
                        // token_version 已递增, 本地凭据失效 -> 强制重新登录
                        manager.revalidateOnline()
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        changePasswordError = (e as? FeAccountApiException)?.error?.message
                            ?: e.message ?: "修改失败"
                    }
                }
            },
            error = changePasswordError,
        )
    }

    if (showBindings) {
        BindingsDialog(
            bindings = bindings,
            error = bindingsError,
            onDismiss = { showBindings = false },
            onOpenWebAccountCenter = {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(ACCOUNT_CENTER_URL))
                context.startActivity(intent)
            },
            onBindEts100 = {
                bindEts100Error = null
                showBindEts100 = true
            },
            onUnbindEts100 = {
                showUnbindConfirm = true
            },
        )
    }

    if (showBindEts100) {
        BindEts100Dialog(
            onDismiss = { showBindEts100 = false },
            onSubmit = { phone, password ->
                scope.launch {
                    isBindingEts100 = true
                    bindEts100Error = null
                    try {
                        manager.bindPlatformAccount(phone, password)
                        showBindEts100 = false
                        Toast.makeText(context, "绑定 ETS100 账号成功", Toast.LENGTH_SHORT).show()
                        refreshBindings()
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        bindEts100Error = (e as? FeAccountApiException)?.error?.message ?: e.message ?: "绑定失败"
                    } finally {
                        isBindingEts100 = false
                    }
                }
            },
            error = bindEts100Error,
            isLoading = isBindingEts100,
        )
    }

    if (showUnbindConfirm) {
        UnbindConfirmDialog(
            onDismiss = { showUnbindConfirm = false },
            onConfirm = {
                scope.launch {
                    isUnbindingEts100 = true
                    try {
                        manager.unbindPlatformAccount()
                        showUnbindConfirm = false
                        Toast.makeText(context, "已解除 ETS100 账号绑定", Toast.LENGTH_SHORT).show()
                        refreshBindings()
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        Toast.makeText(context, e.message ?: "解除绑定失败", Toast.LENGTH_SHORT).show()
                    } finally {
                        isUnbindingEts100 = false
                    }
                }
            },
            isLoading = isUnbindingEts100,
        )
    }
}

@Composable
internal fun AccountOfflineMessage(
    message: String,
    showProgress: Boolean,
    onRetry: () -> Unit,
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("个人中心") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            if (showProgress) CircularProgressIndicator()
            Text(message, style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(16.dp))
            Button(onClick = onRetry) { Text("重试连接") }
        }
    }
}

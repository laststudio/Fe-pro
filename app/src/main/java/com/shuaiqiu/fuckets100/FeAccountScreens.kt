package com.shuaiqiu.fuckets100

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.webkit.JavascriptInterface
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

@Composable
fun FeAccountGate(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val manager = remember { FeAccountManager.get(context) }
    val state by manager.gateState.collectAsState()
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(manager) { manager.checkGate() }
    LaunchedEffect(manager, lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            val resumedState = manager.gateState.value
            if (resumedState is FeAccountGateState.Ready || resumedState is FeAccountGateState.Blocked) {
                manager.revalidateOnline()
            }
            while (true) {
                delay(ONLINE_REVALIDATION_INTERVAL_MS)
                if (manager.gateState.value is FeAccountGateState.Ready) {
                    manager.revalidateOnline()
                }
            }
        }
    }

    when (val current = state) {
        FeAccountGateState.Checking -> AccountGateMessage("正在连接 LA Studio…", showProgress = true)
        FeAccountGateState.LoginRequired -> FeLoginScreen(manager)
        is FeAccountGateState.Blocked -> AccountGateMessage(
            message = current.message,
            actionLabel = current.updateUrl?.let { "立即更新" } ?: if (current.retryable) "重试" else null,
            onAction = if (current.updateUrl != null) {
                { openTrustedHttps(context, current.updateUrl, setOf("oplist.lastudio.cc", "lastudio.cc")) }
            } else if (current.retryable) {
                { scope.launch { manager.checkGate() } }
            } else null,
        )
        is FeAccountGateState.Ready -> content()
    }
}

@Composable
private fun AccountGateMessage(
    message: String,
    showProgress: Boolean = false,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Box(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (showProgress) CircularProgressIndicator()
            Text(message, style = MaterialTheme.typography.bodyLarge)
            if (actionLabel != null && onAction != null) Button(onClick = onAction) { Text(actionLabel) }
        }
    }
}

@Composable
private fun FeLoginScreen(manager: FeAccountManager) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var username by rememberSaveable { mutableStateOf("") }
    // Password is deliberately not saveable and never leaves this in-memory field except for login.
    var password by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    var captchaVisible by remember { mutableStateOf(false) }
    var pendingCaptchaToken by remember { mutableStateOf<String?>(null) }

    fun submit(capToken: String? = pendingCaptchaToken) {
        if (username.isBlank() || password.isBlank()) {
            error = "请输入用户名和密码"
            return
        }
        scope.launch {
            loading = true
            error = null
            val result = manager.login(username, password, capToken)
            loading = false
            result.exceptionOrNull()?.let { failure ->
                val apiError = (failure as? FeAccountApiException)?.error
                if (apiError?.captchaRequired == true) {
                    captchaVisible = true
                    error = "请先完成人机验证"
                } else {
                    error = apiError?.message ?: failure.message ?: "登录失败"
                }
            }
            pendingCaptchaToken = null
        }
    }

    Scaffold(containerColor = MaterialTheme.colorScheme.surface) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(28.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(Icons.Default.AccountCircle, null, Modifier.size(72.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(16.dp))
            Text("登录 Fe", style = MaterialTheme.typography.headlineMedium)
            Text("使用 LA Studio 账号继续", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(28.dp))
            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                enabled = !loading,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("用户名") },
                leadingIcon = { Icon(Icons.Default.Person, null) },
                singleLine = true,
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                enabled = !loading,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("密码") },
                leadingIcon = { Icon(Icons.Default.Lock, null) },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                singleLine = true,
            )
            if (error != null) {
                Spacer(Modifier.height(12.dp))
                Text(error!!, color = MaterialTheme.colorScheme.error)
            }
            Spacer(Modifier.height(20.dp))
            Button(onClick = { submit() }, enabled = !loading, modifier = Modifier.fillMaxWidth()) {
                if (loading) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                else Text("登录")
            }
            TextButton(
                enabled = !loading,
                onClick = {
                    openTrustedHttps(
                        context,
                        "https://auth.lastudio.cc/register",
                        setOf("auth.lastudio.cc"),
                    )
                },
            ) { Text("注册 LA Studio 账号") }
        }
    }

    if (captchaVisible) {
        CapVerificationDialog(
            onDismiss = {
                captchaVisible = false
                pendingCaptchaToken = null
            },
            onToken = {
                captchaVisible = false
                pendingCaptchaToken = it
                submit(it)
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeAccountScreen(
    onBack: () -> Unit,
    onLogoutNavigate: () -> Unit,
) {
    val context = LocalContext.current
    val manager = remember { FeAccountManager.get(context) }
    val state by manager.gateState.collectAsState()
    val session = (state as? FeAccountGateState.Ready)?.session
    val scope = rememberCoroutineScope()

    var showChangePassword by remember { mutableStateOf(false) }
    var showBindings by remember { mutableStateOf(false) }
    var showDevices by remember { mutableStateOf(false) }
    var showSessions by remember { mutableStateOf(false) }
    var bindings by remember { mutableStateOf<FeAccountProfileApi.AccountBindings?>(null) }
    var bindingsError by remember { mutableStateOf<String?>(null) }
    var changePasswordError by remember { mutableStateOf<String?>(null) }
    var logoutError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(session) {
        if (session != null) {
            bindings = null
            bindingsError = null
            runCatching {
                FeAccountProfileApi().bindings(manager.requireAccessToken())
            }.onSuccess { bindings = it }.onFailure {
                if (it is FeAccountApiException && it.error.statusCode == 401) {
                    manager.revalidateOnline()
                } else {
                    bindingsError = it.message ?: "加载绑定信息失败"
                }
            }
        }
    }

    if (showDevices) {
        AccountDevicesPage(manager = manager, onBack = { showDevices = false })
        return
    }
    if (showSessions) {
        AccountSessionsPage(manager = manager, onBack = { showSessions = false })
        return
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
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AccountInitialAvatar(
                    username = session?.user?.username.orEmpty(),
                    size = 84,
                    avatarUrl = session?.user?.avatarUrl,
                    userId = session?.user?.id,
                )
                Spacer(Modifier.width(18.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        session?.user?.username.orEmpty(),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        "LA Studio 用户 #${session?.user?.id ?: "-"}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        "会员等级 L${session?.user?.memberLevel ?: 0}",
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
                        onClick = { showDevices = true },
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    SettingsListItem(
                        icon = Icons.Default.Settings,
                        title = "登录会话",
                        sub = "查看和下线登录会话",
                        onClick = { showSessions = true },
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
        )
    }
}

private fun bindingsSummary(bindings: FeAccountProfileApi.AccountBindings?): String {
    if (bindings == null) return "查看邮箱、e听说绑定情况"
    val email = bindings.email?.let { "邮箱${if (it.isVerified) "✓" else ""}" }
    val etingshuo = bindings.etingshuo?.let { "e听说" }
    return listOfNotNull(email, etingshuo).joinToString(" · ").ifBlank { "未绑定邮箱/e听说" }
}

@Composable
private fun BindingsDialog(
    bindings: FeAccountProfileApi.AccountBindings?,
    error: String?,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("账号绑定") },
        text = {
            when {
                error != null -> Text(error, color = MaterialTheme.colorScheme.error)
                bindings == null -> Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                    Text("加载中…")
                }
                else -> Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    BindingRow(
                        title = "邮箱",
                        value = bindings.email?.let {
                            "${it.email}${if (it.isVerified) "（已验证）" else "（未验证）"}"
                        } ?: "未绑定",
                    )
                    BindingRow(
                        title = "e听说",
                        value = bindings.etingshuo?.account ?: "未绑定",
                    )
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("关闭") } },
    )
}

@Composable
private fun BindingRow(title: String, value: String) {
    Column {
        Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(2.dp))
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun ChangePasswordDialog(
    onDismiss: () -> Unit,
    onSubmit: (old: String, new: String, new2: String) -> Unit,
    error: String?,
) {
    var old by remember { mutableStateOf("") }
    var new by remember { mutableStateOf("") }
    var new2 by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var localError by remember { mutableStateOf<String?>(null) }
    val shownError = error ?: localError

    AlertDialog(
        onDismissRequest = { if (!loading) onDismiss() },
        title = { Text("修改密码") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (shownError != null) {
                    Text(shownError, color = MaterialTheme.colorScheme.error)
                }
                OutlinedTextField(
                    value = old,
                    onValueChange = { old = it },
                    label = { Text("当前密码") },
                    singleLine = true,
                    enabled = !loading,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                )
                OutlinedTextField(
                    value = new,
                    onValueChange = { new = it },
                    label = { Text("新密码（至少 10 位）") },
                    singleLine = true,
                    enabled = !loading,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                )
                OutlinedTextField(
                    value = new2,
                    onValueChange = { new2 = it },
                    label = { Text("确认新密码") },
                    singleLine = true,
                    enabled = !loading,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = !loading,
                onClick = {
                    when {
                        old.isBlank() || new.isBlank() || new2.isBlank() -> localError = "请填写完整"
                        new != new2 -> localError = "两次新密码不一致"
                        new.length < 10 -> localError = "新密码至少 10 位"
                        else -> {
                            localError = null
                            loading = true
                            onSubmit(old, new, new2)
                        }
                    }
                },
            ) {
                if (loading) {
                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Text("确认修改")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !loading) { Text("取消") }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AccountDevicesPage(manager: FeAccountManager, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    var devices by remember { mutableStateOf<List<JSONObject>?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var pendingRevoke by remember { mutableStateOf<JSONObject?>(null) }

    fun refresh() {
        scope.launch {
            error = null
            runCatching { FeAccountProfileApi().devices(manager.requireAccessToken()) }
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
                            .onSuccess { refresh() }
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
private fun AccountDeviceCard(device: JSONObject, onRevoke: () -> Unit) {
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AccountSessionsPage(manager: FeAccountManager, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    var sessions by remember { mutableStateOf<List<JSONObject>?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    fun refresh() {
        scope.launch {
            error = null
            runCatching { FeAccountProfileApi().sessions(manager.requireAccessToken()) }
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
                                        .onSuccess { refresh() }
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
private fun AccountSessionCard(session: JSONObject, onRevoke: () -> Unit) {
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

@Composable
fun AccountInitialAvatar(
    username: String,
    size: Int = 56,
    avatarUrl: String? = null,
    userId: Long? = null,
) {
    val context = LocalContext.current
    var avatar by remember(avatarUrl, userId) { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }
    LaunchedEffect(avatarUrl, userId) {
        val version = AvatarCache.versionFrom(avatarUrl)
        avatar = AvatarCache.load(context, userId ?: -1L, version)
            ?: avatarUrl?.let { path ->
                val resolved = if (path.startsWith("/")) FeAccountApi.BASE_URL + path else path
                try {
                    val uri = Uri.parse(resolved)
                    require(uri.scheme == "https" && uri.host == "api.lastudio.cc")
                    withContext(Dispatchers.IO) {
                        val connection = (URL(resolved).openConnection() as HttpURLConnection).apply {
                            connectTimeout = AVATAR_TIMEOUT_MS
                            readTimeout = AVATAR_TIMEOUT_MS
                            instanceFollowRedirects = false
                        }
                        try {
                            require(connection.responseCode == HttpURLConnection.HTTP_OK)
                            val bitmap = connection.inputStream.use {
                                android.graphics.BitmapFactory.decodeStream(it)
                            } ?: return@withContext null
                            if (userId != null && version != null) {
                                AvatarCache.save(context, userId, version, bitmap)
                            }
                            bitmap.asImageBitmap()
                        } finally {
                            connection.disconnect()
                        }
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    null
                }
            }
    }
    Box(
        Modifier.size(size.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        if (avatar != null) {
            Image(avatar!!, contentDescription = "用户头像", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        } else {
            Text(username.trim().firstOrNull()?.uppercase() ?: "F", style = MaterialTheme.typography.headlineMedium)
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun CapVerificationDialog(
    onDismiss: () -> Unit,
    onToken: (String) -> Unit,
) {
    val currentToken by rememberUpdatedState(onToken)
    var captchaError by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("人机验证") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (captchaError != null) {
                    Text(captchaError!!, color = MaterialTheme.colorScheme.error)
                }
                AndroidView(
                    modifier = Modifier.fillMaxWidth().height(220.dp),
                    factory = { context ->
                        WebView(context).apply {
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            settings.allowFileAccess = false
                            settings.allowContentAccess = false
                            addJavascriptInterface(object {
                                @JavascriptInterface fun solved(token: String) {
                                    post {
                                        if (token.length in 20..2048) {
                                            captchaError = null
                                            currentToken(token)
                                        }
                                    }
                                }
                                @JavascriptInterface fun failed(message: String) {
                                    post { captchaError = message.take(200) }
                                }
                            }, "FeCaptcha")
                            webViewClient = object : WebViewClient() {
                                override fun shouldOverrideUrlLoading(
                                    view: WebView,
                                    request: WebResourceRequest,
                                ): Boolean = !isExpectedCaptchaTopLevel(request.url)

                                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                    val uri = runCatching { Uri.parse(url) }.getOrNull()
                                    if (uri == null || !isExpectedCaptchaTopLevel(uri)) view?.stopLoading()
                                }

                                override fun onReceivedError(
                                    view: WebView?,
                                    request: WebResourceRequest?,
                                    error: WebResourceError?,
                                ) {
                                    if (request?.isForMainFrame == true) {
                                        captchaError = "验证页面加载失败，请检查网络后重试"
                                    }
                                }
                            }
                            val widgetScript = context.assets.open(CAP_WIDGET_ASSET)
                                .bufferedReader()
                                .use { it.readText() }
                            loadDataWithBaseURL(
                                "https://lastudio.cc/",
                                capHtml(widgetScript),
                                "text/html",
                                "UTF-8",
                                null,
                            )
                        }
                    },
                    onRelease = { webView ->
                        webView.removeJavascriptInterface("FeCaptcha")
                        webView.stopLoading()
                        webView.destroy()
                    },
                )
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}

private fun capHtml(widgetScript: String): String = """<!doctype html><html><head><meta name="viewport" content="width=device-width"><meta http-equiv="Content-Security-Policy" content="default-src 'none'; script-src 'unsafe-inline'; connect-src https://lastudio.cc https://cdn.jsdelivr.net; worker-src blob:; style-src 'unsafe-inline'; img-src data:"><script>$widgetScript</script><script>customElements.whenDefined('cap-widget').then(()=>{const w=document.querySelector('cap-widget');w.addEventListener('solve',e=>FeCaptcha.solved(e.detail.token));w.addEventListener('error',e=>FeCaptcha.failed(e.detail.message||'验证失败，请重试'));});</script></head><body><cap-widget data-cap-api-endpoint="https://lastudio.cc/cap/f0d2ec6d51/" data-cap-i18n-initial-state="点击开始验证" data-cap-i18n-verifying-label="正在验证" data-cap-i18n-solved-label="验证完成"></cap-widget></body></html>"""

private fun isExpectedCaptchaTopLevel(uri: Uri): Boolean =
    uri.scheme == "https" && uri.host in setOf("lastudio.cc", "auth.lastudio.cc")

private fun openTrustedHttps(context: android.content.Context, rawUrl: String, allowedHosts: Set<String>) {
    val uri = runCatching { Uri.parse(rawUrl) }.getOrNull() ?: return
    if (uri.scheme != "https" || uri.host !in allowedHosts) return
    context.startActivity(Intent(Intent.ACTION_VIEW, uri))
}

private const val ONLINE_REVALIDATION_INTERVAL_MS = 60_000L
private const val AVATAR_TIMEOUT_MS = 5_000
// Vendored cap-widget 0.1.56; SHA-256 296eb54ccfa39ba072fa68e81cb1013cb4d68cde2cfe22da0501543b2e7a2f9e.
private const val CAP_WIDGET_ASSET = "cap-widget-0.1.56.min.js"

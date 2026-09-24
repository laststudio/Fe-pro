package com.shuaiqiu.fuckets100

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
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
import androidx.compose.ui.res.painterResource
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
fun FeAccountGate(
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val manager = remember { FeAccountManager.get(context) }
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(manager) { manager.checkGate() }
    LaunchedEffect(manager, lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            val resumedState = manager.gateState.value
            if (resumedState is FeAccountGateState.Ready ||
                resumedState is FeAccountGateState.Offline
            ) {
                manager.revalidateOnline()
            }
            while (true) {
                delay(ONLINE_REVALIDATION_INTERVAL_MS)
                if (manager.gateState.value is FeAccountGateState.Ready ||
                    manager.gateState.value is FeAccountGateState.Offline
                ) {
                    manager.revalidateOnline()
                }
            }
        }
    }

    content()
}

@Composable
private fun FeLoginScreen(manager: FeAccountManager) {
    val scope = rememberCoroutineScope()
    var username by rememberSaveable { mutableStateOf("") }
    // Password is deliberately not saveable and never leaves this in-memory field except for login.
    var password by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    var captchaVisible by remember { mutableStateOf(false) }
    var pendingCaptchaToken by remember { mutableStateOf<String?>(null) }
    var showRegister by remember { mutableStateOf(false) }

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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .imePadding()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .size(92.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.fe_logo),
                        contentDescription = "Fe",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(14.dp)),
                    )
                }
                Spacer(Modifier.height(20.dp))
                Text(
                    "登录 Fe",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "使用 LA Studio 账号继续",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(32.dp))

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(28.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        OutlinedTextField(
                            value = username,
                            onValueChange = { username = it },
                            enabled = !loading,
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("用户名") },
                            leadingIcon = { Icon(Icons.Default.Person, null) },
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
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
                            shape = RoundedCornerShape(16.dp),
                        )
                        if (error != null) {
                            Spacer(Modifier.height(12.dp))
                            Text(
                                error!!,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                        Spacer(Modifier.height(20.dp))
                        Button(
                            onClick = { submit() },
                            enabled = !loading,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(16.dp),
                        ) {
                            if (loading) {
                                CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.5.dp)
                            } else {
                                Text("登录", style = MaterialTheme.typography.titleMedium)
                            }
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))
                TextButton(
                    enabled = !loading,
                    onClick = { showRegister = true },
                ) { Text("注册 LA Studio 账号") }
            }
        }
    }

    if (showRegister) {
        InAppRegisterScreen(onBack = { showRegister = false })
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

private const val REGISTER_URL = "https://auth.lastudio.cc/register"
private const val ACCOUNT_CENTER_URL = "https://lastudio.cc/account"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun InAppRegisterScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("注册 LA Studio 账号") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                },
            )
        },
        containerColor = MaterialTheme.colorScheme.surface,
    ) { padding ->
        AndroidView(
            modifier = Modifier.fillMaxSize().padding(padding),
            factory = { context ->
                WebView(context).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.allowFileAccess = false
                    settings.allowContentAccess = false
                    webViewClient = object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(
                            view: WebView,
                            request: WebResourceRequest,
                        ): Boolean = !isTrustedAuthHost(request.url)
                    }
                    loadUrl(REGISTER_URL)
                }
            },
            update = { webView -> webView.loadUrl(REGISTER_URL) },
            onRelease = { webView ->
                webView.stopLoading()
                webView.destroy()
            },
        )
    }
}

private fun isTrustedAuthHost(uri: Uri): Boolean =
    uri.scheme == "https" && uri.host in setOf("auth.lastudio.cc", "lastudio.cc")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeAccountScreen(
    onBack: () -> Unit,
    onNavigateToDevices: () -> Unit,
    onNavigateToSessions: () -> Unit,
    onLogoutNavigate: () -> Unit,
) {
    val context = LocalContext.current
    val manager = remember { FeAccountManager.get(context) }
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
private fun AccountOfflineMessage(
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
    onOpenWebAccountCenter: () -> Unit,
    onBindEts100: () -> Unit,
    onUnbindEts100: () -> Unit,
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
                    Column {
                        Text(
                            "e听说 (ETS100)",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            bindings.etingshuo?.account ?: "未绑定",
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "用于云端模式自动读取作业与解析答案",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(8.dp))
                        if (bindings.etingshuo == null) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                OutlinedButton(
                                    onClick = onBindEts100,
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                                ) {
                                    Text("应用内绑定", style = MaterialTheme.typography.labelMedium)
                                }
                                Button(
                                    onClick = onOpenWebAccountCenter,
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.OpenInNew, null, Modifier.size(14.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("前往官网绑定", style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                OutlinedButton(
                                    onClick = onUnbindEts100,
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                                ) {
                                    Text("解除绑定", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelMedium)
                                }
                                Button(
                                    onClick = onBindEts100,
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                                ) {
                                    Text("更换绑定", style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("关闭") } },
        dismissButton = {
            TextButton(onClick = onOpenWebAccountCenter) {
                Icon(Icons.AutoMirrored.Filled.OpenInNew, null, Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("前往官网个人中心")
            }
        },
    )
}

@Composable
private fun BindEts100Dialog(
    onDismiss: () -> Unit,
    onSubmit: (phone: String, pass: String) -> Unit,
    error: String?,
    isLoading: Boolean,
) {
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        title = { Text("绑定 ETS100 账号") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "绑定的 ETS100 账号凭据将安全加密存储于 LA Studio 后端，仅用于云端作业拉取。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (error != null) {
                    Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("手机号 / 账号") },
                    singleLine = true,
                    enabled = !isLoading,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("密码") },
                    singleLine = true,
                    enabled = !isLoading,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmit(phone.trim(), password) },
                enabled = !isLoading && phone.isNotBlank() && password.isNotBlank(),
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                } else {
                    Text("确定绑定")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isLoading) {
                Text("取消")
            }
        },
    )
}

@Composable
private fun UnbindConfirmDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    isLoading: Boolean,
) {
    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        title = { Text("解除 ETS100 绑定") },
        text = { Text("解除绑定后，云端模式将无法自动拉取该账号的作业。确定要解除绑定吗？") },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = !isLoading,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onError,
                    )
                } else {
                    Text("确定解除")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isLoading) {
                Text("取消")
            }
        },
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
internal fun AccountDevicesPage(manager: FeAccountManager, onBack: () -> Unit) {
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
internal fun AccountSessionsPage(manager: FeAccountManager, onBack: () -> Unit) {
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

@Composable
internal fun CapVerificationDialog(
    onDismiss: () -> Unit,
    onToken: (String) -> Unit,
) {
    val currentToken by rememberUpdatedState(onToken)
    val solver = remember { FeCapSolver() }
    var solveAttempt by remember { mutableIntStateOf(0) }
    var solveState by remember { mutableStateOf<CapSolveState>(CapSolveState.Solving(0f, "正在连接安全验证服务...")) }

    LaunchedEffect(solveAttempt) {
        solveState = CapSolveState.Solving(0f, "正在获取安全验证挑战...")
        try {
            val result = withContext(Dispatchers.IO) {
                solver.solve { progress ->
                    val stage = when {
                        progress < 0.15f -> "正在获取安全挑战..."
                        progress < 0.90f -> "正在计算工作量证明 (${(progress * 100).toInt()}%)..."
                        else -> "正在校验运行环境与提交凭证..."
                    }
                    solveState = CapSolveState.Solving(progress, stage)
                }
            }
            val token = result.getOrThrow()
            solveState = CapSolveState.Success
            delay(350)
            currentToken(token)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            solveState = CapSolveState.Error(e.message ?: "验证失败，请重试")
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Surface(
                shape = CircleShape,
                color = when (solveState) {
                    is CapSolveState.Success -> MaterialTheme.colorScheme.primaryContainer
                    is CapSolveState.Error -> MaterialTheme.colorScheme.errorContainer
                    else -> MaterialTheme.colorScheme.secondaryContainer
                },
                modifier = Modifier.size(52.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = when (solveState) {
                            is CapSolveState.Success -> Icons.Default.CheckCircle
                            is CapSolveState.Error -> Icons.Default.ErrorOutline
                            else -> Icons.Default.Security
                        },
                        contentDescription = null,
                        tint = when (solveState) {
                            is CapSolveState.Success -> MaterialTheme.colorScheme.primary
                            is CapSolveState.Error -> MaterialTheme.colorScheme.error
                            else -> MaterialTheme.colorScheme.onSecondaryContainer
                        },
                        modifier = Modifier.size(30.dp),
                    )
                }
            }
        },
        title = {
            Text(
                text = "安全人机验证",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                when (val state = solveState) {
                    is CapSolveState.Solving -> {
                        val animatedProgress by animateFloatAsState(
                            targetValue = state.progress,
                            animationSpec = tween(durationMillis = 150),
                            label = "cap_progress",
                        )
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.size(92.dp),
                        ) {
                            if (animatedProgress <= 0.01f) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(80.dp),
                                    strokeWidth = 6.dp,
                                )
                            } else {
                                CircularProgressIndicator(
                                    progress = { animatedProgress },
                                    modifier = Modifier.size(80.dp),
                                    strokeWidth = 6.dp,
                                )
                                Text(
                                    text = "${(animatedProgress * 100).toInt()}%",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text(
                                text = state.stage,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = "纯原生并行算法加速 · 无需加载浏览器组件",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline,
                            )
                        }
                    }
                    is CapSolveState.Success -> {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.size(92.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(72.dp),
                            )
                        }
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text(
                                text = "验证通过",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Text(
                                text = "正在完成后续操作...",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline,
                            )
                        }
                    }
                    is CapSolveState.Error -> {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.size(92.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.ErrorOutline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(72.dp),
                            )
                        }
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text(
                                text = "验证失败",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error,
                            )
                            Text(
                                text = state.message,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (solveState is CapSolveState.Error) {
                Button(
                    onClick = { solveAttempt++ },
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("重试")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        },
    )
}

private sealed interface CapSolveState {
    data class Solving(val progress: Float, val stage: String) : CapSolveState
    object Success : CapSolveState
    data class Error(val message: String) : CapSolveState
}

private const val ONLINE_REVALIDATION_INTERVAL_MS = 60_000L
private const val AVATAR_TIMEOUT_MS = 5_000

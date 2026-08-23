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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
    var logoutError by remember { mutableStateOf<String?>(null) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("账号") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            AccountInitialAvatar(
                username = session?.user?.username.orEmpty(),
                size = 88,
                avatarUrl = session?.user?.avatarUrl,
            )
            Spacer(Modifier.height(16.dp))
            Text(session?.user?.username.orEmpty(), style = MaterialTheme.typography.headlineSmall)
            Text("LA Studio 用户 #${session?.user?.id ?: "-"}")
            Text("会员等级 L${session?.user?.memberLevel ?: 0}", color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.weight(1f))
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
            ) { Text("退出登录") }
        }
    }
}

@Composable
fun AccountInitialAvatar(username: String, size: Int = 56, avatarUrl: String? = null) {
    var avatar by remember(avatarUrl) { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }
    LaunchedEffect(avatarUrl) {
        avatar = avatarUrl?.let { path ->
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
                        connection.inputStream.use {
                            android.graphics.BitmapFactory.decodeStream(it)?.asImageBitmap()
                        }
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

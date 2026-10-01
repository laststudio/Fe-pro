package com.shuaiqiu.fuckets100.feature.shared

import android.graphics.Bitmap
import android.net.Uri
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.shuaiqiu.fuckets100.core.designsystem.TonalCard
import java.util.concurrent.atomic.AtomicBoolean

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InAppRegisterScreen(
    onBack: () -> Unit,
    onRegistered: () -> Unit = onBack,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("注册 LA Studio 账号") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回登录")
                    }
                },
                actions = {
                    TextButton(onClick = onBack) { Text("返回登录") }
                },
            )
        },
        containerColor = MaterialTheme.colorScheme.surface,
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding),
        ) {
            TonalCard(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "这是 LA Studio 账号注册页，不是 e听说账号注册页。注册成功后会自动返回登录页，再输入刚注册的账号密码。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            val completionHandled = remember { AtomicBoolean(false) }
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { context ->
                    WebView(context).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.allowFileAccess = false
                        settings.allowContentAccess = false
                        settings.javaScriptCanOpenWindowsAutomatically = false
                        settings.setSupportMultipleWindows(false)
                        settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                        settings.safeBrowsingEnabled = true
                        webViewClient = object : WebViewClient() {
                            override fun shouldOverrideUrlLoading(
                                view: WebView,
                                request: WebResourceRequest,
                            ): Boolean {
                                val uri = request.url
                                if (request.isForMainFrame && isRegistrationCompletionUrl(uri)) {
                                    // 注册页注册成功后会自动登录并跳转到官网。拦截这个回跳，
                                    // 不再让用户停留在官网首页手动寻找「返回登录」。
                                    view.stopLoading()
                                    if (completionHandled.compareAndSet(false, true)) {
                                        onRegistered()
                                    }
                                    return true
                                }
                                return !isTrustedAuthHost(uri)
                            }

                            override fun onPageStarted(
                                view: WebView,
                                url: String,
                                favicon: android.graphics.Bitmap?,
                            ) {
                                super.onPageStarted(view, url, favicon)
                                val uri = Uri.parse(url)
                                if (isRegistrationCompletionUrl(uri)) {
                                    view.stopLoading()
                                    if (completionHandled.compareAndSet(false, true)) {
                                        onRegistered()
                                    }
                                }
                            }
                        }
                        loadUrl(REGISTER_URL)
                    }
                },
                // 不要在每次 Compose 重组时重新加载注册页，否则用户填写到一半会被清空。
                onRelease = { webView ->
                    webView.stopLoading()
                    webView.destroy()
                },
            )
        }
    }
}

fun isTrustedAuthHost(uri: Uri): Boolean =
    uri.scheme == "https" && uri.host in setOf("auth.lastudio.cc", "lastudio.cc")

/**
 * auth.lastudio.cc 注册成功后会自动登录，并将无 transaction 的注册流程带回官网首页。
 * 这是注册完成信号，不是普通的官网链接；必须只匹配主框架且限制到明确的官网入口，
 * 避免用户在 WebView 中点击其他官网链接时误关闭注册页。
 */
fun isRegistrationCompletionUrl(uri: Uri): Boolean =
    uri.scheme == "https" &&
        uri.host == "lastudio.cc" &&
        (uri.path.isNullOrBlank() || uri.path == "/")

package com.shuaiqiu.fuckets100

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.shuaiqiu.fuckets100.component.material.ExpressiveScaffold
import com.shuaiqiu.fuckets100.component.material.TonalCard
import com.shuaiqiu.fuckets100.component.material.TopBarBackButton
import com.shuaiqiu.fuckets100.component.material.expressiveTopAppBarColors
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.json.JSONObject

private const val ACCOUNT_CENTER_URL = "https://lastudio.cc/account"

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun CloudActivationScreen(
    onBack: () -> Unit,
    onLoginSuccess: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var accountDisplay by remember { mutableStateOf<String?>(null) }
    var pendingSession by remember { mutableStateOf<FeAccountApi.CloudSession?>(null) }

    val changyanLoginLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) onLoginSuccess()
    }

    fun completeBoundLogin(session: FeAccountApi.CloudSession, account: JSONObject) {
        ETS100AuthManager.saveDeviceCode(context, session.deviceCode)
        ETS100AuthManager.saveLoginInfo(
            context,
            session.accountDisplay,
            session.token,
            account.getString("parent_id"),
        )
        ETS100AuthManager.saveLoginMethod(context, ETS100AuthManager.LOGIN_METHOD_BACKEND_BOUND)
        ETS100AuthManager.clearPassword(context)
        ETS100AuthManager.saveSelectedEcard(
            context,
            account.optString("id"),
            account.optString("name"),
            account.optString("grade"),
            account.optString("class_id"),
        )
        Toast.makeText(context, "已使用 LA Studio 绑定账号", Toast.LENGTH_SHORT).show()
        onLoginSuccess()
    }

    fun loadBoundAccount() {
        if (isLoading) return
        scope.launch {
            isLoading = true
            errorMessage = null
            try {
                val savedEcard = ETS100AuthManager.getSelectedEcardId(context).orEmpty()
                val session = FeAccountManager.get(context).cloudSession(savedEcard)
                accountDisplay = session.accountDisplay
                val selected = session.selectedAccount
                val selectedStillPreferred = savedEcard.isNotBlank() &&
                    selected.optString("id") == savedEcard
                if (session.validAccounts.size > 1 && !selectedStillPreferred) {
                    pendingSession = session
                } else {
                    completeBoundLogin(session, selected)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                errorMessage = e.message ?: "无法读取 LA Studio 绑定账号"
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) {
        if (ETS100AuthManager.isChangyanWebLogin(context) && ETS100AuthManager.isLoggedIn(context)) {
            onLoginSuccess()
        } else {
            loadBoundAccount()
        }
    }

    ExpressiveScaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        contentWindowInsets = WindowInsets.safeDrawing.only(
            WindowInsetsSides.Top + WindowInsetsSides.Horizontal,
        ),
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text("云端模式") },
                subtitle = { Text("使用 LA Studio 绑定账号或讯飞登录") },
                navigationIcon = { TopBarBackButton(onClick = onBack) },
                windowInsets = WindowInsets.safeDrawing.only(
                    WindowInsetsSides.Top + WindowInsetsSides.Horizontal,
                ),
                colors = expressiveTopAppBarColors(),
                scrollBehavior = scrollBehavior,
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Spacer(Modifier.height(1.dp))
            TonalCard(
                modifier = Modifier.fillMaxWidth(),
                containerColor = MaterialTheme.colorScheme.surfaceBright,
                shape = MaterialTheme.shapes.extraLarge,
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(
                        Icons.Default.Cloud,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(64.dp),
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        accountDisplay ?: "LA Studio 绑定账号",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        errorMessage ?: if (isLoading) {
                            "正在从后端读取绑定账号和设备码"
                        } else {
                            "普通云端模式只使用当前 LA Studio 账号绑定的 ETS100 账号"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (errorMessage == null) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.error
                        },
                    )
                    Spacer(Modifier.height(24.dp))
                    Button(
                        onClick = ::loadBoundAccount,
                        enabled = !isLoading,
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = MaterialTheme.shapes.medium,
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Icon(Icons.AutoMirrored.Filled.Login, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("使用绑定账号", style = MaterialTheme.typography.titleMedium)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        TextButton(
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(ACCOUNT_CENTER_URL))
                                context.startActivity(intent)
                            },
                            enabled = !isLoading,
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.OpenInNew,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(Modifier.width(4.dp))
                            Text("前往个人中心绑定")
                        }
                        TextButton(
                            onClick = {
                                changyanLoginLauncher.launch(ChangyanWebLoginActivity.createIntent(context))
                            },
                            enabled = !isLoading,
                        ) {
                            Text("讯飞登录")
                        }
                    }
                }
            }

            TonalCard(
                modifier = Modifier.fillMaxWidth(),
                containerColor = MaterialTheme.colorScheme.surfaceBright,
                shape = MaterialTheme.shapes.large,
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        "绑定账号和设备码由 LA Studio 后端统一管理。未绑定时请先前往 LA Studio 个人中心完成 ETS100 绑定；讯飞登录可作为独立入口。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }

    pendingSession?.let { session ->
        val labels = session.validAccounts.map { account ->
            account.optString("name").ifBlank { account.optString("grade").ifBlank { "未知账号" } }
        }
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { pendingSession = null },
            title = { Text("选择要读取的账号") },
            text = {
                Column {
                    session.validAccounts.forEachIndexed { index, account ->
                        TextButton(
                            onClick = {
                                pendingSession = null
                                completeBoundLogin(session, account)
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(labels[index])
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { pendingSession = null }) { Text("取消") }
            },
        )
    }
}

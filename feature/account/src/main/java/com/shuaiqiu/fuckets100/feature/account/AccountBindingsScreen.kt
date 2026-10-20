package com.shuaiqiu.fuckets100.feature.account

import android.content.Intent
import androidx.core.net.toUri
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.shuaiqiu.fuckets100.core.designsystem.TonalCard
import com.shuaiqiu.fuckets100.feature.shared.ACCOUNT_CENTER_URL

@Composable
fun AccountBindingsScreen(
    isEtingshuo: Boolean,
    onBack: () -> Unit,
    viewModel: AccountBindingsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    var showBindingForm by rememberSaveable { mutableStateOf(false) }
    var account by rememberSaveable { mutableStateOf("") }
    // 密码不写入页面保存状态或持久存储。
    var password by remember { mutableStateOf("") }
    var showUnbindConfirm by remember { mutableStateOf(false) }
    val busy = state.isRefreshing || state.isSubmitting
    val bindings = state.bindings
    val boundAccount = bindings?.etingshuo?.account
    val email = bindings?.email
    val openWeb = { context.startActivity(Intent(Intent.ACTION_VIEW, ACCOUNT_CENTER_URL.toUri())) }

    LaunchedEffect(viewModel) { viewModel.refresh() }
    val lifecycleOwner = LocalLifecycleOwner.current
    var hasPaused by remember { mutableStateOf(false) }
    DisposableEffect(lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) hasPaused = true
            if (event == Lifecycle.Event.ON_RESUME && hasPaused) viewModel.refresh(force = true)
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val submit: () -> Unit = {
        if (!busy && account.isNotBlank() && password.isNotBlank()) {
            focusManager.clearFocus()
            viewModel.bind(account, password) {
                password = ""
                showBindingForm = false
                Toast.makeText(context, "e听说账号绑定成功", Toast.LENGTH_SHORT).show()
            }
        }
    }
    AccountPageScaffold(
        title = if (isEtingshuo) "e听说绑定" else "邮箱绑定",
        onBack = onBack,
        isRefreshing = state.isRefreshing,
        onRefresh = { viewModel.refresh(force = true) },
    ) {
        state.error?.let { AccountMessage(it, isError = true) }
        TonalCard(modifier = Modifier.fillMaxWidth(), containerColor = MaterialTheme.colorScheme.surfaceContainerLow) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Icon(if (isEtingshuo) Icons.Default.Cloud else Icons.Default.Email, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(if (isEtingshuo) "e听说账号" else "邮箱", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        when {
                            bindings == null && state.isRefreshing -> "正在读取绑定信息…"
                            bindings == null -> "暂未获取绑定信息"
                            isEtingshuo -> boundAccount ?: "未绑定"
                            else -> email?.email ?: "未绑定"
                        },
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    if (!isEtingshuo && email != null) {
                        Text(if (email.isVerified) "已验证" else "未验证", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        if (!isEtingshuo) {
            AccountMessage("邮箱绑定与验证在官网个人中心完成。")
            Button(onClick = openWeb, modifier = Modifier.fillMaxWidth()) { Text("前往官网管理") }
        } else if (bindings != null) {
            if (boundAccount == null || showBindingForm) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(if (boundAccount == null) "绑定 e听说账号" else "更换绑定账号", style = MaterialTheme.typography.titleMedium)
                    Text("填写 e听说学生账号，不是 LastStudio 登录账号。", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    OutlinedTextField(
                        value = account,
                        onValueChange = { account = it },
                        label = { Text("手机号 / 账号") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        enabled = !busy,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                    )
                    AccountPasswordField(password, { password = it }, "e听说密码", !busy, ImeAction.Done, submit)
                    Button(
                        onClick = submit,
                        enabled = !busy && account.isNotBlank() && password.isNotBlank(),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        if (state.isSubmitting) {
                            CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                            Spacer(Modifier.width(8.dp))
                        }
                        Text(if (boundAccount == null) "绑定账号" else "保存新绑定")
                    }
                    TextButton(onClick = openWeb, enabled = !busy, modifier = Modifier.align(Alignment.CenterHorizontally)) { Text("在官网管理绑定") }
                    if (boundAccount != null) {
                        TextButton(onClick = { showBindingForm = false; password = "" }, enabled = !busy, modifier = Modifier.align(Alignment.CenterHorizontally)) { Text("取消更换") }
                    }
                }
            } else {
                AccountMessage("云端模式会使用这个账号读取作业。")
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = { account = boundAccount; showBindingForm = true },
                        modifier = Modifier.fillMaxWidth(), enabled = !busy,
                    ) { Text("更换绑定") }
                    OutlinedButton(onClick = openWeb, modifier = Modifier.fillMaxWidth(), enabled = !busy) { Text("在官网管理绑定") }
                    TextButton(
                        onClick = { showUnbindConfirm = true },
                        enabled = !busy, modifier = Modifier.align(Alignment.CenterHorizontally),
                    ) { Text("解除绑定", color = MaterialTheme.colorScheme.error) }
                }
            }
        }
    }
    if (showUnbindConfirm) {
        UnbindConfirmDialog(
            onDismiss = { if (!state.isSubmitting) showUnbindConfirm = false },
            onConfirm = {
                viewModel.unbind {
                    showUnbindConfirm = false
                    password = ""
                    Toast.makeText(context, "已解除 e听说账号绑定", Toast.LENGTH_SHORT).show()
                }
            },
            isLoading = state.isSubmitting,
            error = state.error,
        )
    }
}

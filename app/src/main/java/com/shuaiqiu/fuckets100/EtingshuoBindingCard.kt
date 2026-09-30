package com.shuaiqiu.fuckets100

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.shuaiqiu.fuckets100.component.material.TonalCard
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/**
 * 云端模式共用的 e听说账号绑定卡片。
 *
 * 明确区分两类账号：这里填写的是 e听说/ETS100 的学生账号，不是 Fe 的 LA Studio 账号。
 * 绑定接口仍由 LA Studio 后端完成，但用户不再需要先跳到官网个人中心寻找入口。
 */
@Composable
internal fun EtingshuoBindingCard(
    manager: FeAccountManager,
    modifier: Modifier = Modifier,
    onBound: () -> Unit = {},
    onOpenWebFallback: (() -> Unit)? = null,
    onChangyanLogin: (() -> Unit)? = null,
) {
    val scope = rememberCoroutineScope()
    var bindings by remember { mutableStateOf<FeAccountProfileApi.AccountBindings?>(null) }
    var isChecking by remember { mutableStateOf(true) }
    var checkError by remember { mutableStateOf<String?>(null) }
    var account by rememberSaveable { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isBinding by remember { mutableStateOf(false) }
    var bindError by remember { mutableStateOf<String?>(null) }

    fun refresh() {
        scope.launch {
            isChecking = true
            checkError = null
            runCatching { manager.accountBindings(force = true) }
                .onSuccess { current ->
                    bindings = current
                }
                .onFailure { failure -> checkError = failure.message ?: "暂时无法读取绑定状态" }
            isChecking = false
        }
    }

    LaunchedEffect(manager) { refresh() }

    val boundAccount = bindings?.etingshuo?.account
    TonalCard(modifier = modifier, shape = MaterialTheme.shapes.large) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (boundAccount == null) Icons.Default.Cloud else Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = if (boundAccount == null) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp),
                )
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(
                        if (boundAccount == null) "绑定 e听说账号" else "e听说账号已绑定",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        if (boundAccount == null) "云端模式需要它来读取作业和答案"
                        else boundAccount,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            if (isChecking) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                    Text("正在检查绑定状态…", style = MaterialTheme.typography.bodySmall)
                }
            } else if (boundAccount != null) {
                Text(
                    "已准备好。进入云端模式时会使用这个账号读取作业。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "这里填写的是 e听说（ETS100）学生账号，不是刚才登录的 LA Studio 账号。通常使用你在 e听说客户端登录的手机号和密码。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                    }
                }
                Text(
                    "只需一步：输入 e听说登录信息，Fe 会安全地交给云端服务读取作业，不会把它显示给其他人。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = account,
                    onValueChange = { account = it },
                    enabled = !isBinding,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("e听说手机号 / 账号") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    enabled = !isBinding,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("e听说密码") },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                )
                if (checkError != null) {
                    Text(checkError!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
                if (bindError != null) {
                    Text(bindError!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
                Button(
                    onClick = {
                        if (account.isBlank() || password.isBlank()) {
                            bindError = "请输入 e听说账号和密码"
                            return@Button
                        }
                        scope.launch {
                            isBinding = true
                            bindError = null
                            try {
                                manager.bindPlatformAccount(account.trim(), password)
                                password = ""
                                refresh()
                                onBound()
                            } catch (e: CancellationException) {
                                throw e
                            } catch (e: Exception) {
                                bindError = (e as? FeAccountApiException)?.error?.message
                                    ?: e.message ?: "绑定失败，请检查 e听说账号和密码"
                            } finally {
                                isBinding = false
                            }
                        }
                    },
                    enabled = !isBinding,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    if (isBinding) {
                        CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        Text("绑定 e听说账号")
                    }
                }
                if (onChangyanLogin != null) {
                    TextButton(
                        onClick = onChangyanLogin,
                        enabled = !isBinding,
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                    ) {
                        Text("使用讯飞登录并绑定")
                    }
                }
                if (onOpenWebFallback != null) {
                    TextButton(
                        onClick = onOpenWebFallback,
                        enabled = !isBinding,
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                    ) {
                        Text("无法在应用内绑定？前往官网个人中心")
                    }
                }
            }
        }
    }
}

package com.shuaiqiu.fuckets100.feature.onboarding

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.shuaiqiu.fuckets100.core.designsystem.TonalCard
import com.shuaiqiu.fuckets100.cachedSessionOrNull
import com.shuaiqiu.fuckets100.feature.account.InAppRegisterScreen
import com.shuaiqiu.fuckets100.feature.account.CapVerificationDialog
import kotlinx.coroutines.launch

import com.shuaiqiu.fuckets100.FeAccountApiException
import com.shuaiqiu.fuckets100.FeAccountManager
// ============================================================================
// 步骤 2: 账号登录 (融入向导)
// ============================================================================

@Composable
internal fun AccountStep(
    accountManager: FeAccountManager,
    onNext: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val gateState by accountManager.gateState.collectAsState()
    val session = gateState.cachedSessionOrNull

    var username by rememberSaveable { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
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
            val result = accountManager.login(username, password, capToken)
            loading = false
            result.onSuccess {
                onNext()
            }.onFailure { failure ->
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(horizontal = 24.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(8.dp))

        // 头部图标与凝练标题
        Box(
            modifier = Modifier
                .size(68.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Default.AccountCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(38.dp),
            )
        }

        Spacer(Modifier.height(16.dp))

        Text(
            text = "连接 LA Studio 账号",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "同步会员特权与云端解析服务",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(14.dp))
        TonalCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "先登录 Fe 账号",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "这是 LA Studio 账号，用来保存设置和使用云端服务。注册完成后回到这里登录；下一步会单独引导你绑定 e听说账号。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        // 如果已经持有有效登录会话 (如老用户或刚才登录成功)
        if (session != null) {
            TonalCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(44.dp),
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "已登录为 ${session.user.username}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(4.dp))
                    SuggestionChip(
                        onClick = {},
                        label = { Text(session.user.tierDisplayName) },
                    )
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = onNext,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = MaterialTheme.shapes.medium,
                    ) {
                        Text("继续下一步")
                    }
                }
            }
        } else {
            // 未登录：极简卡片表单
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.extraLarge,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        enabled = !loading,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("用户名") },
                        leadingIcon = { Icon(Icons.Default.Person, null) },
                        singleLine = true,
                        shape = MaterialTheme.shapes.medium,
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
                        shape = MaterialTheme.shapes.medium,
                    )

                    if (error != null) {
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = error!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }

                    Spacer(Modifier.height(18.dp))

                    Button(
                        onClick = { submit() },
                        enabled = !loading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = MaterialTheme.shapes.medium,
                    ) {
                        if (loading) {
                            CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.5.dp)
                        } else {
                            Text("登录并继续", style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                TextButton(
                    enabled = !loading,
                    onClick = { showRegister = true },
                ) {
                    Text("没有账号？注册新账号")
                }
            }
        }

        Spacer(Modifier.height(16.dp))
    }

    if (showRegister) {
        InAppRegisterScreen(
            onBack = { showRegister = false },
            onRegistered = {
                showRegister = false
                Toast.makeText(
                    context,
                    "注册成功，请使用刚注册的 LA Studio 账号登录",
                    Toast.LENGTH_LONG,
                ).show()
            },
        )
    }

    if (captchaVisible) {
        CapVerificationDialog(
            onDismiss = {
                captchaVisible = false
                pendingCaptchaToken = null
            },
            onToken = { token ->
                captchaVisible = false
                pendingCaptchaToken = token
                submit(token)
            },
        )
    }
}

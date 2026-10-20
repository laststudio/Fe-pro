package com.shuaiqiu.fuckets100.feature.account

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel

@Composable
fun ChangePasswordScreen(
    onBack: () -> Unit,
    onPasswordChanged: () -> Unit,
    viewModel: ChangePasswordViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    var old by remember { mutableStateOf("") }
    var new by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }
    val submit: () -> Unit = {
        focusManager.clearFocus()
        viewModel.submit(old, new, confirmation) {
            old = ""; new = ""; confirmation = ""
            Toast.makeText(context, "密码已修改，请重新登录", Toast.LENGTH_SHORT).show()
            onPasswordChanged()
        }
    }
    AccountPageScaffold(title = "修改密码", onBack = onBack) {
        AccountMessage("修改的是 LastStudio 登录密码，不会更改 e听说密码。修改成功后需要重新登录。")
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            AccountPasswordField(old, { old = it }, "当前密码", !state.isSubmitting)
            AccountPasswordField(new, { new = it }, "新密码（至少 10 位）", !state.isSubmitting)
            AccountPasswordField(confirmation, { confirmation = it }, "确认新密码", !state.isSubmitting, ImeAction.Done, submit)
            state.error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium) }
            Button(
                onClick = submit,
                enabled = !state.isSubmitting,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (state.isSubmitting) {
                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                    Spacer(Modifier.width(8.dp))
                }
                Text("保存密码")
            }
        }
    }
}

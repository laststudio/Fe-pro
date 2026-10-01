package com.shuaiqiu.fuckets100.feature.account

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

@Composable
internal fun ChangePasswordDialog(
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

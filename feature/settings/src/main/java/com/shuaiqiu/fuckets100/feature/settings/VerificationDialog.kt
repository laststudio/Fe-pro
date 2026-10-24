package com.shuaiqiu.fuckets100.feature.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.shuaiqiu.fuckets100.SettingsManager
import com.shuaiqiu.fuckets100.UpdateStatus
import com.shuaiqiu.fuckets100.core.designsystem.ExpressiveDialog

@Composable
fun VerificationDialog(
    status: UpdateStatus,
    onVerified: () -> Unit,
) {
    var inputCode by remember(status.verificationCode) { mutableStateOf("") }
    var errorText by remember(status.verificationCode) { mutableStateOf("") }
    val title = status.verificationTitle.ifBlank { "启动验证" }
    val message = status.verificationMessage.ifBlank {
        "请输入验证码继续使用。"
    }

    ExpressiveDialog(
        onDismissRequest = {},
        icon = {
            Icon(
                imageVector = Icons.Default.Security,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
        },
        title = { Text(title) },
        text = {
            Column {
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = inputCode,
                    onValueChange = {
                        inputCode = it
                        errorText = ""
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    singleLine = true,
                    label = { Text("验证码") },
                    visualTransformation = PasswordVisualTransformation(),
                    isError = errorText.isNotBlank(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    ),
                    supportingText = {
                        if (errorText.isNotBlank()) Text(errorText)
                    },
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val normalizedInput = inputCode.trim()
                    if (normalizedInput == status.verificationCode) {
                        SettingsManager.saveLocalVerificationCode(normalizedInput)
                        onVerified()
                    } else {
                        errorText = "验证码不正确，请重新输入"
                    }
                },
            ) {
                Text("验证并继续")
            }
        },
    )
}

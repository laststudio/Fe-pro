package com.shuaiqiu.fuckets100.feature.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.shuaiqiu.fuckets100.UpdateStatus
import com.shuaiqiu.fuckets100.core.designsystem.ExpressiveDialog

/**
 * 更新弹窗组件。
 * 普通更新允许稍后处理，强制更新保持模态阻断。
 */
@Composable
fun UpdateDialog(
    status: UpdateStatus,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    var openError by remember(status.updateUrl) { mutableStateOf(false) }

    Log.d("UpdateDialog", "显示更新弹窗: message=${status.message}, isForce=${status.isForce}")

    ExpressiveDialog(
        onDismissRequest = {
            if (!status.isForce) onDismiss()
        },
        icon = {
            Icon(
                imageVector = if (status.isForce) Icons.Default.Warning else Icons.Default.SystemUpdate,
                contentDescription = null,
                tint = if (status.isForce) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.primary
                },
            )
        },
        title = {
            Text(if (status.isForce) "需要更新后继续使用" else "发现新版本")
        },
        text = {
            Box(
                modifier = Modifier
                    .heightIn(max = 240.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                androidx.compose.foundation.layout.Column {
                    Text(
                        text = status.message,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (openError) {
                        Text(
                            text = "无法打开更新地址，请检查网络后重试。",
                            modifier = Modifier.padding(top = 12.dp),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val opened = openUpdateUrl(context, status.updateUrl)
                    openError = !opened
                    if (opened && !status.isForce) onDismiss()
                },
            ) {
                Text("立即更新")
            }
        },
        dismissButton = {
            if (!status.isForce) {
                TextButton(onClick = onDismiss) {
                    Text("稍后再说")
                }
            }
        },
    )
}

private fun openUpdateUrl(context: Context, url: String): Boolean {
    if (url.isBlank()) return false
    return try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
        true
    } catch (e: Exception) {
        Log.e("UpdateDialog", "打开更新链接失败: $url", e)
        false
    }
}

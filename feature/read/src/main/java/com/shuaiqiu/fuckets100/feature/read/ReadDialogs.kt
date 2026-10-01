package com.shuaiqiu.fuckets100.feature.read

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
internal fun CloudReadConfirmDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(48.dp)
            )
        },
        title = {
            Text("确认读取云端作业？")
        },
        text = {
            Text(
                "读取云端作业可能会导致 E听说官方客户端退出登录。\n\n" +
                    "请先确认没有正在进行的 E听说练习或考试。建议读取完答案后，再去 E听说做题；否则可能导致 E听说考试或练习连接断开。"
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("确认读取")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

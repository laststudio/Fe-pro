package com.shuaiqiu.fuckets100.feature.settings

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable

/**
 * 公告弹窗组件
 * 用于展示远程下发的公告（支持分版本弹窗公告）。
 *
 * @param title 公告标题
 * @param message 公告正文
 * @param onViewDetail 查看详情回调（进入公告与更新页）
 * @param onDismiss 关闭回调
 */
@Composable
fun AnnouncementDialog(
    title: String,
    message: String,
    onViewDetail: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onViewDetail) { Text("查看详情") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("知道了") }
        },
    )
}

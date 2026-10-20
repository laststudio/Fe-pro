package com.shuaiqiu.fuckets100.feature.account

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.shuaiqiu.fuckets100.FeAccountUser

@Composable
internal fun AccountProfileDialog(user: FeAccountUser, onDismiss: () -> Unit) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("账号资料") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                AccountDetailField("用户名", user.username)
                AccountDetailField("账号 ID", user.id.toString())
                AccountDetailField("账号等级", user.tierDisplayName)
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("关闭") } },
        dismissButton = {
            TextButton(onClick = {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("LastStudio 账号 ID", user.id.toString()))
                Toast.makeText(context, "账号 ID 已复制", Toast.LENGTH_SHORT).show()
            }) { Text("复制账号 ID") }
        },
    )
}

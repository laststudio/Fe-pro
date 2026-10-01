package com.shuaiqiu.fuckets100.feature.activation

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

import com.shuaiqiu.fuckets100.ETS100AuthManager
import com.shuaiqiu.fuckets100.core.common.maskPhoneNumber
/**
 * 云端模式激活面板
 * 喵~ 用于显示登录状态和跳转到登录页面喵！
 */
@Composable
internal fun FeCloudActivationPanel(
    context: android.content.Context,
    isLoggedIn: Boolean,
    phone: String?,
    onCloudAuthChanged: () -> Unit,
    onNavigateToCloudActivation: () -> Unit,
    onNavigateToRead: () -> Unit,
    viewModel: ActivationViewModel = hiltViewModel(),
) {
    val cloudColor = MaterialTheme.colorScheme.tertiary
    val successColor = MaterialTheme.colorScheme.primary

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = when {
                    isLoggedIn -> "云端模式已激活"
                    else -> "云端模式未激活"
                },
                style = MaterialTheme.typography.labelSmall,
                color = if (isLoggedIn) successColor else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (isLoggedIn && !phone.isNullOrBlank()) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = "（${maskPhoneNumber(phone)}）手机号已登录",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(Modifier.height(16.dp))

        if (isLoggedIn) {
            // 已登录，显示进入云端首页按钮
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(
                    onClick = {
                        onNavigateToRead()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = cloudColor, contentColor = MaterialTheme.colorScheme.onTertiary),
                    modifier = Modifier.weight(1f).height(40.dp),
                ) {
                    Icon(Icons.Default.Cloud, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("云端", fontWeight = FontWeight.Bold, maxLines = 1)
                }

                OutlinedButton(
                    onClick = {
                        ETS100AuthManager.logout(context)
                        onCloudAuthChanged()
                        Toast.makeText(context, "已退出登录", Toast.LENGTH_SHORT).show()
                    },
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.weight(1f).height(40.dp),
                ) {
                    Icon(Icons.AutoMirrored.Filled.Logout, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("退出", fontWeight = FontWeight.Bold, maxLines = 1)
                }
            }
        } else {
            // 未登录，显示登录按钮
            Button(
                onClick = {
                    // 跳转到登录页面
                    onNavigateToCloudActivation()
                },
                colors = ButtonDefaults.buttonColors(containerColor = cloudColor, contentColor = MaterialTheme.colorScheme.onTertiary),
                modifier = Modifier.fillMaxWidth().height(40.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.Login, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("登录 ETS100 账号", fontWeight = FontWeight.Bold)
            }
        }

        if (!isLoggedIn) {
            Spacer(Modifier.height(8.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Cloud,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "云端模式说明",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "通过 ETS100 云端 API 在线获取作业列表和答案，无需本地文件",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

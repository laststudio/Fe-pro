package com.shuaiqiu.fuckets100

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

enum class ActivationMode(
    val title: String,
    val desc: String,
    val sysLabel: String,
    val icon: ImageVector,
    val hexColor: Color,
    val isSysOffline: Boolean
) {
    DEFAULT(
        "未激活",
        "请选择授权模式以启用核心功能",
        "SYS_OFFLINE",
        Icons.Default.Warning,
        Color(0xFFFFB4AB),
        true
    ),
    SHIZUKU(
        "Shizuku 模式",
        "借助 Shizuku 在 Root 环境下以系统级 API 实现答题增强功能",
        "SYS_READY",
        Icons.Default.CheckCircle,
        Color(0xFF4ADE80),
        false
    ),
    ROOT(
        "Root 模式",
        "通过 Root 权限直接访问应用数据文件实现答题增强功能",
        "SYS_READY",
        Icons.Default.Security,
        Color(0xFFFFB4AB),
        false
    ),
    DIRECT_READ(
        "Direct Read 漏洞直读",
        "利用零宽字符漏洞绕过 Android 限制直接读取应用内部存储实现答题增强",
        "SYS_DIRECT_READ",
        Icons.Default.Bolt,
        Color(0xFFFBBF24),
        false
    ),
    CLOUD(
        "云端模式",
        "通过 ETS100 云端 API 在线获取作业列表和答案",
        "SYS_READY",
        Icons.Default.Cloud,
        Color(0xFF60A5FA),
        false
    )
}

package com.shuaiqiu.fuckets100.feature.account

internal fun validateAccountPasswordChange(old: String, new: String, confirmation: String): String? = when {
    old.isBlank() || new.isBlank() || confirmation.isBlank() -> "请填写完整"
    new != confirmation -> "两次新密码不一致"
    // 本次只调整界面，保留原修改密码入口的校验规则。
    new.length < 10 -> "新密码至少 10 位"
    else -> null
}

internal fun isCurrentAccountRecord(recordId: String, currentId: String?): Boolean =
    !currentId.isNullOrBlank() && recordId.isNotBlank() && recordId == currentId

internal fun accountRecordStatus(status: String): String = when (status) {
    "active" -> "有效"
    "revoked" -> "已下线"
    "expired" -> "已过期"
    else -> "未知状态"
}

internal fun accountPlatformName(platform: String): String = when (platform.lowercase()) {
    "android" -> "Android"
    "ios" -> "iOS"
    "windows" -> "Windows"
    "linux" -> "Linux"
    "macos", "mac", "darwin" -> "macOS"
    "web" -> "网页客户端"
    else -> platform.ifBlank { "未知平台" }
}

internal fun accountClientName(userAgent: String): String = when {
    userAgent.startsWith("Fe/", ignoreCase = true) && userAgent.contains("Android", ignoreCase = true) -> "Fe · Android"
    userAgent.startsWith("Fe/", ignoreCase = true) -> "Fe 客户端"
    userAgent.startsWith("Mozilla/", ignoreCase = true) -> "网页客户端"
    userAgent.isBlank() -> "未知客户端"
    else -> "其他客户端"
}

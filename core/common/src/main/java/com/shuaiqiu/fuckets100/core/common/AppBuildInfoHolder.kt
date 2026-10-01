package com.shuaiqiu.fuckets100.core.common

/**
 * 进程级构建信息持有者。
 *
 * data 层模块（如账号 API、ETS100 授权）需要版本号 / 包名，但不能依赖 app 层的
 * BuildConfig。Application 启动时写入一次，供各数据模块读取。
 */
object AppBuildInfoHolder {
    @Volatile
    var current: AppBuildInfo? = null

    fun require(): AppBuildInfo = current
        ?: error("AppBuildInfoHolder 尚未初始化，请在 Application.onCreate 中调用")

    fun versionCodeOrZero(): Int = current?.versionCode ?: 0

    fun versionNameOrEmpty(): String = current?.versionName.orEmpty()

    fun applicationIdOrEmpty(): String = current?.applicationId.orEmpty()
}

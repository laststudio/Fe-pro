package com.shuaiqiu.fuckets100

/**
 * 进程级 Shizuku/Sui 运行时状态，由 Application 写入，供平台层读取。
 * 保持进程单例语义，避免 platform 层反向依赖 app 层。
 */
object ShizukuRuntimeState {
    @Volatile
    var isSui: Boolean = false

    @Volatile
    var isBinderReceived: Boolean = false

    @Volatile
    var uid: Int = -1

    @Volatile
    var version: Int = -1
}

package com.shuaiqiu.fuckets100

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 进程级更新/公告状态仓库。
 *
 * 原先是 FeApplication 上的静态可变状态，迁到这里后 feature/data 模块可以
 * 通过 Hilt 注入读取，而不必依赖 app 模块的 Application 类。
 */
class AppUpdateStatusRepository {
    private val _updateStatusFlow = MutableStateFlow<UpdateStatus?>(null)
    val updateStatusFlow: StateFlow<UpdateStatus?> = _updateStatusFlow.asStateFlow()

    private val _remoteStatusFlow = MutableStateFlow<UpdateStatus?>(null)
    val remoteStatusFlow: StateFlow<UpdateStatus?> = _remoteStatusFlow.asStateFlow()

    val updateStatus: UpdateStatus?
        get() = _updateStatusFlow.value

    val remoteStatus: UpdateStatus?
        get() = _remoteStatusFlow.value

    fun setUpdateStatus(status: UpdateStatus?) {
        _updateStatusFlow.value = status
    }

    fun setRemoteStatus(status: UpdateStatus?) {
        _remoteStatusFlow.value = status
    }

    /**
     * 由界面请求刷新远程配置；实际刷新动作由 Application 注册的处理器执行，
     * 这样 feature 模块不需要依赖 app 模块。
     */
    @Volatile
    var remoteRefreshRequest: ((ignoreVerification: Boolean) -> Unit)? = null

    fun requestRemoteRefresh(ignoreVerification: Boolean = false) {
        remoteRefreshRequest?.invoke(ignoreVerification)
    }

    fun refreshAfterVerification() {
        requestRemoteRefresh(ignoreVerification = true)
    }
}

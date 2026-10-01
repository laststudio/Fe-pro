package com.shuaiqiu.fuckets100.app

import android.content.Context
import com.shuaiqiu.fuckets100.FeAccountManager
import com.shuaiqiu.fuckets100.LegalAgreementRepository
import com.shuaiqiu.fuckets100.RemoteConfigManager
import com.shuaiqiu.fuckets100.core.common.AccountAccess
import com.shuaiqiu.fuckets100.core.common.AppBuildInfo
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Process-scoped dependency container.
 *
 * Hilt 图与容器共享同一批 @Singleton 实例；Feature/UI 应从构造函数或
 * ViewModel 注入这些依赖，而不是自行创建全局单例。
 */
@Singleton
class AppContainer @Inject constructor(
    @ApplicationContext context: Context,
    val buildInfo: AppBuildInfo,
    val accountManager: FeAccountManager,
    val accountAccess: AccountAccess,
    val remoteConfigManager: RemoteConfigManager,
    val legalAgreementRepository: LegalAgreementRepository,
) {
    val applicationContext: Context = context.applicationContext
}

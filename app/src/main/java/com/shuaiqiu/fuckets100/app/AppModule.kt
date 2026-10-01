package com.shuaiqiu.fuckets100.app

import android.content.Context
import com.shuaiqiu.fuckets100.FeAccountAccess
import com.shuaiqiu.fuckets100.AppUpdateStatusRepository
import com.shuaiqiu.fuckets100.LegalAgreementRepository
import com.shuaiqiu.fuckets100.RemoteConfigManager
import com.shuaiqiu.fuckets100.FeAccountManager
import com.shuaiqiu.fuckets100.ResolverAuthorizer
import com.shuaiqiu.fuckets100.core.common.AccountAccess
import com.shuaiqiu.fuckets100.core.common.AppBuildInfo
import com.shuaiqiu.fuckets100.core.common.AuthCacheInvalidator
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideAppBuildInfo(): AppBuildInfo = AppBuildInfo(
        applicationId = com.shuaiqiu.fuckets100.BuildConfig.APPLICATION_ID,
        versionCode = com.shuaiqiu.fuckets100.BuildConfig.VERSION_CODE,
        versionName = com.shuaiqiu.fuckets100.BuildConfig.VERSION_NAME,
        expectedPackageName = com.shuaiqiu.fuckets100.BuildConfig.EXPECTED_PACKAGE_NAME,
        expectedSignatureSha256 = com.shuaiqiu.fuckets100.BuildConfig.EXPECTED_SIGNATURE_SHA256,
    )

    @Provides
    @Singleton
    fun provideAuthCacheInvalidator(): AuthCacheInvalidator = ResolverAuthorizer

    @Provides
    @Singleton
    fun provideAppUpdateStatusRepository(): AppUpdateStatusRepository = AppUpdateStatusRepository()

    @Provides
    @Singleton
    fun provideRemoteConfigManager(buildInfo: AppBuildInfo): RemoteConfigManager =
        RemoteConfigManager(buildInfo)

    @Provides
    @Singleton
    fun provideLegalAgreementRepository(
        @ApplicationContext context: Context,
        buildInfo: AppBuildInfo,
    ): LegalAgreementRepository = LegalAgreementRepository(context.applicationContext, buildInfo)

    @Provides
    @Singleton
    fun provideFeAccountManager(
        @ApplicationContext context: Context,
        buildInfo: AppBuildInfo,
        authCacheInvalidator: AuthCacheInvalidator,
        remoteConfigManager: RemoteConfigManager,
    ): FeAccountManager = FeAccountManager.create(
        context = context,
        buildInfo = buildInfo,
        authCacheInvalidator = authCacheInvalidator,
        remoteConfigManager = remoteConfigManager,
    )

    @Provides
    @Singleton
    fun provideAccountAccess(manager: FeAccountManager): AccountAccess = FeAccountAccess(manager)
}

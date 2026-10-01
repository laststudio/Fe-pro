package com.shuaiqiu.fuckets100.app

import android.content.Context
import com.shuaiqiu.fuckets100.FeAccountManager
import com.shuaiqiu.fuckets100.core.common.AppBuildInfo
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
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
    fun provideFeAccountManager(@ApplicationContext context: Context): FeAccountManager =
        FeAccountManager.get(context)
}

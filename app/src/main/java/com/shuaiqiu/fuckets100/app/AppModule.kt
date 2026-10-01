package com.shuaiqiu.fuckets100.app

import com.shuaiqiu.fuckets100.core.common.AppBuildInfo
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
    fun provideAppBuildInfo(): AppBuildInfo = AppBuildInfo.fromBuildConfig()
}

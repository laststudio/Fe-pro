package com.shuaiqiu.fuckets100.app

import android.content.Context
import com.shuaiqiu.fuckets100.core.common.AppBuildInfo
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Process-scoped dependency container.
 *
 * The container intentionally starts small. Services are migrated here in later
 * phases so feature code can stop depending on global singleton objects without
 * changing the existing public APIs in one risky step.
 */
@Singleton
class AppContainer @Inject constructor(
    @ApplicationContext context: Context,
    val buildInfo: AppBuildInfo,
) {
    val applicationContext: Context = context.applicationContext
}

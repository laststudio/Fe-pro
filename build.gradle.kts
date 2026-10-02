// Top-level build file where you can add configuration options common to all sub-projects/modules.
import com.android.build.api.variant.LibraryAndroidComponentsExtension

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.ksp) apply false
}

// 没有 instrumentation 测试的库模块不应生成 androidTest 变体。
//
// 这些模块没有 applicationId，AGP 生成的测试 APK 会把 targetPackage 指向自身，
// 在真机上启动即崩溃（Process crashed），导致 `./gradlew connectedDebugAndroidTest`
// 整体失败。这里统一在缺少 src/androidTest 时禁用该变体；一旦某个模块真正加入
// 设备测试（创建 src/androidTest 目录），该模块会自动恢复 instrumentation 变体。
subprojects {
    plugins.withId("com.android.library") {
        if (!file("src/androidTest").exists()) {
            extensions.configure<LibraryAndroidComponentsExtension> {
                beforeVariants(selector().all()) { variant ->
                    variant.androidTest?.enable = false
                }
            }
        }
    }
}
